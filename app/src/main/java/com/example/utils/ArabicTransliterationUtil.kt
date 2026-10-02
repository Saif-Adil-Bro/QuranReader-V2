package com.example.utils

import java.util.concurrent.ConcurrentHashMap

/**
 * High-accuracy Arabic to Bengali transliteration engine for Quranic words.
 * Handles Uthmani diacritics, Shaddah, Sukun, Tanween, Dagger Alif, Hamza, and Waqf marks.
 */
object ArabicTransliterationUtil {

    private val cache = ConcurrentHashMap<String, String>()

    // Direct high-frequency Quranic words dictionary for natural Islamic pronunciation
    private val commonWordsMap = mapOf(
        "كَانَ" to "কানা",
        "كَانُوا" to "কানু",
        "عَلَيْكُمْ" to "‘আলাইকুম",
        "عَلَيْهِمْ" to "‘আলাইহিম",
        "عَلَيْهِ" to "‘আলাইহি",
        "عَلَيْهَا" to "‘আলাইহা",
        "عَلَى" to "‘আলা",
        "عَلَىٰ" to "‘আলা",
        "رَقِيبًا" to "রক্বীবা",
        "رَقِيبًۭا" to "রক্বীবা",
        "يَـٰٓأَيُّهَا" to "ইয়া আইয়্যুহা",
        "يَـٰٓأَيُّهَا ٱلَّذِينَ" to "ইয়া আইয়্যুহাল্লাযিনা",
        "ٱلنَّاسُ" to "আন্না-স",
        "ٱلنَّاسِ" to "আন্না-সি",
        "ٱلنَّاسَ" to "আন্না-সা",
        "ٱتَّقُوا۟" to "ইত্তাকূ",
        "ٱتَّقُوا" to "ইত্তাকূ",
        "رَبَّكُمُ" to "রব্বাকুমু",
        "رَبَّكُمْ" to "রব্বাকুম",
        "رَبَّنَا" to "রব্বানা",
        "رَبِّ" to "রব্বি",
        "رَبُّكَ" to "রব্বুকা",
        "ٱلَّذِى" to "আল্লাযী",
        "ٱلَّذِينَ" to "আল্লাযিনা",
        "ٱلَّتِى" to "আল্লাতী",
        "خَلَقَكُم" to "খলাক্বাকুম",
        "خَلَقَكُمْ" to "খলাক্বাকুম",
        "خَلَقَ" to "খলাক্বা",
        "مِّن" to "মিন",
        "مِنْ" to "মিন",
        "مِنْهَا" to "মিনহা",
        "مِنْهُمَا" to "মিনহুমা",
        "مِنْهُمْ" to "মিনহুম",
        "مِنْهُ" to "মিনহু",
        "نَّفْسٍۢ" to "নাফসিন",
        "نَفْسٍ" to "নাফসিন",
        "وَٰحِدَةٍۢ" to "ওয়াহিদা",
        "وَاحِدَةٍ" to "ওয়াহিদা",
        "وَخَلَقَ" to "ওয়া খলাক্বা",
        "زَوْجَهَا" to "যাওজাহা",
        "وَبَثَّ" to "ওয়া বাচ্ছা",
        "رِجَالًۭا" to "রিজালা",
        "رِجَالًا" to "রিজালা",
        "كَثِيرًۭا" to "কাছি-রা",
        "كَثِيرًا" to "কাছি-রা",
        "وَنِسَآءًۭ" to "ওয়া নিসা-আ",
        "وَنِسَاءً" to "ওয়া নিসা-আ",
        "وَٱتَّقُوا۟" to "ওয়াত্তাকূ",
        "وَٱتَّقُوا" to "ওয়াত্তাকূ",
        "ٱللَّهَ" to "আল্লা-হ",
        "ٱللَّهُ" to "আল্লা-হু",
        "ٱللَّهِ" to "আল্লা-হি",
        "اللَّهَ" to "আল্লা-হ",
        "اللَّهُ" to "আল্লা-হু",
        "اللَّهِ" to "আল্লা-হি",
        "لِلَّهِ" to "লিল্লাহি",
        "تَسَآءَلُونَ" to "তাসা-আলূন",
        "بِهِۦ" to "বিহি",
        "بِهِ" to "বিহি",
        "وَٱلْأَرْحَامَ" to "ওয়াল আরহাম",
        "إِنَّ" to "ইন্না",
        "أَنَّ" to "আন্না",
        "إِنَّمَا" to "ইন্নামা",
        "إِنَّهُ" to "ইন্নাহু",
        "إِنَّهَا" to "ইন্নাহা",
        "إِنَّهُمْ" to "ইন্নাহুম",
        "بِسْمِ" to "বিসমি",
        "ٱلرَّحْمَـٰنِ" to "আর-রহমানি",
        "ٱلرَّحِيمِ" to "আর-রহিমি",
        "ٱلْحَمْدُ" to "আলহামদু",
        "ٱلْعَـٰلَمِينَ" to "আল-‘আলামিন",
        "مَـٰلِكِ" to "মালিকি",
        "يَوْمِ" to "ইয়াওমি",
        "ٱلدِّينِ" to "আদ-দীনি",
        "إِيَّاكَ" to "ইয়্যাকা",
        "نَعْبُدُ" to "না‘বুদু",
        "وَإِيَّاكَ" to "ওয়া ইয়্যাকা",
        "نَسْتَعِينُ" to "নাসতা‘ঈনু",
        "ٱهْدِنَا" to "ইহদিনা",
        "ٱلصِّرَٰطَ" to "আস-সিরাতা",
        "ٱلْمُسْتَقِيمَ" to "আল-মুসতাক্বিম",
        "صِرَٰطَ" to "সিরাতা",
        "غَيْرِ" to "গাইরি",
        "ٱلْمَغْضُوبِ" to "আল-মাগদূবি",
        "وَلَا" to "ওয়ালা",
        "ٱلضَّآلِّينَ" to "আদ-দ্বোয়াল্লীন",
        "قُلْ" to "ক্বুল",
        "هُوَ" to "হুওয়া",
        "أَحَدٌ" to "আহাদ",
        "أَحَدٌۭ" to "আহাদ",
        "ٱلصَّمَدُ" to "আস-সামাদ",
        "لَمْ" to "লাম",
        "يَلِدْ" to "ইয়ালিদ",
        "وَلَمْ" to "ওয়ালাম",
        "يُولَدْ" to "ইউলাদ",
        "كُفُوًا" to "কুফুওয়ান",
        "كُفُوًۭا" to "কুফুওয়ান"
    )

    /**
     * Converts an Arabic Quranic word/text into accurate Bengali pronunciation.
     */
    fun transliterateToArabicBengali(arabicText: String?): String {
        if (arabicText.isNullOrBlank()) return ""
        val trimmed = cleanWaqfSymbols(arabicText.trim())
        if (trimmed.isEmpty()) return ""

        // Check cache
        cache[trimmed]?.let { return it }

        // Check exact match in common Quran words map
        commonWordsMap[trimmed]?.let {
            cache[trimmed] = it
            return it
        }

        // Try stripped version (remove subtle Quranic marks like small meem, dagger alif etc.)
        val simplified = normalizeArabicForLookup(trimmed)
        commonWordsMap[simplified]?.let {
            cache[trimmed] = it
            return it
        }

        // Algorithmic phonetic transliteration
        val result = phoneticTransliterate(trimmed)
        cache[trimmed] = result
        return result
    }

    private fun cleanWaqfSymbols(text: String): String {
        return text.replace(Regex("[ۖۗۚۛۜۘۙ۩۞\u06D6-\u06ED\u08D4-\u08FF0-9٠-٩()]"), "").trim()
    }

    private fun normalizeArabicForLookup(text: String): String {
        return text
            .replace("ـٰ", "ا")
            .replace("ٰ", "ا")
            .replace("ٱ", "ا")
            .replace("ۦ", "ي")
            .replace("ۥ", "و")
    }

    /**
     * Phonetic parser for general Arabic words
     */
    private fun phoneticTransliterate(arabic: String): String {
        val sb = StringBuilder()
        val len = arabic.length
        var i = 0

        // Consonant mappings
        val consonantMap = mapOf(
            'ب' to "ব",
            'ت' to "ত",
            'ة' to "ত",
            'ۃ' to "ত",
            'ث' to "ছ",
            'ج' to "জ",
            'ح' to "হ",
            'خ' to "খ",
            'د' to "দ",
            'ذ' to "য",
            'ر' to "র",
            'ز' to "য",
            'س' to "স",
            'ش' to "শ",
            'ص' to "ছ",
            'ض' to "দ",
            'ط' to "ত",
            'ظ' to "জ",
            'ع' to "‘আ",
            'غ' to "গ",
            'ف' to "ফ",
            'ق' to "ক্ব",
            'ك' to "ক",
            'ل' to "ল",
            'م' to "ম",
            'ن' to "ন",
            'ه' to "হ",
            'ھ' to "হ",
            'و' to "ওয়া",
            'ي' to "ই",
            'ى' to "আ",
            'ئ' to "ই",
            'ؤ' to "উ",
            'ء' to "আ",
            'أ' to "আ",
            'إ' to "ই",
            'آ' to "আ",
            'ٱ' to "আ"
        )

        while (i < len) {
            val c = arabic[i]

            // Check if char is an Arabic letter
            if (consonantMap.containsKey(c) || c in "اأإآٱءئؤ") {
                var isShaddah = false
                var vowel = ""
                var isSukun = false
                var hasDaggerAlif = false
                var isTanweenFatha = false
                var isTanweenKasra = false
                var isTanweenDamma = false

                // Look ahead for diacritics
                var j = i + 1
                while (j < len) {
                    val d = arabic[j]
                    when (d) {
                        '\u0651' -> isShaddah = true // Shaddah
                        '\u064E' -> vowel = "a" // Fatha
                        '\u0650' -> vowel = "i" // Kasra
                        '\u064F' -> vowel = "u" // Damma
                        '\u064B' -> isTanweenFatha = true // Fathatan
                        '\u064D' -> isTanweenKasra = true // Kasratan
                        '\u064C' -> isTanweenDamma = true // Dammatan
                        '\u0652', '\u06E1' -> isSukun = true // Sukun
                        '\u0670' -> hasDaggerAlif = true // Dagger Alif
                        '\u0653' -> vowel = "aa" // Maddah
                        else -> {
                            if (d in '\u064B'..'\u065F' || d in '\u0670'..'\u06ED') {
                                // Ignore decorative Quranic marks
                            } else {
                                break
                            }
                        }
                    }
                    j++
                }

                val baseConsonant = consonantMap[c] ?: "আ"

                // First letter handling
                val isFirstLetter = sb.isEmpty()

                if (c in "اأإآٱء") {
                    when {
                        c == 'إ' || vowel == "i" -> sb.append("ই")
                        vowel == "u" -> sb.append("উ")
                        else -> sb.append("আ")
                    }
                } else if (c == 'ع') {
                    when (vowel) {
                        "i" -> sb.append("‘ই")
                        "u" -> sb.append("‘উ")
                        else -> sb.append("‘আ")
                    }
                } else if (c == 'و') {
                    if (isFirstLetter) {
                        when (vowel) {
                            "i" -> sb.append("উই")
                            "u" -> sb.append("উ")
                            else -> sb.append("ওয়া")
                        }
                    } else {
                        when (vowel) {
                            "i" -> sb.append("ওয়ি")
                            "u" -> sb.append("উ")
                            else -> sb.append("ওয়া")
                        }
                    }
                } else if (c == 'ي') {
                    if (isFirstLetter) {
                        when (vowel) {
                            "a" -> sb.append("ইয়া")
                            "u" -> sb.append("ইউ")
                            else -> sb.append("ই")
                        }
                    } else {
                        when (vowel) {
                            "a" -> sb.append("য়া")
                            "u" -> sb.append("য়ু")
                            "i" -> sb.append("য়ি")
                            else -> sb.append("ই")
                        }
                    }
                } else {
                    // Standard consonant
                    if (isShaddah) {
                        // Double consonant in Bengali
                        sb.append(baseConsonant).append("্").append(baseConsonant)
                    } else {
                        sb.append(baseConsonant)
                    }

                    // Apply vowel Kar
                    when {
                        isTanweenFatha -> sb.append("া")
                        isTanweenKasra -> sb.append("িন")
                        isTanweenDamma -> sb.append("ুন")
                        hasDaggerAlif -> sb.append("া")
                        vowel == "i" -> sb.append("ি")
                        vowel == "u" -> sb.append("ু")
                        vowel == "a" || vowel == "aa" -> {
                            // On consonants, add 'া' if not first letter with implicit 'অ'
                            if (baseConsonant != "‘আ" && baseConsonant != "ওয়া") {
                                sb.append("া")
                            }
                        }
                        isSukun -> sb.append("্")
                    }
                }

                i = j - 1
            } else if (c == ' ') {
                sb.append(" ")
            }
            i++
        }

        val output = sb.toString()
            .replace("াা", "া")
            .replace("্্", "্")
            .replace("্ ", " ")
            .trim()

        return if (output.isNotEmpty()) output else arabic
    }
}
