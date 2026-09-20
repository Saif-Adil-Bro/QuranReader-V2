package com.example.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import com.example.data.model.PrayerAlarmSoundType
import com.example.data.model.PrayerName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object PrayerSoundManager {

    private var activeAudioTrack: AudioTrack? = null
    private var activeMediaPlayer: MediaPlayer? = null
    private var activeTts: TextToSpeech? = null
    private var playbackJob: Job? = null
    private var vibrator: Vibrator? = null

    var currentlyPlayingType: PrayerAlarmSoundType? = null
        private set

    fun isPlaying(): Boolean = playbackJob?.isActive == true || activeMediaPlayer?.isPlaying == true || activeAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING

    fun stopAll() {
        try {
            playbackJob?.cancel()
            playbackJob = null
        } catch (_: Exception) {}

        try {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
            activeAudioTrack = null
        } catch (_: Exception) {}

        try {
            if (activeMediaPlayer?.isPlaying == true) {
                activeMediaPlayer?.stop()
            }
            activeMediaPlayer?.release()
            activeMediaPlayer = null
        } catch (_: Exception) {}

        try {
            activeTts?.stop()
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
        } catch (_: Exception) {}

        currentlyPlayingType = null
    }

    /**
     * Preview sound for UI demonstration
     */
    fun playPreview(
        context: Context,
        soundType: PrayerAlarmSoundType,
        prayerName: PrayerName,
        onCompletion: () -> Unit = {}
    ) {
        stopAll()
        currentlyPlayingType = soundType

        playbackJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                when (soundType) {
                    PrayerAlarmSoundType.SILENT -> {
                        // Silent - no sound
                        delay(400)
                        currentlyPlayingType = null
                        onCompletion()
                    }
                    PrayerAlarmSoundType.BEEP -> {
                        playSynthesizedBeep()
                        delay(1200)
                        currentlyPlayingType = null
                        onCompletion()
                    }
                    PrayerAlarmSoundType.RING -> {
                        playSynthesizedMelody()
                        delay(2500)
                        currentlyPlayingType = null
                        onCompletion()
                    }
                    PrayerAlarmSoundType.VOICE_NAME -> {
                        val textToSpeak = getVoiceAnnouncementText(prayerName)
                        speakText(context, textToSpeak) {
                            currentlyPlayingType = null
                            onCompletion()
                        }
                    }
                    PrayerAlarmSoundType.NOTIFICATION -> {
                        playSystemNotificationSound(context)
                        delay(1500)
                        currentlyPlayingType = null
                        onCompletion()
                    }
                    PrayerAlarmSoundType.AZAN_MECCA -> {
                        playSynthesizedAzanMecca()
                        delay(4000)
                        currentlyPlayingType = null
                        onCompletion()
                    }
                    PrayerAlarmSoundType.AZAN_MADINA -> {
                        playSynthesizedAzanMadina()
                        delay(4000)
                        currentlyPlayingType = null
                        onCompletion()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                currentlyPlayingType = null
                onCompletion()
            }
        }
    }

    /**
     * Play when an alarm triggers in background / receiver
     */
    fun triggerAlarmSoundAndVibrate(
        context: Context,
        soundType: PrayerAlarmSoundType,
        prayerName: PrayerName,
        enableVibration: Boolean
    ) {
        stopAll()

        if (enableVibration) {
            triggerVibration(context)
        }

        when (soundType) {
            PrayerAlarmSoundType.SILENT -> {
                // Do not play audio
            }
            PrayerAlarmSoundType.BEEP -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repeat(3) {
                        playSynthesizedBeep()
                        delay(800)
                    }
                }
            }
            PrayerAlarmSoundType.RING -> {
                CoroutineScope(Dispatchers.IO).launch {
                    repeat(2) {
                        playSynthesizedMelody()
                        delay(600)
                    }
                }
            }
            PrayerAlarmSoundType.VOICE_NAME -> {
                val announcement = getVoiceAnnouncementText(prayerName)
                speakText(context, announcement)
            }
            PrayerAlarmSoundType.NOTIFICATION -> {
                playSystemNotificationSound(context)
            }
            PrayerAlarmSoundType.AZAN_MECCA -> {
                CoroutineScope(Dispatchers.IO).launch {
                    playSynthesizedAzanMecca()
                }
            }
            PrayerAlarmSoundType.AZAN_MADINA -> {
                CoroutineScope(Dispatchers.IO).launch {
                    playSynthesizedAzanMadina()
                }
            }
        }
    }

    fun triggerVibration(context: Context) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 600, 300, 600, 300, 800)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getVoiceAnnouncementText(prayerName: PrayerName): String {
        return when (prayerName) {
            PrayerName.FAJR -> "ফজরের নামাজের ওয়াক্ত হয়েছে, আস-সালাতু খাইরুম মিনান নাওম"
            PrayerName.DHUHR -> "যুহরের নামাজের ওয়াক্ত হয়েছে, জামাতের প্রস্তুতি নিন"
            PrayerName.ASR -> "আসরের নামাজের ওয়াক্ত হয়েছে, সালাত আদায়ের প্রস্তুতি নিন"
            PrayerName.MAGHRIB -> "মাগরিবের নামাজের ওয়াক্ত হয়েছে, সালাতের প্রস্তুতি নিন"
            PrayerName.ISHA -> "এশার নামাজের ওয়াক্ত হয়েছে"
            PrayerName.SUNRISE -> "সূর্যোদয় হয়েছে, ইশরাকের নামাজের সময় আসন্ন"
            PrayerName.TAHAJJUD -> "তাহাজ্জুদের বিশেষ ফজিলতপূর্ণ সময় হয়েছে"
            PrayerName.SAHRI -> "সাহরির সময় শেষ হয়েছে, রোজার নিয়ত করে নিন"
            PrayerName.IFTAR -> "ইফতারের সময় হয়েছে, বিসমিল্লাহ বলে ইফতার করুন"
        }
    }

    private fun speakText(context: Context, text: String, onFinished: () -> Unit = {}) {
        try {
            if (activeTts != null) {
                activeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PRAYER_ANNOUNCEMENT_ID")
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3000)
                    onFinished()
                }
                return
            }

            activeTts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    try {
                        val bnLocale = Locale("bn", "BD")
                        val result = activeTts?.setLanguage(bnLocale)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            activeTts?.language = Locale.ENGLISH
                        }
                        activeTts?.setSpeechRate(0.9f)
                        activeTts?.setPitch(1.0f)
                        activeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PRAYER_ANNOUNCEMENT_ID")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                CoroutineScope(Dispatchers.Main).launch {
                    delay(3500)
                    onFinished()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onFinished()
        }
    }

    private fun playSystemNotificationSound(context: Context) {
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            activeMediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                prepare()
                start()
            }
        } catch (e: Exception) {
            playSynthesizedBeep()
        }
    }

    private fun playSynthesizedBeep() {
        val sampleRate = 44100
        val durationSec = 0.9
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        val notes = listOf(880.0, 1174.66, 1318.51) // A5, D6, E6
        val subDuration = durationSec / notes.size

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIndex = (t / subDuration).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIndex]
            val subT = t - (noteIndex * subDuration)
            val envelope = exp(-subT * 7.0) // quick decay
            val sample = (sin(2.0 * PI * freq * t) * envelope * 0.8 * Short.MAX_VALUE).toInt().toShort()
            buffer[i] = sample
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playSynthesizedMelody() {
        val sampleRate = 44100
        val durationSec = 2.2
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        // Calming pentatonic melody: C5(523Hz), D5(587Hz), E5(659Hz), G5(783Hz), A5(880Hz), C6(1046Hz)
        val notes = listOf(523.25, 659.25, 783.99, 880.0, 1046.50)
        val noteDur = durationSec / notes.size

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val subT = t - (noteIdx * noteDur)
            val envelope = (1.0 - exp(-subT * 20.0)) * exp(-subT * 3.5) // soft bell attack & decay
            // Fundamental + harmonic
            val raw = sin(2.0 * PI * freq * t) + 0.4 * sin(4.0 * PI * freq * t) + 0.2 * sin(6.0 * PI * freq * t)
            buffer[i] = (raw * envelope * 0.5 * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playSynthesizedAzanMecca() {
        val sampleRate = 44100
        val durationSec = 3.6
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        // Melodic Maqam Bayati motif representation for Allahu Akbar (G4, Bb4, C5, D5, C5, Bb4, A4, G4)
        val notes = listOf(392.0, 466.16, 523.25, 587.33, 523.25, 466.16, 440.0, 392.0)
        val noteDur = durationSec / notes.size

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val subT = t - (noteIdx * noteDur)
            val envelope = (1.0 - exp(-subT * 12.0)) * exp(-subT * 2.0)
            val vibrato = sin(2.0 * PI * 5.0 * t) * 4.0 // subtle vocal vibrato
            val raw = sin(2.0 * PI * (freq + vibrato) * t) + 0.35 * sin(4.0 * PI * freq * t)
            buffer[i] = (raw * envelope * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playSynthesizedAzanMadina() {
        val sampleRate = 44100
        val durationSec = 3.6
        val numSamples = (sampleRate * durationSec).toInt()
        val buffer = ShortArray(numSamples)

        // Melodic Maqam Hijaz motif representation (D4, Eb4, F#4, G4, A4, G4, F#4, Eb4, D4)
        val notes = listOf(293.66, 311.13, 369.99, 392.00, 440.00, 392.00, 369.99, 311.13, 293.66)
        val noteDur = durationSec / notes.size

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.size - 1)
            val freq = notes[noteIdx]
            val subT = t - (noteIdx * noteDur)
            val envelope = (1.0 - exp(-subT * 10.0)) * exp(-subT * 1.8)
            val vibrato = sin(2.0 * PI * 4.5 * t) * 3.5
            val raw = sin(2.0 * PI * (freq + vibrato) * t) + 0.4 * sin(4.0 * PI * freq * t) + 0.15 * sin(6.0 * PI * freq * t)
            buffer[i] = (raw * envelope * 0.6 * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        try {
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(buffer.size * 2)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            activeAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            activeAudioTrack?.write(buffer, 0, buffer.size)
            activeAudioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
