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
import java.time.LocalTime

/**
 * Helper to check when a Hijri month reaches day 30 or day 1 (New Month),
 * and trigger an adjustment notice to open the Modern Hijri Adjustment Dialog.
 */
object HijriNewMonthNotificationHelper {

    private const val PREFS_NAME = "hijri_month_notification_prefs"
    private const val KEY_LAST_NOTIFIED_HIJRI_MONTH = "last_notified_hijri_month"
    private const val KEY_LAST_NOTIFIED_HIJRI_YEAR = "last_notified_hijri_year"

    private const val KEY_LAST_NOTIFIED_30TH_MONTH = "last_notified_30th_month"
    private const val KEY_LAST_NOTIFIED_30TH_YEAR = "last_notified_30th_year"

    private const val CHANNEL_ID = "hijri_new_month_channel"

    /**
     * Checks if today is the 30th day of the current Hijri month in the morning (at or after 6:00 AM).
     * If yes, and not notified yet for this Hijri month & year, shows the date adjustment alert.
     */
    fun checkAndNotify30th(context: Context, hijriOffset: Int = 0) {
        val today = LocalDate.now()
        val hijriInfo = HijriCalendarUtil.getHijriDate(today, hijriOffset)

        // Trigger only on the 30th day of the Hijri month
        if (hijriInfo.hijriDay != 30) {
            return
        }

        // Trigger strictly in the morning at or after 6:00 AM
        val currentHour = LocalTime.now().hour
        if (currentHour < 6) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastMonth = prefs.getInt(KEY_LAST_NOTIFIED_30TH_MONTH, -1)
        val lastYear = prefs.getInt(KEY_LAST_NOTIFIED_30TH_YEAR, -1)

        // If already notified for this specific month & year, skip
        if (lastMonth == hijriInfo.hijriMonth && lastYear == hijriInfo.hijriYear) {
            return
        }

        val isEnglish = NotificationLocalization.isEnglish(context)
        val monthNameBn = hijriInfo.hijriMonthNameBn
        val monthNameEn = hijriInfo.hijriMonthNameEn
        val month = if (isEnglish && monthNameEn.isNotBlank()) monthNameEn else monthNameBn

        val title = if (isEnglish) "Hijri Date Adjustment Notice 🌙" else "হিজরি তারিখ সমন্বয় সতর্কতা 🌙"
        val message = if (isEnglish) {
            "⚠️ Today is 30th $month. If local crescent sighting differs from calendar, please tap to adjust Hijri date (+/-)."
        } else {
            "⚠️ আজ হিজরি ৩০ $month। স্থানীয় চাঁদ দেখার সাথে ক্যালেন্ডারের অমিল দেখা দিলে অনুগ্রহ করে এখনই তারিখ সমন্বয় (-/+) করে নিন।"
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (isEnglish) "Hijri Date Adjustment Notice" else "হিজরি তারিখ সমন্বয় সতর্কতা",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (isEnglish) "Alert to adjust calendar date on 30th/1st of Islamic month" else "হিজরি মাসের ৩০ ও ১ তারিখে তারিখ সমন্বয় করার সতর্কবার্তা"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val timestamp = System.currentTimeMillis()
        val notificationId = ("hijri_30th_${hijriInfo.hijriYear}_${hijriInfo.hijriMonth}").hashCode()

        // 1. Save to local Notification Database so it is visible in the App's Notification Center
        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(context)
            val entity = com.example.data.local.entity.LocalNotificationEntity(
                title = title,
                content = message,
                category = if (isEnglish) "Hijri Calendar" else "হিজরি ক্যালেন্ডার",
                author = if (isEnglish) "Date Adjustment (30th)" else "তারিখ সমন্বয় (৩০ তারিখ)",
                timestamp = timestamp
            )
            GlobalScope.launch(Dispatchers.IO) {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Intent to open directly to the modern Hijri Adjustment popup dialog
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "hijri_adjustment")
            putExtra("show_hijri_adjustment_dialog", true)
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

        // Save that we have notified for 30th day of this Hijri month
        prefs.edit()
            .putInt(KEY_LAST_NOTIFIED_30TH_MONTH, hijriInfo.hijriMonth)
            .putInt(KEY_LAST_NOTIFIED_30TH_YEAR, hijriInfo.hijriYear)
            .apply()
    }

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

        // Trigger in the morning at or after 6:00 AM
        val currentHour = LocalTime.now().hour
        if (currentHour < 6) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastMonth = prefs.getInt(KEY_LAST_NOTIFIED_HIJRI_MONTH, -1)
        val lastYear = prefs.getInt(KEY_LAST_NOTIFIED_HIJRI_YEAR, -1)

        // If already notified for this specific month & year, skip
        if (lastMonth == hijriInfo.hijriMonth && lastYear == hijriInfo.hijriYear) {
            return
        }

        val isEnglish = NotificationLocalization.isEnglish(context)
        val monthNameBn = hijriInfo.hijriMonthNameBn
        val monthNameEn = hijriInfo.hijriMonthNameEn
        val title = NotificationLocalization.getHijriNewMonthTitle(isEnglish)
        val message = NotificationLocalization.getHijriNewMonthMessage(monthNameBn, monthNameEn, isEnglish)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                NotificationLocalization.getHijriNewMonthChannelName(isEnglish),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = NotificationLocalization.getHijriNewMonthChannelDesc(isEnglish)
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
                category = if (isEnglish) "Hijri Calendar" else "হিজরি ক্যালেন্ডার",
                author = if (isEnglish) "Hijri Date Adjustment" else "হিজরি তারিখ সমন্বয়",
                timestamp = timestamp
            )
            GlobalScope.launch(Dispatchers.IO) {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Intent to open directly to the modern Hijri Adjustment popup dialog
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "hijri_adjustment")
            putExtra("show_hijri_adjustment_dialog", true)
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
