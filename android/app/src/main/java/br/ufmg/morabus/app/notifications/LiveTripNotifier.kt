package br.ufmg.morabus.app.notifications

import android.app.Notification

interface LiveTripNotifier {
    fun buildNotification(state: LiveTripState): Notification
    fun showOrUpdate(state: LiveTripState)
    fun dismiss()
    fun isPromotionSupported(): Boolean
}
