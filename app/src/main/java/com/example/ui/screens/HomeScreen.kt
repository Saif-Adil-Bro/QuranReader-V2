package com.example.ui.screens

import com.example.R
import com.example.data.QuranData
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.launch
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodels.HomeViewModel
import com.example.data.repository.RecentReadTrack
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
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.BlogPost

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
                text = stringResource(R.string.tajweed_legend_title),
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
                androidx.compose.material3.Text(stringResource(R.string.action_close), color = com.example.ui.theme.PrimaryGreen)
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
    onNavigateToDhikrReminder: (com.example.utils.DhikrType) -> Unit = {},
    onNavigateToDua: (Int?) -> Unit = {},
    onNavigateToManzil: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToSubjectwise: (String?) -> Unit = {},
    onNavigateToQibla: () -> Unit = {},
    onNavigateToMosque: () -> Unit = {},
    onNavigateToVideoCreator: () -> Unit = {},
    postsViewModel: com.example.ui.viewmodels.PostsViewModel? = null
) {
    val context = LocalContext.current
    val rawBlogPosts by (postsViewModel?.rawBlogPosts ?: kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())
    var notifReadIds by remember { mutableStateOf(com.example.utils.NotificationStateHelper.getReadIds(context)) }
    var notifHiddenIds by remember { mutableStateOf(com.example.utils.NotificationStateHelper.getHiddenIds(context)) }
    var localNotifsList by remember { mutableStateOf(emptyList<com.example.data.model.BlogPost>()) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var backPressedOnce by remember { mutableStateOf(false) }

    val isScrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    androidx.activity.compose.BackHandler {
        if (isScrolled) {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
            backPressedOnce = false
        } else {
            if (backPressedOnce) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressedOnce = true
                android.widget.Toast.makeText(context, context.getString(R.string.exit_prompt), android.widget.Toast.LENGTH_SHORT).show()
            }
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
    val currentLanguage by viewModel.appLanguage.collectAsState()
    val isEnglish = currentLanguage == "en"
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (currentTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemDark
    }
    // New premium bookmarks
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val recentReads by viewModel.recentReads.collectAsState()

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
            isEnglish = isEnglish,
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
            Toast.makeText(context, context.getString(R.string.download_error_prefix, it), Toast.LENGTH_LONG).show()
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
                    text = stringResource(R.string.mushaf_download_required),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF10B981)
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.mushaf_download_desc, currentMushaf.nameBengali, currentMushaf.fileSizeMB),
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
                    Text(stringResource(R.string.action_download), color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showMushafDownloadRequestDialog = false },
                    border = BorderStroke(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black)
                ) {
                    Text(stringResource(R.string.action_cancel), color = if (isDark) Color.White else Color.Black)
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
                    text = if (status?.state is com.example.data.model.DownloadState.Failed) stringResource(R.string.mushaf_download_failed) else stringResource(R.string.mushaf_downloading_title),
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
                            stringResource(R.string.mushaf_download_error_detail)
                        } else {
                            stringResource(R.string.mushaf_downloading_wait_msg, mushafName)
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
                                text = stringResource(R.string.download_progress_pct, progress),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                            if (currentMushaf?.isPdf == true) {
                                Text(
                                    text = stringResource(R.string.mushaf_downloading_title),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.download_pages_count, downloaded, total),
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
                        Text(stringResource(R.string.retry), color = Color.White)
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
                        text = stringResource(R.string.action_close),
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
                        Text(if (isEnglish) "Quran Reader" else "কুরআন রিডার", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryGreen, RoundedCornerShape(12.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(if (isEnglish) "EN" else "BN", color = White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                    contentDescription = if (isEnglish) "Notification Center" else "নোটিফিকেশন সেন্টার",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = if (isEnglish) "Notification Center" else "নোটিফিকেশন সেন্টার",
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
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, bottom = 48.dp)
            ) {
                item {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val lastReadSurahNameForHero = if (isEnglish) {
                            QuranData.surahNames.find { it.first == lastReadSurah }?.second?.second ?: "Al-Fatihah"
                        } else {
                            QuranData.surahNames.find { it.first == lastReadSurah }?.second?.first ?: "আল ফাতিহা"
                        }
                        
                        val actionTextForHero = when (lastReadMode) {
                            "HAFEZI" -> if (isEnglish) "Last Read Page: $lastReadPage" else "সর্বশেষ পঠিত পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(lastReadPage)}"
                            "TAJWEED" -> if (isEnglish) "Last Read Page: $lastReadPage (Surah $lastReadSurahNameForHero)" else "সর্বশেষ পঠিত পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(lastReadPage)} (সূরা $lastReadSurahNameForHero)"
                            "MUSHAF" -> if (isEnglish) "Last Read Page: $lastReadMushafPage" else "সর্বশেষ পঠিত পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(lastReadMushafPage)}"
                            "READING" -> if (isEnglish) "Last Read in Reading Mode" else "সর্বশেষ পঠিত রিডিং মোড"
                            else -> if (isEnglish) "Last Read Surah" else "সর্বশেষ পঠিত সূরা"
                        }
                        val subTextForHero = when (lastReadMode) {
                            "HAFEZI" -> if (isEnglish) "Hafezi Quran (15 Lines)" else "হাফেজী কুরআন (১৫ লাইন)"
                            "TAJWEED" -> if (isEnglish) "Color Tajweed Quran" else "রঙিন তাজবীদ কুরআন"
                            "MUSHAF" -> {
                                val style = viewModel.getMushafStyle(lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId)
                                if (isEnglish) (style?.name ?: (lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId))
                                else (style?.nameBengali ?: (lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId))
                            }
                            else -> lastReadSurahNameForHero
                        }
                        
                        HeroSection(
                            lastReadTitle = actionTextForHero,
                            lastReadSubtitle = subTextForHero,
                            hijriOffset = combinedHijriOffset,
                            prayerSchedule = prayerSchedule,
                            isEnglish = isEnglish,
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
                            SearchSection(isEnglish = isEnglish, onClick = onNavigateToSearch)
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    QuickSurahPills(
                        isEnglish = isEnglish,
                        onNavigateToSurahWithAyah = onNavigateToSurahWithAyah
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    ModesGridSection(
                        isHafeziDownloaded = viewModel.isMushafDownloaded(defaultMushafId),
                        isDark = isDark,
                        isEnglish = isEnglish,
                        onHafeziPdfClick = {
                            onNavigateToMushafPoriciti()
                        },
                        onTajweedClick = onNavigateToTajweedIndex,
                        onTranslationClick = onNavigateToNormalMode,
                        onPlayerClick = onNavigateToPlayer,
                        onPostsClick = onNavigateToPosts
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    TopFeaturesGridSection(
                        isDark = isDark,
                        isEnglish = isEnglish,
                        onQiblaClick = onNavigateToQibla,
                        onMosqueClick = onNavigateToMosque,
                        onDuaClick = { onNavigateToDua(null) },
                        onManzilClick = onNavigateToManzil,
                        onPlannerClick = onNavigateToPlanner,
                        onCalendarClick = onNavigateToCalendar,
                        onVideoCreatorClick = onNavigateToVideoCreator,
                        onSubjectwiseClick = { onNavigateToSubjectwise(null) },
                        onTasbihClick = { onNavigateToDhikrReminder(com.example.utils.DhikrType.DUROOD) },
                        onMoreClick = onSettingsClick
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    NearbyMosqueHomeBanner(
                        isDark = isDark,
                        isEnglish = isEnglish,
                        onMosqueClick = onNavigateToMosque
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DailyDuaFeaturedSection(
                        isDark = isDark,
                        isEnglish = isEnglish,
                        arabicFontName = arabicFontName,
                        onReadDua = { duaItem -> selectedDuaForDetail = duaItem },
                        onViewAllDuas = { onNavigateToDua(null) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    DhikrHabitSection(
                        isDark = isDark,
                        isEnglish = isEnglish,
                        onNavigateToDhikrReminder = onNavigateToDhikrReminder
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SubjectwiseTopCategoriesSection(
                        isDark = isDark,
                        isEnglish = isEnglish,
                        onCategoryClick = { categoryName -> onNavigateToSubjectwise(categoryName) },
                        onViewAllClick = { onNavigateToSubjectwise(null) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    FeaturedIslamicMediaSection(
                        isDark = isDark,
                        isEnglish = isEnglish,
                        blogPosts = rawBlogPosts.filter { it.category != "নোটিফিকেশন" && it.category != "নোটিশ" },
                        onPostClick = { post ->
                            postsViewModel?.setPendingBlogPost(post)
                            onNavigateToPosts()
                        },
                        onViewAllClick = onNavigateToPosts
                    )
                }
                if (recentReads.isNotEmpty() || bookmarks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        BookmarksAndLastReadSection(
                            lastReadSurah = lastReadSurah,
                            lastReadPage = lastReadPage,
                            lastReadMode = lastReadMode,
                            lastReadMushafId = lastReadMushafId,
                            lastReadMushafPage = lastReadMushafPage,
                            lastReadMushafName = if (isEnglish) {
                                (viewModel.getMushafStyle(lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId)?.name ?: (lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId))
                            } else {
                                (viewModel.getMushafStyle(lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId)?.nameBengali ?: (lastReadMushafId?.takeIf { it.isNotEmpty() } ?: defaultMushafId))
                            },
                            defaultMushafId = defaultMushafId,
                            bookmarks = bookmarks,
                            lastReadAyah = lastReadAyah,
                            recentReads = recentReads,
                            isEnglish = isEnglish,
                            onSurahClick = onNavigateToSurah,
                            onNavigateToHafeziMode = onNavigateToHafeziMode,
                            onNavigateToReadingMode = onNavigateToReadingMode,
                            onNavigateToTajweedMode = onNavigateToTajweedMode,
                            onNavigateToMushafPage = onNavigateToMushafPage,
                            onNavigateToSurahWithAyah = onNavigateToSurahWithAyah,
                            onDeleteBookmark = { viewModel.deleteBookmark(it) }
                        )
                    }
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
                        text = if (isEnglish) "Hijri Date Adjustment" else "হিজরি তারিখ সমন্বয়",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (isEnglish) "You can adjust the Hijri calendar date forward or backward by one or more days." else "হিজরি তারিখ একদিন বা কয়েকদিন আগে-পিছে করতে পারেন।",
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
                            text = if (isEnglish) "According to Islamic rules, the next Hijri date begins right after sunset (~6:00 PM). Adjust using + / - below if needed." else "ইসলামী নিয়ম অনুযায়ী সূর্যাস্তের (~সন্ধ্যা ৬টা) পরেই পরবর্তী দিনের জন্য হিজরি তারিখ গণনা শুরু হয়। প্রয়োজনে নিচে + / - চেপে সমন্বয় করতে পারেন।",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val todayHijriStr = if (isEnglish) {
                        val hijri = com.example.utils.HijriCalendarUtil.getHijriDate(java.time.LocalDate.now(), combinedHijriOffset)
                        "${hijri.hijriDay} ${hijri.hijriMonthNameEn} ${hijri.hijriYear} AH"
                    } else {
                        com.example.utils.DateUtil.getTodayHijriDateStr(combinedHijriOffset)
                    }

                    Text(
                        text = if (isEnglish) "Current Date: $todayHijriStr" else "বর্তমান তারিখ: $todayHijriStr",
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

                        val offsetDisplay = if (isEnglish) {
                            if (combinedHijriOffset > 0) "+$combinedHijriOffset day(s)"
                            else if (combinedHijriOffset < 0) "$combinedHijriOffset day(s)"
                            else "0 day(s)"
                        } else {
                            if (combinedHijriOffset > 0) "+${com.example.utils.DateUtil.toBengaliNumerals(combinedHijriOffset)} দিন"
                            else if (combinedHijriOffset < 0) "-${com.example.utils.DateUtil.toBengaliNumerals(-combinedHijriOffset)} দিন"
                            else "${com.example.utils.DateUtil.toBengaliNumerals(0)} দিন"
                        }

                        Text(
                            text = offsetDisplay,
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
                            text = if (isEnglish) "Close" else "বন্ধ করুন",
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
    isEnglish: Boolean = false,
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
                    .height(160.dp),
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
                                val hijriInfo = com.example.utils.HijriCalendarUtil.getHijriDate(java.time.LocalDate.now(), hijriOffset)
                                val hijriDateStr = if (isEnglish) {
                                    "${hijriInfo.hijriDay} ${hijriInfo.hijriMonthNameEn} ${hijriInfo.hijriYear} AH"
                                } else {
                                    com.example.utils.DateUtil.getTodayHijriDateStr(hijriOffset)
                                }
                                val hijriNoteStr = if (isEnglish) {
                                    "Lunar Calendar"
                                } else {
                                    com.example.utils.DateUtil.getHijriNoteStr(hijriOffset)
                                }

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
                                                text = if (isEnglish) "🌾 Bangla Calendar" else "🌾 বাংলা ক্যালেন্ডার",
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
                                                text = if (isEnglish) "🌙 Hijri Calendar" else "🌙 হিজরি ক্যালেন্ডার",
                                                color = White.copy(alpha = 0.82f),
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = hijriDateStr,
                                                color = White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 17.sp
                                            )
                                            Text(
                                                text = hijriNoteStr,
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
                                val duaNumStr = if (isEnglish) duaItem.id.toString() else com.example.utils.DateUtil.toBengaliNumerals(duaItem.id)
                                val duaTitle = duaItem.title

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
                                            text = if (isEnglish) "Dua of the Day" else "আজকের দোয়া",
                                            color = White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Text(
                                        text = "[$duaNumStr] $duaTitle",
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
                                            text = if (isEnglish) "Read Details" else "বিস্তারিত পড়ুন",
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
                                            text = if (isEnglish) "Ayah of the Day" else "আজকের আয়াত",
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
fun SearchSection(isEnglish: Boolean = false, onClick: () -> Unit) {
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
            Text(if (isEnglish) "Search Surah, Juz or Ayah..." else "সূরা, পারা বা আয়াত খুঁজুন...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
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
    isEnglish: Boolean = false,
    onTabSelected: (Int) -> Unit,
    onSurahClick: (Int) -> Unit,
    onNavigateToHafeziMode: (Int) -> Unit,
    onNavigateToReadingMode: (Int) -> Unit,
    onNavigateToTajweedMode: (Int) -> Unit,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit
) {
    val lastReadSurahName = if (isEnglish) {
        QuranData.surahNames.find { it.first == lastReadSurah }?.second?.second ?: "Al-Fatihah"
    } else {
        QuranData.surahNames.find { it.first == lastReadSurah }?.second?.first ?: "আল ফাতিহা"
    }

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
                    val subtitleText = if (isEnglish) {
                        when (lastReadMode) {
                            "HAFEZI" -> "Hafezi: $lastReadPage"
                            "TAJWEED" -> "Tajweed: $lastReadPage • Surah: $lastReadSurahName"
                            "MUSHAF" -> "Mushaf: $lastReadMushafPage"
                            "READING" -> "Reading: $lastReadSurahName"
                            else -> "Detail: $lastReadSurahName"
                        }
                    } else {
                        when (lastReadMode) {
                            "HAFEZI" -> "হাফেজী: ${lastReadPage.toBengaliNumerals()}"
                            "TAJWEED" -> "তাজবীদ: ${lastReadPage.toBengaliNumerals()} • সূরা: $lastReadSurahName"
                            "MUSHAF" -> "মুসহাফ: ${lastReadMushafPage.toBengaliNumerals()}"
                            "READING" -> "রিডিং: $lastReadSurahName"
                            else -> "বিস্তারিত: $lastReadSurahName"
                        }
                    }
                    Text(if (isEnglish) "Last Read" else "সর্বশেষ পঠিত", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp, lineHeight = 10.sp, maxLines = 1)
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
                        Text(if (isEnglish) "Surah" else "সূরা", color = if (selectedTab == 0) White else GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        Text(if (isEnglish) "Juz" else "পারা", color = if (selectedTab == 1) White else GrayText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
    isEnglish: Boolean = false,
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
    
    val amaliList = remember(isEnglish) {
        listOf(
            AmaliSurah(
                title = if (isEnglish) "Surah Al-Kahf" else "সূরা কাহফ",
                subtitle = if (isEnglish) "Friday Deed" else "জুমার আমল",
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
                title = if (isEnglish) "Ayatul Kursi" else "আয়াতুল কুরসী",
                subtitle = if (isEnglish) "After Fard Prayer" else "ফরজ সালাত পর",
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
                title = if (isEnglish) "Surah Ya-Sin" else "সূরা ইয়াসিন",
                subtitle = if (isEnglish) "Fajr Deed" else "ফজরের আমল",
                surahId = 36,
                dotColor = Color(0xFF8B5CF6),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    (hour == 5) || (hour == 6 && minute <= 30)
                }
            ),
            AmaliSurah(
                title = if (isEnglish) "Surah Ar-Rahman" else "সূরা আর-রহমান",
                subtitle = if (isEnglish) "Asr Deed" else "আসর আমল",
                surahId = 55,
                dotColor = Color(0xFFF97316),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    (hour == 16 && minute >= 30) || (hour == 17)
                }
            ),
            AmaliSurah(
                title = if (isEnglish) "Surah Al-Waqi'ah" else "সূরা ওয়াক্বিয়া",
                subtitle = if (isEnglish) "Maghrib Deed" else "মাগরিবের আমল",
                surahId = 56,
                dotColor = Color(0xFFEC4899),
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    val minute = cal.get(Calendar.MINUTE)
                    (hour == 18 && minute >= 30) || (hour == 19)
                }
            ),
            AmaliSurah(
                title = if (isEnglish) "Surah Al-Mulk" else "সূরা মুলক",
                subtitle = if (isEnglish) "Before Sleep" else "ঘুমানোর আমল",
                surahId = 67,
                dotColor = GreenDot,
                isActive = { cal ->
                    val hour = cal.get(Calendar.HOUR_OF_DAY)
                    hour >= 20 || hour < 4
                }
            ),
            AmaliSurah(
                title = if (isEnglish) "Surah Ad-Dukhan" else "সূরা দুখান",
                subtitle = if (isEnglish) "Thursday Night" else "বৃহস্পতিবার রাত",
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
                title = if (isEnglish) "Last 2 Verses of Baqarah" else "বাকারার শেষ ২ আয়াত",
                subtitle = if (isEnglish) "Night Deed" else "রাতের আমল",
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

    val sortedAmaliList = remember(currentTime, amaliList) {
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
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "ACTIVE" else "চলমান",
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
                    Image(
                        painter = painterResource(if (revelationType.contains("মাদানী")) R.drawable.annawabu else R.drawable.kaaba),
                        contentDescription = revelationType,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
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
    recentReads: List<RecentReadTrack> = emptyList(),
    isEnglish: Boolean = false,
    onSurahClick: (Int) -> Unit,
    onNavigateToHafeziMode: (Int) -> Unit,
    onNavigateToReadingMode: (Int) -> Unit,
    onNavigateToTajweedMode: (Int) -> Unit,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit,
    onNavigateToSurahWithAyah: (Int, String, Int) -> Unit,
    onDeleteBookmark: (com.example.data.local.entity.BookmarkEntity) -> Unit
) {
    val displayRecentReads = recentReads.take(5)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (displayRecentReads.isNotEmpty()) {
            // Section Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Recent Reads" else "সর্বশেষ পঠিত",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                if (displayRecentReads.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = if (isEnglish) "${displayRecentReads.size} Recent" else "${com.example.utils.DateUtil.toBengaliNumerals(displayRecentReads.size)}টি সাম্প্রতিক",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (displayRecentReads.size == 1) {
                val track = displayRecentReads.first()
                RecentReadCardItem(
                    track = track,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    defaultMushafId = defaultMushafId,
                    isEnglish = isEnglish,
                    onNavigateToHafeziMode = onNavigateToHafeziMode,
                    onNavigateToReadingMode = onNavigateToReadingMode,
                    onNavigateToTajweedMode = onNavigateToTajweedMode,
                    onNavigateToMushafPage = onNavigateToMushafPage,
                    onNavigateToSurahWithAyah = onNavigateToSurahWithAyah
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(displayRecentReads) { track ->
                        RecentReadCardItem(
                            track = track,
                            modifier = Modifier.width(260.dp),
                            defaultMushafId = defaultMushafId,
                            isEnglish = isEnglish,
                            onNavigateToHafeziMode = onNavigateToHafeziMode,
                            onNavigateToReadingMode = onNavigateToReadingMode,
                            onNavigateToTajweedMode = onNavigateToTajweedMode,
                            onNavigateToMushafPage = onNavigateToMushafPage,
                            onNavigateToSurahWithAyah = onNavigateToSurahWithAyah
                        )
                    }
                }
            }
        }

        if (bookmarks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isEnglish) "Bookmarks" else "বুকমার্ক সমূহ",
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
fun RecentReadCardItem(
    track: RecentReadTrack,
    modifier: Modifier = Modifier,
    defaultMushafId: String,
    isEnglish: Boolean = false,
    onNavigateToHafeziMode: (Int) -> Unit,
    onNavigateToReadingMode: (Int) -> Unit,
    onNavigateToTajweedMode: (Int) -> Unit,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit,
    onNavigateToSurahWithAyah: (Int, String, Int) -> Unit
) {
    val surahName = if (isEnglish) {
        QuranData.surahNames.find { it.first == track.surahNumber }?.second?.second ?: "Surah ${track.surahNumber}"
    } else {
        QuranData.surahNames.find { it.first == track.surahNumber }?.second?.first ?: "সূরা ${track.surahNumber}"
    }
    
    val (badgeText, badgeColor, icon) = when (track.mode) {
        "TAJWEED" -> Triple(if (isEnglish) "Color Tajweed" else "কালার তাজবীদ", Color(0xFF8B5CF6), Icons.Default.Palette)
        "HAFEZI" -> Triple(if (isEnglish) "Hafezi Quran" else "হাফেজী কুরআন", Color(0xFF10B981), Icons.Outlined.MenuBook)
        "MUSHAF" -> Triple(if (isEnglish) "Mushaf Viewer" else "মুসহাফ ভিউয়ার", Color(0xFF0D9488), Icons.Outlined.Book)
        "READING" -> Triple(if (isEnglish) "Paragraph Reading" else "প্যারাগ্রাফ রিডিং", Color(0xFFF59E0B), Icons.Outlined.AutoStories)
        else -> Triple(if (isEnglish) "Translation & Tafsir" else "অনুবাদ ও তাফসীর", Color(0xFF059669), Icons.Outlined.Translate)
    }

    val mainText = if (isEnglish) {
        when (track.mode) {
            "TAJWEED", "HAFEZI", "MUSHAF" -> {
                if (track.pageNumber != null) "Page: ${track.pageNumber}"
                else "Surah $surahName"
            }
            else -> "Surah $surahName"
        }
    } else {
        when (track.mode) {
            "TAJWEED", "HAFEZI", "MUSHAF" -> {
                if (track.pageNumber != null) "পৃষ্ঠা: ${com.example.utils.DateUtil.toBengaliNumerals(track.pageNumber)}"
                else "সূরা $surahName"
            }
            else -> "সূরা $surahName"
        }
    }

    val subtitleText = if (isEnglish) {
        when (track.mode) {
            "TAJWEED" -> "Surah: $surahName • Page based"
            "HAFEZI" -> "Surah: $surahName • 15 Lines"
            "MUSHAF" -> "Mushaf Page ${track.pageNumber ?: 1}"
            "READING" -> "Ayah: ${track.ayahNumber}"
            else -> "Ayah: ${track.ayahNumber}"
        }
    } else {
        when (track.mode) {
            "TAJWEED" -> "সূরা: $surahName • পৃষ্ঠাভিত্তিক"
            "HAFEZI" -> "সূরা: $surahName • ১৫ লাইন"
            "MUSHAF" -> "মুসহাফ পৃষ্ঠা ${com.example.utils.DateUtil.toBengaliNumerals(track.pageNumber ?: 1)}"
            "READING" -> "আয়াত: ${com.example.utils.DateUtil.toBengaliNumerals(track.ayahNumber)}"
            else -> "আয়াত: ${com.example.utils.DateUtil.toBengaliNumerals(track.ayahNumber)}"
        }
    }

    Card(
        modifier = modifier
            .clickable {
                when (track.mode) {
                    "HAFEZI" -> onNavigateToHafeziMode(track.pageNumber ?: 1)
                    "TAJWEED" -> onNavigateToTajweedMode(track.pageNumber ?: 1)
                    "MUSHAF" -> onNavigateToMushafPage(track.mushafId ?: defaultMushafId, track.pageNumber ?: 1, false)
                    "READING" -> onNavigateToReadingMode(track.surahNumber)
                    "DETAIL" -> onNavigateToSurahWithAyah(track.surahNumber, "LIST", track.ayahNumber)
                    else -> onNavigateToSurahWithAyah(track.surahNumber, "LIST", track.ayahNumber)
                }
            },
        shape = RoundedCornerShape(16.dp),
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
                    .size(38.dp)
                    .background(badgeColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = mainText,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = subtitleText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = GrayText,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun ModesGridSection(
    isHafeziDownloaded: Boolean,
    isDark: Boolean,
    isEnglish: Boolean = false,
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
            text = if (isEnglish) "Quran Reading & Listening Modes" else "কুরআন পঠন ও শ্রবণ মোড",
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
                title = if (isEnglish) "Hafezi Quran" else "হাফেজী কুরআন",
                subtitle = if (isEnglish) "15 Lines Image View" else "১৫ লাইন ইমেজ ভিউ",
                icon = Icons.Default.MenuBook,
                containerColor = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
                iconColor = Color(0xFF10B981),
                onClick = onHafeziPdfClick,
                isPdfBadge = false,
                modifier = Modifier.weight(1f)
            )
            ModeItemCard(
                title = if (isEnglish) "Color Quran" else "কালার কুরআন",
                subtitle = if (isEnglish) "Color Tajweed Text" else "রঙিন তাজবীদ টেক্সট",
                icon = Icons.Default.Palette,
                containerColor = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFEFF6FF),
                iconColor = Color(0xFF3B82F6),
                onClick = onTajweedClick,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeItemCard(
                title = if (isEnglish) "Translation & Tafsir" else "অনুবাদ ও তাফসীর",
                subtitle = if (isEnglish) "Deep Meaning & Tafsir" else "গভীর তাফসীর ও অর্থ",
                icon = Icons.Default.MenuBook,
                containerColor = if (isDark) Color(0xFF4C1D95).copy(alpha = 0.5f) else Color(0xFFF5F3FF),
                iconColor = Color(0xFF8B5CF6),
                onClick = onTranslationClick,
                modifier = Modifier.weight(1f)
            )
            ModeItemCard(
                title = if (isEnglish) "Recitation Player" else "তেলাওয়াত প্লেয়ার",
                subtitle = if (isEnglish) "Listen to Various Qaris" else "বিভিন্ন ক্বারীদের তেলওয়াত শুনুন",
                icon = Icons.Default.PlayArrow,
                containerColor = if (isDark) Color(0xFF7C2D12).copy(alpha = 0.5f) else Color(0xFFFFF7ED),
                iconColor = Color(0xFFF97316),
                onClick = onPlayerClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun DhikrHabitSection(
    isDark: Boolean,
    isEnglish: Boolean = false,
    onNavigateToDhikrReminder: (com.example.utils.DhikrType) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val duroodConfig = remember { com.example.utils.DhikrReminderManager.getConfig(context, com.example.utils.DhikrType.DUROOD) }
    val istighfarConfig = remember { com.example.utils.DhikrReminderManager.getConfig(context, com.example.utils.DhikrType.ISTIGHFAR) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isEnglish) "Daily Dhikr & Habit Reminders" else "দৈনিক আমল ও অভ্যাস রিমাইন্ডার",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Surface(
                color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFECFDF5),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
            ) {
                Text(
                    text = if (isEnglish) "Dhikr Habit" else "আমলের অভ্যাস",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Card 1: দুরুদ শরীফ পড়ার অভ্যাস
        DhikrHabitCard(
            title = if (isEnglish) "Practice Reciting Durood Sharif" else "দুরুদ শরীফ পড়ার অভ্যাস করুন",
            subtitle = if (isEnglish) "Keep reminders active to earn regular blessings by sending peace upon the Prophet (ﷺ) throughout the day." else "সারাদিন নবীজী (ﷺ)-এর ওপর নিয়মিত দুরুদ পাঠের সওয়াব অর্জনে রিমাইন্ডার চালু রাখুন।",
            iconEmoji = "📿",
            iconBg = if (isDark) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFECFDF5),
            badgeColor = Color(0xFF10B981),
            isEnabled = duroodConfig.isEnabled,
            intervalMinutes = duroodConfig.intervalMinutes,
            inactiveActionText = if (isEnglish) "Enable Durood reminder to build a daily habit" else "অভ্যাস গড়তে দুরুদ শরীফ রিমাইন্ডার ফিচার চালু করুন",
            isDark = isDark,
            isEnglish = isEnglish,
            onClick = { onNavigateToDhikrReminder(com.example.utils.DhikrType.DUROOD) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Card 2: ইস্তিগফারের অভ্যাস
        DhikrHabitCard(
            title = if (isEnglish) "Practice Daily Istighfar" else "ইস্তিগফারের অভ্যাস করুন",
            subtitle = if (isEnglish) "Keep reminders active to build a habit of seeking forgiveness from Allah throughout the day." else "সারাদিন মহান আল্লাহর কাছে ক্ষমা প্রার্থনার অভ্যাস গড়ে তুলতে নিয়মিত রিমাইন্ডার চালু রাখুন।",
            iconEmoji = "🤲",
            iconBg = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFEFF6FF),
            badgeColor = Color(0xFF3B82F6),
            isEnabled = istighfarConfig.isEnabled,
            intervalMinutes = istighfarConfig.intervalMinutes,
            inactiveActionText = if (isEnglish) "Enable Istighfar reminder to build a daily habit" else "অভ্যাস গড়তে ইস্তিগফার রিমাইন্ডার চালু করুন",
            isDark = isDark,
            isEnglish = isEnglish,
            onClick = { onNavigateToDhikrReminder(com.example.utils.DhikrType.ISTIGHFAR) }
        )
    }
}

@Composable
fun DhikrHabitCard(
    title: String,
    subtitle: String,
    iconEmoji: String,
    iconBg: Color,
    badgeColor: Color,
    isEnabled: Boolean,
    intervalMinutes: Int,
    inactiveActionText: String,
    isDark: Boolean,
    isEnglish: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isEnabled) badgeColor.copy(alpha = 0.4f) else if (isDark) Color(0xFF2E3842) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon emoji box
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = iconEmoji, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Status tag
                if (isEnabled) {
                    Surface(
                        color = badgeColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Text(
                                text = if (isEnglish) "ACTIVE" else "চালু আছে",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp,
                modifier = Modifier.padding(start = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Action banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = if (isEnabled) badgeColor.copy(alpha = 0.08f) else if (isDark) Color(0xFF1E262F) else Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = if (isEnabled) badgeColor else PrimaryGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isEnabled) {
                                if (isEnglish) "Reminder every $intervalMinutes minutes" else "প্রতি $intervalMinutes মিনিট পরপর রিমাইন্ডার আসবে"
                            } else inactiveActionText,
                            fontSize = 12.sp,
                            fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isEnabled) badgeColor else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = if (isEnabled) badgeColor else PrimaryGreen,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TopFeaturesGridSection(
    isDark: Boolean,
    isEnglish: Boolean = false,
    onQiblaClick: () -> Unit,
    onMosqueClick: () -> Unit = {},
    onDuaClick: () -> Unit,
    onManzilClick: () -> Unit,
    onPlannerClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onVideoCreatorClick: () -> Unit,
    onSubjectwiseClick: () -> Unit,
    onTasbihClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E252B) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF2E3842) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = if (isEnglish) "Top Features" else "টপ ফিচার",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = onMoreClick,
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryGreen.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "More" else "আরও দেখুন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2 Rows of 4 Circular Tool Buttons
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TopFeatureCircleButton(
                    title = if (isEnglish) "Qibla" else "কিবলা",
                    icon = Icons.Default.Explore,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF0F3826) else Color(0xFFE8F5E9),
                    iconTint = if (isDark) Color(0xFF34D399) else Color(0xFF10B981),
                    onClick = onQiblaClick
                )
                TopFeatureCircleButton(
                    title = if (isEnglish) "Mosques" else "মসজিদ",
                    icon = Icons.Default.Place,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF042F2E) else Color(0xFFCCFBF1),
                    iconTint = if (isDark) Color(0xFF2DD4BF) else Color(0xFF0D9488),
                    onClick = onMosqueClick
                )
                TopFeatureCircleButton(
                    title = if (isEnglish) "Masnoon Dua" else "মাসনূন দুআ",
                    icon = Icons.Default.VolunteerActivism,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF0C334D) else Color(0xFFE0F2FE),
                    iconTint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                    onClick = onDuaClick
                )
                TopFeatureCircleButton(
                    title = if (isEnglish) "Manzil" else "মানযিল",
                    icon = Icons.Default.Security,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF064E3B) else Color(0xFFD1FAE5),
                    iconTint = if (isDark) Color(0xFF10B981) else Color(0xFF059669),
                    onClick = onManzilClick
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TopFeatureCircleButton(
                    title = if (isEnglish) "Planner" else "প্ল্যানার",
                    icon = Icons.Default.TrackChanges,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF4C0519) else Color(0xFFFFE4E6),
                    iconTint = if (isDark) Color(0xFFFB7185) else Color(0xFFE11D48),
                    onClick = onPlannerClick
                )
                TopFeatureCircleButton(
                    title = if (isEnglish) "Calendar" else "ক্যালেন্ডার",
                    icon = Icons.Default.CalendarMonth,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                    iconTint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                    onClick = onCalendarClick
                )
                TopFeatureCircleButton(
                    title = if (isEnglish) "Video Maker" else "ভিডিও মেকার",
                    icon = Icons.Default.Videocam,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF3B0764) else Color(0xFFF3E8FF),
                    iconTint = if (isDark) Color(0xFFC084FC) else Color(0xFF9333EA),
                    onClick = onVideoCreatorClick
                )
                TopFeatureCircleButton(
                    title = if (isEnglish) "Topics" else "বিষয়ভিত্তিক",
                    icon = Icons.Default.AutoStories,
                    isDark = isDark,
                    bgColor = if (isDark) Color(0xFF1E1B4B) else Color(0xFFE0E7FF),
                    iconTint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    onClick = onSubjectwiseClick
                )
            }
        }
    }
}

@Composable
fun NearbyMosqueHomeBanner(
    isDark: Boolean,
    isEnglish: Boolean = false,
    onMosqueClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onMosqueClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF132A1F) else Color(0xFFEDF8F2)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E4633) else Color(0xFFB7E4C7)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            color = PrimaryGreen,
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isEnglish) "Find Nearby Mosques" else "নিকটবর্তী মসজিদ খুঁজুন",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = Color(0xFFEAB308).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "LIVE GPS" else "লাইভ জিপিএস",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = if (isEnglish) "Nearby Jame Mosques, Jama'ah times & navigation" else "আশপাশের জামে মসজিদ, জামাতের সময় ও দিকনির্দেশনা",
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp,
                        maxLines = 2
                    )
                }
            }

            IconButton(
                onClick = onMosqueClick,
                modifier = Modifier
                    .size(34.dp)
                    .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TopFeatureCircleButton(
    title: String,
    icon: ImageVector,
    isDark: Boolean,
    bgColor: Color,
    iconTint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(bgColor, CircleShape)
                .border(
                    1.dp,
                    if (isDark) iconTint.copy(alpha = 0.25f) else iconTint.copy(alpha = 0.15f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.height(7.dp))

        Text(
            text = title,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun DailyDuaFeaturedSection(
    isDark: Boolean,
    isEnglish: Boolean = false,
    arabicFontName: String = "kfgqpc",
    onReadDua: (com.example.data.DuaItem) -> Unit,
    onViewAllDuas: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    // Ensure DuaData is initialized
    LaunchedEffect(Unit) {
        com.example.data.DuaData.initialize(context)
    }

    val allDuas = com.example.data.DuaData.richDuas
    val featuredDua = remember(allDuas) {
        if (allDuas.isNotEmpty()) {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            allDuas[dayOfYear % allDuas.size]
        } else null
    }

    if (featuredDua == null) return

    val firstSegment = featuredDua.segments.firstOrNull()
    val arabic = firstSegment?.arabic ?: ""
    val transliteration = firstSegment?.transliteration ?: ""
    val translation = firstSegment?.translation ?: ""
    val reference = firstSegment?.reference?.ifEmpty { firstSegment.bottom } ?: ""
    val duaTitle = featuredDua.title

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF192520) else Color(0xFFF4FBF7)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF224233) else Color(0xFFD1EAD9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row
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
                            .size(28.dp)
                            .background(Color(0xFFEAB308).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFEAB308),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (isEnglish) "Dua of the Day" else "আজকের নির্বাচিত দুআ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    onClick = onViewAllDuas,
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryGreen.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "All Duas" else "সব দুআ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryGreen
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title
            Text(
                text = duaTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen
            )

            // Arabic text
            if (arabic.isNotEmpty() && arabic != "null") {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = arabic,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = com.example.ui.theme.getArabicFont(arabicFontName),
                    color = if (isDark) Color(0xFFE6F4EA) else Color(0xFF134E34),
                    textAlign = TextAlign.Right,
                    lineHeight = 34.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Transliteration
            if (transliteration.isNotEmpty() && transliteration != "null") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEnglish) "Pronunciation: $transliteration" else "উচ্চারণ: $transliteration",
                    fontSize = 12.5.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // Translation
            if (translation.isNotEmpty() && translation != "null") {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isEnglish) "Meaning: $translation" else "অর্থ: $translation",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            // Reference
            if (reference.isNotEmpty() && reference != "null") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "— $reference",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryGreen.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Actions Bottom Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onReadDua(featuredDua) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryGreen,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Read Full Dua" else "সম্পূর্ণ দুআ পড়ুন",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            val copyText = buildString {
                                append(duaTitle).append("\n\n")
                                if (arabic.isNotEmpty() && arabic != "null") append(arabic).append("\n\n")
                                if (transliteration.isNotEmpty() && transliteration != "null") append(if (isEnglish) "Pronunciation: " else "উচ্চারণ: ").append(transliteration).append("\n\n")
                                if (translation.isNotEmpty() && translation != "null") append(if (isEnglish) "Meaning: " else "অর্থ: ").append(translation).append("\n")
                                if (reference.isNotEmpty() && reference != "null") append("— ").append(reference)
                            }
                            clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(copyText))
                            Toast.makeText(context, if (isEnglish) "Dua copied to clipboard" else "দুআটি কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isDark) Color(0xFF23362B) else Color(0xFFE2F3E9),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = if (isEnglish) "Copy" else "কপি করুন",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareText = buildString {
                                append("✨ ").append(duaTitle).append("\n\n")
                                if (arabic.isNotEmpty() && arabic != "null") append(arabic).append("\n\n")
                                if (transliteration.isNotEmpty() && transliteration != "null") append(if (isEnglish) "Pronunciation: " else "উচ্চারণ: ").append(transliteration).append("\n\n")
                                if (translation.isNotEmpty() && translation != "null") append(if (isEnglish) "Meaning: " else "অর্থ: ").append(translation).append("\n\n")
                                if (reference.isNotEmpty() && reference != "null") append("— ").append(reference).append("\n\n")
                                append(if (isEnglish) "Al-Quran & Islamic App" else "আল-কুরআন ও ইসলামিক অ্যাপ")
                            }
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, if (isEnglish) "Share Dua" else "দুআটি শেয়ার করুন"))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isDark) Color(0xFF23362B) else Color(0xFFE2F3E9),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = if (isEnglish) "Share" else "শেয়ার করুন",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubjectwiseTopCategoriesSection(
    isDark: Boolean,
    isEnglish: Boolean = false,
    onCategoryClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color(0xFF4F46E5).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (isEnglish) "Subjectwise Quran" else "বিষয়ভিত্তিক কুরআন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                onClick = onViewAllClick,
                shape = RoundedCornerShape(12.dp),
                color = PrimaryGreen.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isEnglish) "All Topics" else "সব বিষয়",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal scrolling categories
        val subjectCategories = if (isEnglish) {
            listOf(
                Triple("Faith & Creed", "Tawheed, Prophethood & Asmaul Husna • 10+ topics", Color(0xFFF59E0B)),
                Triple("Worship & Deeds", "Salah, Sawm, Hajj & Zakat • 8+ topics", Color(0xFF10B981)),
                Triple("Hereafter & Judgment", "Death, Grave, Resurrection & Paradise • 11+ topics", Color(0xFF8B5CF6)),
                Triple("Character & Ethics", "Honesty, Good Behavior & Manners • 8+ topics", Color(0xFF0284C7)),
                Triple("Family & Social Life", "Parents, Relatives & Neighbor Rights • 7+ topics", Color(0xFFF43F5E)),
                Triple("Dua & Dhikr", "Quranic Duas & Remembrance of Allah • 6+ topics", Color(0xFF14B8A6)),
                Triple("Manzil", "Special healing and protection verses • 33 Ayahs", Color(0xFF059669))
            )
        } else {
            listOf(
                Triple("ঈমান ও আকীদা", "তাওহীদ, রিসালাত ও আসমাউল হুসনা • ১০+ বিষয়", Color(0xFFF59E0B)),
                Triple("ইবাদত ও আমল", "সালাত, সিয়াম, হজ ও জাকাত • ৮+ বিষয়", Color(0xFF10B981)),
                Triple("পরকাল, কিয়ামত ও আখিরাত", "মৃত্যু, কবর, হাশর ও জান্নাত • ১১+ বিষয়", Color(0xFF8B5CF6)),
                Triple("আখলাক, চরিত্র ও শিষ্টাচার", "সততা, উত্তম আচরণ ও শিষ্টাচার • ৮+ বিষয়", Color(0xFF0284C7)),
                Triple("পারিবারিক ও সামাজিক জীবন", "পিতা-মাতা, আত্মীয় ও প্রতিবেশীর হক • ৭+ বিষয়", Color(0xFFF43F5E)),
                Triple("দোয়া ও জিকির", "কুরআনের দোয়া ও আল্লাহর জিকির • ৬+ বিষয়", Color(0xFF14B8A6)),
                Triple("মানযিল", "কুরআনের বিশেষ শেফা ও সুরক্ষার আয়াত • ৩৩ আয়াত", Color(0xFF059669))
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(subjectCategories) { (title, subtitle, accentColor) ->
                SubjectwiseCategoryCard(
                    title = title,
                    subtitle = subtitle,
                    accentColor = accentColor,
                    isDark = isDark,
                    onClick = { onCategoryClick(title) }
                )
            }
        }
    }
}

@Composable
fun SubjectwiseCategoryCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1C2229) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) accentColor.copy(alpha = 0.25f) else accentColor.copy(alpha = 0.18f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        modifier = Modifier
            .width(190.dp)
            .height(105.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                lineHeight = 15.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun formatPostTimeAgo(timestamp: Long, isEnglish: Boolean = false): String {
    if (timestamp <= 0L) return if (isEnglish) "Just now" else "এইমাত্র"
    val diffMillis = System.currentTimeMillis() - timestamp
    if (diffMillis < 0) return if (isEnglish) "Just now" else "এইমাত্র"
    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    if (isEnglish) {
        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes mins ago"
            hours < 24 -> "$hours hours ago"
            days < 2 -> "Yesterday"
            days < 7 -> "$days days ago"
            days < 8 -> "1 week ago"
            else -> {
                try {
                    val sdf = java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.ENGLISH)
                    sdf.format(java.util.Date(timestamp))
                } catch (e: Exception) {
                    "Recently"
                }
            }
        }
    }

    fun String.toBanglaDigits(): String {
        val banglaDigits = mapOf(
            '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
            '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
        )
        return this.map { banglaDigits[it] ?: it }.joinToString("")
    }

    return when {
        minutes < 1 -> "এইমাত্র"
        minutes < 60 -> "${minutes.toString().toBanglaDigits()} মিনিট আগে"
        hours < 24 -> "${hours.toString().toBanglaDigits()} ঘণ্টা আগে"
        days < 2 -> "গতকাল"
        days < 7 -> "${days.toString().toBanglaDigits()} দিন আগে"
        days < 8 -> "১ সপ্তাহ আগে"
        else -> {
            try {
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                val d = cal.get(java.util.Calendar.DAY_OF_MONTH)
                val m = cal.get(java.util.Calendar.MONTH)
                val y = cal.get(java.util.Calendar.YEAR)
                val englishMonthsBengali = listOf("জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন", "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর")
                val monthName = if (m in englishMonthsBengali.indices) englishMonthsBengali[m] else ""
                "${d.toString().toBanglaDigits()} $monthName ${y.toString().toBanglaDigits()}"
            } catch (e: Exception) {
                try {
                    val sdf = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale("bn", "BD"))
                    sdf.format(java.util.Date(timestamp))
                } catch (ex: Exception) {
                    "সম্প্রতি"
                }
            }
        }
    }
}

@Composable
fun FeaturedIslamicMediaSection(
    isDark: Boolean,
    isEnglish: Boolean = false,
    blogPosts: List<BlogPost> = emptyList(),
    onPostClick: (BlogPost) -> Unit = {},
    onViewAllClick: () -> Unit = {}
) {
    val fallbackBlogPosts = remember(isEnglish) {
        if (isEnglish) {
            listOf(
                BlogPost(
                    id = "featured_1",
                    title = "Spiritual Tranquility & Virtues of Regular Quran Recitation",
                    content = "The Holy Quran is healing for the believers' hearts and a beacon of divine guidance. Daily regular recitation removes worries and brings deep inner serenity.",
                    author = "Mawlana Abdullah",
                    category = "Quran Light",
                    imageUrl = "",
                    readTime = "4 mins",
                    timestamp = System.currentTimeMillis() - 25 * 60 * 1000L
                ),
                BlogPost(
                    id = "featured_2",
                    title = "Miraculous Blessings of Daily Dua and Istighfar",
                    content = "Whoever constantly seeks forgiveness from Allah, Allah opens doors of relief from every distress and provides sustenance from unimaginable sources.",
                    author = "Mufti Mahmud Hasan",
                    category = "Dua & Deeds",
                    imageUrl = "",
                    readTime = "3 mins",
                    timestamp = System.currentTimeMillis() - 2 * 3600 * 1000L
                ),
                BlogPost(
                    id = "featured_3",
                    title = "Tahajjud Prayer & the Path to Nearness with Allah",
                    content = "In the last third of the night when Allah descends to the lowest heaven, every sincere prayer and tear is directly answered by the Almighty.",
                    author = "Shaykh Ahmadullah",
                    category = "Night Prayers",
                    imageUrl = "",
                    readTime = "5 mins",
                    timestamp = System.currentTimeMillis() - 5 * 3600 * 1000L
                ),
                BlogPost(
                    id = "featured_4",
                    title = "Immense Significance of Excellent Character in Islam",
                    content = "One of the greatest beauties of Islam is good character and noble conduct. Kindness, empathy, and forgiveness reflect true faith.",
                    author = "Dr. Abdullah Jahangir",
                    category = "Ethics & Manners",
                    imageUrl = "",
                    readTime = "4 mins",
                    timestamp = System.currentTimeMillis() - 24 * 3600 * 1000L
                ),
                BlogPost(
                    id = "featured_5",
                    title = "Quran and Sunnah Deeds for Increasing Sustenance (Barakah in Rizq)",
                    content = "Practicing Taqwa, serving parents, maintaining family ties, and giving charity bring unprecedented blessings into one's provision and life.",
                    author = "Mufti Tariq Jameel",
                    category = "Islamic Life",
                    imageUrl = "",
                    readTime = "3 mins",
                    timestamp = System.currentTimeMillis() - 48 * 3600 * 1000L
                )
            )
        } else {
            listOf(
                BlogPost(
                    id = "featured_1",
                    title = "কুরআন নিয়মিত তিলাওয়াতের আত্মিক প্রশান্তি ও ফজিলত",
                    content = "পবিত্র কুরআন মুমিনের অন্তরের শেফা এবং হেদায়েতের আলোকবর্তিকা। দৈনন্দিন জীবনে নিয়মিত তিলাওয়াত মানুষের মন থেকে সকল দুশ্চিন্তা ও পেরেশানি দূর করে আত্মিক শান্তি এনে দেয়।",
                    author = "মাওলানা আব্দুল্লাহ",
                    category = "কুরআনের আলো",
                    imageUrl = "",
                    readTime = "৪ মিনিট",
                    timestamp = System.currentTimeMillis() - 25 * 60 * 1000L
                ),
                BlogPost(
                    id = "featured_2",
                    title = "দৈনন্দিন জীবনে দুআ ও ইস্তিগফারের অলৌকিক বরকত",
                    content = "যে ব্যক্তি বেশি বেশি ইস্তিগফার করে, আল্লাহ তায়ালা তার সকল সংকটে মুক্তির পথ তৈরি করেন এবং এমন উৎস থেকে রিজিকের ব্যবস্থা করেন যা সে কল্পনাও করেনি।",
                    author = "মুফতি মাহমুদ হাসান",
                    category = "আমল ও দুআ",
                    imageUrl = "",
                    readTime = "৩ মিনিট",
                    timestamp = System.currentTimeMillis() - 2 * 3600 * 1000L
                ),
                BlogPost(
                    id = "featured_3",
                    title = "তাহাজ্জুদ নামাজ ও আল্লাহর নৈকট্য অর্জনের পথ",
                    content = "রাতের শেষ তৃতীয়াংশে যখন মহান আল্লাহ প্রথম আসমানে নেমে আসেন, তখন বান্দার প্রতিটি আন্তরিক মুনাজাত ও চোখের পানি সরাসরি আল্লাহর দরবারে কবুল হয়।",
                    author = "শাইখ আহমাদুল্লাহ",
                    category = "নফল ইবাদত",
                    imageUrl = "",
                    readTime = "৫ মিনিট",
                    timestamp = System.currentTimeMillis() - 5 * 3600 * 1000L
                ),
                BlogPost(
                    id = "featured_4",
                    title = "উত্তম চরিত্র ও সুন্দর ব্যবহারের অপরিসীম গুরুত্ব",
                    content = "ইসলামের অন্যতম প্রধান সৌন্দর্য হলো সদ্ব্যবহার ও সদাচার। মানুষের সাথে সুন্দর আচরণ, সহমর্মিতা ও ক্ষমাশীলতার মাধ্যমে পরিপূর্ণ মুমিনের পরিচয় ফুটে ওঠে।",
                    author = "ড. আব্দুল্লাহ জাহাঙ্গীর",
                    category = "আখলাক ও শিষ্টাচার",
                    imageUrl = "",
                    readTime = "৪ মিনিট",
                    timestamp = System.currentTimeMillis() - 24 * 3600 * 1000L
                ),
                BlogPost(
                    id = "featured_5",
                    title = "রিজিকে বরকত বৃদ্ধির কুরআন ও সুন্নাহ নির্দেশিত আমল",
                    content = "তাকওয়া অবলম্বন, পিতা-মাতার সেবা, আত্মীয়তার সম্পর্ক বজায় রাখা ও বেশি বেশি দান-সদকার মাধ্যমে আল্লাহ রাব্বুল আলামিন রিজিকে অভাবনীয় বরকত দান করেন।",
                    author = "মুফতি তারিক জামিল",
                    category = "জীবন বিধান",
                    imageUrl = "",
                    readTime = "৩ মিনিট",
                    timestamp = System.currentTimeMillis() - 48 * 3600 * 1000L
                )
            )
        }
    }

    val displayPosts = remember(blogPosts, fallbackBlogPosts) {
        if (blogPosts.isNotEmpty()) blogPosts.take(6) else fallbackBlogPosts
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = if (isEnglish) "Islamic Articles & Posts" else "ইসলামিক আলোচনা ও ব্লগ পোস্ট",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                onClick = onViewAllClick,
                shape = RoundedCornerShape(12.dp),
                color = PrimaryGreen.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isEnglish) "View All" else "সবগুলো",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(displayPosts, key = { it.id.ifEmpty { it.title } }) { post ->
                DynamicBlogPostCard(
                    post = post,
                    isDark = isDark,
                    isEnglish = isEnglish,
                    onClick = { onPostClick(post) }
                )
            }
            item {
                ViewAllBlogsCard(
                    isDark = isDark,
                    isEnglish = isEnglish,
                    totalPosts = if (blogPosts.isNotEmpty()) blogPosts.size else displayPosts.size,
                    onClick = onViewAllClick
                )
            }
        }
    }
}

@Composable
fun DynamicBlogPostCard(
    post: BlogPost,
    isDark: Boolean,
    isEnglish: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E262E) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (isDark) Color(0xFF2D3845) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 3.dp),
        modifier = Modifier
            .width(260.dp)
            .height(240.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Visual Banner Area (Fixed 105dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D3B2E),
                                Color(0xFF14533D),
                                Color(0xFF08261D)
                            )
                        )
                    )
            ) {
                if (post.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = post.imageUrl,
                        contentDescription = post.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                )
                            )
                    )
                } else {
                    // Decorative Canvas Art
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            radius = size.width * 0.45f,
                            center = Offset(size.width * 0.85f, size.height * 0.2f)
                        )
                        drawCircle(
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            radius = size.width * 0.35f,
                            center = Offset(size.width * 0.15f, size.height * 0.85f)
                        )
                    }
                    // Subtle Islamic icon in background
                    Icon(
                        imageVector = Icons.Default.AutoStories,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.08f),
                        modifier = Modifier
                            .size(68.dp)
                            .align(Alignment.Center)
                    )
                }

                // Category Tag Badge on top left
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = PrimaryGreen.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = post.category.ifBlank { if (isEnglish) "Islamic Knowledge" else "ইসলামিক জ্ঞান" },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }

                // Dynamic Time Ago Badge on top right
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.68f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = formatPostTimeAgo(post.timestamp, isEnglish),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
            }

            // Body Content Area (Uniform remaining height)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = post.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        minLines = 2,
                        lineHeight = 18.sp,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = post.content.replace("\n", " ").trim(),
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        minLines = 2,
                        lineHeight = 15.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Footer with Author and Read CTA
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = post.author.firstOrNull()?.toString() ?: (if (isEnglish) "I" else "ই"),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        Text(
                            text = post.author.ifBlank { if (isEnglish) "Islamic Scholar" else "ইসলামিক স্কলার" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryGreen.copy(alpha = 0.10f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "Read" else "পড়ুন",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ViewAllBlogsCard(
    isDark: Boolean,
    isEnglish: Boolean = false,
    totalPosts: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E262E) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            PrimaryGreen.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
        modifier = Modifier
            .width(150.dp)
            .height(240.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            PrimaryGreen.copy(alpha = 0.05f),
                            PrimaryGreen.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Feed,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isEnglish) "View All Articles" else "সব ব্লগ দেখুন",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isEnglish) "$totalPosts+ Articles" else "$totalPosts+ টি আলোচনা",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SubtleCardDecorativeWave(
    waveColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Layer 1: Broad soft underlying wave sweeping from left to bottom-right
        val wave1 = Path().apply {
            moveTo(0f, height)
            lineTo(0f, height * 0.92f)
            cubicTo(
                width * 0.25f, height * 0.98f,
                width * 0.50f, height * 0.74f,
                width, height * 0.70f
            )
            lineTo(width, height)
            close()
        }
        drawPath(
            path = wave1,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.05f),
                    waveColor.copy(alpha = 0.12f)
                ),
                startY = height * 0.65f,
                endY = height
            )
        )

        // Layer 2: Main organic curved wave in the bottom-right corner
        val wave2 = Path().apply {
            moveTo(width * 0.20f, height)
            cubicTo(
                width * 0.45f, height * 0.94f,
                width * 0.70f, height * 0.62f,
                width, height * 0.82f
            )
            lineTo(width, height)
            close()
        }
        drawPath(
            path = wave2,
            brush = Brush.verticalGradient(
                colors = listOf(
                    waveColor.copy(alpha = 0.08f),
                    waveColor.copy(alpha = 0.18f)
                ),
                startY = height * 0.60f,
                endY = height
            )
        )

        // Layer 3: Subtle accent ripple at the very bottom right
        val wave3 = Path().apply {
            moveTo(width * 0.55f, height)
            cubicTo(
                width * 0.70f, height * 0.96f,
                width * 0.85f, height * 0.78f,
                width, height * 0.88f
            )
            lineTo(width, height)
            close()
        }
        drawPath(
            path = wave3,
            color = waveColor.copy(alpha = 0.10f)
        )
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
    isPdfBadge: Boolean = false
) {
    Card(
        modifier = modifier
            .shadow(3.dp, RoundedCornerShape(18.dp), spotColor = iconColor.copy(alpha = 0.20f))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
        ) {
            // Subtle Decorative Wave at the bottom
            SubtleCardDecorativeWave(
                waveColor = iconColor,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(containerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPdfBadge) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(iconColor, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "PDF",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Subtitle
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
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
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onSelectQari: (String) -> Unit
) {
    val selectionItems = com.example.util.QariData.list.map { item ->
        com.example.ui.components.SelectionItem(
            id = item.id,
            title = if (isEnglish) item.nameEnglish else item.nameBengali,
            subtitle = if (isEnglish) item.nameBengali else item.nameEnglish,
            icon = Icons.Default.RecordVoiceOver
        )
    }

    com.example.ui.components.SmartSelectionDialog(
        title = if (isEnglish) "Select Qari" else "ক্বারী নির্বাচন করুন",
        subtitle = if (isEnglish) "Choose your preferred reciter" else "আপনার পছন্দের তেলাওয়াতকারী বেছে নিন",
        headerIcon = Icons.Default.RecordVoiceOver,
        items = selectionItems,
        selectedId = selectedQariId,
        onSelectItem = onSelectQari,
        onDismiss = onDismiss,
        showSearch = true,
        searchPlaceholder = if (isEnglish) "Search Qari..." else "ক্বারী খুঁজুন..."
    )
}

@Composable
fun SurahSelectorDialog(
    isEnglish: Boolean = false,
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
                    text = if (isEnglish) "Select Surah" else "সূরা নির্বাচন করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isEnglish) "Search Surah..." else "সূরা খুঁজুন...", fontSize = 14.sp) },
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
                        Text(if (isEnglish) "No Surah found!" else "কোনো সূরা পাওয়া যায়নি!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredSurahs) { surahPair ->
                            val surahId = surahPair.first
                            val surahName = if (isEnglish) "Surah ${surahPair.second.first}" else surahPair.second.first
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
                                            text = if (isEnglish) surahId.toString() else surahId.toBengaliNumerals(),
                                            color = PrimaryGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = surahName,
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
                Text(if (isEnglish) "Close" else "বন্ধ করুন", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    isEnglish: Boolean = false,
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
            text = if (isEnglish) "Surah Recitation Player" else "সূরা তেলাওয়াত প্লেয়ার",
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
                    val surahName = QuranData.surahNames.find { it.first == currentPlayingSurah }?.second?.first ?: if (isEnglish) "Surah" else "সূরা"
                    val qariName = com.example.util.QariData.getQariNameEnglish(selectedQariId)
                    val totalAyahs = currentPlayingAyahs.size
                    val progress = if (totalAyahs > 0) (currentPlayingAyahIndex.toFloat() / totalAyahs.toFloat()) else 0f
                    val ayahDisplay = if (isEnglish) {
                        "Ayah: ${currentPlayingAyahIndex + 1} / $totalAyahs"
                    } else {
                        "আয়াত: ${(currentPlayingAyahIndex + 1).toBengaliNumerals()} / ${totalAyahs.toBengaliNumerals()}"
                    }
                    
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
                                text = if (isEnglish) "Surah $surahName" else "সূরা $surahName",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isEnglish) "Reciter: $qariName" else "ক্বারী: $qariName",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = ayahDisplay,
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
                            Text(if (isEnglish) "Ayah Loop" else "আয়াত লুপ", color = if (isRepeatAyahEnabled) White else PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                            Text(if (isEnglish) "Surah Loop" else "সূরা লুপ", color = if (isRepeatSurahEnabled) White else PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else {
                    // Placeholder / Set Selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEnglish) "No Recitation Playing" else "কোনো তেলাওয়াত সচল নেই",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnglish) "Select Qari & Surah to enjoy Quran recitation" else "ক্বারী ও সূরা নির্বাচন করে তেলাওয়াত উপভোগ করুন",
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
                                    Text(if (isEnglish) "Select Qari" else "ক্বারী নির্বাচন", color = PrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                                    Text(if (isEnglish) "Start Surah" else "সূরা চালু করুন", color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
    isEnglish: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPlayingSurah by viewModel.currentPlayingSurah.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()

    if (currentPlayingSurah != null) {
        val surahNamePair = QuranData.surahNames.find { it.first == currentPlayingSurah }
        val name = if (isEnglish) "Surah ${surahNamePair?.second?.first ?: ""}" else "সূরা ${surahNamePair?.second?.first ?: ""}"

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
                        text = name,
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (isPlaying) (if (isEnglish) "Playing..." else "চলছে...") else (if (isEnglish) "Paused" else "বন্ধ আছে"),
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
    isEnglish: Boolean = false,
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
                        text = if (isEnglish) "Copy" else "কপি",
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
                            text = if (isEnglish) "Share" else "শেয়ার",
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
                                    text = if (isEnglish) "Share Text" else "টেক্সট শেয়ার",
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
                                    text = if (isEnglish) "Share Image" else "ছবি শেয়ার",
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
                text = if (isEnglish) "❝Quran Reader❞ App" else "❝কুরআন রিডার❞ অ্যাপ",
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
    isEnglish: Boolean = false,
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
                        val formattedIndex = if (isEnglish) dua.id.toString() else formatToBanglaNumber(dua.id)
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
                                    val isBismillah = cleanForBismillah == "بسماللهالرحمنالرحিম" || cleanForBismillah == "بسمٱللهٱلرحمنٱلرحيم"
                                    
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
                                                text = if (isEnglish) "Meaning:" else "অর্থ:",
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
                                                text = if (isEnglish) "Pronunciation:" else "উচ্চারণ:",
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
                                        val contextText = if (trimmed.startsWith("দোয়ার প্রেক্ষাপট") || trimmed.startsWith("দোয়ার প্রেক্ষাপট") || trimmed.startsWith("Context:")) {
                                            trimmed
                                        } else {
                                            if (isEnglish) "Context: ${segment.bottom}" else "দোয়ার প্রেক্ষাপট: ${segment.bottom}"
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
                        DuaActionButtonsRow(dua = dua, isEnglish = isEnglish)
                    }
                }
            }
        }
    }
}
