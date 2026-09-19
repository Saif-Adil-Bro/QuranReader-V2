package com.example.ui.screens

import com.example.ui.components.AyahOptionsBottomSheet
import com.example.ui.components.quranPageSlideTransition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.List
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CombinedAyah
import com.example.data.model.removeWaqfSigns
import com.example.data.model.formatWaqfSigns
import com.example.data.model.appendStyledWaqfText
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.text.ClickableText
import com.example.ui.state.UiState
import com.example.ui.theme.getArabicFont
import com.example.ui.viewmodels.HafeziModeViewModel
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HafeziModeScreen(
    viewModel: HafeziModeViewModel,
    initialPage: Int,
    onNavigateBack: () -> Unit
) {
    // Initialize the viewmodel's current page to initialPage on first composition
    remember(initialPage) {
        viewModel.updateActivePage(initialPage)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isPageMemorized by viewModel.isPageMemorized.collectAsState()
    val currentPlayingAyahNumber by viewModel.currentPlayingAyahNumber.collectAsState()
    val repeatCount by viewModel.repeatCount.collectAsState()
    
    val currentPage by viewModel.currentPage.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val arabicFontName by viewModel.arabicFontName.collectAsState()
    val themeRaw by viewModel.theme.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val theme = when (themeRaw) {
        "System" -> if (isSystemDark) "Dark" else "Light"
        else -> themeRaw
    }
    val showWaqfSigns by viewModel.showWaqfSigns.collectAsState()
    val arabicLineSpacing by viewModel.arabicLineSpacing.collectAsState()
    val showTajweed by viewModel.showTajweed.collectAsState()
    val scrollDirection by viewModel.scrollDirection.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = (initialPage - 1).coerceIn(0, 609),
        pageCount = { 610 }
    )
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(
        initialFirstVisibleItemIndex = (initialPage - 1).coerceIn(0, 609)
    )

    if (scrollDirection == "Horizontal") {
        LaunchedEffect(pagerState.currentPage) {
            if (pagerState.isScrollInProgress) {
                val targetPage = pagerState.currentPage + 1
                if (targetPage != currentPage) {
                    viewModel.updateActivePage(targetPage)
                }
            }
        }
        LaunchedEffect(currentPage) {
            val targetIndex = currentPage - 1
            if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
                if (currentPage == 1 && initialPage != 1 && pagerState.currentPage == initialPage - 1) {
                    return@LaunchedEffect
                }
                pagerState.scrollToPage(targetIndex)
            }
        }
    } else {
        LaunchedEffect(lazyListState.firstVisibleItemIndex) {
            if (lazyListState.isScrollInProgress) {
                val targetPage = lazyListState.firstVisibleItemIndex + 1
                if (targetPage != currentPage) {
                    viewModel.updateActivePage(targetPage)
                }
            }
        }
        LaunchedEffect(currentPage) {
            val targetIndex = currentPage - 1
            if (targetIndex >= 0 && lazyListState.firstVisibleItemIndex != targetIndex) {
                if (currentPage == 1 && initialPage != 1 && lazyListState.firstVisibleItemIndex == initialPage - 1) {
                    return@LaunchedEffect
                }
                lazyListState.scrollToItem(targetIndex)
            }
        }
    }

    LaunchedEffect(scrollDirection) {
        val targetIndex = currentPage - 1
        if (targetIndex >= 0) {
            if (scrollDirection == "Horizontal") {
                if (pagerState.currentPage != targetIndex) {
                    pagerState.scrollToPage(targetIndex)
                }
            } else {
                if (lazyListState.firstVisibleItemIndex != targetIndex) {
                    lazyListState.scrollToItem(targetIndex)
                }
            }
        }
    }
    
    var showSettings by remember { mutableStateOf(false) }
    var showJuzList by remember { mutableStateOf(false) }
    var selectedAyahForOptions by remember { mutableStateOf<CombinedAyah?>(null) }
    var hafeziSelectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val hafeziJuzListState = rememberLazyListState()
    val hafeziSurahListState = rememberLazyListState()
    var showJumpToPageDialog by remember { mutableStateOf(false) }

    // Dynamic coloring based on user-selected reading theme
    val backgroundColor = when (theme) {
        "Dark" -> Color(0xFF121212)
        "Sepia" -> Color(0xFFFBF0D9)
        else -> Color(0xFFF8F6F0)
    }
    val containerColor = when (theme) {
        "Dark" -> Color(0xFF1E1E1E)
        "Sepia" -> Color(0xFFF1E4C3)
        else -> Color(0xFFEFECE4)
    }
    val topBarContentColor = when (theme) {
        "Dark" -> Color(0xFFE0E0E0)
        "Sepia" -> Color(0xFF5D4037)
        else -> Color(0xFF333333)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { showJumpToPageDialog = true }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "পৃষ্ঠা ${currentPage.toBengaliNumerals()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Jump to Page",
                            modifier = Modifier.size(20.dp),
                            tint = topBarContentColor
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showJuzList = true }) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Juz List",
                            tint = topBarContentColor
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) Color(0xFFE5A93C) else topBarContentColor
                        )
                    }
                    IconButton(onClick = { viewModel.toggleMemorized() }) {
                        Icon(
                            if (isPageMemorized) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                            contentDescription = "Mark Memorized",
                            tint = if (isPageMemorized) Color(0xFF1E5631) else topBarContentColor
                        )
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = topBarContentColor,
                    navigationIconContentColor = topBarContentColor,
                    actionIconContentColor = topBarContentColor
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = containerColor,
                contentColor = topBarContentColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { viewModel.nextPage() }, 
                        enabled = currentPage < 610,
                        colors = ButtonDefaults.textButtonColors(contentColor = topBarContentColor)
                    ) {
                        Text("পরবর্তী", fontWeight = FontWeight.Bold)
                    }
                    
                    FloatingActionButton(
                        onClick = {
                            if (isPlaying) {
                                viewModel.pauseAudio()
                            } else {
                                if (currentPlayingAyahNumber != null) {
                                    viewModel.resumeAudio()
                                } else {
                                    viewModel.playAudio()
                                }
                            }
                        },
                        containerColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                        contentColor = Color.White,
                        elevation = FloatingActionButtonDefaults.elevation(2.dp)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause"
                        )
                    }
                    
                    TextButton(
                        onClick = { viewModel.previousPage() }, 
                        enabled = currentPage > 1,
                        colors = ButtonDefaults.textButtonColors(contentColor = topBarContentColor)
                    ) {
                        Text("পূর্ববর্তী", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        if (scrollDirection == "Vertical") {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(610) { pageIndex ->
                    HafeziPageLoader(
                        pageNumber = pageIndex + 1,
                        viewModel = viewModel,
                        playingAyahNumber = currentPlayingAyahNumber,
                        arabicFontSize = arabicFontSize,
                        arabicFontName = arabicFontName,
                        theme = theme,
                        showWaqfSigns = showWaqfSigns,
                        arabicLineSpacing = arabicLineSpacing,
                        showTajweed = showTajweed,
                        isVerticalScrollEnabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 600.dp),
                        onAyahClick = { viewModel.playAyah(it) },
                        onAyahLongClick = { selectedAyahForOptions = it }
                    )
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(backgroundColor),
                reverseLayout = true
            ) { pageIndex ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .quranPageSlideTransition(pagerState, pageIndex)
                ) {
                    HafeziPageLoader(
                        pageNumber = pageIndex + 1,
                        viewModel = viewModel,
                        playingAyahNumber = currentPlayingAyahNumber,
                        arabicFontSize = arabicFontSize,
                        arabicFontName = arabicFontName,
                        theme = theme,
                        showWaqfSigns = showWaqfSigns,
                        arabicLineSpacing = arabicLineSpacing,
                        showTajweed = showTajweed,
                        isVerticalScrollEnabled = true,
                        onAyahClick = { viewModel.playAyah(it) },
                        onAyahLongClick = { selectedAyahForOptions = it }
                    )
                }
            }
        }
        
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = containerColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = "পঠন ও অডিও সেটিংস",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = topBarContentColor,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )

                    // Repeat count
                    Text(
                        text = "আয়াত পুনরাবৃত্তি: ${repeatCount.toBengaliNumerals()} বার", 
                        style = MaterialTheme.typography.bodyMedium,
                        color = topBarContentColor
                    )
                    Slider(
                        value = repeatCount.toFloat(),
                        onValueChange = { viewModel.setRepeatCount(it.toInt()) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                            activeTrackColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Font Size SettingAdjustmentRow
                    com.example.ui.components.SettingAdjustmentRow(
                        label = "আরবি হরফের আকার",
                        valueText = "${arabicFontSize.toInt()}".toBengaliNumerals(),
                        onDecrease = {
                            val newSize = (arabicFontSize - 1f).coerceIn(18f, 40f)
                            viewModel.setArabicFontSize(newSize)
                        },
                        onIncrease = {
                            val newSize = (arabicFontSize + 1f).coerceIn(18f, 40f)
                            viewModel.setArabicFontSize(newSize)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Arabic Line Spacing Settings
                    com.example.ui.components.SettingAdjustmentRow(
                        label = "আরবি লাইন স্পেস",
                        valueText = String.format("%.2f", arabicLineSpacing).toBengaliNumerals(),
                        onDecrease = {
                            val newSpacing = (arabicLineSpacing - 0.05f).coerceIn(2.00f, 3.00f)
                            viewModel.setArabicLineSpacing(newSpacing)
                        },
                        onIncrease = {
                            val newSpacing = (arabicLineSpacing + 0.05f).coerceIn(2.00f, 3.00f)
                            viewModel.setArabicLineSpacing(newSpacing)
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme selector
                    Text(
                        text = "থিম", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = topBarContentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Light" to "লাইট", "Sepia" to "সেপিয়া", "Dark" to "ডার্ক").forEach { (tKey, tName) ->
                            val isSel = tKey == theme
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) (if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)) else containerColor.copy(alpha = 0.5f),
                                contentColor = if (isSel) Color.White else topBarContentColor,
                                border = BorderStroke(1.dp, topBarContentColor.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setTheme(tKey) }
                            ) {
                                Text(
                                    text = tName,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scroll direction selector
                    Text(
                        text = "পৃষ্ঠা পরিবর্তন পদ্ধতি", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = topBarContentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Horizontal" to "ডানে-বামে", "Vertical" to "উপর-নিচ").forEach { (dKey, dName) ->
                            val isSel = dKey == scrollDirection
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) (if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)) else containerColor.copy(alpha = 0.5f),
                                contentColor = if (isSel) Color.White else topBarContentColor,
                                border = BorderStroke(1.dp, topBarContentColor.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setScrollDirection(dKey) }
                            ) {
                                Text(
                                    text = dName,
                                    textAlign = TextAlign.Center,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Waqf signs toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "থামার চিহ্ন প্রদর্শন (م، ج،صلے)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = topBarContentColor
                        )
                        Switch(
                            checked = showWaqfSigns,
                            onCheckedChange = { viewModel.setShowWaqfSigns(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = containerColor.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Tajweed Colors toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "তাজবীদ কালার",
                            style = MaterialTheme.typography.bodyMedium,
                            color = topBarContentColor
                        )
                        Switch(
                            checked = showTajweed,
                            onCheckedChange = { viewModel.setShowTajweed(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = containerColor.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Font style selector
                    Text(
                        text = "ফন্ট স্টাইল", 
                        style = MaterialTheme.typography.bodyMedium, 
                        color = topBarContentColor,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val fonts = listOf("Me Quran" to "মি কুরআন", "PDMS Saleem" to "পিডিএমএস সালীম", "Noorehira" to "নূরেহেরা")
                        fonts.forEach { (fKey, fName) ->
                            val isSel = fKey == arabicFontName
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) (if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)) else containerColor.copy(alpha = 0.5f),
                                contentColor = if (isSel) Color.White else topBarContentColor,
                                border = BorderStroke(1.dp, topBarContentColor.copy(alpha = 0.2f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setArabicFontName(fKey) }
                            ) {
                                Text(
                                    text = fName,
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showJuzList) {
            ModalBottomSheet(
                onDismissRequest = { showJuzList = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = containerColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    androidx.compose.material3.TabRow(
                        selectedTabIndex = hafeziSelectedTabIndex,
                        containerColor = containerColor,
                        contentColor = topBarContentColor,
                        indicator = { tabPositions ->
                            TabRowDefaults.Indicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[hafeziSelectedTabIndex]),
                                color = topBarContentColor
                            )
                        }
                    ) {
                        androidx.compose.material3.Tab(
                            selected = hafeziSelectedTabIndex == 0,
                            onClick = { hafeziSelectedTabIndex = 0 },
                            text = { Text("পারা", fontWeight = FontWeight.Bold) }
                        )
                        androidx.compose.material3.Tab(
                            selected = hafeziSelectedTabIndex == 1,
                            onClick = { hafeziSelectedTabIndex = 1 },
                            text = { Text("সূরা", fontWeight = FontWeight.Bold) }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        state = if (hafeziSelectedTabIndex == 0) hafeziJuzListState else hafeziSurahListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp)
                    ) {
                        if (hafeziSelectedTabIndex == 0) {
                            items(30, key = { it + 1 }) { index ->
                                val juzNum = index + 1
                                val juzName = paraNamesBangla[index]
                                val startPage = getJuzStartPage(juzNum)
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadPage(startPage)
                                            showJuzList = false
                                        }
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "পারা ${juzNum.toBengaliNumerals()}: $juzName",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = topBarContentColor
                                        )
                                        Text(
                                            text = "পৃষ্ঠা ${startPage.toBengaliNumerals()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = topBarContentColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Go to Juz",
                                        tint = topBarContentColor.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (index < 29) {
                                    HorizontalDivider(color = topBarContentColor.copy(alpha = 0.1f))
                                }
                            }
                        } else {
                            items(114, key = { it + 1 }) { index ->
                                val surahNum = index + 1
                                val surahName = com.example.data.QuranData.surahNames.find { it.first == surahNum }?.second?.first ?: ""
                                val surahNameArabic = ""
                                val startPage = com.example.data.QuranData.surahStartPages[index]
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.loadPage(startPage)
                                            showJuzList = false
                                        }
                                        .padding(vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "$surahNum. $surahName",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = topBarContentColor
                                        )
                                        Text(
                                            text = "পৃষ্ঠা ${startPage.toBengaliNumerals()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = topBarContentColor.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        text = surahNameArabic,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = getArabicFont(arabicFontName),
                                        color = topBarContentColor
                                    )
                                }
                                if (index < 113) {
                                    HorizontalDivider(color = topBarContentColor.copy(alpha = 0.1f))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        if (showJumpToPageDialog) {
            var pageInput by remember { mutableStateOf("") }
            var isError by remember { mutableStateOf(false) }
            
            AlertDialog(
                onDismissRequest = { showJumpToPageDialog = false },
                title = {
                    Text(
                        text = "পৃষ্ঠা পরিবর্তন করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = topBarContentColor
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "১ থেকে ৬০৪ এর মধ্যে পৃষ্ঠা নম্বর লিখুন:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = topBarContentColor.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { input ->
                                val englishInput = input.toEnglishNumerals()
                                val filtered = englishInput.filter { it.isDigit() }
                                pageInput = filtered
                                if (filtered.isNotEmpty()) {
                                    val num = filtered.toIntOrNull()
                                    isError = num == null || num !in 1..610
                                } else {
                                    isError = false
                                }
                            },
                            label = { Text("পৃষ্ঠা নম্বর") },
                            placeholder = { Text("যেমন: ১২৩") },
                            isError = isError,
                            supportingText = {
                                if (isError) {
                                    Text("অনুগ্রহ করে ১ থেকে ৬১০ এর মধ্যে একটি সঠিক নম্বর লিখুন", color = MaterialTheme.colorScheme.error)
                                }
                            },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                                focusedLabelColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                                cursorColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val englishInput = pageInput.toEnglishNumerals()
                            val num = englishInput.toIntOrNull()
                            if (num != null && num in 1..610) {
                                viewModel.loadPage(num)
                                showJumpToPageDialog = false
                            } else {
                                isError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                        )
                    ) {
                        Text("নিশ্চিত করুন")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showJumpToPageDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = topBarContentColor)
                    ) {
                        Text("বাতিল")
                    }
                },
                containerColor = containerColor
            )
        }

        // Ayah Options Bottom Sheet (Translation, Copy, Share, Tafsir)
        if (selectedAyahForOptions != null) {
            val ayahForOptions = selectedAyahForOptions!!
            AyahOptionsBottomSheet(
                ayah = ayahForOptions,
                isPlaying = isPlaying && currentPlayingAyahNumber == ayahForOptions.number,
                onPlayToggle = { ayah ->
                    if (isPlaying && currentPlayingAyahNumber == ayah.number) {
                        viewModel.pauseAudio()
                    } else {
                        viewModel.playAyah(ayah.number)
                    }
                },
                onDismiss = { selectedAyahForOptions = null },
                theme = theme,
                showTajweed = showTajweed,
                arabicFontName = arabicFontName,
                arabicFontSize = arabicFontSize
            )
        }
    }
}

@Composable
fun HafeziPageContent(
    ayahs: List<CombinedAyah>, 
    playingAyahNumber: Int?,
    arabicFontSize: Float,
    arabicFontName: String,
    theme: String,
    currentPage: Int,
    showWaqfSigns: Boolean = true,
    arabicLineSpacing: Float = 2.0f,
    showTajweed: Boolean = false,
    isVerticalScrollEnabled: Boolean = true,
    onAyahClick: (Int) -> Unit,
    onAyahLongClick: (CombinedAyah) -> Unit = {}
) {
    val arabicFont = if (showTajweed) com.example.ui.theme.getArabicFontForTajweed(arabicFontName) else getArabicFont(arabicFontName)
    val firstAyah = ayahs.firstOrNull()
    
    // Resolve Page Headers
    val surahData = firstAyah?.let { com.example.data.QuranData.surahNames.find { s -> s.first == it.surahNumber } }
    val surahNameArabic = surahData?.second?.first ?: "" // e.g. سورة الفাতحة
    val juzNum = firstAyah?.juz ?: 1
    val juzName = "পারা ${juzNum.toBengaliNumerals()}"

    val scrollModifier = if (isVerticalScrollEnabled) {
        Modifier.verticalScroll(rememberScrollState())
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(scrollModifier)
            .padding(16.dp)
    ) {
        TanzilMushafFrame(
            titleRight = surahNameArabic,
            titleLeft = juzName,
            pageNumber = currentPage.toBengaliNumerals(),
            theme = theme
        ) {
            // Flow the text in Right-to-Left (RTL) mode, completely avoiding bracket layout bugs
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                // Group ayahs on this page by their Surah
                val sections = remember(ayahs) {
                    val list = mutableListOf<Pair<Int, List<CombinedAyah>>>()
                    var currentSurahId = -1
                    var currentList = mutableListOf<CombinedAyah>()
                    for (ayah in ayahs) {
                        if (ayah.surahNumber != currentSurahId) {
                            if (currentList.isNotEmpty()) {
                                list.add(currentSurahId to currentList)
                            }
                            currentSurahId = ayah.surahNumber
                            currentList = mutableListOf()
                        }
                        currentList.add(ayah)
                    }
                    if (currentList.isNotEmpty()) {
                        list.add(currentSurahId to currentList)
                    }
                    list
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEach { (surahId, surahAyahs) ->
                        val firstAyahOfSurah = surahAyahs.firstOrNull()
                        
                        if (firstAyahOfSurah?.numberInSurah == 1) {
                            val surahInfo = com.example.data.surahInfoList.find { it.first == surahId }?.second
                            val arabicName = surahInfo?.arabicName ?: "سورة $surahId"
                            val ayahsArabic = com.example.data.QuranData.toArabicNumerals(surahInfo?.ayahCount ?: 0)
                            val rukusArabic = com.example.data.QuranData.toArabicNumerals(surahInfo?.rukuCount ?: 0)
                            
                            // Surah Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                                    .background(
                                        color = if (theme == "Dark") Color(0xFF2A2A2A) else Color(0xFFF0F0F0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (theme == "Dark") Color(0xFF404040) else Color(0xFFE0E0E0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = arabicName,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = arabicFont,
                                        color = if (theme == "Dark") Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "آيات: $ayahsArabic  |  ركوع: $rukusArabic",
                                        fontSize = 14.sp,
                                        fontFamily = arabicFont,
                                        color = if (theme == "Dark") Color(0xFFA0A0A0) else Color(0xFF606060)
                                    )
                                }
                            }

                            if (surahId != 1 && surahId != 9) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        fontFamily = arabicFont,
                                        fontSize = (arabicFontSize * 1.15f).sp,
                                        color = when (theme) {
                                            "Dark" -> Color(0xFFE0E0E0)
                                            "Sepia" -> Color(0xFF4E342E)
                                            else -> Color(0xFF1A1A1A)
                                        },
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        val annotatedString = remember(surahAyahs, playingAyahNumber, theme, showWaqfSigns) {
                            buildAnnotatedString {
                                surahAyahs.forEachIndexed { index, ayah ->
                                    val start = length
                                    var textToDisplay = ayah.arabicText.trim()
                                    
                                    val prefixes = listOf(
                                        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ ",
                                        "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ ",
                                        "بِسْمِ اللَّهِ الرَّحْمَنِ الرَّحِيمِ",
                                        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ ",
                                        "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                        "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ ",
                                        "بِسْمِ اللهِ الرَّحْمٰنِ الرَّحِيْمِ",
                                        "بسم الله الرحمن الرحيم ",
                                        "بسم الله الرحمن الرحيم"
                                    )

                                    if (ayah.numberInSurah == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9) {
                                        for (prefix in prefixes) {
                                            if (textToDisplay.startsWith(prefix)) {
                                                textToDisplay = textToDisplay.removePrefix(prefix).trim()
                                                break
                                            }
                                        }
                                    }

                                    if (showTajweed) {
                                        val defaultTextColor = when (theme) {
                                            "Dark" -> Color(0xFFE0E0E0)
                                            "Sepia" -> Color(0xFF4E342E)
                                            else -> Color(0xFF1A1A1A)
                                        }
                                        val tajweedParsed = com.example.utils.IndoPakTajweedParser.parseIndoPakTajweed(
                                            text = textToDisplay,
                                            defaultColor = defaultTextColor,
                                            fontSize = arabicFontSize,
                                            showWaqfSigns = showWaqfSigns,
                                            arabicFontName = arabicFontName
                                        )
                                        append(tajweedParsed)
                                        val numInSurahStr = ayah.numberInSurah.toArabicNumerals()
                                        withStyle(androidx.compose.ui.text.SpanStyle(fontFamily = com.example.ui.theme.amiriFont)) {
                                            append(" ﴿$numInSurahStr﴾ ")
                                        }
                                    } else {
                                        appendStyledWaqfText(textToDisplay, arabicFontSize, showWaqfSigns, arabicFontName)
                                        val numInSurahStr = ayah.numberInSurah.toArabicNumerals()
                                        withStyle(androidx.compose.ui.text.SpanStyle(fontFamily = com.example.ui.theme.amiriFont)) {
                                            append(" ﴿$numInSurahStr﴾ ")
                                        }
                                    }
                                    
                                    val end = length
                                    
                                    addStringAnnotation(
                                        tag = "AYAH_NUMBER",
                                        annotation = ayah.number.toString(),
                                        start = start,
                                        end = end
                                    )

                                    // Highlight currently active/playing verse beautifully in green/accent shade
                                    if (ayah.number == playingAyahNumber) {
                                        addStyle(
                                            style = SpanStyle(
                                                background = if (theme == "Dark") Color(0xFF1E3524) else Color(0xFFE8F5E9),
                                                color = if (theme == "Dark") Color(0xFF81C784) else Color(0xFF1B5E20),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            start = start,
                                            end = end
                                        )
                                    }
                                    
                                    if (index < surahAyahs.lastIndex) {
                                        append(" ") // Visual negative space between consecutive verses
                                    }
                                }
                            }
                        }

                        var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }

                        Text(
                            text = annotatedString,
                            onTextLayout = { textLayoutResult = it },
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = arabicFontSize.sp,
                                lineHeight = (arabicFontSize * arabicLineSpacing).sp,
                                fontFamily = arabicFont,
                                color = when (theme) {
                                    "Dark" -> Color(0xFFE0E0E0)
                                    "Sepia" -> Color(0xFF4E342E)
                                    else -> Color(0xFF1A1A1A)
                                },
                                textAlign = TextAlign.Justify
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .pointerInput(annotatedString) {
                                    detectTapGestures(
                                        onTap = { offset ->
                                            textLayoutResult?.let { currentLayout ->
                                                val charIndex = currentLayout.getOffsetForPosition(offset)
                                                annotatedString.getStringAnnotations(tag = "AYAH_NUMBER", start = charIndex, end = charIndex)
                                                    .firstOrNull()?.let { annotation ->
                                                        annotation.item.toIntOrNull()?.let { ayahNumber ->
                                                            onAyahClick(ayahNumber)
                                                        }
                                                    }
                                            }
                                        },
                                        onLongPress = { offset ->
                                            textLayoutResult?.let { currentLayout ->
                                                val charIndex = currentLayout.getOffsetForPosition(offset)
                                                annotatedString.getStringAnnotations(tag = "AYAH_NUMBER", start = charIndex, end = charIndex)
                                                    .firstOrNull()?.let { annotation ->
                                                        annotation.item.toIntOrNull()?.let { ayahNumber ->
                                                            val targetAyah = ayahs.find { it.number == ayahNumber }
                                                            if (targetAyah != null) {
                                                                onAyahLongClick(targetAyah)
                                                            }
                                                        }
                                                    }
                                            }
                                        }
                                    )
                                }
                                .drawBehind {
                                    textLayoutResult?.let { layoutResult ->
                                        val lineCount = layoutResult.lineCount
                                        val lineColor = when (theme) {
                                            "Dark" -> Color.White.copy(alpha = 0.08f)
                                            "Sepia" -> Color(0xFF8B7355).copy(alpha = 0.15f)
                                            else -> Color(0xFF8B7355).copy(alpha = 0.12f)
                                        }
                                        val strokeWidth = 1.dp.toPx()
                                        for (i in 0 until lineCount) {
                                            val lineBottom = layoutResult.getLineBottom(i)
                                            drawLine(
                                                color = lineColor,
                                                start = androidx.compose.ui.geometry.Offset(0f, lineBottom - 2.dp.toPx()),
                                                end = androidx.compose.ui.geometry.Offset(size.width, lineBottom - 2.dp.toPx()),
                                                strokeWidth = strokeWidth
                                            )
                                        }
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
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

@Composable
fun TanzilMushafFrame(
    titleRight: String = "",
    titleLeft: String = "",
    pageNumber: String = "",
    theme: String = "Light",
    content: @Composable BoxScope.() -> Unit
) {
    // Elegant, authentic Mushaf border styling mimicking Tanzil.net
    val backgroundColor = when (theme) {
        "Dark" -> Color(0xFF1E1E1E)
        "Sepia" -> Color(0xFFF7EED6)
        else -> Color(0xFFFDFBF7)
    }
    val borderColor = when (theme) {
        "Dark" -> Color(0xFF5A493B)
        "Sepia" -> Color(0xFF8B6C4F)
        else -> Color(0xFF8B7355) // Antique gold border
    }
    val innerBorderColor = when (theme) {
        "Dark" -> Color(0xFF3E3127)
        "Sepia" -> Color(0xFFB59372)
        else -> Color(0xFFC5A059) // Thin golden guide lines
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        // Inner thin guiding border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, innerBorderColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            // Draw traditional corner accents at the four inner corners
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val thickness = 1.dp.toPx()
                val offset = 1.dp.toPx()
                val length = 6.dp.toPx()
                
                // Top-Left cornerTicks
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(offset, offset), androidx.compose.ui.geometry.Offset(offset + length, offset), thickness)
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(offset, offset), androidx.compose.ui.geometry.Offset(offset, offset + length), thickness)
                
                // Top-Right cornerTicks
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(w - offset, offset), androidx.compose.ui.geometry.Offset(w - offset - length, offset), thickness)
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(w - offset, offset), androidx.compose.ui.geometry.Offset(w - offset, offset + length), thickness)
                
                // Bottom-Left cornerTicks
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(offset, h - offset), androidx.compose.ui.geometry.Offset(offset + length, h - offset), thickness)
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(offset, h - offset), androidx.compose.ui.geometry.Offset(offset, h - offset - length), thickness)
                
                // Bottom-Right cornerTicks
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(w - offset, h - offset), androidx.compose.ui.geometry.Offset(w - offset - length, h - offset), thickness)
                drawLine(innerBorderColor, androidx.compose.ui.geometry.Offset(w - offset, h - offset), androidx.compose.ui.geometry.Offset(w - offset, h - offset - length), thickness)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Render Page Headers if they are provided
                if (titleRight.isNotEmpty() || titleLeft.isNotEmpty() || pageNumber.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = titleLeft,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = borderColor.copy(alpha = 0.85f)
                        )
                        if (pageNumber.isNotEmpty()) {
                            Text(
                                text = "— পৃষ্ঠা $pageNumber —",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = borderColor.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = titleRight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = borderColor.copy(alpha = 0.85f)
                        )
                    }
                    HorizontalDivider(
                        color = innerBorderColor.copy(alpha = 0.35f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Main Content inside the frame
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }
            }
        }
    }
}

fun String.toEnglishNumerals(): String {
    val englishNumerals = "0123456789"
    val bengaliNumerals = "০১২৩৪৫৬৭৮৯"
    return this.map { char ->
        val index = bengaliNumerals.indexOf(char)
        if (index != -1) englishNumerals[index] else char
    }.joinToString("")
}

@Composable
fun HafeziPageLoader(
    pageNumber: Int,
    viewModel: HafeziModeViewModel,
    playingAyahNumber: Int?,
    arabicFontSize: Float,
    arabicFontName: String,
    theme: String,
    showWaqfSigns: Boolean,
    arabicLineSpacing: Float,
    showTajweed: Boolean,
    isVerticalScrollEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    onAyahClick: (Int) -> Unit,
    onAyahLongClick: (CombinedAyah) -> Unit = {}
) {
    var pageData by remember(pageNumber) { mutableStateOf<List<CombinedAyah>?>(null) }
    var isLoading by remember(pageNumber) { mutableStateOf(true) }
    var errorMsg by remember(pageNumber) { mutableStateOf<String?>(null) }

    LaunchedEffect(pageNumber) {
        isLoading = true
        errorMsg = null
        try {
            val data = viewModel.getPageDetails(pageNumber)
            pageData = data
            isLoading = false
        } catch (e: Exception) {
            errorMsg = e.message ?: "Failed to load Page"
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(
                modifier = modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                com.example.ui.components.QuranLoadingAnimation(
                    text = "পৃষ্ঠা ${pageNumber.toBengaliNumerals()} লোড হচ্ছে...", 
                    color = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                )
            }
        }
        errorMsg != null -> {
            Column(
                modifier = modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = errorMsg!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        // Retry loading
                        pageData = null
                        errorMsg = null
                        isLoading = true
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                    )
                ) {
                    Text("আবার চেষ্টা করুন", color = Color.White)
                }
            }
        }
        pageData != null -> {
            Box(modifier = modifier) {
                HafeziPageContent(
                    ayahs = pageData!!,
                    playingAyahNumber = playingAyahNumber,
                    arabicFontSize = arabicFontSize,
                    arabicFontName = arabicFontName,
                    theme = theme,
                    currentPage = pageNumber,
                    showWaqfSigns = showWaqfSigns,
                    arabicLineSpacing = arabicLineSpacing,
                    showTajweed = showTajweed,
                    isVerticalScrollEnabled = isVerticalScrollEnabled,
                    onAyahClick = onAyahClick,
                    onAyahLongClick = onAyahLongClick
                )
            }
        }
    }
}
