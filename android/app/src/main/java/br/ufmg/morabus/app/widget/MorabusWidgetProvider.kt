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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            updateAllWidgets(context)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        UserPreferencesRepository.init(context)
        val favoriteStop = UserPreferencesRepository.favoriteStop.value
        val status = ScheduleEngine.calculateHomeStatus(Calendar.getInstance(), favoriteStop)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_morabus)

            // Configura textos
            if (!favoriteStop.isNullOrBlank()) {
                views.setTextViewText(R.id.widget_label_target, "⭐ $favoriteStop")
            } else {
                views.setTextViewText(R.id.widget_label_target, "Próximo ônibus")
            }

            if (status.isServiceActive && status.nextTrip != null) {
                views.setTextViewText(R.id.widget_tv_time, status.nextTrip.time)
                val minText = status.nextTripMinutes?.let { "em $it min" } ?: ""
                views.setTextViewText(R.id.widget_tv_countdown, minText)

                val originDesc = when {
                    status.nextTrip.isPassagem -> "Moradia ➔ Ponto final"
                    status.nextTrip.origin.contains("Moradia") -> "Moradia ➔ Campus"
                    else -> "Campus ➔ Moradia"
                }
                views.setTextViewText(R.id.widget_tv_origin, originDesc)

                val vCount = status.nextTrip.vehicles
                views.setTextViewText(
                    R.id.widget_tv_vehicles,
                    "🚌 $vCount veículo${if (vCount > 1) "s" else ""}"
                )
            } else {
                views.setTextViewText(R.id.widget_tv_time, "Encerrado")
                views.setTextViewText(R.id.widget_tv_countdown, "Sem viagens hoje")
                views.setTextViewText(R.id.widget_tv_origin, "Retorna às 06:45")
                views.setTextViewText(R.id.widget_tv_vehicles, "💤 Garagem")
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
