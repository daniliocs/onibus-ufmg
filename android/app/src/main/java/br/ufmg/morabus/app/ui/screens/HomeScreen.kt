package br.ufmg.morabus.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.data.BusStop
import br.ufmg.morabus.app.data.FirebaseRealtimeRepository
import br.ufmg.morabus.app.data.ScheduleEngine
import br.ufmg.morabus.app.data.UserPreferencesRepository
import br.ufmg.morabus.app.notifications.LiveTripService
import br.ufmg.morabus.app.ui.components.FavoriteStopBottomSheet
import br.ufmg.morabus.app.ui.components.LiveBusCard
import br.ufmg.morabus.app.ui.theme.MorabusIcons
import br.ufmg.morabus.app.ui.theme.MorabusTheme
import br.ufmg.morabus.app.ui.theme.StatusAmber
import br.ufmg.morabus.app.ui.theme.StatusGreen
import br.ufmg.morabus.app.ui.theme.StatusRed
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMap: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToNotices: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        UserPreferencesRepository.init(context)
        FirebaseRealtimeRepository.startPolling()
        LiveTripService.checkAndUpdateRunningState(context)
        br.ufmg.morabus.app.data.CampusRadarEngine.triggerImmediateCheck(context)
        br.ufmg.morabus.app.data.CampusRadarEngine.startPeriodicRadar(context)
    }

    // Ponto Favorito persistente e Rotinas de Lembretes
    val favoriteStop by UserPreferencesRepository.favoriteStop.collectAsState()
    val remindedTrips by UserPreferencesRepository.scheduledReminders.collectAsState()
    val calendarBanner by ScheduleEngine.calendarBanner.collectAsState()
    var previousFavoriteStop by remember { mutableStateOf<String?>(null) }
    var isBottomSheetOpen by remember { mutableStateOf(false) }

    // Radar Automático de Comunicados (FUMP, UFMG, Defesa Civil / Clima) - Máx 2 cards na tela inicial
    val homeNotices by br.ufmg.morabus.app.data.CampusRadarEngine.homeNotices.collectAsState()
    var dismissedCommunityAlert by remember { mutableStateOf<String?>(null) }

    // Atualizações Remotas (OTA)
    val availableUpdate by br.ufmg.morabus.app.data.AppUpdateManager.availableUpdate.collectAsState()
    val isDownloadingUpdate by br.ufmg.morabus.app.data.AppUpdateManager.isDownloading.collectAsState()
    val updateProgress by br.ufmg.morabus.app.data.AppUpdateManager.downloadProgress.collectAsState()
    val downloadedFile by br.ufmg.morabus.app.data.AppUpdateManager.downloadedApkFile.collectAsState()
    val downloadError by br.ufmg.morabus.app.data.AppUpdateManager.downloadError.collectAsState()

    // GPS local do usuário para descoberta inteligente de parada mais próxima
    var userGpsLocation by remember { mutableStateOf<Location?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Sentido contextual com auto-descoberta via GPS
    var manualCampusToggle by remember { mutableStateOf<Boolean?>(null) }
    val userLocationIsCampus = remember(userGpsLocation, manualCampusToggle) {
        manualCampusToggle ?: run {
            if (userGpsLocation != null) {
                val distMoradia = ScheduleEngine.calculateDistanceMeters(userGpsLocation!!.latitude, userGpsLocation!!.longitude, -19.8661, -43.9845)
                val distCampus = ScheduleEngine.calculateDistanceMeters(userGpsLocation!!.latitude, userGpsLocation!!.longitude, -19.8685, -43.9650)
                distCampus < distMoradia
            } else false
        }
    }

    // Compartilhamento colaborativo via Foreground Service
    val isSharingLocation by br.ufmg.morabus.app.sharing.BusSharingService.isSharing.collectAsState()

    // Sincronização em tempo real do Firebase RTDB
    val telemetry by FirebaseRealtimeRepository.telemetry.collectAsState()
    val recentPassage by FirebaseRealtimeRepository.recentPassage.collectAsState()
    val waitingCounts by FirebaseRealtimeRepository.waitingCounts.collectAsState()
    val userWaitingStop by FirebaseRealtimeRepository.userWaitingStop.collectAsState()

    // Controle do Modal de todos os RUs da UFMG
    var isRuModalOpen by remember { mutableStateOf(false) }

    // Sincronização do estado do serviço de notificação ao vivo
    val isLiveNotificationRunning by LiveTripService.isRunning.collectAsState()

    val hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasFineLocation, hasCoarseLocation) {
        if (hasFineLocation || hasCoarseLocation) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) userGpsLocation = loc
            }
        }
    }

    // Dispara checagem imediata do Radar de Comunicados ao abrir a tela inicial
    LaunchedEffect(Unit) {
        br.ufmg.morabus.app.data.CampusRadarEngine.triggerImmediateCheck(context)
    }

    // Atualizador periódico do relógio (recalcula a cada 10 segundos)
    var timeTrigger by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000L)
            timeTrigger = System.currentTimeMillis()
            if (hasFineLocation || hasCoarseLocation) {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    if (loc != null) userGpsLocation = loc
                }
            }
        }
    }

    val currentCal = remember(timeTrigger) { Calendar.getInstance() }
    val currentMinutes = currentCal.get(Calendar.HOUR_OF_DAY) * 60 + currentCal.get(Calendar.MINUTE)
    val dayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK)
    val isWeekday = dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY

    val ruRecommendation = remember(timeTrigger) {
        br.ufmg.morabus.app.data.RuEngine.calculateMobilityRecommendation(Calendar.getInstance())
    }
    val ruPampulhaStatus = remember(timeTrigger) {
        br.ufmg.morabus.app.data.RuEngine.calculateUnitStatus(br.ufmg.morabus.app.data.RuEngine.ALL_RUS[0], Calendar.getInstance())
    }

    // Parada mais próxima detectada via GPS
    val nearestGpsResult: Pair<BusStop, Double>? = remember(userGpsLocation) {
        userGpsLocation?.let { ScheduleEngine.findNearestStop(it.latitude, it.longitude) }
    }

    // Parada ativa para cálculo de previsão: se o usuário tiver favorito, usa o favorito; se não, usa a mais próxima via GPS
    val effectiveTargetStop: String? = remember(favoriteStop, nearestGpsResult) {
        favoriteStop ?: nearestGpsResult?.first?.name
    }

    val scheduleStatus = remember(timeTrigger, effectiveTargetStop) {
        ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), effectiveTargetStop)
    }

    // Atraso ou adiantamento em tempo real (via GPS contínuo ou via botão "Passou aqui")
    val effectiveDelayMinutes = remember(telemetry, recentPassage) {
        when {
            telemetry.isActive -> telemetry.delayMinutes
            recentPassage != null -> recentPassage!!.calculatedDelayMinutes
            else -> 0
        }
    }

    // Previsão de minutos para a parada alvo ou saída geral (ajustada em tempo real)
    val minutesToArrival = remember(scheduleStatus, effectiveDelayMinutes) {
        if (scheduleStatus.favoriteArrival != null) {
            Math.max(0, scheduleStatus.favoriteArrival.minutesUntilArrival + effectiveDelayMinutes)
        } else {
            Math.max(0, (scheduleStatus.nextTripMinutes ?: 0) + effectiveDelayMinutes)
        }
    }

    // Conselho de caminhada ("Quando devo sair?") calculado inteligentemente para a parada mais próxima via GPS
    val departureAdviceText = remember(nearestGpsResult, minutesToArrival, currentMinutes) {
        if (nearestGpsResult != null) {
            ScheduleEngine.calculateDepartureAdvice(nearestGpsResult.second, Math.max(0, minutesToArrival), currentMinutes)
        } else null
    }

    val isLiveTracked = telemetry.isActive || (recentPassage != null && !recentPassage!!.estimatedCurrentSegment.isNullOrBlank())
    val activeSharingContributors = telemetry.contributorsCount
    val busCurrentLocation = remember(telemetry, recentPassage, scheduleStatus) {
        if (telemetry.isActive && !telemetry.streetName.isNullOrBlank()) {
            "Na ${telemetry.streetName}"
        } else if (recentPassage != null && !recentPassage!!.estimatedCurrentSegment.isNullOrBlank()) {
            recentPassage!!.estimatedCurrentSegment!!
        } else {
            scheduleStatus.estimatedLocationText
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val grantedLoc = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (grantedLoc) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) userGpsLocation = loc
                val target = favoriteStop ?: loc?.let { ScheduleEngine.findNearestStop(it.latitude, it.longitude)?.first?.name }
                LiveTripService.start(
                    context = context,
                    targetStop = target,
                    etaMinutes = minutesToArrival,
                    departureAdvice = departureAdviceText,
                    isNearestGps = favoriteStop == null
                )
            }
        } else {
            LiveTripService.start(
                context = context,
                targetStop = favoriteStop,
                etaMinutes = minutesToArrival,
                departureAdvice = departureAdviceText,
                isNearestGps = favoriteStop == null
            )
        }
    }

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
                            Icons.Default.Place,
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

            // BANNER DE ATUALIZAÇÃO REMOTA (OTA)
            if (availableUpdate != null) {
                val update = availableUpdate!!
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
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
                                Text("🚀", style = MaterialTheme.typography.titleMedium)
                                Column {
                                    Text(
                                        text = "Nova Versão ${update.versionName} Disponível",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = update.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            if (!update.isForceUpdate) {
                                IconButton(
                                    onClick = { br.ufmg.morabus.app.data.AppUpdateManager.dismissUpdate() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Dispensar",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isDownloadingUpdate) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                LinearProgressIndicator(
                                    progress = { updateProgress / 100f },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Baixando atualização: $updateProgress%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        } else if (downloadedFile != null && downloadedFile!!.exists()) {
                            Button(
                                onClick = { br.ufmg.morabus.app.data.AppUpdateManager.promptInstall(context, downloadedFile) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Instalar Versão ${update.versionName}")
                            }
                        } else {
                            Button(
                                onClick = { br.ufmg.morabus.app.data.AppUpdateManager.startDownloadAndInstall(context, update) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Baixar e Atualizar Agora")
                            }
                        }

                        if (!downloadError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = downloadError!!,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // RADAR AUTOMÁTICO DE COMUNICADOS (Máx 2 cards na tela inicial: compacto para urgentes, padrão para normais)
            br.ufmg.morabus.app.ui.components.CampusRadarBannerList(
                notices = homeNotices,
                onDismissNotice = { id -> br.ufmg.morabus.app.data.CampusRadarEngine.dismissHomeNotice(context, id) },
                onOpenUrl = { url ->
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                },
                onNavigateToNoticesTab = onNavigateToNotices
            )

            // BANNER DE ALERTA DA COMUNIDADE (se houver aviso recente do Firebase)
            if (!telemetry.alertMessage.isNullOrBlank() && dismissedCommunityAlert != telemetry.alertMessage) {
                Surface(
                    color = StatusAmber.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StatusAmber.copy(alpha = 0.5f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                MorabusIcons.Warning,
                                contentDescription = null,
                                tint = StatusAmber,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = telemetry.alertMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { dismissedCommunityAlert = telemetry.alertMessage },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fechar aviso",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ALERTA NOTURNO DE SEGURANÇA / ÚLTIMO ÔNIBUS (Dias úteis normais à noite)
            if (isWeekday && !scheduleStatus.isHolidayOrSunday && currentMinutes >= 21 * 60) {
                val isCritical = currentMinutes in (22 * 60 + 35)..(23 * 60 + 15)
                val alertBg = if (isCritical) StatusRed.copy(alpha = 0.18f) else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                val alertBorder = if (isCritical) StatusRed else MaterialTheme.colorScheme.tertiary
                Surface(
                    color = alertBg,
                    shape = RoundedCornerShape(14.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(alertBorder.copy(alpha = 0.6f))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(if (isCritical) "🚨" else "🌙", style = MaterialTheme.typography.titleLarge)
                        Column {
                            Text(
                                text = if (isCritical) "⚠️ ÚLTIMAS VIAGENS DA NOITE (22:50 E 23:10)!" else "🌙 Alerta Noturno de Fim de Expediente",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isCritical) StatusRed else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (currentMinutes <= 22 * 60 + 50) {
                                    "Última saída do Campus (Escola de Música) p/ Moradias às 22:50. Às 23:10 o ônibus passa nas Moradias permitindo voltar à UFMG (fica no Ponto final até o próximo dia)."
                                } else if (currentMinutes <= 23 * 60 + 15) {
                                    "Último ônibus às 23:10 passando nas Moradias c/ retorno à UFMG (encerra no Ponto final do campus até o próximo dia de funcionamento)."
                                } else {
                                    "Saídas noturnas de hoje concluídas. ${scheduleStatus.nextServiceShortText}."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 1. CARD "PRÓXIMO ÔNIBUS" (Com Ponto Favorito ou Ponto mais próximo inteligente)
            // Contexto Acadêmico UFMG & Rotina Inteligente do Usuário
            val activeRoutineCount = remindedTrips.size
            val calendarNote = scheduleStatus.calendarAcademicNote ?: calendarBanner
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (activeRoutineCount > 0) {
                            "🎓 $calendarNote · 🔔 $activeRoutineCount na sua rotina"
                        } else {
                            "🎓 $calendarNote"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

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
                        Column {
                            Text(
                                text = stringResource(R.string.home_next_bus_title).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!effectiveTargetStop.isNullOrBlank()) {
                                Text(
                                    text = if (favoriteStop != null) "⭐ $favoriteStop" else "📍 Próximo a você: $effectiveTargetStop",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Selo Previsto vs Ao Vivo
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

                    if (scheduleStatus.isServiceActive) {
                        val etaColor = when {
                            minutesToArrival > 10 -> StatusGreen
                            minutesToArrival in 5..10 -> StatusAmber
                            else -> StatusRed
                        }

                        if (minutesToArrival >= 60) {
                            val h = minutesToArrival / 60
                            val m = minutesToArrival % 60
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${h}h${String.format("%02d", m)}",
                                    style = MaterialTheme.typography.displayMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = etaColor
                                )
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = Math.max(0, minutesToArrival).toString(),
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
                                if (minutesToArrival in 1..4) {
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
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quantidade de veículos programados para a saída / comboio
                        val activeOrNextTrip = scheduleStatus.activeTrip ?: scheduleStatus.nextTrip
                        val vehicleCount = activeOrNextTrip?.vehicles ?: 1
                        val busEmojis = "🚌".repeat(vehicleCount)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = busEmojis,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = "$vehicleCount veículo${if (vehicleCount > 1) "s" else ""} programado${if (vehicleCount > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Onde o ônibus está agora
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                MorabusIcons.Navigation,
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

                        // Informação de quando sair (caminhada calculada pelo GPS)
                        if (!departureAdviceText.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = departureAdviceText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (effectiveDelayMinutes > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⏱️ Atraso estimado (+${effectiveDelayMinutes} min) • Horário ajustado em tempo real",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusAmber,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (effectiveDelayMinutes < 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "⚡ Adiantado (${effectiveDelayMinutes} min) • Horário ajustado em tempo real",
                                style = MaterialTheme.typography.bodySmall,
                                color = StatusGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        // ESTADO ENCERRADO / DOMINGO / FERIADO (Respeitando Calendário Nacional, MG, BH e UFMG)
                        Text(
                            text = if (scheduleStatus.isHolidayOrSunday) {
                                scheduleStatus.holidayOrSundayMessage ?: stringResource(R.string.home_service_finished_title)
                            } else {
                                stringResource(R.string.home_service_finished_title)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = scheduleStatus.nextServiceFullText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Botão para ativar/parar Notificação ao Vivo
                    Button(
                        onClick = {
                            if (isLiveNotificationRunning) {
                                LiveTripService.stop(context)
                            } else {
                                val neededPermissions = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                val missing = neededPermissions.filter {
                                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                                }
                                if (missing.isNotEmpty()) {
                                    permissionLauncher.launch(missing.toTypedArray())
                                } else {
                                    LiveTripService.start(
                                        context = context,
                                        targetStop = effectiveTargetStop,
                                        etaMinutes = minutesToArrival,
                                        departureAdvice = departureAdviceText,
                                        isNearestGps = favoriteStop == null
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isLiveNotificationRunning) StatusRed else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isLiveNotificationRunning) MorabusIcons.Stop else Icons.Default.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLiveNotificationRunning) "Parar Notificação ao Vivo" else "Ativar Notificação ao Vivo",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // 2. PAINEL DUPLO DE SENTIDOS (Saindo da Moradia e Saindo do Campus - Clicáveis para filtrar grade)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Sentido Campus: Saindo da Moradia
                OutlinedCard(
                    onClick = {
                        UserPreferencesRepository.setSchedulesFilter(context, "moradia")
                        onNavigateToSchedules()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🏠 Saindo da Moradia",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scheduleStatus.nextMoradiaTrip?.time ?: "Encerrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val moradiaSubText = scheduleStatus.nextMoradiaMinutes?.let { min ->
                            val adjusted = Math.max(0, min + effectiveDelayMinutes)
                            val suffix = if (scheduleStatus.nextMoradiaTrip?.isPassagem == true) " (➔ UFMG)" else ""
                            "em ${ScheduleEngine.formatDurationMin(adjusted)}$suffix"
                        } ?: scheduleStatus.nextServiceShortText
                        Text(
                            text = moradiaSubText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Sentido Moradia: Saindo do Campus
                OutlinedCard(
                    onClick = {
                        UserPreferencesRepository.setSchedulesFilter(context, "campus")
                        onNavigateToSchedules()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎓 Saindo do Campus",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scheduleStatus.nextCampusTrip?.time ?: "Encerrado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val campusSubText = scheduleStatus.nextCampusMinutes?.let { min ->
                            val adjusted = Math.max(0, min + effectiveDelayMinutes)
                            "em ${ScheduleEngine.formatDurationMin(adjusted)}"
                        } ?: scheduleStatus.nextServiceShortText
                        Text(
                            text = campusSubText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. CARD RESTAURANTE UNIVERSITÁRIO (RU FUMP) COM RECOMENDAÇÃO DE MOBILIDADE
            br.ufmg.morabus.app.ui.components.RuRecommendationCard(
                recommendation = ruRecommendation,
                pampulhaStatus = ruPampulhaStatus,
                onOpenRuList = { isRuModalOpen = true }
            )

            // 4. CARD "ÔNIBUS AO VIVO" (GPS Colaborativo + Ações Rápidas sem Bateria)
            val currentSharingTrip = scheduleStatus.activeTrip?.time ?: scheduleStatus.nextTrip?.time ?: "22:15"
            LiveBusCard(
                isSharing = isSharingLocation,
                sharingCount = activeSharingContributors,
                currentTripTime = currentSharingTrip,
                targetStopName = effectiveTargetStop,
                recentPassage = recentPassage,
                waitingCount = waitingCounts[effectiveTargetStop ?: ""] ?: 0,
                isUserWaiting = userWaitingStop == effectiveTargetStop && !effectiveTargetStop.isNullOrBlank(),
                onStartSharing = {
                    val neededPermissions = mutableListOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val missing = neededPermissions.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isNotEmpty()) {
                        permissionLauncher.launch(missing.toTypedArray())
                    } else {
                        br.ufmg.morabus.app.sharing.BusSharingService.start(context, currentSharingTrip)
                    }
                },
                onStopSharing = {
                    br.ufmg.morabus.app.sharing.BusSharingService.stop(context)
                },
                onQuickPassageReport = {
                    val stop = effectiveTargetStop ?: "Moradias 2 e 3"
                    FirebaseRealtimeRepository.sendQuickPassageReport(stop, currentSharingTrip)
                    scope.launch {
                        snackbarHostState.showSnackbar("Relato rápido registrado: ônibus acabou de passar em $stop")
                    }
                },
                onToggleWaiting = {
                    val stop = effectiveTargetStop ?: "Moradias 2 e 3"
                    if (userWaitingStop == stop) {
                        FirebaseRealtimeRepository.unregisterWaitingAtStop(stop)
                        scope.launch {
                            snackbarHostState.showSnackbar("Você não está mais aguardando em $stop")
                        }
                    } else {
                        FirebaseRealtimeRepository.registerWaitingAtStop(stop)
                        scope.launch {
                            snackbarHostState.showSnackbar("Confirmado: você está aguardando em $stop")
                        }
                    }
                },
                onSendConditionReport = { code ->
                    FirebaseRealtimeRepository.sendCommunityAlert(code, currentSharingTrip)
                    scope.launch {
                        snackbarHostState.showSnackbar("Relato registrado no sistema. Obrigado!")
                    }
                }
            )

            // 4. ATALHO CONTEXTUAL "IR PARA A MORADIA / CAMPUS" (Detectado inteligentemente por GPS)
            val contextualWalkingText = remember(nearestGpsResult) {
                if (nearestGpsResult != null) {
                    val walkMin = Math.max(1, Math.ceil(nearestGpsResult.second / 75.0).toInt())
                    val distM = nearestGpsResult.second.toInt()
                    "Caminhada até ${nearestGpsResult.first.name}: ~$walkMin min (${distM}m)"
                } else {
                    "Caminhada ~4 min até o ponto"
                }
            }

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
                                imageVector = if (userLocationIsCampus) Icons.Default.Home else MorabusIcons.School,
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

                        IconButton(onClick = { manualCampusToggle = !userLocationIsCampus }) {
                            Icon(
                                MorabusIcons.SwapVert,
                                contentDescription = "Inverter Direção Contextual",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val relevantTrip = if (userLocationIsCampus) scheduleStatus.nextCampusTrip else scheduleStatus.nextMoradiaTrip
                    val relevantMinutes = if (userLocationIsCampus) scheduleStatus.nextCampusMinutes else scheduleStatus.nextMoradiaMinutes

                    if (relevantTrip != null && relevantMinutes != null) {
                        val adjustedRelMin = Math.max(0, relevantMinutes + effectiveDelayMinutes)
                        Text(
                            text = "Próxima saída (${relevantTrip.origin}): ${relevantTrip.time} (em ${ScheduleEngine.formatDurationMin(adjustedRelMin)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Encerrado hoje nesta direção • ${scheduleStatus.nextServiceShortText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = contextualWalkingText,
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
                            Icon(MorabusIcons.Map, contentDescription = null, modifier = Modifier.size(18.dp))
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
                    UserPreferencesRepository.setFavoriteStop(context, selected)
                    isBottomSheetOpen = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.favorite_stop_snackbar_format, selected),
                            actionLabel = context.getString(R.string.favorite_stop_snackbar_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            UserPreferencesRepository.setFavoriteStop(context, previousFavoriteStop)
                        }
                    }
                },
                onRemoveFavorite = {
                    previousFavoriteStop = favoriteStop
                    UserPreferencesRepository.setFavoriteStop(context, null)
                    isBottomSheetOpen = false
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(R.string.favorite_stop_removed_snackbar),
                            actionLabel = context.getString(R.string.favorite_stop_snackbar_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            UserPreferencesRepository.setFavoriteStop(context, previousFavoriteStop)
                        }
                    }
                },
                onDismissRequest = { isBottomSheetOpen = false }
            )
        }

        // MODAL DE HORÁRIOS OFICIAIS DOS 5 RUS DA UFMG
        if (isRuModalOpen) {
            br.ufmg.morabus.app.ui.components.RuAllUnitsModal(
                onDismissRequest = { isRuModalOpen = false }
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
