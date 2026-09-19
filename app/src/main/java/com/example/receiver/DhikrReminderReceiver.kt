package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.utils.DhikrReminderManager
import com.example.utils.DhikrType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DhikrReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            DhikrReminderManager.rescheduleAll(context)
            return
        }

        val typeName = intent.getStringExtra("dhikr_type") ?: DhikrType.DUROOD.name
        val dhikrType = try {
            DhikrType.valueOf(typeName)
        } catch (e: Exception) {
            DhikrType.DUROOD
        }

        val config = DhikrReminderManager.getConfig(context, dhikrType)
        if (!config.isEnabled) {
            return
        }

        // Check if inside quiet hours
        val isQuiet = DhikrReminderManager.isQuietTime(config)

        if (!isQuiet) {
            showNotification(context, dhikrType, config.selectedAudioId)
            try {
                DhikrReminderManager.speakReminder(context, dhikrType, config.selectedAudioId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Re-schedule next interval
        DhikrReminderManager.scheduleReminder(context, dhikrType, config.intervalMinutes)
    }

    private fun showNotification(context: Context, type: DhikrType, audioId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "dhikr_continuous_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Dhikr & Salawat Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Regular reminders for Durood and Istighfar"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val optionsList = if (type == DhikrType.DUROOD) {
            DhikrReminderManager.duroodAudioOptions
        } else {
            DhikrReminderManager.istighfarAudioOptions
        }

        val selectedOption = optionsList.find { it.id == audioId } ?: optionsList.first()

        val notifTitle = if (type == DhikrType.DUROOD) {
            "দরূদ পাঠের স্মরণিকা ✨"
        } else {
            "ইস্তেগফারের স্মরণিকা 🤲"
        }

        val notifContent = "${selectedOption.arabicText}\n${selectedOption.phoneticText}\n(${selectedOption.translationText})"

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "notifications")
            putExtra("target_screen", "notifications")
        }

        val notifId = if (type == DhikrType.DUROOD) 4001 else 4002
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val iconRes = com.example.R.mipmap.ic_launcher

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconRes)
            .setContentTitle(notifTitle)
            .setContentText(selectedOption.arabicText + " - " + selectedOption.phoneticText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifContent))
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(notifId, notification)
    }
}
