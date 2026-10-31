package br.ufmg.morabus.app.data

import android.content.Context
import android.util.Log
import android.webkit.WebResourceResponse
import kotlinx.coroutines.*
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object MapTileCacheManager {

    private const val TAG = "MorabusMapCache"
    private const val USER_AGENT = "MorabusUFMG/3.1 (https://github.com/daniliocs/onibus-ufmg; contato: morabus.ufmg@gmail.com)"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isPreCaching = false

    // Bounding Box do Trajeto Moradias ↔ Campus Pampulha
    private const val MIN_LAT = -19.8800
    private const val MAX_LAT = -19.8580
    private const val MIN_LON = -19.8800 // -43.9920
    private const val ROUTE_MIN_LON = -43.9920
    private const val ROUTE_MAX_LON = -43.9550

    private fun getTileCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "map_tiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getTileFile(context: Context, urlString: String): File {
        val sanitized = urlString
            .replace("https://", "")
            .replace("http://", "")
            .replace("/", "_")
            .replace("?", "_")
            .replace("&", "_")
        return File(getTileCacheDir(context), sanitized)
    }

    /**
     * Intercepta o carregamento de blocos do mapa pelo WebView.
     * Se o bloco já estiver em cache no disco, entrega instantaneamente sem tocar na internet.
     * Se não estiver, baixa, salva no disco para sempre e entrega a resposta.
     */
    fun getOrFetchTile(context: Context, urlString: String): WebResourceResponse {
        val tileFile = getTileFile(context, urlString)

        if (tileFile.exists() && tileFile.length() > 0) {
            try {
                return WebResourceResponse(
                    "image/png",
                    null,
                    FileInputStream(tileFile)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao ler bloco em cache", e)
            }
        }

        // Não estava no cache: tenta buscar online
        try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 7000
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode == 200) {
                val bytes = conn.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    scope.launch {
                        try {
                            FileOutputStream(tileFile).use { it.write(bytes) }
                        } catch (_: Exception) {}
                    }
                    return WebResourceResponse(
                        "image/png",
                        null,
                        ByteArrayInputStream(bytes)
                    )
                }
            }
        } catch (_: Exception) {
            // Em caso de offline, não quebra a página
        }

        // Retorna bloco transparente 1x1 em PNG para evitar erros de renderização
        val transparent1x1Png = byteArrayOf(
            0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x1A.toByte(), 0x0A.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0D.toByte(), 0x49.toByte(), 0x48.toByte(), 0x44.toByte(), 0x52.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x01.toByte(),
            0x08.toByte(), 0x06.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x1F.toByte(), 0x15.toByte(), 0xC4.toByte(),
            0x89.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x0A.toByte(), 0x49.toByte(), 0x44.toByte(), 0x41.toByte(),
            0x54.toByte(), 0x78.toByte(), 0x9C.toByte(), 0x63.toByte(), 0x00.toByte(), 0x01.toByte(), 0x00.toByte(), 0x00.toByte(),
            0x05.toByte(), 0x00.toByte(), 0x01.toByte(), 0x0D.toByte(), 0x0A.toByte(), 0x2D.toByte(), 0xB4.toByte(), 0x00.toByte(),
            0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x49.toByte(), 0x45.toByte(), 0x4E.toByte(), 0x44.toByte(), 0xAE.toByte(),
            0x42.toByte(), 0x60.toByte(), 0x82.toByte()
        )
        return WebResourceResponse("image/png", null, ByteArrayInputStream(transparent1x1Png))
    }

    /**
     * Limpa blocos corrompidos ou obsoletos do cache
     */
    fun clearDirtyCache(context: Context) {
        scope.launch {
            try {
                val dir = getTileCacheDir(context)
                dir.listFiles()?.forEach { file ->
                    if (file.name.contains("cartocdn") || file.length() < 1000L) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    /**
     * Pré-carrega em segundo plano os blocos essenciais do corredor Pampulha ↔ Moradias
     * nos níveis de zoom 13, 14 e 15 para garantir funcionamento 100% offline.
     */
    fun preCacheCorridorTiles(context: Context) {
        if (isPreCaching) return
        isPreCaching = true

        scope.launch {
            try {
                val zooms = listOf(13, 14, 15)
                for (z in zooms) {
                    val minX = lonToTileX(ROUTE_MIN_LON, z)
                    val maxX = lonToTileX(ROUTE_MAX_LON, z)
                    val minY = latToTileY(MAX_LAT, z)
                    val maxY = latToTileY(MIN_LAT, z)

                    for (x in minX..maxX) {
                        for (y in minY..maxY) {
                            val url = "https://tile.openstreetmap.org/$z/$x/$y.png"
                            val file = getTileFile(context, url)
                            if (!file.exists() || file.length() == 0L) {
                                try {
                                    val u = URL(url)
                                    val c = u.openConnection() as HttpURLConnection
                                    c.connectTimeout = 4000
                                    c.readTimeout = 5000
                                    c.setRequestProperty("User-Agent", USER_AGENT)
                                    if (c.responseCode == 200) {
                                        val bytes = c.inputStream.use { it.readBytes() }
                                        if (bytes.isNotEmpty()) {
                                            FileOutputStream(file).use { it.write(bytes) }
                                        }
                                    }
                                    c.disconnect()
                                    delay(40L) // Respeita rate limit
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Erro no pré-cache de blocos", e)
            } finally {
                isPreCaching = false
            }
        }
    }

    private fun lonToTileX(lon: Double, zoom: Int): Int {
        return Math.floor((lon + 180.0) / 360.0 * (1 shl zoom)).toInt()
    }

    private fun latToTileY(lat: Double, zoom: Int): Int {
        val latRad = Math.toRadians(lat)
        return Math.floor((1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * (1 shl zoom)).toInt()
    }
}
