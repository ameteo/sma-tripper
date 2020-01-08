package sma.tripper

import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Trip(val from : LocalDate, val to : LocalDate, val destination : String) {
    val tripDays : HashMap<String, LocalDate> = initializeTripDays()

    private fun initializeTripDays(): HashMap<String, LocalDate> {
        val tripDays : HashMap<String, LocalDate> = HashMap()
        var currentDate = from
        while(currentDate.isBefore(to.plusDays(1))) {
            tripDays[currentDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))] = LocalDate.from(currentDate)
            currentDate = currentDate.plusDays(1)
        }
        return tripDays
    }

    val events : ArrayList<Event> = ArrayList()

    fun addEvent(event: Event) {
        events.add(event)
    }

    fun removeEvent(event: Event) {
        events.remove(event)
    }
}