package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
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
import com.example.utils.HijriCalendarUtil
import com.example.utils.HijriDateInfo
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IslamicCalendarView(
    hijriOffset: Int = 0,
    onHijriOffsetChange: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showGuidanceDialogPair by remember { mutableStateOf<Pair<String, String>?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current

    val today = LocalDate.now()

    // Days grid calculation
    val daysInMonth = currentYearMonth.lengthOfMonth()
    val firstDayOfWeek = currentYearMonth.atDay(1).dayOfWeek.value % 7 // 0 = Sunday, ..., 6 = Saturday

    // Calculate Hijri info for selected date
    val selectedHijriInfo = remember(selectedDate, hijriOffset) {
        HijriCalendarUtil.getHijriDate(selectedDate, hijriOffset)
    }

    // Collect special events for current running Hijri month (e.g. Safar, Rabi' al-Awwal)
    val monthEvents = remember(selectedHijriInfo.hijriMonth, selectedHijriInfo.hijriYear, hijriOffset) {
        val eventsList = mutableListOf<Pair<LocalDate, HijriDateInfo>>()
        val targetHijriMonth = selectedHijriInfo.hijriMonth
        val targetHijriYear = selectedHijriInfo.hijriYear

        val startDate = selectedDate.minusDays(32)
        val endDate = selectedDate.plusDays(32)
        var curr = startDate
        while (!curr.isAfter(endDate)) {
            val info = HijriCalendarUtil.getHijriDate(curr, hijriOffset)
            if (info.hijriMonth == targetHijriMonth && info.hijriYear == targetHijriYear) {
                if (info.specialEvents.isNotEmpty()) {
                    eventsList.add(curr to info)
                }
            }
            curr = curr.plusDays(1)
        }
        eventsList.distinctBy { it.first }
    }

    // Determine primary Hijri Month & Year span for header (e.g. "সফর-রবিউল আউয়াল ১৪৪৮")
    val currentMonthHijriHeaderStr = remember(currentYearMonth, hijriOffset) {
        val startHijri = HijriCalendarUtil.getHijriDateForGrid(currentYearMonth.atDay(1), hijriOffset)
        val endHijri = HijriCalendarUtil.getHijriDateForGrid(currentYearMonth.atDay(daysInMonth), hijriOffset)

        if (startHijri.hijriMonthNameBn == endHijri.hijriMonthNameBn) {
            "${startHijri.hijriMonthNameBn} ${HijriCalendarUtil.toBengaliNumerals(startHijri.hijriYear)}"
        } else if (startHijri.hijriYear == endHijri.hijriYear) {
            "${startHijri.hijriMonthNameBn}-${endHijri.hijriMonthNameBn} ${HijriCalendarUtil.toBengaliNumerals(startHijri.hijriYear)}"
        } else {
            "${startHijri.hijriMonthNameBn} ${HijriCalendarUtil.toBengaliNumerals(startHijri.hijriYear)}-${endHijri.hijriMonthNameBn} ${HijriCalendarUtil.toBengaliNumerals(endHijri.hijriYear)}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. TOP HERO CARD (MINIMAL BANNER FROM MOCKUP)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF10B981), // Vibrant Emerald
                                        Color(0xFF059669)  // Deep Emerald
                                    )
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Main English (Gregorian) Date with Weekday
                            Text(
                                text = "${HijriCalendarUtil.getBengaliWeekdayName(selectedDate)}, ${HijriCalendarUtil.toBengaliNumerals(selectedDate.dayOfMonth)} ${HijriCalendarUtil.getBengaliMonthName(selectedDate.monthValue)} ${HijriCalendarUtil.toBengaliNumerals(selectedDate.year)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Thin Divider Line
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.25f),
                                thickness = 1.dp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Sub Date: Only Hijri Date (with (i) icon) is clickable for adjustment
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { showOffsetDialog = true }
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${HijriCalendarUtil.toBengaliNumerals(selectedHijriInfo.hijriDay)} ${selectedHijriInfo.hijriMonthNameBn} ${HijriCalendarUtil.toBengaliNumerals(selectedHijriInfo.hijriYear)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.95f)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Hijri Adjustment",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                Text(
                                    text = "  •  ${HijriCalendarUtil.getBanglaDateStr(selectedDate, includeSuffix = false)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                            }

                            if (selectedHijriInfo.isSunnahFast || selectedHijriInfo.specialEvents.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val title = selectedHijriInfo.specialEvents.firstOrNull() ?: selectedHijriInfo.sunnahFastReason ?: "সুন্নাত আমল"
                                        showGuidanceDialogPair = com.example.utils.IslamicEventGuidanceHelper.getGuidanceForDateAndTitle(selectedDate, title, selectedHijriInfo)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "📖 আমল ও শরীঈ বিধান দেখুন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. SECTION HEADER 1: ক্যালেন্ডার
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ক্যালেন্ডার",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // 3. CALENDAR GRID CARD
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Header with Month Switcher (e.g. "< সফর ১৪৪৮ >")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.minusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowLeft,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${HijriCalendarUtil.getBengaliMonthName(currentYearMonth.monthValue)} ${HijriCalendarUtil.toBengaliNumerals(currentYearMonth.year)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = currentMonthHijriHeaderStr,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { currentYearMonth = currentYearMonth.plusMonths(1) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Weekdays Row
                        val weekdays = listOf("রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র", "শনি")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            weekdays.forEachIndexed { idx, day ->
                                Text(
                                    text = day,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (idx == 5) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Calendar Days Grid
                        val totalCells = firstDayOfWeek + daysInMonth
                        val rows = (totalCells + 6) / 7

                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (c in 0..6) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - firstDayOfWeek + 1

                                    if (dayNum in 1..daysInMonth) {
                                        val date = currentYearMonth.atDay(dayNum)
                                        val hijriInfo = HijriCalendarUtil.getHijriDateForGrid(date, hijriOffset)

                                        val isSelected = date == selectedDate
                                        val isToday = date == today

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(0.9f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    when {
                                                        isSelected -> Color(0xFF10B981)
                                                        isToday -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                        else -> Color.Transparent
                                                    }
                                                )
                                                .border(
                                                    width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                    color = if (isToday && !isSelected) Color(0xFF10B981) else Color.Transparent,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { selectedDate = date },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                // Top Number: Gregorian Day (Main / Bold)
                                                Text(
                                                    text = HijriCalendarUtil.toBengaliNumerals(dayNum),
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        c == 5 -> Color(0xFF10B981) // Friday Green
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                // Bottom Number: Hijri Day (Sub / Smaller)
                                                Text(
                                                    text = HijriCalendarUtil.toBengaliNumerals(hijriInfo.hijriDay),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White.copy(alpha = 0.85f)
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Info text below calendar and above Islamic events
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ইসলামী নিয়ম অনুযায়ী সূর্যাস্তের (~সন্ধ্যা ৬টা) পরেই পরবর্তী দিনের জন্য হিজরি তারিখ গণনা শুরু হয়।",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            // 4. SECTION HEADER 2: ইসলামিক ইভেন্ট
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Event,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ইসলামিক ইভেন্ট",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // 5. ISLAMIC EVENTS LIST CARDS
            if (monthEvents.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${selectedHijriInfo.hijriMonthNameBn} মাসে কোনো বিশেষ ইসলামিক ইভেন্ট নেই।",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(monthEvents) { (date, info) ->
                    val isPast = date.isBefore(today)
                    val isCurrentDay = date.isEqual(today)
                    val title = if (info.specialEvents.isNotEmpty()) {
                        info.specialEvents.joinToString(", ")
                    } else {
                        info.sunnahFastReason ?: "সুন্নাত আমল"
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedDate = date
                                showGuidanceDialogPair = com.example.utils.IslamicEventGuidanceHelper.getGuidanceForDateAndTitle(date, title, info)
                            }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Text(
                                        text = "${HijriCalendarUtil.toBengaliNumerals(date.dayOfMonth)} ${HijriCalendarUtil.getBengaliMonthName(date.monthValue)} (${HijriCalendarUtil.toBengaliNumerals(info.hijriDay)} ${info.hijriMonthNameBn})",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Event Status Badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = when {
                                    isCurrentDay -> Color(0xFF10B981).copy(alpha = 0.15f)
                                    isPast -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    else -> Color(0xFFFEF3C7)
                                }
                            ) {
                                Text(
                                    text = when {
                                        isCurrentDay -> "আজ"
                                        isPast -> "পার হয়ে গেছে"
                                        else -> "আসন্ন"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        isCurrentDay -> Color(0xFF10B981)
                                        isPast -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> Color(0xFFD97706)
                                    },
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // SHARIA GUIDANCE MODAL BOTTOM SHEET
    val currentGuidance = showGuidanceDialogPair
    if (currentGuidance != null) {
        var isFullScreenGuidance by remember { mutableStateOf(false) }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showGuidanceDialogPair = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = if (isFullScreenGuidance) Modifier.fillMaxHeight() else Modifier,
            shape = if (isFullScreenGuidance) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (isFullScreenGuidance) Modifier.fillMaxHeight() else Modifier)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currentGuidance.first,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isFullScreenGuidance = !isFullScreenGuidance }) {
                        Icon(
                            imageVector = if (isFullScreenGuidance) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreenGuidance) "ছোট করুন" else "ফুল স্ক্রিন",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showGuidanceDialogPair = null }) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isFullScreenGuidance) Modifier.weight(1f)
                            else Modifier.heightIn(max = 460.dp)
                        )
                        .verticalScroll(scrollState)
                ) {
                    FormattedIslamicText(
                        text = currentGuidance.second,
                        baseFontSize = 14.sp,
                        baseColor = MaterialTheme.colorScheme.onSurface,
                        arabicColor = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val fullText = "${currentGuidance.first}\n\n${currentGuidance.second}"
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Guidance", fullText)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "অনুলিপি করা হয়েছে!", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "অনুলিপি করুন",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("কপি", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            val fullText = "${currentGuidance.first}\n\n${currentGuidance.second}"
                            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_SUBJECT, currentGuidance.first)
                                putExtra(android.content.Intent.EXTRA_TEXT, fullText)
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "শেয়ার করুন"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার করুন",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("শেয়ার", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // HIJRI OFFSET ADJUSTMENT DIALOG
    if (showOffsetDialog) {
        Dialog(onDismissRequest = { showOffsetDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF26272B))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "হিজরি তারিখ সমন্বয়",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "হিজরি তারিখ একদিন বা কয়েকদিন আগে-পিছে করতে পারেন।",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = "ইসলামী নিয়ম অনুযায়ী সূর্যাস্তের (~সন্ধ্যা ৬টা) পরেই পরবর্তী দিনের জন্য হিজরি তারিখ গণনা শুরু হয়। প্রয়োজনে নিচে + / - চেপে সমন্বয় করতে পারেন।",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "বর্তমান তারিখ: ${HijriCalendarUtil.toBengaliNumerals(selectedHijriInfo.hijriDay)} ${selectedHijriInfo.hijriMonthNameBn} ${HijriCalendarUtil.toBengaliNumerals(selectedHijriInfo.hijriYear)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2DD4BF)
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { if (hijriOffset > -5) onHijriOffsetChange(hijriOffset - 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease offset",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        val offsetDisplay = if (hijriOffset > 0) "+${HijriCalendarUtil.toBengaliNumerals(hijriOffset)}"
                                           else if (hijriOffset < 0) "-${HijriCalendarUtil.toBengaliNumerals(-hijriOffset)}"
                                           else HijriCalendarUtil.toBengaliNumerals(0)

                        Text(
                            text = "$offsetDisplay দিন",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = { if (hijriOffset < 5) onHijriOffsetChange(hijriOffset + 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase offset",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "বন্ধ করুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2DD4BF),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showOffsetDialog = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
