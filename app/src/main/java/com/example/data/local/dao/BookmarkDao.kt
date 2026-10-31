package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE type = :type AND referenceId = :referenceId LIMIT 1")
    suspend fun getBookmark(type: String, referenceId: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE type = :type AND referenceId = :referenceId")
    suspend fun deleteBookmarkByReference(type: String, referenceId: Int)

    // --- Folder support ---
    @Query("SELECT * FROM bookmarks WHERE folderId = :folderId ORDER BY timestamp DESC")
    fun getBookmarksByFolder(folderId: Long): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE folderId IS NULL ORDER BY timestamp DESC")
    fun getUnfiledBookmarks(): Flow<List<BookmarkEntity>>

    @Query("UPDATE bookmarks SET folderId = :folderId WHERE id = :bookmarkId")
    suspend fun moveBookmarkToFolder(bookmarkId: Int, folderId: Long?)
}
