package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.di.AppContainer

class QuranApplication : Application(), ImageLoaderFactory {

    lateinit var container: AppContainer

    companion object {
        lateinit var instance: QuranApplication
            private set
    }

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(com.example.utils.LocaleHelper.onAttach(base))
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Safely initialize Firebase to avoid any runtime startup crashes on Appetize/CI/Emulators
        try {
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = try {
                    com.google.firebase.FirebaseOptions.fromResource(this)
                } catch (e: Exception) {
                    null
                }
                if (options != null && !options.apiKey.isNullOrBlank()) {
                    com.google.firebase.FirebaseApp.initializeApp(this, options)
                } else {
                    val fallbackOptions = com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:1039813595123:android:827244eaf363d1ae0f4c86")
                        .setApiKey("AIzaSyAFud_Pg8hJ_WjLSsf6HYNA7zzSzibermc")
                        .setProjectId("quranreader-67b33")
                        .setStorageBucket("quranreader-67b33.firebasestorage.app")
                        .build()
                    com.google.firebase.FirebaseApp.initializeApp(this, fallbackOptions)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("QuranApplication", "Firebase initialization safe error: ${e.message}")
        }

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

            com.example.utils.PrayerNotificationHelper.createNotificationChannel(this)
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
