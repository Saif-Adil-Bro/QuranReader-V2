package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.data.model.AlertCategory
import com.example.data.model.PrayerAlarmSoundType
import com.example.data.model.PrayerName
import com.example.data.model.WaqtAlarmConfig
import com.example.utils.DateUtil
import com.example.utils.DeviceSettingsHelper
import com.example.utils.PrayerNotificationHelper
import com.example.utils.PrayerSoundManager
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaqtAlarmConfigDialog(
    prayerName: PrayerName,
    baseTimeFormatted: String = "", // e.g. "০৪:১৩ PM"
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
    var selectedCategory by remember { mutableStateOf(initialConfig.soundType.category) }
    var customRingtoneUri by remember { mutableStateOf(initialConfig.customRingtoneUri) }
    var customRingtoneTitle by remember { mutableStateOf(initialConfig.customRingtoneTitle) }
    var isVibrationEnabled by remember { mutableStateOf(initialConfig.isVibrationEnabled) }

    var previewPlayingSoundType by remember { mutableStateOf<PrayerAlarmSoundType?>(null) }

    // Stop audio on exit
    DisposableEffect(Unit) {
        onDispose {
            PrayerSoundManager.stopAll()
        }
    }

    // Ringtone Picker Launcher
    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }

            if (uri != null) {
                customRingtoneUri = uri.toString()
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context) ?: "কাস্টম রিংটোন"
                customRingtoneTitle = title
                selectedSoundType = PrayerAlarmSoundType.CUSTOM_RINGTONE
                selectedCategory = AlertCategory.ALARM
                Toast.makeText(context, "রিংটোন নির্বাচিত: $title", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val emeraldGreen = Color(0xFF0D9488)
    val cardBackground = Color(0xFF1E293B)
    val subtleBorder = Color(0xFF334155)
    val lightGreenCircle = emeraldGreen.copy(alpha = 0.2f)
    val lightBlueCircle = Color(0xFF0284C7).copy(alpha = 0.2f)
    val goldColor = Color(0xFFFBBF24)

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
                .fillMaxHeight(0.94f)
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = prayerName.nameBn,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "অ্যালার্ট ও রিংটোন সেটিংস",
                        fontSize = 12.sp,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(38.dp))
            }

            // 2. Main Alert Enable/Disable Toggle Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, subtleBorder),
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
                                text = "ওয়াক্তের সতর্কবার্তা",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isAlarmEnabled) "সতর্কবার্তা চালু আছে" else "সতর্কবার্তা বন্ধ",
                                fontSize = 13.sp,
                                color = if (isAlarmEnabled) emeraldGreen else Color.LightGray.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { checked ->
                            isAlarmEnabled = checked
                            if (!checked) {
                                PrayerSoundManager.stopAll()
                                previewPlayingSoundType = null
                            }
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

            // 3. Category Selector Tabs: "অ্যালার্ম ও আযান" vs "নোটিফিকেশন"
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    // Alarm Tab
                    val isAlarmTab = selectedCategory == AlertCategory.ALARM
                    val alarmTabBg by animateColorAsState(
                        targetValue = if (isAlarmTab) emeraldGreen else Color.Transparent,
                        label = "alarmTabBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(alarmTabBg)
                            .clickable {
                                PrayerSoundManager.stopAll()
                                previewPlayingSoundType = null
                                selectedCategory = AlertCategory.ALARM
                                if (selectedSoundType.isNotification) {
                                    selectedSoundType = if (prayerName == PrayerName.FAJR) PrayerAlarmSoundType.AZAN_FAJR else PrayerAlarmSoundType.AZAN_MECCA
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Alarm,
                                contentDescription = null,
                                tint = if (isAlarmTab) Color.White else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "অ্যালার্ম ও আযান",
                                fontSize = 13.5.sp,
                                fontWeight = if (isAlarmTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAlarmTab) Color.White else Color.LightGray
                            )
                        }
                    }

                    // Notification Tab
                    val isNotifTab = selectedCategory == AlertCategory.NOTIFICATION
                    val notifTabBg by animateColorAsState(
                        targetValue = if (isNotifTab) Color(0xFF0284C7) else Color.Transparent,
                        label = "notifTabBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(notifTabBg)
                            .clickable {
                                PrayerSoundManager.stopAll()
                                previewPlayingSoundType = null
                                selectedCategory = AlertCategory.NOTIFICATION
                                if (selectedSoundType.isAlarm) {
                                    selectedSoundType = PrayerAlarmSoundType.BEEP
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = if (isNotifTab) Color.White else Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "নোটিফিকেশন",
                                fontSize = 13.5.sp,
                                fontWeight = if (isNotifTab) FontWeight.Bold else FontWeight.Medium,
                                color = if (isNotifTab) Color.White else Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Sound Options Card according to active Category
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedCategory == AlertCategory.ALARM) "অ্যালার্ম ও আযান টোন" else "নোটিফিকেশন টোন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.95f)
                        )

                        Text(
                            text = if (selectedCategory == AlertCategory.ALARM) "ফুল অ্যালার্ম বাজবে" else "হালকা অ্যালার্ট হবে",
                            fontSize = 11.5.sp,
                            color = if (selectedCategory == AlertCategory.ALARM) goldColor else Color(0xFF38BDF8)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedCategory == AlertCategory.ALARM) {
                        // Alarm Options: Azan Mecca, Azan Madina, Azan Fajr, Custom Device Ringtone
                        val alarmOptions = listOf(
                            PrayerAlarmSoundType.AZAN_MECCA,
                            PrayerAlarmSoundType.AZAN_MADINA,
                            PrayerAlarmSoundType.AZAN_FAJR,
                            PrayerAlarmSoundType.CUSTOM_RINGTONE
                        )

                        alarmOptions.forEachIndexed { index, soundType ->
                            val isSelected = (selectedSoundType == soundType && selectedCategory == AlertCategory.ALARM)
                            val isPlaying = (previewPlayingSoundType == soundType && PrayerSoundManager.isPlaying())

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) emeraldGreen.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, emeraldGreen.copy(alpha = 0.5f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedSoundType = soundType
                                        selectedCategory = AlertCategory.ALARM
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
                                        // Custom Radio Indicator
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    2.dp,
                                                    if (isSelected) emeraldGreen else Color.LightGray.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                                .background(if (isSelected) emeraldGreen else Color.Transparent),
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

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = if (soundType == PrayerAlarmSoundType.CUSTOM_RINGTONE && !customRingtoneTitle.isNullOrBlank()) {
                                                        customRingtoneTitle!!
                                                    } else {
                                                        soundType.titleBn
                                                    },
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                                                )
                                                if (soundType == PrayerAlarmSoundType.CUSTOM_RINGTONE) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = goldColor.copy(alpha = 0.15f),
                                                        border = BorderStroke(0.5.dp, goldColor.copy(alpha = 0.4f))
                                                    ) {
                                                        Text(
                                                            text = "ফোন রিংটোন",
                                                            fontSize = 10.sp,
                                                            color = goldColor,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = if (soundType == PrayerAlarmSoundType.CUSTOM_RINGTONE) {
                                                    if (customRingtoneTitle != null) "ফোনের নিজস্ব রিংটোন সেট করা হয়েছে" else soundType.subtitleBn
                                                } else soundType.subtitleBn,
                                                fontSize = 11.5.sp,
                                                color = Color.LightGray.copy(alpha = 0.65f)
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Pick Ringtone Button for Custom Ringtone
                                        if (soundType == PrayerAlarmSoundType.CUSTOM_RINGTONE) {
                                            OutlinedButton(
                                                onClick = {
                                                    PrayerSoundManager.stopAll()
                                                    previewPlayingSoundType = null
                                                    val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM or RingtoneManager.TYPE_RINGTONE)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "ওয়াক্তের অ্যালার্ম রিংটোন নির্বাচন করুন")
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                                        if (!customRingtoneUri.isNullOrBlank()) {
                                                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(customRingtoneUri))
                                                        }
                                                    }
                                                    ringtonePickerLauncher.launch(pickerIntent)
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .padding(end = 6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.LibraryMusic,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "রিংটোন বাছুন",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        // Play / Stop Preview Button
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
                                                        if (isVibrationEnabled) {
                                                            PrayerSoundManager.triggerVibration(context, isRepeating = false)
                                                        }
                                                        PrayerSoundManager.playPreview(
                                                            context = context,
                                                            soundType = soundType,
                                                            prayerName = prayerName,
                                                            customRingtoneUri = customRingtoneUri
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

                            if (index < alarmOptions.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    } else {
                        // Notification Options: Beep, Ring, Voice Name, Default Notification, Silent
                        val notifOptions = listOf(
                            PrayerAlarmSoundType.BEEP,
                            PrayerAlarmSoundType.RING,
                            PrayerAlarmSoundType.VOICE_NAME,
                            PrayerAlarmSoundType.NOTIFICATION,
                            PrayerAlarmSoundType.SILENT
                        )

                        notifOptions.forEachIndexed { index, soundType ->
                            val isSelected = (selectedSoundType == soundType && selectedCategory == AlertCategory.NOTIFICATION)
                            val isPlaying = (previewPlayingSoundType == soundType && PrayerSoundManager.isPlaying())

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFF0284C7).copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSelected) BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.5f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedSoundType = soundType
                                        selectedCategory = AlertCategory.NOTIFICATION
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
                                        // Radio indicator
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .border(
                                                    2.dp,
                                                    if (isSelected) Color(0xFF0284C7) else Color.LightGray.copy(alpha = 0.4f),
                                                    CircleShape
                                                )
                                                .background(if (isSelected) Color(0xFF0284C7) else Color.Transparent),
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

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = soundType.titleBn,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.85f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = soundType.subtitleBn,
                                                fontSize = 11.5.sp,
                                                color = Color.LightGray.copy(alpha = 0.65f)
                                            )
                                        }
                                    }

                                    // Play / Stop Preview Button (except for silent)
                                    if (soundType != PrayerAlarmSoundType.SILENT) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (isPlaying) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.15f))
                                                .clickable {
                                                    if (isPlaying) {
                                                        PrayerSoundManager.stopAll()
                                                        previewPlayingSoundType = null
                                                    } else {
                                                        previewPlayingSoundType = soundType
                                                        if (isVibrationEnabled) {
                                                            PrayerSoundManager.triggerVibration(context, isRepeating = false)
                                                        }
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
                                                tint = if (isPlaying) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (index < notifOptions.size - 1) {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Time Offset Adjustment Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সময় সমন্বয় (Offset)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.9f)
                        )

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = emeraldGreen.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, emeraldGreen.copy(alpha = 0.5f))
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

                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            sliderPosition = it
                            offsetMinutes = it.roundToInt()
                        },
                        valueRange = -30f..30f,
                        steps = 59,
                        enabled = isAlarmEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = emeraldGreen,
                            activeTrackColor = emeraldGreen,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "-৩০ মি.",
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (offsetMinutes == 0) "ঠিক ওয়াক্তের শুরুতে" else "${DateUtil.toBengaliNumerals(kotlin.math.abs(offsetMinutes))} মিনিট ${if (offsetMinutes < 0) "আগে" else "পরে"}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (offsetMinutes == 0) emeraldGreen else Color(0xFF38BDF8)
                        )
                        Text(
                            text = "+৩০ মি.",
                            fontSize = 12.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when {
                            offsetMinutes == 0 -> "ঠিক ওয়াক্ত শুরু হওয়ার মুহূর্তে সতর্কবার্তা দেওয়া হবে।"
                            offsetMinutes < 0 -> "ওয়াক্ত শুরু হওয়ার ${DateUtil.toBengaliNumerals(-offsetMinutes)} মিনিট আগে প্রস্তুতি নেওয়ার জন্য সতর্কবার্তা দেওয়া হবে।"
                            else -> "ওয়াক্ত শুরু হওয়ার ${DateUtil.toBengaliNumerals(offsetMinutes)} মিনিট পর সতর্কবার্তা দেওয়া হবে।"
                        },
                        fontSize = 11.5.sp,
                        color = Color.LightGray.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Vibration Card
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = cardBackground,
                border = BorderStroke(1.dp, subtleBorder),
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
                                text = "ভাইব্রেশন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "অ্যালার্টের সাথে ভাইব্রেশন হবে",
                                fontSize = 12.5.sp,
                                color = Color.LightGray.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = { checked ->
                            isVibrationEnabled = checked
                            if (checked) {
                                PrayerSoundManager.triggerVibration(context)
                            }
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

            Spacer(modifier = Modifier.height(20.dp))

            // 7. Bottom Full-Width Save Button
            Button(
                onClick = {
                    PrayerSoundManager.stopAll()
                    val newConfig = WaqtAlarmConfig(
                        prayerName = prayerName,
                        isEnabled = isAlarmEnabled,
                        offsetMinutes = offsetMinutes,
                        soundType = selectedSoundType,
                        customRingtoneUri = customRingtoneUri,
                        customRingtoneTitle = customRingtoneTitle,
                        isVibrationEnabled = isVibrationEnabled
                    )
                    PrayerNotificationHelper.savePrayerAlarmConfig(context, newConfig)
                    if (isAlarmEnabled && !DeviceSettingsHelper.isBatteryOptimizationIgnored(context)) {
                        DeviceSettingsHelper.openBatteryOptimizationSettings(context)
                    }
                    Toast.makeText(context, "${prayerName.nameBn} অ্যালার্ট সেটিংস সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
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
