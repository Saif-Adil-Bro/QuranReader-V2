package com.example.data.umrah

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UmrahSessionEntity::class,
        UmrahChecklistEntity::class,
        UmrahRoundLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UmrahDatabase : RoomDatabase() {
    abstract fun umrahDao(): UmrahDao

    companion object {
        @Volatile
        private var INSTANCE: UmrahDatabase? = null

        fun getDatabase(context: Context): UmrahDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UmrahDatabase::class.java,
                    "umrah_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
