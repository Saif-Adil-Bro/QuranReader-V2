package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.utils.DateUtil
import com.example.utils.HijriCalendarUtil
import java.time.LocalDate

/**
 * Modern, spiritual and elegant Hijri Date Adjustment Dialog.
 * Synchronized with the app's Hijri calendar calculation, providing real-time preview of adjusted dates.
 */
@Composable
fun ModernHijriAdjustmentDialog(
    currentOffset: Int,
    globalOffset: Int = -1,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onSaveOffset: (Int) -> Unit
) {
    // Always initially select 0 (Normal / ০ দিন স্বাভাবিক) when opening the dialog as confirmed by user
    var selectedOffset by remember { mutableIntStateOf(0) }
    val today = remember { LocalDate.now() }

    // Total effective offset combining user's selected personal offset and Remote Config global offset
    val effectiveOffset = remember(selectedOffset, globalOffset) {
        selectedOffset + globalOffset
    }

    // Live preview of the Hijri date based on total effective offset
    val previewHijri = remember(effectiveOffset) {
        HijriCalendarUtil.getHijriDate(today, effectiveOffset)
    }

    val dayBn = DateUtil.toBengaliNumerals(previewHijri.hijriDay)
    val yearBn = DateUtil.toBengaliNumerals(previewHijri.hijriYear)

    val displayedHijriDate = if (isEnglish) {
        "${previewHijri.hijriDay} ${previewHijri.hijriMonthNameEn} ${previewHijri.hijriYear} AH"
    } else {
        "$dayBn ${previewHijri.hijriMonthNameBn} $yearBn হিজরী"
    }

    val gregorianFormatted = if (isEnglish) {
        val monthEn = today.month.name.lowercase().replaceFirstChar { it.uppercase() }
        "${today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${today.dayOfMonth} $monthEn ${today.year}"
    } else {
        val bar = when (today.dayOfWeek) {
            java.time.DayOfWeek.SATURDAY -> "শনিবার"
            java.time.DayOfWeek.SUNDAY -> "রবিবার"
            java.time.DayOfWeek.MONDAY -> "সোমবার"
            java.time.DayOfWeek.TUESDAY -> "মঙ্গলবার"
            java.time.DayOfWeek.WEDNESDAY -> "বুধবার"
            java.time.DayOfWeek.THURSDAY -> "বৃহস্পতিবার"
            java.time.DayOfWeek.FRIDAY -> "শুক্রবার"
        }
        val monthBn = when (today.monthValue) {
            1 -> "জানুয়ারি"
            2 -> "ফেব্রুয়ারি"
            3 -> "মার্চ"
            4 -> "এপ্রিল"
            5 -> "মে"
            6 -> "জুন"
            7 -> "জুলাই"
            8 -> "আগস্ট"
            9 -> "সেপ্টেম্বর"
            10 -> "অক্টোবর"
            11 -> "নভেম্বর"
            else -> "ডিসেম্বর"
        }
        "$bar, ${DateUtil.toBengaliNumerals(today.dayOfMonth)} $monthBn ${DateUtil.toBengaliNumerals(today.year)}"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF061E14)
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.2.dp,
                brush = Brush.verticalGradient(
                    listOf(Color(0xFF34D399).copy(alpha = 0.6f), Color(0xFF0F5A3E).copy(alpha = 0.2f))
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Icon and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.16f))
                                .border(1.dp, Color(0xFF34D399).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NightlightRound,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Hijri Date Adjustment" else "হিজরি তারিখ সমন্বয়",
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isEnglish) "Calendar Moon Sighting Sync" else "চাঁদ দেখা সাপেক্ষে ক্যালেন্ডার সংশোধন",
                                fontSize = 11.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFFE2E8F0),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Live Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0B3322),
                                    Color(0xFF072418)
                                )
                            )
                        )
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = gregorianFormatted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Animated Hijri Date Text
                        AnimatedContent(
                            targetState = displayedHijriDate,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "HijriDateAnimation"
                        ) { targetDate ->
                            Text(
                                text = targetDate,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF34D399),
                                textAlign = TextAlign.Center,
                                lineHeight = 28.sp
                            )
                        }

                        if (previewHijri.hijriMonthNameAr.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${previewHijri.hijriDay} ${previewHijri.hijriMonthNameAr} ${previewHijri.hijriYear} هـ",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Helpful Info Note
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnglish) {
                            "Adjust +/- if your local Islamic crescent sighting differs by 1 or 2 days."
                        } else {
                            "স্থানীয় চাঁদ দেখার ভিত্তিতে তারিখ ১ বা ২ দিন কম-বেশি হলে এখান থেকে সংশোধন করতে পারেন।"
                        },
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFFE2E8F0)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Stepper Control
                Text(
                    text = if (isEnglish) "Select Offset (Days):" else "সমন্বয়ের পরিমাণ (দিন):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF03140D))
                        .border(1.dp, Color(0xFF10B981).copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Minus Button
                    IconButton(
                        onClick = {
                            if (selectedOffset > -3) selectedOffset -= 1
                        },
                        enabled = selectedOffset > -3,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (selectedOffset > -3) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "Decrease",
                            tint = if (selectedOffset > -3) Color(0xFF34D399) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Offset Display
                    val offsetText = when {
                        selectedOffset == 0 -> if (isEnglish) "0 Day (Default)" else "০ দিন (স্বাভাবিক)"
                        selectedOffset > 0 -> if (isEnglish) "+$selectedOffset Day" else "+${DateUtil.toBengaliNumerals(selectedOffset)} দিন"
                        else -> if (isEnglish) "$selectedOffset Day" else "${DateUtil.toBengaliNumerals(selectedOffset)} দিন"
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = offsetText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedOffset == 0) Color(0xFFF1F5F9) else Color(0xFF34D399)
                        )
                        if (selectedOffset != 0) {
                            Text(
                                text = if (selectedOffset > 0) {
                                    if (isEnglish) "(Advanced by $selectedOffset)" else "(${DateUtil.toBengaliNumerals(selectedOffset)} দিন বেশি/অগ্রবর্তী)"
                                } else {
                                    if (isEnglish) "(Delayed by ${-selectedOffset})" else "(${DateUtil.toBengaliNumerals(-selectedOffset)} দিন কম/পশ্চাৎবর্তী)"
                                },
                                fontSize = 10.5.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    // Plus Button
                    IconButton(
                        onClick = {
                            if (selectedOffset < 3) selectedOffset += 1
                        },
                        enabled = selectedOffset < 3,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (selectedOffset < 3) Color(0xFF10B981).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.04f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase",
                            tint = if (selectedOffset < 3) Color(0xFF34D399) else Color(0xFF64748B),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Preset Chips: -1, 0, +1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(-1, 0, 1)
                    presets.forEach { offset ->
                        val isSelected = (selectedOffset == offset)
                        val label = when (offset) {
                            -1 -> if (isEnglish) "-1 Day" else "-১ দিন"
                            0 -> if (isEnglish) "Default (0)" else "স্বাভাবিক (০)"
                            1 -> if (isEnglish) "+1 Day" else "+১ দিন"
                            else -> "$offset"
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) Color(0xFF10B981).copy(alpha = 0.28f) else Color.White.copy(alpha = 0.05f))
                                .border(
                                    width = if (isSelected) 1.2.dp else 0.8.dp,
                                    color = if (isSelected) Color(0xFF34D399) else Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedOffset = offset }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color(0xFF34D399) else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFCBD5E1)
                        )
                    ) {
                        Text(
                            text = if (isEnglish) "Cancel" else "বাতিল",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            onSaveOffset(selectedOffset)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                tint = Color(0xFF022013),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "Save & Apply" else "সংরক্ষণ করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF022013)
                            )
                        }
                    }
                }
            }
        }
    }
}
