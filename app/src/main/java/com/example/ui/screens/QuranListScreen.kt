package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.QuranIndexComponent
import com.example.ui.viewmodels.HomeViewModel
import com.example.ui.viewmodels.QuranListViewModel

import com.example.ui.theme.PrimaryGreen
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.utils.DateUtil.toBengaliNumerals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranListScreen(
    viewModel: QuranListViewModel,
    homeViewModel: HomeViewModel,
    mode: String = "normal",
    onSurahClick: (Int) -> Unit,
    onNavigateToSurahWithAyah: (Int, String, Int) -> Unit,
    onJuzClick: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val recentTracks by homeViewModel.recentReads.collectAsState()

    val recentReads = remember(recentTracks) {
        recentTracks
            .filter { it.mode == "DETAIL" || it.mode == "LIST" }
            .map { track ->
                com.example.ui.components.RecentReadItem(
                    title = track.title,
                    surahNumber = track.surahNumber,
                    ayahNumber = track.ayahNumber,
                    pageNumber = null,
                    mode = track.mode
                )
            }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (mode == "reading") "প্যারাগ্রাফ পঠন" else "সূরা ও পারা সূচী",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            QuranIndexComponent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSurahClick = onSurahClick,
                onJuzClick = onJuzClick,
                onNavigateToSurahWithAyah = { surah, ayah ->
                    onNavigateToSurahWithAyah(surah, "LIST", ayah)
                },
                recentReads = recentReads,
                onRetryClick = { viewModel.loadSurahs() }
            )
        }
    }
}

