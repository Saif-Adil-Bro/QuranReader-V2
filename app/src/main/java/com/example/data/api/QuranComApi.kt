package com.example.data.api

import com.example.data.model.QuranComResponse
import com.example.data.model.QuranComTafsirResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface QuranComApi {
    @GET("verses/by_chapter/{chapter_number}")
    suspend fun getSurahVerses(
        @Path("chapter_number") chapterNumber: Int,
        @Query("words") words: Boolean = true,
        @Query("word_fields") wordFields: String? = "text_uthmani,translation,transliteration,position,location,audio_url",
        @Query("translations") translations: String = "161",
        @Query("language") language: String? = "bn",
        @Query("word_translation_language") wordTranslationLanguage: String? = "bn",
        @Query("fields") fields: String? = "text_uthmani_tajweed",
        @Query("per_page") perPage: Int = 1000,
        @Query("tafsirs") tafsirs: String? = null
    ): QuranComResponse

    @GET("verses/by_juz/{juz_number}")
    suspend fun getJuzVerses(
        @Path("juz_number") juzNumber: Int,
        @Query("words") words: Boolean = true,
        @Query("word_fields") wordFields: String? = "text_uthmani,translation,transliteration,position,location,audio_url",
        @Query("translations") translations: String = "161",
        @Query("language") language: String? = "bn",
        @Query("word_translation_language") wordTranslationLanguage: String? = "bn",
        @Query("fields") fields: String? = "text_uthmani_tajweed",
        @Query("per_page") perPage: Int = 1000,
        @Query("tafsirs") tafsirs: String? = null
    ): QuranComResponse

    @GET("verses/by_page/{page_number}")
    suspend fun getPageVerses(
        @Path("page_number") pageNumber: Int,
        @Query("words") words: Boolean = true,
        @Query("word_fields") wordFields: String? = "text_uthmani,translation,transliteration,position,location,audio_url",
        @Query("translations") translations: String = "161",
        @Query("language") language: String? = "bn",
        @Query("word_translation_language") wordTranslationLanguage: String? = "bn",
        @Query("fields") fields: String? = "text_uthmani_tajweed",
        @Query("per_page") perPage: Int = 1000,
        @Query("tafsirs") tafsirs: String? = null
    ): QuranComResponse

    @GET("tafsirs/{tafsir_id}/by_chapter/{chapter_number}")
    suspend fun getSurahTafsirs(
        @Path("chapter_number") chapterNumber: Int,
        @Path("tafsir_id") tafsirId: String = "164",
        @Query("per_page") perPage: Int = 1000
    ): QuranComTafsirResponse

    @GET("tafsirs/{tafsir_id}/by_juz/{juz_number}")
    suspend fun getJuzTafsirs(
        @Path("juz_number") juzNumber: Int,
        @Path("tafsir_id") tafsirId: String = "164",
        @Query("per_page") perPage: Int = 1000
    ): QuranComTafsirResponse

    @GET("tafsirs/{tafsir_id}/by_page/{page_number}")
    suspend fun getPageTafsirs(
        @Path("page_number") pageNumber: Int,
        @Path("tafsir_id") tafsirId: String = "164",
        @Query("per_page") perPage: Int = 1000
    ): QuranComTafsirResponse

    @GET("resources/tafsirs")
    suspend fun getAvailableTafsirs(
        @Query("language") language: String = "bn"
    ): com.example.data.model.TafsirResourceResponse

    @GET("resources/translations")
    suspend fun getAvailableTranslations(
        @Query("language") language: String = "bn"
    ): com.example.data.model.TranslationResourceResponse
}
