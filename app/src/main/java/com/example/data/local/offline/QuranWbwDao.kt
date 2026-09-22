package com.example.data.local.offline

import androidx.room.Dao
import androidx.room.Query

@Dao
interface QuranWbwDao {
    @Query("SELECT * FROM quran_words WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC, position ASC")
    suspend fun getWordsBySurah(surahNumber: Int): List<QuranWordEntity>

    @Query("SELECT * FROM quran_words WHERE surahNumber = :surahNumber AND ayahNumber BETWEEN :startAyah AND :endAyah ORDER BY ayahNumber ASC, position ASC")
    suspend fun getWordsBySurahRange(surahNumber: Int, startAyah: Int, endAyah: Int): List<QuranWordEntity>

    @Query("SELECT * FROM quran_words WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber ORDER BY position ASC")
    suspend fun getWordsBySurahAndAyah(surahNumber: Int, ayahNumber: Int): List<QuranWordEntity>

    @Query("SELECT * FROM quran_words WHERE translationBengali IS NOT NULL AND translationBengali != '' ORDER BY RANDOM() LIMIT :limit")
    suspend fun getRandomWordsWithMeaning(limit: Int): List<QuranWordEntity>

    @Query("SELECT COUNT(*) FROM quran_words")
    suspend fun getTotalWordsCount(): Int

    @Query("SELECT * FROM quran_words WHERE textUthmani LIKE '%' || :query || '%' ORDER BY surahNumber ASC, ayahNumber ASC, position ASC LIMIT :limit")
    suspend fun searchWordsByArabic(query: String, limit: Int = 150): List<QuranWordEntity>

    @Query("SELECT * FROM quran_words WHERE translationBengali LIKE '%' || :query || '%' ORDER BY surahNumber ASC, ayahNumber ASC, position ASC LIMIT :limit")
    suspend fun searchWordsByBengali(query: String, limit: Int = 150): List<QuranWordEntity>
}
