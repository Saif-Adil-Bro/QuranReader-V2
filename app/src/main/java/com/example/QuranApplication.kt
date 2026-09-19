package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.di.AppContainer

class QuranApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        com.example.data.DuaData.initialize(this)
        com.example.sync.NetworkSyncManager.initialize(this)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            val channelId = "quran_planner_reminder"
            val channelName = "Quran Planner Reminder"
            val channelDescription = "Reminds you to read Quran to complete your daily goal"
            val importance = android.app.NotificationManager.IMPORTANCE_DEFAULT
            val channel = android.app.NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
            }
            notificationManager.createNotificationChannel(channel)

            val prayerChannelId = "prayer_times_notification_channel"
            val prayerChannelName = "ওয়াক্ত শুরুর নোটিফিকেশন"
            val prayerChannelDesc = "প্রতিটি ওয়াক্তের সালাত শুরু হলে স্মরণ করিয়ে দেওয়া হয়"
            val prayerChannel = android.app.NotificationChannel(prayerChannelId, prayerChannelName, android.app.NotificationManager.IMPORTANCE_HIGH).apply {
                description = prayerChannelDesc
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(prayerChannel)
        }

        try {
            com.example.receiver.IslamicEventReceiver.scheduleNextAlarm(this)
            com.example.utils.PrayerNotificationHelper.scheduleNextPrayerAlarms(this)
            val sharedPrefs = getSharedPreferences("quran_menu_prefs", android.content.Context.MODE_PRIVATE)
            val hijriOffset = sharedPrefs.getInt("hijri_offset", 0)
            com.example.utils.HijriNewMonthNotificationHelper.checkAndNotifyNewMonth(this, hijriOffset)
            com.example.utils.MoonSightingNotificationHelper.checkAndNotify29th(this, hijriOffset)
            com.example.utils.JumuahReminderHelper.checkAndNotifyFriday(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Cache images even if HTTP response headers restrict caching
            .build()
    }
}
