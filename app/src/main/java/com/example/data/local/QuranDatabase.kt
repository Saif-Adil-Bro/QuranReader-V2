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
    version = 5,
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

        /**
         * v4 -> v5: Type-scoped dua folders.
         * Each dua collection (কুরআনিক দুআ / সকাল-সন্ধ্যার দুআ / মানযিল) now has
         * its OWN folder set, so one tab's folders no longer show inside the others.
         * - Adds a NOT NULL `type` column to `dua_bookmark_folders`.
         * - Existing folders are auto-assigned to the collection that actually
         *   uses them (all bookmarks morning-evening -> MORNING_EVENING_DUA,
         *   all manzil -> MANZIL, otherwise GENERAL_DUA).
         */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `dua_bookmark_folders_v5` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`name` TEXT NOT NULL, " +
                            "`type` TEXT NOT NULL, " +
                            "`colorHex` TEXT NOT NULL, " +
                            "`createdAt` INTEGER NOT NULL)"
                )
                db.execSQL(
                    "INSERT INTO `dua_bookmark_folders_v5` (`id`, `name`, `type`, `colorHex`, `createdAt`) " +
                            "SELECT f.`id`, f.`name`, " +
                            "CASE " +
                            "WHEN NOT EXISTS (SELECT 1 FROM `dua_bookmarks` b WHERE b.`folderId` = f.`id` AND b.`type` != 'MORNING_EVENING_DUA') " +
                            "AND EXISTS (SELECT 1 FROM `dua_bookmarks` b WHERE b.`folderId` = f.`id` AND b.`type` = 'MORNING_EVENING_DUA') " +
                            "THEN 'MORNING_EVENING_DUA' " +
                            "WHEN NOT EXISTS (SELECT 1 FROM `dua_bookmarks` b WHERE b.`folderId` = f.`id` AND b.`type` != 'MANZIL') " +
                            "AND EXISTS (SELECT 1 FROM `dua_bookmarks` b WHERE b.`folderId` = f.`id` AND b.`type` = 'MANZIL') " +
                            "THEN 'MANZIL' " +
                            "ELSE 'GENERAL_DUA' END, " +
                            "f.`colorHex`, f.`createdAt` " +
                            "FROM `dua_bookmark_folders` f"
                )
                db.execSQL("DROP TABLE `dua_bookmark_folders`")
                db.execSQL("ALTER TABLE `dua_bookmark_folders_v5` RENAME TO `dua_bookmark_folders`")
            }
        }

        fun getDatabase(context: Context): QuranDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuranDatabase::class.java,
                    "quran_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
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
