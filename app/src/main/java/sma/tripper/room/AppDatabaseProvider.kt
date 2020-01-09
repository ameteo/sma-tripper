package sma.tripper.room

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseProvider {
    private var appDatabase: AppDatabase? = null

    private val MIGRATION_1_2 = object: Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE trips ADD COLUMN lat TEXT")
            database.execSQL("ALTER TABLE trips ADD COLUMN lng TEXT")
        }
    }

    fun getDb(context: Context): AppDatabase {
        if (appDatabase == null)
            appDatabase = Room.databaseBuilder(context, AppDatabase::class.java, "data")
                    .allowMainThreadQueries()
                    .addMigrations(MIGRATION_1_2)
                    .build()

        return appDatabase!!
    }
}