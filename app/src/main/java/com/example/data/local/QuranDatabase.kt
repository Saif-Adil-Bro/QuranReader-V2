package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.BookmarkFolderDao
import com.example.data.local.dao.DuaBookmarkDao
import com.example.data.local.dao.MemorizedPageDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.BookmarkFolderEntity
import com.example.data.local.entity.DuaBookmarkEntity
import com.example.data.local.entity.DuaBookmarkFolderEntity
import com.example.data.local.entity.MemorizedPageEntity

@Database(
    entities = [
        BookmarkEntity::class,
        BookmarkFolderEntity::class,
        MemorizedPageEntity::class,
        DuaBookmarkEntity::class,
        DuaBookmarkFolderEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class QuranDatabase : RoomDatabase() {
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun bookmarkFolderDao(): BookmarkFolderDao
    abstract fun duaBookmarkDao(): DuaBookmarkDao
    abstract fun memorizedPageDao(): MemorizedPageDao

    companion object {
        @Volatile
        private var INSTANCE: QuranDatabase? = null

        /**
         * v2 -> v3: Bookmark folders feature.
         * - Creates the new `bookmark_folders` table.
         * - Adds a nullable `folderId` column to existing `bookmarks` rows
         *   (NULL = unfiled, so existing bookmarks keep working untouched).
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bookmark_folders` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`colorHex` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL("ALTER TABLE `bookmarks` ADD COLUMN `folderId` INTEGER")
            }
        }

        /**
         * v3 -> v4: Separate Dua bookmark & folder system
         * (Manzil, morning-evening duas, general duas).
         * Purely additive — creates two new tables, touches nothing existing.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dua_bookmark_folders` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`colorHex` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dua_bookmarks` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`type` TEXT NOT NULL, " +
                            "`duaId` INTEGER NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`folderId` INTEGER, " +
                            "`timestamp` INTEGER NOT NULL)"
                )
            }
        }

        fun getDatabase(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                // Safety net only for unknown legacy schema paths (e.g. v1).
                // Known paths (2 -> 3) use the real migration above so user
                // bookmarks & memorized pages are preserved.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
