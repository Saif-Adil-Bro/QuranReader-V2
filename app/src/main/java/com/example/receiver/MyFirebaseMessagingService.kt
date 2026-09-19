package com.example.receiver
import kotlinx.coroutines.GlobalScope


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "ইসলামিক বার্তা"
            
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: remoteMessage.data["message"] 
            ?: remoteMessage.data["text"] 
            ?: ""

        val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            ?: remoteMessage.data["image"]
            ?: remoteMessage.data["imageUrl"]
            ?: remoteMessage.data["image_url"]
            ?: remoteMessage.data["img_url"]

        if (!imageUrl.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                val bitmap = fetchBitmap(imageUrl)
                withContext(Dispatchers.Main) {
                    sendNotification(title, body, bitmap, remoteMessage.data)
                }
            }
        } else {
            sendNotification(title, body, null, remoteMessage.data)
        }
    }

    override fun onNewToken(token: String) {
        // Handle token refresh if needed
        super.onNewToken(token)
    }

    private suspend fun fetchBitmap(url: String): Bitmap? {
        return try {
            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false) // Crucial for notification/widget bitmaps to prevent rendering crashes
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun sendNotification(
        title: String, 
        messageBody: String, 
        bitmap: Bitmap?,
        dataMap: Map<String, String> = emptyMap()
    ) {
        val postId = dataMap["post_id"] ?: dataMap["blog_post_id"] ?: ""
        val reqId = if (postId.isNotBlank()) postId.hashCode() else (title + messageBody).hashCode()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", dataMap["navigate_to"] ?: "posts")
            for ((key, value) in dataMap) {
                putExtra(key, value)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, reqId, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        var finalTitle = title
        var finalText = messageBody
        
        if (title.contains("•")) {
            val parts = title.split("•", limit = 2)
            finalTitle = parts[0].trim()
            finalText = parts[1].trim()
        } else if (title.contains("-")) {
            val parts = title.split("-", limit = 2)
            finalTitle = parts[0].trim()
            finalText = parts[1].trim()
        }

        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(this)
            val entity = com.example.data.local.entity.LocalNotificationEntity(
                title = finalTitle,
                content = finalText,
                category = "নোটিফিকেশন",
                author = "পুশ নোটিফিকেশন",
                timestamp = System.currentTimeMillis()
            )
            kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val channelId = "fcm_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (bitmap != null) {
            notificationBuilder.setLargeIcon(bitmap)
            notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bitmap)
                    .bigLargeIcon(null as Bitmap?)
            )
        }

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Push Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(reqId, notificationBuilder.build())
    }
}
