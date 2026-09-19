package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.launch
import com.example.data.model.CombinedAyah
import com.example.data.model.removeWaqfSigns
import com.example.data.model.formatWaqfSigns
import com.example.data.model.appendStyledWaqfText
import com.example.ui.state.UiState
import com.example.ui.theme.*
import com.example.ui.screens.toBengaliNumerals
import com.example.ui.viewmodels.SurahDetailViewModel
import com.example.ui.viewmodels.PlaybackMode
import com.example.ui.components.WordByWordText
import com.example.ui.components.AyahNumberCircle
import com.example.ui.components.parseHtmlToAnnotatedString

enum class ViewMode { LIST, READING, MUSHAF, TAFSIR }

data class ProcessedWord(
    val id: Int,
    val position: Int,
    val charTypeName: String,
    val textUthmani: String,
    val translationText: String?,
    val audioUrl: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SurahDetailScreen(
    surahNumber: Int,
    isJuz: Boolean = false,
    initialViewMode: String? = null,
    initialAyah: Int = -1,
    viewModel: SurahDetailViewModel,
    onNavigateBack: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onNavigateBack)
    val uiState by viewModel.uiState.collectAsState()
    val showTranslation by viewModel.showTranslation.collectAsState()
    val showTransliteration by viewModel.showTransliteration.collectAsState()
    val availableTranslations by viewModel.availableTranslations.collectAsState()
    val showTajweed by viewModel.showTajweed.collectAsState()
    val bookmarkList by viewModel.bookmarks.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val bengaliFontSize by viewModel.bengaliFontSize.collectAsState()
    val arabicFontName by viewModel.arabicFontName.collectAsState()
    val bengaliFontName by viewModel.bengaliFontName.collectAsState()
    val tanzilTextStyle by viewModel.tanzilTextStyle.collectAsState()
    val showWaqfSigns by viewModel.showWaqfSigns.collectAsState()
    val arabicLineSpacing by viewModel.arabicLineSpacing.collectAsState()
    
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPlayingAyahNumber by viewModel.currentPlayingAyahNumber.collectAsState()
    val currentPlayingWordUrl by viewModel.audioRepository.currentPlayingWordUrl.collectAsState()
    val playbackMode by viewModel.playbackMode.collectAsState()

    // Download States
    val isDownloadingOffline by viewModel.isDownloadingOffline.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val downloadError by viewModel.downloadError.collectAsState()

    val selectedTafsirIds by viewModel.selectedTafsirIds.collectAsState()
    val selectedTafsirNames by viewModel.selectedTafsirNames.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val availableTafsirs by viewModel.availableTafsirs.collectAsState()
    val selectedTranslationIds by viewModel.selectedTranslationIds.collectAsState()
    
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val parsedViewMode = when (initialViewMode) {
        "MUSHAF" -> ViewMode.MUSHAF
        "READING" -> ViewMode.READING
        "TAFSIR" -> ViewMode.TAFSIR
        else -> ViewMode.LIST
    }
    var viewMode by remember { mutableStateOf(parsedViewMode) }
    var isTafsirSwitching by remember { mutableStateOf(false) }

    val pageOrder = remember { listOf(ViewMode.LIST, ViewMode.READING, ViewMode.TAFSIR, ViewMode.MUSHAF) }
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = pageOrder.indexOf(parsedViewMode).takeIf { it >= 0 } ?: 0,
        pageCount = { 4 }
    )

    LaunchedEffect(pagerState.targetPage) {
        val newMode = pageOrder[pagerState.targetPage]
        if (viewMode != newMode) {
            if (newMode == ViewMode.TAFSIR && viewMode != ViewMode.TAFSIR) {
                isTafsirSwitching = true
            } else {
                isTafsirSwitching = false
            }
            viewMode = newMode
        }
    }

    LaunchedEffect(viewMode) {
        if (viewMode == ViewMode.TAFSIR) {
            kotlinx.coroutines.delay(120)
            isTafsirSwitching = false
        } else {
            isTafsirSwitching = false
        }
    }

    var showPlayerBottomSheet by remember { mutableStateOf(false) }
    var showSettingsBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val settingsSheetState = rememberModalBottomSheetState()
    
    val currentPlayingAyah = (uiState as? UiState.Success)?.data?.find { it.numberInSurah == currentPlayingAyahNumber }

    var activeSurahNumber by remember(surahNumber, isJuz) {
        androidx.compose.runtime.mutableIntStateOf(
            if (isJuz) com.example.data.QuranData.getJuzStartSurah(surahNumber) else surahNumber
        )
    }

    LaunchedEffect(activeSurahNumber, tanzilTextStyle, selectedTafsirIds) {
        viewModel.loadSurah(activeSurahNumber)
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
                        Text("কুরআন রিডার", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp)
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
                    IconButton(onClick = { showSettingsBottomSheet = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = GrayText)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        bottomBar = {
            FloatingAudioPlayer(
                isPlaying = isPlaying,
                currentPlayingAyahNum = currentPlayingAyahNumber,
                playbackMode = playbackMode,
                onPlayPauseClick = {
                    val currentAyah = currentPlayingAyah ?: (uiState as? UiState.Success)?.data?.firstOrNull()
                    viewModel.togglePlayPause(currentAyah, surahNumber)
                },
                onPreviousClick = {
                    val currentAyah = currentPlayingAyah ?: (uiState as? UiState.Success)?.data?.firstOrNull()
                    viewModel.playPrevious(currentAyah, surahNumber)
                },
                onNextClick = {
                    val currentAyah = currentPlayingAyah ?: (uiState as? UiState.Success)?.data?.firstOrNull()
                    viewModel.playNext(currentAyah, surahNumber)
                },
                onModeToggleClick = {
                    viewModel.setPlaybackMode(
                        if (playbackMode == PlaybackMode.SURAH) PlaybackMode.AYAH else PlaybackMode.SURAH
                    )
                },
                onPlayerTabClick = {
                    showPlayerBottomSheet = true
                },
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.components.QuranLoadingAnimation(text = "লোড হচ্ছে...")
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { 
                            if (isJuz) {
                                viewModel.loadJuz(surahNumber)
                            } else {
                                viewModel.loadSurah(surahNumber) 
                            }
                        }, colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)) {
                            Text("আবার চেষ্টা করুন", color = White)
                        }
                    }
                }
                is UiState.Success -> {
                    val listStates = remember { List(4) { androidx.compose.foundation.lazy.LazyListState() } }
                    val coroutineScope = rememberCoroutineScope()
                    var searchQuery by remember { mutableStateOf("") }
                    var highlightedAyahNumber by remember { mutableStateOf<Int?>(null) }
                    
                    val effectiveInitialAyah = remember(surahNumber, isJuz, initialAyah) {
                        if (initialAyah > 0) {
                            initialAyah
                        } else if (isJuz) {
                            com.example.data.QuranData.getJuzStartAyah(surahNumber)
                        } else {
                            -1
                        }
                    }

                    val isStandaloneAyatAlKursi = surahNumber == 2 && effectiveInitialAyah == 255 && !isJuz
                    val isStandaloneLastTwoBaqarah = surahNumber == 2 && effectiveInitialAyah == 285 && !isJuz
                    val isStandalone = isStandaloneAyatAlKursi || isStandaloneLastTwoBaqarah
                    
                    val rawDisplayedData = if (isStandaloneAyatAlKursi) {
                        state.data.filter { it.numberInSurah == effectiveInitialAyah }
                    } else if (isStandaloneLastTwoBaqarah) {
                        state.data.filter { it.numberInSurah == 285 || it.numberInSurah == 286 }
                    } else {
                        state.data
                    }
                    val displayedData = remember(rawDisplayedData, showWaqfSigns) {
                        if (showWaqfSigns) {
                            rawDisplayedData.map { ayah ->
                                ayah.copy(
                                    arabicText = ayah.arabicText.formatWaqfSigns(),
                                    words = ayah.words.map { word ->
                                        word.copy(
                                            textUthmani = word.textUthmani?.formatWaqfSigns()
                                        )
                                    }
                                )
                            }
                        } else {
                            rawDisplayedData.map { ayah ->
                                ayah.copy(
                                    arabicText = ayah.arabicText.removeWaqfSigns(),
                                    words = ayah.words.map { word ->
                                        word.copy(
                                            textUthmani = word.textUthmani?.removeWaqfSigns()
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    LaunchedEffect(displayedData, effectiveInitialAyah) {
                        if (effectiveInitialAyah > 0 && displayedData.isNotEmpty()) {
                            highlightedAyahNumber = effectiveInitialAyah
                            
                            val headerCount = if (activeSurahNumber != 1 && activeSurahNumber != 9 && !isStandalone) 2 else 1
                            
                            // Allow LazyColumn to finish its initial layout before scrolling
                            kotlinx.coroutines.delay(150)
                            
                            val ayahIndex = displayedData.indexOfFirst { it.numberInSurah == effectiveInitialAyah }
                            if (ayahIndex != -1) {
                                val targetIndex = ayahIndex + headerCount
                                listStates.forEach { listState ->
                                    listState.scrollToItem(targetIndex)
                                }
                            }
                            
                            val targetAyah = displayedData.find { it.numberInSurah == effectiveInitialAyah }
                            if (targetAyah != null) {
                                val ayahsByPage = displayedData.groupBy { it.page }
                                val pagesList = ayahsByPage.keys.toList()
                                val pageIndex = pagesList.indexOf(targetAyah.page)
                                if (pageIndex != -1 && listStates.size > 3) {
                                    listStates[3].scrollToItem(pageIndex + headerCount)
                                }
                            }
                        }
                    }

                    // Auto-remove highlight after a few seconds
                    LaunchedEffect(highlightedAyahNumber) {
                        if (highlightedAyahNumber != null) {
                            kotlinx.coroutines.delay(2500)
                            highlightedAyahNumber = null
                        }
                    }

                    // Auto-scroll to currently playing ayah during audio play
                    LaunchedEffect(currentPlayingAyahNumber) {
                        val playingAyahNum = currentPlayingAyahNumber
                        if (playingAyahNum != null && playingAyahNum > 0) {
                            val targetAyah = displayedData.find { it.numberInSurah == playingAyahNum }
                            if (targetAyah != null) {
                                val headerCount = if (activeSurahNumber != 1 && activeSurahNumber != 9 && !isStandalone) 2 else 1
                                val currentListState = listStates[pageOrder.indexOf(viewMode).takeIf { it >= 0 } ?: 0]
                                if (viewMode == ViewMode.MUSHAF) {
                                    val ayahsByPage = displayedData.groupBy { it.page }
                                    val pagesList = ayahsByPage.keys.toList()
                                    val pageIndex = pagesList.indexOf(targetAyah.page)
                                    if (pageIndex != -1) {
                                        currentListState.animateScrollToItem(pageIndex + headerCount)
                                    }
                                } else {
                                    val ayahIndex = displayedData.indexOfFirst { it.numberInSurah == playingAyahNum }
                                    if (ayahIndex != -1) {
                                        currentListState.animateScrollToItem(ayahIndex + headerCount)
                                    }
                                }
                            }
                        }
                    }
                    
                    val currentListState = listStates[pageOrder.indexOf(viewMode).takeIf { it >= 0 } ?: 0]
                    val firstVisibleItemIndex by remember(currentListState) { derivedStateOf { currentListState.firstVisibleItemIndex } }
                    LaunchedEffect(firstVisibleItemIndex, viewMode, displayedData) {
                        val headerCount = if (activeSurahNumber != 1 && activeSurahNumber != 9 && !isStandalone) 2 else 1
                        if (viewMode == ViewMode.MUSHAF) {
                            val ayahsByPage = displayedData.groupBy { it.page }
                            val pagesList = ayahsByPage.keys.toList()
                            val visiblePageIndex = firstVisibleItemIndex - headerCount
                            if (visiblePageIndex >= 0 && visiblePageIndex < pagesList.size) {
                                val pageNum = pagesList[visiblePageIndex]
                                val ayahsInPage = ayahsByPage[pageNum]
                                val firstAyahInPage = ayahsInPage?.firstOrNull()
                                if (firstAyahInPage != null) {
                                    viewModel.updateLastReadAyah(firstAyahInPage.numberInSurah)
                                }
                            }
                        } else {
                            val visibleAyahIndex = firstVisibleItemIndex - headerCount
                            if (visibleAyahIndex >= 0 && visibleAyahIndex < displayedData.size) {
                                val currentAyah = displayedData[visibleAyahIndex]
                                viewModel.updateLastReadAyah(currentAyah.numberInSurah)
                            }
                        }
                    }
                    
                    val surahData = com.example.data.QuranData.surahNames.find { it.first == activeSurahNumber }
                    val surahName = surahData?.second?.first ?: "সূরা $activeSurahNumber"
                    val title = if (surahNumber == 2 && effectiveInitialAyah == 255 && !isJuz) {
                        "আয়াতুল কুরসি"
                    } else if (surahNumber == 2 && effectiveInitialAyah == 285 && !isJuz) {
                        "বাকারার শেষ ২ আয়াত"
                    } else {
                        surahName
                    }
                    val subtitle = if (surahNumber == 2 && effectiveInitialAyah == 255 && !isJuz) "সূরা আল-বাকারাহ, আয়াত ২৫৫" else if (surahNumber == 2 && effectiveInitialAyah == 285 && !isJuz) "সূরা আল-বাকারাহ, আয়াত ২৮৫-২৮৬" else (surahData?.second?.second ?: "")
                    val info1 = "সূরা: ${com.example.utils.DateUtil.toBengaliNumerals(activeSurahNumber)}"
                    val info2 = com.example.data.QuranData.getSurahType(activeSurahNumber)
                    val info3 = if (surahNumber == 2 && effectiveInitialAyah == 255 && !isJuz) "১টি আয়াত" else if (surahNumber == 2 && effectiveInitialAyah == 285 && !isJuz) "২টি আয়াত" else "মোট আয়াত: ${com.example.utils.DateUtil.toBengaliNumerals(displayedData.size)}"

                    var headerHeightPx by remember { mutableFloatStateOf(0f) }
                    val headerHeightDp = with(androidx.compose.ui.platform.LocalDensity.current) { headerHeightPx.toDp() }
                    
                    val headerOffsetPx = if (currentListState.firstVisibleItemIndex == 0) {
                        -currentListState.firstVisibleItemScrollOffset.toFloat()
                    } else {
                        -headerHeightPx
                    }
                    
                    var showFloatingMenu by remember { mutableStateOf(false) }
                    LaunchedEffect(currentListState.isScrollInProgress, headerOffsetPx, headerHeightPx) {
                        if (headerHeightPx > 0 && headerOffsetPx <= -headerHeightPx) {
                            if (currentListState.isScrollInProgress) {
                                showFloatingMenu = true
                            } else if (showFloatingMenu) {
                                kotlinx.coroutines.delay(2500)
                                showFloatingMenu = false
                            }
                        } else {
                            showFloatingMenu = false
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        androidx.compose.foundation.pager.HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.Top
                        ) { page ->
                            val pageViewMode = pageOrder[page]
                            val pageListState = listStates[page]
                            
                            LazyColumn(
                                state = pageListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                            item {
                                Spacer(modifier = Modifier.height(headerHeightDp + 16.dp))
                            }
                        if (activeSurahNumber != 1 && activeSurahNumber != 9 && !isStandalone) {
                            item {
                                BismillahSection(arabicFontName = arabicFontName)
                            }
                        }
                        if (pageViewMode == ViewMode.TAFSIR && isTafsirSwitching) {
                            items(3) { index ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.3f)
                                                .height(16.dp)
                                                .background(Color(0xFFE5E7EB), RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(24.dp)
                                                .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .height(20.dp)
                                                .background(Color(0xFFF3F4F6), RoundedCornerShape(4.dp))
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                        )
                                    }
                                }
                            }
                        } else if (pageViewMode == ViewMode.MUSHAF) {
                            val ayahsByPage = displayedData.groupBy { it.page }
                            ayahsByPage.forEach { (page, ayahs) ->
                                item {
                                    MushafPageView(
                                        page = page,
                                        ayahs = ayahs,
                                        surahNumber = activeSurahNumber,
                                        onPlayWord = { viewModel.playWord(it) },
                                        onPlayAyah = { viewModel.togglePlayPause(it, activeSurahNumber) },
                                        arabicFontName = arabicFontName,
                                        arabicFontSize = arabicFontSize,
                                        currentPlayingWordUrl = currentPlayingWordUrl,
                                        currentPlayingAyahNumber = currentPlayingAyahNumber,
                                        highlightedAyahNumber = highlightedAyahNumber,
                                        isPlaying = isPlaying,
                                        arabicLineSpacing = arabicLineSpacing,
                                        tanzilTextStyle = tanzilTextStyle
                                    )
                                }
                            }
                        } else {
                            items(
                                items = displayedData,
                                key = { it.number }
                            ) { ayah ->
                                val isAyahPlaying = isPlaying && currentPlayingAyahNumber == ayah.numberInSurah
                                val isBookmarked = bookmarkList.any { it.type == "AYAH" && it.referenceId == ayah.number }
                                AyahCard(
                                    ayah = ayah,
                                    viewMode = pageViewMode,
                                    surahNumber = activeSurahNumber,
                                    playAudio = { viewModel.togglePlayPause(ayah, activeSurahNumber) },
                                    onPlayWord = { viewModel.playWord(it) },
                                    isPlaying = isAyahPlaying,
                                    isHighlighted = (highlightedAyahNumber == ayah.numberInSurah),
                                    showTranslation = showTranslation,
                                    showTransliteration = showTransliteration,
                                    showTajweed = showTajweed,
                                    arabicFontSize = arabicFontSize,
                                    bengaliFontSize = bengaliFontSize,
                                    arabicFontName = arabicFontName,
                                    bengaliFontName = bengaliFontName,
                                    currentPlayingWordUrl = currentPlayingWordUrl,
                                    isBookmarked = isBookmarked,
                                    onToggleBookmark = { viewModel.toggleBookmark(ayah, activeSurahNumber) },
                                    arabicLineSpacing = arabicLineSpacing,
                                    selectedTafsirNames = selectedTafsirNames
                                )
                            }
                        }

                        if (displayedData.isNotEmpty() && (!isTafsirSwitching || pageViewMode != ViewMode.TAFSIR)) {
                            item(key = "surah_nav_$activeSurahNumber") {
                                com.example.ui.components.SurahNavigationButtons(
                                    currentSurahNumber = activeSurahNumber,
                                    onNavigateToSurah = { newSurah ->
                                        activeSurahNumber = newSurah
                                        coroutineScope.launch {
                                            pageListState.scrollToItem(0)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    } // End of HorizontalPager

                        // HeaderCard
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .onGloballyPositioned { headerHeightPx = it.size.height.toFloat() }
                                .offset { androidx.compose.ui.unit.IntOffset(0, kotlin.math.round(headerOffsetPx).toInt()) }
                                .padding(16.dp)
                        ) {
                            HeaderCard(
                                title = title,
                                subtitle = subtitle,
                                info1 = info1,
                                info2 = info2,
                                info3 = info3,
                                viewMode = pageOrder[pagerState.currentPage], 
                                onModeChange = { mode ->
                                     coroutineScope.launch {
                                         pagerState.animateScrollToPage(pageOrder.indexOf(mode))
                                     }
                                 },
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                onSearch = {
                                    fun convertDigits(input: String): String {
                                        val bDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
                                        var res = input.trim()
                                        bDigits.forEachIndexed { idx, ch ->
                                            res = res.replace(ch, ('0' + idx))
                                        }
                                        return res
                                    }
                                    val targetNum = convertDigits(searchQuery).toIntOrNull()
                                    if (targetNum != null && targetNum > 0) {
                                        val targetAyah = displayedData.find { it.numberInSurah == targetNum }
                                        if (targetAyah != null) {
                                            highlightedAyahNumber = targetNum
                                            val headerCount = if (activeSurahNumber != 1 && activeSurahNumber != 9 && !isStandalone) 2 else 1
                                            val activeMode = pageOrder[pagerState.currentPage]
                                            if (activeMode == ViewMode.MUSHAF) {
                                                val ayahsByPage = displayedData.groupBy { it.page }
                                                val pagesList = ayahsByPage.keys.toList()
                                                val pageIndex = pagesList.indexOf(targetAyah.page)
                                                if (pageIndex != -1) {
                                                    coroutineScope.launch {
                                                        currentListState.animateScrollToItem(pageIndex + headerCount)
                                                    }
                                                }
                                            } else {
                                                val ayahIndex = displayedData.indexOfFirst { it.numberInSurah == targetNum }
                                                if (ayahIndex != -1) {
                                                    coroutineScope.launch {
                                                        currentListState.animateScrollToItem(ayahIndex + headerCount)
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "আয়াত $targetNum এই সূরায় নেই", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onPlayerClick = {
                                    val currentAyah = currentPlayingAyah ?: displayedData.firstOrNull()
                                    viewModel.togglePlayPause(currentAyah, surahNumber)
                                }
                            )
                        }

                        // Floating Menu
                        androidx.compose.animation.AnimatedVisibility(
                            visible = showFloatingMenu,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { -it }),
                            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it })
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(8.dp, RoundedCornerShape(100.dp))
                                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                                    .border(1.dp, Border, RoundedCornerShape(100.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val currentMode = pageOrder[pagerState.currentPage]
                                ViewModeToggle("লিস্ট", Icons.Default.List, currentMode == ViewMode.LIST) { 
                                    coroutineScope.launch { pagerState.animateScrollToPage(pageOrder.indexOf(ViewMode.LIST)) } 
                                }
                                ViewModeToggle("শব্দার্থ", Icons.Outlined.Book, currentMode == ViewMode.READING) { 
                                    coroutineScope.launch { pagerState.animateScrollToPage(pageOrder.indexOf(ViewMode.READING)) } 
                                }
                                ViewModeToggle("তাফসির", Icons.Outlined.Info, currentMode == ViewMode.TAFSIR) { 
                                    coroutineScope.launch { pagerState.animateScrollToPage(pageOrder.indexOf(ViewMode.TAFSIR)) } 
                                }
                                ViewModeToggle("মুসহাফ", Icons.Outlined.MenuBook, currentMode == ViewMode.MUSHAF) { 
                                    coroutineScope.launch { pagerState.animateScrollToPage(pageOrder.indexOf(ViewMode.MUSHAF)) } 
                                }
                            }
                        }
                }
            }
        }
        
        }
        if (showPlayerBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPlayerBottomSheet = false },
                sheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                PlayerBottomSheetContent(
                    currentPlayingAyahNum = currentPlayingAyahNumber,
                    playbackMode = playbackMode,
                    onModeChange = { viewModel.setPlaybackMode(it) },
                    onClose = { showPlayerBottomSheet = false },
                    currentAyah = currentPlayingAyah,
                    arabicFontName = arabicFontName
                )
            }
        }

        if (showSettingsBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsBottomSheet = false },
                sheetState = settingsSheetState,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReaderSettingsBottomSheetContent(
                    keepScreenOn = keepScreenOn,
                    onKeepScreenOnToggle = { viewModel.setKeepScreenOn(it) },
                    availableTranslations = availableTranslations,
                    availableTafsirs = availableTafsirs,
                    selectedTranslationIds = selectedTranslationIds,
                    onSelectedTranslationIdsChange = { viewModel.setSelectedTranslationIds(it) },
                    selectedTafsirIds = selectedTafsirIds,
                    onSelectedTafsirIdsChange = { viewModel.setSelectedTafsirIds(it) },
                    showTranslation = showTranslation,
                    onShowTranslationToggle = { viewModel.toggleTranslation() },
                    showWaqfSigns = showWaqfSigns,
                    onShowWaqfSignsToggle = { viewModel.setShowWaqfSigns(it) },
                    arabicFontSize = arabicFontSize,
                    onArabicFontSizeChange = { viewModel.setArabicFontSize(it) },
                    bengaliFontSize = bengaliFontSize,
                    onBengaliFontSizeChange = { viewModel.setBengaliFontSize(it) },
                    arabicFontName = arabicFontName,
                    onArabicFontNameChange = { viewModel.setArabicFontName(it) },
                    bengaliFontName = bengaliFontName,
                    onBengaliFontNameChange = { viewModel.setBengaliFontName(it) },
                    tanzilTextStyle = tanzilTextStyle,
                    onTanzilTextStyleChange = { viewModel.setTanzilTextStyle(it) },
                    isDownloadingOffline = isDownloadingOffline,
                    downloadProgress = downloadProgress,
                    downloadStatus = downloadStatus,
                    downloadError = downloadError,
                    onDownloadClick = {
                        viewModel.downloadSurahOffline(surahNumber, "এই")
                    },
                    onCancelDownloadClick = { viewModel.cancelOfflineDownload() },
                    onClose = { showSettingsBottomSheet = false },
                    arabicLineSpacing = arabicLineSpacing,
                    onArabicLineSpacingChange = { viewModel.setArabicLineSpacing(it) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HeaderCard(
    title: String, 
    subtitle: String, 
    info1: String, 
    info2: String, 
    info3: String, 
    viewMode: ViewMode, 
    onModeChange: (ViewMode) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onPlayerClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(24.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(subtitle, fontSize = 14.sp, color = PrimaryGreen)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .shadow(2.dp, RoundedCornerShape(100.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                        .border(0.5.dp, Border, RoundedCornerShape(100.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(info1, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                
                if (info2.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                            .border(0.5.dp, PrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (info2.contains("মাক্কী")) Icons.Outlined.Mosque else Icons.Outlined.Place,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(info2, fontSize = 11.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (info3.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .shadow(2.dp, RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                            .border(0.5.dp, Border, RoundedCornerShape(100.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(info3, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                    .shadow(2.dp, RoundedCornerShape(100.dp))
                    .background(PrimaryGreen, RoundedCornerShape(100.dp))
                    .clickable { onPlayerClick() }
                    .padding(horizontal = 12.dp, vertical = 4.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("প্লেয়ার", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .width(160.dp)
                        .height(38.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 12.dp, end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "আয়াত নং",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = onSearchQueryChange,
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Search
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onSearch = { onSearch() },
                                    onDone = { onSearch() }
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryGreen)
                                .clickable { onSearch() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Search",
                                tint = White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(100.dp))
                    .border(1.dp, Border, RoundedCornerShape(100.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ViewModeToggle("লিস্ট", Icons.Default.List, viewMode == ViewMode.LIST) { onModeChange(ViewMode.LIST) }
                ViewModeToggle("শব্দার্থ", Icons.Outlined.Book, viewMode == ViewMode.READING) { onModeChange(ViewMode.READING) }
                ViewModeToggle("তাফসির", Icons.Outlined.Info, viewMode == ViewMode.TAFSIR) { onModeChange(ViewMode.TAFSIR) }
                ViewModeToggle("মুসহাফ", Icons.Outlined.MenuBook, viewMode == ViewMode.MUSHAF) { onModeChange(ViewMode.MUSHAF) }
            }
        }
    }
}

@Composable
fun InfoChip(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryGreen)
            Spacer(modifier = Modifier.width(4.dp))
        }
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun RowScope.ViewModeToggle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surface,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
    )
    val contentColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isSelected) White else GrayText,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 300)
    )
    Box(
        modifier = Modifier
            .weight(1f)
            .background(bgColor, RoundedCornerShape(100.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (isSelected) White else GrayText, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text, color = if (isSelected) White else GrayText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BismillahSection(arabicFontName: String = "Me Quran") {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            fontSize = 36.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontFamily = com.example.ui.theme.getArabicFont(arabicFontName)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AyahCard(
    ayah: CombinedAyah,
    viewMode: ViewMode,
    surahNumber: Int,
    playAudio: () -> Unit,
    onPlayWord: (String) -> Unit,
    isPlaying: Boolean,
    isHighlighted: Boolean = false,
    showTranslation: Boolean,
    showTransliteration: Boolean = false,
    showTajweed: Boolean = false,
    arabicFontSize: Float,
    availableTranslations: List<com.example.data.model.TranslationResourceDto> = emptyList(),
    bengaliFontSize: Float,
    arabicFontName: String = "Me Quran",
    bengaliFontName: String = "SolaimanLipi",
    currentPlayingWordUrl: String? = null,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    arabicLineSpacing: Float = 2.0f,
    selectedTafsirNames: List<String> = emptyList()
) {
    var showTafsirDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    val parsedTafsir = remember(ayah.tafsirText) {
        ayah.tafsirText?.parseHtmlToAnnotatedString(PrimaryGreen)
    }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val bengaliFont = com.example.ui.theme.getBengaliFont(bengaliFontName)
    
    val shareText = buildString {
        append(ayah.arabicText)
        append("\n\n")
        append(ayah.bengaliText)
        if (!ayah.tafsirText.isNullOrEmpty()) {
            append("\n\nতাফসীর:\n")
            append(android.text.Html.fromHtml(ayah.tafsirText, android.text.Html.FROM_HTML_MODE_LEGACY).toString())
        }
    }
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    val surahData = remember(surahNumber) {
        com.example.data.QuranData.surahNames.find { it.first == surahNumber }
    }
    val surahName = surahData?.second?.first ?: "সূরা $surahNumber"

    if (showTafsirDialog) {
        AlertDialog(
            onDismissRequest = { showTafsirDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
            title = {
                Column {
                    val tafsirTitle = if (selectedTafsirNames.isNotEmpty()) {
                        selectedTafsirNames.joinToString(", ")
                    } else {
                        "তাফসীর"
                    }
                    Text(text = tafsirTitle, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "আয়াত ${ayah.numberInSurah}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (parsedTafsir != null) {
                        Text(
                            text = parsedTafsir,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Justify
                        )
                    } else {
                        Text(text = "এই আয়াতের তাফসীর পাওয়া যায়নি।", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTafsirDialog = false }) {
                    Text("বন্ধ করুন", color = PrimaryGreen)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

    val isCardHighlighted = isPlaying || isHighlighted
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(20.dp))
            .background(if (isCardHighlighted) PrimaryGreen.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .border(if (isCardHighlighted) 1.5.dp else 0.dp, if (isCardHighlighted) PrimaryGreen else MaterialTheme.colorScheme.surface, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column {
            if (viewMode == ViewMode.LIST) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AyahNumberCircle(
                            number = ayah.numberInSurah,
                            size = 32.dp,
                            backgroundColor = Color(0xFFE8F5E9),
                            borderColor = PrimaryGreen,
                            textColor = PrimaryGreen,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পারা ${ayah.juz} • পৃষ্ঠা ${ayah.page}", color = PrimaryGreen, fontSize = 10.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) PrimaryGreen else GrayText
                            )
                        }
                        IconButton(onClick = { showTafsirDialog = true }) {
                            Icon(Icons.Outlined.Book, contentDescription = "Tafsir", tint = GrayText)
                        }
                        IconButton(onClick = playAudio) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                contentDescription = "Play", 
                                tint = if (isPlaying) PrimaryGreen else GrayText
                            )
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = GrayText)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("কপি করুন", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        clipboardManager.setText(AnnotatedString(shareText))
                                        Toast.makeText(context, "কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryGreen) }
                                )
                                DropdownMenuItem(
                                    text = { Text("শেয়ার করুন", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryGreen) }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (viewMode == ViewMode.READING) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AyahNumberCircle(
                            number = ayah.numberInSurah,
                            size = 32.dp,
                            backgroundColor = Color(0xFFE8F5E9),
                            borderColor = PrimaryGreen,
                            textColor = PrimaryGreen,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পারা ${ayah.juz} • পৃষ্ঠা ${ayah.page}", color = PrimaryGreen, fontSize = 10.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onToggleBookmark) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) PrimaryGreen else GrayText
                            )
                        }
                        IconButton(onClick = { showTafsirDialog = true }) {
                            Icon(Icons.Outlined.Book, contentDescription = "Tafsir", tint = GrayText)
                        }
                        IconButton(onClick = playAudio) {
                            Icon(
                                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, 
                                contentDescription = "Play", 
                                tint = if (isPlaying) PrimaryGreen else GrayText
                            )
                        }
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More", tint = GrayText)
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("কপি করুন", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        clipboardManager.setText(AnnotatedString(shareText))
                                        Toast.makeText(context, "কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                                    },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryGreen) }
                                )
                                DropdownMenuItem(
                                    text = { Text("শেয়ার করুন", color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        showMoreMenu = false
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, shareText)
                                            type = "text/plain"
                                        }
                                        val shareIntent = Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryGreen) }
                                )
                            }
                        }
                    }
                }
                if (ayah.words.isNotEmpty()) {
                    WordByWordText(
                        words = ayah.words,
                        ayahNumber = ayah.numberInSurah,
                        arabicFontSize = arabicFontSize,
                        arabicFont = arabicFont,
                        showTransliteration = showTransliteration,
                        onWordPlay = { onPlayWord(it) },
                        currentPlayingWordUrl = currentPlayingWordUrl,
                        surahNumber = surahNumber,
                        ayahNumberInSurah = ayah.numberInSurah,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    AyahInlineText(
                        arabicText = ayah.arabicText,
                        ayahNumber = ayah.numberInSurah,
                        fontSize = arabicFontSize.toFloat(),
                        fontFamily = arabicFont,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        lineSpacing = arabicLineSpacing,
                        arabicFontName = arabicFontName,
                        showTajweed = showTajweed
                    )
                }
                                if (showTranslation) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (ayah.translations.isNotEmpty()) {
                        ayah.translations.forEach { trans ->
                                                        val translationInfo = availableTranslations.find { it.id == trans.resourceId }
                            val fallbackNames = mapOf(161 to "Taisirul Quran", 162 to "Bayan Foundation", 163 to "Dr. Abu Bakr Muhammad Zakaria", 213 to "Suhel Syed Siraj", 85 to "M.A.S. Abdel Haleem", 131 to "Dr. Mustafa Khattab", 20 to "Saheeh International", 97 to "Mufti Taqi Usmani", 54 to "Maulana Ashraf Ali Thanvi", 136 to "Mufti Taqi Usmani")
                            val translatorName = translationInfo?.translatedName?.name ?: translationInfo?.name ?: fallbackNames[trans.resourceId] ?: "Unknown" 
                            
                            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(
                                        text = translatorName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontFamily = bengaliFont
                                    )
                                    if (translationInfo != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .background(PrimaryGreen.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = translationInfo.languageName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                                fontSize = 9.sp,
                                                color = PrimaryGreen,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                fontFamily = bengaliFont
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = android.text.Html.fromHtml(trans.text, android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                                    fontSize = bengaliFontSize.sp,
                                    fontFamily = bengaliFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = android.text.Html.fromHtml(ayah.bengaliText, android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                            fontSize = bengaliFontSize.sp,
                            fontFamily = bengaliFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else if (viewMode == ViewMode.TAFSIR) {
                AyahInlineText(
                    arabicText = ayah.arabicText,
                    ayahNumber = ayah.numberInSurah,
                    fontSize = arabicFontSize.toFloat(),
                    fontFamily = arabicFont,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    lineSpacing = arabicLineSpacing,
                    arabicFontName = arabicFontName,
                    showTajweed = showTajweed
                )
                if (showTranslation) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = ayah.bengaliText,
                        fontSize = bengaliFontSize.sp,
                        fontFamily = bengaliFont,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (parsedTafsir != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    val tafsirHeader = if (selectedTafsirNames.isNotEmpty()) {
                        selectedTafsirNames.joinToString(", ")
                    } else {
                        "তাফসীর"
                    }
                    Text(
                        text = tafsirHeader,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = parsedTafsir,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                AyahActionButtonsRow(
                    ayah = ayah,
                    surahName = surahName
                )
            } else {
                AyahInlineText(
                    arabicText = ayah.arabicText,
                    ayahNumber = ayah.numberInSurah,
                    fontSize = arabicFontSize.toFloat(),
                    fontFamily = arabicFont,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                    lineSpacing = arabicLineSpacing
                )
                                if (showTranslation) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (ayah.translations.isNotEmpty()) {
                        ayah.translations.forEach { trans ->
                                                        val translationInfo = availableTranslations.find { it.id == trans.resourceId }
                            val fallbackNames = mapOf(161 to "Taisirul Quran", 162 to "Bayan Foundation", 163 to "Dr. Abu Bakr Muhammad Zakaria", 213 to "Suhel Syed Siraj", 85 to "M.A.S. Abdel Haleem", 131 to "Dr. Mustafa Khattab", 20 to "Saheeh International", 97 to "Mufti Taqi Usmani", 54 to "Maulana Ashraf Ali Thanvi", 136 to "Mufti Taqi Usmani")
                            val translatorName = translationInfo?.translatedName?.name ?: translationInfo?.name ?: fallbackNames[trans.resourceId] ?: "Unknown" 
                            
                            androidx.compose.foundation.layout.Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    Text(
                                        text = translatorName,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        fontFamily = bengaliFont
                                    )
                                    if (translationInfo != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .background(PrimaryGreen.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = translationInfo.languageName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                                                fontSize = 9.sp,
                                                color = PrimaryGreen,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                                fontFamily = bengaliFont
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = android.text.Html.fromHtml(trans.text, android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                                    fontSize = bengaliFontSize.sp,
                                    fontFamily = bengaliFont,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Text(
                            text = android.text.Html.fromHtml(ayah.bengaliText, android.text.Html.FROM_HTML_MODE_LEGACY).toString(),
                            fontSize = bengaliFontSize.sp,
                            fontFamily = bengaliFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingAudioPlayer(
    isPlaying: Boolean,
    currentPlayingAyahNum: Int?,
    playbackMode: PlaybackMode,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onModeToggleClick: () -> Unit,
    onPlayerTabClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = GrayText,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Center Pill controller
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp))
                    .border(1.dp, GrayText.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                    .clickable { onPlayerTabClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mode toggle
                IconButton(
                    onClick = onModeToggleClick, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (playbackMode == PlaybackMode.SURAH) Icons.Default.Repeat else Icons.Default.RepeatOne,
                        contentDescription = "Mode",
                        tint = if (playbackMode == PlaybackMode.SURAH) PrimaryGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous
                IconButton(
                    onClick = onPreviousClick, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(PrimaryGreen, CircleShape)
                        .clickable { onPlayPauseClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Next
                IconButton(
                    onClick = onNextClick, 
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Symmetrical spacer to balance the back button
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun PlayerBottomSheetContent(
    currentPlayingAyahNum: Int?,
    playbackMode: PlaybackMode,
    onModeChange: (PlaybackMode) -> Unit,
    onClose: () -> Unit,
    currentAyah: CombinedAyah?,
    arabicFontName: String = "Me Quran"
) {
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "কুরআন প্লেয়ার",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = GrayText)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (currentAyah != null) {
            Text(
                text = currentAyah.arabicText,
                fontSize = 24.sp,
                fontFamily = arabicFont,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentAyah.bengaliText,
                fontSize = 14.sp,
                fontFamily = com.example.ui.theme.getBengaliFont(arabicFontName), // or LocalBengaliFont
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )
        } else {
            Text(
                text = "কোনো আয়াত বাজানো হচ্ছে না",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Mode Toggles (Single Ayah vs Continuous Surah)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (playbackMode == PlaybackMode.AYAH) PrimaryGreen else Color.Transparent)
                    .clickable { onModeChange(PlaybackMode.AYAH) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "সিঙ্গেল আয়াত",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (playbackMode == PlaybackMode.AYAH) White else MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (playbackMode == PlaybackMode.SURAH) PrimaryGreen else Color.Transparent)
                    .clickable { onModeChange(PlaybackMode.SURAH) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "সূরা কন্টিনিউয়াস",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (playbackMode == PlaybackMode.SURAH) White else MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ReaderSettingsBottomSheetContent(
    keepScreenOn: Boolean = false,
    onKeepScreenOnToggle: (Boolean) -> Unit = {},
    availableTranslations: List<com.example.data.model.TranslationResourceDto> = emptyList(),
    availableTafsirs: List<com.example.data.model.TafsirResourceDto> = emptyList(),
    selectedTranslationIds: Set<String> = emptySet(),
    onSelectedTranslationIdsChange: (Set<String>) -> Unit = {},
    selectedTafsirIds: Set<String> = emptySet(),
    onSelectedTafsirIdsChange: (Set<String>) -> Unit = {},
    showTranslation: Boolean,
    onShowTranslationToggle: (Boolean) -> Unit,
    showWaqfSigns: Boolean = true,
    onShowWaqfSignsToggle: (Boolean) -> Unit = {},
    arabicFontSize: Float,
    onArabicFontSizeChange: (Float) -> Unit,
    bengaliFontSize: Float,
    onBengaliFontSizeChange: (Float) -> Unit,
    arabicFontName: String = "Me Quran",
    onArabicFontNameChange: (String) -> Unit = {},
    bengaliFontName: String = "SolaimanLipi",
    onBengaliFontNameChange: (String) -> Unit = {},
    tanzilTextStyle: String = "default-indopak",
    onTanzilTextStyleChange: (String) -> Unit = {},
    isDownloadingOffline: Boolean,
    downloadProgress: Int,
    downloadStatus: String?,
    downloadError: String?,
    onDownloadClick: () -> Unit,
    onCancelDownloadClick: () -> Unit,
    onClose: () -> Unit,
    arabicLineSpacing: Float = 2.0f,
    onArabicLineSpacingChange: (Float) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "রিডার সেটিংস",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = GrayText)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Keep Screen On Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "স্ক্রিন চালু রাখুন (Always On Display)",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = keepScreenOn,
                onCheckedChange = onKeepScreenOnToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = PrimaryGreen,
                    uncheckedThumbColor = GrayText,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Translation Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "বাংলা অনুবাদ প্রদর্শন",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = showTranslation,
                onCheckedChange = onShowTranslationToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = PrimaryGreen,
                    uncheckedThumbColor = GrayText,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Waqf Signs Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "থামার চিহ্ন প্রদর্শন (م، ج،صلے)",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = showWaqfSigns,
                onCheckedChange = onShowWaqfSignsToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = White,
                    checkedTrackColor = PrimaryGreen,
                    uncheckedThumbColor = GrayText,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Arabic Font Size
        com.example.ui.components.SettingAdjustmentRow(
            label = "আরবি হরফের আকার",
            valueText = "${arabicFontSize.toInt()}".toBengaliNumerals(),
            onDecrease = {
                val newSize = (arabicFontSize - 1f).coerceIn(24f, 48f)
                onArabicFontSizeChange(newSize)
            },
            onIncrease = {
                val newSize = (arabicFontSize + 1f).coerceIn(24f, 48f)
                onArabicFontSizeChange(newSize)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Bengali Font Size
        com.example.ui.components.SettingAdjustmentRow(
            label = "বাংলা হরফের আকার",
            valueText = "${bengaliFontSize.toInt()}".toBengaliNumerals(),
            onDecrease = {
                val newSize = (bengaliFontSize - 1f).coerceIn(12f, 28f)
                onBengaliFontSizeChange(newSize)
            },
            onIncrease = {
                val newSize = (bengaliFontSize + 1f).coerceIn(12f, 28f)
                onBengaliFontSizeChange(newSize)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Arabic Line Spacing Settings
        com.example.ui.components.SettingAdjustmentRow(
            label = "আরবি লাইন স্পেস",
            valueText = String.format(java.util.Locale.US, "%.2f", arabicLineSpacing).toBengaliNumerals(),
            onDecrease = {
                val newSpacing = (arabicLineSpacing - 0.05f).coerceIn(2.00f, 3.00f)
                onArabicLineSpacingChange(newSpacing)
            },
            onIncrease = {
                val newSpacing = (arabicLineSpacing + 0.05f).coerceIn(2.00f, 3.00f)
                onArabicLineSpacingChange(newSpacing)
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Translation Selection
        Text(
            text = "অনুবাদ নির্বাচন করুন",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        var translationExpanded by remember { mutableStateOf(false) }
        val selectedTranslation = availableTranslations.find { selectedTranslationIds.contains(it.id.toString()) }?.let {
            it.translatedName?.name ?: it.name ?: "Unknown"
        } ?: "অনুবাদ নির্বাচন করুন"
        
        OutlinedButton(
            onClick = { translationExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        ) {
            Text(text = selectedTranslation, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }

        if (translationExpanded) {
            val selectionItems = availableTranslations.map { translation ->
                com.example.ui.components.SelectionItem(
                    id = translation.id.toString(),
                    title = translation.translatedName?.name ?: translation.name ?: "Unknown",
                    subtitle = translation.authorName ?: translation.languageName,
                    icon = Icons.Default.Translate
                )
            }
            com.example.ui.components.SmartSelectionDialog(
                title = "অনুবাদ নির্বাচন করুন",
                subtitle = "আপনার পছন্দের কুরআন অনুবাদ বেছে নিন",
                headerIcon = Icons.Default.Translate,
                items = selectionItems,
                selectedId = selectedTranslationIds.firstOrNull() ?: "",
                onSelectItem = { translationId ->
                    onSelectedTranslationIdsChange(setOf(translationId))
                    translationExpanded = false
                },
                onDismiss = { translationExpanded = false },
                showSearch = true,
                searchPlaceholder = "অনুবাদ খুঁজুন..."
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Tafsir Selection
        Text(
            text = "তাফসির নির্বাচন করুন",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        var tafsirExpanded by remember { mutableStateOf(false) }
        val selectedTafsir = availableTafsirs.find { selectedTafsirIds.contains(it.id.toString()) }?.name ?: "তাফসির নির্বাচন করুন"
        
        OutlinedButton(
            onClick = { tafsirExpanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        ) {
            Text(text = selectedTafsir, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
        }

        if (tafsirExpanded) {
            val selectionItems = availableTafsirs.map { tafsir ->
                com.example.ui.components.SelectionItem(
                    id = tafsir.id.toString(),
                    title = tafsir.name ?: "Unknown",
                    subtitle = tafsir.authorName ?: tafsir.languageName,
                    icon = Icons.Default.MenuBook
                )
            }
            com.example.ui.components.SmartSelectionDialog(
                title = "তাফসির নির্বাচন করুন",
                subtitle = "আপনার পছন্দের তাফসির গ্রন্থ বেছে নিন",
                headerIcon = Icons.Default.MenuBook,
                items = selectionItems,
                selectedId = selectedTafsirIds.firstOrNull() ?: "",
                onSelectItem = { tafsirId ->
                    onSelectedTafsirIdsChange(setOf(tafsirId))
                    tafsirExpanded = false
                },
                onDismiss = { tafsirExpanded = false },
                showSearch = true,
                searchPlaceholder = "তাফসির খুঁজুন..."
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        // Arabic Font Style Selection
        Text(
            text = "আরবি ফন্ট নির্বাচন করুন",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(com.example.ui.theme.arabicFontsList) { font ->
                val isSelected = font == arabicFontName
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGreen else Border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onArabicFontNameChange(font) }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "الحمد لله",
                        color = if (isSelected) White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        fontFamily = com.example.ui.theme.getArabicFont(font)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bengali Font Style Selection
        Text(
            text = "বাংলা ফন্ট নির্বাচন করুন",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(com.example.ui.theme.bengaliFontsList) { font ->
                val isSelected = font == bengaliFontName
                val displayName = when(font) {
                    "SolaimanLipi" -> "সলাইমান লিপি"
                    "Hind Siliguri" -> "হিন্দ শিলিগুড়ি"
                    "Shorif Shishir" -> "শরীফ শিশির"
                    else -> font
                }
                Box(
                    modifier = Modifier
                        .height(56.dp)
                        .background(
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGreen else Border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onBengaliFontNameChange(font) }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayName,
                        color = if (isSelected) White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontFamily = com.example.ui.theme.getBengaliFont(font)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Tanzil Quran Script Style Selection
        Text(
            text = "কুরআন স্ক্রিপ্ট স্টাইল নির্বাচন করুন",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        val scriptOptions = listOf(
            Pair("default-indopak", "ডিফল্ট (Indo-Pak)"),
            Pair("quran-uthmani", "উসমানী স্ক্রিপ্ট"),
            Pair("quran-simple", "সহজ স্ক্রিপ্ট"),
            Pair("quran-simple-clean", "হরকত ছাড়া ক্লিন"),
            Pair("quran-simple-plain", "প্লেইন স্ক্রিপ্ট")
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(scriptOptions) { (styleId, styleName) ->
                val isSelected = styleId == tanzilTextStyle
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            if (isSelected) PrimaryGreen else Border,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onTanzilTextStyleChange(styleId) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = styleName,
                        color = if (isSelected) White else MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
        Spacer(modifier = Modifier.height(16.dp))

        // Offline Download Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "অফলাইন ডাউনলোড",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (isDownloadingOffline) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF06B6D4).copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFF06B6D4).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = downloadStatus ?: "অডিও ও ডাটা ফাইল ডাউনলোড হচ্ছে...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ডাউনলোড অগ্রগতি:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$downloadProgress%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF06B6D4),
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancelDownloadClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("ডাউনলোড বাতিল করুন", fontSize = 12.sp)
                }
            }
        } else {
            downloadStatus?.let { status ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = status,
                        color = PrimaryGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            downloadError?.let { err ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "ত্রুটি: $err",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onDownloadClick,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF06B6D4)),
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("সম্পূর্ণ সুরার অফলাইন অডিও ও ডাটা নামান", fontSize = 12.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun MushafPageView(
    page: Int,
    ayahs: List<CombinedAyah>,
    surahNumber: Int,
    onPlayWord: (String) -> Unit,
    onPlayAyah: (CombinedAyah) -> Unit,
    arabicFontName: String = "Me Quran",
    arabicFontSize: Float = 28f,
    currentPlayingWordUrl: String? = null,
    currentPlayingAyahNumber: Int? = null,
    highlightedAyahNumber: Int? = null,
    isPlaying: Boolean = false,
    arabicLineSpacing: Float = 2.0f,
    tanzilTextStyle: String = "default-indopak"
) {
    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    
    var annotatedString by remember { mutableStateOf<androidx.compose.ui.text.AnnotatedString?>(null) }
    
    LaunchedEffect(ayahs, surahNumber, currentPlayingWordUrl, currentPlayingAyahNumber, isPlaying, tanzilTextStyle, highlightedAyahNumber) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            val result = buildAnnotatedString {
                ayahs.forEachIndexed { index, ayah ->
                    val ayahStart = length
                    val isAyahPlaying = (isPlaying && currentPlayingAyahNumber == ayah.numberInSurah) || (highlightedAyahNumber == ayah.numberInSurah)
                    
                    if (ayah.words.isNotEmpty() && tanzilTextStyle != "quran-simple-clean" && tanzilTextStyle != "quran-simple-plain") {
                        val processedWords = mutableListOf<ProcessedWord>()
                        ayah.words.forEach { word ->
                            val text = word.textUthmani ?: ""
                            val isPause = word.charTypeName == "pause" || 
                                          word.charTypeName == "stop" ||
                                          text.trim() in listOf("ۖ", "ۗ", "ۚ", "ۛ", "ۜ", "ۘ", "ۙ", "ج", "لا", "صلى", "صلے", " his", "qaly", "qala", "صلے", "কুত", "صلى", "صلے", "قلے", "قلى")
                            
                            if (isPause) {
                                val lastWordIndex = processedWords.indexOfLast { it.charTypeName == "word" }
                                if (lastWordIndex != -1) {
                                    val lastWord = processedWords[lastWordIndex]
                                    processedWords[lastWordIndex] = lastWord.copy(
                                        textUthmani = lastWord.textUthmani + text
                                    )
                                } else {
                                    processedWords.add(ProcessedWord(word.id, word.position, "word", text, word.translation?.text, word.audioUrl))
                                }
                            } else {
                                processedWords.add(ProcessedWord(word.id, word.position, word.charTypeName, text, word.translation?.text, word.audioUrl))
                            }
                        }
                        
                        processedWords.forEachIndexed { wIndex, word ->
                            if (word.charTypeName != "end") {
                                val wordStart = length
                                val rawAudioUrl = word.audioUrl
                                val url = if (!rawAudioUrl.isNullOrEmpty()) {
                                    if (rawAudioUrl.startsWith("http://") || rawAudioUrl.startsWith("https://")) {
                                        rawAudioUrl
                                    } else if (rawAudioUrl.startsWith("//")) {
                                        "https:$rawAudioUrl"
                                    } else {
                                        "https://audio.qurancdn.com/$rawAudioUrl"
                                    }
                                } else {
                                    String.format(
                                        java.util.Locale.US,
                                        "https://audio.qurancdn.com/wbw/%03d_%03d_%03d.mp3",
                                        surahNumber,
                                        ayah.numberInSurah,
                                        word.position
                                    )
                                }
                                val isHighlighted = url == currentPlayingWordUrl
                                
                                if (isHighlighted) {
                                    withStyle(
                                        style = SpanStyle(
                                            background = PrimaryGreen.copy(alpha = 0.25f),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    ) {
                                        append(word.textUthmani)
                                    }
                                } else {
                                    append(word.textUthmani)
                                }
                                
                                val wordEnd = length
                                
                                addStringAnnotation(
                                    tag = "word_url",
                                    annotation = url,
                                    start = wordStart,
                                    end = wordEnd
                                )
                                
                                if (wIndex < processedWords.lastIndex) {
                                    append(" ")
                                }
                            }
                        }
                    } else {
                        // Fallback to full Arabic text if words are empty
                        append(ayah.arabicText)
                    }
                    
                    val inlineId = "mushaf_circle_${ayah.surahNumber}_${ayah.numberInSurah}"
                    append(" ")
                    appendInlineContent(inlineId, " [${ayah.numberInSurah}]")
                    
                    // Add a space between ayahs
                    if (index < ayahs.lastIndex) {
                        append("  ")
                    }
                    
                    val ayahEnd = length
                    
                    if (isAyahPlaying) {
                        addStyle(
                            style = SpanStyle(
                                background = PrimaryGreen.copy(alpha = 0.12f)
                            ),
                            start = ayahStart,
                            end = ayahEnd
                        )
                    }
                    
                    // Add base ayah_index annotation covering the entire ayah text, circle, and spacing
                    addStringAnnotation(
                        tag = "ayah_index",
                        annotation = index.toString(),
                        start = ayahStart,
                        end = ayahEnd
                    )
                }
            }
            annotatedString = result
        }
    }
    
    if (annotatedString == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            com.example.ui.components.QuranLoadingAnimation(text = "")
        }
        return
    }
    
    val circleSizeDp = (arabicFontSize * 0.9f).coerceIn(24f, 36f)
    val circleSize = circleSizeDp.dp
    val circleFontSize = (arabicFontSize * 0.42f).coerceIn(11f, 16f).sp

    val inlineContentMap = remember(ayahs, arabicFontSize) {
        val map = mutableMapOf<String, InlineTextContent>()
        ayahs.forEach { ayah ->
            val inlineId = "mushaf_circle_${ayah.surahNumber}_${ayah.numberInSurah}"
            map[inlineId] = InlineTextContent(
                placeholder = Placeholder(
                    width = (circleSizeDp + 6).sp,
                    height = circleSizeDp.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                )
            ) {
                com.example.ui.components.AyahNumberCircle(
                    number = ayah.numberInSurah,
                    size = circleSize,
                    backgroundColor = Color.Transparent,
                    borderColor = MaterialTheme.colorScheme.onSurface,
                    textColor = MaterialTheme.colorScheme.onSurface,
                    fontSize = circleFontSize
                )
            }
        }
        map
    }

    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
            Text(
                text = annotatedString!!,
                inlineContent = inlineContentMap,
                fontSize = arabicFontSize.sp,
                lineHeight = (arabicFontSize * arabicLineSpacing).sp,
                fontFamily = arabicFont,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .pointerInput(annotatedString) {
                        detectTapGestures { offset ->
                            val currentLayoutResult = layoutResult ?: return@detectTapGestures
                            val charIndex = currentLayoutResult.getOffsetForPosition(offset)
                            
                            // Check if word_url annotation exists at click offset
                            val wordUrlAnnotations = annotatedString!!.getStringAnnotations(
                                tag = "word_url",
                                start = charIndex,
                                end = charIndex
                            )
                            
                            if (wordUrlAnnotations.isNotEmpty()) {
                                onPlayWord(wordUrlAnnotations.first().item)
                            } else {
                                // Fallback: check if ayah_index annotation exists
                                val ayahAnnotations = annotatedString!!.getStringAnnotations(
                                    tag = "ayah_index",
                                    start = charIndex,
                                    end = charIndex
                                )
                                if (ayahAnnotations.isNotEmpty()) {
                                    val idx = ayahAnnotations.first().item.toIntOrNull()
                                    if (idx != null && idx in ayahs.indices) {
                                        onPlayAyah(ayahs[idx])
                                    }
                                }
                            }
                        }
                    },
                onTextLayout = { layoutResult = it }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Page Divider
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), modifier = Modifier.fillMaxWidth())
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "পৃষ্ঠা $page",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

fun Int.toArabicNumerals(): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    return this.toString().map { if (it.isDigit()) arabicDigits[it - '0'] else it }.joinToString("")
}

@Composable
fun AyahCircle(
    number: Int,
    fontSize: Float = 28f,
    color: Color = PrimaryGreen,
    modifier: Modifier = Modifier
) {
    AyahNumberCircle(
        number = number,
        size = (fontSize * 0.95f).coerceIn(28f, 36f).dp,
        backgroundColor = Color.Transparent,
        borderColor = color,
        textColor = color,
        fontSize = (fontSize * 0.45f).coerceIn(11f, 16f).sp,
        modifier = modifier
    )
}

@Composable
fun AyahInlineText(
    arabicText: String,
    ayahNumber: Int,
    fontSize: Float,
    fontFamily: androidx.compose.ui.text.font.FontFamily,
    color: Color,
    modifier: Modifier = Modifier,
    lineSpacing: Float = 2.0f,
    arabicFontName: String = "Me Quran",
    showTajweed: Boolean = false
) {
    val inlineContentId = "ayah_circle_$ayahNumber"
    val annotatedText = androidx.compose.ui.text.buildAnnotatedString {
        if (showTajweed) {
            val tajweedParsed = com.example.utils.IndoPakTajweedParser.parseIndoPakTajweed(
                text = arabicText,
                defaultColor = color,
                fontSize = fontSize,
                showWaqfSigns = true,
                arabicFontName = arabicFontName
            )
            append(tajweedParsed)
        } else {
            appendStyledWaqfText(arabicText, fontSize, showWaqfSigns = true, arabicFontName = arabicFontName)
        }
        append(" ")
        appendInlineContent(inlineContentId, "[$ayahNumber]")
    }

    val circleSizeDp = (fontSize * 0.9f).coerceIn(24f, 36f)
    val circleSize = circleSizeDp.dp
    val circleFontSize = (fontSize * 0.42f).coerceIn(11f, 16f).sp

    val inlineContent = mapOf(
        inlineContentId to InlineTextContent(
            placeholder = Placeholder(
                width = (circleSizeDp + 6).sp,
                height = circleSizeDp.sp,
                placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
            )
        ) {
            AyahNumberCircle(
                number = ayahNumber,
                size = circleSize,
                backgroundColor = Color.Transparent,
                borderColor = color,
                textColor = color,
                fontSize = circleFontSize
            )
        }
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = annotatedText,
            inlineContent = inlineContent,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * lineSpacing).sp,
            fontFamily = fontFamily,
            color = color,
            textAlign = TextAlign.Right,
            modifier = modifier
        )
    }
}

@Composable
private fun AyahActionButtonsRow(
    ayah: CombinedAyah,
    surahName: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showShareMenu by remember { mutableStateOf(false) }
    var showPhotoCardDialog by remember { mutableStateOf(false) }
    
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
            OutlinedButton(
                onClick = { com.example.utils.AyahShareUtil.copyToClipboard(context, ayah, surahName) },
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
                        imageVector = Icons.Default.ContentCopy,
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
                OutlinedButton(
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
                            imageVector = Icons.Default.Share,
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
                DropdownMenu(
                    expanded = showShareMenu,
                    onDismissRequest = { showShareMenu = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
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
                            com.example.utils.AyahShareUtil.shareAsText(context, ayah, surahName)
                        }
                    )
                    
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
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
                            com.example.utils.AyahShareUtil.shareAsImage(context, ayah, surahName)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = "Photo Card Maker",
                                    tint = PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "ফটো কার্ড তৈরি করুন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            showShareMenu = false
                            showPhotoCardDialog = true
                        }
                    )
                }
            }
        }

        if (showPhotoCardDialog) {
            val quickCardPost = remember(ayah, surahName) {
                com.example.utils.PhotoCardBridge.fromAyah(ayah, surahName).toShortPost()
            }
            com.example.ui.screens.PhotoCardCustomizerDialog(
                post = quickCardPost,
                onDismiss = { showPhotoCardDialog = false }
            )
        }
    }
}
