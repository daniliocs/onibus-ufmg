package br.ufmg.morabus.app.notifications

enum class TripPhase(val stepIndex: Int) {
    DEPARTURE(1),      // "Saia em X min" (caminhada para o ponto)
    EN_ROUTE(2),       // "Ônibus a X min do seu ponto"
    APPROACHING(3),    // "Ônibus chegando"
    ON_BOARD(4),       // "A bordo — próxima parada: X"
    ALIGHT_NEXT(5),    // "Desça na próxima: <ponto>"
    COMPLETED(6)       // Finalizada / Auto-dismiss
}

data class LiveTripState(
    val phase: TripPhase,
    val targetStop: String,
    val currentLocationName: String,
    val etaMinutes: Int,
    val targetTimestampMs: Long,
    val progressPercent: Int,
    val delayMinutes: Int = 0,
    val nextStopName: String = "",
    val currentSegment: Int = 1,
    val totalSegments: Int = 12
)
