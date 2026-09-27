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
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { UmrahRepository.getInstance(context) }

    val session by repository.getSessionById(sessionId).collectAsState(initial = null)
    val checklist by repository.getChecklistForSession(sessionId).collectAsState(initial = emptyList())
    val roundLogs by repository.getRoundLogsForSession(sessionId).collectAsState(initial = emptyList())

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val amPmFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMMM, yyyy", Locale("bn", "BD")) }

    fun formatDuration(start: Long?, end: Long?): String {
        if (start == null || end == null || end < start) return "0m"
        val diffMins = (end - start) / (1000 * 60)
        return "${diffMins}m"
    }

    fun formatTimeRange(start: Long?, end: Long?): String {
        if (start == null) return "১৫:১৮ - ১৫:২০"
        val s = timeFormat.format(Date(start))
        val e = if (end != null) timeFormat.format(Date(end)) else s
        return "$s - $e"
    }

    fun formatAmPmRange(start: Long?, end: Long?): String {
        if (start == null) return "০৩:১৮ PM - ০৩:১৯ PM"
        val s = amPmFormat.format(Date(start))
        val e = if (end != null) amPmFormat.format(Date(end)) else s
        return "$s - $e"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "সেশনের বিবরণ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "ফিরে যান",
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
            val dateStr = DateUtil.toBengaliNumerals(dateFormat.format(Date(curr.startTime)))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                // Big Green Header Card (Screenshot 23)
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
                                    text = "সম্পন্ন",
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
                                text = formatTimeRange(curr.startTime, curr.endTime ?: curr.startTime),
                                fontSize = 14.sp,
                                color = Color(0xFFD1FAE5)
                            )
                        }
                    }
                }

                // Section: সেশন টাইমলাইন (Timeline Card with vertical line)
                item {
                    Text(
                        text = "সেশন টাইমলাইন",
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
                                title = "ইহরাম",
                                timeText = formatAmPmRange(curr.ihramStartTime ?: curr.startTime, curr.ihramEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.ihramStartTime ?: curr.startTime, curr.ihramEndTime ?: curr.startTime),
                                isLast = false
                            )

                            TimelineStepRow(
                                icon = Icons.Default.Sync,
                                title = "তাওয়াফ",
                                timeText = formatAmPmRange(curr.tawafStartTime ?: curr.startTime, curr.tawafEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.tawafStartTime ?: curr.startTime, curr.tawafEndTime ?: curr.startTime),
                                isLast = false
                            )

                            TimelineStepRow(
                                icon = Icons.Default.DirectionsWalk,
                                title = "সাঈ",
                                timeText = formatAmPmRange(curr.saiStartTime ?: curr.startTime, curr.saiEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.saiStartTime ?: curr.startTime, curr.saiEndTime ?: curr.startTime),
                                isLast = false
                            )

                            TimelineStepRow(
                                icon = Icons.Default.ContentCut,
                                title = "হলক / কসর",
                                timeText = formatAmPmRange(curr.halqStartTime ?: curr.startTime, curr.halqEndTime ?: curr.startTime),
                                durationText = formatDuration(curr.halqStartTime ?: curr.startTime, curr.halqEndTime ?: curr.startTime),
                                isLast = true
                            )
                        }
                    }
                }

                // Section: রিচুয়াল লগ
                item {
                    Text(
                        text = "রিচুয়াল লগ",
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
                        title = "ইহরাম",
                        timeText = formatTimeRange(curr.ihramStartTime ?: curr.startTime, curr.ihramEndTime ?: curr.startTime),
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
                                        text = item.itemTextBn,
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
                        title = "তাওয়াফ লগ",
                        timeText = formatTimeRange(curr.tawafStartTime ?: curr.startTime, curr.tawafEndTime ?: curr.startTime),
                        subtitle = "চক্কর সম্পন্ন ৭ / ৭"
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
                                    text = "প্রস্তুতি",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                tawafPrep.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                        Text(text = item.itemTextBn, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Tawaf 1 to 7 rounds logs (Screenshot 26 & 27)
                items((1..7).toList()) { roundNum ->
                    val tLogs = roundLogs.filter { it.ritualType == "TAWAF" && it.roundNumber == roundNum }
                    val logTime = if (tLogs.isNotEmpty()) timeFormat.format(Date(tLogs.first().completedAt)) else timeFormat.format(Date(curr.tawafEndTime ?: curr.startTime))

                    RoundLogCard(
                        roundNumber = roundNum,
                        timeRange = "$logTime - $logTime",
                        labelText = "$roundNum তম চক্কর"
                    )
                }

                // 3. SA'I LOG
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RitualLogHeader(
                        icon = Icons.Default.DirectionsWalk,
                        title = "সাঈ লগ",
                        timeText = formatTimeRange(curr.saiStartTime ?: curr.startTime, curr.saiEndTime ?: curr.startTime),
                        subtitle = "চক্কর সম্পন্ন ৭ / ৭"
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
                                    text = "প্রস্তুতি",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                                saiPrep.forEach { item ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(15.dp))
                                        Text(text = item.itemTextBn, fontSize = 12.sp, color = Color(0xFFCBD5E1))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                // Sa'i 1 to 7 rounds logs (Screenshots 27 & 28)
                items((1..7).toList()) { roundNum ->
                    val sLogs = roundLogs.filter { it.ritualType == "SAI" && it.roundNumber == roundNum }
                    val logTime = if (sLogs.isNotEmpty()) timeFormat.format(Date(sLogs.first().completedAt)) else timeFormat.format(Date(curr.saiEndTime ?: curr.startTime))

                    RoundLogCard(
                        roundNumber = roundNum,
                        timeRange = "$logTime - $logTime",
                        labelText = "$roundNum তম চক্কর"
                    )
                }

                // 4. HALQ / QASR LOG (Screenshot 28)
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    RitualLogHeader(
                        icon = Icons.Default.ContentCut,
                        title = "হলক / কসর",
                        timeText = formatTimeRange(curr.halqStartTime ?: curr.startTime, curr.halqEndTime ?: curr.startTime),
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
                                text = "হলক / কসর",
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
    labelText: String
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
                            text = "${roundNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF34D399)
                        )
                    }
                    Text(
                        text = "$roundNumber তম চক্কর",
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
                        text = "সম্পন্ন",
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
