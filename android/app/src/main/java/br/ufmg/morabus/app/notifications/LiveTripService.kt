package br.ufmg.morabus.app.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.ufmg.morabus.app.data.ScheduleEngine
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class LiveTripService : Service() {

    companion object {
        const val EXTRA_TARGET_STOP = "extra_target_stop"
        const val EXTRA_ETA_MINUTES = "extra_eta_minutes"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_DEPARTURE_ADVICE = "extra_departure_advice"
        const val EXTRA_WALKING_MINUTES = "extra_walking_minutes"
        const val EXTRA_IS_NEAREST_GPS = "extra_is_nearest_gps"

        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val ACTION_MARK_ON_BOARD = "action_mark_on_board"
        const val ACTION_SNOOZE = "action_snooze"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        fun checkAndUpdateRunningState(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val hasNotification = notificationManager.activeNotifications?.any {
                it.id == StandardLiveTripNotifier.NOTIFICATION_ID
            } ?: false
            val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance())
            if (!status.isServiceActive && hasNotification) {
                StandardLiveTripNotifier(context).dismiss()
                _isRunning.value = false
                return false
            }
            _isRunning.value = hasNotification
            return hasNotification
        }

        fun start(
            context: Context,
            targetStop: String?,
            etaMinutes: Int = 18,
            departureAdvice: String? = null,
            walkingMinutes: Int? = null,
            isNearestGps: Boolean = false
        ) {
            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            val resolvedTarget = if (!targetStop.isNullOrBlank()) targetStop else "Ponto mais próximo"
            val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), resolvedTarget)
            if (!status.isServiceActive) {
                _isRunning.value = false
                return
            }

            _isRunning.value = true
            val isBusMoving = status.activeTrip != null
            val initialPhase = when {
                !isBusMoving -> TripPhase.DEPARTURE
                etaMinutes <= 3 -> TripPhase.APPROACHING
                else -> TripPhase.EN_ROUTE
            }
            val initialLocation = if (!isBusMoving) {
                val nextTime = status.nextTrip?.time ?: "--"
                val nextOrigin = status.nextTrip?.origin ?: "Terminal"
                "No ponto final • Próxima saída às $nextTime ($nextOrigin)"
            } else {
                status.estimatedLocationText
            }

            if (!hasFine && !hasCoarse) {
                val notifier = StandardLiveTripNotifier(context)
                val targetTs = System.currentTimeMillis() + (etaMinutes * 60 * 1000L)
                notifier.showOrUpdate(
                    LiveTripState(
                        phase = initialPhase,
                        targetStop = resolvedTarget,
                        currentLocationName = initialLocation,
                        etaMinutes = etaMinutes,
                        targetTimestampMs = targetTs,
                        progressPercent = 45,
                        delayMinutes = 0,
                        nextStopName = resolvedTarget,
                        departureAdvice = departureAdvice,
                        walkingMinutes = walkingMinutes
                    )
                )
                return
            }

            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_STOP, resolvedTarget)
                putExtra(EXTRA_ETA_MINUTES, etaMinutes)
                putExtra(EXTRA_DEPARTURE_ADVICE, departureAdvice)
                putExtra(EXTRA_WALKING_MINUTES, walkingMinutes ?: -1)
                putExtra(EXTRA_IS_NEAREST_GPS, isNearestGps || targetStop.isNullOrBlank())
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {
                val notifier = StandardLiveTripNotifier(context)
                notifier.showOrUpdate(
                    LiveTripState(
                        phase = if (etaMinutes <= 5) TripPhase.APPROACHING else TripPhase.EN_ROUTE,
                        targetStop = resolvedTarget,
                        currentLocationName = "A caminho da parada",
                        etaMinutes = etaMinutes,
                        targetTimestampMs = System.currentTimeMillis() + (etaMinutes * 60 * 1000L),
                        progressPercent = 45,
                        delayMinutes = 0,
                        nextStopName = resolvedTarget,
                        departureAdvice = departureAdvice,
                        walkingMinutes = walkingMinutes
                    )
                )
            }
        }

        fun stop(context: Context) {
            _isRunning.value = false
            try {
                context.stopService(Intent(context, LiveTripService::class.java))
                StandardLiveTripNotifier(context).dismiss()
            } catch (_: Exception) {
            }
        }

        fun markOnBoard(context: Context) {
            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_MARK_ON_BOARD
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }

        fun snooze(context: Context, minutes: Int = 5) {
            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_SNOOZE_MINUTES, minutes)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private lateinit var notifier: LiveTripNotifier
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentPhase = TripPhase.EN_ROUTE
    private var targetStop = "Ponto mais próximo"
    private var remainingMinutes = 15
    private var targetTimestampMs = System.currentTimeMillis() + (15 * 60 * 1000)
    private var departureAdvice: String? = null
    private var walkingMinutes: Int? = null
    private var isUsingNearestGpsStop = false

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        notifier = StandardLiveTripNotifier(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        _isRunning.value = true
        when (intent?.action) {
            ACTION_START -> {
                targetStop = intent.getStringExtra(EXTRA_TARGET_STOP) ?: "Ponto mais próximo"
                val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), targetStop)
                if (!status.isServiceActive) {
                    _isRunning.value = false
                    stopSelf()
                    return START_NOT_STICKY
                }

                remainingMinutes = intent.getIntExtra(EXTRA_ETA_MINUTES, 15)
                targetTimestampMs = System.currentTimeMillis() + (remainingMinutes * 60 * 1000)
                departureAdvice = intent.getStringExtra(EXTRA_DEPARTURE_ADVICE)
                val walkExtra = intent.getIntExtra(EXTRA_WALKING_MINUTES, -1)
                walkingMinutes = if (walkExtra > 0) walkExtra else null
                isUsingNearestGpsStop = intent.getBooleanExtra(EXTRA_IS_NEAREST_GPS, false)

                val isBusMoving = status.activeTrip != null
                currentPhase = when {
                    !isBusMoving -> TripPhase.DEPARTURE
                    remainingMinutes <= 3 -> TripPhase.APPROACHING
                    else -> TripPhase.EN_ROUTE
                }

                if (startForegroundWithState()) {
                    startLocationUpdates()
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP -> {
                _isRunning.value = false
                stopSelf()
            }
            ACTION_MARK_ON_BOARD -> {
                currentPhase = TripPhase.ON_BOARD
                updateTripState()
            }
            ACTION_SNOOZE -> {
                val snoozeMin = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)
                remainingMinutes += snoozeMin
                targetTimestampMs += (snoozeMin * 60 * 1000)
                updateTripState()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithState(): Boolean {
        val state = buildCurrentState()
        val notification = notifier.buildNotification(state)
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else 0

        return try {
            ServiceCompat.startForeground(
                this,
                StandardLiveTripNotifier.NOTIFICATION_ID,
                notification,
                foregroundType
            )
            true
        } catch (_: Exception) {
            notifier.showOrUpdate(state)
            false
        }
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            8000L
        ).apply {
            setMinUpdateIntervalMillis(5000L)
            setMaxUpdateDelayMillis(12000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val loc = locationResult.lastLocation ?: return
                if (isUsingNearestGpsStop) {
                    val nearest = ScheduleEngine.findNearestStop(loc.latitude, loc.longitude)
                    if (nearest != null) {
                        targetStop = nearest.first.name
                        val dist = nearest.second
                        val walk = Math.max(1, Math.ceil(dist / 75.0).toInt())
                        walkingMinutes = walk

                        val cal = Calendar.getInstance()
                        val curMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                        val status = ScheduleEngine.calculateHomeStatus(cal, targetStop)
                        val arrival = status.favoriteArrival
                        if (arrival != null) {
                            remainingMinutes = arrival.minutesUntilArrival
                            targetTimestampMs = System.currentTimeMillis() + (remainingMinutes * 60 * 1000L)
                            departureAdvice = ScheduleEngine.calculateDepartureAdvice(dist, remainingMinutes, curMin)
                        }
                    }
                }
                checkTripProgression()
            }
        }

        try {
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
        }
    }

    private fun checkTripProgression() {
        val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), targetStop)
        if (!status.isServiceActive) {
            _isRunning.value = false
            stopSelf()
            return
        }

        val remainingSec = Math.max(0, (targetTimestampMs - System.currentTimeMillis()) / 1000)
        remainingMinutes = Math.max(0, (remainingSec / 60).toInt())

        when {
            remainingMinutes == 0 && currentPhase == TripPhase.ON_BOARD -> {
                currentPhase = TripPhase.ALIGHT_NEXT
                updateTripState()
                serviceScope.launch {
                    delay(120000)
                    stopSelf()
                }
            }
            remainingMinutes <= 3 && currentPhase == TripPhase.EN_ROUTE -> {
                currentPhase = TripPhase.APPROACHING
                updateTripState()
            }
            else -> {
                updateTripState()
            }
        }
    }

    private fun updateTripState() {
        val state = buildCurrentState()
        notifier.showOrUpdate(state)
    }

    private fun buildCurrentState(): LiveTripState {
        val totalMin = 20
        val progress = Math.min(100, Math.max(5, ((totalMin - remainingMinutes) * 100) / totalMin))

        val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), targetStop)
        val isBusMoving = status.activeTrip != null

        val locationText = when {
            currentPhase == TripPhase.ON_BOARD -> "Em deslocamento pelo Campus"
            currentPhase == TripPhase.ALIGHT_NEXT -> "Próxima parada: $targetStop"
            currentPhase == TripPhase.COMPLETED -> "Viagem concluída"
            isBusMoving -> {
                if (remainingMinutes <= 3) "Aproximando-se da parada"
                else status.estimatedLocationText
            }
            else -> {
                val nextTime = status.nextTrip?.time ?: "--"
                val nextOrigin = status.nextTrip?.origin ?: "Terminal"
                "No ponto final • Saída às $nextTime ($nextOrigin)"
            }
        }

        val effectivePhase = when {
            currentPhase == TripPhase.ON_BOARD || currentPhase == TripPhase.ALIGHT_NEXT || currentPhase == TripPhase.COMPLETED -> currentPhase
            !isBusMoving -> TripPhase.DEPARTURE
            remainingMinutes <= 3 -> TripPhase.APPROACHING
            else -> TripPhase.EN_ROUTE
        }

        return LiveTripState(
            phase = effectivePhase,
            targetStop = targetStop,
            currentLocationName = locationText,
            etaMinutes = remainingMinutes,
            targetTimestampMs = targetTimestampMs,
            progressPercent = progress,
            delayMinutes = 0,
            nextStopName = if (currentPhase == TripPhase.ON_BOARD) "Educação Física" else targetStop,
            departureAdvice = departureAdvice,
            walkingMinutes = walkingMinutes
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        try {
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        } catch (_: Exception) {
        }
        notifier.dismiss()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
