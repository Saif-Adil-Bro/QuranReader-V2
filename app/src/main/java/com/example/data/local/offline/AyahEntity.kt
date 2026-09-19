package com.example.data.local.offline

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ayah")
data class AyahEntity(
    @PrimaryKey val globalNumber: Int,
    val surahNumber: Int,
    val numberInSurah: Int,
    val juz: Int,
    val page: Int,
    val arabicText: String,
    val englishText: String,
    val bengaliText: String
)
