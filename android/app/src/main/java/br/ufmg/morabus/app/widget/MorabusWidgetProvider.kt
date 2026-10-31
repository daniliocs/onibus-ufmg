package br.ufmg.morabus.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import br.ufmg.morabus.app.MainActivity
import br.ufmg.morabus.app.R
import br.ufmg.morabus.app.data.ScheduleEngine
import br.ufmg.morabus.app.data.UserPreferencesRepository
import java.util.Calendar

class MorabusWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "br.ufmg.morabus.app.ACTION_REFRESH_WIDGET"
        private const val TICK_REQUEST_CODE = 4401

        fun requestRefresh(context: Context) = updateAllWidgets(context)

        private fun tickPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MorabusWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            return PendingIntent.getBroadcast(
                context,
                TICK_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        /**
         * Mantém o contador do widget fresco: a cada ~1 min enquanto há ônibus rodando ou prestes a sair,
         * e a cada 15 min fora disso (economiza bateria). Alarme inexato: não exige permissão especial.
         */
        private fun scheduleNextTick(context: Context, serviceActive: Boolean) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val delayMs = if (serviceActive) 60_000L else 15 * 60_000L
            try {
                alarmManager.setAndAllowWhileIdle(
                    android.app.AlarmManager.RTC,
                    System.currentTimeMillis() + delayMs,
                    tickPendingIntent(context)
                )
            } catch (_: Exception) {
            }
        }

        private fun cancelTicks(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            alarmManager.cancel(tickPendingIntent(context))
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, MorabusWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val intent = Intent(context, MorabusWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
                }
                context.sendBroadcast(intent)
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextTick(context, true)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelTicks(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        UserPreferencesRepository.init(context)
        ScheduleEngine.initAndSyncDynamicSchedules(context)
        val favoriteStop = UserPreferencesRepository.favoriteStop.value
        val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), favoriteStop)
        scheduleNextTick(context, status.isServiceActive)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_morabus)

            if (!favoriteStop.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_label_target, "⭐ $favoriteStop")
            } else {
                views.setTextViewText(R.id.widget_label_target, "Próxima saída geral")
            }

            if (status.isServiceActive && (status.favoriteArrival != null || status.nextTrip != null)) {
                val fav = status.favoriteArrival
                val trip = status.nextTrip ?: status.activeTrip

                if (fav != null) {
                    views.setTextViewText(R.id.widget_tv_time, fav.arrivalTime)
                    val favCountdown = if (fav.minutesUntilArrival <= 0) "AGORA" else "em ${ScheduleEngine.formatDurationMin(fav.minutesUntilArrival)}"
                    views.setTextViewText(
                        R.id.widget_tv_countdown,
                        favCountdown
                    )
                    views.setTextViewText(
                        R.id.widget_tv_origin,
                        "Saída ${fav.scheduledDepartureTime} (${fav.origin})"
                    )
                } else if (trip != null) {
                    views.setTextViewText(R.id.widget_tv_time, trip.time)
                    val minText = status.nextTripMinutes?.let {
                        if (it <= 0) "AGORA" else "em ${ScheduleEngine.formatDurationMin(it)}"
                    } ?: "Em viagem"
                    views.setTextViewText(R.id.widget_tv_countdown, minText)

                    val originDesc = when {
                        trip.isPassagem -> "Moradia ➔ Ponto final"
                        trip.origin.contains("Moradia") -> "Moradia ➔ Campus"
                        else -> "Campus ➔ Moradia"
                    }
                    views.setTextViewText(R.id.widget_tv_origin, originDesc)
                }

                val vCount = trip?.vehicles ?: 1
                views.setTextViewText(R.id.widget_tv_vehicles, "🚌 ${vCount}x")

                val moradiaTime = status.nextMoradiaTrip?.time ?: status.firstMoradiaTripTomorrow?.time ?: "--:--"
                val campusTime = status.nextCampusTrip?.time ?: status.firstCampusTripTomorrow?.time ?: "--:--"
                views.setTextViewText(
                    R.id.widget_tv_directions,
                    "🏠 $moradiaTime  •  🎓 $campusTime"
                )
            } else {
                views.setTextViewText(R.id.widget_tv_time, "Encerrado")
                val reason = if (status.isHolidayOrSunday) {
                    status.holidayOrSundayMessage ?: "Sem ônibus hoje"
                } else {
                    "Concluído hoje"
                }
                views.setTextViewText(R.id.widget_tv_countdown, "")
                views.setTextViewText(R.id.widget_tv_origin, reason)
                views.setTextViewText(R.id.widget_tv_vehicles, "💤")
                val moradiaTime = status.firstMoradiaTripTomorrow?.time ?: "06:45"
                val campusTime = status.firstCampusTripTomorrow?.time ?: "07:20"
                views.setTextViewText(R.id.widget_tv_directions, "🏠 $moradiaTime  •  🎓 $campusTime")
            }

            // Ação ao tocar no card do widget: abre o app
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, openPendingIntent)

            // Ação ao tocar no botão de atualizar
            val refreshIntent = Intent(context, MorabusWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}