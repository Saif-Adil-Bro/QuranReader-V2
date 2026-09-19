package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Surface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.BackgroundOverlay
import com.example.data.model.PreparedAyahAudio
import com.example.data.model.QuranVideoConfig
import com.example.data.model.TextAnimationStyle
import com.example.data.model.VideoAspectRatio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.ByteBuffer

object QuranVideoExporter {

    data class ExportResult(
        val isSuccess: Boolean,
        val videoUri: Uri? = null,
        val videoFile: File? = null,
        val errorMessage: String? = null
    )

    private data class QueuedVideoPacket(
        val buffer: ByteBuffer,
        val offset: Int,
        val size: Int,
        val presentationTimeUs: Long,
        val flags: Int
    )

    /**
     * Renders a 720p H.264 MP4 video using native MediaCodec & MediaMuxer and merges sequential ayah audio.
     */
    suspend fun exportQuranVideo(
        context: Context,
        config: QuranVideoConfig,
        audioFileUrls: List<String>,
        preparedAudios: List<PreparedAyahAudio> = emptyList(),
        onProgress: (Float) -> Unit
    ): ExportResult = withContext(Dispatchers.IO) {
        val width = config.aspectRatio.width
        val height = config.aspectRatio.height
        val bitRate = 3_000_000 // 3 Mbps for crisp 720p
        val frameRate = 30
        val iFrameInterval = 1 // 1 sec keyframe

        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var inputSurface: Surface? = null
        val outputVideoFile = File(context.cacheDir, "quran_video_${System.currentTimeMillis()}.mp4")
        val downloadedAudioFiles = mutableListOf<File>()
        val aacAudioFiles = mutableListOf<File>()

        try {
            onProgress(0.05f)

            // Step 1: Collect sequential audio files for selected ayahs
            var totalDurationUs = 0L
            val audioDurations = mutableListOf<Long>()

            if (preparedAudios.isNotEmpty() && preparedAudios.all { it.localFile.exists() }) {
                // Reuse already downloaded and verified audio files from Step 2
                for (prep in preparedAudios) {
                    downloadedAudioFiles.add(prep.localFile)
                    val durUs = (prep.durationMs * 1000L).coerceAtLeast(1_000_000L)
                    audioDurations.add(durUs)
                    totalDurationUs += durUs
                }
            } else {
                // Fallback to downloading
                for (i in audioFileUrls.indices) {
                    val urlString = audioFileUrls[i]
                    val localAudio = File(context.cacheDir, "ayah_audio_${System.currentTimeMillis()}_$i.mp3")
                    var durationUs = 3_500_000L // 3.5s default

                    try {
                        val url = URL(urlString)
                        val conn = url.openConnection()
                        conn.connectTimeout = 10000
                        conn.readTimeout = 15000
                        conn.getInputStream().use { input ->
                            FileOutputStream(localAudio).use { output ->
                                input.copyTo(output)
                            }
                        }

                        if (localAudio.exists() && localAudio.length() > 500) {
                            downloadedAudioFiles.add(localAudio)

                            // Get accurate duration using MediaExtractor
                            var durationFound = false
                            val extractor = MediaExtractor()
                            try {
                                extractor.setDataSource(localAudio.absolutePath)
                                for (t in 0 until extractor.trackCount) {
                                    val format = extractor.getTrackFormat(t)
                                    val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                                    if (mime.startsWith("audio/")) {
                                        if (format.containsKey(MediaFormat.KEY_DURATION)) {
                                            val durUs = format.getLong(MediaFormat.KEY_DURATION)
                                            if (durUs > 300_000L) {
                                                durationUs = durUs
                                                durationFound = true
                                            }
                                        }
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                // ignore
                            } finally {
                                try { extractor.release() } catch (e: Exception) {}
                            }

                            if (!durationFound) {
                                val mmr = MediaMetadataRetriever()
                                try {
                                    mmr.setDataSource(localAudio.absolutePath)
                                    val durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                    val durMs = durStr?.toLongOrNull()
                                    if (durMs != null && durMs > 500) {
                                        durationUs = durMs * 1000L
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("QuranVideoExporter", "Metadata error on audio $i", e)
                                } finally {
                                    try { mmr.release() } catch (e: Exception) {}
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("QuranVideoExporter", "Audio download error $urlString", e)
                    }

                    audioDurations.add(durationUs)
                    totalDurationUs += durationUs
                }
            }

            if (totalDurationUs <= 0L) {
                totalDurationUs = 4_000_000L
            }

            onProgress(0.15f)

            // Step 2: Transcode MP3 files to AAC for standard MP4 muxing
            for (i in downloadedAudioFiles.indices) {
                val srcFile = downloadedAudioFiles[i]
                val aacFile = File(context.cacheDir, "ayah_aac_${System.currentTimeMillis()}_$i.mp4")
                val success = transcodeMp3ToAac(srcFile, aacFile)
                if (success && aacFile.exists() && aacFile.length() > 0) {
                    aacAudioFiles.add(aacFile)
                }
                onProgress(0.15f + (0.15f * ((i + 1).toFloat() / downloadedAudioFiles.size.coerceAtLeast(1))))
            }

            // Extract accurate duration directly from transcoded AAC files
            if (aacAudioFiles.isNotEmpty()) {
                val accurateAacDurations = mutableListOf<Long>()
                for (aacFile in aacAudioFiles) {
                    var durUs = 0L
                    val extractor = MediaExtractor()
                    try {
                        extractor.setDataSource(aacFile.absolutePath)
                        for (t in 0 until extractor.trackCount) {
                            val fmt = extractor.getTrackFormat(t)
                            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                            if (mime.startsWith("audio/") && fmt.containsKey(MediaFormat.KEY_DURATION)) {
                                durUs = fmt.getLong(MediaFormat.KEY_DURATION)
                                break
                            }
                        }
                    } catch (e: Exception) {
                    } finally {
                        try { extractor.release() } catch (e: Exception) {}
                    }
                    accurateAacDurations.add(if (durUs > 300_000L) durUs else 3_500_000L)
                }
                if (accurateAacDurations.size == audioDurations.size) {
                    audioDurations.clear()
                    audioDurations.addAll(accurateAacDurations)
                    totalDurationUs = audioDurations.sum()
                }
            }

            onProgress(0.30f)

            // Prepare Audio Extractor format if AAC audio is ready
            var audioFormat: MediaFormat? = null
            if (aacAudioFiles.isNotEmpty()) {
                val extractor = MediaExtractor()
                try {
                    extractor.setDataSource(aacAudioFiles[0].absolutePath)
                    for (t in 0 until extractor.trackCount) {
                        val fmt = extractor.getTrackFormat(t)
                        val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                        if (mime.startsWith("audio/")) {
                            audioFormat = fmt
                            break
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("QuranVideoExporter", "Audio format extract failed", e)
                } finally {
                    try { extractor.release() } catch (e: Exception) {}
                }
            }

            // Prepare Video Encoder
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
                setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            }

            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            encoder.start()

            // Prepare Muxer
            muxer = MediaMuxer(outputVideoFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var videoTrackIndex = -1
            var audioTrackIndex = -1
            var muxerStarted = false
            var videoFormatReady: MediaFormat? = null
            val queuedVideoPackets = mutableListOf<QueuedVideoPacket>()

            // Decode Background if any
            val bgBitmap = config.backgroundPresetName?.let { preset ->
                try {
                    val resId = context.resources.getIdentifier(preset, "drawable", context.packageName)
                    if (resId != 0) BitmapFactory.decodeResource(context.resources, resId) else null
                } catch (e: Exception) { null }
            } ?: config.customImageUri?.let { uriStr ->
                try {
                    val uri = Uri.parse(uriStr)
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                } catch (e: Exception) { null }
            }

            val totalFrames = ((totalDurationUs / 1_000_000.0) * frameRate).toInt().coerceAtLeast(frameRate * 2)
            val bufferInfo = MediaCodec.BufferInfo()

            val arabicFontRes = when (config.arabicFontName) {
                "Me Quran", "মি কুরআন" -> R.font.me_quran
                "PDMS Saleem", "মলভি এ-এম", "সেলিম", "Saleem" -> R.font.pdms_saleem
                "Noorehira", "নুরে হুদা", "নূরে হেরা" -> R.font.noorehira
                "Uthman Taha", "উছমান তাহা নাসখ" -> R.font.uthman_taha
                "Amiri Quran", "আমিরি কুরআন" -> R.font.amiri_quran
                "Amiri", "আমিরি" -> R.font.amiri_regular
                "Lateef", "লতীফ" -> R.font.lateef_regular
                "Almarai", "আল মারাই" -> R.font.almarai_regular
                "Tajawal", "তাজাওয়াল" -> R.font.tajawal_regular
                "Scheherazade New", "শাহরাজাদ" -> R.font.scheherazade_new
                else -> R.font.scheherazade_new
            }

            val arabicTypeface = try {
                ResourcesCompat.getFont(context, arabicFontRes) ?: Typeface.DEFAULT_BOLD
            } catch (e: Exception) { Typeface.DEFAULT_BOLD }

            val banglaFontRes = when (config.bengaliFontName) {
                "SolaimanLipi", "সলাইমান লিপি", "সোলাইমান লিপি" -> R.font.solaimanlipi
                "Hind Siliguri", "হিন্দ শিলিগুড়ি" -> R.font.hind_siliguri
                "Shorif Shishir", "Shorif Shishir Unicode", "শরীফ শিশির" -> R.font.shorif_shishir
                else -> R.font.solaimanlipi
            }

            val banglaTypeface = try {
                ResourcesCompat.getFont(context, banglaFontRes) ?: Typeface.DEFAULT
            } catch (e: Exception) { Typeface.DEFAULT }

            // Helper to start muxer safely once video format is ready
            fun checkAndStartMuxer(newFormat: MediaFormat) {
                videoFormatReady = newFormat
                videoTrackIndex = muxer!!.addTrack(newFormat)
                if (audioFormat != null) {
                    audioTrackIndex = muxer!!.addTrack(audioFormat)
                }
                muxer!!.start()
                muxerStarted = true

                // Drain queued video packets
                for (pkt in queuedVideoPackets) {
                    val info = MediaCodec.BufferInfo().apply {
                        set(pkt.offset, pkt.size, pkt.presentationTimeUs, pkt.flags)
                    }
                    muxer!!.writeSampleData(videoTrackIndex, pkt.buffer, info)
                }
                queuedVideoPackets.clear()
            }

            // Step 3: Render Frames to InputSurface
            for (frame in 0 until totalFrames) {
                val presentationTimeUs = (frame * 1_000_000L) / frameRate

                // Ayah synchronization with natural speech lead
                // Qari audio recordings start with ~400-500ms of room silence/breath before vocalization.
                // We align the active ayah timing with the recitation speech onset.
                val syncOffsetUs = 350_000L // 350ms speech onset sync
                val adjustedTimeUs = (presentationTimeUs - syncOffsetUs).coerceAtLeast(0L)

                var runningUs = 0L
                var activeAyahIndex = 0
                for (aIdx in audioDurations.indices) {
                    val dur = audioDurations[aIdx]
                    if (adjustedTimeUs >= runningUs && adjustedTimeUs < (runningUs + dur)) {
                        activeAyahIndex = aIdx
                        break
                    }
                    runningUs += dur
                    activeAyahIndex = aIdx
                }

                // Animation alpha calculation
                val ayahStartTimeUs = if (activeAyahIndex == 0) 0L else audioDurations.take(activeAyahIndex).sum()
                val ayahDurationUs = audioDurations.getOrElse(activeAyahIndex) { 3_500_000L }
                val timeInAyahSec = ((adjustedTimeUs - ayahStartTimeUs).coerceAtLeast(0L)) / 1_000_000f

                val animAlpha = when (config.animationStyle) {
                    TextAnimationStyle.FADE_IN -> (timeInAyahSec / 0.4f).coerceIn(0.15f, 1.0f)
                    TextAnimationStyle.SLIDE_UP -> (timeInAyahSec / 0.35f).coerceIn(0.2f, 1.0f)
                    TextAnimationStyle.SLIDE_DOWN -> (timeInAyahSec / 0.35f).coerceIn(0.2f, 1.0f)
                    TextAnimationStyle.FADE_OUT -> {
                        val remainingSec = (ayahDurationUs - (adjustedTimeUs - ayahStartTimeUs)) / 1_000_000f
                        (remainingSec / 0.4f).coerceIn(0.15f, 1.0f)
                    }
                    TextAnimationStyle.NONE -> 1.0f
                }

                val ayahProgress = if (ayahDurationUs > 0) {
                    ((adjustedTimeUs - ayahStartTimeUs).toFloat() / ayahDurationUs).coerceIn(0.0f, 1.0f)
                } else 0f

                // Lock Canvas on Surface (Software lockCanvas is fully reliable and avoids RenderThread / HIDL DEAD_OBJECT crashes on MIUI/HyperOS)
                val canvas = try {
                    inputSurface?.lockCanvas(null)
                } catch (e: Exception) {
                    android.util.Log.e("QuranVideoExporter", "Failed to lock surface canvas", e)
                    null
                }

                if (canvas != null) {
                    try {
                        drawVideoFrame(
                            canvas = canvas,
                            width = width,
                            height = height,
                            config = config,
                            activeAyahIndex = activeAyahIndex,
                            bgBitmap = bgBitmap,
                            arabicTypeface = arabicTypeface,
                            banglaTypeface = banglaTypeface,
                            animAlpha = animAlpha,
                            ayahProgress = ayahProgress
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("QuranVideoExporter", "Error rendering frame $frame", e)
                    } finally {
                        try {
                            inputSurface?.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            android.util.Log.e("QuranVideoExporter", "Failed to unlock canvas and post", e)
                        }
                    }
                }

                // Drain Encoder output
                while (true) {
                    val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 0)
                    if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        break
                    } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        if (!muxerStarted) {
                            checkAndStartMuxer(encoder.outputFormat)
                        }
                    } else if (outputIndex >= 0) {
                        val encodedData = encoder.getOutputBuffer(outputIndex)
                        if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                            if (muxerStarted && bufferInfo.size > 0) {
                                bufferInfo.presentationTimeUs = presentationTimeUs
                                muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                            } else if (!muxerStarted && bufferInfo.size > 0) {
                                val directBuffer = ByteBuffer.allocateDirect(bufferInfo.size)
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                directBuffer.put(encodedData)
                                directBuffer.flip()
                                queuedVideoPackets.add(
                                    QueuedVideoPacket(
                                        buffer = directBuffer,
                                        offset = 0,
                                        size = bufferInfo.size,
                                        presentationTimeUs = presentationTimeUs,
                                        flags = bufferInfo.flags
                                    )
                                )
                            }
                        }
                        encoder.releaseOutputBuffer(outputIndex, false)
                    }
                }

                val renderProgress = 0.30f + (0.50f * (frame.toFloat() / totalFrames))
                onProgress(renderProgress)
            }

            // Signal End of Video Stream
            encoder.signalEndOfInputStream()

            // Drain remaining frames from encoder
            var isEos = false
            var eosAttempts = 0
            while (!isEos && eosAttempts < 50) {
                val outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    eosAttempts++
                } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        checkAndStartMuxer(encoder.outputFormat)
                    }
                } else if (outputIndex >= 0) {
                    if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        isEos = true
                    }
                    val encodedData = encoder.getOutputBuffer(outputIndex)
                    if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bufferInfo.size > 0) {
                        bufferInfo.presentationTimeUs = totalDurationUs
                        if (muxerStarted) {
                            muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        }
                    }
                    encoder.releaseOutputBuffer(outputIndex, false)
                }
            }

            onProgress(0.80f)

            // Step 4: Write Sequential Audio Samples into Muxer
            if (audioTrackIndex >= 0 && aacAudioFiles.isNotEmpty() && muxerStarted) {
                var audioTimeOffsetUs = 0L
                val audioBuffer = ByteBuffer.allocateDirect(1024 * 64)
                val audioBufferInfo = MediaCodec.BufferInfo()

                for (i in aacAudioFiles.indices) {
                    val aacFile = aacAudioFiles[i]
                    val extractor = MediaExtractor()
                    try {
                        extractor.setDataSource(aacFile.absolutePath)
                        var trackIdx = -1
                        for (t in 0 until extractor.trackCount) {
                            val fmt = extractor.getTrackFormat(t)
                            val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                            if (mime.startsWith("audio/")) {
                                trackIdx = t
                                break
                            }
                        }

                        if (trackIdx >= 0) {
                            extractor.selectTrack(trackIdx)
                            var firstSampleTimeUs = -1L
                            var lastSampleTimeUs = 0L

                            while (true) {
                                audioBuffer.clear()
                                val sampleSize = extractor.readSampleData(audioBuffer, 0)
                                if (sampleSize < 0) break

                                val sampleTimeUs = extractor.sampleTime
                                if (firstSampleTimeUs < 0) {
                                    firstSampleTimeUs = sampleTimeUs
                                }
                                val normalizedSampleTimeUs = (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L)
                                val flags = extractor.sampleFlags

                                audioBufferInfo.offset = 0
                                audioBufferInfo.size = sampleSize
                                audioBufferInfo.presentationTimeUs = audioTimeOffsetUs + normalizedSampleTimeUs
                                audioBufferInfo.flags = flags

                                muxer.writeSampleData(audioTrackIndex, audioBuffer, audioBufferInfo)
                                lastSampleTimeUs = normalizedSampleTimeUs
                                extractor.advance()
                            }

                            val thisAyahDurationUs = audioDurations.getOrElse(i) { 3_500_000L }
                            val effectiveAyahDuration = if (lastSampleTimeUs > 0) lastSampleTimeUs else thisAyahDurationUs
                            audioTimeOffsetUs += effectiveAyahDuration
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("QuranVideoExporter", "Audio write error for ayah $i", e)
                    } finally {
                        try { extractor.release() } catch (e: Exception) {}
                    }
                }
            }

            onProgress(0.90f)

            muxer.stop()
            muxer.release()
            muxer = null

            encoder.stop()
            encoder.release()
            encoder = null

            onProgress(0.95f)

            // Step 5: Save final Video to Gallery
            val savedUri = saveVideoToGallery(context, outputVideoFile, "Quran_${config.surahName}_Ayah_${config.ayahStart}.mp4")

            onProgress(1.0f)
            return@withContext ExportResult(
                isSuccess = true,
                videoUri = savedUri,
                videoFile = outputVideoFile
            )

        } catch (e: Exception) {
            android.util.Log.e("QuranVideoExporter", "Export failed", e)
            return@withContext ExportResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "ভিডিও রেন্ডার করতে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।"
            )
        } finally {
            try { inputSurface?.release() } catch (e: Exception) {}
            try { encoder?.release() } catch (e: Exception) {}
            try { muxer?.release() } catch (e: Exception) {}
            downloadedAudioFiles.forEach { it.delete() }
            aacAudioFiles.forEach { it.delete() }
        }
    }

    /**
     * Decodes MP3 input stream and encodes to AAC format using native Android MediaCodec.
     */
    private fun transcodeMp3ToAac(inputFile: File, outputFile: File): Boolean {
        var extractor: MediaExtractor? = null
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrack = -1
            var inputFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    audioTrack = i
                    inputFormat = format
                    break
                }
            }

            if (audioTrack < 0 || inputFormat == null) {
                return false
            }

            extractor.selectTrack(audioTrack)
            val sampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else 44100
            val channelCount = if (inputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                inputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else 2

            // Decoder for MP3
            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_MPEG
            decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            // Encoder for AAC
            val outputFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
            }
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            var muxerStarted = false

            val decBufferInfo = MediaCodec.BufferInfo()
            val encBufferInfo = MediaCodec.BufferInfo()

            var decInputEos = false
            var decOutputEos = false
            var encOutputEos = false
            var maxLoopIterations = 0
            val maxAllowedLoops = 100_000
            var firstExtractorTimeUs = -1L

            while (!encOutputEos && maxLoopIterations < maxAllowedLoops) {
                maxLoopIterations++

                // 1. Feed Extractor to Decoder
                if (!decInputEos) {
                    val inputBufIndex = decoder.dequeueInputBuffer(2000)
                    if (inputBufIndex >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIndex)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                decInputEos = true
                            } else {
                                val timeUs = extractor.sampleTime
                                if (firstExtractorTimeUs < 0) {
                                    firstExtractorTimeUs = timeUs
                                }
                                val normalizedPts = (timeUs - firstExtractorTimeUs).coerceAtLeast(0L)
                                decoder.queueInputBuffer(inputBufIndex, 0, sampleSize, normalizedPts, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2. Drain Decoder -> Feed Encoder
                if (!decOutputEos) {
                    val decOutIndex = decoder.dequeueOutputBuffer(decBufferInfo, 2000)
                    if (decOutIndex >= 0) {
                        val pcmBuffer = decoder.getOutputBuffer(decOutIndex)
                        val isEos = (decBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        if (pcmBuffer != null && decBufferInfo.size > 0) {
                            var encInIndex = encoder.dequeueInputBuffer(2000)
                            var retry = 0
                            while (encInIndex < 0 && retry < 5) {
                                // Try draining encoder to free buffer
                                val encOut = encoder.dequeueOutputBuffer(encBufferInfo, 1000)
                                if (encOut == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                                    if (!muxerStarted) {
                                        muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                                        muxer.start()
                                        muxerStarted = true
                                    }
                                } else if (encOut >= 0) {
                                    val aacBuf = encoder.getOutputBuffer(encOut)
                                    if (aacBuf != null && muxerStarted && encBufferInfo.size > 0 && (encBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                                        muxer.writeSampleData(muxerTrackIndex, aacBuf, encBufferInfo)
                                    }
                                    if ((encBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                                        encOutputEos = true
                                    }
                                    encoder.releaseOutputBuffer(encOut, false)
                                }
                                encInIndex = encoder.dequeueInputBuffer(2000)
                                retry++
                            }

                            if (encInIndex >= 0) {
                                val encInBuf = encoder.getInputBuffer(encInIndex)
                                if (encInBuf != null) {
                                    encInBuf.clear()
                                    pcmBuffer.position(decBufferInfo.offset)
                                    pcmBuffer.limit(decBufferInfo.offset + decBufferInfo.size)
                                    encInBuf.put(pcmBuffer)

                                    val flags = if (isEos) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                    encoder.queueInputBuffer(encInIndex, 0, decBufferInfo.size, decBufferInfo.presentationTimeUs, flags)
                                }
                            }
                        } else if (isEos) {
                            val encInIndex = encoder.dequeueInputBuffer(2000)
                            if (encInIndex >= 0) {
                                encoder.queueInputBuffer(encInIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            }
                        }

                        if (isEos) {
                            decOutputEos = true
                        }
                        decoder.releaseOutputBuffer(decOutIndex, false)
                    }
                }

                // 3. Drain Encoder -> Write to Muxer
                val encOutIndex = encoder.dequeueOutputBuffer(encBufferInfo, 2000)
                if (encOutIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (!muxerStarted) {
                        muxerTrackIndex = muxer.addTrack(encoder.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    }
                } else if (encOutIndex >= 0) {
                    val aacBuf = encoder.getOutputBuffer(encOutIndex)
                    if (aacBuf != null && (encBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (muxerStarted && encBufferInfo.size > 0) {
                            muxer.writeSampleData(muxerTrackIndex, aacBuf, encBufferInfo)
                        }
                    }
                    if ((encBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encOutputEos = true
                    }
                    encoder.releaseOutputBuffer(encOutIndex, false)
                }
            }

            return muxerStarted
        } catch (e: Exception) {
            android.util.Log.e("QuranVideoExporter", "Transcode error", e)
            return false
        } finally {
            try { extractor?.release() } catch (e: Exception) {}
            try { decoder?.stop(); decoder?.release() } catch (e: Exception) {}
            try { encoder?.stop(); encoder?.release() } catch (e: Exception) {}
            try { muxer?.stop(); muxer?.release() } catch (e: Exception) {}
        }
    }

    private fun drawVideoFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        config: QuranVideoConfig,
        activeAyahIndex: Int,
        bgBitmap: Bitmap?,
        arabicTypeface: Typeface,
        banglaTypeface: Typeface,
        animAlpha: Float,
        ayahProgress: Float = 0f
    ) {
        val t = config.template

        // 1. Draw Background
        if (bgBitmap != null) {
            val srcRect = Rect(0, 0, bgBitmap.width, bgBitmap.height)
            val dstRect = Rect(0, 0, width, height)
            canvas.drawBitmap(bgBitmap, srcRect, dstRect, null)
        } else {
            val gradientColors = t.gradientColors.map { c ->
                android.graphics.Color.argb(
                    (c.alpha * 255).toInt(),
                    (c.red * 255).toInt(),
                    (c.green * 255).toInt(),
                    (c.blue * 255).toInt()
                )
            }.toIntArray()

            val shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                gradientColors,
                null,
                Shader.TileMode.CLAMP
            )
            val bgPaint = Paint().apply {
                this.shader = shader
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        }

        // 2. Draw Overlay
        val overlayAlpha = when (config.overlay) {
            BackgroundOverlay.NONE -> 0.0f
            BackgroundOverlay.LIGHT -> 0.25f
            BackgroundOverlay.MEDIUM -> 0.50f
            BackgroundOverlay.DARK -> 0.75f
        }
        if (overlayAlpha > 0f) {
            val overlayPaint = Paint().apply {
                color = android.graphics.Color.argb((overlayAlpha * 255).toInt(), 0, 0, 0)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        }

        // 3. Draw Decorative Frame / Border
        val margin = 36f
        val framePaint = Paint().apply {
            color = android.graphics.Color.argb(
                120,
                (t.accentColor.red * 255).toInt(),
                (t.accentColor.green * 255).toInt(),
                (t.accentColor.blue * 255).toInt()
            )
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        canvas.drawRoundRect(margin, margin, width - margin, height - margin, 24f, 24f, framePaint)

        var currentY = margin + 50f

        // 4. Draw Logo / Channel Name
        if (config.showLogo && config.logoText.isNotBlank()) {
            val logoPaint = Paint().apply {
                color = android.graphics.Color.argb(
                    230,
                    (t.accentColor.red * 255).toInt(),
                    (t.accentColor.green * 255).toInt(),
                    (t.accentColor.blue * 255).toInt()
                )
                textSize = 34f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(config.logoText, width / 2f, currentY, logoPaint)
            currentY += 40f
        }

        // 5. Draw Reference Badge (সূরা ও আয়াত)
        if (config.showReference) {
            val activeAyah = config.selectedAyahs.getOrNull(activeAyahIndex)
            val refText = if (activeAyah != null && activeAyah.surahNumber == 1 && activeAyah.numberInSurah == 1 && config.surahNumber != 1) {
                "সূরা ${config.surahName} • বিসমিল্লাহ"
            } else {
                val currentNumberInSurah = activeAyah?.numberInSurah ?: (config.ayahStart + activeAyahIndex)
                val bnNumerals = currentNumberInSurah.toString().map { ch ->
                    when (ch) {
                        '0' -> '০'
                        '1' -> '১'
                        '2' -> '২'
                        '3' -> '৩'
                        '4' -> '৪'
                        '5' -> '৫'
                        '6' -> '৬'
                        '7' -> '৭'
                        '8' -> '৮'
                        '9' -> '৯'
                        else -> ch
                    }
                }.joinToString("")
                "সূরা ${config.surahName} • আয়াত $bnNumerals"
            }
            val refPaint = Paint().apply {
                color = android.graphics.Color.argb(
                    240,
                    (t.referenceColor.red * 255).toInt(),
                    (t.referenceColor.green * 255).toInt(),
                    (t.referenceColor.blue * 255).toInt()
                )
                textSize = 30f
                typeface = banglaTypeface
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(refText, width / 2f, currentY + 20f, refPaint)
            currentY += 60f
        }

        // 6. Draw Content Box (Arabic Quran + Translation)
        val contentPadding = 64f
        val textWidth = (width - 2 * contentPadding).toInt()

        val activeAyah = config.selectedAyahs.getOrNull(activeAyahIndex)
        val rawArabic = activeAyah?.arabicText ?: "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
        val arabicText = QuranIndoPakNormalizer.processIndoPakText(
            rawText = rawArabic,
            showWaqfSigns = config.showWaqfSigns
        )
        val banglaText = activeAyah?.bengaliText ?: ""

        val arabicPaint = TextPaint().apply {
            color = android.graphics.Color.argb(
                (animAlpha * 255).toInt(),
                (t.arabicColor.red * 255).toInt(),
                (t.arabicColor.green * 255).toInt(),
                (t.arabicColor.blue * 255).toInt()
            )
            textSize = config.arabicFontSize * 1.8f
            typeface = arabicTypeface
            isAntiAlias = true
        }

        // Proportional line spacing multiplier for Arabic fonts in StaticLayout.
        // Arabic TrueType fonts have large internal ascent/descent (2.5x - 3.2x of font size).
        // Multiplying by (config.arabicLineSpacing * 0.45f) scales line spacing cleanly and compactly,
        // preventing massive empty vertical gaps while keeping harakat clear and legible.
        val arabicSpacingMult = (config.arabicLineSpacing * 0.45f).coerceIn(0.50f, 1.15f)

        val arabicLayout = StaticLayout.Builder.obtain(arabicText, 0, arabicText.length, arabicPaint, textWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, arabicSpacingMult)
            .setIncludePad(false)
            .build()

        val banglaPaint = TextPaint().apply {
            color = android.graphics.Color.argb(
                (animAlpha * 235).toInt(),
                (t.translationColor.red * 255).toInt(),
                (t.translationColor.green * 255).toInt(),
                (t.translationColor.blue * 255).toInt()
            )
            textSize = config.translationFontSize * 1.8f
            typeface = banglaTypeface
            isAntiAlias = true
        }

        val banglaSpacingMult = (config.translationLineSpacing * 0.70f).coerceIn(0.65f, 1.25f)

        val banglaLayout = if (config.showBanglaTranslation && banglaText.isNotBlank()) {
            StaticLayout.Builder.obtain(banglaText, 0, banglaText.length, banglaPaint, textWidth)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0f, banglaSpacingMult)
                .setIncludePad(false)
                .build()
        } else null

        val spacingBetween = if (banglaLayout != null) 36f else 0f
        val totalContentHeight = arabicLayout.height + (banglaLayout?.height ?: 0) + spacingBetween

        // Safe Available Viewport Bounds
        val footerMargin = if (config.showCredit) margin + 50f else margin + 20f
        val topLimit = currentY + 16f
        val bottomLimit = height - footerMargin
        val availableHeight = bottomLimit - topLimit

        // Calculate smooth vertical scroll offset if content exceeds viewport
        val startDrawY = if (totalContentHeight <= availableHeight) {
            // Fits nicely: Center vertically in viewport
            topLimit + ((availableHeight - totalContentHeight) / 2f)
        } else {
            // Large Ayah: Ken-Burns Smooth Vertical Pan (Scrolling from top to bottom)
            val overflow = totalContentHeight - availableHeight + 40f
            val scrollOffset = overflow * ayahProgress
            topLimit - scrollOffset
        }

        // Clip viewport to keep text strictly within border bounds during scroll
        canvas.save()
        canvas.clipRect(contentPadding - 16f, topLimit, width - contentPadding + 16f, bottomLimit)

        // Draw Arabic
        canvas.save()
        canvas.translate(contentPadding, startDrawY)
        arabicLayout.draw(canvas)
        canvas.restore()

        // Draw Bangla Translation
        if (banglaLayout != null) {
            canvas.save()
            canvas.translate(contentPadding, startDrawY + arabicLayout.height + spacingBetween)
            banglaLayout.draw(canvas)
            canvas.restore()
        }

        canvas.restore() // Restore Clip

        // 7. Draw Footer / Credit
        if (config.showCredit) {
            val footerY = height - margin - 24f
            val creditPaint = Paint().apply {
                color = android.graphics.Color.argb(
                    180,
                    (t.translationColor.red * 255).toInt(),
                    (t.translationColor.green * 255).toInt(),
                    (t.translationColor.blue * 255).toInt()
                )
                textSize = 22f
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText(config.creditText, width / 2f, footerY, creditPaint)
        }
    }

    private fun saveVideoToGallery(context: Context, videoFile: File, title: String): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, title)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/QuranReader")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null

        try {
            resolver.openOutputStream(uri)?.use { out ->
                videoFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            return uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            return null
        }
    }
}
