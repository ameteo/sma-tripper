package sma.tripper

import sma.tripper.data.Trip

interface TripRepository {
    fun getAllTrips() : List<Trip>
    fun addTrip(trip: Trip)
    fun removeTrip(trip: Trip)
}