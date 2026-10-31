package br.ufmg.morabus.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserPreferencesRepository {

    private const val PREFS_NAME = "morabus_user_prefs"
    private const val KEY_FAVORITE_STOP = "key_favorite_stop"
    private const val KEY_RADAR_ENABLED = "key_radar_enabled"
    private const val KEY_THEME_MODE = "key_theme_mode" // "system", "dark", "light"
    private const val KEY_VIBRATION_ENABLED = "key_vibration_enabled"
    private const val KEY_SCHEDULED_REMINDERS = "key_scheduled_reminders"
    private const val KEY_DISMISSED_NOTICES = "key_dismissed_notices"
    private const val KEY_NOTIFIED_NOTICES = "key_notified_notices"

    // Novas preferências v3.0: Histórico de Avisos e Filtro de Severidade
    private const val KEY_NOTICES_RETENTION = "key_notices_retention" // "day", "week", "month"
    private const val KEY_NOTICES_MIN_SEVERITY = "key_notices_min_severity" // "urgente", "importante", "all"
    private const val KEY_NOTIFY_FUMP = "key_notify_fump"
    private const val KEY_NOTIFY_UFMG = "key_notify_ufmg"
    private const val KEY_NOTIFY_WEATHER = "key_notify_weather"
    private const val KEY_DISMISSED_HOME_NOTICES = "key_dismissed_home_notices"

    // Retenção de filtros das abas Horários e Avisos
    private const val KEY_SCHEDULES_HIDE_PAST = "key_schedules_hide_past"
    private const val KEY_SCHEDULES_FILTER = "key_schedules_filter" // "todos", "moradia", "campus", "passagem"
    private const val KEY_AVISOS_SOURCE_FILTER = "key_avisos_source_filter" // null, "FUMP", "UFMG", "CLIMA"
    private const val KEY_INSTALL_ID = "key_install_id"
    private const val KEY_COMMUNITY_USER_COUNT = "key_community_user_count"

    private val _favoriteStop = MutableStateFlow<String?>(null)
    val favoriteStop: StateFlow<String?> = _favoriteStop.asStateFlow()

    private val _radarEnabled = MutableStateFlow(true)
    val radarEnabled: StateFlow<Boolean> = _radarEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow("system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _scheduledReminders = MutableStateFlow<Set<String>>(emptySet())
    val scheduledReminders: StateFlow<Set<String>> = _scheduledReminders.asStateFlow()

    private val _dismissedNoticeIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedNoticeIds: StateFlow<Set<String>> = _dismissedNoticeIds.asStateFlow()

    // Retenção do histórico de avisos: "day", "week" (padrão), "month"
    private val _noticesRetention = MutableStateFlow("week")
    val noticesRetention: StateFlow<String> = _noticesRetention.asStateFlow()

    // Nível mínimo de importância para disparar notificações: "urgente", "importante" (padrão), "all"
    private val _noticesMinSeverity = MutableStateFlow("importante")
    val noticesMinSeverity: StateFlow<String> = _noticesMinSeverity.asStateFlow()

    // Toggles de canais de notificação
    private val _notifyFump = MutableStateFlow(true)
    val notifyFump: StateFlow<Boolean> = _notifyFump.asStateFlow()

    private val _notifyUfmg = MutableStateFlow(true)
    val notifyUfmg: StateFlow<Boolean> = _notifyUfmg.asStateFlow()

    private val _notifyWeather = MutableStateFlow(true)
    val notifyWeather: StateFlow<Boolean> = _notifyWeather.asStateFlow()

    // Avisos dispensados apenas da tela de Início (mantendo-os na aba Avisos)
    private val _dismissedHomeNoticeIds = MutableStateFlow<Set<String>>(emptySet())
    val dismissedHomeNoticeIds: StateFlow<Set<String>> = _dismissedHomeNoticeIds.asStateFlow()

    // Retenção do estado da aba Horários (ocultar passados e filtro de origem)
    private val _schedulesHidePast = MutableStateFlow(false)
    val schedulesHidePast: StateFlow<Boolean> = _schedulesHidePast.asStateFlow()

    private val _schedulesFilter = MutableStateFlow("todos")
    val schedulesFilter: StateFlow<String> = _schedulesFilter.asStateFlow()

    // Retenção do estado de filtro da aba Avisos
    private val _avisosSourceFilter = MutableStateFlow<String?>(null)
    val avisosSourceFilter: StateFlow<String?> = _avisosSourceFilter.asStateFlow()

    // Contador de moradores com o app instalado (número real de instalações registradas)
    private val _communityUserCount = MutableStateFlow(1)
    val communityUserCount: StateFlow<Int> = _communityUserCount.asStateFlow()

    private var cachedInstallId: String? = null

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_FAVORITE_STOP, null)
        _favoriteStop.value = if (saved.isNullOrBlank()) null else saved
        _radarEnabled.value = prefs.getBoolean(KEY_RADAR_ENABLED, true)
        _themeMode.value = prefs.getString(KEY_THEME_MODE, "system") ?: "system"
        _vibrationEnabled.value = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        _scheduledReminders.value = prefs.getStringSet(KEY_SCHEDULED_REMINDERS, emptySet()) ?: emptySet()
        _dismissedNoticeIds.value = prefs.getStringSet(KEY_DISMISSED_NOTICES, emptySet()) ?: emptySet()

        _noticesRetention.value = prefs.getString(KEY_NOTICES_RETENTION, "week") ?: "week"
        _noticesMinSeverity.value = prefs.getString(KEY_NOTICES_MIN_SEVERITY, "importante") ?: "importante"
        _notifyFump.value = prefs.getBoolean(KEY_NOTIFY_FUMP, true)
        _notifyUfmg.value = prefs.getBoolean(KEY_NOTIFY_UFMG, true)
        _notifyWeather.value = prefs.getBoolean(KEY_NOTIFY_WEATHER, true)
        _dismissedHomeNoticeIds.value = prefs.getStringSet(KEY_DISMISSED_HOME_NOTICES, emptySet()) ?: emptySet()

        _schedulesHidePast.value = prefs.getBoolean(KEY_SCHEDULES_HIDE_PAST, false)
        _schedulesFilter.value = prefs.getString(KEY_SCHEDULES_FILTER, "todos") ?: "todos"
        _avisosSourceFilter.value = prefs.getString(KEY_AVISOS_SOURCE_FILTER, null)
        _communityUserCount.value = prefs.getInt(KEY_COMMUNITY_USER_COUNT, 1)
        cachedInstallId = prefs.getString(KEY_INSTALL_ID, null)
        if (cachedInstallId.isNullOrBlank()) {
            cachedInstallId = java.util.UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_INSTALL_ID, cachedInstallId).apply()
        }

        isInitialized = true
    }

    fun setFavoriteStop(context: Context, stopName: String?) {
        val cleanName = if (stopName.isNullOrBlank()) null else stopName
        _favoriteStop.value = cleanName
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (cleanName != null) {
                putString(KEY_FAVORITE_STOP, cleanName)
            } else {
                remove(KEY_FAVORITE_STOP)
            }
            apply()
        }
        try {
            br.ufmg.morabus.app.widget.MorabusWidgetProvider.updateAllWidgets(context)
        } catch (_: Exception) {}
    }

    fun setRadarEnabled(context: Context, enabled: Boolean) {
        _radarEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_RADAR_ENABLED, enabled).apply()
    }

    fun setThemeMode(context: Context, mode: String) {
        _themeMode.value = mode
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setVibrationEnabled(context: Context, enabled: Boolean) {
        _vibrationEnabled.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply()
    }

    fun setNoticesRetention(context: Context, retention: String) {
        _noticesRetention.value = retention
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTICES_RETENTION, retention).apply()
    }

    fun setNoticesMinSeverity(context: Context, severity: String) {
        _noticesMinSeverity.value = severity
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTICES_MIN_SEVERITY, severity).apply()
    }

    fun setNotifyFump(context: Context, enabled: Boolean) {
        _notifyFump.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFY_FUMP, enabled).apply()
    }

    fun setNotifyUfmg(context: Context, enabled: Boolean) {
        _notifyUfmg.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFY_UFMG, enabled).apply()
    }

    fun setNotifyWeather(context: Context, enabled: Boolean) {
        _notifyWeather.value = enabled
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFY_WEATHER, enabled).apply()
    }

    fun toggleReminder(context: Context, tripKey: String): Boolean {
        val current = _scheduledReminders.value.toMutableSet()
        val isAdded: Boolean
        if (current.contains(tripKey)) {
            current.remove(tripKey)
            isAdded = false
        } else {
            current.add(tripKey)
            isAdded = true
        }
        _scheduledReminders.value = current
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SCHEDULED_REMINDERS, current).apply()
        return isAdded
    }

    fun isReminderSet(tripKey: String): Boolean {
        return _scheduledReminders.value.contains(tripKey)
    }

    // Dismissal específico da tela Início (mantendo o aviso na aba Avisos)
    fun dismissHomeNotice(context: Context, noticeId: String) {
        val current = _dismissedHomeNoticeIds.value.toMutableSet()
        current.add(noticeId)
        _dismissedHomeNoticeIds.value = current
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_DISMISSED_HOME_NOTICES, current).apply()
    }

    fun isHomeNoticeDismissed(noticeId: String): Boolean {
        return _dismissedHomeNoticeIds.value.contains(noticeId)
    }

    // Dismissal global (se o usuário excluir explicitamente da aba Avisos)
    fun dismissNotice(context: Context, noticeId: String) {
        val current = _dismissedNoticeIds.value.toMutableSet()
        current.add(noticeId)
        _dismissedNoticeIds.value = current
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_DISMISSED_NOTICES, current).apply()
    }

    fun isNoticeDismissed(noticeId: String): Boolean {
        return _dismissedNoticeIds.value.contains(noticeId)
    }

    fun isNoticeNotified(context: Context, noticeId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_NOTIFIED_NOTICES, emptySet()) ?: emptySet()
        return set.contains(noticeId)
    }

    fun markNoticeNotified(context: Context, noticeId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getStringSet(KEY_NOTIFIED_NOTICES, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(noticeId)
        prefs.edit().putStringSet(KEY_NOTIFIED_NOTICES, current).apply()
    }

    // Setters com retenção persistente para a aba Horários
    fun setSchedulesHidePast(context: Context, hide: Boolean) {
        _schedulesHidePast.value = hide
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SCHEDULES_HIDE_PAST, hide).apply()
    }

    fun setSchedulesFilter(context: Context, filter: String) {
        _schedulesFilter.value = filter
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SCHEDULES_FILTER, filter).apply()
    }

    // Setter com retenção persistente para a aba Avisos
    fun setAvisosSourceFilter(context: Context, filter: String?) {
        _avisosSourceFilter.value = filter
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            if (filter != null) {
                putString(KEY_AVISOS_SOURCE_FILTER, filter)
            } else {
                remove(KEY_AVISOS_SOURCE_FILTER)
            }
            apply()
        }
    }

    fun getInstallId(context: Context): String {
        cachedInstallId?.let { return it }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_INSTALL_ID, null)
        if (id.isNullOrBlank()) {
            id = java.util.UUID.randomUUID().toString().take(12)
            prefs.edit().putString(KEY_INSTALL_ID, id).apply()
        }
        cachedInstallId = id
        return id
    }

    fun setCommunityUserCount(context: Context, count: Int) {
        val safeCount = Math.max(1, count)
        _communityUserCount.value = safeCount
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_COMMUNITY_USER_COUNT, safeCount).apply()
    }
}
