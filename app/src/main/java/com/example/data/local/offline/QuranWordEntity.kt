package com.example.data.local.offline

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quran_words",
    indices = [
        Index(value = ["surahNumber", "ayahNumber"], name = "idx_words_surah_ayah")
    ]
)
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
