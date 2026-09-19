package com.example.utils

import com.example.data.DuaItem
import com.example.data.SubjectwiseVerse
import com.example.data.model.CombinedAyah
import com.example.data.model.ShortPost
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Quick Create Architecture for Islamic Photo Card Maker
 * 
 * Provides unified payload and conversion utilities so any feature in Quran Reader
 * (Quran Ayah, Hadith, Dua, Subjectwise Verse, Daily Post) can easily launch
 * the Photo Card Maker with pre-filled content and automatically selected templates.
 */
object PhotoCardBridge {

    enum class CardSource {
        QURAN_AYAH,
        HADITH,
        DUA,
        SUBJECTWISE_QURAN,
        NOTIFICATION,
        MANUAL
    }

    data class QuickCardPayload(
        val id: String = java.util.UUID.randomUUID().toString(),
        val source: CardSource = CardSource.MANUAL,
        val category: String = "কুরআনের আয়াত",
        val title: String = "",
        val arabicText: String? = null,
        val translationText: String = "",
        val transliterationText: String? = null,
        val reference: String = "",
        val author: String = "কুরআন রিডার",
        val suggestedTemplateCategory: PostShareUtil.TemplateCategory = PostShareUtil.TemplateCategory.ALL,
        val suggestedTemplateId: String? = null,
        val defaultFontName: String? = null,
        val defaultFontSize: Float? = null,
        val defaultLineSpacing: Float? = null,
        val defaultTextAlign: String? = null
    ) {
        /**
         * Converts payload into a ShortPost for the existing editor engine.
         */
        fun toShortPost(): ShortPost {
            val combinedBody = buildString {
                if (!arabicText.isNullOrBlank()) {
                    append(arabicText.trim())
                    if (translationText.isNotBlank()) {
                        append("\n\n")
                    }
                }
                if (translationText.isNotBlank()) {
                    append(translationText.trim())
                }
            }

            return ShortPost(
                id = id,
                text = combinedBody,
                reference = reference,
                category = category,
                author = author,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    // Shared global state flow for cross-screen quick create trigger
    private val _activePayload = MutableStateFlow<QuickCardPayload?>(null)
    val activePayload: StateFlow<QuickCardPayload?> = _activePayload.asStateFlow()

    fun launchQuickCard(payload: QuickCardPayload) {
        _activePayload.value = payload
    }

    fun clearActivePayload() {
        _activePayload.value = null
    }

    /**
     * Converts a Quran Ayah into a QuickCardPayload with Quran template pre-selected.
     */
    fun fromAyah(ayah: CombinedAyah, surahName: String): QuickCardPayload {
        val bengaliAyahNum = DateUtil.toBengaliNumerals(ayah.numberInSurah)
        val reference = "সূরা $surahName • আয়াত $bengaliAyahNum"
        
        return QuickCardPayload(
            source = CardSource.QURAN_AYAH,
            category = "কুরআনের আয়াত",
            title = reference,
            arabicText = ayah.arabicText.ifBlank { null },
            translationText = ayah.bengaliText,
            reference = reference,
            author = "আল-কুরআনুল কারীম",
            suggestedTemplateCategory = PostShareUtil.TemplateCategory.QURAN,
            suggestedTemplateId = "royal_night",
            defaultFontName = "SolaimanLipi",
            defaultFontSize = 42f,
            defaultLineSpacing = 1.25f,
            defaultTextAlign = "CENTER"
        )
    }

    /**
     * Converts a Subjectwise Verse into a QuickCardPayload.
     */
    fun fromSubjectwiseVerse(verse: SubjectwiseVerse, categoryName: String): QuickCardPayload {
        val reference = "${verse.surahName} : ${verse.verseNo}"
        return QuickCardPayload(
            source = CardSource.SUBJECTWISE_QURAN,
            category = "বিষয়ভিত্তিক কুরআন • $categoryName",
            title = "$categoryName ($reference)",
            arabicText = verse.arabicText.ifBlank { null },
            translationText = verse.banglaTranslation,
            reference = reference,
            author = "আল-কুরআনুল কারীম",
            suggestedTemplateCategory = PostShareUtil.TemplateCategory.QURAN,
            suggestedTemplateId = "emerald",
            defaultFontName = "SolaimanLipi"
        )
    }

    /**
     * Converts a DuaItem into a QuickCardPayload with Dua template pre-selected.
     */
    fun fromDua(dua: DuaItem): QuickCardPayload {
        val firstSegment = dua.segments.firstOrNull()
        val arabic = firstSegment?.arabic?.takeIf { it != "null" && it.isNotBlank() }
        val translation = firstSegment?.translation?.takeIf { it != "null" && it.isNotBlank() } ?: ""
        val ref = firstSegment?.reference?.takeIf { it != "null" && it.isNotBlank() } ?: "দৈনিক দোয়া"

        return QuickCardPayload(
            source = CardSource.DUA,
            category = "দোয়া ও মোনাজাত",
            title = dua.title,
            arabicText = arabic,
            translationText = translation,
            transliterationText = firstSegment?.transliteration?.takeIf { it != "null" && it.isNotBlank() },
            reference = ref,
            author = "মাসনুন দোয়া",
            suggestedTemplateCategory = PostShareUtil.TemplateCategory.DUA,
            suggestedTemplateId = "emerald",
            defaultFontName = "SolaimanLipi"
        )
    }
}

// Convenient Kotlin extension functions
fun CombinedAyah.toQuickCardPayload(surahName: String): PhotoCardBridge.QuickCardPayload {
    return PhotoCardBridge.fromAyah(this, surahName)
}

fun CombinedAyah.toShortPost(surahName: String): ShortPost {
    return PhotoCardBridge.fromAyah(this, surahName).toShortPost()
}

fun SubjectwiseVerse.toQuickCardPayload(categoryName: String): PhotoCardBridge.QuickCardPayload {
    return PhotoCardBridge.fromSubjectwiseVerse(this, categoryName)
}

fun SubjectwiseVerse.toShortPost(categoryName: String): ShortPost {
    return PhotoCardBridge.fromSubjectwiseVerse(this, categoryName).toShortPost()
}

fun DuaItem.toQuickCardPayload(): PhotoCardBridge.QuickCardPayload {
    return PhotoCardBridge.fromDua(this)
}

fun DuaItem.toShortPost(): ShortPost {
    return PhotoCardBridge.fromDua(this).toShortPost()
}
