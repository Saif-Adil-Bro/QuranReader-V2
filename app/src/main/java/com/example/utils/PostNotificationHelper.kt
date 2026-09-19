package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BlogPost
import com.example.data.model.ShortPost
import com.example.receiver.PostNotificationShareReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object PostNotificationHelper {

    private const val CHANNEL_ID = "islamic_posts_channel"
    private const val CHANNEL_NAME = "ইসলামিক পোস্ট ও নসীহত"
    private const val CHANNEL_DESC = "নতুন ইসলামিক আপডেট, ব্লগ ও ফটো কার্ডের নোটিফিকেশন"

    private const val PREFS_NOTIFIED = "notified_posts_prefs"
    private const val KEY_NOTIFIED_IDS = "notified_ids"

    fun saveSystemNotificationToFirestore(
        context: Context,
        title: String,
        content: String,
        category: String = "নোটিফিকেশন",
        author: String = "ইসলামিক এডমিন",
        imageUrl: String = "",
        navigateTo: String = ""
    ) {
        if (title.isBlank() && content.isBlank()) return
        val notifKey = (title + content).hashCode().toString()
        if (isAlreadyNotified(context, "saved_notif_$notifKey")) return
        markAsNotified(context, "saved_notif_$notifKey")

        val now = System.currentTimeMillis()
        val data = hashMapOf<String, Any>(
            "title" to title,
            "name" to title,
            "content" to content,
            "text" to content,
            "body" to content,
            "category" to category,
            "author" to author,
            "imageUrl" to imageUrl,
            "image" to imageUrl,
            "readTime" to "${(content.length / 300).coerceAtLeast(1)} মিনিট",
            "timestamp" to now,
            "createdAt" to com.google.firebase.Timestamp.now()
        )
        if (navigateTo.isNotBlank()) {
            data["navigate_to"] = navigateTo
        }

        try {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("notifications")
                .add(data)
                .addOnSuccessListener {
                    android.util.Log.d("PostNotificationHelper", "Notification saved to Firestore: ${it.id}")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("PostNotificationHelper", "Error saving notification to notifications collection, fallback to blog_posts", e)
                    try {
                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            .collection("blog_posts")
                            .add(data)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isAlreadyNotified(context: Context, id: String): Boolean {
        if (id.isBlank()) return false
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()
        return set.contains(id)
    }

    fun markAsNotified(context: Context, id: String) {
        if (id.isBlank()) return
        val prefs = context.getSharedPreferences(PREFS_NOTIFIED, Context.MODE_PRIVATE)
        val set = (prefs.getStringSet(KEY_NOTIFIED_IDS, emptySet()) ?: emptySet()).toMutableSet()
        set.add(id)
        prefs.edit().putStringSet(KEY_NOTIFIED_IDS, set).apply()
    }

    fun shouldNotifyPost(context: Context, postTimestamp: Long, id: String): Boolean {
        if (id.isNotBlank() && isAlreadyNotified(context, id)) return false
        val syncPrefs = context.getSharedPreferences("posts_sync_prefs", Context.MODE_PRIVATE)
        var installTime = syncPrefs.getLong("app_first_install_time", 0L)
        val now = System.currentTimeMillis()
        if (installTime == 0L) {
            installTime = now
            syncPrefs.edit().putLong("app_first_install_time", now).putLong("last_sync_timestamp", now).apply()
            return false // Fresh install / first open: do not spam notifications for existing posts
        }
        // If post timestamp is missing or is older than the app first install / launch time, skip notification
        if (postTimestamp <= 0L || postTimestamp < (installTime - 30_000L)) {
            return false
        }
        return true
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showPhotoCardNotification(context: Context, post: ShortPost) {
        val notifKey = if (post.id.isNotBlank()) post.id else (post.category + post.text)
        if (isAlreadyNotified(context, notifKey)) return
        markAsNotified(context, notifKey)

        if (!shouldNotifyPost(context, post.timestamp, notifKey)) {
            return
        }

        createNotificationChannel(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cardBitmap = PostShareUtil.generateCardBitmap(
                    context = context,
                    post = post,
                    template = PostShareUtil.preDefinedTemplates.first(), // Emerald is the first one
                    bgImageUrl = null,
                    overlayAlpha = 0.65f,
                    textAlignName = "CENTER",
                    fontName = "SolaimanLipi",
                    fontSizeSp = 22f,
                    customCategory = post.category,
                    customText = post.text,
                    customRef = post.reference,
                    showLogo = true,
                    showWatermark = true
                )

                val reqId = if (post.id.isNotBlank()) post.id.hashCode() else (post.category + post.text).hashCode()

                // Intent for Opening Customizer (used by Notification Click, Share, and Edit)
                val openCustomizerIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("navigate_to", "posts")
                    putExtra("open_photo_card_edit", true)
                    putExtra("post_id", post.id)
                    putExtra("post_text", post.text)
                    putExtra("post_ref", post.reference)
                    putExtra("post_category", post.category)
                    putExtra("post_author", post.author)
                }

                val openCustomizerPendingIntent = PendingIntent.getActivity(
                    context,
                    reqId,
                    openCustomizerIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val titleText = if (post.category.isNotBlank()) "নতুন ফটো কার্ড: ${post.category}" else "নতুন ইসলামিক ফটো কার্ড"

                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(titleText)
                    .setContentText(post.text)
                    .setLargeIcon(cardBitmap)
                    .setStyle(
                        NotificationCompat.BigPictureStyle()
                            .bigPicture(cardBitmap)
                            .bigLargeIcon(null as Bitmap?)
                            .setBigContentTitle(titleText)
                            .setSummaryText(post.text)
                    )
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(openCustomizerPendingIntent)
                    .addAction(
                        android.R.drawable.ic_menu_share,
                        "Share",
                        openCustomizerPendingIntent
                    )
                    .addAction(
                        android.R.drawable.ic_menu_edit,
                        "Edit",
                        openCustomizerPendingIntent
                    )
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(reqId, notification)
                
                try {
                    val db = com.example.data.local.NotificationDatabase.getDatabase(context)
                    val entity = com.example.data.local.entity.LocalNotificationEntity(
                        title = titleText,
                        content = post.text,
                        category = "নোটিফিকেশন",
                        author = "ফটো কার্ড",
                        timestamp = System.currentTimeMillis()
                    )
                    db.localNotificationDao().insertNotification(entity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun showBlogPostNotification(context: Context, post: BlogPost) {
        val notifKey = if (post.id.isNotBlank()) post.id else (post.title + post.content)
        if (isAlreadyNotified(context, notifKey)) return
        markAsNotified(context, notifKey)

        if (!shouldNotifyPost(context, post.timestamp, notifKey)) {
            return
        }

        createNotificationChannel(context)

        val reqId = if (post.id.isNotBlank()) post.id.hashCode() else (post.title + post.content).hashCode()

        val openDetailIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", if (post.category == "নোটিফিকেশন" || post.category == "নোটিশ") "notifications" else "posts")
            putExtra("open_blog_post_detail", true)
            putExtra("blog_post_id", post.id)
            putExtra("blog_post_title", post.title)
            putExtra("blog_post_content", post.content)
            putExtra("blog_post_category", post.category)
            putExtra("blog_post_author", post.author)
            putExtra("blog_post_read_time", post.readTime)
            putExtra("blog_post_image_url", post.imageUrl)
            putExtra("blog_post_timestamp", post.timestamp)
        }

        val openDetailPendingIntent = PendingIntent.getActivity(
            context,
            reqId,
            openDetailIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (post.category == "নোটিফিকেশন" || post.category == "নোটিশ") {
            "📢 ${post.title.ifBlank { "নতুন নোটিফিকেশন" }}"
        } else {
            "নতুন পোস্ট: ${post.title}"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(post.title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(openDetailPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "পড়ুন",
                openDetailPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_share,
                "শেয়ার",
                openDetailPendingIntent
            )
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(reqId, notification)

        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(context)
            val entity = com.example.data.local.entity.LocalNotificationEntity(
                title = titleText,
                content = post.content,
                category = "নোটিফিকেশন",
                author = post.author,
                timestamp = System.currentTimeMillis()
            )
            CoroutineScope(Dispatchers.IO).launch {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showPostNotification(context: Context, title: String, message: String, postId: String = "") {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "posts")
            putExtra("post_id", postId)
        }

        val notificationId = if (postId.isNotBlank()) postId.hashCode() else (title + message).hashCode()
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        var finalTitle = title
        var finalText = message
        
        if (title.contains("•")) {
            val parts = title.split("•", limit = 2)
            finalTitle = parts[0].trim()
            finalText = parts[1].trim()
        } else if (title.contains("-")) {
            val parts = title.split("-", limit = 2)
            finalTitle = parts[0].trim()
            finalText = parts[1].trim()
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(finalTitle)
            .setContentText(finalText)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())

        try {
            val db = com.example.data.local.NotificationDatabase.getDatabase(context)
            val entity = com.example.data.local.entity.LocalNotificationEntity(
                title = finalTitle,
                content = finalText,
                category = "নোটিফিকেশন",
                author = "ইসলামিক এডমিন",
                timestamp = System.currentTimeMillis()
            )
            CoroutineScope(Dispatchers.IO).launch {
                db.localNotificationDao().insertNotification(entity)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
