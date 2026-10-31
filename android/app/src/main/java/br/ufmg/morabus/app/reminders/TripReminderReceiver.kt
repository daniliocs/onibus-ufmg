package br.ufmg.morabus.app.reminders

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import br.ufmg.morabus.app.MainActivity
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.data.ScheduleEngine
import br.ufmg.morabus.app.data.UserPreferencesRepository
import java.util.Calendar

class TripReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_REMINDERS = "morabus_routine_reminders"
        const val EXTRA_TRIP_TIME = "extra_trip_time"
        const val EXTRA_ORIGIN = "extra_origin"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val tripTime = intent.getStringExtra(EXTRA_TRIP_TIME) ?: return
        val origin = intent.getStringExtra(EXTRA_ORIGIN) ?: "Moradia"

        createChannel(context)

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        UserPreferencesRepository.init(context)
        val vibrationEnabled = UserPreferencesRepository.vibrationEnabled.value

        val builder = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_bus_tracker)
            .setContentTitle("⏰ Lembrete de Viagem: Ônibus das $tripTime")
            .setContentText("Saindo de $origin em 15 minutos! Prepare-se para caminhar.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("O ônibus das $tripTime sairá de $origin em aproximadamente 15 minutos.\nSaia com antecedência para chegar ao seu ponto com tranquilidade!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        if (vibrationEnabled) {
            builder.setVibrate(longArrayOf(0, 350, 200, 350))
        } else {
            builder.setVibrate(longArrayOf(0))
        }

        val notificationId = 888000 + ScheduleEngine.timeToMinutes(tripTime)
        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (_: SecurityException) {}
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_REMINDERS,
                "Lembretes da Grade de Horários",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos prévios de 15 minutos antes dos ônibus selecionados da sua rotina"
                enableLights(true)
                setShowBadge(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}

object TripReminderManager {

    fun scheduleReminder(context: Context, tripTime: String, origin: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, TripReminderReceiver::class.java).apply {
            putExtra(TripReminderReceiver.EXTRA_TRIP_TIME, tripTime)
            putExtra(TripReminderReceiver.EXTRA_ORIGIN, origin)
        }
        val requestCode = 770000 + ScheduleEngine.timeToMinutes(tripTime)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tripMin = ScheduleEngine.timeToMinutes(tripTime)
        val reminderMin = tripMin - 15 // 15 minutos antes

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderMin / 60)
            set(Calendar.MINUTE, reminderMin % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1) // agenda para o dia seguinte
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun cancelReminder(context: Context, tripTime: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TripReminderReceiver::class.java)
        val requestCode = 770000 + ScheduleEngine.timeToMinutes(tripTime)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
