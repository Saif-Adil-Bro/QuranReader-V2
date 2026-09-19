package com.example.receiver
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Dispatchers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Calendar

class DailyMessageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val sharedPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            val enabled = sharedPrefs.getBoolean("daily_message_enabled", true)
            if (enabled) {
                scheduleNextAlarm(context)
            }
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "daily_islamic_message"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Islamic Message",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily Ayah, Hadith or Islamic reminder"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Ensure DuaData is initialized to fetch real Quranic Duas
        try {
            com.example.data.DuaData.initialize(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Check and notify for 29th Hijri day Moon Sighting notice at morning
        try {
            val sharedPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            val hijriOffset = sharedPrefs.getInt("hijri_offset", 0)
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

        val richDuas = com.example.data.DuaData.richDuas
        val selectedDua = if (richDuas.isNotEmpty()) {
            val calendar = java.util.Calendar.getInstance()
            val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
            richDuas[dayOfYear % richDuas.size]
        } else {
            com.example.data.DuaItem(
                id = 1,
                title = "দুনিয়া ও পরকালের কল্যাণের দুআ",
                segments = listOf(
                    com.example.data.DuaSegment(
                        translation = "হে আমাদের রব! আমাদেরকে দুনিয়াতে কল্যাণ দান করুন এবং আখিরাতেও কল্যাণ দান করুন। (সূরা বাকারা: ২০১)"
                    )
                )
            )
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "dua")
            putExtra("target_screen", "dua")
            putExtra("dua_id", selectedDua.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val duaTitle = "কুরআনিক দোয়া"
        val duaSubtitle = selectedDua.title

        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(context)
            val entity = com.example.data.local.entity.LocalNotificationEntity(
                title = duaTitle,
                content = duaSubtitle,
                category = "নোটিফিকেশন",
                author = "কুরআনিক দুআ",
                timestamp = System.currentTimeMillis()
            )
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val iconRes = com.example.R.mipmap.ic_launcher
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(duaTitle)
            .setContentText(duaSubtitle)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(2001, notification)
        
        scheduleNextAlarm(context)
    }

    companion object {
        fun scheduleNextAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, DailyMessageReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val sharedPrefs = context.getSharedPreferences("quran_menu_prefs", Context.MODE_PRIVATE)
            val hour = sharedPrefs.getInt("daily_message_hour", 8)
            val minute = sharedPrefs.getInt("daily_message_minute", 0)

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
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }

        fun cancelAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val intent = Intent(context, DailyMessageReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                2001,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
