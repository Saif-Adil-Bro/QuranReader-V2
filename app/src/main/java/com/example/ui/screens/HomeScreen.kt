package com.example.ui.screens

import com.example.data.QuranData
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Palette
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodels.HomeViewModel
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt
import androidx.compose.foundation.BorderStroke
import com.example.data.model.CombinedAyah
import androidx.compose.foundation.interaction.MutableInteractionSource
import java.util.Calendar
import kotlinx.coroutines.delay
import com.example.utils.DateUtil
import androidx.compose.foundation.border
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.geometry.Offset

fun String.toArabicNumerals(): String {
    val englishNumerals = "0123456789"
    val arabicNumerals = "٠١٢٣٤٥٦٧٨٩"
    return this.map { char ->
        val index = englishNumerals.indexOf(char)
        if (index != -1) arabicNumerals[index] else char
    }.joinToString("")
}

fun String.toBengaliNumerals(): String {
    val englishNumerals = "0123456789"
    val bengaliNumerals = "০১২৩৪৫৬৭৮৯"
    return this.map { char ->
        val index = englishNumerals.indexOf(char)
        if (index != -1) bengaliNumerals[index] else char
    }.joinToString("")
}

fun Int.toBengaliNumerals(): String {
    return this.toString().toBengaliNumerals()
}

@Composable
fun TajweedLegendDialog(onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = "তাজবীদের রঙের পরিচিতি",
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            androidx.compose.foundation.lazy.LazyColumn(
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                items(com.example.ui.theme.TajweedLegend.toList().size) { index ->
                    val (key, pair) = com.example.ui.theme.TajweedLegend.toList()[index]
                    val (label, color) = pair
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = androidx.compose.ui.Modifier
                                .size(24.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(color)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(12.dp))
                        androidx.compose.material3.Text(
                            text = label,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("বন্ধ করুন", color = com.example.ui.theme.PrimaryGreen)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToJuz: (Int) -> Unit,
    onNavigateToNormalMode: () -> Unit,
    onNavigateToReadingMode: (Int) -> Unit,
    onNavigateToHafeziMode: (Int) -> Unit,
    onNavigateToSearch: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToMushaf: () -> Unit,
    onNavigateToMushafPoriciti: () -> Unit,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit,
    onNavigateToSurahWithAyah: (Int, String, Int) -> Unit,
    onNavigateToTajweedIndex: () -> Unit,
    onNavigateToTajweedMode: (Int) -> Unit,
    onNavigateToPlayer: () -> Unit,
    onNavigateToPosts: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    postsViewModel: com.example.ui.viewmodels.PostsViewModel? = null
) {
    val context = LocalContext.current
    val rawBlogPosts by (postsViewModel?.rawBlogPosts ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
    var notifReadIds by remember { mutableStateOf(com.example.utils.NotificationStateHelper.getReadIds(context)) }
    var notifHiddenIds by remember { mutableStateOf(com.example.utils.NotificationStateHelper.getHiddenIds(context)) }
    var localNotifsList by remember { mutableStateOf(emptyList<com.example.data.model.BlogPost>()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var backPressedOnce by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler {
        if (backPressedOnce) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedOnce = true
            android.widget.Toast.makeText(context, "অ্যাপ থেকে বের হতে আবার ব্যাক প্রেস করুন", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            kotlinx.coroutines.delay(2000L)
            backPressedOnce = false
        }
    }

    LaunchedEffect(Unit) {
        val db = com.example.data.local.NotificationDatabase.getDatabase(context)
        db.localNotificationDao().getAllNotifications().collect { entities ->
            localNotifsList = entities.map { entity ->
                com.example.data.model.BlogPost(
                    id = "local_${entity.id}",
                    title = entity.title,
                    content = entity.content,
                    category = entity.category,
                    author = entity.author,
                    timestamp = entity.timestamp
                )
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                notifReadIds = com.example.utils.NotificationStateHelper.getReadIds(context)
                notifHiddenIds = com.example.utils.NotificationStateHelper.getHiddenIds(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val appFirstInstallTime = remember(context) { com.example.utils.NotificationStateHelper.getAppFirstInstallTime(context) }

    val unreadNotificationCount = remember(rawBlogPosts, localNotifsList, notifReadIds, notifHiddenIds, appFirstInstallTime) {
        val firestoreNotifs = rawBlogPosts.filter { 
            (it.category == "নোটিফিকেশন" || it.category == "নোটিশ") && 
            !notifHiddenIds.contains(it.id.ifBlank { (it.title + it.content).hashCode().toString() }) &&
            (it.timestamp >= appFirstInstallTime)
        }
        val localNotifs = localNotifsList.filter { 
            !notifHiddenIds.contains(it.id) &&
            (it.timestamp >= appFirstInstallTime)
        }
        val allNotifs = firestoreNotifs + localNotifs
        allNotifs.count { post ->
            val nId = post.id.ifBlank { (post.title + post.content).hashCode().toString() }
            !notifReadIds.contains(nId)
        }
    }
    val lastReadSurah by viewModel.lastReadSurah.collectAsState()
    val lastReadPage by viewModel.lastReadPage.collectAsState()
    val lastReadMode by viewModel.lastReadMode.collectAsState()
    val lastReadMushafId by viewModel.lastReadMushafId.collectAsState()
    val lastReadMushafPage by viewModel.lastReadMushafPage.collectAsState()
    val lastReadAyah by viewModel.lastReadAyah.collectAsState()
    val defaultMushafId by viewModel.defaultMushafId.collectAsState()
    val hijriOffset by viewModel.hijriOffset.collectAsState()
    val combinedHijriOffset by viewModel.combinedHijriOffset.collectAsState()
    val surahList by viewModel.surahs.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (currentTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemDark
    }
    // New premium bookmarks
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())

    val hasAskedDownloadPrompt by viewModel.hasAskedDownloadPrompt.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()
    var showTajweedLegend by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    val mushafDownloadStatus by viewModel.mushafDownloadStatus.collectAsState()
    var showMushafDownloadRequestDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showMushafDownloadProgressDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showHijriAdjustDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var selectedDuaForDetail by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<com.example.data.DuaItem?>(null) }

    val arabicFontName by viewModel.arabicFontName.collectAsState()

    // Dynamic Prayer Times Setup
    val prayerRepo = remember(context) { com.example.data.repository.PrayerTimesRepository.getInstance(context) }
    val prayerSchedule by prayerRepo.todaySchedule.collectAsState()
    val isHanafiAsr by prayerRepo.isHanafi.collectAsState()
    var showPrayerTimesDetailSheet by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    // Auto-refresh prayer timer every 30 seconds
    LaunchedEffect(Unit) {
        while (true) {
            prayerRepo.refreshSchedule()
            delay(30000)
        }
    }

    if (showPrayerTimesDetailSheet) {
        com.example.ui.components.PrayerTimesDetailSheet(
            schedule = prayerSchedule,
            isHanafi = isHanafiAsr,
            hijriOffset = combinedHijriOffset,
            onDistrictSelected = { prayerRepo.setDistrict(it) },
            onHanafiChanged = { prayerRepo.setHanafi(it) },
            onDismiss = { showPrayerTimesDetailSheet = false }
        )
    }

    if (selectedDuaForDetail != null) {
        DuaDetailDialog(
            dua = selectedDuaForDetail!!,
            arabicFontName = arabicFontName,
            onDismiss = { selectedDuaForDetail = null }
        )
    }

    androidx.compose.runtime.LaunchedEffect(mushafDownloadStatus) {
        val status = mushafDownloadStatus
        if (status != null && status.state is com.example.data.model.DownloadState.Downloaded) {
            showMushafDownloadProgressDialog = false
            viewModel.clearMushafDownloadStatus()
            onNavigateToMushafPage(defaultMushafId, 1, true)
        }
    }

    // Show first-time download prompt dialog
    if (showTajweedLegend) {
        TajweedLegendDialog(onDismiss = { showTajweedLegend = false })
    }



    // Show download error toast if any
    LaunchedEffect(downloadError) {
        downloadError?.let {
            Toast.makeText(context, "ডাউনলোড ত্রুটি: $it", Toast.LENGTH_LONG).show()
        }
    }

    if (showMushafDownloadRequestDialog) {
        val currentMushaf = viewModel.getMushafStyle(defaultMushafId) ?: com.example.data.model.MushafStyle(
            id = "imdadia_hafezi",
            name = "Imdadia Hafezi Quran",
            nameBengali = "ইমদাদিয়া হাফেজী কুরআন",
            description = "Imdadia 15-Line Hafezi Quran PDF",
            descriptionBengali = "ইমদাদিয়া ১৫-লাইন হাফেজী কুরআন (একক ফাইল, সম্পূর্ণ অফলাইন)",
            totalPages = 611,
            fileSizeMB = 30,
            thumbnailUrl = "",
            baseUrl = ""
        )
        AlertDialog(
            onDismissRequest = { showMushafDownloadRequestDialog = false },
            title = {
                Text(
                    text = "মুসহাফ ডাউনলোড প্রয়োজন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF10B981)
                )
            },
            text = {
                Text(
                    text = "আপনার নির্বাচিত মুসহাফটি (${currentMushaf.nameBengali}) এখনো ডাউনলোড করা হয়নি। পড়ার জন্য ফাইলটি ডাউনলোড করা প্রয়োজন।\n\nফাইল সাইজ: ~${currentMushaf.fileSizeMB} মেগাবাইট\n\nআপনি কি এখনই ডাউনলোড করতে চান?",
                    fontSize = 15.sp,
                    color = if (isDark) Color.LightGray else Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showMushafDownloadRequestDialog = false
                        showMushafDownloadProgressDialog = true
                        viewModel.downloadDefaultMushaf(defaultMushafId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981)
                    )
                ) {
                    Text("ডাউনলোড করুন", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMushafDownloadRequestDialog = false },
                    border = BorderStroke(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black)
                ) {
                    Text("বাতিল", color = if (isDark) Color.White else Color.Black)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showMushafDownloadProgressDialog) {
        val status = mushafDownloadStatus
        val currentMushaf = viewModel.getMushafStyle(defaultMushafId)
        val mushafName = currentMushaf?.nameBengali ?: "পিডিএফ মুসহাফ"
        AlertDialog(
            onDismissRequest = { /* Prevent dismiss by clicking outside */ },
            title = {
                Text(
                    text = if (status?.state is com.example.data.model.DownloadState.Failed) "ডাউনলোড ব্যর্থ হয়েছে" else "ডাউনলোড হচ্ছে...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (status?.state is com.example.data.model.DownloadState.Failed) Color.Red else Color(0xFF10B981)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (status?.state is com.example.data.model.DownloadState.Failed) {
                            "দুঃখিত, ডাউনলোড করার সময় সমস্যা হয়েছে। আপনার ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।"
                        } else {
                            "$mushafName ফাইলটি ডাউনলোড করা হচ্ছে। অনুগ্রহ করে অপেক্ষা করুন।"
                        },
                        fontSize = 14.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    if (status?.state !is com.example.data.model.DownloadState.Failed) {
                        val progress = status?.progress ?: 0
                        val downloaded = status?.downloadedPages ?: 0
                        val total = status?.totalPages ?: 604
                        
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            color = Color(0xFF10B981),
                            trackColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFE5E7EB),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "অগ্রগতি: $progress%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            if (currentMushaf?.isPdf == true) {
                                Text(
                                    text = "ডাউনলোড হচ্ছে...",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else {
                                Text(
                                    text = "$downloaded / $total পৃষ্ঠা",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (status?.state is com.example.data.model.DownloadState.Failed) {
                    Button(
                        onClick = {
                            viewModel.downloadDefaultMushaf(defaultMushafId)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        )
                    ) {
                        Text("আবার চেষ্টা করুন", color = Color.White)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelMushafDownload(defaultMushafId)
                        showMushafDownloadProgressDialog = false
                    }
                ) {
                    Text(
                        text = "বন্ধ করুন",
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(2.dp, RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("কুরআন রিডার", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryGreen, RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("BN", color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToNotifications) {
                        if (unreadNotificationCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = Color(0xFFE53935)) {
                                        Text(
                                            text = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "নোটিফিকেশন সেন্টার",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "নোটিফিকেশন সেন্টার",
                                tint = Color(0xFF10B981)
                            )
                        }
                    }
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDark) Icons.Filled.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDark) "Light Mode" else "Dark Mode",
                            tint = if (isDark) OrangeAccent else GrayText
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        floatingActionButton = {},
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val isTablet = maxWidth > 600.dp
            val horizontalPadding = if (isTablet) 32.dp else 0.dp

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, bottom = 48.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val lastReadSurahNameForHero = QuranData.surahNames.find { it.first == lastReadSurah }?.second?.first ?: "আল ফাতিহা"
                        
                        val actionTextForHero = when (lastReadMode) {
                            "HAFEZI" -> "সর্বশেষ পঠিত পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(lastReadPage)}"
                            "TAJWEED" -> "সর্বশেষ পঠিত পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(lastReadPage)} (সূরা $lastReadSurahNameForHero)"
                            "MUSHAF" -> "সর্বশেষ পঠিত পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(lastReadMushafPage)}"
                            "READING" -> "সর্বশেষ পঠিত রিডিং মোড"
                            else -> "সর্বশেষ পঠিত সূরা"
                        }
                        val subTextForHero = when (lastReadMode) {
                            "HAFEZI" -> "হাফেজী কুরআন (১৫ লাইন)"
                            "TAJWEED" -> "রঙিন তাজবীদ কুরআন"
                            "MUSHAF" -> viewModel.getMushafStyle(lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId)?.nameBengali ?: (lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId)
                            else -> lastReadSurahNameForHero
                        }
                        
                        HeroSection(
                            lastReadTitle = actionTextForHero,
                            lastReadSubtitle = subTextForHero,
                            hijriOffset = combinedHijriOffset,
                            prayerSchedule = prayerSchedule,
                            onResumeClick = {
                                when (lastReadMode) {
                                    "HAFEZI" -> onNavigateToHafeziMode(lastReadPage)
                                    "TAJWEED" -> onNavigateToTajweedMode(lastReadPage)
                                    "READING" -> onNavigateToReadingMode(lastReadSurah)
                                    "MUSHAF" -> onNavigateToMushafPage(lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId, lastReadMushafPage, false)
                                    "DETAIL" -> onNavigateToSurahWithAyah(lastReadSurah, "LIST", lastReadAyah)
                                    else -> onNavigateToSurahWithAyah(lastReadSurah, "LIST", lastReadAyah)
                                }
                            },
                            onHijriDateClick = { onNavigateToCalendar() },
                            onDuaClick = { selectedDuaForDetail = it },
                            onPrayerTimesClick = { showPrayerTimesDetailSheet = true }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .offset(y = 24.dp)
                        ) {
                            SearchSection(onNavigateToSearch)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    QuickSurahPills(
                        onNavigateToSurahWithAyah = onNavigateToSurahWithAyah
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    BookmarksAndLastReadSection(
                        lastReadSurah = lastReadSurah,
                        lastReadPage = lastReadPage,
                        lastReadMode = lastReadMode,
                        lastReadMushafId = lastReadMushafId,
                        lastReadMushafPage = lastReadMushafPage,
                        lastReadMushafName = viewModel.getMushafStyle(lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId)?.nameBengali ?: (lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId),
                        defaultMushafId = defaultMushafId,
                        bookmarks = bookmarks,
                        lastReadAyah = lastReadAyah,
                        onSurahClick = onNavigateToSurah,
                        onNavigateToHafeziMode = onNavigateToHafeziMode,
                        onNavigateToReadingMode = onNavigateToReadingMode,
                        onNavigateToTajweedMode = onNavigateToTajweedMode,
                        onNavigateToMushafPage = onNavigateToMushafPage,
                        onNavigateToSurahWithAyah = onNavigateToSurahWithAyah,
                        onDeleteBookmark = { viewModel.deleteBookmark(it) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    ModesGridSection(
                        isHafeziDownloaded = viewModel.isMushafDownloaded(defaultMushafId),
                        isDark = isDark,
                        onHafeziPdfClick = {
                            onNavigateToMushafPoriciti()
                        },
                        onTajweedClick = onNavigateToTajweedIndex,
                        onTranslationClick = onNavigateToNormalMode,
                        onPlayerClick = onNavigateToPlayer,
                        onPostsClick = onNavigateToPosts
                    )
                }
            }
        }
    }

    if (showHijriAdjustDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showHijriAdjustDialog = false }) {
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
                            .background(Color.White.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Info,
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
                        text = "বর্তমান তারিখ: ${com.example.utils.DateUtil.getTodayHijriDateStr(combinedHijriOffset)}",
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
                        androidx.compose.material3.IconButton(
                            onClick = { if (hijriOffset > -5) viewModel.updateHijriOffset(hijriOffset - 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        val offsetDisplay = if (combinedHijriOffset > 0) "+${com.example.utils.DateUtil.toBengaliNumerals(combinedHijriOffset)}"
                                           else if (combinedHijriOffset < 0) "-${com.example.utils.DateUtil.toBengaliNumerals(-combinedHijriOffset)}"
                                           else com.example.utils.DateUtil.toBengaliNumerals(0)

                        Text(
                            text = "$offsetDisplay দিন",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        androidx.compose.material3.IconButton(
                            onClick = { if (hijriOffset < 5) viewModel.updateHijriOffset(hijriOffset + 1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                contentDescription = "Increase",
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
                                .clickable { showHijriAdjustDialog = false }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroSection(
    lastReadTitle: String = "",
    lastReadSubtitle: String = "",
    hijriOffset: Int,
    prayerSchedule: com.example.data.model.DailyPrayerSchedule,
    onResumeClick: () -> Unit = {},
    onHijriDateClick: () -> Unit = {},
    onDuaClick: (com.example.data.DuaItem) -> Unit = {},
    onPrayerTimesClick: () -> Unit = {}
) {
    val totalSlides = 4
    val pagerState = rememberPagerState(pageCount = { totalSlides })
    
    // Auto-scroll loop: whenever user changes the page manually (or page scrolls), 
    // the coroutine restarts, resetting the 9-second countdown for the active slide.
    LaunchedEffect(pagerState.currentPage, pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress) {
            delay(9000)
            try {
                val nextPage = (pagerState.currentPage + 1) % totalSlides
                pagerState.animateScrollToPage(nextPage)
            } catch (e: Exception) {
                // Ignore layout/detachment crashes when navigating away
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0E382A), // Deep Islamic Pine/Forest Green
                        Color(0xFF1B5B45), // Rich Emerald Green
                        Color(0xFF124333)  // Deep Emerald Base
                    )
                )
            )
            .padding(top = 16.dp, bottom = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp)
        ) { page ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(158.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF185340), // Perfectly aligned Dark Emerald
                                    Color(0xFF247358)  // Vibrant Islamic Emerald
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.22f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                        when (page) {
                            0 -> {
                                // Slide 1: Prayer Times Dynamic Card
                                com.example.ui.components.PrayerTimesBannerSlide(
                                    schedule = prayerSchedule,
                                    onClick = onPrayerTimesClick,
                                    onLocationClick = onPrayerTimesClick
                                )
                            }
                            1 -> {
                                // Slide 2: Quick Info (Today's Date & Calendars)
                                val bengaliDate = com.example.utils.DateUtil.getTodayBengaliDateStr()
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { onHijriDateClick() }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 14.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CalendarMonth,
                                            contentDescription = null,
                                            tint = Color(0xFFFFD54F),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = com.example.utils.DateUtil.getTodayEnglishDateStr(),
                                            color = White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.22f), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "🌾 বাংলা ক্যালেন্ডার",
                                                color = White.copy(alpha = 0.82f),
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = bengaliDate.first,
                                                color = White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 17.sp
                                            )
                                            Text(
                                                text = bengaliDate.second,
                                                color = White.copy(alpha = 0.82f),
                                                fontSize = 10.5.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .width(1.dp)
                                                .height(34.dp)
                                                .background(Color.White.copy(alpha = 0.22f))
                                        )
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "🌙 হিজরি ক্যালেন্ডার",
                                                color = White.copy(alpha = 0.82f),
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = com.example.utils.DateUtil.getTodayHijriDateStr(hijriOffset),
                                                color = White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 17.sp
                                            )
                                            Text(
                                                text = com.example.utils.DateUtil.getHijriNoteStr(hijriOffset),
                                                color = White.copy(alpha = 0.82f),
                                                fontSize = 10.5.sp,
                                                lineHeight = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Slide 3: Dua of the day
                                val duaItem = com.example.data.DuaData.getDuaItemOfTheDay()
                                val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
                                val banglaNumber = duaItem.id.toString().map { char ->
                                    if (char.isDigit()) banglaDigits[char - '0'] else char
                                }.joinToString("")

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(vertical = 14.dp, horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "আজকের দোয়া",
                                            color = White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Text(
                                        text = "[$banglaNumber] ${duaItem.title}",
                                        color = White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onDuaClick(duaItem) }
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "বিস্তারিত পড়ুন",
                                            color = White.copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = null,
                                            tint = White.copy(alpha = 0.9f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            3 -> {
                                // Slide 4: Ayah of the day
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(100.dp))
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "আজকের আয়াত",
                                            color = White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = com.example.data.AyahData.getAyahOfTheDay(),
                                        color = White,
                                        fontSize = 14.5.sp,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Medium,
                                        lineHeight = 21.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(totalSlides) { iteration ->
                val isCurrent = pagerState.currentPage == iteration
                val color = if (isCurrent) Color.White else Color.White.copy(alpha = 0.38f)
                val width = if (isCurrent) 16.dp else 5.dp
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.5.dp)
                        .clip(if (isCurrent) RoundedCornerShape(100.dp) else CircleShape)
                        .background(color)
                        .size(width = width, height = 5.dp)
                )
            }
        }
    }
}

@Composable
fun SearchSection(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, androidx.compose.foundation.shape.CircleShape)
            .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = GrayText)
            Spacer(modifier = Modifier.width(12.dp))
            Text("সূরা, পারা বা আয়াত খুঁজুন...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
        }
    }
}

@Composable
fun QuickAccessSection(
    selectedTab: Int,
    lastReadSurah: Int,
    lastReadPage: Int,
    lastReadMode: String,
    lastReadMushafId: String?,
    lastReadMushafPage: Int,
    defaultMushafId: String,
    onTabSelected: (Int) -> Unit,
    onSurahClick: (Int) -> Unit,
    onNavigateToHafeziMode: (Int) -> Unit,
    onNavigateToReadingMode: (Int) -> Unit,
    onNavigateToTajweedMode: (Int) -> Unit,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit
) {
    val lastReadSurahName = QuranData.surahNames.find { it.first == lastReadSurah }?.second?.first ?: "আল ফাতিহা"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Last Read Card
        Box(
            modifier = Modifier
                .weight(1.1f)
                .shadow(2.dp, RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                .clickable {
                    when (lastReadMode) {
                        "HAFEZI" -> onNavigateToHafeziMode(lastReadPage)
                        "READING" -> onNavigateToReadingMode(lastReadSurah)
                        "TAJWEED" -> onNavigateToTajweedMode(lastReadPage)
                        "MUSHAF" -> {
                            val targetMushafId = lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId
                            onNavigateToMushafPage(targetMushafId, lastReadMushafPage, false)
                        }
                        else -> onSurahClick(lastReadSurah)
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    val subtitleText = when (lastReadMode) {
                        "HAFEZI" -> "হাফেজী: ${lastReadPage.toBengaliNumerals()}"
                        "TAJWEED" -> "তাজবীদ: ${lastReadPage.toBengaliNumerals()} • সূরা: $lastReadSurahName"
                        "MUSHAF" -> "মুসহাফ: ${lastReadMushafPage.toBengaliNumerals()}"
                        "READING" -> "রিডিং: $lastReadSurahName"
                        else -> "বিস্তারিত: $lastReadSurahName"
                    }
                    Text("সর্বশেষ পঠিত", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1)
                    Text(subtitleText, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, lineHeight = 12.sp)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
            }
        }

        // Toggle Buttons
        Box(
            modifier = Modifier
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(100.dp))
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                .padding(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(0) }
                        .background(
                            if (selectedTab == 0) PrimaryGreen else Color.Transparent, 
                            RoundedCornerShape(100.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = if (selectedTab == 0) White else GrayText, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("সূরা", color = if (selectedTab == 0) White else GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(1) }
                        .background(
                            if (selectedTab == 1) PrimaryGreen else Color.Transparent, 
                            RoundedCornerShape(100.dp)
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = if (selectedTab == 1) White else GrayText, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("পারা", color = if (selectedTab == 1) White else GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class AmaliSurah(
    val title: String,
    val subtitle: String,
    val surahId: Int,
    val startAyah: Int? = null,
    val dotColor: Color,
    val isActive: (Calendar) -> Boolean
)

@Composable
fun QuickSurahPills(
    onNavigateToSurahWithAyah: (Int, String, Int) -> Unit
) {
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000) // check every 10 seconds to keep dynamic cards live
            currentTime = Calendar.getInstance()
        }
    }
    
    val isDark = MaterialTheme.colorScheme.surface.let { (it.red + it.green + it.blue) < 1.5f }
    
    val amaliList = remember {
        listOf(
            AmaliSurah(
                title = "সূরা কাহফ",
                subtitle = "জুমার আমল",
                surahId = 18,
                dotColor = OrangeAccent,
                isActive = { cal ->
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    day == Calendar.FRIDAY && (hour in 5..12 || (hour == 13 && minute <= 30))
                }
            ),
            AmaliSurah(
                title = "আয়াতুল কুরসী",
                subtitle = "ফরজ সালাত পর",
                surahId = 2,
                startAyah = 255,
                dotColor = BlueDot,
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    val timeInMinutes = hour * 60 + minute
                    (timeInMinutes in 315..360) || // 5:15 - 6:00
                    (timeInMinutes in 810..855) || // 13:30 - 14:15
                    (timeInMinutes in 1005..1050) || // 16:45 - 17:30
                    (timeInMinutes in 1140..1185) || // 19:00 - 19:45
                    (timeInMinutes in 1230..1275)    // 20:30 - 21:15
                }
            ),
            AmaliSurah(
                title = "সূরা ইয়াসিন",
                subtitle = "ফজরের আমল",
                surahId = 36,
                dotColor = Color(0xFF8B5CF6),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    (hour == 5) || (hour == 6 && minute <= 30)
                }
            ),
            AmaliSurah(
                title = "সূরা আর-রহমান",
                subtitle = "আসর আমল",
                surahId = 55,
                dotColor = Color(0xFFF97316),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    (hour == 16 && minute >= 30) || (hour == 17)
                }
            ),
            AmaliSurah(
                title = "সূরা ওয়াক্বিয়া",
                subtitle = "মাগরিবের আমল",
                surahId = 56,
                dotColor = Color(0xFFEC4899),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    (hour == 18 && minute >= 30) || (hour == 19)
                }
            ),
            AmaliSurah(
                title = "সূরা মুলক",
                subtitle = "ঘুমানোর আমল",
                surahId = 67,
                dotColor = GreenDot,
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hour >= 20 || hour < 4
                }
            ),
            AmaliSurah(
                title = "সূরা দুখান",
                subtitle = "বৃহস্পতিবার রাত",
                surahId = 44,
                dotColor = Color(0xFF06B6D4),
                isActive = { cal ->
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    (day == Calendar.THURSDAY && hour >= 18 && (hour > 18 || cal.get(Calendar.MINUTE) >= 30)) ||
                    (day == Calendar.FRIDAY && hour < 4)
                }
            ),
            AmaliSurah(
                title = "বাকারার শেষ ২ আয়াত",
                subtitle = "রাতের আমল",
                surahId = 2,
                startAyah = 285,
                dotColor = Color(0xFF14B8A6),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hour in 19..21
                }
            )
        )
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "amal_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val sortedAmaliList = remember(currentTime) {
        amaliList.sortedByDescending { it.isActive(currentTime) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        sortedAmaliList.forEach { item ->
            val isActive = item.isActive(currentTime)
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .shadow(if (isActive) 4.dp else 2.dp, RoundedCornerShape(100.dp))
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(100.dp)
                    )
                    .then(
                        if (isActive) {
                            Modifier.border(
                                width = 1.5.dp,
                                color = item.dotColor.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(100.dp)
                            )
                        } else {
                            Modifier.border(
                                width = 1.dp,
                                color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(100.dp)
                            )
                        }
                    )
                    .clickable {
                        onNavigateToSurahWithAyah(item.surahId, "MUSHAF", item.startAyah ?: 1)
                    }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .size((12 * pulseScale).dp)
                                    .background(item.dotColor.copy(alpha = pulseAlpha), RoundedCornerShape(50))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(item.dotColor, RoundedCornerShape(50))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(verticalArrangement = Arrangement.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.subtitle,
                                color = if (isActive) item.dotColor else GrayText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 10.sp
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(item.dotColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "চলমান",
                                        color = item.dotColor,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 8.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = item.title,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SurahGridSection(surahList: List<com.example.data.model.Surah>, onSurahClick: (Int) -> Unit) {
    val dummySurahs = QuranData.surahNames
    
    BoxWithConstraints(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        val columns = maxOf(2, (maxWidth / 160.dp).toInt())
        val itemWidth = (maxWidth - (12.dp * (columns - 1))) / columns

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dummySurahs.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { surahPair ->
                        val surahId = surahPair.first
                        val apiSurah = surahList.find { it.number == surahId }
                        val ayahCount = apiSurah?.numberOfAyahs ?: when(surahId) {
                            1 -> 7
                            2 -> 286
                            3 -> 200
                            4 -> 176
                            5 -> 120
                            6 -> 165
                            7 -> 206
                            8 -> 75
                            9 -> 129
                            10 -> 109
                            11 -> 123
                            12 -> 111
                            13 -> 43
                            14 -> 52
                            15 -> 99
                            16 -> 128
                            17 -> 111
                            18 -> 110
                            19 -> 98
                            20 -> 135
                            21 -> 112
                            22 -> 78
                            23 -> 118
                            24 -> 64
                            25 -> 77
                            26 -> 227
                            27 -> 93
                            28 -> 88
                            29 -> 69
                            30 -> 60
                            31 -> 34
                            32 -> 30
                            33 -> 73
                            34 -> 54
                            35 -> 45
                            36 -> 83
                            37 -> 182
                            38 -> 88
                            39 -> 75
                            40 -> 85
                            41 -> 54
                            42 -> 53
                            43 -> 89
                            44 -> 59
                            45 -> 37
                            46 -> 35
                            47 -> 38
                            48 -> 29
                            49 -> 18
                            50 -> 45
                            51 -> 60
                            52 -> 49
                            53 -> 62
                            54 -> 55
                            55 -> 78
                            56 -> 96
                            57 -> 29
                            58 -> 22
                            59 -> 24
                            60 -> 13
                            61 -> 14
                            62 -> 11
                            63 -> 11
                            64 -> 18
                            65 -> 12
                            66 -> 12
                            67 -> 30
                            68 -> 52
                            69 -> 52
                            70 -> 44
                            71 -> 28
                            72 -> 28
                            73 -> 20
                            74 -> 56
                            75 -> 40
                            76 -> 31
                            77 -> 50
                            78 -> 40
                            79 -> 46
                            80 -> 42
                            81 -> 29
                            82 -> 19
                            83 -> 36
                            84 -> 25
                            85 -> 22
                            86 -> 17
                            87 -> 19
                            88 -> 26
                            89 -> 30
                            90 -> 20
                            91 -> 15
                            92 -> 21
                            93 -> 11
                            94 -> 8
                            95 -> 8
                            96 -> 19
                            97 -> 5
                            98 -> 8
                            99 -> 8
                            100 -> 11
                            101 -> 11
                            102 -> 8
                            103 -> 3
                            104 -> 9
                            105 -> 5
                            106 -> 4
                            107 -> 7
                            108 -> 3
                            109 -> 6
                            110 -> 3
                            111 -> 5
                            112 -> 4
                            113 -> 5
                            114 -> 6
                            else -> 7
                        }
                        
                        val rawType = apiSurah?.revelationType
                        val revelationType = if (rawType != null) {
                            if (rawType.equals("Meccan", ignoreCase = true)) "মাক্কী" else "মাদানী"
                        } else {
                            com.example.data.QuranData.getSurahType(surahId)
                        }

                        SurahCard(
                            modifier = Modifier.width(itemWidth),
                            number = surahId.toString(), 
                            title = surahPair.second.first, 
                            translation = surahPair.second.second, 
                            ayahCount = ayahCount,
                            revelationType = revelationType,
                            onClick = { onSurahClick(surahId) }
                        )
                    }
                    val emptySpots = columns - rowItems.size
                    repeat(emptySpots) {
                        Spacer(modifier = Modifier.width(itemWidth))
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun ParaGridSection(onParaClick: (Int) -> Unit) {
    val dummyParas = (1..30).toList()
    
    BoxWithConstraints(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        val columns = maxOf(2, (maxWidth / 160.dp).toInt())
        val itemWidth = (maxWidth - (12.dp * (columns - 1))) / columns

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            dummyParas.chunked(columns).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowItems.forEach { para ->
                        SurahCard( // Reusing SurahCard for Para
                            modifier = Modifier.width(itemWidth),
                            number = para.toString(), 
                            title = "পারা $para", 
                            translation = "Juz $para", 
                            onClick = { onParaClick(para) }
                        )
                    }
                    val emptySpots = columns - rowItems.size
                    repeat(emptySpots) {
                        Spacer(modifier = Modifier.width(itemWidth))
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(32.dp))
}

@Composable
fun SurahCard(
    modifier: Modifier = Modifier, 
    number: String, 
    title: String, 
    translation: String, 
    ayahCount: Int? = null,
    revelationType: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                    )
                    Text(number, color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        title, 
                        color = MaterialTheme.colorScheme.onSurface, 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 14.sp, 
                        maxLines = 1, 
                        lineHeight = 14.sp
                    )
                    Text(
                        translation, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, 
                        fontSize = 10.sp, 
                        maxLines = 1, 
                        lineHeight = 10.sp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }
            }
            if (ayahCount != null && revelationType != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.MenuBook, contentDescription = null, tint = GrayText, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${ayahCount.toBengaliNumerals()} আয়াত", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(3.dp).background(GrayText, RoundedCornerShape(50)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(revelationType, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                }
            }
        }
    }
}

private val paraNamesBangla = listOf(
    "আলিফ লাম মীম", "সাইয়াকুল", "তিলকাল রুসুল", "লান তানালু", "ওয়াল মুহসানাত",
    "লা ইউহিব্বুল্লাহ", "ওয়া ইজা সামিউ", "ওয়া লাও আন্নানা", "ক্বলাল মালাইউ", "ওয়া'লামু",
    "ইয়া'তাজিরুন", "ওয়া মা মিন দাব্বাহ", "ওয়া মা উবাররিউ", "রুবামা", "সুবহানাল্লাজি",
    "ক্বলা আলাম", "ইক্বতারা বা লিন্নাস", "ক্বদ আফলাহা", "ওয়া ক্বলাল্লাজিনা", "আম্মান খালাক্ব",
    "উতলু মা উহিয়া", "ওয়া মান ইয়াক্বনুত", "ওয়া মালিয়া", "ফামান আজলামু", "ইলাইহি ইয়ুরাদদু",
    "হা মীম", "ক্বলা ফামা খাতবুকুম", "ক্বদ সামিয়াল্লাহ", "তাবারাকাল্লাজি", "আম্মা ইয়াতাসায়ালুন"
)

private fun getJuzStartPage(juz: Int): Int {
    return com.example.data.HafeziQuranData.getParaStartPage(juz, 1)
}

private fun getJuzStartSurah(juz: Int): Int {
    return when (juz) {
        1 -> 1
        2 -> 2
        3 -> 2
        4 -> 3
        5 -> 4
        6 -> 4
        7 -> 5
        8 -> 6
        9 -> 7
        10 -> 8
        11 -> 9
        12 -> 11
        13 -> 12
        14 -> 15
        15 -> 17
        16 -> 18
        17 -> 21
        18 -> 23
        19 -> 25
        20 -> 27
        21 -> 29
        22 -> 33
        23 -> 36
        24 -> 39
        25 -> 41
        26 -> 46
        27 -> 51
        28 -> 58
        29 -> 67
        30 -> 78
        else -> 1
    }
}

@Composable
fun BookmarksAndLastReadSection(
    lastReadSurah: Int,
    lastReadPage: Int,
    lastReadMode: String,
    lastReadMushafId: String?,
    lastReadMushafPage: Int,
    lastReadMushafName: String,
    defaultMushafId: String,
    bookmarks: List<com.example.data.local.entity.BookmarkEntity>,
    lastReadAyah: Int = 1,
    onSurahClick: (Int) -> Unit,
    onNavigateToHafeziMode: (Int) -> Unit,
    onNavigateToReadingMode: (Int) -> Unit,
    onNavigateToTajweedMode: (Int) -> Unit,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit,
    onNavigateToSurahWithAyah: (Int, String, Int) -> Unit,
    onDeleteBookmark: (com.example.data.local.entity.BookmarkEntity) -> Unit
) {
    val lastReadSurahName = QuranData.surahNames.find { it.first == lastReadSurah }?.second?.first ?: "আল ফাতিহা"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Last Read Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        when (lastReadMode) {
                            "HAFEZI" -> onNavigateToHafeziMode(lastReadPage)
                            "READING" -> onNavigateToReadingMode(lastReadSurah)
                            "TAJWEED" -> onNavigateToTajweedMode(lastReadPage)
                            "MUSHAF" -> {
                                val targetMushafId = lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId
                                onNavigateToMushafPage(targetMushafId, lastReadMushafPage, false)
                            }
                            "DETAIL" -> onNavigateToSurahWithAyah(lastReadSurah, "LIST", lastReadAyah)
                            else -> onNavigateToSurahWithAyah(lastReadSurah, "LIST", lastReadAyah)
                        }
                    }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val cardTitleText = when (lastReadMode) {
                        "HAFEZI", "TAJWEED", "MUSHAF" -> "সর্বশেষ পঠিত মুসহাফ"
                        "READING" -> "সর্বশেষ পঠিত রিডিং মোড"
                        else -> "সর্বশেষ পঠিত সূরা"
                    }
                    val cardMainText = when (lastReadMode) {
                        "HAFEZI" -> "হাফেজী কুরআন (১৫ লাইন)"
                        "TAJWEED" -> "রঙিন তাজবীদ কুরআন"
                        "MUSHAF" -> lastReadMushafName
                        else -> lastReadSurahName
                    }
                    val cardSubtitleText = when (lastReadMode) {
                        "HAFEZI" -> "পৃষ্ঠা: ${lastReadPage.toBengaliNumerals()}"
                        "TAJWEED" -> "পৃষ্ঠা: ${lastReadPage.toBengaliNumerals()} • সূরা: $lastReadSurahName"
                        "MUSHAF" -> "পৃষ্ঠা: ${lastReadMushafPage.toBengaliNumerals()}"
                        "READING" -> "সূরা: $lastReadSurahName"
                        else -> "সর্বশেষ বিস্তারিত: $lastReadSurahName"
                    }
                    Text(
                        text = cardTitleText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = cardMainText,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = cardSubtitleText,
                        fontSize = 11.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = GrayText
                )
            }
        }

        if (bookmarks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "বুকমার্ক সমূহ",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(bookmarks) { bookmark ->
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                        modifier = Modifier.clickable {
                            when (bookmark.type) {
                                "SURAH" -> onSurahClick(bookmark.referenceId)
                                "MUSHAF_PAGE" -> {
                                    val targetMushaf = bookmark.mushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId
                                    onNavigateToMushafPage(targetMushaf, bookmark.referenceId, false)
                                }
                                "PAGE" -> onNavigateToHafeziMode(bookmark.referenceId)
                                "JUZ" -> {
                                    val startPage = getJuzStartPage(bookmark.referenceId)
                                    onNavigateToHafeziMode(startPage)
                                }
                                "AYAH" -> {
                                    val (surahNum, ayahNum) = com.example.data.QuranData.getSurahAndAyahFromGlobal(bookmark.referenceId)
                                    onNavigateToSurahWithAyah(surahNum, "LIST", ayahNum)
                                }
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = bookmark.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onDeleteBookmark(bookmark) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModesGridSection(
    isHafeziDownloaded: Boolean,
    isDark: Boolean,
    onHafeziPdfClick: () -> Unit,
    onTajweedClick: () -> Unit,
    onTranslationClick: () -> Unit,
    onPlayerClick: () -> Unit,
    onPostsClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "কুরআন পঠন ও শ্রবণ মোড",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeItemCard(
                title = "হাফেজী মোড",
                subtitle = "১৫ লাইন পিডিএফ",
                icon = Icons.Default.PictureAsPdf,
                containerColor = Color(0xFFECFDF5),
                iconColor = Color(0xFF10B981),
                onClick = onHafeziPdfClick,
                badgeText = if (isHafeziDownloaded) "অফলাইন" else "অনলাইন",
                badgeIcon = if (isHafeziDownloaded) Icons.Default.CloudDone else Icons.Default.CloudDownload,
                badgeBgColor = if (isHafeziDownloaded) {
                    if (isDark) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFD1FAE5)
                } else {
                    if (isDark) Color(0xFF0EA5E9).copy(alpha = 0.15f) else Color(0xFFE0F2FE)
                },
                badgeBorderColor = if (isHafeziDownloaded) {
                    Color(0xFF10B981).copy(alpha = 0.4f)
                } else {
                    Color(0xFF38BDF8).copy(alpha = 0.4f)
                },
                badgeContentColor = if (isHafeziDownloaded) {
                    if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                } else {
                    if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1)
                },
                modifier = Modifier.weight(1f)
            )
            ModeItemCard(
                title = "তাজবীদ মোড",
                subtitle = "রঙিন তাজবীদ টেক্সট",
                icon = Icons.Default.Palette,
                containerColor = Color(0xFFEFF6FF),
                iconColor = Color(0xFF3B82F6),
                onClick = onTajweedClick,
                badgeText = "আংশিক অফলাইন",
                badgeIcon = Icons.Default.Cloud,
                badgeBgColor = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFFFEF3C7),
                badgeBorderColor = Color(0xFFFBBF24).copy(alpha = 0.4f),
                badgeContentColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeItemCard(
                title = "অনুবাদ ও তাফসীর",
                subtitle = "গভীর তাফসীর ও অর্থ",
                icon = Icons.Default.MenuBook,
                containerColor = Color(0xFFF5F3FF),
                iconColor = Color(0xFF8B5CF6),
                onClick = onTranslationClick,
                badgeText = "আংশিক অফলাইন",
                badgeIcon = Icons.Default.Cloud,
                badgeBgColor = if (isDark) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFFFEF3C7),
                badgeBorderColor = Color(0xFFFBBF24).copy(alpha = 0.4f),
                badgeContentColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                modifier = Modifier.weight(1f)
            )
            ModeItemCard(
                title = "তেলাওয়াত প্লেয়ার",
                subtitle = "ক্বারী সহ তেলাওয়াত",
                icon = Icons.Default.PlayCircle,
                containerColor = Color(0xFFFFF7ED),
                iconColor = Color(0xFFF97316),
                onClick = onPlayerClick,
                badgeText = "অনলাইন",
                badgeIcon = Icons.Default.Cloud,
                badgeBgColor = if (isDark) Color(0xFF0EA5E9).copy(alpha = 0.15f) else Color(0xFFE0F2FE),
                badgeBorderColor = Color(0xFF38BDF8).copy(alpha = 0.4f),
                badgeContentColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0369A1),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ModeItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    badgeIcon: ImageVector? = null,
    badgeBgColor: Color = Color.Transparent,
    badgeBorderColor: Color = Color.Transparent,
    badgeContentColor: Color = Color.Transparent
) {
    Card(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = iconColor.copy(alpha = 0.5f))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            if (badgeIcon != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 10.dp, start = 10.dp)
                        .background(badgeBgColor, CircleShape)
                        .border(0.5.dp, badgeBorderColor, CircleShape)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon,
                        contentDescription = badgeText,
                        tint = badgeContentColor,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp, bottom = 18.dp, start = 12.dp, end = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(containerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}



@Composable
fun QariSelectorDialog(
    selectedQariId: String,
    onDismiss: () -> Unit,
    onSelectQari: (String) -> Unit
) {
    val selectionItems = com.example.util.QariData.list.map { item ->
        com.example.ui.components.SelectionItem(
            id = item.id,
            title = item.nameEnglish,
            subtitle = item.nameBengali,
            icon = Icons.Default.RecordVoiceOver
        )
    }

    com.example.ui.components.SmartSelectionDialog(
        title = "ক্বারী নির্বাচন করুন",
        subtitle = "আপনার পছন্দের তেলাওয়াতকারী বেছে নিন",
        headerIcon = Icons.Default.RecordVoiceOver,
        items = selectionItems,
        selectedId = selectedQariId,
        onSelectItem = onSelectQari,
        onDismiss = onDismiss,
        showSearch = true,
        searchPlaceholder = "ক্বারী খুঁজুন..."
    )
}

@Composable
fun SurahSelectorDialog(
    onDismiss: () -> Unit,
    onSelectSurah: (Int) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSurahs = remember(searchQuery) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isEmpty()) {
            QuranData.surahNames
        } else {
            QuranData.surahNames.filter { surah ->
                val surahId = surah.first
                val bengaliName = surah.second.first
                val meaning = surah.second.second

                bengaliName.contains(trimmedQuery, ignoreCase = true) ||
                meaning.contains(trimmedQuery, ignoreCase = true) ||
                surahId.toString().contains(trimmedQuery) ||
                surahId.toBengaliNumerals().contains(trimmedQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "সূরা নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("সূরা খুঁজুন...", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    singleLine = true
                )
            }
        },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                if (filteredSurahs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("কোনো সূরা পাওয়া যায়নি!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSurahs) { surahPair ->
                            val surahId = surahPair.first
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectSurah(surahId) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = surahId.toBengaliNumerals(),
                                            color = PrimaryGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = surahPair.second.first,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = surahPair.second.second,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun RecitationPlayerPanel(
    currentPlayingSurah: Int?,
    currentPlayingAyahIndex: Int,
    currentPlayingAyahs: List<CombinedAyah>,
    isPlaying: Boolean,
    selectedQariId: String,
    isRepeatAyahEnabled: Boolean,
    isRepeatSurahEnabled: Boolean,
    playbackSpeed: Float,
    onQariClick: () -> Unit,
    onSurahSelectorClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onToggleRepeatAyah: () -> Unit,
    onToggleRepeatSurah: () -> Unit,
    onSpeedClick: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "সূরা তেলাওয়াত প্লেয়ার",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (currentPlayingSurah != null) {
                    val surahName = QuranData.surahNames.find { it.first == currentPlayingSurah }?.second?.first ?: "সূরা"
                    val qariName = com.example.util.QariData.getQariNameEnglish(selectedQariId)
                    val totalAyahs = currentPlayingAyahs.size
                    val progress = if (totalAyahs > 0) (currentPlayingAyahIndex.toFloat() / totalAyahs.toFloat()) else 0f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সূরা $surahName",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ক্বারী: $qariName",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "আয়াত: ${(currentPlayingAyahIndex + 1).toBengaliNumerals()} / ${totalAyahs.toBengaliNumerals()}",
                                fontSize = 11.sp,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Currently Playing Ayah with Arabic and Bangla translation
                    val currentAyahObj = currentPlayingAyahs.getOrNull(currentPlayingAyahIndex)
                    if (currentAyahObj != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .border(1.dp, PrimaryGreen.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentAyahObj.arabicText,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentAyahObj.bengaliText,
                                fontSize = 11.sp,
                                fontFamily = com.example.ui.theme.LocalBengaliFont.current,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(100.dp)),
                        color = PrimaryGreen,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onPrevClick) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        FilledIconButton(
                            onClick = onPlayPauseClick,
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = PrimaryGreen),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        IconButton(onClick = onNextClick) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(20.dp))
                        IconButton(onClick = onStopClick) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.Red, modifier = Modifier.size(28.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Speed & Loop control buttons row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed Cycle toggle
                        val nextSpeed = when(playbackSpeed) {
                            0.75f -> 1.0f
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 1.75f
                            1.75f -> 2.0f
                            else -> 0.75f
                        }
                        Button(
                            onClick = { onSpeedClick(nextSpeed) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${playbackSpeed}x", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Ayah repeat
                        Button(
                            onClick = onToggleRepeatAyah,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRepeatAyahEnabled) PrimaryGreen else MaterialTheme.colorScheme.primaryContainer
                            ),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RepeatOne,
                                contentDescription = null,
                                tint = if (isRepeatAyahEnabled) White else PrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("আয়াত লুপ", color = if (isRepeatAyahEnabled) White else PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        // Surah repeat
                        Button(
                            onClick = onToggleRepeatSurah,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isRepeatSurahEnabled) PrimaryGreen else MaterialTheme.colorScheme.primaryContainer
                            ),
                            border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (isRepeatSurahEnabled) White else PrimaryGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("সূরা লুপ", color = if (isRepeatSurahEnabled) White else PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    // Placeholder / Set Selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "কোনো তেলাওয়াত সচল নেই",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ক্বারী ও সূরা নির্বাচন করে তেলাওয়াত উপভোগ করুন",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onQariClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.2f)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ক্বারী নির্বাচন", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                            
                            Button(
                                onClick = onSurahSelectorClick,
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("সূরা চালু করুন", color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
fun FloatingPlayerShortcut(
    viewModel: HomeViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayingSurah by viewModel.currentPlayingSurah.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    if (currentPlayingSurah != null) {
        val surahNamePair = QuranData.surahNames.find { it.first == currentPlayingSurah }
        val bengaliName = surahNamePair?.second?.first ?: "সূরা"

        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        var parentWidth by remember { mutableStateOf(0) }
        var parentHeight by remember { mutableStateOf(0) }
        var cardWidth by remember { mutableStateOf(0) }
        var cardHeight by remember { mutableStateOf(0) }

        val density = LocalDensity.current
        val paddingPx = with(density) { 16.dp.toPx() }

        val minX = if (parentWidth > 0) -(parentWidth - cardWidth - paddingPx) else -Float.MAX_VALUE
        val maxX = if (parentWidth > 0) paddingPx else Float.MAX_VALUE
        val minY = if (parentHeight > 0) -(parentHeight - cardHeight - paddingPx) else -Float.MAX_VALUE
        val maxY = if (parentHeight > 0) paddingPx else Float.MAX_VALUE

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        Card(
            modifier = modifier
                .padding(end = 16.dp, bottom = 16.dp)
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(minX, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                    }
                }
                .onGloballyPositioned { coordinates ->
                    cardWidth = coordinates.size.width
                    cardHeight = coordinates.size.height
                    coordinates.parentLayoutCoordinates?.let { parentCoordinates ->
                        parentWidth = parentCoordinates.size.width
                        parentHeight = parentCoordinates.size.height
                    }
                }
                .size(width = 215.dp, height = 62.dp)
                .shadow(12.dp, RoundedCornerShape(30.dp))
                .clickable { onClick() },
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
            border = BorderStroke(1.5.dp, White.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Player Shortcut",
                        tint = White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "সূরা $bengaliName",
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isPlaying) "চলছে..." else "বন্ধ আছে",
                        color = White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(White)
                        .clickable {
                            if (isPlaying) {
                                viewModel.pauseSurahAudio()
                            } else {
                                viewModel.resumeSurahAudio()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(White.copy(alpha = 0.2f))
                        .clickable {
                            viewModel.stopSurahAudio()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Player",
                        tint = White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private @Composable
fun DuaActionButtonsRow(
    dua: com.example.data.DuaItem,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var showShareMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Copy Button
            androidx.compose.material3.OutlinedButton(
                onClick = { com.example.utils.DuaShareUtil.copyToClipboard(context, dua) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "কপি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // 2. Share Button (With Dropdown Menu)
            Box(
                modifier = Modifier.weight(1f)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showShareMenu = true },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Share,
                            contentDescription = "Share",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "শেয়ার",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                // Dropdown Menu for Image & Text Share Options
                androidx.compose.material3.DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Share,
                                    contentDescription = "Text Share",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "টেক্সট শেয়ার",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            showShareMenu = false
                            com.example.utils.DuaShareUtil.shareAsText(context, dua)
                        }
                    )
                    
                    androidx.compose.material3.DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Image,
                                    contentDescription = "Image Share",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ছবি শেয়ার",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            showShareMenu = false
                            com.example.utils.DuaShareUtil.shareAsImage(context, dua)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        // App Credit with Logo & Name: (logo) ❝কুরআন রিডার❞ অ্যাপ
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.ic_launcher),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "❝কুরআন রিডার❞ অ্যাপ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DuaDetailDialog(
    dua: com.example.data.DuaItem,
    arabicFontName: String,
    onDismiss: () -> Unit
) {
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    
    fun formatToBanglaNumber(num: Int): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return num.toString().map { char ->
            if (char.isDigit()) banglaDigits[char - '0'] else char
        }.joinToString("")
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .size(36.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 16.dp)
                    ) {
                        val formattedIndex = formatToBanglaNumber(dua.id)
                        Text(
                            text = "[$formattedIndex] ${dua.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 24.sp
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            dua.segments.forEachIndexed { index, segment ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                }
                                
                                // Arabic Text
                                if (segment.arabic.isNotEmpty() && segment.arabic != "null") {
                                    val cleanForBismillah = segment.arabic.replace(Regex("[\\s\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]"), "")
                                    val isBismillah = cleanForBismillah == "بسماللهالرحمنالرحيم" || cleanForBismillah == "بسمٱللهٱلرحمنٱلرحيم"
                                    
                                    androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                                        Text(
                                            text = segment.arabic,
                                            fontSize = 24.sp,
                                            fontFamily = arabicFont,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = if (isBismillah) TextAlign.Center else TextAlign.Right,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            lineHeight = 40.sp
                                        )
                                    }
                                }
                                
                                // Translation
                                if (segment.translation.isNotEmpty() && segment.translation != "null") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF00B4D8), RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "অর্থ:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00B4D8)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = segment.translation,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                                
                                // Transliteration
                                if (segment.transliteration.isNotEmpty() && segment.transliteration != "null") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min)
                                            .padding(vertical = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .fillMaxHeight()
                                                .background(Color(0xFF00B4D8).copy(alpha = 0.6f), RoundedCornerShape(2.dp))
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "উচ্চারণ:",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF00B4D8).copy(alpha = 0.8f)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = segment.transliteration,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                                
                                // Prekkhapot (Dua's context)
                                if (segment.bottom.isNotEmpty() && segment.bottom != "null") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        val trimmed = segment.bottom.trim()
                                        val contextText = if (trimmed.startsWith("দোয়ার প্রেক্ষাপট") || trimmed.startsWith("দোয়ার প্রেক্ষাপট")) {
                                            trimmed
                                        } else {
                                            "দোয়ার প্রেক্ষাপট: ${segment.bottom}"
                                        }
                                        Text(
                                            text = contextText,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                            lineHeight = 20.sp
                                        )
                                    }
                                }
                                
                                // Reference
                                if (segment.reference.isNotEmpty() && segment.reference != "null") {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = segment.reference,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Copy & Share Actions Row
                        DuaActionButtonsRow(dua = dua)
                    }
                }
            }
        }
    }
}
