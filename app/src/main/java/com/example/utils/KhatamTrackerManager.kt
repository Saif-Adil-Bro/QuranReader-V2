package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.time.LocalDate

data class KhatamPlan(
    val id: String = "primary_khatam",
    val title: String = "পবিত্র কুরআন খতম",
    val targetDays: Int = 30,
    val totalPages: Int = 604,
    var pagesRead: Int = 0,
    val startDate: Long = System.currentTimeMillis(),
    var lastReadDate: String = "",
    var streak: Int = 0,
    var autoSyncEnabled: Boolean = true,
    var isCompleted: Boolean = false,
    var completedDate: Long = 0L,
    var highestPageReached: Int = 0
)

data class WaqtPacing(
    val dailyTargetPages: Int,
    val fajarPages: Int,
    val dhuhrPages: Int,
    val asrPages: Int,
    val maghribPages: Int,
    val ishaPages: Int,
    val isFajarDone: Boolean,
    val isDhuhrDone: Boolean,
    val isAsrDone: Boolean,
    val isMaghribDone: Boolean,
    val isIshaDone: Boolean
)

object KhatamTrackerManager {
    private const val PREFS_NAME = "khatam_tracker_prefs"
    private const val KEY_PLAN_DATA = "active_khatam_plan"
    private const val KEY_WAQT_DONE_PREFIX = "waqt_done_"
    private const val KEY_DAILY_LOG_PREFIX = "daily_pages_"

    private val _planFlow = MutableStateFlow(KhatamPlan())
    val planFlow: StateFlow<KhatamPlan> = _planFlow.asStateFlow()

    private val _waqtPacingFlow = MutableStateFlow(
        WaqtPacing(
            dailyTargetPages = 20,
            fajarPages = 4,
            dhuhrPages = 4,
            asrPages = 4,
            maghribPages = 4,
            ishaPages = 4,
            isFajarDone = false,
            isDhuhrDone = false,
            isAsrDone = false,
            isMaghribDone = false,
            isIshaDone = false
        )
    )
    val waqtPacingFlow: StateFlow<WaqtPacing> = _waqtPacingFlow.asStateFlow()

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(KEY_PLAN_DATA, null)

        val plan: KhatamPlan
        if (!savedJson.isNullOrBlank()) {
            plan = parsePlan(savedJson)
        } else {
            // Check legacy planner in quran_menu_prefs
            val legacyPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            val legacyTarget = legacyPrefs.getString("planner_target", "৩০ দিনে খতম") ?: "৩০ দিনে খতম"
            val legacyPages = legacyPrefs.getInt("planner_pages_read", 0)
            val legacyStart = legacyPrefs.getLong("planner_start_date", System.currentTimeMillis())
            val legacyStreak = legacyPrefs.getInt("planner_streak", 0)

            val days = when {
                legacyTarget.contains("30") || legacyTarget.contains("৩০") -> 30
                legacyTarget.contains("60") || legacyTarget.contains("৬০") -> 60
                legacyTarget.contains("90") || legacyTarget.contains("৯০") -> 90
                legacyTarget.contains("180") || legacyTarget.contains("৬ মাস") -> 180
                legacyTarget.contains("365") || legacyTarget.contains("১ বছর") -> 365
                else -> 30
            }

            plan = KhatamPlan(
                title = legacyTarget,
                targetDays = days,
                pagesRead = legacyPages,
                startDate = legacyStart,
                streak = legacyStreak,
                autoSyncEnabled = true
            )
            savePlanInternal(prefs, plan)
        }

        _planFlow.value = plan
        updateWaqtPacing(context, plan)
    }

    private fun parsePlan(jsonStr: String): KhatamPlan {
        return try {
            val obj = JSONObject(jsonStr)
            KhatamPlan(
                id = obj.optString("id", "primary_khatam"),
                title = obj.optString("title", "পবিত্র কুরআন খতম"),
                targetDays = obj.optInt("targetDays", 30),
                totalPages = obj.optInt("totalPages", 604),
                pagesRead = obj.optInt("pagesRead", 0),
                startDate = obj.optLong("startDate", System.currentTimeMillis()),
                lastReadDate = obj.optString("lastReadDate", ""),
                streak = obj.optInt("streak", 0),
                autoSyncEnabled = obj.optBoolean("autoSyncEnabled", true),
                isCompleted = obj.optBoolean("isCompleted", false),
                completedDate = obj.optLong("completedDate", 0L),
                highestPageReached = obj.optInt("highestPageReached", 0)
            )
        } catch (e: Exception) {
            KhatamPlan()
        }
    }

    private fun savePlanInternal(prefs: SharedPreferences, plan: KhatamPlan) {
        val obj = JSONObject().apply {
            put("id", plan.id)
            put("title", plan.title)
            put("targetDays", plan.targetDays)
            put("totalPages", plan.totalPages)
            put("pagesRead", plan.pagesRead)
            put("startDate", plan.startDate)
            put("lastReadDate", plan.lastReadDate)
            put("streak", plan.streak)
            put("autoSyncEnabled", plan.autoSyncEnabled)
            put("isCompleted", plan.isCompleted)
            put("completedDate", plan.completedDate)
            put("highestPageReached", plan.highestPageReached)
        }
        prefs.edit().putString(KEY_PLAN_DATA, obj.toString()).apply()

        // Also update legacy prefs for backward compatibility with settings
        try {
            val legacyPrefs = prefs
            val quranPrefs = com.example.QuranApplication.instance.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            quranPrefs.edit()
                .putString("planner_target", plan.title)
                .putInt("planner_pages_read", plan.pagesRead)
                .putInt("planner_streak", plan.streak)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updatePlan(context: Context, newTargetDays: Int, customTitle: String = "") {
        val current = _planFlow.value
        val title = if (customTitle.isNotBlank()) customTitle else "$newTargetDays দিনে খতম"
        val updated = current.copy(
            title = title,
            targetDays = newTargetDays,
            startDate = System.currentTimeMillis(),
            pagesRead = 0,
            isCompleted = false,
            completedDate = 0L
        )
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        savePlanInternal(prefs, updated)
        _planFlow.value = updated
        updateWaqtPacing(context, updated)
    }

    fun toggleAutoSync(context: Context, enabled: Boolean) {
        val current = _planFlow.value
        val updated = current.copy(autoSyncEnabled = enabled)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        savePlanInternal(prefs, updated)
        _planFlow.value = updated
    }

    fun addPages(context: Context, pagesCount: Int) {
        val current = _planFlow.value
        val newTotal = (current.pagesRead + pagesCount).coerceIn(0, current.totalPages)
        val completed = newTotal >= current.totalPages
        val todayStr = LocalDate.now().toString()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updatedStreak = calculateStreak(current.lastReadDate, current.streak, todayStr)

        // Log daily pages
        val dailyKey = KEY_DAILY_LOG_PREFIX + todayStr
        val currentDaily = prefs.getInt(dailyKey, 0)
        prefs.edit().putInt(dailyKey, currentDaily + pagesCount).apply()

        val updated = current.copy(
            pagesRead = newTotal,
            isCompleted = completed,
            completedDate = if (completed && !current.isCompleted) System.currentTimeMillis() else current.completedDate,
            lastReadDate = todayStr,
            streak = updatedStreak,
            highestPageReached = maxOf(current.highestPageReached, newTotal)
        )

        savePlanInternal(prefs, updated)
        _planFlow.value = updated
        updateWaqtPacing(context, updated)
    }

    fun setDirectPages(context: Context, pages: Int) {
        val current = _planFlow.value
        val newTotal = pages.coerceIn(0, current.totalPages)
        val completed = newTotal >= current.totalPages
        val todayStr = LocalDate.now().toString()

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val updated = current.copy(
            pagesRead = newTotal,
            isCompleted = completed,
            completedDate = if (completed && !current.isCompleted) System.currentTimeMillis() else current.completedDate,
            lastReadDate = todayStr,
            highestPageReached = maxOf(current.highestPageReached, newTotal)
        )

        savePlanInternal(prefs, updated)
        _planFlow.value = updated
        updateWaqtPacing(context, updated)
    }

    fun onPageReadFromReader(context: Context, pageNumber: Int) {
        val plan = _planFlow.value
        if (!plan.autoSyncEnabled || plan.isCompleted) return

        if (pageNumber > plan.highestPageReached && pageNumber in 1..plan.totalPages) {
            val newlyRead = pageNumber - plan.highestPageReached
            if (newlyRead in 1..10) { // Safety check to avoid accidental giant leaps
                addPages(context, newlyRead)
            } else {
                // User jumped to a specific page
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val updated = plan.copy(
                    highestPageReached = pageNumber,
                    pagesRead = maxOf(plan.pagesRead, pageNumber)
                )
                savePlanInternal(prefs, updated)
                _planFlow.value = updated
                updateWaqtPacing(context, updated)
            }
        }
    }

    fun toggleWaqt(context: Context, waqtIndex: Int) {
        val todayStr = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${KEY_WAQT_DONE_PREFIX}${waqtIndex}_$todayStr"
        val current = prefs.getBoolean(key, false)
        prefs.edit().putBoolean(key, !current).apply()

        updateWaqtPacing(context, _planFlow.value)
    }

    private fun calculateStreak(lastReadDate: String, currentStreak: Int, todayStr: String): Int {
        if (lastReadDate == todayStr) return currentStreak
        val yesterdayStr = LocalDate.now().minusDays(1).toString()
        return if (lastReadDate == yesterdayStr) {
            currentStreak + 1
        } else if (lastReadDate.isEmpty()) {
            1
        } else {
            1
        }
    }

    private fun updateWaqtPacing(context: Context, plan: KhatamPlan) {
        val passedDays = maxOf(0, ((System.currentTimeMillis() - plan.startDate) / (1000 * 60 * 60 * 24)).toInt())
        val remainingDays = maxOf(1, plan.targetDays - passedDays)
        val remainingPages = maxOf(0, plan.totalPages - plan.pagesRead)

        val dailyTarget = maxOf(1, kotlin.math.ceil(remainingPages.toDouble() / remainingDays).toInt())
        val perWaqtBase = dailyTarget / 5
        val remainder = dailyTarget % 5

        val fajar = perWaqtBase + if (remainder > 0) 1 else 0
        val dhuhr = perWaqtBase + if (remainder > 1) 1 else 0
        val asr = perWaqtBase + if (remainder > 2) 1 else 0
        val maghrib = perWaqtBase + if (remainder > 3) 1 else 0
        val isha = perWaqtBase

        val todayStr = LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        _waqtPacingFlow.value = WaqtPacing(
            dailyTargetPages = dailyTarget,
            fajarPages = fajar,
            dhuhrPages = dhuhr,
            asrPages = asr,
            maghribPages = maghrib,
            ishaPages = isha,
            isFajarDone = prefs.getBoolean("${KEY_WAQT_DONE_PREFIX}0_$todayStr", false),
            isDhuhrDone = prefs.getBoolean("${KEY_WAQT_DONE_PREFIX}1_$todayStr", false),
            isAsrDone = prefs.getBoolean("${KEY_WAQT_DONE_PREFIX}2_$todayStr", false),
            isMaghribDone = prefs.getBoolean("${KEY_WAQT_DONE_PREFIX}3_$todayStr", false),
            isIshaDone = prefs.getBoolean("${KEY_WAQT_DONE_PREFIX}4_$todayStr", false)
        )
    }

    fun getDuaKhatmulQuran(): Triple<String, String, String> {
        val arabic = """
اللَّهُمَّ ارْحَمْنِي بِالْقُرْآنِ، وَاجْعَلْهُ لِي إِمَامًا وَنُورًا وَهُدًى وَرَحْمَةً،
اللَّهُمَّ ذَكِّرْنِي مِنْهُ مَا نَسِيتُ، وَعَلِّمْنِي مِنْهُ مَا جَهِلْتُ،
وَارْزُقْنِي تِلَاوَتَهُ آنَاءَ اللَّيْلِ وَأَطْرَافَ النَّهَارِ،
وَاجْعَلْهُ لِي حُجَّةً يَا رَبَّ الْعَالَمِينَ.
        """.trimIndent()

        val transliteration = """
আল্লাহুম্মারহামনী বিল-কুরআন, ওয়াজ'আলহু লী ইমা-মান ওয়া নূরান ওয়া হাদান ওয়া রাহমাহ।
আল্লাহুম্মা যাক্কিরনী মিনহু মা নাসীতু, ওয়া 'আল্লিমনি মিনহু মা জাহিলতু,
ওয়ারযুক্বনী তিলা-ওয়াতাহু আ-না-আল লাইলি ওয়া আত্বরা-ফান নাহা-র,
ওয়াজ'আলহু লী হুজ্জাতান ইয়া রাব্বাল 'আলামীন।
        """.trimIndent()

        val bangla = """
হে আল্লাহ! কুরআনের মাধ্যমে আমার উপর দয়া ও রহমত বর্ষণ করুন এবং একে আমার জন্য পথপ্রদর্শক, জ্যোতি, হিদায়াত ও করুণা বানিয়ে দিন।
হে আল্লাহ! কুরআনের যে অংশ আমি ভুলে গেছি তা আমাকে স্মরণ করিয়ে দিন, যা আমি অবগত নই তা শিক্ষা দিন এবং রাত ও দিনের প্রতিটি প্রহরে এর তিলাওয়াতের তাওফিক দান করুন।
আর হে রব্বুল আলামীন! একে আমার পক্ষে দলীল ও মুক্তির মাধ্যম বানিয়ে দিন। আমীন।
        """.trimIndent()

        return Triple(arabic, transliteration, bangla)
    }
}
