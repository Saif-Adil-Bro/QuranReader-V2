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
            val districtNameEn = intent.getStringExtra("district_name_en") ?: ""
            val isEnglish = com.example.utils.NotificationLocalization.isEnglish(context)

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
            val title = com.example.utils.NotificationLocalization.getPrayerNotificationTitle(
                prayerName = prayerName,
                isFriday = isFriday,
                offsetMinutes = offsetMinutes,
                isEnglish = isEnglish
            )

            val message = com.example.utils.NotificationLocalization.getPrayerNotificationMessage(
                prayerName = prayerName,
                isFriday = isFriday,
                prayerTimeFormatted = prayerTimeFormatted,
                prayerRangeFormatted = prayerRangeFormatted,
                districtNameBn = districtNameBn,
                districtNameEn = districtNameEn,
                isEnglish = isEnglish
            )

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
                builder.addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    com.example.utils.NotificationLocalization.getActionDismissLabel(isEnglish),
                    stopPendingIntent
                )
                builder.addAction(
                    android.R.drawable.ic_lock_idle_alarm,
                    com.example.utils.NotificationLocalization.getActionSnoozeLabel(isEnglish),
                    snoozePendingIntent
                )

                if (config.isVibrationEnabled) {
                    builder.setVibrate(PrayerNotificationHelper.ALARM_VIBRATION_PATTERN)
                } else {
                    builder.setVibrate(longArrayOf(0))
                }
            } else {
                // Gentle standard notification
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.setCategory(NotificationCompat.CATEGORY_EVENT)
                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    com.example.utils.NotificationLocalization.getActionViewScheduleLabel(isEnglish),
                    pendingIntent
                )

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
