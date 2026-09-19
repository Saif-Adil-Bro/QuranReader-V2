package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranData
import com.example.data.model.SearchMatch
import com.example.data.model.Surah
import com.example.ui.state.UiState
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.SearchViewModel
import com.example.ui.viewmodels.SearchResultItemType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSurah: (Int) -> Unit,
    onNavigateToAyah: (Int, Int) -> Unit = { _, _ -> }
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val theme by viewModel.theme.collectAsState()
    val arabicFont by viewModel.arabicFont.collectAsState()
    val focusManager = LocalFocusManager.current

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (theme) {
        "Dark" -> true
        "Light" -> false
        else -> isSystemDark
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "কুরআন অনুসন্ধান", 
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isDark) Color.White else Color(0xFF1F2937)
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF1F2937)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        }
    ) { paddingValues ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isTablet = maxWidth > 600.dp
            val horizontalPadding = if (isTablet) 32.dp else 0.dp
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
            ) {
                // Search Input Box
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onQueryChange(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    placeholder = { 
                        Text(
                            "বাংলা, ইংরেজি বা আরবি শব্দ দিয়ে খুঁজুন...", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        ) 
                    },
                    leadingIcon = { 
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search",
                            tint = PrimaryGreen
                        ) 
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(
                                    Icons.Default.Clear, 
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = PrimaryGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                        focusedLabelColor = PrimaryGreen
                    )
                )

                if (searchQuery.isBlank()) {
                    // Initial Welcome / Suggestion Screen
                    InitialSearchLayout(
                        onTagClick = { tag ->
                            viewModel.onQueryChange(tag)
                            focusManager.clearFocus()
                        },
                        isDark = isDark
                    )
                } else {
                    // Search Content Results
                    when (val state = uiState) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(), 
                                contentAlignment = Alignment.Center
                            ) {
                                com.example.ui.components.QuranLoadingAnimation(text = "অনুসন্ধান করা হচ্ছে...")
                            }
                        }
                        is UiState.Success -> {
                            val matches = state.data
                            if (matches.isEmpty()) {
                                EmptySearchResultsLayout(searchQuery, isDark)
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Text(
                                            text = "মোট ফলাফল: ${matches.size} টি মিল পাওয়া গেছে",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = PrimaryGreen,
                                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                                        )
                                    }
                                    items(matches) { item ->
                                        when (item) {
                                            is SearchResultItemType.SurahItem -> {
                                                SurahSearchResultItem(
                                                    surah = item.surah,
                                                    banglaName = item.banglaName,
                                                    banglaMeaning = item.banglaMeaning,
                                                    isDark = isDark,
                                                    onClick = {
                                                        onNavigateToSurah(item.surah.number)
                                                    }
                                                )
                                            }
                                            is SearchResultItemType.AyahItem -> {
                                                SearchResultItem(
                                                    match = item.match, 
                                                    searchQuery = searchQuery,
                                                    isDark = isDark,
                                                    arabicText = item.arabicText,
                                                    banglaText = item.banglaText,
                                                    arabicFontName = arabicFont,
                                                    onClick = {
                                                        onNavigateToAyah(item.match.surah.number, item.match.numberInSurah)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        is UiState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(), 
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Info, 
                                        contentDescription = "Error", 
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "দুঃখিত, অনুসন্ধান ব্যর্থ হয়েছে", 
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        state.message, 
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
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

@Composable
fun InitialSearchLayout(
    onTagClick: (String) -> Unit,
    isDark: Boolean
) {
    val popularTags = listOf(
        "সালাত", "ঈমান", "জান্নাত", "তওবা", 
        "الله", "صبر", "صلاة", "قرآن", 
        "ধৈর্য", "রমজান", "তাকওয়া", "ক্ষমা"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cozy welcome icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = PrimaryGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "কুরআনের যেকোনো বিষয় খুঁজুন",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "কুরআনের যেকোনো আয়াত বা শিক্ষা সহজে বাংলা, ইংরেজি অথবা আরবি কীওয়ার্ড দিয়ে অনুসন্ধান করুন।",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "জনপ্রিয় বিষয়সমূহ (Popular Topics)",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PrimaryGreen,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Tag Cloud Flow
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Chunked tags into rows for a cleaner, structured look
            val rows = popularTags.chunked(4)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(1.dp, RoundedCornerShape(100.dp))
                                .background(
                                    if (isDark) Color(0xFF1E293B) else Color.White, 
                                    RoundedCornerShape(100.dp)
                                )
                                .border(
                                    1.dp, 
                                    PrimaryGreen.copy(alpha = 0.15f), 
                                    RoundedCornerShape(100.dp)
                                )
                                .clickable { onTagClick(tag) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tag,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    val emptySpots = 4 - row.size
                    repeat(emptySpots) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchResultsLayout(query: String, isDark: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(
                    if (isDark) Color(0xFF1E293B) else Color(0xFFF3F4F6), 
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "কোনো ফলাফল পাওয়া যায়নি",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "আমরা \"$query\" শব্দটির জন্য কোনো আয়াত খুঁজে পাইনি। অনুগ্রহ করে সঠিক বানানটি নিশ্চিত করুন অথবা অন্য কোনো শব্দ ব্যবহার করুন।",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun SearchResultItem(
    match: SearchMatch, 
    searchQuery: String, 
    isDark: Boolean,
    arabicText: String? = null,
    banglaText: String? = null,
    arabicFontName: String = "Me Quran",
    onClick: () -> Unit
) {
    // Look up Bengali Surah Name & Translation Meaning from local QuranData mapping
    val surahPair = QuranData.surahNames.find { it.first == match.surah.number }
    val banglaSurahName = surahPair?.second?.first ?: match.surah.englishName
    val banglaSurahMeaning = surahPair?.second?.second ?: match.surah.englishNameTranslation

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            1.dp, 
            PrimaryGreen.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah Index Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = match.surah.number.toBengaliNumerals(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Surah names
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "সূরা $banglaSurahName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${match.surah.englishName} • $banglaSurahMeaning",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Ayah Index Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "আয়াত ${match.numberInSurah.toBengaliNumerals()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Subdued divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Arabic Text Section (if available)
            if (!arabicText.isNullOrBlank()) {
                val highlightedArabic = highlightText(
                    text = arabicText,
                    query = searchQuery,
                    highlightColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                )
                androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    Text(
                        text = highlightedArabic,
                        fontFamily = com.example.ui.theme.getArabicFont(arabicFontName),
                        fontSize = 24.sp,
                        lineHeight = 38.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Bengali Translation Section with Keyword Highlighting!
            val displayText = banglaText ?: if (arabicText.isNullOrBlank()) match.text else ""
            if (displayText.isNotBlank()) {
                val highlightedText = highlightText(
                    text = displayText,
                    query = searchQuery,
                    highlightColor = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706) // Soft warm gold/amber accent for high readability
                )

                Text(
                    text = highlightedText,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Builds an AnnotatedString that highlights matches of the query string in the text.
 */
fun highlightText(text: String, query: String, highlightColor: Color): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    
    return buildAnnotatedString {
        var startIdx = 0
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase()
        
        while (true) {
            val idx = lowerText.indexOf(lowerQuery, startIdx)
            if (idx == -1) {
                append(text.substring(startIdx))
                break
            }
            
            // Append the plain text before match
            append(text.substring(startIdx, idx))
            
            // Append the highlighted matched substring
            withStyle(
                style = SpanStyle(
                    color = highlightColor, 
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(text.substring(idx, idx + query.length))
            }
            
            startIdx = idx + query.length
        }
    }
}

@Composable
fun SurahSearchResultItem(
    surah: Surah,
    banglaName: String,
    banglaMeaning: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            1.dp, 
            PrimaryGreen.copy(alpha = 0.25f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Surah Number Badge with a distinctive style
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = surah.number.toBengaliNumerals(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "সূরা $banglaName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        // A distinctive tag/badge indicating "Surah"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryGreen)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "সূরা",
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${surah.englishName} • $banglaMeaning",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val banglaRevelation = if (surah.revelationType.lowercase() == "meccan") "মাক্কী" else "মাদানী"
                    Text(
                        text = banglaRevelation,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryGreen
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${surah.numberOfAyahs.toBengaliNumerals()} আয়াত",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
