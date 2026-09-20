package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.PrayerAlarmSoundType
import com.example.data.model.PrayerName
import com.example.data.model.WaqtAlarmConfig
import com.example.utils.DateUtil
import com.example.utils.PrayerNotificationHelper
import com.example.utils.PrayerSoundManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaqtAlarmConfigDialog(
    prayerName: PrayerName,
    baseTimeFormatted: String = "", // e.g. "০৪:১৩ PM" or "০৪:১৩"
    baseTimestampMillis: Long = 0L,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Load initial config from helper
    val initialConfig = remember(prayerName) {
        PrayerNotificationHelper.getPrayerAlarmConfig(context, prayerName)
    }

    var isAlarmEnabled by remember { mutableStateOf(initialConfig.isEnabled) }
    var offsetMinutes by remember { mutableIntStateOf(initialConfig.offsetMinutes) }
    var sliderPosition by remember { mutableFloatStateOf(initialConfig.offsetMinutes.toFloat()) }
    var selectedSoundType by remember { mutableStateOf(initialConfig.soundType) }
    var isVibrationEnabled by remember { mutableStateOf(initialConfig.isVibrationEnabled) }

    var previewPlayingSoundType by remember { mutableStateOf<PrayerAlarmSoundType?>(null) }

    // Stop audio on exit
    DisposableEffect(Unit) {
        onDispose {
            PrayerSoundManager.stopAll()
        }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isAlarmEnabled = true
            Toast.makeText(context, "নোটিফিকেশন অনুমতি সক্রিয় হয়েছে", Toast.LENGTH_SHORT).show()
        } else {
            isAlarmEnabled = false
            Toast.makeText(context, "নোটিফিকেশন অনুমতি দেওয়া হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    val emeraldGreen = Color(0xFF0D9488)
    val cardBackground = Color(0xFF1E293B)
    val subtleBorder = Color(0xFF334155)
    val lightGreenCircle = emeraldGreen.copy(alpha = 0.2f)
    val lightBlueCircle = Color(0xFF0284C7).copy(alpha = 0.2f)

    // Calculate dynamic time display based on baseTimestampMillis & offset
    val calculatedTimeDisplay = remember(baseTimestampMillis, baseTimeFormatted, offsetMinutes) {
        if (baseTimestampMillis > 0L) {
            val adjustedMillis = baseTimestampMillis + (offsetMinutes * 60 * 1000L)
            val time = java.time.Instant.ofEpochMilli(adjustedMillis)
                .atZone(java.time.ZoneId.of("Asia/Dhaka"))
                .toLocalTime()
            val hour = if (time.hour == 0) 12 else if (time.hour > 12) time.hour - 12 else time.hour
            val minute = time.minute
            val hourStr = DateUtil.toBengaliNumerals(String.format(java.util.Locale.US, "%02d", hour))
            val minStr = DateUtil.toBengaliNumerals(String.format(java.util.Locale.US, "%02d", minute))
            "$hourStr:$minStr"
        } else if (baseTimeFormatted.isNotBlank()) {
            baseTimeFormatted
        } else {
            "ওয়াক্তের সময়"
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            PrayerSoundManager.stopAll()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // 1. Top Bar: Back arrow + Centered Waqt Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        PrayerSoundManager.stopAll()
                        onDismiss()
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "ফিরে যান",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = prayerName.nameBn,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(38.dp)) // balance layout
            }

            // 2. Main Alarm Toggle Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(lightGreenCircle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WbSunny,
                                contentDescription = null,
                                tint = emeraldGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "নামাজের অ্যালার্ম",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAlarmEnabled) "অ্যালার্ম চালু" else "অ্যালার্ম বন্ধ",
                                fontSize = 13.sp,
                                color = if (isAlarmEnabled) emeraldGreen else Color.LightGray.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                            }
                            isAlarmEnabled = checked
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = emeraldGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF475569)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Time Adjustment Slider Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header with Calculated Time Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time Adjustment",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        // Bell Badge with adjusted time
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = emeraldGreen.copy(alpha = 0.2f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, emeraldGreen.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Notifications,
                                    contentDescription = null,
                                    tint = emeraldGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = calculatedTimeDisplay,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = emeraldGreen
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Slider (-30 to +30 min)
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                            offsetMinutes = it.roundToInt()
                        },
                        valueRange = -30f..30f,
                        steps = 59, // 1 min increments
                        enabled = isAlarmEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = emeraldGreen,
                            activeTrackColor = emeraldGreen,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Labels below slider: "-30 min", "Exact time", "+30 min"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "-30 min",
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (offsetMinutes == 0) "Exact time (ঠিক সময়ে)" else "${DateUtil.toBengaliNumerals(kotlin.math.abs(offsetMinutes))} মিনিট ${if (offsetMinutes < 0) "আগে" else "পরে"}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (offsetMinutes == 0) emeraldGreen else Color(0xFF38BDF8)
                        )
                        Text(
                            text = "+30 min",
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            offsetMinutes == 0 -> "ঠিক ওয়াক্ত শুরু হওয়ার মুহূর্তে অ্যালার্ম বাজবে।"
                            offsetMinutes < 0 -> "ওয়াক্ত শুরু হওয়ার ${DateUtil.toBengaliNumerals(-offsetMinutes)} মিনিট আগে প্রস্তুতি নেওয়ার জন্য অ্যালার্ম বাজবে।"
                            else -> "ওয়াক্ত শুরু হওয়ার ${DateUtil.toBengaliNumerals(offsetMinutes)} মিনিট পর অ্যালার্ম বাজবে।"
                        },
                        fontSize = 11.5.sp,
                        color = Color.LightGray.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Alarm Sound Options Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Alarm Sound",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val soundOptions = listOf(
                        PrayerAlarmSoundType.SILENT,
                        PrayerAlarmSoundType.BEEP,
                        PrayerAlarmSoundType.RING,
                        PrayerAlarmSoundType.VOICE_NAME,
                        PrayerAlarmSoundType.NOTIFICATION,
                        PrayerAlarmSoundType.AZAN_MECCA,
                        PrayerAlarmSoundType.AZAN_MADINA
                    )

                    soundOptions.forEachIndexed { index, soundType ->
                        val isSelected = (selectedSoundType == soundType)
                        val isPlaying = (previewPlayingSoundType == soundType && PrayerSoundManager.isPlaying())

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) emeraldGreen.copy(alpha = 0.15f) else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, emeraldGreen.copy(alpha = 0.4f)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedSoundType = soundType
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Custom Radio/Check Circle
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) emeraldGreen else Color.Transparent)
                                            .border(
                                                width = 1.5.dp,
                                                color = if (isSelected) emeraldGreen else Color.LightGray.copy(alpha = 0.4f),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    Column {
                                        Text(
                                            text = soundType.titleBn,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f)
                                        )
                                        Text(
                                            text = soundType.subtitleBn,
                                            fontSize = 11.sp,
                                            color = Color.LightGray.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                // Play / Stop Preview Button (except for silent)
                                if (soundType != PrayerAlarmSoundType.SILENT) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(if (isPlaying) Color(0xFFEF4444).copy(alpha = 0.2f) else emeraldGreen.copy(alpha = 0.15f))
                                            .clickable {
                                                if (isPlaying) {
                                                    PrayerSoundManager.stopAll()
                                                    previewPlayingSoundType = null
                                                } else {
                                                    previewPlayingSoundType = soundType
                                                    PrayerSoundManager.playPreview(
                                                        context = context,
                                                        soundType = soundType,
                                                        prayerName = prayerName
                                                    ) {
                                                        previewPlayingSoundType = null
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) "থামান" else "বাজিয়ে শুনুন",
                                            tint = if (isPlaying) Color(0xFFEF4444) else emeraldGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (index < soundOptions.size - 1) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Vibration Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(lightBlueCircle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Vibration,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Vibration",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Vibrate with alarm",
                                fontSize = 12.5.sp,
                                color = Color.LightGray.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = { isVibrationEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = emeraldGreen,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF475569)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Bottom Full-Width Save Button
            Button(
                onClick = {
                    PrayerSoundManager.stopAll()
                    val newConfig = WaqtAlarmConfig(
                        prayerName = prayerName,
                        isEnabled = isAlarmEnabled,
                        offsetMinutes = offsetMinutes,
                        soundType = selectedSoundType,
                        isVibrationEnabled = isVibrationEnabled
                    )
                    PrayerNotificationHelper.savePrayerAlarmConfig(context, newConfig)
                    Toast.makeText(context, "${prayerName.nameBn} অ্যালার্ম সেটিংস সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = emeraldGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "সংরক্ষণ করুন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
