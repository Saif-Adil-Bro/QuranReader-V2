package com.example.data.local.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quran_words")
data class QuranWordEntity(
    @PrimaryKey val id: Int,
    val surahNumber: Int,
    val ayahNumber: Int,
    val position: Int,
    val charTypeName: String?,
    val textUthmani: String,
    val translationBengali: String?,
    val audioUrl: String?
)
