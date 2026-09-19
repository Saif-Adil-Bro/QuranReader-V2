package com.example.ui.screens.mushaf

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.HomeViewModel
import com.example.utils.DateUtil

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
    val lastReadMushafPage by viewModel.lastReadMushafPage.collectAsState()
    val mushafDownloadStatus by viewModel.mushafDownloadStatus.collectAsState()

    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarksDialog by remember { mutableStateOf(false) }
    var showDownloadRequestDialog by remember { mutableStateOf(false) }
    var showDownloadProgressDialog by remember { mutableStateOf(false) }

    // Collect bookmarks of type PAGE to display in Bookmark action
    val bookmarks by viewModel.bookmarks.collectAsState(initial = emptyList())
    val pageBookmarks = remember(bookmarks) {
        bookmarks.filter { it.type == "PAGE" || it.type == "MUSHAF_PAGE" }
    }

    // Colors
    val backgroundColor = if (isDark) Color(0xFF0C1916) else Color(0xFFF0FDFA)
    val cardBgColor = if (isDark) Color(0xFF142B24) else Color.White
    val textPrimary = if (isDark) Color.White else Color(0xFF111827)
    val textSecondary = if (isDark) Color(0xFF9CA3AF) else Color(0xFF4B5563)
    val dividerColor = if (isDark) Color(0xFF1E3A31) else Color(0xFFE5E7EB)

    // Monitor download status
    LaunchedEffect(mushafDownloadStatus) {
        val status = mushafDownloadStatus
        if (status != null && status.state is com.example.data.model.DownloadState.Downloaded) {
            showDownloadProgressDialog = false
            viewModel.clearMushafDownloadStatus()
            // Open the PDF index (sucipotro) immediately upon download completion
            onNavigateToMushafPage(defaultMushafId, 1, true)
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "কুরআন পরিচিতি",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lanterns and Calligraphy Decor Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background decorations (e.g. geometric patterns)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Left and right hanging lantern guidelines can be simulated with lines
                }

                // Calligraphy Circular Emblem
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White, CircleShape)
                        .border(4.dp, PrimaryGreen, CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.5.dp, Color(0xFF0D9488).copy(alpha = 0.4f), CircleShape)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ornamental background stars in the emblem
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(45f)
                                .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .rotate(22.5f)
                                .border(0.5.dp, Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        )

                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_launcher),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Titles
            Text(
                text = "কুরআন মাজীদ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "হাফেজি ও তাজভীদ কুরআন",
                fontSize = 14.sp,
                color = textSecondary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action 1: "পড়ুন" Button (Primary read with Index open - "sucipotro")
            Button(
                onClick = {
                    if (viewModel.isMushafDownloaded(defaultMushafId)) {
                        onNavigateToMushafPage(defaultMushafId, lastReadMushafPage, true)
                    } else {
                        showDownloadRequestDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2DD4BF) // Bright Turquoise/Cyan as in Image 1
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = "Read Index",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পড়ুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row of Action 2: "বুকমার্ক" and Action 3: "সর্বশেষ পঠিত"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showBookmarksDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBgColor
                    ),
                    border = BorderStroke(1.dp, dividerColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmarks",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "বুকমার্ক",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }

                Button(
                    onClick = {
                        if (viewModel.isMushafDownloaded(defaultMushafId)) {
                            onNavigateToMushafPage(defaultMushafId, lastReadMushafPage, false)
                        } else {
                            showDownloadRequestDialog = true
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = cardBgColor
                    ),
                    border = BorderStroke(1.dp, dividerColor)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Last Read",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সর্বশেষ পঠিত",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action 4: "পৃষ্ঠায় যান" Button
            Button(
                onClick = { showJumpDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(100.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cardBgColor
                ),
                border = BorderStroke(1.dp, dividerColor)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Go to Page",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "পৃষ্ঠায় যান",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description / Poriciti Section Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(1.dp, dividerColor)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Introduction Icon",
                            tint = PrimaryGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পরিচিতি",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    HorizontalDivider(color = dividerColor, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "হাফেজি ও তাজভীদ কুরআন শরীফ\n৩০ পারা, ১১৪ সূরা।\n\nমোট ৬ টি মুসহাফ সমৃদ্ধ হাফেজী কুরআন শরীফ।\n\nমুসহাফ সমূহ:\n১. হাফেজী - এমদাদিয়া লাইব্রেরী (বাংলাদেশ)\n২. হাফেজী ১৫-লাইন কুরআন (স্ট্যান্ডার্ড)\n৩. ইন্দো-পাক লিপি মুসহাফ\n৪. তাজভীদ রঙিন মুসহাফ\n\nএটি একটি উচ্চ মানের অফলাইন পিডিএফ সংস্করণ, যা নিখুঁত স্পর্শ এবং চমৎকার পড়ার অভিজ্ঞতা নিশ্চিত করে। জুম ইন/আউট করে খুব সহজেই পড়া যায়।",
                        fontSize = 14.sp,
                        color = textSecondary,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. Jump to Page Dialog ("নির্দিষ্ট পৃষ্ঠায় যান") - Image 3 Style
    if (showJumpDialog) {
        Dialog(
            onDismissRequest = { showJumpDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = BorderStroke(1.dp, dividerColor),
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
                        color = PrimaryGreen,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Select Para Quick list
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
                                border = BorderStroke(1.dp, dividerColor),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = paras[selectedParaIndex].second,
                                        color = textPrimary,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = PrimaryGreen
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedPara,
                                onDismissRequest = { expandedPara = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .heightIn(max = 250.dp)
                                    .background(cardBgColor)
                            ) {
                                paras.forEachIndexed { idx, item ->
                                    DropdownMenuItem(
                                        text = { Text(item.second, color = textPrimary) },
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
                            label = { Text("পৃষ্ঠা...", color = textSecondary, fontSize = 12.sp) },
                            placeholder = { Text("১ - ${DateUtil.toBengaliNumerals(maxPagesInPara)}", color = Color.Gray, fontSize = 11.sp) },
                            modifier = Modifier.weight(0.35f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = dividerColor,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
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
                            color = PrimaryGreen,
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
                            Text("বাদ দিন", color = textSecondary, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val cleanInputVal = pageInput.map { char ->
                                    if (char in '০'..'৯') (char - '০' + '0'.code).toChar() else char
                                }.joinToString("")
                                val targetParaPage = cleanInputVal.toIntOrNull()
                                if (targetParaPage != null && targetParaPage in 1..maxPagesInPara) {
                                    val targetPage = getParaStartPage(selectedParaNum) + targetParaPage - 1
                                    if (targetPage in 1..611) {
                                        showJumpDialog = false
                                        if (viewModel.isMushafDownloaded(defaultMushafId)) {
                                            onNavigateToMushafPage(defaultMushafId, targetPage, false)
                                        } else {
                                            showDownloadRequestDialog = true
                                        }
                                    } else {
                                        android.widget.Toast.makeText(context, "সঠিক পৃষ্ঠা নম্বর লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val maxPagesBengali = DateUtil.toBengaliNumerals(maxPagesInPara)
                                    android.widget.Toast.makeText(context, "১ থেকে $maxPagesBengali এর মধ্যে পৃষ্ঠা নম্বর লিখুন", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
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

    // 2. Bookmarks Dialog
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
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                    )

                    HorizontalDivider(color = dividerColor)

                    Spacer(modifier = Modifier.height(12.dp))

                    if (pageBookmarks.isEmpty()) {
                        Text(
                            text = "কোনো বুকমার্ক পাওয়া যায়নি!\nমুসহাফ পৃষ্ঠা পড়ার সময় উপরে বুকমার্ক করুন।",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
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
                                                    showDownloadRequestDialog = true
                                                }
                                            }
                                            .padding(vertical = 12.dp, horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Bookmark, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
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

    // 3. Download Request Dialog
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

    // 4. Download Progress Dialog
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
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
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
