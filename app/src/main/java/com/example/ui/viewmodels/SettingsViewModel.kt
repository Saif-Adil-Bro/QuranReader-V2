package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.entity.BookmarkEntity
import com.example.data.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import java.net.URL

import com.example.data.repository.QuranRepository
import com.example.data.repository.AudioRepository
import com.example.data.model.Surah
import java.io.File

data class UserNote(
    val id: String,
    val title: String,
    val content: String,
    val timestamp: Long
)


enum class GamePhase { SETUP, LOADING, PLAYING, RESULT }
enum class GameSource { ENTIRE_QURAN, SPECIFIC_SURAH }
enum class GameType { ARABIC_TO_BENGALI, BENGALI_TO_ARABIC }

data class WordGameConfig(
    val source: GameSource = GameSource.ENTIRE_QURAN,
    val selectedSurah: Int = 1,
    val type: GameType = GameType.ARABIC_TO_BENGALI,
    val totalQuestions: Int = 10
)

data class WordQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: String
)

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val bookmarkDao: BookmarkDao,
    val quranRepository: QuranRepository,
    val audioRepository: AudioRepository
) : ViewModel() {

    
    val themeState: StateFlow<String> = repository.themeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "System"
    )
    
    fun setTheme(theme: String) {
        viewModelScope.launch { repository.setTheme(theme) }
    }

    val arabicFontName: StateFlow<String> = repository.arabicFontNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Me Quran"
        )

    val bengaliFontName: StateFlow<String> = repository.bengaliFontNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "SolaimanLipi"
        )

    val arabicFontSize: StateFlow<Float> = repository.arabicFontSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 20f
        )

    val bengaliFontSize: StateFlow<Float> = repository.bengaliFontSizeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 16f
        )

    fun setArabicFontName(fontName: String) {
        viewModelScope.launch { repository.setArabicFontName(fontName) }
    }

    fun setBengaliFontName(fontName: String) {
        viewModelScope.launch { repository.setBengaliFontName(fontName) }
    }

    fun setArabicFontSize(size: Float) {
        viewModelScope.launch { repository.setArabicFontSize(size) }
    }

    fun setBengaliFontSize(size: Float) {
        viewModelScope.launch { repository.setBengaliFontSize(size) }
    }


    private val _downloadingTafsirIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTafsirIds: StateFlow<Set<String>> = _downloadingTafsirIds.asStateFlow()

    private val _downloadedTafsirIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTafsirIds: StateFlow<Set<String>> = _downloadedTafsirIds.asStateFlow()

    private val _tafsirDownloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val tafsirDownloadProgress: StateFlow<Map<String, Float>> = _tafsirDownloadProgress.asStateFlow()
    
    fun updateDownloadedTafsirs() {
        viewModelScope.launch {
            val available = _availableTafsirs.value
            val downloaded = available.filter { quranRepository.isTafsirDownloaded(it.id.toString()) }.map { it.id.toString() }.toSet()
            _downloadedTafsirIds.value = downloaded
            
            // Auto-select 164 if nothing is selected and it's downloaded
            val currentSelected = selectedTafsirIds.value
            if (currentSelected.isEmpty() && downloaded.contains("164")) {
                setSelectedTafsirIds(setOf("164"))
            }
        }
    }

    fun downloadTafsir(id: String) {
        if (_downloadingTafsirIds.value.contains(id)) return
        _downloadingTafsirIds.value = _downloadingTafsirIds.value + id
        _tafsirDownloadProgress.value = _tafsirDownloadProgress.value.toMutableMap().apply { put(id, 0f) }
        
        viewModelScope.launch {
            try {
                quranRepository.downloadTafsir(id) { progress ->
                    _tafsirDownloadProgress.value = _tafsirDownloadProgress.value.toMutableMap().apply { put(id, progress) }
                }
                updateDownloadedTafsirs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _downloadingTafsirIds.value = _downloadingTafsirIds.value - id
                _tafsirDownloadProgress.value = _tafsirDownloadProgress.value.toMutableMap().apply { remove(id) }
            }
        }
    }

    

    private val _downloadingTranslationIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTranslationIds: StateFlow<Set<String>> = _downloadingTranslationIds.asStateFlow()

    private val _translationDownloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val translationDownloadProgress: StateFlow<Map<String, Float>> = _translationDownloadProgress.asStateFlow()

    private val _downloadedTranslations = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTranslations: StateFlow<Set<String>> = _downloadedTranslations.asStateFlow()

    private fun updateDownloadedTranslations() {
        viewModelScope.launch(Dispatchers.IO) {
            val downloaded = _availableTranslations.value.filter {
                quranRepository.isTranslationDownloaded(it.id.toString())
            }.map { it.id.toString() }.toSet()
            _downloadedTranslations.value = downloaded
        }
    }

    fun downloadTranslation(id: String) {
        if (_downloadingTranslationIds.value.contains(id)) return
        _downloadingTranslationIds.value = _downloadingTranslationIds.value + id
        _translationDownloadProgress.value = _translationDownloadProgress.value.toMutableMap().apply { put(id, 0f) }
        
        viewModelScope.launch {
            try {
                quranRepository.downloadTranslation(id) { progress ->
                    _translationDownloadProgress.value = _translationDownloadProgress.value.toMutableMap().apply { put(id, progress) }
                }
                updateDownloadedTranslations()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _downloadingTranslationIds.value = _downloadingTranslationIds.value - id
                _translationDownloadProgress.value = _translationDownloadProgress.value.toMutableMap().apply { remove(id) }
            }
        }
    }

    // ---------------- Offline Sync States ----------------

    
// ---------------- Offline Sync States ----------------
    private val _isDownloadingQuran = MutableStateFlow(false)
    val isDownloadingQuran: StateFlow<Boolean> = _isDownloadingQuran.asStateFlow()

    private val _quranDownloadProgress = MutableStateFlow(0) // 0 to 114
    val quranDownloadProgress: StateFlow<Int> = _quranDownloadProgress.asStateFlow()

    private val _quranDownloadError = MutableStateFlow<String?>(null)
    val quranDownloadError: StateFlow<String?> = _quranDownloadError.asStateFlow()

    private val _downloadedSurahsCount = MutableStateFlow(0)
    val downloadedSurahsCount: StateFlow<Int> = _downloadedSurahsCount.asStateFlow()

    private val _audioCacheSize = MutableStateFlow(0L)
    val audioCacheSize: StateFlow<Long> = _audioCacheSize.asStateFlow()

    // --- Audio manual download states ---
    private val _isDownloadingAudio = MutableStateFlow(false)
    val isDownloadingAudio: StateFlow<Boolean> = _isDownloadingAudio.asStateFlow()

    private val _audioDownloadProgress = MutableStateFlow(0) // 0 to 100%
    val audioDownloadProgress: StateFlow<Int> = _audioDownloadProgress.asStateFlow()

    private val _audioDownloadStatus = MutableStateFlow<String?>(null)
    val audioDownloadStatus: StateFlow<String?> = _audioDownloadStatus.asStateFlow()

    private val _audioDownloadError = MutableStateFlow<String?>(null)
    val audioDownloadError: StateFlow<String?> = _audioDownloadError.asStateFlow()

    private val _surahList = MutableStateFlow<List<Surah>>(emptyList())
    val surahList: StateFlow<List<Surah>> = _surahList.asStateFlow()

    private var audioDownloadJob: Job? = null

    fun loadSurahList() {
        viewModelScope.launch {
            try {
                _surahList.value = quranRepository.getSurahs()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun downloadAudioForSurah(surahNumber: Int, surahName: String) {
        audioDownloadJob?.cancel()
        audioDownloadJob = viewModelScope.launch {
            _isDownloadingAudio.value = true
            _audioDownloadProgress.value = 0
            val bengaliName = com.example.data.QuranData.surahNames.find { it.first == surahNumber }?.second?.first ?: surahName
            _audioDownloadStatus.value = "সুরা $bengaliName-এর অডিও ফাইল ডাউনলোড হচ্ছে..."
            _audioDownloadError.value = null
            try {
                val selectedQariId = repository.selectedQariIdFlow.first()
                val combinedAyahs = quranRepository.getSurahDetailsCombined(surahNumber)
                val totalAyahs = combinedAyahs.size
                if (totalAyahs == 0) {
                    _audioDownloadStatus.value = "কোনো আয়াত পাওয়া যায়নি"
                    return@launch
                }

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

                        if (downloadedOk) {
                            successCount++
                        } else {
                            failCount++
                        }

                        val progress = ((index + 1) * 100) / totalAyahs
                        _audioDownloadProgress.value = progress
                        updateAudioCacheSize()
                        updateDownloadedSurahsCount()
                    }
                }

                if (failCount == totalAyahs) {
                    _audioDownloadError.value = "অডিও ফাইল ডাউনলোড করতে ব্যর্থ হয়েছে! ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                    _audioDownloadStatus.value = null
                } else if (failCount > 0) {
                    _audioDownloadStatus.value = "সুরা $bengaliName-এর $successCount/$totalAyahs অডিও সম্পূর্ণ ডাউনলোড হয়েছে"
                } else {
                    _audioDownloadStatus.value = "সুরা $bengaliName-এর অডিও ডাউনলোড সফল হয়েছে!"
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _audioDownloadError.value = e.localizedMessage ?: "অডিও ডাউনলোড ব্যর্থ হয়েছে"
                _audioDownloadStatus.value = null
            } finally {
                _isDownloadingAudio.value = false
                updateAudioCacheSize()
                updateDownloadedSurahsCount()
            }
        }
    }

    fun cancelAudioDownload() {
        audioDownloadJob?.cancel()
        _isDownloadingAudio.value = false
        _audioDownloadStatus.value = "ডাউনলোড বাতিল করা হয়েছে"
        updateAudioCacheSize()
        updateDownloadedSurahsCount()
    }


    fun clearAudioCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(repository.context.filesDir, "quran_audio")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            updateAudioCacheSize()
        }
    }

    fun updateAudioCacheSize() {
        val dir = File(repository.context.filesDir, "quran_audio")
        _audioCacheSize.value = getFolderSize(dir)
    }

    private fun getFolderSize(folder: File): Long {
        var length = 0L
        val files = folder.listFiles()
        if (files != null) {
            for (file in files) {
                if (file.isFile) {
                    length += file.length()
                } else {
                    length += getFolderSize(file)
                }
            }
        }
        return length
    }


    private var downloadJob: Job? = null

    fun stopQuranDownload() {
        downloadJob?.cancel()
        _isDownloadingQuran.value = false
    }

    fun downloadAllQuranData() {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            _isDownloadingQuran.value = true
            _quranDownloadProgress.value = 0
            _quranDownloadError.value = null
            try {
                quranRepository.getSurahs()
                for (i in 1..114) {
                    ensureActive()
                    if (!quranRepository.isSurahDownloaded(i)) {
                        try {
                            val edition = tanzilTextStyle.value
                            quranRepository.downloadSurahDetailsSync(i, edition)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    _quranDownloadProgress.value = i
                    _downloadedSurahsCount.value = i
                }
            } catch (e: CancellationException) {
            } catch (e: Exception) {
                _quranDownloadError.value = e.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে"
            } finally {
                _isDownloadingQuran.value = false
                updateDownloadedSurahsCount()
            }
        }
    }

    fun deleteDownloadedQuranData() {
        viewModelScope.launch {
            quranRepository.deleteDownloadedSurahs()
            updateDownloadedSurahsCount()
        }
    }
    
    fun updateDownloadedSurahsCount() {
        viewModelScope.launch {
            val count = withContext(Dispatchers.IO) {
                quranRepository.getDownloadedSurahsCount()
            }
            _downloadedSurahsCount.value = count
        }
    }
    val hijriOffset: StateFlow<Int> = repository.hijriOffsetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 0
        )

    val combinedHijriOffset: StateFlow<Int> = repository.combinedHijriOffsetFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = 0
        )

    fun setHijriOffset(offset: Int) {
        viewModelScope.launch {
            repository.setHijriOffset(offset)
        }
    }

    val showTranslation: StateFlow<Boolean> = repository.showTranslationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = true
        )

    val showTransliteration: StateFlow<Boolean> = repository.showTransliterationFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    val showTajweed: StateFlow<Boolean> = repository.showTajweedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    val keepScreenOnFlow: StateFlow<Boolean> = repository.keepScreenOnFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    fun setKeepScreenOn(keep: Boolean) {
        viewModelScope.launch {
            repository.setKeepScreenOn(keep)
        }
    }
        
    fun setShowTransliteration(show: Boolean) {
        viewModelScope.launch { repository.setShowTransliteration(show) }
    }
    fun setShowTajweed(show: Boolean) {
        viewModelScope.launch { repository.setShowTajweed(show) }
    }

    private val _availableTafsirs = MutableStateFlow<List<com.example.data.model.TafsirResourceDto>>(emptyList())
    val availableTafsirs: StateFlow<List<com.example.data.model.TafsirResourceDto>> = _availableTafsirs.asStateFlow()

    private val _availableTranslations = MutableStateFlow<List<com.example.data.model.TranslationResourceDto>>(emptyList())
    val availableTranslations: StateFlow<List<com.example.data.model.TranslationResourceDto>> = _availableTranslations.asStateFlow()

    val selectedTranslationIds: StateFlow<Set<String>> = repository.selectedTranslationIdsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = setOf("161")
        )

    fun setSelectedTranslationIds(ids: Set<String>) {
        viewModelScope.launch { repository.setSelectedTranslationIds(ids) }
    }

    fun toggleTranslationId(id: String) {
        val current = selectedTranslationIds.value.toMutableSet()
        if (current.contains(id)) {
            if (current.size > 1) { // Prevent deselecting all
                current.remove(id)
            }
        } else {
            current.add(id)
        }
        setSelectedTranslationIds(current)
    }

    val selectedTafsirIds: StateFlow<Set<String>> = repository.selectedTafsirIdsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = setOf("164")
        )

    fun setSelectedTafsirIds(ids: Set<String>) {
        viewModelScope.launch { repository.setSelectedTafsirIds(ids) }
    }

    val selectedQariId: StateFlow<String> = repository.selectedQariIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "ar.alafasy"
        )

    fun setSelectedQariId(qariId: String) {
        viewModelScope.launch { repository.setSelectedQariId(qariId) }
    }
    
    fun toggleTafsir(id: String) {
        val current = selectedTafsirIds.value.toMutableSet()
        val isDownloaded = downloadedTafsirIds.value.contains(id)

        if (current.contains(id)) {
            if (current.size > 1) { // ensure at least one is selected
                current.remove(id)
                setSelectedTafsirIds(current)
            }
        } else {
            if (!isDownloaded) {
                current.clear()
                current.add(id)
            } else {
                val nonDownloaded = current.filter { !downloadedTafsirIds.value.contains(it) }
                current.removeAll(nonDownloaded.toSet())
                
                if (current.size >= 3) {
                    current.remove(current.first())
                }
                current.add(id)
            }
            setSelectedTafsirIds(current)
        }
    }

    val tanzilTextStyle: StateFlow<String> = repository.tanzilTextStyleFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "default-indopak"
        )

    fun toggleTranslation(show: Boolean) {
        viewModelScope.launch {
            repository.setShowTranslation(show)
        }
    }

    fun setTanzilTextStyle(style: String) {
        viewModelScope.launch {
            repository.setTanzilTextStyle(style)
        }
    }

    // 1. Bookmarks flow
    val bookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.getAllBookmarks()

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            bookmarkDao.deleteBookmark(bookmark)
        }
    }

    // 2. Profile & Notes Storage
    private val sharedPrefs by lazy {
        repository.context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
    }

    // Profile State
    private val _username = MutableStateFlow("দ্বীনদার বান্দা")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _readingTimeMinutes = MutableStateFlow(60) // Default 1 hour reading
    val readingTimeMinutes: StateFlow<Int> = _readingTimeMinutes.asStateFlow()

    fun updateUsername(newName: String) {
        _username.value = newName
        sharedPrefs.edit().putString("username", newName).apply()
    }

    fun addReadingTime(mins: Int) {
        _readingTimeMinutes.value += mins
        sharedPrefs.edit().putInt("reading_time", _readingTimeMinutes.value).apply()
    }

    // Notes State
    private val _notes = MutableStateFlow<List<UserNote>>(emptyList())
    val notes: StateFlow<List<UserNote>> = _notes.asStateFlow()

    fun addNote(title: String, content: String) {
        val newNote = UserNote(
            id = System.currentTimeMillis().toString(),
            title = title,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        val updated = _notes.value + newNote
        _notes.value = updated
        saveNotesToPrefs(updated)
    }

    fun deleteNote(noteId: String) {
        val updated = _notes.value.filterNot { it.id == noteId }
        _notes.value = updated
        saveNotesToPrefs(updated)
    }

    private fun loadNotesFromPrefs() {
        val notesSet = sharedPrefs.getStringSet("user_notes_set", emptySet()) ?: emptySet()
        val loaded = notesSet.mapNotNull { noteStr ->
            val parts = noteStr.split("|||")
            if (parts.size >= 4) {
                UserNote(
                    id = parts[0],
                    title = parts[1],
                    content = parts[2],
                    timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis()
                )
            } else null
        }.sortedByDescending { it.timestamp }
        _notes.value = loaded
    }

    private fun saveNotesToPrefs(notesList: List<UserNote>) {
        val notesSet = notesList.map { "${it.id}|||${it.title}|||${it.content}|||${it.timestamp}" }.toSet()
        sharedPrefs.edit().putStringSet("user_notes_set", notesSet).apply()
    }

    // 3. Word Game State
    private val _gamePhase = MutableStateFlow(GamePhase.SETUP)
    val gamePhase: StateFlow<GamePhase> = _gamePhase.asStateFlow()

    private val _gameErrorMessage = MutableStateFlow<String?>(null)
    val gameErrorMessage: StateFlow<String?> = _gameErrorMessage.asStateFlow()

    fun clearGameErrorMessage() {
        _gameErrorMessage.value = null
    }

    private val _gameConfig = MutableStateFlow(WordGameConfig())
    val gameConfig: StateFlow<WordGameConfig> = _gameConfig.asStateFlow()

    private val _dynamicQuestions = MutableStateFlow<List<WordQuestion>>(emptyList())
    val dynamicQuestions: StateFlow<List<WordQuestion>> = _dynamicQuestions.asStateFlow()

    private val _gameScore = MutableStateFlow(0)
    val gameScore: StateFlow<Int> = _gameScore.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _lastAnswerCorrect = MutableStateFlow<Boolean?>(null)
    val lastAnswerCorrect: StateFlow<Boolean?> = _lastAnswerCorrect.asStateFlow()
    private val _selectedAnswer = MutableStateFlow<String?>(null)
    val selectedAnswer: StateFlow<String?> = _selectedAnswer.asStateFlow()

    fun updateGameConfig(config: WordGameConfig) {
        _gameConfig.value = config
    }
    
    fun setGamePhase(phase: GamePhase) {
        _gamePhase.value = phase
    }

    fun startDynamicGame() {
        _gamePhase.value = GamePhase.LOADING
        viewModelScope.launch {
            try {
                val config = _gameConfig.value
                val numRegex = Regex("[0-9০-৯٠-٩]")
                val allWords = mutableListOf<com.example.data.model.QuranComWord>()
                val seenKeys = mutableSetOf<Pair<String, String>>()
                
                if (config.source == GameSource.ENTIRE_QURAN) {
                    var attempts = 0
                    while (seenKeys.size < config.totalQuestions && attempts < 15) {
                        attempts++
                        val randomSurah = (1..114).random()
                        val words = quranRepository.getSurahWords(randomSurah)
                        val filtered = words.filter { 
                            it.charTypeName == "word" && 
                            it.translation?.text != null && 
                            it.textUthmani != null && 
                            it.translation.text.isNotBlank() && 
                            it.textUthmani.isNotBlank() 
                        }
                        for (w in filtered) {
                                val cleanAr = w.textUthmani?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                                val cleanBn = w.translation?.text?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                                if (cleanAr.isNotEmpty() && cleanBn.isNotEmpty()) {
                                    val pair = cleanAr to cleanBn
                                    if (pair !in seenKeys) {
                                        seenKeys.add(pair)
                                        allWords.add(w)
                                    }
                                }
                            }
                    }
                } else {
                    val words = quranRepository.getSurahWords(config.selectedSurah)
                    val filtered = words.filter { 
                        it.charTypeName == "word" && 
                        it.translation?.text != null && 
                        it.textUthmani != null && 
                        it.translation.text.isNotBlank() && 
                        it.textUthmani.isNotBlank() 
                    }
                    for (w in filtered) {
                            val cleanAr = w.textUthmani?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                            val cleanBn = w.translation?.text?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                            if (cleanAr.isNotEmpty() && cleanBn.isNotEmpty()) {
                                val pair = cleanAr to cleanBn
                                if (pair !in seenKeys) {
                                    seenKeys.add(pair)
                                    allWords.add(w)
                                }
                            }
                        }
                    
                    // Supplement with Surah Al-Baqarah if not enough unique words in the selected surah
                    if (allWords.size < config.totalQuestions) {
                        val fallbackWords = quranRepository.getSurahWords(2)
                        val filtered = fallbackWords.filter { 
                            it.charTypeName == "word" && 
                            it.translation?.text != null && 
                            it.textUthmani != null && 
                            it.translation.text.isNotBlank() && 
                            it.textUthmani.isNotBlank() 
                        }
                        for (w in filtered) {
                                if (allWords.size >= config.totalQuestions) break
                                val cleanAr = w.textUthmani?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                                val cleanBn = w.translation?.text?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                                if (cleanAr.isNotEmpty() && cleanBn.isNotEmpty()) {
                                    val pair = cleanAr to cleanBn
                                    if (pair !in seenKeys) {
                                        seenKeys.add(pair)
                                        allWords.add(w)
                                    }
                                }
                            }
                    }
                }
                
                val selectedWords = allWords.shuffled().take(config.totalQuestions)
                
                val generatedQuestions = selectedWords.map { word ->
                    val isArabicToBengali = config.type == GameType.ARABIC_TO_BENGALI
                    val questionTextRaw = if (isArabicToBengali) "${word.textUthmani}" else "${word.translation?.text}"
                    val correctAnsRaw = if (isArabicToBengali) "${word.translation?.text}" else "${word.textUthmani}"
                    
                    val questionText = questionTextRaw.replace(numRegex, "").trim()
                    val correctAns = correctAnsRaw.replace(numRegex, "").trim()
                    
                    // Generate unique wrong options that are visually/textually distinct from correctAns
                    val otherWords = allWords.filter {
                        val otherAr = it.textUthmani?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                        val otherBn = it.translation?.text?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                        val thisAr = word.textUthmani?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                        val thisBn = word.translation?.text?.replace(numRegex, "")?.trim()?.lowercase() ?: ""
                        
                        if (isArabicToBengali) {
                            otherBn != thisBn && otherBn.isNotEmpty()
                        } else {
                            otherAr != thisAr && otherAr.isNotEmpty()
                        }
                    }
                    
                    val wrongOptions = otherWords.map {
                        val raw = if (isArabicToBengali) "${it.translation?.text}" else "${it.textUthmani}"
                        raw.replace(numRegex, "").trim()
                    }.filter { it.isNotEmpty() }.distinct().shuffled().take(3)
                    
                    var options = (wrongOptions + correctAns).distinct()
                    
                    // Ensure we have exactly 4 options by filling with high-quality fallback options if needed
                    if (options.size < 4) {
                        val defaultWrong = if (isArabicToBengali) {
                            listOf("দয়া", "শান্তি", "জ্ঞান", "বিশ্বাস", "পরিত্রাণ", "ধৈর্য", "সত্য")
                        } else {
                            listOf("رَحْمَة", "سَلَام", "عِلْم", "إِيمَان", "نَجَاة", "صَبْر", "حَقّ")
                        }
                        for (fallbackOpt in defaultWrong) {
                            if (options.size >= 4) break
                            if (!options.contains(fallbackOpt)) {
                                options = options + fallbackOpt
                            }
                        }
                    }
                    
                    WordQuestion(questionText, options.shuffled(), correctAns)
                }
                
                if (generatedQuestions.isEmpty()) {
                    _gameErrorMessage.value = "গেম শুরু করা যাচ্ছে না। অনুগ্রহ করে নিশ্চিত করুন যে আপনি ইন্টারনেটে যুক্ত আছেন বা অফলাইন ডাটা ডাউনলোড করেছেন।"
                    _gamePhase.value = GamePhase.SETUP
                    return@launch
                }
                
                _dynamicQuestions.value = generatedQuestions
                _gameScore.value = 0
                _currentQuestionIndex.value = 0
                _lastAnswerCorrect.value = null
                _selectedAnswer.value = null
                _gamePhase.value = GamePhase.PLAYING
                
            } catch (e: Exception) {
                e.printStackTrace()
                _gamePhase.value = GamePhase.SETUP // go back on error
            }
        }
    }

    fun submitAnswer(selectedAnswer: String) {
        _selectedAnswer.value = selectedAnswer
        val currentQ = _dynamicQuestions.value[_currentQuestionIndex.value]
        val isCorrect = selectedAnswer == currentQ.correctAnswer
        _lastAnswerCorrect.value = isCorrect
        if (isCorrect) {
            _gameScore.value += 1
        }
    }

    fun nextQuestion() {
        _lastAnswerCorrect.value = null
        _selectedAnswer.value = null
        val nextIdx = _currentQuestionIndex.value + 1
        if (nextIdx < _dynamicQuestions.value.size) {
            _currentQuestionIndex.value = nextIdx
        } else {
            _gamePhase.value = GamePhase.RESULT
        }
    }

    fun resetGame() {
        _gamePhase.value = GamePhase.SETUP
        _gameScore.value = 0
        _currentQuestionIndex.value = 0
        _lastAnswerCorrect.value = null
        _selectedAnswer.value = null
        _dynamicQuestions.value = emptyList()
    }

    // 4. Quran Planner State
    private val _plannerTarget = MutableStateFlow("৩০ দিনে খতম")
    val plannerTarget: StateFlow<String> = _plannerTarget.asStateFlow()

    private val _plannerPagesRead = MutableStateFlow(0)
    val plannerPagesRead: StateFlow<Int> = _plannerPagesRead.asStateFlow()

    private val _plannerStartDate = MutableStateFlow(System.currentTimeMillis())
    val plannerStartDate: StateFlow<Long> = _plannerStartDate.asStateFlow()
    
    private val _plannerStreak = MutableStateFlow(0)
    val plannerStreak: StateFlow<Int> = _plannerStreak.asStateFlow()
    
    private val _plannerReminderEnabled = MutableStateFlow(false)
    val plannerReminderEnabled: StateFlow<Boolean> = _plannerReminderEnabled.asStateFlow()

    private val _plannerReminderHour = MutableStateFlow(20)
    val plannerReminderHour: StateFlow<Int> = _plannerReminderHour.asStateFlow()

    private val _plannerReminderMinute = MutableStateFlow(0)
    val plannerReminderMinute: StateFlow<Int> = _plannerReminderMinute.asStateFlow()

    
    private val _dailyMessageEnabled = MutableStateFlow(true)
    val dailyMessageEnabled: StateFlow<Boolean> = _dailyMessageEnabled.asStateFlow()

    private val _dailyMessageHour = MutableStateFlow(8)
    val dailyMessageHour: StateFlow<Int> = _dailyMessageHour.asStateFlow()

    private val _dailyMessageMinute = MutableStateFlow(0)
    val dailyMessageMinute: StateFlow<Int> = _dailyMessageMinute.asStateFlow()

    fun toggleDailyMessage(enabled: Boolean) {
        _dailyMessageEnabled.value = enabled
        sharedPrefs.edit().putBoolean("daily_message_enabled", enabled).apply()
        val context = repository.context
        if (enabled) {
            com.example.receiver.DailyMessageReceiver.scheduleNextAlarm(context)
        } else {
            com.example.receiver.DailyMessageReceiver.cancelAlarm(context)
        }
    }

    fun updateDailyMessageTime(hour: Int, minute: Int) {
        _dailyMessageHour.value = hour
        _dailyMessageMinute.value = minute
        sharedPrefs.edit()
            .putInt("daily_message_hour", hour)
            .putInt("daily_message_minute", minute)
            .apply()
        
        if (_dailyMessageEnabled.value) {
            com.example.receiver.DailyMessageReceiver.scheduleNextAlarm(repository.context)
        }
    }

    fun updatePlannerTarget(target: String) {
        _plannerTarget.value = target
        _plannerPagesRead.value = 0
        _plannerStartDate.value = System.currentTimeMillis()
        _plannerStreak.value = 0
        sharedPrefs.edit()
            .putString("planner_target", target)
            .putInt("planner_pages_read", 0)
            .putLong("planner_start_date", _plannerStartDate.value)
            .putInt("planner_streak", 0)
            .apply()
    }

    fun addPlannerPages(pages: Int) {
        val total = (_plannerPagesRead.value + pages).coerceAtMost(610).coerceAtLeast(0)
        _plannerPagesRead.value = total
        
        // Simple streak logic for demo: increment streak if adding pages
        if (pages > 0) {
            _plannerStreak.value += 1
            sharedPrefs.edit().putInt("planner_streak", _plannerStreak.value).apply()
        }
        
        sharedPrefs.edit().putInt("planner_pages_read", total).apply()
    }
    
    fun togglePlannerReminder(enabled: Boolean) {
        _plannerReminderEnabled.value = enabled
        sharedPrefs.edit().putBoolean("planner_reminder", enabled).apply()
        val context = repository.context
        if (enabled) {
            com.example.receiver.ReminderReceiver.scheduleNextAlarm(context)
        } else {
            com.example.receiver.ReminderReceiver.cancelAlarm(context)
        }
    }

    fun updatePlannerReminderTime(hour: Int, minute: Int) {
        _plannerReminderHour.value = hour
        _plannerReminderMinute.value = minute
        sharedPrefs.edit()
            .putInt("planner_reminder_hour", hour)
            .putInt("planner_reminder_minute", minute)
            .apply()
        
        // Re-schedule alarm if enabled
        if (_plannerReminderEnabled.value) {
            com.example.receiver.ReminderReceiver.scheduleNextAlarm(repository.context)
        }
    }

    // 5. Quran Hifz State
    private val _hifzProgress = MutableStateFlow<Map<String, String>>(emptyMap())
    val hifzProgress: StateFlow<Map<String, String>> = _hifzProgress.asStateFlow()

    fun updateHifzProgress(surahName: String, status: String) {
        val current = _hifzProgress.value.toMutableMap()
        current[surahName] = status
        _hifzProgress.value = current
        val serializedMap = current.entries.joinToString(";") { "${it.key}:${it.value}" }
        sharedPrefs.edit().putString("hifz_progress_map", serializedMap).apply()
    }

    init {
        _username.value = sharedPrefs.getString("username", "দ্বীনদার বান্দা") ?: "দ্বীনদার বান্দা"
        _readingTimeMinutes.value = sharedPrefs.getInt("reading_time", 60)
        _plannerTarget.value = sharedPrefs.getString("planner_target", "৩০ দিনে খতম") ?: "৩০ দিনে খতম"
        _plannerPagesRead.value = sharedPrefs.getInt("planner_pages_read", 0)
        _plannerStartDate.value = sharedPrefs.getLong("planner_start_date", System.currentTimeMillis())
        _plannerStreak.value = sharedPrefs.getInt("planner_streak", 0)
        
        _dailyMessageEnabled.value = sharedPrefs.getBoolean("daily_message_enabled", true)
        _dailyMessageHour.value = sharedPrefs.getInt("daily_message_hour", 8)
        _dailyMessageMinute.value = sharedPrefs.getInt("daily_message_minute", 0)

        _plannerReminderEnabled.value = sharedPrefs.getBoolean("planner_reminder", false)
        _plannerReminderHour.value = sharedPrefs.getInt("planner_reminder_hour", 20)
        _plannerReminderMinute.value = sharedPrefs.getInt("planner_reminder_minute", 0)

        val alarmsInitialized = sharedPrefs.getBoolean("alarms_initialized", false)
        if (!alarmsInitialized) {
            sharedPrefs.edit()
                .putBoolean("daily_message_enabled", true)
                .putBoolean("planner_reminder", false)
                .putBoolean("alarms_initialized", true)
                .apply()
            
            try {
                com.example.receiver.DailyMessageReceiver.scheduleNextAlarm(repository.context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        val hifzStr = sharedPrefs.getString("hifz_progress_map", "") ?: ""
        if (hifzStr.isNotEmpty()) {
            val hifzMap = hifzStr.split(";").mapNotNull { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
            _hifzProgress.value = hifzMap
        }

        loadNotesFromPrefs()
        
        updateAudioCacheSize()
        updateDownloadedSurahsCount()
        
        viewModelScope.launch {
            _availableTafsirs.value = quranRepository.getAvailableTafsirs("bn")
            _availableTranslations.value = quranRepository.getAvailableTranslations("bn")
            updateDownloadedTafsirs()
            updateDownloadedTranslations()
        }
    }
}
