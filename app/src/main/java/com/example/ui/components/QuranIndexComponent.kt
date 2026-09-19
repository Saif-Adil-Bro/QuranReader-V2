package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranData
import com.example.data.model.Surah
import com.example.ui.state.UiState
import com.example.ui.theme.BackgroundGreen
import com.example.ui.theme.PrimaryGreen
import com.example.ui.theme.GrayText
import com.example.ui.theme.Border

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveable

private val paraNamesBangla = listOf(
    "আলিফ লাম মীম", "সাইয়াকুল", "তিলকাল রুসুল", "লান তানালু", "ওয়াল মুহসানাত",
    "লা ইউহিব্বুল্লাহ", "ওয়া ইজা সামিউ", "ওয়া লাও আন্নানা", "ক্বলাল মালাইউ", "ওয়া'লামু",
    "ইয়া'তাজিরুন", "ওয়া মা মিন দাব্বাহ", "ওয়া মা উবাররিউ", "রুবামা", "সুবহানাল্লাজি",
    "ক্বলা আলাম", "ইক্বতারা বা লিন্নাস", "ক্বদ আফলাহা", "ওয়া ক্বলাল্লাজিনা", "আম্মান খালাক্ব",
    "উতলু মা উহিয়া", "ওয়া মান ইয়াক্বনুত", "ওয়া মالية", "ফামান আজলামু", "ইলাইহি ইয়ুরাদদু",
    "হা মীম", "ক্বলা ফামা খাতবুকুম", "ক্বদ সামিয়াল্লাহ", "তাবারাকাল্লাজি", "আম্মা ইয়াতাসায়ালুন"
)

private fun getJuzStartPage(juz: Int): Int {
    return com.example.data.HafeziQuranData.getParaStartPage(juz, 1)
}

private fun Int.toBengaliNumerals(): String {
    val bDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return this.toString().map { char ->
        if (char in '0'..'9') bDigits[char - '0'] else char
    }.joinToString("")
}

@Composable
fun QuranIndexComponent(
    modifier: Modifier = Modifier,
    uiState: UiState<List<Surah>>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSurahClick: (Int) -> Unit,
    onJuzClick: (Int) -> Unit,
    onRetryClick: () -> Unit
) {
    val tabs = listOf("সূরা", "পারা")
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val surahListState = rememberLazyListState()
    val paraListState = rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = PrimaryGreen,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = PrimaryGreen,
                    height = 3.dp
                )
            },
            divider = {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedTabIndex == index) PrimaryGreen else GrayText
                        )
                    }
                )
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = {
                Text(
                    text = if (selectedTabIndex == 1) "পারা খুঁজুন..." else "সূরা খুঁজুন...",
                    color = GrayText
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search Icon",
                    tint = GrayText
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
        )

        if (selectedTabIndex == 1) {
            // Para Grid View
            val filteredParas = (1..30).map { juzNumber ->
                val paraName = paraNamesBangla[juzNumber - 1]
                val title = "পারা ${juzNumber.toBengaliNumerals()}"
                val translation = paraName
                Triple(juzNumber, title, translation)
            }.filter {
                searchQuery.isEmpty() ||
                it.second.contains(searchQuery, ignoreCase = true) ||
                it.third.contains(searchQuery, ignoreCase = true) ||
                it.first.toString().contains(searchQuery)
            }

            if (filteredParas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("কোনো পারা পাওয়া যায়নি", color = GrayText)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val columns = maxOf(2, (maxWidth / 160.dp).toInt())
                    LazyColumn(
                        state = paraListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val chunkedParas = filteredParas.chunked(columns)
                        items(chunkedParas, key = { row -> row.first().first }) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { para ->
                                    SurahCard(
                                        modifier = Modifier.weight(1f),
                                        number = para.first.toBengaliNumerals(),
                                        title = para.second,
                                        translation = para.third,
                                        onClick = { onJuzClick(para.first) }
                                    )
                                }
                                val emptySpots = columns - rowItems.size
                                repeat(emptySpots) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Surah Grid View
            when (val state = uiState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        QuranLoadingAnimation(text = "লোড হচ্ছে...")
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "ত্রুটি: ${state.message}", color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onRetryClick) {
                                Text("আবার চেষ্টা করুন")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670-\\u06D6\\u06DC-\\u06ED]")
                    val trimmedQuery = searchQuery.trim()
                    val normalizedQuery = trimmedQuery.replace(diacriticsRegex, "").lowercase()

                    val filteredSurahs = state.data.filter { surah ->
                        val bengaliName = QuranData.surahNames.find { it.first == surah.number }?.second?.first ?: ""
                        val meaning = QuranData.surahNames.find { it.first == surah.number }?.second?.second ?: ""
                        val arabicNameRaw = surah.name ?: ""
                        val normalizedArabicName = arabicNameRaw.replace(diacriticsRegex, "")

                        trimmedQuery.isEmpty() ||
                        surah.englishName.contains(trimmedQuery, ignoreCase = true) ||
                        surah.englishNameTranslation.contains(trimmedQuery, ignoreCase = true) ||
                        arabicNameRaw.contains(trimmedQuery, ignoreCase = true) ||
                        (normalizedQuery.isNotEmpty() && normalizedArabicName.lowercase().contains(normalizedQuery)) ||
                        bengaliName.contains(trimmedQuery, ignoreCase = true) ||
                        meaning.contains(trimmedQuery, ignoreCase = true) ||
                        surah.number.toString().contains(trimmedQuery) ||
                        surah.number.toBengaliNumerals().contains(trimmedQuery)
                    }

                    if (filteredSurahs.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("কোনো সূরা পাওয়া যায়নি", color = GrayText)
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val columns = maxOf(2, (maxWidth / 160.dp).toInt())
                            LazyColumn(
                                state = surahListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val chunkedSurahs = filteredSurahs.chunked(columns)
                                items(chunkedSurahs, key = { row -> row.first().number }) { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { surah ->
                                            val bengaliName = QuranData.surahNames.find { it.first == surah.number }?.second?.first ?: surah.englishName
                                            val translation = QuranData.surahNames.find { it.first == surah.number }?.second?.second ?: surah.englishNameTranslation
                                            val rawType = surah.revelationType
                                            val revelationType = if (rawType.equals("Meccan", ignoreCase = true)) "মাক্কী" else "মাদানী"

                                            SurahCard(
                                                modifier = Modifier.weight(1f),
                                                number = surah.number.toBengaliNumerals(),
                                                title = bengaliName,
                                                translation = translation,
                                                ayahCount = surah.numberOfAyahs,
                                                revelationType = revelationType,
                                                onClick = { onSurahClick(surah.number) }
                                            )
                                        }
                                        val emptySpots = columns - rowItems.size
                                        repeat(emptySpots) {
                                            Spacer(modifier = Modifier.weight(1f))
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
                            .background(BackgroundGreen, RoundedCornerShape(8.dp))
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
                        color = GrayText,
                        fontSize = 10.sp,
                        maxLines = 1,
                        lineHeight = 10.sp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                }
            }
            if (ayahCount != null && revelationType != null) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Border)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.MenuBook,
                        contentDescription = null,
                        tint = GrayText,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${ayahCount.toBengaliNumerals()} আয়াত", color = GrayText, fontSize = 10.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(3.dp).background(GrayText, RoundedCornerShape(50)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(revelationType, color = GrayText, fontSize = 10.sp)
                }
            }
        }
    }
}
