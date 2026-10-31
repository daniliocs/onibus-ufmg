package br.ufmg.morabus.app.data

import java.util.Calendar

data class RuMealTime(
    val name: String,         // "Café da Manhã", "Almoço", "Jantar"
    val startMinutes: Int,
    val endMinutes: Int,
    val formattedHours: String,
    val notes: String? = null
)

data class RuUnit(
    val id: String,
    val name: String,
    val campus: String,
    val weekdayMeals: List<RuMealTime>,
    val saturdayMeals: List<RuMealTime>,
    val sundayOpen: Boolean = false,
    val nearestMoraBusStop: String? = null
)

data class RuStatus(
    val unit: RuUnit,
    val isOpenNow: Boolean,
    val currentOrNextMealText: String,
    val closingCountdownMinutes: Int? = null,
    val badgeColorType: String // "open", "closing_soon", "closed"
)

data class RuMobilityRecommendation(
    val title: String,
    val subtitle: String,
    val targetMeal: String,
    val recommendedBusTime: String?,
    val isClosingSoon: Boolean
)

object RuEngine {

    val ALL_RUS = listOf(
        RuUnit(
            id = "setorial_1",
            name = "RU Setorial I",
            campus = "Campus Pampulha",
            nearestMoraBusStop = "RU 1",
            weekdayMeals = listOf(
                RuMealTime("Almoço", 10 * 60 + 30, 14 * 60, "10h30 às 14h"),
                RuMealTime("Jantar", 17 * 60 + 10, 19 * 60 + 30, "17h10 às 19h30")
            ),
            saturdayMeals = listOf(
                RuMealTime("Almoço", 11 * 60, 13 * 60, "11h às 13h", "Sem café nem jantar aos sábados")
            )
        ),
        RuUnit(
            id = "setorial_2",
            name = "RU Setorial II",
            campus = "Campus Pampulha",
            nearestMoraBusStop = "RU 2",
            weekdayMeals = listOf(
                RuMealTime("Café da Manhã", 6 * 60 + 45, 8 * 60, "06h45 às 08h", "Gratuito para níveis I, II e III da FUMP"),
                RuMealTime("Almoço", 10 * 60 + 30, 14 * 60, "10h30 às 14h")
            ),
            saturdayMeals = emptyList() // Não abre aos sábados
        ),
        RuUnit(
            id = "saude",
            name = "RU Campus Saúde",
            campus = "Área Hospitalar",
            weekdayMeals = listOf(
                RuMealTime("Café da Manhã", 7 * 60, 8 * 60, "07h às 08h"),
                RuMealTime("Almoço", 10 * 60 + 30, 14 * 60, "10h30 às 14h"),
                RuMealTime("Jantar", 17 * 60 + 15, 19 * 60 + 30, "17h15 às 19h30")
            ),
            saturdayMeals = listOf(
                RuMealTime("Almoço", 11 * 60 + 30, 13 * 60, "11h30 às 13h")
            )
        ),
        RuUnit(
            id = "direito",
            name = "RU Faculdade de Direito",
            campus = "Centro / Praça Afonso Pena",
            weekdayMeals = listOf(
                RuMealTime("Café da Manhã", 7 * 60, 8 * 60, "07h às 08h"),
                RuMealTime("Almoço", 11 * 60, 14 * 60, "11h às 14h"),
                RuMealTime("Jantar", 17 * 60 + 30, 19 * 60 + 30, "17h30 às 19h30")
            ),
            saturdayMeals = emptyList() // Não abre aos sábados
        ),
        RuUnit(
            id = "ica",
            name = "RU ICA",
            campus = "Montes Claros",
            weekdayMeals = listOf(
                RuMealTime("Café da Manhã", 6 * 60 + 45, 8 * 60, "06h45 às 08h"),
                RuMealTime("Almoço", 11 * 60, 13 * 60 + 30, "11h às 13h30"),
                RuMealTime("Jantar", 17 * 60 + 30, 19 * 60 + 30, "17h30 às 19h30")
            ),
            saturdayMeals = listOf(
                RuMealTime("Almoço", 11 * 60 + 15, 13 * 60, "11h15 às 13h")
            )
        )
    )

    fun calculateUnitStatus(unit: RuUnit, cal: Calendar): RuStatus {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        if (dayOfWeek == Calendar.SUNDAY) {
            return RuStatus(unit, false, "Fechado aos Domingos", null, "closed")
        }

        val meals = if (dayOfWeek == Calendar.SATURDAY) unit.saturdayMeals else unit.weekdayMeals
        if (meals.isEmpty()) {
            return RuStatus(unit, false, "Fechado aos Sábados", null, "closed")
        }

        // Verifica se alguma refeição está aberta agora
        for (meal in meals) {
            if (currentMinutes in meal.startMinutes until meal.endMinutes) {
                val remaining = meal.endMinutes - currentMinutes
                val colorType = if (remaining <= 30) "closing_soon" else "open"
                val text = "${meal.name} aberto até às ${formatMinutes(meal.endMinutes)} (${remaining} min restantes)"
                return RuStatus(unit, true, text, remaining, colorType)
            }
        }

        // Verifica a próxima refeição do dia
        val nextMeal = meals.firstOrNull { it.startMinutes > currentMinutes }
        if (nextMeal != null) {
            val waitMin = nextMeal.startMinutes - currentMinutes
            val text = "Próximo: ${nextMeal.name} às ${formatMinutes(nextMeal.startMinutes)} (abre em ${waitMin} min)"
            return RuStatus(unit, false, text, null, "closed")
        }

        return RuStatus(unit, false, "Encerrado hoje", null, "closed")
    }

    /**
     * Recomenda o ônibus correto saindo da Moradia para que o estudante alcance
     * as refeições do RU Setorial I ou II antes do fechamento, considerando
     * o tempo médio de deslocamento de 25 min (35 min com trânsito/chuva).
     */
    fun calculateMobilityRecommendation(cal: Calendar): RuMobilityRecommendation? {
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY) return null

        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        // 1. Manhã: Café no Setorial II (06:45 às 08:00) em dias úteis
        if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY && currentMinutes in 360..470) {
            val isUrgent = currentMinutes >= 405 // após 06:45
            val subtitle = if (currentMinutes < 405) {
                "Embarque às 06:45 na Moradia (chegada ~07:10). Gratuito para níveis I, II e III."
            } else {
                "Portões do café fecham às 08:00. Ônibus das 06:45 já partiu."
            }
            return RuMobilityRecommendation(
                title = "☕ Café da Manhã no RU Setorial II (06:45 às 08:00)",
                subtitle = subtitle,
                targetMeal = "Café da Manhã",
                recommendedBusTime = if (currentMinutes < 405) "06:45" else null,
                isClosingSoon = isUrgent
            )
        }

        // 2. Almoço: Setorial I e II (Fecha 14:00 seg-sex / 13:00 sáb)
        val lunchClose = if (dayOfWeek == Calendar.SATURDAY) 13 * 60 else 14 * 60
        if (currentMinutes in 570 until lunchClose) {
            val remainingToClose = lunchClose - currentMinutes
            val isUrgent = remainingToClose <= 40

            val (recBus, subtitle) = if (dayOfWeek == Calendar.SATURDAY) {
                when {
                    currentMinutes < 660 -> Pair("11:00", "Saída recomendada: 11:00 (chega ~11:25). Última do sábado: 12:20.")
                    currentMinutes <= 740 -> Pair("12:20", "ÚLTIMA saída da Moradia no sábado: 12:20 (chega ~12:45). RU fecha às 13:00!")
                    else -> Pair(null, "Almoço aos sábados encerra às 13:00. Não há mais viagens a tempo.")
                }
            } else {
                when {
                    currentMinutes < 660 -> Pair("11:00", "Saídas às 11:00 e 12:15 garantem almoço com calma.")
                    currentMinutes < 735 -> Pair("12:15", "Última saída com folga: 12:15 (chegada ~12:40). Ônibus das 13:30 chega no limite.")
                    currentMinutes <= 810 -> Pair("13:30", "ÚLTIMA saída da Moradia: 13:30 (chega ~13:55). Portões do RU encerram às 14:00!")
                    else -> Pair(null, "Portões do almoço encerram às 14:00. Não há mais viagens a tempo.")
                }
            }

            val ruTitleName = if (dayOfWeek == Calendar.SATURDAY) "RU Setorial I" else "RU Setorial I e II"
            return RuMobilityRecommendation(
                title = "🍽️ Almoço no $ruTitleName (até às ${formatMinutes(lunchClose)})",
                subtitle = subtitle,
                targetMeal = "Almoço",
                recommendedBusTime = recBus,
                isClosingSoon = isUrgent
            )
        }

        // 3. Jantar: Setorial I (17:10 às 19:30) em dias úteis
        if (dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY && currentMinutes in 960 until (19 * 60 + 30)) {
            val dinnerClose = 19 * 60 + 30
            val remaining = dinnerClose - currentMinutes
            val isUrgent = remaining <= 45

            val (recBus, subtitle) = when {
                currentMinutes < 1040 -> Pair("17:20", "Saída das 17:20 chega ~17:45. Última para jantar: 18:20 (chega ~18:45).")
                currentMinutes <= 1100 -> Pair("18:20", "ÚLTIMA saída viável da Moradia: 18:20 (chegada ~18:45). Portões do RU fecham às 19:30!")
                else -> Pair(null, "A saída das 18:20 já partiu. Próxima viagem (19:20) chegará após o encerramento do RU (19:30).")
            }

            return RuMobilityRecommendation(
                title = "🍲 Jantar no RU Setorial I (até às 19:30)",
                subtitle = subtitle,
                targetMeal = "Jantar",
                recommendedBusTime = recBus,
                isClosingSoon = isUrgent
            )
        }

        return null
    }

    private fun formatMinutes(min: Int): String {
        val h = min / 60
        val m = min % 60
        return String.format("%02dh%02d", h, m)
    }
}
