package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.DistrictInfo
import com.example.utils.DateUtil
import com.example.utils.HijriCalendarUtil
import com.example.utils.PrayerTimesCalculator
import com.example.utils.PrayerTimesShareUtil
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val DarkBackground = Color(0xFF10171B)
private val DarkCardSurface = Color(0xFF182228)
private val EmeraldAccent = Color(0xFF00C288)
private val EmeraldDarkPill = Color(0xFF0C2D23)
private val ForbiddenCardBg = Color(0xFF38191C)
private val ForbiddenCardBorder = Color(0xFF562529)
private val ForbiddenTextRed = Color(0xFFF87171)
private val MutedText = Color(0xFF94A3B8)
private val AmberBullet = Color(0xFFFB923C)
private val GreenBullet = Color(0xFF34D399)
private val AmberWarning = Color(0xFFF59E0B)
private val DarkModalBg = Color(0xFF182228)

enum class ReferenceType(val title: String) {
    FARD_PRAYERS("সালাতের ওয়াক্ত সম্পর্কিত হাদিস ও রেফারেন্স"),
    NAFL_PRAYERS("নফল সালাত সম্পর্কিত হাদিস ও রেফারেন্স"),
    FORBIDDEN_TIMES("সালাতের নিষিদ্ধ সময় সম্পর্কিত হাদিস ও রেফারেন্স")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesDetailSheet(
    schedule: DailyPrayerSchedule,
    isHanafi: Boolean,
    hijriOffset: Int = 0,
    onDistrictSelected: (DistrictInfo) -> Unit,
    onHanafiChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showDistrictPicker by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSharingImage by remember { mutableStateOf(false) }
    var activeReferenceType by remember { mutableStateOf<ReferenceType?>(null) }

    // Prayer Notification Preferences State
    var isMasterNotifEnabled by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isMasterEnabled(context)) }
    var isNotifFajr by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.FAJR)) }
    var isNotifDhuhr by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.DHUHR)) }
    var isNotifAsr by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.ASR)) }
    var isNotifMaghrib by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.MAGHRIB)) }
    var isNotifIsha by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.ISHA)) }
    var isNotifSahri by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.SAHRI)) }
    var isNotifIftar by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.IFTAR)) }
    var isNotifSound by remember { mutableStateOf(com.example.utils.PrayerNotificationHelper.isSoundEnabled(context)) }

    val settingsRepo = remember(context) { com.example.data.repository.SettingsRepository.getInstance(context) }
    val sahriOffset by settingsRepo.sahriOffsetFlow.collectAsState(initial = -3)
    val iftarOffset by settingsRepo.iftarOffsetFlow.collectAsState(initial = 0)

    var showSawmAdjustDialog by remember { mutableStateOf(false) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isMasterNotifEnabled = true
            com.example.utils.PrayerNotificationHelper.setMasterEnabled(context, true)
            Toast.makeText(context, "ওয়াক্ত শুরুর নোটিফিকেশন সক্রিয় করা হয়েছে", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "নোটিফিকেশন অনুমতি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
        }
    }

    // Date selection state
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val isToday = remember(selectedDate) { selectedDate == LocalDate.now() }

    // Calculate active schedule dynamically for selected date
    val activeSchedule = remember(selectedDate, schedule.district, isHanafi, sahriOffset, iftarOffset) {
        PrayerTimesCalculator.calculatePrayerSchedule(
            date = selectedDate,
            district = schedule.district,
            isHanafi = isHanafi,
            sahriOffsetMinutes = sahriOffset,
            iftarOffsetMinutes = iftarOffset
        )
    }

    // Hijri date info for selected date using global/local offset
    val hijriInfo = remember(selectedDate, hijriOffset) {
        HijriCalendarUtil.getHijriDate(selectedDate, hijriOffset)
    }

    var selectedLocationTab by remember { mutableIntStateOf(if (schedule.district.countryBn == "বাংলাদেশ") 0 else 1) }

    val filteredLocations = remember(searchQuery, selectedLocationTab) {
        if (searchQuery.isBlank()) {
            if (selectedLocationTab == 0) {
                PrayerTimesCalculator.BANGLADESH_DISTRICTS
            } else {
                PrayerTimesCalculator.INTERNATIONAL_CITIES
            }
        } else {
            PrayerTimesCalculator.ALL_LOCATIONS.filter {
                it.nameBn.contains(searchQuery, ignoreCase = true) ||
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                it.countryBn.contains(searchQuery, ignoreCase = true) ||
                it.countryEn.contains(searchQuery, ignoreCase = true) ||
                it.divisionBn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = DarkBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(38.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF334155))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Top Navigation Bar (Back, Title, Share, District)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "বন্ধ করুন",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ক্যালেন্ডার ও সময়সূচি",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // District selector chip
                    Surface(
                        onClick = { showDistrictPicker = true },
                        shape = RoundedCornerShape(100.dp),
                        color = EmeraldAccent.copy(alpha = 0.14f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            val locationLabel = if (schedule.district.countryBn == "বাংলাদেশ") {
                                schedule.district.nameBn
                            } else {
                                "${schedule.district.nameBn}, ${schedule.district.countryBn}"
                            }
                            Text(
                                text = locationLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                                maxLines = 1
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Share button
                    IconButton(
                        onClick = {
                            if (!isSharingImage) {
                                isSharingImage = true
                                coroutineScope.launch {
                                    PrayerTimesShareUtil.shareAsImage(
                                        context = context,
                                        schedule = activeSchedule,
                                        date = selectedDate,
                                        hijriOffset = hijriOffset
                                    )
                                    isSharingImage = false
                                }
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        if (isSharingImage) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "শেয়ার করুন",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1.5 Top Visual Sun Path Hero Card
            if (isToday) {
                com.example.ui.components.PrayerSunPathCard(
                    schedule = activeSchedule,
                    onDetailsClick = { /* Already in detail view */ },
                    notificationStates = mapOf(
                        com.example.data.model.PrayerName.FAJR to isNotifFajr,
                        com.example.data.model.PrayerName.DHUHR to isNotifDhuhr,
                        com.example.data.model.PrayerName.ASR to isNotifAsr,
                        com.example.data.model.PrayerName.MAGHRIB to isNotifMaghrib,
                        com.example.data.model.PrayerName.ISHA to isNotifIsha,
                        com.example.data.model.PrayerName.SAHRI to isNotifSahri,
                        com.example.data.model.PrayerName.IFTAR to isNotifIftar
                    ),
                    onToggleNotification = { prayerName, isEnabled ->
                        com.example.utils.PrayerNotificationHelper.setPrayerEnabled(context, prayerName, isEnabled)
                        when (prayerName) {
                            com.example.data.model.PrayerName.FAJR -> isNotifFajr = isEnabled
                            com.example.data.model.PrayerName.DHUHR -> isNotifDhuhr = isEnabled
                            com.example.data.model.PrayerName.ASR -> isNotifAsr = isEnabled
                            com.example.data.model.PrayerName.MAGHRIB -> isNotifMaghrib = isEnabled
                            com.example.data.model.PrayerName.ISHA -> isNotifIsha = isEnabled
                            com.example.data.model.PrayerName.SAHRI -> isNotifSahri = isEnabled
                            com.example.data.model.PrayerName.IFTAR -> isNotifIftar = isEnabled
                            else -> { /* No-op */ }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. Date Header & Weekly 7-Day Selector Bar
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Date Row: e.g. ১৫ আগস্ট, ২০২৬ • ৩১ শ্রাবণ, ১৪৩৩
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = DateUtil.getFullHeaderDateStr(selectedDate),
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = { showCalendarDialog = true },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "ক্যালেন্ডার খুলুন",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Horizontal 7-Day Strip
                    val daysRange = remember(selectedDate) {
                        (-3..3).map { selectedDate.plusDays(it.toLong()) }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        daysRange.forEach { dayDate ->
                            val isSelected = (dayDate == selectedDate)
                            val shortName = DateUtil.getShortDayNameBn(dayDate)
                            val dayNumberStr = dayDate.dayOfMonth.toString()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedDate = dayDate }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    text = shortName,
                                    fontSize = 11.5.sp,
                                    color = if (isSelected) EmeraldAccent else MutedText,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) EmeraldAccent else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumberStr,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hijri Date Capsule Pill
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = EmeraldDarkPill,
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, EmeraldAccent.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🌙", fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${DateUtil.toBengaliNumerals(hijriInfo.hijriDay)} ${hijriInfo.hijriMonthNameBn}, ${DateUtil.toBengaliNumerals(hijriInfo.hijriYear)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldAccent
                                )
                            }
                        }
                    }

                    // Reset to today button if viewing other date
                    if (!isToday) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { selectedDate = LocalDate.now() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Today,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আজকের তারিখে ফিরুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Section 1: সালাতের সময় (5 Fard Prayers)
            DarkSectionCard(
                title = "সালাতের সময়",
                onReferenceClick = { activeReferenceType = ReferenceType.FARD_PRAYERS }
            ) {
                // Fajr
                PrayerDetailRow(
                    icon = Icons.Outlined.WbTwilight,
                    name = "ফজর",
                    timeRange = activeSchedule.fajrRange
                )

                PrayerDivider()

                // Dhuhr
                PrayerDetailRow(
                    icon = Icons.Outlined.WbSunny,
                    name = "যুহর",
                    timeRange = activeSchedule.dhuhrRange
                )

                PrayerDivider()

                // Asr
                PrayerDetailRow(
                    icon = Icons.Outlined.Brightness5,
                    name = "আসর",
                    timeRange = activeSchedule.asrRange,
                    subItems = listOf(
                        BulletSubItem("মাকরূহ: ${activeSchedule.asrMakruhTime}", AmberBullet)
                    )
                )

                PrayerDivider()

                // Maghrib
                PrayerDetailRow(
                    icon = Icons.Outlined.WbCloudy,
                    name = "মাগরিব",
                    timeRange = activeSchedule.maghribRange
                )

                PrayerDivider()

                // Isha
                PrayerDetailRow(
                    icon = Icons.Outlined.Nightlight,
                    name = "ইশা",
                    timeRange = activeSchedule.ishaRange,
                    subItems = listOf(
                        BulletSubItem("উত্তম সময় শেষ: ${activeSchedule.ishaUttomTime}", GreenBullet),
                        BulletSubItem("মাকরূহ: ${activeSchedule.ishaMakruhTime}", AmberBullet)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Section 2: নফল সালাতের সময়
            DarkSectionCard(
                title = "নফল সালাতের সময়",
                onReferenceClick = { activeReferenceType = ReferenceType.NAFL_PRAYERS }
            ) {
                // Duha
                PrayerDetailRow(
                    icon = Icons.Outlined.WbSunny,
                    name = "দুহা",
                    timeRange = activeSchedule.duhaRange
                )

                PrayerDivider()

                // Zawal
                PrayerDetailRow(
                    icon = Icons.Outlined.AccountBalance,
                    name = "জাওয়াল শুরু",
                    timeRange = activeSchedule.zawalStartTime
                )

                PrayerDivider()

                // Awwabin
                PrayerDetailRow(
                    icon = Icons.Outlined.WbCloudy,
                    name = "আওয়াবিন",
                    timeRange = activeSchedule.awwabinRange
                )

                PrayerDivider()

                // Tahajjud
                PrayerDetailRow(
                    icon = Icons.Outlined.Bedtime,
                    name = "তাহাজ্জুদ",
                    timeRange = activeSchedule.tahajjudRange,
                    subItems = listOf(
                        BulletSubItem("রাতের শেষ ১/৩ শুরু: ${activeSchedule.tahajjudLastThirdStart}", GreenBullet)
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Section 3: সালাতের নিষিদ্ধ সময় (Forbidden Times)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "সালাতের নিষিদ্ধ সময়",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForbiddenTextRed
                        )

                        Text(
                            text = "রেফারেন্স দেখুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { activeReferenceType = ReferenceType.FORBIDDEN_TIMES }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "এই সময়গুলোতে সালাত আদায় নিষিদ্ধ (ব্যতিক্রম: সূর্যাস্তকালীন একই দিনের আসর সালাত)",
                        fontSize = 11.sp,
                        color = MutedText,
                        lineHeight = 15.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3 Boxes in a row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ForbiddenTimeBox(
                            title = "সকাল",
                            timeRange = activeSchedule.forbiddenMorningRange,
                            modifier = Modifier.weight(1f)
                        )
                        ForbiddenTimeBox(
                            title = "দুপুর",
                            timeRange = activeSchedule.forbiddenNoonRange,
                            modifier = Modifier.weight(1f)
                        )
                        ForbiddenTimeBox(
                            title = "সন্ধ্যা",
                            timeRange = activeSchedule.forbiddenEveningRange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Section 4: সাওমের সময়সূচী (Fasting / Sawm)
            DarkSectionCard(
                title = "সাওমের সময়সূচী",
                referenceLabel = "সেটিংস ও সতর্কতা",
                onReferenceClick = { showSawmAdjustDialog = true }
            ) {
                PrayerDetailRow(
                    icon = Icons.Outlined.Restaurant,
                    name = "সাহরি",
                    timeRange = activeSchedule.sahriTimeDigits,
                    subItems = listOf(
                        BulletSubItem(
                            text = if (sahriOffset != 0) "সতর্কতামূলক অফসেট: ${DateUtil.toBengaliNumerals(sahriOffset)} মি." else "স্ট্যান্ডার্ড ফজর ওয়াক্ত",
                            color = EmeraldAccent
                        )
                    )
                )

                PrayerDivider()

                PrayerDetailRow(
                    icon = Icons.Outlined.SoupKitchen,
                    name = "ইফতার",
                    timeRange = activeSchedule.iftarTimeDigits,
                    subItems = listOf(
                        BulletSubItem(
                            text = if (iftarOffset != 0) "সতর্কতামূলক অফসেট: ${DateUtil.toBengaliNumerals(iftarOffset)} মি." else "মাগরিব ওয়াক্ত শুরু",
                            color = AmberWarning
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Section 5: সূর্যোদয় ও সূর্যাস্ত
            DarkSectionCard(
                title = "সূর্যোদয় ও সূর্যাস্ত"
            ) {
                PrayerDetailRow(
                    icon = Icons.Outlined.WbTwilight,
                    name = "সূর্যোদয়",
                    timeRange = activeSchedule.sunriseTimeDigits
                )

                PrayerDivider()

                PrayerDetailRow(
                    icon = Icons.Outlined.WbSunny,
                    name = "সূর্যাস্ত",
                    timeRange = activeSchedule.sunsetTimeDigits
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Section: ওয়াক্ত শুরুর নোটিফিকেশন (Prayer Start Notification Settings)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isMasterNotifEnabled) EmeraldAccent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMasterNotifEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    tint = if (isMasterNotifEnabled) EmeraldAccent else MutedText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "ওয়াক্ত শুরুর নোটিফিকেশন",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isMasterNotifEnabled) "ওয়াক্ত শুরু হলে স্বয়ংক্রিয় অ্যালার্ট আসবে" else "নোটিফিকেশন বন্ধ রয়েছে",
                                    fontSize = 11.sp,
                                    color = MutedText
                                )
                            }
                        }

                        Switch(
                            checked = isMasterNotifEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            return@Switch
                                        }
                                    }
                                    isMasterNotifEnabled = true
                                    com.example.utils.PrayerNotificationHelper.setMasterEnabled(context, true)
                                    Toast.makeText(context, "ওয়াক্তের নোটিফিকেশন চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    isMasterNotifEnabled = false
                                    com.example.utils.PrayerNotificationHelper.setMasterEnabled(context, false)
                                    Toast.makeText(context, "ওয়াক্তের নোটিফিকেশন বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = EmeraldAccent,
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color(0xFF334155)
                            )
                        )
                    }

                    if (isMasterNotifEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(thickness = 0.6.dp, color = Color(0xFF26333D))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "কোন কোন ওয়াক্তের নোটিফিকেশন চান নির্বাচন করুন:",
                            fontSize = 11.5.sp,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Chips for individual prayers
                        val prayersToToggle = listOf(
                            Triple("সাহরি", isNotifSahri, com.example.data.model.PrayerName.SAHRI),
                            Triple("ফজর", isNotifFajr, com.example.data.model.PrayerName.FAJR),
                            Triple("যুহর", isNotifDhuhr, com.example.data.model.PrayerName.DHUHR),
                            Triple("আসর", isNotifAsr, com.example.data.model.PrayerName.ASR),
                            Triple("মাগরিব", isNotifMaghrib, com.example.data.model.PrayerName.MAGHRIB),
                            Triple("ইফতার", isNotifIftar, com.example.data.model.PrayerName.IFTAR),
                            Triple("এশা", isNotifIsha, com.example.data.model.PrayerName.ISHA)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            prayersToToggle.forEach { (name, isEnabled, pEnum) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isEnabled) EmeraldAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isEnabled) EmeraldAccent.copy(alpha = 0.6f) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .clickable {
                                            val nextVal = !isEnabled
                                            when (pEnum) {
                                                com.example.data.model.PrayerName.SAHRI -> isNotifSahri = nextVal
                                                com.example.data.model.PrayerName.FAJR -> isNotifFajr = nextVal
                                                com.example.data.model.PrayerName.DHUHR -> isNotifDhuhr = nextVal
                                                com.example.data.model.PrayerName.ASR -> isNotifAsr = nextVal
                                                com.example.data.model.PrayerName.MAGHRIB -> isNotifMaghrib = nextVal
                                                com.example.data.model.PrayerName.IFTAR -> isNotifIftar = nextVal
                                                com.example.data.model.PrayerName.ISHA -> isNotifIsha = nextVal
                                                else -> {}
                                            }
                                            com.example.utils.PrayerNotificationHelper.setPrayerEnabled(context, pEnum, nextVal)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 11.sp,
                                            fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isEnabled) EmeraldAccent else Color.White.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Sound switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextVal = !isNotifSound
                                    isNotifSound = nextVal
                                    com.example.utils.PrayerNotificationHelper.setSoundEnabled(context, nextVal)
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isNotifSound) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                                    contentDescription = null,
                                    tint = MutedText,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "সাউন্ড ও ভাইব্রেশন",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Switch(
                                checked = isNotifSound,
                                onCheckedChange = { checked ->
                                    isNotifSound = checked
                                    com.example.utils.PrayerNotificationHelper.setSoundEnabled(context, checked)
                                },
                                modifier = Modifier.height(24.dp),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = EmeraldAccent,
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color(0xFF334155)
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 8. Asr Calculation Method Switch (Hanafi / Standard)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "আসরের পদ্ধতি: ${if (isHanafi) "হানাফী (মিছলে সানি)" else "শাফেয়ী / জমহুর"}",
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    TextButton(
                        onClick = { onHanafiChanged(!isHanafi) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isHanafi) "পরিবর্তন" else "হানাফী করুন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 9. Copy Text Button
            OutlinedButton(
                onClick = {
                    PrayerTimesShareUtil.copyToClipboard(context, activeSchedule, selectedDate, hijriOffset)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = EmeraldAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("সময়সূচির টেক্সট কপি করুন", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Reference Hadith Dialog
    if (activeReferenceType != null) {
        PrayerReferenceDialog(
            type = activeReferenceType!!,
            onDismiss = { activeReferenceType = null }
        )
    }

    // Calendar Picker Dialog
    if (showCalendarDialog) {
        PrayerTimesCalendarDialog(
            selectedDate = selectedDate,
            hijriOffset = hijriOffset,
            onDateSelected = {
                selectedDate = it
                showCalendarDialog = false
            },
            onDismiss = { showCalendarDialog = false }
        )
    }

    // Location / District Selector Dialog
    if (showDistrictPicker) {
        DistrictSelectionModal(
            searchQuery = searchQuery,
            onSearchQueryChange = { searchQuery = it },
            selectedTab = selectedLocationTab,
            onTabSelected = { selectedLocationTab = it },
            filteredLocations = filteredLocations,
            selectedDistrict = schedule.district,
            onSelect = {
                onDistrictSelected(it)
                showDistrictPicker = false
                searchQuery = ""
            },
            onDismiss = {
                showDistrictPicker = false
                searchQuery = ""
            }
        )
    }

    // Sawm (Sahri & Iftar) Offset & Notification Settings Dialog
    if (showSawmAdjustDialog) {
        SawmSettingsDialog(
            currentSahriOffset = sahriOffset,
            currentIftarOffset = iftarOffset,
            isNotifSahri = isNotifSahri,
            isNotifIftar = isNotifIftar,
            onUpdateSahriOffset = { newOffset ->
                coroutineScope.launch {
                    settingsRepo.setSahriOffset(newOffset)
                }
            },
            onUpdateIftarOffset = { newOffset ->
                coroutineScope.launch {
                    settingsRepo.setIftarOffset(newOffset)
                }
            },
            onToggleSahriNotif = { enabled ->
                isNotifSahri = enabled
                com.example.utils.PrayerNotificationHelper.setPrayerEnabled(context, com.example.data.model.PrayerName.SAHRI, enabled)
            },
            onToggleIftarNotif = { enabled ->
                isNotifIftar = enabled
                com.example.utils.PrayerNotificationHelper.setPrayerEnabled(context, com.example.data.model.PrayerName.IFTAR, enabled)
            },
            onDismiss = { showSawmAdjustDialog = false }
        )
    }
}

data class BulletSubItem(val text: String, val color: Color)

@Composable
private fun DarkSectionCard(
    title: String,
    referenceLabel: String = "রেফারেন্স দেখুন",
    onReferenceClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (onReferenceClick != null) {
                    Text(
                        text = referenceLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onReferenceClick() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun PrayerDetailRow(
    icon: ImageVector,
    name: String,
    timeRange: String,
    subItems: List<BulletSubItem> = emptyList()
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFE2E8F0),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF1F5F9)
                )
            }

            Text(
                text = timeRange,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Sub items (e.g. Makruh, Uttom somoy)
        if (subItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            subItems.forEach { subItem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(subItem.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = subItem.text,
                        fontSize = 11.5.sp,
                        color = subItem.color
                    )
                }
            }
        }
    }
}

@Composable
private fun ForbiddenTimeBox(
    title: String,
    timeRange: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ForbiddenCardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, ForbiddenCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD1D5)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeRange,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrayerDivider() {
    HorizontalDivider(
        color = Color(0xFF26333D).copy(alpha = 0.6f),
        thickness = 0.8.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PrayerReferenceDialog(
    type: ReferenceType,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val referenceContent = when (type) {
                    ReferenceType.FARD_PRAYERS -> """
                        • ফজর: সুবহে সাদিক থেকে শুরু হয়ে সূর্যোদয়ের পূর্ব পর্যন্ত। (সহীহ মুসলিম ৬১২)
                        • যুহর: সূর্য পশ্চিমাকাশে ঢলে পড়ার পর থেকে শুরু করে প্রতিটি বস্তুর ছায়া সমপরিমাণ হওয়া পর্যন্ত। (সহীহ বুখারী ৫৪১)
                        • আসর: আসরের ওয়াক্ত শুরু হয় ছায়া দ্বিগুণ হওয়ার পর (হানাফী) বা এক গুণ পর (জমহুর) থেকে সূর্যাস্তের পূর্ব পর্যন্ত।
                        • মাগরিব: সূর্যাস্তের পর থেকে পশ্চিমাকাশের লাল আভা (শাফাক) বিলীন হওয়া পর্যন্ত।
                        • ইশা: পশ্চিমাকাশের লালিমা দূর হওয়ার পর থেকে ফজর উদয় পর্যন্ত (উত্তম সময় রাতের প্রথমার্ধ)।
                    """.trimIndent()

                    ReferenceType.NAFL_PRAYERS -> """
                        • দুহা (ইশরাক/চাশত): সূর্যোদয়ের ১৫-২০ মিনিট পর থেকে ঠিক দ্বিপ্রহরের (জাওয়াল) ১০ মিনিট পূর্ব পর্যন্ত। রাসুলুল্লাহ (ﷺ) নিয়মিত দুহার সালাত পড়ার অসিয়ত করেছেন। (বুখারী ১৯৮১)
                        • জাওয়াল: ঠিক দুপুরে সূর্য যখন মধ্যাকাশে অবস্থান করে, তখন সালাত মাকরূহ। সূর্য সামান্য ঢলে পড়ার পরই যোহরের ওয়াক্ত হয়।
                        • আওয়াবিন: মাগরিবের ফরজের পর ৬ রাকাত পর্যন্ত নফল সালাত আদায় করা মুস্তাহাব।
                        • তাহাজ্জুদ: ইশার সালাত ও ঘুমের পর থেকে সুবহে সাদিক পর্যন্ত। রাতের শেষ তৃতীয়াংশ সর্বোত্তম সময়। (সহীহ বুখারী ১১৪৫)
                    """.trimIndent()

                    ReferenceType.FORBIDDEN_TIMES -> """
                        রাসূলুল্লাহ (ﷺ) তিন সময়ে সালাত আদায় এবং মৃতদের দাফন করতে নিষেধ করেছেন:
                        ১. সূর্যোদয়ের সময়, যতক্ষণ না তা সম্পূর্ণ ওপরে ওঠে (১৫ মিনিট)।
                        ২. ঠিক দুপুরে সূর্য মধ্যাকাশে অবস্থানকালে, যতক্ষণ না তা ঢলে পড়ে।
                        ৩. সূর্যাস্তের সময়, যতক্ষণ না তা পুরোপুরি ডুবে যায়।
                        (সহীহ মুসলিম ৮৩১, সুনানে তিরমিযী ১০৬০)

                        ব্যতিক্রম: কোনো কারণে সেদিন আসরের নামাজ দেরি হয়ে গেলে সূর্যাস্তের পূর্বে হলেও তা আদায় করে নেওয়া আবশ্যক।
                    """.trimIndent()
                }

                Text(
                    text = referenceContent,
                    fontSize = 12.5.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                ) {
                    Text("ঠিক আছে", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DistrictSelectionModal(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    filteredLocations: List<DistrictInfo>,
    selectedDistrict: DistrictInfo,
    onSelect: (DistrictInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isDetectingLocation by remember { mutableStateOf(false) }

    val detectLocation = {
        isDetectingLocation = true
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) {
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)

                if (lastKnown != null) {
                    val closest = PrayerTimesCalculator.findClosestDistrict(lastKnown.latitude, lastKnown.longitude)
                    isDetectingLocation = false
                    Toast.makeText(context, "📍 আপনার অবস্থান: ${closest.nameBn} (${closest.countryBn})", Toast.LENGTH_SHORT).show()
                    onSelect(closest)
                } else {
                    // Request single update
                    val listener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            val closest = PrayerTimesCalculator.findClosestDistrict(loc.latitude, loc.longitude)
                            isDetectingLocation = false
                            Toast.makeText(context, "📍 আপনার অবস্থান: ${closest.nameBn} (${closest.countryBn})", Toast.LENGTH_SHORT).show()
                            onSelect(closest)
                            try { locationManager.removeUpdates(this) } catch (e: Exception) {}
                        }
                    }
                    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, null)
                    } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, null)
                    } else {
                        isDetectingLocation = false
                        Toast.makeText(context, "ডিভাইসের লোকেশন (GPS) চালু করুন", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                isDetectingLocation = false
            }
        } catch (e: Exception) {
            isDetectingLocation = false
            Toast.makeText(context, "লোকেশন নির্ণয় করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            detectLocation()
        } else {
            isDetectingLocation = false
            Toast.makeText(context, "লোকেশন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Title and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "স্থান বা দেশ নির্বাচন করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // GPS Auto Location Button
                Surface(
                    onClick = {
                        val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (fineGranted || coarseGranted) {
                            detectLocation()
                        } else {
                            isDetectingLocation = true
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldAccent.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = EmeraldAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "জিপিএস লোকেশন শনাক্ত করা হচ্ছে...",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Current GPS Location",
                                tint = EmeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "বর্তমান অবস্থান স্বয়ংক্রিয়ভাবে শনাক্ত করুন (GPS)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("জেলা, শহর বা দেশের নাম দিয়ে খুঁজুন...", fontSize = 12.5.sp, color = MutedText) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldAccent) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MutedText, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = EmeraldAccent,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = DarkBackground,
                        unfocusedContainerColor = DarkBackground
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs (only when search is blank so user can switch between Bangladesh & International)
                if (searchQuery.isBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBackground)
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            onClick = { onTabSelected(0) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) EmeraldAccent else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🇧🇩 বাংলাদেশ",
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 0) Color.Black else MutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            onClick = { onTabSelected(1) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) EmeraldAccent else Color.Transparent,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "🌍 আন্তর্জাতিক",
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == 1) Color.Black else MutedText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Locations list
                if (filteredLocations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো স্থান খুঁজে পাওয়া যায়নি",
                            fontSize = 13.sp,
                            color = MutedText
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredLocations) { dist ->
                            val isSel = (dist.id == selectedDistrict.id)
                            val isInternational = (dist.countryBn != "বাংলাদেশ")

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) EmeraldAccent.copy(alpha = 0.15f) else Color.Transparent,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(dist) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = dist.nameBn,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) EmeraldAccent else Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${dist.nameEn})",
                                                fontSize = 12.sp,
                                                color = MutedText
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val subInfo = if (isInternational) {
                                            "দেশ: ${dist.countryBn} • অঞ্চল: ${dist.divisionBn}"
                                        } else {
                                            "বিভাগ: ${dist.divisionBn}"
                                        }

                                        Text(
                                            text = subInfo,
                                            fontSize = 11.sp,
                                            color = if (isSel) EmeraldAccent.copy(alpha = 0.85f) else MutedText
                                        )
                                    }

                                    if (isSel) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = EmeraldAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PrayerTimesCalendarDialog(
    selectedDate: LocalDate,
    hijriOffset: Int,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    val monthNamesBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    val displayedMonthName = monthNamesBn.getOrElse(displayedMonth.monthValue - 1) { "" }
    val displayedYearBn = DateUtil.toBengaliNumerals(displayedMonth.year)

    // Hijri month for current viewed month
    val firstDayHijri = remember(displayedMonth, hijriOffset) {
        HijriCalendarUtil.getHijriDate(displayedMonth.atDay(1), hijriOffset)
    }
    val lastDayHijri = remember(displayedMonth, hijriOffset) {
        HijriCalendarUtil.getHijriDate(displayedMonth.atEndOfMonth(), hijriOffset)
    }
    val hijriMonthHeader = if (firstDayHijri.hijriMonth == lastDayHijri.hijriMonth) {
        "${firstDayHijri.hijriMonthNameBn} ${DateUtil.toBengaliNumerals(firstDayHijri.hijriYear)} হিজরী"
    } else {
        "${firstDayHijri.hijriMonthNameBn} - ${lastDayHijri.hijriMonthNameBn} ${DateUtil.toBengaliNumerals(lastDayHijri.hijriYear)} হিজরী"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF26333D)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight()
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header: Month Navigation & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$displayedMonthName $displayedYearBn",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = hijriMonthHeader,
                            fontSize = 12.sp,
                            color = EmeraldAccent
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Month",
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Month",
                                tint = Color.White
                            )
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Days of week header (Saturday to Friday)
                val weekDays = listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    weekDays.forEachIndexed { index, dayName ->
                        val isFriday = (index == 6)
                        Text(
                            text = dayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFriday) EmeraldAccent else MutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color(0xFF26333D), thickness = 1.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Days grid calculation (Saturday start)
                val firstDayOfMonth = displayedMonth.atDay(1)
                val daysInMonth = displayedMonth.lengthOfMonth()
                
                val firstDayDayOfWeek = firstDayOfMonth.dayOfWeek.value
                val offset = (firstDayDayOfWeek + 1) % 7

                val totalCells = ((offset + daysInMonth + 6) / 7) * 7

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (week in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (dayCol in 0 until 7) {
                                val cellIndex = week * 7 + dayCol
                                val dayNumber = cellIndex - offset + 1

                                if (dayNumber in 1..daysInMonth) {
                                    val date = displayedMonth.atDay(dayNumber)
                                    val isSelected = (date == selectedDate)
                                    val isToday = (date == LocalDate.now())
                                    val hijriDateInfo = HijriCalendarUtil.getHijriDate(date, hijriOffset)

                                    Surface(
                                        onClick = { onDateSelected(date) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isSelected -> EmeraldAccent
                                            isToday -> EmeraldAccent.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        },
                                        border = if (isToday && !isSelected) androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.5f)) else null,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                fontSize = 13.5.sp,
                                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.Black else Color.White
                                            )
                                            Text(
                                                text = DateUtil.toBengaliNumerals(hijriDateInfo.hijriDay),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) Color.Black.copy(alpha = 0.85f) else EmeraldAccent
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f).padding(horizontal = 2.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF26333D), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Bottom actions: Go to Today & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val today = LocalDate.now()
                            displayedMonth = YearMonth.from(today)
                            onDateSelected(today)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = EmeraldAccent
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.4f))
                    ) {
                        Text("আজকের তারিখ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        Text("ঠিক আছে", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SawmSettingsDialog(
    currentSahriOffset: Int,
    currentIftarOffset: Int,
    isNotifSahri: Boolean,
    isNotifIftar: Boolean,
    onUpdateSahriOffset: (Int) -> Unit,
    onUpdateIftarOffset: (Int) -> Unit,
    onToggleSahriNotif: (Boolean) -> Unit,
    onToggleIftarNotif: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DarkModalBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2D3B47)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldAccent.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Restaurant,
                                contentDescription = null,
                                tint = EmeraldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "সাওম (রোজা) সেটিংস",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFF26333D), thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Sahri Offset Selector
                Text(
                    text = "সাহরির সতর্কতামূলক অফসেট",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = "ফজর ওয়াক্ত শুরুর আগে সাহরি শেষ করার সতর্কতা সময় (ডিফল্ট -৩ মিনিট)",
                    fontSize = 11.sp,
                    color = MutedText,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val sahriOptions = listOf(0, -1, -2, -3, -4, -5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sahriOptions.forEach { offset ->
                        val isSelected = currentSahriOffset == offset
                        val label = if (offset == 0) "০ মি." else "${DateUtil.toBengaliNumerals(offset)} মি."
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) EmeraldAccent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) EmeraldAccent else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateSahriOffset(offset) }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) EmeraldAccent else Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Iftar Offset Selector
                Text(
                    text = "ইফতারের সতর্কতামূলক অফসেট",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = "মাগরিব ওয়াক্তের সাথে সতর্কতামূলক অতিরিক্ত সময় (ডিফল্ট ০ মিনিট)",
                    fontSize = 11.sp,
                    color = MutedText,
                    lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                val iftarOptions = listOf(0, 1, 2, 3, 4, 5)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    iftarOptions.forEach { offset ->
                        val isSelected = currentIftarOffset == offset
                        val label = if (offset == 0) "০ মি." else "+${DateUtil.toBengaliNumerals(offset)} মি."
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) AmberWarning.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) AmberWarning else Color(0xFF334155)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onUpdateIftarOffset(offset) }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AmberWarning else Color.White.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF26333D), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // 3. Notification Toggles for Sahri & Iftar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সাহরির শেষ সময়ের নোটিফিকেশন",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "সাহরির শেষ সময় হলে সতর্কবার্তা দেবে",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = isNotifSahri,
                        onCheckedChange = { onToggleSahriNotif(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldAccent,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "ইফতারের সময়ের নোটিফিকেশন",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = "ইফতারের ওয়াক্ত হওয়ার সাথে সাথে বার্তা পাঠাবে",
                            fontSize = 11.sp,
                            color = MutedText
                        )
                    }
                    Switch(
                        checked = isNotifIftar,
                        onCheckedChange = { onToggleIftarNotif(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldAccent,
                            uncheckedThumbColor = Color.LightGray,
                            uncheckedTrackColor = Color(0xFF334155)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Done Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("সংরক্ষণ করুন ও বন্ধ করুন", color = Color.Black, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
