package br.ufmg.morabus.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.R

data class ScheduleEntry(
    val time: String,
    val origin: String,
    val destination: String,
    val vehicles: Int,
    val isPast: Boolean = false
)

@Composable
fun SchedulesScreen(modifier: Modifier = Modifier) {
    var selectedTab by remember { mutableStateOf(0) }
    var hidePast by remember { mutableStateOf(true) }

    val sampleSchedules = remember {
        listOf(
            ScheduleEntry("06:45", "Moradias 2 e 3", "Escola de Música", 1, true),
            ScheduleEntry("07:10", "Moradias 2 e 3", "Escola de Música", 2, true),
            ScheduleEntry("07:35", "Moradias 2 e 3", "Escola de Música", 2, false),
            ScheduleEntry("11:30", "Escola de Música", "Moradias 2 e 3", 1, false),
            ScheduleEntry("12:00", "Moradias 2 e 3", "Escola de Música", 1, false),
            ScheduleEntry("17:20", "Escola de Música", "Moradias 2 e 3", 2, false),
            ScheduleEntry("17:50", "Moradias 2 e 3", "Escola de Música", 2, false),
            ScheduleEntry("20:00", "Escola de Música", "Moradias 2 e 3", 1, false),
            ScheduleEntry("22:15", "Escola de Música", "Moradias 2 e 3", 1, false)
        )
    }

    val filteredList = remember(hidePast, selectedTab) {
        if (hidePast) sampleSchedules.filter { !it.isPast } else sampleSchedules
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Abas Dias Úteis / Sábados
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text(stringResource(R.string.schedule_tab_weekdays)) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text(stringResource(R.string.schedule_tab_saturday)) }
            )
        }

        // Barra de Controles: Ocultar anteriores + Legenda
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.schedule_hide_past_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Switch(
                checked = hidePast,
                onCheckedChange = { hidePast = it }
            )
        }

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(filteredList) { entry ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (entry.isPast)
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.time,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "${entry.vehicles} veículos",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${entry.origin} ➔ ${entry.destination}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { /* Configurar alarme */ }) {
                            Icon(
                                Icons.Default.Alarm,
                                contentDescription = stringResource(R.string.schedule_set_alarm_action),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}
