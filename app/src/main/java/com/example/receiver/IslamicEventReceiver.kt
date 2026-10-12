package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.utils.IslamicEventGuidanceHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

class IslamicEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            val enabled = sharedPrefs.getBoolean("islamic_events_reminder_enabled", true)
            if (enabled) {
                scheduleNextAlarm(context)
            }
            return
        }

        val sharedPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean("islamic_events_reminder_enabled", true)
        if (!enabled) {
            return
        }

        val hijriOffset = sharedPrefs.getInt("hijri_offset", -1)

        // 1. Check and notify for 30th Hijri day Date Adjustment (Morning at or after 6:00 AM)
        try {
            com.example.utils.HijriNewMonthNotificationHelper.checkAndNotify30th(context, hijriOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Check and notify for New Hijri Month transition (1st day morning at or after 6:00 AM)
        try {
            com.example.utils.HijriNewMonthNotificationHelper.checkAndNotifyNewMonth(context, hijriOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Check and notify for 29th Hijri day Moon Sighting notice (Afternoon at or after 4:00 PM / 16:00)
        try {
            com.example.utils.MoonSightingNotificationHelper.checkAndNotify29th(context, hijriOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Check and notify for Friday Jumu'ah reminder
        try {
            com.example.utils.JumuahReminderHelper.checkAndNotifyFriday(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Islamic Event / Sunnah Fast reminder for upcoming days
        val isEnglish = com.example.utils.NotificationLocalization.isEnglish(context)
        val today = LocalDate.now()
        val eventInfo = IslamicEventGuidanceHelper.checkNotificationForDate(today, hijriOffset, isEnglish)

        if (eventInfo != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "islamic_events_reminder"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    if (isEnglish) "Islamic Events & Sunnah Fasts" else "ইসলামিক দিবস ও রোজা রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = if (isEnglish) "Reminders for Monday/Thursday Sunnah fasts, Ayyam al-Bidh, and Islamic events" else "সোম-বৃহস্পতিবারের সুন্নাত রোজা, আইয়ামে বীজ ও ইসলামিক দিবসের নোটিফিকেশন"
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Insert into local database so it shows on NotificationScreen & Home badge
            val timestamp = System.currentTimeMillis()
            try {
                val db = com.example.data.local.NotificationDatabase.getDatabase(context)
                val entity = com.example.data.local.entity.LocalNotificationEntity(
                    title = eventInfo.title,
                    content = eventInfo.fullGuidanceContent,
                    category = eventInfo.category,
                    author = eventInfo.author,
                    timestamp = timestamp
                )
                GlobalScope.launch(Dispatchers.IO) {
                    db.localNotificationDao().insertNotification(entity)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Intent to open directly to the detailed Sharia guidance screen
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigate_to", "notifications")
                putExtra("target_screen", "notifications")
                putExtra("open_blog_post_detail", true)
                putExtra("blog_post_id", eventInfo.id)
                putExtra("blog_post_title", eventInfo.title)
                putExtra("blog_post_content", eventInfo.fullGuidanceContent)
                putExtra("blog_post_category", eventInfo.category)
                putExtra("blog_post_author", eventInfo.author)
                putExtra("blog_post_timestamp", timestamp)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                eventInfo.id.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val iconRes = com.example.R.mipmap.ic_launcher
            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(iconRes)
                .setContentTitle(eventInfo.title)
                .setContentText(eventInfo.shortSubtitle)
                .setStyle(NotificationCompat.BigTextStyle().bigText("${eventInfo.shortSubtitle}\n\n${eventInfo.fullGuidanceContent}"))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(eventInfo.id.hashCode(), notification)
        }

        // Schedule next alarms for tomorrow
        scheduleNextAlarm(context)
    }

    companion object {
        private const val REQUEST_CODE_AFTERNOON = 3001 // 16:00 (4:00 PM) for 29th moon sighting & events
        private const val REQUEST_CODE_MORNING = 3002   // 06:00 (6:00 AM) for 30th date adjustment & new month

        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // 1. Schedule Morning Alarm at 6:00 AM (06:00)
            scheduleDailyAlarm(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQUEST_CODE_MORNING,
                targetHour = 6,
                targetMinute = 0
            )

            // 2. Schedule Afternoon Alarm at 4:00 PM (16:00)
            scheduleDailyAlarm(
                context = context,
                alarmManager = alarmManager,
                requestCode = REQUEST_CODE_AFTERNOON,
                targetHour = 16,
                targetMinute = 0
            )
        }

        private fun scheduleDailyAlarm(
            context: Context,
            alarmManager: AlarmManager,
            requestCode: Int,
            targetHour: Int,
            targetMinute: Int
        ) {
            val intent = Intent(context, IslamicEventReceiver::class.java).apply {
                action = "com.example.ACTION_ISLAMIC_ALARM_$requestCode"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            for (code in listOf(REQUEST_CODE_AFTERNOON, REQUEST_CODE_MORNING)) {
                val intent = Intent(context, IslamicEventReceiver::class.java).apply {
                    action = "com.example.ACTION_ISLAMIC_ALARM_$code"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    code,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
