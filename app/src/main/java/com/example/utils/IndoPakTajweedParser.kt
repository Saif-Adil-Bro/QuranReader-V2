package com.example.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import com.example.data.model.WAQF_CHARS
import com.example.data.model.removeWaqfSigns
import com.example.ui.theme.TajweedColors

object IndoPakTajweedParser {

    private const val SUKUN_1 = '\u0652' // Standard Sukoon / Jazm
    private const val SUKUN_2 = '\u06E1' // Quranic Sukoon
    private const val SHADDAH = '\u0651' // Tashdeed
    private const val FATHATAN = '\u064B' // Do Zabar
    private const val DAMMATAN = '\u064C' // Do Pesh
    private const val KASRATAN = '\u064D' // Do Zer
    private const val FATHA = '\u064E'
    private const val DAMMA = '\u064F'
    private const val KASRA = '\u0650'
    private const val DAGGER_ALIF = '\u0670' // Khada Zabar
    private const val SUBSCRIPT_ALEF = '\u0656' // Khada Zer
    private const val INVERTED_DAMMA = '\u0657' // Ulta Pesh
    private const val MADDAH = '\u0653' // Maddah sign
    private const val SMALL_HIGH_MEEM = '\u06E2' // Iqlab sign
    private const val SMALL_LOW_MEEM = '\u06ED'

    private val QALQALAH_LETTERS = setOf('ق', 'ط', 'ب', 'ج', 'د')
    private val IKHFA_LETTERS = setOf('ت', 'ث', 'ج', 'د', 'ذ', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ف', 'ق', 'ك')
    private val IDGHAM_GHUNNAH_LETTERS = setOf('ي', 'ن', 'م', 'و')
    private val IDGHAM_NO_GHUNNAH_LETTERS = setOf('ل', 'ر')
    private val SHAMSI_LETTERS = setOf('ت', 'ث', 'د', 'ذ', 'ر', 'ز', 'س', 'ش', 'ص', 'ض', 'ط', 'ظ', 'ل', 'ن')
    private val THROAT_LETTERS = setOf('ء', 'أ', 'إ', 'ه', 'ع', 'ح', 'غ', 'خ')

    private val DIACRITICS = setOf(
        '\u064B', '\u064C', '\u064D', '\u064E', '\u064F', '\u0650', '\u0651', '\u0652',
        '\u0653', '\u0654', '\u0655', '\u0656', '\u0657', '\u0658', '\u0670', '\u06DF',
        '\u06E0', '\u06E1', '\u06E2', '\u06ED'
    )

    private fun isDiacritic(c: Char): Boolean = DIACRITICS.contains(c)

    private fun isArabicLetter(c: Char): Boolean {
        return (c in '\u0621'..'\u064A' || c in '\u0671'..'\u06D3') && !isDiacritic(c) && !WAQF_CHARS.contains(c)
    }

    /**
     * Parses an Indo-Pak / Hafezi Quran string and applies authentic Tajweed color annotations
     * directly onto the native characters without changing the font or text format.
     */
    fun parseIndoPakTajweed(
        text: String,
        defaultColor: Color,
        fontSize: Float,
        showWaqfSigns: Boolean = true,
        arabicFontName: String = "Me Quran"
    ): AnnotatedString {
        if (text.isEmpty()) return buildAnnotatedString { }

        val n = text.length
        val colorRanges = Array<Color?>(n) { null }

        var i = 0
        while (i < n) {
            val c = text[i]

            // 1. Check for Ghunnah (واجب غنہ: نّ and مّ)
            if (c == 'ن' || c == 'م') {
                var hasShaddah = false
                var endOfCluster = i + 1
                while (endOfCluster < n && isDiacritic(text[endOfCluster])) {
                    if (text[endOfCluster] == SHADDAH) {
                        hasShaddah = true
                    }
                    endOfCluster++
                }
                if (hasShaddah) {
                    val color = TajweedColors["ghunnah"] ?: Color(0xFFE53935)
                    for (k in i until endOfCluster) {
                        colorRanges[k] = color
                    }
                    i = endOfCluster
                    continue
                }
            }

            // 2. Check for Maddah (مد)
            var hasMaddah = false
            var clusterEnd = i + 1
            while (clusterEnd < n && isDiacritic(text[clusterEnd])) {
                if (text[clusterEnd] == MADDAH || text[clusterEnd] == '~') {
                    hasMaddah = true
                }
                clusterEnd++
            }
            if (hasMaddah) {
                // Determine Madd type
                var nextLetterIdx = clusterEnd
                while (nextLetterIdx < n && (text[nextLetterIdx] == ' ' || isDiacritic(text[nextLetterIdx]) || WAQF_CHARS.contains(text[nextLetterIdx]))) {
                    nextLetterIdx++
                }
                var maddColor = TajweedColors["madda_permissible"] ?: Color(0xFF1976D2)
                if (nextLetterIdx < n) {
                    val nextChar = text[nextLetterIdx]
                    // Check if next letter has Shaddah -> Madd Lazim
                    var nextHasShaddah = false
                    var scanIdx = nextLetterIdx + 1
                    while (scanIdx < n && isDiacritic(text[scanIdx])) {
                        if (text[scanIdx] == SHADDAH) nextHasShaddah = true
                        scanIdx++
                    }
                    if (nextHasShaddah) {
                        maddColor = TajweedColors["madda_necessary"] ?: Color(0xFF002171)
                    } else if (nextChar == 'ء' || nextChar == 'أ' || nextChar == 'إ' || nextChar == 'ئ' || nextChar == 'ؤ') {
                        maddColor = TajweedColors["madda_obligatory"] ?: Color(0xFF0D47A1)
                    }
                }
                for (k in i until clusterEnd) {
                    colorRanges[k] = maddColor
                }
                i = clusterEnd
                continue
            }

            // 3. Check for Qalqalah (ق ط ب ج د with Sukoon)
            if (QALQALAH_LETTERS.contains(c)) {
                var hasSukun = false
                var hasShaddah = false
                var endOfCluster = i + 1
                while (endOfCluster < n && isDiacritic(text[endOfCluster])) {
                    if (text[endOfCluster] == SUKUN_1 || text[endOfCluster] == SUKUN_2) {
                        hasSukun = true
                    }
                    if (text[endOfCluster] == SHADDAH) {
                        hasShaddah = true
                    }
                    endOfCluster++
                }
                // Check if at word boundary or pause
                var isAtEnd = false
                if (endOfCluster >= n || text[endOfCluster] == ' ' || WAQF_CHARS.contains(text[endOfCluster])) {
                    isAtEnd = true
                }

                if ((hasSukun || isAtEnd) && !hasShaddah) {
                    val color = TajweedColors["qalaqah"] ?: Color(0xFFFDD835)
                    for (k in i until endOfCluster) {
                        colorRanges[k] = color
                    }
                    i = endOfCluster
                    continue
                }
            }

            // 4. Check for Noon Sakin or Tanween (إخفاء, إدغام, إقلاب)
            val isNoon = (c == 'ن')
            var isNoonSakin = false
            var isTanween = false
            var endOfNoonOrTanween = i + 1

            if (isNoon) {
                var hasAnyVowel = false
                while (endOfNoonOrTanween < n && isDiacritic(text[endOfNoonOrTanween])) {
                    val dia = text[endOfNoonOrTanween]
                    if (dia == SUKUN_1 || dia == SUKUN_2) isNoonSakin = true
                    if (dia == FATHA || dia == DAMMA || dia == KASRA || dia == FATHATAN || dia == DAMMATAN || dia == KASRATAN || dia == SHADDAH) {
                        hasAnyVowel = true
                    }
                    endOfNoonOrTanween++
                }
                if (!hasAnyVowel) isNoonSakin = true // Bare Noon before non-vowel
            } else {
                while (endOfNoonOrTanween < n && isDiacritic(text[endOfNoonOrTanween])) {
                    val dia = text[endOfNoonOrTanween]
                    if (dia == FATHATAN || dia == DAMMATAN || dia == KASRATAN) {
                        isTanween = true
                    }
                    endOfNoonOrTanween++
                }
            }

            if (isNoonSakin || isTanween) {
                // Find next pronounced Arabic letter
                var nextIdx = endOfNoonOrTanween
                while (nextIdx < n && (text[nextIdx] == ' ' || text[nextIdx] == 'ا' || text[nextIdx] == 'ى' || text[nextIdx] == 'ٱ' || WAQF_CHARS.contains(text[nextIdx]) || text[nextIdx] == SMALL_HIGH_MEEM || text[nextIdx] == SMALL_LOW_MEEM)) {
                    if (text[nextIdx] == 'ا' || text[nextIdx] == 'ى') {
                        // If Alif has harakah or next is not space, don't skip blindly
                        if (nextIdx + 1 < n && isDiacritic(text[nextIdx + 1])) {
                            break
                        }
                    }
                    nextIdx++
                }

                if (nextIdx < n) {
                    val nextLetter = text[nextIdx]
                    if (nextLetter == 'ب') {
                        // Iqlab
                        val color = TajweedColors["iqlab"] ?: Color(0xFFFB8C00)
                        for (k in i until endOfNoonOrTanween) colorRanges[k] = color
                    } else if (IDGHAM_GHUNNAH_LETTERS.contains(nextLetter)) {
                        // Idgham with Ghunnah
                        val color = TajweedColors["idgham_ghunnah"] ?: Color(0xFF8E24AA)
                        for (k in i until endOfNoonOrTanween) colorRanges[k] = color
                    } else if (IDGHAM_NO_GHUNNAH_LETTERS.contains(nextLetter)) {
                        // Idgham without Ghunnah
                        val color = TajweedColors["idgham_wo_ghunnah"] ?: Color(0xFFBA68C8)
                        for (k in i until endOfNoonOrTanween) colorRanges[k] = color
                    } else if (IKHFA_LETTERS.contains(nextLetter)) {
                        // Ikhfa
                        val color = TajweedColors["ikhafa"] ?: Color(0xFF43A047)
                        for (k in i until endOfNoonOrTanween) colorRanges[k] = color
                    }
                }
            }

            // 5. Check for Meem Sakin (Ikhfa Shafawi & Idgham Shafawi)
            if (c == 'م') {
                var isMeemSakin = false
                var endMeem = i + 1
                var hasVowel = false
                while (endMeem < n && isDiacritic(text[endMeem])) {
                    val dia = text[endMeem]
                    if (dia == SUKUN_1 || dia == SUKUN_2) isMeemSakin = true
                    if (dia == FATHA || dia == DAMMA || dia == KASRA || dia == FATHATAN || dia == DAMMATAN || dia == KASRATAN || dia == SHADDAH) {
                        hasVowel = true
                    }
                    endMeem++
                }
                if (!hasVowel) isMeemSakin = true

                if (isMeemSakin) {
                    var nextIdx = endMeem
                    while (nextIdx < n && (text[nextIdx] == ' ' || WAQF_CHARS.contains(text[nextIdx]))) {
                        nextIdx++
                    }
                    if (nextIdx < n) {
                        val nextLetter = text[nextIdx]
                        if (nextLetter == 'ب') {
                            val color = TajweedColors["ikhafa_shafawi"] ?: Color(0xFF00ACC1)
                            for (k in i until endMeem) colorRanges[k] = color
                        } else if (nextLetter == 'م') {
                            val color = TajweedColors["idgham_shafawi"] ?: Color(0xFF3949AB)
                            for (k in i until endMeem) colorRanges[k] = color
                        }
                    }
                }
            }

            // 6. Check for Laam Shamsiyah
            if (c == 'ا' || c == 'ٱ') {
                if (i + 1 < n && text[i + 1] == 'ل') {
                    // Check if followed by Shamsi letter with Shaddah
                    var scanShamsi = i + 2
                    while (scanShamsi < n && isDiacritic(text[scanShamsi])) scanShamsi++
                    if (scanShamsi < n && SHAMSI_LETTERS.contains(text[scanShamsi])) {
                        var hasShaddah = false
                        var checkShaddah = scanShamsi + 1
                        while (checkShaddah < n && isDiacritic(text[checkShaddah])) {
                            if (text[checkShaddah] == SHADDAH) hasShaddah = true
                            checkShaddah++
                        }
                        if (hasShaddah) {
                            colorRanges[i + 1] = TajweedColors["laam_shamsiyah"] ?: Color(0xFF757575)
                        }
                    }
                }
            }

            i++
        }

        // Build AnnotatedString with styled Waqf marks and Tajweed colors
        return buildAnnotatedString {
            var lastColor: Color? = null
            var segmentStart = 0

            var idx = 0
            while (idx < n) {
                val char = text[idx]

                if (WAQF_CHARS.contains(char)) {
                    if (idx > segmentStart) {
                        val sub = text.substring(segmentStart, idx)
                        if (lastColor != null) {
                            withStyle(SpanStyle(color = lastColor)) {
                                append(sub)
                            }
                        } else {
                            append(sub)
                        }
                    }

                    if (showWaqfSigns) {
                        // Waqf styling
                        if (length > 0) {
                            val cur = this.toAnnotatedString().text
                            if (cur.isNotEmpty() && cur.last() != ' ' && cur.last() != '\u200A') {
                                append("\u200A")
                            }
                        }

                        withStyle(
                            style = SpanStyle(
                                fontFamily = com.example.ui.theme.amiriFont,
                                fontSize = (fontSize * 0.70f).sp,
                                baselineShift = BaselineShift(0.0f)
                            )
                        ) {
                            append(char.toString())
                        }

                        if (idx + 1 < n && text[idx + 1] != ' ' && text[idx + 1] != '\u200A') {
                            append("\u200A")
                        }
                    }

                    idx++
                    segmentStart = idx
                    lastColor = if (idx < n) colorRanges[idx] else null
                    continue
                }

                val currentColor = colorRanges[idx]
                if (currentColor != lastColor) {
                    if (idx > segmentStart) {
                        val sub = text.substring(segmentStart, idx)
                        if (lastColor != null) {
                            withStyle(SpanStyle(color = lastColor)) {
                                append(sub)
                            }
                        } else {
                            append(sub)
                        }
                    }
                    segmentStart = idx
                    lastColor = currentColor
                }
                idx++
            }

            if (segmentStart < n) {
                val sub = text.substring(segmentStart)
                if (lastColor != null) {
                    withStyle(SpanStyle(color = lastColor)) {
                        append(sub)
                    }
                } else {
                    append(sub)
                }
            }
        }
    }
}
