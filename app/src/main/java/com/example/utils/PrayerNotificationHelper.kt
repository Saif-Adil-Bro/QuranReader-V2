package com.example.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import com.example.data.model.DistrictInfo
import com.example.data.model.PrayerAlarmSoundType
import com.example.data.model.PrayerName
import com.example.data.model.SinglePrayerTime
import com.example.data.model.WaqtAlarmConfig
import com.example.data.repository.PrayerTimesRepository
import com.example.receiver.PrayerNotificationReceiver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object PrayerNotificationHelper {

    private const val PREFS_NAME = "prayer_notification_prefs"
    const val PRAYER_NOTIFICATION_CHANNEL_ID = "prayer_times_notification_channel_v3"
    const val KEY_MASTER_ENABLED = "prayer_notif_master_enabled"
    const val KEY_NOTIF_SOUND = "prayer_notif_sound"

    // Legacy keys for backward compatibility
    const val KEY_NOTIF_FAJR = "prayer_notif_fajr"
    const val KEY_NOTIF_DHUHR = "prayer_notif_dhuhr"
    const val KEY_NOTIF_ASR = "prayer_notif_asr"
    const val KEY_NOTIF_MAGHRIB = "prayer_notif_maghrib"
    const val KEY_NOTIF_ISHA = "prayer_notif_isha"
    const val KEY_NOTIF_SAHRI = "prayer_notif_sahri"
    const val KEY_NOTIF_IFTAR = "prayer_notif_iftar"
    const val KEY_NOTIF_SUNRISE = "prayer_notif_sunrise"
    const val KEY_NOTIF_TAHAJJUD = "prayer_notif_tahajjud"

    val VIBRATION_PATTERN = longArrayOf(0, 500, 250, 500)

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            try {
                notificationManager.deleteNotificationChannel("prayer_times_notification_channel")
                notificationManager.deleteNotificationChannel("prayer_times_notification_channel_v2")
            } catch (_: Exception) {}

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channel = NotificationChannel(
                PRAYER_NOTIFICATION_CHANNEL_ID,
                "ওয়াক্ত ও সালাত অ্যালার্ম",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "প্রতিটি ওয়াক্তের সালাত, সাহরি, ইফতার ও তাহাজ্জুদ অ্যালার্ম এবং স্মরণ"
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                setSound(defaultSoundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isMasterEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_MASTER_ENABLED, true)
    }

    fun setMasterEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_MASTER_ENABLED, enabled).apply()
        if (enabled) {
            scheduleNextPrayerAlarms(context)
        } else {
            cancelAllPrayerAlarms(context)
        }
    }

    fun getPrayerAlarmConfig(context: Context, prayerName: PrayerName): WaqtAlarmConfig {
        val prefs = getPrefs(context)
        val defaultEnabled = when (prayerName) {
            PrayerName.FAJR, PrayerName.DHUHR, PrayerName.ASR, PrayerName.MAGHRIB, PrayerName.ISHA, PrayerName.SAHRI, PrayerName.IFTAR -> true
            PrayerName.SUNRISE, PrayerName.TAHAJJUD -> false
        }

        // Check modern key or legacy key
        val legacyKey = getLegacyKey(prayerName)
        val isEnabled = if (prefs.contains("config_enabled_${prayerName.name}")) {
            prefs.getBoolean("config_enabled_${prayerName.name}", defaultEnabled)
        } else if (legacyKey != null) {
            prefs.getBoolean(legacyKey, defaultEnabled)
        } else {
            defaultEnabled
        }

        val offsetMinutes = prefs.getInt("config_offset_${prayerName.name}", 0)
        val soundTypeId = prefs.getString("config_sound_${prayerName.name}", PrayerAlarmSoundType.NOTIFICATION.id) ?: PrayerAlarmSoundType.NOTIFICATION.id
        val soundType = PrayerAlarmSoundType.values().find { it.id == soundTypeId } ?: PrayerAlarmSoundType.NOTIFICATION
        val isVibration = prefs.getBoolean("config_vibrate_${prayerName.name}", true)

        return WaqtAlarmConfig(
            prayerName = prayerName,
            isEnabled = isEnabled,
            offsetMinutes = offsetMinutes,
            soundType = soundType,
            isVibrationEnabled = isVibration
        )
    }

    fun savePrayerAlarmConfig(context: Context, config: WaqtAlarmConfig) {
        val prefs = getPrefs(context)
        val editor = prefs.edit()
        editor.putBoolean("config_enabled_${config.prayerName.name}", config.isEnabled)
        editor.putInt("config_offset_${config.prayerName.name}", config.offsetMinutes)
        editor.putString("config_sound_${config.prayerName.name}", config.soundType.id)
        editor.putBoolean("config_vibrate_${config.prayerName.name}", config.isVibrationEnabled)

        // Sync legacy key
        val legacyKey = getLegacyKey(config.prayerName)
        if (legacyKey != null) {
            editor.putBoolean(legacyKey, config.isEnabled)
        }
        editor.apply()

        // Reschedule alarms immediately with new configuration
        scheduleNextPrayerAlarms(context)
    }

    private fun getLegacyKey(prayerName: PrayerName): String? {
        return when (prayerName) {
            PrayerName.FAJR -> KEY_NOTIF_FAJR
            PrayerName.DHUHR -> KEY_NOTIF_DHUHR
            PrayerName.ASR -> KEY_NOTIF_ASR
            PrayerName.MAGHRIB -> KEY_NOTIF_MAGHRIB
            PrayerName.ISHA -> KEY_NOTIF_ISHA
            PrayerName.SAHRI -> KEY_NOTIF_SAHRI
            PrayerName.IFTAR -> KEY_NOTIF_IFTAR
            PrayerName.SUNRISE -> KEY_NOTIF_SUNRISE
            PrayerName.TAHAJJUD -> KEY_NOTIF_TAHAJJUD
        }
    }

    fun isPrayerEnabled(context: Context, prayerName: PrayerName): Boolean {
        return getPrayerAlarmConfig(context, prayerName).isEnabled
    }

    fun setPrayerEnabled(context: Context, prayerName: PrayerName, enabled: Boolean) {
        val currentConfig = getPrayerAlarmConfig(context, prayerName)
        savePrayerAlarmConfig(context, currentConfig.copy(isEnabled = enabled))
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_SOUND, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_SOUND, enabled).apply()
    }

    fun getRequestCodeForPrayer(prayerName: PrayerName): Int {
        return when (prayerName) {
            PrayerName.FAJR -> 3001
            PrayerName.DHUHR -> 3002
            PrayerName.ASR -> 3003
            PrayerName.MAGHRIB -> 3004
            PrayerName.ISHA -> 3005
            PrayerName.SUNRISE -> 3006
            PrayerName.SAHRI -> 3007
            PrayerName.IFTAR -> 3008
            PrayerName.TAHAJJUD -> 3009
        }
    }

    /**
     * Schedules the next exact alarms for all enabled prayers with offsets.
     */
    fun scheduleNextPrayerAlarms(context: Context) {
        if (!isMasterEnabled(context)) {
            cancelAllPrayerAlarms(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val prayerRepo = PrayerTimesRepository.getInstance(context)
        val district = prayerRepo.selectedDistrict.value
        val isHanafi = prayerRepo.isHanafi.value

        val settingsRepo = com.example.data.repository.SettingsRepository.getInstance(context)
        val sahriOffset = try {
            runBlocking { settingsRepo.sahriOffsetFlow.first() }
        } catch (e: Exception) {
            -3
        }
        val iftarOffset = try {
            runBlocking { settingsRepo.iftarOffsetFlow.first() }
        } catch (e: Exception) {
            0
        }

        val zoneId = try {
            ZoneId.of(district.timeZoneId)
        } catch (e: Exception) {
            ZoneId.of("Asia/Dhaka")
        }

        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)
        val dayAfterTomorrow = today.plusDays(2)

        val scheduleToday = PrayerTimesCalculator.calculatePrayerSchedule(
            date = today,
            district = district,
            isHanafi = isHanafi,
            sahriOffsetMinutes = sahriOffset,
            iftarOffsetMinutes = iftarOffset
        )
        val scheduleTomorrow = PrayerTimesCalculator.calculatePrayerSchedule(
            date = tomorrow,
            district = district,
            isHanafi = isHanafi,
            sahriOffsetMinutes = sahriOffset,
            iftarOffsetMinutes = iftarOffset
        )
        val scheduleDayAfter = PrayerTimesCalculator.calculatePrayerSchedule(
            date = dayAfterTomorrow,
            district = district,
            isHanafi = isHanafi,
            sahriOffsetMinutes = sahriOffset,
            iftarOffsetMinutes = iftarOffset
        )

        val nowMillis = System.currentTimeMillis()

        val allWaqtItems = listOf(
            PrayerName.FAJR,
            PrayerName.SUNRISE,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA,
            PrayerName.SAHRI,
            PrayerName.IFTAR,
            PrayerName.TAHAJJUD
        )

        for (prayerName in allWaqtItems) {
            val config = getPrayerAlarmConfig(context, prayerName)
            if (!config.isEnabled) {
                cancelSingleAlarm(context, prayerName)
                continue
            }

            val offsetMillis = config.offsetMinutes * 60 * 1000L

            // Get standard prayer time objects
            val todayPrayer = getSinglePrayerTime(scheduleToday, today, zoneId, prayerName, sahriOffset, iftarOffset)
            val tomorrowPrayer = getSinglePrayerTime(scheduleTomorrow, tomorrow, zoneId, prayerName, sahriOffset, iftarOffset)
            val dayAfterPrayer = getSinglePrayerTime(scheduleDayAfter, dayAfterTomorrow, zoneId, prayerName, sahriOffset, iftarOffset)

            val targetPrayer: SinglePrayerTime? = when {
                todayPrayer != null && (todayPrayer.timestampMillis + offsetMillis) > nowMillis + 5000L -> todayPrayer
                tomorrowPrayer != null && (tomorrowPrayer.timestampMillis + offsetMillis) > nowMillis + 5000L -> tomorrowPrayer
                dayAfterPrayer != null && (dayAfterPrayer.timestampMillis + offsetMillis) > nowMillis + 5000L -> dayAfterPrayer
                else -> null
            }

            if (targetPrayer != null) {
                val triggerMillis = targetPrayer.timestampMillis + offsetMillis
                if (triggerMillis > nowMillis + 3000L) {
                    scheduleAlarmForPrayer(context, alarmManager, targetPrayer, district, config, triggerMillis)
                }
            }
        }

        // Schedule midnight schedule refresh alarm
        scheduleDailyMidnightRefresher(context, alarmManager, zoneId)
    }

    private fun getSinglePrayerTime(
        schedule: com.example.data.model.DailyPrayerSchedule,
        date: LocalDate,
        zoneId: ZoneId,
        prayerName: PrayerName,
        sahriOffset: Int,
        iftarOffset: Int
    ): SinglePrayerTime? {
        val standard = schedule.prayers.find { it.name == prayerName }
        if (standard != null) return standard

        // Special handling for SAHRI, IFTAR, TAHAJJUD if not in standard list
        return when (prayerName) {
            PrayerName.SAHRI -> {
                val fajr = schedule.prayers.find { it.name == PrayerName.FAJR } ?: return null
                val sahriMillis = fajr.timestampMillis + (sahriOffset * 60 * 1000L)
                SinglePrayerTime(
                    name = PrayerName.SAHRI,
                    timeDigits = schedule.sahriTimeDigits,
                    amPm = "AM",
                    timeFormatted = schedule.sahriEndTimeFormatted,
                    timestampMillis = sahriMillis,
                    endTimeDigits = "",
                    endTimeFormatted = "",
                    timeRangeFormatted = "সাহরির শেষ সময়: ${schedule.sahriEndTimeFormatted}"
                )
            }
            PrayerName.IFTAR -> {
                val maghrib = schedule.prayers.find { it.name == PrayerName.MAGHRIB } ?: return null
                val iftarMillis = maghrib.timestampMillis + (iftarOffset * 60 * 1000L)
                SinglePrayerTime(
                    name = PrayerName.IFTAR,
                    timeDigits = schedule.iftarTimeDigits,
                    amPm = "PM",
                    timeFormatted = schedule.iftarTimeFormatted,
                    timestampMillis = iftarMillis,
                    endTimeDigits = "",
                    endTimeFormatted = "",
                    timeRangeFormatted = "ইফতারের সময়: ${schedule.iftarTimeFormatted}"
                )
            }
            PrayerName.TAHAJJUD -> {
                val fajr = schedule.prayers.find { it.name == PrayerName.FAJR } ?: return null
                // Tahajjud optimal time: ~1 hour before Fajr
                val tahajjudMillis = fajr.timestampMillis - (60 * 60 * 1000L)
                SinglePrayerTime(
                    name = PrayerName.TAHAJJUD,
                    timeDigits = "",
                    amPm = "AM",
                    timeFormatted = schedule.tahajjudEndTimeFormatted,
                    timestampMillis = tahajjudMillis,
                    endTimeDigits = "",
                    endTimeFormatted = "",
                    timeRangeFormatted = "তাহাজ্জুদ: ${schedule.tahajjudRange}"
                )
            }
            else -> null
        }
    }

    private fun scheduleAlarmForPrayer(
        context: Context,
        alarmManager: AlarmManager,
        prayer: SinglePrayerTime,
        district: DistrictInfo,
        config: WaqtAlarmConfig,
        triggerMillis: Long
    ) {
        val requestCode = getRequestCodeForPrayer(prayer.name)
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_NOTIFICATION"
            putExtra("prayer_name", prayer.name.name)
            putExtra("prayer_time_digits", prayer.timeDigits)
            putExtra("prayer_am_pm", prayer.amPm)
            putExtra("prayer_time_formatted", prayer.timeFormatted)
            putExtra("prayer_end_formatted", prayer.endTimeFormatted)
            putExtra("prayer_range_formatted", prayer.timeRangeFormatted)
            putExtra("district_name_bn", district.nameBn)
            putExtra("offset_minutes", config.offsetMinutes)
            putExtra("sound_type", config.soundType.id)
            putExtra("vibration_enabled", config.isVibrationEnabled)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scheduleDailyMidnightRefresher(context: Context, alarmManager: AlarmManager, zoneId: ZoneId) {
        val midnightIntent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_REFRESH_PRAYER_ALARMS"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            3000,
            midnightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val tomorrowMidnight = LocalDate.now(zoneId).plusDays(1).atStartOfDay().plusMinutes(5)
        val triggerMillis = tomorrowMidnight.atZone(zoneId).toInstant().toEpochMilli()

        if (triggerMillis <= System.currentTimeMillis()) {
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun cancelSingleAlarm(context: Context, prayerName: PrayerName) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCodeForPrayer(prayerName)
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelAllPrayerAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allPrayers = listOf(
            PrayerName.FAJR,
            PrayerName.SUNRISE,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA,
            PrayerName.SAHRI,
            PrayerName.IFTAR,
            PrayerName.TAHAJJUD
        )

        for (p in allPrayers) {
            val requestCode = getRequestCodeForPrayer(p)
            val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                action = "com.example.ACTION_PRAYER_NOTIFICATION"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        // Cancel midnight refresh alarm as well
        val refreshIntent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_REFRESH_PRAYER_ALARMS"
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            3000,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(refreshPendingIntent)
    }

    /**
     * Snooze an alarm by 10 minutes
     */
    fun snoozePrayerAlarm(context: Context, prayerName: PrayerName) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCode = getRequestCodeForPrayer(prayerName) + 1000
        val snoozeMillis = System.currentTimeMillis() + (10 * 60 * 1000L)

        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "com.example.ACTION_PRAYER_NOTIFICATION"
            putExtra("prayer_name", prayerName.name)
            putExtra("is_snooze", true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, snoozeMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }
}
