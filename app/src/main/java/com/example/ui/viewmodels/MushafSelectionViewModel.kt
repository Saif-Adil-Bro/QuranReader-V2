package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DownloadState
import com.example.data.model.DownloadStatus
import com.example.data.model.MushafStyle
import com.example.data.repository.MushafRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MushafSelectionViewModel(
    private val repository: MushafRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val lastReadMushafId: StateFlow<String?> = settingsRepository.lastReadMushafIdFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val lastReadMushafPage: StateFlow<Int> = settingsRepository.lastReadMushafPageFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val defaultMushafId: StateFlow<String> = settingsRepository.defaultMushafIdFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "imdadia_hafezi")

    fun setDefaultMushafId(mushafId: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultMushafId(mushafId)
        }
    }

    private val _mushafs = MutableStateFlow<List<MushafStyle>>(emptyList())
    val mushafs: StateFlow<List<MushafStyle>> = _mushafs.asStateFlow()

    private val _downloadStatus = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
    val downloadStatus: StateFlow<Map<String, DownloadStatus>> = _downloadStatus.asStateFlow()

    init {
        loadMushafs()
    }

    private fun loadMushafs() {
        val available = repository.getAvailableMushafs()
        val currentStatusMap = _downloadStatus.value.toMutableMap()
        
        available.forEach { mushaf ->
            val downloadedCount = repository.getDownloadedPagesCount(mushaf.id)
            if (downloadedCount == mushaf.totalPages) {
                currentStatusMap[mushaf.id] = DownloadStatus(mushaf.id, DownloadState.Downloaded, 100, downloadedCount, mushaf.totalPages)
            } else if (downloadedCount > 0 && !currentStatusMap.containsKey(mushaf.id)) {
                // Incomplete download that is not currently downloading
                currentStatusMap[mushaf.id] = DownloadStatus(mushaf.id, DownloadState.NotDownloaded, (downloadedCount * 100) / mushaf.totalPages, downloadedCount, mushaf.totalPages)
            } else if (!currentStatusMap.containsKey(mushaf.id)) {
                currentStatusMap[mushaf.id] = DownloadStatus(mushaf.id, DownloadState.NotDownloaded, 0, 0, mushaf.totalPages)
            }
        }
        
        _downloadStatus.value = currentStatusMap
        _mushafs.value = available
    }

    fun downloadMushaf(mushaf: MushafStyle) {
        viewModelScope.launch {
            repository.downloadMushaf(mushaf, viewModelScope) { status ->
                val currentMap = _downloadStatus.value.toMutableMap()
                currentMap[mushaf.id] = status
                _downloadStatus.value = currentMap
            }
        }
    }

    fun importCustomPdf(inputStream: java.io.InputStream) {
        viewModelScope.launch {
            val success = repository.importCustomPdf(inputStream)
            if (success) {
                loadMushafs()
            }
        }
    }

    fun pauseDownload(mushafId: String) {
        repository.pauseDownload(mushafId)
        val currentMap = _downloadStatus.value.toMutableMap()
        val current = currentMap[mushafId]
        if (current != null) {
            currentMap[mushafId] = current.copy(state = DownloadState.NotDownloaded) // Paused state treated as not downloading but keeping progress
            _downloadStatus.value = currentMap
        }
    }

    fun cancelDownload(mushafId: String) {
        repository.cancelDownload(mushafId)
        val currentMap = _downloadStatus.value.toMutableMap()
        val mushaf = _mushafs.value.find { it.id == mushafId }
        val totalPages = mushaf?.totalPages ?: 604
        currentMap[mushafId] = DownloadStatus(mushafId, DownloadState.NotDownloaded, 0, 0, totalPages)
        _downloadStatus.value = currentMap
    }

    fun deleteMushaf(mushafId: String) {
        if (repository.deleteMushaf(mushafId)) {
            val currentMap = _downloadStatus.value.toMutableMap()
            val mushaf = _mushafs.value.find { it.id == mushafId }
            val totalPages = mushaf?.totalPages ?: 604
            currentMap[mushafId] = DownloadStatus(mushafId, DownloadState.NotDownloaded, 0, 0, totalPages)
            _downloadStatus.value = currentMap
        }
    }
}
