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

            // Trigger alarm audio & vibration according to config
            val isSoundEnabled = PrayerNotificationHelper.isSoundEnabled(context)
            val soundTypeToPlay = if (isSoundEnabled) config.soundType else PrayerAlarmSoundType.SILENT
            PrayerSoundManager.triggerAlarmSoundAndVibrate(
                context = context,
                soundType = soundTypeToPlay,
                prayerName = prayerName,
                enableVibration = config.isVibrationEnabled
            )

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

            val builder = NotificationCompat.Builder(context, PrayerNotificationHelper.PRAYER_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(iconRes)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "বন্ধ করুন", stopPendingIntent)
                .addAction(android.R.drawable.ic_lock_idle_alarm, "১০ মিনিট পর", snoozePendingIntent)

            if (config.isVibrationEnabled) {
                builder.setVibrate(PrayerNotificationHelper.VIBRATION_PATTERN)
            } else {
                builder.setVibrate(longArrayOf(0))
            }

            if (isSoundEnabled && config.soundType != PrayerAlarmSoundType.SILENT) {
                builder.setSound(defaultSoundUri)
                builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            } else {
                builder.setSound(null)
                builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            }

            notificationManager.notify(notifId, builder.build())

            // Reschedule subsequent prayer alarms
            PrayerNotificationHelper.scheduleNextPrayerAlarms(context)
        }
    }
}
