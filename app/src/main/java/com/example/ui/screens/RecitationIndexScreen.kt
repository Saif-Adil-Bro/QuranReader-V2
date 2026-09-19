package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranData
import com.example.ui.theme.*
import com.example.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationIndexScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val surahList by viewModel.surahs.collectAsState()
    val currentPlayingSurah by viewModel.currentPlayingSurah.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val selectedQariId by viewModel.selectedQariId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showQariSelectorDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val filteredSurahList = remember(searchQuery, surahList) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isEmpty()) {
            surahList
        } else {
            val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670-\\u06D6\\u06DC-\\u06ED]")
            val normalizedQuery = trimmedQuery.replace(diacriticsRegex, "").lowercase()

            surahList.filter { surah ->
                val surahNamePair = QuranData.surahNames.find { it.first == surah.number }
                val bengaliName = surahNamePair?.second?.first ?: ""
                val bengaliMeaning = surahNamePair?.second?.second ?: ""
                val arabicNameRaw = surah.name ?: ""
                val normalizedArabicName = arabicNameRaw.replace(diacriticsRegex, "")

                surah.englishName.contains(trimmedQuery, ignoreCase = true) ||
                surah.englishNameTranslation.contains(trimmedQuery, ignoreCase = true) ||
                arabicNameRaw.contains(trimmedQuery, ignoreCase = true) ||
                (normalizedQuery.isNotEmpty() && normalizedArabicName.lowercase().contains(normalizedQuery)) ||
                bengaliName.contains(trimmedQuery, ignoreCase = true) ||
                bengaliMeaning.contains(trimmedQuery, ignoreCase = true) ||
                surah.number.toString().contains(trimmedQuery) ||
                surah.number.toBengaliNumerals().contains(trimmedQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "তেলাওয়াত প্লেয়ার",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "কুরআন শুনুন এবং রিফ্রেশ করুন",
                            fontSize = 11.sp,
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
                            tint = PrimaryGreen
                        )
                    }
                },
                actions = {
                    Surface(
                        onClick = { showQariSelectorDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = PrimaryGreen.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = "ক্বারী সিলেক্ট করুন",
                                tint = PrimaryGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "ক্বারী",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.shadow(2.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Search and selection header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("সূরা খুঁজুন (যেমন: ফাতিহা, Fatihah, 1)", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GrayText) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = GrayText)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = Border
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (searchQuery.isEmpty()) "সকল সূরাসমূহ (${surahList.size.toBengaliNumerals()})" else "খোঁজা হয়েছে (${filteredSurahList.size.toBengaliNumerals()})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Surah list for playing
                if (filteredSurahList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = GrayText,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "কোনো সূরা খুঁজে পাওয়া যায়নি!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredSurahList, key = { it.number }) { surah ->
                            val isCurrent = currentPlayingSurah == surah.number
                            val surahNamePair = QuranData.surahNames.find { it.first == surah.number }
                            val bengaliName = surahNamePair?.second?.first ?: surah.englishName
                            val bengaliMeaning = surahNamePair?.second?.second ?: surah.englishNameTranslation

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .clickable {
                                        if (isCurrent) {
                                            // Already current, just navigate to show player screen
                                            onNavigateToPlayer()
                                        } else {
                                            // Play selected Surah and navigate to player screen
                                            viewModel.playSurahAudio(surah.number)
                                            onNavigateToPlayer()
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isCurrent) PrimaryGreen.copy(alpha = 0.4f) else Border.copy(alpha = 0.5f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Surah index box
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (isCurrent) PrimaryGreen else MaterialTheme.colorScheme.primaryContainer,
                                                RoundedCornerShape(8.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = surah.number.toBengaliNumerals(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isCurrent) MaterialTheme.colorScheme.surface else PrimaryGreen
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(14.dp))

                                    // Name information
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "সূরা $bengaliName",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (isCurrent) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(${surah.englishName})",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "$bengaliMeaning • ${surah.numberOfAyahs.toBengaliNumerals()} আয়াত",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Playback status icon / button
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCurrent && isPlaying) PrimaryGreen else MaterialTheme.colorScheme.primaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrent && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = "Play/Pause",
                                            tint = if (isCurrent && isPlaying) MaterialTheme.colorScheme.surface else PrimaryGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom mini bar if a surah is active/playing
            if (currentPlayingSurah != null) {
                val surahNamePair = QuranData.surahNames.find { it.first == currentPlayingSurah }
                val bengaliName = surahNamePair?.second?.first ?: "সূরা"
                
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .shadow(8.dp, RoundedCornerShape(16.dp))
                        .clickable { onNavigateToPlayer() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryGreen),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সূরা $bengaliName বাজছে",
                                color = MaterialTheme.colorScheme.surface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isPlaying) "ট্যাপ করে প্লেয়ার স্ক্রিনে যান" else "তেলাওয়াত বন্ধ রয়েছে",
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    viewModel.pauseSurahAudio()
                                } else {
                                    viewModel.resumeSurahAudio()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showQariSelectorDialog) {
        QariSelectorDialog(
            selectedQariId = selectedQariId,
            onDismiss = { showQariSelectorDialog = false },
            onSelectQari = { qariId ->
                viewModel.setSelectedQariId(qariId)
                showQariSelectorDialog = false
            }
        )
    }
}
