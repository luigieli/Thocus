package com.wellbeing.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Main Room Database for Thocus.
 */
@Database(
    entities = [TimeWindowEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun timeWindowDao(): TimeWindowDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            val existing = instance
            if (existing != null) {
                return existing
            }

            synchronized(this) {
                val doubleChecked = instance
                if (doubleChecked != null) {
                    return doubleChecked
                }

                val created = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "thocus_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                instance = created
                return created
            }
        }
    }
}
