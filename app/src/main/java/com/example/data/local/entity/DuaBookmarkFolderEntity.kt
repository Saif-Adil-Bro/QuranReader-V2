package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created folder for organizing DUA bookmarks
 * (Manzil, morning-evening duas, general duas).
 * This is a separate system from Quran bookmark folders.
 *
 * @param type Which dua collection this folder belongs to:
 * "GENERAL_DUA" (কুরআনিক দুআ), "MORNING_EVENING_DUA" (সকাল-সন্ধ্যার দুআ)
 * or "MANZIL". Folders are scoped per collection so one tab's folders
 * never show inside another tab.
 */
@Entity(tableName = "dua_bookmark_folders")
data class DuaBookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String = "GENERAL_DUA",
    val colorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis()
)
