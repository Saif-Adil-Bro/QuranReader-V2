package com.example.ui.screens.mushaf

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.QuranData
import com.example.ui.components.QuranIndexComponent
import com.example.ui.components.RecentReadItem
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.HomeViewModel
import com.example.utils.DateUtil
import com.example.utils.DateUtil.toBengaliNumerals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranPoricitiScreen(
    viewModel: HomeViewModel,
    onNavigateToMushafPage: (String, Int, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val currentTheme by viewModel.theme.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (currentTheme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemDark
    }

    val defaultMushafId by viewModel.defaultMushafId.collectAsState()
    val mushafDownloadStatus by viewModel.mushafDownloadStatus.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showDownloadRequestDialog by remember { mutableStateOf(false) }
    var showDownloadProgressDialog by remember { mutableStateOf(false) }
    var pendingTargetPage by remember { mutableIntStateOf(1) }

    // Colors
    val cardBgColor = if (isDark) Color(0xFF18181B) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF111827)
    val textSecondary = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
    val dividerColor = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    // Bookmarks of type PAGE
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val pageBookmarks = remember(bookmarks) {
        bookmarks.filter { it.type == "PAGE" || it.type == "MUSHAF_PAGE" }
    }

    // Recent Reads for PDF / Hafezi
    val recentTracks by viewModel.recentReads.collectAsState()
    val recentReads = remember(recentTracks) {
        recentTracks
            .filter { it.mode == "HAFEZI" || it.mode == "MUSHAF" || it.pageNumber != null }
            .map { track ->
                val pNum = track.pageNumber ?: 1
                val sName = QuranData.surahNames.find { it.first == track.surahNumber }?.second?.first
                val title = if (!sName.isNullOrEmpty()) {
                    "পৃষ্ঠা ${toBengaliNumerals(pNum)} ($sName)"
                } else {
                    "পৃষ্ঠা ${toBengaliNumerals(pNum)}"
                }
                RecentReadItem(
                    title = title,
                    surahNumber = track.surahNumber,
                    ayahNumber = track.ayahNumber,
                    pageNumber = pNum,
                    mode = track.mode
                )
            }
    }

    // Monitor download status
    LaunchedEffect(mushafDownloadStatus) {
        val status = mushafDownloadStatus
        if (status != null && status.state is com.example.data.model.DownloadState.Downloaded) {
            showDownloadProgressDialog = false
            viewModel.clearMushafDownloadStatus()
            onNavigateToMushafPage(defaultMushafId, pendingTargetPage, false)
        }
    }

    fun navigateToPageIfDownloaded(targetPage: Int) {
        val isDownloaded = viewModel.isMushafDownloaded(defaultMushafId)
        if (isDownloaded) {
            onNavigateToMushafPage(defaultMushafId, targetPage, false)
        } else {
            pendingTargetPage = targetPage
            showDownloadRequestDialog = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "হাফেজী কুরআন সূচী",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "১৫-লাইন স্ট্যান্ডার্ড মুসহাফ",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Normal
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showBookmarksDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "বুকমার্ক",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            QuranIndexComponent(
                modifier = Modifier.fillMaxSize(),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSurahClick = { surahNumber ->
                    val startPage = QuranData.surahStartPages.getOrNull(surahNumber - 1) ?: 1
                    navigateToPageIfDownloaded(startPage)
                },
                onJuzClick = { juzNumber ->
                    val startPage = com.example.data.HafeziQuranData.getParaStartPage(juzNumber, 1)
                    navigateToPageIfDownloaded(startPage)
                },
                onPageClick = { pageNumber ->
                    navigateToPageIfDownloaded(pageNumber)
                },
                onNavigateToSurahWithAyah = { surahNumber, ayahNumber ->
                    if (surahNumber == 2 && ayahNumber == 255) {
                        navigateToPageIfDownloaded(42)
                    } else {
                        val startPage = QuranData.surahStartPages.getOrNull(surahNumber - 1) ?: 1
                        navigateToPageIfDownloaded(startPage)
                    }
                },
                recentReads = recentReads,
                headerContent = {
                    if (searchQuery.isEmpty()) {
                        QuickJumpToPageCard(
                            isDark = isDark,
                            onJumpToPage = { page ->
                                navigateToPageIfDownloaded(page)
                            }
                        )
                    }
                }
            )
        }
    }

    // --- DIALOGS ---

    // 1. Bookmarks Dialog
    if (showBookmarksDialog) {
        Dialog(onDismissRequest = { showBookmarksDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(1.dp, dividerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "বুকমার্ক তালিকা",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PrimaryGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )

                    HorizontalDivider(color = dividerColor)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pageBookmarks.isEmpty()) {
                        Text(
                            text = "কোনো বুকমার্ক পাওয়া যায়নি!\nমুসহাফ পৃষ্ঠা পড়ার সময় উপরে বুকমার্ক করুন।",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column {
                                pageBookmarks.forEach { bookmark ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showBookmarksDialog = false
                                                val targetmId = bookmark.mushafId ?: defaultMushafId
                                                if (viewModel.isMushafDownloaded(targetmId)) {
                                                    onNavigateToMushafPage(targetmId, bookmark.referenceId, false)
                                                } else {
                                                    pendingTargetPage = bookmark.referenceId
                                                    showDownloadRequestDialog = true
                                                }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Bookmark,
                                                contentDescription = null,
                                                tint = PrimaryGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = bookmark.name,
                                                fontSize = 14.sp,
                                                color = textPrimary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = textSecondary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { viewModel.deleteBookmark(bookmark) }
                                        )
                                    }
                                    HorizontalDivider(color = dividerColor.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { showBookmarksDialog = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("বন্ধ করুন", color = PrimaryGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // 2. Download Request Dialog
    if (showDownloadRequestDialog) {
        val currentMushaf = viewModel.getMushafStyle(defaultMushafId)
        val mushafName = currentMushaf?.nameBengali ?: "হাফেজী কুরআন"
        val sizeStr = "${currentMushaf?.fileSizeMB ?: 30} MB"
        AlertDialog(
            onDismissRequest = { showDownloadRequestDialog = false },
            title = {
                Text(
                    text = "মুসহাফ ডাউনলোড",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PrimaryGreen
                )
            },
            text = {
                Text(
                    text = "$mushafName ফাইলটি পড়ার জন্য প্রথমে ডাউনলোড করতে হবে। সাইজ: $sizeStr।\nআপনি কি ডাউনলোড করতে চান?",
                    fontSize = 14.sp,
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDownloadRequestDialog = false
                        showDownloadProgressDialog = true
                        viewModel.downloadDefaultMushaf(defaultMushafId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                ) {
                    Text("ডাউনলোড করুন", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDownloadRequestDialog = false },
                    border = BorderStroke(1.dp, dividerColor),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary)
                ) {
                    Text("বাতিল")
                }
            },
            containerColor = cardBgColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // 3. Download Progress Dialog
    if (showDownloadProgressDialog) {
        val status = mushafDownloadStatus
        val currentMushaf = viewModel.getMushafStyle(defaultMushafId)
        val mushafName = currentMushaf?.nameBengali ?: "হাফেজী কুরআন"
        AlertDialog(
            onDismissRequest = { /* Don't dismiss by tapping outside */ },
            title = {
                Text(
                    text = if (status?.state is com.example.data.model.DownloadState.Failed) "ডাউনলোড ব্যর্থ হয়েছে" else "ডাউনলোড হচ্ছে...",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = if (status?.state is com.example.data.model.DownloadState.Failed) Color.Red else PrimaryGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (status?.state is com.example.data.model.DownloadState.Failed) {
                            "দুঃখিত, ডাউনলোড ব্যর্থ হয়েছে। ইন্টারনেট সংযোগ চেক করে আবার চেষ্টা করুন।"
                        } else {
                            "$mushafName ডাউনলোড হচ্ছে। অনুগ্রহ করে অপেক্ষা করুন..."
                        },
                        fontSize = 14.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    if (status?.state !is com.example.data.model.DownloadState.Failed) {
                        val progress = status?.progress ?: 0
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            color = PrimaryGreen,
                            trackColor = dividerColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "অগ্রগতি: ${DateUtil.toBengaliNumerals(progress)}%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "ডাউনলোড হচ্ছে...",
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (status?.state is com.example.data.model.DownloadState.Failed) {
                    Button(
                        onClick = {
                            viewModel.clearMushafDownloadStatus()
                            showDownloadProgressDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text("ঠিক আছে", color = Color.White)
                    }
                } else {
                    TextButton(
                        onClick = {
                            viewModel.cancelMushafDownload(defaultMushafId)
                            showDownloadProgressDialog = false
                        }
                    ) {
                        Text("বাতিল করুন", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            },
            containerColor = cardBgColor,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun QuickJumpToPageCard(
    isDark: Boolean,
    onJumpToPage: (Int) -> Unit
) {
    val context = LocalContext.current
    var selectedParaIndex by rememberSaveable { mutableIntStateOf(0) }
    var pageInput by rememberSaveable { mutableStateOf("১") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

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

    val selectedParaNum = selectedParaIndex + 1
    val maxPagesInPara = getParaPageCount(selectedParaNum)
    val cleanDigits = pageInput.map { char ->
        if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
    }.filter { it.isDigit() }.joinToString("")
    val pageNumInPara = cleanDigits.toIntOrNull()?.coerceIn(1, maxPagesInPara) ?: 1
    val calculatedAbsolutePage = getParaStartPage(selectedParaNum) + pageNumInPara - 1

    val cardBg = if (isDark) Color(0xFF18181B) else Color.White
    val cardBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(emeraldAccent.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Quick Jump",
                            tint = emeraldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "নির্দিষ্ট পৃষ্ঠায় যান",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = emeraldAccent.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "মোট পৃষ্ঠা: ${toBengaliNumerals(calculatedAbsolutePage)}",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = emeraldAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Para Selector
                Box(
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                ) {
                    Surface(
                        onClick = { isDropdownExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFF3F3F46) else Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${toBengaliNumerals(selectedParaNum)} পারা",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Select Para",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isDropdownExpanded,
                        onDismissRequest = { isDropdownExpanded = false },
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        (1..30).forEachIndexed { idx, pNum ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "${toBengaliNumerals(pNum)} পারা - ${QuranData.paraNamesBangla.getOrElse(idx) { "" }}",
                                        fontSize = 13.sp
                                    )
                                },
                                onClick = {
                                    selectedParaIndex = idx
                                    isDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Page in Para Input
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { input ->
                        val digitsOnly = input.map { char ->
                            if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
                        }.filter { it.isDigit() }.joinToString("")

                        if (digitsOnly.isEmpty()) {
                            pageInput = ""
                        } else {
                            val num = digitsOnly.toIntOrNull() ?: 1
                            if (num <= maxPagesInPara) {
                                pageInput = toBengaliNumerals(num)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(0.95f)
                        .height(48.dp),
                    placeholder = {
                        Text(
                            text = "১-${toBengaliNumerals(maxPagesInPara)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = emeraldAccent,
                        unfocusedBorderColor = if (isDark) Color(0xFF3F3F46) else Color(0xFFE2E8F0),
                        focusedContainerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF27272A) else Color(0xFFF8FAFC),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Go Button
                Button(
                    onClick = {
                        val cleanInput = pageInput.map { char ->
                            if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
                        }.filter { it.isDigit() }.joinToString("")

                        val pageNum = cleanInput.toIntOrNull()
                        if (pageNum != null && pageNum in 1..maxPagesInPara) {
                            val targetPage = getParaStartPage(selectedParaNum) + pageNum - 1
                            onJumpToPage(targetPage)
                        } else {
                            Toast.makeText(
                                context,
                                "১ থেকে ${toBengaliNumerals(maxPagesInPara)} এর মধ্যে পৃষ্ঠা লিখুন",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "যান",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Go",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    }
}
