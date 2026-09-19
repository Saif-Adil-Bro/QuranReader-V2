package com.example.utils

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

object DateUtil {
    private val bengaliMonths = listOf("বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন", "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র")
    private val hijriMonths = listOf("মহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি", "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান", "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ")
    private val englishMonthsBengali = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
    private val daysOfWeekBengali = listOf("রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার")
    private val seasons = listOf("গ্রীষ্মকাল", "বর্ষাকাল", "শরৎকাল", "হেমন্তকাল", "শীতকাল", "বসন্তকাল")

    fun toBengaliNumerals(number: Int): String {
        return number.toString().map { 
            if (it in '0'..'9') (it - '0' + '০'.code).toChar() else it 
        }.joinToString("")
    }

    fun toBengaliNumerals(number: Long): String {
        return number.toString().map { 
            if (it in '0'..'9') (it - '0' + '০'.code).toChar() else it 
        }.joinToString("")
    }

    fun toBengaliNumerals(text: String): String {
        return text.map { 
            if (it in '0'..'9') (it - '0' + '০'.code).toChar() else it 
        }.joinToString("")
    }

    fun toArabicNumerals(number: Int): String {
        return number.toString().map { 
            if (it in '0'..'9') (it - '0' + '٠'.code).toChar() else it 
        }.joinToString("")
    }

    fun getTodayEnglishDateStr(): String {
        return formatDateStr(java.time.LocalDate.now())
    }

    fun formatDateStr(date: java.time.LocalDate): String {
        val dayOfWeekIndex = when (date.dayOfWeek) {
            java.time.DayOfWeek.SUNDAY -> 0
            java.time.DayOfWeek.MONDAY -> 1
            java.time.DayOfWeek.TUESDAY -> 2
            java.time.DayOfWeek.WEDNESDAY -> 3
            java.time.DayOfWeek.THURSDAY -> 4
            java.time.DayOfWeek.FRIDAY -> 5
            java.time.DayOfWeek.SATURDAY -> 6
        }
        val day = date.dayOfMonth
        val month = date.monthValue - 1
        val year = date.year
        return "${daysOfWeekBengali[dayOfWeekIndex]}, ${toBengaliNumerals(day)} ${englishMonthsBengali[month]} ${toBengaliNumerals(year)}"
    }

    fun getTodayHijriDateStr(hijriOffset: Int = 0): String {
        return try {
            val today = java.time.LocalDate.now()
            val info = HijriCalendarUtil.getHijriDate(today, hijriOffset)
            "${toBengaliNumerals(info.hijriDay)} ${info.hijriMonthNameBn} ${toBengaliNumerals(info.hijriYear)}"
        } catch (e: Exception) {
            "..."
        }
    }

    fun getHijriNoteStr(hijriOffset: Int = 0): String {
        return try {
            val today = java.time.LocalDate.now()
            val info = HijriCalendarUtil.getHijriDate(today, hijriOffset)
            val month = info.hijriMonth - 1
            val day = info.hijriDay
            if (month == 0 && day == 1) "(নতুন বছর শুরু)"
            else if (month == 8) "(রহমতের মাস)"
            else "(পবিত্র আরবি মাস)"
        } catch (e: Exception) {
            "(পবিত্র আরবি মাস)"
        }
    }

    fun getBengaliDateStr(date: java.time.LocalDate = java.time.LocalDate.now()): Pair<String, String> {
        val day = date.dayOfMonth
        val month = date.monthValue
        val year = date.year

        var bDay = 0
        var bMonth = 0
        var bYear = year - 593

        // Days in Gregorian months before mid-month where Bengali month changes
        val dateValue = month * 100 + day
        
        if (dateValue >= 414 && dateValue <= 514) { bMonth = 1; bDay = if (dateValue <= 430) day - 13 else day + 17; if(bDay > 31) bDay -= 31 }
        else if (dateValue >= 515 && dateValue <= 614) { bMonth = 2; bDay = if (dateValue <= 531) day - 14 else day + 17; if(bDay > 31) bDay -= 31 }
        else if (dateValue >= 615 && dateValue <= 715) { bMonth = 3; bDay = if (dateValue <= 630) day - 14 else day + 16; if(bDay > 31) bDay -= 31 }
        else if (dateValue >= 716 && dateValue <= 815) { bMonth = 4; bDay = if (dateValue <= 731) day - 15 else day + 16; if(bDay > 31) bDay -= 31 }
        else if (dateValue >= 816 && dateValue <= 915) { bMonth = 5; bDay = if (dateValue <= 831) day - 15 else day + 16; if(bDay > 31) bDay -= 31 }
        else if (dateValue >= 916 && dateValue <= 1015) { bMonth = 6; bDay = if (dateValue <= 930) day - 15 else day + 15; if(bDay > 31) bDay -= 31 }
        else if (dateValue >= 1016 && dateValue <= 1114) { bMonth = 7; bDay = if (dateValue <= 1031) day - 15 else day + 16; if(bDay > 30) bDay -= 30 }
        else if (dateValue >= 1115 && dateValue <= 1214) { bMonth = 8; bDay = if (dateValue <= 1130) day - 14 else day + 16; if(bDay > 30) bDay -= 30 }
        else if (dateValue >= 1215 || dateValue <= 113) { bMonth = 9; bDay = if (dateValue >= 1215) day - 14 else day + 17; if(dateValue <= 113) bYear -= 1; if(bDay > 30) bDay -= 30 }
        else if (dateValue >= 114 && dateValue <= 213) { bMonth = 10; bDay = if (dateValue <= 131) day - 13 else day + 18; bYear -= 1; if(bDay > 30) bDay -= 30 }
        else if (dateValue >= 214 && dateValue <= 314) { bMonth = 11; bDay = if (dateValue <= 229) day - 13 else day + 15; bYear -= 1; if(bDay > 30) bDay -= 30 }
        else if (dateValue >= 315 && dateValue <= 413) { bMonth = 12; bDay = if (dateValue <= 331) day - 14 else day + 17; bYear -= 1; if(bDay > 30) bDay -= 30 }

        if (bMonth == 0) bMonth = 1
        
        val seasonIndex = (bMonth - 1) / 2
        val season = if (seasonIndex in seasons.indices) seasons[seasonIndex] else seasons[0]

        return Pair("${toBengaliNumerals(bDay)} ${bengaliMonths[bMonth - 1]}, ${toBengaliNumerals(bYear)}", "(ঋতু: $season)")
    }

    fun getTodayBengaliDateStr(): Pair<String, String> {
        return getBengaliDateStr(java.time.LocalDate.now())
    }

    fun getShortDayNameBn(date: java.time.LocalDate): String {
        return when (date.dayOfWeek) {
            java.time.DayOfWeek.SATURDAY -> "শনি"
            java.time.DayOfWeek.SUNDAY -> "রবি"
            java.time.DayOfWeek.MONDAY -> "সোম"
            java.time.DayOfWeek.TUESDAY -> "মঙ্গল"
            java.time.DayOfWeek.WEDNESDAY -> "বুধ"
            java.time.DayOfWeek.THURSDAY -> "বৃহঃ"
            java.time.DayOfWeek.FRIDAY -> "শুক্র"
        }
    }

    fun getFullHeaderDateStr(date: java.time.LocalDate): String {
        val engMonth = englishMonthsBengali[date.monthValue - 1]
        val engDay = toBengaliNumerals(date.dayOfMonth)
        val engYear = toBengaliNumerals(date.year)
        val banglaDate = getBengaliDateStr(date).first
        return "$engDay $engMonth, $engYear • $banglaDate"
    }
}

fun Int.toArabicNumerals(): String = DateUtil.toArabicNumerals(this)
