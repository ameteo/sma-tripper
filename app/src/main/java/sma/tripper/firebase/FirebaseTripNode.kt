package sma.tripper.firebase

import sma.tripper.data.Event
import java.time.LocalDate

data class FirebaseTripNode(
    val tripId: Long,
    val from: String,
    val to: String,
    val destination: String,
    val lat: String,
    val lng: String,
    val events: List<FirebaseEventNode>
) {
    constructor(): this(0L, "", "", "", "", "", listOf())
}