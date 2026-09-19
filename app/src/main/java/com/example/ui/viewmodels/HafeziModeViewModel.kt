package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.MemorizedPageDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.local.entity.MemorizedPageEntity
import com.example.data.model.CombinedAyah
import com.example.data.repository.AudioRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.usecase.GetPageDetailsUseCase
import com.example.ui.state.UiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HafeziModeViewModel(
    private val getPageDetailsUseCase: GetPageDetailsUseCase,
    private val audioRepository: AudioRepository,
    private val settingsRepository: SettingsRepository,
    private val bookmarkDao: BookmarkDao,
    private val memorizedPageDao: MemorizedPageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<CombinedAyah>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<CombinedAyah>>> = _uiState.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    val scrollDirection: StateFlow<String> = settingsRepository.mushafScrollDirectionFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "Horizontal")

    private val _isPageMemorized = MutableStateFlow(false)
    val isPageMemorized: StateFlow<Boolean> = _isPageMemorized.asStateFlow()

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    val isPlaying: StateFlow<Boolean> = audioRepository.isPlaying
    val currentPlayingAyahNumber: StateFlow<Int?> = audioRepository.currentPlayingAyahNumber

    val selectedQariId: StateFlow<String> = settingsRepository.selectedQariIdFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "ar.alafasy")

    val repeatCount: StateFlow<Int> = settingsRepository.repeatCountFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1)

    val showTajweed: StateFlow<Boolean> = settingsRepository.showTajweedFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    val arabicFontSize: StateFlow<Float> = settingsRepository.arabicFontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 18f)

    val arabicFontName: StateFlow<String> = settingsRepository.arabicFontNameFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "Me Quran")

    val theme: StateFlow<String> = settingsRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "Light")

    val showWaqfSigns: StateFlow<Boolean> = settingsRepository.showWaqfSignsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val arabicLineSpacing: StateFlow<Float> = settingsRepository.arabicLineSpacingFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 2.5f)

    private var currentRepeatIteration = 0
    private var playlist: List<CombinedAyah> = emptyList()
    private var currentPlaylistIndex = 0
    private var playbackJob: Job? = null

    fun loadPage(pageNumber: Int) {
        if (pageNumber !in 1..610) return
        _currentPage.value = pageNumber
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val ayahs = getPageDetailsUseCase(pageNumber)
                playlist = ayahs
                _uiState.value = UiState.Success(ayahs)
                
                // Save last read page, surah and mode
                settingsRepository.setLastReadPage(pageNumber)
                ayahs.firstOrNull()?.let {
                    settingsRepository.setLastReadSurah(it.surahNumber)
                }
                settingsRepository.setLastReadMode("HAFEZI")
                
                // Check if memorized
                val memorizedEntity = memorizedPageDao.getMemorizedPage(pageNumber)
                _isPageMemorized.value = memorizedEntity?.isMemorized == true

                // Check if bookmarked
                val bookmarkEntity = bookmarkDao.getBookmark("PAGE", pageNumber)
                _isBookmarked.value = bookmarkEntity != null

            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load Page details")
            }
        }
    }

    fun setRepeatCount(count: Int) {
        viewModelScope.launch {
            settingsRepository.setRepeatCount(count)
        }
    }

    fun setArabicFontSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.setArabicFontSize(size)
        }
    }

    fun setArabicFontName(fontName: String) {
        viewModelScope.launch {
            settingsRepository.setArabicFontName(fontName)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setShowWaqfSigns(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowWaqfSigns(show)
        }
    }

    fun setShowTajweed(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowTajweed(show)
        }
    }

    fun setArabicLineSpacing(spacing: Float) {
        viewModelScope.launch {
            settingsRepository.setArabicLineSpacing(spacing)
        }
    }

    fun playAudio() {
        if (playlist.isEmpty()) return
        currentRepeatIteration = 0
        currentPlaylistIndex = 0
        playCurrentIndex()
    }

    fun playAyah(ayahNumber: Int) {
        if (playlist.isEmpty()) return
        val index = playlist.indexOfFirst { it.number == ayahNumber }
        if (index != -1) {
            currentRepeatIteration = 0
            currentPlaylistIndex = index
            playCurrentIndex()
        }
    }

    fun pauseAudio() {
        audioRepository.pauseAudio()
    }

    fun resumeAudio() {
        audioRepository.onPlaybackEnded = {
            playNextAyah()
        }
        audioRepository.resumeAudio()
    }

    fun stopAudio() {
        audioRepository.onPlaybackEnded = null
        audioRepository.stopAudio()
        currentPlaylistIndex = 0
        currentRepeatIteration = 0
    }

    private fun playNextAyah() {
        if (playlist.isEmpty()) return
        currentPlaylistIndex++
        
        if (currentPlaylistIndex >= playlist.size) {
            // Reached end of page, check repeat
            currentRepeatIteration++
            val targetRepeat = repeatCount.value
            
            if (currentRepeatIteration < targetRepeat) {
                currentPlaylistIndex = 0
                playCurrentIndex()
            } else {
                stopAudio()
            }
        } else {
            playCurrentIndex()
        }
    }

    private fun playCurrentIndex() {
        if (currentPlaylistIndex < playlist.size) {
            val ayah = playlist[currentPlaylistIndex]
            val qari = selectedQariId.value
            val audioUrl = com.example.util.AudioUtils.getAudioUrl(qari, ayah.number)
            audioRepository.onPlaybackEnded = {
                playNextAyah()
            }
            audioRepository.playAudio(audioUrl, ayah.number)
        }
    }

    fun toggleMemorized() {
        viewModelScope.launch {
            val page = _currentPage.value
            val current = _isPageMemorized.value
            if (current) {
                memorizedPageDao.deleteMemorizedPage(page)
                _isPageMemorized.value = false
            } else {
                memorizedPageDao.insertMemorizedPage(MemorizedPageEntity(page))
                _isPageMemorized.value = true
            }
        }
    }

    fun toggleBookmark() {
        viewModelScope.launch {
            val page = _currentPage.value
            val current = _isBookmarked.value
            if (current) {
                bookmarkDao.deleteBookmarkByReference("PAGE", page)
                _isBookmarked.value = false
            } else {
                bookmarkDao.insertBookmark(BookmarkEntity(type = "PAGE", referenceId = page, name = "Page $page"))
                _isBookmarked.value = true
            }
        }
    }

    fun nextPage() {
        if (_currentPage.value < 610) {
            stopAudio()
            loadPage(_currentPage.value + 1)
        }
    }

    fun previousPage() {
        if (_currentPage.value > 1) {
            stopAudio()
            loadPage(_currentPage.value - 1)
        }
    }

    override fun onCleared() {
        super.onCleared()
    }

    fun setScrollDirection(direction: String) {
        viewModelScope.launch {
            settingsRepository.setMushafScrollDirection(direction)
        }
    }

    suspend fun getPageDetails(pageNumber: Int): List<CombinedAyah> {
        return getPageDetailsUseCase(pageNumber)
    }

    fun updateActivePage(pageNumber: Int) {
        if (pageNumber !in 1..610) return
        _currentPage.value = pageNumber
        viewModelScope.launch {
            try {
                val ayahs = getPageDetailsUseCase(pageNumber)
                playlist = ayahs
                
                // Save last read page, surah and mode
                settingsRepository.setLastReadPage(pageNumber)
                ayahs.firstOrNull()?.let {
                    settingsRepository.setLastReadSurah(it.surahNumber)
                }
                settingsRepository.setLastReadMode("HAFEZI")
                
                // Check if memorized
                val memorizedEntity = memorizedPageDao.getMemorizedPage(pageNumber)
                _isPageMemorized.value = memorizedEntity?.isMemorized == true

                // Check if bookmarked
                val bookmarkEntity = bookmarkDao.getBookmark("PAGE", pageNumber)
                _isBookmarked.value = bookmarkEntity != null
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
