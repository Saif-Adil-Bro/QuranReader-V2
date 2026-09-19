package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.QuranData
import com.example.ui.components.QuranIndexComponent
import com.example.ui.components.RecentReadItem
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.HomeViewModel
import com.example.utils.DateUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationIndexScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val currentPlayingSurah by viewModel.currentPlayingSurah.collectAsState()
    val selectedQariId by viewModel.selectedQariId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showQariSelectorDialog by remember { mutableStateOf(false) }

    val recentReads = remember(currentPlayingSurah) {
        val sNum = currentPlayingSurah ?: 1
        val sName = QuranData.surahNames.find { it.first == sNum }?.second?.first ?: "ফাতিহা"
        listOf(
            RecentReadItem("সূরা $sName", sNum, 1),
            RecentReadItem("সূরা মারইয়াম", 19, 1),
            RecentReadItem("সূরা ইউসুফ", 12, 1),
            RecentReadItem("সূরা ইয়াসীন", 36, 1)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "তেলাওয়াত প্লেয়ার সূচী",
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
                            tint = MaterialTheme.colorScheme.onSurface
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
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            QuranIndexComponent(
                modifier = Modifier.fillMaxSize(),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSurahClick = { surahNumber ->
                    viewModel.playSurahAudio(surahNumber)
                    onNavigateToPlayer()
                },
                onJuzClick = { juzNumber ->
                    val surahNumber = getSurahForJuz(juzNumber)
                    viewModel.playSurahAudio(surahNumber)
                    onNavigateToPlayer()
                },
                onNavigateToSurahWithAyah = { surahNumber, _ ->
                    viewModel.playSurahAudio(surahNumber)
                    onNavigateToPlayer()
                },
                recentReads = recentReads
            )
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

private fun getSurahForJuz(juz: Int): Int {
    return when (juz) {
        1 -> 1
        2 -> 2
        3 -> 2
        4 -> 3
        5 -> 4
        6 -> 4
        7 -> 5
        8 -> 6
        9 -> 7
        10 -> 8
        11 -> 9
        12 -> 11
        13 -> 12
        14 -> 15
        15 -> 17
        16 -> 18
        17 -> 21
        18 -> 23
        19 -> 25
        20 -> 27
        21 -> 29
        22 -> 33
        23 -> 36
        24 -> 39
        25 -> 41
        26 -> 46
        27 -> 51
        28 -> 58
        29 -> 67
        30 -> 78
        else -> 1
    }
}
