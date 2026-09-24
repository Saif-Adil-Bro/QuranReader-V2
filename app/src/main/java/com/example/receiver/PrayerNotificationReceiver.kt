package com.example.receiver

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.PrayerAlarmSoundType
import com.example.data.model.PrayerName
import com.example.utils.PrayerNotificationHelper
import com.example.utils.PrayerSoundManager
import java.time.DayOfWeek
import java.time.LocalDate

class PrayerNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED || action == "com.example.ACTION_REFRESH_PRAYER_ALARMS") {
            PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
            return
        }

        if (action == "com.example.ACTION_STOP_PRAYER_ALARM") {
            val notifId = intent.getIntExtra("notif_id", -1)
            PrayerSoundManager.stopAll()
            if (notifId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notifId)
            }
            return
        }

        if (action == "com.example.ACTION_SNOOZE_PRAYER_ALARM") {
            val prayerNameStr = intent.getStringExtra("prayer_name") ?: return
            val prayerName = try {
                PrayerName.valueOf(prayerNameStr)
            } catch (e: Exception) {
                null
            } ?: return
            val notifId = intent.getIntExtra("notif_id", -1)
            PrayerSoundManager.stopAll()
            if (notifId != -1) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notifId)
            }
            PrayerNotificationHelper.snoozePrayerAlarm(context, prayerName)
            return
        }

        if (action == "com.example.ACTION_PRAYER_NOTIFICATION") {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            val wakeLock = powerManager?.newWakeLock(
                android.os.PowerManager.PARTIAL_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "quran:prayer_notification_wakelock"
            )
            wakeLock?.acquire(45_000L) // 45 seconds wake lock timeout

            val prayerNameStr = intent.getStringExtra("prayer_name") ?: return
            val prayerName = try {
                PrayerName.valueOf(prayerNameStr)
            } catch (e: Exception) {
                null
            } ?: return

            if (!PrayerNotificationHelper.isMasterEnabled(context)) {
                PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
                return
            }

            val config = PrayerNotificationHelper.getPrayerAlarmConfig(context, prayerName)
            if (!config.isEnabled) {
                PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
                return
            }

            val isSnooze = intent.getBooleanExtra("is_snooze", false)
            val offsetMinutes = intent.getIntExtra("offset_minutes", config.offsetMinutes)

            // Deduplication Guard: Do not show notification for the same prayer within 5 minutes unless it is snooze
            val prefs = context.getSharedPreferences("prayer_notification_prefs", Context.MODE_PRIVATE)
            val lastNotifiedKey = "last_notified_${prayerName.name}"
            val lastNotifiedTime = prefs.getLong(lastNotifiedKey, 0L)
            val nowTime = System.currentTimeMillis()

            if (!isSnooze && (nowTime - lastNotifiedTime < 5 * 60 * 1000L)) {
                PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
                return
            }
            prefs.edit().putLong(lastNotifiedKey, nowTime).apply()

            var prayerRangeFormatted = intent.getStringExtra("prayer_range_formatted") ?: ""
            val prayerTimeFormatted = intent.getStringExtra("prayer_time_formatted") ?: ""
            val districtNameBn = intent.getStringExtra("district_name_bn") ?: "ঢাকা"

            val isSoundEnabled = PrayerNotificationHelper.isSoundEnabled(context)
            val isAlarm = isSoundEnabled && config.soundType.isAlarm

            // Trigger alarm audio & vibration or gentle notification sound according to config
            if (isAlarm) {
                PrayerSoundManager.triggerAlarmSoundAndVibrate(
                    context = context,
                    soundType = config.soundType,
                    prayerName = prayerName,
                    enableVibration = config.isVibrationEnabled,
                    customRingtoneUri = config.customRingtoneUri
                )
            } else {
                val notifSoundToPlay = if (isSoundEnabled) config.soundType else PrayerAlarmSoundType.SILENT
                PrayerSoundManager.triggerNotificationSoundAndVibrate(
                    context = context,
                    soundType = notifSoundToPlay,
                    prayerName = prayerName,
                    enableVibration = config.isVibrationEnabled
                )
            }

            val isFriday = LocalDate.now().dayOfWeek == DayOfWeek.FRIDAY
            val isDhuhrOnFriday = isFriday && prayerName == PrayerName.DHUHR

            val title = when (prayerName) {
                PrayerName.FAJR -> if (offsetMinutes < 0) "ফজরের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌅" else "ফজরের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.DHUHR -> if (isDhuhrOnFriday) "পবিত্র জুমুআর ওয়াক্ত হয়েছে 🕌✨" else if (offsetMinutes < 0) "যুহরের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) ☀️" else "যুহরের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.ASR -> if (offsetMinutes < 0) "আসরের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌤️" else "আসরের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.MAGHRIB -> if (offsetMinutes < 0) "মাগরিবের ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌇" else "মাগরিবের ওয়াক্ত শুরু হয়েছে 🕌"
                PrayerName.ISHA -> if (offsetMinutes < 0) "এশার ওয়াক্ত আসন্ন (${-offsetMinutes} মিনিট বাকি) 🌙" else "এশার ওয়াক্ত শুরু হয়েছে 🌙"
                PrayerName.SUNRISE -> "সূর্যোদয় হয়েছে ☀️"
                PrayerName.TAHAJJUD -> "তাহাজ্জুদের বিশেষ সময় হয়েছে 🌌"
                PrayerName.SAHRI -> "সাহরির সময় শেষ হতে যাচ্ছে 🌙"
                PrayerName.IFTAR -> "ইফতারের সময় হয়েছে ✨"
                PrayerName.MAKRUH_SUNRISE -> "মাকরূহ ওয়াক্ত: সূর্যোদয় ⚠️"
                PrayerName.MAKRUH_ZAWAL -> "মাকরূহ ওয়াক্ত: দ্বিপ্রহর (জাওয়াল) ⚠️"
                PrayerName.MAKRUH_SUNSET -> "মাকরূহ ওয়াক্ত: সূর্যাস্ত ⚠️"
            }

            val prayerDisplayTitle = when (prayerName) {
                PrayerName.FAJR -> "ফজর"
                PrayerName.DHUHR -> if (isDhuhrOnFriday) "জুমুআ" else "যুহর"
                PrayerName.ASR -> "আসর"
                PrayerName.MAGHRIB -> "মাগরিব"
                PrayerName.ISHA -> "এশা"
                PrayerName.SUNRISE -> "সূর্যোদয়"
                PrayerName.TAHAJJUD -> "তাহাজ্জুদ"
                PrayerName.SAHRI -> "সাহরি শেষ"
                PrayerName.IFTAR -> "ইফতার"
                PrayerName.MAKRUH_SUNRISE -> "সূর্যোদয় মাকরূহ সময়"
                PrayerName.MAKRUH_ZAWAL -> "দ্বিপ্রহর (জাওয়াল) মাকরূহ সময়"
                PrayerName.MAKRUH_SUNSET -> "সূর্যাস্ত মাকরূহ সময়"
            }

            val message = when (prayerName) {
                PrayerName.SAHRI -> {
                    if (prayerTimeFormatted.isNotBlank()) "সাহরির শেষ সময়: $prayerTimeFormatted ($districtNameBn)। রোজার নিয়ত করে নিন।"
                    else "সাহরির সময় শেষ হয়েছে ($districtNameBn)। রোজার নিয়ত করে নিন।"
                }
                PrayerName.IFTAR -> {
                    if (prayerTimeFormatted.isNotBlank()) "ইফতারের সময়: $prayerTimeFormatted ($districtNameBn)। দুআ পাঠ করে ইফতার করুন: আল্লাহুম্মা লাকা সুমতু..."
                    else "ইফতারের সময় হয়েছে ($districtNameBn)। দুআ পাঠ করে ইফতার করুন।"
                }
                PrayerName.TAHAJJUD -> {
                    "তাহাজ্জুদের বরকতময় সময়। শেষ রাতে রবের দরবারে তাওবা ও দুআ করার উত্তম মুহূর্ত।"
                }
                PrayerName.MAKRUH_SUNRISE -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "সূর্যোদয়কালীন সময়"
                    "সূর্যোদয়ের নিষিদ্ধ (মাকরূহ) ওয়াক্ত: $timeDisplay ($districtNameBn)। সূর্যোদয়কালীন এই সময়ে কোনো সালাত বা সিজদাহ আদায় করা নিষিদ্ধ।"
                }
                PrayerName.MAKRUH_ZAWAL -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "দ্বিপ্রহরের সময়"
                    "দ্বিপ্রহরের নিষিদ্ধ (মাকরূহ) ওয়াক্ত: $timeDisplay ($districtNameBn)। ঠিক দুপুরে সূর্য মাথার ওপর অবস্থানকালে সালাত বা সিজদাহ আদায় করা নিষিদ্ধ।"
                }
                PrayerName.MAKRUH_SUNSET -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) prayerRangeFormatted else if (prayerTimeFormatted.isNotBlank()) prayerTimeFormatted else "সূর্যাস্তের সময়"
                    "সূর্যাস্তের নিষিদ্ধ (মাকরূহ) ওয়াক্ত: $timeDisplay ($districtNameBn)। সূর্যাস্তের পূর্ববর্তী এই সময়ে সালাত বা সিজদাহ আদায় করা নিষিদ্ধ।"
                }
                else -> {
                    val timeDisplay = if (prayerRangeFormatted.isNotBlank()) {
                        prayerRangeFormatted
                    } else if (prayerTimeFormatted.isNotBlank()) {
                        prayerTimeFormatted
                    } else ""

                    if (timeDisplay.isNotBlank()) {
                        "$prayerDisplayTitle সালাতের সময় : $timeDisplay ($districtNameBn)। ওয়াক্তমত সালাত আদায় করার প্রস্তুতি নিন।"
                    } else {
                        "$prayerDisplayTitle সালাতের সময় হয়েছে ($districtNameBn)। ওয়াক্তমত সালাত আদায় করার প্রস্তুতি নিন।"
                    }
                }
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            PrayerNotificationHelper.createNotificationChannel(context)

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", "prayer_times")
                putExtra("target_screen", "prayer_times")
            }

            val notifId = PrayerNotificationHelper.getRequestCodeForPrayer(prayerName)
            val pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Stop Action
            val stopIntent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                setAction("com.example.ACTION_STOP_PRAYER_ALARM")
                putExtra("notif_id", notifId)
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 500,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Snooze Action
            val snoozeIntent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                setAction("com.example.ACTION_SNOOZE_PRAYER_ALARM")
                putExtra("prayer_name", prayerName.name)
                putExtra("notif_id", notifId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 600,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val iconRes = R.mipmap.ic_launcher
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val channelId = if (isAlarm) {
                PrayerNotificationHelper.PRAYER_ALARM_CHANNEL_ID
            } else {
                PrayerNotificationHelper.PRAYER_NOTIFICATION_CHANNEL_ID
            }

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            if (isAlarm) {
                // Full-Screen Alarm Intent for Locked / Unlocked screen
                val fullScreenIntent = Intent(context, com.example.ui.screens.alarm.PrayerAlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    putExtra("prayer_name", prayerName.name)
                    putExtra("title", title)
                    putExtra("message", message)
                    putExtra("notif_id", notifId)
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId + 800,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder.setFullScreenIntent(fullScreenPendingIntent, true)
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "বন্ধ করুন", stopPendingIntent)
                builder.addAction(android.R.drawable.ic_lock_idle_alarm, "১০ মিনিট পর (স্নুজ)", snoozePendingIntent)

                if (config.isVibrationEnabled) {
                    builder.setVibrate(PrayerNotificationHelper.ALARM_VIBRATION_PATTERN)
                } else {
                    builder.setVibrate(longArrayOf(0))
                }
            } else {
                // Gentle standard notification
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.setCategory(NotificationCompat.CATEGORY_EVENT)
                builder.addAction(android.R.drawable.ic_menu_view, "সময়সূচি দেখুন", pendingIntent)

                if (config.isVibrationEnabled) {
                    builder.setVibrate(PrayerNotificationHelper.VIBRATION_PATTERN)
                } else {
                    builder.setVibrate(longArrayOf(0))
                }

                if (isSoundEnabled && config.soundType == PrayerAlarmSoundType.NOTIFICATION) {
                    builder.setSound(defaultSoundUri)
                } else {
                    builder.setSound(null)
                }
            }

            notificationManager.notify(notifId, builder.build())

            // On locked screen or waking up, directly launch the full screen alarm activity for alarms
            if (isAlarm) {
                try {
                    val activityIntent = Intent(context, com.example.ui.screens.alarm.PrayerAlarmActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        putExtra("prayer_name", prayerName.name)
                        putExtra("title", title)
                        putExtra("message", message)
                        putExtra("notif_id", notifId)
                    }
                    context.startActivity(activityIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Reschedule subsequent prayer alarms
            PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        }
    }
}
