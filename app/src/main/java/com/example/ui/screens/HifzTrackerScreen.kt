package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PrimaryGreen
import com.example.utils.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HifzTrackerScreen(
    onBackClick: () -> Unit,
    onNavigateToKhatam: () -> Unit,
    onNavigateToSurah: (Int) -> Unit
) {
    BackHandler { onBackClick() }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        HifzTrackerManager.initialize(context)
    }

    val items by HifzTrackerManager.itemsFlow.collectAsState()
    val stats by HifzTrackerManager.statsFlow.collectAsState()
    val activeSabaqSurah by HifzTrackerManager.activeSabaqSurah.collectAsState()
    val activeSabaqRange by HifzTrackerManager.activeSabaqRange.collectAsState()
    val isSabaqDoneToday by HifzTrackerManager.isSabaqDoneToday.collectAsState()
    val isSabqiDoneToday by HifzTrackerManager.isSabqiDoneToday.collectAsState()
    val isManzilDoneToday by HifzTrackerManager.isManzilDoneToday.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("সবক (নতুন পড়া)", "সবকি (আমূখতা)", "মঞ্জিল (দোর)", "১১৪ সুরা ডিরেক্টরি")

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf<HifzStatus?>(null) }
    var showEditSabaqDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "কুরআন হিফজ ট্র্যাকার",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "সবক • সবকি • মঞ্জিল মেথডোলজি",
                            fontSize = 11.sp,
                            color = PrimaryGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Quick Switch to Khatam Tracker
                    Surface(
                        onClick = onNavigateToKhatam,
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "খতম ট্র্যাকার",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Overview Stats Banner ---
            HifzStatsBanner(stats = stats)

            // --- Tab Selection ---
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryGreen,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Tab Contents ---
            when (selectedTab) {
                0 -> SabaqTabContent(
                    activeSurahNumber = activeSabaqSurah,
                    activeRange = activeSabaqRange,
                    isDoneToday = isSabaqDoneToday,
                    onMarkDone = { HifzTrackerManager.markSabaqCompletedToday(context) },
                    onEditClick = { showEditSabaqDialog = true },
                    onReadInQuran = { onNavigateToSurah(activeSabaqSurah) }
                )
                1 -> SabqiTabContent(
                    items = items,
                    isDoneToday = isSabqiDoneToday,
                    onToggleDone = { HifzTrackerManager.toggleSabqiCompletedToday(context) },
                    onSurahClick = { onNavigateToSurah(it) }
                )
                2 -> ManzilTabContent(
                    items = items,
                    isDoneToday = isManzilDoneToday,
                    onToggleDone = { HifzTrackerManager.toggleManzilCompletedToday(context) },
                    onMarkRevised = { HifzTrackerManager.markSurahRevised(context, it) },
                    onStrengthChange = { num, strength -> HifzTrackerManager.updateSurahStrength(context, num, strength) },
                    onSurahClick = { onNavigateToSurah(it) }
                )
                3 -> AllSurahsTabContent(
                    items = items,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    selectedFilter = selectedFilter,
                    onFilterChange = { selectedFilter = it },
                    onStatusChange = { num, status -> HifzTrackerManager.updateSurahStatus(context, num, status) },
                    onSurahClick = { onNavigateToSurah(it) }
                )
            }
        }
    }

    if (showEditSabaqDialog) {
        EditSabaqDialog(
            currentSurah = activeSabaqSurah,
            currentStart = activeSabaqRange.first,
            currentEnd = activeSabaqRange.second,
            onDismiss = { showEditSabaqDialog = false },
            onSave = { surah, start, end ->
                HifzTrackerManager.setActiveSabaq(context, surah, start, end)
                showEditSabaqDialog = false
            }
        )
    }
}

@Composable
fun HifzStatsBanner(stats: HifzStatistics) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(68.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        strokeWidth = 6.dp
                    )
                    CircularProgressIndicator(
                        progress = { stats.completionPercentage / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = PrimaryGreen,
                        strokeWidth = 6.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = String.format("%.1f%%", stats.completionPercentage),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "সম্পন্ন",
                            fontSize = 8.sp,
                            color = PrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Numerical Summary
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(title = "হিফজ সুরা", value = "${stats.memorizedSurahs} / 114")
                        StatItem(title = "হিফজ আয়াত", value = "${stats.memorizedAyahs} / 6236")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatItem(title = "চলমান", value = "${stats.memorizingSurahs} টি", tint = Color(0xFFF59E0B))
                        StatItem(title = "রিভিশন প্রয়োজন", value = "${stats.needsRevisionSurahs} টি", tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(title: String, value: String, tint: Color? = null) {
    Column {
        Text(
            text = title,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = tint ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SabaqTabContent(
    activeSurahNumber: Int,
    activeRange: Pair<Int, Int>,
    isDoneToday: Boolean,
    onMarkDone: () -> Unit,
    onEditClick: () -> Unit,
    onReadInQuran: () -> Unit
) {
    val surahName = com.example.data.QuranData.getSurahNameBangla(activeSurahNumber)
    val arabicName = com.example.data.QuranData.getSurahNameArabic(activeSurahNumber)
    val totalAyahs = com.example.data.QuranData.getAyahCount(activeSurahNumber)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // --- Active Sabaq Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = PrimaryGreen.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "আজকের সবক (নতুন পাঠ)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Sabaq",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$activeSurahNumber. $surahName",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = arabicName,
                        fontSize = 18.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "আয়াত রেঞ্জ: ${activeRange.first} নং থেকে ${activeRange.second} নং আয়াত (মোট ${activeRange.second - activeRange.first + 1} আয়াত)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReadInQuran,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("কুরআনে পড়ুন", color = PrimaryGreen, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onMarkDone,
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDoneToday) Color(0xFF10B981) else PrimaryGreen
                            )
                        ) {
                            Icon(
                                imageVector = if (isDoneToday) Icons.Default.CheckCircle else Icons.Default.Done,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isDoneToday) "আজকের সবক সম্পন্ন ✓" else "সবক সম্পন্ন করুন",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {
            // --- Method Explanation Card ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "সবক মুখস্থ করার সুন্নাহ পদ্ধতি",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "১. প্রতিটি আয়াত প্রথমে দেখে দেখে বিশুদ্ধ মাখরাজ ও তাজবীদসহ ৩ থেকে ৫ বার পড়ুন।\n" +
                               "২. এরপর মুখ না দেখে ৩ বার পড়ুন। ভুল হলে আবার দেখে পড়ুন।\n" +
                               "৩. সবক সম্পন্ন হলে 'সবক সম্পন্ন করুন' বাটনে চাপ দিন, পরবর্তী আয়াতের রেঞ্জ স্বয়ংক্রিয়ভাবে প্রস্তুত হয়ে যাবে।",
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SabqiTabContent(
    items: List<SurahHifzItem>,
    isDoneToday: Boolean,
    onToggleDone: () -> Unit,
    onSurahClick: (Int) -> Unit
) {
    // Sabqi = Recently memorized or in progress surahs
    val sabqiItems = items.filter { it.status == HifzStatus.MEMORIZING || (it.status == HifzStatus.MEMORIZED && it.number >= 78) }
        .take(10)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "সবকি / আমুখতা (সাম্প্রতিক রিভিশন)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "সম্প্রতি মুখস্থ করা শেষ ৫-১০ পৃষ্ঠা প্রতিদিন অন্তত একবার রিভিশন দেওয়া আবশ্যক।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onToggleDone,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDoneToday) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isDoneToday) "সম্পন্ন ✓" else "আজ তিলাওয়াত করেছি",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "সাম্প্রতিক সবকির তালিকা (${sabqiItems.size}টি সুরা)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        items(sabqiItems) { surah ->
            SurahCompactCard(surah = surah, onClick = { onSurahClick(surah.number) })
        }
    }
}

@Composable
fun ManzilTabContent(
    items: List<SurahHifzItem>,
    isDoneToday: Boolean,
    onToggleDone: () -> Unit,
    onMarkRevised: (Int) -> Unit,
    onStrengthChange: (Int, HifzStrength) -> Unit,
    onSurahClick: (Int) -> Unit
) {
    val memorizedItems = items.filter { it.status == HifzStatus.MEMORIZED || it.status == HifzStatus.NEEDS_REVISION }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মঞ্জিল / দোর (পেছনের স্থায়ী রিভিশন)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "পূর্বে হিফজ করা অংশ নিয়মিত চক্রাকারে তিলাওয়াত করে মজবুত রাখুন।",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onToggleDone,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDoneToday) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = if (isDoneToday) "আজকের মঞ্জিল সম্পন্ন ✓" else "মঞ্জিল সম্পন্ন করেছি",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (memorizedItems.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "এখনো কোনো সুরা সম্পূর্ণ হিফজ মার্ক করা হয়নি। '১১৪ সুরা ডিরেক্টরি' থেকে সুরা হিফজ মার্ক করুন।",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(memorizedItems) { surah ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.clickable { onSurahClick(surah.number) }) {
                                Text(
                                    text = "${surah.number}. ${surah.banglaName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${surah.arabicName} • ${surah.totalAyahs} আয়াত",
                                    fontSize = 11.sp,
                                    color = PrimaryGreen
                                )
                            }

                            // Mark Revised Button
                            OutlinedButton(
                                onClick = { onMarkRevised(surah.number) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("রিভিশন সম্পন্ন ✓", fontSize = 10.sp, color = PrimaryGreen)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Strength Selector Chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "ধারণক্ষমতা:",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            HifzStrength.values().forEach { strength ->
                                val active = surah.strength == strength
                                val chipColor = when (strength) {
                                    HifzStrength.STRONG -> Color(0xFF10B981)
                                    HifzStrength.MEDIUM -> Color(0xFFF59E0B)
                                    HifzStrength.WEAK -> Color(0xFFEF4444)
                                }
                                Surface(
                                    onClick = { onStrengthChange(surah.number, strength) },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (active) chipColor else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.padding(2.dp)
                                ) {
                                    Text(
                                        text = strength.labelBn,
                                        fontSize = 10.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
fun AllSurahsTabContent(
    items: List<SurahHifzItem>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilter: HifzStatus?,
    onFilterChange: (HifzStatus?) -> Unit,
    onStatusChange: (Int, HifzStatus) -> Unit,
    onSurahClick: (Int) -> Unit
) {
    val filtered = items.filter { item ->
        val matchesSearch = searchQuery.isBlank() ||
                item.banglaName.contains(searchQuery, ignoreCase = true) ||
                item.englishName.contains(searchQuery, ignoreCase = true) ||
                item.number.toString() == searchQuery.trim()
        val matchesFilter = selectedFilter == null || item.status == selectedFilter
        matchesSearch && matchesFilter
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Search TextField
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("সুরা নম্বর বা নাম দিয়ে খুঁজুন...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { onFilterChange(null) },
                label = { Text("সকল (${items.size})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == HifzStatus.MEMORIZED,
                onClick = { onFilterChange(if (selectedFilter == HifzStatus.MEMORIZED) null else HifzStatus.MEMORIZED) },
                label = { Text("হিফজ (${items.count { it.status == HifzStatus.MEMORIZED }})", fontSize = 11.sp) }
            )
            FilterChip(
                selected = selectedFilter == HifzStatus.MEMORIZING,
                onClick = { onFilterChange(if (selectedFilter == HifzStatus.MEMORIZING) null else HifzStatus.MEMORIZING) },
                label = { Text("চলমান (${items.count { it.status == HifzStatus.MEMORIZING }})", fontSize = 11.sp) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { surah ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSurahClick(surah.number) }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = PrimaryGreen.copy(alpha = 0.12f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${surah.number}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = surah.banglaName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "${surah.arabicName} • ${surah.totalAyahs} আয়াত • পারা ${surah.juz}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        }

                        // Status Selector Toggle
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(HifzStatus.MEMORIZING, HifzStatus.MEMORIZED).forEach { status ->
                                val active = surah.status == status
                                val col = if (status == HifzStatus.MEMORIZED) Color(0xFF10B981) else Color(0xFFF59E0B)
                                Surface(
                                    onClick = {
                                        val newStatus = if (active) HifzStatus.NOT_STARTED else status
                                        onStatusChange(surah.number, newStatus)
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (active) col else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (active) col else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = if (status == HifzStatus.MEMORIZED) "হিফজ" else "চলছে",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
fun SurahCompactCard(surah: SurahHifzItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryGreen.copy(alpha = 0.1f),
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${surah.number}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(surah.banglaName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("${surah.totalAyahs} আয়াত • পারা ${surah.juz}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (surah.status == HifzStatus.MEMORIZED) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
            ) {
                Text(
                    text = surah.status.labelBn,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (surah.status == HifzStatus.MEMORIZED) Color(0xFF10B981) else Color(0xFFF59E0B),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun EditSabaqDialog(
    currentSurah: Int,
    currentStart: Int,
    currentEnd: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var surahNum by remember { mutableIntStateOf(currentSurah) }
    var startAyah by remember { mutableIntStateOf(currentStart) }
    var endAyah by remember { mutableIntStateOf(currentEnd) }

    val totalAyahs = com.example.data.QuranData.getAyahCount(surahNum)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("সবক (নতুন পাঠ) নির্বাচন", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "সুরা: $surahNum. ${com.example.data.QuranData.getSurahNameBangla(surahNum)} (মোট $totalAyahs আয়াত)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimaryGreen
                )

                // Surah Number Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("সুরা নং:", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                    IconButton(onClick = { if (surahNum > 1) { surahNum--; startAyah = 1; endAyah = minOf(7, com.example.data.QuranData.getAyahCount(surahNum)) } }) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("$surahNum", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (surahNum < 114) { surahNum++; startAyah = 1; endAyah = minOf(7, com.example.data.QuranData.getAyahCount(surahNum)) } }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }

                // Start Ayah Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("শুরু আয়াত:", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                    IconButton(onClick = { if (startAyah > 1) startAyah-- }) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("$startAyah", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (startAyah < totalAyahs) { startAyah++; if (endAyah < startAyah) endAyah = startAyah } }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }

                // End Ayah Selector
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("শেষ আয়াত:", fontSize = 12.sp, modifier = Modifier.width(70.dp))
                    IconButton(onClick = { if (endAyah > startAyah) endAyah-- }) {
                        Icon(Icons.Default.Remove, contentDescription = null)
                    }
                    Text("$endAyah", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    IconButton(onClick = { if (endAyah < totalAyahs) endAyah++ }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(surahNum, startAyah, endAyah) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
