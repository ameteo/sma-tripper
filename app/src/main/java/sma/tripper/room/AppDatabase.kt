package sma.tripper.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import sma.tripper.dao.EventDao
import sma.tripper.dao.TripDao
import sma.tripper.dao.TripWithEventsDao
import sma.tripper.entity.EventEntity
import sma.tripper.entity.TripEntity
import sma.tripper.entity.TripWithEventsEntity

@Database(entities = [TripEntity::class, EventEntity::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun eventDao(): EventDao
    abstract fun tripWithEventsDao(): TripWithEventsDao
}