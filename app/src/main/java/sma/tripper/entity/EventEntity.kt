package sma.tripper.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import kotlin.random.Random

@Entity(tableName = "events")
data class EventEntity (
    @PrimaryKey val eventId: Long,
    val tripOwnerId: Long,
    val name: String,
    val address: String,
    val thumbnailUrl: String?,
    val date: LocalDate?,
    val hour: Int?,
    val minute: Int?
)