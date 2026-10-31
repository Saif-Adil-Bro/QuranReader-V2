package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-created folder for organizing bookmarks (e.g. "তাফসির পড়ার লিস্ট", "হিফজ রিভিশন").
 * Bookmarks with folderId == null are shown as "unfiled" (সবগুলো / সাধারণ).
 */
@Entity(tableName = "bookmark_folders")
data class BookmarkFolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#10B981",
    val createdAt: Long = System.currentTimeMillis()
)
