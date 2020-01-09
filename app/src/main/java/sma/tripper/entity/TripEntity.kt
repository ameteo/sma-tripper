package sma.tripper.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import kotlin.random.Random

@Entity(tableName = "trips")
data class TripEntity (
    @PrimaryKey val tripId: Long,
    val from: LocalDate,
    val to: LocalDate,
    val destination: String,
    val lat: String?,
    val lng: String?
)