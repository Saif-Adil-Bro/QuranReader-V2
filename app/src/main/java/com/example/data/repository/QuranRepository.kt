package com.example.data.repository

import android.content.Context
import com.example.data.api.QuranApi
import com.example.data.api.QuranComApi
import com.example.data.model.CombinedAyah
import com.example.data.model.Surah
import com.example.data.model.QuranComTafsirResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.first
import java.io.File

/**
 * Repository to manage data operations for the Quran Reader app
 */
class QuranRepository(
    private val api: QuranApi,
    private val quranComApi: QuranComApi,
    private val settingsRepository: SettingsRepository,
    private val offlineDao: com.example.data.local.offline.OfflineQuranDao,
    private val quranWbwDao: com.example.data.local.offline.QuranWbwDao,
    val context: Context
) {
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    val tafsirSyncingSurahs = kotlinx.coroutines.flow.MutableStateFlow<Set<Int>>(emptySet())
    val surahDataUpdated = kotlinx.coroutines.flow.MutableSharedFlow<Int>(extraBufferCapacity = 10)
    private val downloadedSurahsCache = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
    fun isTafsirDownloaded(tafsirId: String): Boolean {
        val dir = File(context.filesDir, "tafsir_cache/$tafsirId")
        if (!dir.exists()) return false
        return (dir.listFiles()?.size ?: 0) >= 114
    }

    suspend fun downloadTranslation(translationId: String, onProgress: (Float) -> Unit) {
        val cleanId = translationId.trim()
        if (cleanId.isEmpty()) return
        val dir = java.io.File(context.filesDir, "translation_cache/$cleanId")
        dir.mkdirs()
        for (i in 1..114) {
            val file = java.io.File(dir, "$i.json")
            if (!file.exists() || file.length() == 0L) {
                try {
                    val response = quranComApi.getSurahVerses(chapterNumber = i, translations = cleanId, words = false, fields = null, wordFields = null)
                    file.writeText(com.google.gson.Gson().toJson(response))
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw Exception("Failed to download Surah $i for translation $cleanId: ${e.message}")
                }
            }
            onProgress(i / 114f)
        }
    }

    private fun getLocalSurahTranslation(surahNumber: Int, translationId: String): com.example.data.model.QuranComResponse? {
        val cleanId = translationId.trim()
        if (cleanId.isEmpty()) return null
        val file = java.io.File(context.filesDir, "translation_cache/$cleanId/$surahNumber.json")
        if (file.exists() && file.length() > 0) {
            try {
                return com.google.gson.Gson().fromJson(file.readText(), com.example.data.model.QuranComResponse::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private suspend fun fetchSingleSurahTranslation(surahNumber: Int, translationId: String): com.example.data.model.QuranComResponse? {
        val cleanId = translationId.trim()
        if (cleanId.isEmpty()) return null
        val file = java.io.File(context.filesDir, "translation_cache/$cleanId/$surahNumber.json")
        if (file.exists() && file.length() > 0) {
            try {
                return com.google.gson.Gson().fromJson(file.readText(), com.example.data.model.QuranComResponse::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return try {
            val response = quranComApi.getSurahVerses(chapterNumber = surahNumber, translations = cleanId, words = false, fields = null, wordFields = null)
            if (response != null && !response.verses.isNullOrEmpty()) {
                try {
                    file.parentFile?.mkdirs()
                    file.writeText(com.google.gson.Gson().toJson(response))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            response
        } catch (e: Exception) {
            null
        }
    }

    private fun getCombinedSurahTranslations(surahNumber: Int, translationIdsStr: String): Map<Int, List<com.example.data.model.QuranComTranslation>> {
        val verseTranslationsMap = mutableMapOf<Int, MutableList<com.example.data.model.QuranComTranslation>>()
        val translationIds = translationIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (translationIds.isEmpty()) return verseTranslationsMap
        
        for (translationId in translationIds) {
            val response = getLocalSurahTranslation(surahNumber, translationId)
            if (response != null) {
                response.verses.forEach { verse ->
                    val list = verseTranslationsMap.getOrPut(verse.verseNumber) { mutableListOf() }
                    verse.translations?.let { list.addAll(it) }
                }
            }
        }
        return verseTranslationsMap
    }

    private suspend fun enrichAyahsWithLocalTranslationsAndTafsirs(
        list: List<CombinedAyah>,
        tafsirIdsStr: String,
        translationIdsStr: String
    ): List<CombinedAyah> {
        if (list.isEmpty()) return list
        val hasTafsir = tafsirIdsStr.split(",").any { it.trim().isNotEmpty() }
        val hasTranslation = translationIdsStr.split(",").any { it.trim().isNotEmpty() }
        if (!hasTafsir && !hasTranslation) return list

        val surahsInList = list.map {
            if (it.surahNumber > 0) it.surahNumber else com.example.data.QuranData.getSurahAndAyahFromGlobal(it.number).first
        }.distinct()

        val translationsBySurah = mutableMapOf<Int, Map<Int, List<com.example.data.model.QuranComTranslation>>>()
        val tafsirsBySurah = mutableMapOf<Int, QuranComTafsirResponse?>()

        for (sNum in surahsInList) {
            if (hasTranslation) {
                val trans = getCombinedSurahTranslations(sNum, translationIdsStr)
                if (trans.isNotEmpty()) {
                    translationsBySurah[sNum] = trans
                }
            }
            if (hasTafsir) {
                val tafsirResp = getCombinedSurahTafsirs(sNum, tafsirIdsStr)
                if (tafsirResp != null && !tafsirResp.tafsirs.isNullOrEmpty()) {
                    tafsirsBySurah[sNum] = tafsirResp
                }
            }
        }

        return list.map { ayah ->
            val sNum = if (ayah.surahNumber > 0) ayah.surahNumber else com.example.data.QuranData.getSurahAndAyahFromGlobal(ayah.number).first
            val transMap = translationsBySurah[sNum]
            val tafsirResp = tafsirsBySurah[sNum]

            var updatedAyah = ayah
            if (hasTranslation) {
                val trans = transMap?.get(ayah.numberInSurah)
                if (!trans.isNullOrEmpty()) {
                    updatedAyah = updatedAyah.copy(translations = trans)
                } else if (transMap != null && transMap.isNotEmpty()) {
                    updatedAyah = updatedAyah.copy(translations = emptyList())
                }
            }

            if (hasTafsir) {
                val verseKey = "$sNum:${ayah.numberInSurah}"
                val tafsirText = if (tafsirResp != null) buildCombinedTafsirText(tafsirResp.tafsirs, verseKey) else null
                if (!tafsirText.isNullOrBlank()) {
                    updatedAyah = updatedAyah.copy(tafsirText = tafsirText)
                }
            }

            updatedAyah
        }
    }

    private suspend fun enrichAyahsWithOfflineTranslationsAndTafsirs(
        list: List<CombinedAyah>,
        tafsirIdsStr: String,
        translationIdsStr: String
    ): List<CombinedAyah> {
        return enrichAyahsWithLocalTranslationsAndTafsirs(list, tafsirIdsStr, translationIdsStr)
    }

    suspend fun isTranslationDownloaded(translationId: String): Boolean {
        val dir = java.io.File(context.filesDir, "translation_cache/$translationId")
        if (!dir.exists() || !dir.isDirectory) return false
        val files = dir.listFiles()
        return files != null && files.size >= 114
    }

    suspend fun downloadTafsir(tafsirId: String, onProgress: (Float) -> Unit) {
        val dir = File(context.filesDir, "tafsir_cache/$tafsirId")
        dir.mkdirs()
        for (i in 1..114) {
            val file = File(dir, "$i.json")
            if (!file.exists() || file.length() == 0L) {
                try {
                    val response = quranComApi.getSurahTafsirs(i, tafsirId)
                    file.writeText(Gson().toJson(response))
                } catch (e: Exception) {
                    e.printStackTrace()
                    throw Exception("Failed to download Surah $i for tafsir $tafsirId: ${e.message}")
                }
            }
            onProgress(i / 114f)
        }
    }

    private fun getLocalSurahTafsir(surahNumber: Int, tafsirId: String): QuranComTafsirResponse? {
        val cleanId = tafsirId.trim()
        if (cleanId.isEmpty()) return null
        val file = File(context.filesDir, "tafsir_cache/$cleanId/$surahNumber.json")
        if (file.exists() && file.length() > 0) {
            try {
                return Gson().fromJson(file.readText(), QuranComTafsirResponse::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    private suspend fun fetchSingleSurahTafsir(surahNumber: Int, tafsirId: String): QuranComTafsirResponse? {
        val cleanId = tafsirId.trim()
        if (cleanId.isEmpty()) return null
        val file = File(context.filesDir, "tafsir_cache/$cleanId/$surahNumber.json")
        if (file.exists() && file.length() > 0) {
            try {
                return Gson().fromJson(file.readText(), QuranComTafsirResponse::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return try {
            val response = quranComApi.getSurahTafsirs(surahNumber, cleanId)
            if (response != null && !response.tafsirs.isNullOrEmpty()) {
                try {
                    file.parentFile?.mkdirs()
                    file.writeText(Gson().toJson(response))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            response
        } catch (e: Exception) {
            null
        }
    }

    private fun getCombinedSurahTafsirs(surahNumber: Int, tafsirIdsStr: String): QuranComTafsirResponse? {
        val ids = tafsirIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (ids.isEmpty()) return null
        val allTafsirs = mutableListOf<com.example.data.model.QuranComTafsirItem>()
        for (id in ids) {
            val resp = getLocalSurahTafsir(surahNumber, id)
            if (resp != null && !resp.tafsirs.isNullOrEmpty()) {
                allTafsirs.addAll(resp.tafsirs)
            }
        }
        return if (allTafsirs.isNotEmpty()) QuranComTafsirResponse(tafsirs = allTafsirs) else null
    }

    private suspend fun getCombinedPageTafsirs(pageNumber: Int, tafsirIdsStr: String): QuranComTafsirResponse? = coroutineScope {
        try {
            val ids = tafsirIdsStr.split(",")
            val deferreds = ids.map { id ->
                async { quranComApi.getPageTafsirs(pageNumber, id.trim()) }
            }
            val responses = deferreds.awaitAll()
            val allTafsirs = responses.flatMap { it?.tafsirs ?: emptyList() }
            QuranComTafsirResponse(tafsirs = allTafsirs)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getCombinedJuzTafsirs(juzNumber: Int, tafsirIdsStr: String): QuranComTafsirResponse? = coroutineScope {
        try {
            val ids = tafsirIdsStr.split(",")
            val deferreds = ids.map { id ->
                async { quranComApi.getJuzTafsirs(juzNumber, id.trim()) }
            }
            val responses = deferreds.awaitAll()
            val allTafsirs = responses.flatMap { it?.tafsirs ?: emptyList() }
            QuranComTafsirResponse(tafsirs = allTafsirs)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun buildCombinedTafsirText(tafsirs: List<com.example.data.model.QuranComTafsirItem>?, verseKey: String): String? {
        if (tafsirs.isNullOrEmpty()) return null
        val verseTafsirs = tafsirs.filter { it.verseKey == verseKey }
        if (verseTafsirs.isEmpty()) return null
        
        if (verseTafsirs.size == 1) {
            return verseTafsirs.first().text
        }
        
        val availableTafsirs = getAvailableTafsirs("bn")
        return verseTafsirs.joinToString("<br><br>") { item ->
            val tafsirInfo = availableTafsirs.find { it.id == item.resourceId }
            val name = tafsirInfo?.name ?: "Tafsir ${item.resourceId}"
            val lang = tafsirInfo?.languageName?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: ""
            "<b>$name ($lang)</b><br>${item.text}"
        }
    }

    sealed class TafsirResult {
        data class Success(val tafsirText: String) : TafsirResult()
        object NoTafsirSelected : TafsirResult()
        object NoInternet : TafsirResult()
        object NotFound : TafsirResult()
        data class Error(val message: String) : TafsirResult()
    }

    suspend fun getAyahTafsirResult(surahNumber: Int, ayahNumberInSurah: Int): TafsirResult {
        val tafsirIdsSet = settingsRepository.selectedTafsirIdsFlow.first()
        val tafsirIdsStr = tafsirIdsSet.filter { it.isNotBlank() }.joinToString(",")
        if (tafsirIdsStr.isBlank()) {
            return TafsirResult.NoTafsirSelected
        }

        val verseKey = "$surahNumber:$ayahNumberInSurah"

        // 1. Check local files
        val localCombined = getCombinedSurahTafsirs(surahNumber, tafsirIdsStr)
        if (localCombined != null) {
            val localText = buildCombinedTafsirText(localCombined.tafsirs, verseKey)
            if (!localText.isNullOrBlank()) {
                return TafsirResult.Success(localText)
            }
        }

        // 2. Check internet connection
        val isOnline = com.example.util.NetworkUtils.isNetworkAvailable(context)
        if (!isOnline) {
            return TafsirResult.NoInternet
        }

        // 3. Online fetch with timeout
        return try {
            kotlinx.coroutines.withTimeout(5000L) {
                val ids = tafsirIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                for (id in ids) {
                    val file = File(context.filesDir, "tafsir_cache/$id/$surahNumber.json")
                    if (!file.exists() || file.length() == 0L) {
                        try {
                            val response = quranComApi.getSurahTafsirs(surahNumber, id)
                            if (response != null && !response.tafsirs.isNullOrEmpty()) {
                                file.parentFile?.mkdirs()
                                file.writeText(Gson().toJson(response))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                val freshlyLoaded = getCombinedSurahTafsirs(surahNumber, tafsirIdsStr)
                val freshlyParsedText = if (freshlyLoaded != null) buildCombinedTafsirText(freshlyLoaded.tafsirs, verseKey) else null
                if (!freshlyParsedText.isNullOrBlank()) {
                    TafsirResult.Success(freshlyParsedText)
                } else {
                    TafsirResult.NotFound
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            TafsirResult.Error("তাফসীর লোড হতে অতিরিক্ত সময় নিচ্ছে। অনুগ্রহ করে ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।")
        } catch (e: Exception) {
            TafsirResult.Error("তাফসীর লোড করতে সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}")
        }
    }


    private val BISMILLAH_PREFIXES = listOf(
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ",
        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ",
        "بِّسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ ",
        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ",
        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
        "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ ",
        "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ",
        "بسم الله الرحمن الرحيم ",
        "بسم الله الرحمن الرحيم"
    )

    // In-memory caching structures to optimize loading times and prevent repeated network calls
    private var cachedSurahs: List<Surah>? = null
    private val cachedSurahDetails = java.util.concurrent.ConcurrentHashMap<String, List<CombinedAyah>>()
    private val cachedPageDetails = java.util.concurrent.ConcurrentHashMap<String, List<CombinedAyah>>()
    private val cachedJuzDetails = java.util.concurrent.ConcurrentHashMap<String, List<CombinedAyah>>()

    private val muqattaatMap = mapOf(
        "الم" to "الٓمٓ",
        "المص" to "الٓمٓصٓ",
        "الر" to "الٓر",
        "المر" to "الٓمٓر",
        "كهيعص" to "كٓهٰیٰعٓصٓ",
        "طه" to "طٰهٰ",
        "طسم" to "طسٓمٓ",
        "طس" to "طسٓ",
        "يس" to "يٰسٓ",
        "ص" to "صٓ",
        "حم" to "حٰمٓ",
        "عسق" to "عٓسٓقٓ",
        "ق" to "قٓ",
        "ন" to "نٓ",
        "ن" to "نٓ"
    )

    private fun formatHurufeMuqattaat(text: String): String {
        val diacriticsRegex = Regex("[\\u064B-\\u065F\\u0670\\u06E1\\u06E2\\u06D6-\\u06DC]")
        val words = text.split(" ")
        val formattedWords = words.map { word ->
            val cleanWord = word.replace(diacriticsRegex, "").trim()
            muqattaatMap[cleanWord] ?: word
        }
        return formattedWords.joinToString(" ")
    }

    private fun processArabicText(ayah: com.example.data.model.Ayah, defaultSurahNumber: Int = -1): String {
        val surahNumber = ayah.surah?.number ?: defaultSurahNumber
        var text = ayah.text.replace(Regex("[\uE000-\uF8FF]"), "")
        if (ayah.numberInSurah == 1 && surahNumber != 1 && surahNumber != 9) {
            for (prefix in BISMILLAH_PREFIXES) {
                if (text.startsWith(prefix)) {
                    text = text.removePrefix(prefix).trimStart()
                    break
                }
            }
        }
        return text
    }

    private fun cleanCombinedAyahList(list: List<CombinedAyah>): List<CombinedAyah> {
        val tajweedRegex = Regex("[\u06E2\u06E5\u06E6]")
        return list.map { ayah ->
            val sNum = ayah.surahNumber.takeIf { it > 0 } ?: com.example.data.QuranData.getSurahAndAyahFromGlobal(ayah.number).first
            
            val rawWords = ayah.words
            var cleanedArabicText = ayah.arabicText.replace(Regex("[\uE000-\uF8FF]"), "")
            if (ayah.numberInSurah == 1 && sNum != 1 && sNum != 9) {
                for (prefix in BISMILLAH_PREFIXES) {
                    if (cleanedArabicText.startsWith(prefix)) {
                        cleanedArabicText = cleanedArabicText.removePrefix(prefix).trimStart()
                        break
                    }
                }
            }
            
            // Format Hurufe Muqatta'at in full text
            cleanedArabicText = formatHurufeMuqattaat(cleanedArabicText)
            
            // Remove specific Tajweed marks
            cleanedArabicText = cleanedArabicText.replace(tajweedRegex, "")
            
            // Format Hurufe Muqatta'at in individual word-by-word text and remove Tajweed marks
            val formattedWords = rawWords.map { word ->
                val text = word.textUthmani
                if (text != null) {
                    word.copy(textUthmani = formatHurufeMuqattaat(text).replace(tajweedRegex, ""))
                } else {
                    word
                }
            }
            
            ayah.copy(words = formattedWords, arabicText = cleanedArabicText, surahNumber = sNum)
        }
    }

    private val surahCacheFile by lazy {
        File(context.filesDir, "quran_text_cache/surahs.json")
    }

    private fun getSurahDetailsCacheFile(surahNumber: Int, tafsirIds: String, translationIds: String, arabicEdition: String = "quran-uthmani", audioEdition: String = ""): File {
        return File(context.filesDir, "quran_text_cache/surah_details_${surahNumber}_${arabicEdition}_${audioEdition}_t_${translationIds}_tf_${tafsirIds}.json")
    }

    private fun getPageDetailsCacheFile(pageNumber: Int, tafsirIds: String, translationIds: String, audioEdition: String = ""): File {
        return File(context.filesDir, "quran_text_cache/page_details_${pageNumber}_${audioEdition}_t_${translationIds}_tf_${tafsirIds}.json")
    }

    private fun getJuzDetailsCacheFile(juzNumber: Int, tafsirIds: String, translationIds: String, audioEdition: String = ""): File {
        return File(context.filesDir, "quran_text_cache/juz_details_${juzNumber}_${audioEdition}_t_${translationIds}_tf_${tafsirIds}.json")
    }

    fun getDownloadedSurahsCount(): Int {
        var count = 0
        for (i in 1..114) {
            if (isSurahDownloaded(i)) {
                count++
            }
        }
        return count
    }

    fun isSurahDownloaded(surahNumber: Int): Boolean {
        downloadedSurahsCache[surahNumber]?.let { return it }
        val cacheDir = File(context.filesDir, "quran_text_cache")
        if (!cacheDir.exists()) return false
        val cacheFiles = cacheDir.listFiles { _, name ->
            name.startsWith("surah_details_${surahNumber}_") && name.endsWith(".json")
        }
        if (cacheFiles.isNullOrEmpty()) return false
        
        for (cacheFile in cacheFiles) {
            if (cacheFile.length() > 1000L) {
                downloadedSurahsCache[surahNumber] = true
                return true
            }
        }
        return false
    }

    fun isAllSurahsDownloaded(): Boolean {
        return getDownloadedSurahsCount() == 114
    }

    fun deleteDownloadedSurahs() {
        val dir = File(context.filesDir, "quran_text_cache")
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        cachedSurahs = null
        cachedSurahDetails.clear()
        cachedPageDetails.clear()
        cachedJuzDetails.clear()
        downloadedSurahsCache.clear()
    }

    /**
     * Fetches the list of all Surahs
     */
    suspend fun getSurahs(): List<Surah> {
        cachedSurahs?.let { return it }
        return withContext(Dispatchers.IO) {
            var dbError: String? = null
            try {
                // Fetch from pre-packaged offline DB first
                val offlineSurahs = offlineDao.getAllSurahs()
                if (offlineSurahs.isNotEmpty()) {
                    val list = offlineSurahs.map {
                        Surah(
                            number = it.number,
                            name = it.name,
                            englishName = it.englishName,
                            englishNameTranslation = it.englishNameTranslation,
                            numberOfAyahs = it.numberOfAyahs,
                            revelationType = it.revelationType
                        )
                    }
                    cachedSurahs = list
                    return@withContext list
                } else {
                    dbError = "DB empty"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                dbError = e.message ?: e.toString()
            }

            // First, try loading from cache
            if (surahCacheFile.exists() && surahCacheFile.length() > 0) {
                try {
                    val json = surahCacheFile.readText()
                    val type = object : TypeToken<List<Surah>>() {}.type
                    val list = Gson().fromJson<List<Surah>>(json, type)
                    if (!list.isNullOrEmpty()) {
                        cachedSurahs = list
                        return@withContext list
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fetch from network
            try {
                val response = api.getSurahs()
                if (response.code == 200) {
                    val data = response.data
                    cachedSurahs = data
                    try {
                        surahCacheFile.parentFile?.mkdirs()
                        surahCacheFile.writeText(Gson().toJson(data))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    data
                } else {
                    throw Exception("Failed to load Surahs: ${response.status}")
                }
            } catch (e: Exception) {
                if (dbError != null) {
                    throw Exception("Network Error: ${e.message}. DB Error: $dbError")
                } else {
                    throw e
                }
            }
        }
    }

    private val cachedSurahWords = java.util.concurrent.ConcurrentHashMap<Int, List<com.example.data.model.QuranComWord>>()

    suspend fun getSurahWords(surahNumber: Int): List<com.example.data.model.QuranComWord> {
        val inMem = cachedSurahWords[surahNumber]
        if (inMem != null && inMem.isNotEmpty()) return inMem

        return try {
            val dbWords = quranWbwDao.getWordsBySurah(surahNumber)
            if (dbWords.isNotEmpty()) {
                val words = dbWords.map { entity ->
                    com.example.data.model.QuranComWord(
                        id = entity.id,
                        position = entity.position,
                        charTypeName = entity.charTypeName ?: "word",
                        textUthmani = entity.textUthmani,
                        translation = com.example.data.model.QuranComWordTranslation(text = entity.translationBengali),
                        transliteration = null,
                        audioUrl = entity.audioUrl
                    )
                }
                cachedSurahWords[surahNumber] = words
                words
            } else {
                val words = quranComApi.getSurahVerses(surahNumber).verses.flatMap { it.words }
                cachedSurahWords[surahNumber] = words
                words
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val cached = getSurahDetailsCombined(surahNumber).flatMap { it.words }
            if (cached.isNotEmpty()) cached else emptyList()
        }
    }

    suspend fun downloadSurahDetailsSync(surahNumber: Int, arabicEdition: String = "quran-uthmani") {
        val tafsirIdsSet = settingsRepository.selectedTafsirIdsFlow.first()
        val tafsirIdsStr = tafsirIdsSet.joinToString(",")
        val translationIdsSet = settingsRepository.selectedTranslationIdsFlow.first()
        val translationIdsStr = translationIdsSet.joinToString(",")
        val audioEdition = settingsRepository.selectedQariIdFlow.first()
        val cacheKey = "${surahNumber}_${tafsirIdsStr}_${translationIdsStr}_${audioEdition}_${arabicEdition}"
        val cacheFile = getSurahDetailsCacheFile(surahNumber, tafsirIdsStr, translationIdsStr, arabicEdition, audioEdition)
        
        var fallbackList: List<CombinedAyah>? = null
        try {
            val offlineAyahs = offlineDao.getAyahsBySurah(surahNumber)
            if (offlineAyahs.isNotEmpty()) {
                val dbList = offlineAyahs.map {
                    CombinedAyah(
                        number = it.globalNumber,
                        numberInSurah = it.numberInSurah,
                        page = it.page,
                        juz = it.juz,
                        surahNumber = surahNumber,
                        arabicText = it.arabicText,
                        bengaliText = it.bengaliText,
                        tafsirText = null,
                        audioUrl = null,
                        words = emptyList(),
                        textUthmaniTajweed = null
                    )
                }
                fallbackList = cleanCombinedAyahList(dbList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        fetchAndCacheSurahFromNetwork(
            surahNumber = surahNumber,
            cacheKey = cacheKey,
            cacheFile = cacheFile,
            tafsirIdsStr = tafsirIdsStr,
            translationIdsStr = translationIdsStr,
            audioEdition = audioEdition,
            arabicEdition = arabicEdition,
            fallbackList = fallbackList
        )
    }

    private suspend fun syncSurahTafsirAndTranslationInBackground(
        surahNumber: Int,
        cacheKey: String,
        cacheFile: File,
        tafsirIdsStr: String,
        translationIdsStr: String,
        currentList: List<CombinedAyah>
    ) {
        tafsirSyncingSurahs.value = tafsirSyncingSurahs.value + surahNumber
        val tafsirIds = tafsirIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val translationIds = translationIdsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        var updated = false

        try {
            for (tafsirId in tafsirIds) {
                val file = File(context.filesDir, "tafsir_cache/$tafsirId/$surahNumber.json")
                if (!file.exists() || file.length() == 0L) {
                    try {
                        val response = quranComApi.getSurahTafsirs(surahNumber, tafsirId)
                        if (response != null && !response.tafsirs.isNullOrEmpty()) {
                            file.parentFile?.mkdirs()
                            file.writeText(Gson().toJson(response))
                            updated = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            for (translationId in translationIds) {
                val file = File(context.filesDir, "translation_cache/$translationId/$surahNumber.json")
                if (!file.exists() || file.length() == 0L) {
                    try {
                        val response = quranComApi.getSurahVerses(
                            chapterNumber = surahNumber,
                            translations = translationId,
                            words = false,
                            fields = null,
                            wordFields = null
                        )
                        if (response != null && !response.verses.isNullOrEmpty()) {
                            file.parentFile?.mkdirs()
                            file.writeText(Gson().toJson(response))
                            updated = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            if (updated) {
                val newlyEnriched = enrichAyahsWithLocalTranslationsAndTafsirs(currentList, tafsirIdsStr, translationIdsStr)
                cachedSurahDetails[cacheKey] = newlyEnriched
                try {
                    cacheFile.parentFile?.mkdirs()
                    cacheFile.writeText(Gson().toJson(newlyEnriched))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                surahDataUpdated.tryEmit(surahNumber)
            }
        } finally {
            tafsirSyncingSurahs.value = tafsirSyncingSurahs.value - surahNumber
        }
    }

    /**
     * Fetches a specific Surah with both Arabic text and Bengali translation,
     * and combines them into a list of CombinedAyah for easy UI consumption.
     */
    suspend fun getSurahDetailsCombined(surahNumber: Int, arabicEdition: String = "default-indopak", audioEditionOverride: String? = null): List<CombinedAyah> {
        val tafsirIdsSet = settingsRepository.selectedTafsirIdsFlow.first()
        val tafsirIdsStr = tafsirIdsSet.joinToString(",")
        val translationIdsSet = settingsRepository.selectedTranslationIdsFlow.first()
        val translationIdsStr = translationIdsSet.joinToString(",")
        val audioEdition = audioEditionOverride ?: settingsRepository.selectedQariIdFlow.first()
        val cacheKey = "${surahNumber}_${tafsirIdsStr}_${translationIdsStr}_${audioEdition}_${arabicEdition}"
        val inMemory = cachedSurahDetails[cacheKey]
        if (inMemory != null && inMemory.isNotEmpty()) {
            return inMemory
        }
        return withContext(Dispatchers.IO) {
            val cacheFile = getSurahDetailsCacheFile(surahNumber, tafsirIdsStr, translationIdsStr, arabicEdition, audioEdition)
            var rawList: List<CombinedAyah>? = null

            // 1. Fetch immediately from pre-packaged offline SQLite database (zero delay)
            try {
                val offlineAyahs = offlineDao.getAyahsBySurah(surahNumber)
                if (offlineAyahs.isNotEmpty()) {
                    val totalAyahs = offlineAyahs.size
                    // Fast Progressive Loading: For large surahs (e.g. Al-Baqarah), instantly load the first chunk (30 ayahs)
                    // and progressively load remaining words asynchronously in background.
                    val isLargeSurah = totalAyahs > 40
                    val initialChunkSize = if (isLargeSurah) 30 else totalAyahs

                    val initialWords = if (isLargeSurah) {
                        quranWbwDao.getWordsBySurahRange(surahNumber, 1, initialChunkSize)
                    } else {
                        quranWbwDao.getWordsBySurah(surahNumber)
                    }
                    val wordsByAyah = initialWords.groupBy { it.ayahNumber }

                    rawList = offlineAyahs.map { ayahEntity ->
                        val ayahWords = wordsByAyah[ayahEntity.numberInSurah]?.map { w ->
                            com.example.data.model.QuranComWord(
                                id = w.id,
                                position = w.position,
                                charTypeName = w.charTypeName ?: "word",
                                textUthmani = w.textUthmani,
                                translation = com.example.data.model.QuranComWordTranslation(text = w.translationBengali),
                                transliteration = null,
                                audioUrl = w.audioUrl
                            )
                        } ?: emptyList()

                        CombinedAyah(
                            number = ayahEntity.globalNumber,
                            numberInSurah = ayahEntity.numberInSurah,
                            page = ayahEntity.page,
                            juz = ayahEntity.juz,
                            surahNumber = surahNumber,
                            arabicText = ayahEntity.arabicText,
                            bengaliText = ayahEntity.bengaliText,
                            tafsirText = null,
                            audioUrl = null,
                            words = ayahWords,
                            textUthmaniTajweed = null
                        )
                    }

                    // For large surahs, lazily stream and populate the rest of the words in background chunks
                    if (isLargeSurah) {
                        repositoryScope.launch(Dispatchers.IO) {
                            try {
                                val remainingWords = quranWbwDao.getWordsBySurahRange(surahNumber, initialChunkSize + 1, totalAyahs)
                                if (remainingWords.isNotEmpty()) {
                                    val fullWordsByAyah = (initialWords + remainingWords).groupBy { it.ayahNumber }
                                    val currentCached = cachedSurahDetails[cacheKey] ?: rawList
                                    val fullyPopulated = currentCached?.map { ayah ->
                                        if (ayah.words.isEmpty()) {
                                            val wList = fullWordsByAyah[ayah.numberInSurah]?.map { w ->
                                                com.example.data.model.QuranComWord(
                                                    id = w.id,
                                                    position = w.position,
                                                    charTypeName = w.charTypeName ?: "word",
                                                    textUthmani = w.textUthmani,
                                                    translation = com.example.data.model.QuranComWordTranslation(text = w.translationBengali),
                                                    transliteration = null,
                                                    audioUrl = w.audioUrl
                                                )
                                            } ?: emptyList()
                                            ayah.copy(words = wList)
                                        } else {
                                            ayah
                                        }
                                    }
                                    if (fullyPopulated != null) {
                                        cachedSurahDetails[cacheKey] = fullyPopulated
                                        surahDataUpdated.tryEmit(surahNumber)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Try disk cache if DB was not loaded
            if (rawList.isNullOrEmpty() && cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    val json = cacheFile.readText()
                    val type = object : TypeToken<List<CombinedAyah>>() {}.type
                    val list = Gson().fromJson<List<CombinedAyah>>(json, type)
                    if (!list.isNullOrEmpty()) {
                        rawList = list
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. If offline data exists, enrich ONLY with local offline translations & tafsirs (NO blocking network)
            if (!rawList.isNullOrEmpty()) {
                val cleaned = cleanCombinedAyahList(rawList)
                val enriched = enrichAyahsWithLocalTranslationsAndTafsirs(cleaned, tafsirIdsStr, translationIdsStr)
                cachedSurahDetails[cacheKey] = enriched

                // Fast non-blocking background disk cache write and sync
                repositoryScope.launch {
                    try {
                        cacheFile.parentFile?.mkdirs()
                        cacheFile.writeText(Gson().toJson(enriched))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                        try {
                            syncSurahTafsirAndTranslationInBackground(
                                surahNumber = surahNumber,
                                cacheKey = cacheKey,
                                cacheFile = cacheFile,
                                tafsirIdsStr = tafsirIdsStr,
                                translationIdsStr = translationIdsStr,
                                currentList = enriched
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }

                return@withContext enriched
            }

            if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                throw com.example.util.NoInternetException()
            }

            try {
                fetchAndCacheSurahFromNetwork(
                    surahNumber = surahNumber,
                    cacheKey = cacheKey,
                    cacheFile = cacheFile,
                    tafsirIdsStr = tafsirIdsStr,
                    translationIdsStr = translationIdsStr,
                    audioEdition = audioEdition,
                    arabicEdition = arabicEdition,
                    fallbackList = null
                )
                cachedSurahDetails[cacheKey] ?: throw Exception("Failed to load Surah details.")
            } catch (e: Exception) {
                throw Exception(e.message ?: e.toString())
            }
        }
    }

    /**
     * Fetches a specific page of the Quran
     */
    suspend fun getPageCombined(pageNumber: Int, audioEditionOverride: String? = null): List<CombinedAyah> {
        val cacheKey = "page_$pageNumber"
        val inMemory = cachedPageDetails[cacheKey]
        if (inMemory != null && inMemory.isNotEmpty()) {
            return inMemory
        }

        return withContext(Dispatchers.IO) {
            val pageRange = com.example.data.HafeziQuranData.getPageRange(pageNumber)
            var rangeAyahs: List<CombinedAyah> = emptyList()
            if (pageRange != null) {
                rangeAyahs = getAyahsByHafeziRange(pageRange, audioEditionOverride)
            }

            if (rangeAyahs.isEmpty()) {
                try {
                    val offlineAyahs = offlineDao.getAyahsByPage(pageNumber)
                    if (offlineAyahs.isNotEmpty()) {
                        val dbList = offlineAyahs.map {
                            CombinedAyah(
                                number = it.globalNumber,
                                numberInSurah = it.numberInSurah,
                                page = pageNumber,
                                juz = it.juz,
                                surahNumber = it.surahNumber,
                                arabicText = it.arabicText,
                                bengaliText = it.bengaliText,
                                tafsirText = null,
                                audioUrl = null,
                                words = emptyList(),
                                textUthmaniTajweed = null
                            )
                        }
                        rangeAyahs = cleanCombinedAyahList(dbList)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val finalAyahs = rangeAyahs.map { it.copy(page = pageNumber) }
            cachedPageDetails[cacheKey] = finalAyahs
            finalAyahs
        }
    }

    private suspend fun getAyahsByHafeziRange(
        range: com.example.data.HafeziPageRange,
        audioEditionOverride: String?
    ): List<CombinedAyah> {
        val result = mutableListOf<CombinedAyah>()
        for (surahNum in range.fromSurah..range.toSurah) {
            val startV = if (surahNum == range.fromSurah) range.fromAyah else 1
            val endV = if (surahNum == range.toSurah) range.toAyah else 999
            try {
                val dbAyahs = offlineDao.getAyahsBySurahRange(surahNum, startV, endV)
                val rawList = dbAyahs.map {
                    CombinedAyah(
                        number = it.globalNumber,
                        numberInSurah = it.numberInSurah,
                        page = it.page,
                        juz = it.juz,
                        surahNumber = it.surahNumber,
                        arabicText = it.arabicText,
                        bengaliText = it.bengaliText,
                        tafsirText = null,
                        audioUrl = null,
                        words = emptyList(),
                        textUthmaniTajweed = null
                    )
                }
                result.addAll(cleanCombinedAyahList(rawList))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    /**
     * Fetches a specific Juz of the Quran
     */
    suspend fun getJuzCombined(juzNumber: Int, audioEditionOverride: String? = null): List<CombinedAyah> {
        val tafsirIdsSet = settingsRepository.selectedTafsirIdsFlow.first()
        val tafsirIdsStr = tafsirIdsSet.joinToString(",")
        val translationIdsSet = settingsRepository.selectedTranslationIdsFlow.first()
        val translationIdsStr = translationIdsSet.joinToString(",")
        val audioEdition = audioEditionOverride ?: settingsRepository.selectedQariIdFlow.first()
        val cacheKey = "${juzNumber}_${tafsirIdsStr}_${translationIdsStr}_${audioEdition}"
        val inMemory = cachedJuzDetails[cacheKey]
        if (inMemory != null && inMemory.isNotEmpty()) {
            return inMemory
        }
        return withContext(Dispatchers.IO) {
            val cacheFile = getJuzDetailsCacheFile(juzNumber, tafsirIdsStr, translationIdsStr, audioEdition)
            var rawList: List<CombinedAyah>? = null

            // 1. Fetch immediately from pre-packaged SQLite (zero delay)
            try {
                val offlineAyahs = offlineDao.getAyahsByJuz(juzNumber)
                if (offlineAyahs.isNotEmpty()) {
                    rawList = offlineAyahs.map {
                        CombinedAyah(
                            number = it.globalNumber,
                            numberInSurah = it.numberInSurah,
                            page = it.page,
                            juz = it.juz,
                            surahNumber = it.surahNumber,
                            arabicText = it.arabicText,
                            bengaliText = it.bengaliText,
                            tafsirText = null,
                            audioUrl = null,
                            words = emptyList(),
                            textUthmaniTajweed = null
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (rawList.isNullOrEmpty() && cacheFile.exists() && cacheFile.length() > 0) {
                try {
                    val json = cacheFile.readText()
                    val type = object : TypeToken<List<CombinedAyah>>() {}.type
                    val list = Gson().fromJson<List<CombinedAyah>>(json, type)
                    if (!list.isNullOrEmpty()) {
                        rawList = list
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Always return offline database or file cache data IMMEDIATELY
            if (!rawList.isNullOrEmpty()) {
                val cleaned = cleanCombinedAyahList(rawList)
                val enrichedWithTajweed = enrichAyahsWithTajweed(cleaned)
                val fullyEnriched = enrichAyahsWithLocalTranslationsAndTafsirs(enrichedWithTajweed, tafsirIdsStr, translationIdsStr)
                cachedJuzDetails[cacheKey] = fullyEnriched
                try {
                    cacheFile.parentFile?.mkdirs()
                    cacheFile.writeText(Gson().toJson(fullyEnriched))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@withContext fullyEnriched
            }

            if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                throw com.example.util.NoInternetException()
            }

            try {
                fetchAndCacheJuzFromNetwork(juzNumber, cacheKey, cacheFile, tafsirIdsStr, translationIdsStr, audioEdition, null)
                cachedJuzDetails[cacheKey] ?: throw Exception("Failed to load Juz details.")
            } catch (e: Exception) {
                throw Exception(e.message ?: e.toString())
            }
        }
    }
    private data class CachedAyahSearchItem(
        val entity: com.example.data.local.offline.AyahEntity,
        val normArabic: String,
        val normBengali: String
    )

    private var cachedAyahSearchItems: List<CachedAyahSearchItem>? = null

    private suspend fun getCachedAyahSearchItems(): List<CachedAyahSearchItem> {
        cachedAyahSearchItems?.let { return it }
        val allAyahs = try {
            offlineDao.getAllAyahs()
        } catch (e: Exception) {
            emptyList()
        }
        val items = allAyahs.map {
            CachedAyahSearchItem(
                entity = it,
                normArabic = normalizeArabicText(it.arabicText),
                normBengali = normalizeBengaliText(it.bengaliText)
            )
        }
        cachedAyahSearchItems = items
        return items
    }

    /**
     * Searches the Quran by a keyword (Supports Offline-First with fast cached local SQLite database)
     */
    suspend fun searchQuranOffline(query: String, isArabic: Boolean): List<com.example.data.model.OfflineAyahSearchResult> {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val surahs = getSurahs()
            val surahMap = surahs.associateBy { it.number }
            val searchItems = getCachedAyahSearchItems()

            val matchedKeys = mutableSetOf<Pair<Int, Int>>()
            val resultList = mutableListOf<com.example.data.model.OfflineAyahSearchResult>()

            if (isArabic) {
                val searchVariants = getArabicSearchVariants(query)

                // 1. Search against normalized full Quran verses in memory
                for (item in searchItems) {
                    if (searchVariants.any { v -> item.normArabic.contains(v) }) {
                        val key = Pair(item.entity.surahNumber, item.entity.numberInSurah)
                        if (matchedKeys.add(key)) {
                            val surah = surahMap[item.entity.surahNumber] ?: Surah(
                                number = item.entity.surahNumber,
                                name = "",
                                englishName = "Surah ${item.entity.surahNumber}",
                                englishNameTranslation = "",
                                numberOfAyahs = 0,
                                revelationType = ""
                            )
                            resultList.add(
                                com.example.data.model.OfflineAyahSearchResult(
                                    match = com.example.data.model.SearchMatch(
                                        number = item.entity.globalNumber,
                                        text = item.entity.bengaliText,
                                        edition = com.example.data.model.Edition(
                                            identifier = "bn.bengali",
                                            language = "bn",
                                            name = "Bengali",
                                            englishName = "Bengali",
                                            format = "text",
                                            type = "translation"
                                        ),
                                        surah = surah,
                                        numberInSurah = item.entity.numberInSurah
                                    ),
                                    arabicText = item.entity.arabicText,
                                    bengaliText = item.entity.bengaliText
                                )
                            )
                        }
                    }
                }

                // 2. Also search Word-by-Word table for rare word forms if results are low
                if (resultList.size < 50) {
                    for (v in searchVariants) {
                        val wordMatches = quranWbwDao.searchWordsByArabic(v, limit = 50)
                        for (w in wordMatches) {
                            val key = Pair(w.surahNumber, w.ayahNumber)
                            if (matchedKeys.add(key)) {
                                val entity = offlineDao.getAyahBySurahAndNumber(w.surahNumber, w.ayahNumber)
                                if (entity != null) {
                                    val surah = surahMap[entity.surahNumber] ?: Surah(
                                        number = entity.surahNumber,
                                        name = "",
                                        englishName = "Surah ${entity.surahNumber}",
                                        englishNameTranslation = "",
                                        numberOfAyahs = 0,
                                        revelationType = ""
                                    )
                                    resultList.add(
                                        com.example.data.model.OfflineAyahSearchResult(
                                            match = com.example.data.model.SearchMatch(
                                                number = entity.globalNumber,
                                                text = entity.bengaliText,
                                                edition = com.example.data.model.Edition(
                                                    identifier = "bn.bengali",
                                                    language = "bn",
                                                    name = "Bengali",
                                                    englishName = "Bengali",
                                                    format = "text",
                                                    type = "translation"
                                                ),
                                                surah = surah,
                                                numberInSurah = entity.numberInSurah
                                            ),
                                            arabicText = entity.arabicText,
                                            bengaliText = entity.bengaliText
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                resultList
            } else {
                val searchVariants = getBengaliSearchVariants(query)

                // 1. Fast in-memory search on pre-normalized Bengali Quran translation
                for (item in searchItems) {
                    if (searchVariants.any { v -> item.normBengali.contains(v) }) {
                        val key = Pair(item.entity.surahNumber, item.entity.numberInSurah)
                        if (matchedKeys.add(key)) {
                            val surah = surahMap[item.entity.surahNumber] ?: Surah(
                                number = item.entity.surahNumber,
                                name = "",
                                englishName = "Surah ${item.entity.surahNumber}",
                                englishNameTranslation = "",
                                numberOfAyahs = 0,
                                revelationType = ""
                            )
                            resultList.add(
                                com.example.data.model.OfflineAyahSearchResult(
                                    match = com.example.data.model.SearchMatch(
                                        number = item.entity.globalNumber,
                                        text = item.entity.bengaliText,
                                        edition = com.example.data.model.Edition(
                                            identifier = "bn.bengali",
                                            language = "bn",
                                            name = "Bengali",
                                            englishName = "Bengali",
                                            format = "text",
                                            type = "translation"
                                        ),
                                        surah = surah,
                                        numberInSurah = item.entity.numberInSurah
                                    ),
                                    arabicText = item.entity.arabicText,
                                    bengaliText = item.entity.bengaliText
                                )
                            )
                        }
                    }
                }

                // 2. Also search Word-by-Word Bengali table if results are small
                if (resultList.size < 50) {
                    for (token in searchVariants) {
                        val wordMatches = quranWbwDao.searchWordsByBengali(token, limit = 50)
                        for (w in wordMatches) {
                            val key = Pair(w.surahNumber, w.ayahNumber)
                            if (matchedKeys.add(key)) {
                                val entity = offlineDao.getAyahBySurahAndNumber(w.surahNumber, w.ayahNumber)
                                if (entity != null) {
                                    val surah = surahMap[entity.surahNumber] ?: Surah(
                                        number = entity.surahNumber,
                                        name = "",
                                        englishName = "Surah ${entity.surahNumber}",
                                        englishNameTranslation = "",
                                        numberOfAyahs = 0,
                                        revelationType = ""
                                    )
                                    resultList.add(
                                        com.example.data.model.OfflineAyahSearchResult(
                                            match = com.example.data.model.SearchMatch(
                                                number = entity.globalNumber,
                                                text = entity.bengaliText,
                                                edition = com.example.data.model.Edition(
                                                    identifier = "bn.bengali",
                                                    language = "bn",
                                                    name = "Bengali",
                                                    englishName = "Bengali",
                                                    format = "text",
                                                    type = "translation"
                                                ),
                                                surah = surah,
                                                numberInSurah = entity.numberInSurah
                                            ),
                                            arabicText = entity.arabicText,
                                            bengaliText = entity.bengaliText
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
                resultList
            }
        }
    }

    private fun normalizeArabicText(text: String): String {
        if (text.isEmpty()) return ""
        val withoutDiacritics = text.replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0610-\\u061A\\u08F0-\\u08FF\\uFD3E\\uFD3F\\u200B-\\u200F\\uFEFF]"), "")
        return withoutDiacritics
            .replace(Regex("[أإآٱٲٳ]"), "ا")
            .replace(Regex("[ىيئۍێ]"), "ي")
            .replace("ة", "ه")
            .replace("ؤ", "و")
            .replace("ـ", "")
            .trim()
    }

    private fun getArabicSearchVariants(query: String): List<String> {
        val norm = normalizeArabicText(query)
        if (norm.isEmpty()) return emptyList()
        val variants = mutableSetOf(norm)
        
        if (norm.contains("لاه")) variants.add(norm.replace("لاه", "لوه"))
        if (norm.contains("كاه")) variants.add(norm.replace("كاه", "كوه"))
        if (norm.contains("ياه")) variants.add(norm.replace("ياه", "يوه"))
        if (norm.contains("لا")) variants.add(norm.replace("لا", "لو"))
        if (norm.contains("الربا")) variants.add(norm.replace("الربا", "الربوا"))
        
        return variants.toList()
    }

    fun normalizeBengaliText(text: String): String {
        if (text.isEmpty()) return ""
        var t = text
        t = t.replace("\u09CB", "\u09C7\u09BE") // ো -> ে + া
        t = t.replace("\u09CC", "\u09C7\u09D7") // ৌ -> ে + ৗ
        t = t.replace("\u09AF\u09BC", "\u09DF") // য+় -> য়
        t = t.replace("\u09A1\u09BC", "\u09DC") // ড+় -> ড়
        t = t.replace("\u09A2\u09BC", "\u09DD") // ঢ+় -> ঢ়
        return t.trim()
    }

    fun getBengaliSearchVariants(query: String): List<String> {
        val q = query.trim()
        val normQ = normalizeBengaliText(q)
        val variants = mutableSetOf(normQ, q)

        val synonymsMap = mapOf(
            "নামাজ" to listOf("নামাজ", "নামায", "সালাত", "সোলাত", "নামাযের", "সালাতের"),
            "নামায" to listOf("নামায", "নামাজ", "সালাত", "সোলাত", "নামাযের", "সালাতের"),
            "সালাত" to listOf("সালাত", "নামায", "নামাজ", "সোলাত", "সালাতের", "নামাযের"),
            "রোজা" to listOf("রোজা", "রোযা", "রোজা", "রোযা", "সিয়াম", "সিয়াম", "সাওম", "রোজার", "রোযার"),
            "রোযা" to listOf("রোযা", "রোজা", "রোযা", "রোজা", "সিয়াম", "সিয়াম", "সাওম", "রোজার", "রোযার"),
            "রোজা" to listOf("রোজা", "রোযা", "রোজা", "রোযা", "সিয়াম", "সিয়াম", "সাওম", "রোজার", "রোযার"),
            "রোযা" to listOf("রোযা", "রোজা", "রোযা", "রোজা", "সিয়াম", "সিয়াম", "সাওম", "রোজার", "রোযার"),
            "সিয়াম" to listOf("সিয়াম", "সিয়াম", "রোজা", "রোযা", "রোজা", "রোযা"),
            "সিয়াম" to listOf("সিয়াম", "সিয়াম", "রোজা", "রোযা", "রোজা", "রোযা"),
            "যাকাত" to listOf("যাকাত", "জাকাত", "যাকাতের", "জাকাতের"),
            "জাকাত" to listOf("জাকাত", "যাকাত", "জাকাতের", "যাকাতের"),
            "হজ" to listOf("হজ", "হজ্জ", "হজের", "হজ্জের"),
            "হজ্জ" to listOf("হজ্জ", "হজ", "হজ্জের", "হজের"),
            "জান্নাত" to listOf("জান্নাত", "বেহেশত", "জান্নাতের", "উদ্যান", "বাগ-বাগিচা"),
            "বেহেশত" to listOf("বেহেশত", "জান্নাত", "জান্নাতের"),
            "জাহান্নাম" to listOf("জাহান্নাম", "দোযখ", "দোজখ", "জাহান্নামের", "আগুন", "শাস্তি"),
            "দোযখ" to listOf("দোযখ", "দোজখ", "জাহান্নাম", "জাহান্নামের"),
            "দোজখ" to listOf("দোজখ", "দোযখ", "জাহান্নাম", "জাহান্নামের"),
            "ইব্রাহিম" to listOf("ইব্রাহীম", "ইব্রাহিম", "ইব্রাহীমের", "ইব্রাহিমের"),
            "ইব্রাহীম" to listOf("ইব্রাহীম", "ইব্রাহিম", "ইব্রাহীমের", "ইব্রাহিমের"),
            "মুসা" to listOf("মূসা", "মুসা", "মূসার", "মুসার"),
            "মূসা" to listOf("মূসা", "মুসা", "মূসার", "মুসার"),
            "ঈসা" to listOf("ঈসা", "ঈসার", "মসীহ"),
            "দাউদ" to listOf("দাউদ", "দাঊদ"),
            "দাঊদ" to listOf("দাঊদ", "দাউদ"),
            "ইউনুস" to listOf("ইউনুস", "ইউনূস"),
            "ইউনূস" to listOf("ইউনূস", "ইউনুস"),
            "ইউসুফ" to listOf("ইউসুফ", "ইউসূফ"),
            "ইউসূফ" to listOf("ইউসূফ", "ইউসুফ"),
            "সোলায়মান" to listOf("সোলায়মান", "সোলায়মান", "সোলায়মান", "সুলাইমান"),
            "সোলায়মান" to listOf("সোলায়মান", "সোলায়মান", "সোলায়মান", "সুলাইমান"),
            "সুলাইমান" to listOf("সোলায়মান", "সুলাইমান", "সোলায়মান"),
            "হারুন" to listOf("হারুন", "হারূন"),
            "হারূন" to listOf("হারূন", "হারুন"),
            "লুত" to listOf("লূত", "লুত"),
            "লূত" to listOf("লূত", "লুত"),
            "ইয়াকুব" to listOf("ইয়াকুব", "ইয়াকূব", "ইয়াকুব"),
            "ইয়াকুব" to listOf("ইয়াকুব", "ইয়াকূব", "ইয়াকুব"),
            "ইসমাইল" to listOf("ইসমাঈল", "ইসমাইল"),
            "ইসমাঈল" to listOf("ইসমাঈল", "ইসমাইল"),
            "শয়তান" to listOf("শয়তান", "শয়তান", "শয়তানের", "ইবলিস"),
            "শয়তান" to listOf("শয়তান", "শয়তান", "শয়তানের", "ইবলিস"),
            "ফেরাউন" to listOf("ফেরাউন", "ফেরআউন", "ফেরাউনের", "ফেরআউনের"),
            "ফেরআউন" to listOf("ফেরআউন", "ফেরাউন", "ফেরআউনের", "ফেরাউনের"),
            "কেয়ামত" to listOf("কেয়ামত", "কেয়ামত", "কিয়ামত", "কিয়ামাত", "কেয়ামতের", "কিয়ামতের"),
            "কেয়ামত" to listOf("কেয়ামত", "কেয়ামত", "কিয়ামত", "কিয়ামাত", "কেয়ামতের", "কিয়ামতের"),
            "কিয়ামত" to listOf("কেয়ামত", "কেয়ামত", "কিয়ামত", "কিয়ামাত", "কেয়ামতের", "কিয়ামতের"),
            "কুরআন" to listOf("কুরআন", "কোরআন", "কুরআনের", "কোরআনের", "কিতাব"),
            "কোরআন" to listOf("কোরআন", "কুরআন", "কুরআনের", "কোরআনের", "কিতাব"),
            "দয়ালু" to listOf("দয়ালু", "দয়ালু", "পরম দয়ালু"),
            "দয়ালু" to listOf("দয়ালু", "দয়ালু", "পরম দয়ালু"),
            "করুণাময়" to listOf("করুণাময়", "করুণাময়", "পরম করুণাময়"),
            "করুণাময়" to listOf("করুণাময়", "করুণাময়", "পরম করুণাময়")
        )

        for ((key, list) in synonymsMap) {
            if (normalizeBengaliText(key) == normQ || key == q) {
                for (item in list) {
                    variants.add(normalizeBengaliText(item))
                    variants.add(item)
                }
            }
        }

        val currentVariants = variants.toList()
        for (v in currentVariants) {
            variants.add(v.replace('ি', 'ী'))
            variants.add(v.replace('ী', 'ি'))
            variants.add(v.replace('ু', 'ূ'))
            variants.add(v.replace('ূ', 'ু'))
        }

        return variants.map { normalizeBengaliText(it) }.filter { it.isNotBlank() }.distinct()
    }

    /**
     * Searches the Quran by a keyword
     */
    suspend fun searchQuran(keyword: String, edition: String = "bn.bengali"): com.example.data.model.SearchResponse {
        return withContext(kotlinx.coroutines.Dispatchers.IO) {
            val response = api.searchQuranWithEdition(keyword, edition)
            if (response.code == 200) {
                response.data
            } else {
                throw Exception("Search failed: ${response.status}")
            }
        }
    }

    private var cachedTafsirs: List<com.example.data.model.TafsirResourceDto>? = null
    private var cachedTranslations: List<com.example.data.model.TranslationResourceDto>? = null

    private fun getTranslationsCacheFile() = java.io.File(context.filesDir, "translations_meta_cache.json")
    private fun getTafsirsCacheFile() = java.io.File(context.filesDir, "tafsirs_meta_cache.json")

    suspend fun getAvailableTranslations(language: String = "bn"): List<com.example.data.model.TranslationResourceDto> {
        if (cachedTranslations != null && cachedTranslations!!.isNotEmpty()) return cachedTranslations!!
        val cacheFile = getTranslationsCacheFile()

        // Fast local check from disk cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.example.data.model.TranslationResourceDto>>() {}.type
                val list: List<com.example.data.model.TranslationResourceDto> = com.google.gson.Gson().fromJson(cacheFile.readText(), type)
                if (!list.isNullOrEmpty()) {
                    cachedTranslations = list
                    return list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Return instant default list if network is slow or offline
        cachedTranslations = defaultBengaliTranslations

        // Asynchronously update from network in background if available
        if (com.example.util.NetworkUtils.isNetworkAvailable(context)) {
            repositoryScope.launch {
                try {
                    val response = quranComApi.getAvailableTranslations(language)
                    val filtered = response.translations.filter { item ->
                        val langMatch = item.languageName.equals("bengali", ignoreCase = true) ||
                                item.languageName.equals("english", ignoreCase = true) ||
                                item.languageName.equals("urdu", ignoreCase = true)
                        val nameLower = (item.name ?: "").lowercase()
                        val transNameLower = (item.translatedName?.name ?: "").lowercase()
                        val isTafsirOrCommentary = nameLower.contains("tafsir") || nameLower.contains("tafseer") ||
                                nameLower.contains("tafhim") || nameLower.contains("tafheem") ||
                                nameLower.contains("commentary") || nameLower.contains("transliteration") ||
                                nameLower.contains("zilal") || nameLower.contains("bayan-ul-quran") ||
                                transNameLower.contains("tafsir") || transNameLower.contains("tafseer") ||
                                transNameLower.contains("tafhim") || transNameLower.contains("tafheem") ||
                                transNameLower.contains("commentary") || transNameLower.contains("transliteration")
                        langMatch && !isTafsirOrCommentary
                    }
                    if (filtered.isNotEmpty()) {
                        cachedTranslations = filtered
                        cacheFile.writeText(com.google.gson.Gson().toJson(filtered))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return defaultBengaliTranslations
    }

    private val defaultBengaliTafsirs = listOf(
        com.example.data.model.TafsirResourceDto(id = 164, name = "তাফসীর আল-মুয়াসসার", authorName = "কিং ফাহাদ কুরআন কমপ্লেক্স", languageName = "bengali"),
        com.example.data.model.TafsirResourceDto(id = 166, name = "তাফসীর আল-জালালাইন", authorName = "জালালুদ্দিন সুয়ূতী ও মাহাল্লী", languageName = "bengali"),
        com.example.data.model.TafsirResourceDto(id = 168, name = "তাফসীর আহসানুল বয়ান", authorName = "সালাহুদ্দীন ইউসুফ", languageName = "bengali"),
        com.example.data.model.TafsirResourceDto(id = 169, name = "তাফসীর আবু বকর জাকারিয়া", authorName = "ড. আবু বকর মুহাম্মাদ যাকারিয়া", languageName = "bengali"),
        com.example.data.model.TafsirResourceDto(id = 171, name = "তাফসীর ইবনে কাসীর", authorName = "হাফেজ ইবনে কাসীর", languageName = "bengali"),
        com.example.data.model.TafsirResourceDto(id = 165, name = "তাফসীর ইবনে কাসীর (আরবি)", authorName = "হাফেজ ইবনে কাসীর", languageName = "arabic")
    )

    private val defaultBengaliTranslations = listOf(
        com.example.data.model.TranslationResourceDto(id = 161, name = "তাওহীদ পাবলিকেশন্স", authorName = "তাওহীদ পাবলিকেশন্স", languageName = "bengali"),
        com.example.data.model.TranslationResourceDto(id = 163, name = "আবু বকর যাকারিয়া", authorName = "ড. আবু বকর মুহাম্মাদ যাকারিয়া", languageName = "bengali"),
        com.example.data.model.TranslationResourceDto(id = 20, name = "Saheeh International", authorName = "Saheeh International", languageName = "english"),
        com.example.data.model.TranslationResourceDto(id = 234, name = "মুফতি ত্বকী উসমানী", authorName = "মুফতি তকী উসমানী", languageName = "urdu")
    )

    suspend fun getAvailableTafsirs(language: String = "bn"): List<com.example.data.model.TafsirResourceDto> {
        if (cachedTafsirs != null && cachedTafsirs!!.isNotEmpty()) return cachedTafsirs!!
        val cacheFile = getTafsirsCacheFile()
        
        // Fast local check from disk cache first
        if (cacheFile.exists() && cacheFile.length() > 0) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.example.data.model.TafsirResourceDto>>() {}.type
                val list: List<com.example.data.model.TafsirResourceDto> = com.google.gson.Gson().fromJson(cacheFile.readText(), type)
                if (!list.isNullOrEmpty()) {
                    cachedTafsirs = list
                    return list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Return instant default list if network is slow or offline
        cachedTafsirs = defaultBengaliTafsirs
        
        // Asynchronously update from network in background if available
        if (com.example.util.NetworkUtils.isNetworkAvailable(context)) {
            repositoryScope.launch {
                try {
                    val response = quranComApi.getAvailableTafsirs(language)
                    val filtered = response.tafsirs.filter {
                        it.languageName.equals("bengali", ignoreCase = true) ||
                                it.languageName.equals("english", ignoreCase = true) ||
                                it.languageName.equals("urdu", ignoreCase = true) ||
                                it.languageName.equals("arabic", ignoreCase = true)
                    }
                    if (filtered.isNotEmpty()) {
                        cachedTafsirs = filtered
                        cacheFile.writeText(com.google.gson.Gson().toJson(filtered))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return defaultBengaliTafsirs
    }

    private suspend fun fetchAndCacheSurahFromNetwork(
        surahNumber: Int,
        cacheKey: String,
        cacheFile: File,
        tafsirIdsStr: String,
        translationIdsStr: String,
        audioEdition: String,
        arabicEdition: String,
        fallbackList: List<CombinedAyah>?
    ) {
        val isDefaultIndoPak = arabicEdition == "default-indopak" || arabicEdition.isBlank()
        val editionToFetch = if (isDefaultIndoPak) "quran-uthmani" else arabicEdition
        val response = api.getSurahWithEditions(surahNumber, "$editionToFetch,bn.bengali,$audioEdition")
        
        val quranComResponse = try {
            quranComApi.getSurahVerses(surahNumber, translations = translationIdsStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val quranComTafsirResponse = getCombinedSurahTafsirs(surahNumber, tafsirIdsStr)

        if (response.code == 200 && response.data.size >= 2) {
            val arabicEditionObj = response.data.find { it.edition.identifier == editionToFetch }
                ?: response.data.find { it.edition.language == "ar" && it.edition.format == "text" }
            val bengaliEdition = response.data.find { it.edition.identifier == "bn.bengali" }
            val audioEditionObj = response.data.find { it.edition.identifier == audioEdition }

            if (arabicEditionObj != null && bengaliEdition != null) {
                val arabicAyahs = arabicEditionObj.ayahs
                val bengaliAyahs = bengaliEdition.ayahs
                val audioAyahs = audioEditionObj?.ayahs

                val offlineAyahs = if (isDefaultIndoPak && fallbackList.isNullOrEmpty()) {
                    try { offlineDao.getAyahsBySurah(surahNumber) } catch (e: Exception) { emptyList() }
                } else emptyList()

                val combined = arabicAyahs.mapIndexed { index, arabicAyah ->
                    val quranComVerse = quranComResponse?.verses?.find { it.verseNumber == arabicAyah.numberInSurah }
                    val verseKey = "$surahNumber:${arabicAyah.numberInSurah}"
                    val tafsir = buildCombinedTafsirText(quranComTafsirResponse?.tafsirs, verseKey)
                    val cachedWords = fallbackList?.getOrNull(index)?.words ?: emptyList()

                    val finalArabicText = if (isDefaultIndoPak) {
                        fallbackList?.getOrNull(index)?.arabicText?.takeIf { it.isNotBlank() }
                            ?: offlineAyahs.getOrNull(index)?.arabicText?.takeIf { it.isNotBlank() }
                            ?: processArabicText(arabicAyah, surahNumber)
                    } else {
                        processArabicText(arabicAyah, surahNumber)
                    }

                    CombinedAyah(
                        number = arabicAyah.number,
                        numberInSurah = arabicAyah.numberInSurah,
                        page = arabicAyah.page,
                        juz = arabicAyah.juz,
                        surahNumber = surahNumber,
                        arabicText = finalArabicText,
                        bengaliText = bengaliAyahs.getOrNull(index)?.text ?: "Translation not available",
                        translations = quranComVerse?.translations ?: fallbackList?.getOrNull(index)?.translations ?: emptyList(),
                        tafsirText = tafsir,
                        audioUrl = audioAyahs?.getOrNull(index)?.audio,
                        words = if (quranComVerse != null && quranComVerse.words.isNotEmpty()) quranComVerse.words else cachedWords,
                        textUthmaniTajweed = quranComVerse?.textUthmaniTajweed ?: fallbackList?.getOrNull(index)?.textUthmaniTajweed
                    )
                }
                val cleaned = cleanCombinedAyahList(combined)
                cachedSurahDetails[cacheKey] = cleaned
                try {
                    cacheFile.parentFile?.mkdirs()
                    cacheFile.writeText(Gson().toJson(cleaned))
                    downloadedSurahsCache[surahNumber] = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private suspend fun fetchAndCachePageFromNetwork(
        pageNumber: Int,
        cacheKey: String,
        cacheFile: File,
        tafsirIdsStr: String,
        translationIdsStr: String,
        audioEdition: String
    ) {
        val arabicResponse = api.getPageArabic(pageNumber)
        val audioResponse = api.getPageEdition(pageNumber, audioEdition)
        
        val quranComResponse = try {
            quranComApi.getPageVerses(pageNumber, translations = translationIdsStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val quranComTafsirResponse = getCombinedPageTafsirs(pageNumber, tafsirIdsStr)

        if (arabicResponse.code == 200 && audioResponse.code == 200) {
            val arabicAyahs = arabicResponse.data.ayahs
            val audioAyahs = audioResponse.data.ayahs
            
            val combined = arabicAyahs.mapIndexed { index, arabicAyah ->
                val computedSurahNumber = arabicAyah.surah?.number?.takeIf { it > 0 } 
                    ?: com.example.data.QuranData.getSurahAndAyahFromGlobal(arabicAyah.number).first
                val verseKey = "$computedSurahNumber:${arabicAyah.numberInSurah}"
                val quranComVerse = quranComResponse?.verses?.find { it.verseKey == verseKey }
                val tafsir = buildCombinedTafsirText(quranComTafsirResponse?.tafsirs, verseKey)
                CombinedAyah(
                    number = arabicAyah.number,
                    numberInSurah = arabicAyah.numberInSurah,
                    page = arabicAyah.page,
                    juz = arabicAyah.juz,
                    surahNumber = computedSurahNumber,
                    arabicText = processArabicText(arabicAyah),
                    bengaliText = "",
                    translations = quranComVerse?.translations ?: emptyList(),
                    tafsirText = tafsir,
                    audioUrl = audioAyahs.getOrNull(index)?.audio,
                    words = quranComVerse?.words ?: emptyList(),
                    textUthmaniTajweed = quranComVerse?.textUthmaniTajweed
                )
            }
            val cleaned = enrichAyahsWithTajweed(cleanCombinedAyahList(combined))
            val fullyEnriched = enrichAyahsWithOfflineTranslationsAndTafsirs(cleaned, tafsirIdsStr, translationIdsStr)
            cachedPageDetails[cacheKey] = fullyEnriched
            
            try {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(Gson().toJson(fullyEnriched))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchAndCacheJuzFromNetwork(
        juzNumber: Int,
        cacheKey: String,
        cacheFile: File,
        tafsirIdsStr: String,
        translationIdsStr: String,
        audioEdition: String,
        fallbackList: List<CombinedAyah>?
    ) {
        val arabicResponse = api.getJuzArabic(juzNumber)
        val bengaliResponse = api.getJuzBengali(juzNumber)
        val audioResponse = api.getJuzEdition(juzNumber, audioEdition)
        
        val quranComResponse = try {
            quranComApi.getJuzVerses(juzNumber, translations = translationIdsStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val quranComTafsirResponse = getCombinedJuzTafsirs(juzNumber, tafsirIdsStr)

        if (arabicResponse.code == 200 && bengaliResponse.code == 200) {
            val arabicAyahs = arabicResponse.data.ayahs
            val bengaliAyahs = bengaliResponse.data.ayahs
            val audioAyahs = audioResponse.data.ayahs
            
            val combined = arabicAyahs.mapIndexed { index, arabicAyah ->
                val computedSurahNumber = arabicAyah.surah?.number?.takeIf { it > 0 } 
                    ?: com.example.data.QuranData.getSurahAndAyahFromGlobal(arabicAyah.number).first
                val verseKey = "$computedSurahNumber:${arabicAyah.numberInSurah}"
                val quranComVerse = quranComResponse?.verses?.find { it.verseKey == verseKey }
                val tafsir = buildCombinedTafsirText(quranComTafsirResponse?.tafsirs, verseKey)
                val cachedWords = fallbackList?.getOrNull(index)?.words ?: emptyList()
                CombinedAyah(
                    number = arabicAyah.number,
                    numberInSurah = arabicAyah.numberInSurah,
                    page = arabicAyah.page,
                    juz = arabicAyah.juz,
                    surahNumber = computedSurahNumber,
                    arabicText = processArabicText(arabicAyah),
                    bengaliText = bengaliAyahs.getOrNull(index)?.text ?: "Translation not available",
                    translations = quranComVerse?.translations ?: fallbackList?.getOrNull(index)?.translations ?: emptyList(),
                    tafsirText = tafsir ?: fallbackList?.getOrNull(index)?.tafsirText,
                    audioUrl = audioAyahs.getOrNull(index)?.audio,
                    words = if (quranComVerse != null && quranComVerse.words.isNotEmpty()) quranComVerse.words else cachedWords,
                    textUthmaniTajweed = quranComVerse?.textUthmaniTajweed ?: fallbackList?.getOrNull(index)?.textUthmaniTajweed
                )
            }
            val cleaned = enrichAyahsWithTajweed(cleanCombinedAyahList(combined))
            val fullyEnriched = enrichAyahsWithOfflineTranslationsAndTafsirs(cleaned, tafsirIdsStr, translationIdsStr)
            cachedJuzDetails[cacheKey] = fullyEnriched
            
            try {
                cacheFile.parentFile?.mkdirs()
                cacheFile.writeText(Gson().toJson(fullyEnriched))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun enrichAyahsWithTajweed(ayahs: List<CombinedAyah>): List<CombinedAyah> {
        if (ayahs.isEmpty()) return ayahs
        val needsTajweed = ayahs.any { it.textUthmaniTajweed.isNullOrEmpty() || it.words.isEmpty() }
        if (!needsTajweed) return ayahs

        val surahNumbers = ayahs.map { it.surahNumber }.filter { it > 0 }.distinct()
        val surahAyahMap = mutableMapOf<Pair<Int, Int>, CombinedAyah>()

        for (surahNum in surahNumbers) {
            val inMemorySurah = cachedSurahDetails.values.firstOrNull { list ->
                list.firstOrNull()?.surahNumber == surahNum && list.any { !it.textUthmaniTajweed.isNullOrEmpty() }
            }
            if (inMemorySurah != null) {
                inMemorySurah.forEach { surahAyah ->
                    surahAyahMap[Pair(surahNum, surahAyah.numberInSurah)] = surahAyah
                }
                continue
            }

            val cacheDir = File(context.filesDir, "quran_text_cache")
            if (cacheDir.exists()) {
                val surahFiles = cacheDir.listFiles { _, name ->
                    name.startsWith("surah_details_${surahNum}_") && name.endsWith(".json")
                }
                if (!surahFiles.isNullOrEmpty()) {
                    val latestFile = surahFiles.maxByOrNull { it.lastModified() }
                    if (latestFile != null && latestFile.length() > 0) {
                        try {
                            val json = latestFile.readText()
                            val type = object : TypeToken<List<CombinedAyah>>() {}.type
                            val list = Gson().fromJson<List<CombinedAyah>>(json, type)
                            if (!list.isNullOrEmpty()) {
                                list.forEach { surahAyah ->
                                    surahAyahMap[Pair(surahNum, surahAyah.numberInSurah)] = surahAyah
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        if (surahAyahMap.isEmpty()) return ayahs

        return ayahs.map { ayah ->
            val surahAyah = surahAyahMap[Pair(ayah.surahNumber, ayah.numberInSurah)]
            if (surahAyah != null) {
                ayah.copy(
                    textUthmaniTajweed = if (!ayah.textUthmaniTajweed.isNullOrEmpty()) ayah.textUthmaniTajweed else surahAyah.textUthmaniTajweed,
                    words = if (ayah.words.isNotEmpty()) ayah.words else surahAyah.words
                )
            } else {
                ayah
            }
        }
    }
}
