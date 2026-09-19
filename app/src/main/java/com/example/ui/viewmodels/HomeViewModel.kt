package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.QuranRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.MushafRepository
import com.example.data.repository.AudioRepository
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.Surah
import com.example.data.model.CombinedAyah
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException

class HomeViewModel(
    private val settingsRepository: SettingsRepository,
    private val quranRepository: QuranRepository,
    private val mushafRepository: MushafRepository,
    private val bookmarkDao: BookmarkDao,
    private val audioRepository: AudioRepository
) : ViewModel() {

    val lastReadSurah: StateFlow<Int> = settingsRepository.lastReadSurahFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 1
        )

    val defaultMushafId: StateFlow<String> = settingsRepository.defaultMushafIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "imdadia_hafezi"
        )

    val lastReadPage: StateFlow<Int> = settingsRepository.lastReadPageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 1
        )

    val lastReadMode: StateFlow<String> = settingsRepository.lastReadModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "DETAIL"
        )

    val lastReadAyah: StateFlow<Int> = settingsRepository.lastReadAyahFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 1
        )

    val lastReadMushafId: StateFlow<String?> = settingsRepository.lastReadMushafIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val lastReadMushafPage: StateFlow<Int> = settingsRepository.lastReadMushafPageFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 1
        )

    val hijriOffset: StateFlow<Int> = settingsRepository.hijriOffsetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 0
        )

    val combinedHijriOffset: StateFlow<Int> = settingsRepository.combinedHijriOffsetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 0
        )

    val theme: StateFlow<String> = settingsRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "Light"
        )

    val hasAskedDownloadPrompt: StateFlow<Boolean> = settingsRepository.hasAskedDownloadPromptFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = true
        )

    val arabicFontName: StateFlow<String> = settingsRepository.arabicFontNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "Me Quran"
        )

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading

    private val _mushafDownloadStatus = MutableStateFlow<com.example.data.model.DownloadStatus?>(null)
    val mushafDownloadStatus: StateFlow<com.example.data.model.DownloadStatus?> = _mushafDownloadStatus.asStateFlow()

    fun getMushafStyle(mushafId: String): com.example.data.model.MushafStyle? {
        return mushafRepository.getAvailableMushafs().find { it.id == mushafId }
    }

    fun downloadDefaultMushaf(mushafId: String) {
        val style = getMushafStyle(mushafId) ?: return
        viewModelScope.launch {
            mushafRepository.downloadMushaf(style, viewModelScope) { status ->
                _mushafDownloadStatus.value = status
            }
        }
    }

    fun cancelMushafDownload(mushafId: String) {
        mushafRepository.cancelDownload(mushafId)
        _mushafDownloadStatus.value = null
    }

    fun clearMushafDownloadStatus() {
        _mushafDownloadStatus.value = null
    }

    private val _downloadProgress = MutableStateFlow(0)
    val downloadProgress: StateFlow<Int> = _downloadProgress

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError

    fun setHasAskedDownloadPrompt() {
        viewModelScope.launch {
            settingsRepository.setHasAskedDownloadPrompt(true)
        }
    }

    fun enableTajweedMode() {
        viewModelScope.launch {
            settingsRepository.setShowTajweed(true)
        }
    }

    private var downloadJob: Job? = null

    fun stopQuranDownload() {
        downloadJob?.cancel()
        _isDownloading.value = false
    }

    fun downloadAllQuranData() {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            _isDownloading.value = true
            _downloadProgress.value = 0
            _downloadError.value = null
            try {
                // First download Surah list
                quranRepository.getSurahs()
                
                // Then download each of the 114 Surahs
                for (i in 1..114) {
                    ensureActive()
                    if (!quranRepository.isSurahDownloaded(i)) {
                        try {
                            val edition = settingsRepository.tanzilTextStyleFlow.first()
                            quranRepository.downloadSurahDetailsSync(i, edition)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    _downloadProgress.value = i
                }
            } catch (e: CancellationException) {
                // Ignored - job was cancelled
            } catch (e: Exception) {
                _downloadError.value = e.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে"
            } finally {
                _isDownloading.value = false
            }
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val nextTheme = if (theme.value == "Dark") "Light" else "Dark"
            settingsRepository.setTheme(nextTheme)
        }
    }

    private val _surahs = MutableStateFlow<List<Surah>>(emptyList())
    val surahs: StateFlow<List<Surah>> = _surahs

    // --- Audio Player Support ---
    private val _currentPlayingSurah = MutableStateFlow<Int?>(null)
    val currentPlayingSurah: StateFlow<Int?> = _currentPlayingSurah.asStateFlow()

    private val _currentPlayingAyahIndex = MutableStateFlow(0)
    val currentPlayingAyahIndex: StateFlow<Int> = _currentPlayingAyahIndex.asStateFlow()

    private val _currentPlayingAyahs = MutableStateFlow<List<CombinedAyah>>(emptyList())
    val currentPlayingAyahs: StateFlow<List<CombinedAyah>> = _currentPlayingAyahs.asStateFlow()

    val isPlaying: StateFlow<Boolean> = audioRepository.isPlaying
    val currentPlayingAyahNumber: StateFlow<Int?> = audioRepository.currentPlayingAyahNumber

    private val _selectedQariId = MutableStateFlow("ar.alafasy")
    val selectedQariId: StateFlow<String> = _selectedQariId.asStateFlow()

    private val _isRepeatAyahEnabled = MutableStateFlow(false)
    val isRepeatAyahEnabled: StateFlow<Boolean> = _isRepeatAyahEnabled.asStateFlow()

    private val _isRepeatSurahEnabled = MutableStateFlow(false)
    val isRepeatSurahEnabled: StateFlow<Boolean> = _isRepeatSurahEnabled.asStateFlow()

    val playbackSpeed: StateFlow<Float> = audioRepository.playbackSpeed

    init {
        viewModelScope.launch { settingsRepository.selectedQariIdFlow.collect { _selectedQariId.value = it } }
        loadSurahs()
    }

    private fun loadSurahs() {
        viewModelScope.launch {
            try {
                _surahs.value = quranRepository.getSurahs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun isMushafDownloaded(mushafId: String): Boolean {
        return mushafRepository.isMushafDownloaded(mushafId)
    }

    // --- Bookmarks Support ---
    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun deleteBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkDao.deleteBookmark(bookmark)
        }
    }

    fun toggleRepeatAyah() {
        _isRepeatAyahEnabled.value = !_isRepeatAyahEnabled.value
        if (_isRepeatAyahEnabled.value) {
            _isRepeatSurahEnabled.value = false
        }
    }

    fun toggleRepeatSurah() {
        _isRepeatSurahEnabled.value = !_isRepeatSurahEnabled.value
        if (_isRepeatSurahEnabled.value) {
            _isRepeatAyahEnabled.value = false
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        audioRepository.setPlaybackSpeed(speed)
    }

    fun setSelectedQariId(qariId: String) {
        _selectedQariId.value = qariId
        viewModelScope.launch {
            settingsRepository.setSelectedQariId(qariId)
            
            // If we are currently playing/viewing a surah, reload its details and restart at the current ayah index with the new Qari immediately
            val surah = _currentPlayingSurah.value
            if (surah != null) {
                try {
                    val edition = settingsRepository.tanzilTextStyleFlow.first()
                    val ayahs = quranRepository.getSurahDetailsCombined(surah, edition, qariId)
                    _currentPlayingAyahs.value = ayahs
                    
                    val currentIndex = _currentPlayingAyahIndex.value
                    if (currentIndex >= 0 && currentIndex < ayahs.size) {
                        playAyahAtIndex(currentIndex)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun playSurahAudio(surahNumber: Int, startAyahIndex: Int = 0) {
        viewModelScope.launch {
            try {
                _currentPlayingSurah.value = surahNumber
                _currentPlayingAyahs.value = emptyList() // Clear previous ayahs to show loading state
                _currentPlayingAyahIndex.value = 0

                val qari = selectedQariId.value
                val edition = settingsRepository.tanzilTextStyleFlow.first()
                val ayahs = quranRepository.getSurahDetailsCombined(surahNumber, edition, qari)
                
                _currentPlayingAyahs.value = ayahs
                _currentPlayingAyahIndex.value = startAyahIndex
                
                if (ayahs.isNotEmpty() && startAyahIndex < ayahs.size) {
                    playAyahAtIndex(startAyahIndex)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateHijriOffset(offset: Int) {
        viewModelScope.launch {
            settingsRepository.setHijriOffset(offset)
        }
    }

    private fun playAyahAtIndex(index: Int) {
        val ayahs = _currentPlayingAyahs.value
        if (index >= 0 && index < ayahs.size) {
            _currentPlayingAyahIndex.value = index
            val ayah = ayahs[index]
            val qari = selectedQariId.value
            val audioUrl = com.example.util.AudioUtils.getAudioUrl(qari, ayah.number)
            
            audioRepository.playAudio(audioUrl, ayah.numberInSurah)
            audioRepository.onPlaybackEnded = {
                if (_isRepeatAyahEnabled.value) {
                    playAyahAtIndex(index)
                } else if (index + 1 < ayahs.size) {
                    playAyahAtIndex(index + 1)
                } else if (_isRepeatSurahEnabled.value) {
                    playSurahAudio(currentPlayingSurah.value ?: 1, 0)
                } else {
                    stopSurahAudio()
                }
            }
        }
    }

    fun pauseSurahAudio() {
        audioRepository.pauseAudio()
    }

    fun resumeSurahAudio() {
        audioRepository.resumeAudio()
    }

    fun stopSurahAudio() {
        audioRepository.stopAudio()
        _currentPlayingSurah.value = null
        _currentPlayingAyahs.value = emptyList()
        _currentPlayingAyahIndex.value = 0
    }

    fun nextAyah() {
        val nextIndex = _currentPlayingAyahIndex.value + 1
        if (nextIndex < _currentPlayingAyahs.value.size) {
            playAyahAtIndex(nextIndex)
        }
    }

    fun previousAyah() {
        val prevIndex = _currentPlayingAyahIndex.value - 1
        if (prevIndex >= 0) {
            playAyahAtIndex(prevIndex)
        }
    }
}
