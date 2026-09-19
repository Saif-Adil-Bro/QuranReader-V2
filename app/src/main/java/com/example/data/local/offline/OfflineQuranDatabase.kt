package com.example.data.local.offline

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SurahEntity::class, AyahEntity::class], version = 1, exportSchema = false)
abstract class OfflineQuranDatabase : RoomDatabase() {
    abstract fun offlineQuranDao(): OfflineQuranDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineQuranDatabase? = null

        fun getDatabase(context: Context): OfflineQuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineQuranDatabase::class.java,
                    "offline_quran_database_v10"
                )
                .createFromAsset("databases/quran.db")
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
