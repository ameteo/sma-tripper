package sma.tripper

import java.time.LocalDate

data class Trip(val from : LocalDate, val to : LocalDate, val destination : String){
    val events : ArrayList<Event> = ArrayList()

    fun addEvent(event: Event) {
        events.add(event)
    }

    fun removeEvent(event: Event) {
        events.remove(event)
    }
}