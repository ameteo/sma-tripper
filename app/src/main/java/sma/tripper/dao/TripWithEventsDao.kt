package sma.tripper.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import sma.tripper.entity.TripWithEventsEntity

@Dao
interface TripWithEventsDao {
    @Transaction
    @Query("SELECT * FROM trips")
    fun getTripsWithEvents(): List<TripWithEventsEntity>
}