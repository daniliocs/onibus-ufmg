package br.ufmg.morabus.app.notifications

interface LiveTripNotifier {
    fun showOrUpdate(state: LiveTripState)
    fun dismiss()
    fun isPromotionSupported(): Boolean
}
