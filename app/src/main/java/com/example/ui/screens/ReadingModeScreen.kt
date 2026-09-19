package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CombinedAyah
import com.example.data.model.removeWaqfSigns
import com.example.data.model.formatWaqfSigns
import com.example.data.model.appendStyledWaqfText
import com.example.ui.state.UiState
import com.example.ui.theme.getArabicFont
import com.example.ui.theme.PrimaryGreen
import com.example.ui.components.AyahNumberCircle
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import com.example.ui.viewmodels.ReadingModeViewModel
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingModeScreen(
    surahNumber: Int,
    viewModel: ReadingModeViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)
    val uiState by viewModel.uiState.collectAsState()
    val arabicFontSize by viewModel.arabicFontSize.collectAsState()
    val arabicFontName by viewModel.arabicFontName.collectAsState()
    val themeRaw by viewModel.theme.collectAsState()
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val theme = when (themeRaw) {
        "System" -> if (isSystemDark) "Dark" else "Light"
        else -> themeRaw
    }
    val tanzilTextStyle by viewModel.tanzilTextStyle.collectAsState()
    val showWaqfSigns by viewModel.showWaqfSigns.collectAsState()
    val arabicLineSpacing by viewModel.arabicLineSpacing.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    var activeSurahNumber by remember(surahNumber) { androidx.compose.runtime.mutableIntStateOf(surahNumber) }

    LaunchedEffect(activeSurahNumber, tanzilTextStyle) {
        viewModel.loadSurah(activeSurahNumber)
    }

    // Determine theme colors
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
                    val firstAyah = (uiState as? UiState.Success)?.data?.firstOrNull()
                    val surahData = firstAyah?.let { com.example.data.QuranData.surahNames.find { s -> s.first == it.surahNumber } }
                    val surahNameBangla = surahData?.second?.first ?: "সূরা $surahNumber"
                    Text(surahNameBangla, fontWeight = FontWeight.Bold, fontSize = 18.sp) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Reading Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = containerColor,
                    titleContentColor = topBarContentColor,
                    navigationIconContentColor = topBarContentColor,
                    actionIconContentColor = topBarContentColor
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.example.ui.components.QuranLoadingAnimation(
                            text = "সুরা লোড হচ্ছে...", 
                            color = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                        )
                    }
                }
                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadSurah(surahNumber) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)
                            )
                        ) {
                            Text("আবার চেষ্টা করুন", color = Color.White)
                        }
                    }
                }
                is UiState.Success -> {
                    val firstAyah = state.data.firstOrNull()
                    val surahData = firstAyah?.let { com.example.data.QuranData.surahNames.find { s -> s.first == it.surahNumber } }
                    val surahNameArabic = surahData?.second?.first ?: "سورة $surahNumber"
                    val juzNum = firstAyah?.juz ?: 1
                    val juzName = "পারা ${juzNum.toBengaliNumerals()}"
                    
                    val ayahsByPage = state.data.groupBy { it.page }
                    
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        ayahsByPage.forEach { (page, ayahs) ->
                            item(key = page) {
                                TanzilMushafFrame(
                                    titleRight = surahNameArabic,
                                    titleLeft = juzName,
                                    pageNumber = page.toBengaliNumerals(),
                                    theme = theme
                                ) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        val arabicFont = getArabicFont(arabicFontName)
                                        
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
                                                
                                                if (firstAyahOfSurah?.numberInSurah == 1 && surahId != 1 && surahId != 9) {
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
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
                                                
                                                val (annotatedString, inlineMap) = remember(surahAyahs, showWaqfSigns, arabicFontSize, theme) {
                                                    val map = mutableMapOf<String, InlineTextContent>()
                                                    val circleSizeDp = (arabicFontSize * 0.9f).coerceIn(24f, 36f)
                                                    val circleSize = circleSizeDp.dp
                                                    val circleFontSize = (arabicFontSize * 0.42f).coerceIn(11f, 16f).sp
                                                    val textColor = when (theme) {
                                                        "Dark" -> Color(0xFFE0E0E0)
                                                        "Sepia" -> Color(0xFF4E342E)
                                                        else -> Color(0xFF1A1A1A)
                                                    }

                                                    val str = buildAnnotatedString {
                                                        surahAyahs.forEachIndexed { index, ayah ->
                                                            var textToDisplay = ayah.arabicText // Base text without processing
                                                            
                                                            if (ayah.numberInSurah == 1 && ayah.surahNumber != 1 && ayah.surahNumber != 9) {
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
                                                                for (prefix in prefixes) {
                                                                    if (textToDisplay.startsWith(prefix)) {
                                                                        textToDisplay = textToDisplay.removePrefix(prefix).trimStart()
                                                                        break
                                                                    }
                                                                }
                                                            }
                                                            
                                                            appendStyledWaqfText(textToDisplay.trim(), arabicFontSize, showWaqfSigns, arabicFontName)
                                                            
                                                            val inlineId = "ayah_circle_${ayah.surahNumber}_${ayah.numberInSurah}"
                                                            append(" ")
                                                            appendInlineContent(inlineId, " [${ayah.numberInSurah}]")

                                                            map[inlineId] = InlineTextContent(
                                                                placeholder = Placeholder(
                                                                    width = (circleSizeDp + 6).sp,
                                                                    height = circleSizeDp.sp,
                                                                    placeholderVerticalAlign = PlaceholderVerticalAlign.AboveBaseline
                                                                )
                                                            ) {
                                                                AyahNumberCircle(
                                                                    number = ayah.numberInSurah,
                                                                    size = circleSize,
                                                                    backgroundColor = Color.Transparent,
                                                                    borderColor = textColor,
                                                                    textColor = textColor,
                                                                    fontSize = circleFontSize
                                                                )
                                                            }
                                                            
                                                            if (index < surahAyahs.lastIndex) {
                                                                append(" ")
                                                            }
                                                        }
                                                    }
                                                    Pair(str, map)
                                                }
                                                var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                                                Text(
                                                    text = annotatedString,
                                                    inlineContent = inlineMap,
                                                    fontSize = arabicFontSize.sp,
                                                    lineHeight = (arabicFontSize * arabicLineSpacing).sp,
                                                    fontFamily = arabicFont,
                                                    color = when (theme) {
                                                        "Dark" -> Color(0xFFE0E0E0)
                                                        "Sepia" -> Color(0xFF4E342E)
                                                        else -> Color(0xFF1A1A1A)
                                                    },
                                                    textAlign = TextAlign.Justify,
                                                    onTextLayout = { textLayoutResult = it },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp)
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
                                                Spacer(modifier = Modifier.height(16.dp))
                                                com.example.ui.components.SurahNavigationButtons(
                                                    currentSurahNumber = activeSurahNumber,
                                                    onNavigateToSurah = { newSurah ->
                                                        activeSurahNumber = newSurah
                                                    },
                                                    containerColor = containerColor,
                                                    contentColor = when (theme) {
                                                        "Dark" -> Color(0xFFE0E0E0)
                                                        "Sepia" -> Color(0xFF4E342E)
                                                        else -> Color(0xFF1A1A1A)
                                                    },
                                                    subtextColor = when (theme) {
                                                        "Dark" -> Color.LightGray
                                                        "Sepia" -> Color(0xFF795548)
                                                        else -> Color.Gray
                                                    }
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
        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = rememberModalBottomSheetState(),
                containerColor = containerColor
            ) {
                ReadingSettingsContent(
                    arabicFontSize = arabicFontSize,
                    onArabicFontSizeChange = { viewModel.setArabicFontSize(it) },
                    theme = theme,
                    onThemeChange = { viewModel.setTheme(it) },
                    tanzilTextStyle = tanzilTextStyle,
                    onTanzilTextStyleChange = { viewModel.setTanzilTextStyle(it) },
                    showWaqfSigns = showWaqfSigns,
                    onShowWaqfSignsToggle = { viewModel.setShowWaqfSigns(it) },
                    arabicLineSpacing = arabicLineSpacing,
                    onArabicLineSpacingChange = { viewModel.setArabicLineSpacing(it) },
                    topBarContentColor = topBarContentColor,
                    containerColor = containerColor
                )
            }
        }
    }
}

@Composable
fun ReadingSettingsContent(
    arabicFontSize: Float,
    onArabicFontSizeChange: (Float) -> Unit,
    theme: String,
    onThemeChange: (String) -> Unit,
    tanzilTextStyle: String,
    onTanzilTextStyleChange: (String) -> Unit,
    showWaqfSigns: Boolean = true,
    onShowWaqfSignsToggle: (Boolean) -> Unit = {},
    arabicLineSpacing: Float = 2.0f,
    onArabicLineSpacingChange: (Float) -> Unit = {},
    topBarContentColor: Color,
    containerColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "পঠন সেটিংস",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = topBarContentColor,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Arabic font size
        com.example.ui.components.SettingAdjustmentRow(
            label = "আরবি হরফের আকার",
            valueText = "${arabicFontSize.toInt()}".toBengaliNumerals(),
            onDecrease = {
                val newSize = (arabicFontSize - 1f).coerceIn(18f, 40f)
                onArabicFontSizeChange(newSize)
            },
            onIncrease = {
                val newSize = (arabicFontSize + 1f).coerceIn(18f, 40f)
                onArabicFontSizeChange(newSize)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Arabic Line Spacing Settings
        com.example.ui.components.SettingAdjustmentRow(
            label = "আরবি লাইন স্পেস",
            valueText = String.format("%.2f", arabicLineSpacing).toBengaliNumerals(),
            onDecrease = {
                val newSpacing = (arabicLineSpacing - 0.05f).coerceIn(2.00f, 3.00f)
                onArabicLineSpacingChange(newSpacing)
            },
            onIncrease = {
                val newSpacing = (arabicLineSpacing + 0.05f).coerceIn(2.00f, 3.00f)
                onArabicLineSpacingChange(newSpacing)
            }
        )

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
                onCheckedChange = onShowWaqfSignsToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = containerColor.copy(alpha = 0.5f)
                )
            )
        }

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
                        .clickable { onThemeChange(tKey) }
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
        
        Spacer(modifier = Modifier.height(20.dp))

        // Tanzil Quran Script selector
        Text(
            text = "কুরআন স্ক্রিপ্ট স্টাইল", 
            style = MaterialTheme.typography.bodyMedium, 
            color = topBarContentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val scripts = listOf(
                Pair("default-indopak", "ডিফল্ট"),
                Pair("quran-uthmani", "উসমানী"),
                Pair("quran-simple", "সহজ"),
                Pair("quran-simple-clean", "ক্লিন"),
                Pair("quran-simple-plain", "প্লেইন")
            )
            scripts.forEach { (styleId, styleName) ->
                val isSel = styleId == tanzilTextStyle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSel) (if (theme == "Dark") Color(0xFF6B5843) else Color(0xFF1E5631)) else containerColor.copy(alpha = 0.5f),
                    contentColor = if (isSel) Color.White else topBarContentColor,
                    border = BorderStroke(1.dp, topBarContentColor.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTanzilTextStyleChange(styleId) }
                ) {
                    Text(
                        text = styleName,
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

@Composable
fun ThemeOption(name: String, currentTheme: String, onClick: () -> Unit) {
    val isSelected = name == currentTheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
