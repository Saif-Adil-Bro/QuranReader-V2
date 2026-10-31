package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A bookmarked dua / manzil item.
 * Separate from Quran (ayah/page) bookmarks so the two systems stay independent.
 *
 * @param type  "MANZIL", "MORNING_EVENING_DUA" or "GENERAL_DUA"
 * @param duaId DuaItem.id from DuaData (for MANZIL this is 0 — there is only one manzil)
 */
@Entity(tableName = "dua_bookmarks")
data class DuaBookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,
    val duaId: Int,
    val title: String,
    val folderId: Long? = null, // DuaBookmarkFolderEntity.id, null = unfiled
    val timestamp: Long = System.currentTimeMillis()
)
