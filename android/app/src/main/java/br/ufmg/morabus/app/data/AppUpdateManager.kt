package br.ufmg.morabus.app.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
        scope.launch {
            try {
                val currentVersionCode = getInstalledVersionCode(context)
                val remote = fetchRemoteVersion()

                if (remote != null && remote.versionCode > currentVersionCode) {
                    _availableUpdate.value = remote
                    withContext(Dispatchers.Main) {
                        onResult?.invoke(true, remote)
                    }
                } else {
                    _availableUpdate.value = null
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
            7 // Default da v3.1.0
        }
    }

    private fun fetchRemoteVersion(): RemoteAppVersion? {
        // Segurança: consulta exclusivamente o repositório oficial no GitHub (onde apenas o autor tem permissão de escrita)
        val jsonStr = httpGet(GITHUB_VERSION_URL) ?: return null
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
                RemoteAppVersion(code, name, url, notes, force)
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
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
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
                val apkFile = downloadApk(context, update.apkUrl, update.versionName)
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

    private suspend fun downloadApk(context: Context, apkUrl: String, versionName: String): File = withContext(Dispatchers.IO) {
        val destDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.cacheDir
        if (!destDir.exists()) destDir.mkdirs()
        val destFile = File(destDir, "morabus_$versionName.apk")
        if (destFile.exists()) destFile.delete()

        var conn: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            var targetUrl = apkUrl
            var redirects = 0
            while (redirects < 5) {
                val url = URL(targetUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.instanceFollowRedirects = true
                conn.connectTimeout = 15000
                conn.readTimeout = 30000
                conn.setRequestProperty("User-Agent", "MorabusUFMG-Updater/3.1.1")
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
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
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
