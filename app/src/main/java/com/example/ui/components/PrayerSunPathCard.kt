package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerName
import com.example.utils.DateUtil
import com.example.utils.HijriCalendarUtil
import kotlinx.coroutines.delay
import java.util.Calendar

private val GoldAccent = Color(0xFFD4AF37)
private val GoldBright = Color(0xFFFFDF78)
private val SoftWhite = Color(0xFFD6EAE0)
private val GlassBg = Color.White.copy(alpha = 0.08f)
private val GlassBorder = Color.White.copy(alpha = 0.14f)

data class VisualPrayerPoint(
    val name: String,
    val bengaliName: String,
    val englishName: String,
    val timeMinutes: Int,
    val timeString: String,
    val timeStringEn: String,
    val iconType: PrayerName
)

private fun formatDurationEnglish(minutes: Int): String {
    val hrs = minutes / 60
    val mins = minutes % 60
    return when {
        hrs > 0 && mins > 0 -> "${hrs}h ${mins}m"
        hrs > 0 -> "${hrs}h"
        else -> "${mins}m"
    }
}

@Composable
fun PrayerSunPathCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier,
    isEnglish: Boolean = false,
    onDetailsClick: () -> Unit = {},
    notificationStates: Map<com.example.data.model.PrayerName, Boolean> = emptyMap(),
    onToggleNotification: (com.example.data.model.PrayerName, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currentDate = remember(schedule, isEnglish) {
        if (isEnglish) {
            DateUtil.formatDateEnglish(java.time.LocalDate.now())
        } else {
            schedule.dateStrBn
        }
    }
    val hijriDate = remember(isEnglish) {
        DateUtil.getTodayHijriDateStr(0, isEnglish = isEnglish)
    }

    val selectedLocation = if (isEnglish) {
        if (schedule.district.countryEn == "Bangladesh") schedule.district.nameEn else "${schedule.district.nameEn}, ${schedule.district.countryEn}"
    } else {
        if (schedule.district.countryBn == "বাংলাদেশ") schedule.district.nameBn else "${schedule.district.nameBn}, ${schedule.district.countryBn}"
    }

    val zoneId = remember(schedule.district) {
        try {
            java.time.ZoneId.of(schedule.district.timeZoneId)
        } catch (e: Exception) {
            java.time.ZoneId.of("Asia/Dhaka")
        }
    }

    val prayers = remember(schedule, zoneId) {
        val list = mutableListOf<VisualPrayerPoint>()
        schedule.prayers.forEach { prayerTime ->
            if (prayerTime.name != PrayerName.SAHRI && prayerTime.name != PrayerName.IFTAR) {
                val zdt = java.time.Instant.ofEpochMilli(prayerTime.timestampMillis).atZone(zoneId)
                val minutes = zdt.hour * 60 + zdt.minute
                
                list.add(
                    VisualPrayerPoint(
                        name = prayerTime.name.name,
                        bengaliName = prayerTime.displayNameBn,
                        englishName = prayerTime.getDisplayName(isEn = true),
                        timeMinutes = minutes,
                        timeString = "${prayerTime.timeDigits} ${prayerTime.amPm}",
                        timeStringEn = "${DateUtil.toEnglishNumerals(prayerTime.timeDigits)} ${prayerTime.amPm.replace("am", "AM").replace("pm", "PM")}",
                        iconType = prayerTime.name
                    )
                )
            }
        }
        list.sortedBy { it.timeMinutes }
    }

    val currentMinutes by rememberCurrentMinutes(zoneId)
    val currentIndex = findCurrentPrayerIndex(prayers, currentMinutes)

    val sunriseTimeItem = remember(schedule) { schedule.prayers.find { it.name == PrayerName.SUNRISE } }
    val dhuhrTimeItem = remember(schedule) { schedule.prayers.find { it.name == PrayerName.DHUHR } }
    val fajrTimeItem = remember(schedule) { schedule.prayers.find { it.name == PrayerName.FAJR } }

    val sunriseMinutes = remember(sunriseTimeItem, zoneId) {
        sunriseTimeItem?.let {
            val zdt = java.time.Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId)
            zdt.hour * 60 + zdt.minute
        } ?: (6 * 60)
    }
    val duhaStartMinutes = sunriseMinutes + 16
    val dhuhrMinutes = remember(dhuhrTimeItem, zoneId) {
        dhuhrTimeItem?.let {
            val zdt = java.time.Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId)
            zdt.hour * 60 + zdt.minute
        } ?: (12 * 60)
    }
    val duhaEndMinutes = (dhuhrMinutes - 4).coerceAtLeast(duhaStartMinutes)

    val isSunriseMakruhNow = (currentIndex == 1 && currentMinutes in sunriseMinutes until duhaStartMinutes)
    val isDuhaNow = (currentIndex == 1 && currentMinutes in duhaStartMinutes until duhaEndMinutes)
    val isZawalMakruhNow = (currentIndex == 1 && currentMinutes in duhaEndMinutes until dhuhrMinutes)

    val dynamicColors = remember(currentIndex, isSunriseMakruhNow, isDuhaNow, isZawalMakruhNow) {
        when {
            isSunriseMakruhNow || isZawalMakruhNow -> listOf(Color(0xFF38191C), Color(0xFF1E0E10))
            isDuhaNow -> listOf(Color(0xFF0D4B3C), Color(0xFF05241D)) // Morning vibrant warm golden green
            currentIndex == 0 -> listOf(Color(0xFF0C2B3C), Color(0xFF071922)) // Fajr/Sunrise: Dawn Blues
            currentIndex == 1 -> listOf(Color(0xFF0E465A), Color(0xFF07242E)) // Sunrise
            currentIndex == 2 -> listOf(Color(0xFF084B5B), Color(0xFF04262E)) // Dhuhr: Bright Midday
            currentIndex == 3 -> listOf(Color(0xFF4A3415), Color(0xFF2B1D0B)) // Asr: Afternoon Warm
            currentIndex == 4 -> listOf(Color(0xFF441818), Color(0xFF220A0A)) // Maghrib: Sunset Deep Orange/Red
            else -> listOf(Color(0xFF0B1120), Color(0xFF060912)) // Isha/Night: Midnight
        }
    }

    // Toggle states for notification

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = dynamicColors
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDetailsClick
            )
    ) {
        // Dynamic Sun/Moon Background
        DynamicSkyBackground(
            currentIndex = currentIndex,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header (Date + Location)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: Golden Calendar Icon + Bangla Date Capsule
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentDate,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = hijriDate,
                        color = SoftWhite.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Top-Right: Golden Location Pin + District Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedLocation,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Visual Sun/Moon Path
            VisualSunPathSection(
                prayers = prayers,
                currentIndex = currentIndex,
                currentMinutes = currentMinutes,
                isEnglish = isEnglish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Current Waqt Center Display
            if (currentIndex in 0 until prayers.size) {
                val current = prayers[currentIndex]
                val next = prayers.getOrNull((currentIndex + 1) % prayers.size) ?: prayers.first()
                
                // Dynamic Waqt metadata based on morning Chasht/Duha or Makruh periods
                val displayPill: String
                val displayName: String
                val displayTime: String
                val elapsedMinutes: Int
                val totalMinutes: Int
                val leftLabel: String
                val leftValue: String
                val rightLabel: String
                val rightValue: String
                val progressFraction: Float
                val nextWaqtDisplayName: String
                val nextWaqtDisplayTime: String

                when {
                    isSunriseMakruhNow -> {
                        displayPill = if (isEnglish) "⚠️ Forbidden Prayer Time" else "⚠️ সালাতের নিষিদ্ধ সময়"
                        displayName = if (isEnglish) "Sunrise (Makruh)" else "সূর্যোদয় (মাকরূহ)"
                        displayTime = if (isEnglish) DateUtil.toEnglishNumerals(schedule.forbiddenMorningRange).ifBlank { "15-16 mins from sunrise" } else schedule.forbiddenMorningRange.ifBlank { "সূর্যোদয় থেকে ১৫-১৬ মিনিট" }
                        elapsedMinutes = (currentMinutes - sunriseMinutes).coerceAtLeast(0)
                        totalMinutes = (duhaStartMinutes - sunriseMinutes).coerceAtLeast(1)
                        val remainingToDuha = (duhaStartMinutes - currentMinutes).coerceAtLeast(0)
                        leftLabel = if (isEnglish) "Started" else "শুরু হয়েছে"
                        leftValue = if (isEnglish) "${formatDurationEnglish(elapsedMinutes)} ago" else "${formatDurationBangla(elapsedMinutes)} আগে"
                        rightLabel = if (isEnglish) "Chasht Starts" else "চাশত শুরু"
                        rightValue = if (isEnglish) "${formatDurationEnglish(remainingToDuha)} left" else "${formatDurationBangla(remainingToDuha)} বাকি"
                        progressFraction = (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
                        nextWaqtDisplayName = if (isEnglish) "Chasht & Duha" else "চাশত ও দুহা"
                        nextWaqtDisplayTime = if (isEnglish) DateUtil.toEnglishNumerals(schedule.duhaRange.split("-").firstOrNull()?.trim() ?: "Morning") else (schedule.duhaRange.split("-").firstOrNull()?.trim() ?: "সকাল")
                    }
                    isDuhaNow -> {
                        displayPill = if (isEnglish) "Nafl Prayer Time" else "নফল সালাতের ওয়াক্ত"
                        displayName = if (isEnglish) "Chasht & Duha" else "চাশত ও দুহা"
                        displayTime = if (isEnglish) DateUtil.toEnglishNumerals(schedule.duhaRange).ifBlank { "Morning to Midday" } else schedule.duhaRange.ifBlank { "সকাল থেকে দ্বিপ্রহর" }
                        elapsedMinutes = (currentMinutes - duhaStartMinutes).coerceAtLeast(0)
                        totalMinutes = (duhaEndMinutes - duhaStartMinutes).coerceAtLeast(1)
                        val remainingToZawal = (duhaEndMinutes - currentMinutes).coerceAtLeast(0)
                        leftLabel = if (isEnglish) "Started" else "শুরু হয়েছে"
                        leftValue = if (isEnglish) "${formatDurationEnglish(elapsedMinutes)} ago" else "${formatDurationBangla(elapsedMinutes)} আগে"
                        rightLabel = if (isEnglish) "Waqt Ends" else "ওয়াক্ত শেষ"
                        rightValue = if (isEnglish) "${formatDurationEnglish(remainingToZawal)} left" else "${formatDurationBangla(remainingToZawal)} বাকি"
                        progressFraction = (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
                        nextWaqtDisplayName = if (isEnglish) (if (schedule.isFriday) "Jumu'ah" else "Dhuhr") else (if (schedule.isFriday) "জুমুআ" else "যোহর")
                        nextWaqtDisplayTime = if (isEnglish) next.timeStringEn else next.timeString
                    }
                    isZawalMakruhNow -> {
                        displayPill = if (isEnglish) "⚠️ Forbidden Prayer Time" else "⚠️ সালাতের নিষিদ্ধ সময়"
                        displayName = if (isEnglish) "Midday (Zawal)" else "দ্বিপ্রহর (জাওয়াল)"
                        displayTime = if (isEnglish) DateUtil.toEnglishNumerals(schedule.forbiddenNoonRange).ifBlank { "Midday forbidden time" } else schedule.forbiddenNoonRange.ifBlank { "দ্বিপ্রহরের নিষিদ্ধ সময়" }
                        elapsedMinutes = (currentMinutes - duhaEndMinutes).coerceAtLeast(0)
                        totalMinutes = (dhuhrMinutes - duhaEndMinutes).coerceAtLeast(1)
                        val remainingToDhuhr = (dhuhrMinutes - currentMinutes).coerceAtLeast(0)
                        leftLabel = if (isEnglish) "Started" else "শুরু হয়েছে"
                        leftValue = if (isEnglish) "${formatDurationEnglish(elapsedMinutes)} ago" else "${formatDurationBangla(elapsedMinutes)} আগে"
                        rightLabel = if (isEnglish) (if (schedule.isFriday) "Jumu'ah Starts" else "Dhuhr Starts") else (if (schedule.isFriday) "জুমুআ শুরু" else "যোহর শুরু")
                        rightValue = if (isEnglish) "${formatDurationEnglish(remainingToDhuhr)} left" else "${formatDurationBangla(remainingToDhuhr)} বাকি"
                        progressFraction = (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
                        nextWaqtDisplayName = if (isEnglish) (if (schedule.isFriday) "Jumu'ah" else "Dhuhr") else (if (schedule.isFriday) "জুমুআ" else "যোহর")
                        nextWaqtDisplayTime = if (isEnglish) next.timeStringEn else next.timeString
                    }
                    else -> {
                        displayPill = if (isEnglish) "Current Waqt" else "বর্তমান ওয়াক্ত"
                        displayName = if (isEnglish) current.englishName else current.bengaliName
                        displayTime = if (isEnglish) current.timeStringEn else current.timeString
                        val rawTotal = if (next.timeMinutes > current.timeMinutes) {
                            next.timeMinutes - current.timeMinutes
                        } else {
                            (24 * 60 - current.timeMinutes) + next.timeMinutes
                        }
                        totalMinutes = rawTotal.coerceAtLeast(1)
                        val rawElapsed = if (currentMinutes >= current.timeMinutes) {
                            currentMinutes - current.timeMinutes
                        } else {
                            (24 * 60 - current.timeMinutes) + currentMinutes
                        }
                        elapsedMinutes = rawElapsed
                        val remaining = (totalMinutes - elapsedMinutes).coerceAtLeast(0)
                        leftLabel = if (isEnglish) "Started" else "শুরু হয়েছে"
                        leftValue = if (isEnglish) "${formatDurationEnglish(elapsedMinutes)} ago" else "${formatDurationBangla(elapsedMinutes)} আগে"
                        rightLabel = if (isEnglish) "Waqt Ends" else "ওয়াক্ত শেষ"
                        rightValue = if (isEnglish) "${formatDurationEnglish(remaining)} left" else "${formatDurationBangla(remaining)} বাকি"
                        progressFraction = (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
                        nextWaqtDisplayName = if (isEnglish) next.englishName else next.bengaliName
                        nextWaqtDisplayTime = if (isEnglish) next.timeStringEn else next.timeString
                    }
                }

                // "বর্তমান ওয়াক্ত" / "নফল সালাতের ওয়াক্ত" / "⚠️ সালাতের নিষিদ্ধ সময়" pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            when {
                                isSunriseMakruhNow || isZawalMakruhNow -> Color(0xFFDC2626).copy(alpha = 0.35f)
                                isDuhaNow -> Color(0xFF00C288).copy(alpha = 0.25f)
                                else -> Color.White.copy(alpha = 0.12f)
                            }
                        )
                        .border(
                            0.8.dp,
                            when {
                                isSunriseMakruhNow || isZawalMakruhNow -> Color(0xFFF87171).copy(alpha = 0.6f)
                                isDuhaNow -> Color(0xFF00C288).copy(alpha = 0.6f)
                                else -> Color.White.copy(alpha = 0.2f)
                            },
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 14.dp, vertical = 3.5.dp)
                ) {
                    Text(
                        text = displayPill,
                        color = when {
                            isSunriseMakruhNow || isZawalMakruhNow -> Color(0xFFFFCDD2)
                            isDuhaNow -> Color(0xFF34D399)
                            else -> SoftWhite
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Spacer(modifier = Modifier.height(5.dp))
                
                // Current Prayer Name & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = displayName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = GoldBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = displayTime,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Progress Block (Start -> Progress -> Remaining)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .height(58.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Started time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy((-1).dp)) {
                                Text(
                                    text = leftLabel,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = leftValue,
                                    color = GoldBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Center: Progress Bar
                        Column(
                            modifier = Modifier.weight(1.8f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-1).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Progress fill
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = if (isSunriseMakruhNow || isZawalMakruhNow) {
                                                    listOf(Color(0xFFEF4444), Color(0xFFF87171))
                                                } else {
                                                    listOf(GoldAccent, Color(0xFF34D399))
                                                }
                                            )
                                        )
                                )
                                // Thumb indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .offset(x = 6.dp)
                                            .clip(CircleShape)
                                            .background(if (isSunriseMakruhNow || isZawalMakruhNow) Color(0xFFF87171) else Color(0xFF34D399))
                                            .border(1.5.dp, Color(0xFF0F3E29), CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${DateUtil.toBengaliNumerals((progressFraction * 100).toInt())}% সম্পন্ন",
                                color = if (isSunriseMakruhNow || isZawalMakruhNow) Color(0xFFFCA5A5) else Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Remaining time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy((-1).dp)
                            ) {
                                Text(
                                    text = rightLabel,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = rightValue,
                                    color = GoldBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Next Prayer Details Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .height(58.dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Next Prayer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy((-1).dp)) {
                                Text(
                                    text = if (isEnglish) "Next Waqt" else "পরবর্তী ওয়াক্ত",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "$nextWaqtDisplayName • $nextWaqtDisplayTime",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        
                        // Center Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Middle: Azaan / Waqt Start Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy((-1).dp)) {
                                Text(
                                    text = if (isSunriseMakruhNow) {
                                        if (isEnglish) "Waqt Starts" else "ওয়াক্ত শুরু"
                                    } else {
                                        if (isEnglish) "Adhan Time" else "আজানের সময়"
                                    },
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = nextWaqtDisplayTime,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                        
                        // Center Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Right: Notification Toggle
                        val notifTargetIcon = if (isSunriseMakruhNow) PrayerName.SUNRISE else next.iconType
                        val isNotificationOn = notificationStates[notifTargetIcon] ?: true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable { 
                                onToggleNotification(notifTargetIcon, !isNotificationOn)
                            }
                        ) {
                            Text(
                                text = if (isEnglish) "Alert" else "নোটিফিকেশন",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                            // Custom Pill Toggle
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isNotificationOn) Color(0xFF34D399).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, if (isNotificationOn) Color(0xFF34D399) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isNotificationOn) Color(0xFF34D399) else Color.White.copy(alpha = 0.5f))
                                        .align(if (isNotificationOn) Alignment.CenterEnd else Alignment.CenterStart)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SoftWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "Timetable: Based on Islamic Foundation (Bangladesh)" else "সময়সূচী: ইসলামিক ফাউন্ডেশন (বাংলাদেশ) অনুযায়ী",
                    color = SoftWhite.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
@Composable
private fun VisualSunPathSection(
    prayers: List<VisualPrayerPoint>,
    currentIndex: Int,
    currentMinutes: Int,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = maxHeight

        val infiniteTransition = rememberInfiniteTransition(label = "sunPulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Exact node positions mathematically matching quadraticBezierTo to align perfectly
        val nodes = remember(prayers, width, height) {
            prayers.mapIndexed { index, _ ->
                val t = index.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                val oneMinusT = 1f - t
                // Exact Bezier curve equations mapping to our Canvas control points:
                // start(0.05, 0.85), peak(0.50, -0.30), end(0.95, 0.85)
                val x = (oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * t * 0.50f) + (t * t * 0.95f)
                val y = (oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * t * -0.30f) + (t * t * 0.85f)
                Offset(x, y)
            }
        }

        // Arc & Nodes Drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (prayers.size < 2) return@Canvas

            val pathStart = Offset(x = size.width * 0.05f, y = size.height * 0.85f)
            val pathEnd = Offset(x = size.width * 0.95f, y = size.height * 0.85f)
            val peak = Offset(x = size.width * 0.50f, y = -size.height * 0.30f)

            val path = Path().apply {
                moveTo(pathStart.x, pathStart.y)
                quadraticBezierTo(peak.x, peak.y, pathEnd.x, pathEnd.y)
            }

            // Glow path behind
            drawPath(
                path = path,
                color = GoldAccent.copy(alpha = 0.15f),
                style = Stroke(width = 6.dp.toPx())
            )

            // Solid celestial path (removed dashPathEffect)
            drawPath(
                path = path,
                color = GoldAccent.copy(alpha = 0.9f),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Fixed nodes
            nodes.forEachIndexed { index, relativeOffset ->
                // Hide middle nodes during Isha (night)
                if (currentIndex == prayers.lastIndex && index in 1 until prayers.lastIndex) return@forEachIndexed

                val x = size.width * relativeOffset.x
                val y = size.height * relativeOffset.y
                val isCurrent = index == currentIndex

                drawCircle(
                    color = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.8f),
                    radius = if (isCurrent) 4.5.dp.toPx() else 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // Draw Tahajjud node during Isha
            if (currentIndex == prayers.lastIndex && prayers.isNotEmpty()) {
                val t = 0.5f
                val oneMinusT = 1f - t
                val relX = (oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * t * 0.50f) + (t * t * 0.95f)
                val relY = (oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * t * -0.30f) + (t * t * 0.85f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(size.width * relX, size.height * relY)
                )
            }

            // Calculate dynamic sun position
            if (prayers.isNotEmpty() && currentIndex in prayers.indices) {
                val current = prayers[currentIndex]
                val nextIndex = (currentIndex + 1) % prayers.size
                val next = prayers[nextIndex]
                
                val totalMinutes = if (next.timeMinutes > current.timeMinutes) {
                    next.timeMinutes - current.timeMinutes
                } else {
                    (24 * 60 - current.timeMinutes) + next.timeMinutes
                }
                val elapsedMinutes = if (currentMinutes >= current.timeMinutes) {
                    currentMinutes - current.timeMinutes
                } else {
                    (24 * 60 - current.timeMinutes) + currentMinutes
                }
                val progress = (elapsedMinutes.toFloat() / totalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                
                val currentT = currentIndex.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                // If nextIndex is 0 (wrapping around to Fajr), its T would naturally be 1.0 + something.
                // But in our arch, 0 to 5 maps to T 0.0 to 1.0. 
                // Wrapping around means from Isha (T=1.0) to Fajr (T=0.0). We shouldn't visually wrap back across the whole screen.
                // It looks better if we just clamp it, or if it's Isha->Fajr, let it slowly move towards the end or disappear.
                // Let's implement wrap-around visually: from Isha to Fajr, the sun could move off-screen or jump back.
                // But on the arch, Isha is at T=1.0 and Fajr is at T=0.0.
                val nextT = if (nextIndex == 0) 1.2f else nextIndex.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                
                var sunT = currentT + (nextT - currentT) * progress
                // If sunT > 1.0 (Isha to Fajr), we can map it back to 0.0 gradually if we wanted, 
                // or just let it follow an imaginary extended curve (which nextT=1.2f does roughly).
                // Actually, wrapping around from right to left smoothly might look weird.
                // Let's just let it slide back smoothly from 1.0 to 0.0 over the night.
                if (nextIndex == 0) {
                    sunT = currentT + (0.0f - currentT) * progress // slides backwards during night
                }
                
                val oneMinusT = 1f - sunT
                val sunX = size.width * ((oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * sunT * 0.50f) + (sunT * sunT * 0.95f))
                val sunY = size.height * ((oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * sunT * -0.30f) + (sunT * sunT * 0.85f))
                
                // Draw dynamic glowing sun
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.35f),
                    radius = 8.dp.toPx() * pulseScale,
                    center = Offset(sunX, sunY)
                )
                drawCircle(
                    color = GoldBright,
                    radius = 4.5.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
            }
        }

        // Labels & Icons along the path
        nodes.forEachIndexed { index, relativeOffset ->
            // Hide middle labels during Isha (night)
            if (currentIndex == prayers.lastIndex && index in 1 until prayers.lastIndex) return@forEachIndexed

            val prayer = prayers[index]
            val isCurrent = index == currentIndex
            
            // X position is aligned exactly to the node center
            val xOffset = width * relativeOffset.x
            // Y position is placed just above the node center
            // Place column such that the icon is directly on the node (icon height/2 = 8dp max)
            val verticalY = (height * relativeOffset.y) - 8.dp

            Column(
                modifier = Modifier
                    .offset(x = xOffset - 32.dp, y = verticalY) // -32dp centers the 64dp wide column over X
                    .width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Minimal icon circle (AT TOP NOW)
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 16.dp else 13.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) GoldAccent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            width = if (isCurrent) 1.5.dp else 0.8.dp,
                            color = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val fallbackIcon = when (prayer.iconType) {
                        PrayerName.FAJR -> Icons.Default.Nightlight
                        PrayerName.SUNRISE -> Icons.Default.WbSunny
                        PrayerName.DHUHR -> Icons.Default.WbSunny
                        PrayerName.ASR -> Icons.Default.WbSunny
                        PrayerName.MAGHRIB -> Icons.Default.WbSunny
                        PrayerName.ISHA -> Icons.Default.Nightlight
                        else -> Icons.Default.Schedule
                    }
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(if (isCurrent) 10.dp else 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val nodeTitle = if (index == 1 && isCurrent && currentMinutes >= prayer.timeMinutes + 16) {
                    if (isEnglish) "Chasht / Duha" else "চাশত / দুহা"
                } else {
                    if (isEnglish) prayer.englishName else prayer.bengaliName
                }
                Text(
                    text = nodeTitle,
                    color = if (isCurrent) GoldBright else Color.White,
                    fontSize = if (isCurrent) 8.5.sp else 7.5.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = (if (isEnglish) prayer.timeStringEn else prayer.timeString).replace(" ", "\n"),
                    color = if (isCurrent) Color.White else SoftWhite.copy(alpha = 0.85f),
                    fontSize = if (isCurrent) 7.5.sp else 6.5.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 8.sp,
                    maxLines = 2
                )
            }
        }
        
        // Tahajjud Label & Icon during Isha
        if (currentIndex == prayers.lastIndex && prayers.isNotEmpty()) {
            val t = 0.5f
            val oneMinusT = 1f - t
            val relX = (oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * t * 0.50f) + (t * t * 0.95f)
            val relY = (oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * t * -0.30f) + (t * t * 0.85f)
            
            val xOffset = width * relX
            val verticalY = (height * relY) - 8.dp
            
            Column(
                modifier = Modifier
                    .offset(x = xOffset - 32.dp, y = verticalY)
                    .width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Minimal icon circle
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isEnglish) "Tahajjud" else "তাহাজ্জুদ",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isEnglish) "Late\nNight" else "রাতের\nশেষাংশ",
                    color = SoftWhite.copy(alpha = 0.75f),
                    fontSize = 6.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 7.5.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun rememberCurrentMinutes(zoneId: java.time.ZoneId): State<Int> {
    val currentMinutes = remember(zoneId) {
        val now = java.time.ZonedDateTime.now(zoneId)
        mutableStateOf(now.hour * 60 + now.minute)
    }
    LaunchedEffect(zoneId) {
        while(true) {
            val now = java.time.ZonedDateTime.now(zoneId)
            currentMinutes.value = now.hour * 60 + now.minute
            delay(1000 * 30)
        }
    }
    return currentMinutes
}

private fun findCurrentPrayerIndex(prayers: List<VisualPrayerPoint>, currentMinutes: Int): Int {
    if (prayers.isEmpty()) return 0
    for (i in 0 until prayers.size - 1) {
        if (currentMinutes >= prayers[i].timeMinutes && currentMinutes < prayers[i+1].timeMinutes) {
            return i
        }
    }
    if (currentMinutes < prayers.first().timeMinutes) return prayers.lastIndex
    if (currentMinutes >= prayers.last().timeMinutes) return prayers.lastIndex
    return 0
}

private fun formatDurationBangla(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    val hrStr = if (hours > 0) "${com.example.utils.DateUtil.toBengaliNumerals(hours)} ঘণ্টা " else ""
    val minStr = if (mins > 0 || hours == 0) "${com.example.utils.DateUtil.toBengaliNumerals(mins)} মিনিট" else ""
    return (hrStr + minStr).trim()
}

@Composable
private fun DynamicSkyBackground(
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val isNight = currentIndex == 5 || currentIndex == 0 // Isha or Fajr
    val isSunset = currentIndex == 4
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        if (isNight) {
            val moonCenter = Offset(w * 0.85f, h * 0.25f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE2E8F0).copy(alpha = 0.25f), Color.Transparent),
                    center = moonCenter,
                    radius = w * 0.4f
                ),
                radius = w * 0.4f,
                center = moonCenter
            )
            drawCircle(
                color = Color(0xFFF8FAFC).copy(alpha = 0.95f),
                radius = 18.dp.toPx(),
                center = moonCenter
            )
            val skyColor = if (currentIndex == 0) Color(0xFF0C2B3C) else Color(0xFF0B1120)
            drawCircle(
                color = skyColor, 
                radius = 15.dp.toPx(),
                center = Offset(moonCenter.x - 6.dp.toPx(), moonCenter.y - 4.dp.toPx())
            )
        } else {
            val sunCenter = Offset(w * 0.85f, h * 0.25f)
            val sunCore = if (isSunset) Color(0xFFFF8A65) else Color(0xFFFFD54F)
            val sunGlow = if (isSunset) Color(0xFFE64A19) else Color(0xFFFFA000)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sunGlow.copy(alpha = 0.4f), Color.Transparent),
                    center = sunCenter,
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = sunCenter
            )
            drawCircle(
                color = sunCore.copy(alpha = 0.95f),
                radius = 22.dp.toPx(),
                center = sunCenter
            )
        }
        
        if (isNight) {
            val random = java.util.Random(42)
            for (i in 0..15) {
                val x = random.nextFloat() * w
                val y = random.nextFloat() * (h * 0.6f)
                val starRadius = (random.nextFloat() * 1.5f + 0.5f).dp.toPx()
                val alpha = random.nextFloat() * 0.5f + 0.2f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = starRadius,
                    center = Offset(x, y)
                )
            }
        } else {
            val cloudColor = Color.White.copy(alpha = 0.08f)
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.1f, h * 0.15f),
                size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.04f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f)
            )
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.15f, h * 0.13f),
                size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.05f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f)
            )
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.65f, h * 0.35f),
                size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.03f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f)
            )
        }
    }
}
