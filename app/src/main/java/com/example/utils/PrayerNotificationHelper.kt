package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.example.data.model.DistrictInfo
import com.example.data.model.PrayerName
import com.example.data.model.SinglePrayerTime
import com.example.data.repository.PrayerTimesRepository
import com.example.receiver.PrayerNotificationReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object PrayerNotificationHelper {

    private const val PREFS_NAME = "prayer_notification_prefs"
    const val KEY_MASTER_ENABLED = "prayer_notif_master_enabled"
    const val KEY_NOTIF_FAJR = "prayer_notif_fajr"
    const val KEY_NOTIF_DHUHR = "prayer_notif_dhuhr"
    const val KEY_NOTIF_ASR = "prayer_notif_asr"
    const val KEY_NOTIF_MAGHRIB = "prayer_notif_maghrib"
    const val KEY_NOTIF_ISHA = "prayer_notif_isha"
    const val KEY_NOTIF_SAHRI = "prayer_notif_sahri"
    const val KEY_NOTIF_IFTAR = "prayer_notif_iftar"
    const val KEY_NOTIF_SOUND = "prayer_notif_sound"

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

    fun isPrayerEnabled(context: Context, prayerName: PrayerName): Boolean {
        val prefs = getPrefs(context)
        return when (prayerName) {
            PrayerName.FAJR -> prefs.getBoolean(KEY_NOTIF_FAJR, true)
            PrayerName.DHUHR -> prefs.getBoolean(KEY_NOTIF_DHUHR, true)
            PrayerName.ASR -> prefs.getBoolean(KEY_NOTIF_ASR, true)
            PrayerName.MAGHRIB -> prefs.getBoolean(KEY_NOTIF_MAGHRIB, true)
            PrayerName.ISHA -> prefs.getBoolean(KEY_NOTIF_ISHA, true)
            PrayerName.SAHRI -> prefs.getBoolean(KEY_NOTIF_SAHRI, true)
            PrayerName.IFTAR -> prefs.getBoolean(KEY_NOTIF_IFTAR, true)
            PrayerName.SUNRISE -> false
        }
    }

    fun setPrayerEnabled(context: Context, prayerName: PrayerName, enabled: Boolean) {
        val key = when (prayerName) {
            PrayerName.FAJR -> KEY_NOTIF_FAJR
            PrayerName.DHUHR -> KEY_NOTIF_DHUHR
            PrayerName.ASR -> KEY_NOTIF_ASR
            PrayerName.MAGHRIB -> KEY_NOTIF_MAGHRIB
            PrayerName.ISHA -> KEY_NOTIF_ISHA
            PrayerName.SAHRI -> KEY_NOTIF_SAHRI
            PrayerName.IFTAR -> KEY_NOTIF_IFTAR
            PrayerName.SUNRISE -> return
        }
        getPrefs(context).edit().putBoolean(key, enabled).apply()
        scheduleNextPrayerAlarms(context)
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
        }
    }

    /**
     * Schedules the next exact alarms for all enabled prayers.
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

        val zoneId = try {
            ZoneId.of(district.timeZoneId)
        } catch (e: Exception) {
            ZoneId.of("Asia/Dhaka")
        }

        val today = LocalDate.now(zoneId)
        val tomorrow = today.plusDays(1)

        val scheduleToday = PrayerTimesCalculator.calculatePrayerSchedule(today, district, isHanafi)
        val scheduleTomorrow = PrayerTimesCalculator.calculatePrayerSchedule(tomorrow, district, isHanafi)

        val nowMillis = System.currentTimeMillis()

        val fardPrayers = listOf(
            PrayerName.FAJR,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA
        )

        for (prayerName in fardPrayers) {
            if (!isPrayerEnabled(context, prayerName)) {
                cancelSingleAlarm(context, prayerName)
                continue
            }

            val todayPrayer = scheduleToday.prayers.find { it.name == prayerName }
            val tomorrowPrayer = scheduleTomorrow.prayers.find { it.name == prayerName }

            // Find the closest upcoming trigger timestamp that is strictly in the future
            val targetPrayer: SinglePrayerTime? = when {
                todayPrayer != null && todayPrayer.timestampMillis > nowMillis + 5000L -> todayPrayer
                tomorrowPrayer != null && tomorrowPrayer.timestampMillis > nowMillis + 5000L -> tomorrowPrayer
                else -> {
                    val dayAfterTomorrow = today.plusDays(2)
                    val scheduleDayAfter = PrayerTimesCalculator.calculatePrayerSchedule(dayAfterTomorrow, district, isHanafi)
                    scheduleDayAfter.prayers.find { it.name == prayerName }
                }
            }

            if (targetPrayer != null && targetPrayer.timestampMillis > nowMillis + 3000L) {
                scheduleAlarmForPrayer(context, alarmManager, targetPrayer, district)
            }
        }

        // Schedule midnight schedule refresh alarm
        scheduleDailyMidnightRefresher(context, alarmManager, zoneId)
    }

    private fun scheduleAlarmForPrayer(
        context: Context,
        alarmManager: AlarmManager,
        prayer: SinglePrayerTime,
        district: DistrictInfo
    ) {
        val nowMillis = System.currentTimeMillis()
        if (prayer.timestampMillis <= nowMillis + 3000L) {
            return
        }

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
                    prayer.timestampMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    prayer.timestampMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // In case exact alarm permission is restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                prayer.timestampMillis,
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
        val fardPrayers = listOf(
            PrayerName.FAJR,
            PrayerName.DHUHR,
            PrayerName.ASR,
            PrayerName.MAGHRIB,
            PrayerName.ISHA,
            PrayerName.SUNRISE
        )

        for (p in fardPrayers) {
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
}
