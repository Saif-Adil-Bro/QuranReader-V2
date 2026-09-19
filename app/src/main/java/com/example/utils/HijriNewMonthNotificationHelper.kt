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
import java.time.LocalDate

/**
 * Helper to check when a new Hijri month starts (Hijri day == 1)
 * and trigger a warning notification to guide the user to check/adjust the Hijri date in Settings.
 */
object HijriNewMonthNotificationHelper {

    private const val PREFS_NAME = "hijri_month_notification_prefs"
    private const val KEY_LAST_NOTIFIED_HIJRI_MONTH = "last_notified_hijri_month"
    private const val KEY_LAST_NOTIFIED_HIJRI_YEAR = "last_notified_hijri_year"
    private const val CHANNEL_ID = "hijri_new_month_channel"

    /**
     * Checks if today is the 1st day of a new Hijri month.
     * If yes, and not notified yet for this Hijri month & year, shows the warning notification.
     */
    fun checkAndNotifyNewMonth(context: Context, hijriOffset: Int = 0) {
        val today = LocalDate.now()
        val hijriInfo = HijriCalendarUtil.getHijriDate(today, hijriOffset)

        // Trigger on the 1st day of the Hijri month
        if (hijriInfo.hijriDay != 1) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastMonth = prefs.getInt(KEY_LAST_NOTIFIED_HIJRI_MONTH, -1)
        val lastYear = prefs.getInt(KEY_LAST_NOTIFIED_HIJRI_YEAR, -1)

        // If already notified for this specific month & year, skip
        if (lastMonth == hijriInfo.hijriMonth && lastYear == hijriInfo.hijriYear) {
            return
        }

        val monthName = hijriInfo.hijriMonthNameBn
        val title = "হিজরি নতুন মাস শুরু 🌙"
        val message = "⚠️ আজ থেকে \"$monthName\" মাস শুরু। তারিখে অসামঞ্জস্য দেখা দিলে অনুগ্রহ করে সেটিংস → হিজরি তারিখ সমন্বয় থেকে তারিখ ঠিক করে নিন। 🌙"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "নতুন হিজরি মাস সতর্কতা",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "নতুন আরবি মাস শুরু হলে তারিখ সমন্বয় করার সতর্কবার্তা"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val timestamp = System.currentTimeMillis()
        val notificationId = ("hijri_new_month_${hijriInfo.hijriYear}_${hijriInfo.hijriMonth}").hashCode()

        // 1. Save to local Notification Database so it is visible in the App's Notification Center
        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(context)
            val entity = com.example.data.local.entity.LocalNotificationEntity(
                title = title,
                content = message,
                category = "হিজরি ক্যালেন্ডার",
                author = "হিজরি তারিখ সমন্বয়",
                timestamp = timestamp
            )
            GlobalScope.launch(Dispatchers.IO) {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Intent to open directly to the Hijri Adjustment Section in Settings
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "hijri_adjustment")
            putExtra("target_screen", "hijri_adjustment")
            putExtra("highlight_hijri_adjustment", true)
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

        // Save that we have notified for this Hijri month
        prefs.edit()
            .putInt(KEY_LAST_NOTIFIED_HIJRI_MONTH, hijriInfo.hijriMonth)
            .putInt(KEY_LAST_NOTIFIED_HIJRI_YEAR, hijriInfo.hijriYear)
            .apply()
    }
}
