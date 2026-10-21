package br.ufmg.morabus.app.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.notifications.LiveTripService
import br.ufmg.morabus.app.ui.components.FavoriteStopBottomSheet
import br.ufmg.morabus.app.ui.components.LiveBusCard
import br.ufmg.morabus.app.ui.theme.MorabusTheme
import br.ufmg.morabus.app.ui.theme.StatusAmber
import br.ufmg.morabus.app.ui.theme.StatusGreen
import br.ufmg.morabus.app.ui.theme.StatusRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Estados Reativos
    var favoriteStop by remember { mutableStateOf<String?>("Moradias 2 e 3") }
    var previousFavoriteStop by remember { mutableStateOf<String?>(null) }
    var isBottomSheetOpen by remember { mutableStateOf(false) }

    var userLocationIsCampus by remember { mutableStateOf(true) }
    var minutesToArrival by remember { mutableStateOf(7) }
    var isLiveTracked by remember { mutableStateOf(true) }
    var delayMinutes by remember { mutableStateOf(2) }
    var isServiceActive by remember { mutableStateOf(true) } // simulação de operação ativa vs encerrada
    var busCurrentLocation by remember { mutableStateOf("Passando na Av. Fleming") }

    // Estado do Card "Ônibus ao vivo" (Colaborativo)
    var isSharingLocation by remember { mutableStateOf(false) }
    var activeSharingContributors by remember { mutableStateOf(1) }

    // Estado do Foreground Service de Notificações ao Vivo
    val context = LocalContext.current
    var liveNotificationRunning by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CABEÇALHO: Título + Botão/Chip do Ponto Favorito
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Se houver favorito, mostra o chip com estrela; se não, mostra o botão "Escolher ponto"
                if (!favoriteStop.isNullOrBlank()) {
                    AssistChip(
                        onClick = { isBottomSheetOpen = true },
                        label = {
                            Text(
                                text = favoriteStop!!,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Ponto Favorito",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier.heightIn(min = 36.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = { isBottomSheetOpen = true },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 36.dp)
                    ) {
                        Icon(
                            Icons.Default.AddLocationAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.favorite_stop_choose_button),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. CARD "PRÓXIMO ÔNIBUS" (Contagem Única e Tratamento de Encerrado)
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.home_next_bus_title).uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Selo Previsto vs Ao Vivo (Sem repetir minutos no selo)
                        val badgeColor = if (isLiveTracked) StatusGreen else MaterialTheme.colorScheme.secondary
                        Surface(
                            color = badgeColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor)
                                )
                                Text(
                                    text = if (isLiveTracked) stringResource(R.string.home_status_live) else stringResource(R.string.home_status_predicted),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isServiceActive) {
                        // CONTAGEM REGRESSIVA EXIBIDA UMA ÚNICA VEZ
                        val etaColor = when {
                            minutesToArrival > 10 -> StatusGreen
                            minutesToArrival in 5..10 -> StatusAmber
                            else -> StatusRed
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = minutesToArrival.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = etaColor
                            )
                            Text(
                                text = "min",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = etaColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            if (minutesToArrival < 5) {
                                Surface(
                                    color = StatusRed.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Text(
                                        text = "Saia agora!",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Onde o ônibus está agora
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Navigation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = busCurrentLocation,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (delayMinutes > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.home_status_delayed, delayMinutes),
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAmber,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // ESTADO ENCERRADO (Mostra última saída e próxima de amanhã)
                        Text(
                            text = stringResource(R.string.home_service_finished_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.home_service_finished_details, "18:20", "06:45"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão para ativar Notificação ao Vivo
                    Button(
                        onClick = {
                            if (liveNotificationRunning) {
                                LiveTripService.stop(context)
                                liveNotificationRunning = false
                            } else {
                                LiveTripService.start(context, favoriteStop ?: "Moradias 2 e 3", minutesToArrival)
                                liveNotificationRunning = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (liveNotificationRunning) StatusRed else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (liveNotificationRunning) Icons.Default.Stop else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (liveNotificationRunning) "Parar Notificação ao Vivo" else "Ativar Notificação ao Vivo (Now Bar)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 2. CARD "ÔNIBUS AO VIVO" (GPS COLABORATIVO REFORMULADO)
            LiveBusCard(
                isSharing = isSharingLocation,
                sharingCount = activeSharingContributors,
                currentTripTime = "22:15",
                onStartSharing = {
                    isSharingLocation = true
                    activeSharingContributors += 1
                },
                onStopSharing = {
                    isSharingLocation = false
                    if (activeSharingContributors > 0) activeSharingContributors -= 1
                },
                onSendConditionReport = { _ ->
                    scope.launch {
                        snackbarHostState.showSnackbar("Relato registrado. Obrigado por colaborar!")
                    }
                }
            )

            // 3. ATALHO CONTEXTUAL "VOLTAR PRA CASA" (Nunca termina em beco sem saída)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (userLocationIsCampus) Icons.Default.Home else Icons.Default.School,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (userLocationIsCampus) stringResource(R.string.action_go_to_housing) else stringResource(R.string.action_go_to_campus),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(onClick = { userLocationIsCampus = !userLocationIsCampus }) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = "Inverter Direção Contextual",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(
                            R.string.context_next_from_stop,
                            if (userLocationIsCampus) "Escola de Música" else "Moradias 2 e 3",
                            "20:00",
                            minutesToArrival
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = stringResource(R.string.context_walking_time, 4),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToMap,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_notification_view_map), style = MaterialTheme.typography.labelLarge)
                        }

                        OutlinedButton(
                            onClick = onNavigateToSchedules,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                        ) {
                            Text(stringResource(R.string.nav_schedules), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // BOTTOM SHEET DO SELETOR DE PONTO FAVORITO
        if (isBottomSheetOpen) {
            FavoriteStopBottomSheet(
                currentFavorite = favoriteStop,
                onSelectStop = { selected ->
                    previousFavoriteStop = favoriteStop
                    favoriteStop = selected
                    isBottomSheetOpen = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.favorite_stop_snackbar_format, selected),
                            actionLabel = context.getString(R.string.favorite_stop_snackbar_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            favoriteStop = previousFavoriteStop
                        }
                    }
                },
                onRemoveFavorite = {
                    previousFavoriteStop = favoriteStop
                    favoriteStop = null
                    isBottomSheetOpen = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.favorite_stop_removed_snackbar),
                            actionLabel = context.getString(R.string.favorite_stop_snackbar_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            favoriteStop = previousFavoriteStop
                        }
                    }
                },
                onDismissRequest = { isBottomSheetOpen = false }
            )
        }
    }
}

// =========================================================================
// PREVIEWS DE ACESSIBILIDADE E ESCALA DE FONTE (1.0 até 2.0 em tela de 360dp)
// =========================================================================

@Preview(name = "Fonte Normal (1.0) - 360dp", widthDp = 360, fontScale = 1.0f, showBackground = true)
@Composable
fun HomeScreenPreviewFontScale10() {
    MorabusTheme(darkTheme = true) {
        HomeScreen(onNavigateToMap = {}, onNavigateToSchedules = {})
    }
}

@Preview(name = "Fonte Ampliada (1.3) - 360dp", widthDp = 360, fontScale = 1.3f, showBackground = true)
@Composable
fun HomeScreenPreviewFontScale13() {
    MorabusTheme(darkTheme = true) {
        HomeScreen(onNavigateToMap = {}, onNavigateToSchedules = {})
    }
}

@Preview(name = "Fonte Muito Grande (1.6) - 360dp", widthDp = 360, fontScale = 1.6f, showBackground = true)
@Composable
fun HomeScreenPreviewFontScale16() {
    MorabusTheme(darkTheme = true) {
        HomeScreen(onNavigateToMap = {}, onNavigateToSchedules = {})
    }
}

@Preview(name = "Acessibilidade Máxima (2.0) - 360dp", widthDp = 360, fontScale = 2.0f, showBackground = true)
@Composable
fun HomeScreenPreviewFontScale20() {
    MorabusTheme(darkTheme = true) {
        HomeScreen(onNavigateToMap = {}, onNavigateToSchedules = {})
    }
}

@PreviewFontScale
@PreviewScreenSizes
@Composable
fun HomeScreenMultiScalePreview() {
    MorabusTheme(darkTheme = true) {
        HomeScreen(onNavigateToMap = {}, onNavigateToSchedules = {})
    }
}
