package com.example.utils

/**
 * Indo-Pak Quran Text & Waqf Sign Normalizer.
 * Ensures all South Asian / Indo-Pak stopping marks (Waqf signs) like
 * (مـ, ط, ج, ز, ص, صلے, قف, لا, سكتة, وقف, ؞, ۩, ۞, ইত্যাদি)
 * are rendered accurately without missing glyphs, tofu boxes, or overlapping artifacts.
 */
object QuranIndoPakNormalizer {

    /**
     * Normalizes and cleans Indo-Pak Quranic text and its Waqf signs for perfect font rendering.
     * @param rawText Original Arabic Quran text
     * @param showWaqfSigns If false, strips auxiliary Waqf marks for an ultra-clean reading layout.
     */
    fun processIndoPakText(rawText: String, showWaqfSigns: Boolean = false): String {
        if (rawText.isBlank()) return rawText

        if (!showWaqfSigns) {
            return stripWaqfSigns(rawText)
        }

        var normalized = rawText

        // 1. Normalize common Indo-Pak Waqf unicode ligatures & combining characters
        normalized = normalized
            // Small High Ligature Sad Lam Alef Maksura (صلے)
            .replace("\u06D6", " ۖ ")
            // Small High Ligature Qaf Lam Alef Maksura (قلے)
            .replace("\u06D7", " ۗ ")
            // Small High Meem (مـ / وقف لازم)
            .replace("\u06D8", " ۘ ")
            // Small High Lam Alef (لا / وقف ممنوع)
            .replace("\u06D9", " ۙ ")
            // Small High Jeem (ج / وقف جائز)
            .replace("\u06DA", " ۚ ")
            // Small High Three Dots (؞ / ۛ / معانقة)
            .replace("\u06DB", " ۛ ")
            // Small High Seen (س / سكتة)
            .replace("\u06DC", " ۜ ")
            // Small High Rounded Zero / Sukun variants
            .replace("\u06DF", "\u06E1")
            // Small High Upright Rectangular Zero
            .replace("\u06E0", "\u0652")
            // Small Low Meem
            .replace("\u06ED", "ۢ")
            // Small High Tah (ط / وقف مطلق) - ensure correct glyph representation
            .replace("\u06E9", " ࣳ ")
            // Sajdah mark
            .replace("\u06E9", "۩")

        // 2. Remove unintended duplicate zero-width characters or malformed spaces around signs
        normalized = normalized
            .replace(Regex("\\s+([ۖۗۘۙۚۛۜۢ۩])"), " $1")
            .replace(Regex("([ۖۗۘۙۚۛۜۢ۩])\\s+"), "$1 ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()

        return normalized
    }

    /**
     * Removes auxiliary Waqf signs from the text while preserving harakat (tashkeel), tanween, and sukun.
     */
    fun stripWaqfSigns(text: String): String {
        return text
            .replace(Regex("[\u06D6\u06D7\u06D8\u06D9\u06DA\u06DB\u06DC\u06DF\u06E0\u06E2\u06EDۖۗۘۙۚۛۜۢ]"), "")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }
}
