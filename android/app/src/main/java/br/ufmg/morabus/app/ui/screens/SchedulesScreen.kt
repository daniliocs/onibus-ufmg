package br.ufmg.morabus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.data.CrowdLevel
import br.ufmg.morabus.app.data.ScheduleEngine
import br.ufmg.morabus.app.data.ScheduleTrip
import br.ufmg.morabus.app.data.UserPreferencesRepository
import br.ufmg.morabus.app.reminders.TripReminderManager
import br.ufmg.morabus.app.ui.theme.MorabusIcons
import br.ufmg.morabus.app.ui.theme.StatusAmber
import br.ufmg.morabus.app.ui.theme.StatusGreen
import br.ufmg.morabus.app.ui.theme.StatusRed
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        UserPreferencesRepository.init(context)
    }

    val remindedTrips by UserPreferencesRepository.scheduledReminders.collectAsState()
    val favoriteStop by UserPreferencesRepository.favoriteStop.collectAsState()
    var selectedTripForSimulation by remember { mutableStateOf<ScheduleTrip?>(null) }

    val currentCal = remember { Calendar.getInstance() }
    val currentDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
    val currentMinutes = currentCal.get(Calendar.HOUR_OF_DAY) * 60 + currentCal.get(Calendar.MINUTE)
    val holidayToday = remember(currentCal) { ScheduleEngine.getHolidayName(currentCal) }

    val isSaturdayToday = currentDayOfWeek == Calendar.SATURDAY
    val defaultTab = if (isSaturdayToday) 1 else 0

    var selectedTab by rememberSaveable { mutableStateOf(defaultTab) }
    val hidePast by UserPreferencesRepository.schedulesHidePast.collectAsState()
    val selectedFilter by UserPreferencesRepository.schedulesFilter.collectAsState() // "todos", "moradia", "campus", "passagem"

    val isViewingTodaySchedule = holidayToday == null && ((selectedTab == 1 && isSaturdayToday) || (selectedTab == 0 && currentDayOfWeek in Calendar.MONDAY..Calendar.FRIDAY))

    val rawTrips = if (selectedTab == 0) ScheduleEngine.HORARIOS_SEMANA else ScheduleEngine.HORARIOS_SABADO

    // Encontra o próximo horário se estiver visualizando o dia de hoje
    val nextTripTime = remember(rawTrips, currentMinutes, isViewingTodaySchedule) {
        if (isViewingTodaySchedule) {
            rawTrips.firstOrNull { it.minutesFromMidnight > currentMinutes }?.time
        } else null
    }

    val pastCount = remember(rawTrips, currentMinutes, isViewingTodaySchedule) {
        if (isViewingTodaySchedule) {
            rawTrips.count { it.minutesFromMidnight <= currentMinutes }
        } else 0
    }

    val filteredList = remember(rawTrips, selectedFilter, hidePast, currentMinutes, isViewingTodaySchedule) {
        rawTrips.filter { trip ->
            val matchesFilter = when (selectedFilter) {
                "moradia" -> trip.origin.contains("Moradia") && !trip.isPassagem
                "campus" -> trip.origin.contains("Escola")
                "passagem" -> trip.isPassagem
                else -> true
            }
            val isPast = isViewingTodaySchedule && trip.minutesFromMidnight <= currentMinutes
            if (hidePast && isPast) false else matchesFilter
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Abas Dias Úteis (27 saídas) / Sábados (6 saídas)
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Dias Úteis (${ScheduleEngine.HORARIOS_SEMANA.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Sábados (${ScheduleEngine.HORARIOS_SABADO.size})") }
            )
        }

        // Aviso quando estiver em modo consulta ou feriado oficial
        if (!isViewingTodaySchedule) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = when {
                            holidayToday != null -> "📅 Hoje é $holidayToday (sem circulação). Modo consulta."
                            selectedTab == 1 -> "Visualizando a grade de Sábado (modo consulta)"
                            else -> "Visualizando a grade de Dias Úteis (modo consulta)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // Filtros rápidos
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == "todos",
                    onClick = { UserPreferencesRepository.setSchedulesFilter(context, "todos") },
                    label = { Text("Todos") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "moradia",
                    onClick = { UserPreferencesRepository.setSchedulesFilter(context, "moradia") },
                    label = { Text("Moradia ➔ Campus") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "campus",
                    onClick = { UserPreferencesRepository.setSchedulesFilter(context, "campus") },
                    label = { Text("Campus ➔ Moradia") }
                )
            }
            item {
                FilterChip(
                    selected = selectedFilter == "passagem",
                    onClick = { UserPreferencesRepository.setSchedulesFilter(context, "passagem") },
                    label = { Text("Passagem (noturno)") }
                )
            }
        }

        // Barra de Controles: Ocultar anteriores
        if (isViewingTodaySchedule && pastCount > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (hidePast) "Ocultando $pastCount horários já passados" else "Mostrar horários anteriores",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = hidePast,
                    onCheckedChange = { UserPreferencesRepository.setSchedulesHidePast(context, it) }
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Nenhum horário correspondente ao filtro selecionado.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(filteredList, key = { "${it.time}_${it.origin}_${it.type}" }) { trip ->
                    val isPast = isViewingTodaySchedule && trip.minutesFromMidnight <= currentMinutes
                    val isNext = trip.time == nextTripTime

                    ElevatedCard(
                        onClick = { selectedTripForSimulation = trip },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = when {
                                isNext -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = trip.time,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    )

                                    if (isNext) {
                                        Surface(
                                            color = StatusGreen.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "Próximo",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = StatusGreen,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    // Indicador de Comboio
                                    val busEmojis = "🚌".repeat(trip.vehicles)
                                    Text(
                                        text = "$busEmojis ${trip.vehicles} veículo${if (trip.vehicles > 1) "s" else ""}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val originText = when {
                                    trip.isPassagem -> "Passando na Moradia ➔ Ponto final"
                                    trip.origin.contains("Moradia") -> "Saída Moradia ➔ Escola de Música"
                                    else -> "Saída Escola de Música ➔ Moradias"
                                }

                                Text(
                                    text = originText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Lotação Habitual + Dica de toque para simular paradas
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val crowdColor = when (trip.crowdLevel) {
                                        CrowdLevel.ALTA -> StatusRed
                                        CrowdLevel.MEDIA -> StatusAmber
                                        CrowdLevel.BAIXA -> StatusGreen
                                    }
                                    Text(
                                        text = "${trip.crowdLevel.emoji} ${trip.crowdLevel.label}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = crowdColor
                                    )
                                    Text(
                                        text = "• Toque p/ ver paradas",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // Botão de Lembrete / Alarme de Viagem (15 min antes)
                            val tripKey = "${trip.time}_${trip.origin}"
                            val isReminded = remindedTrips.contains(tripKey)

                            IconButton(
                                onClick = {
                                    val added = UserPreferencesRepository.toggleReminder(context, tripKey)
                                    if (added) {
                                        TripReminderManager.scheduleReminder(context, trip.time, trip.origin)
                                    } else {
                                        TripReminderManager.cancelReminder(context, trip.time)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isReminded) MorabusIcons.NotificationsActive else MorabusIcons.NotificationsNone,
                                    contentDescription = if (isReminded) "Lembrete ativo" else "Definir lembrete",
                                    tint = if (isReminded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // MODAL DE SIMULAÇÃO PONTO A PONTO DA VIAGEM SELECIONADA
        if (selectedTripForSimulation != null) {
            val simTrip = selectedTripForSimulation!!
            val simulatedStops = remember(simTrip, currentMinutes, isViewingTodaySchedule) {
                ScheduleEngine.simulateTripStops(simTrip, if (isViewingTodaySchedule) currentMinutes else null)
            }
            ModalBottomSheet(
                onDismissRequest = { selectedTripForSimulation = null },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp)
                ) {
                    Text(
                        text = "Viagem das ${simTrip.time}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Saída: ${simTrip.origin} ➔ ${simTrip.destination} • ${simTrip.vehicles} veículo${if (simTrip.vehicles > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${simTrip.crowdLevel.emoji} Lotação habitual: ${simTrip.crowdLevel.label}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(simulatedStops) { stopInfo ->
                            val isFav = stopInfo.stopName == favoriteStop
                            Surface(
                                color = if (isFav) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isFav) MaterialTheme.colorScheme.primary
                                                    else if (stopInfo.isPassed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                                    else StatusGreen
                                                )
                                        )
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = stopInfo.stopName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isFav) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (stopInfo.isPassed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isFav) {
                                                    Icon(
                                                        Icons.Default.Star,
                                                        contentDescription = "Seu Ponto Favorito",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (stopInfo.offsetMinutes == 0) "Partida inicial" else "+${stopInfo.offsetMinutes} min após a saída",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = stopInfo.arrivalTimeFormatted,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isFav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        if (stopInfo.minutesFromNow != null) {
                                            Text(
                                                text = "em ${ScheduleEngine.formatDurationMin(stopInfo.minutesFromNow)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = StatusGreen
                                            )
                                        } else if (stopInfo.isPassed) {
                                            Text(
                                                text = "já passou",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
