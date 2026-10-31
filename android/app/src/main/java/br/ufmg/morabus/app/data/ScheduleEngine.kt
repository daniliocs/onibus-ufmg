package br.ufmg.morabus.app.data

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar

data class BusStop(
    val name: String,
    val lat: Double?,
    val lon: Double?,
    val isCampus: Boolean
)

data class RoutePoint(
    val stopName: String,
    val timeMinutes: Int
)

enum class CrowdLevel(val label: String, val emoji: String) {
    BAIXA("Tranquilo", "🟢"),
    MEDIA("Moderado", "🟡"),
    ALTA("Costuma lotar", "🔴")
}

data class ScheduleTrip(
    val time: String,          // "06:45"
    val origin: String,        // "Moradia", "Escola de Música", "Moradias"
    val type: String?,         // "passagem" or null
    val vehicles: Int,         // 1, 2, 3
    val minutesFromMidnight: Int = ScheduleEngine.timeToMinutes(time)
) {
    val isPassagem: Boolean get() = type == "passagem" && origin.contains("Moradia")
    val staysAtCampusOvernight: Boolean get() = isPassagem && (time == "21:45" || time == "22:35" || time == "23:10")
    val destination: String get() = if (origin.contains("Moradia")) {
        if (isPassagem) "Campus UFMG (Ponto final)" else "Escola de Música"
    } else {
        "Moradias 2 e 3"
    }

    val crowdLevel: CrowdLevel get() = when (time) {
        "06:45", "07:20", "07:40", "11:50", "12:15", "17:50", "18:20", "19:00" -> CrowdLevel.ALTA
        "10:00", "11:00", "13:10", "13:30", "17:00", "17:20", "21:00", "22:15" -> CrowdLevel.MEDIA
        else -> CrowdLevel.BAIXA
    }
}

data class SimulatedStopArrival(
    val stopName: String,
    val offsetMinutes: Int,
    val arrivalTimeFormatted: String,
    val minutesFromNow: Int?,
    val isPassed: Boolean
)

data class StopArrivalInfo(
    val stopName: String,
    val scheduledDepartureTime: String,
    val origin: String,
    val arrivalTime: String,
    val minutesUntilArrival: Int,
    val delayMinutes: Int = 0
)

data class HomeStatus(
    val isServiceActive: Boolean,
    val isFinishedToday: Boolean,
    val isHolidayOrSunday: Boolean,
    val holidayOrSundayMessage: String?,
    val calendarAcademicNote: String?,
    val nextTrip: ScheduleTrip?,
    val nextTripMinutes: Int?,
    val nextMoradiaTrip: ScheduleTrip?,
    val nextMoradiaMinutes: Int?,
    val nextCampusTrip: ScheduleTrip?,
    val nextCampusMinutes: Int?,
    val activeTrip: ScheduleTrip?,
    val favoriteArrival: StopArrivalInfo?,
    val estimatedLocationText: String,
    val lastTripToday: ScheduleTrip?,
    val firstTripTomorrow: ScheduleTrip?,
    val nextServiceDayLabel: String = "Amanhã",
    val nextServiceFullText: String = "",
    val nextServiceShortText: String = ""
)

object ScheduleEngine {

    private const val PREFS_SCHEDULES = "morabus_dynamic_schedules"
    private const val KEY_CACHED_JSON = "cached_schedules_json"
    private const val REMOTE_SCHEDULES_URL = "https://raw.githubusercontent.com/daniliocs/onibus-ufmg/main/schedules.json"

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSyncing = false

    val STOPS = listOf(
        BusStop("Moradia 1", -19.8706, -43.9862, false),
        BusStop("Moradias 2 e 3", -19.8661, -43.9845, false),
        BusStop("Escola de Música", -19.8621, -43.9618, true),
        BusStop("Belas Artes", -19.863787, -43.960242, true),
        BusStop("FaE", -19.867845, -43.958610, true),
        BusStop("RU 1", -19.868928, -43.960290, true),
        BusStop("FACE", -19.868211, -43.963662, true),
        BusStop("Reitoria", -19.866381, -43.965288, true),
        BusStop("CAD 1", -19.867173, -43.967189, true),
        BusStop("RU 2", -19.869941, -43.966892, true),
        BusStop("Engenharia", -19.869706, -43.964092, true),
        BusStop("IGC", -19.868652, -43.962783, true),
        BusStop("Farmácia", -19.870094, -43.968643, true),
        BusStop("Veterinária", -19.871567, -43.970531, true),
        BusStop("Odonto", -19.872823, -43.973524, true),
        BusStop("Educação Física", -19.875905, -43.975670, true),
        BusStop("Unidade Administrativa II", -19.863061, -43.966889, true),
        BusStop("Rotatória", -19.863364, -43.964325, true),
        BusStop("Ponto final", -19.862100, -43.961800, true)
    )

    val ROTA_CAMPUS = listOf(
        RoutePoint("Escola de Música", 0),
        RoutePoint("Belas Artes", 1),
        RoutePoint("FaE", 2),
        RoutePoint("RU 1", 3),
        RoutePoint("FACE", 4),
        RoutePoint("Reitoria", 5),
        RoutePoint("CAD 1", 7),
        RoutePoint("Farmácia", 8),
        RoutePoint("Veterinária", 10),
        RoutePoint("Odonto", 11),
        RoutePoint("Educação Física", 12),
        RoutePoint("Moradia 1", 18),
        RoutePoint("Moradias 2 e 3", 20)
    )

    val ROTA_MORADIA = listOf(
        RoutePoint("Moradia 1", 0),
        RoutePoint("Moradias 2 e 3", 2),
        RoutePoint("Educação Física", 7),
        RoutePoint("Odonto", 9),
        RoutePoint("Veterinária", 10),
        RoutePoint("Farmácia", 11),
        RoutePoint("RU 2", 12),
        RoutePoint("Engenharia", 14),
        RoutePoint("IGC", 15),
        RoutePoint("RU 1", 16),
        RoutePoint("FaE", 18),
        RoutePoint("Belas Artes", 20),
        RoutePoint("Escola de Música", 22)
    )

    val ROTA_PASSAGEM = listOf(
        RoutePoint("Moradia 1", 0),
        RoutePoint("Moradias 2 e 3", 2),
        RoutePoint("Unidade Administrativa II", 7),
        RoutePoint("Rotatória", 8),
        RoutePoint("Ponto final", 9)
    )

    private val DEFAULT_HORARIOS_SEMANA = listOf(
        ScheduleTrip("06:45", "Moradia", null, 3),
        ScheduleTrip("07:20", "Escola de Música", null, 3),
        ScheduleTrip("07:40", "Moradia", null, 3),
        ScheduleTrip("10:00", "Escola de Música", null, 3),
        ScheduleTrip("11:00", "Moradia", null, 3),
        ScheduleTrip("11:50", "Escola de Música", null, 3),
        ScheduleTrip("12:15", "Moradia", null, 3),
        ScheduleTrip("13:10", "Escola de Música", null, 3),
        ScheduleTrip("13:30", "Moradia", null, 3),
        ScheduleTrip("15:00", "Escola de Música", null, 1),
        ScheduleTrip("15:20", "Moradia", null, 1),
        ScheduleTrip("17:00", "Escola de Música", null, 2),
        ScheduleTrip("17:20", "Moradia", null, 2),
        ScheduleTrip("17:50", "Escola de Música", null, 2),
        ScheduleTrip("18:20", "Moradia", null, 2),
        ScheduleTrip("19:00", "Escola de Música", null, 2),
        ScheduleTrip("19:20", "Moradias", "passagem", 2),
        ScheduleTrip("19:40", "Escola de Música", null, 2),
        ScheduleTrip("20:00", "Moradias", "passagem", 2),
        ScheduleTrip("21:00", "Escola de Música", null, 2),
        ScheduleTrip("21:20", "Moradias", "passagem", 2),
        ScheduleTrip("21:35", "Escola de Música", null, 1),
        ScheduleTrip("21:45", "Moradias", "passagem", 1),
        ScheduleTrip("22:15", "Escola de Música", null, 1),
        ScheduleTrip("22:35", "Moradias", "passagem", 1),
        ScheduleTrip("22:50", "Escola de Música", null, 1),
        ScheduleTrip("23:10", "Moradias", "passagem", 1)
    )

    private val DEFAULT_HORARIOS_SABADO = listOf(
        ScheduleTrip("07:40", "Moradia", null, 1),
        ScheduleTrip("10:00", "Escola de Música", null, 2),
        ScheduleTrip("11:00", "Moradia", null, 2),
        ScheduleTrip("12:00", "Escola de Música", null, 2),
        ScheduleTrip("12:20", "Moradia", null, 2),
        ScheduleTrip("13:30", "Escola de Música", null, 2)
    )

    @Volatile
    var HORARIOS_SEMANA: List<ScheduleTrip> = DEFAULT_HORARIOS_SEMANA
        private set

    @Volatile
    var HORARIOS_SABADO: List<ScheduleTrip> = DEFAULT_HORARIOS_SABADO
        private set

    private val _calendarBanner = MutableStateFlow("Período Letivo Oficial UFMG • Grade Regular")
    val calendarBanner: StateFlow<String> = _calendarBanner.asStateFlow()

    private val dynamicExtraHolidays = mutableMapOf<String, String>()

    // Feriados Nacionais, Estaduais (Minas Gerais), Municipais (Belo Horizonte) e Recessos Fixos do Calendário Acadêmico UFMG
    private val FERIADOS_OFICIAIS = mapOf(
        "01/01" to "Feriado Nacional: Confraternização Universal",
        "21/04" to "Feriado Nacional/MG: Tiradentes e Data Magna de Minas",
        "01/05" to "Feriado Nacional: Dia do Trabalhador",
        "15/08" to "Feriado Municipal (BH): Assunção de Nossa Senhora",
        "07/09" to "Feriado Nacional: Independência do Brasil",
        "12/10" to "Feriado Nacional: Nossa Senhora Aparecida",
        "28/10" to "Recesso Oficial UFMG: Dia do Servidor Público",
        "02/11" to "Feriado Nacional: Finados",
        "15/11" to "Feriado Nacional: Proclamação da República",
        "20/11" to "Feriado Nacional: Dia da Consciência Negra",
        "08/12" to "Feriado Municipal (BH): Imaculada Conceição",
        "24/12" to "Recesso Oficial UFMG: Véspera de Natal",
        "25/12" to "Feriado Nacional: Natal",
        "31/12" to "Recesso Oficial UFMG: Véspera de Ano Novo"
    )

    /**
     * Inicializa os horários do cache local e dispara sincronização silenciosa com a nuvem (GitHub).
     */
    fun initAndSyncDynamicSchedules(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_SCHEDULES, Context.MODE_PRIVATE)
        val cachedJson = prefs.getString(KEY_CACHED_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            applyDynamicSchedulesJson(cachedJson)
        }

        if (isSyncing) return
        isSyncing = true
        scope.launch {
            try {
                val remoteJson = fetchRemoteSchedulesJson()
                if (!remoteJson.isNullOrBlank()) {
                    if (applyDynamicSchedulesJson(remoteJson)) {
                        prefs.edit().putString(KEY_CACHED_JSON, remoteJson).apply()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isSyncing = false
            }
        }
    }

    private fun fetchRemoteSchedulesJson(): String? {
        val url = URL(REMOTE_SCHEDULES_URL)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = 7000
        conn.readTimeout = 7000
        conn.setRequestProperty("User-Agent", "MorabusUFMG-Schedules/3.1.2")
        return try {
            if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
        } finally {
            conn.disconnect()
        }
    }

    private fun applyDynamicSchedulesJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val banner = root.optString("calendarBanner", "")
            if (banner.isNotBlank()) {
                _calendarBanner.value = banner
            }

            val extraArr = root.optJSONArray("extraHolidays")
            if (extraArr != null) {
                synchronized(dynamicExtraHolidays) {
                    dynamicExtraHolidays.clear()
                    for (i in 0 until extraArr.length()) {
                        val obj = extraArr.optJSONObject(i) ?: continue
                        val date = obj.optString("date", "")
                        val name = obj.optString("name", "Recesso Acadêmico UFMG")
                        if (date.isNotBlank()) {
                            dynamicExtraHolidays[date] = name
                        }
                    }
                }
            }

            val weekArr = root.optJSONArray("weekdayTrips")
            if (weekArr != null && weekArr.length() > 0) {
                val list = mutableListOf<ScheduleTrip>()
                for (i in 0 until weekArr.length()) {
                    val item = weekArr.optJSONObject(i) ?: continue
                    val time = item.optString("time", "")
                    val origin = item.optString("origin", "Moradia")
                    val type = if (item.isNull("type")) null else item.optString("type").takeIf { it.isNotBlank() && it != "null" }
                    val vehicles = item.optInt("vehicles", 1).coerceIn(1, 5)
                    if (time.matches(Regex("^\\d{2}:\\d{2}$"))) {
                        list.add(ScheduleTrip(time, origin, type, vehicles))
                    }
                }
                if (list.isNotEmpty()) {
                    HORARIOS_SEMANA = list
                }
            }

            val satArr = root.optJSONArray("saturdayTrips")
            if (satArr != null && satArr.length() > 0) {
                val list = mutableListOf<ScheduleTrip>()
                for (i in 0 until satArr.length()) {
                    val item = satArr.optJSONObject(i) ?: continue
                    val time = item.optString("time", "")
                    val origin = item.optString("origin", "Moradia")
                    val type = if (item.isNull("type")) null else item.optString("type").takeIf { it.isNotBlank() && it != "null" }
                    val vehicles = item.optInt("vehicles", 1).coerceIn(1, 5)
                    if (time.matches(Regex("^\\d{2}:\\d{2}$"))) {
                        list.add(ScheduleTrip(time, origin, type, vehicles))
                    }
                }
                if (list.isNotEmpty()) {
                    HORARIOS_SABADO = list
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun getRoute(origin: String, type: String?): List<RoutePoint> {
        return when {
            type == "passagem" && origin.contains("Moradia") -> ROTA_PASSAGEM
            origin.contains("Moradia") -> ROTA_MORADIA
            else -> ROTA_CAMPUS
        }
    }

    fun getRouteForTripTime(tripTime: String, cal: Calendar = Calendar.getInstance()): List<RoutePoint> {
        val trips = getTripsForDate(cal).ifEmpty { HORARIOS_SEMANA }
        val matched = trips.find { it.time == tripTime }
            ?: HORARIOS_SEMANA.find { it.time == tripTime }
            ?: HORARIOS_SABADO.find { it.time == tripTime }
        return if (matched != null) {
            getRoute(matched.origin, matched.type)
        } else {
            ROTA_CAMPUS
        }
    }

    fun getEstimatedStopOffsetForTrip(stopName: String, tripTime: String, cal: Calendar = Calendar.getInstance()): Int {
        val route = getRouteForTripTime(tripTime, cal)
        route.firstOrNull { it.stopName == stopName }?.let { return it.timeMinutes }
        ROTA_MORADIA.firstOrNull { it.stopName == stopName }?.let { return it.timeMinutes }
        ROTA_CAMPUS.firstOrNull { it.stopName == stopName }?.let { return it.timeMinutes }
        ROTA_PASSAGEM.firstOrNull { it.stopName == stopName }?.let { return it.timeMinutes }
        return 5
    }

    fun getRouteDuration(origin: String, type: String?): Int {
        val route = getRoute(origin, type)
        return route.lastOrNull()?.timeMinutes ?: 20
    }

    /**
     * Simula a linha do tempo ponto a ponto de uma viagem específica,
     * aplicando ajuste em tempo real (delayMinutes) quando houver GPS ou relato "Passou aqui".
     */
    fun simulateTripStops(
        trip: ScheduleTrip,
        currentMinutes: Int? = null,
        delayMinutes: Int = 0
    ): List<SimulatedStopArrival> {
        val route = getRoute(trip.origin, trip.type)
        return route.map { pt ->
            val arrivalMin = trip.minutesFromMidnight + pt.timeMinutes + delayMinutes
            val diff = currentMinutes?.let { arrivalMin - it }
            SimulatedStopArrival(
                stopName = pt.stopName,
                offsetMinutes = pt.timeMinutes,
                arrivalTimeFormatted = minutesToHHMM(arrivalMin),
                minutesFromNow = if (diff != null && diff >= 0) diff else null,
                isPassed = diff != null && diff < 0
            )
        }
    }

    /**
     * Retorna a descrição do Feriado Nacional, Estadual (MG), Municipal (BH) ou Recesso Acadêmico UFMG,
     * ou null se for um dia normal de operação.
     */
    fun getHolidayName(cal: Calendar): String? {
        val dd = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        val mm = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val yyyy = cal.get(Calendar.YEAR)
        val ddmm = "$dd/$mm"
        val yyyymmdd = "$yyyy-$mm-$dd"

        synchronized(dynamicExtraHolidays) {
            dynamicExtraHolidays[yyyymmdd]?.let { return it }
            dynamicExtraHolidays[ddmm]?.let { return it }
        }

        FERIADOS_OFICIAIS[ddmm]?.let { return it }

        val easter = calculateEaster(yyyy)
        val calMidnight = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val diffDays = Math.round((calMidnight.timeInMillis - easter.timeInMillis) / 86400000.0).toInt()

        return when (diffDays) {
            -48 -> "Recesso Acadêmico UFMG / Segunda de Carnaval"
            -47 -> "Feriado Nacional: Terça-feira de Carnaval"
            -46 -> "Recesso Acadêmico UFMG: Quarta-feira de Cinzas"
            -3 -> "Recesso Acadêmico UFMG: Quinta-feira Santa"
            -2 -> "Feriado Nacional: Sexta-feira da Paixão"
            -1 -> "Recesso Acadêmico UFMG: Sábado Santo"
            60 -> "Feriado Municipal (BH): Corpus Christi"
            else -> null
        }
    }

    fun isHoliday(cal: Calendar): Boolean = getHolidayName(cal) != null

    /**
     * Informa o contexto do Calendário Acadêmico da UFMG para o dia atual.
     */
    fun getAcademicCalendarContext(cal: Calendar): String {
        val holidayName = getHolidayName(cal)
        if (holidayName != null) return "📅 $holidayName"

        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        return when {
            month == 1 || (month == 2 && day <= 20) -> "🏖️ Período de Férias de Verão UFMG • Confira se há grade especial"
            month == 7 && day >= 15 -> "🏖️ Recesso Acadêmico de Inverno UFMG • Confira o mural de avisos"
            else -> _calendarBanner.value
        }
    }

    private fun calculateEaster(year: Int): Calendar {
        val a = year % 19
        val b = year / 100
        val c = year % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val n = h + l - 7 * m + 114
        val month = (n / 31) - 1
        val day = (n % 31) + 1
        return Calendar.getInstance().apply {
            set(year, month, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    fun getTripsForDate(cal: Calendar): List<ScheduleTrip> {
        if (isHoliday(cal)) return emptyList()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        return when (dayOfWeek) {
            Calendar.SUNDAY -> emptyList()
            Calendar.SATURDAY -> HORARIOS_SABADO
            else -> HORARIOS_SEMANA
        }
    }

    fun getUpcomingDepartureAtStop(
        stopName: String,
        trips: List<ScheduleTrip>,
        currentMinutes: Int
    ): StopArrivalInfo? {
        for (trip in trips) {
            val route = getRoute(trip.origin, trip.type)
            val pt = route.find { it.stopName == stopName } ?: continue
            val arrivalMin = trip.minutesFromMidnight + pt.timeMinutes
            if (arrivalMin >= currentMinutes) {
                return StopArrivalInfo(
                    stopName = stopName,
                    scheduledDepartureTime = trip.time,
                    origin = trip.origin,
                    arrivalTime = minutesToHHMM(arrivalMin),
                    minutesUntilArrival = arrivalMin - currentMinutes
                )
            }
        }
        return null
    }

    fun getDayOfWeekName(cal: Calendar): String {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Domingo"
            Calendar.MONDAY -> "Segunda-feira"
            Calendar.TUESDAY -> "Terça-feira"
            Calendar.WEDNESDAY -> "Quarta-feira"
            Calendar.THURSDAY -> "Quinta-feira"
            Calendar.FRIDAY -> "Sexta-feira"
            Calendar.SATURDAY -> "Sábado"
            else -> ""
        }
    }

    fun getDayOfWeekShortName(cal: Calendar): String {
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "Dom"
            Calendar.MONDAY -> "Seg"
            Calendar.TUESDAY -> "Ter"
            Calendar.WEDNESDAY -> "Qua"
            Calendar.THURSDAY -> "Qui"
            Calendar.FRIDAY -> "Sex"
            Calendar.SATURDAY -> "Sáb"
            else -> ""
        }
    }

    fun calculateHomeStatus(
        cal: Calendar = Calendar.getInstance(),
        favoriteStopName: String? = null
    ): HomeStatus {
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val todayTrips = getTripsForDate(cal)
        val holidayName = getHolidayName(cal)
        val isHolidayOrSunday = todayTrips.isEmpty()
        val holidayOrSundayMessage = when {
            holidayName != null -> "$holidayName (Sem ônibus hoje)"
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY -> "Domingo (Sem ônibus hoje)"
            else -> null
        }

        var nextTrip: ScheduleTrip? = null
        var nextMoradiaTrip: ScheduleTrip? = null
        var nextCampusTrip: ScheduleTrip? = null
        var activeTrip: ScheduleTrip? = null

        for (trip in todayTrips) {
            val tripDuration = getRouteDuration(trip.origin, trip.type)
            val tripEndMin = trip.minutesFromMidnight + tripDuration

            if (currentMinutes in trip.minutesFromMidnight..tripEndMin) {
                activeTrip = trip
            }

            if (trip.minutesFromMidnight > currentMinutes) {
                if (nextTrip == null) nextTrip = trip
                if (nextMoradiaTrip == null && trip.origin.contains("Moradia")) {
                    nextMoradiaTrip = trip
                }
                if (nextCampusTrip == null && trip.origin.contains("Escola")) {
                    nextCampusTrip = trip
                }
            }
        }

        var firstTripNextOperatingDay: ScheduleTrip? = null
        var nextOperatingDayOffset = 1
        var nextOperatingCal: Calendar? = null
        val skippedHolidaysNotes = mutableListOf<String>()

        for (i in 1..14) {
            val futureCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val futureTrips = getTripsForDate(futureCal)
            if (futureTrips.isNotEmpty()) {
                firstTripNextOperatingDay = futureTrips.first()
                nextOperatingDayOffset = i
                nextOperatingCal = futureCal
                break
            } else {
                val futureHoliday = getHolidayName(futureCal)
                if (futureHoliday != null) {
                    val fdd = String.format("%02d", futureCal.get(Calendar.DAY_OF_MONTH))
                    val fmm = String.format("%02d", futureCal.get(Calendar.MONTH) + 1)
                    val shortDay = getDayOfWeekShortName(futureCal)
                    val cleanHoliday = futureHoliday.substringAfter(": ").trim()
                    skippedHolidaysNotes.add("$shortDay $fdd/$fmm é Feriado ($cleanHoliday)")
                }
            }
        }

        val nextServiceDayLabel = if (nextOperatingCal != null) {
            val fdd = String.format("%02d", nextOperatingCal.get(Calendar.DAY_OF_MONTH))
            val fmm = String.format("%02d", nextOperatingCal.get(Calendar.MONTH) + 1)
            if (nextOperatingDayOffset == 1) {
                "Amanhã (${getDayOfWeekShortName(nextOperatingCal)}, $fdd/$fmm)"
            } else {
                "${getDayOfWeekName(nextOperatingCal)} ($fdd/$fmm)"
            }
        } else {
            "Amanhã"
        }

        val nextServiceShortLabel = if (nextOperatingCal != null) {
            val fdd = String.format("%02d", nextOperatingCal.get(Calendar.DAY_OF_MONTH))
            val fmm = String.format("%02d", nextOperatingCal.get(Calendar.MONTH) + 1)
            if (nextOperatingDayOffset == 1) {
                "Amanhã"
            } else {
                "${getDayOfWeekShortName(nextOperatingCal)} ($fdd/$fmm)"
            }
        } else {
            "Amanhã"
        }

        val nextTimeStr = firstTripNextOperatingDay?.time ?: "06:45"
        val skippedSuffix = if (skippedHolidaysNotes.isNotEmpty()) {
            " • 📅 ${skippedHolidaysNotes.joinToString(", ")}"
        } else ""

        val lastTripToday = todayTrips.lastOrNull()
        val nextServiceFullText = if (lastTripToday != null) {
            "Última saída hoje: ${lastTripToday.time} • Próxima: $nextServiceDayLabel às $nextTimeStr$skippedSuffix"
        } else {
            "Próxima saída: $nextServiceDayLabel às $nextTimeStr$skippedSuffix"
        }
        val nextServiceShortText = "Próxima: $nextServiceShortLabel às $nextTimeStr"

        val favoriteArrival = if (!favoriteStopName.isNullOrBlank()) {
            getUpcomingDepartureAtStop(favoriteStopName, todayTrips, currentMinutes)
        } else null

        val locationText = when {
            isHolidayOrSunday -> holidayOrSundayMessage ?: "Sem viagens programadas"
            activeTrip != null -> {
                val elapsed = currentMinutes - activeTrip.minutesFromMidnight
                val route = getRoute(activeTrip.origin, activeTrip.type)
                var currentSegment = "Em viagem"
                for (i in 0 until route.size - 1) {
                    if (elapsed in route[i].timeMinutes..route[i + 1].timeMinutes) {
                        currentSegment = "Entre ${route[i].stopName} e ${route[i + 1].stopName}"
                        break
                    }
                }
                "Ônibus das ${activeTrip.time} · $currentSegment"
            }
            nextTrip != null -> {
                val waitMin = nextTrip.minutesFromMidnight - currentMinutes
                val originDesc = if (nextTrip.isPassagem) "Passando na Moradia ➔ UFMG" else "Saída ${nextTrip.origin}"
                "Próxima saída às ${nextTrip.time} ($originDesc) em ${formatDurationMin(waitMin)}"
            }
            else -> {
                "Viagens encerradas por hoje. $nextServiceShortText"
            }
        }

        val isFinishedToday = !isHolidayOrSunday && nextTrip == null && activeTrip == null

        return HomeStatus(
            isServiceActive = activeTrip != null || (nextTrip != null && !isHolidayOrSunday),
            isFinishedToday = isFinishedToday,
            isHolidayOrSunday = isHolidayOrSunday,
            holidayOrSundayMessage = holidayOrSundayMessage,
            calendarAcademicNote = getAcademicCalendarContext(cal),
            nextTrip = nextTrip,
            nextTripMinutes = nextTrip?.let { it.minutesFromMidnight - currentMinutes },
            nextMoradiaTrip = nextMoradiaTrip,
            nextMoradiaMinutes = nextMoradiaTrip?.let { it.minutesFromMidnight - currentMinutes },
            nextCampusTrip = nextCampusTrip,
            nextCampusMinutes = nextCampusTrip?.let { it.minutesFromMidnight - currentMinutes },
            activeTrip = activeTrip,
            favoriteArrival = favoriteArrival,
            estimatedLocationText = locationText,
            lastTripToday = lastTripToday,
            firstTripTomorrow = firstTripNextOperatingDay,
            nextServiceDayLabel = nextServiceDayLabel,
            nextServiceFullText = nextServiceFullText,
            nextServiceShortText = nextServiceShortText
        )
    }

    fun timeToMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size < 2) return 0
        return parts[0].toIntOrNull()?.let { h ->
            parts[1].toIntOrNull()?.let { m -> h * 60 + m }
        } ?: 0
    }

    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }

    fun findNearestStop(lat: Double, lon: Double): Pair<BusStop, Double>? {
        var closest: BusStop? = null
        var minDistance = Double.MAX_VALUE
        for (stop in STOPS) {
            if (stop.lat == null || stop.lon == null) continue
            val dist = calculateDistanceMeters(lat, lon, stop.lat, stop.lon)
            if (dist < minDistance) {
                minDistance = dist
                closest = stop
            }
        }
        return closest?.let { Pair(it, minDistance) }
    }

    /**
     * Formata minutos seguindo o padrão oficial do site:
     * - Menor que 60 min: "X min"
     * - Maior ou igual a 60 min: em horas ("1h01", "1h15", etc.)
     */
    fun formatDurationMin(min: Int): String {
        val clamped = Math.max(0, min)
        val h = clamped / 60
        val m = clamped % 60
        return if (h > 0) {
            String.format("%dh%02d", h, m)
        } else {
            "$m min"
        }
    }

    fun calculateDepartureAdvice(distanceMeters: Double, etaMinutes: Int, currentMinutes: Int): String {
        val walkingMinutes = Math.max(1, Math.ceil(distanceMeters / 75.0).toInt())
        val timeUntilNeedToLeave = etaMinutes - walkingMinutes - 1
        val idealDepartureMin = currentMinutes + Math.max(0, timeUntilNeedToLeave)
        val idealDepartureTime = minutesToHHMM(idealDepartureMin)

        return when {
            etaMinutes < walkingMinutes -> {
                "🏃 Passo apertado! Ônibus passa em ${formatDurationMin(etaMinutes)} e caminhada leva ~$walkingMinutes min."
            }
            timeUntilNeedToLeave <= 1 -> {
                "🚶 Saia agora mesmo! Caminhada de ~$walkingMinutes min dá tempo certo."
            }
            else -> {
                "🕒 Saia às $idealDepartureTime (em ${formatDurationMin(timeUntilNeedToLeave)}) · Caminhada ~$walkingMinutes min (${Math.round(distanceMeters)}m)"
            }
        }
    }

    fun minutesToHHMM(totalMinutes: Int): String {
        val normalized = ((totalMinutes % 1440) + 1440) % 1440
        val h = normalized / 60
        val m = normalized % 60
        return String.format("%02d:%02d", h, m)
    }
}
