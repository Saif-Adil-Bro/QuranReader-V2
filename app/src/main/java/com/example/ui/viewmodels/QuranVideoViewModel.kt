package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.QuranData
import com.example.data.local.offline.OfflineQuranDatabase
import com.example.data.model.*
import com.example.util.AudioUtils
import com.example.util.QariData
import com.example.utils.QuranVideoExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class QuranVideoViewModel(application: Application) : AndroidViewModel(application) {

    private val context: Context get() = getApplication<Application>().applicationContext
    private val offlineDao by lazy { OfflineQuranDatabase.getDatabase(context).offlineQuranDao() }

    // UI Config State
    private val _config = MutableStateFlow(QuranVideoConfig())
    val config: StateFlow<QuranVideoConfig> = _config.asStateFlow()

    // Current screen step (1: Content, 2: Customize, 3: Preview, 4: Export)
    private val _currentStep = MutableStateFlow(1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    // Audio Preparation State (Step 2)
    private val _audioPrepState = MutableStateFlow(AudioPreparationState())
    val audioPrepState: StateFlow<AudioPreparationState> = _audioPrepState.asStateFlow()

    // Audio Player State
    private var exoPlayer: ExoPlayer? = null
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlaybackProgress = MutableStateFlow(0f)
    val currentPlaybackProgress: StateFlow<Float> = _currentPlaybackProgress.asStateFlow()

    private val _currentAyahIndex = MutableStateFlow(0)
    val currentAyahIndex: StateFlow<Int> = _currentAyahIndex.asStateFlow()

    private val _playingAyahNumber = MutableStateFlow<Int?>(null)
    val playingAyahNumber: StateFlow<Int?> = _playingAyahNumber.asStateFlow()

    // Export State
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportProgress = MutableStateFlow(0f)
    val exportProgress: StateFlow<Float> = _exportProgress.asStateFlow()

    private val _exportedVideoUri = MutableStateFlow<Uri?>(null)
    val exportedVideoUri: StateFlow<Uri?> = _exportedVideoUri.asStateFlow()

    private val _exportedVideoFile = MutableStateFlow<File?>(null)
    val exportedVideoFile: StateFlow<File?> = _exportedVideoFile.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    private var progressTrackingJob: Job? = null
    private var singleAyahPlayer: ExoPlayer? = null
    private var loadSurahJob: Job? = null

    init {
        // Load initial Surah Al-Fatiha
        loadSurah(1, 1, 7)
    }

    fun setStep(step: Int) {
        _currentStep.value = step.coerceIn(1, 4)
        if (step != 3) {
            pausePreviewAudio()
        } else {
            prepareAndPlayPreviewAudio()
        }
    }

    fun loadSurah(surahNumber: Int, ayahStart: Int = 1, ayahEnd: Int = 1, includeBismillah: Boolean? = null) {
        loadSurahJob?.cancel()
        val bismillah = if (surahNumber == 1) false else (includeBismillah ?: _config.value.includeBismillah)
        val totalCount = QuranData.getAyahCount(surahNumber)
        val start = ayahStart.coerceIn(1, totalCount)
        val end = ayahEnd.coerceIn(start, totalCount)
        val banglaName = QuranData.getSurahNameBangla(surahNumber)

        loadSurahJob = viewModelScope.launch {
            val dbAyahs = withContext(Dispatchers.IO) {
                try {
                    offlineDao.getAyahsBySurahRange(surahNumber, start, end)
                } catch (e: Exception) {
                    emptyList()
                }
            }

            val surahAyahs = if (dbAyahs.isNotEmpty()) {
                dbAyahs.map { entity ->
                    CombinedAyah(
                        number = entity.globalNumber,
                        numberInSurah = entity.numberInSurah,
                        page = entity.page,
                        juz = entity.juz,
                        surahNumber = surahNumber,
                        arabicText = entity.arabicText,
                        bengaliText = entity.bengaliText,
                        tafsirText = null,
                        audioUrl = null,
                        words = emptyList(),
                        textUthmaniTajweed = null
                    )
                }
            } else {
                (start..end).map { ayahNum ->
                    val globalNum = QuranData.getGlobalAyahNumber(surahNumber, ayahNum)
                    CombinedAyah(
                        number = globalNum,
                        numberInSurah = ayahNum,
                        page = 1,
                        juz = 1,
                        surahNumber = surahNumber,
                        arabicText = QuranData.getArabicAyah(surahNumber, ayahNum),
                        bengaliText = QuranData.getBanglaAyah(surahNumber, ayahNum),
                        tafsirText = null,
                        audioUrl = null,
                        words = emptyList(),
                        textUthmaniTajweed = null
                    )
                }
            }

            val finalAyahs = if (surahNumber > 1 && bismillah) {
                val bismillahAyah = withContext(Dispatchers.IO) {
                    try {
                        offlineDao.getAyahBySurahAndNumber(1, 1)?.let { entity ->
                            CombinedAyah(
                                number = entity.globalNumber, // 1
                                numberInSurah = 1,
                                page = entity.page,
                                juz = entity.juz,
                                surahNumber = 1,
                                arabicText = entity.arabicText,
                                bengaliText = entity.bengaliText,
                                tafsirText = null,
                                audioUrl = null,
                                words = emptyList(),
                                textUthmaniTajweed = null
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                } ?: CombinedAyah(
                    number = 1,
                    numberInSurah = 1,
                    page = 1,
                    juz = 1,
                    surahNumber = 1,
                    arabicText = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                    bengaliText = "শুরু করছি আল্লাহর নামে যিনি পরম করুণাময়, অতি দয়ালু।",
                    tafsirText = null,
                    audioUrl = null,
                    words = emptyList(),
                    textUthmaniTajweed = null
                )
                listOf(bismillahAyah) + surahAyahs
            } else {
                surahAyahs
            }

            _config.value = _config.value.copy(
                surahNumber = surahNumber,
                surahName = banglaName,
                ayahStart = start,
                ayahEnd = end,
                includeBismillah = bismillah,
                selectedAyahs = finalAyahs
            )
            _audioPrepState.value = AudioPreparationState()
        }
    }

    fun toggleIncludeBismillah(include: Boolean) {
        val current = _config.value
        loadSurah(current.surahNumber, current.ayahStart, current.ayahEnd, includeBismillah = include)
    }

    fun loadFromQuickCreate(surahNumber: Int, ayahNumber: Int, ayah: CombinedAyah, surahName: String) {
        loadSurahJob?.cancel()
        val computedSurahNum = when {
            surahNumber > 0 -> surahNumber
            ayah.surahNumber > 0 -> ayah.surahNumber
            else -> QuranData.getSurahAndAyahFromGlobal(ayah.number).first
        }
        val computedAyahNum = if (ayahNumber > 0) ayahNumber else ayah.numberInSurah
        val computedSurahName = when {
            surahName.isNotBlank() && !surahName.startsWith("সূরা 0") -> surahName
            else -> QuranData.getSurahNameBangla(computedSurahNum)
        }
        val formattedAyah = ayah.copy(surahNumber = computedSurahNum)

        _config.value = _config.value.copy(
            surahNumber = computedSurahNum,
            surahName = computedSurahName,
            ayahStart = computedAyahNum,
            ayahEnd = computedAyahNum,
            includeBismillah = false,
            selectedAyahs = listOf(formattedAyah)
        )
        _audioPrepState.value = AudioPreparationState()
        setStep(2) // Jump directly to Customize screen
    }

    fun setTemplate(template: QuranVideoTemplate) {
        _config.value = _config.value.copy(
            template = template,
            overlay = template.defaultOverlay,
            animationStyle = template.defaultAnimation
        )
    }

    fun setAspectRatio(ratio: VideoAspectRatio) {
        _config.value = _config.value.copy(aspectRatio = ratio)
    }

    fun setBackgroundPreset(presetName: String?) {
        _config.value = _config.value.copy(
            backgroundPresetName = presetName,
            customImageUri = null
        )
    }

    fun setCustomImage(uriString: String?) {
        _config.value = _config.value.copy(
            customImageUri = uriString,
            backgroundPresetName = null
        )
    }

    fun setOverlay(overlay: BackgroundOverlay) {
        _config.value = _config.value.copy(overlay = overlay)
    }

    fun setAnimationStyle(style: TextAnimationStyle) {
        _config.value = _config.value.copy(animationStyle = style)
    }

    fun toggleBanglaTranslation(show: Boolean) {
        _config.value = _config.value.copy(showBanglaTranslation = show)
    }

    fun toggleReference(show: Boolean) {
        _config.value = _config.value.copy(showReference = show)
    }

    fun toggleLogo(show: Boolean) {
        _config.value = _config.value.copy(showLogo = show)
    }

    fun toggleCredit(show: Boolean) {
        _config.value = _config.value.copy(showCredit = show)
    }

    fun setCreditText(text: String) {
        _config.value = _config.value.copy(creditText = text)
    }

    fun setLogoText(text: String) {
        _config.value = _config.value.copy(logoText = text)
    }

    fun toggleWaqfSigns(show: Boolean) {
        _config.value = _config.value.copy(showWaqfSigns = show)
    }

    fun setArabicFontSize(size: Float) {
        _config.value = _config.value.copy(arabicFontSize = size)
    }

    fun setArabicLineSpacing(spacing: Float) {
        _config.value = _config.value.copy(arabicLineSpacing = spacing)
    }

    fun setTranslationFontSize(size: Float) {
        _config.value = _config.value.copy(translationFontSize = size)
    }

    fun setTranslationLineSpacing(spacing: Float) {
        _config.value = _config.value.copy(translationLineSpacing = spacing)
    }

    fun setArabicFontName(fontName: String) {
        _config.value = _config.value.copy(arabicFontName = fontName)
    }

    fun setBengaliFontName(fontName: String) {
        _config.value = _config.value.copy(bengaliFontName = fontName)
    }

    fun setQari(qariId: String, qariName: String) {
        _config.value = _config.value.copy(qariId = qariId, qariName = qariName)
        // Invalidate previous prepared audio if qari changed
        _audioPrepState.value = AudioPreparationState()
    }

    // Audio Preparation Workflow (Step 2)
    fun startAudioPreparation(onComplete: (() -> Unit)? = null) {
        val ayahs = _config.value.selectedAyahs
        if (ayahs.isEmpty()) return

        stopSingleAyahAudio()
        pausePreviewAudio()

        viewModelScope.launch {
            _audioPrepState.value = AudioPreparationState(
                isDownloading = true,
                isDownloaded = false,
                progress = 0.05f,
                preparedAudios = emptyList(),
                error = null
            )

            val qariId = _config.value.qariId
            val preparedList = mutableListOf<PreparedAyahAudio>()
            var totalDuration = 0L

            val downloadDir = File(context.cacheDir, "quran_video_audios").apply { if (!exists()) mkdirs() }

            for (i in ayahs.indices) {
                val ayah = ayahs[i]
                _audioPrepState.value = _audioPrepState.value.copy(
                    currentAyahNumber = ayah.numberInSurah,
                    progress = 0.05f + (0.90f * (i.toFloat() / ayahs.size))
                )

                val audioUrl = AudioUtils.getAudioUrl(qariId, ayah.number)
                val destFile = File(downloadDir, "ayah_${qariId}_${ayah.number}.mp3")

                var durationMs = 3500L
                var downloadSuccess = false

                withContext(Dispatchers.IO) {
                    try {
                        if (destFile.exists() && destFile.length() > 500) {
                            downloadSuccess = true
                        } else {
                            val url = java.net.URL(audioUrl)
                            val conn = url.openConnection()
                            conn.connectTimeout = 12000
                            conn.readTimeout = 20000
                            conn.getInputStream().use { input ->
                                java.io.FileOutputStream(destFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                            downloadSuccess = destFile.exists() && destFile.length() > 500
                        }

                        if (downloadSuccess) {
                            var durationFound = false
                            val extractor = android.media.MediaExtractor()
                            try {
                                extractor.setDataSource(destFile.absolutePath)
                                for (t in 0 until extractor.trackCount) {
                                    val format = extractor.getTrackFormat(t)
                                    val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: ""
                                    if (mime.startsWith("audio/")) {
                                        if (format.containsKey(android.media.MediaFormat.KEY_DURATION)) {
                                            val durUs = format.getLong(android.media.MediaFormat.KEY_DURATION)
                                            if (durUs > 300_000L) { // > 300ms
                                                durationMs = durUs / 1000L
                                                durationFound = true
                                            }
                                        }
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("QuranVideoViewModel", "MediaExtractor failed", e)
                            } finally {
                                try { extractor.release() } catch (e: Exception) {}
                            }

                            if (!durationFound) {
                                val mmr = android.media.MediaMetadataRetriever()
                                try {
                                    mmr.setDataSource(destFile.absolutePath)
                                    val durStr = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    val dur = durStr?.toLongOrNull()
                                    if (dur != null && dur > 300) {
                                        durationMs = dur
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("QuranVideoViewModel", "Metadata extraction failed for ${ayah.number}", e)
                                } finally {
                                    try { mmr.release() } catch (e: Exception) {}
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("QuranVideoViewModel", "Download error $audioUrl", e)
                    }
                }

                val item = PreparedAyahAudio(
                    ayahNumber = ayah.number,
                    numberInSurah = ayah.numberInSurah,
                    localFile = destFile,
                    durationMs = durationMs,
                    isReady = downloadSuccess,
                    arabicPreview = ayah.arabicText.take(50)
                )
                preparedList.add(item)
                totalDuration += durationMs
            }

            _audioPrepState.value = AudioPreparationState(
                isDownloading = false,
                isDownloaded = true,
                progress = 1.0f,
                currentAyahNumber = 0,
                preparedAudios = preparedList,
                totalDurationMs = totalDuration,
                error = null
            )

            onComplete?.invoke()
        }
    }

    fun playSingleAyahAudio(item: PreparedAyahAudio) {
        stopSingleAyahAudio()
        pausePreviewAudio()

        if (!item.localFile.exists()) return

        _playingAyahNumber.value = item.numberInSurah
        singleAyahPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(item.localFile)))
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _playingAyahNumber.value = null
                    }
                }
            })
        }
    }

    fun stopSingleAyahAudio() {
        singleAyahPlayer?.release()
        singleAyahPlayer = null
        _playingAyahNumber.value = null
    }

    // Audio Preview Controls (Step 3)
    fun prepareAndPlayPreviewAudio() {
        stopSingleAyahAudio()
        releasePlayer()

        val currentAyahs = _config.value.selectedAyahs
        if (currentAyahs.isEmpty()) return

        val preparedList = _audioPrepState.value.preparedAudios
        val mediaItems = currentAyahs.map { ayah ->
            val prep = preparedList.find { it.ayahNumber == ayah.number }
            if (prep != null && prep.localFile.exists()) {
                MediaItem.fromUri(Uri.fromFile(prep.localFile))
            } else {
                val audioUrl = AudioUtils.getAudioUrl(_config.value.qariId, ayah.number)
                MediaItem.fromUri(audioUrl)
            }
        }

        exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItems(mediaItems)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    _isPlaying.value = playing
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    _currentAyahIndex.value = currentMediaItemIndex
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        _isPlaying.value = false
                        _currentPlaybackProgress.value = 1f
                    }
                }
            })
        }

        startProgressTracker()
    }

    fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        } ?: prepareAndPlayPreviewAudio()
    }

    fun seekToProgress(progress: Float) {
        exoPlayer?.let { player ->
            val duration = player.duration
            if (duration > 0) {
                val seekPosition = (duration * progress).toLong()
                player.seekTo(seekPosition)
            }
        }
    }

    fun pausePreviewAudio() {
        exoPlayer?.pause()
        _isPlaying.value = false
        progressTrackingJob?.cancel()
    }

    private fun startProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = viewModelScope.launch {
            while (true) {
                exoPlayer?.let { player ->
                    val pos = player.currentPosition
                    val dur = player.duration
                    if (dur > 0) {
                        _currentPlaybackProgress.value = (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
                    }
                }
                delay(100)
            }
        }
    }

    // Export Video (Step 4)
    fun startExport() {
        stopSingleAyahAudio()
        pausePreviewAudio()
        _isExporting.value = true
        _exportProgress.value = 0f
        _exportError.value = null
        _exportedVideoUri.value = null
        _exportedVideoFile.value = null
        setStep(4)

        viewModelScope.launch {
            val audioUrls = _config.value.selectedAyahs.map { ayah ->
                AudioUtils.getAudioUrl(_config.value.qariId, ayah.number)
            }

            val result = QuranVideoExporter.exportQuranVideo(
                context = context,
                config = _config.value,
                audioFileUrls = audioUrls,
                preparedAudios = _audioPrepState.value.preparedAudios,
                onProgress = { progress ->
                    _exportProgress.value = progress
                }
            )

            _isExporting.value = false
            if (result.isSuccess) {
                _exportedVideoUri.value = result.videoUri
                _exportedVideoFile.value = result.videoFile
                Toast.makeText(context, "ভিডিও সফলভাবে গ্যালারিতে সংরক্ষিত হয়েছে!", Toast.LENGTH_LONG).show()
            } else {
                _exportError.value = result.errorMessage ?: "ভিডিও রেন্ডারিং ব্যর্থ হয়েছে।"
            }
        }
    }

    fun shareExportedVideo() {
        val file = _exportedVideoFile.value
        val mediaUri = _exportedVideoUri.value

        if (file == null && mediaUri == null) {
            Toast.makeText(context, "শেয়ার করার মতো কোনো ভিডিও পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val shareUri: Uri = if (file != null && file.exists()) {
                androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else if (mediaUri != null) {
                mediaUri
            } else {
                Toast.makeText(context, "ভিডিও ফাইল খুঁজে পাওয়া যায়নি।", Toast.LENGTH_SHORT).show()
                return
            }

            val shareText = "সূরা ${_config.value.surahName} • আয়াত ${_config.value.ayahStart}\nকুরআন রিডার অ্যাপ থেকে তৈরি"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "ভিডিও শেয়ার করুন").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "শেয়ার করতে ব্যর্থ হয়েছে: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun releasePlayer() {
        progressTrackingJob?.cancel()
        exoPlayer?.release()
        exoPlayer = null
        _isPlaying.value = false
        _currentPlaybackProgress.value = 0f
    }

    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        stopSingleAyahAudio()
    }
}
