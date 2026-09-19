package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.QuranData
import com.example.data.model.Surah
import com.example.data.surahInfoList
import com.example.ui.state.UiState
import com.example.utils.DateUtil

private fun Int.toBengaliNumerals(): String = DateUtil.toBengaliNumerals(this)

val surahNameFont = FontFamily(Font(R.font.surah_name_v4))

fun getSurahNameGlyph(surahNumber: Int): String {
    return if (surahNumber in 1..114) {
        Char(0xE000 + surahNumber).toString()
    } else {
        ""
    }
}

data class QuickLinkItem(
    val title: String,
    val surahNumber: Int,
    val ayahNumber: Int = 1
)

data class RecentReadItem(
    val title: String,
    val surahNumber: Int,
    val ayahNumber: Int = 1,
    val pageNumber: Int? = null,
    val mode: String? = null
)

private val defaultQuickLinks = listOf(
    QuickLinkItem("আয়াতুল কুরসী", 2, 255),
    QuickLinkItem("সূরা ইয়াসীন", 36, 1),
    QuickLinkItem("সূরা আল কাহফ", 18, 1),
    QuickLinkItem("সূরা আর-রহমান", 55, 1),
    QuickLinkItem("সূরা আল মূলক", 67, 1),
    QuickLinkItem("সূরা আল ওয়াকিয়া", 56, 1)
)

private val paraNamesBangla = listOf(
    "আলিফ লাম মীম", "সাইয়াকুল", "তিলকাল রুসুল", "লান তানালু", "ওয়াল মুহসানাত",
    "লা ইউহিব্বুল্লাহ", "ওয়া ইজা সামিউ", "ওয়া লাও আন্নানা", "ক্বলাল মালাইউ", "ওয়া'লামু",
    "ইয়া'তাজিরুন", "ওয়া মা মিন দাব্বাহ", "ওয়া মা উবাররিউ", "রুবামা", "সুবহানাল্লাজি",
    "ক্বলা আলাম", "ইক্বতারা বা লিন্নাস", "ক্বদ আফলাহা", "ওয়া ক্বলাল্লাজিনা", "আম্মান খালাক্ব",
    "উতলু মা উহিয়া", "ওয়া মান ইয়াক্বনুত", "ওয়া মالية", "ফামান আজলামু", "ইলাইহি ইয়ুরাদদু",
    "হা মীম", "ক্বলা ফামা খাতবুকুম", "ক্বদ সামিয়াল্লাহ", "তাবারাকাল্লাজি", "আম্মা ইয়াতাসায়ালুন"
)

private val paraNamesArabic = listOf(
    "الم", "سَيَقُولُ", "تِلْكَ الرُّسُلُ", "لَنْ تَنَالُوا", "وَالْمُحْصَنَاتُ",
    "لَا يُحِبُّ اللَّهُ", "وَإِذَا سَمِعُوا", "وَلَوْ أَنَّنَا", "قَالَ الْمَلَأُ", "وَاعْلَمُوا",
    "يَعْتَذِرُونَ", "وَمَا مِنْ دَابَّةٍ", "وَمَا أُبَرِّئُ", "رُبَمَا", "سُبْحَانَ الَّذِي",
    "قَالَ أَلَمْ", "اقْتَرَبَ لِلنَّاسِ", "قَدْ أَفْلَحَ", "وَقَالَ الَّذِينَ", "أَمَّنْ خَلَقَ",
    "اتْلُ مَا أُوحِيَ", "وَمَنْ يَقْنُتْ", "وَمَا لِيَ", "فَمَنْ أَظْلَمُ", "إِلَيْهِ يُرَدُّ",
    "حم", "قَالَ فَمَا خَطْبُكُمْ", "قَدْ سَمِعَ اللَّهُ", "تَبَارَكَ الَّذِي", "عَمَّ يَتَسَاءَلُونَ"
)

private val paraSurahRange = listOf(
    "সূরা আল ফাতিহা ১ - আল বাকারা ১৪১",
    "সূরা আল বাকারা ১৪২ - ২৫২",
    "সূরা আল বাকারা ২৫৩ - আলে ইমরান ৯২",
    "সূরা আলে ইমরান ৯৩ - আন নিসা ২৩",
    "সূরা আন নিসা ২৪ - ১৪৭",
    "সূরা আন নিসা ১৪৮ - আল মায়িদাহ ৮১",
    "সূরা আল মায়িদাহ ৮২ - আল আনআম ১১০",
    "সূরা আল আনআম ১১১ - আল আরাফ ৮৭",
    "সূরা আল আরাফ ৮৮ - আল আনফাল ৪০",
    "সূরা আল আনফাল ৪১ - আত তাওবাহ ৯২",
    "সূরা আত তাওবাহ ৯৩ - হুদ ৫",
    "সূরা হুদ ৬ - ইউসুফ ৫২",
    "সূরা ইউসুফ ৫৩ - ইবরাহিম ৫২",
    "সূরা আল হিজর ১ - আন নাহল ১২৮",
    "সূরা আল ইসরা ১ - আল কাহফ ৭৪",
    "সূরা আল কাহফ ৭৫ - তা-হা ১৩৫",
    "সূরা আল আম্বিয়া ১ - আল হজ ৭৮",
    "সূরা আল মু'মিনুন ১ - আল ফুরকান ২০",
    "সূরা আল ফুরকান ২১ - আন নামল ৫৫",
    "সূরা আন নামল ৫৬ - আল আনকাবুত ৪৫",
    "সূরা আল আনকাবুত ৪৬ - আল আহযাব ৩০",
    "সূরা আল আহযাব ৩১ - ইয়াসীন ২১",
    "সূরা ইয়াসীন ২২ - আয যুমার ৩১",
    "সূরা আয যুমার ৩২ - হা-মীম সিজদাহ ৪৬",
    "সূরা হা-মীম সিজদাহ ৪৭ - আল জাসিয়াহ ৩৭",
    "সূরা আল আহকাফ ১ - আয যারিয়াত ৩০",
    "সূরা আয যারিয়াত ৩১ - আল হাদীদ ২৯",
    "সূরা আল মুজাদালাহ ১ - আত তাহরীম ১২",
    "সূরা আল মুলক ১ - আল মুরসালাত ৫০",
    "সূরা আন নাবা ১ - আন নাস ৬"
)

private val madaniSurahs = setOf(2, 3, 4, 5, 8, 9, 22, 24, 33, 47, 48, 49, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 76, 98, 110)

fun isSurahMadani(surahNumber: Int): Boolean = madaniSurahs.contains(surahNumber)

fun getCleanArabicSurahName(surahNumber: Int): String {
    val raw = surahInfoList.find { it.first == surahNumber }?.second?.arabicName ?: ""
    return raw.replace("سُورَةُ", "").trim()
}

@Composable
fun QuranIndexComponent(
    modifier: Modifier = Modifier,
    uiState: UiState<List<Surah>> = UiState.Success(emptyList()),
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onSurahClick: (Int) -> Unit,
    onJuzClick: (Int) -> Unit,
    onPageClick: ((Int) -> Unit)? = null,
    onNavigateToSurahWithAyah: ((Int, Int) -> Unit)? = null,
    recentReads: List<RecentReadItem>? = null,
    onRetryClick: () -> Unit = {}
) {
    // Dynamic Dark/Light detection based on current App background luminance
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) } // 0: সূরাসমূহ, 1: পারা
    val surahListState = rememberLazyListState()
    val paraListState = rememberLazyListState()

    // Adaptive Theme Palette
    val emeraldAccent = if (isDark) Color(0xFF34D399) else Color(0xFF059669)
    val emeraldBorder = if (isDark) Color(0xFF10B981).copy(alpha = 0.40f) else Color(0xFF10B981).copy(alpha = 0.35f)
    val chipBg = if (isDark) Color(0xFF18181B) else Color.White
    val chipBorder = if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF10B981).copy(alpha = 0.25f)
    val chipTextColor = if (isDark) Color(0xFF34D399) else Color(0xFF047857)

    val inactivePillBg = if (isDark) Color(0xFF18181B) else Color.White
    val inactivePillBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)
    val inactivePillText = if (isDark) Color(0xFF9CA3AF) else Color(0xFF475569)

    val searchFieldBg = if (isDark) Color(0xFF18181B) else Color.White
    val searchFieldBorder = if (isDark) Color(0xFF27272A) else Color(0xFFE2E8F0)

    // Data parsing
    val allSurahs = remember {
        QuranData.surahNames.map { (number, pair) ->
            val info = surahInfoList.find { it.first == number }?.second
            val isMadani = isSurahMadani(number)
            val cleanArabic = getCleanArabicSurahName(number)
            SurahItemData(
                number = number,
                banglaName = pair.first,
                meaning = pair.second,
                arabicName = cleanArabic,
                ayahCount = info?.ayahCount ?: 0,
                isMadani = isMadani
            )
        }
    }

    val filteredSurahs = remember(searchQuery, allSurahs) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            allSurahs
        } else {
            val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670-\\u06D6\\u06DC-\\u06ED]")
            val normalizedQuery = trimmed.replace(diacriticsRegex, "").lowercase()
            allSurahs.filter { item ->
                item.banglaName.contains(trimmed, ignoreCase = true) ||
                item.meaning.contains(trimmed, ignoreCase = true) ||
                item.arabicName.replace(diacriticsRegex, "").contains(normalizedQuery, ignoreCase = true) ||
                item.number.toString().contains(trimmed) ||
                item.number.toBengaliNumerals().contains(trimmed)
            }
        }
    }

    val filteredParas = remember(searchQuery) {
        val trimmed = searchQuery.trim()
        (1..30).map { juzNum ->
            val bName = paraNamesBangla[juzNum - 1]
            val aName = paraNamesArabic[juzNum - 1]
            val range = paraSurahRange[juzNum - 1]
            ParaItemData(
                number = juzNum,
                banglaName = bName,
                arabicName = aName,
                surahRange = range
            )
        }.filter {
            trimmed.isEmpty() ||
            it.banglaName.contains(trimmed, ignoreCase = true) ||
            it.arabicName.contains(trimmed, ignoreCase = true) ||
            it.surahRange.contains(trimmed, ignoreCase = true) ||
            it.number.toString().contains(trimmed) ||
            it.number.toBengaliNumerals().contains(trimmed)
        }
    }

    val currentListState = if (selectedTabIndex == 0) surahListState else paraListState

    LazyColumn(
        state = currentListState,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. Search Bar
        item(key = "search_field") {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        text = if (selectedTabIndex == 1) "পারা খুঁজুন..." else "সূরা খুঁজুন (নাম বা নম্বর)...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = emeraldAccent,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = emeraldAccent,
                    unfocusedBorderColor = searchFieldBorder,
                    focusedContainerColor = searchFieldBg,
                    unfocusedContainerColor = searchFieldBg,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }

        // When search is not active, show Recently Read & Quick Links
        if (searchQuery.isEmpty()) {
            // 2. সর্বশেষ পঠিত (Recently Read) - Show ONLY when there is history/track, max 5 items
            if (!recentReads.isNullOrEmpty()) {
                val displayRecentReads = recentReads.take(5)
                item(key = "recent_reads_section") {
                    Column(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                        Text(
                            text = "সর্বশেষ পঠিত",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            displayRecentReads.forEach { item ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(chipBg)
                                        .border(1.dp, chipBorder, RoundedCornerShape(100.dp))
                                        .clickable {
                                            if (item.pageNumber != null && onPageClick != null) {
                                                onPageClick(item.pageNumber)
                                            } else if (onNavigateToSurahWithAyah != null) {
                                                onNavigateToSurahWithAyah(item.surahNumber, item.ayahNumber)
                                            } else {
                                                onSurahClick(item.surahNumber)
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = item.title,
                                        color = chipTextColor,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. কুইক লিংক (Quick Links)
            item(key = "quick_links_section") {
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Text(
                        text = "কুইক লিংক",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        defaultQuickLinks.forEach { qLink ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(100.dp))
                                    .clickable {
                                        onNavigateToSurahWithAyah?.invoke(qLink.surahNumber, qLink.ayahNumber)
                                            ?: onSurahClick(qLink.surahNumber)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = qLink.title,
                                    color = chipTextColor,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Pill Tab Switcher [ সূরাসমূহ ] [ পারা ]
        item(key = "tab_switcher_section") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Tab: সূরাসমূহ
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (selectedTabIndex == 0) emeraldAccent else inactivePillBg)
                        .border(
                            width = 1.dp,
                            color = if (selectedTabIndex == 0) emeraldAccent else inactivePillBorder,
                            shape = RoundedCornerShape(100.dp)
                        )
                        .clickable { selectedTabIndex = 0 }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "সূরাসমূহ",
                        color = if (selectedTabIndex == 0) Color.White else inactivePillText,
                        fontSize = 13.5.sp,
                        fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Tab: পারা
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (selectedTabIndex == 1) emeraldAccent else inactivePillBg)
                        .border(
                            width = 1.dp,
                            color = if (selectedTabIndex == 1) emeraldAccent else inactivePillBorder,
                            shape = RoundedCornerShape(100.dp)
                        )
                        .clickable { selectedTabIndex = 1 }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "পারা",
                        color = if (selectedTabIndex == 1) Color.White else inactivePillText,
                        fontSize = 13.5.sp,
                        fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        // 5. Content List (Surahs or Paras)
        if (selectedTabIndex == 0) {
            // Surah List
            if (filteredSurahs.isEmpty()) {
                item(key = "empty_surahs_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো সূরা পাওয়া যায়নি",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                items(filteredSurahs, key = { "surah_${it.number}" }) { surah ->
                    SurahIndexRow(
                        item = surah,
                        emeraldAccent = emeraldAccent,
                        emeraldBorder = emeraldBorder,
                        onClick = { onSurahClick(surah.number) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            // Para List
            if (filteredParas.isEmpty()) {
                item(key = "empty_paras_state") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "কোনো পারা পাওয়া যায়নি",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 15.sp
                        )
                    }
                }
            } else {
                items(filteredParas, key = { "para_${it.number}" }) { para ->
                    ParaIndexRow(
                        item = para,
                        emeraldAccent = emeraldAccent,
                        emeraldBorder = emeraldBorder,
                        onClick = { onJuzClick(para.number) }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        thickness = 0.6.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

data class SurahItemData(
    val number: Int,
    val banglaName: String,
    val meaning: String,
    val arabicName: String,
    val ayahCount: Int,
    val isMadani: Boolean
)

data class ParaItemData(
    val number: Int,
    val banglaName: String,
    val arabicName: String,
    val surahRange: String
)

@Composable
fun SurahIndexRow(
    item: SurahItemData,
    emeraldAccent: Color,
    emeraldBorder: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Circular Badge with Bengali number
        Box(
            modifier = Modifier
                .size(38.dp)
                .border(1.2.dp, emeraldAccent.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.number.toBengaliNumerals(),
                color = emeraldAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Middle: Surah Bangla Name and Type/Ayahs
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "সূরা ${item.banglaName}",
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = if (item.isMadani) R.drawable.annawabu else R.drawable.kaaba),
                    contentDescription = if (item.isMadani) "মাদানী" else "মাক্কী",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (item.isMadani) "মাদানী" else "মাক্কী",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.ayahCount.toBengaliNumerals()} আয়াত",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right: Arabic Calligraphy / Surah Name using Tarteel V4 Font
        Text(
            text = getSurahNameGlyph(item.number),
            fontFamily = surahNameFont,
            color = emeraldAccent,
            fontSize = 32.sp,
            maxLines = 1
        )
    }
}

@Composable
fun ParaIndexRow(
    item: ParaItemData,
    emeraldAccent: Color,
    emeraldBorder: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Circular Badge with Para number
        Box(
            modifier = Modifier
                .size(38.dp)
                .border(1.2.dp, emeraldAccent.copy(alpha = 0.7f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.number.toBengaliNumerals(),
                color = emeraldAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Middle: Para Bangla Name & Range
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "পারা ${item.number.toBengaliNumerals()}: ${item.banglaName}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = item.surahRange,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Right: Arabic Name of Para
        Text(
            text = item.arabicName,
            color = emeraldAccent,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}
