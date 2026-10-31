package br.ufmg.morabus.app.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import br.ufmg.morabus.app.data.FirebaseRealtimeRepository
import br.ufmg.morabus.app.data.ScheduleEngine
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Calendar

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val telemetry by FirebaseRealtimeRepository.telemetry.collectAsState()
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val themeMode by br.ufmg.morabus.app.data.UserPreferencesRepository.themeMode.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    LaunchedEffect(Unit) {
        FirebaseRealtimeRepository.startPolling()
        br.ufmg.morabus.app.data.MapTileCacheManager.clearDirtyCache(context)
        br.ufmg.morabus.app.data.MapTileCacheManager.preCacheCorridorTiles(context)
    }

    LaunchedEffect(isDark) {
        val wv = webViewRef ?: return@LaunchedEffect
        wv.evaluateJavascript("if (window.setMapTheme) { window.setMapTheme($isDark); }", null)
    }

    fun escapeJsSingleQuoted(input: String?): String {
        if (input == null) return ""
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", "")
            .replace("<", "")
            .replace(">", "")
            .take(80)
    }

    LaunchedEffect(telemetry) {
        val wv = webViewRef ?: return@LaunchedEffect
        if (telemetry.isActive && telemetry.lat != null && telemetry.lon != null) {
            val title = escapeJsSingleQuoted(if (telemetry.tripTime != null) "Ônibus das ${telemetry.tripTime}" else "Ônibus ao vivo")
            val subtitle = escapeJsSingleQuoted(telemetry.streetName ?: "Localização em tempo real")
            val js = "if (window.updateBusLocation) { window.updateBusLocation(${telemetry.lat}, ${telemetry.lon}, '$title', '$subtitle'); }"
            wv.evaluateJavascript(js, null)
        } else {
            val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance())
            if (status.activeTrip != null) {
                val origin = status.activeTrip.origin
                val isCampus = origin.contains("Escola")
                val lat = if (isCampus) -19.8685 else -19.8706
                val lon = if (isCampus) -43.9650 else -43.9800
                val title = escapeJsSingleQuoted("Ônibus das ${status.activeTrip.time}")
                val subtitle = escapeJsSingleQuoted(status.estimatedLocationText)
                val js = "if (window.updateBusLocation) { window.updateBusLocation($lat, $lon, '$title', '$subtitle'); }"
                wv.evaluateJavascript(js, null)
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewRef = this
                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?
                    ): android.webkit.WebResourceResponse? {
                        val url = request?.url?.toString()
                        if (url != null && (url.contains("basemaps.cartocdn.com") || url.contains("tile.openstreetmap.org"))) {
                            return br.ufmg.morabus.app.data.MapTileCacheManager.getOrFetchTile(ctx, url)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.i("MorabusMap", "Page finished loading: $url")
                        view?.evaluateJavascript("if (window.mapa) { window.mapa.invalidateSize(); }", null)
                        view?.evaluateJavascript("if (window.setMapTheme) { window.setMapTheme($isDark); }", null)
                        if (telemetry.isActive && telemetry.lat != null && telemetry.lon != null) {
                            val safeStreet = escapeJsSingleQuoted(telemetry.streetName ?: "")
                            val js = "if (window.updateBusLocation) { window.updateBusLocation(${telemetry.lat}, ${telemetry.lon}, 'Ônibus ao vivo', '$safeStreet'); }"
                            view?.evaluateJavascript(js, null)
                        }
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        callback?.invoke(origin, true, false)
                    }

                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        Log.i("MorabusMap", "JS [${consoleMessage?.messageLevel()}]: ${consoleMessage?.message()} (line ${consoleMessage?.lineNumber()})")
                        return true
                    }
                }
                addJavascriptInterface(object {
                    @android.webkit.JavascriptInterface
                    fun setFavoriteStop(stopName: String) {
                        br.ufmg.morabus.app.data.UserPreferencesRepository.setFavoriteStop(ctx, stopName)
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            android.widget.Toast.makeText(ctx, "Ponto favorito definido: $stopName", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                }, "AndroidBridge")

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    allowFileAccess = true
                    allowContentAccess = false
                    @Suppress("DEPRECATION")
                    allowFileAccessFromFileURLs = false
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = false
                    setGeolocationEnabled(true)
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    // Cache automático para economia extrema de dados 4G e mapa offline
                    cacheMode = WebSettings.LOAD_DEFAULT
                    userAgentString = "MorabusUFMG/3.1 (https://github.com/daniliocs/onibus-ufmg; contato: morabus.ufmg@gmail.com)"
                }

                // Carrega assets locais (Leaflet offline zero-data) com suporte a tiles em cache
                try {
                    val htmlContent = ctx.assets.open("public/mapa.html").bufferedReader().use { it.readText() }
                    loadDataWithBaseURL("file:///android_asset/public/", htmlContent, "text/html", "UTF-8", null)
                } catch (e: Exception) {
                    Log.e("MorabusMap", "Error reading mapa.html", e)
                    loadUrl("file:///android_asset/public/mapa.html")
                }
            }
        },
        update = { webView ->
            webViewRef = webView
        }
    )
}
