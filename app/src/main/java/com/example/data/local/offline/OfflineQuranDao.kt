package com.example.data.local.offline

import androidx.room.Dao
import androidx.room.Query

@Dao
interface OfflineQuranDao {
    @Query("SELECT * FROM surah")
    suspend fun getAllSurahs(): List<SurahEntity>

    @Query("SELECT * FROM surah WHERE number = :number")
    suspend fun getSurahByNumber(number: Int): SurahEntity?

    @Query("SELECT * FROM ayah WHERE surahNumber = :surahNumber ORDER BY numberInSurah ASC")
    suspend fun getAyahsBySurah(surahNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayah WHERE juz = :juzNumber ORDER BY globalNumber ASC")
    suspend fun getAyahsByJuz(juzNumber: Int): List<AyahEntity>
    
    @Query("SELECT * FROM ayah WHERE page = :pageNumber ORDER BY globalNumber ASC")
    suspend fun getAyahsByPage(pageNumber: Int): List<AyahEntity>

    @Query("SELECT * FROM ayah WHERE globalNumber = :globalNumber")
    suspend fun getAyahByGlobalNumber(globalNumber: Int): AyahEntity?

    @Query("SELECT * FROM ayah WHERE surahNumber = :surahNumber AND numberInSurah = :numberInSurah")
    suspend fun getAyahBySurahAndNumber(surahNumber: Int, numberInSurah: Int): AyahEntity?

    @Query("SELECT * FROM ayah WHERE surahNumber = :surahNumber AND numberInSurah BETWEEN :startVerse AND :endVerse ORDER BY numberInSurah ASC")
    suspend fun getAyahsBySurahRange(surahNumber: Int, startVerse: Int, endVerse: Int): List<AyahEntity>
}
