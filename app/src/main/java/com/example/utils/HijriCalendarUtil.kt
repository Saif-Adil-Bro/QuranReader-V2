package com.example.utils

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDateInfo(
    val gregorianDate: LocalDate,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val hijriMonthNameBn: String,
    val hijriMonthNameAr: String,
    val specialEvents: List<String>,
    val isSunnahFast: Boolean,
    val sunnahFastReason: String?
)

object HijriCalendarUtil {

    private val hijriMonthNamesBengali = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শা'বান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ্জ"
    )

    private val hijriMonthNamesArabic = listOf(
        "المحرّم", "صفر", "ربيع الأوّل", "ربيع الثاني",
        "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
        "رمضان", "شوّال", "ذو القعدة", "ذو الحجة"
    )

    fun getHijriDate(date: LocalDate, offsetDays: Int = 0): HijriDateInfo {
        return calculateHijriDate(date, offsetDays)
    }

    /**
     * Used for full-month Calendar Grid display to ensure sequential, consistent day-by-day dates.
     */
    fun getHijriDateForGrid(date: LocalDate, offsetDays: Int = 0): HijriDateInfo {
        return calculateHijriDate(date, offsetDays)
    }

    private fun calculateHijriDate(date: LocalDate, offsetDays: Int): HijriDateInfo {
        val (hDay, hMonth, hYear) = try {
            val islamicCalendar = android.icu.util.IslamicCalendar()
            val calendar = java.util.Calendar.getInstance()
            calendar.set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            islamicCalendar.time = calendar.time
            if (offsetDays != 0) {
                islamicCalendar.add(android.icu.util.IslamicCalendar.DAY_OF_MONTH, offsetDays)
            }
            Triple(
                islamicCalendar.get(android.icu.util.IslamicCalendar.DAY_OF_MONTH),
                islamicCalendar.get(android.icu.util.IslamicCalendar.MONTH) + 1,
                islamicCalendar.get(android.icu.util.IslamicCalendar.YEAR)
            )
        } catch (e: Exception) {
            val hijrahDate = HijrahDate.from(date.plusDays(offsetDays.toLong()))
            Triple(
                hijrahDate.get(ChronoField.DAY_OF_MONTH),
                hijrahDate.get(ChronoField.MONTH_OF_YEAR),
                hijrahDate.get(ChronoField.YEAR)
            )
        }

        val monthBn = hijriMonthNamesBengali.getOrElse(hMonth - 1) { "হিজরী" }
        val monthAr = hijriMonthNamesArabic.getOrElse(hMonth - 1) { "" }

        val events = mutableListOf<String>()
        
        // Special Islamic Days based on Hijri Month & Day
        when (hMonth) {
            1 -> { // Muharram
                if (hDay == 1) events.add("ইসলামী নববর্ষ (১ মুহাররম)")
                if (hDay == 9) events.add("তাসূআ (পবিত্র আশুরার আগের দিন)")
                if (hDay == 10) events.add("পবিত্র আশুরা (১০ মুহাররম)")
            }
            3 -> { // Rabi al-Awwal
                if (hDay == 12) events.add("পবিত্র ঈদে মিলাদুন্নবী (সা:)")
            }
            7 -> { // Rajab
                if (hDay == 27) events.add("পবিত্র শবে মেরাজ (২৭ রজব)")
            }
            8 -> { // Sha'ban
                if (hDay == 15) events.add("পবিত্র শবে বরাত (১৫ শা'বান)")
            }
            9 -> { // Ramadan
                if (hDay == 1) events.add("পবিত্র রমজান মাসের প্রথম দিন")
                if (hDay in 21..29 && hDay % 2 != 0) events.add("লাইলাতুল কদর সম্ভাবনা (রমজানের শেষ দশক)")
                if (hDay == 27) events.add("পবিত্র লাইলাতুল কদর (২৭ রমজান)")
            }
            10 -> { // Shawwal
                if (hDay == 1) events.add("পবিত্র ঈদুল ফিতর (১ শাওয়াল)")
                if (hDay in 2..7) events.add("শাওয়ালের ৬ রোজা (নফল)")
            }
            12 -> { // Dhul-Hijjah
                if (hDay == 1) events.add("জিলহজ্জ মাসের ১ম দশকের সুন্নাত আমল")
                if (hDay == 8) events.add("পবিত্র হজ্জ শুরু (৮ জিলহজ্জ)")
                if (hDay == 9) events.add("ইয়াওমে আরাফাহ (আরাফাহর রোজা)")
                if (hDay == 10) events.add("পবিত্র ঈদুল আজহা (১০ জিলহজ্জ)")
                if (hDay in 11..13) events.add("আইয়ামে তাশরীক (কোরবানির দিনসমূহ)")
            }
        }

        // Sunnah Fasting Rules:
        // 1. Mondays & Thursdays
        // 2. Ayyam al-Beed (13th, 14th, 15th of each Hijri month)
        // 3. 9th & 10th Muharram, 9th Dhul-Hijjah
        var isSunnah = false
        var sunnahReason: String? = null

        val dayOfWeek = date.dayOfWeek.value // 1 = Monday, 4 = Thursday
        if (hDay in 13..15) {
            isSunnah = true
            sunnahReason = "আইয়ামে বীজ (হিজরী চাঁদের ১৩, ১৪, ১৫ তারিখের নফল রোজা)"
        } else if (dayOfWeek == 1) {
            isSunnah = true
            sunnahReason = "সোমবারে রাসূলুল্লাহ (সা:)-এর সুন্নাত রোজা"
        } else if (dayOfWeek == 4) {
            isSunnah = true
            sunnahReason = "বৃহস্পতিবারের সুন্নাত রোজা"
        } else if (hMonth == 12 && hDay == 9) {
            isSunnah = true
            sunnahReason = "ইয়াওমে আরাফাহর ফজিলতপূর্ণ রোজা"
        } else if (hMonth == 1 && (hDay == 9 || hDay == 10)) {
            isSunnah = true
            sunnahReason = "আশুরার রোজা"
        }

        return HijriDateInfo(
            gregorianDate = date,
            hijriDay = hDay,
            hijriMonth = hMonth,
            hijriYear = hYear,
            hijriMonthNameBn = monthBn,
            hijriMonthNameAr = monthAr,
            specialEvents = events,
            isSunnahFast = isSunnah,
            sunnahFastReason = sunnahReason
        )
    }

    fun getBanglaDateStr(date: LocalDate, includeSuffix: Boolean = true): String {
        val year = date.year
        val isLeapYear = date.isLeapYear

        // Check if date is before 14th April
        val banglaYear = if (date.monthValue < 4 || (date.monthValue == 4 && date.dayOfMonth < 14)) {
            year - 594
        } else {
            year - 593
        }

        val monthDays = if (isLeapYear) {
            intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 31, 30)
        } else {
            intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 30)
        }

        val banglaMonths = listOf(
            "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
            "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
        )

        // Reference start: April 14 is 1st Boishakh
        val boishakhStart = LocalDate.of(year, 4, 14)
        val dayDiff = if (!date.isBefore(boishakhStart)) {
            java.time.temporal.ChronoUnit.DAYS.between(boishakhStart, date).toInt()
        } else {
            val prevBoishakhStart = LocalDate.of(year - 1, 4, 14)
            java.time.temporal.ChronoUnit.DAYS.between(prevBoishakhStart, date).toInt()
        }

        var remainingDays = dayDiff
        var mIndex = 0
        while (mIndex < 12 && remainingDays >= monthDays[mIndex]) {
            remainingDays -= monthDays[mIndex]
            mIndex++
        }

        val bDay = remainingDays + 1
        val bMonth = banglaMonths.getOrElse(mIndex % 12) { "" }

        val suffixStr = if (includeSuffix) " বঙ্গাব্দ" else ""
        return "${toBengaliNumerals(bDay)} $bMonth ${toBengaliNumerals(banglaYear)}$suffixStr"
    }

    fun toBengaliNumerals(num: Int): String {
        val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val sb = StringBuilder()
        num.toString().forEach { ch ->
            if (ch in '0'..'9') {
                sb.append(bengaliDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun toArabicNumerals(num: Int): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder()
        num.toString().forEach { ch ->
            if (ch in '0'..'9') {
                sb.append(arabicDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun getBengaliWeekdayName(date: LocalDate): String {
        return when (date.dayOfWeek.value) {
            1 -> "সোমবার"
            2 -> "মঙ্গলবার"
            3 -> "বুধবার"
            4 -> "বৃহস্পতিবার"
            5 -> "শুক্রবার"
            6 -> "শনিবার"
            7 -> "রবিবার"
            else -> ""
        }
    }

    fun getBengaliMonthName(monthVal: Int): String {
        return when (monthVal) {
            1 -> "জানুয়ারি"
            2 -> "ফেব্রুয়ারি"
            3 -> "মার্চ"
            4 -> "এপ্রিল"
            5 -> "মে"
            6 -> "জুন"
            7 -> "জুলাই"
            8 -> "আগস্ট"
            9 -> "সেপ্টেম্বর"
            10 -> "অক্টোবর"
            11 -> "নভেম্বর"
            12 -> "ডিসেম্বর"
            else -> ""
        }
    }
}
