package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.data.QuranData
import com.example.data.surahInfoList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class HifzStatus(val key: String, val labelBn: String, val labelEn: String) {
    NOT_STARTED("not_started", "শুরু করা হয়নি", "Not Started"),
    MEMORIZING("memorizing", "চলছে (সবক)", "Memorizing"),
    MEMORIZED("memorized", "হিফজ সম্পন্ন", "Memorized"),
    NEEDS_REVISION("needs_revision", "রিভিশন প্রয়োজন", "Needs Revision")
}

enum class HifzStrength(val key: String, val labelBn: String, val labelEn: String) {
    STRONG("strong", "মজবুত", "Strong"),
    MEDIUM("medium", "মোটামুটি", "Medium"),
    WEAK("weak", "দুর্বল", "Weak")
}

data class SurahHifzItem(
    val number: Int,
    val banglaName: String,
    val arabicName: String,
    val englishName: String,
    val totalAyahs: Int,
    val juz: Int,
    var status: HifzStatus = HifzStatus.NOT_STARTED,
    var memorizedAyahsCount: Int = 0,
    var sabaqStartAyah: Int = 1,
    var sabaqEndAyah: Int = 1,
    var strength: HifzStrength = HifzStrength.STRONG,
    var lastRevisedTimestamp: Long = 0L,
    var revisionCount: Int = 0
)

data class HifzStatistics(
    val totalSurahs: Int = 114,
    val memorizedSurahs: Int = 0,
    val memorizingSurahs: Int = 0,
    val needsRevisionSurahs: Int = 0,
    val totalQuranAyahs: Int = 6236,
    val memorizedAyahs: Int = 0,
    val completionPercentage: Float = 0f,
    val currentStreak: Int = 0
)

object HifzTrackerManager {
    private const val PREFS_NAME = "hifz_tracker_prefs"
    private const val KEY_HIFZ_DATA = "hifz_data_json"
    private const val KEY_ACTIVE_SABAQ_SURAH = "active_sabaq_surah"
    private const val KEY_ACTIVE_SABAQ_START = "active_sabaq_start"
    private const val KEY_ACTIVE_SABAQ_END = "active_sabaq_end"
    private const val KEY_DAILY_AYAH_GOAL = "daily_ayah_goal"
    private const val KEY_SABAQ_DONE_DATE = "sabaq_done_date"
    private const val KEY_SABQI_DONE_DATE = "sabqi_done_date"
    private const val KEY_MANZIL_DONE_DATE = "manzil_done_date"
    private const val KEY_HIFZ_STREAK = "hifz_streak"
    private const val KEY_LAST_ACTIVE_DATE = "last_active_date"

    private val _itemsFlow = MutableStateFlow<List<SurahHifzItem>>(emptyList())
    val itemsFlow: StateFlow<List<SurahHifzItem>> = _itemsFlow.asStateFlow()

    private val _statsFlow = MutableStateFlow(HifzStatistics())
    val statsFlow: StateFlow<HifzStatistics> = _statsFlow.asStateFlow()

    private val _activeSabaqSurah = MutableStateFlow(1)
    val activeSabaqSurah: StateFlow<Int> = _activeSabaqSurah.asStateFlow()

    private val _activeSabaqRange = MutableStateFlow(Pair(1, 7))
    val activeSabaqRange: StateFlow<Pair<Int, Int>> = _activeSabaqRange.asStateFlow()

    private val _dailyGoalAyahs = MutableStateFlow(5)
    val dailyGoalAyahs: StateFlow<Int> = _dailyGoalAyahs.asStateFlow()

    private val _isSabaqDoneToday = MutableStateFlow(false)
    val isSabaqDoneToday: StateFlow<Boolean> = _isSabaqDoneToday.asStateFlow()

    private val _isSabqiDoneToday = MutableStateFlow(false)
    val isSabqiDoneToday: StateFlow<Boolean> = _isSabqiDoneToday.asStateFlow()

    private val _isManzilDoneToday = MutableStateFlow(false)
    val isManzilDoneToday: StateFlow<Boolean> = _isManzilDoneToday.asStateFlow()

    // Map each Surah to approximate starting Juz
    private val surahToJuzMap = intArrayOf(
        1,  // 1 Fatihah
        1,  // 2 Baqarah
        3,  // 3 Al-Imran
        4,  // 4 Nisa
        6,  // 5 Maidah
        7,  // 6 An'am
        8,  // 7 A'raf
        9,  // 8 Anfal
        10, // 9 Tawbah
        11, // 10 Yunus
        11, // 11 Hud
        12, // 12 Yusuf
        13, // 13 Ra'd
        13, // 14 Ibrahim
        14, // 15 Hijr
        14, // 16 Nahl
        15, // 17 Isra
        15, // 18 Kahf
        16, // 19 Maryam
        16, // 20 Taha
        17, // 21 Anbiya
        17, // 22 Hajj
        18, // 23 Mu'minun
        18, // 24 Nur
        18, // 25 Furqan
        19, // 26 Shu'ara
        19, // 27 Naml
        20, // 28 Qasas
        20, // 29 Ankabut
        21, // 30 Rum
        21, // 31 Luqman
        21, // 32 Sajdah
        21, // 33 Ahzab
        22, // 34 Saba
        22, // 35 Fatir
        22, // 36 Yasin
        23, // 37 Saffat
        23, // 38 Sad
        23, // 39 Zumar
        24, // 40 Ghafir
        24, // 41 Fussilat
        25, // 42 Shura
        25, // 43 Zukhruf
        25, // 44 Dukhan
        26, // 45 Jathiyah
        26, // 46 Ahqaf
        26, // 47 Muhammad
        26, // 48 Fath
        26, // 49 Hujurat
        26, // 50 Qaf
        26, // 51 Dhariyat
        27, // 52 Tur
        27, // 53 Najm
        27, // 54 Qamar
        27, // 55 Rahman
        27, // 56 Waqi'ah
        27, // 57 Hadid
        28, // 58 Mujadilah
        28, // 59 Hashr
        28, // 60 Mumtahanah
        28, // 61 Saff
        28, // 62 Jumu'ah
        28, // 63 Munafiqun
        28, // 64 Taghabun
        28, // 65 Talaq
        28, // 66 Tahrim
        29, // 67 Mulk
        29, // 68 Qalam
        29, // 69 Haqqah
        29, // 70 Ma'arij
        29, // 71 Nuh
        29, // 72 Jinn
        29, // 73 Muzzammil
        29, // 74 Muddaththir
        29, // 75 Qiyamah
        29, // 76 Insan
        29, // 77 Mursalat
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, // 78-87
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, // 88-97
        30, 30, 30, 30, 30, 30, 30, 30, 30, 30, // 98-107
        30, 30, 30, 30, 30, 30, 30              // 108-114
    )

    private val surahEnglishNames = listOf(
        "Al-Fatihah", "Al-Baqarah", "Ali 'Imran", "An-Nisa", "Al-Ma'idah",
        "Al-An'am", "Al-A'raf", "Al-Anfal", "At-Tawbah", "Yunus",
        "Hud", "Yusuf", "Ar-Ra'd", "Ibrahim", "Al-Hijr",
        "An-Nahl", "Al-Isra", "Al-Kahf", "Maryam", "Ta-Ha",
        "Al-Anbiya", "Al-Hajj", "Al-Mu'minun", "An-Nur", "Al-Furqan",
        "Ash-Shu'ara", "An-Naml", "Al-Qasas", "Al-'Ankabut", "Ar-Rum",
        "Luqman", "As-Sajdah", "Al-Ahzab", "Saba", "Fatir",
        "Ya-Sin", "As-Saffat", "Sad", "Az-Zumar", "Ghafir",
        "Fussilat", "Ash-Shura", "Az-Zukhruf", "Ad-Dukhan", "Al-Jathiyah",
        "Al-Ahqaf", "Muhammad", "Al-Fath", "Al-Hujurat", "Qaf",
        "Adh-Dhariyat", "At-Tur", "An-Najm", "Al-Qamar", "Ar-Rahman",
        "Al-Waqi'ah", "Al-Hadid", "Al-Mujadila", "Al-Hashr", "Al-Mumtahanah",
        "As-Saff", "Al-Jumu'ah", "Al-Munafiqun", "At-Taghabun", "At-Talaq",
        "At-Tahrim", "Al-Mulk", "Al-Qalam", "Al-Haqqah", "Al-Ma'arij",
        "Nuh", "Al-Jinn", "Al-Muzzammil", "Al-Muddaththir", "Al-Qiyamah",
        "Al-Insan", "Al-Mursalat", "An-Naba", "An-Nazi'at", "'Abasa",
        "At-Takwir", "Al-Infitar", "Al-Mutaffifin", "Al-Inshiqaq", "Al-Buruj",
        "At-Tariq", "Al-A'la", "Al-Ghashiyah", "Al-Fajr", "Al-Balad",
        "Ash-Shams", "Al-Layl", "Ad-Duha", "Ash-Sharh", "At-Tin",
        "Al-'Alaq", "Al-Qadr", "Al-Bayyinah", "Az-Zalzalah", "Al-'Adiyat",
        "Al-Qari'ah", "At-Takathur", "Al-'Asr", "Al-Humazah", "Al-Fil",
        "Quraysh", "Al-Ma'un", "Al-Kawthar", "Al-Kafirun", "An-Nasr",
        "Al-Masad", "Al-Ikhlas", "Al-Falaq", "An-Nas"
    )

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = java.time.LocalDate.now().toString()

        _activeSabaqSurah.value = prefs.getInt(KEY_ACTIVE_SABAQ_SURAH, 1)
        _activeSabaqRange.value = Pair(
            prefs.getInt(KEY_ACTIVE_SABAQ_START, 1),
            prefs.getInt(KEY_ACTIVE_SABAQ_END, QuranData.getAyahCount(_activeSabaqSurah.value).coerceAtMost(7))
        )
        _dailyGoalAyahs.value = prefs.getInt(KEY_DAILY_AYAH_GOAL, 5)

        _isSabaqDoneToday.value = (prefs.getString(KEY_SABAQ_DONE_DATE, "") == todayStr)
        _isSabqiDoneToday.value = (prefs.getString(KEY_SABQI_DONE_DATE, "") == todayStr)
        _isManzilDoneToday.value = (prefs.getString(KEY_MANZIL_DONE_DATE, "") == todayStr)

        loadItems(context)
    }

    private fun loadItems(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_HIFZ_DATA, null)

        val itemsMap = mutableMapOf<Int, JSONObject>()
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    itemsMap[obj.getInt("num")] = obj
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Also migrate existing legacy hifz if present in quran_menu_prefs
        val legacyPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
        val legacyHifzStr = legacyPrefs.getString("hifz_progress_map", "") ?: ""
        val legacyMap = mutableMapOf<String, String>()
        if (legacyHifzStr.isNotBlank()) {
            legacyHifzStr.split(";").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) legacyMap[parts[0]] = parts[1]
            }
        }

        val list = mutableListOf<SurahHifzItem>()
        for (i in 1..114) {
            val banglaName = QuranData.getSurahNameBangla(i)
            val arabicName = QuranData.getSurahNameArabic(i)
            val englishName = surahEnglishNames.getOrElse(i - 1) { "Surah $i" }
            val totalAyahs = QuranData.getAyahCount(i)
            val juz = surahToJuzMap.getOrElse(i - 1) { 30 }

            val item = SurahHifzItem(
                number = i,
                banglaName = banglaName,
                arabicName = arabicName,
                englishName = englishName,
                totalAyahs = totalAyahs,
                juz = juz
            )

            val saved = itemsMap[i]
            if (saved != null) {
                val statusKey = saved.optString("status", HifzStatus.NOT_STARTED.key)
                item.status = HifzStatus.values().find { it.key == statusKey } ?: HifzStatus.NOT_STARTED
                item.memorizedAyahsCount = saved.optInt("memorizedAyahs", if (item.status == HifzStatus.MEMORIZED) totalAyahs else 0)
                item.sabaqStartAyah = saved.optInt("sabaqStart", 1)
                item.sabaqEndAyah = saved.optInt("sabaqEnd", 5.coerceAtMost(totalAyahs))
                val strengthKey = saved.optString("strength", HifzStrength.STRONG.key)
                item.strength = HifzStrength.values().find { it.key == strengthKey } ?: HifzStrength.STRONG
                item.lastRevisedTimestamp = saved.optLong("lastRevised", 0L)
                item.revisionCount = saved.optInt("revCount", 0)
            } else {
                // Check legacy map
                val legacyKey = "সুরা $banglaName"
                val legacyStatus = legacyMap[legacyKey] ?: legacyMap[banglaName]
                if (legacyStatus == "হিফজ করা হয়েছে") {
                    item.status = HifzStatus.MEMORIZED
                    item.memorizedAyahsCount = totalAyahs
                } else if (legacyStatus == "চলছে") {
                    item.status = HifzStatus.MEMORIZING
                    item.memorizedAyahsCount = (totalAyahs / 2).coerceAtLeast(1)
                }
            }
            list.add(item)
        }

        _itemsFlow.value = list
        updateStats(prefs, list)
    }

    private fun saveItems(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (item in _itemsFlow.value) {
            val obj = JSONObject().apply {
                put("num", item.number)
                put("status", item.status.key)
                put("memorizedAyahs", item.memorizedAyahsCount)
                put("sabaqStart", item.sabaqStartAyah)
                put("sabaqEnd", item.sabaqEndAyah)
                put("strength", item.strength.key)
                put("lastRevised", item.lastRevisedTimestamp)
                put("revCount", item.revisionCount)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_HIFZ_DATA, array.toString()).apply()
        updateStats(prefs, _itemsFlow.value)
    }

    private fun updateStats(prefs: SharedPreferences, items: List<SurahHifzItem>) {
        var memorizedSurahs = 0
        var memorizingSurahs = 0
        var needsRevisionSurahs = 0
        var memorizedAyahs = 0

        for (item in items) {
            when (item.status) {
                HifzStatus.MEMORIZED -> {
                    memorizedSurahs++
                    memorizedAyahs += item.totalAyahs
                }
                HifzStatus.MEMORIZING -> {
                    memorizingSurahs++
                    memorizedAyahs += item.memorizedAyahsCount.coerceAtMost(item.totalAyahs)
                }
                HifzStatus.NEEDS_REVISION -> {
                    needsRevisionSurahs++
                    memorizedAyahs += item.totalAyahs
                }
                HifzStatus.NOT_STARTED -> {}
            }
        }

        val percentage = (memorizedAyahs.toFloat() / 6236f * 100f).coerceIn(0f, 100f)
        val streak = prefs.getInt(KEY_HIFZ_STREAK, 0)

        _statsFlow.value = HifzStatistics(
            memorizedSurahs = memorizedSurahs,
            memorizingSurahs = memorizingSurahs,
            needsRevisionSurahs = needsRevisionSurahs,
            memorizedAyahs = memorizedAyahs,
            completionPercentage = percentage,
            currentStreak = streak
        )
    }

    fun updateSurahStatus(context: Context, surahNumber: Int, newStatus: HifzStatus) {
        val currentList = _itemsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.number == surahNumber }
        if (index != -1) {
            val old = currentList[index]
            val updated = old.copy(
                status = newStatus,
                memorizedAyahsCount = if (newStatus == HifzStatus.MEMORIZED) old.totalAyahs else if (newStatus == HifzStatus.NOT_STARTED) 0 else old.memorizedAyahsCount,
                lastRevisedTimestamp = if (newStatus == HifzStatus.MEMORIZED) System.currentTimeMillis() else old.lastRevisedTimestamp
            )
            currentList[index] = updated
            _itemsFlow.value = currentList
            saveItems(context)
        }
    }

    fun updateSurahStrength(context: Context, surahNumber: Int, newStrength: HifzStrength) {
        val currentList = _itemsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.number == surahNumber }
        if (index != -1) {
            val old = currentList[index]
            val updated = old.copy(
                strength = newStrength,
                status = if (newStrength == HifzStrength.WEAK) HifzStatus.NEEDS_REVISION else if (old.status == HifzStatus.NEEDS_REVISION) HifzStatus.MEMORIZED else old.status
            )
            currentList[index] = updated
            _itemsFlow.value = currentList
            saveItems(context)
        }
    }

    fun markSurahRevised(context: Context, surahNumber: Int) {
        val currentList = _itemsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.number == surahNumber }
        if (index != -1) {
            val old = currentList[index]
            val updated = old.copy(
                lastRevisedTimestamp = System.currentTimeMillis(),
                revisionCount = old.revisionCount + 1,
                status = if (old.status == HifzStatus.NEEDS_REVISION) HifzStatus.MEMORIZED else old.status,
                strength = if (old.strength == HifzStrength.WEAK) HifzStrength.MEDIUM else old.strength
            )
            currentList[index] = updated
            _itemsFlow.value = currentList
            saveItems(context)
        }
    }

    fun setActiveSabaq(context: Context, surahNumber: Int, startAyah: Int, endAyah: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_ACTIVE_SABAQ_SURAH, surahNumber)
            .putInt(KEY_ACTIVE_SABAQ_START, startAyah)
            .putInt(KEY_ACTIVE_SABAQ_END, endAyah)
            .apply()

        _activeSabaqSurah.value = surahNumber
        _activeSabaqRange.value = Pair(startAyah, endAyah)

        val currentList = _itemsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.number == surahNumber }
        if (index != -1) {
            val old = currentList[index]
            if (old.status == HifzStatus.NOT_STARTED) {
                currentList[index] = old.copy(status = HifzStatus.MEMORIZING, sabaqStartAyah = startAyah, sabaqEndAyah = endAyah)
                _itemsFlow.value = currentList
                saveItems(context)
            }
        }
    }

    fun markSabaqCompletedToday(context: Context) {
        val todayStr = java.time.LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SABAQ_DONE_DATE, todayStr).apply()
        _isSabaqDoneToday.value = true

        updateStreak(context)

        // Increment memorized count for active surah
        val surahNum = _activeSabaqSurah.value
        val (start, end) = _activeSabaqRange.value
        val currentList = _itemsFlow.value.toMutableList()
        val index = currentList.indexOfFirst { it.number == surahNum }
        if (index != -1) {
            val old = currentList[index]
            val newlyMemorizedCount = (end - start + 1).coerceAtLeast(0)
            val updatedCount = (old.memorizedAyahsCount + newlyMemorizedCount).coerceAtMost(old.totalAyahs)
            val isFullyCompleted = (updatedCount >= old.totalAyahs)
            currentList[index] = old.copy(
                memorizedAyahsCount = updatedCount,
                status = if (isFullyCompleted) HifzStatus.MEMORIZED else HifzStatus.MEMORIZING,
                lastRevisedTimestamp = System.currentTimeMillis()
            )
            _itemsFlow.value = currentList
            saveItems(context)

            // Advance sabaq range for tomorrow
            if (!isFullyCompleted) {
                val nextStart = end + 1
                val goal = _dailyGoalAyahs.value
                val nextEnd = (nextStart + goal - 1).coerceAtMost(old.totalAyahs)
                setActiveSabaq(context, surahNum, nextStart, nextEnd)
            }
        }
    }

    fun toggleSabqiCompletedToday(context: Context) {
        val todayStr = java.time.LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = _isSabqiDoneToday.value
        val newStatus = !current
        prefs.edit().putString(KEY_SABQI_DONE_DATE, if (newStatus) todayStr else "").apply()
        _isSabqiDoneToday.value = newStatus
        if (newStatus) updateStreak(context)
    }

    fun toggleManzilCompletedToday(context: Context) {
        val todayStr = java.time.LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = _isManzilDoneToday.value
        val newStatus = !current
        prefs.edit().putString(KEY_MANZIL_DONE_DATE, if (newStatus) todayStr else "").apply()
        _isManzilDoneToday.value = newStatus
        if (newStatus) updateStreak(context)
    }

    private fun updateStreak(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val todayStr = java.time.LocalDate.now().toString()
        val lastDate = prefs.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        var streak = prefs.getInt(KEY_HIFZ_STREAK, 0)

        if (lastDate != todayStr) {
            val yesterdayStr = java.time.LocalDate.now().minusDays(1).toString()
            if (lastDate == yesterdayStr) {
                streak += 1
            } else if (lastDate.isEmpty()) {
                streak = 1
            } else {
                streak = 1
            }
            prefs.edit()
                .putString(KEY_LAST_ACTIVE_DATE, todayStr)
                .putInt(KEY_HIFZ_STREAK, streak)
                .apply()
            updateStats(prefs, _itemsFlow.value)
        }
    }
}
