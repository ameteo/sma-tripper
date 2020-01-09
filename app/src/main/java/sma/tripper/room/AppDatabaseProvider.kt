package sma.tripper.room

import android.content.Context
import androidx.room.Room

object AppDatabaseProvider {
    private var appDatabase: AppDatabase? = null

    fun getDb(context: Context): AppDatabase {
        if (appDatabase == null)
            appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "data")
                    .allowMainThreadQueries().build()

        return appDatabase!!
    }
}