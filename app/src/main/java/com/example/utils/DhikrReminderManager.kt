package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import com.example.receiver.DhikrReminderReceiver
import java.util.Calendar
import java.util.Locale

enum class DhikrType(val id: String, val title: String, val author: String) {
    DUROOD("durood", "দরূদ রিমাইন্ডার", "দরূদ রিমাইন্ডার"),
    ISTIGHFAR("istighfar", "ইস্তেগফার রিমাইন্ডার", "ইস্তেগফার রিমাইন্ডার")
}

data class DhikrAudioOption(
    val id: String,
    val label: String,
    val arabicText: String,
    val phoneticText: String,
    val translationText: String,
    val speechLocale: Locale
)

data class DhikrReminderConfig(
    val isEnabled: Boolean,
    val intervalMinutes: Int,
    val selectedAudioId: String,
    val isQuietHoursEnabled: Boolean,
    val quietStartHour: Int,
    val quietStartMinute: Int,
    val quietEndHour: Int,
    val quietEndMinute: Int
)

object DhikrReminderManager {
    private const val PREFS_NAME = "dhikr_reminder_preferences"

    // Audio options for Durood
    val duroodAudioOptions = listOf(
        DhikrAudioOption(
            id = "bn",
            label = "বাংলা",
            arabicText = "দরূদ পড়ুন",
            phoneticText = "দরূদ পড়ুন",
            translationText = "দরূদ পড়ুন",
            speechLocale = Locale("bn", "BD")
        ),
        DhikrAudioOption(
            id = "en",
            label = "ইংরেজি",
            arabicText = "Recite Durood",
            phoneticText = "Recite Durood",
            translationText = "Recite Durood",
            speechLocale = Locale.ENGLISH
        ),
        DhikrAudioOption(
            id = "ar_1",
            label = "আরবি - ১",
            arabicText = "صَلِّ عَلَى النَّبِيْ",
            phoneticText = "সাল্লি আলান নাবিয়্য",
            translationText = "নবী (ﷺ)-এর ওপর দরূদ পাঠ করুন।",
            speechLocale = Locale("ar", "SA")
        ),
        DhikrAudioOption(
            id = "ar_2",
            label = "আরবি - ২",
            arabicText = "صَلِّ عَلَى مُحَمَّدْ",
            phoneticText = "সাল্লি আলা মুহাম্মদ",
            translationText = "মুহাম্মদ (ﷺ)-এর ওপর দরূদ পাঠ করুন।",
            speechLocale = Locale("ar", "SA")
        ),
        DhikrAudioOption(
            id = "ar_3",
            label = "আরবি - ৩",
            arabicText = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدْ",
            phoneticText = "আল্লাহুম্মা সাল্লি ওয়া সাল্লিম আলা নাবিয়্যিনা মুহাম্মদ",
            translationText = "হে আল্লাহ! আমাদের নবী মুহাম্মাদের ওপর সালাত ও সালাম বর্ষণ করুন।",
            speechLocale = Locale("ar", "SA")
        )
    )

    // Audio options for Istighfar
    val istighfarAudioOptions = listOf(
        DhikrAudioOption(
            id = "bn",
            label = "বাংলা",
            arabicText = "ইস্তেগফার পড়ুন",
            phoneticText = "ইস্তেগফার পড়ুন",
            translationText = "ইস্তেগফার পড়ুন",
            speechLocale = Locale("bn", "BD")
        ),
        DhikrAudioOption(
            id = "en",
            label = "ইংরেজি",
            arabicText = "Recite Istighfar",
            phoneticText = "Recite Istighfar",
            translationText = "Recite Istighfar",
            speechLocale = Locale.ENGLISH
        ),
        DhikrAudioOption(
            id = "ar_1",
            label = "আরবি - ১",
            arabicText = "أَسْتَغْفِرُ اللَّهْ",
            phoneticText = "আস্তাগফিরুল্লাহ",
            translationText = "আমি আল্লাহর নিকট ক্ষমা প্রার্থনা করছি।",
            speechLocale = Locale("ar", "SA")
        ),
        DhikrAudioOption(
            id = "ar_2",
            label = "আরবি - ২",
            arabicText = "اسْتَغْفِرْ لِذَنْبِكْ",
            phoneticText = "ইস্তাগফির লিযানবিক",
            translationText = "আপনার গুনাহের জন্য ক্ষমা প্রার্থনা করুন।",
            speechLocale = Locale("ar", "SA")
        ),
        DhikrAudioOption(
            id = "ar_3",
            label = "আরবি - ৩",
            arabicText = "اللَّهُمَّ اغْفِرْ لِيْ",
            phoneticText = "আল্লাহুম্মাগফির লী",
            translationText = "হে আল্লাহ! আমাকে ক্ষমা করুন।",
            speechLocale = Locale("ar", "SA")
        )
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getConfig(context: Context, type: DhikrType): DhikrReminderConfig {
        val prefs = getPrefs(context)
        val prefix = type.id
        return DhikrReminderConfig(
            isEnabled = prefs.getBoolean("${prefix}_enabled", false),
            intervalMinutes = prefs.getInt("${prefix}_interval", 15),
            selectedAudioId = prefs.getString("${prefix}_audio", "ar_1") ?: "ar_1",
            isQuietHoursEnabled = prefs.getBoolean("${prefix}_quiet_enabled", true),
            quietStartHour = prefs.getInt("${prefix}_quiet_start_hour", 22), // 10:00 PM
            quietStartMinute = prefs.getInt("${prefix}_quiet_start_min", 0),
            quietEndHour = prefs.getInt("${prefix}_quiet_end_hour", 8),    // 8:00 AM
            quietEndMinute = prefs.getInt("${prefix}_quiet_end_min", 0)
        )
    }

    fun saveConfig(context: Context, type: DhikrType, config: DhikrReminderConfig) {
        val prefs = getPrefs(context)
        val prefix = type.id
        prefs.edit()
            .putBoolean("${prefix}_enabled", config.isEnabled)
            .putInt("${prefix}_interval", config.intervalMinutes)
            .putString("${prefix}_audio", config.selectedAudioId)
            .putBoolean("${prefix}_quiet_enabled", config.isQuietHoursEnabled)
            .putInt("${prefix}_quiet_start_hour", config.quietStartHour)
            .putInt("${prefix}_quiet_start_min", config.quietStartMinute)
            .putInt("${prefix}_quiet_end_hour", config.quietEndHour)
            .putInt("${prefix}_quiet_end_min", config.quietEndMinute)
            .apply()

        if (config.isEnabled) {
            scheduleReminder(context, type, config.intervalMinutes)
        } else {
            cancelReminder(context, type)
        }
    }

    fun scheduleReminder(context: Context, type: DhikrType, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = if (type == DhikrType.DUROOD) 3001 else 3002
        
        val intent = Intent(context, DhikrReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DHIKR_REMINDER_${type.name}"
            putExtra("dhikr_type", type.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + (intervalMinutes * 60 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        }
    }

    fun cancelReminder(context: Context, type: DhikrType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = if (type == DhikrType.DUROOD) 3001 else 3002
        val intent = Intent(context, DhikrReminderReceiver::class.java).apply {
            action = "com.example.ACTION_DHIKR_REMINDER_${type.name}"
            putExtra("dhikr_type", type.name)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun rescheduleAll(context: Context) {
        val duroodConfig = getConfig(context, DhikrType.DUROOD)
        if (duroodConfig.isEnabled) {
            scheduleReminder(context, DhikrType.DUROOD, duroodConfig.intervalMinutes)
        }
        val istighfarConfig = getConfig(context, DhikrType.ISTIGHFAR)
        if (istighfarConfig.isEnabled) {
            scheduleReminder(context, DhikrType.ISTIGHFAR, istighfarConfig.intervalMinutes)
        }
    }

    /**
     * Checks if current time is inside quiet hours
     */
    fun isQuietTime(config: DhikrReminderConfig): Boolean {
        if (!config.isQuietHoursEnabled) return false

        val calendar = Calendar.getInstance()
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMinutes = config.quietStartHour * 60 + config.quietStartMinute
        val endMinutes = config.quietEndHour * 60 + config.quietEndMinute

        return if (startMinutes < endMinutes) {
            // Same day range: e.g. 13:00 to 15:00
            currentMinutes in startMinutes until endMinutes
        } else {
            // Overnight range: e.g. 22:00 (10 PM) to 08:00 (8 AM)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun formatTime12Hour(hour: Int, minute: Int): String {
        val isPm = hour >= 12
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val period = if (isPm) "PM" else "AM"
        val minStr = String.format("%02d", minute)
        return "$h12:$minStr $period"
    }

    // TTS Preview & Playback Manager
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    fun previewAudio(context: Context, option: DhikrAudioOption, onFinish: () -> Unit) {
        try {
            if (tts == null) {
                tts = TextToSpeech(context.applicationContext) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        isTtsInitialized = true
                        speakOption(option, onFinish)
                    } else {
                        playToneFallback(onFinish)
                    }
                }
            } else {
                speakOption(option, onFinish)
            }
        } catch (e: Exception) {
            playToneFallback(onFinish)
        }
    }

    fun speakReminder(context: Context, type: DhikrType, audioId: String) {
        val optionsList = if (type == DhikrType.DUROOD) duroodAudioOptions else istighfarAudioOptions
        val selectedOption = optionsList.find { it.id == audioId } ?: optionsList.first()
        previewAudio(context, selectedOption) {}
    }

    private fun speakOption(option: DhikrAudioOption, onFinish: () -> Unit) {
        try {
            val ttsEngine = tts ?: return
            val result = ttsEngine.setLanguage(option.speechLocale)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                ttsEngine.language = Locale.ENGLISH
            }

            // পুরুষালি মোটা গাম্ভীর্যপূর্ণ কণ্ঠ (Deep masculine tone)
            ttsEngine.setPitch(0.70f)
            // স্পষ্ট ও শান্ত উচ্চারণ গতি (Calm, measured pacing)
            ttsEngine.setSpeechRate(0.82f)

            // Prefer male voice if available in the system TTS voices
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                try {
                    val voices = ttsEngine.voices
                    val maleVoice = voices?.firstOrNull { v ->
                        v.locale.language == option.speechLocale.language &&
                                (v.name.contains("male", ignoreCase = true) ||
                                 v.name.contains("#male", ignoreCase = true) ||
                                 v.name.contains("man", ignoreCase = true) ||
                                 v.name.contains("male-1", ignoreCase = true))
                    } ?: voices?.firstOrNull { v ->
                        v.locale.language == option.speechLocale.language && !v.name.contains("female", ignoreCase = true)
                    }
                    if (maleVoice != null) {
                        ttsEngine.voice = maleVoice
                    }
                } catch (e: Exception) {
                    // Fallback to pitch lowering
                }
            }

            val textToSpeak = when (option.speechLocale.language) {
                "ar" -> option.arabicText // Ends with sukun ْ for accurate Arabic stopping pronunciation
                "bn" -> option.phoneticText // "দরূদ পড়ুন" / "ইস্তেগফার পড়ুন"
                else -> option.translationText // "Recite Durood" / "Recite Istighfar"
            }

            val utteranceId = "dhikr_${System.currentTimeMillis()}"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ttsEngine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                ttsEngine.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null)
            }
        } catch (e: Exception) {
            playToneFallback(onFinish)
        }
    }

    fun stopAudio() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    private fun playToneFallback(onFinish: () -> Unit) {
        try {
            val tone = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            onFinish()
        } catch (e: Exception) {
            onFinish()
        }
    }
}
