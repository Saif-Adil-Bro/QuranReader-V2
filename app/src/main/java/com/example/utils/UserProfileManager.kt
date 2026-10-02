package com.example.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

data class UserProfileData(
    val username: String = "দ্বীনদার বান্দা",
    val bio: String = "প্রতিদিন কুরআন তিলাওয়াত ও আমল",
    val avatarType: String = "preset_mosque", // "preset_mosque", "preset_quran", "preset_kaaba", "preset_crescent", "preset_star", "preset_heart", "custom_uri"
    val customAvatarUri: String? = null,
    val dailyGoalMinutes: Int = 20,
    val totalReadingMinutes: Int = 60,
    val todayReadingMinutes: Int = 0,
    val streakDays: Int = 1,
    val totalAyahsRead: Int = 0,
    val totalSurahsCompleted: Int = 0,
    val lastReadDate: String = "",
    val lastReadSurah: Int = 1,
    val lastReadSurahName: String = "আল-ফাতিহা",
    val lastReadAyah: Int = 1,
    val lastReadPage: Int = 1,
    val khatamCompletedPages: Int = 0,
    val khatamTargetPages: Int = 604,
    val khatamStartDate: String = ""
)

data class IslamicBadge(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val descBn: String,
    val descEn: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val progress: Float = 1f,
    val currentProgressText: String = "",
    val targetProgressText: String = ""
)

object UserProfileManager {

    private const val PREFS_NAME = "user_profile_prefs"

    private val _profileFlow = MutableStateFlow(UserProfileData())
    val profileFlow: StateFlow<UserProfileData> = _profileFlow.asStateFlow()

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = getPrefs(context)
        loadFromPrefs(prefs)
        isInitialized = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun loadFromPrefs(prefs: SharedPreferences) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val savedLastDate = prefs.getString("last_read_date", "") ?: ""

        var todayMins = prefs.getInt("today_reading_minutes", 0)
        if (savedLastDate != todayStr) {
            todayMins = 0 // Reset today's reading if date changed
        }

        val legacyPrefs = prefs.getString("username", null)
        val legacyName = if (legacyPrefs.isNullOrBlank()) {
            "দ্বীনদার বান্দা"
        } else legacyPrefs

        val data = UserProfileData(
            username = legacyName,
            bio = prefs.getString("bio", "প্রতিদিন কুরআন তিলাওয়াত ও আমল") ?: "প্রতিদিন কুরআন তিলাওয়াত ও আমল",
            avatarType = prefs.getString("avatar_type", "preset_mosque") ?: "preset_mosque",
            customAvatarUri = prefs.getString("custom_avatar_uri", null),
            dailyGoalMinutes = prefs.getInt("daily_goal_minutes", 20),
            totalReadingMinutes = prefs.getInt("total_reading_minutes", 60),
            todayReadingMinutes = todayMins,
            streakDays = prefs.getInt("streak_days", 1).coerceAtLeast(1),
            totalAyahsRead = prefs.getInt("total_ayahs_read", 0),
            totalSurahsCompleted = prefs.getInt("total_surahs_completed", 0),
            lastReadDate = savedLastDate,
            lastReadSurah = prefs.getInt("last_read_surah", 1),
            lastReadSurahName = prefs.getString("last_read_surah_name", "আল-ফাতিহা") ?: "আল-ফাতিহা",
            lastReadAyah = prefs.getInt("last_read_ayah", 1),
            lastReadPage = prefs.getInt("last_read_page", 1),
            khatamCompletedPages = prefs.getInt("khatam_completed_pages", 0),
            khatamTargetPages = 604,
            khatamStartDate = prefs.getString("khatam_start_date", todayStr) ?: todayStr
        )
        _profileFlow.value = data
    }

    fun updateProfile(
        context: Context,
        username: String? = null,
        bio: String? = null,
        avatarType: String? = null,
        customAvatarUri: String? = null,
        dailyGoalMinutes: Int? = null
    ) {
        val current = _profileFlow.value
        val updated = current.copy(
            username = username ?: current.username,
            bio = bio ?: current.bio,
            avatarType = avatarType ?: current.avatarType,
            customAvatarUri = if (avatarType == "custom_uri") customAvatarUri else if (avatarType != null) null else current.customAvatarUri,
            dailyGoalMinutes = dailyGoalMinutes ?: current.dailyGoalMinutes
        )
        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().apply {
            putString("username", updated.username)
            putString("bio", updated.bio)
            putString("avatar_type", updated.avatarType)
            putString("custom_avatar_uri", updated.customAvatarUri)
            putInt("daily_goal_minutes", updated.dailyGoalMinutes)
        }.apply()

        // Also sync legacy prefs for cross-compatibility
        try {
            val quranMenuPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            quranMenuPrefs.edit().putString("username", updated.username).apply()
        } catch (_: Exception) {}
    }

    fun setDailyGoal(context: Context, goalMinutes: Int) {
        val current = _profileFlow.value
        val updated = current.copy(dailyGoalMinutes = goalMinutes)
        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().putInt("daily_goal_minutes", goalMinutes).apply()
    }

    /**
     * Call this when a reading session ends or periodically while reading.
     */
    fun recordReadingSession(
        context: Context,
        secondsRead: Int,
        ayahsRead: Int = 0,
        pagesRead: Int = 0,
        surahNumber: Int? = null,
        surahName: String? = null,
        ayahNumber: Int? = null,
        pageNumber: Int? = null
    ) {
        if (secondsRead <= 0 && ayahsRead <= 0 && pagesRead <= 0) return

        val minsToAdd = (secondsRead / 60).coerceAtLeast(if (secondsRead >= 30) 1 else 0)
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val current = _profileFlow.value
        val lastDateStr = current.lastReadDate

        // Calculate Streak
        val newStreak = if (lastDateStr.isBlank()) {
            1
        } else {
            try {
                val lastDate = LocalDate.parse(lastDateStr, DateTimeFormatter.ISO_LOCAL_DATE)
                val daysDiff = ChronoUnit.DAYS.between(lastDate, today)
                when {
                    daysDiff == 0L -> current.streakDays // Same day, maintain streak
                    daysDiff == 1L -> current.streakDays + 1 // Consecutive day, increment
                    else -> 1 // Gap > 1 day, reset to 1
                }
            } catch (_: Exception) {
                1
            }
        }

        val newTodayMins = if (lastDateStr == todayStr) {
            current.todayReadingMinutes + minsToAdd
        } else {
            minsToAdd // New day start
        }

        val newTotalMins = current.totalReadingMinutes + minsToAdd
        val newAyahsRead = current.totalAyahsRead + ayahsRead
        val newCompletedPages = (current.khatamCompletedPages + pagesRead).coerceAtMost(604)

        val updated = current.copy(
            todayReadingMinutes = newTodayMins,
            totalReadingMinutes = newTotalMins,
            streakDays = newStreak,
            totalAyahsRead = newAyahsRead,
            lastReadDate = todayStr,
            lastReadSurah = surahNumber ?: current.lastReadSurah,
            lastReadSurahName = surahName ?: current.lastReadSurahName,
            lastReadAyah = ayahNumber ?: current.lastReadAyah,
            lastReadPage = pageNumber ?: current.lastReadPage,
            khatamCompletedPages = newCompletedPages
        )

        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().apply {
            putInt("today_reading_minutes", newTodayMins)
            putInt("total_reading_minutes", newTotalMins)
            putInt("streak_days", newStreak)
            putInt("total_ayahs_read", newAyahsRead)
            putString("last_read_date", todayStr)
            if (surahNumber != null) putInt("last_read_surah", surahNumber)
            if (surahName != null) putString("last_read_surah_name", surahName)
            if (ayahNumber != null) putInt("last_read_ayah", ayahNumber)
            if (pageNumber != null) putInt("last_read_page", pageNumber)
            putInt("khatam_completed_pages", newCompletedPages)
        }.apply()

        // Sync legacy reading time
        try {
            val quranMenuPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            quranMenuPrefs.edit().putInt("reading_time", newTotalMins).apply()
        } catch (_: Exception) {}
    }

    fun addManualReadingMinutes(context: Context, minutes: Int, ayahs: Int = 0, pages: Int = 0) {
        val current = _profileFlow.value
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val newTotal = current.totalReadingMinutes + minutes
        val newToday = current.todayReadingMinutes + minutes
        val newAyahs = current.totalAyahsRead + ayahs
        val newPages = (current.khatamCompletedPages + pages).coerceAtMost(604)

        val updated = current.copy(
            totalReadingMinutes = newTotal,
            todayReadingMinutes = newToday,
            totalAyahsRead = newAyahs,
            khatamCompletedPages = newPages,
            lastReadDate = todayStr
        )
        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().apply {
            putInt("total_reading_minutes", newTotal)
            putInt("today_reading_minutes", newToday)
            putInt("total_ayahs_read", newAyahs)
            putInt("khatam_completed_pages", newPages)
            putString("last_read_date", todayStr)
        }.apply()

        try {
            val quranMenuPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            quranMenuPrefs.edit().putInt("reading_time", newTotal).apply()
        } catch (_: Exception) {}
    }

    fun updateKhatamProgress(context: Context, completedPages: Int) {
        val clamped = completedPages.coerceIn(0, 604)
        val current = _profileFlow.value
        val updated = current.copy(khatamCompletedPages = clamped)
        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().putInt("khatam_completed_pages", clamped).apply()
    }

    fun resetKhatamProgress(context: Context) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val current = _profileFlow.value
        val updated = current.copy(
            khatamCompletedPages = 0,
            khatamStartDate = todayStr
        )
        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().apply {
            putInt("khatam_completed_pages", 0)
            putString("khatam_start_date", todayStr)
        }.apply()
    }

    fun resetAllStats(context: Context) {
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val current = _profileFlow.value
        val updated = current.copy(
            totalReadingMinutes = 0,
            todayReadingMinutes = 0,
            streakDays = 1,
            totalAyahsRead = 0,
            totalSurahsCompleted = 0,
            khatamCompletedPages = 0,
            khatamStartDate = todayStr,
            lastReadDate = todayStr
        )
        _profileFlow.value = updated

        val prefs = getPrefs(context)
        prefs.edit().apply {
            putInt("total_reading_minutes", 0)
            putInt("today_reading_minutes", 0)
            putInt("streak_days", 1)
            putInt("total_ayahs_read", 0)
            putInt("total_surahs_completed", 0)
            putInt("khatam_completed_pages", 0)
            putString("khatam_start_date", todayStr)
            putString("last_read_date", todayStr)
        }.apply()

        try {
            val quranMenuPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            quranMenuPrefs.edit().putInt("reading_time", 0).apply()
        } catch (_: Exception) {}
    }

    fun getBadges(profile: UserProfileData): List<IslamicBadge> {
        return listOf(
            IslamicBadge(
                id = "b_first_step",
                titleBn = "প্রথম পদক্ষেপ",
                titleEn = "First Step",
                descBn = "কুরআন তিলাওয়াত শুরু করেছেন",
                descEn = "Started reading the Holy Quran",
                iconName = "sparkles",
                isUnlocked = profile.totalReadingMinutes > 0 || profile.totalAyahsRead > 0,
                progress = if (profile.totalReadingMinutes > 0 || profile.totalAyahsRead > 0) 1f else 0f,
                currentProgressText = if (profile.totalReadingMinutes > 0) "১ মিনিট" else "০",
                targetProgressText = "১ মিনিট"
            ),
            IslamicBadge(
                id = "b_streak_3",
                titleBn = "ধারাবাহিক তিলাওয়াত",
                titleEn = "3-Day Streak",
                descBn = "টানা ৩ দিন নিয়মিত তিলাওয়াত",
                descEn = "Read Quran for 3 consecutive days",
                iconName = "fire",
                isUnlocked = profile.streakDays >= 3,
                progress = (profile.streakDays / 3f).coerceIn(0f, 1f),
                currentProgressText = "${profile.streakDays} দিন",
                targetProgressText = "৩ দিন"
            ),
            IslamicBadge(
                id = "b_streak_7",
                titleBn = "কুরআন অনুরাগী",
                titleEn = "Devoted Reader",
                descBn = "টানা ৭ দিনের তিলাওয়াত স্ট্রিক",
                descEn = "Achieved a 7-day reading streak",
                iconName = "trophy",
                isUnlocked = profile.streakDays >= 7,
                progress = (profile.streakDays / 7f).coerceIn(0f, 1f),
                currentProgressText = "${profile.streakDays} দিন",
                targetProgressText = "৭ দিন"
            ),
            IslamicBadge(
                id = "b_streak_30",
                titleBn = "একনিষ্ঠ সালেহীন",
                titleEn = "30-Day Master",
                descBn = "টানা ৩০ দিন নিয়মিত কুরআন পাঠ",
                descEn = "Read Quran for 30 consecutive days",
                iconName = "crown",
                isUnlocked = profile.streakDays >= 30,
                progress = (profile.streakDays / 30f).coerceIn(0f, 1f),
                currentProgressText = "${profile.streakDays} দিন",
                targetProgressText = "৩০ দিন"
            ),
            IslamicBadge(
                id = "b_daily_goal",
                titleBn = "দৈনিক লক্ষ্য পূরণ",
                titleEn = "Goal Achiever",
                descBn = "আজকের পড়ার লক্ষ্যমাত্রা পূর্ণ হয়েছে",
                descEn = "Completed today's reading target",
                iconName = "target",
                isUnlocked = profile.todayReadingMinutes >= profile.dailyGoalMinutes && profile.dailyGoalMinutes > 0,
                progress = if (profile.dailyGoalMinutes > 0) (profile.todayReadingMinutes.toFloat() / profile.dailyGoalMinutes).coerceIn(0f, 1f) else 1f,
                currentProgressText = "${profile.todayReadingMinutes} মি.",
                targetProgressText = "${profile.dailyGoalMinutes} মি."
            ),
            IslamicBadge(
                id = "b_1_hour",
                titleBn = "১ ঘণ্টা অধ্যয়ন",
                titleEn = "1 Hour Milestone",
                descBn = "মোট ৬০ মিনিট কুরআন পাঠ সম্পন্ন",
                descEn = "Completed 1 hour of total reading",
                iconName = "clock",
                isUnlocked = profile.totalReadingMinutes >= 60,
                progress = (profile.totalReadingMinutes / 60f).coerceIn(0f, 1f),
                currentProgressText = "${profile.totalReadingMinutes} মি.",
                targetProgressText = "৬০ মি."
            ),
            IslamicBadge(
                id = "b_10_hours",
                titleBn = "১০ ঘণ্টা তিলাওয়াত",
                titleEn = "10 Hours Master",
                descBn = "মোট ১০ ঘণ্টা কুরআন অধ্যয়ন সম্পন্ন",
                descEn = "Completed 10 hours of Quran reading",
                iconName = "stars",
                isUnlocked = profile.totalReadingMinutes >= 600,
                progress = (profile.totalReadingMinutes / 600f).coerceIn(0f, 1f),
                currentProgressText = "${profile.totalReadingMinutes / 60} ঘণ্টা",
                targetProgressText = "১০ ঘণ্টা"
            ),
            IslamicBadge(
                id = "b_100_ayahs",
                titleBn = "শত আয়াত পাঠ",
                titleEn = "100 Verses Read",
                descBn = "১০০+ আয়াত তিলাওয়াত সম্পন্ন",
                descEn = "Recited 100+ verses",
                iconName = "book",
                isUnlocked = profile.totalAyahsRead >= 100,
                progress = (profile.totalAyahsRead / 100f).coerceIn(0f, 1f),
                currentProgressText = "${profile.totalAyahsRead}",
                targetProgressText = "১০০"
            ),
            IslamicBadge(
                id = "b_500_ayahs",
                titleBn = "৫০০ আয়াত তিলাওয়াত",
                titleEn = "500 Verses Recited",
                descBn = "৫০০+ আয়াত অধ্যয়ন সম্পন্ন",
                descEn = "Recited 500+ verses",
                iconName = "library_books",
                isUnlocked = profile.totalAyahsRead >= 500,
                progress = (profile.totalAyahsRead / 500f).coerceIn(0f, 1f),
                currentProgressText = "${profile.totalAyahsRead}",
                targetProgressText = "৫০০"
            ),
            IslamicBadge(
                id = "b_khatam_1",
                titleBn = "পূর্ণ কুরআন খতম",
                titleEn = "Complete Khatam",
                descBn = "পবিত্র কুরআনের ৬০৪ পৃষ্ঠা পাঠ সম্পন্ন",
                descEn = "Completed recitation of all 604 pages",
                iconName = "workspace_premium",
                isUnlocked = profile.khatamCompletedPages >= 604,
                progress = (profile.khatamCompletedPages / 604f).coerceIn(0f, 1f),
                currentProgressText = "${profile.khatamCompletedPages} পৃষ্ঠা",
                targetProgressText = "৬০৪ পৃষ্ঠা"
            )
        )
    }
}

