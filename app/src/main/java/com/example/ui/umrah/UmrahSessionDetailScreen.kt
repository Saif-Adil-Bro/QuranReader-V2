package com.example.ui.umrah

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.umrah.UmrahChecklistEntity
import com.example.data.umrah.UmrahContentData
import com.example.data.umrah.UmrahRepository
import com.example.data.umrah.UmrahRoundLogEntity
import com.example.data.umrah.UmrahSessionEntity
import com.example.utils.DateUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmrahSessionDetailScreen(
    sessionId: Long,
    isEnglish: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { UmrahRepository.getInstance(context) }

    val session by repository.getSessionById(sessionId).collectAsState(initial = null)
    val checklist by repository.getChecklistForSession(sessionId).collectAsState(initial = emptyList())
    val roundLogs by repository.getRoundLogsForSession(sessionId).collectAsState(initial = emptyList())

    val amPmFormat = remember { SimpleDateFormat("hh:mm a", Locale.ENGLISH) }
    val dateFormat = remember(isEnglish) {
        if (isEnglish) SimpleDateFormat("dd MMMM, yyyy", Locale.ENGLISH)
        else SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD"))
    }

    fun formatAmPmTime(timeMillis: Long?): String {
        if (timeMillis == null) return ""
        val formatted = amPmFormat.format(Date(timeMillis))
        return if (isEnglish) formatted else DateUtil.toBengaliNumerals(formatted)
    }

    fun formatDuration(start: Long?, end: Long?): String {
        if (start == null || end == null || end < start) return if (isEnglish) "0 min" else "০ মিনিট"
        val totalSeconds = (end - start) / 1000
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        return if (isEnglish) {
            when {
                hours > 0 && mins > 0 -> "${hours}h ${mins}m"
                hours > 0 -> "${hours}h"
                mins > 0 -> "${mins}m"
                else -> "${totalSeconds}s"
            }
        } else {
            when {
                hours > 0 && mins > 0 -> "${DateUtil.toBengaliNumerals(hours)} ঘণ্টা ${DateUtil.toBengaliNumerals(mins)} মিনিট"
                hours > 0 -> "${DateUtil.toBengaliNumerals(hours)} ঘণ্টা"
                mins > 0 -> "${DateUtil.toBengaliNumerals(mins)} মিনিট"
                else -> "${DateUtil.toBengaliNumerals(totalSeconds)} সেকেন্ড"
            }
        }
    }

    fun formatAmPmRange(start: Long?, end: Long?): String {
        if (start == null) return if (isEnglish) "03:18 PM - 03:19 PM" else "০৩:১৮ PM - ০৩:১৯ PM"
        val s = formatAmPmTime(start)
        val e = if (end != null) formatAmPmTime(end) else s
        return "$s - $e"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Session Details" else "সেশনের বিবরণ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F181F)
                )
            )
        },
        containerColor = Color(0xFF0F181F)
    ) { paddingValues ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF10B981))
            }
        } else {
            val curr = session!!
            val rawDate = dateFormat.format(Date(curr.startTime))
            val dateStr = if (isEnglish) rawDate else DateUtil.toBengaliNumerals(rawDate)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                // Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF165243)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = if (isEnglish) "Completed" else "সম্পন্ন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF6EE7B7),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = dateStr,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = formatAmPmRange(curr.startTime, curr.endTime ?: curr.startTime),
                                fontSize = 14.sp,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                }

                // Section: Timeline
                item {
                    Text(
                        text = if (isEnglish) "Session Timeline" else "সেশন টাইমলাইন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16232D)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TimelineStepRow(
                                icon = Icons.Default.Person,
                                title = if (isEnglish) "Ihram" else "ইহরাম",
                                timeText = formatAmPmRange(curr.ihramStartTime ?: curr.startTime, curr.ihramEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.ihramStartTime ?: curr.startTime, curr.ihramEndTime ?: curr.startTime),
                                isLast = false
                            )

                            TimelineStepRow(
                                icon = Icons.Default.Sync,
                                title = if (isEnglish) "Tawaf" else "তাওয়াফ",
                                timeText = formatAmPmRange(curr.tawafStartTime ?: curr.startTime, curr.tawafEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.tawafStartTime ?: curr.startTime, curr.tawafEndTime ?: curr.startTime),
                                isLast = false
                            )

                            TimelineStepRow(
                                icon = Icons.Default.DirectionsWalk,
                                title = if (isEnglish) "Sa'i" else "সাঈ",
                                timeText = formatAmPmRange(curr.saiStartTime ?: curr.startTime, curr.saiEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.saiStartTime ?: curr.startTime, curr.saiEndTime ?: curr.startTime),
                                isLast = false
                            )

                            TimelineStepRow(
                                icon = Icons.Default.ContentCut,
                                title = if (isEnglish) "Halq / Qasr" else "হলক / কসর",
                                timeText = formatAmPmRange(curr.halqStartTime ?: curr.startTime, curr.halqEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.halqStartTime ?: curr.startTime, curr.halqEndTime ?: curr.startTime),
                                isLast = true
                            )
                        }
                    }
                }

                // Section: Ritual Log
                item {
                    Text(
                        text = if (isEnglish) "Ritual Log" else "রিচুয়াল লগ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // 1. IHRAM LOG
                item {
                    val ihramItems = checklist.filter { it.stepKey == "IHRAM" }
                    RitualLogHeader(
                        icon = Icons.Default.Person,
                        title = if (isEnglish) "Ihram Log" else "ইহরাম",
                        timeText = formatAmPmRange(curr.ihramStartTime ?: curr.startTime, curr.ihramEndTime ?: curr.startTime),
                        badgeText = "COMPLETED"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF132A32)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ihramItems.forEach { item ->
                                val text = UmrahContentData.getChecklistText(item.stepKey, item.itemIndex, isEnglish)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = text,
                                        fontSize = 13.sp,
                                        color = Color(0xFFCBD5E1),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. TAWAF LOG
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RitualLogHeader(
                        icon = Icons.Default.Sync,
                        title = if (isEnglish) "Tawaf Log" else "তাওয়াফ লগ",
                        timeText = formatAmPmRange(curr.tawafStartTime ?: curr.startTime, curr.tawafEndTime ?: curr.startTime),
                        subtitle = if (isEnglish) "7 / 7 Laps Done" else "চক্কর সম্পন্ন ৭ / ৭"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Prep card
                    val tawafPrep = checklist.filter { it.stepKey == "TAWAF_PREP" }
                    if (tawafPrep.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132A32))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isEnglish) "Preparation" else "প্রস্তুতি",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                tawafPrep.forEach { item ->
                                    val text = UmrahContentData.getChecklistText(item.stepKey, item.itemIndex, isEnglish)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                        Text(text = text, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Tawaf 1 to 7 rounds logs
                items((1..7).toList()) { roundNum ->
                    val tLogs = roundLogs.filter { it.ritualType == "TAWAF" && it.roundNumber == roundNum }
                    val logTime = if (tLogs.isNotEmpty()) formatAmPmTime(tLogs.first().completedAt) else formatAmPmTime(curr.tawafEndTime ?: curr.startTime)
                    val label = if (isEnglish) "Lap $roundNum" else "${DateUtil.toBengaliNumerals(roundNum)} তম চক্কর"

                    RoundLogCard(
                        roundNumber = roundNum,
                        timeRange = "$logTime - $logTime",
                        labelText = label,
                        isEnglish = isEnglish
                    )
                }

                // 3. SA'I LOG
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RitualLogHeader(
                        icon = Icons.Default.DirectionsWalk,
                        title = if (isEnglish) "Sa'i Log" else "সাঈ লগ",
                        timeText = formatAmPmRange(curr.saiStartTime ?: curr.startTime, curr.saiEndTime ?: curr.startTime),
                        subtitle = if (isEnglish) "7 / 7 Laps Done" else "চক্কর সম্পন্ন ৭ / ৭"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val saiPrep = checklist.filter { it.stepKey == "SAI_PREP" }
                    if (saiPrep.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF132A32))
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isEnglish) "Preparation" else "প্রস্তুতি",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                saiPrep.forEach { item ->
                                    val text = UmrahContentData.getChecklistText(item.stepKey, item.itemIndex, isEnglish)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                        Text(text = text, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Sa'i 1 to 7 rounds logs
                items((1..7).toList()) { roundNum ->
                    val sLogs = roundLogs.filter { it.ritualType == "SAI" && it.roundNumber == roundNum }
                    val logTime = if (sLogs.isNotEmpty()) formatAmPmTime(sLogs.first().completedAt) else formatAmPmTime(curr.saiEndTime ?: curr.startTime)
                    val label = if (isEnglish) "Lap $roundNum" else "${DateUtil.toBengaliNumerals(roundNum)} তম চক্কর"

                    RoundLogCard(
                        roundNumber = roundNum,
                        timeRange = "$logTime - $logTime",
                        labelText = label,
                        isEnglish = isEnglish
                    )
                }

                // 4. HALQ / QASR LOG
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RitualLogHeader(
                        icon = Icons.Default.ContentCut,
                        title = if (isEnglish) "Halq / Qasr Log" else "হলক / কসর",
                        timeText = formatAmPmRange(curr.halqStartTime ?: curr.startTime, curr.halqEndTime ?: curr.startTime),
                        badgeText = "COMPLETED"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF132A32))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isEnglish) "Halq / Qasr completed successfully" else "হলক / কসর সফলভাবে সম্পন্ন হয়েছে",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TimelineStepRow(
    icon: ImageVector,
    title: String,
    timeText: String,
    durationText: String,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF165243).copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF34D399),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeText,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF0F181F)
        ) {
            Text(
                text = durationText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF34D399),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun RitualLogHeader(
    icon: ImageVector,
    title: String,
    timeText: String,
    badgeText: String? = null,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF0F766E).copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF2DD4BF), modifier = Modifier.size(18.dp))
            }
            Column {
                Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(text = timeText, fontSize = 12.sp, color = Color(0xFF94A3B8))
            }
        }

        if (badgeText != null) {
            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF1E293B)) {
                Text(text = badgeText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        } else if (subtitle != null) {
            Text(text = subtitle, fontSize = 12.sp, color = Color(0xFF94A3B8))
        }
    }
}

@Composable
fun RoundLogCard(
    roundNumber: Int,
    timeRange: String,
    labelText: String,
    isEnglish: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF132A32))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color(0xFF0F181F), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isEnglish) "$roundNumber" else DateUtil.toBengaliNumerals(roundNumber),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                    Text(
                        text = if (isEnglish) "Lap $roundNumber" else "${DateUtil.toBengaliNumerals(roundNumber)} তম চক্কর",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF064E3B)
                ) {
                    Text(
                        text = if (isEnglish) "Done" else "সম্পন্ন",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF34D399),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(text = timeRange, fontSize = 11.sp, color = Color(0xFF94A3B8), modifier = Modifier.padding(start = 32.dp))

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                Text(text = labelText, fontSize = 13.sp, color = Color(0xFFCBD5E1))
            }
        }
    }
}
