package br.ufmg.morabus.app.notifications

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LiveTripService : Service() {

    companion object {
        const val EXTRA_ACTION = "extra_action"
        const val EXTRA_TARGET_STOP = "extra_target_stop"
        const val EXTRA_ETA_MINUTES = "extra_eta_minutes"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
        const val ACTION_MARK_ON_BOARD = "action_mark_on_board"
        const val ACTION_SNOOZE = "action_snooze"

        fun start(context: Context, targetStop: String = "Moradias 2 e 3", etaMinutes: Int = 18) {
            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_STOP, targetStop)
                putExtra(EXTRA_ETA_MINUTES, etaMinutes)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun markOnBoard(context: Context) {
            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_MARK_ON_BOARD
            }
            context.startService(intent)
        }

        fun snooze(context: Context, minutes: Int = 5) {
            val intent = Intent(context, LiveTripService::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_SNOOZE_MINUTES, minutes)
            }
            context.startService(intent)
        }
    }

    private lateinit var notifier: LiveTripNotifier
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var currentPhase = TripPhase.EN_ROUTE
    private var targetStop = "Moradias 2 e 3"
    private var remainingMinutes = 15
    private var targetTimestampMs = System.currentTimeMillis() + (15 * 60 * 1000)

    override fun onCreate() {
        super.onCreate()
        notifier = StandardLiveTripNotifier(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                targetStop = intent.getStringExtra(EXTRA_TARGET_STOP) ?: "Moradias 2 e 3"
                remainingMinutes = intent.getIntExtra(EXTRA_ETA_MINUTES, 15)
                targetTimestampMs = System.currentTimeMillis() + (remainingMinutes * 60 * 1000)
                currentPhase = if (remainingMinutes <= 5) TripPhase.APPROACHING else TripPhase.EN_ROUTE

                startForegroundWithState()
                startLocationUpdates()
            }
            ACTION_STOP -> {
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

    private fun startForegroundWithState() {
        val state = buildCurrentState()
        // Inicializa o serviço em primeiro plano associado ao tipo location
        val foregroundType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else 0

        ServiceCompat.startForeground(
            this,
            StandardLiveTripNotifier.NOTIFICATION_ID,
            buildForegroundNotification(state),
            foregroundType
        )
    }

    private fun buildForegroundNotification(state: LiveTripState): android.app.Notification {
        notifier.showOrUpdate(state)
        // O notifier já posta no NotificationManager, mas retornamos um builder inicial para startForeground
        val builder = androidx.core.app.NotificationCompat.Builder(this, StandardLiveTripNotifier.CHANNEL_LIVE)
            .setSmallIcon(br.ufmg.morabus.app.R.drawable.ic_bus_tracker)
            .setContentTitle("Morabus Ao Vivo")
            .setContentText("Acompanhando viagem")
            .setOngoing(true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_PROGRESS)
        return builder.build()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, // Modo balanceado: economiza bateria e cumpre o requisito
            8000 // Intervalo de 8 segundos
        ).apply {
            setMinUpdateIntervalMillis(5000)
            setMaxUpdateDelayMillis(12000)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                // Atualização suave baseada na aproximação
                if (locationResult.lastLocation == null) return
                checkTripProgression()
            }
        }

        try {
            locationCallback?.let {
                fusedLocationClient.requestLocationUpdates(locationRequest, it, Looper.getMainLooper())
            }
        } catch (e: SecurityException) {
            // Permissão tratada pelo app
        }
    }

    private fun checkTripProgression() {
        val remainingSec = Math.max(0, (targetTimestampMs - System.currentTimeMillis()) / 1000)
        remainingMinutes = Math.max(0, (remainingSec / 60).toInt())

        when {
            remainingMinutes == 0 && currentPhase == TripPhase.ON_BOARD -> {
                // Fase 5: Desça na próxima
                currentPhase = TripPhase.ALIGHT_NEXT
                updateTripState()
                // Auto-encerramento após 2 minutos do desembarque
                serviceScope.launch {
                    delay(120000)
                    stopSelf()
                }
            }
            remainingMinutes <= 3 && currentPhase == TripPhase.EN_ROUTE -> {
                // Fase 3: Ônibus chegando
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

        return LiveTripState(
            phase = currentPhase,
            targetStop = targetStop,
            currentLocationName = when (currentPhase) {
                TripPhase.DEPARTURE -> "A caminho do ponto de embarque"
                TripPhase.EN_ROUTE -> "Passando na Av. Fleming"
                TripPhase.APPROACHING -> "Aproximando-se da parada"
                TripPhase.ON_BOARD -> "Em deslocamento pelo Campus"
                TripPhase.ALIGHT_NEXT -> "Próxima parada: $targetStop"
                TripPhase.COMPLETED -> "Viagem concluída"
            },
            etaMinutes = remainingMinutes,
            targetTimestampMs = targetTimestampMs,
            progressPercent = progress,
            delayMinutes = 0,
            nextStopName = if (currentPhase == TripPhase.ON_BOARD) "Educação Física" else targetStop
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        notifier.dismiss()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
