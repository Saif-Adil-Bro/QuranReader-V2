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

        val hijriOffset = sharedPrefs.getInt("hijri_offset", 0)

        // Check and notify for New Hijri Month transition
        try {
            com.example.utils.HijriNewMonthNotificationHelper.checkAndNotifyNewMonth(context, hijriOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check and notify for 29th Hijri day Moon Sighting notice
        try {
            com.example.utils.MoonSightingNotificationHelper.checkAndNotify29th(context, hijriOffset)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check and notify for Friday Jumu'ah reminder
        try {
            com.example.utils.JumuahReminderHelper.checkAndNotifyFriday(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val today = LocalDate.now()
        val eventInfo = IslamicEventGuidanceHelper.checkNotificationForDate(today, hijriOffset)

        if (eventInfo != null) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "islamic_events_reminder"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "ইসলামিক দিবস ও রোজা রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "সোম-বৃহস্পতিবারের সুন্নাত রোজা, আইয়ামে বীজ ও ইসলামিক দিবসের নোটিফিকেশন"
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

        // Schedule next alarm for tomorrow evening
        scheduleNextAlarm(context)
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, IslamicEventReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val sharedPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            val hour = sharedPrefs.getInt("islamic_events_hour", 17) // Default 5:15 PM (17:15) - Afternoon after Asr
            val minute = sharedPrefs.getInt("islamic_events_minute", 15)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
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
            val intent = Intent(context, IslamicEventReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                3001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
