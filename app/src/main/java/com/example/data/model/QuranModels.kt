package com.example.data.model

import com.google.gson.annotations.SerializedName
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.sp

/**
 * Represents the base response from Al-Quran API
 */
data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("status") val status: String,
    @SerializedName("data") val data: T
)

/**
 * Represents a Surah item in the list
 */
data class Surah(
    @SerializedName("number") val number: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("englishName") val englishName: String,
    @SerializedName("englishNameTranslation") val englishNameTranslation: String,
    @SerializedName("numberOfAyahs") val numberOfAyahs: Int,
    @SerializedName("revelationType") val revelationType: String
)

/**
 * Represents a detailed Surah response with its ayahs and edition info
 */
data class SurahDetail(
    @SerializedName("number") val number: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("englishName") val englishName: String,
    @SerializedName("englishNameTranslation") val englishNameTranslation: String,
    @SerializedName("revelationType") val revelationType: String,
    @SerializedName("numberOfAyahs") val numberOfAyahs: Int,
    @SerializedName("ayahs") val ayahs: List<Ayah>,
    @SerializedName("edition") val edition: Edition
)

/**
 * Represents an Ayah (verse) in a Surah
 */
data class Ayah(
    @SerializedName("number") val number: Int,
    @SerializedName("text") val text: String,
    @SerializedName("surah") val surah: Surah? = null,
    @SerializedName("numberInSurah") val numberInSurah: Int,
    @SerializedName("juz") val juz: Int,
    @SerializedName("manzil") val manzil: Int,
    @SerializedName("page") val page: Int,
    @SerializedName("ruku") val ruku: Int,
    @SerializedName("hizbQuarter") val hizbQuarter: Int,
    @SerializedName("audio") val audio: String? = null,
    @SerializedName("audioSecondary") val audioSecondary: List<String>? = null
)

/**
 * Represents the edition of the Quran text (e.g., Arabic Uthmani or Bengali translation)
 */
data class Edition(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("language") val language: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("englishName") val englishName: String,
    @SerializedName("format") val format: String,
    @SerializedName("type") val type: String
)

/**
 * A combined UI model for displaying an Ayah with its translation side-by-side
 */
data class JuzResponse(
    @SerializedName("number") val number: Int,
    @SerializedName("ayahs") val ayahs: List<Ayah>,
    @SerializedName("edition") val edition: Edition
)

data class PageResponse(
    @SerializedName("number") val number: Int,
    @SerializedName("ayahs") val ayahs: List<Ayah>,
    @SerializedName("edition") val edition: Edition
)

/**
 * A combined UI model for displaying an Ayah with its translation side-by-side
 */
data class CombinedAyah(
    val number: Int, // Overall Ayah number in the Quran
    val numberInSurah: Int, // Ayah number within the Surah
    val page: Int, // Page number of the Ayah
    val juz: Int = 0, // Juz number of the Ayah
    val surahNumber: Int = 0, // Surah number of the Ayah
    val arabicText: String,
    val bengaliText: String,
    val translations: List<QuranComTranslation> = emptyList(),
    val tafsirText: String? = null,
    val audioUrl: String? = null,
    val words: List<QuranComWord> = emptyList(),
    val textUthmaniTajweed: String? = null
)

/**
 * Models for Search
 */
data class SearchResponse(
    @SerializedName("count") val count: Int,
    @SerializedName("matches") val matches: List<SearchMatch>
)

data class SearchMatch(
    @SerializedName("number") val number: Int,
    @SerializedName("text") val text: String,
    @SerializedName("edition") val edition: Edition,
    @SerializedName("surah") val surah: Surah,
    @SerializedName("numberInSurah") val numberInSurah: Int
)

/**
 * Models for Quran.com API v4 (Word by Word)
 */
data class QuranComResponse(
    @SerializedName("verses") val verses: List<QuranComVerse>
)

data class QuranComVerse(
    @SerializedName("id") val id: Int,
    @SerializedName("verse_key") val verseKey: String,
    @SerializedName("verse_number") val verseNumber: Int,
    @SerializedName("words") val words: List<QuranComWord> = emptyList(),
    @SerializedName("text_uthmani_tajweed") val textUthmaniTajweed: String? = null,
    @SerializedName("translations") val translations: List<QuranComTranslation>? = null,
    @SerializedName("tafsirs") val tafsirs: List<QuranComTafsir>? = null
)

data class QuranComTafsir(
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("text") val text: String
)

data class QuranComWord(
    @SerializedName("id") val id: Int,
    @SerializedName("position") val position: Int,
    @SerializedName("char_type_name") val charTypeName: String,
    @SerializedName("text_uthmani") val textUthmani: String? = null,
    @SerializedName("translation") val translation: QuranComWordTranslation? = null,
    @SerializedName("transliteration") val transliteration: QuranComWordTransliteration? = null,
    @SerializedName("audio_url") val audioUrl: String? = null
)

typealias WordDto = QuranComWord

data class QuranComWordTranslation(
    @SerializedName("text") val text: String?
)

data class QuranComWordTransliteration(
    @SerializedName("text") val text: String?
)

data class QuranComTranslation(
    @SerializedName("id") val id: Int,
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("text") val text: String
)

data class QuranComTafsirResponse(
    @SerializedName("tafsirs") val tafsirs: List<QuranComTafsirItem>
)

data class QuranComTafsirItem(
    @SerializedName("id") val id: Int,
    @SerializedName("resource_id") val resourceId: Int,
    @SerializedName("verse_key") val verseKey: String,
    @SerializedName("language_id") val languageId: Int,
    @SerializedName("text") val text: String,
    @SerializedName("slug") val slug: String? = null
)

val WAQF_CHARS = charArrayOf(
    '\u06D6', // ۖ (صلے)
    '\u06D7', // ۗ (قلے)
    '\u06D8', // ۘ (مـ)
    '\u06D9', // ۙ (لا)
    '\u06DA', // ۚ (ج)
    '\u06DB', // ۛ (three dots)
    '\u06DC'  // ۜ (seen)
)

fun String.removeWaqfSigns(): String {
    var cleaned = this
    for (char in WAQF_CHARS) {
        cleaned = cleaned.replace(char.toString(), "")
    }
    return cleaned
}

fun String.formatWaqfSigns(): String {
    return this
}

fun AnnotatedString.Builder.appendStyledWaqfText(
    text: String,
    fontSize: Float,
    showWaqfSigns: Boolean = true,
    arabicFontName: String = "Me Quran"
) {
    if (!showWaqfSigns) {
        append(text.removeWaqfSigns())
        return
    }
    
    var lastIndex = 0
    for (i in text.indices) {
        val char = text[i]
        if (WAQF_CHARS.contains(char)) {
            if (i > lastIndex) {
                append(text.substring(lastIndex, i))
            }
            
            // If preceding text does not end with space, add hair space to prevent glyph collision
            if (length > 0) {
                val currentText = this.toAnnotatedString().text
                if (currentText.isNotEmpty() && currentText.last() != ' ' && currentText.last() != '\u200A') {
                    append("\u200A")
                }
            }

            // Style the waqf sign without negative baselineShift so it doesn't overlap the letters below
            if (arabicFontName.contains("Saleem", ignoreCase = true)) {
                withStyle(
                    style = SpanStyle(
                        fontFamily = com.example.ui.theme.amiriFont,
                        fontSize = (fontSize * 0.70f).sp,
                        baselineShift = BaselineShift(0.0f)
                    )
                ) {
                    append(char.toString())
                }
            } else {
                withStyle(
                    style = SpanStyle(
                        fontSize = (fontSize * 0.70f).sp,
                        baselineShift = BaselineShift(0.0f)
                    )
                ) {
                    append(char.toString())
                }
            }

            // Add hair space after waqf sign if not followed by space
            if (i + 1 < text.length && text[i + 1] != ' ' && text[i + 1] != '\u200A') {
                append("\u200A")
            }

            lastIndex = i + 1
        }
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

data class TafsirResourceResponse(
    @SerializedName("tafsirs") val tafsirs: List<TafsirResourceDto>
)

data class TafsirResourceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("language_name") val languageName: String,
    @SerializedName("translated_name") val translatedName: TranslatedName? = null
)

data class TranslatedName(
    @SerializedName("name") val name: String? = null,
    @SerializedName("language_name") val languageName: String
)
data class TranslationResourceResponse(
    @SerializedName("translations") val translations: List<TranslationResourceDto>
)

data class TranslationResourceDto(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("author_name") val authorName: String? = null,
    @SerializedName("slug") val slug: String? = null,
    @SerializedName("language_name") val languageName: String,
    @SerializedName("translated_name") val translatedName: TranslatedName? = null
)
