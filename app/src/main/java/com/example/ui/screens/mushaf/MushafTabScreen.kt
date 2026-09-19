package com.example.ui.screens.mushaf

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.data.model.MushafStyle
import com.example.ui.viewmodels.MushafSelectionViewModel

@Composable
fun MushafTabScreen(
    onMushafSelected: (MushafStyle) -> Unit,
    onLastReadSelected: (String, Int) -> Unit,
    viewModel: MushafSelectionViewModel
) {
    val mushafs by viewModel.mushafs.collectAsState()
    val downloadStatus by viewModel.downloadStatus.collectAsState()
    val lastReadMushafId by viewModel.lastReadMushafId.collectAsState()
    val lastReadMushafPage by viewModel.lastReadMushafPage.collectAsState()
    val defaultMushafId by viewModel.defaultMushafId.collectAsState()

    MushafSelectionScreen(
        mushafs = mushafs,
        downloadStatus = downloadStatus,
        lastReadMushafId = lastReadMushafId,
        lastReadMushafPage = lastReadMushafPage,
        defaultMushafId = defaultMushafId,
        onSetDefaultMushaf = { viewModel.setDefaultMushafId(it) },
        onResumeReading = onLastReadSelected,
        onSelectMushaf = onMushafSelected,
        onImportPdf = { viewModel.importCustomPdf(it) },
        onDownload = { viewModel.downloadMushaf(it) },
        onPause = { viewModel.pauseDownload(it) },
        onCancel = { viewModel.cancelDownload(it) },
        onDelete = { viewModel.deleteMushaf(it) }
    )
}
