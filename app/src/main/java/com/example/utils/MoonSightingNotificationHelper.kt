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
 * Helper to check when it is the 29th day of any Hijri month at 8:30 AM
 * and trigger the Moon Sighting Announcement notification.
 * Clicking this notification opens the specific Firestore notification post "r6mthyrNWvXtjppIbHFp".
 */
object MoonSightingNotificationHelper {

    private const val PREFS_NAME = "moon_sighting_notification_prefs"
    private const val KEY_LAST_NOTIFIED_HIJRI_MONTH = "last_notified_moon_sighting_month"
    private const val KEY_LAST_NOTIFIED_HIJRI_YEAR = "last_notified_moon_sighting_year"
    private const val CHANNEL_ID = "moon_sighting_reminder_channel"
    const val TARGET_POST_ID = "r6mthyrNWvXtjppIbHFp"

    private val hijriMonthNamesBengali = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শা'বান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ্জ"
    )

    /**
     * Dynamically formats the introductory sentence of the Moon Sighting announcement
     * with the current day of the week, running Hijri month, Hijri year, next month, and next year.
     */
    fun formatDynamicMoonSightingContent(content: String, hijriOffset: Int = 0): String {
        try {
            val today = LocalDate.now()
            val hijriInfo = HijriCalendarUtil.getHijriDate(today, hijriOffset)

            val bar = when (today.dayOfWeek) {
                java.time.DayOfWeek.SATURDAY -> "শনিবার"
                java.time.DayOfWeek.SUNDAY -> "রবিবার"
                java.time.DayOfWeek.MONDAY -> "সোমবার"
                java.time.DayOfWeek.TUESDAY -> "মঙ্গলবার"
                java.time.DayOfWeek.WEDNESDAY -> "বুধবার"
                java.time.DayOfWeek.THURSDAY -> "বৃহস্পতিবার"
                java.time.DayOfWeek.FRIDAY -> "শুক্রবার"
            }

            val runningDayBn = if (hijriInfo.hijriDay == 29) "২৯ শে" else "${DateUtil.toBengaliNumerals(hijriInfo.hijriDay)} শে"
            val runningMonth = hijriInfo.hijriMonthNameBn
            val runningYear = DateUtil.toBengaliNumerals(hijriInfo.hijriYear)

            val nextMonthIdx = if (hijriInfo.hijriMonth == 12) 1 else hijriInfo.hijriMonth + 1
            val nextMonth = hijriMonthNamesBengali.getOrElse(nextMonthIdx - 1) { "পরবর্তী মাস" }
            val nextYear = DateUtil.toBengaliNumerals(if (hijriInfo.hijriMonth == 12) hijriInfo.hijriYear + 1 else hijriInfo.hijriYear)

            val dynamicIntro = "আজ  $bar $runningDayBn $runningMonth $runningYear হিজরী সূর্যাস্তের পর বাংলাদেশের আকাশ সীমায় $nextMonth $nextYear  হিজরী সনের নতুন চাঁদ অনুসন্ধানের জন্য আমরা সবাই ব্যাপক উদ্যোগী হই এবং অন্যদেরকেও খুব উৎসাহিত করতে থাকি। বাংলাদেশের আকাশ সীমায় কোথাও চাঁদ দেখা গেলে তাৎক্ষণিক লিখিত বা ভয়েসে 'আল হক চাঁদ দেখা কমিটি বাংলাদেশ' এ এভাবে জানানোর অনুরোধ করছি,"

            val pattern = Regex(
                "আজ\\s+[^\\n]+?হিজরী\\s+সূর্যাস্তের\\s+পর\\s+বাংলাদেশের\\s+আকাশ\\s+সীমায়?[^\\n]+?হিজরী\\s+সনের\\s+নতুন\\s+চাঁদ\\s+অনুসন্ধানের\\s+জন্য\\s+আমরা\\s+সবাই\\s+ব্যাপক\\s+উদ্যোগী\\s+হই\\s+এবং\\s+অন্যদেরকেও\\s+খুব\\s+উৎসাহিত\\s+করতে\\s+থাকি।\\s*বাংলাদেশের\\s+আকাশ\\s+সীমায়?[^\\n]+?'আল হক চাঁদ দেখা কমিটি বাংলাদেশ'\\s*এ\\s*এভাবে\\s*জানানোর\\s*অনুরোধ\\s*করছি[,\n]*"
            )

            if (pattern.containsMatchIn(content)) {
                return pattern.replace(content, "$dynamicIntro\n")
            }

            val fallbackPattern = Regex(
                "আজ\\s+.*?সূর্যাস্তের পর বাংলাদেশের আকাশ সীমায়.*?নতুন চাঁদ অনুসন্ধানের জন্য.*?অনুরোধ করছি[,\n]*",
                RegexOption.DOT_MATCHES_ALL
            )
            if (fallbackPattern.containsMatchIn(content)) {
                return fallbackPattern.replace(content, "$dynamicIntro\n")
            }

            // If it's the target post and content doesn't match the regex directly, prepend or format it
            if (content.isNotBlank() && content.contains("চাঁদ অনুসন্ধানের জন্য")) {
                return "$dynamicIntro\n\n$content"
            }

            return content
        } catch (e: Exception) {
            return content
        }
    }

    /**
     * Checks if today is the 29th day of the current Hijri month.
     * If yes, and not notified yet for this Hijri month & year, shows the moon sighting reminder notification.
     */
    fun checkAndNotify29th(context: Context, hijriOffset: Int = 0) {
        val today = LocalDate.now()
        val hijriInfo = HijriCalendarUtil.getHijriDate(today, hijriOffset)

        // Trigger on the 29th day of the Hijri month
        if (hijriInfo.hijriDay != 29) {
            return
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastMonth = prefs.getInt(KEY_LAST_NOTIFIED_HIJRI_MONTH, -1)
        val lastYear = prefs.getInt(KEY_LAST_NOTIFIED_HIJRI_YEAR, -1)

        // If already notified for this specific month & year, skip
        if (lastMonth == hijriInfo.hijriMonth && lastYear == hijriInfo.hijriYear) {
            return
        }

        val title = "✨ চাঁদ দেখা সংক্রান্ত জরুরি জ্ঞাতব্য.. ✨"
        val message = "দ্বীনের বৃহত্তর এক যরূরত পূরণে সর্বস্তরের ওলামায়ে কেরামের কাছে বিশেষ আবেদন:"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "চাঁদ দেখা সংক্রান্ত বিজ্ঞপ্তি",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "প্রতি আরবি মাসের ২৯ তারিখে চাঁদ দেখা সংক্রান্ত জরুরি জ্ঞাতব্য ও বিশেষ আবেদন"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val timestamp = System.currentTimeMillis()
        val notificationId = ("moon_sighting_29_${hijriInfo.hijriYear}_${hijriInfo.hijriMonth}").hashCode()

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
            putExtra("blog_post_author", "চাঁদ দেখা বিজ্ঞপ্তি")
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

        // Mark as notified for this Hijri month & year
        prefs.edit()
            .putInt(KEY_LAST_NOTIFIED_HIJRI_MONTH, hijriInfo.hijriMonth)
            .putInt(KEY_LAST_NOTIFIED_HIJRI_YEAR, hijriInfo.hijriYear)
            .apply()
    }
}
