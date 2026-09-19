package com.example.data.api

import com.example.data.model.ApiResponse
import com.example.data.model.Surah
import com.example.data.model.SurahDetail
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * API definitions for Al-Quran Cloud
 */
interface QuranApi {
    /**
     * Fetches the complete list of 114 Surahs
     */
    @GET("surah")
    suspend fun getSurahs(): ApiResponse<List<Surah>>

    /**
     * Fetches a specific Surah with multiple editions.
     * We request "quran-uthmani" (Arabic) and "bn.bengali" (Bengali translation).
     */
    @GET("surah/{number}/editions/quran-uthmani,bn.bengali,ar.alafasy")
    suspend fun getSurahWithTranslation(
        @Path("number") surahNumber: Int
    ): ApiResponse<List<SurahDetail>>

    /**
     * Fetches a specific Surah with custom editions list.
     */
    @GET("surah/{number}/editions/{editions}")
    suspend fun getSurahWithEditions(
        @Path("number") surahNumber: Int,
        @Path("editions") editions: String
    ): ApiResponse<List<SurahDetail>>

    /**
     * Fetches a specific page of the Quran
     */
    @GET("page/{pageNumber}/quran-uthmani")
    suspend fun getPageArabic(@Path("pageNumber") pageNumber: Int): ApiResponse<com.example.data.model.PageResponse>

    @GET("page/{pageNumber}/{edition}")
    suspend fun getPageEdition(@Path("pageNumber") pageNumber: Int, @Path("edition") edition: String): ApiResponse<com.example.data.model.PageResponse>

    /**
     * Fetches a specific Juz of the Quran (Arabic)
     */
    @GET("juz/{juzNumber}/quran-uthmani")
    suspend fun getJuzArabic(
        @Path("juzNumber") juzNumber: Int
    ): ApiResponse<com.example.data.model.JuzResponse>

    /**
     * Fetches a specific Juz of the Quran (Bengali)
     */
    @GET("juz/{juzNumber}/bn.bengali")
    suspend fun getJuzBengali(
        @Path("juzNumber") juzNumber: Int
    ): ApiResponse<com.example.data.model.JuzResponse>

    @GET("juz/{juzNumber}/{edition}")
    suspend fun getJuzEdition(
        @Path("juzNumber") juzNumber: Int,
        @Path("edition") edition: String
    ): ApiResponse<com.example.data.model.JuzResponse>

    /**
     * Search the Quran by keyword. 
     * E.g. search/keyword/all/bn.bengali
     */
    @GET("search/{keyword}/all/bn.bengali")
    suspend fun searchQuran(
        @Path("keyword") keyword: String
    ): ApiResponse<com.example.data.model.SearchResponse>

    /**
     * Search the Quran by keyword in a specific edition.
     */
    @GET("search/{keyword}/all/{edition}")
    suspend fun searchQuranWithEdition(
        @Path("keyword") keyword: String,
        @Path("edition") edition: String
    ): ApiResponse<com.example.data.model.SearchResponse>
}
