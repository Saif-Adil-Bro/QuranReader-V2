package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.PrayerAlarmSoundType
import com.example.data.model.PrayerName
import com.example.data.model.WaqtAlarmConfig
import com.example.utils.DateUtil
import com.example.utils.PrayerNotificationHelper
import com.example.utils.PrayerSoundManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaqtAlarmOverviewSheet(
    schedule: DailyPrayerSchedule,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isMasterEnabled by remember { mutableStateOf(PrayerNotificationHelper.isMasterEnabled(context)) }
    var selectedWaqtForEdit by remember { mutableStateOf<PrayerName?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    val emeraldGreen = Color(0xFF0D9488)
    val cardBackground = Color(0xFF1E293B)
    val subtleBorder = Color(0xFF334155)

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isMasterEnabled = true
            PrayerNotificationHelper.setMasterEnabled(context, true)
            Toast.makeText(context, "ওয়াক্তের অ্যালার্ম চালু হয়েছে", Toast.LENGTH_SHORT).show()
        } else {
            isMasterEnabled = false
            Toast.makeText(context, "নোটিফিকেশন অনুমতি প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    // If user selected a waqt to edit, show the dedicated customization dialog
    if (selectedWaqtForEdit != null) {
        val pName = selectedWaqtForEdit!!
        val matchingPrayer = schedule.prayers.find { it.name == pName }
        val formattedTime = when (pName) {
            PrayerName.SAHRI -> schedule.sahriEndTimeFormatted
            PrayerName.IFTAR -> schedule.iftarTimeFormatted
            PrayerName.TAHAJJUD -> schedule.tahajjudEndTimeFormatted
            else -> matchingPrayer?.timeFormatted ?: ""
        }
        val timestampMillis = matchingPrayer?.timestampMillis ?: 0L

        WaqtAlarmConfigDialog(
            prayerName = pName,
            baseTimeFormatted = formattedTime,
            baseTimestampMillis = timestampMillis,
            onDismiss = {
                selectedWaqtForEdit = null
                reloadTrigger++
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(emeraldGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Alarm,
                            contentDescription = null,
                            tint = emeraldGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "নামাজ ও ইবাদত অ্যালার্ম",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "ওয়াক্ত অনুযায়ী কাস্টম আজান ও অ্যালার্ম শিডিউল",
                            fontSize = 11.5.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "বন্ধ করুন",
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Master Alarm Switch Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, subtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isMasterEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = if (isMasterEnabled) emeraldGreen else Color.LightGray.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "সকল ওয়াক্তের অ্যালার্ম",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isMasterEnabled) "অ্যালার্ম সিস্টেম সক্রিয় রয়েছে" else "সকল অ্যালার্ম সাময়িকভাবে বন্ধ",
                                fontSize = 11.5.sp,
                                color = if (isMasterEnabled) emeraldGreen else Color.LightGray.copy(alpha = 0.6f)
                            )
                        }
                    }

                    Switch(
                        checked = isMasterEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                        notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        return@Switch
                                    }
                                }
                                isMasterEnabled = true
                                PrayerNotificationHelper.setMasterEnabled(context, true)
                            } else {
                                isMasterEnabled = false
                                PrayerNotificationHelper.setMasterEnabled(context, false)
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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ওয়াক্তভিত্তিক অ্যালার্ম কনফিগারেশন:",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.LightGray.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            val waqtList = listOf(
                PrayerName.FAJR,
                PrayerName.SUNRISE,
                PrayerName.DHUHR,
                PrayerName.ASR,
                PrayerName.MAGHRIB,
                PrayerName.ISHA,
                PrayerName.TAHAJJUD,
                PrayerName.SAHRI,
                PrayerName.IFTAR
            )

            waqtList.forEach { pName ->
                val config = remember(pName, reloadTrigger) {
                    PrayerNotificationHelper.getPrayerAlarmConfig(context, pName)
                }
                val prayerItem = schedule.prayers.find { it.name == pName }
                val timeStr = when (pName) {
                    PrayerName.SAHRI -> schedule.sahriEndTimeFormatted
                    PrayerName.IFTAR -> schedule.iftarTimeFormatted
                    PrayerName.TAHAJJUD -> schedule.tahajjudEndTimeFormatted
                    else -> prayerItem?.timeFormatted ?: ""
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (config.isEnabled) cardBackground else cardBackground.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (config.isEnabled) subtleBorder else subtleBorder.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            selectedWaqtForEdit = pName
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = pName.icon,
                                fontSize = 20.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = pName.nameBn,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (config.isEnabled) Color.White else Color.LightGray.copy(alpha = 0.6f)
                                    )
                                    if (timeStr.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "($timeStr)",
                                            fontSize = 12.sp,
                                            color = emeraldGreen
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = config.soundType.titleBn,
                                        fontSize = 11.5.sp,
                                        color = if (config.soundType == PrayerAlarmSoundType.SILENT) Color.LightGray.copy(alpha = 0.5f) else Color(0xFF38BDF8)
                                    )
                                    if (config.offsetMinutes != 0) {
                                        Text(
                                            text = " • ${DateUtil.toBengaliNumerals(kotlin.math.abs(config.offsetMinutes))} মি. ${if (config.offsetMinutes < 0) "আগে" else "পরে"}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                    if (config.isVibrationEnabled) {
                                        Text(
                                            text = " • ভাইব্রেশন",
                                            fontSize = 11.5.sp,
                                            color = Color.LightGray.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Quick Toggle Switch
                            Switch(
                                checked = config.isEnabled,
                                onCheckedChange = { checked ->
                                    val updated = config.copy(isEnabled = checked)
                                    PrayerNotificationHelper.savePrayerAlarmConfig(context, updated)
                                    reloadTrigger++
                                },
                                modifier = Modifier.height(28.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = emeraldGreen,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color(0xFF475569)
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "কাস্টমাইজ করুন",
                                tint = Color.LightGray.copy(alpha = 0.5f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
