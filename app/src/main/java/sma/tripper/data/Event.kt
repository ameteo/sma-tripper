package sma.tripper.data

import java.time.LocalDate

data class Event @JvmOverloads constructor(
    val eventId: Long,
    val name: String,
    val address: String,
    val thumbnailUrl: String? = null,
    var date: LocalDate? = null,
    var hour: Int? = null,
    var minute: Int? = null
)