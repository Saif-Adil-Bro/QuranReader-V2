package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranData
import com.example.data.model.CombinedAyah
import com.example.data.model.appendStyledWaqfText
import com.example.data.repository.QuranRepository
import com.example.utils.IndoPakTajweedParser
import com.example.ui.theme.PrimaryGreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.viewmodels.SurahDetailViewModel
import com.example.utils.DateUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahTafsirDetailModal(
    ayahs: List<CombinedAyah>,
    initialAyahIndex: Int,
    surahNumber: Int,
    viewModel: SurahDetailViewModel,
    showTajweed: Boolean = false,
    arabicFontSize: Float = 24f,
    bengaliFontSize: Float = 16f,
    arabicFontName: String = "Me Quran",
    bengaliFontName: String = "SolaimanLipi",
    selectedTafsirNames: List<String> = emptyList(),
    isBookmarked: (CombinedAyah) -> Boolean = { false },
    onToggleBookmark: (CombinedAyah) -> Unit = {},
    onDismiss: (lastVisibleAyahIndex: Int) -> Unit
) {
    if (ayahs.isEmpty()) return

    val validInitialIndex = initialAyahIndex.coerceIn(0, ayahs.size - 1)
    val pagerState = rememberPagerState(initialPage = validInitialIndex) { ayahs.size }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentAyahIndex = pagerState.currentPage
    val currentAyah = ayahs[currentAyahIndex]

    // Intercept hardware/system back to dismiss modal smoothly
    BackHandler {
        onDismiss(pagerState.currentPage)
    }

    val surahData = remember(surahNumber) {
        QuranData.surahNames.find { it.first == surahNumber }
    }
    val surahBanglaName = surahData?.second?.first ?: QuranData.getSurahNameBangla(surahNumber)
    val surahArabicName = QuranData.getSurahNameArabic(surahNumber)

    val isDark = MaterialTheme.colorScheme.surface.let {
        // Calculate relative luminance or check dark scheme
        (it.red * 0.299 + it.green * 0.587 + it.blue * 0.114) < 0.5
    }

    val modalBgColor = if (isDark) Color(0xFF0D1815) else Color(0xFFF7FAF8)
    val headerBgColor = if (isDark) Color(0xFF13221E) else Color(0xFFFFFFFF)
    val cardBgColor = if (isDark) Color(0xFF162722) else Color(0xFFFFFFFF)
    val cardBorderColor = if (isDark) Color(0xFF1F352E) else Color(0xFFE2EBE5)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryAccent = PrimaryGreen

    // Dropdown state for Ayah Selector
    var showAyahDropdown by remember { mutableStateOf(false) }

    // Full photo card customizer dialog state
    var showPhotoCardDialog by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = { onDismiss(pagerState.currentPage) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val isPlayingState by viewModel.isPlaying.collectAsState()
        val currentPlayingAyahNum by viewModel.currentPlayingAyahNumber.collectAsState()
        val isThisAyahPlaying = isPlayingState && currentPlayingAyahNum == currentAyah.numberInSurah
        val isCurrentBookmarked = isBookmarked(currentAyah)

        Scaffold(
            topBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = headerBgColor,
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Icon (<)
                            IconButton(onClick = { onDismiss(pagerState.currentPage) }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "ফিরে যান",
                                    tint = primaryAccent
                                )
                            }

                            // Center Title: তাফসীর ও অনুবাদ
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "তাফসীর ও অনুবাদ",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                val tafsirTitle = if (selectedTafsirNames.isNotEmpty()) {
                                    selectedTafsirNames.joinToString(", ")
                                } else {
                                    "তাফসীর আল-মুয়াসসার"
                                }
                                Text(
                                    text = tafsirTitle,
                                    fontSize = 12.sp,
                                    color = primaryAccent,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Right close or index button
                            IconButton(onClick = { onDismiss(pagerState.currentPage) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    tint = textSecondary
                                )
                            }
                        }

                        HorizontalDivider(
                            color = cardBorderColor,
                            thickness = 1.dp
                        )

                        // Sub-Header (Prev, Dropdown Selector, Next)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Previous Button (< আয়াত N-1)
                            if (currentAyahIndex > 0) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = currentAyahIndex - 1,
                                                animationSpec = tween(280)
                                            )
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "পূর্ববর্তী",
                                            tint = primaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val prevAyahNum = ayahs[currentAyahIndex - 1].numberInSurah
                                        Text(
                                            text = "আয়াত ${DateUtil.toBengaliNumerals(prevAyahNum)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryAccent
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(72.dp))
                            }

                            // Center Dropdown Selector: সূরা ... - আয়াত X ⌄
                            Box(contentAlignment = Alignment.Center) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = primaryAccent.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.35f)),
                                    modifier = Modifier.clickable { showAyahDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "সূরা $surahBanglaName • আয়াত ${DateUtil.toBengaliNumerals(currentAyah.numberInSurah)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = primaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "আয়াত বাছাই করুন",
                                            tint = primaryAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Ayah selection dropdown menu
                                DropdownMenu(
                                    expanded = showAyahDropdown,
                                    onDismissRequest = { showAyahDropdown = false },
                                    modifier = Modifier
                                        .widthIn(min = 220.dp, max = 280.dp)
                                        .heightIn(max = 360.dp)
                                        .background(cardBgColor, RoundedCornerShape(16.dp))
                                        .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                                ) {
                                    Text(
                                        text = "আয়াতে জাম্প করুন (${DateUtil.toBengaliNumerals(ayahs.size)}টি আয়াত)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryAccent,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                    HorizontalDivider(color = cardBorderColor)
                                    ayahs.forEachIndexed { idx, item ->
                                        val isSelected = idx == currentAyahIndex
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "আয়াত ${DateUtil.toBengaliNumerals(item.numberInSurah)}",
                                                        fontSize = 14.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (isSelected) primaryAccent else textPrimary
                                                    )
                                                    if (isSelected) {
                                                        Icon(
                                                            imageVector = Icons.Default.Check,
                                                            contentDescription = null,
                                                            tint = primaryAccent,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                showAyahDropdown = false
                                                coroutineScope.launch {
                                                    pagerState.scrollToPage(idx)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            // Next Button (আয়াত N+1 >)
                            if (currentAyahIndex < ayahs.size - 1) {
                                TextButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                page = currentAyahIndex + 1,
                                                animationSpec = tween(280)
                                            )
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val nextAyahNum = ayahs[currentAyahIndex + 1].numberInSurah
                                        Text(
                                            text = "আয়াত ${DateUtil.toBengaliNumerals(nextAyahNum)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryAccent
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "পরবর্তী",
                                            tint = primaryAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(72.dp))
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = headerBgColor,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 36.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play/Pause Audio Button
                        Button(
                            onClick = { viewModel.togglePlayPause(currentAyah, surahNumber) },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.35f)
                                .height(44.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (isThisAyahPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "অডিও প্লে",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isThisAyahPlaying) "থামান" else "তিলাওয়াত শুনুন",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Bookmark Button
                        IconButton(
                            onClick = { onToggleBookmark(currentAyah) },
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isCurrentBookmarked) primaryAccent.copy(alpha = 0.15f)
                                    else cardBorderColor.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "বুকমার্ক",
                                tint = if (isCurrentBookmarked) primaryAccent else textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Copy Button
                        IconButton(
                            onClick = {
                                val copyContent = buildString {
                                    append(currentAyah.arabicText)
                                    append("\n\n")
                                    append(currentAyah.bengaliText)
                                    if (!currentAyah.tafsirText.isNullOrBlank()) {
                                        append("\n\nতাফসীর:\n")
                                        append(
                                            android.text.Html.fromHtml(
                                                currentAyah.tafsirText,
                                                android.text.Html.FROM_HTML_MODE_LEGACY
                                            ).toString()
                                        )
                                    }
                                    append("\n\n[সূরা $surahBanglaName : আয়াত ${currentAyah.numberInSurah}]")
                                }
                                clipboardManager.setText(AnnotatedString(copyContent))
                                Toast.makeText(context, "আয়াত ও তাফসীর কপি করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(cardBorderColor.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "কপি",
                                tint = primaryAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Share Button
                        IconButton(
                            onClick = {
                                val shareContent = buildString {
                                    append(currentAyah.arabicText)
                                    append("\n\n")
                                    append(currentAyah.bengaliText)
                                    if (!currentAyah.tafsirText.isNullOrBlank()) {
                                        append("\n\nতাফসীর:\n")
                                        append(
                                            android.text.Html.fromHtml(
                                                currentAyah.tafsirText,
                                                android.text.Html.FROM_HTML_MODE_LEGACY
                                            ).toString()
                                        )
                                    }
                                    append("\n\n[সূরা $surahBanglaName : আয়াত ${currentAyah.numberInSurah}]")
                                }
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareContent)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(intent, "আয়াত ও তাফসীর শেয়ার করুন"))
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(cardBorderColor.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "শেয়ার",
                                tint = primaryAccent,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            },
            containerColor = modalBgColor
        ) { paddingValues ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalAlignment = Alignment.Top
            ) { page ->
                val ayah = ayahs[page]
                AyahTafsirPageContent(
                    ayah = ayah,
                    surahNumber = surahNumber,
                    surahBanglaName = surahBanglaName,
                    surahArabicName = surahArabicName,
                    viewModel = viewModel,
                    showTajweed = showTajweed,
                    arabicFontSize = arabicFontSize,
                    bengaliFontSize = bengaliFontSize,
                    arabicFontName = arabicFontName,
                    bengaliFontName = bengaliFontName,
                    selectedTafsirNames = selectedTafsirNames,
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    primaryAccent = primaryAccent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    onOpenPhotoCard = { showPhotoCardDialog = true }
                )
            }
        }

        if (showPhotoCardDialog) {
            val quickCardPost = remember(currentAyah, surahBanglaName) {
                com.example.utils.PhotoCardBridge.fromAyah(currentAyah, surahBanglaName).toShortPost()
            }
            com.example.ui.screens.PhotoCardCustomizerDialog(
                post = quickCardPost,
                onDismiss = { showPhotoCardDialog = false }
            )
        }
    }
}

private fun parseTafsirSections(rawText: String?, defaultNames: List<String>): List<Pair<String, String>> {
    if (rawText.isNullOrBlank()) return emptyList()

    val boldPattern = Regex("<b>(.*?)</b><br>(.*?)(?=(<b>.*?</b><br>|$))", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    val matches = boldPattern.findAll(rawText).toList()

    if (matches.isNotEmpty()) {
        return matches.map { match ->
            val title = match.groupValues[1].trim()
            val body = match.groupValues[2].trim()
            Pair(title, body)
        }
    }

    val primaryTitle = if (defaultNames.isNotEmpty()) defaultNames.first() else "তাফসীর আল-মুয়াসসার"
    return listOf(Pair(primaryTitle, rawText))
}

@Composable
private fun CollapsibleTafsirCard(
    tafsirTitle: String,
    rawContent: String?,
    isResolving: Boolean,
    tafsirResult: QuranRepository.TafsirResult?,
    cardBgColor: Color,
    cardBorderColor: Color,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    bengaliFontSize: Float,
    bengaliFont: androidx.compose.ui.text.font.FontFamily?,
    onRetry: () -> Unit,
    onOpenPhotoCard: () -> Unit,
    initiallyExpanded: Boolean = true
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val parsedAnnotated = remember(rawContent, primaryAccent) {
        rawContent?.parseHtmlToAnnotatedString(primaryAccent)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBgColor),
        border = BorderStroke(1.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row (Accordion Clickable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp, 18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF059669))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = tafsirTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) "সংক্ষিপ্ত করুন" else "বিস্তারিত দেখুন",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "সংক্ষিপ্ত করুন" else "বিস্তারিত দেখুন",
                        tint = primaryAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    HorizontalDivider(color = cardBorderColor.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (parsedAnnotated != null && parsedAnnotated.isNotEmpty()) {
                        Text(
                            text = parsedAnnotated,
                            fontSize = bengaliFontSize.sp,
                            lineHeight = (bengaliFontSize * 1.65f).sp,
                            fontFamily = bengaliFont,
                            color = textPrimary,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else if (isResolving) {
                        MinimalTafsirLoadingIndicator(
                            text = "তাফসীর লোড হচ্ছে...",
                            subText = "অনলাইন বা সংরক্ষিত ক্যাশ থেকে তাফসীর সংগ্রহ করা হচ্ছে..."
                        )
                    } else {
                        when (tafsirResult) {
                            is QuranRepository.TafsirResult.NoInternet -> {
                                MinimalTafsirFallbackCard(
                                    type = TafsirFallbackType.NO_INTERNET,
                                    onAction = onRetry
                                )
                            }
                            is QuranRepository.TafsirResult.NoTafsirSelected -> {
                                MinimalTafsirFallbackCard(
                                    type = TafsirFallbackType.NO_TAFSIR_SELECTED
                                )
                            }
                            else -> {
                                MinimalTafsirFallbackCard(
                                    type = TafsirFallbackType.NOT_FOUND,
                                    onAction = onRetry
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = onOpenPhotoCard,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "ফটো কার্ড তৈরি করুন",
                                tint = primaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "এই আয়াতের ফটো কার্ড তৈরি করুন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AyahTafsirPageContent(
    ayah: CombinedAyah,
    surahNumber: Int,
    surahBanglaName: String,
    surahArabicName: String,
    viewModel: SurahDetailViewModel,
    showTajweed: Boolean,
    arabicFontSize: Float,
    bengaliFontSize: Float,
    arabicFontName: String,
    bengaliFontName: String,
    selectedTafsirNames: List<String>,
    cardBgColor: Color,
    cardBorderColor: Color,
    primaryAccent: Color,
    textPrimary: Color,
    textSecondary: Color,
    onOpenPhotoCard: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Dynamic tafsir resolution for this ayah if not pre-cached
    var tafsirResult by remember(ayah.number, surahNumber) {
        mutableStateOf<QuranRepository.TafsirResult?>(null)
    }
    var isTafsirResolving by remember(ayah.number, surahNumber) {
        mutableStateOf(false)
    }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(ayah.number, surahNumber, retryTrigger) {
        if (!ayah.tafsirText.isNullOrBlank()) {
            tafsirResult = QuranRepository.TafsirResult.Success(ayah.tafsirText)
            return@LaunchedEffect
        }
        isTafsirResolving = true
        val res = viewModel.resolveAyahTafsir(surahNumber, ayah.numberInSurah)
        tafsirResult = res
        isTafsirResolving = false
    }

    val activeTafsirRaw = (tafsirResult as? QuranRepository.TafsirResult.Success)?.tafsirText
        ?: ayah.tafsirText

    val arabicFont = com.example.ui.theme.getArabicFont(arabicFontName)
    val bengaliFont = com.example.ui.theme.getBengaliFont(bengaliFontName)

    val cleanBengaliText = remember(ayah.bengaliText) {
        if (ayah.bengaliText.contains('<') || ayah.bengaliText.contains('&')) {
            android.text.Html.fromHtml(ayah.bengaliText, android.text.Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            ayah.bengaliText
        }
    }

    val tafsirSections = remember(activeTafsirRaw, selectedTafsirNames) {
        parseTafsirSections(activeTafsirRaw, selectedTafsirNames)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ================= Card 1: সম্পূর্ণ আরবি আয়াত (100% Width) =================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = primaryAccent.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "আয়াত ${DateUtil.toBengaliNumerals(ayah.numberInSurah)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পারা ${DateUtil.toBengaliNumerals(ayah.juz)} • পৃষ্ঠা ${DateUtil.toBengaliNumerals(ayah.page)}",
                            fontSize = 11.sp,
                            color = textSecondary
                        )
                    }

                    Text(
                        text = surahArabicName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryAccent,
                        fontFamily = arabicFont
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val formattedArabic = remember(ayah.arabicText, showTajweed, arabicFontName, arabicFontSize) {
                    if (showTajweed) {
                        IndoPakTajweedParser.parseIndoPakTajweed(
                            text = ayah.arabicText,
                            defaultColor = textPrimary,
                            fontSize = arabicFontSize,
                            showWaqfSigns = true,
                            arabicFontName = arabicFontName
                        )
                    } else {
                        androidx.compose.ui.text.buildAnnotatedString {
                            appendStyledWaqfText(
                                text = ayah.arabicText,
                                fontSize = arabicFontSize,
                                showWaqfSigns = true,
                                arabicFontName = arabicFontName
                            )
                        }
                    }
                }

                Text(
                    text = formattedArabic,
                    fontSize = (arabicFontSize + 4).sp,
                    lineHeight = ((arabicFontSize + 4) * 2.1f).sp,
                    fontFamily = arabicFont,
                    color = textPrimary,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ================= Card 2: বাংলা অনুবাদ (100% Width) =================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            border = BorderStroke(1.dp, cardBorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp, 18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(primaryAccent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "বাংলা অনুবাদ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryAccent
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (ayah.translations.isNotEmpty()) {
                    ayah.translations.forEachIndexed { idx, trans ->
                        if (idx > 0) {
                            HorizontalDivider(
                                color = cardBorderColor.copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                        val cleanText = android.text.Html.fromHtml(
                            trans.text,
                            android.text.Html.FROM_HTML_MODE_LEGACY
                        ).toString()
                        Text(
                            text = cleanText,
                            fontSize = (bengaliFontSize + 1).sp,
                            lineHeight = ((bengaliFontSize + 1) * 1.6f).sp,
                            fontFamily = bengaliFont,
                            color = textPrimary,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Text(
                        text = cleanBengaliText.ifBlank { "অনুবাদ উপলব্ধ নেই" },
                        fontSize = (bengaliFontSize + 1).sp,
                        lineHeight = ((bengaliFontSize + 1) * 1.6f).sp,
                        fontFamily = bengaliFont,
                        color = textPrimary,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // ================= Card 3+: বিস্তারিত কলাপ্সিবল তাফসীরসমূহ (100% Width) =================
        if (tafsirSections.isNotEmpty()) {
            tafsirSections.forEachIndexed { idx, pair ->
                CollapsibleTafsirCard(
                    tafsirTitle = pair.first,
                    rawContent = pair.second,
                    isResolving = isTafsirResolving,
                    tafsirResult = tafsirResult,
                    cardBgColor = cardBgColor,
                    cardBorderColor = cardBorderColor,
                    primaryAccent = primaryAccent,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    bengaliFontSize = bengaliFontSize,
                    bengaliFont = bengaliFont,
                    onRetry = { retryTrigger++ },
                    onOpenPhotoCard = onOpenPhotoCard,
                    initiallyExpanded = (idx == 0)
                )
            }
        } else {
            val primaryTitle = if (selectedTafsirNames.isNotEmpty()) selectedTafsirNames.first() else "তাফসীর আল-মুয়াসসার"
            CollapsibleTafsirCard(
                tafsirTitle = primaryTitle,
                rawContent = activeTafsirRaw,
                isResolving = isTafsirResolving,
                tafsirResult = tafsirResult,
                cardBgColor = cardBgColor,
                cardBorderColor = cardBorderColor,
                primaryAccent = primaryAccent,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                bengaliFontSize = bengaliFontSize,
                bengaliFont = bengaliFont,
                onRetry = { retryTrigger++ },
                onOpenPhotoCard = onOpenPhotoCard,
                initiallyExpanded = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
