package sma.tripper.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import sma.tripper.entity.EventEntity

@Dao
interface EventDao {
    @Query("SELECT * FROM events")
    fun getAll(): List<EventEntity>

    @Insert
    fun insertAll(events: List<EventEntity>)

    @Delete
    fun delete(event: EventEntity)
}