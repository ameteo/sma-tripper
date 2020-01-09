package sma.tripper.room

import sma.tripper.TripRepository
import sma.tripper.data.Event
import sma.tripper.data.Trip
import sma.tripper.entity.EventEntity
import sma.tripper.entity.TripEntity
import sma.tripper.entity.TripWithEventsEntity

class RoomTripRepository(private val appDatabase: AppDatabase) : TripRepository {
    override suspend fun getAllTrips(): List<Trip> {
        return appDatabase.tripWithEventsDao().getTripsWithEvents().map { it.toModel() }
    }

    override suspend fun addTrip(trip: Trip) {
        appDatabase.tripDao().insertAll(listOf(trip.toEntity()))
        appDatabase.eventDao().insertAll(trip.events.map { event ->
            EventEntity(event.eventId, trip.tripId, event.name, event.address, event.thumbnailUrl, event.date, event.hour, event.minute)
        })
    }

    override suspend fun removeTrip(trip: Trip) {
        appDatabase.tripDao().delete(trip.toEntity())
    }

    private fun EventEntity.toModel() = Event(eventId, name, address, thumbnailUrl, date, hour, minute)
    private fun Trip.toEntity() = TripEntity(tripId, from, to, destination, lat, lng)
    private fun TripWithEventsEntity.toModel() : Trip {
        val tripWithEvents = Trip(trip.tripId, trip.from, trip.to, trip.destination)
        tripWithEvents.lat = trip.lat
        tripWithEvents.lng = trip.lng
        tripWithEvents.events = events.toMutableList().map { it.toModel() }.toMutableList()
        return tripWithEvents
    }


}