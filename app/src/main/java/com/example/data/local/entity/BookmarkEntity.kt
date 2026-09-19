package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String, // "SURAH", "PAGE", "MUSHAF_PAGE", "JUZ", "AYAH"
    val referenceId: Int, // Surah number, Page number, or Juz number
    val name: String, // e.g. "Surah Al-Fatihah" or "Page 1"
    val mushafId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
