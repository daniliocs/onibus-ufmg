package br.ufmg.morabus.app.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

data class RemoteAppVersion(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val isForceUpdate: Boolean = false
)

object AppUpdateManager {

    private const val TAG = "MorabusUpdater"
    private const val GITHUB_VERSION_URL = "https://raw.githubusercontent.com/daniliocs/onibus-ufmg/main/version.json"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _availableUpdate = MutableStateFlow<RemoteAppVersion?>(null)
    val availableUpdate: StateFlow<RemoteAppVersion?> = _availableUpdate.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking: StateFlow<Boolean> = _isChecking.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private val _downloadedApkFile = MutableStateFlow<File?>(null)
    val downloadedApkFile: StateFlow<File?> = _downloadedApkFile.asStateFlow()

    fun checkForUpdates(context: Context, onResult: ((Boolean, RemoteAppVersion?) -> Unit)? = null) {
        if (_isChecking.value) return
        _isChecking.value = true
        _downloadError.value = null
        scope.launch {
            try {
                val currentVersionCode = getInstalledVersionCode(context)
                val remote = fetchRemoteVersion()

                if (remote != null && remote.versionCode > currentVersionCode) {
                    // Verifica se o arquivo baixado anteriormente em memória pertence realmente a esta versão
                    val expectedFileName = getExpectedFileName(remote)
                    val existingFile = _downloadedApkFile.value
                    if (existingFile != null && existingFile.name != expectedFileName) {
                        _downloadedApkFile.value = null
                    }
                    cleanOldCachedApks(context, expectedFileName)

                    _availableUpdate.value = remote
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(true, remote)
                    }
                } else {
                    _availableUpdate.value = null
                    _downloadedApkFile.value = null
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(false, null)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao verificar atualizações", e)
                withContext(Dispatchers.Main) {
                    onResult?.invoke(false, null)
                }
            } finally {
                _isChecking.value = false
            }
        }
    }

    private fun getExpectedFileName(update: RemoteAppVersion): String {
        return "morabus_${update.versionName}_v${update.versionCode}.apk"
    }

    private fun cleanOldCachedApks(context: Context, keepFileName: String) {
        try {
            val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
            destDir.listFiles()?.forEach { f ->
                if (f.name.startsWith("morabus_") && f.name.endsWith(".apk") && f.name != keepFileName) {
                    f.delete()
                }
            }
        } catch (_: Exception) {}
    }

    private fun getInstalledVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            10
        }
    }

    private fun getArchiveVersionCode(context: Context, apkFile: File): Int {
        return try {
            val archiveInfo = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0) ?: return -1
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                archiveInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                archiveInfo.versionCode
            }
        } catch (e: Exception) {
            -1
        }
    }

    private fun fetchRemoteVersion(): RemoteAppVersion? {
        // Anti-cache com timestamp para evitar resposta antiga da CDN
        val cacheBustedUrl = "$GITHUB_VERSION_URL?t=${System.currentTimeMillis()}"
        val jsonStr = httpGet(cacheBustedUrl) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val code = obj.optInt("versionCode", 0)
            val name = obj.optString("versionName", "")
            val url = obj.optString("apkUrl", "")
            val notes = obj.optString("releaseNotes", "")
            val force = obj.optBoolean("forceUpdate", false)

            val isTrustedDomain = url.startsWith("https://raw.githubusercontent.com/daniliocs/onibus-ufmg/") ||
                    url.startsWith("https://github.com/daniliocs/onibus-ufmg/")

            if (code > 0 && url.isNotBlank() && isTrustedDomain) {
                val cleanBaseUrl = url.substringBefore("?")
                val versionedApkUrl = "$cleanBaseUrl?v=$code"
                RemoteAppVersion(code, name, versionedApkUrl, notes, force)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro parseando JSON de versão", e)
            null
        }
    }

    private fun httpGet(urlString: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.useCaches = false
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
            conn.setRequestProperty("Pragma", "no-cache")
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } else null
        } catch (e: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    fun startDownloadAndInstall(context: Context, update: RemoteAppVersion) {
        if (_isDownloading.value) return
        _isDownloading.value = true
        _downloadProgress.value = 0
        _downloadError.value = null

        scope.launch {
            try {
                val apkFile = downloadApk(context, update)
                val downloadedCode = getArchiveVersionCode(context, apkFile)

                if (downloadedCode <= 0) {
                    apkFile.delete()
                    _downloadedApkFile.value = null
                    _downloadError.value = "O arquivo baixado estava incompleto. Toque novamente para baixar."
                    _isDownloading.value = false
                    return@launch
                }

                if (downloadedCode < update.versionCode) {
                    // A CDN do GitHub ainda entregou a versão anterior em cache (janela de até 5 min após publicação)
                    apkFile.delete()
                    _downloadedApkFile.value = null
                    _downloadError.value = "O servidor do GitHub ainda está propagando o novo arquivo da v${update.versionName} (leva ~2 min). Aguarde 1 minuto e toque novamente."
                    _isDownloading.value = false
                    return@launch
                }

                _downloadedApkFile.value = apkFile
                _downloadProgress.value = 100
                _isDownloading.value = false

                withContext(Dispatchers.Main) {
                    promptInstall(context, apkFile)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro baixando APK", e)
                _downloadError.value = "Erro no download: ${e.localizedMessage ?: "Falha de rede"}"
                _isDownloading.value = false
            }
        }
    }

    private suspend fun downloadApk(context: Context, update: RemoteAppVersion): File = withContext(Dispatchers.IO) {
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        if (!destDir.exists()) destDir.mkdirs()
        val destFile = File(destDir, getExpectedFileName(update))
        if (destFile.exists()) destFile.delete()

        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val baseApkUrl = update.apkUrl.substringBefore("?")
            var targetUrl = "$baseApkUrl?v=${update.versionCode}&t=${System.currentTimeMillis()}"
            var redirects = 0
            while (redirects < 5) {
                val url = URL(targetUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.useCaches = false
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "MorabusUFMG-Updater/3.1.4")
                conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                conn.setRequestProperty("Pragma", "no-cache")
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_MOVED_PERM || code == HttpURLConnection.HTTP_MOVED_TEMP || code == 307 || code == 308) {
                    val newLocation = conn.getHeaderField("Location") ?: break
                    conn.disconnect()
                    targetUrl = newLocation
                    redirects++
                } else break
            }

            val fileLength = conn!!.contentLength
            inputStream = conn.inputStream
            outputStream = FileOutputStream(destFile)

            val buffer = ByteArray(8192)
            var total: Long = 0
            var count: Int

            while (inputStream.read(buffer).also { count = it } != -1) {
                total += count
                outputStream.write(buffer, 0, count)
                if (fileLength > 0) {
                    val progress = ((total * 100) / fileLength).toInt()
                    _downloadProgress.value = progress
                }
            }
            outputStream.flush()
            destFile
        } finally {
            outputStream?.close()
            inputStream?.close()
            conn?.disconnect()
        }
    }

    @Suppress("DEPRECATION")
    fun promptInstall(context: Context, file: File? = _downloadedApkFile.value) {
        val apkFile = file ?: return
        if (!apkFile.exists()) return

        try {
            // No Android 8.0+, verifica se o app tem permissão para instalar fontes desconhecidas
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val manageIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(manageIntent)
                    return
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Erro disparando instalador do Android", e)
            _downloadError.value = "Falha ao abrir instalador: ${e.message}"
        }
    }

    fun dismissUpdate() {
        _availableUpdate.value = null
    }
}
