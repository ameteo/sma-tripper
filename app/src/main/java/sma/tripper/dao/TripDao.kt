package sma.tripper.dao

import androidx.room.*
import sma.tripper.entity.TripEntity

@Dao
interface TripDao {
    @Query("SELECT * FROM trips")
    fun getAll(): List<TripEntity>

    @Insert
    fun insertAll(trips: List<TripEntity>)

    @Delete
    fun delete(trip: TripEntity)
}