package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created folder for organizing DUA bookmarks
 * (Manzil, morning-evening duas, general duas).
 * This is a separate system from Quran bookmark folders.
 */
@Entity(tableName = "dua_bookmark_folders")
data class DuaBookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis()
)
