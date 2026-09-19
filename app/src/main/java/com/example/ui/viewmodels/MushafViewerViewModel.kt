package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MushafRepository
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entity.BookmarkEntity

import com.example.data.repository.QuranRepository
import com.example.data.repository.AudioRepository
import com.example.data.model.CombinedAyah
import com.example.util.AudioUtils

class MushafViewerViewModel(
    private val repository: MushafRepository,
    private val settingsRepository: SettingsRepository,
    private val bookmarkDao: BookmarkDao,
    private val quranRepository: QuranRepository? = null,
    val audioRepository: AudioRepository? = null
) : ViewModel() {

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _pageAyahs = MutableStateFlow<List<CombinedAyah>>(emptyList())
    val pageAyahs: StateFlow<List<CombinedAyah>> = _pageAyahs.asStateFlow()

    private val _selectedAyah = MutableStateFlow<CombinedAyah?>(null)
    val selectedAyah: StateFlow<CombinedAyah?> = _selectedAyah.asStateFlow()

    private val _isLoadingAyahs = MutableStateFlow(false)
    val isLoadingAyahs: StateFlow<Boolean> = _isLoadingAyahs.asStateFlow()

    val isAudioPlaying: StateFlow<Boolean> = audioRepository?.isPlaying ?: MutableStateFlow(false)
    val currentPlayingAyahNumber: StateFlow<Int?> = audioRepository?.currentPlayingAyahNumber ?: MutableStateFlow(null)

    val theme: StateFlow<String> = settingsRepository.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = "Light"
    )

    val scrollDirection: StateFlow<String> = settingsRepository.mushafScrollDirectionFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = "Horizontal"
    )

    val pageHeightScale: StateFlow<Float> = settingsRepository.mushafPageHeightScaleFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = 1.2f
    )

    private val _currentPagePath = MutableStateFlow<String?>(null)
    val currentPagePath: StateFlow<String?> = _currentPagePath.asStateFlow()

    private val _currentPageNumber = MutableStateFlow(1)
    val currentPageNumber: StateFlow<Int> = _currentPageNumber.asStateFlow()
    
    private val _pdfPageOffset = MutableStateFlow(0)
    val pdfPageOffset: StateFlow<Int> = _pdfPageOffset.asStateFlow()

    private val _isPdf = MutableStateFlow(false)
    val isPdf: StateFlow<Boolean> = _isPdf.asStateFlow()

    private val _totalPages = MutableStateFlow(604)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _isDownloaded = MutableStateFlow(false)
    val isDownloaded: StateFlow<Boolean> = _isDownloaded.asStateFlow()
    
    var currentMushafId = ""

    fun initMushaf(mushafId: String, initialPage: Int = 1) {
        currentMushafId = mushafId
        val style = repository.getAvailableMushafs().find { it.id == mushafId }
        _isPdf.value = style?.isPdf == true
        _totalPages.value = style?.totalPages ?: 604
        _isDownloaded.value = repository.isMushafDownloaded(mushafId)
        // Set the page synchronously BEFORE the suspend call below.
        // Without this, currentPage stays at its default (1) while pagerState
        // is already created at the correct initialPage, causing the
        // LaunchedEffect(currentPage) in MushafViewerScreen to force-scroll
        // the pager back to page 1 before jumpToPage(initialPage) runs.
        _currentPageNumber.value = initialPage

        viewModelScope.launch {
            val customOffset = settingsRepository.getMushafOffset(mushafId).first()
            _pdfPageOffset.value = customOffset ?: style?.pdfPageOffset ?: 0
            jumpToPage(initialPage)
            _isReady.value = true
        }
    }

    fun jumpToPage(pageNumber: Int) {
        _currentPageNumber.value = pageNumber
        viewModelScope.launch(Dispatchers.IO) {
            val path = repository.getMushafPagePath(currentMushafId, pageNumber, _pdfPageOffset.value)
            _currentPagePath.value = path
            
            // Save last read state
            settingsRepository.setLastReadMushaf(currentMushafId, pageNumber)
            settingsRepository.setLastReadMode("MUSHAF")

            val bookmarkEntity = bookmarkDao.getBookmark("MUSHAF_PAGE", pageNumber)
                ?: bookmarkDao.getBookmark("PAGE", pageNumber)
            _isBookmarked.value = bookmarkEntity != null
        }
        loadPageAyahs(pageNumber)
    }

    fun loadPageAyahs(pageNumber: Int) {
        if (quranRepository == null) return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingAyahs.value = true
            try {
                val ayahs = quranRepository.getPageCombined(pageNumber)
                _pageAyahs.value = ayahs
                if (ayahs.isNotEmpty()) {
                    val currentSel = _selectedAyah.value
                    if (currentSel == null || currentSel.page != pageNumber) {
                        _selectedAyah.value = ayahs.first()
                    }
                } else {
                    _selectedAyah.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingAyahs.value = false
            }
        }
    }

    fun selectAyah(ayah: CombinedAyah) {
        _selectedAyah.value = ayah
    }

    fun togglePlayPauseAyah(ayah: CombinedAyah) {
        if (audioRepository == null) return
        val currentlyPlayingNum = currentPlayingAyahNumber.value
        val isCurrentlyPlaying = isAudioPlaying.value

        if (currentlyPlayingNum == ayah.numberInSurah && isCurrentlyPlaying) {
            audioRepository.pauseAudio()
        } else if (currentlyPlayingNum == ayah.numberInSurah && !isCurrentlyPlaying) {
            audioRepository.resumeAudio()
        } else {
            val audioUrl = AudioUtils.getAudioUrl("ar.alafasy", ayah.number)
            audioRepository.playAudio(audioUrl, ayah.numberInSurah)
        }
    }

    fun toggleBookmark() {
        val page = _currentPageNumber.value
        val mId = currentMushafId
        viewModelScope.launch(Dispatchers.IO) {
            val currentlyBookmarked = _isBookmarked.value
            if (currentlyBookmarked) {
                bookmarkDao.deleteBookmarkByReference("MUSHAF_PAGE", page)
                bookmarkDao.deleteBookmarkByReference("PAGE", page)
                _isBookmarked.value = false
            } else {
                val styleName = repository.getAvailableMushafs().find { it.id == mId }?.nameBengali ?: "মুসহাফ"
                val pageStr = com.example.utils.DateUtil.toBengaliNumerals(page)
                val nameText = "$styleName: পৃষ্ঠা $pageStr"
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        type = "MUSHAF_PAGE",
                        referenceId = page,
                        name = nameText,
                        mushafId = mId
                    )
                )
                _isBookmarked.value = true
            }
        }
    }

    suspend fun getPagePath(mushafId: String, pageNumber: Int): String? = withContext(Dispatchers.IO) {
        repository.getMushafPagePath(mushafId, pageNumber, _pdfPageOffset.value)
    }

    suspend fun downloadPageOnDemand(mushafId: String, pageNumber: Int): Boolean {
        return repository.downloadSinglePage(mushafId, pageNumber)
    }

    fun adjustOffset(increment: Int) {
        val newOffset = _pdfPageOffset.value + increment
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearRenderedPages(currentMushafId)
            }
            _pdfPageOffset.value = newOffset
            settingsRepository.setMushafOffset(currentMushafId, newOffset)
            jumpToPage(_currentPageNumber.value)
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = settingsRepository.themeFlow.first()
            val next = if (current == "Dark") "Light" else "Dark"
            settingsRepository.setTheme(next)
        }
    }

    fun setTheme(themeKey: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(themeKey)
        }
    }

    fun setScrollDirection(direction: String) {
        viewModelScope.launch {
            settingsRepository.setMushafScrollDirection(direction)
        }
    }

    fun setPageHeightScale(scale: Float) {
        viewModelScope.launch {
            settingsRepository.setMushafPageHeightScale(scale)
        }
    }

    fun prefetchPages(mushafId: String, currentPage: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            for (i in 1..2) {
                if (currentPage + i <= _totalPages.value) {
                    repository.getMushafPagePath(mushafId, currentPage + i, _pdfPageOffset.value)
                }
                if (currentPage - i > 0) {
                    repository.getMushafPagePath(mushafId, currentPage - i, _pdfPageOffset.value)
                }
            }
        }
    }
}
