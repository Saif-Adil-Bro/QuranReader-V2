package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memorized_pages")
data class MemorizedPageEntity(
    @PrimaryKey
    val pageNumber: Int,
    val isMemorized: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
