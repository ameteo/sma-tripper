package sma.tripper

import sma.tripper.data.Trip

interface TripRepository {
    suspend fun getAllTrips() : List<Trip>
    suspend fun addTrip(trip: Trip)
    suspend fun removeTrip(trip: Trip)
}