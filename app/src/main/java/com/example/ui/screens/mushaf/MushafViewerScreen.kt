package com.example.ui.screens.mushaf

import com.example.ui.components.quranPageSlideTransition
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextAlign
import com.example.ui.screens.mushaf.components.PageViewer
import com.example.ui.viewmodels.MushafViewerViewModel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import android.content.Intent
import com.example.data.model.CombinedAyah
import com.example.ui.components.parseHtmlToAnnotatedString
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog
import com.example.utils.DateUtil
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MushafViewerScreen(
    mushafId: String,
    initialPage: Int = 1,
    initialShowIndex: Boolean = false,
    onBack: () -> Unit,
    viewModel: MushafViewerViewModel
) {
    remember(mushafId, initialPage) {
        viewModel.initMushaf(mushafId, initialPage)
        true
    }

    val currentPage by viewModel.currentPageNumber.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val pagePath by viewModel.currentPagePath.collectAsState()
    val isPdf by viewModel.isPdf.collectAsState()
    val pdfPageOffset by viewModel.pdfPageOffset.collectAsState()
    val totalPages by viewModel.totalPages.collectAsState()
    val isReady by viewModel.isReady.collectAsState()
    val isDownloaded by viewModel.isDownloaded.collectAsState()
    val currentTheme by viewModel.theme.collectAsState()
    val scrollDirection by viewModel.scrollDirection.collectAsState()
    val pageHeightScale by viewModel.pageHeightScale.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDark = when (currentTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemDark
    }
    val isVertical = scrollDirection == "Vertical"
    
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val pageAyahs by viewModel.pageAyahs.collectAsState()
    val selectedAyah by viewModel.selectedAyah.collectAsState()
    val isLoadingAyahs by viewModel.isLoadingAyahs.collectAsState()
    val isAudioPlaying by viewModel.isAudioPlaying.collectAsState()
    val currentPlayingAyahNum by viewModel.currentPlayingAyahNumber.collectAsState()

    var showAyahSheet by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "infoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    var showSelectorSheet by remember { mutableStateOf(initialShowIndex) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 for Surah, 1 for Para
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val surahListState = rememberLazyListState()
    val juzListState = rememberLazyListState()

    val pagerState = rememberPagerState(initialPage = initialPage - 1, pageCount = { totalPages })
    var showOffsetDialog by remember { mutableStateOf(false) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) {
        val newPage = pagerState.currentPage + 1
        if (newPage != currentPage) {
            viewModel.jumpToPage(newPage)
            viewModel.prefetchPages(mushafId, newPage)
        }
    }
    
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage + 1 != currentPage) {
            pagerState.scrollToPage(currentPage - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "পৃষ্ঠা ${com.example.utils.DateUtil.toBengaliNumerals(currentPage)}", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 18.sp,
                        color = Color(0xFF10B981)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.loadPageAyahs(currentPage)
                        showAyahSheet = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "অনুবাদ ও তাফসীর",
                            tint = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleBookmark() }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark Page",
                            tint = if (isBookmarked) Color(0xFFE5A93C) else Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = { 
                        showSelectorSheet = true 
                    }) {
                        Icon(
                            imageVector = Icons.Default.List, 
                            contentDescription = "সূচী", 
                            tint = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = { showSettingsSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "পড়ার সেটিংস",
                            tint = Color(0xFF10B981)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) Color(0xFF1A1A1A) else Color.White,
                    titleContentColor = Color(0xFF10B981),
                    navigationIconContentColor = if (isDark) Color.White else Color.Black
                )
            )
        },
        containerColor = if (isDark) Color.Black else Color.White
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color.White)
                .padding(paddingValues)
        ) {
            if (!isReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else if (isVertical) {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val pageNum = page + 1
                    OnDemandPageViewer(
                        mushafId = mushafId,
                        pageNumber = pageNum,
                        pdfPageOffset = pdfPageOffset,
                        pageHeightScale = pageHeightScale,
                        isDark = isDark,
                        viewModel = viewModel
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    // Quran is read right-to-left
                    
                ) { page ->
                    val pageNum = page + 1
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .quranPageSlideTransition(pagerState, page)
                    ) {
                        OnDemandPageViewer(
                            mushafId = mushafId,
                            pageNumber = pageNum,
                            pdfPageOffset = pdfPageOffset,
                            pageHeightScale = pageHeightScale,
                            isDark = isDark,
                            viewModel = viewModel
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        viewModel.loadPageAyahs(currentPage)
                        showAyahSheet = true
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "অনুবাদ ও তাফসীর",
                    tint = Color(0xFF10B981),
                    modifier = Modifier
                        .size(20.dp)
                        .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale)
                )
                Text(
                    text = "অনুবাদ ও তাফসীর",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }

    if (showAyahSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAyahSheet = false },
            containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
            scrimColor = Color.Black.copy(alpha = 0.5f)
        ) {
            AyahSelectionSheetContent(
                currentPage = currentPage,
                pageAyahs = pageAyahs,
                selectedAyah = selectedAyah,
                isLoading = isLoadingAyahs,
                isPlaying = isAudioPlaying,
                currentPlayingAyahNum = currentPlayingAyahNum,
                isDark = isDark,
                onSelectAyah = { viewModel.selectAyah(it) },
                onTogglePlayPause = { viewModel.togglePlayPauseAyah(it) },
                onClose = { showAyahSheet = false }
            )
        }
    }

    if (showSelectorSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSelectorSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                var selectedParaIndex by remember { mutableStateOf(0) }
                var pageInput by remember { mutableStateOf("") }
                var expandedPara by remember { mutableStateOf(false) }

                val paras = remember {
                    (1..30).map { paraNum ->
                        val paraName = "${com.example.utils.DateUtil.toBengaliNumerals(paraNum)} পারা"
                        Pair(paraNum, paraName)
                    }
                }

                fun getParaPageCount(para: Int): Int {
                    return when (para) {
                        1 -> 21
                        29 -> 24
                        30 -> 25
                        else -> 20
                    }
                }

                fun getParaStartPage(para: Int): Int {
                    var startPage = 1
                    for (i in 1 until para) {
                        startPage += getParaPageCount(i)
                    }
                    return startPage
                }

                val selectedParaNum = paras[selectedParaIndex].first
                val maxPagesInPara = getParaPageCount(selectedParaNum)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "সূচী",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কুরআন সূচী",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                    IconButton(onClick = { showSelectorSheet = false }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = if (isDark) Color.LightGray else Color.Gray
                        )
                    }
                }

                // Jump to page setting row in place of scroll settings
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "নির্দিষ্ট পৃষ্ঠায় যান:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Para Selector
                        Box(modifier = Modifier.weight(0.45f)) {
                            OutlinedButton(
                                onClick = { expandedPara = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D2D) else Color(0xFFE5E7EB)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = paras[selectedParaIndex].second,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedPara,
                                onDismissRequest = { expandedPara = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .heightIn(max = 200.dp)
                                    .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                            ) {
                                paras.forEachIndexed { idx, item ->
                                    DropdownMenuItem(
                                        text = { Text(item.second, color = if (isDark) Color.White else Color.Black, fontSize = 13.sp) },
                                        onClick = {
                                            selectedParaIndex = idx
                                            pageInput = ""
                                            expandedPara = false
                                        }
                                    )
                                }
                            }
                        }

                        // Page Input
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it in '০'..'৯' }
                                if (filtered.length <= 2) {
                                    val converted = filtered.map { char ->
                                        if (char in '0'..'9') (char - '0' + '০'.code).toChar() else char
                                    }.joinToString("")
                                    pageInput = converted
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            placeholder = { Text("১-${com.example.utils.DateUtil.toBengaliNumerals(maxPagesInPara)} পৃষ্ঠা", color = Color.Gray, fontSize = 12.sp) },
                            modifier = Modifier.weight(0.35f).height(48.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFE5E7EB),
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black
                            )
                        )

                        // Go Button
                        Button(
                            onClick = {
                                val cleanInputVal = pageInput.map { char ->
                                    if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
                                }.joinToString("")
                                val targetParaPage = cleanInputVal.toIntOrNull()
                                if (targetParaPage != null && targetParaPage in 1..maxPagesInPara) {
                                    val targetPage = getParaStartPage(selectedParaNum) + targetParaPage - 1
                                    if (targetPage in 1..totalPages) {
                                        showSelectorSheet = false
                                        viewModel.jumpToPage(targetPage)
                                    } else {
                                        android.widget.Toast.makeText(context, "সঠিক পৃষ্ঠা নম্বর লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val maxPagesBengali = com.example.utils.DateUtil.toBengaliNumerals(maxPagesInPara)
                                    android.widget.Toast.makeText(context, "১ থেকে $maxPagesBengali এর মধ্যে পৃষ্ঠা নম্বর লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(0.2f).height(48.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("যাও", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                 TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFF10B981)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; searchQuery = "" },
                        text = {
                            Text(
                                text = "সূরা সূচী",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (selectedTab == 0) Color(0xFF10B981) else (if (isDark) Color.LightGray else Color.Gray)
                            )
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; searchQuery = "" },
                        text = {
                            Text(
                                text = "পারা সূচী",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (selectedTab == 1) Color(0xFF10B981) else (if (isDark) Color.LightGray else Color.Gray)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { 
                        Text(
                            text = if (selectedTab == 0) "সূরা খুঁজুন..." else "পারা খুঁজুন...",
                            color = if (isDark) Color.Gray else Color.LightGray
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.4f),
                        cursorColor = Color(0xFF10B981),
                        focusedTextColor = if (isDark) Color.White else Color.Black,
                        unfocusedTextColor = if (isDark) Color.White else Color.Black,
                        focusedPlaceholderColor = if (isDark) Color.Gray else Color.LightGray,
                        unfocusedPlaceholderColor = if (isDark) Color.Gray else Color.LightGray
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    val filteredSurahs = remember(searchQuery) {
                        com.example.data.QuranData.surahNames.filter { surah ->
                            val num = surah.first.toString()
                            val name = surah.second.first
                            val meaning = surah.second.second
                            num.contains(searchQuery) || name.contains(searchQuery) || meaning.contains(searchQuery)
                        }
                    }

                    LazyColumn(
                        state = surahListState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredSurahs, key = { it.first }) { surah ->
                            val surahNum = surah.first
                            val name = surah.second.first
                            val meaning = surah.second.second
                            val startPage = com.example.data.QuranData.surahStartPages[surahNum - 1]
                            val isCurrent = currentPage in startPage..(if (surahNum < 114) com.example.data.QuranData.surahStartPages[surahNum] - 1 else totalPages)
                            
                            val cardBgColor = if (isDark) {
                                if (isCurrent) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF2D2D2D)
                            } else {
                                if (isCurrent) Color(0xFFECFDF5) else Color(0xFFF3F4F6)
                            }
                            val cardBorder = if (isCurrent) {
                                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0))
                            } else {
                                if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
                            }
                            
                            Surface(
                                onClick = {
                                    viewModel.jumpToPage(startPage)
                                    showSelectorSheet = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = cardBgColor,
                                border = cardBorder
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF10B981), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = surahNum.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = if (isDark) Color.White else Color(0xFF1F2937)
                                            )
                                            Text(
                                                text = meaning,
                                                fontSize = 12.sp,
                                                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "পৃষ্ঠা $startPage",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedTab == 1) {
                    val filteredParas = remember(searchQuery) {
                        com.example.data.QuranData.juzList.filter { juz ->
                            val num = juz.first.toString()
                            val name = juz.second
                            num.contains(searchQuery) || name.contains(searchQuery)
                        }
                    }

                    LazyColumn(
                        state = juzListState,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(filteredParas, key = { it.first }) { juz ->
                            val juzNum = juz.first
                            val name = juz.second
                            val isHafezi = (mushafId == "hafizi_15line" || mushafId == "imdadia_hafezi" || mushafId == "custom_pdf")
                            val startPage = if (isHafezi) {
                                com.example.data.HafeziQuranData.getParaStartPage(juzNum, 1)
                            } else {
                                juz.third
                            }
                            val endPage = if (isHafezi) {
                                if (juzNum < 30) com.example.data.HafeziQuranData.getParaStartPage(juzNum + 1, 1) - 1 else totalPages
                            } else {
                                if (juzNum < 30) com.example.data.QuranData.juzList[juzNum].third - 1 else totalPages
                            }
                            val isCurrent = currentPage in startPage..endPage
                            
                            val cardBgColor = if (isDark) {
                                if (isCurrent) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFF2D2D2D)
                            } else {
                                if (isCurrent) Color(0xFFECFDF5) else Color(0xFFF3F4F6)
                            }
                            val cardBorder = if (isCurrent) {
                                androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF10B981) else Color(0xFFA7F3D0))
                            } else {
                                if (isDark) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)) else null
                            }

                            Surface(
                                onClick = {
                                    viewModel.jumpToPage(startPage)
                                    showSelectorSheet = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = cardBgColor,
                                border = cardBorder
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF059669), shape = CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = juzNum.toString(),
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isDark) Color.White else Color(0xFF1F2937)
                                        )
                                    }
                                    Text(
                                        text = "পৃষ্ঠা $startPage",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showOffsetDialog) {
        AlertDialog(
            onDismissRequest = { showOffsetDialog = false },
            title = {
                Text(
                    text = "সূরা ফাতিহা পৃষ্ঠা সমন্বয়",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF10B981)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "আপনার পিডিএফ কিতাবের কত নম্বর পাতায় সূরা ফাতিহা বা মূল কুরআন শুরু হয়েছে তা সিলেক্ট করুন। এর পর অফসেট স্বয়ংক্রিয়ভাবে সমন্বয় হয়ে যাবে।",
                        fontSize = 13.sp,
                        color = if (isDark) Color.LightGray else Color.DarkGray
                    )
                    
                    val currentFatihahPage = pdfPageOffset + 1
                    
                    Text(
                        text = "সূরা ফাতিহা এর পৃষ্ঠা নম্বর (PDF অনুযায়ী):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                if (currentFatihahPage > 1) {
                                    viewModel.adjustOffset(-1)
                                }
                            },
                            modifier = Modifier.background(if (isDark) Color(0xFF2D2D2D) else Color(0xFFF3F4F6), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease Page", tint = if (isDark) Color.White else Color.Black)
                        }
                        
                        Text(
                            text = "$currentFatihahPage নং পৃষ্ঠা",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        IconButton(
                            onClick = { viewModel.adjustOffset(1) },
                            modifier = Modifier.background(if (isDark) Color(0xFF2D2D2D) else Color(0xFFF3F4F6), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase Page", tint = if (isDark) Color.White else Color.Black)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "সহজ নির্বাচন (ক্লিক করুন):",
                        fontSize = 12.sp,
                        color = if (isDark) Color.LightGray else Color.Gray
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 4, 5).forEach { pageNum ->
                            val isSelected = currentFatihahPage == pageNum
                            Surface(
                                onClick = {
                                    val newOffset = pageNum - 1
                                    val diff = newOffset - pdfPageOffset
                                    viewModel.adjustOffset(diff)
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF10B981) else (if (isDark) Color(0xFF2D2D2D) else Color(0xFFF3F4F6)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pageNum.toString(),
                                        color = if (isSelected) Color.White else (if (isDark) Color.White else Color.Black),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOffsetDialog = false }) {
                    Text("ঠিক আছে", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showJumpDialog) {
        Dialog(
            onDismissRequest = { showJumpDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                ),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D2D) else Color(0xFFE5E7EB)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "নির্দিষ্ট পৃষ্ঠায় যান",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF10B981),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    var selectedParaIndex by remember { mutableStateOf(0) }
                    var pageInput by remember { mutableStateOf("") }

                    // Dynamic list of 30 paras
                    val paras = remember {
                        (1..30).map { paraNum ->
                            val paraName = "${DateUtil.toBengaliNumerals(paraNum)} পারা"
                            Pair(paraNum, paraName)
                        }
                    }

                    // Helper logic for custom page system
                    fun getParaPageCount(para: Int): Int {
                        return when (para) {
                            1 -> 21
                            29 -> 24
                            30 -> 25
                            else -> 20
                        }
                    }

                    fun getParaStartPage(para: Int): Int {
                        var startPage = 1
                        for (i in 1 until para) {
                            startPage += getParaPageCount(i)
                        }
                        return startPage
                    }

                    val selectedParaNum = paras[selectedParaIndex].first
                    val maxPagesInPara = getParaPageCount(selectedParaNum)

                    var expandedPara by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Para Selector (Box with Dropdown)
                        Box(modifier = Modifier.weight(0.65f)) {
                            OutlinedButton(
                                onClick = { expandedPara = true },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (isDark) Color(0xFF2D2D2D) else Color(0xFFE5E7EB)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDark) Color.White else Color.Black),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = paras[selectedParaIndex].second,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedPara,
                                onDismissRequest = { expandedPara = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .heightIn(max = 250.dp)
                                    .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
                            ) {
                                paras.forEachIndexed { idx, item ->
                                    DropdownMenuItem(
                                        text = { Text(item.second, color = if (isDark) Color.White else Color.Black) },
                                        onClick = {
                                            selectedParaIndex = idx
                                            pageInput = "" // Clear page input when changing para
                                            expandedPara = false
                                        }
                                    )
                                }
                            }
                        }

                        // Right: Page input (OutlinedTextField)
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it in '০'..'৯' }
                                if (filtered.length <= 2) {
                                    val converted = filtered.map { char ->
                                        if (char in '0'..'9') (char - '0' + '০'.code).toChar() else char
                                    }.joinToString("")
                                    pageInput = converted
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            label = { Text("পৃষ্ঠা...", color = if (isDark) Color.LightGray else Color.Gray, fontSize = 12.sp) },
                            placeholder = { Text("১ - ${DateUtil.toBengaliNumerals(maxPagesInPara)}", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = if (isDark) Color(0xFF2D2D2D) else Color(0xFFE5E7EB),
                                focusedTextColor = if (isDark) Color.White else Color.Black,
                                unfocusedTextColor = if (isDark) Color.White else Color.Black
                            )
                        )
                    }

                    // Real-time page preview math
                    val cleanInput = pageInput.map { char ->
                        if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
                    }.joinToString("")
                    val parsedPage = cleanInput.toIntOrNull()
                    if (parsedPage != null && parsedPage in 1..maxPagesInPara) {
                        val prevPagesCount = getParaStartPage(selectedParaNum) - 1
                        val absPage = prevPagesCount + parsedPage
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "হিসাব: ${DateUtil.toBengaliNumerals(prevPagesCount)} + ${DateUtil.toBengaliNumerals(parsedPage)} = ${DateUtil.toBengaliNumerals(absPage)} নম্বর পৃষ্ঠা",
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = { showJumpDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("বাদ দিন", color = if (isDark) Color.LightGray else Color.Gray, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cleanInputVal = pageInput.map { char ->
                                    if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
                                }.joinToString("")
                                val targetParaPage = cleanInputVal.toIntOrNull()
                                if (targetParaPage != null && targetParaPage in 1..maxPagesInPara) {
                                    val targetPage = getParaStartPage(selectedParaNum) + targetParaPage - 1
                                    if (targetPage in 1..totalPages) {
                                        showJumpDialog = false
                                        viewModel.jumpToPage(targetPage)
                                    } else {
                                        android.widget.Toast.makeText(context, "সঠিক পৃষ্ঠা নম্বর লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val maxPagesBengali = DateUtil.toBengaliNumerals(maxPagesInPara)
                                    android.widget.Toast.makeText(context, "১ থেকে $maxPagesBengali এর মধ্যে পৃষ্ঠা নম্বর লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("পৃষ্ঠায় যান", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            containerColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "পড়ার সেটিংস",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 1. Dark Mode Theme Row Container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF252528) else Color(0xFFF9FAFB),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF323236) else Color(0xFFE5E7EB),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "ডার্ক মোড",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "স্ক্রিনের আলো ও থিম নিয়ন্ত্রণ করুন",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .background(
                                if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E7EB).copy(alpha = 0.6f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp)
                    ) {
                        val themes = listOf(
                            Pair("Light", "লাইট"),
                            Pair("Dark", "ডার্ক")
                        )
                        themes.forEach { (themeKey, label) ->
                            val isSelected = currentTheme == themeKey || (themeKey == "Dark" && isDark && currentTheme == "System")
                            Surface(
                                onClick = { viewModel.setTheme(themeKey) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Scroll Mode Row Container
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF252528) else Color(0xFFF9FAFB),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF323236) else Color(0xFFE5E7EB),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (scrollDirection == "Vertical") Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "স্ক্রোলিং মোড",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "পড়ার পৃষ্ঠা পরিবর্তনের ধরন",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .background(
                                if (isDark) Color(0xFF1C1C1E) else Color(0xFFE5E7EB).copy(alpha = 0.6f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp)
                    ) {
                        val modes = listOf(
                            Pair("Horizontal", "ডানে-বামে"),
                            Pair("Vertical", "ওপর-নিচ")
                        )
                        modes.forEach { (mode, label) ->
                            val isSelected = scrollDirection == mode
                            Surface(
                                onClick = { viewModel.setScrollDirection(mode) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF10B981) else Color.Transparent,
                                modifier = Modifier.padding(horizontal = 1.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Page Height Adjustment Container
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF252528) else Color(0xFFF9FAFB),
                            RoundedCornerShape(16.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF323236) else Color(0xFFE5E7EB),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "পৃষ্ঠার উচ্চতা (Page Height)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                text = "পিডিএফ পৃষ্ঠার লম্বালম্বি উচ্চতা নির্বাচন করুন",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val heightScales = listOf(
                            Pair(1.0f, "১০০%"),
                            Pair(1.1f, "১১০%"),
                            Pair(1.2f, "১২০%"),
                            Pair(1.3f, "১৩০%"),
                            Pair(1.4f, "১৪০%"),
                            Pair(1.5f, "১৫০%")
                        )
                        heightScales.forEach { (scaleVal, label) ->
                            val isSelected = kotlin.math.abs(pageHeightScale - scaleVal) < 0.05f
                            Surface(
                                onClick = { viewModel.setPageHeightScale(scaleVal) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFF10B981) else (if (isDark) Color(0xFF1C1C1E) else Color.White),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent else (if (isDark) Color(0xFF2D2D30) else Color(0xFFE5E7EB))
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.White else (if (isDark) Color(0xFFD1D1D6) else Color(0xFF48484A)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Go to Page Search Bar Button
                Surface(
                    onClick = {
                        showSettingsSheet = false
                        showJumpDialog = true
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDark) Color(0xFF252528) else Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, if (isDark) Color(0xFF323236) else Color(0xFFE5E7EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "যেকোনো পৃষ্ঠায় যান",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    text = "পারা, সূরা বা নির্দিষ্ট পৃষ্ঠা সিলেক্ট করুন",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                        
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 5. PDF Offset Adjustment (Surah Fatiha Page Adjustment) - Placed LAST as requested
                if (isPdf) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) Color(0xFF252528) else Color(0xFFF9FAFB),
                                RoundedCornerShape(16.dp)
                            )
                            .border(
                                1.dp,
                                if (isDark) Color(0xFF323236) else Color(0xFFE5E7EB),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = "সূরা ফাতিহা পৃষ্ঠা সমন্বয় (অফসেট)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDark) Color.White else Color.Black
                                )
                                Text(
                                    text = "পিডিএফে সূরা ফাতিহা কত নং পাতায় রয়েছে সেটি মেলান",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }

                        val currentFatihahPage = pdfPageOffset + 1

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDark) Color(0xFF1C1C1E) else Color(0xFFF3F4F6),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (currentFatihahPage > 1) {
                                        viewModel.adjustOffset(-1)
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isDark) Color(0xFF2D2D30) else Color.White, CircleShape)
                                    .border(1.dp, if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E7EB), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Decrease Page",
                                    tint = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Text(
                                text = "$currentFatihahPage নং পৃষ্ঠা",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF10B981)
                            )

                            IconButton(
                                onClick = { viewModel.adjustOffset(1) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isDark) Color(0xFF2D2D30) else Color.White, CircleShape)
                                    .border(1.dp, if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E7EB), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Increase Page",
                                    tint = if (isDark) Color.White else Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Short-cut page selector pill row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3, 4, 5).forEach { pageNum ->
                                val isSelected = currentFatihahPage == pageNum
                                Surface(
                                    onClick = {
                                        val newOffset = pageNum - 1
                                        val diff = newOffset - pdfPageOffset
                                        viewModel.adjustOffset(diff)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Color(0xFF10B981) else (if (isDark) Color(0xFF1C1C1E) else Color.White),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) Color.Transparent else (if (isDark) Color(0xFF2D2D30) else Color(0xFFE5E7EB))
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = pageNum.toString(),
                                            color = if (isSelected) Color.White else (if (isDark) Color(0xFFD1D1D6) else Color(0xFF48484A)),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
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
fun OnDemandPageViewer(
    mushafId: String,
    pageNumber: Int,
    pdfPageOffset: Int,
    pageHeightScale: Float = 1.2f,
    isDark: Boolean,
    viewModel: MushafViewerViewModel
) {
    var pagePath by remember(mushafId, pageNumber, pdfPageOffset) { mutableStateOf<String?>(null) }
    var isLoading by remember(mushafId, pageNumber, pdfPageOffset) { mutableStateOf(true) }
    var hasError by remember(mushafId, pageNumber, pdfPageOffset) { mutableStateOf(false) }

    LaunchedEffect(mushafId, pageNumber, pdfPageOffset) {
        isLoading = true
        hasError = false
        val path = viewModel.getPagePath(mushafId, pageNumber)
        if (path != null) {
            pagePath = path
            isLoading = false
        } else {
            val success = viewModel.downloadPageOnDemand(mushafId, pageNumber)
            if (success) {
                pagePath = viewModel.getPagePath(mushafId, pageNumber)
                isLoading = false
            } else {
                isLoading = false
                hasError = true
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color.White), 
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Color(0xFF10B981))
                Spacer(modifier = Modifier.height(8.dp))
                Text("লোড হচ্ছে...", color = if (isDark) Color.LightGray else Color.Gray, fontSize = 13.sp)
            }
        }
    } else if (hasError) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDark) Color.Black else Color.White), 
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("পাতা লোড করা যায়নি", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text("ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন", color = if (isDark) Color.LightGray else Color.Gray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        isLoading = true
                        hasError = false
                        pagePath = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("আবার চেষ্টা করুন", color = Color.White)
                }
            }
        }
    } else if (pagePath != null) {
        PageViewer(pagePath = pagePath!!, isDark = isDark, pageHeightScale = pageHeightScale)
    }
}

@Composable
fun AyahSelectionSheetContent(
    currentPage: Int,
    pageAyahs: List<CombinedAyah>,
    selectedAyah: CombinedAyah?,
    isLoading: Boolean,
    isPlaying: Boolean,
    currentPlayingAyahNum: Int?,
    isDark: Boolean,
    onSelectAyah: (CombinedAyah) -> Unit,
    onTogglePlayPause: (CombinedAyah) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showFullTafsirDialog by remember { mutableStateOf(false) }

    val primaryGreen = Color(0xFF10B981)
    val cardBg = if (isDark) Color(0xFF2A2A2A) else Color(0xFFF8FAF8)
    val textColor = if (isDark) Color.White else Color(0xFF1F2937)
    val subtitleColor = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "পৃষ্ঠা ${DateUtil.toBengaliNumerals(currentPage)} - আয়াত নির্বাচন ও তাফসীর",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryGreen
                )
                Text(
                    text = if (pageAyahs.isNotEmpty()) "${DateUtil.toBengaliNumerals(pageAyahs.size)} টি আয়াত রয়েছে" else "আয়াত লোড হচ্ছে...",
                    fontSize = 12.sp,
                    color = subtitleColor
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন", tint = subtitleColor)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = primaryGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("আয়াত এবং তাফসীর তথ্য লোড হচ্ছে...", color = subtitleColor, fontSize = 13.sp)
                }
            }
        } else if (pageAyahs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("এই পৃষ্ঠার কোনো আয়াত তথ্য পাওয়া যায়নি", color = subtitleColor)
            }
        } else {
            // 2. Ayah Selector Chips
            Text(
                text = "আয়াত নির্বাচন করুন:",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(pageAyahs) { ayah ->
                    val isSelected = selectedAyah?.number == ayah.number
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectAyah(ayah) },
                        label = {
                            Text(
                                "আয়াত ${DateUtil.toBengaliNumerals(ayah.numberInSurah)}",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = primaryGreen,
                            selectedLabelColor = Color.White,
                            containerColor = cardBg,
                            labelColor = textColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Selected Ayah Details Scrollable View
            if (selectedAyah != null) {
                val currentAyah = selectedAyah
                val surahData = remember(currentAyah.surahNumber) {
                    com.example.data.QuranData.surahNames.find { it.first == currentAyah.surahNumber }
                }
                val surahName = surahData?.second?.first ?: "সূরা ${currentAyah.surahNumber}"
                val isCurrentPlaying = isPlaying && currentPlayingAyahNum == currentAyah.numberInSurah

                val parsedTafsir = remember(currentAyah.tafsirText) {
                    currentAyah.tafsirText?.parseHtmlToAnnotatedString(primaryGreen)
                }

                if (showFullTafsirDialog) {
                    AlertDialog(
                        onDismissRequest = { showFullTafsirDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false),
                        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.85f),
                        title = {
                            Column {
                                Text("$surahName • আয়াত ${DateUtil.toBengaliNumerals(currentAyah.numberInSurah)}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = primaryGreen)
                                Text("সম্পূর্ণ তাফসীর", fontSize = 13.sp, color = subtitleColor)
                            }
                        },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                if (parsedTafsir != null && parsedTafsir.isNotEmpty()) {
                                    Text(
                                        text = parsedTafsir,
                                        fontSize = 16.sp,
                                        lineHeight = 26.sp,
                                        color = textColor,
                                        textAlign = TextAlign.Justify
                                    )
                                } else {
                                    Text("এই আয়াতের তাফসীর তথ্য পাওয়া যায়নি।", fontSize = 15.sp, color = subtitleColor)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFullTafsirDialog = false }) {
                                Text("বন্ধ করুন", color = primaryGreen, fontWeight = FontWeight.Bold)
                            }
                        },
                        containerColor = if (isDark) Color(0xFF2A2A2A) else Color.White
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Ayah Header Info Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$surahName • আয়াত ${DateUtil.toBengaliNumerals(currentAyah.numberInSurah)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = primaryGreen
                                )
                                Text(
                                    text = "পারা ${DateUtil.toBengaliNumerals(currentAyah.juz)}",
                                    fontSize = 12.sp,
                                    color = subtitleColor
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Arabic Text
                            Text(
                                text = currentAyah.arabicText,
                                fontSize = 24.sp,
                                lineHeight = 42.sp,
                                fontFamily = com.example.ui.theme.getArabicFont("Me Quran"),
                                color = textColor,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Audio Playback Button & Navigation Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Ayah Button
                                val currentIndex = pageAyahs.indexOfFirst { it.number == currentAyah.number }
                                IconButton(
                                    onClick = {
                                        if (currentIndex > 0) {
                                            onSelectAyah(pageAyahs[currentIndex - 1])
                                        }
                                    },
                                    enabled = currentIndex > 0
                                ) {
                                    Icon(
                                        Icons.Default.SkipPrevious,
                                        contentDescription = "পূর্ববর্তী আয়াত",
                                        tint = if (currentIndex > 0) primaryGreen else subtitleColor.copy(alpha = 0.4f)
                                    )
                                }

                                // Play Audio Button
                                Button(
                                    onClick = { onTogglePlayPause(currentAyah) },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isCurrentPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isCurrentPlaying) "বিরতি দিন" else "তিলাওয়াত শুনুন",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Next Ayah Button
                                IconButton(
                                    onClick = {
                                        if (currentIndex != -1 && currentIndex + 1 < pageAyahs.size) {
                                            onSelectAyah(pageAyahs[currentIndex + 1])
                                        }
                                    },
                                    enabled = currentIndex != -1 && currentIndex + 1 < pageAyahs.size
                                ) {
                                    Icon(
                                        Icons.Default.SkipNext,
                                        contentDescription = "পরবর্তী আয়াত",
                                        tint = if (currentIndex != -1 && currentIndex + 1 < pageAyahs.size) primaryGreen else subtitleColor.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bangla Translation Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "বাংলা অনুবাদ:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = primaryGreen
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = currentAyah.bengaliText.ifEmpty { "অনুবাদ উপলব্ধ নয়" },
                                fontSize = 15.sp,
                                lineHeight = 24.sp,
                                color = textColor,
                                fontFamily = com.example.ui.theme.getBengaliFont("SolaimanLipi")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tafsir Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "তাফসীর:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = primaryGreen
                                )
                                TextButton(onClick = { showFullTafsirDialog = true }) {
                                    Text("সম্পূর্ণ দেখুন", color = primaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (parsedTafsir != null && parsedTafsir.isNotEmpty()) {
                                Text(
                                    text = parsedTafsir,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = textColor,
                                    maxLines = 6,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                Text(
                                    text = "এই আয়াতের তাফসীর লোড করতে 'সম্পূর্ণ দেখুন' বাটনে ক্লিক করুন অথবা অনলাইন তাফসীর প্যাক ডাউনলোড করুন।",
                                    fontSize = 13.sp,
                                    color = subtitleColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons (Copy / Share)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val textToCopy = "${currentAyah.arabicText}\n\n${currentAyah.bengaliText}\n\n($surahName: ${currentAyah.numberInSurah})"
                                clipboardManager.setText(AnnotatedString(textToCopy))
                                Toast.makeText(context, "আয়াত ও অনুবাদ কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, primaryGreen)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = primaryGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কপি করুন", color = primaryGreen)
                        }

                        OutlinedButton(
                            onClick = {
                                val textToShare = "${currentAyah.arabicText}\n\n${currentAyah.bengaliText}\n\n($surahName: ${currentAyah.numberInSurah})"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, textToShare)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "শেয়ার করুন"))
                            },
                            modifier = Modifier.weight(1f),
                            border = BorderStroke(1.dp, primaryGreen)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, tint = primaryGreen)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("শেয়ার করুন", color = primaryGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
