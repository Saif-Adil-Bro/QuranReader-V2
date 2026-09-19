package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Helper to notify user on Fridays about Jumu'ah virtues.
 * Clicking this notification opens Firestore notification post "A2QBK9KEEjW72dwAwlV7".
 */
object JumuahReminderHelper {

    private const val PREFS_NAME = "jumuah_notification_prefs"
    private const val KEY_LAST_NOTIFIED_DATE = "last_notified_jumuah_date"
    private const val CHANNEL_ID = "jumuah_reminder_channel"
    const val TARGET_POST_ID = "A2QBK9KEEjW72dwAwlV7"

    /**
     * Checks if today is Friday.
     * If yes, and not notified yet for today, triggers the Jumu'ah reminder notification.
     */
    fun checkAndNotifyFriday(context: Context) {
        val today = LocalDate.now()

        // Check if today is Friday
        if (today.dayOfWeek != DayOfWeek.FRIDAY) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastNotifiedDate = prefs.getString(KEY_LAST_NOTIFIED_DATE, null)
        val todayStr = today.toString() // Format: YYYY-MM-DD

        // If already notified for today's Friday, skip
        if (lastNotifiedDate == todayStr) {
            return
        }

        val title = "জুমুআর দিনের বিশেষ গুরুত্ব ও ফজিলত"
        val message = "প্রতি কদমে এক বছরের সওয়াব! 🕌✨"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "জুমুআ রিমাইন্ডার",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "প্রতি শুক্রবার জুমুআর দিনের বিশেষ গুরুত্ব ও ফজিলত নোটিফিকেশন"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val timestamp = System.currentTimeMillis()
        val notificationId = ("jumuah_reminder_$todayStr").hashCode()

        // 1. Clean up any previous dummy local entries
        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(context)
            GlobalScope.launch(Dispatchers.IO) {
                db.localNotificationDao().cleanupDummyNotifications()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Intent to open directly to the specific Firestore notification post
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "notifications")
            putExtra("open_blog_post_detail", true)
            putExtra("blog_post_id", TARGET_POST_ID)
            putExtra("blog_post_title", title)
            putExtra("blog_post_content", "")
            putExtra("blog_post_category", "নোটিফিকেশন")
            putExtra("blog_post_author", "জুমুআ মোবারক")
            putExtra("blog_post_timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = com.example.R.mipmap.ic_launcher
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notificationId, notification)

        // Mark as notified for today
        prefs.edit()
            .putString(KEY_LAST_NOTIFIED_DATE, todayStr)
            .apply()
    }
}
