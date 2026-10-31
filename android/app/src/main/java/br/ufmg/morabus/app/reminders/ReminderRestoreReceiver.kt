package br.ufmg.morabus.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import br.ufmg.morabus.app.widget.MorabusWidgetProvider

/**
 * Restaura alarmes de lembrete (e atualiza o widget) após reinício do aparelho,
 * atualização do app ou mudança de data/fuso horário. Alarmes do sistema são perdidos nesses casos.
 */
class ReminderRestoreReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED -> {
                try {
                    TripReminderManager.rescheduleAll(context)
                } catch (_: Exception) {
                }
                try {
                    MorabusWidgetProvider.requestRefresh(context)
                } catch (_: Exception) {
                }
            }
        }
    }
}
