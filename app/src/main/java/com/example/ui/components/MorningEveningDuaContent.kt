package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DuaData
import com.example.data.MorningEveningSection
import com.example.ui.theme.PrimaryGreen
import com.example.utils.AdhkarProgressManager
import java.time.LocalTime

private fun formatBnDigits(num: Int): String {
    val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return num.toString().map { ch ->
        if (ch.isDigit()) bnDigits[ch - '0'] else ch
    }.joinToString("")
}

/**
 * 3-Card Hub for Morning & Evening Duas:
 * - Top: Compact Fazilat banner card
 * - Bottom: 2 side-by-side large cards (Morning Duas on left, Evening Duas on right)
 *   with time-of-day active badge highlight and daily completion progress.
 */
@Composable
fun MorningEveningHub(
    isEn: Boolean,
    onSelectSection: (MorningEveningSection) -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    // Ensure data is loaded
    LaunchedEffect(Unit) {
        DuaData.initialize(context)
    }

    val fazilatList = DuaData.fazilatDuas
    val morningList = DuaData.morningDuas
    val eveningList = DuaData.eveningDuas

    val morningCompleted = remember(morningList) {
        AdhkarProgressManager.getCompletedCount(context, MorningEveningSection.MORNING, morningList)
    }
    val eveningCompleted = remember(eveningList) {
        AdhkarProgressManager.getCompletedCount(context, MorningEveningSection.EVENING, eveningList)
    }

    val currentHour = remember { LocalTime.now().hour }
    val isMorningActive = currentHour in 4..11
    val isEveningActive = currentHour in 15..23

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. TOP COMPACT BANNER CARD: ফযীলত (Fazilat)
        val fazilatGradient = if (isDark) {
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF134E4A), Color(0xFF065F46))
            )
        } else {
            Brush.horizontalGradient(
                colors = listOf(Color(0xFF0F766E), Color(0xFF059669))
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { onSelectSection(MorningEveningSection.FAZILAT) },
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(fazilatGradient)
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEn) "Virtues of Adhkar (Fazilat)" else "যিকর ও দোয়ার ফযীলত",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val countStr = if (isEn) "${fazilatList.size} Virtues & Hadiths" else "${formatBnDigits(fazilatList.size)}টি ফযীলত ও হাদীস"
                        Text(
                            text = countStr,
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // 2. BOTTOM TWO SIDE-BY-SIDE LARGE CARDS: সকালের দোয়া (Left) & সন্ধ্যার দোয়া (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left: Morning Duas
            TimeOfDayDuaCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                title = if (isEn) "Morning Duas" else "সকালের দোয়া",
                subtitle = if (isEn) "After Fajr till Sunrise" else "ফজরের পর থেকে সূর্যোদয়",
                totalCount = morningList.size,
                completedCount = morningCompleted,
                isActiveNow = isMorningActive,
                activeBadgeText = if (isEn) "Morning Now" else "সকালের সময়",
                icon = Icons.Default.WbSunny,
                accentColor = Color(0xFFF59E0B),
                gradientColors = if (isDark) {
                    listOf(Color(0xFF3B2810), Color(0xFF23180A))
                } else {
                    listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
                },
                isDark = isDark,
                isEn = isEn,
                onClick = { onSelectSection(MorningEveningSection.MORNING) }
            )

            // Right: Evening Duas
            TimeOfDayDuaCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                title = if (isEn) "Evening Duas" else "সন্ধ্যার দোয়া",
                subtitle = if (isEn) "After Asr till Maghrib" else "আসরের পর থেকে সন্ধ্যা",
                totalCount = eveningList.size,
                completedCount = eveningCompleted,
                isActiveNow = isEveningActive,
                activeBadgeText = if (isEn) "Evening Now" else "সন্ধ্যার সময়",
                icon = Icons.Default.NightsStay,
                accentColor = Color(0xFF6366F1),
                gradientColors = if (isDark) {
                    listOf(Color(0xFF1E1B4B), Color(0xFF13142E))
                } else {
                    listOf(Color(0xFFEEF2FF), Color(0xFFE0E7FF))
                },
                isDark = isDark,
                isEn = isEn,
                onClick = { onSelectSection(MorningEveningSection.EVENING) }
            )
        }

        // Subtle daily reset info strip
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isEn) {
                        "Your daily recitation progress resets automatically each new day."
                    } else {
                        "প্রতিদিনের সকাল ও সন্ধ্যার আমলের হিসাব পরের দিন স্বয়ংক্রিয়ভাবে নতুন করে শুরু হবে।"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun TimeOfDayDuaCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    totalCount: Int,
    completedCount: Int,
    isActiveNow: Boolean,
    activeBadgeText: String,
    icon: ImageVector,
    accentColor: Color,
    gradientColors: List<Color>,
    isDark: Boolean,
    isEn: Boolean,
    onClick: () -> Unit
) {
    val progress = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val borderStroke = if (isActiveNow) {
        BorderStroke(1.8.dp, accentColor)
    } else {
        BorderStroke(1.dp, accentColor.copy(alpha = if (isDark) 0.25f else 0.22f))
    }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        border = borderStroke,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isActiveNow) 4.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(gradientColors))
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top row: Icon + Active badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    if (isActiveNow) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor
                        ) {
                            Text(
                                text = activeBadgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom progress section
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val countLabel = if (isEn) {
                        "$totalCount Duas"
                    } else {
                        "${formatBnDigits(totalCount)}টি দোয়া"
                    }
                    Text(
                        text = countLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )

                    val progressLabel = if (isEn) {
                        "$completedCount/$totalCount"
                    } else {
                        "${formatBnDigits(completedCount)}/${formatBnDigits(totalCount)}"
                    }
                    Text(
                        text = progressLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (completedCount >= totalCount && totalCount > 0) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (completedCount >= totalCount && totalCount > 0) PrimaryGreen else accentColor,
                    trackColor = accentColor.copy(alpha = if (isDark) 0.18f else 0.16f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Interactive daily recitation counter / completion pill shown in the detail view
 * for Morning and Evening Duas.
 */
@Composable
fun AdhkarRecitationCounterBar(
    currentCount: Int,
    targetCount: Int,
    isEn: Boolean,
    onTap: () -> Unit,
    onReset: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val safeTarget = targetCount.coerceAtLeast(1)
    val isCompleted = currentCount >= safeTarget
    val progress = (currentCount.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCompleted) {
            PrimaryGreen.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isCompleted) PrimaryGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTap()
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isCompleted) PrimaryGreen else PrimaryGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        } else {
                            Text(
                                text = if (isEn) currentCount.toString() else formatBnDigits(currentCount),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        val mainText = when {
                            isCompleted -> if (isEn) "Completed Today" else "আজকের আমল সম্পন্ন ✓"
                            safeTarget == 1 -> if (isEn) "Tap to mark as recited" else "পড়া শেষে ট্যাপ করে চিহ্নিত করুন"
                            else -> if (isEn) {
                                "Recited $currentCount of $safeTarget times"
                            } else {
                                "পঠিত: ${formatBnDigits(currentCount)} / ${formatBnDigits(safeTarget)} বার"
                            }
                        }
                        Text(
                            text = mainText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isCompleted) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                        )
                        if (!isCompleted && safeTarget > 1) {
                            Text(
                                text = if (isEn) "Tap anywhere here to count" else "গণনা করতে এখানে ট্যাপ করুন",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (currentCount > 0) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = if (isEn) "Reset" else "রিসেট",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (safeTarget > 1) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = PrimaryGreen,
                    trackColor = PrimaryGreen.copy(alpha = 0.12f)
                )
            }
        }
    }
}
