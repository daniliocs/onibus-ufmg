package br.ufmg.morabus.app.sharing

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import br.ufmg.morabus.app.MainActivity
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.data.FirebaseRealtimeRepository
import br.ufmg.morabus.app.data.ScheduleEngine
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

class BusSharingService : Service() {

    companion object {
        const val NOTIFICATION_ID = 999200
        const val CHANNEL_ID = "morabus_sharing"
        const val EXTRA_TRIP_TIME = "extra_trip_time"

        const val ACTION_START = "br.ufmg.morabus.app.ACTION_START_SHARING"
        const val ACTION_STOP = "br.ufmg.morabus.app.ACTION_STOP_SHARING"

        private val _isSharing = MutableStateFlow(false)
        val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

        private val _currentTrip = MutableStateFlow<String?>(null)
        val currentTrip: StateFlow<String?> = _currentTrip.asStateFlow()

        fun start(context: Context, tripTime: String) {
            val intent = Intent(context, BusSharingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TRIP_TIME, tripTime)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, BusSharingService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null

    private var activeTripTime: String = "22:15"
    private var lastMovementTimeMs: Long = System.currentTimeMillis()
    private var lastFixedLat: Double = 0.0
    private var lastFixedLon: Double = 0.0

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSharingAndSelf()
            return START_NOT_STICKY
        }

        activeTripTime = intent?.getStringExtra(EXTRA_TRIP_TIME) ?: "22:15"
        _currentTrip.value = activeTripTime
        _isSharing.value = true
        lastMovementTimeMs = System.currentTimeMillis()

        FirebaseRealtimeRepository.startSharing(activeTripTime)

        val notification = buildNotification("Iniciando compartilhamento...", "Aguardando sinal GPS")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        val fineGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fineGranted) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 8000L).apply {
            setMinUpdateIntervalMillis(4000L)
            setMinUpdateDistanceMeters(3.0f)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                handleNewLocation(loc.latitude, loc.longitude, loc.speed.toDouble() * 3.6)
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback!!, Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    private fun handleNewLocation(lat: Double, lon: Double, speedKmh: Double) {
        val now = System.currentTimeMillis()

        // 1. Verificação de imobilidade (> 20 minutos parado no mesmo lugar)
        if (lastFixedLat == 0.0 && lastFixedLon == 0.0) {
            lastFixedLat = lat
            lastFixedLon = lon
            lastMovementTimeMs = now
        } else {
            val distFromFixed = ScheduleEngine.calculateDistanceMeters(lat, lon, lastFixedLat, lastFixedLon)
            if (distFromFixed > 35.0) { // Deslocou-se mais de 35 metros
                lastFixedLat = lat
                lastFixedLon = lon
                lastMovementTimeMs = now
            } else if (now - lastMovementTimeMs > 20 * 60 * 1000L) {
                // Parado no mesmo lugar há mais de 20 minutos -> Auto encerramento inteligente
                stopSharingAndSelf()
                return
            }
        }

        // 2. Filtro Anti-Ruído: raio de 1 km (1000m) em relação aos pontos oficiais da linha
        val nearestPair = ScheduleEngine.findNearestStop(lat, lon)
        val distanceToRouteMeters = nearestPair?.second ?: Double.MAX_VALUE

        if (distanceToRouteMeters > 1000.0) {
            // Fora do raio de 1km da rota: não transmite coordenada falsa para a comunidade
            val notif = buildNotification(
                "Ônibus das $activeTripTime",
                "Aguardando aproximação do itinerário (> 1km de distância)"
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, notif)
            return
        }

        // 3. Cálculo completo no próprio aplicativo
        val nearestStopName = nearestPair?.first?.name ?: "Itinerário"
        val streetDesc = "Próximo a $nearestStopName"

        // Cálculo de atraso estimado
        val cal = Calendar.getInstance()
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val tripStartMinutes = ScheduleEngine.timeToMinutes(activeTripTime)
        val expectedMinutesAtStop = tripStartMinutes + getEstimatedStopOffset(nearestStopName)
        val delayMinutes = Math.max(0, currentMinutes - expectedMinutesAtStop)

        // Envia ao Firebase
        FirebaseRealtimeRepository.sendLocationTelemetry(
            lat = lat,
            lon = lon,
            speedKmh = speedKmh,
            streetName = streetDesc,
            delayMinutes = delayMinutes,
            tripTime = activeTripTime
        )

        val delayText = if (delayMinutes > 0) " (+${delayMinutes} min)" else " (no horário)"
        val notif = buildNotification(
            "🚌 Compartilhando · Viagem das $activeTripTime",
            "$streetDesc$delayText • ${speedKmh.toInt()} km/h"
        )
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notif)
    }

    private fun getEstimatedStopOffset(stopName: String): Int {
        val campusOffset = ScheduleEngine.ROTA_CAMPUS.firstOrNull { it.stopName == stopName }?.timeMinutes
        if (campusOffset != null) return campusOffset
        val moradiaOffset = ScheduleEngine.ROTA_MORADIA.firstOrNull { it.stopName == stopName }?.timeMinutes
        if (moradiaOffset != null) return moradiaOffset
        return 5
    }

    private fun buildNotification(title: String, text: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, BusSharingService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bus_tracker)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Parar Compartilhamento", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Compartilhamento do Ônibus (Colaborativo)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação ativa durante o compartilhamento voluntário da posição do ônibus"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun stopSharingAndSelf() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        FirebaseRealtimeRepository.stopSharing()
        _isSharing.value = false
        _currentTrip.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopSharingAndSelf()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
