package sma.tripper.firebase

data class FirebaseEventNode(
    val eventId: Long,
    val name: String,
    val address: String,
    val thumbnailUrl: String? = null,
    var date: String? = null,
    var hour: Int? = null,
    var minute: Int? = null
) {
    constructor(): this(0L, "", "", null, null, null, null)
}