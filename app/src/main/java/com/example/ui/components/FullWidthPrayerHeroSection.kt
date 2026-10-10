package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerName
import com.example.data.model.SinglePrayerTime
import com.example.utils.DateUtil
import com.example.utils.HijriCalendarUtil
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Instant
import java.time.ZonedDateTime

/**
 * MASTER PROMPT SPECIFICATION — Prayer Timetable Hero Section
 * Exactly reproduces reference design with three main horizontal sections:
 * 1. Top Information Header (Date | Location | Sunrise)
 * 2. Five-Column Prayer Timetable (Fajr, Dhuhr, Asr, Maghrib, Isha)
 *    - With live relative duration ("শুরু হয়েছে X মিনিট আগে" on active, and "ওয়াক্ত শেষ হতে X ঘণ্টা Y মিনিট" on upcoming)
 * 3. Bottom Dark Emerald-Green Prayer Status Panel
 *    - Single-row layout: Left (Mosque icon + Current Prayer Name), Right (Two capsules: Started Ago + Next Prayer Countdown & Arrow)
 *    - Islamic mosque silhouettes subtly embedded in background
 * 4. Sehri & Iftar Card matching exact height of running prayer waqt panel
 */
@Composable
fun FullWidthPrayerHeroSection(
    prayerSchedule: DailyPrayerSchedule,
    hijriOffset: Int,
    isEnglish: Boolean = false,
    extraBottomPadding: Dp = 10.dp,
    onPrayerTimesClick: () -> Unit,
    onLocationClick: () -> Unit,
    onCalendarClick: () -> Unit = {}
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    // -------------------------------------------------------------------------
    // Dates calculation
    // -------------------------------------------------------------------------
    val zoneId = remember(prayerSchedule) {
        try {
            java.time.ZoneId.of(prayerSchedule.district.timeZoneId)
        } catch (e: Exception) {
            java.time.ZoneId.systemDefault()
        }
    }

    val today = remember(prayerSchedule, zoneId) {
        LocalDate.now(zoneId)
    }

    val dateLine1 = remember(today, hijriOffset, isEnglish) {
        val shortDay = DateUtil.getShortDayName(today, isEnglish)
        val hijriInfo = HijriCalendarUtil.getHijriDate(today, hijriOffset)
        if (isEnglish) {
            "$shortDay, ${hijriInfo.hijriDay} ${hijriInfo.hijriMonthNameEn}"
        } else {
            val hDayBn = DateUtil.toBengaliNumerals(hijriInfo.hijriDay)
            "$shortDay, $hDayBn ${hijriInfo.hijriMonthNameBn}"
        }
    }

    val dateLine2 = remember(today, isEnglish) {
        val day = today.dayOfMonth
        val monthIdx = today.monthValue - 1
        val banglaDayMonth = DateUtil.getBengaliDayAndMonthStr(today, isEnglish)
        if (isEnglish) {
            val monthsEn = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
            "$day ${monthsEn[monthIdx]}, $banglaDayMonth"
        } else {
            val monthsBn = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
            val dayBn = DateUtil.toBengaliNumerals(day)
            "$dayBn ${monthsBn[monthIdx]}, $banglaDayMonth"
        }
    }

    // -------------------------------------------------------------------------
    // Prayers, Sunrise & Sunset timestamps & calculation
    // -------------------------------------------------------------------------
    val mainPrayers = remember(prayerSchedule) {
        prayerSchedule.prayers.filter { it.name != PrayerName.SUNRISE }
    }

    val fajrItem = remember(mainPrayers) { mainPrayers.find { it.name == PrayerName.FAJR } }
    val dhuhrItem = remember(mainPrayers) { mainPrayers.find { it.name == PrayerName.DHUHR } }
    val asrItem = remember(mainPrayers) { mainPrayers.find { it.name == PrayerName.ASR } }
    val maghribItem = remember(mainPrayers) { mainPrayers.find { it.name == PrayerName.MAGHRIB } }
    val ishaItem = remember(mainPrayers) { mainPrayers.find { it.name == PrayerName.ISHA } }
    val sunrisePrayer = remember(prayerSchedule) { prayerSchedule.prayers.find { it.name == PrayerName.SUNRISE } }

    val fajrStart = fajrItem?.timestampMillis ?: 0L
    val sunriseMillis = sunrisePrayer?.timestampMillis ?: 0L
    val dhuhrStart = dhuhrItem?.timestampMillis ?: 0L
    val asrStart = asrItem?.timestampMillis ?: 0L
    val maghribStart = maghribItem?.timestampMillis ?: 0L // Maghrib start = Sunset
    val ishaStart = ishaItem?.timestampMillis ?: 0L

    // Match core module's calculated prayerSchedule for consistency
    val currentPrayerItem: SinglePrayerTime? = remember(prayerSchedule, mainPrayers, currentTimeMillis, fajrStart, sunriseMillis, dhuhrStart, asrStart, maghribStart, ishaStart) {
        if (sunriseMillis > 0L && dhuhrStart > 0L && currentTimeMillis in sunriseMillis until dhuhrStart) {
            null // Between Sunrise and Dhuhr (Chasht / Duha or Makruh periods)
        } else if (fajrStart > 0L && sunriseMillis > 0L && currentTimeMillis in fajrStart until sunriseMillis) {
            fajrItem
        } else if (dhuhrStart > 0L && asrStart > 0L && currentTimeMillis in dhuhrStart until asrStart) {
            dhuhrItem
        } else if (asrStart > 0L && maghribStart > 0L && currentTimeMillis in asrStart until maghribStart) {
            asrItem
        } else if (maghribStart > 0L && ishaStart > 0L && currentTimeMillis in maghribStart until ishaStart) {
            maghribItem
        } else if (ishaStart > 0L && (currentTimeMillis >= ishaStart || (fajrStart > 0L && currentTimeMillis < fajrStart))) {
            ishaItem
        } else {
            val candidate = prayerSchedule.currentPrayer ?: mainPrayers.firstOrNull { it.isCurrent }
            if (candidate?.name == PrayerName.SUNRISE) null else candidate
        }
    }

    // End time of current prayer
    val currentPrayerEndMillis: Long = remember(currentPrayerItem, sunriseMillis, asrStart, maghribStart, ishaStart, fajrStart, currentTimeMillis) {
        when (currentPrayerItem?.name) {
            PrayerName.FAJR -> sunriseMillis
            PrayerName.DHUHR -> asrStart
            PrayerName.ASR -> maghribStart
            PrayerName.MAGHRIB -> ishaStart
            PrayerName.ISHA -> {
                if (currentTimeMillis >= ishaStart && ishaStart > 0L) {
                    fajrStart + 24 * 3600 * 1000L
                } else if (fajrStart > 0L && currentTimeMillis < fajrStart) {
                    fajrStart
                } else {
                    fajrStart + 24 * 3600 * 1000L
                }
            }
            else -> 0L
        }
    }

    // Remaining time of current prayer
    val currentPrayerRemainingMillis: Long = if (currentPrayerEndMillis > currentTimeMillis) {
        currentPrayerEndMillis - currentTimeMillis
    } else 0L

    // Next upcoming obligatory prayer and start timestamp
    val (nextPrayerItem, nextPrayerStartMillis) = remember(prayerSchedule, mainPrayers, fajrStart, sunriseMillis, dhuhrStart, asrStart, maghribStart, ishaStart, currentTimeMillis) {
        val nextCandidate = prayerSchedule.nextPrayer
            ?: mainPrayers.firstOrNull { it.isNext }
            ?: run {
                when {
                    currentTimeMillis < fajrStart -> fajrItem ?: mainPrayers.first()
                    currentTimeMillis < sunriseMillis -> dhuhrItem ?: mainPrayers.first()
                    currentTimeMillis < dhuhrStart -> dhuhrItem ?: mainPrayers.first()
                    currentTimeMillis < asrStart -> asrItem ?: mainPrayers.first()
                    currentTimeMillis < maghribStart -> maghribItem ?: mainPrayers.first()
                    currentTimeMillis < ishaStart -> ishaItem ?: mainPrayers.first()
                    else -> fajrItem ?: mainPrayers.first()
                }
            }
        val targetMillis = if (nextCandidate.timestampMillis > currentTimeMillis) {
            nextCandidate.timestampMillis
        } else {
            if (nextCandidate.timestampMillis > 0L) nextCandidate.timestampMillis + 24 * 3600 * 1000L
            else fajrStart + 24 * 3600 * 1000L
        }
        Pair(nextCandidate, targetMillis)
    }

    val nextStartsInMillis = (nextPrayerStartMillis - currentTimeMillis).coerceAtLeast(0L)

    val currentZdt = remember(currentTimeMillis, zoneId) {
        try {
            Instant.ofEpochMilli(currentTimeMillis).atZone(zoneId)
        } catch (e: Exception) {
            ZonedDateTime.now(zoneId)
        }
    }
    val currentLocalDate = currentZdt.toLocalDate()
    val currentLocalTime = currentZdt.toLocalTime()
    val currentMinutes = currentLocalTime.hour * 60 + currentLocalTime.minute

    val sunriseMinutes = remember(sunriseMillis, zoneId) {
        if (sunriseMillis > 0L) {
            try {
                val zdt = Instant.ofEpochMilli(sunriseMillis).atZone(zoneId)
                zdt.hour * 60 + zdt.minute
            } catch (e: Exception) { 6 * 60 }
        } else 6 * 60
    }
    val duhaStartMinutes = sunriseMinutes + 16

    val dhuhrMinutes = remember(dhuhrStart, zoneId) {
        if (dhuhrStart > 0L) {
            try {
                val zdt = Instant.ofEpochMilli(dhuhrStart).atZone(zoneId)
                zdt.hour * 60 + zdt.minute
            } catch (e: Exception) { 12 * 60 }
        } else 12 * 60
    }
    val duhaEndMinutes = (dhuhrMinutes - 4).coerceAtLeast(duhaStartMinutes)

    // -------------------------------------------------------------------------
    // Forbidden Prayer Times (মাকরূহ / নিষিদ্ধ সময়) & Duha Calculation
    // -------------------------------------------------------------------------
    val isSunriseMakruhNow = sunriseMillis > 0L && currentTimeMillis in sunriseMillis until (sunriseMillis + 16 * 60 * 1000L)
    val isZawalMakruhNow = dhuhrStart > 0L && currentTimeMillis in (dhuhrStart - 12 * 60 * 1000L) until dhuhrStart
    val isSunsetMakruhNow = maghribStart > 0L && currentTimeMillis in (maghribStart - 15 * 60 * 1000L) until maghribStart
    val isDuhaNow = !isSunriseMakruhNow && !isZawalMakruhNow && (currentMinutes in duhaStartMinutes until duhaEndMinutes)

    val isForbiddenTimeNow = isSunriseMakruhNow || isZawalMakruhNow || isSunsetMakruhNow || prayerSchedule.isForbiddenTimeNow

    val forbiddenReasonText = remember(currentTimeMillis, sunriseMillis, dhuhrStart, maghribStart, prayerSchedule.forbiddenTimeReason, isEnglish) {
        when {
            isSunriseMakruhNow -> if (isEnglish) "Prohibited: Sunrise" else "সূর্যোদয় (মাকরূহ)"
            isZawalMakruhNow -> if (isEnglish) "Prohibited: Midday" else "দ্বিপ্রহর (জাওয়াল)"
            isSunsetMakruhNow -> if (isEnglish) "Prohibited: Sunset" else "সূর্যাস্ত (মাকরূহ)"
            else -> prayerSchedule.forbiddenTimeReason ?: if (isEnglish) "Prohibited Prayer Time" else "নামাজের নিষিদ্ধ সময়"
        }
    }

    val forbiddenTimeRange = remember(currentTimeMillis, sunriseMillis, dhuhrStart, maghribStart, prayerSchedule, isEnglish) {
        val raw = when {
            isSunriseMakruhNow -> prayerSchedule.forbiddenMorningRange
            isZawalMakruhNow -> prayerSchedule.forbiddenNoonRange
            isSunsetMakruhNow -> prayerSchedule.forbiddenEveningRange
            else -> ""
        }
        if (isEnglish) toEnglishDigits(raw) else raw
    }

    val (overrideTitle, overrideTimeRange) = remember(
        isSunriseMakruhNow,
        isDuhaNow,
        isZawalMakruhNow,
        isSunsetMakruhNow,
        prayerSchedule,
        forbiddenReasonText,
        forbiddenTimeRange,
        isEnglish
    ) {
        when {
            isSunriseMakruhNow -> Pair(forbiddenReasonText, forbiddenTimeRange)
            isDuhaNow -> {
                val title = if (isEnglish) "Chasht & Duha" else "চাশত ও দুহা"
                val rawRange = prayerSchedule.duhaRange.ifBlank { "সকাল থেকে দ্বিপ্রহর" }
                val range = if (isEnglish) toEnglishDigits(rawRange) else rawRange
                Pair(title, range)
            }
            isZawalMakruhNow -> Pair(forbiddenReasonText, forbiddenTimeRange)
            isSunsetMakruhNow -> Pair(forbiddenReasonText, forbiddenTimeRange)
            else -> Pair("", "")
        }
    }

    // -------------------------------------------------------------------------
    // User Specification for Sun Time Display
    // -------------------------------------------------------------------------
    val sunriseLocalTime = remember(sunriseMillis, zoneId) {
        if (sunriseMillis > 0L) {
            try {
                Instant.ofEpochMilli(sunriseMillis).atZone(zoneId).toLocalTime()
            } catch (e: Exception) {
                LocalTime.of(5, 45)
            }
        } else {
            LocalTime.of(5, 45)
        }
    }

    val sunsetLocalTime = remember(maghribStart, zoneId) {
        if (maghribStart > 0L) {
            try {
                Instant.ofEpochMilli(maghribStart).atZone(zoneId).toLocalTime()
            } catch (e: Exception) {
                LocalTime.of(17, 45)
            }
        } else {
            LocalTime.of(17, 45)
        }
    }

    val isDaytime = currentLocalTime >= sunriseLocalTime && currentLocalTime < sunsetLocalTime
    val showSunrise = !isDaytime

    // Dynamic sun time label, digits, and relative countdown text
    val (sunTitle, sunDigits, sunRelativeText) = remember(
        showSunrise,
        currentTimeMillis,
        currentLocalDate,
        currentLocalTime,
        sunriseLocalTime,
        sunsetLocalTime,
        sunrisePrayer,
        maghribItem,
        isEnglish
    ) {
        if (showSunrise) {
            val title = if (isEnglish) "Sunrise" else "সূর্যোদয়"
            val rawDigits = sunrisePrayer?.timeDigits ?: prayerSchedule.sunriseTimeDigits.ifEmpty { "০৫:৪৫" }
            val formattedDigits = if (isEnglish) toEnglishDigits(rawDigits) else rawDigits

            val targetSunriseMillis = if (currentLocalTime >= sunsetLocalTime) {
                LocalDateTime.of(currentLocalDate.plusDays(1), sunriseLocalTime)
                    .atZone(zoneId).toInstant().toEpochMilli()
            } else {
                LocalDateTime.of(currentLocalDate, sunriseLocalTime)
                    .atZone(zoneId).toInstant().toEpochMilli()
            }

            val diff = (targetSunriseMillis - currentTimeMillis).coerceAtLeast(0L)
            val diffMins = diff / (60 * 1000)
            val hrs = diffMins / 60
            val mins = diffMins % 60
            val relText = if (isEnglish) {
                if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
            } else {
                val hBn = DateUtil.toBengaliNumerals(hrs)
                val mBn = DateUtil.toBengaliNumerals(mins)
                if (hrs > 0) "${hBn} ঘণ্টা বাকি" else "${mBn} মিনিট বাকি"
            }
            Triple(title, formattedDigits, relText)
        } else {
            val title = if (isEnglish) "Sunset" else "সূর্যাস্ত"
            val rawDigits = maghribItem?.timeDigits ?: prayerSchedule.sunsetTimeDigits.ifEmpty { "০৫:৪৫" }
            val formattedDigits = if (isEnglish) toEnglishDigits(rawDigits) else rawDigits

            val todaySunsetMillis = LocalDateTime.of(currentLocalDate, sunsetLocalTime)
                .atZone(zoneId).toInstant().toEpochMilli()
            val diff = (todaySunsetMillis - currentTimeMillis).coerceAtLeast(0L)
            val diffMins = diff / (60 * 1000)
            val hrs = diffMins / 60
            val mins = diffMins % 60
            val relText = if (isEnglish) {
                if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
            } else {
                val hBn = DateUtil.toBengaliNumerals(hrs)
                val mBn = DateUtil.toBengaliNumerals(mins)
                if (hrs > 0) "${hBn} ঘণ্টা বাকি" else "${mBn} মিনিট বাকি"
            }
            Triple(title, formattedDigits, relText)
        }
    }

    val isAfterSunrise = currentTimeMillis in sunriseMillis until (if (dhuhrStart > 0L) dhuhrStart else (sunriseMillis + 6 * 3600 * 1000L))

    // -------------------------------------------------------------------------
    // Capsule 1: Current Waqt Remaining Dynamic Time (বর্তমান ওয়াক্ত কত মিনিট বাকি)
    // -------------------------------------------------------------------------
    val (currentWaqtLabel, currentWaqtTimeText) = remember(
        currentPrayerItem,
        currentPrayerRemainingMillis,
        isSunriseMakruhNow,
        isDuhaNow,
        isZawalMakruhNow,
        isSunsetMakruhNow,
        currentMinutes,
        duhaStartMinutes,
        duhaEndMinutes,
        dhuhrMinutes,
        maghribStart,
        currentTimeMillis,
        isEnglish
    ) {
        when {
            isSunriseMakruhNow -> {
                val label = if (isEnglish) "Chasht Starts" else "চাশত শুরু"
                val remainingMins = (duhaStartMinutes - currentMinutes).coerceAtLeast(0)
                val hrs = remainingMins / 60
                val mins = remainingMins % 60
                val text = if (isEnglish) {
                    if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
                } else {
                    val hrsBn = DateUtil.toBengaliNumerals(hrs)
                    val minsBn = DateUtil.toBengaliNumerals(mins)
                    if (hrs > 0) "${hrsBn} ঘ. ${minsBn} মি. পর" else "${minsBn} মিনিট পর"
                }
                Pair(label, text)
            }
            isDuhaNow -> {
                val label = if (isEnglish) "Waqt Remaining" else "ওয়াক্ত বাকি"
                val remainingMins = (duhaEndMinutes - currentMinutes).coerceAtLeast(0)
                val hrs = remainingMins / 60
                val mins = remainingMins % 60
                val text = if (isEnglish) {
                    if (hrs > 0) "${hrs}h ${mins}m left" else "${mins}m left"
                } else {
                    val hrsBn = DateUtil.toBengaliNumerals(hrs)
                    val minsBn = DateUtil.toBengaliNumerals(mins)
                    if (hrs > 0) "${hrsBn} ঘণ্টা ${minsBn} মি." else "${minsBn} মিনিট বাকি"
                }
                Pair(label, text)
            }
            isZawalMakruhNow -> {
                val label = if (isEnglish) "Dhuhr Starts" else "যোহর শুরু"
                val remainingMins = (dhuhrMinutes - currentMinutes).coerceAtLeast(0)
                val hrs = remainingMins / 60
                val mins = remainingMins % 60
                val text = if (isEnglish) {
                    if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
                } else {
                    val hrsBn = DateUtil.toBengaliNumerals(hrs)
                    val minsBn = DateUtil.toBengaliNumerals(mins)
                    if (hrs > 0) "${hrsBn} ঘ. ${minsBn} মি. পর" else "${minsBn} মিনিট পর"
                }
                Pair(label, text)
            }
            isSunsetMakruhNow -> {
                val label = if (isEnglish) "Maghrib Starts" else "মাগরিব শুরু"
                val remainingMins = (maghribStart - currentTimeMillis).coerceAtLeast(0L) / (1000 * 60)
                val hrs = (remainingMins / 60).toInt()
                val mins = (remainingMins % 60).toInt()
                val text = if (isEnglish) {
                    if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
                } else {
                    val hrsBn = DateUtil.toBengaliNumerals(hrs)
                    val minsBn = DateUtil.toBengaliNumerals(mins)
                    if (hrs > 0) "${hrsBn} ঘ. ${minsBn} মি. পর" else "${minsBn} মিনিট পর"
                }
                Pair(label, text)
            }
            currentPrayerItem != null -> {
                val totalMins = currentPrayerRemainingMillis / (1000 * 60)
                val hrs = totalMins / 60
                val mins = totalMins % 60
                val label = if (isEnglish) "Waqt Remaining" else "ওয়াক্ত বাকি"
                val text = if (isEnglish) {
                    if (hrs > 0) "${hrs}h ${mins}m left" else "${mins}m left"
                } else {
                    val hrsBn = DateUtil.toBengaliNumerals(hrs)
                    val minsBn = DateUtil.toBengaliNumerals(mins)
                    if (hrs > 0) "${hrsBn} ঘণ্টা ${minsBn} মি." else "${minsBn} মিনিট বাকি"
                }
                Pair(label, text)
            }
            else -> {
                val label = if (isEnglish) "Current Status" else "বর্তমান অবস্থা"
                val text = if (isEnglish) "Tahajjud" else "তাহাজ্জুদ"
                Pair(label, text)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Capsule 2: Next Prayer Starts in Dynamic Countdown (পরবর্তী ওয়াক্ত কত মিনিট পর শুরু)
    // -------------------------------------------------------------------------
    val (nextPrayerLabel, countdownFormatted) = remember(nextPrayerItem, nextStartsInMillis, isEnglish) {
        val totalSec = nextStartsInMillis / 1000
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val nextName = nextPrayerItem.getDisplayName(isEnglish)
        if (isEnglish) {
            val label = "Next Prayer"
            val text = "$nextName in ${if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"}"
            Pair(label, text)
        } else {
            val label = "পরবর্তী নামাজ"
            val hrsBn = DateUtil.toBengaliNumerals(hrs)
            val minsBn = DateUtil.toBengaliNumerals(mins)
            val text = if (hrs > 0) "$nextName ${hrsBn} ঘ. ${minsBn} মি. পর" else "$nextName ${minsBn} মিনিট পর"
            Pair(label, text)
        }
    }

    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Outer container with overlapping RamadanSehriIftarCard at the bottom
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Outer Main Card — Islamic Emerald Theme with 100% full-width profile
        // Light mode features rich emerald Islamic green background with soft border,
        // and dark mode maintains serene deep dark emerald surface.
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 25.dp),
            shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) {
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF061E14),
                                    Color(0xFF03140D)
                                )
                            )
                        } else {
                            // Rich Islamic Emerald Green shape in light mode
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF094E35),
                                    Color(0xFF063B28)
                                )
                            )
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color(0xFF16442E).copy(alpha = 0.6f) else Color(0xFF1B7351).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 28.dp + extraBottomPadding)
                ) {
                    // =================================================================
                    // SECTION 1: TOP INFORMATION HEADER (Date | Location | Sunrise/Sunset)
                    // =================================================================
                    TopInformationHeader(
                        dateLine1 = dateLine1,
                        dateLine2 = dateLine2,
                        districtName = if (isEnglish) prayerSchedule.district.nameEn else prayerSchedule.district.nameBn,
                        sunTitle = sunTitle,
                        sunDigits = sunDigits,
                        sunRelativeText = sunRelativeText,
                        showSunrise = showSunrise,
                        isEnglish = isEnglish,
                        onCalendarClick = onCalendarClick,
                        onLocationClick = onLocationClick
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // =================================================================
                    // SECTION 2: FIVE-COLUMN PRAYER TIMETABLE
                    // =================================================================
                    FiveColumnPrayerTimetable(
                        mainPrayers = mainPrayers,
                        currentPrayerItem = currentPrayerItem,
                        isEnglish = isEnglish,
                        onPrayerTimesClick = onPrayerTimesClick
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    // =================================================================
                    // SECTION 3: BOTTOM DARK-GREEN / RED PRAYER STATUS PANEL (Running Waqt Card)
                    // =================================================================
                    DarkEmeraldPrayerStatusPanel(
                        currentPrayerItem = currentPrayerItem,
                        nextPrayerItem = nextPrayerItem,
                        currentWaqtLabel = currentWaqtLabel,
                        currentWaqtTimeText = currentWaqtTimeText,
                        nextPrayerLabel = nextPrayerLabel,
                        countdownFormatted = countdownFormatted,
                        isForbiddenTime = isForbiddenTimeNow,
                        forbiddenReasonText = forbiddenReasonText,
                        forbiddenTimeRange = forbiddenTimeRange,
                        overrideTitle = overrideTitle,
                        overrideTimeRange = overrideTimeRange,
                        isEnglish = isEnglish,
                        onClick = onPrayerTimesClick,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // =================================================================
        // SECTION 4: OVERLAPPING SEHRI & IFTAR STATUS CARD
        // Overlaps the bottom boundary like the search box
        // =================================================================
        RamadanSehriIftarCard(
            sahriDigits = prayerSchedule.sahriTimeDigits,
            iftarDigits = prayerSchedule.iftarTimeDigits,
            today = today,
            maghribStart = maghribStart,
            zoneId = zoneId,
            currentTimeMillis = currentTimeMillis,
            isEnglish = isEnglish,
            isDark = isDark,
            onClick = onPrayerTimesClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp)
        )
    }
}

// =============================================================================
// SUB-COMPONENTS
// =============================================================================

/**
 * SECTION 1: Top Information Header (Minimal, tight spacing, clean fonts)
 */
@Composable
private fun TopInformationHeader(
    dateLine1: String,
    dateLine2: String,
    districtName: String,
    sunTitle: String,
    sunDigits: String,
    sunRelativeText: String,
    showSunrise: Boolean,
    isEnglish: Boolean,
    onCalendarClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Theme palette:
    // Highlighted text: Light Green (Color(0xFF86EFAC) / Color(0xFFA7F3D0))
    // Normal / other text: White or soft white (Color.White / Color.White.copy(alpha = 0.85f))
    // Icons: Golden (Color(0xFFFFD54F) / Color(0xFFFBBF24))
    val goldenIconTint = Color(0xFFFFD54F)
    val lightGreenHighlight = Color(0xFF86EFAC)
    val textWhite = Color.White
    val textSoftWhite = Color.White.copy(alpha = 0.82f)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = if (isDark) Color(0xFF081F15).copy(alpha = 0.55f) else Color(0xFF032619).copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group 1: Left — Date: Line 1 (শুক্র, ২৬ রবিউস সানী) & Line 2 (১০ অক্টোবর, ২৪ আশ্বিন)
            Row(
                modifier = Modifier
                    .weight(1.35f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onCalendarClick() }
                    .padding(vertical = 1.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF0F3122) else Color(0xFF0F3B2A).copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = "Calendar",
                        tint = goldenIconTint,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                    Text(
                        text = dateLine1,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 13.sp,
                        color = textWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateLine2,
                        fontSize = 9.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSoftWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Divider 1
            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                thickness = 0.8.dp,
                color = Color.White.copy(alpha = 0.22f)
            )

            // Group 2: Center — Location (Pin + District + Dropdown)
            Row(
                modifier = Modifier
                    .weight(0.95f)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onLocationClick() }
                    .padding(vertical = 2.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = goldenIconTint,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = districtName,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = lightGreenHighlight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = goldenIconTint,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Divider 2
            VerticalDivider(
                modifier = Modifier
                    .height(20.dp)
                    .padding(horizontal = 2.dp),
                thickness = 0.8.dp,
                color = Color.White.copy(alpha = 0.22f)
            )

            // Group 3: Right — Sunrise / Sunset (Sun icon + time + relative text)
            Row(
                modifier = Modifier
                    .weight(1.15f)
                    .padding(vertical = 1.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDark) Color(0xFF2C2208) else Color(0xFF2A2812).copy(alpha = 0.7f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = sunTitle,
                        tint = goldenIconTint,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "$sunTitle: $sunDigits",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp,
                        color = textWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = sunRelativeText,
                        fontSize = 9.sp,
                        lineHeight = 10.sp,
                        color = lightGreenHighlight,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * SECTION 2: Five-Column Prayer Timetable (Enlarged icons, prayer names & digits, countdown removed)
 */
@Composable
private fun FiveColumnPrayerTimetable(
    mainPrayers: List<SinglePrayerTime>,
    currentPrayerItem: SinglePrayerTime?,
    isEnglish: Boolean,
    onPrayerTimesClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme() || MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPrayerTimesClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isDark) Color(0xFF071B12).copy(alpha = 0.7f) else Color(0xFF04271A).copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(
            0.8.dp,
            if (isDark) Color(0xFF163E2B).copy(alpha = 0.6f) else Color(0xFF1E6346).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mainPrayers.forEachIndexed { index, prayer ->
                val isCurrent = (currentPrayerItem?.name == prayer.name)

                PrayerColumnItem(
                    prayer = prayer,
                    isCurrent = isCurrent,
                    isEnglish = isEnglish,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Divider between columns
                if (index < mainPrayers.size - 1) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(38.dp)
                            .padding(vertical = 2.dp),
                        thickness = 0.6.dp,
                        color = Color.White.copy(alpha = 0.16f)
                    )
                }
            }
        }
    }
}

/**
 * Individual Prayer Column in the 5-column timetable
 * Colors follow user's design instructions:
 * - Highlighted text: Light Green (Color(0xFF86EFAC) / Color(0xFF6EE7B7))
 * - Other / secondary text: White or soft white (Color.White / Color.White.copy(alpha = 0.85f))
 * - Icons: Golden (Color(0xFFFFD54F) / Color(0xFFFBBF24))
 */
@Composable
private fun PrayerColumnItem(
    prayer: SinglePrayerTime,
    isCurrent: Boolean,
    isEnglish: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val timeDigits = if (isEnglish) {
        toEnglishDigits(prayer.timeDigits)
    } else {
        prayer.timeDigits
    }

    val goldenColor = Color(0xFFFFD54F)
    val lightGreenHighlight = Color(0xFF86EFAC)
    val textWhite = Color.White

    // Current active item card background: Subtle dark green tint with soft golden/green border
    val cardBg = if (isCurrent) {
        Color(0xFF0E3D29).copy(alpha = 0.85f)
    } else Color.Transparent

    val cardBorder = if (isCurrent) {
        Color(0xFF2E8B5B).copy(alpha = 0.75f)
    } else Color.Transparent

    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(cardBg)
            .border(
                width = if (isCurrent) 1.dp else 0.dp,
                color = cardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(vertical = 5.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Prayer Vector Icon (Golden color, enlarged for clear viewing)
            MinimalVectorPrayerIcon(
                prayerName = prayer.name,
                tint = if (isCurrent) goldenColor else goldenColor.copy(alpha = 0.82f),
                modifier = Modifier.size(16.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // 2. Prayer Name (e.g. "ফজর", "যোহর")
            Text(
                text = prayer.getDisplayName(isEnglish),
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) lightGreenHighlight else textWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // 3. Large Time Digits (e.g. "৪:৩১")
            Text(
                text = timeDigits,
                fontSize = 13.5.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) lightGreenHighlight else textWhite,
                maxLines = 1
            )
        }
    }
}

/**
 * SECTION 3: Bottom Dark-Green Prayer Status Panel
 * EXACT SINGLE-ROW DESIGN MATCHING SCREENSHOT:
 * Left: Mosque Vector Badge + Prayer Name (e.g. "ইশা") + Live Pulsing Dot • + Time Range "০৬:৫৪ - ০৪:৩৮"
 * Right: Two rounded outlined capsules side-by-side:
 *   - Capsule 1: Clock Icon + "ওয়াক্ত বাকি" + "৪২ মিনিট বাকি"
 *   - Capsule 2: Clock Icon + "পরবর্তী নামাজ" + "ফজর ৪ ঘ. ১২ মি. পর" + Arrow Right
 */
@Composable
private fun DarkEmeraldPrayerStatusPanel(
    currentPrayerItem: SinglePrayerTime?,
    nextPrayerItem: SinglePrayerTime,
    currentWaqtLabel: String,
    currentWaqtTimeText: String,
    nextPrayerLabel: String,
    countdownFormatted: String,
    isEnglish: Boolean,
    isForbiddenTime: Boolean = false,
    forbiddenReasonText: String = "",
    forbiddenTimeRange: String = "",
    overrideTitle: String = "",
    overrideTimeRange: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activePrayer = currentPrayerItem ?: nextPrayerItem
    val activePrayerName = activePrayer.getDisplayName(isEnglish)

    val startTimeStr = if (isEnglish) toEnglishDigits(activePrayer.timeDigits) else activePrayer.timeDigits
    val endTimeStr = if (isEnglish) toEnglishDigits(activePrayer.endTimeDigits) else activePrayer.endTimeDigits
    val activePrayerTimeRange = if (startTimeStr.isNotEmpty() && endTimeStr.isNotEmpty()) {
        "$startTimeStr - $endTimeStr"
    } else {
        startTimeStr
    }

    val displayTitle = when {
        overrideTitle.isNotEmpty() -> overrideTitle
        isForbiddenTime -> if (forbiddenReasonText.isNotEmpty()) forbiddenReasonText else if (isEnglish) "Prohibited Time" else "নামাজের নিষিদ্ধ সময়"
        else -> activePrayerName
    }

    val displayTimeRange = when {
        overrideTimeRange.isNotEmpty() -> overrideTimeRange
        isForbiddenTime && forbiddenTimeRange.isNotEmpty() -> forbiddenTimeRange
        else -> activePrayerTimeRange
    }

    val cardBg = if (isForbiddenTime) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF3D0810), // Deep Dark Crimson
                Color(0xFF5E0F1D), // Rich Wine Red
                Color(0xFF801627)  // Ruby Red highlight
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF063321), // Rich Dark Islamic Green
                Color(0xFF0A442D), // Deep Emerald
                Color(0xFF0F5438)  // Subtle Emerald highlight
            )
        )
    }

    val cardBorderColor = if (isForbiddenTime) {
        Color(0xFFF87171).copy(alpha = 0.6f)
    } else {
        Color(0xFF2E7D5B).copy(alpha = 0.4f)
    }

    val badgeTint = if (isForbiddenTime) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
    val titleTextColor = if (isForbiddenTime) Color(0xFFFECACA) else Color(0xFF86EFAC)
    val dotColor = if (isForbiddenTime) Color(0xFFEF4444) else Color(0xFF4ADE80)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBg)
            .border(
                width = 0.8.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        // Mosque silhouette in background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .matchParentSize()
        ) {
            val w = size.width
            val h = size.height
            val silhouetteColor = Color.Black.copy(alpha = 0.12f)
            val domePath = Path().apply {
                moveTo(w * 0.18f, h)
                lineTo(w * 0.18f, h * 0.45f)
                cubicTo(w * 0.20f, h * 0.35f, w * 0.23f, h * 0.22f, w * 0.25f, h * 0.12f)
                cubicTo(w * 0.27f, h * 0.22f, w * 0.30f, h * 0.35f, w * 0.32f, h * 0.45f)
                lineTo(w * 0.32f, h)
                close()
            }
            drawPath(domePath, silhouetteColor, style = Fill)

            val minaretPath = Path().apply {
                moveTo(w * 0.15f, h)
                lineTo(w * 0.15f, h * 0.25f)
                lineTo(w * 0.165f, h * 0.1f)
                lineTo(w * 0.18f, h * 0.25f)
                lineTo(w * 0.18f, h)
                close()
            }
            drawPath(minaretPath, silhouetteColor, style = Fill)
        }

        // STRICT SINGLE ROW: Left Info + Right Two Capsules
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT: Mosque Vector Outline + Active Waqt Name + Live Dot + Time Range
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                MosqueOutlineBadge(
                    modifier = Modifier.size(32.dp),
                    tint = badgeTint
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            fontSize = 16.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = titleTextColor
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        LiveIndicatorDot(color = dotColor)
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = displayTimeRange,
                        fontSize = 13.5.sp,
                        lineHeight = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            // RIGHT: Two capsules in the same row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Capsule 1: ওয়াক্ত বাকি ৪২ মিনিট / নামাজ নিষেধ
                DarkCapsuleButton(
                    icon = Icons.Outlined.AccessTime,
                    title = currentWaqtLabel,
                    subtitle = currentWaqtTimeText,
                    hasArrow = false,
                    isForbidden = isForbiddenTime
                )

                // Capsule 2: পরবর্তী নামাজ যোহর ২ ঘ. ১২ মি. পর >
                DarkCapsuleButton(
                    icon = Icons.Outlined.AccessTime,
                    title = nextPrayerLabel,
                    subtitle = countdownFormatted,
                    hasArrow = true,
                    isForbidden = isForbiddenTime
                )
            }
        }
    }
}

/**
 * Pulsing live indicator dot for running/active waqt (Liquid smooth radar pulse)
 */
@Composable
private fun LiveIndicatorDot(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4ADE80)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LiveDotRadarPulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )
    val coreAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "coreAlpha"
    )

    Box(
        modifier = modifier.size(14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer expanding and fading ripple ring
        val rippleScale = 0.6f + 0.9f * pulseProgress
        val rippleAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.5f
        Box(
            modifier = Modifier
                .size(13.dp * rippleScale)
                .clip(CircleShape)
                .background(color.copy(alpha = rippleAlpha))
        )
        // Middle ambient halo
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.25f * coreAlpha))
        )
        // Solid core dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = coreAlpha))
        )
    }
}

/**
 * Convert Bengali numerals to English numerals
 */
private fun toEnglishDigits(str: String): String {
    val bn = "০১২৩৪৫৬৭৮৯"
    val en = "0123456789"
    return str.map { c ->
        val idx = bn.indexOf(c)
        if (idx != -1) en[idx] else c
    }.joinToString("")
}

/**
 * Capsule button matching design:
 * Dark translucent pill with green/red border, clock icon, title on top, subtitle below, optional right chevron.
 */
@Composable
private fun DarkCapsuleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    hasArrow: Boolean,
    isForbidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val pillBg = if (isForbidden) Color(0xFF3A080F).copy(alpha = 0.88f) else Color(0xFF04281A).copy(alpha = 0.85f)
    val pillBorder = if (isForbidden) Color(0xFFF87171).copy(alpha = 0.7f) else Color(0xFF2E7D5B).copy(alpha = 0.75f)
    val iconTint = if (isForbidden) Color(0xFFFCA5A5) else Color(0xFFFFD54F)
    val subtitleColor = if (isForbidden) Color(0xFFFECACA) else Color(0xFF86EFAC)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(pillBg)
            .border(
                width = 0.8.dp,
                color = pillBorder,
                shape = RoundedCornerShape(100.dp)
            )
            .padding(horizontal = 7.dp, vertical = 3.5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock icon
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(0.8.dp, iconTint.copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(10.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 7.8.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 8.8.sp,
                    lineHeight = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasArrow) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Phosphor Mosque Bold Icon
 * https://composables.com/icons/icon-libraries/phosphor/mosque?v=bold
 */
@Composable
private fun MosqueOutlineBadge(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFFFD54F)
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_phosphor_mosque_bold),
        contentDescription = "Mosque",
        tint = tint,
        modifier = modifier
    )
}

/**
 * Clean vector icons for Fajr, Dhuhr, Asr, Maghrib, Isha
 */
@Composable
private fun MinimalVectorPrayerIcon(
    prayerName: PrayerName,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (prayerName) {
            PrayerName.FAJR -> {
                drawLine(
                    color = tint,
                    start = Offset(0f, h * 0.75f),
                    end = Offset(w, h * 0.75f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.22f, h * 0.35f),
                    size = Size(w * 0.56f, h * 0.56f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.12f), Offset(w * 0.5f, h * 0.26f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(tint, Offset(w * 0.22f, h * 0.26f), Offset(w * 0.32f, h * 0.38f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(tint, Offset(w * 0.78f, h * 0.26f), Offset(w * 0.68f, h * 0.38f), 1.dp.toPx(), StrokeCap.Round)
            }
            PrayerName.DHUHR, PrayerName.SUNRISE -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.24f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                val rayLen = w * 0.14f
                for (i in 0 until 6) {
                    val angle = Math.toRadians((i * 60).toDouble())
                    val startR = w * 0.32f
                    val endR = startR + rayLen
                    drawLine(
                        color = tint,
                        start = Offset((center.x + startR * Math.cos(angle)).toFloat(), (center.y + startR * Math.sin(angle)).toFloat()),
                        end = Offset((center.x + endR * Math.cos(angle)).toFloat(), (center.y + endR * Math.sin(angle)).toFloat()),
                        strokeWidth = 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            PrayerName.ASR -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.22f,
                    center = Offset(w * 0.36f, h * 0.45f),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.15f, h * 0.85f),
                    end = Offset(w * 0.85f, h * 0.85f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(tint, Offset(w * 0.60f, h * 0.40f), Offset(w * 0.82f, h * 0.58f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(tint, Offset(w * 0.52f, h * 0.22f), Offset(w * 0.68f, h * 0.32f), 1.dp.toPx(), StrokeCap.Round)
            }
            PrayerName.MAGHRIB -> {
                drawLine(
                    color = tint,
                    start = Offset(0f, h * 0.65f),
                    end = Offset(w, h * 0.65f),
                    strokeWidth = 1.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = tint,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.25f, h * 0.40f),
                    size = Size(w * 0.5f, h * 0.5f),
                    style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawLine(tint, Offset(w * 0.5f, h * 0.15f), Offset(w * 0.5f, h * 0.38f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(tint, Offset(w * 0.42f, h * 0.30f), Offset(w * 0.5f, h * 0.38f), 1.dp.toPx(), StrokeCap.Round)
                drawLine(tint, Offset(w * 0.58f, h * 0.30f), Offset(w * 0.5f, h * 0.38f), 1.dp.toPx(), StrokeCap.Round)
            }
            PrayerName.ISHA -> {
                val moonPath = Path().apply {
                    moveTo(w * 0.55f, h * 0.2f)
                    cubicTo(w * 0.28f, h * 0.22f, w * 0.22f, h * 0.72f, w * 0.58f, h * 0.82f)
                    cubicTo(w * 0.38f, h * 0.75f, w * 0.36f, h * 0.32f, w * 0.55f, h * 0.2f)
                    close()
                }
                drawPath(moonPath, tint, style = Fill)
                drawCircle(tint, radius = 1.2.dp.toPx(), center = Offset(w * 0.70f, h * 0.32f))
            }
            else -> {
                drawCircle(
                    color = tint,
                    radius = w * 0.24f,
                    center = center,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }
        }
    }
}

/**
 * SECTION 4: Sehri & Iftar Card matching reference screenshot:
 * Three equal columns:
 * 1. Sahri time (e.g. "০৪:৩৬") + "পরবর্তী সাহরি"
 * 2. Iftar time (e.g. "০৫:৩৭") + "পরবর্তী ইফতার"
 * 3. Live countdown (e.g. "০১:১৮:২৩") + "ইফতারের বাকি" / "সাহরির বাকি"
 */
@Composable
private fun RamadanSehriIftarCard(
    sahriDigits: String,
    iftarDigits: String,
    today: LocalDate,
    maghribStart: Long,
    zoneId: java.time.ZoneId,
    currentTimeMillis: Long,
    isEnglish: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displaySahri = if (isEnglish) toEnglishDigits(sahriDigits) else sahriDigits
    val displayIftar = if (isEnglish) toEnglishDigits(iftarDigits) else iftarDigits

    // Calculate timestamps for today and tomorrow
    val todaySahriMillis = remember(today, sahriDigits, zoneId) {
        parseTimeToMillis(today, sahriDigits, isPm = false, zoneId = zoneId)
    }
    val todayIftarMillis = remember(today, iftarDigits, maghribStart, zoneId) {
        if (maghribStart > 0L) maghribStart
        else parseTimeToMillis(today, iftarDigits, isPm = true, zoneId = zoneId)
    }
    val tomorrowSahriMillis = remember(today, sahriDigits, zoneId) {
        parseTimeToMillis(today.plusDays(1), sahriDigits, isPm = false, zoneId = zoneId)
    }

    val (isIftarNext, targetMillis) = remember(currentTimeMillis, todaySahriMillis, todayIftarMillis, tomorrowSahriMillis) {
        if (currentTimeMillis < todaySahriMillis) {
            // Before today's Sahri: next event is today's Sahri
            Pair(false, todaySahriMillis)
        } else if (currentTimeMillis < todayIftarMillis) {
            // Daytime fast: next event is today's Iftar
            Pair(true, todayIftarMillis)
        } else {
            // After Iftar: next event is tomorrow's Sahri
            Pair(false, tomorrowSahriMillis)
        }
    }

    val countdownSecs = ((targetMillis - currentTimeMillis).coerceAtLeast(0L)) / 1000
    val cdHours = countdownSecs / 3600
    val cdMins = (countdownSecs % 3600) / 60
    val cdSecs = countdownSecs % 60

    val countdownStr = remember(cdHours, cdMins, cdSecs, isEnglish) {
        val hStr = String.format(java.util.Locale.US, "%02d", cdHours)
        val mStr = String.format(java.util.Locale.US, "%02d", cdMins)
        val sStr = String.format(java.util.Locale.US, "%02d", cdSecs)
        val raw = "$hStr:$mStr:$sStr"
        if (isEnglish) raw else DateUtil.toBengaliNumerals(raw)
    }

    val countdownSubtitle = remember(isIftarNext, isEnglish) {
        if (isIftarNext) {
            if (isEnglish) "Time to Iftar" else "ইফতারের বাকি"
        } else {
            if (isEnglish) "Time to Sahri" else "সাহরির বাকি"
        }
    }

    val cardBg = if (isDark) Color(0xFF0D0D0D) else Color.White
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE5E7EB)
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color(0xFFA1A1AA) else Color(0xFF374151)
    val dividerColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE5E7EB)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(14.dp),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.6f) else Color(0x33000000)
            )
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(
                width = 1.dp,
                color = cardBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Column 1: Sahri
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displaySahri,
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = if (isEnglish) "Next Sahri" else "পরবর্তী সাহরি",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                thickness = 0.8.dp,
                color = dividerColor
            )

            // Column 2: Iftar
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayIftar,
                    fontSize = 15.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = if (isEnglish) "Next Iftar" else "পরবর্তী ইফতার",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                thickness = 0.8.dp,
                color = dividerColor
            )

            // Column 3: Live Countdown
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = countdownStr,
                    fontSize = 14.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = countdownSubtitle,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = textSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Helper to parse time string like "০৪:৩৬" or "04:36" into timestamp millis for a given date
 */
private fun parseTimeToMillis(date: LocalDate, digits: String, isPm: Boolean, zoneId: java.time.ZoneId): Long {
    try {
        val enDigits = DateUtil.toEnglishNumerals(digits)
        val parts = enDigits.split(":")
        if (parts.size >= 2) {
            var h = parts[0].trim().toIntOrNull() ?: return 0L
            val m = parts[1].trim().toIntOrNull() ?: return 0L
            if (isPm && h < 12) h += 12
            else if (!isPm && h == 12) h = 0
            return date.atTime(h, m).atZone(zoneId).toInstant().toEpochMilli()
        }
    } catch (e: Exception) {
        // fallback
    }
    return 0L
}
