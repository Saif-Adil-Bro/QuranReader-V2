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

enum class ReferenceType(val titleBn: String, val titleEn: String) {
    FARD_PRAYERS("সালাতের ওয়াক্ত সম্পর্কিত হাদিস ও রেফারেন্স", "Hadith & References on Fard Prayer Times"),
    NAFL_PRAYERS("নফল সালাত সম্পর্কিত হাদিস ও রেফারেন্স", "Hadith & References on Nafl Prayers"),
    FORBIDDEN_TIMES("সালাতের নিষিদ্ধ সময় সম্পর্কিত হাদিস ও রেফারেন্স", "Hadith & References on Forbidden Prayer Times")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerTimesDetailSheet(
    schedule: DailyPrayerSchedule,
    isHanafi: Boolean,
    hijriOffset: Int = 0,
    isEnglish: Boolean = false,
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

    val settingsRepo = remember(context) { com.example.data.repository.SettingsRepository.getInstance(context) }
    val storedAppLang by settingsRepo.appLanguageFlow.collectAsState(initial = "bn")
    val effectiveIsEnglish = isEnglish || storedAppLang == "en"

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

    val sahriOffset by settingsRepo.sahriOffsetFlow.collectAsState(initial = -3)
    val iftarOffset by settingsRepo.iftarOffsetFlow.collectAsState(initial = 0)

    var showSawmAdjustDialog by remember { mutableStateOf(false) }
    var selectedWaqtForAlarmSettings by remember { mutableStateOf<com.example.data.model.PrayerName?>(null) }
    var showAlarmOverviewSheet by remember { mutableStateOf(false) }


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
                            contentDescription = if (effectiveIsEnglish) "Close" else "বন্ধ করুন",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (effectiveIsEnglish) "Calendar & Schedule" else "ক্যালেন্ডার ও সময়সূচি",
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
                            val locationLabel = if (effectiveIsEnglish) {
                                if (schedule.district.countryEn == "Bangladesh") schedule.district.nameEn else "${schedule.district.nameEn}, ${schedule.district.countryEn}"
                            } else {
                                if (schedule.district.countryBn == "বাংলাদেশ") schedule.district.nameBn else "${schedule.district.nameBn}, ${schedule.district.countryBn}"
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
                                        hijriOffset = hijriOffset,
                                        isEnglish = effectiveIsEnglish
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
                                contentDescription = if (effectiveIsEnglish) "Share" else "শেয়ার করুন",
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
                    isEnglish = effectiveIsEnglish,
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
                            text = if (effectiveIsEnglish) DateUtil.formatDateEnglish(selectedDate) else DateUtil.getFullHeaderDateStr(selectedDate),
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
                                contentDescription = if (effectiveIsEnglish) "Open Calendar" else "ক্যালেন্ডার খুলুন",
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
                            val shortName = if (effectiveIsEnglish) {
                                dayDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                            } else {
                                DateUtil.getShortDayNameBn(dayDate)
                            }
                            val dayNumberStr = if (effectiveIsEnglish) dayDate.dayOfMonth.toString() else DateUtil.toBengaliNumerals(dayDate.dayOfMonth)

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
                                val hijriCapsuleStr = if (effectiveIsEnglish) {
                                    "${hijriInfo.hijriDay} ${hijriInfo.hijriMonthNameEn}, ${hijriInfo.hijriYear} AH"
                                } else {
                                    "${DateUtil.toBengaliNumerals(hijriInfo.hijriDay)} ${hijriInfo.hijriMonthNameBn}, ${DateUtil.toBengaliNumerals(hijriInfo.hijriYear)}"
                                }
                                Text(
                                    text = hijriCapsuleStr,
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
                                text = if (effectiveIsEnglish) "Return to Today" else "আজকের তারিখে ফিরুন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timetable active waqt calculations for today
            val zoneId = remember(activeSchedule.district) {
                try {
                    java.time.ZoneId.of(activeSchedule.district.timeZoneId)
                } catch (e: Exception) {
                    java.time.ZoneId.of("Asia/Dhaka")
                }
            }
            val nowZoned = java.time.ZonedDateTime.now(zoneId)
            val currentMinutesNow = nowZoned.hour * 60 + nowZoned.minute

            fun getLocalMin(prayerTime: com.example.data.model.SinglePrayerTime?): Int {
                if (prayerTime == null) return 0
                val zdt = java.time.Instant.ofEpochMilli(prayerTime.timestampMillis).atZone(zoneId)
                return zdt.hour * 60 + zdt.minute
            }

            val fTime = activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.FAJR }
            val sTime = activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.SUNRISE }
            val dTime = activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.DHUHR }
            val aTime = activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.ASR }
            val mTime = activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.MAGHRIB }
            val iTime = activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.ISHA }

            val fajrMin = getLocalMin(fTime)
            val sunriseMin = getLocalMin(sTime)
            val duhaStartMin = sunriseMin + 16
            val dhuhrMin = getLocalMin(dTime)
            val duhaEndMin = (dhuhrMin - 4).coerceAtLeast(duhaStartMin)
            val asrMin = getLocalMin(aTime)
            val maghribMin = getLocalMin(mTime)
            val sunsetMakruhMin = maghribMin - 15
            val ishaMin = getLocalMin(iTime)

            // Active flags (Accurate, mutually exclusive & timezone aligned)
            val isFajrActive = isToday && (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.FAJR }?.isCurrent == true)
            val isDhuhrActive = isToday && (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.DHUHR }?.isCurrent == true)
            val isAsrActive = isToday && (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.ASR }?.isCurrent == true)
            val isMaghribActive = isToday && (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.MAGHRIB }?.isCurrent == true)
            val isIshaActive = isToday && (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.ISHA }?.isCurrent == true)

            // Nafl & Forbidden times
            val isSunriseMakruhActive = isToday && (currentMinutesNow in sunriseMin until duhaStartMin)
            val isDuhaActive = isToday && (currentMinutesNow in duhaStartMin until duhaEndMin)
            val isZawalMakruhActive = isToday && (currentMinutesNow in duhaEndMin until dhuhrMin)
            val isSunsetMakruhActive = isToday && (currentMinutesNow in sunsetMakruhMin until maghribMin)
            val isTahajjudActive = isToday && isIshaActive && (currentMinutesNow in 120 until (fajrMin - 15))

            // 3. Section 1: সালাতের সময় (5 Fard Prayers)
            DarkSectionCard(
                title = if (effectiveIsEnglish) "Daily Prayers (Fard)" else "সালাতের সময়",
                referenceLabel = if (effectiveIsEnglish) "View Reference" else "রেফারেন্স দেখুন",
                onReferenceClick = { activeReferenceType = ReferenceType.FARD_PRAYERS }
            ) {
                // Fajr
                PrayerDetailRow(
                    icon = Icons.Outlined.WbTwilight,
                    name = if (effectiveIsEnglish) "Fajr" else "ফজর",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.fajrRange) else activeSchedule.fajrRange,
                    prayerName = com.example.data.model.PrayerName.FAJR,
                    isCurrentWaqt = isFajrActive,
                    currentBadgeText = if (effectiveIsEnglish) "Active" else "চলমান",
                    onAlarmClick = { selectedWaqtForAlarmSettings = it }
                )

                if (!isFajrActive && !isDhuhrActive) PrayerDivider()

                // Dhuhr / Jumu'ah on Friday
                val dhuhrName = if (effectiveIsEnglish) {
                    if (activeSchedule.isFriday) "Jumu'ah" else "Dhuhr"
                } else {
                    if (activeSchedule.isFriday) "জুমুআ" else "যোহর"
                }
                PrayerDetailRow(
                    icon = Icons.Outlined.WbSunny,
                    name = dhuhrName,
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.dhuhrRange) else activeSchedule.dhuhrRange,
                    prayerName = com.example.data.model.PrayerName.DHUHR,
                    isCurrentWaqt = isDhuhrActive,
                    currentBadgeText = if (effectiveIsEnglish) "Active" else "চলমান",
                    onAlarmClick = { selectedWaqtForAlarmSettings = it }
                )

                if (!isDhuhrActive && !isAsrActive) PrayerDivider()

                // Asr
                PrayerDetailRow(
                    icon = Icons.Outlined.Brightness5,
                    name = if (effectiveIsEnglish) "Asr" else "আসর",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.asrRange) else activeSchedule.asrRange,
                    subItems = listOf(
                        BulletSubItem(
                            if (effectiveIsEnglish) "Makruh: ${DateUtil.toEnglishNumerals(activeSchedule.asrMakruhTime)}" else "মাকরূহ: ${activeSchedule.asrMakruhTime}",
                            AmberBullet
                        )
                    ),
                    prayerName = com.example.data.model.PrayerName.ASR,
                    isCurrentWaqt = isAsrActive,
                    currentBadgeText = if (effectiveIsEnglish) "Active" else "চলমান",
                    onAlarmClick = { selectedWaqtForAlarmSettings = it }
                )

                if (!isAsrActive && !isMaghribActive) PrayerDivider()

                // Maghrib
                PrayerDetailRow(
                    icon = Icons.Outlined.WbCloudy,
                    name = if (effectiveIsEnglish) "Maghrib" else "মাগরিব",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.maghribRange) else activeSchedule.maghribRange,
                    prayerName = com.example.data.model.PrayerName.MAGHRIB,
                    isCurrentWaqt = isMaghribActive,
                    currentBadgeText = if (effectiveIsEnglish) "Active" else "চলমান",
                    onAlarmClick = { selectedWaqtForAlarmSettings = it }
                )

                if (!isMaghribActive && !isIshaActive) PrayerDivider()

                // Isha
                PrayerDetailRow(
                    icon = Icons.Outlined.Nightlight,
                    name = if (effectiveIsEnglish) "Isha" else "এশা",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.ishaRange) else activeSchedule.ishaRange,
                    subItems = listOf(
                        BulletSubItem(
                            if (effectiveIsEnglish) "Preferred Time Ends: ${DateUtil.toEnglishNumerals(activeSchedule.ishaUttomTime)}" else "উত্তম সময় শেষ: ${activeSchedule.ishaUttomTime}",
                            GreenBullet
                        ),
                        BulletSubItem(
                            if (effectiveIsEnglish) "Makruh: ${DateUtil.toEnglishNumerals(activeSchedule.ishaMakruhTime)}" else "মাকরূহ: ${activeSchedule.ishaMakruhTime}",
                            AmberBullet
                        )
                    ),
                    prayerName = com.example.data.model.PrayerName.ISHA,
                    isCurrentWaqt = isIshaActive,
                    currentBadgeText = if (effectiveIsEnglish) "Active" else "চলমান",
                    onAlarmClick = { selectedWaqtForAlarmSettings = it }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Section 2: নফল সালাতের সময়
            DarkSectionCard(
                title = if (effectiveIsEnglish) "Nafl Prayers" else "নফল সালাতের সময়",
                referenceLabel = if (effectiveIsEnglish) "View Reference" else "রেফারেন্স দেখুন",
                onReferenceClick = { activeReferenceType = ReferenceType.NAFL_PRAYERS }
            ) {
                // Duha & Chasht (Ishraq)
                PrayerDetailRow(
                    icon = Icons.Outlined.WbSunny,
                    name = if (effectiveIsEnglish) "Chasht & Duha (Ishraq)" else "চাশত ও দুহা (ইশরাক)",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.duhaRange) else activeSchedule.duhaRange,
                    isCurrentWaqt = isDuhaActive,
                    currentBadgeText = if (effectiveIsEnglish) "Active Nafl" else "চলমান নফল"
                )

                if (!isDuhaActive && !isZawalMakruhActive) PrayerDivider()

                // Zawal
                PrayerDetailRow(
                    icon = Icons.Outlined.AccountBalance,
                    name = if (effectiveIsEnglish) "Zawal Starts" else "জাওয়াল শুরু",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.zawalStartTime) else activeSchedule.zawalStartTime,
                    isCurrentWaqt = isZawalMakruhActive,
                    currentBadgeText = if (effectiveIsEnglish) "Forbidden Time" else "নিষিদ্ধ সময়"
                )

                if (!isZawalMakruhActive) PrayerDivider()

                // Awwabin
                PrayerDetailRow(
                    icon = Icons.Outlined.WbCloudy,
                    name = if (effectiveIsEnglish) "Awwabin" else "আওয়াবিন",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.awwabinRange) else activeSchedule.awwabinRange,
                    isCurrentWaqt = isMaghribActive,
                    currentBadgeText = if (effectiveIsEnglish) "After Maghrib" else "মাগরিবের পর"
                )

                if (!isTahajjudActive) PrayerDivider()

                // Tahajjud
                PrayerDetailRow(
                    icon = Icons.Outlined.Bedtime,
                    name = if (effectiveIsEnglish) "Tahajjud" else "তাহাজ্জুদ",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.tahajjudRange) else activeSchedule.tahajjudRange,
                    subItems = listOf(
                        BulletSubItem(
                            if (effectiveIsEnglish) "Last 1/3 of Night Starts: ${DateUtil.toEnglishNumerals(activeSchedule.tahajjudLastThirdStart)}" else "রাতের শেষ ১/৩ শুরু: ${activeSchedule.tahajjudLastThirdStart}",
                            GreenBullet
                        )
                    ),
                    prayerName = com.example.data.model.PrayerName.TAHAJJUD,
                    isCurrentWaqt = isTahajjudActive,
                    currentBadgeText = if (effectiveIsEnglish) "Best Time" else "উত্তম সময়",
                    onAlarmClick = { selectedWaqtForAlarmSettings = it }
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
                            text = if (effectiveIsEnglish) "Forbidden Prayer Times" else "সালাতের নিষিদ্ধ সময়",
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = ForbiddenTextRed
                        )

                        Text(
                            text = if (effectiveIsEnglish) "View Reference" else "রেফারেন্স দেখুন",
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
                        text = if (effectiveIsEnglish) "Prayers are prohibited during these times (Exception: Asr of the same day during sunset)" else "এই সময়গুলোতে সালাত আদায় নিষিদ্ধ (ব্যতিক্রম: সূর্যাস্তকালীন একই দিনের আসর সালাত)",
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
                            title = if (effectiveIsEnglish) "Morning" else "সকাল",
                            timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.forbiddenMorningRange) else activeSchedule.forbiddenMorningRange,
                            prayerName = com.example.data.model.PrayerName.MAKRUH_SUNRISE,
                            isActive = isSunriseMakruhActive,
                            isEnglish = effectiveIsEnglish,
                            onAlarmClick = { selectedWaqtForAlarmSettings = it },
                            modifier = Modifier.weight(1f)
                        )
                        ForbiddenTimeBox(
                            title = if (effectiveIsEnglish) "Midday" else "দুপুর",
                            timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.forbiddenNoonRange) else activeSchedule.forbiddenNoonRange,
                            prayerName = com.example.data.model.PrayerName.MAKRUH_ZAWAL,
                            isActive = isZawalMakruhActive,
                            isEnglish = effectiveIsEnglish,
                            onAlarmClick = { selectedWaqtForAlarmSettings = it },
                            modifier = Modifier.weight(1f)
                        )
                        ForbiddenTimeBox(
                            title = if (effectiveIsEnglish) "Evening" else "সন্ধ্যা",
                            timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.forbiddenEveningRange) else activeSchedule.forbiddenEveningRange,
                            prayerName = com.example.data.model.PrayerName.MAKRUH_SUNSET,
                            isActive = isSunsetMakruhActive,
                            isEnglish = effectiveIsEnglish,
                            onAlarmClick = { selectedWaqtForAlarmSettings = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Section 4: সাওমের সময়সূচী (Fasting / Sawm)
            DarkSectionCard(
                title = if (effectiveIsEnglish) "Fasting (Sawm) Schedule" else "সাওমের সময়সূচী",
                referenceLabel = if (effectiveIsEnglish) "Settings & Alerts" else "সেটিংস ও সতর্কতা",
                onReferenceClick = { showSawmAdjustDialog = true }
            ) {
                PrayerDetailRow(
                    icon = Icons.Outlined.Restaurant,
                    name = if (effectiveIsEnglish) "Sahri End" else "সাহরি",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.sahriTimeDigits) else activeSchedule.sahriTimeDigits,
                    subItems = listOf(
                        BulletSubItem(
                            text = if (effectiveIsEnglish) {
                                if (sahriOffset != 0) "Precautionary offset: ${sahriOffset} mins" else "Standard Fajr start"
                            } else {
                                if (sahriOffset != 0) "সতর্কতামূলক অফসেট: ${DateUtil.toBengaliNumerals(sahriOffset)} মি." else "স্ট্যান্ডার্ড ফজর ওয়াক্ত"
                            },
                            color = EmeraldAccent
                        )
                    )
                )

                PrayerDivider()

                PrayerDetailRow(
                    icon = Icons.Outlined.SoupKitchen,
                    name = if (effectiveIsEnglish) "Iftar" else "ইফতার",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.iftarTimeDigits) else activeSchedule.iftarTimeDigits,
                    subItems = listOf(
                        BulletSubItem(
                            text = if (effectiveIsEnglish) {
                                if (iftarOffset != 0) "Precautionary offset: +${iftarOffset} mins" else "Maghrib start"
                            } else {
                                if (iftarOffset != 0) "সতর্কতামূলক অফসেট: ${DateUtil.toBengaliNumerals(iftarOffset)} মি." else "মাগরিব ওয়াক্ত শুরু"
                            },
                            color = AmberWarning
                        )
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Section 5: সূর্যোদয় ও সূর্যাস্ত
            DarkSectionCard(
                title = if (effectiveIsEnglish) "Sunrise & Sunset" else "সূর্যোদয় ও সূর্যাস্ত"
            ) {
                PrayerDetailRow(
                    icon = Icons.Outlined.WbTwilight,
                    name = if (effectiveIsEnglish) "Sunrise" else "সূর্যোদয়",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.sunriseTimeDigits) else activeSchedule.sunriseTimeDigits
                )

                PrayerDivider()

                PrayerDetailRow(
                    icon = Icons.Outlined.WbSunny,
                    name = if (effectiveIsEnglish) "Sunset" else "সূর্যাস্ত",
                    timeRange = if (effectiveIsEnglish) DateUtil.toEnglishNumerals(activeSchedule.sunsetTimeDigits) else activeSchedule.sunsetTimeDigits
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
                                    text = if (effectiveIsEnglish) "Prayer Time Notifications" else "ওয়াক্ত শুরুর নোটিফিকেশন",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (effectiveIsEnglish) {
                                        if (isMasterNotifEnabled) "Automatic alert when waqt begins" else "Notifications are turned off"
                                    } else {
                                        if (isMasterNotifEnabled) "ওয়াক্ত শুরু হলে স্বয়ংক্রিয় অ্যালার্ট আসবে" else "নোটিফিকেশন বন্ধ রয়েছে"
                                    },
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
                                    Toast.makeText(context, if (effectiveIsEnglish) "Prayer notifications enabled" else "ওয়াক্তের নোটিফিকেশন চালু করা হয়েছে", Toast.LENGTH_SHORT).show()
                                } else {
                                    isMasterNotifEnabled = false
                                    com.example.utils.PrayerNotificationHelper.setMasterEnabled(context, false)
                                    Toast.makeText(context, if (effectiveIsEnglish) "Prayer notifications disabled" else "ওয়াক্তের নোটিফিকেশন বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
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
                            text = if (effectiveIsEnglish) "Select which waqt notifications you want:" else "কোন কোন ওয়াক্তের নোটিফিকেশন চান নির্বাচন করুন:",
                            fontSize = 11.5.sp,
                            color = MutedText
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Chips for individual prayers
                        val dhuhrToggleName = if (effectiveIsEnglish) {
                            if (activeSchedule.isFriday) "Jumu'ah" else "Dhuhr"
                        } else {
                            if (activeSchedule.isFriday) "জুমুআ" else "যোহর"
                        }
                        val prayersToToggle = listOf(
                            Triple(if (effectiveIsEnglish) "Sahri" else "সাহরি", isNotifSahri, com.example.data.model.PrayerName.SAHRI),
                            Triple(if (effectiveIsEnglish) "Fajr" else "ফজর", isNotifFajr, com.example.data.model.PrayerName.FAJR),
                            Triple(dhuhrToggleName, isNotifDhuhr, com.example.data.model.PrayerName.DHUHR),
                            Triple(if (effectiveIsEnglish) "Asr" else "আসর", isNotifAsr, com.example.data.model.PrayerName.ASR),
                            Triple(if (effectiveIsEnglish) "Maghrib" else "মাগরিব", isNotifMaghrib, com.example.data.model.PrayerName.MAGHRIB),
                            Triple(if (effectiveIsEnglish) "Iftar" else "ইফতার", isNotifIftar, com.example.data.model.PrayerName.IFTAR),
                            Triple(if (effectiveIsEnglish) "Isha" else "এশা", isNotifIsha, com.example.data.model.PrayerName.ISHA)
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
                                    text = if (effectiveIsEnglish) "Sound & Vibration" else "সাউন্ড ও ভাইব্রেশন",
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dynamic Alarm & Adhan Customization Button
                        Surface(
                            onClick = { showAlarmOverviewSheet = true },
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldAccent.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (effectiveIsEnglish) "Dynamic Alarm & Adhan Settings" else "ডায়নামিক অ্যালার্ম ও আজান সেটিংস",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (effectiveIsEnglish) "Waqt time adjustments, Adhan & ringtone selection" else "ওয়াক্ত অনুযায়ী সময় সমন্বয়, আজান ও রিংটোন নির্বাচন",
                                            fontSize = 10.5.sp,
                                            color = EmeraldAccent
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = EmeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
                        val asrMethodStr = if (effectiveIsEnglish) {
                            "Asr Method: ${if (isHanafi) "Hanafi (2x Shadow)" else "Shafi'i / Standard"}"
                        } else {
                            "আসরের পদ্ধতি: ${if (isHanafi) "হানাফী (মিছলে সানি)" else "শাফেয়ী / জমহুর"}"
                        }
                        Text(
                            text = asrMethodStr,
                            fontSize = 12.sp,
                            color = MutedText
                        )
                    }

                    TextButton(
                        onClick = { onHanafiChanged(!isHanafi) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        val asrBtnStr = if (effectiveIsEnglish) {
                            if (isHanafi) "Change" else "Use Hanafi"
                        } else {
                            if (isHanafi) "পরিবর্তন" else "হানাফী করুন"
                        }
                        Text(
                            text = asrBtnStr,
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
                Text(
                    text = if (effectiveIsEnglish) "Copy Prayer Times Text" else "সময়সূচির টেক্সট কপি করুন",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    // Reference Hadith Dialog
    if (activeReferenceType != null) {
        PrayerReferenceDialog(
            type = activeReferenceType!!,
            isEnglish = effectiveIsEnglish,
            onDismiss = { activeReferenceType = null }
        )
    }

    // Calendar Picker Dialog
    if (showCalendarDialog) {
        PrayerTimesCalendarDialog(
            selectedDate = selectedDate,
            hijriOffset = hijriOffset,
            isEnglish = effectiveIsEnglish,
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
            isEnglish = effectiveIsEnglish,
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
            isEnglish = effectiveIsEnglish,
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

    // Dynamic Waqt Alarm Customization Dialog
    if (selectedWaqtForAlarmSettings != null) {
        val pName = selectedWaqtForAlarmSettings!!
        val matchingPrayer = activeSchedule.prayers.find { it.name == pName }
        val formattedTime = when (pName) {
            com.example.data.model.PrayerName.SAHRI -> activeSchedule.sahriEndTimeFormatted
            com.example.data.model.PrayerName.IFTAR -> activeSchedule.iftarTimeFormatted
            com.example.data.model.PrayerName.TAHAJJUD -> activeSchedule.tahajjudEndTimeFormatted
            com.example.data.model.PrayerName.MAKRUH_SUNRISE -> activeSchedule.forbiddenSunriseFormatted.ifBlank { activeSchedule.forbiddenMorningRange }
            com.example.data.model.PrayerName.MAKRUH_ZAWAL -> activeSchedule.forbiddenMiddayFormatted.ifBlank { activeSchedule.forbiddenNoonRange }
            com.example.data.model.PrayerName.MAKRUH_SUNSET -> activeSchedule.forbiddenSunsetFormatted.ifBlank { activeSchedule.forbiddenEveningRange }
            else -> matchingPrayer?.timeFormatted ?: ""
        }
        val timestampMillis = when (pName) {
            com.example.data.model.PrayerName.MAKRUH_SUNRISE -> activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.SUNRISE }?.timestampMillis ?: 0L
            com.example.data.model.PrayerName.MAKRUH_ZAWAL -> (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.DHUHR }?.timestampMillis ?: 0L) - 12 * 60 * 1000L
            com.example.data.model.PrayerName.MAKRUH_SUNSET -> (activeSchedule.prayers.find { it.name == com.example.data.model.PrayerName.MAGHRIB }?.timestampMillis ?: 0L) - 15 * 60 * 1000L
            else -> matchingPrayer?.timestampMillis ?: 0L
        }

        WaqtAlarmConfigDialog(
            prayerName = pName,
            baseTimeFormatted = formattedTime,
            baseTimestampMillis = timestampMillis,
            isEnglish = effectiveIsEnglish,
            isFriday = activeSchedule.isFriday,
            onDismiss = {
                selectedWaqtForAlarmSettings = null
                isMasterNotifEnabled = com.example.utils.PrayerNotificationHelper.isMasterEnabled(context)
                isNotifFajr = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.FAJR)
                isNotifDhuhr = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.DHUHR)
                isNotifAsr = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.ASR)
                isNotifMaghrib = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.MAGHRIB)
                isNotifIsha = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.ISHA)
                isNotifSahri = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.SAHRI)
                isNotifIftar = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.IFTAR)
                isNotifSound = com.example.utils.PrayerNotificationHelper.isSoundEnabled(context)
            }
        )
    }

    // Dynamic Waqt Alarm Overview Sheet
    if (showAlarmOverviewSheet) {
        WaqtAlarmOverviewSheet(
            schedule = activeSchedule,
            isEnglish = effectiveIsEnglish,
            onDismiss = {
                showAlarmOverviewSheet = false
                isMasterNotifEnabled = com.example.utils.PrayerNotificationHelper.isMasterEnabled(context)
                isNotifFajr = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.FAJR)
                isNotifDhuhr = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.DHUHR)
                isNotifAsr = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.ASR)
                isNotifMaghrib = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.MAGHRIB)
                isNotifIsha = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.ISHA)
                isNotifSahri = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.SAHRI)
                isNotifIftar = com.example.utils.PrayerNotificationHelper.isPrayerEnabled(context, com.example.data.model.PrayerName.IFTAR)
                isNotifSound = com.example.utils.PrayerNotificationHelper.isSoundEnabled(context)
            }
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
    subItems: List<BulletSubItem> = emptyList(),
    prayerName: com.example.data.model.PrayerName? = null,
    isCurrentWaqt: Boolean = false,
    currentBadgeText: String = "চলমান",
    onAlarmClick: ((com.example.data.model.PrayerName) -> Unit)? = null
) {
    val context = LocalContext.current
    val config = prayerName?.let {
        com.example.utils.PrayerNotificationHelper.getPrayerAlarmConfig(context, it)
    }

    // Pure soft background highlight without any heavy borders
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isCurrentWaqt) Color(0xFF10B981).copy(alpha = 0.14f) else Color.Transparent)
            .padding(
                horizontal = if (isCurrentWaqt) 10.dp else 4.dp,
                vertical = if (isCurrentWaqt) 8.dp else 5.dp
            )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (isCurrentWaqt) Color(0xFF10B981).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isCurrentWaqt) Color(0xFF34D399) else Color(0xFFE2E8F0),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = name,
                                fontSize = 14.5.sp,
                                fontWeight = if (isCurrentWaqt) FontWeight.Bold else FontWeight.Medium,
                                color = if (isCurrentWaqt) Color.White else Color(0xFFF1F5F9)
                            )
                            if (isCurrentWaqt) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.25f))
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34D399))
                                        )
                                        Spacer(modifier = Modifier.width(3.5.dp))
                                        Text(
                                            text = currentBadgeText,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF34D399)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeRange,
                        fontSize = 15.sp,
                        fontWeight = if (isCurrentWaqt) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isCurrentWaqt) Color(0xFF6EE7B7) else Color.White
                    )

                    if (prayerName != null && onAlarmClick != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { onAlarmClick(prayerName) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (config?.isEnabled == true) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "অ্যালার্ম কাস্টমাইজ করুন",
                                tint = if (config?.isEnabled == true) EmeraldAccent else Color(0xFF64748B),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // Sub items (e.g. Makruh, Uttom somoy)
            if (subItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                subItems.forEach { subItem ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(subItem.color)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = subItem.text,
                            fontSize = 11.sp,
                            color = subItem.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForbiddenTimeBox(
    title: String,
    timeRange: String,
    prayerName: com.example.data.model.PrayerName? = null,
    isActive: Boolean = false,
    isEnglish: Boolean = false,
    onAlarmClick: ((com.example.data.model.PrayerName) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = remember(prayerName) {
        prayerName?.let { com.example.utils.PrayerNotificationHelper.getPrayerAlarmConfig(context, it) }
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFFDC2626).copy(alpha = 0.15f) else ForbiddenCardBg,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isActive) Color(0xFFEF4444).copy(alpha = 0.4f) else ForbiddenCardBorder
        ),
        modifier = modifier
            .then(
                if (prayerName != null && onAlarmClick != null) {
                    Modifier.clickable { onAlarmClick(prayerName) }
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isActive) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFDC2626).copy(alpha = 0.35f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isEnglish) "● Forbidden" else "● এখন নিষিদ্ধ",
                        color = Color(0xFFFCA5A5),
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) Color(0xFFFCA5A5) else Color(0xFFFFD1D5)
                )
                if (prayerName != null && config != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (config.isEnabled) Icons.Filled.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = "মাকরূহ অ্যালার্ট",
                        tint = if (config.isEnabled) Color(0xFFF87171) else Color(0xFF64748B),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
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
    isEnglish: Boolean = false,
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
                        text = if (isEnglish) type.titleEn else type.titleBn,
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
                    ReferenceType.FARD_PRAYERS -> if (isEnglish) """
                        • Fajr: From dawn (Subh Sadiq) until before sunrise. (Sahih Muslim 612)
                        • Dhuhr: After the sun passes the meridian until an object's shadow equals its length. (Sahih Bukhari 541)
                        • Asr: When shadow is twice the length (Hanafi) or equal length (Jumhoor) until sunset.
                        • Maghrib: From sunset until the red twilight (Shafaq) disappears.
                        • Isha: From disappearance of twilight until dawn (preferred in the first half of night).
                    """.trimIndent() else """
                        • ফজর: সুবহে সাদিক থেকে শুরু হয়ে সূর্যোদয়ের পূর্ব পর্যন্ত। (সহীহ মুসলিম ৬১২)
                        • যোহর: সূর্য পশ্চিমাকাশে ঢলে পড়ার পর থেকে শুরু করে প্রতিটি বস্তুর ছায়া সমপরিমাণ হওয়া পর্যন্ত। (সহীহ বুখারী ৫৪১)
                        • আসর: আসরের ওয়াক্ত শুরু হয় ছায়া দ্বিগুণ হওয়ার পর (হানাফী) বা এক গুণ পর (জমহুর) থেকে সূর্যাস্তের পূর্ব পর্যন্ত।
                        • মাগরিব: সূর্যাস্তের পর থেকে পশ্চিমাকাশের লাল আভা (শাফাক) বিলীন হওয়া পর্যন্ত।
                        • এশা: পশ্চিমাকাশের লালিমা দূর হওয়ার পর থেকে ফজর উদয় পর্যন্ত (উত্তম সময় রাতের প্রথমার্ধ)।
                    """.trimIndent()

                    ReferenceType.NAFL_PRAYERS -> if (isEnglish) """
                        • Duha (Ishraq / Chasht): From 15-20 minutes after sunrise until 10 minutes before midday (Zawal). The Prophet (ﷺ) advised observing Duha prayer regularly. (Sahih Bukhari 1981)
                        • Zawal: Midday zenith when the sun is at its highest point; praying is forbidden until the sun declines slightly.
                        • Awwabin: 6 rak'ahs of voluntary prayer after Maghrib fard are recommended.
                        • Tahajjud: After Isha prayer and sleep until Subh Sadiq (dawn). The last third of the night is the most virtuous time. (Sahih Bukhari 1145)
                    """.trimIndent() else """
                        • দুহা (ইশরাক/চাশত): সূর্যোদয়ের ১৫-২০ মিনিট পর থেকে ঠিক দ্বিপ্রহরের (জাওয়াল) ১০ মিনিট পূর্ব পর্যন্ত। রাসুলুল্লাহ (ﷺ) নিয়মিত দুহার সালাত পড়ার অসিয়ত করেছেন। (বুখারী ১৯৮১)
                        • জাওয়াল: ঠিক দুপুরে সূর্য যখন মধ্যাকাশে অবস্থান করে, তখন সালাত মাকরূহ। সূর্য সামান্য ঢলে পড়ার পরই যোহরের ওয়াক্ত হয়।
                        • আওয়াবিন: মাগরিবের ফরজের পর ৬ রাকাত পর্যন্ত নফল সালাত আদায় করা মুস্তাহাব।
                        • তাহাজ্জুদ: এশার সালাত ও ঘুমের পর থেকে সুবহে সাদিক পর্যন্ত। রাতের শেষ তৃতীয়াংশ সর্বোত্তম সময়। (সহীহ বুখারী ১১৪৫)
                    """.trimIndent()

                    ReferenceType.FORBIDDEN_TIMES -> if (isEnglish) """
                        The Messenger of Allah (ﷺ) forbade prayers and burying the dead at three times:
                        1. At sunrise, until it has fully risen (approx. 15 mins).
                        2. At midday when the sun is at its meridian, until it declines.
                        3. At sunset, until it has completely set.
                        (Sahih Muslim 831, Jami` at-Tirmidhi 1060)

                        Exception: If Asr prayer was delayed for a valid reason, it must be performed even shortly before sunset.
                    """.trimIndent() else """
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
                    Text(if (isEnglish) "OK" else "ঠিক আছে", color = Color.Black, fontWeight = FontWeight.Bold)
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
    isEnglish: Boolean = false,
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
                    val locMsg = if (isEnglish) "📍 Your location: ${closest.nameEn} (${closest.countryEn})" else "📍 আপনার অবস্থান: ${closest.nameBn} (${closest.countryBn})"
                    Toast.makeText(context, locMsg, Toast.LENGTH_SHORT).show()
                    onSelect(closest)
                } else {
                    // Request single update
                    val listener = object : LocationListener {
                        override fun onLocationChanged(loc: Location) {
                            val closest = PrayerTimesCalculator.findClosestDistrict(loc.latitude, loc.longitude)
                            isDetectingLocation = false
                            val locMsg = if (isEnglish) "📍 Your location: ${closest.nameEn} (${closest.countryEn})" else "📍 আপনার অবস্থান: ${closest.nameBn} (${closest.countryBn})"
                            Toast.makeText(context, locMsg, Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context, if (isEnglish) "Please enable device location (GPS)" else "ডিভাইসের লোকেশন (GPS) চালু করুন", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                isDetectingLocation = false
            }
        } catch (e: Exception) {
            isDetectingLocation = false
            Toast.makeText(context, if (isEnglish) "Failed to detect location" else "লোকেশন নির্ণয় করতে সমস্যা হয়েছে", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(context, if (isEnglish) "Location permission required" else "লোকেশন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
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
                        text = if (isEnglish) "Select Location or Country" else "স্থান বা দেশ নির্বাচন করুন",
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
                                text = if (isEnglish) "Detecting GPS location..." else "জিপিএস লোকেশন শনাক্ত করা হচ্ছে...",
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
                                text = if (isEnglish) "Auto-detect current location (GPS)" else "বর্তমান অবস্থান স্বয়ংক্রিয়ভাবে শনাক্ত করুন (GPS)",
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
                    placeholder = { Text(if (isEnglish) "Search district, city or country..." else "জেলা, শহর বা দেশের নাম দিয়ে খুঁজুন...", fontSize = 12.5.sp, color = MutedText) },
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
                                text = if (isEnglish) "🇧🇩 Bangladesh" else "🇧🇩 বাংলাদেশ",
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
                                text = if (isEnglish) "🌍 International" else "🌍 আন্তর্জাতিক",
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
                            text = if (isEnglish) "No locations found" else "কোনো স্থান খুঁজে পাওয়া যায়নি",
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
                                                text = if (isEnglish) dist.nameEn else dist.nameBn,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSel) EmeraldAccent else Color.White
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isEnglish) "(${dist.nameBn})" else "(${dist.nameEn})",
                                                fontSize = 12.sp,
                                                color = MutedText
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        val subInfo = if (isInternational) {
                                            if (isEnglish) "Country: ${dist.countryEn}" + (if (dist.divisionEn.isNotBlank()) " • Region: ${dist.divisionEn}" else "")
                                            else "দেশ: ${dist.countryBn} • অঞ্চল: ${dist.divisionBn}"
                                        } else {
                                            val divName = if (isEnglish && dist.divisionEn.isNotBlank()) dist.divisionEn else dist.divisionBn
                                            if (isEnglish) "Division: $divName" else "বিভাগ: ${dist.divisionBn}"
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
    isEnglish: Boolean = false,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

    val monthNamesBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )
    val monthNamesEn = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val displayedMonthName = if (isEnglish) {
        monthNamesEn.getOrElse(displayedMonth.monthValue - 1) { "" }
    } else {
        monthNamesBn.getOrElse(displayedMonth.monthValue - 1) { "" }
    }
    val displayedYearStr = if (isEnglish) displayedMonth.year.toString() else DateUtil.toBengaliNumerals(displayedMonth.year)

    // Hijri month for current viewed month
    val firstDayHijri = remember(displayedMonth, hijriOffset) {
        HijriCalendarUtil.getHijriDate(displayedMonth.atDay(1), hijriOffset)
    }
    val lastDayHijri = remember(displayedMonth, hijriOffset) {
        HijriCalendarUtil.getHijriDate(displayedMonth.atEndOfMonth(), hijriOffset)
    }
    val hijriMonthHeader = if (isEnglish) {
        if (firstDayHijri.hijriMonth == lastDayHijri.hijriMonth) {
            "${firstDayHijri.hijriMonthNameEn} ${firstDayHijri.hijriYear} AH"
        } else {
            "${firstDayHijri.hijriMonthNameEn} - ${lastDayHijri.hijriMonthNameEn} ${lastDayHijri.hijriYear} AH"
        }
    } else {
        if (firstDayHijri.hijriMonth == lastDayHijri.hijriMonth) {
            "${firstDayHijri.hijriMonthNameBn} ${DateUtil.toBengaliNumerals(firstDayHijri.hijriYear)} হিজরী"
        } else {
            "${firstDayHijri.hijriMonthNameBn} - ${lastDayHijri.hijriMonthNameBn} ${DateUtil.toBengaliNumerals(lastDayHijri.hijriYear)} হিজরী"
        }
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
                            text = "$displayedMonthName $displayedYearStr",
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
                val weekDays = if (isEnglish) {
                    listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
                } else {
                    listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র")
                }
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
                                                text = if (isEnglish) hijriDateInfo.hijriDay.toString() else DateUtil.toBengaliNumerals(hijriDateInfo.hijriDay),
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
                        Text(if (isEnglish) "Today" else "আজকের তারিখ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent)
                    ) {
                        Text(if (isEnglish) "OK" else "ঠিক আছে", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
    isEnglish: Boolean = false,
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
                            text = if (isEnglish) "Sawm (Fasting) Settings" else "সাওম (রোজা) সেটিংস",
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
                    text = if (isEnglish) "Sahri Precautionary Offset" else "সাহরির সতর্কতামূলক অফসেট",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = if (isEnglish) "Precautionary buffer before Fajr begins (default -3 mins)" else "ফজর ওয়াক্ত শুরুর আগে সাহরি শেষ করার সতর্কতা সময় (ডিফল্ট -৩ মিনিট)",
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
                        val label = if (offset == 0) {
                            if (isEnglish) "0 min" else "০ মি."
                        } else {
                            if (isEnglish) "$offset min" else "${DateUtil.toBengaliNumerals(offset)} মি."
                        }
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
                    text = if (isEnglish) "Iftar Precautionary Offset" else "ইফতারের সতর্কতামূলক অফসেট",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF1F5F9)
                )
                Text(
                    text = if (isEnglish) "Additional buffer added to Maghrib time (default 0 min)" else "মাগরিব ওয়াক্তের সাথে সতর্কতামূলক অতিরিক্ত সময় (ডিফল্ট ০ মিনিট)",
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
                        val label = if (offset == 0) {
                            if (isEnglish) "0 min" else "০ মি."
                        } else {
                            if (isEnglish) "+$offset min" else "+${DateUtil.toBengaliNumerals(offset)} মি."
                        }
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
                            text = if (isEnglish) "Sahri End Time Notification" else "সাহরির শেষ সময়ের নোটিফিকেশন",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnglish) "Alert when Sahri time is ending" else "সাহরির শেষ সময় হলে সতর্কবার্তা দেবে",
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
                            text = if (isEnglish) "Iftar Time Notification" else "ইফতারের সময়ের নোটিফিকেশন",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            text = if (isEnglish) "Alert immediately when Iftar arrives" else "ইফতারের ওয়াক্ত হওয়ার সাথে সাথে বার্তা পাঠাবে",
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
                    Text(if (isEnglish) "Save & Close" else "সংরক্ষণ করুন ও বন্ধ করুন", color = Color.Black, fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
