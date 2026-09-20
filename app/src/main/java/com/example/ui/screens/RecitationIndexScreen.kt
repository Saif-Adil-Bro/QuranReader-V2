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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QuranIndexComponent
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecitationIndexScreen(
    viewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onNavigateToPlayer: () -> Unit
) {
    val selectedQariId by viewModel.selectedQariId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showQariSelectorDialog by remember { mutableStateOf(false) }

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
                    viewModel.playSurahAudio(surahNumber, 0)
                    onNavigateToPlayer()
                },
                onJuzClick = { juzNumber ->
                    val (surahNumber, startAyah) = getJuzStartSurahAndAyah(juzNumber)
                    viewModel.playSurahAudio(surahNumber, (startAyah - 1).coerceAtLeast(0))
                    onNavigateToPlayer()
                },
                onNavigateToSurahWithAyah = { surahNumber, ayahNumber ->
                    viewModel.playSurahAudio(surahNumber, (ayahNumber - 1).coerceAtLeast(0))
                    onNavigateToPlayer()
                },
                recentReads = null
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

private fun getJuzStartSurahAndAyah(juz: Int): Pair<Int, Int> {
    return when (juz) {
        1 -> Pair(1, 1)
        2 -> Pair(2, 142)
        3 -> Pair(2, 253)
        4 -> Pair(3, 93)
        5 -> Pair(4, 24)
        6 -> Pair(4, 148)
        7 -> Pair(5, 82)
        8 -> Pair(6, 111)
        9 -> Pair(7, 88)
        10 -> Pair(8, 41)
        11 -> Pair(9, 93)
        12 -> Pair(11, 6)
        13 -> Pair(12, 53)
        14 -> Pair(15, 1)
        15 -> Pair(17, 1)
        16 -> Pair(18, 75)
        17 -> Pair(21, 1)
        18 -> Pair(23, 1)
        19 -> Pair(25, 21)
        20 -> Pair(27, 56)
        21 -> Pair(29, 46)
        22 -> Pair(33, 31)
        23 -> Pair(36, 28)
        24 -> Pair(39, 32)
        25 -> Pair(41, 47)
        26 -> Pair(46, 1)
        27 -> Pair(51, 31)
        28 -> Pair(58, 1)
        29 -> Pair(67, 1)
        30 -> Pair(78, 1)
        else -> Pair(1, 1)
    }
}
