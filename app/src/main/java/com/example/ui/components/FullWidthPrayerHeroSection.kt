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
    extraBottomPadding: Dp = 28.dp,
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
    val currentPrayerItem = remember(prayerSchedule, mainPrayers, currentTimeMillis) {
        prayerSchedule.currentPrayer
            ?: mainPrayers.firstOrNull { it.isCurrent }
            ?: run {
                when {
                    fajrStart > 0L && sunriseMillis > 0L && currentTimeMillis in fajrStart until sunriseMillis -> fajrItem
                    dhuhrStart > 0L && asrStart > 0L && currentTimeMillis in dhuhrStart until asrStart -> dhuhrItem
                    asrStart > 0L && maghribStart > 0L && currentTimeMillis in asrStart until maghribStart -> asrItem
                    maghribStart > 0L && ishaStart > 0L && currentTimeMillis in maghribStart until ishaStart -> maghribItem
                    ishaStart > 0L && (currentTimeMillis >= ishaStart || (fajrStart > 0L && currentTimeMillis < fajrStart)) -> ishaItem
                    else -> null // Between sunrise and Dhuhr (Ishraq / Chasht) or pre-Fajr
                }
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

    // -------------------------------------------------------------------------
    // User Specification for Sun Time Display:
    // - সূর্যাস্তের এক ঘণ্টা পর থেকে সূর্যোদয়ের এক ঘণ্টা পর পর্যন্ত সূর্যোদয়ের টাইম শো করবে।
    // - সূর্যোদয়ের এক ঘণ্টা পর থেকে সূর্যাস্তের এক ঘণ্টা পর পর্যন্ত সূর্যাস্তের টাইম শো করবে।
    // -------------------------------------------------------------------------
    val oneHourMillis = 3600 * 1000L
    val sunsetMillis = maghribStart

    val showSunrise = remember(currentTimeMillis, sunriseMillis, sunsetMillis) {
        if (sunriseMillis > 0L && sunsetMillis > 0L) {
            val sunsetPlus1h = sunsetMillis + oneHourMillis
            val sunrisePlus1h = sunriseMillis + oneHourMillis
            // After sunset+1h (night until midnight and early morning) or before sunrise+1h
            if (currentTimeMillis >= sunsetPlus1h || currentTimeMillis < sunrisePlus1h) {
                true
            } else {
                false
            }
        } else {
            true // default fallback
        }
    }

    // Dynamic sun time label, digits, and relative countdown/elapsed text
    val (sunTitle, sunDigits, sunRelativeText) = remember(showSunrise, currentTimeMillis, sunriseMillis, sunsetMillis, sunrisePrayer, maghribItem, isEnglish) {
        if (showSunrise) {
            val title = if (isEnglish) "Sunrise" else "সূর্যোদয়"
            val rawDigits = sunrisePrayer?.timeDigits ?: ""
            val formattedDigits = if (isEnglish) toEnglishDigits(rawDigits) else rawDigits

            val diff = currentTimeMillis - sunriseMillis
            val isPassed = diff >= 0
            val absDiff = Math.abs(diff)
            val diffMins = absDiff / (60 * 1000)
            val hrs = diffMins / 60
            val mins = diffMins % 60

            val relText = if (isEnglish) {
                if (isPassed) {
                    if (hrs > 0) "${hrs}h ${mins}m ago" else "${mins}m ago"
                } else {
                    if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
                }
            } else {
                val hBn = DateUtil.toBengaliNumerals(hrs)
                val mBn = DateUtil.toBengaliNumerals(mins)
                if (isPassed) {
                    if (hrs > 0) "${hBn} ঘণ্টা আগে" else "${mBn} মিনিট আগে"
                } else {
                    if (hrs > 0) "${hBn} ঘণ্টা বাকি" else "${mBn} মিনিট বাকি"
                }
            }
            Triple(title, formattedDigits, relText)
        } else {
            val title = if (isEnglish) "Sunset" else "সূর্যাস্ত"
            val rawDigits = maghribItem?.timeDigits ?: ""
            val formattedDigits = if (isEnglish) toEnglishDigits(rawDigits) else rawDigits

            val diff = currentTimeMillis - sunsetMillis
            val isPassed = diff >= 0
            val absDiff = Math.abs(diff)
            val diffMins = absDiff / (60 * 1000)
            val hrs = diffMins / 60
            val mins = diffMins % 60

            val relText = if (isEnglish) {
                if (isPassed) {
                    if (hrs > 0) "${hrs}h ${mins}m ago" else "${mins}m ago"
                } else {
                    if (hrs > 0) "in ${hrs}h ${mins}m" else "in ${mins}m"
                }
            } else {
                val hBn = DateUtil.toBengaliNumerals(hrs)
                val mBn = DateUtil.toBengaliNumerals(mins)
                if (isPassed) {
                    if (hrs > 0) "${hBn} ঘণ্টা আগে" else "${mBn} মিনিট আগে"
                } else {
                    if (hrs > 0) "${hBn} ঘণ্টা বাকি" else "${mBn} মিনিট বাকি"
                }
            }
            Triple(title, formattedDigits, relText)
        }
    }

    // Whether current time is in daytime after sunrise but before Dhuhr
    val isAfterSunrise = currentTimeMillis in sunriseMillis until (if (dhuhrStart > 0L) dhuhrStart else (sunriseMillis + 6 * 3600 * 1000L))

    // -------------------------------------------------------------------------
    // Capsule 1: Current Waqt Remaining Dynamic Time (বর্তমান ওয়াক্ত কত মিনিট বাকি)
    // -------------------------------------------------------------------------
    val (currentWaqtLabel, currentWaqtTimeText) = remember(currentPrayerItem, currentPrayerRemainingMillis, isAfterSunrise, isEnglish) {
        if (currentPrayerItem != null) {
            val totalMins = currentPrayerRemainingMillis / (1000 * 60)
            val hrs = totalMins / 60
            val mins = totalMins % 60
            if (isEnglish) {
                val label = "Waqt Remaining"
                val text = if (hrs > 0) "${hrs}h ${mins}m left" else "${mins}m left"
                Pair(label, text)
            } else {
                val label = "ওয়াক্ত বাকি"
                val hrsBn = DateUtil.toBengaliNumerals(hrs)
                val minsBn = DateUtil.toBengaliNumerals(mins)
                val text = if (hrs > 0) "${hrsBn} ঘণ্টা ${minsBn} মি." else "${minsBn} মিনিট বাকি"
                Pair(label, text)
            }
        } else {
            if (isEnglish) {
                Pair("Current Status", if (isAfterSunrise) "Ishraq / Chasht" else "Tahajjud")
            } else {
                Pair("বর্তমান অবস্থা", if (isAfterSunrise) "ইশরাক / চাশত" else "তাহাজ্জুদ")
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

    // Outer Main Card — Islamic Emerald Theme with 100% full-width profile
    // Light mode features rich emerald Islamic green background with soft border,
    // and dark mode maintains serene deep dark emerald surface.
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp + extraBottomPadding)
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
                    currentPrayerRemainingMillis = currentPrayerRemainingMillis,
                    currentTimeMillis = currentTimeMillis,
                    isEnglish = isEnglish,
                    onPrayerTimesClick = onPrayerTimesClick
                )

                Spacer(modifier = Modifier.height(4.dp))

                // =================================================================
                // SECTION 3: BOTTOM DARK-GREEN PRAYER STATUS PANEL (Exact Single-Row Layout)
                // =================================================================
                DarkEmeraldPrayerStatusPanel(
                    currentPrayerItem = currentPrayerItem,
                    nextPrayerItem = nextPrayerItem,
                    currentWaqtLabel = currentWaqtLabel,
                    currentWaqtTimeText = currentWaqtTimeText,
                    nextPrayerLabel = nextPrayerLabel,
                    countdownFormatted = countdownFormatted,
                    isEnglish = isEnglish,
                    onClick = onPrayerTimesClick,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // =================================================================
                // SECTION 4: SEHRI & IFTAR STATUS CARD (Matching reference image)
                // =================================================================
                RamadanSehriIftarCard(
                    sahriDigits = prayerSchedule.sahriTimeDigits,
                    iftarDigits = prayerSchedule.iftarTimeDigits,
                    today = today,
                    maghribStart = maghribStart,
                    zoneId = zoneId,
                    currentTimeMillis = currentTimeMillis,
                    isEnglish = isEnglish,
                    onClick = onPrayerTimesClick,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
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
 * SECTION 2: Five-Column Prayer Timetable (Minimal height, balanced gap, matching reference image)
 */
@Composable
private fun FiveColumnPrayerTimetable(
    mainPrayers: List<SinglePrayerTime>,
    currentPrayerItem: SinglePrayerTime?,
    currentPrayerRemainingMillis: Long,
    currentTimeMillis: Long,
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
                .padding(vertical = 3.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            mainPrayers.forEachIndexed { index, prayer ->
                val isCurrent = (currentPrayerItem?.name == prayer.name)

                // Dynamic relative status line 1 and line 2
                val (line1, line2) = remember(prayer, currentTimeMillis, isCurrent, currentPrayerRemainingMillis, isEnglish) {
                    if (isCurrent) {
                        val totalMins = currentPrayerRemainingMillis / (1000 * 60)
                        val hrs = totalMins / 60
                        val mins = totalMins % 60
                        if (isEnglish) {
                            val l1 = "Time Left"
                            val l2 = if (totalMins <= 0) "Ending soon" else if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m left"
                            Pair(l1, l2)
                        } else {
                            val l1 = "ওয়াক্ত বাকি"
                            val hrsBn = DateUtil.toBengaliNumerals(hrs)
                            val minsBn = DateUtil.toBengaliNumerals(mins)
                            val l2 = if (totalMins <= 0) "শেষ পর্যায়ে" else if (hrs > 0) "${hrsBn} ঘ. ${minsBn} মি." else "${minsBn} মিনিট"
                            Pair(l1, l2)
                        }
                    } else {
                        val diffMillis = prayer.timestampMillis - currentTimeMillis
                        if (diffMillis > 0) {
                            val totalMins = diffMillis / (1000 * 60)
                            val hrs = totalMins / 60
                            val mins = totalMins % 60
                            if (isEnglish) {
                                val l1 = "Starts in"
                                val l2 = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
                                Pair(l1, l2)
                            } else {
                                val l1 = "শুরু হতে বাকি"
                                val hrsBn = DateUtil.toBengaliNumerals(hrs)
                                val minsBn = DateUtil.toBengaliNumerals(mins)
                                val l2 = if (hrs > 0) "${hrsBn} ঘ. ${minsBn} মি." else "${minsBn} মিনিট"
                                Pair(l1, l2)
                            }
                        } else {
                            if (isEnglish) {
                                Pair("Status", "Ended")
                            } else {
                                Pair("ওয়াক্ত", "সময় শেষ")
                            }
                        }
                    }
                }

                PrayerColumnItem(
                    prayer = prayer,
                    isCurrent = isCurrent,
                    statusLine1 = line1,
                    statusLine2 = line2,
                    isEnglish = isEnglish,
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )

                // Divider between columns
                if (index < mainPrayers.size - 1) {
                    VerticalDivider(
                        modifier = Modifier
                            .height(48.dp)
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
    statusLine1: String,
    statusLine2: String,
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
    val textSoftWhite = Color.White.copy(alpha = 0.82f)

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
            .padding(vertical = 3.dp, horizontal = 1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. Prayer Vector Icon (Golden color as requested by user)
            MinimalVectorPrayerIcon(
                prayerName = prayer.name,
                tint = if (isCurrent) goldenColor else goldenColor.copy(alpha = 0.78f),
                modifier = Modifier.size(13.dp)
            )

            Spacer(modifier = Modifier.height(1.dp))

            // 2. Prayer Name (e.g. "ফজর", "যোহর")
            // Active is highlighted with light green; non-active is white/soft white
            Text(
                text = prayer.getDisplayName(isEnglish),
                fontSize = 9.2.sp,
                lineHeight = 10.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isCurrent) lightGreenHighlight else textWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(1.dp))

            // 3. Large Time Digits (e.g. "৪:৩১")
            // Highlighted with light green when current; crisp white otherwise
            Text(
                text = timeDigits,
                fontSize = 11.8.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) lightGreenHighlight else textWhite,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(1.dp))

            // 4. Supporting Status Line 1 (e.g. "ওয়াক্ত বাকি" / "শুরু হতে বাকি")
            Text(
                text = statusLine1,
                fontSize = 7.2.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.Normal,
                color = textSoftWhite,
                maxLines = 1
            )

            // 5. Supporting Status Line 2 (e.g. "৪২ মিনিট" / "১ ঘ. ১২ মি.")
            // Highlighting status line with light green for clear reading
            Text(
                text = statusLine2,
                fontSize = 7.8.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCurrent) lightGreenHighlight else Color(0xFFA7F3D0).copy(alpha = 0.9f),
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF063321), // Rich Dark Islamic Green
                        Color(0xFF0A442D), // Deep Emerald
                        Color(0xFF0F5438)  // Subtle Emerald highlight
                    )
                )
            )
            .border(
                width = 0.8.dp,
                color = Color(0xFF2E7D5B).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp),
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
            // LEFT: Mosque Vector Outline + Active Waqt Name + Live Dot + Time Range (৮:১০ - ৪:৩৭)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                MosqueOutlineBadge(
                    modifier = Modifier.size(28.dp),
                    tint = Color(0xFF86EFAC)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activePrayerName,
                            fontSize = 14.5.sp,
                            lineHeight = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF86EFAC)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        LiveIndicatorDot()
                    }
                    Text(
                        text = activePrayerTimeRange,
                        fontSize = 9.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f),
                        maxLines = 1
                    )
                }
            }

            // RIGHT: Two capsules in the same row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                // Capsule 1: ওয়াক্ত বাকি ৪২ মিনিট
                DarkCapsuleButton(
                    icon = Icons.Outlined.AccessTime,
                    title = currentWaqtLabel,
                    subtitle = currentWaqtTimeText,
                    hasArrow = false
                )

                // Capsule 2: পরবর্তী নামাজ যোহর ২ ঘ. ১২ মি. পর >
                DarkCapsuleButton(
                    icon = Icons.Outlined.AccessTime,
                    title = nextPrayerLabel,
                    subtitle = countdownFormatted,
                    hasArrow = true
                )
            }
        }
    }
}

/**
 * Pulsing live indicator dot for running/active waqt
 */
@Composable
private fun LiveIndicatorDot(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4ADE80)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LivePulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing halo
        Box(
            modifier = Modifier
                .size(9.dp * scale)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha * 0.4f))
        )
        // Inner solid dot
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
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
 * Capsule button matching the screenshot:
 * Dark translucent pill with green border, clock icon, title on top, subtitle below, optional right chevron.
 */
@Composable
private fun DarkCapsuleButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    hasArrow: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF04281A).copy(alpha = 0.85f))
            .border(
                width = 0.8.dp,
                color = Color(0xFF2E7D5B).copy(alpha = 0.75f),
                shape = RoundedCornerShape(100.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock icon with golden tint
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .clip(CircleShape)
                    .border(0.8.dp, Color(0xFFFFD54F).copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(9.5.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 7.2.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 8.sp,
                    lineHeight = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF86EFAC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (hasArrow) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(12.dp)
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF063321), // Rich Dark Islamic Green
                        Color(0xFF0A442D), // Deep Emerald
                        Color(0xFF0F5438)  // Subtle Emerald highlight
                    )
                )
            )
            .border(
                width = 0.8.dp,
                color = Color(0xFF2E7D5B).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
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
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = if (isEnglish) "Next Sahri" else "পরবর্তী সাহরি",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                thickness = 0.8.dp,
                color = Color(0xFF2E7D5B).copy(alpha = 0.5f)
            )

            // Column 2: Iftar
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayIftar,
                    fontSize = 14.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = if (isEnglish) "Next Iftar" else "পরবর্তী ইফতার",
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 2.dp),
                thickness = 0.8.dp,
                color = Color(0xFF2E7D5B).copy(alpha = 0.5f)
            )

            // Column 3: Live Countdown
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = countdownStr,
                    fontSize = 13.5.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF86EFAC),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = countdownSubtitle,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f),
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
