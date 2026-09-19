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
    val timeMinutes: Int,
    val timeString: String,
    val iconType: PrayerName
)

@Composable
fun PrayerSunPathCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier,
    onDetailsClick: () -> Unit = {},
    notificationStates: Map<com.example.data.model.PrayerName, Boolean> = emptyMap(),
    onToggleNotification: (com.example.data.model.PrayerName, Boolean) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val currentDate = remember(schedule) { schedule.dateStrBn }
    val hijriDate = remember { DateUtil.getTodayHijriDateStr(0) }

    val selectedLocation = if (schedule.district.countryBn == "বাংলাদেশ") {
        schedule.district.nameBn
    } else {
        "${schedule.district.nameBn}, ${schedule.district.countryBn}"
    }

    val prayers = remember(schedule) {
        val list = mutableListOf<VisualPrayerPoint>()
        schedule.prayers.forEach { prayerTime ->
            if (prayerTime.name != PrayerName.SAHRI && prayerTime.name != PrayerName.IFTAR) {
                val cal = Calendar.getInstance().apply { timeInMillis = prayerTime.timestampMillis }
                val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                
                list.add(
                    VisualPrayerPoint(
                        name = prayerTime.name.name,
                        bengaliName = prayerTime.name.nameBn,
                        timeMinutes = minutes,
                        timeString = "${prayerTime.timeDigits} ${prayerTime.amPm}",
                        iconType = prayerTime.name
                    )
                )
            }
        }
        list.sortedBy { it.timeMinutes }
    }

    val currentMinutes by rememberCurrentMinutes()
    val currentIndex = findCurrentPrayerIndex(prayers, currentMinutes)

    val dynamicColors = remember(currentIndex) {
        when (currentIndex) {
            0, 1 -> listOf(Color(0xFF0C2B3C), Color(0xFF071922)) // Fajr/Sunrise: Dawn Blues
            2 -> listOf(Color(0xFF084B5B), Color(0xFF04262E)) // Dhuhr: Bright Midday
            3 -> listOf(Color(0xFF4A3415), Color(0xFF2B1D0B)) // Asr: Afternoon Warm
            4 -> listOf(Color(0xFF441818), Color(0xFF220A0A)) // Maghrib: Sunset Deep Orange/Red
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Current Waqt Center Display
            if (currentIndex in 0 until prayers.size) {
                val current = prayers[currentIndex]
                val next = prayers.getOrNull((currentIndex + 1) % prayers.size) ?: prayers.first()
                
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

                // "বর্তমান ওয়াক্ত" pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "বর্তমান ওয়াক্ত",
                        color = SoftWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Current Prayer Name & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = current.bengaliName,
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
                        text = current.timeString,
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
                        .background(Color.Black.copy(alpha = 0.35f)) // Slightly darker for contrast
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
                                    text = "শুরু হয়েছে",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "${formatDurationBangla(elapsedMinutes)} আগে",
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
                            modifier = Modifier.weight(1.8f), // increased weight to make it wider
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy((-1).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp) // made progress bar thicker
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Progress fill
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(GoldAccent, Color(0xFF34D399))
                                            )
                                        )
                                )
                                // Thumb indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .offset(x = 6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                            .border(1.5.dp, Color(0xFF0F3E29), CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${DateUtil.toBengaliNumerals((progress * 100).toInt())}% সম্পন্ন",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        // Right: Remaining time (ওয়াক্ত শেষ)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy((-1).dp)
                            ) {
                                Text(
                                    text = "ওয়াক্ত শেষ",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "${formatDurationBangla(totalMinutes - elapsedMinutes)} বাকি",
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
                                    text = "পরবর্তী ওয়াক্ত",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "${next.bengaliName} • ${next.timeString}",
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

                        // Middle: Azaan Time
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
                                    text = "আজানের সময়",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = next.timeString, // This matches alarm logic natively
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
                        val isNotificationOn = notificationStates[next.iconType] ?: true
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.clickable { 
                                onToggleNotification(next.iconType, !isNotificationOn)
                            }
                        ) {
                            Text(
                                text = "নোটিফিকেশন",
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
                    text = "সময়সূচী: ইসলামিক ফাউন্ডেশন (বাংলাদেশ) অনুযায়ী",
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
                Text(
                    text = prayer.bengaliName,
                    color = if (isCurrent) GoldBright else Color.White,
                    fontSize = if (isCurrent) 8.5.sp else 7.5.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = prayer.timeString.replace(" ", "\n"),
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
                    text = "তাহাজ্জুদ",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "রাতের\nশেষাংশ",
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
private fun rememberCurrentMinutes(): State<Int> {
    val currentMinutes = remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while(true) {
            val cal = Calendar.getInstance()
            currentMinutes.value = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            delay(1000 * 60)
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
