package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.BookmarkFolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkFolderDao {

    @Query("SELECT * FROM bookmark_folders ORDER BY createdAt ASC")
    fun getAllFolders(): Flow<List<BookmarkFolderEntity>>

    @Query("SELECT * FROM bookmark_folders ORDER BY createdAt ASC")
    suspend fun getAllFoldersOnce(): List<BookmarkFolderEntity>

    @Query("SELECT * FROM bookmark_folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Long): BookmarkFolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: BookmarkFolderEntity): Long

    @Query("UPDATE bookmark_folders SET name = :newName WHERE id = :folderId")
    suspend fun renameFolder(folderId: Long, newName: String)

    @Query("DELETE FROM bookmark_folders WHERE id = :folderId")
    suspend fun deleteFolder(folderId: Long)

    /**
     * When a folder is deleted, its bookmarks are kept and moved back to "unfiled"
     * so that user data is never lost.
     */
    @Query("UPDATE bookmarks SET folderId = NULL WHERE folderId = :folderId")
    suspend fun detachBookmarksOfFolder(folderId: Long)

    @Query("SELECT COUNT(*) FROM bookmarks WHERE folderId = :folderId")
    suspend fun getBookmarkCountInFolder(folderId: Long): Int
}
