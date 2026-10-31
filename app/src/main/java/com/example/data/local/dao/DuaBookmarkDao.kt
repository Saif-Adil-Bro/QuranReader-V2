package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.DuaBookmarkEntity
import com.example.data.local.entity.DuaBookmarkFolderEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for the separate Dua bookmark & folder system
 * (Manzil + morning-evening duas + general duas).
 */
@Dao
interface DuaBookmarkDao {

    // --- Bookmarks ---
    @Query("SELECT * FROM dua_bookmarks ORDER BY timestamp DESC")
    fun getAllDuaBookmarks(): Flow<List<DuaBookmarkEntity>>

    @Query("SELECT * FROM dua_bookmarks WHERE type = :type AND duaId = :duaId LIMIT 1")
    suspend fun getDuaBookmark(type: String, duaId: Int): DuaBookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuaBookmark(bookmark: DuaBookmarkEntity)

    @Query("DELETE FROM dua_bookmarks WHERE type = :type AND duaId = :duaId")
    suspend fun deleteDuaBookmark(type: String, duaId: Int)

    @Query("UPDATE dua_bookmarks SET folderId = :folderId WHERE id = :bookmarkId")
    suspend fun moveDuaBookmarkToFolder(bookmarkId: Int, folderId: Long?)

    // --- Folders ---
    @Query("SELECT * FROM dua_bookmark_folders ORDER BY createdAt ASC")
    fun getAllDuaFolders(): Flow<List<DuaBookmarkFolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDuaFolder(folder: DuaBookmarkFolderEntity): Long

    @Query("UPDATE dua_bookmark_folders SET name = :newName WHERE id = :folderId")
    suspend fun renameDuaFolder(folderId: Long, newName: String)

    /**
     * When a folder is deleted, its duas are kept and moved back to "unfiled"
     * so that user data is never lost.
     */
    @Query("UPDATE dua_bookmarks SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachDuaBookmarksOfFolder(folderId: Long)

    @Query("DELETE FROM dua_bookmark_folders WHERE id = :folderId")
    suspend fun deleteDuaFolder(folderId: Long)
}
