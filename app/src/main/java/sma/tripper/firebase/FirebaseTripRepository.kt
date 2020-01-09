package sma.tripper.firebase

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import sma.tripper.TripRepository
import sma.tripper.data.Event
import sma.tripper.data.Trip
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseTripRepository(val userId: String) : TripRepository {
    private val database = FirebaseDatabase.getInstance().reference.child("users").child(userId)

    override suspend fun getAllTrips(): List<Trip> = suspendCoroutine { continuation ->
        database.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                database.removeEventListener(this)
                continuation.resumeWithException(databaseError.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val trips = dataSnapshot.children
                    .map { it.getValue(FirebaseTripNode::class.java) }
                    .map { it !! }
                    .map { it.toModel() }

                database.removeEventListener(this)
                continuation.resume(trips)
            }
        })
    }

    override suspend fun addTrip(trip: Trip) {
        database.child(trip.tripId.toString()).setValue(trip.toFirebaseNode())
    }

    override suspend fun removeTrip(trip: Trip): Unit = suspendCoroutine { continuation ->
        database.addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                database.removeEventListener(this)
                continuation.resumeWithException(databaseError.toException())
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.children.map { it.key }.contains(trip.tripId.toString()))
                    database.child(trip.tripId.toString()).removeValue()

                database.removeEventListener(this)
                continuation.resume(Unit)
            }
        })
    }

    private fun FirebaseTripNode.toModel() : Trip {
        val trip = Trip(tripId, from.let { LocalDate.parse(it) }, to.let { LocalDate.parse(it) }, destination)
        trip.lat = lat
        trip.lng = lng
        trip.events = events.map { it.toModel() }.toMutableList()
        return trip
    }

    private fun Trip.toFirebaseNode() = FirebaseTripNode(tripId, from.toString(), to.toString(), destination, lat!!, lng!!, events.map { it.toFirebaseNode() })

    private fun FirebaseEventNode.toModel() = Event(eventId, name, address, thumbnailUrl, date.let { LocalDate.parse(it) }, hour, minute)

    private fun Event.toFirebaseNode() = FirebaseEventNode(eventId, name, address, thumbnailUrl, date.toString(), hour, minute)
}