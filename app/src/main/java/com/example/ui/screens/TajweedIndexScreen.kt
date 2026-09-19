package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.components.QuranIndexComponent
import com.example.ui.components.RecentReadItem
import com.example.ui.viewmodels.HomeViewModel
import com.example.utils.DateUtil.toBengaliNumerals

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TajweedIndexScreen(
    homeViewModel: HomeViewModel,
    onBackClick: () -> Unit,
    onPageClick: (Int) -> Unit,
    onSurahClick: (Int) -> Unit,
    onJuzClick: (Int) -> Unit,
    onSettingsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val lastReadPage by homeViewModel.lastReadPage.collectAsState()

    val recentReads = remember(lastReadPage) {
        listOf(
            RecentReadItem(
                title = "তাজবীদ পৃষ্ঠা ${lastReadPage.toBengaliNumerals()}",
                surahNumber = 1,
                ayahNumber = 1
            ),
            RecentReadItem("সূরা মারইয়াম আয়াত ২", 19, 2),
            RecentReadItem("সূরা ইউসুফ আয়াত ১১", 12, 11)
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "কালার তাজবীদ সূচী",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
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
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            QuranIndexComponent(
                modifier = Modifier.fillMaxSize(),
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSurahClick = onSurahClick,
                onJuzClick = onJuzClick,
                onNavigateToSurahWithAyah = { surah, _ ->
                    onSurahClick(surah)
                },
                recentReads = recentReads
            )
        }
    }
}
