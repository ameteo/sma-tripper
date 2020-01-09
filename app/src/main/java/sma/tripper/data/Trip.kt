package sma.tripper.data

import sma.tripper.data.Event
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.random.Random

data class Trip(val tripId: Long, val from: LocalDate, val to: LocalDate, val destination: String) {
    val tripDays : HashMap<String, LocalDate> = initializeTripDays()
    var lat: String? = null
    var lng: String? = null

    private fun initializeTripDays(): HashMap<String, LocalDate> {
        val tripDays : HashMap<String, LocalDate> = HashMap()
        var currentDate = from
        while(currentDate.isBefore(to.plusDays(1))) {
            tripDays[currentDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))] = LocalDate.from(currentDate)
            currentDate = currentDate.plusDays(1)
        }
        return tripDays
    }

    var events : MutableList<Event> = ArrayList()

    fun addEvent(event: Event) {
        events.add(event)
    }

    fun removeEvent(event: Event) {
        events.remove(event)
    }
}