package br.ufmg.morabus.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.ufmg.morabus.app.MainActivity
import br.ufmg.morabus.app.R

class StandardLiveTripNotifier(
    private val context: Context
) : LiveTripNotifier {

    companion object {
        const val NOTIFICATION_ID = 999100

        const val CHANNEL_LIVE = "morabus_live"
        const val CHANNEL_DEPARTURE = "morabus_departure"
        const val CHANNEL_ALERTS = "morabus_alerts"

        const val ACTION_STOP = "br.ufmg.morabus.app.ACTION_STOP"
        const val ACTION_ON_BOARD = "br.ufmg.morabus.app.ACTION_ON_BOARD"
        const val ACTION_SNOOZE = "br.ufmg.morabus.app.ACTION_SNOOZE"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        // 1. Canal Ao Vivo (Silencioso, contínuo, integração Now Bar e Live Updates)
        val liveChannel = NotificationChannel(
            CHANNEL_LIVE,
            context.getString(R.string.notification_channel_live_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_live_desc)
            enableLights(false)
            enableVibration(false)
            vibrationPattern = null
            setSound(null, null)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // 2. Canal Hora de Sair (Prioridade alta com som e vibração para a caminhada)
        val departureChannel = NotificationChannel(
            CHANNEL_DEPARTURE,
            context.getString(R.string.notification_channel_departure_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_departure_desc)
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        // 3. Canal Avisos e Atrasos (Prioridade alta para alertas da linha)
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notification_channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alerts_desc)
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }

        notificationManager.createNotificationChannels(listOf(liveChannel, departureChannel, alertsChannel))
    }

    override fun isPromotionSupported(): Boolean {
        if (Build.VERSION.SDK_INT >= 36) {
            try {
                val method = notificationManager.javaClass.getMethod("canPostPromotedNotifications")
                return (method.invoke(notificationManager) as? Boolean) ?: false
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    override fun showOrUpdate(state: LiveTripState) {
        if (state.phase == TripPhase.COMPLETED) {
            dismiss()
            return
        }

        val channelId = if (state.phase == TripPhase.DEPARTURE) CHANNEL_DEPARTURE else CHANNEL_LIVE

        // Intenção ao clicar no corpo da notificação (abre o app na tela inicial)
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intenções das Ações
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, LiveTripActionReceiver::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val onBoardPendingIntent = PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, LiveTripActionReceiver::class.java).apply { action = ACTION_ON_BOARD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            3,
            Intent(context, LiveTripActionReceiver::class.java).apply { action = ACTION_SNOOZE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Textos da notificação por fase (Fases 1 a 5)
        val titleText = when (state.phase) {
            TripPhase.DEPARTURE -> context.getString(R.string.notification_phase_departure, state.etaMinutes)
            TripPhase.EN_ROUTE -> context.getString(R.string.notification_phase_en_route, state.etaMinutes)
            TripPhase.APPROACHING -> context.getString(R.string.notification_phase_approaching)
            TripPhase.ON_BOARD -> context.getString(R.string.notification_phase_on_board, state.nextStopName)
            TripPhase.ALIGHT_NEXT -> context.getString(R.string.notification_phase_alight_next, state.targetStop)
            TripPhase.COMPLETED -> ""
        }

        val delayText = if (state.delayMinutes > 0) " (+${state.delayMinutes} min atraso)" else ""
        val bodyText = "${state.targetStop} • ${state.currentLocationName}$delayText"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_bus_tracker)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setSubText(context.getString(R.string.notification_status_chip_eta, state.etaMinutes)) // Texto crítico do chip na barra de status
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true) // Crucial: nunca vibra nas atualizações subsequentes
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(if (state.phase == TripPhase.DEPARTURE) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(false)

        // Contagem regressiva nativa pelo SO (evita atualizações a cada segundo)
        if (state.targetTimestampMs > System.currentTimeMillis()) {
            builder.setWhen(state.targetTimestampMs)
            builder.setUsesChronometer(true)
            builder.setChronometerCountDown(true)
        }

        // Ações interativas da notificação
        when (state.phase) {
            TripPhase.DEPARTURE -> {
                builder.addAction(0, context.getString(R.string.action_notification_snooze_5m), snoozePendingIntent)
                builder.addAction(0, context.getString(R.string.action_notification_stop), stopPendingIntent)
            }
            TripPhase.EN_ROUTE, TripPhase.APPROACHING -> {
                builder.addAction(0, context.getString(R.string.action_notification_on_board), onBoardPendingIntent)
                builder.addAction(0, context.getString(R.string.action_notification_stop), stopPendingIntent)
            }
            TripPhase.ON_BOARD, TripPhase.ALIGHT_NEXT -> {
                builder.addAction(0, context.getString(R.string.action_notification_stop), stopPendingIntent)
            }
            else -> {}
        }

        // Tenta aplicar Notification.ProgressStyle e solicitação de promoção no Android 16/17 (API 36+)
        var appliedLiveUpdate = false
        if (Build.VERSION.SDK_INT >= 36) {
            try {
                if (isPromotionSupported()) {
                    val reqMethod = builder.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                    reqMethod.invoke(builder, true)
                }

                val progressStyleClass = Class.forName("android.app.Notification\$ProgressStyle")
                val progressStyle = progressStyleClass.getConstructor().newInstance()
                val setProgressMethod = progressStyleClass.getMethod("setProgress", Int::class.javaPrimitiveType)
                setProgressMethod.invoke(progressStyle, state.progressPercent)

                val setStyleMethod = builder.javaClass.getMethod("setStyle", Class.forName("android.app.Notification\$Style"))
                setStyleMethod.invoke(builder, progressStyle)
                appliedLiveUpdate = true
            } catch (e: Exception) {
                appliedLiveUpdate = false
            }
        }

        // Fallback para API < 36 ou se ProgressStyle não estiver disponível
        if (!appliedLiveUpdate) {
            builder.setProgress(100, state.progressPercent, false)
        }

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    override fun dismiss() {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }
}
