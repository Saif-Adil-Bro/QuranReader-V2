package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerName
import com.example.data.model.SinglePrayerTime
import kotlinx.coroutines.delay

// Design Constants matching exact prompt & mockup specs
private val PrimaryGreenDark = Color(0xFF1B4D3E)
private val PrimaryGreenLight = Color(0xFF2E7D32)
private val AccentGold = Color(0xFFFFD54F)
private val AccentGoldWarm = Color(0xFFD4AF37)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFE2EFEA)

@Composable
fun PrayerTimesBannerSlide(
    schedule: DailyPrayerSchedule,
    onClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClick() }
    ) {
        // High-Fidelity 3D-Illuminated Mosque Silhouette Watermark in Background
        MosqueSilhouetteBackground(
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Header (Date Capsule on Left, Dynamic Status/Countdown in Center, Location Capsule with Dropdown on Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: Golden Calendar Icon + Bangla Date Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(10.5.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = schedule.dateStrBn,
                            color = TextPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Center: Dynamic Context / Live Countdown in HH:MM:SS / Forbidden Time Badge
                if (schedule.isForbiddenTimeNow) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFDC2626).copy(alpha = 0.35f))
                            .border(0.8.dp, Color(0xFFF87171).copy(alpha = 0.6f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 7.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⚠️ ${schedule.forbiddenTimeReason ?: "নামাযের নিষিদ্ধ সময়"}",
                            color = Color(0xFFFFCDD2),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                } else if (schedule.nextPrayer != null) {
                    val nextPrayer = schedule.nextPrayer
                    val nextMillis = nextPrayer.timestampMillis
                    val diffMillis = (nextMillis - currentTimeMillis).coerceAtLeast(0L)
                    val totalSeconds = diffMillis / 1000
                    val hours = totalSeconds / 3600
                    val minutes = (totalSeconds % 3600) / 60
                    val seconds = totalSeconds % 60
                    val timeFormatted = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                    val banglaCountdown = com.example.utils.DateUtil.toBengaliNumerals(timeFormatted)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFF0D3B2E).copy(alpha = 0.75f))
                            .border(0.8.dp, AccentGold.copy(alpha = 0.45f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            HourglassMinimalIcon(
                                tint = AccentGold,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.5.dp))
                            Text(
                                text = "${nextPrayer.name.nameBn}: ",
                                color = AccentGold,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = banglaCountdown,
                                color = TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Top-Right: Golden Location Pin + District Capsule with Dropdown Arrow
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(100.dp))
                        .clickable { onLocationClick() }
                        .padding(horizontal = 7.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AccentGold,
                            modifier = Modifier.size(10.5.dp)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        val locationName = if (schedule.district.countryBn == "বাংলাদেশ") {
                            schedule.district.nameBn
                        } else {
                            "${schedule.district.nameBn}, ${schedule.district.countryBn}"
                        }
                        Text(
                            text = "$locationName ▾",
                            color = TextPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // 2. Middle Section: Integrated 5 Main Prayers Grid with Active Highlighting & Live Progress Animation
            val mainPrayers = schedule.prayers.filter { it.name != PrayerName.SUNRISE }
            val hasCurrent = mainPrayers.any { it.isCurrent }
            val currentPrayerItem = mainPrayers.firstOrNull { it.isCurrent }
            val nextPrayerItem = schedule.nextPrayer ?: mainPrayers.firstOrNull { it.name == PrayerName.ISHA } ?: mainPrayers.last()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                mainPrayers.forEachIndexed { index, prayer ->
                    val isCurrent = prayer.isCurrent
                    val isNext = !hasCurrent && (prayer.name == nextPrayerItem.name)

                    // Calculate live progress and remaining countdown for current prayer
                    var progressFraction = 0f
                    var countdownText = ""
                    if (isCurrent) {
                        val startMillis = prayer.timestampMillis
                        val endMillis = when (prayer.name) {
                            PrayerName.FAJR -> schedule.prayers.find { it.name == PrayerName.SUNRISE }?.timestampMillis ?: (startMillis + 90 * 60 * 1000L)
                            PrayerName.DHUHR -> schedule.prayers.find { it.name == PrayerName.ASR }?.timestampMillis ?: (startMillis + 180 * 60 * 1000L)
                            PrayerName.ASR -> schedule.prayers.find { it.name == PrayerName.MAGHRIB }?.timestampMillis ?: (startMillis + 120 * 60 * 1000L)
                            PrayerName.MAGHRIB -> schedule.prayers.find { it.name == PrayerName.ISHA }?.timestampMillis ?: (startMillis + 75 * 60 * 1000L)
                            PrayerName.ISHA -> {
                                val fajr = schedule.prayers.find { it.name == PrayerName.FAJR }
                                if (fajr != null && fajr.timestampMillis > startMillis) {
                                    fajr.timestampMillis
                                } else {
                                    (fajr?.timestampMillis ?: startMillis) + 24 * 3600 * 1000L
                                }
                            }
                            else -> startMillis + 60 * 60 * 1000L
                        }
                        val totalDuration = (endMillis - startMillis).coerceAtLeast(1L)
                        val elapsed = (currentTimeMillis - startMillis).coerceAtLeast(0L)
                        progressFraction = (elapsed.toFloat() / totalDuration.toFloat()).coerceIn(0f, 1f)
                        val remainingMillis = (endMillis - currentTimeMillis).coerceAtLeast(0L)
                        val remainingMins = remainingMillis / (1000 * 60)
                        val remainingSecs = (remainingMillis / 1000) % 60
                        countdownText = if (remainingMins >= 60) {
                            val hrs = remainingMins / 60
                            val mins = remainingMins % 60
                            "${com.example.utils.DateUtil.toBengaliNumerals(hrs)}ঘ. ${com.example.utils.DateUtil.toBengaliNumerals(mins)}মি."
                        } else if (remainingMins > 0) {
                            "${com.example.utils.DateUtil.toBengaliNumerals(remainingMins)}মি."
                        } else {
                            "${com.example.utils.DateUtil.toBengaliNumerals(remainingSecs)}সে."
                        }
                    }

                    PrayerUnifiedColumnItem(
                        prayer = prayer,
                        isCurrent = isCurrent,
                        isNext = isNext,
                        progress = progressFraction,
                        countdownText = countdownText,
                        modifier = Modifier.weight(1f)
                    )

                    if (index < mainPrayers.size - 1 && !isCurrent && !isNext && (index + 1 < mainPrayers.size && !mainPrayers[index + 1].isCurrent && mainPrayers[index + 1].name != nextPrayerItem.name)) {
                        // Thin subtle vertical separator between non-active items
                        Box(
                            modifier = Modifier
                                .width(0.6.dp)
                                .height(32.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )
                    }
                }
            }

            // 3. Footer (Thin divider, Left Info disclaimer, Right "বিস্তারিত ও নিষিদ্ধ সময় →")
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(Color.White.copy(alpha = 0.20f))
                )

                Spacer(modifier = Modifier.height(2.5.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.85f),
                            modifier = Modifier.size(10.5.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "ইসলামিক ফাউন্ডেশন (বাংলাদেশ)",
                            color = TextSecondary.copy(alpha = 0.85f),
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Text(
                            text = "বিস্তারিত ও নিষিদ্ধ সময় →",
                            color = AccentGold,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerUnifiedColumnItem(
    prayer: SinglePrayerTime,
    isCurrent: Boolean,
    isNext: Boolean,
    progress: Float,
    countdownText: String,
    modifier: Modifier = Modifier
) {
    if (isCurrent) {
        // Dynamic Glowing Active Animation for Current Waqt
        val infiniteTransition = rememberInfiniteTransition(label = "currentWaqtActiveGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "liveProgress"
        )

        Box(
            modifier = modifier
                .padding(horizontal = 1.5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF26785C).copy(alpha = 0.85f),
                            Color(0xFF134E3A).copy(alpha = 0.95f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AccentGold.copy(alpha = glowAlpha),
                            Color(0xFF86EFAC).copy(alpha = glowAlpha),
                            AccentGold.copy(alpha = glowAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 3.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
            ) {
                // Top Badge: Pulsing Dot + "চলমান"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .clip(CircleShape)
                            .background(AccentGold.copy(alpha = glowAlpha))
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "চলমান",
                        color = AccentGold,
                        fontSize = 7.5.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Bengali Name
                Text(
                    text = prayer.name.nameBn,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Large clear time digits
                Text(
                    text = prayer.timeDigits,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Live Dynamic Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = animatedProgress.coerceIn(0.06f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981),
                                        AccentGold
                                    )
                                )
                            )
                    )
                }

                // Live Countdown Text
                Text(
                    text = if (countdownText.isNotBlank()) "বাকি $countdownText" else prayer.amPm,
                    color = AccentGold,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
    } else if (isNext) {
        // Next Waqt Accent Capsule Box
        Box(
            modifier = modifier
                .padding(horizontal = 1.5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E8566).copy(alpha = 0.45f),
                            Color(0xFF1B5D46).copy(alpha = 0.55f)
                        )
                    )
                )
                .border(0.9.dp, AccentGold.copy(alpha = 0.70f), RoundedCornerShape(10.dp))
                .padding(horizontal = 3.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
            ) {
                // Tiny badge
                Text(
                    text = "পরবর্তী",
                    color = AccentGold,
                    fontSize = 7.5.sp,
                    lineHeight = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                // Bengali Name
                Text(
                    text = prayer.name.nameBn,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Large clear time digits
                Text(
                    text = prayer.timeDigits,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // AM / PM
                Text(
                    text = prayer.amPm,
                    color = AccentGold.copy(alpha = 0.95f),
                    fontSize = 7.5.sp,
                    lineHeight = 8.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        // Standard Elegant Inactive Column
        Column(
            modifier = modifier
                .padding(horizontal = 1.5.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically)
        ) {
            // Golden minimal Islamic prayer icon
            GoldenPrayerIcon(
                prayerName = prayer.name,
                modifier = Modifier.size(13.dp)
            )

            // Bengali Name
            Text(
                text = prayer.name.nameBn,
                color = TextSecondary,
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium
            )

            // Clear time digits
            Text(
                text = prayer.timeDigits,
                color = TextPrimary,
                fontSize = 11.5.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Bold
            )

            // AM / PM
            Text(
                text = prayer.amPm,
                color = TextSecondary.copy(alpha = 0.8f),
                fontSize = 7.5.sp,
                lineHeight = 8.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HourglassMinimalIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.12f)
            lineTo(w * 0.85f, h * 0.12f)
            lineTo(w * 0.53f, h * 0.50f)
            lineTo(w * 0.85f, h * 0.88f)
            lineTo(w * 0.15f, h * 0.88f)
            lineTo(w * 0.47f, h * 0.50f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun GoldenPrayerIcon(
    prayerName: PrayerName,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val goldColor = AccentGold

        when (prayerName) {
            PrayerName.FAJR -> {
                // Rising sun with horizontal horizon and bold rays
                drawLine(
                    color = goldColor,
                    start = Offset(0f, height * 0.75f),
                    end = Offset(width, height * 0.75f),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
                drawArc(
                    color = goldColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(width * 0.22f, height * 0.32f),
                    size = Size(width * 0.56f, height * 0.56f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                // Bold Rays
                drawLine(goldColor, Offset(width * 0.5f, height * 0.08f), Offset(width * 0.5f, height * 0.24f), 2.2f, StrokeCap.Round)
                drawLine(goldColor, Offset(width * 0.16f, height * 0.22f), Offset(width * 0.30f, height * 0.36f), 2.2f, StrokeCap.Round)
                drawLine(goldColor, Offset(width * 0.84f, height * 0.22f), Offset(width * 0.70f, height * 0.36f), 2.2f, StrokeCap.Round)
            }
            PrayerName.DHUHR, PrayerName.SUNRISE -> {
                // High bright midday sun with bold radial rays
                drawCircle(
                    color = goldColor,
                    radius = width * 0.22f,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
                val rayLen = width * 0.14f
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val startR = width * 0.30f
                    val endR = startR + rayLen
                    drawLine(
                        color = goldColor,
                        start = Offset((center.x + startR * Math.cos(angle)).toFloat(), (center.y + startR * Math.sin(angle)).toFloat()),
                        end = Offset((center.x + endR * Math.cos(angle)).toFloat(), (center.y + endR * Math.sin(angle)).toFloat()),
                        strokeWidth = 2.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
            PrayerName.ASR -> {
                // Afternoon sun with bold tilted rays
                drawArc(
                    color = goldColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(width * 0.22f, height * 0.32f),
                    size = Size(width * 0.56f, height * 0.56f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                drawLine(goldColor, Offset(width * 0.08f, height * 0.75f), Offset(width * 0.92f, height * 0.75f), 2.5f, StrokeCap.Round)
                drawLine(goldColor, Offset(width * 0.5f, height * 0.08f), Offset(width * 0.5f, height * 0.24f), 2.2f, StrokeCap.Round)
                drawLine(goldColor, Offset(width * 0.18f, height * 0.18f), Offset(width * 0.30f, height * 0.32f), 2.2f, StrokeCap.Round)
                drawLine(goldColor, Offset(width * 0.82f, height * 0.18f), Offset(width * 0.70f, height * 0.32f), 2.2f, StrokeCap.Round)
            }
            PrayerName.MAGHRIB -> {
                // Sunset (Sun dipping into horizon) with bold strokes
                drawLine(goldColor, Offset(0f, height * 0.65f), Offset(width, height * 0.65f), 2.5f, StrokeCap.Round)
                drawLine(goldColor, Offset(width * 0.15f, height * 0.82f), Offset(width * 0.85f, height * 0.82f), 2.0f, StrokeCap.Round)
                drawArc(
                    color = goldColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(width * 0.26f, height * 0.30f),
                    size = Size(width * 0.48f, height * 0.48f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                drawLine(goldColor, Offset(width * 0.5f, height * 0.10f), Offset(width * 0.5f, height * 0.22f), 2.2f, StrokeCap.Round)
            }
            PrayerName.ISHA -> {
                // Golden Crescent Moon & Star (Bolder profile)
                val moonPath = Path().apply {
                    moveTo(width * 0.58f, height * 0.12f)
                    cubicTo(
                        width * 0.10f, height * 0.22f,
                        width * 0.10f, height * 0.78f,
                        width * 0.58f, height * 0.88f
                    )
                    cubicTo(
                        width * 0.30f, height * 0.70f,
                        width * 0.30f, height * 0.30f,
                        width * 0.58f, height * 0.12f
                    )
                    close()
                }
                drawPath(moonPath, goldColor, style = Fill)

                // Little star
                val starCenter = Offset(width * 0.75f, height * 0.38f)
                drawCircle(goldColor, radius = width * 0.10f, center = starCenter)
            }
            PrayerName.SAHRI -> {
                // Sahri icon: bold crescent
                val moonPath = Path().apply {
                    moveTo(width * 0.54f, height * 0.15f)
                    cubicTo(width * 0.15f, height * 0.25f, width * 0.15f, height * 0.75f, width * 0.54f, height * 0.85f)
                    cubicTo(width * 0.32f, height * 0.68f, width * 0.32f, height * 0.32f, width * 0.54f, height * 0.15f)
                    close()
                }
                drawPath(moonPath, goldColor, style = Fill)
            }
            PrayerName.IFTAR -> {
                // Iftar icon: setting sun with horizon
                drawLine(goldColor, Offset(0f, height * 0.7f), Offset(width, height * 0.7f), 2.5f, StrokeCap.Round)
                drawArc(
                    color = goldColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(width * 0.26f, height * 0.36f),
                    size = Size(width * 0.48f, height * 0.48f),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun MosqueSilhouetteBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Soft atmospheric light glow radiating from the central mosque dome
        val glowCenter = Offset(w * 0.72f, h * 0.45f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color(0xFF81C784).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = glowCenter,
                radius = w * 0.35f
            ),
            radius = w * 0.35f,
            center = glowCenter
        )

        // Shading colors for 3D depth matching the reference image
        val lightSilhouette = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color(0xFF66BB6A).copy(alpha = 0.12f),
                Color.White.copy(alpha = 0.06f)
            ),
            start = Offset(w * 0.65f, 0f),
            end = Offset(w * 0.85f, h)
        )
        val deepSilhouette = Color.White.copy(alpha = 0.10f)
        val outerMinaretColor = Color.White.copy(alpha = 0.07f)
        val goldFinialColor = AccentGold.copy(alpha = 0.65f)

        // 2. Far Outer Minarets (Layer 1 - Background)
        // Left Far Minaret
        val farLeftM = w * 0.52f
        drawRect(
            color = outerMinaretColor,
            topLeft = Offset(farLeftM, h * 0.32f),
            size = Size(w * 0.016f, h * 0.50f)
        )
        val farLeftCap = Path().apply {
            moveTo(farLeftM - 1f, h * 0.32f)
            lineTo(farLeftM + w * 0.008f, h * 0.23f)
            lineTo(farLeftM + w * 0.016f + 1f, h * 0.32f)
            close()
        }
        drawPath(farLeftCap, outerMinaretColor)

        // Right Far Minaret
        val farRightM = w * 0.91f
        drawRect(
            color = outerMinaretColor,
            topLeft = Offset(farRightM, h * 0.32f),
            size = Size(w * 0.016f, h * 0.50f)
        )
        val farRightCap = Path().apply {
            moveTo(farRightM - 1f, h * 0.32f)
            lineTo(farRightM + w * 0.008f, h * 0.23f)
            lineTo(farRightM + w * 0.016f + 1f, h * 0.32f)
            close()
        }
        drawPath(farRightCap, outerMinaretColor)

        // 3. Middle Side Domes
        // Left Mid Dome
        val leftDomeX = w * 0.63f
        val leftDomeBaseY = h * 0.72f
        val leftDomeR = w * 0.055f
        val leftDomePath = Path().apply {
            moveTo(leftDomeX - leftDomeR, leftDomeBaseY)
            cubicTo(
                leftDomeX - leftDomeR * 0.9f, leftDomeBaseY - leftDomeR * 1.3f,
                leftDomeX, leftDomeBaseY - leftDomeR * 1.6f,
                leftDomeX, leftDomeBaseY - leftDomeR * 1.7f
            )
            cubicTo(
                leftDomeX, leftDomeBaseY - leftDomeR * 1.6f,
                leftDomeX + leftDomeR * 0.9f, leftDomeBaseY - leftDomeR * 1.3f,
                leftDomeX + leftDomeR, leftDomeBaseY
            )
            close()
        }
        drawPath(leftDomePath, deepSilhouette)

        // Right Mid Dome
        val rightDomeX = w * 0.81f
        val rightDomeBaseY = h * 0.72f
        val rightDomeR = w * 0.055f
        val rightDomePath = Path().apply {
            moveTo(rightDomeX - rightDomeR, rightDomeBaseY)
            cubicTo(
                rightDomeX - rightDomeR * 0.9f, rightDomeBaseY - rightDomeR * 1.3f,
                rightDomeX, rightDomeBaseY - rightDomeR * 1.6f,
                rightDomeX, rightDomeBaseY - rightDomeR * 1.7f
            )
            cubicTo(
                rightDomeX, rightDomeBaseY - rightDomeR * 1.6f,
                rightDomeX + rightDomeR * 0.9f, rightDomeBaseY - rightDomeR * 1.3f,
                rightDomeX + rightDomeR, rightDomeBaseY
            )
            close()
        }
        drawPath(rightDomePath, deepSilhouette)

        // 4. Main Grand Dome (Layer 2 - Central 3D Volumetric Dome)
        val domeCenterX = w * 0.72f
        val domeBaseY = h * 0.75f
        val domeRadius = w * 0.095f

        // Dome Base / Drum
        drawRect(
            brush = lightSilhouette,
            topLeft = Offset(domeCenterX - domeRadius * 0.92f, domeBaseY - h * 0.06f),
            size = Size(domeRadius * 1.84f, h * 0.08f)
        )

        // Drum arched window slits
        val windowColor = Color(0xFF1B4D3E).copy(alpha = 0.35f)
        for (i in -2..2) {
            val winX = domeCenterX + i * (domeRadius * 0.32f) - 3f
            drawRoundRect(
                color = windowColor,
                topLeft = Offset(winX, domeBaseY - h * 0.045f),
                size = Size(6f, h * 0.035f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }

        // 3D Pointed Grand Dome Path
        val domePath = Path().apply {
            moveTo(domeCenterX - domeRadius, domeBaseY - h * 0.06f)
            cubicTo(
                domeCenterX - domeRadius * 0.95f, domeBaseY - domeRadius * 1.4f,
                domeCenterX - domeRadius * 0.15f, domeBaseY - domeRadius * 1.85f,
                domeCenterX, domeBaseY - domeRadius * 2.05f
            )
            cubicTo(
                domeCenterX + domeRadius * 0.15f, domeBaseY - domeRadius * 1.85f,
                domeCenterX + domeRadius * 0.95f, domeBaseY - domeRadius * 1.4f,
                domeCenterX + domeRadius, domeBaseY - h * 0.06f
            )
            close()
        }
        drawPath(domePath, lightSilhouette)

        // Golden Crescent Finial at Dome Top Peak
        val finialTopY = domeBaseY - domeRadius * 2.05f
        drawLine(
            color = goldFinialColor,
            start = Offset(domeCenterX, finialTopY),
            end = Offset(domeCenterX, finialTopY - h * 0.07f),
            strokeWidth = 1.5f,
            cap = StrokeCap.Round
        )
        // Crescent on finial
        drawArc(
            color = goldFinialColor,
            startAngle = 130f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = Offset(domeCenterX - 4.5f, finialTopY - h * 0.08f),
            size = Size(9f, 9f),
            style = Stroke(width = 1.3f)
        )

        // 5. Inner Grand Minarets (Layer 3)
        // Left Main Minaret
        val min1X = w * 0.58f
        drawRect(
            brush = lightSilhouette,
            topLeft = Offset(min1X, h * 0.25f),
            size = Size(w * 0.022f, h * 0.55f)
        )
        // Balcony 1
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(min1X - 2.5f, h * 0.36f),
            size = Size(w * 0.022f + 5f, 3.5f)
        )
        // Balcony 2
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(min1X - 2.5f, h * 0.25f),
            size = Size(w * 0.022f + 5f, 3.5f)
        )
        // Conical Roof + Needle
        val min1Roof = Path().apply {
            moveTo(min1X - 1.5f, h * 0.25f)
            lineTo(min1X + w * 0.011f, h * 0.15f)
            lineTo(min1X + w * 0.022f + 1.5f, h * 0.25f)
            close()
        }
        drawPath(min1Roof, lightSilhouette)
        drawLine(
            color = goldFinialColor,
            start = Offset(min1X + w * 0.011f, h * 0.15f),
            end = Offset(min1X + w * 0.011f, h * 0.11f),
            strokeWidth = 1.2f
        )

        // Right Main Minaret
        val min2X = w * 0.86f
        drawRect(
            brush = lightSilhouette,
            topLeft = Offset(min2X, h * 0.25f),
            size = Size(w * 0.022f, h * 0.55f)
        )
        // Balcony 1
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(min2X - 2.5f, h * 0.36f),
            size = Size(w * 0.022f + 5f, 3.5f)
        )
        // Balcony 2
        drawRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(min2X - 2.5f, h * 0.25f),
            size = Size(w * 0.022f + 5f, 3.5f)
        )
        // Conical Roof + Needle
        val min2Roof = Path().apply {
            moveTo(min2X - 1.5f, h * 0.25f)
            lineTo(min2X + w * 0.011f, h * 0.15f)
            lineTo(min2X + w * 0.022f + 1.5f, h * 0.25f)
            close()
        }
        drawPath(min2Roof, lightSilhouette)
        drawLine(
            color = goldFinialColor,
            start = Offset(min2X + w * 0.011f, h * 0.15f),
            end = Offset(min2X + w * 0.011f, h * 0.11f),
            strokeWidth = 1.2f
        )

        // 6. Base Arcade Arches
        val archBaseY = h * 0.77f
        val archW = w * 0.035f
        for (i in 0..7) {
            val startArchX = w * 0.56f + i * (archW * 1.35f)
            val archPath = Path().apply {
                moveTo(startArchX, h)
                lineTo(startArchX, archBaseY + archW * 0.5f)
                cubicTo(
                    startArchX, archBaseY,
                    startArchX + archW, archBaseY,
                    startArchX + archW, archBaseY + archW * 0.5f
                )
                lineTo(startArchX + archW, h)
                close()
            }
            drawPath(archPath, Color.White.copy(alpha = 0.05f))
        }
    }
}
