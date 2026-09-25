package com.example.utils

import android.content.Context
import com.example.data.model.PrayerName

/**
 * Dedicated Localization Helper for all Android System Notifications & Background Receivers.
 * Supports Bangla (bn) and English (en) dynamically based on user's selected app language.
 */
object NotificationLocalization {

    fun isEnglish(context: Context): Boolean {
        val lang = LocaleHelper.getLanguage(context)
        return lang.equals(LocaleHelper.LANGUAGE_ENGLISH, ignoreCase = true)
    }

    // ==========================================
    // 1. PRAYER TIMES NOTIFICATION LOCALIZATION
    // ==========================================

    fun getPrayerDisplayName(prayerName: PrayerName, isFriday: Boolean, isEnglish: Boolean): String {
        return if (isEnglish) {
            when (prayerName) {
                PrayerName.FAJR -> "Fajr"
                PrayerName.DHUHR -> if (isFriday) "Jumu'ah" else "Dhuhr"
                PrayerName.ASR -> "Asr"
                PrayerName.MAGHRIB -> "Maghrib"
                PrayerName.ISHA -> "Isha"
                PrayerName.SUNRISE -> "Sunrise"
                PrayerName.TAHAJJUD -> "Tahajjud"
                PrayerName.SAHRI -> "Sahri End"
                PrayerName.IFTAR -> "Iftar"
                PrayerName.MAKRUH_SUNRISE -> "Sunrise (Forbidden Time)"
                PrayerName.MAKRUH_ZAWAL -> "Midday Zawal (Forbidden Time)"
                PrayerName.MAKRUH_SUNSET -> "Sunset (Forbidden Time)"
            }
        } else {
            when (prayerName) {
                PrayerName.FAJR -> "ফজর"
                PrayerName.DHUHR -> if (isFriday) "জুমুআ" else "যোহর"
                PrayerName.ASR -> "আসর"
                PrayerName.MAGHRIB -> "মাগরিব"
                PrayerName.ISHA -> "এশা"
                PrayerName.SUNRISE -> "সূর্যোদয়"
                PrayerName.TAHAJJUD -> "তাহাজ্জুদ"
                PrayerName.SAHRI -> "সাহরি শেষ"
                PrayerName.IFTAR -> "ইফতার"
                PrayerName.MAKRUH_SUNRISE -> "সূর্যোদয় মাকরূহ সময়"
                PrayerName.MAKRUH_ZAWAL -> "দ্বিপ্রহর (জাওয়াল) মাকরূহ সময়"
                PrayerName.MAKRUH_SUNSET -> "সূর্যাস্ত মাকরূহ সময়"
            }
        }
    }

    fun getPrayerNotificationTitle(
        prayerName: PrayerName,
        isFriday: Boolean,
        offsetMinutes: Int,
        isEnglish: Boolean
    ): String {
        val isDhuhrOnFriday = isFriday && prayerName == PrayerName.DHUHR
        return if (isEnglish) {
            when (prayerName) {
                PrayerName.FAJR -> if (offsetMinutes < 0) "Fajr time approaching (${-offsetMinutes} mins left) 🌅" else "Fajr prayer time has started 🕌"
                PrayerName.DHUHR -> if (isDhuhrOnFriday) "Blessed Jumu'ah prayer time has started 🕌✨" else if (offsetMinutes < 0) "Dhuhr time approaching (${-offsetMinutes} mins left) ☀️" else "Dhuhr prayer time has started 🕌"
                PrayerName.ASR -> if (offsetMinutes < 0) "Asr time approaching (${-offsetMinutes} mins left) 🌤️" else "Asr prayer time has started 🕌"
                PrayerName.MAGHRIB -> if (offsetMinutes < 0) "Maghrib time approaching (${-offsetMinutes} mins left) 🌇" else "Maghrib prayer time has started 🕌"
                PrayerName.ISHA -> if (offsetMinutes < 0) "Isha time approaching (${-offsetMinutes} mins left) 🌙" else "Isha prayer time has started 🌙"
                PrayerName.SUNRISE -> "Sunrise ☀️"
                PrayerName.TAHAJJUD -> "Blessed time for Tahajjud prayer 🌌"
                PrayerName.SAHRI -> "Sahri time is about to end 🌙"
                PrayerName.IFTAR -> "Iftar time has arrived ✨"
                PrayerName.MAKRUH_SUNRISE -> "Forbidden (Makruh) Time: Sunrise ⚠️"
                PrayerName.MAKRUH_ZAWAL -> "Forbidden (Makruh) Time: Midday (Zawal) ⚠️"
                PrayerName.MAKRUH_SUNSET -> "Forbidden (Makruh) Time: Sunset ⚠️"
            }
        } else {
            when (prayerName) {
                PrayerName.FAJR -> if (offsetMinutes < 0) "ফজরের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌅" else "ফজরের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.DHUHR -> if (isDhuhrOnFriday) "পবিত্র জুমুআর ওয়াক্ত হয়েছে 🕌✨" else if (offsetMinutes < 0) "যোহরের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) ☀️" else "যোহরের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.ASR -> if (offsetMinutes < 0) "আসরের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌤️" else "আসরের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.MAGHRIB -> if (offsetMinutes < 0) "মাগরিবের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌇" else "মাগরিবের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.ISHA -> if (offsetMinutes < 0) "এশার ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌙" else "এশার ওয়াক্ত শুরু হয়েছে 🌙"
                PrayerName.SUNRISE -> "সূর্যোদয় হয়েছে ☀️"
                PrayerName.TAHAJJUD -> "তাহাজ্জুদের বিশেষ সময় হয়েছে 🌌"
                PrayerName.SAHRI -> "সাহরির সময় শেষ হতে যাচ্ছে 🌙"
                PrayerName.IFTAR -> "ইফতারের সময় হয়েছে ✨"
                PrayerName.MAKRUH_SUNRISE -> "মাকরূহ ওয়াক্ত: সূর্যোদয় ⚠️"
                PrayerName.MAKRUH_ZAWAL -> "মাকরূহ ওয়াক্ত: দ্বিপ্রহর (জাওয়াল) ⚠️"
                PrayerName.MAKRUH_SUNSET -> "মাকরূহ ওয়াক্ত: সূর্যাস্ত ⚠️"
            }
        }
    }

    fun getPrayerNotificationMessage(
        prayerName: PrayerName,
        isFriday: Boolean,
        prayerTimeFormatted: String,
        prayerRangeFormatted: String,
        districtNameBn: String,
        districtNameEn: String,
        isEnglish: Boolean
    ): String {
        val prayerDisplay = getPrayerDisplayName(prayerName, isFriday, isEnglish)
        val district = if (isEnglish && districtNameEn.isNotBlank()) districtNameEn else districtNameBn

        return if (isEnglish) {
            when (prayerName) {
                PrayerName.SAHRI -> {
                    if (prayerTimeFormatted.isNotBlank()) "Sahri end time: $prayerTimeFormatted ($district). Make intention for fasting."
                    else "Sahri time has ended ($district). Make intention for fasting."
                }
                PrayerName.IFTAR -> {
                    if (prayerTimeFormatted.isNotBlank()) "Iftar time: $prayerTimeFormatted ($district). Break your fast with Dua: Allahumma laka sumtu..."
                    else "Iftar time has arrived ($district). Recite Dua and break your fast."
                }
                PrayerName.TAHAJJUD -> {
                    "Blessed time for Tahajjud. A noble moment in the last third of the night for repentance and sincere Dua."
                }
                PrayerName.MAKRUH_SUNRISE -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "Sunrise time"
                    "Forbidden (Makruh) Salah Time: $timeDisplay ($district). Performing prayer or Sujood during sunrise is prohibited."
                }
                PrayerName.MAKRUH_ZAWAL -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "Midday Zawal"
                    "Forbidden (Makruh) Salah Time: $timeDisplay ($district). Performing prayer or Sujood when the sun is at its zenith is prohibited."
                }
                PrayerName.MAKRUH_SUNSET -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "Sunset time"
                    "Forbidden (Makruh) Salah Time: $timeDisplay ($district). Performing prayer or Sujood during sunset is prohibited."
                }
                else -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else prayerTimeFormatted
                    if (timeDisplay.isNotBlank()) {
                        "$prayerDisplay Salah Time : $timeDisplay ($district). Prepare to offer your prayer on time."
                    } else {
                        "$prayerDisplay Salah time has arrived ($district). Prepare to offer your prayer on time."
                    }
                }
            }
        } else {
            when (prayerName) {
                PrayerName.SAHRI -> {
                    if (prayerTimeFormatted.isNotBlank()) "সাহরির শেষ সময়: $prayerTimeFormatted ($district)। রোজার নিয়ত করে নিন।"
                    else "সাহরির সময় শেষ হয়েছে ($district)। রোজার নিয়ত করে নিন।"
                }
                PrayerName.IFTAR -> {
                    if (prayerTimeFormatted.isNotBlank()) "ইফতারের সময়: $prayerTimeFormatted ($district)। দুআ পাঠ করে ইফতার করুন: আল্লাহুম্মা লাকা সুমতু..."
                    else "ইফতারের সময় হয়েছে ($district)। দুআ পাঠ করে ইফতার করুন।"
                }
                PrayerName.TAHAJJUD -> {
                    "তাহাজ্জুদের বরকতময় সময়। শেষ রাতে রবের দরবারে তাওবা ও দুআ করার উত্তম মুহূর্ত।"
                }
                PrayerName.MAKRUH_SUNRISE -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "সূর্যোদয়কালীন সময়"
                    "সূর্যোদয়ের নিষিদ্ধ (মাকরূহ) ওয়াক্ত: $timeDisplay ($district)। সূর্যোদয়কালীন এই সময়ে কোনো সালাত বা সিজদাহ আদায় করা নিষিদ্ধ।"
                }
                PrayerName.MAKRUH_ZAWAL -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "দ্বিপ্রহরের সময়"
                    "দ্বিপ্রহরের নিষিদ্ধ (মাকরূহ) ওয়াক্ত: $timeDisplay ($district)। ঠিক দুপুরে সূর্য মাথার ওপর অবস্থানকালে সালাত বা সিজদাহ আদায় করা নিষিদ্ধ।"
                }
                PrayerName.MAKRUH_SUNSET -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "সূর্যাস্তের সময়"
                    "সূর্যাস্তের নিষিদ্ধ (মাকরূহ) ওয়াক্ত: $timeDisplay ($district)। সূর্যাস্তের পূর্ববর্তী এই সময়ে সালাত বা সিজদাহ আদায় করা নিষিদ্ধ।"
                }
                else -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else prayerTimeFormatted
                    if (timeDisplay.isNotBlank()) {
                        "$prayerDisplay সালাতের সময় : $timeDisplay ($district)। ওয়াক্তমত সালাত আদায় করার প্রস্তুতি নিন।"
                    } else {
                        "$prayerDisplay সালাতের সময় হয়েছে ($district)। ওয়াক্তমত সালাত আদায় করার প্রস্তুতি নিন।"
                    }
                }
            }
        }
    }

    // Action button labels in notifications
    fun getActionDismissLabel(isEnglish: Boolean): String = if (isEnglish) "Dismiss" else "বন্ধ করুন"
    fun getActionSnoozeLabel(isEnglish: Boolean): String = if (isEnglish) "Snooze 10m" else "১০ মিনিট পর (স্নুজ)"
    fun getActionViewScheduleLabel(isEnglish: Boolean): String = if (isEnglish) "View Schedule" else "সময়সূচি দেখুন"
    fun getActionReadLabel(isEnglish: Boolean): String = if (isEnglish) "Read" else "পড়ুন"
    fun getActionShareLabel(isEnglish: Boolean): String = if (isEnglish) "Share" else "শেয়ার"

    // Alarm Activity Screen Strings
    fun getAlarmStopButtonLabel(isEnglish: Boolean): String = if (isEnglish) "Stop Alarm" else "অ্যালার্ম বন্ধ করুন"
    fun getAlarmSnoozeButtonLabel(isEnglish: Boolean): String = if (isEnglish) "Snooze (10 Mins)" else "স্নুজ করুন (১০ মিনিট)"
    fun getAlarmOpenAppButtonLabel(isEnglish: Boolean): String = if (isEnglish) "Open App" else "অ্যাপে যান"
    fun getAlarmStoppedToast(isEnglish: Boolean): String = if (isEnglish) "Alarm stopped" else "অ্যালার্ম বন্ধ করা হয়েছে"
    fun getAlarmSnoozedToast(isEnglish: Boolean): String = if (isEnglish) "Alarm will ring again in 10 minutes" else "১০ মিনিট পর আবার অ্যালার্ম বাজবে"
    fun getAlarmDefaultTitle(isEnglish: Boolean): String = if (isEnglish) "Prayer Alarm" else "ওয়াক্তের অ্যালার্ম"
    fun getAlarmDefaultMessage(isEnglish: Boolean): String = if (isEnglish) "Establish Salah" else "সালাত কায়েম করুন"

    // TTS Voice Announcement
    fun getVoiceAnnouncementText(prayerName: PrayerName, isFriday: Boolean, isEnglish: Boolean): String {
        return if (isEnglish) {
            when (prayerName) {
                PrayerName.FAJR -> "It is time for Fajr prayer. As-Salatu Khairum Minan Nawm."
                PrayerName.DHUHR -> if (isFriday) "It is time for blessed Jumu'ah prayer. Prepare for congregation." else "It is time for Dhuhr prayer. Prepare for congregation."
                PrayerName.ASR -> "It is time for Asr prayer. Prepare to offer your prayer."
                PrayerName.MAGHRIB -> "It is time for Maghrib prayer. Prepare to offer your prayer."
                PrayerName.ISHA -> "It is time for Isha prayer."
                PrayerName.SUNRISE -> "Sunrise has begun. Ishraq prayer time is approaching."
                PrayerName.TAHAJJUD -> "It is the blessed time for Tahajjud prayer."
                PrayerName.SAHRI -> "Sahri time has ended. Make your intention for fasting."
                PrayerName.IFTAR -> "It is time for Iftar. Break your fast in the name of Allah."
                PrayerName.MAKRUH_SUNRISE -> "Forbidden sunrise prayer time has begun. Praying Salah during this time is prohibited."
                PrayerName.MAKRUH_ZAWAL -> "Forbidden midday Zawal time has begun. Praying Salah during this time is prohibited."
                PrayerName.MAKRUH_SUNSET -> "Forbidden sunset prayer time has begun. Praying Salah during this time is prohibited."
            }
        } else {
            when (prayerName) {
                PrayerName.FAJR -> "ফজরের নামাজের ওয়াক্ত হয়েছে, আস-সালাতু খাইরুম মিনান নাওম"
                PrayerName.DHUHR -> if (isFriday) "পবিত্র জুমুআর ওয়াক্ত হয়েছে, জামাতের প্রস্তুতি নিন" else "যোহরের নামাজের ওয়াক্ত হয়েছে, জামাতের প্রস্তুতি নিন"
                PrayerName.ASR -> "আসরের নামাজের ওয়াক্ত হয়েছে, সালাত আদায়ের প্রস্তুতি নিন"
                PrayerName.MAGHRIB -> "মাগরিবের নামাজের ওয়াক্ত হয়েছে, সালাতের প্রস্তুতি নিন"
                PrayerName.ISHA -> "এশার নামাজের ওয়াক্ত হয়েছে"
                PrayerName.SUNRISE -> "সূর্যোদয় হয়েছে, ইশরাকের নামাজের সময় আসন্ন"
                PrayerName.TAHAJJUD -> "তাহাজ্জুদের বিশেষ ফজিলতপূর্ণ সময় হয়েছে"
                PrayerName.SAHRI -> "সাহরির সময় শেষ হয়েছে, রোজার নিয়ত করে নিন"
                PrayerName.IFTAR -> "ইফতারের সময় হয়েছে, বিসমিল্লাহ বলে ইফতার করুন"
                PrayerName.MAKRUH_SUNRISE -> "সূর্যোদয়কালীন মাকরূহ সময় শুরু হয়েছে, এই সময়ে সালাত আদায় করা নিষিদ্ধ"
                PrayerName.MAKRUH_ZAWAL -> "দ্বিপ্রহরের মাকরূহ সময় শুরু হয়েছে, এই সময়ে সালাত আদায় করা নিষেধ"
                PrayerName.MAKRUH_SUNSET -> "সূর্যাস্তকালীন মাকরূহ সময় শুরু হয়েছে, এই সময়ে সালাত আদায় করা নিষেধ"
            }
        }
    }

    // ==========================================
    // 2. DAILY MESSAGE / QURAN DUA LOCALIZATION
    // ==========================================
    fun getDailyDuaNotificationTitle(isEnglish: Boolean): String = if (isEnglish) "Daily Quranic Dua" else "কুরআনিক দোয়া"
    fun getDailyMessageChannelName(isEnglish: Boolean): String = if (isEnglish) "Daily Islamic Message" else "দৈনিক ইসলামিক বার্তা"
    fun getDailyMessageChannelDesc(isEnglish: Boolean): String = if (isEnglish) "Daily Ayah, Hadith or Islamic reminder" else "দৈনিক আয়াত, হাদিস ও ইসলামিক স্মরণিকা"

    // ==========================================
    // 3. DHIKR & SALAWAT LOCALIZATION
    // ==========================================
    fun getDuroodReminderTitle(isEnglish: Boolean): String = if (isEnglish) "Durood & Salawat Reminder ✨" else "দুরুদ পাঠের স্মরণিকা ✨"
    fun getIstighfarReminderTitle(isEnglish: Boolean): String = if (isEnglish) "Istighfar Reminder 🤲" else "ইস্তিগফারের স্মরণিকা 🤲"
    fun getDhikrChannelName(isEnglish: Boolean): String = if (isEnglish) "Dhikr & Salawat Reminders" else "যিকির ও দুরুদ রিমাইন্ডার"
    fun getDhikrChannelDesc(isEnglish: Boolean): String = if (isEnglish) "Regular reminders for Durood and Istighfar" else "নিয়মিত দুরুদ ও ইস্তিগফার পাঠের নোটিফিকেশন"

    // ==========================================
    // 4. FRIDAY JUMU'AH REMINDER LOCALIZATION
    // ==========================================
    fun getJumuahReminderTitle(isEnglish: Boolean): String = if (isEnglish) "Virtues & Importance of Jumu'ah Day" else "জুমুআর দিনের বিশেষ গুরুত্ব ও ফজিলত"
    fun getJumuahReminderMessage(isEnglish: Boolean): String = if (isEnglish) "Rewards of a whole year with every step! 🕌✨" else "প্রতি কদমে এক বছরের সওয়াব! 🕌✨"
    fun getJumuahChannelName(isEnglish: Boolean): String = if (isEnglish) "Jumu'ah Reminder" else "জুমুআ রিমাইন্ডার"
    fun getJumuahChannelDesc(isEnglish: Boolean): String = if (isEnglish) "Weekly Friday Jumu'ah virtues and reminders" else "প্রতি শুক্রবার জুমুআর দিনের বিশেষ গুরুত্ব ও ফজিলত নোটিফিকেশন"

    // ==========================================
    // 5. HIJRI NEW MONTH WARNING LOCALIZATION
    // ==========================================
    fun getHijriNewMonthTitle(isEnglish: Boolean): String = if (isEnglish) "New Hijri Month Started 🌙" else "হিজরি নতুন মাস শুরু 🌙"
    fun getHijriNewMonthMessage(monthNameBn: String, monthNameEn: String, isEnglish: Boolean): String {
        val month = if (isEnglish && monthNameEn.isNotBlank()) monthNameEn else monthNameBn
        return if (isEnglish) {
            "⚠️ Month of \"$month\" begins today. If date differs from local moon sighting, please adjust in Settings → Hijri Adjustment. 🌙"
        } else {
            "⚠️ আজ থেকে \"$month\" মাস শুরু। তারিখে অসামঞ্জস্য দেখা দিলে অনুগ্রহ করে সেটিংস → হিজরি তারিখ সমন্বয় থেকে তারিখ ঠিক করে নিন। 🌙"
        }
    }
    fun getHijriNewMonthChannelName(isEnglish: Boolean): String = if (isEnglish) "New Hijri Month Notice" else "নতুন হিজরি মাস সতর্কতা"
    fun getHijriNewMonthChannelDesc(isEnglish: Boolean): String = if (isEnglish) "Alert to adjust calendar date when a new Islamic month begins" else "নতুন আরবি মাস শুরু হলে তারিখ সমন্বয় করার সতর্কবার্তা"

    // ==========================================
    // 6. MOON SIGHTING NOTICE LOCALIZATION
    // ==========================================
    fun getMoonSightingTitle(isEnglish: Boolean): String = if (isEnglish) "✨ Important Crescent Moon Sighting Notice ✨" else "✨ চাঁদ দেখা সংক্রান্ত জরুরি জ্ঞাতব্য.. ✨"
    fun getMoonSightingMessage(isEnglish: Boolean): String = if (isEnglish) "Important Sharia guideline and appeal for moon sighting:" else "দ্বীনের বৃহত্তর এক যরূরত পূরণে সর্বস্তরের ওলামায়ে কেরামের কাছে বিশেষ আবেদন:"
    fun getMoonSightingChannelName(isEnglish: Boolean): String = if (isEnglish) "Moon Sighting Notice" else "চাঁদ দেখা সংক্রান্ত বিজ্ঞপ্তি"
    fun getMoonSightingChannelDesc(isEnglish: Boolean): String = if (isEnglish) "Guidance and notices for 29th of every Islamic month" else "প্রতি আরবি মাসের ২৯ তারিখে চাঁদ দেখা সংক্রান্ত জরুরি জ্ঞাতব্য ও বিশেষ আবেদন"

    // ==========================================
    // 7. QURAN PLANNER REMINDER LOCALIZATION
    // ==========================================
    fun getPlannerReminderTitle(isEnglish: Boolean): String {
        return if (isEnglish) {
            listOf(
                "Time for Quran Reading!",
                "Have you reached today's goal?",
                "Daily Quran Recitation"
            ).random()
        } else {
            listOf(
                "কুরআন পড়ার সময় হয়েছে!",
                "আজকের লক্ষ্য কি পূরণ করেছেন?",
                "দৈনিক কুরআন তিলাওয়াত"
            ).random()
        }
    }

    fun getPlannerReminderMessage(isEnglish: Boolean): String {
        return if (isEnglish) {
            listOf(
                "Start reciting now to complete today's target...",
                "Let's continue our Quran reading plan today.",
                "Stay connected with the Quran, bless your life."
            ).random()
        } else {
            listOf(
                "আপনার আজকের লক্ষ্য পূরণ করতে তেলওয়াত শুরু করুন...",
                "আসুন আজকেও কুরআন পড়ে আমাদের প্ল্যান এগিয়ে নিয়ে যাই।",
                "কুরআনের সাথে থাকুন, জীবনকে বরকতময় করুন।"
            ).random()
        }
    }
    fun getPlannerChannelName(isEnglish: Boolean): String = if (isEnglish) "Quran Planner Reminder" else "কুরআন প্ল্যানার রিমাইন্ডার"
    fun getPlannerChannelDesc(isEnglish: Boolean): String = if (isEnglish) "Reminds you to read Quran to complete your daily goal" else "দৈনিক লক্ষ্য অনুযায়ী কুরআন তিলাওয়াত করার রিমাইন্ডার"
}
