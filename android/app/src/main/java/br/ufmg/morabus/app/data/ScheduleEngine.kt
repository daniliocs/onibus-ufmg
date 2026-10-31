package br.ufmg.morabus.app.data

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

data class ScheduleTrip(
    val time: String,          // "06:45"
    val origin: String,        // "Moradia", "Escola de Música", "Moradias"
    val type: String?,         // "passagem" or null
    val vehicles: Int,         // 1, 2, 3
    val minutesFromMidnight: Int = ScheduleEngine.timeToMinutes(time)
) {
    val isPassagem: Boolean get() = type == "passagem" && origin.contains("Moradia")
    val destination: String get() = if (origin.contains("Moradia")) {
        if (isPassagem) "Ponto final" else "Escola de Música"
    } else {
        "Moradias 2 e 3"
    }
}

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
    val firstTripTomorrow: ScheduleTrip?
)

object ScheduleEngine {

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

    val HORARIOS_SEMANA = listOf(
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

    val HORARIOS_SABADO = listOf(
        ScheduleTrip("07:40", "Moradia", null, 1),
        ScheduleTrip("10:00", "Escola de Música", null, 2),
        ScheduleTrip("11:00", "Moradia", null, 2),
        ScheduleTrip("12:00", "Escola de Música", null, 2),
        ScheduleTrip("12:20", "Moradia", null, 2),
        ScheduleTrip("13:30", "Escola de Música", null, 2)
    )

    private val FERIADOS_FIXOS = setOf(
        "01/01", "21/04", "01/05", "15/08", "07/09",
        "12/10", "02/11", "15/11", "20/11", "08/12", "25/12"
    )

    fun getRoute(origin: String, type: String?): List<RoutePoint> {
        return when {
            type == "passagem" && origin.contains("Moradia") -> ROTA_PASSAGEM
            origin.contains("Moradia") -> ROTA_MORADIA
            else -> ROTA_CAMPUS
        }
    }

    fun getRouteDuration(origin: String, type: String?): Int {
        val route = getRoute(origin, type)
        return route.lastOrNull()?.timeMinutes ?: 20
    }

    fun isHoliday(cal: Calendar): Boolean {
        val dd = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        val mm = String.format("%02d", cal.get(Calendar.MONTH) + 1)
        val ddmm = "$dd/$mm"
        if (FERIADOS_FIXOS.contains(ddmm)) return true

        val year = cal.get(Calendar.YEAR)
        val easter = calculateEaster(year)
        val diffDays = ((cal.timeInMillis - easter.timeInMillis) / 86400000L).toInt()
        // Carnaval (-48, -47), Sexta-feira Santa (-2), Corpus Christi (60)
        return diffDays in setOf(-48, -47, -2, 60)
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

    fun calculateHomeStatus(
        cal: Calendar = Calendar.getInstance(),
        favoriteStopName: String? = null
    ): HomeStatus {
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val todayTrips = getTripsForDate(cal)
        val isHolidayOrSunday = todayTrips.isEmpty()
        val holidayOrSundayMessage = if (isHoliday(cal)) {
            "Feriado (Sem ônibus hoje)"
        } else if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            "Domingo (Sem ônibus hoje)"
        } else null

        var nextTrip: ScheduleTrip? = null
        var nextMoradiaTrip: ScheduleTrip? = null
        var nextCampusTrip: ScheduleTrip? = null
        var activeTrip: ScheduleTrip? = null
        var lastPassedTrip: ScheduleTrip? = null

        for (trip in todayTrips) {
            val tripDuration = getRouteDuration(trip.origin, trip.type)
            val tripEndMin = trip.minutesFromMidnight + tripDuration

            if (currentMinutes in trip.minutesFromMidnight..tripEndMin) {
                activeTrip = trip
            }

            if (trip.minutesFromMidnight > currentMinutes) {
                if (nextTrip == null) nextTrip = trip
                if (nextMoradiaTrip == null && trip.origin.contains("Moradia") && !trip.isPassagem) {
                    nextMoradiaTrip = trip
                }
                if (nextCampusTrip == null && trip.origin.contains("Escola")) {
                    nextCampusTrip = trip
                }
            } else {
                lastPassedTrip = trip
            }
        }

        // Se acabaram as viagens de hoje, busca a primeira de amanhã/próximo dia útil
        val tomorrowCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
        var firstTripTomorrow: ScheduleTrip? = null
        for (i in 1..7) {
            val futureCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, i) }
            val futureTrips = getTripsForDate(futureCal)
            if (futureTrips.isNotEmpty()) {
                firstTripTomorrow = futureTrips.first()
                break
            }
        }

        val favoriteArrival = if (!favoriteStopName.isNullOrBlank()) {
            getUpcomingDepartureAtStop(favoriteStopName, todayTrips, currentMinutes)
        } else null

        // Determina a descrição estimada de localização
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
                val originDesc = if (nextTrip.isPassagem) "Passando na Moradia" else "Saída ${nextTrip.origin}"
                "Próxima saída às ${nextTrip.time} ($originDesc) em ${formatDurationMin(waitMin)}"
            }
            else -> {
                val nextDesc = firstTripTomorrow?.let { "Próxima saída: ${it.time} (${it.origin})" } ?: ""
                "Viagens encerradas por hoje. $nextDesc"
            }
        }

        val isFinishedToday = !isHolidayOrSunday && nextTrip == null && activeTrip == null

        return HomeStatus(
            isServiceActive = activeTrip != null || (nextTrip != null && !isHolidayOrSunday),
            isFinishedToday = isFinishedToday,
            isHolidayOrSunday = isHolidayOrSunday,
            holidayOrSundayMessage = holidayOrSundayMessage,
            nextTrip = nextTrip,
            nextTripMinutes = nextTrip?.let { it.minutesFromMidnight - currentMinutes },
            nextMoradiaTrip = nextMoradiaTrip,
            nextMoradiaMinutes = nextMoradiaTrip?.let { it.minutesFromMidnight - currentMinutes },
            nextCampusTrip = nextCampusTrip,
            nextCampusMinutes = nextCampusTrip?.let { it.minutesFromMidnight - currentMinutes },
            activeTrip = activeTrip,
            favoriteArrival = favoriteArrival,
            estimatedLocationText = locationText,
            lastTripToday = todayTrips.lastOrNull(),
            firstTripTomorrow = firstTripTomorrow
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
                "🏃 Passo apertado! Ônibus chega em ${formatDurationMin(etaMinutes)} e caminhada leva ~$walkingMinutes min."
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
