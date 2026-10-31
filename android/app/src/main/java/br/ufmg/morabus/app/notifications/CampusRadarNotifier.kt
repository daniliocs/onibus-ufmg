package br.ufmg.morabus.app.notifications

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
import br.ufmg.morabus.app.data.CampusNotice
import br.ufmg.morabus.app.data.NoticeSeverity

object CampusRadarNotifier {

    const val CHANNEL_ID = "comunicados_oficiais"
    // Dois slots fixos no Android: no máximo 2 notificações ativas (1 urgente e 1 regular)
    const val NOTIF_ID_URGENT = 1001
    const val NOTIF_ID_REGULAR = 1002

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Comunicados Oficiais UFMG & FUMP",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações automáticas sobre RUs, aulas, ponto facultativo e alertas de chuva na Pampulha"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun showNoticeNotification(context: Context, notice: CampusNotice) {
        createChannel(context)

        val notifId = if (notice.severity == NoticeSeverity.URGENTE) NOTIF_ID_URGENT else NOTIF_ID_REGULAR

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_TAB", "notices")
            putExtra("EXTRA_NOTICE_ID", notice.id)
            if (!notice.url.isNullOrBlank()) {
                putExtra("EXTRA_NOTICE_URL", notice.url)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val severityEmoji = notice.severity.emoji
        val titleText = "$severityEmoji ${notice.source.emoji} ${notice.title}"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bus_tracker)
            .setContentTitle(titleText)
            .setContentText(notice.summary)
            .setSubText("${notice.source.displayName} • ${notice.severity.displayName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${notice.summary}\n\nFonte: ${notice.source.displayName} • ${notice.dateFormatted}"))
            .setPriority(if (notice.severity == NoticeSeverity.URGENTE) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)          // Notificação comum: dispensa com um deslize
            .setOngoing(false)            // Não permanente!

        if (!notice.url.isNullOrBlank()) {
            val browserIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(notice.url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val browserPendingIntent = PendingIntent.getActivity(
                context,
                notifId + 10,
                browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                0,
                "Abrir Nota Oficial",
                browserPendingIntent
            )
        }

        if (br.ufmg.morabus.app.data.UserPreferencesRepository.vibrationEnabled.value) {
            val vibrationPattern = if (notice.severity == NoticeSeverity.URGENTE) {
                longArrayOf(0, 300, 150, 300, 150, 300)
            } else {
                longArrayOf(0, 250, 150, 250)
            }
            builder.setVibrate(vibrationPattern)
        }

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            // Atualiza o slot correspondente (substitui caso já exista uma notificação anterior daquela severidade)
            notificationManager.notify(notifId, builder.build())
        } catch (_: SecurityException) {
            // Permissão de notificação negada
        }
    }
}
