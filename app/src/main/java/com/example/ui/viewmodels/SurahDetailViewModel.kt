package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.model.CombinedAyah
import com.example.data.repository.QuranRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.RecentReadTrack
import com.example.data.repository.AiRepository
import com.example.data.repository.AudioRepository
import com.example.data.QuranData
import com.example.utils.DateUtil
import com.example.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.File
import java.net.URL

enum class PlaybackMode { AYAH, SURAH }

class SurahDetailViewModel(
    private val repository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val aiRepository: AiRepository,
    val audioRepository: AudioRepository,
    private val bookmarkDao: BookmarkDao
) : ViewModel() {

    private var currentSelectedQariId = "ar.alafasy"

    val selectedTafsirIds: StateFlow<Set<String>> = settingsRepository.selectedTafsirIdsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf("164")
        )

    private val _availableTafsirs = MutableStateFlow<List<com.example.data.model.TafsirResourceDto>>(emptyList())
    val availableTafsirs: StateFlow<List<com.example.data.model.TafsirResourceDto>> = _availableTafsirs.asStateFlow()

    private val _availableTranslations = MutableStateFlow<List<com.example.data.model.TranslationResourceDto>>(emptyList())
    val availableTranslations: StateFlow<List<com.example.data.model.TranslationResourceDto>> = _availableTranslations.asStateFlow()

    val selectedTafsirNames: StateFlow<List<String>> = kotlinx.coroutines.flow.combine(
        selectedTafsirIds,
        availableTafsirs
    ) { ids, available ->
        ids.map { id ->
            val found = available.find { it.id.toString() == id }
            if (found != null) {
                found.name ?: "Unknown Tafsir"
            } else {
                when (id) {
                    "164" -> "তাফসীর আল-মুয়াসসার"
                    "165" -> "তাফসীর ইবনে কাসীর (আরবি)"
                    "166" -> "তাফসীর আল-জালালাইন"
                    "168" -> "তাফসীর আহসানুল বয়ান"
                    "169" -> "তাফসীর আবু বকর জাকারিয়া"
                    "171" -> "তাফসীর ইবনে কাসীর"
                    else -> "তাফসীর $id"
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("তাফসীর আল-মুয়াসসার")
    )

    private var currentLoadedSurahNumber: Int? = null
    private var currentLoadedJuzNumber: Int? = null

    init {
        viewModelScope.launch {
            settingsRepository.selectedQariIdFlow.collect { qariId ->
                val changed = currentSelectedQariId != qariId
                currentSelectedQariId = qariId
                if (changed && audioRepository.isPlaying.value) {
                    val playingAyahNum = audioRepository.currentPlayingAyahNumber.value
                    val state = _uiState.value
                    if (playingAyahNum != null && state is UiState.Success) {
                        val currentAyah = state.data.find { it.numberInSurah == playingAyahNum }
                        if (currentAyah != null) {
                            playAyah(currentAyah, currentAyah.surahNumber)
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                settingsRepository.selectedTranslationIdsFlow,
                settingsRepository.selectedTafsirIdsFlow,
                settingsRepository.tanzilTextStyleFlow
            ) { trans, tafsir, fontStyle ->
                Triple(trans, tafsir, fontStyle)
            }.collect { (_, _, fontStyle) ->
                val surahNum = currentLoadedSurahNumber
                val juzNum = currentLoadedJuzNumber
                if (surahNum != null) {
                    try {
                        val updated = repository.getSurahDetailsCombined(surahNum, fontStyle)
                        _uiState.value = UiState.Success(updated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else if (juzNum != null) {
                    try {
                        val updated = repository.getJuzCombined(juzNum)
                        _uiState.value = UiState.Success(updated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        viewModelScope.launch {
            try {
                _availableTafsirs.value = repository.getAvailableTafsirs("bn")
                _availableTranslations.value = repository.getAvailableTranslations("bn")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            repository.surahDataUpdated.collect { updatedSurahNum ->
                if (currentLoadedSurahNumber == updatedSurahNum) {
                    try {
                        val fontStyle = settingsRepository.tanzilTextStyleFlow.first()
                        val updated = repository.getSurahDetailsCombined(updatedSurahNum, fontStyle)
                        _uiState.value = UiState.Success(updated)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private val _uiState = MutableStateFlow<UiState<List<CombinedAyah>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<CombinedAyah>>> = _uiState.asStateFlow()

    val isTafsirSyncing: StateFlow<Boolean> = repository.tafsirSyncingSurahs.map { syncingSet ->
        val current = currentLoadedSurahNumber
        current != null && syncingSet.contains(current)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _tafsirState = MutableStateFlow<UiState<String>>(UiState.Loading)
    val tafsirState: StateFlow<UiState<String>> = _tafsirState.asStateFlow()

    // --- Manual offline caching state for current surah ---
    private val _isDownloadingOffline = MutableStateFlow(false)
    val isDownloadingOffline: StateFlow<Boolean> = _isDownloadingOffline.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0) // 0 to 100%
    val downloadProgress: StateFlow<Int> = _downloadProgress.asStateFlow()

    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus: StateFlow<String?> = _downloadStatus.asStateFlow()

    private val _downloadError = MutableStateFlow<String?>(null)
    val downloadError: StateFlow<String?> = _downloadError.asStateFlow()

    private var downloadJob: Job? = null

    // Observe translation toggle
    val keepScreenOn: StateFlow<Boolean> = settingsRepository.keepScreenOnFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val selectedTranslationIds: StateFlow<Set<String>> = settingsRepository.selectedTranslationIdsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = setOf("161")
        )

    val showTranslation: StateFlow<Boolean> = settingsRepository.showTranslationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = true
        )
        
    val showTransliteration: StateFlow<Boolean> = settingsRepository.showTransliterationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    val showTajweed: StateFlow<Boolean> = settingsRepository.showTajweedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    // Observe Font Sizes
    val arabicFontSize: StateFlow<Float> = settingsRepository.arabicFontSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 18f
        )

    val bengaliFontSize: StateFlow<Float> = settingsRepository.bengaliFontSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 16f
        )

    val arabicFontName: StateFlow<String> = settingsRepository.arabicFontNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "Me Quran"
        )

    val bengaliFontName: StateFlow<String> = settingsRepository.bengaliFontNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "SolaimanLipi"
        )

    val tanzilTextStyle: StateFlow<String> = settingsRepository.tanzilTextStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "default-indopak"
        )

    val arabicLineSpacing: StateFlow<Float> = settingsRepository.arabicLineSpacingFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 2.5f
        )

    val showWaqfSigns: StateFlow<Boolean> = settingsRepository.showWaqfSignsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = true
        )

    // Playback state and Mode
    val isPlaying: StateFlow<Boolean> = audioRepository.isPlaying
    val currentPlayingAyahNumber: StateFlow<Int?> = audioRepository.currentPlayingAyahNumber

    private val _playbackMode = MutableStateFlow(PlaybackMode.SURAH)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun toggleBookmark(ayah: CombinedAyah, defaultSurahNumber: Int) {
        viewModelScope.launch {
            val existing = bookmarkDao.getBookmark("AYAH", ayah.number)
            if (existing != null) {
                bookmarkDao.deleteBookmark(existing)
            } else {
                val sNum = if (ayah.surahNumber > 0) ayah.surahNumber else defaultSurahNumber
                val surahPair = com.example.data.QuranData.surahNames.find { it.first == sNum }
                val surahName = surahPair?.second?.first ?: "সুরা $sNum"
                val bookmarkName = "$surahName: আয়াত ${ayah.numberInSurah}"
                bookmarkDao.insertBookmark(
                    BookmarkEntity(
                        type = "AYAH",
                        referenceId = ayah.number,
                        name = bookmarkName
                    )
                )
            }
        }
    }

    fun setKeepScreenOn(keep: Boolean) {
        viewModelScope.launch {
            settingsRepository.setKeepScreenOn(keep)
        }
    }

    fun setSelectedTranslationIds(ids: Set<String>) {
        viewModelScope.launch {
            settingsRepository.setSelectedTranslationIds(ids)
            val surah = (_uiState.value as? UiState.Success)?.data?.firstOrNull()?.surahNumber
            if (surah != null) loadSurah(surah)
        }
    }

    fun setSelectedTafsirIds(ids: Set<String>) {
        viewModelScope.launch {
            settingsRepository.setSelectedTafsirIds(ids)
            val surah = (_uiState.value as? UiState.Success)?.data?.firstOrNull()?.surahNumber
            if (surah != null) loadSurah(surah)
        }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
    }

    fun playAyah(ayah: CombinedAyah, surahNumber: Int) {
        val audioUrl = com.example.util.AudioUtils.getAudioUrl(currentSelectedQariId, ayah.number)
        audioRepository.playAudio(audioUrl, ayah.numberInSurah)
        
        // Setup completion callback for continuous play
        audioRepository.onPlaybackEnded = {
            if (_playbackMode.value == PlaybackMode.SURAH) {
                playNextAyah(ayah, surahNumber)
            }
        }
    }

    private fun playNextAyah(currentAyah: CombinedAyah, surahNumber: Int) {
        val state = _uiState.value
        if (state is UiState.Success) {
            val currentIndex = state.data.indexOfFirst { it.number == currentAyah.number }
            if (currentIndex != -1 && currentIndex + 1 < state.data.size) {
                val nextAyah = state.data[currentIndex + 1]
                playAyah(nextAyah, surahNumber)
            }
        }
    }

    fun playWord(url: String) {
        audioRepository.onPlaybackEnded = null
        audioRepository.playAudio(url, -1)
    }

    fun togglePlayPause(currentAyah: CombinedAyah?, surahNumber: Int) {
        val playingAyahNum = audioRepository.currentPlayingAyahNumber.value
        if (currentAyah != null && playingAyahNum == currentAyah.numberInSurah) {
            if (audioRepository.isPlaying.value) {
                audioRepository.pauseAudio()
            } else {
                audioRepository.resumeAudio()
            }
        } else if (currentAyah != null) {
            playAyah(currentAyah, surahNumber)
        } else {
            // If nothing playing, play first ayah from loaded list
            val state = _uiState.value
            if (state is UiState.Success && state.data.isNotEmpty()) {
                playAyah(state.data.first(), surahNumber)
            }
        }
    }

    fun playPrevious(currentAyah: CombinedAyah?, surahNumber: Int) {
        val state = _uiState.value
        if (state is UiState.Success && currentAyah != null) {
            val currentIndex = state.data.indexOfFirst { it.number == currentAyah.number }
            if (currentIndex > 0) {
                val prevAyah = state.data[currentIndex - 1]
                playAyah(prevAyah, surahNumber)
            }
        }
    }

    fun playNext(currentAyah: CombinedAyah?, surahNumber: Int) {
        val state = _uiState.value
        if (state is UiState.Success && currentAyah != null) {
            val currentIndex = state.data.indexOfFirst { it.number == currentAyah.number }
            if (currentIndex != -1 && currentIndex + 1 < state.data.size) {
                val nextAyah = state.data[currentIndex + 1]
                playAyah(nextAyah, surahNumber)
            }
        }
    }

    fun setArabicFontSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.setArabicFontSize(size)
        }
    }

    fun setBengaliFontSize(size: Float) {
        viewModelScope.launch {
            settingsRepository.setBengaliFontSize(size)
        }
    }

    fun setArabicFontName(fontName: String) {
        viewModelScope.launch {
            settingsRepository.setArabicFontName(fontName)
        }
    }

    fun setBengaliFontName(fontName: String) {
        viewModelScope.launch {
            settingsRepository.setBengaliFontName(fontName)
        }
    }

    fun setTanzilTextStyle(style: String) {
        viewModelScope.launch {
            settingsRepository.setTanzilTextStyle(style)
        }
    }

    fun setArabicLineSpacing(spacing: Float) {
        viewModelScope.launch {
            settingsRepository.setArabicLineSpacing(spacing)
        }
    }

    fun toggleTranslation() {
        viewModelScope.launch {
            settingsRepository.setShowTranslation(!showTranslation.value)
        }
    }

    fun setShowWaqfSigns(show: Boolean) {
        viewModelScope.launch {
            settingsRepository.setShowWaqfSigns(show)
        }
    }

    fun loadSurah(surahNumber: Int) {
        currentLoadedSurahNumber = surahNumber
        currentLoadedJuzNumber = null
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val combinedAyahs = repository.getSurahDetailsCombined(surahNumber, tanzilTextStyle.value)
                _uiState.value = UiState.Success(combinedAyahs)
                settingsRepository.setLastReadSurah(surahNumber)
                settingsRepository.setLastReadMode("DETAIL")

                val surahName = QuranData.surahNames.find { it.first == surahNumber }?.second?.first ?: "সূরা $surahNumber"
                settingsRepository.addRecentRead(
                    RecentReadTrack(
                        title = "সূরা $surahName",
                        surahNumber = surahNumber,
                        ayahNumber = 1,
                        mode = "DETAIL"
                    )
                )
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load Surah details")
            }
        }
    }

    fun updateLastReadAyah(ayahNumber: Int) {
        viewModelScope.launch {
            settingsRepository.setLastReadAyah(ayahNumber)
            currentLoadedSurahNumber?.let { sNum ->
                val surahName = QuranData.surahNames.find { it.first == sNum }?.second?.first ?: "সূরা $sNum"
                settingsRepository.addRecentRead(
                    RecentReadTrack(
                        title = "সূরা $surahName আয়াত ${DateUtil.toBengaliNumerals(ayahNumber)}",
                        surahNumber = sNum,
                        ayahNumber = ayahNumber,
                        mode = "DETAIL"
                    )
                )
            }
        }
    }

    fun loadJuz(juzNumber: Int) {
        currentLoadedJuzNumber = juzNumber
        currentLoadedSurahNumber = null
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val combinedAyahs = repository.getJuzCombined(juzNumber)
                _uiState.value = UiState.Success(combinedAyahs)
                combinedAyahs.firstOrNull()?.let {
                    settingsRepository.setLastReadSurah(it.surahNumber)
                    settingsRepository.setLastReadMode("DETAIL")

                    val surahName = QuranData.surahNames.find { s -> s.first == it.surahNumber }?.second?.first ?: "সূরা ${it.surahNumber}"
                    settingsRepository.addRecentRead(
                        RecentReadTrack(
                            title = "পারা ${DateUtil.toBengaliNumerals(juzNumber)} (সূরা $surahName)",
                            surahNumber = it.surahNumber,
                            ayahNumber = it.numberInSurah,
                            mode = "DETAIL"
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load Juz details")
            }
        }
    }

    fun getTafsir(surahName: String, ayahNumber: Int, ayahText: String) {
        viewModelScope.launch {
            _tafsirState.value = UiState.Loading
            val result = aiRepository.getTafsir(surahName, ayahNumber, ayahText)
            if (result.isSuccess) {
                _tafsirState.value = UiState.Success(result.getOrNull() ?: "")
            } else {
                _tafsirState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun downloadSurahOffline(surahNumber: Int, surahName: String) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            _isDownloadingOffline.value = true
            _downloadProgress.value = 0
            _downloadStatus.value = "সুরা ডাটা ও অনুবাদ ডাউনলোড হচ্ছে..."
            _downloadError.value = null
            try {
                // 1. Pre-cache text data
                val combinedAyahs = repository.getSurahDetailsCombined(surahNumber, tanzilTextStyle.value)
                val totalAyahs = combinedAyahs.size
                if (totalAyahs == 0) {
                    _downloadStatus.value = "কোনো আয়াত পাওয়া যায়নি"
                    return@launch
                }

                _downloadStatus.value = "অডিও ফাইল ডাউনলোড হচ্ছে..."
                val selectedQariId = settingsRepository.selectedQariIdFlow.first()
                var successCount = 0
                var failCount = 0

                withContext(Dispatchers.IO) {
                    for ((index, ayah) in combinedAyahs.withIndex()) {
                        ensureActive()
                        val targetUrl = com.example.util.AudioUtils.getAudioUrl(selectedQariId, ayah.number)
                        val localFile = audioRepository.getLocalAudioFile(targetUrl)

                        var downloadedOk = false
                        if (localFile.exists() && localFile.length() > 0) {
                            downloadedOk = true
                        } else {
                            downloadedOk = audioRepository.downloadUrlToFile(targetUrl, localFile)
                            if (!downloadedOk && !ayah.audioUrl.isNullOrEmpty() && ayah.audioUrl != targetUrl) {
                                val altUrl = ayah.audioUrl.replace("http://", "https://")
                                val altFile = audioRepository.getLocalAudioFile(altUrl)
                                downloadedOk = audioRepository.downloadUrlToFile(altUrl, altFile)
                            }
                        }

                        if (downloadedOk) successCount++ else failCount++

                        val progress = ((index + 1) * 100) / totalAyahs
                        _downloadProgress.value = progress
                    }
                }

                if (failCount == totalAyahs) {
                    _downloadError.value = "অডিও ফাইল ডাউনলোড করতে ব্যর্থ হয়েছে! ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                    _downloadStatus.value = null
                } else if (failCount > 0) {
                    _downloadStatus.value = "সুরার $successCount/$totalAyahs অডিও ডাউনলোড হয়েছে"
                } else {
                    _downloadStatus.value = "সম্পূর্ণ সুরা অফলাইন ডাউনলোড সফল হয়েছে!"
                }
            } catch (e: Exception) {
                _downloadError.value = e.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে"
                _downloadStatus.value = null
            } finally {
                _isDownloadingOffline.value = false
            }
        }
    }

    fun cancelOfflineDownload() {
        downloadJob?.cancel()
        _isDownloadingOffline.value = false
        _downloadStatus.value = "ডাউনলোড বাতিল করা হয়েছে"
    }

    override fun onCleared() {
        super.onCleared()
        downloadJob?.cancel()
    }
}
