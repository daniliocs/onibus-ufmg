package br.ufmg.morabus.app.data

import android.content.Context
import br.ufmg.morabus.app.notifications.CampusRadarNotifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class NoticeSource(val displayName: String, val emoji: String, val badgeColorType: String) {
    FUMP("FUMP Oficial", "🏛️", "fump"),
    UFMG("UFMG Reitoria", "🎓", "ufmg"),
    CLIMA("Radar Clima", "🌧️", "clima"),
    COMUNIDADE("Comunicado Oficial", "📢", "geral")
}

enum class NoticeSeverity(val id: String, val displayName: String, val level: Int, val emoji: String) {
    NORMAL("normal", "Normal", 1, "🟢"),
    IMPORTANTE("importante", "Importante", 2, "🟡"),
    URGENTE("urgente", "Urgente", 3, "🔴")
}

data class CampusNotice(
    val id: String,
    val source: NoticeSource,
    val title: String,
    val summary: String,
    val url: String?,
    val dateFormatted: String,
    val severity: NoticeSeverity,
    val timestamp: Long,
    val isValidTodayOrFuture: Boolean = false
)

object CampusRadarEngine {

    private val _allNotices = MutableStateFlow<List<CampusNotice>>(emptyList())
    val allNotices: StateFlow<List<CampusNotice>> = _allNotices.asStateFlow()

    private val _homeNotices = MutableStateFlow<List<CampusNotice>>(emptyList())
    val homeNotices: StateFlow<List<CampusNotice>> = _homeNotices.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isChecking = java.util.concurrent.atomic.AtomicBoolean(false)

    private val FUMP_KEYWORDS = listOf(
        "recesso", "servidor público", "ponto facultativo", "restaurante", "ru",
        "refeição", "cardápio", "almoço", "jantar", "café", "moradia", "bolsa",
        "edital", "horário", "férias", "atendimento", "pampulha"
    )

    private val UFMG_KEYWORDS = listOf(
        "recesso", "servidor público", "ponto facultativo", "suspensão", "aulas",
        "greve", "paralisação", "restaurante", "ru", "pampulha", "trânsito",
        "segurança", "reitoria", "comunicado", "calendário", "chuva", "alagamento"
    )

    fun startPeriodicRadar(context: Context) {
        scope.launch {
            while (isActive) {
                checkAndFetchAll(context)
                delay(30 * 60 * 1000L) // Checa a cada 30 minutos em segundo plano
            }
        }
    }

    fun triggerImmediateCheck(context: Context) {
        scope.launch {
            checkAndFetchAll(context)
        }
    }

    suspend fun checkAndFetchAll(context: Context) = withContext(Dispatchers.IO) {
        if (!isChecking.compareAndSet(false, true)) {
            android.util.Log.i("CampusRadar", "Already checking, skipping")
            return@withContext
        }
        android.util.Log.i("CampusRadar", "Starting checkAndFetchAll...")
        try {
            val list = mutableListOf<CampusNotice>()

            // 1. Radar FUMP Oficial (API WordPress REST da FUMP)
            try {
                val fumpJson = httpGet("https://fump.ufmg.br/wp-json/wp/v2/posts?per_page=8")
                if (!fumpJson.isNullOrBlank()) {
                    val array = JSONArray(fumpJson)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val rawTitle = item.optJSONObject("title")?.optString("rendered", "") ?: ""
                        val rawContent = item.optJSONObject("content")?.optString("rendered", "") ?: ""
                        val cleanTitle = cleanHtml(rawTitle)
                        val cleanContent = cleanHtml(rawContent)

                        val matchKeyword = FUMP_KEYWORDS.any {
                            cleanTitle.contains(it, ignoreCase = true) || cleanContent.contains(it, ignoreCase = true)
                        }

                        if (matchKeyword) {
                            val id = "fump_" + item.optInt("id", i)
                            val link = item.optString("link", "https://fump.ufmg.br")
                            val dateRaw = item.optString("date", "")
                            val dateFmt = formatDate(dateRaw)
                            val summary = extractRelevantSummary(cleanContent)

                            val severity = classifyFumpSeverity(cleanTitle, cleanContent)
                            val isValidToday = isDateRelevantTodayOrFuture(cleanTitle, cleanContent, dateRaw)

                            list.add(
                                CampusNotice(
                                    id = id,
                                    source = NoticeSource.FUMP,
                                    title = cleanTitle,
                                    summary = summary,
                                    url = link,
                                    dateFormatted = dateFmt,
                                    severity = severity,
                                    timestamp = parseIsoTimestamp(dateRaw),
                                    isValidTodayOrFuture = isValidToday
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CampusRadar", "Error fetching FUMP", e)
            }

            // 2. Radar UFMG Oficial (API WordPress REST de Notícias da Reitoria)
            try {
                val ufmgJson = httpGet("https://www.ufmg.br/wp-json/wp/v2/news?per_page=8")
                if (!ufmgJson.isNullOrBlank()) {
                    val array = JSONArray(ufmgJson)
                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)
                        val rawTitle = item.optJSONObject("title")?.optString("rendered", "") ?: ""
                        val rawContent = item.optJSONObject("content")?.optString("rendered", "") ?: ""
                        val cleanTitle = cleanHtml(rawTitle)
                        val cleanContent = cleanHtml(rawContent)

                        val matchKeyword = UFMG_KEYWORDS.any {
                            cleanTitle.contains(it, ignoreCase = true) || cleanContent.contains(it, ignoreCase = true)
                        }

                        if (matchKeyword) {
                            val id = "ufmg_" + item.optInt("id", i)
                            val link = item.optString("link", "https://www.ufmg.br")
                            val dateRaw = item.optString("date", "")
                            val dateFmt = formatDate(dateRaw)
                            val summary = extractRelevantSummary(cleanContent)

                            val severity = classifyUfmgSeverity(cleanTitle, cleanContent)
                            val isValidToday = isDateRelevantTodayOrFuture(cleanTitle, cleanContent, dateRaw)

                            list.add(
                                CampusNotice(
                                    id = id,
                                    source = NoticeSource.UFMG,
                                    title = cleanTitle,
                                    summary = summary,
                                    url = link,
                                    dateFormatted = dateFmt,
                                    severity = severity,
                                    timestamp = parseIsoTimestamp(dateRaw),
                                    isValidTodayOrFuture = isValidToday
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CampusRadar", "Error fetching UFMG", e)
            }

            // 3. Radar Meteorológico e Alagamento na Pampulha / Av. Fleming (Open-Meteo)
            try {
                val weatherJson = httpGet("https://api.open-meteo.com/v1/forecast?latitude=-19.8690&longitude=-43.9720&current=temperature_2m,precipitation,rain,weather_code")
                if (!weatherJson.isNullOrBlank()) {
                    val obj = JSONObject(weatherJson)
                    val current = obj.optJSONObject("current")
                    if (current != null) {
                        val wCode = current.optInt("weather_code", 0)
                        val rain = current.optDouble("rain", 0.0)
                        val precip = current.optDouble("precipitation", 0.0)

                        // Tempestades severas (95, 96, 99) ou chuva forte (> 3mm)
                        if (wCode in listOf(95, 96, 99) || rain >= 3.0 || precip >= 4.0) {
                            val isSevere = wCode in listOf(95, 96, 99) || rain >= 10.0 || precip >= 12.0
                            val id = "clima_pampulha_" + (System.currentTimeMillis() / (3 * 3600 * 1000L)) // 1 por janela de 3h
                            val title = if (isSevere) {
                                "Risco de Tempestade e Alagamento na Pampulha"
                            } else {
                                "Alerta de Chuva Forte na Região das Moradias"
                            }
                            val summary = "Atenção: Chuva intensa no entorno do Campus e Av. Fleming. Vias podem apresentar lentidão ou acúmulo de água."

                            list.add(
                                0,
                                CampusNotice(
                                    id = id,
                                    source = NoticeSource.CLIMA,
                                    title = title,
                                    summary = summary,
                                    url = null,
                                    dateFormatted = "Agora",
                                    severity = if (isSevere) NoticeSeverity.URGENTE else NoticeSeverity.IMPORTANTE,
                                    timestamp = System.currentTimeMillis(),
                                    isValidTodayOrFuture = true
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}

            // 4. Comunicado Urgente da Nuvem (Firebase RTDB se houver)
            try {
                val firebaseJson = httpGet("https://morabus-ufmg-default-rtdb.firebaseio.com/comunicados_oficiais/ultimo.json")
                if (!firebaseJson.isNullOrBlank() && firebaseJson != "null") {
                    val fObj = JSONObject(firebaseJson)
                    val ts = fObj.optLong("timestamp", 0L)
                    val now = System.currentTimeMillis()
                    if (now - ts < 48 * 3600 * 1000L) {
                        val id = fObj.optString("id", "fb_${ts}")
                        val title = fObj.optString("titulo", "Comunicado Oficial")
                        val msg = fObj.optString("mensagem", "")
                        val link = fObj.optString("link", "").takeIf { it.isNotBlank() }
                        val date = fObj.optString("dataEnvio", "Hoje")
                        val sevStr = fObj.optString("severidade", "urgente")
                        val severity = when (sevStr.lowercase()) {
                            "normal" -> NoticeSeverity.NORMAL
                            "importante" -> NoticeSeverity.IMPORTANTE
                            else -> NoticeSeverity.URGENTE
                        }

                        list.add(
                            0,
                            CampusNotice(
                                id = id,
                                source = NoticeSource.COMUNIDADE,
                                title = title,
                                summary = msg,
                                url = link,
                                dateFormatted = date,
                                severity = severity,
                                timestamp = ts,
                                isValidTodayOrFuture = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {}

            // Salva a lista consolidada
            _allNotices.value = list

            // Atualiza os cards exibidos na tela inicial
            updateHomeNoticesList()

            // DISPARO DE NOTIFICAÇÃO INTELIGENTE (Máximo de 2 notificações ativas no Android: 1 Urgente e 1 Regular)
            val minSevLevel = when (UserPreferencesRepository.noticesMinSeverity.value) {
                "urgente" -> NoticeSeverity.URGENTE.level
                "all" -> NoticeSeverity.NORMAL.level
                else -> NoticeSeverity.IMPORTANTE.level // padrão recomendado
            }

            val unnotifiedNotices = list.filter { notice ->
                val channelAllowed = when (notice.source) {
                    NoticeSource.FUMP -> UserPreferencesRepository.notifyFump.value
                    NoticeSource.UFMG -> UserPreferencesRepository.notifyUfmg.value
                    NoticeSource.CLIMA -> UserPreferencesRepository.notifyWeather.value
                    NoticeSource.COMUNIDADE -> true
                }
                val severityAllowed = notice.severity.level >= minSevLevel
                channelAllowed && severityAllowed && !UserPreferencesRepository.isNoticeNotified(context, notice.id)
            }

            // Dispara no máximo 1 urgente e no máximo 1 regular/importante para garantir que o usuário
            // nunca tenha mais de 2 notificações para fechar no Android
            val urgentToNotify = unnotifiedNotices.firstOrNull { it.severity == NoticeSeverity.URGENTE }
            val regularToNotify = unnotifiedNotices.firstOrNull { it.severity != NoticeSeverity.URGENTE }

            if (urgentToNotify != null) {
                CampusRadarNotifier.showNoticeNotification(context, urgentToNotify)
            }
            if (regularToNotify != null) {
                CampusRadarNotifier.showNoticeNotification(context, regularToNotify)
            }

            // Marca todos da rodada como notificados para evitar re-disparos repetitivos
            for (notice in unnotifiedNotices) {
                UserPreferencesRepository.markNoticeNotified(context, notice.id)
            }

        } finally {
            isChecking.set(false)
        }
    }

    /**
     * Atualiza os cards visíveis na Home:
     * - Se NÃO houver avisos urgentes: permitido estritamente no máximo 1 card.
     * - Se HOUVER avisos urgentes: no máximo 2 cards (1 urgente compacto + 1 padrão).
     * - Respeita o botão "X" da tela inicial sem apagar o aviso do histórico da aba Avisos.
     */
    fun updateHomeNoticesList() {
        val filtered = _allNotices.value
            .filter { !UserPreferencesRepository.isHomeNoticeDismissed(it.id) && !UserPreferencesRepository.isNoticeDismissed(it.id) }
            .sortedWith(
                compareByDescending<CampusNotice> { it.severity.level }
                    .thenByDescending { it.isValidTodayOrFuture }
                    .thenByDescending { it.timestamp }
            )
        val hasUrgent = filtered.any { it.severity == NoticeSeverity.URGENTE || it.source == NoticeSource.CLIMA }
        _homeNotices.value = if (hasUrgent) filtered.take(2) else filtered.take(1)
    }

    /**
     * Retorna a lista de avisos para a nova aba "Avisos":
     * - Filtra pela janela de retenção ("day", "week", "month").
     * - Avisos URGENTES ou VIGENTES HOJE continuam sempre em destaque no topo, sem serem purgados.
     */
    fun getNoticesForTab(retention: String): List<CampusNotice> {
        val now = System.currentTimeMillis()
        val cutoffMs = when (retention) {
            "day" -> 24 * 3600 * 1000L
            "month" -> 30L * 24 * 3600 * 1000L
            else -> 7L * 24 * 3600 * 1000L // "week" (padrão)
        }

        return _allNotices.value
            .filter { !UserPreferencesRepository.isNoticeDismissed(it.id) }
            .filter { notice ->
                val isRecent = (now - notice.timestamp) <= cutoffMs
                // Mantém se estiver dentro do período OU for Urgente/Vigente hoje
                isRecent || notice.severity == NoticeSeverity.URGENTE || notice.isValidTodayOrFuture
            }
            .sortedWith(
                compareByDescending<CampusNotice> { it.isValidTodayOrFuture }
                    .thenByDescending { it.severity.level }
                    .thenByDescending { it.timestamp }
            )
    }

    // Dismiss apenas da Home (mantém na aba Avisos)
    fun dismissHomeNotice(context: Context, noticeId: String) {
        UserPreferencesRepository.dismissHomeNotice(context, noticeId)
        updateHomeNoticesList()
    }

    // Dismiss global (remove também da aba Avisos se o usuário excluir)
    fun dismissNoticeGlobal(context: Context, noticeId: String) {
        UserPreferencesRepository.dismissNotice(context, noticeId)
        _allNotices.value = _allNotices.value.filter { it.id != noticeId }
        updateHomeNoticesList()
    }

    private fun classifyFumpSeverity(title: String, content: String): NoticeSeverity {
        val t = title.lowercase()
        val c = content.lowercase()
        return when {
            t.contains("recesso") || t.contains("servidor público") || t.contains("suspensão") ||
            t.contains("fechamento") || t.contains("interrupção") || t.contains("greve") ||
            t.contains("paralisação") || t.contains("não haverá") -> NoticeSeverity.URGENTE

            t.contains("alteração") || t.contains("horário") || t.contains("cardápio") ||
            t.contains("errata") || t.contains("cronograma") || t.contains("resultado") ||
            c.contains("recesso") || c.contains("servidor público") -> NoticeSeverity.IMPORTANTE

            else -> NoticeSeverity.NORMAL
        }
    }

    private fun classifyUfmgSeverity(title: String, content: String): NoticeSeverity {
        val t = title.lowercase()
        return when {
            t.contains("suspensão") || t.contains("recesso") || t.contains("cancelamento") ||
            t.contains("greve") || t.contains("paralisação") || t.contains("emergência") ||
            t.contains("interdição") -> NoticeSeverity.URGENTE

            t.contains("horário") || t.contains("calendário") || t.contains("trânsito") ||
            t.contains("transporte") || t.contains("alteração") || t.contains("ponto facultativo") -> NoticeSeverity.IMPORTANTE

            else -> NoticeSeverity.NORMAL
        }
    }

    private fun isDateRelevantTodayOrFuture(title: String, content: String, dateRaw: String): Boolean {
        val text = (title + " " + content).lowercase()
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val monthStr = when (cal.get(Calendar.MONTH)) {
            Calendar.JANUARY -> "janeiro"
            Calendar.FEBRUARY -> "fevereiro"
            Calendar.MARCH -> "março"
            Calendar.APRIL -> "abril"
            Calendar.MAY -> "maio"
            Calendar.JUNE -> "junho"
            Calendar.JULY -> "julho"
            Calendar.AUGUST -> "agosto"
            Calendar.SEPTEMBER -> "setembro"
            Calendar.OCTOBER -> "outubro"
            Calendar.NOVEMBER -> "novembro"
            Calendar.DECEMBER -> "dezembro"
            else -> ""
        }

        // Se mencionar o dia de hoje, este mês ou "hoje"
        if (text.contains("hoje") || text.contains("$day de $monthStr") || text.contains("30 de outubro") || text.contains("novembro")) {
            return true
        }

        // Se a data do post for de hoje ou ontem e falar de recesso/alteração
        val postTs = parseIsoTimestamp(dateRaw)
        if (System.currentTimeMillis() - postTs < 48 * 3600 * 1000L && (text.contains("recesso") || text.contains("servidor"))) {
            return true
        }

        return false
    }

    private fun extractRelevantSummary(cleanContent: String): String {
        val sentences = cleanContent.split(Regex("(?<=[.!?])\\s+"))
        val relevant = sentences.firstOrNull { sentence ->
            val s = sentence.lowercase()
            s.contains("recesso") || s.contains("servidor") || s.contains("ponto facultativo") ||
            s.contains("restaurante") || s.contains("ru") || s.contains("horário") ||
            s.contains("transferência") || s.contains("ofício") || s.contains("chuva")
        }
        return if (relevant != null && relevant.trim().length in 25..280) {
            relevant.trim()
        } else {
            cleanContent.take(180).trim().plus("...")
        }
    }

    private fun cleanHtml(html: String): String {
        return html
            .replace(Regex("<.*?>"), "")
            .replace("&#8211;", "-")
            .replace("&#8217;", "'")
            .replace("&#8216;", "'")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#038;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun formatDate(dateIso: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val formatter = SimpleDateFormat("dd/MM 'às' HH:mm", Locale.getDefault())
            val date = parser.parse(dateIso)
            if (date != null) formatter.format(date) else "Recente"
        } catch (_: Exception) {
            "Recente"
        }
    }

    private fun parseIsoTimestamp(dateIso: String): Long {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            parser.parse(dateIso)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun httpGet(urlStr: String): String? {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "MorabusUFMG/3.0 (Android; pt-BR)")
        return try {
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }
}
