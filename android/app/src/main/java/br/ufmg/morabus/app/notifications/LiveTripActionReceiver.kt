package br.ufmg.morabus.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LiveTripActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            StandardLiveTripNotifier.ACTION_STOP -> {
                LiveTripService.stop(context)
            }
            StandardLiveTripNotifier.ACTION_ON_BOARD -> {
                LiveTripService.markOnBoard(context)
            }
            StandardLiveTripNotifier.ACTION_SNOOZE -> {
                LiveTripService.snooze(context, 5)
            }
        }
    }
}
