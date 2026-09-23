package com.example.ui.screens

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Border
import com.example.ui.theme.PrimaryGreen
import com.example.utils.DhikrAudioOption
import com.example.utils.DhikrReminderConfig
import com.example.utils.DhikrReminderManager
import com.example.utils.DhikrType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DhikrReminderScreen(
    type: DhikrType,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val initialConfig = remember { DhikrReminderManager.getConfig(context, type) }

    var isEnabled by remember { mutableStateOf(initialConfig.isEnabled) }
    var intervalMinutes by remember { mutableIntStateOf(initialConfig.intervalMinutes) }
    var selectedAudioId by remember { mutableStateOf(initialConfig.selectedAudioId) }
    var isQuietHoursEnabled by remember { mutableStateOf(initialConfig.isQuietHoursEnabled) }
    var quietStartHour by remember { mutableIntStateOf(initialConfig.quietStartHour) }
    var quietStartMinute by remember { mutableIntStateOf(initialConfig.quietStartMinute) }
    var quietEndHour by remember { mutableIntStateOf(initialConfig.quietEndHour) }
    var quietEndMinute by remember { mutableIntStateOf(initialConfig.quietEndMinute) }

    var currentlyPlayingAudioId by remember { mutableStateOf<String?>(null) }
    var showIntervalDialog by remember { mutableStateOf(false) }
    var showTimeRangeDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val audioOptions = remember(type) {
        if (type == DhikrType.DUROOD) {
            DhikrReminderManager.duroodAudioOptions
        } else {
            DhikrReminderManager.istighfarAudioOptions
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            DhikrReminderManager.stopAudio()
        }
    }

    val screenTitle = if (type == DhikrType.DUROOD) "দুরুদ রিমাইন্ডার" else "ইস্তিগফার রিমাইন্ডার"
    val screenDescription = if (type == DhikrType.DUROOD) {
        "সারাদিন নবীজী (ﷺ)-এর ওপর দুরুদ পড়ার সওয়াব অর্জনে নিয়মিত রিমাইন্ডার পেতে পারেন।"
    } else {
        "সারাদিন মহান আল্লাহর কাছে ক্ষমা প্রার্থনার সওয়াব অর্জনে নিয়মিত রিমাইন্ডার পেতে পারেন।"
    }

    val intervalDisplayStr = when (intervalMinutes) {
        15 -> "15 মিনিট"
        20 -> "20 মিনিট"
        30 -> "30 মিনিট"
        45 -> "45 মিনিট"
        60 -> "1 ঘণ্টা"
        120 -> "2 ঘণ্টা"
        180 -> "3 ঘণ্টা"
        240 -> "4 ঘণ্টা"
        else -> "$intervalMinutes মিনিট"
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "পিছনে যান",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = screenTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                HorizontalDivider(color = Border)
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, Border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = "বাতিল",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            val newConfig = DhikrReminderConfig(
                                isEnabled = isEnabled,
                                intervalMinutes = intervalMinutes,
                                selectedAudioId = selectedAudioId,
                                isQuietHoursEnabled = isQuietHoursEnabled,
                                quietStartHour = quietStartHour,
                                quietStartMinute = quietStartMinute,
                                quietEndHour = quietEndHour,
                                quietEndMinute = quietEndMinute
                            )
                            DhikrReminderManager.saveConfig(context, type, newConfig)
                            Toast.makeText(context, "$screenTitle সফলভাবে সংরক্ষণ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            onBackClick()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "সংরক্ষণ করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                // Header descriptive text
                Text(
                    text = screenDescription,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            item {
                // Master Toggle Switch Row
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isEnabled = !isEnabled }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = screenTitle,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PrimaryGreen,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            if (isEnabled) {
                // Reminder Interval Section
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showIntervalDialog = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Text(
                                text = "রিমাইন্ডার ব্যবধান",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = intervalDisplayStr,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = PrimaryGreen
                            )
                        }
                    }
                }

                // Reminder Audio Section
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "রিমাইন্ডার অডিও",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            audioOptions.forEachIndexed { index, option ->
                                val isSelected = selectedAudioId == option.id
                                val isPlaying = currentlyPlayingAudioId == option.id

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAudioId = option.id
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Play / Pause Audio preview button
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isPlaying) PrimaryGreen else PrimaryGreen.copy(alpha = 0.12f)
                                            )
                                            .clickable {
                                                if (isPlaying) {
                                                    DhikrReminderManager.stopAudio()
                                                    currentlyPlayingAudioId = null
                                                } else {
                                                    currentlyPlayingAudioId = option.id
                                                    DhikrReminderManager.previewAudio(context, option) {
                                                        currentlyPlayingAudioId = null
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Stop" else "Play",
                                            tint = if (isPlaying) Color.White else PrimaryGreen,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Audio Title & Arabic Subtitle
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = option.label,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (option.arabicText.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = option.arabicText,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Custom Checkmark Radio Icon
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) PrimaryGreen else Color.Transparent)
                                            .then(
                                                if (!isSelected) {
                                                    Modifier.background(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = CircleShape
                                                    )
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                if (index < audioOptions.size - 1) {
                                    HorizontalDivider(
                                        color = Border,
                                        thickness = 0.8.dp,
                                        modifier = Modifier.padding(start = 68.dp, end = 14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quiet Hours Section
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, Border)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "রিমাইন্ডার বিরতির সময়",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ব্যস্ততা কিংবা বিশ্রামের সময়গুলোতে রিমাইন্ডার বন্ধ রাখতে রিমাইন্ডার বিরতির সময় সেট করুন।",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            if (isQuietHoursEnabled) {
                                // Active quiet hours entry row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val startStr = DhikrReminderManager.formatTime12Hour(quietStartHour, quietStartMinute)
                                    val endStr = DhikrReminderManager.formatTime12Hour(quietEndHour, quietEndMinute)
                                    Text(
                                        text = "$startStr - $endStr",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    IconButton(
                                        onClick = {
                                            isQuietHoursEnabled = false
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "মুছুন",
                                            tint = Color(0xFFEF5350),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            // "+ বিরতির সময় যোগ করুন" button
                            TextButton(
                                onClick = {
                                    showTimeRangeDialog = true
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isQuietHoursEnabled) "বিরতির সময় পরিবর্তন করুন" else "বিরতির সময় যোগ করুন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryGreen
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Interval Picker Dialog
    if (showIntervalDialog) {
        val intervalOptions = listOf(
            15 to "15 মিনিট",
            20 to "20 মিনিট",
            30 to "30 মিনিট",
            45 to "45 মিনিট",
            60 to "1 ঘণ্টা",
            120 to "2 ঘণ্টা",
            180 to "3 ঘণ্টা",
            240 to "4 ঘণ্টা"
        )

        AlertDialog(
            onDismissRequest = { showIntervalDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "রিমাইন্ডার ব্যবধান নির্বাচন করুন",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    intervalOptions.forEach { (mins, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    intervalMinutes = mins
                                    showIntervalDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = label,
                                fontSize = 15.sp,
                                color = if (intervalMinutes == mins) PrimaryGreen else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (intervalMinutes == mins) FontWeight.Bold else FontWeight.Normal
                            )
                            if (intervalMinutes == mins) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIntervalDialog = false }) {
                    Text("বন্ধ করুন", color = PrimaryGreen)
                }
            }
        )
    }

    // Quiet Hours Range Picker Dialog
    if (showTimeRangeDialog) {
        var tempStartHour by remember { mutableIntStateOf(quietStartHour) }
        var tempStartMin by remember { mutableIntStateOf(quietStartMinute) }
        var tempEndHour by remember { mutableIntStateOf(quietEndHour) }
        var tempEndMin by remember { mutableIntStateOf(quietEndMinute) }

        AlertDialog(
            onDismissRequest = { showTimeRangeDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "বিরতির সময় নির্ধারণ করুন",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "যে সময়ে রিমাইন্ডার বন্ধ রাখতে চান:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Start Time selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        tempStartHour = h
                                        tempStartMin = m
                                    },
                                    tempStartHour,
                                    tempStartMin,
                                    false
                                ).show()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "শুরুর সময়:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DhikrReminderManager.formatTime12Hour(tempStartHour, tempStartMin),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }

                    // End Time selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        tempEndHour = h
                                        tempEndMin = m
                                    },
                                    tempEndHour,
                                    tempEndMin,
                                    false
                                ).show()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "শেষের সময়:",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = DhikrReminderManager.formatTime12Hour(tempEndHour, tempEndMin),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        quietStartHour = tempStartHour
                        quietStartMinute = tempStartMin
                        quietEndHour = tempEndHour
                        quietEndMinute = tempEndMin
                        isQuietHoursEnabled = true
                        showTimeRangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("ঠিক আছে", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeRangeDialog = false }) {
                    Text("বাতিল", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
