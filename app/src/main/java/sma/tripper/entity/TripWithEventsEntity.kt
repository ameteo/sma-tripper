package sma.tripper.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TripWithEventsEntity(
    @Embedded val trip: TripEntity,
    @Relation(
        parentColumn = "tripId",
        entityColumn = "tripOwnerId"
    )
    val events: List<EventEntity>
)