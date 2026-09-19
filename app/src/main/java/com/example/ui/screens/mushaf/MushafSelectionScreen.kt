package com.example.ui.screens.mushaf

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DownloadStatus
import com.example.data.model.MushafStyle
import com.example.ui.screens.mushaf.components.MushafCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MushafSelectionScreen(
    mushafs: List<MushafStyle>,
    downloadStatus: Map<String, DownloadStatus>,
    lastReadMushafId: String?,
    lastReadMushafPage: Int,
    defaultMushafId: String,
    onSetDefaultMushaf: (String) -> Unit,
    onResumeReading: (String, Int) -> Unit,
    onSelectMushaf: (MushafStyle) -> Unit,
    onImportPdf: (java.io.InputStream) -> Unit,
    onDownload: (MushafStyle) -> Unit,
    onPause: (String) -> Unit,
    onCancel: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    val context = LocalContext.current
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    onImportPdf(inputStream)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("মুসহাফ লাইব্রেরি", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { pdfLauncher.launch("application/pdf") }) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Import PDF",
                            tint = Color(0xFF10B981)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color(0xFF10B981)
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
            if (mushafs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF10B981))
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (lastReadMushafId != null) {
                        val lastMushaf = mushafs.find { it.id == lastReadMushafId }
                        if (lastMushaf != null) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFECFDF5) // Very light elegant emerald green
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Bookmark,
                                                    contentDescription = "Bookmark",
                                                    tint = Color(0xFF10B981),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "সর্বশেষ পঠিত",
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF047857),
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = lastMushaf.nameBengali,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 18.sp,
                                                color = Color(0xFF065F46)
                                            )
                                            Text(
                                                text = "পৃষ্ঠা নম্বর: $lastReadMushafPage",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = Color(0xFF047857)
                                            )
                                        }
                                        Button(
                                            onClick = { onResumeReading(lastReadMushafId, lastReadMushafPage) },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF10B981)
                                            ),
                                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("পড়ুন", color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(mushafs) { mushaf ->
                        MushafCard(
                            mushaf = mushaf,
                            downloadStatus = downloadStatus[mushaf.id],
                            isDefault = defaultMushafId == mushaf.id,
                            onSetDefault = { onSetDefaultMushaf(mushaf.id) },
                            onSelect = { onSelectMushaf(mushaf) },
                            onDownload = { onDownload(mushaf) },
                            onPause = { onPause(mushaf.id) },
                            onCancel = { onCancel(mushaf.id) },
                            onDelete = { onDelete(mushaf.id) }
                        )
                    }
                }
            }
        }
    }
}
