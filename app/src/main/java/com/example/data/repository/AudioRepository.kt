package com.example.data.repository

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.service.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * Repository to manage audio playback using ExoPlayer via MediaSessionService.
 */
class AudioRepository(private val context: Context) {

    private var exoPlayer: Player? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPlayingAyahNumber = MutableStateFlow<Int?>(null)
    val currentPlayingAyahNumber: StateFlow<Int?> = _currentPlayingAyahNumber.asStateFlow()

    private val _currentPlayingWordUrl = MutableStateFlow<String?>(null)
    val currentPlayingWordUrl: StateFlow<String?> = _currentPlayingWordUrl.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    var onPlaybackEnded: (() -> Unit)? = null
    
    private var isInitializing = false
    private var pendingPlayUrl: String? = null
    private var pendingAyahNumber: Int? = null

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        exoPlayer?.setPlaybackSpeed(speed)
    }

    fun getLocalAudioFile(url: String): File {
        val dir = File(context.filesDir, "quran_audio")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val filename = url.substringAfter("://").replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
        return File(dir, filename)
    }

    fun initializePlayer() {
        if (exoPlayer == null && !isInitializing) {
            isInitializing = true
            val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
            val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            controllerFuture.addListener(
                {
                    exoPlayer = controllerFuture.get()
                    isInitializing = false
                    exoPlayer?.setPlaybackSpeed(_playbackSpeed.value)
                    exoPlayer?.addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlayingState: Boolean) {
                            _isPlaying.value = isPlayingState
                            if (!isPlayingState) {
                                _currentPlayingWordUrl.value = null
                            }
                        }

                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                _isPlaying.value = false
                                _currentPlayingAyahNumber.value = null
                                _currentPlayingWordUrl.value = null
                                onPlaybackEnded?.invoke()
                            }
                        }

                        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                            _isPlaying.value = false
                            _currentPlayingAyahNumber.value = null
                            _currentPlayingWordUrl.value = null
                            if (!com.example.util.NetworkUtils.isNetworkAvailable(context)) {
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    android.widget.Toast.makeText(
                                        context,
                                        "নেটওয়ার্ক ত্রুটি! অডিও প্লে করতে ইন্টারনেট সংযোগ প্রয়োজন।",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    })
                    
                    // Handle pending play request
                    pendingPlayUrl?.let { url ->
                        val ayah = pendingAyahNumber ?: -1
                        pendingPlayUrl = null
                        pendingAyahNumber = null
                        playAudio(url, ayah)
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
        }
    }

    fun playAudio(url: String, ayahNumber: Int) {
        if (exoPlayer == null) {
            pendingPlayUrl = url
            pendingAyahNumber = ayahNumber
            initializePlayer()
            return
        }
        
        if (ayahNumber == -1) {
            _currentPlayingWordUrl.value = url
        } else {
            _currentPlayingWordUrl.value = null
        }
        
        val localFile = getLocalAudioFile(url)
        val isLocal = localFile.exists() && localFile.length() > 0

        if (!isLocal && !com.example.util.NetworkUtils.isNetworkAvailable(context)) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(
                    context,
                    "আপনি অফলাইনে আছেন! অডিও প্লে করতে ইন্টারনেট সংযোগ প্রয়োজন অথবা ডাউনলোড করা থাকতে হবে।",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val uri = if (isLocal) {
            android.net.Uri.fromFile(localFile)
        } else {
            android.net.Uri.parse(url)
        }

        exoPlayer?.apply {
            setMediaItem(MediaItem.fromUri(uri))
            setPlaybackSpeed(_playbackSpeed.value)
            prepare()
            play()
            _currentPlayingAyahNumber.value = ayahNumber
        }

        // Download asynchronously in the background if not already downloaded and network is available
        if (!isLocal && com.example.util.NetworkUtils.isNetworkAvailable(context)) {
            CoroutineScope(Dispatchers.IO).launch {
                downloadUrlToFile(url, localFile)
            }
        }
    }

    fun downloadUrlToFile(url: String, targetFile: File): Boolean {
        if (targetFile.exists() && targetFile.length() > 0) return true
        val tempFile = File(targetFile.parent, targetFile.name + ".temp")
        return try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            connection.instanceFollowRedirects = true
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode in 200..299) {
                connection.inputStream.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists() && tempFile.length() > 0) {
                    tempFile.renameTo(targetFile)
                    true
                } else {
                    if (tempFile.exists()) tempFile.delete()
                    false
                }
            } else {
                if (tempFile.exists()) tempFile.delete()
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            if (tempFile.exists()) tempFile.delete()
            false
        }
    }

    fun pauseAudio() {
        exoPlayer?.pause()
    }

    fun resumeAudio() {
        exoPlayer?.play()
    }

    fun stopAudio() {
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
        _currentPlayingAyahNumber.value = null
        _isPlaying.value = false
    }

    fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }
}
