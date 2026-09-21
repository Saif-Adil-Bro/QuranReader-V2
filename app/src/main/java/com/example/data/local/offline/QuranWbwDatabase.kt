package com.example.data.local.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [QuranWordEntity::class], version = 1, exportSchema = false)
abstract class QuranWbwDatabase : RoomDatabase() {
    abstract fun quranWbwDao(): QuranWbwDao

    companion object {
        @Volatile
        private var INSTANCE: QuranWbwDatabase? = null

        fun getDatabase(context: Context): QuranWbwDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranWbwDatabase::class.java,
                    "quran_wbw_database_v1"
                )
                .createFromAsset("databases/quran_wbw.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
