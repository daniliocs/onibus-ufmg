package br.ufmg.morabus.app.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class LiveTelemetry(
    val isActive: Boolean = false,
    val contributorsCount: Int = 0,
    val lat: Double? = null,
    val lon: Double? = null,
    val speedKmh: Double = 0.0,
    val streetName: String? = null,
    val tripTime: String? = null,
    val delayMinutes: Int = 0,
    val alertMessage: String? = null,
    val lastUpdateTimestamp: Long = 0
)

data class PassageReport(
    val stopName: String,
    val timestamp: Long,
    val timeAgoMinutes: Int,
    val formattedText: String
)

object FirebaseRealtimeRepository {

    private const val FIREBASE_URL = "https://morabus-ufmg-default-rtdb.firebaseio.com"

    private val _telemetry = MutableStateFlow(LiveTelemetry())
    val telemetry: StateFlow<LiveTelemetry> = _telemetry.asStateFlow()

    // Relato colaborativo de 1 toque (sem consumo de bateria por GPS contínuo)
    private val _recentPassage = MutableStateFlow<PassageReport?>(null)
    val recentPassage: StateFlow<PassageReport?> = _recentPassage.asStateFlow()

    // Contagem de estudantes esperando no ponto (segurança coletiva)
    private val _waitingCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val waitingCounts: StateFlow<Map<String, Int>> = _waitingCounts.asStateFlow()

    private val _userWaitingStop = MutableStateFlow<String?>(null)
    val userWaitingStop: StateFlow<String?> = _userWaitingStop.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null

    private val sessionId = "android_" + UUID.randomUUID().toString().take(8)
    private var isSharing = false
    private var currentSharingTrip: String? = null
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                fetchLatestData()
                val cal = java.util.Calendar.getInstance()
                val status = ScheduleEngine.calculateHomeStatus(cal)
                // Economia extrema de bateria e dados para estudantes:
                // Fora do horário de operação ou aos domingos, atrasa a consulta para 3 minutos
                val pollDelay = if (status.isServiceActive) 12000L else 180000L
                delay(pollDelay)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    fun triggerRefresh() {
        scope.launch { fetchLatestData() }
    }

    private suspend fun fetchLatestData() = withContext(Dispatchers.IO) {
        try {
            val nowMs = System.currentTimeMillis()
            val cutoffMs = nowMs - 120_000L // 2 minutos de validade

            val telemetriaJson = httpGet("$FIREBASE_URL/telemetria.json")
            val alertaJson = httpGet("$FIREBASE_URL/alertas_comunidade/ultimo.json")

            var validEntries = 0
            var latestTs = 0L
            var bestLat: Double? = null
            var bestLon: Double? = null
            var bestSpeed = 0.0
            var bestStreet: String? = null
            var bestTripTime: String? = null
            var bestDelay = 0

            if (!telemetriaJson.isNullOrBlank() && telemetriaJson != "null") {
                val obj = JSONObject(telemetriaJson)
                val keys = obj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val item = obj.optJSONObject(key) ?: continue
                    val ts = item.optLong("timestamp", 0L)
                    if (ts >= cutoffMs) {
                        validEntries++
                        if (ts > latestTs) {
                            latestTs = ts
                            bestLat = item.optDouble("lat", Double.NaN).takeIf { !it.isNaN() }
                            bestLon = item.optDouble("lon", Double.NaN).takeIf { !it.isNaN() }
                            bestSpeed = item.optDouble("velocidadeKmh", 0.0)
                            bestStreet = item.optString("viaAtual").takeIf { it.isNotBlank() }
                            bestTripTime = item.optString("horarioViagem").takeIf { it.isNotBlank() }
                            bestDelay = item.optInt("atrasoMinutos", 0)
                        }
                    }
                }
            }

            var alertText: String? = null
            if (!alertaJson.isNullOrBlank() && alertaJson != "null") {
                val aObj = JSONObject(alertaJson)
                val ts = aObj.optLong("timestamp", 0L)
                if (nowMs - ts < 30 * 60 * 1000L) { // alerta nos últimos 30 min
                    val cod = aObj.optString("codigo")
                    val hora = aObj.optString("horaEnvio", "")
                    alertText = formatAlertCode(cod, hora)
                }
            }

            val isActiveLive = validEntries > 0 && bestLat != null && bestLon != null

            _telemetry.value = LiveTelemetry(
                isActive = isActiveLive,
                contributorsCount = validEntries,
                lat = bestLat,
                lon = bestLon,
                speedKmh = bestSpeed,
                streetName = bestStreet,
                tripTime = bestTripTime,
                delayMinutes = bestDelay,
                alertMessage = alertText,
                lastUpdateTimestamp = latestTs
            )

            // 3. Relato rápido de passagem recente (1 toque sem GPS contínuo)
            try {
                val passagemJson = httpGet("$FIREBASE_URL/passagens_recentes/ultimo.json")
                if (!passagemJson.isNullOrBlank() && passagemJson != "null") {
                    val pObj = JSONObject(passagemJson)
                    val ts = pObj.optLong("timestamp", 0L)
                    val stop = pObj.optString("ponto")
                    val diffMs = nowMs - ts
                    if (diffMs < 15 * 60 * 1000L && stop.isNotBlank()) {
                        val minAgo = Math.max(0, (diffMs / 60000L).toInt())
                        val text = if (minAgo == 0) "acabou de passar" else "visto há $minAgo min"
                        _recentPassage.value = PassageReport(stop, ts, minAgo, "⚡ Ônibus $text em: $stop")
                    } else {
                        _recentPassage.value = null
                    }
                } else {
                    _recentPassage.value = null
                }
            } catch (_: Exception) {}

            // 4. Estudantes aguardando nos pontos (segurança coletiva)
            try {
                val esperandoJson = httpGet("$FIREBASE_URL/esperando_no_ponto.json")
                val countsMap = mutableMapOf<String, Int>()
                if (!esperandoJson.isNullOrBlank() && esperandoJson != "null") {
                    val rootObj = JSONObject(esperandoJson)
                    val keys = rootObj.keys()
                    while (keys.hasNext()) {
                        val stopKey = keys.next()
                        val stopObj = rootObj.optJSONObject(stopKey) ?: continue
                        var activeInStop = 0
                        val sessionKeys = stopObj.keys()
                        while (sessionKeys.hasNext()) {
                            val sKey = sessionKeys.next()
                            val entry = stopObj.optJSONObject(sKey) ?: continue
                            val ts = entry.optLong("timestamp", 0L)
                            if (nowMs - ts < 30 * 60 * 1000L) { // válido por 30 minutos
                                activeInStop++
                            }
                        }
                        if (activeInStop > 0) {
                            val realName = decodeStopKey(stopKey)
                            countsMap[realName] = activeInStop
                        }
                    }
                }
                _waitingCounts.value = countsMap
            } catch (_: Exception) {}

            // 5. Registro de Instalação e Contagem de Moradores
            try {
                val ctx = appContext
                if (ctx != null) {
                    val installId = UserPreferencesRepository.getInstallId(ctx)
                    val pingJson = "{\"lastSeen\":$nowMs,\"version\":\"3.1.1\"}"
                    httpPut("$FIREBASE_URL/installs/$installId.json", pingJson)

                    // Busca a contagem real de instalações registradas (soma de todas as versões)
                    val installsJson = httpGet("$FIREBASE_URL/installs.json?shallow=true")
                    if (!installsJson.isNullOrBlank() && installsJson != "null") {
                        val rootObj = JSONObject(installsJson)
                        val realCount = rootObj.length()
                        if (realCount > 0) {
                            UserPreferencesRepository.setCommunityUserCount(ctx, realCount)
                        }
                    }
                }
            } catch (_: Exception) {}
        } catch (_: Exception) {
            // Em caso de falha de conexão, mantém o estado anterior
        }
    }

    private fun formatAlertCode(code: String, hora: String): String {
        val horaSuffix = if (hora.isNotBlank()) " às $hora" else ""
        return when (code.lowercase()) {
            "transito", "traffic" -> "⚠️ Trânsito lento informado$horaSuffix"
            "chuva", "rain" -> "🌧️ Chuva forte/alagamento na rota$horaSuffix"
            "defeito", "defect" -> "⚠️ Ônibus com defeito mecânico informado$horaSuffix"
            "acidente", "accident" -> "🚨 Acidente informado no itinerário$horaSuffix"
            "lotado", "crowded" -> "👥 Ônibus muito lotado informado$horaSuffix"
            else -> "Aviso da comunidade$horaSuffix"
        }
    }

    fun startSharing(tripTime: String) {
        isSharing = true
        currentSharingTrip = tripTime
    }

    fun stopSharing() {
        isSharing = false
        currentSharingTrip = null
        scope.launch {
            try {
                httpDelete("$FIREBASE_URL/telemetria/$sessionId.json")
                fetchLatestData()
            } catch (_: Exception) {}
        }
    }

    fun sendLocationTelemetry(
        lat: Double,
        lon: Double,
        speedKmh: Double = 0.0,
        streetName: String? = null,
        delayMinutes: Int = 0,
        tripTime: String? = null
    ) {
        if (!isSharing) return
        scope.launch {
            try {
                val payload = JSONObject().apply {
                    put("id", sessionId)
                    put("lat", lat)
                    put("lon", lon)
                    put("velocidadeKmh", speedKmh)
                    if (!streetName.isNullOrBlank()) {
                        put("viaAtual", streetName)
                    }
                    put("atrasoMinutos", delayMinutes)
                    put("horarioViagem", tripTime ?: currentSharingTrip ?: "")
                    put("timestamp", System.currentTimeMillis())
                }
                httpPut("$FIREBASE_URL/telemetria/$sessionId.json", payload.toString())
                fetchLatestData()
            } catch (_: Exception) {}
        }
    }

    fun sendCommunityAlert(code: String, tripTime: String? = null) {
        scope.launch {
            try {
                val cal = java.util.Calendar.getInstance()
                val horaStr = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
                val payload = JSONObject().apply {
                    put("codigo", code)
                    put("horaEnvio", horaStr)
                    put("horarioViagem", tripTime ?: "")
                    put("timestamp", System.currentTimeMillis())
                }
                httpPut("$FIREBASE_URL/alertas_comunidade/ultimo.json", payload.toString())
                fetchLatestData()
            } catch (_: Exception) {}
        }
    }

    /**
     * Relato rápido de 1 toque: registra que o ônibus acabou de passar neste ponto.
     * Não necessita de GPS contínuo e não gasta bateria.
     */
    fun sendQuickPassageReport(stopName: String, tripTime: String? = null) {
        scope.launch {
            try {
                val now = System.currentTimeMillis()
                val payload = JSONObject().apply {
                    put("ponto", stopName)
                    put("horarioViagem", tripTime ?: "")
                    put("sessionId", sessionId)
                    put("timestamp", now)
                }
                httpPut("$FIREBASE_URL/passagens_recentes/ultimo.json", payload.toString())
                fetchLatestData()
            } catch (_: Exception) {}
        }
    }

    /**
     * Informa que o estudante está aguardando no ponto (segurança coletiva).
     */
    fun registerWaitingAtStop(stopName: String) {
        _userWaitingStop.value = stopName
        scope.launch {
            try {
                val key = encodeStopKey(stopName)
                val payload = JSONObject().apply {
                    put("timestamp", System.currentTimeMillis())
                }
                httpPut("$FIREBASE_URL/esperando_no_ponto/$key/$sessionId.json", payload.toString())
                fetchLatestData()
            } catch (_: Exception) {}
        }
    }

    /**
     * Remove o estudante da fila de espera do ponto.
     */
    fun unregisterWaitingAtStop(stopName: String) {
        if (_userWaitingStop.value == stopName) {
            _userWaitingStop.value = null
        }
        scope.launch {
            try {
                val key = encodeStopKey(stopName)
                httpDelete("$FIREBASE_URL/esperando_no_ponto/$key/$sessionId.json")
                fetchLatestData()
            } catch (_: Exception) {}
        }
    }

    private fun encodeStopKey(name: String): String {
        return name.replace(" ", "_").replace("/", "_").replace(".", "").replace("ç", "c").replace("ã", "a")
    }

    private fun decodeStopKey(key: String): String {
        return key.replace("_", " ")
    }

    private fun httpGet(urlStr: String): String? {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 7000
        conn.readTimeout = 7000
        return try {
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun httpPut(urlStr: String, json: String): Boolean {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "PUT"
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.connectTimeout = 7000
        conn.readTimeout = 7000
        return try {
            OutputStreamWriter(conn.outputStream).use { it.write(json) }
            conn.responseCode in 200..299
        } finally {
            conn.disconnect()
        }
    }

    private fun httpDelete(urlStr: String): Boolean {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "DELETE"
        conn.connectTimeout = 7000
        conn.readTimeout = 7000
        return try {
            conn.responseCode in 200..299
        } finally {
            conn.disconnect()
        }
    }
}
