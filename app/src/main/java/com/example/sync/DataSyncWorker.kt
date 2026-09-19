package com.example.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.model.BlogPost
import com.example.data.model.ShortPost
import com.example.utils.PostNotificationHelper
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DataSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val prefs: SharedPreferences by lazy {
        appContext.getSharedPreferences("posts_sync_prefs", Context.MODE_PRIVATE)
    }

    override suspend fun doWork(): Result {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val currentMills = System.currentTimeMillis()
            var installTime = prefs.getLong("app_first_install_time", 0L)
            if (installTime == 0L) {
                installTime = currentMills
                prefs.edit().putLong("app_first_install_time", currentMills).putLong("last_sync_timestamp", currentMills).apply()
                return Result.success()
            }

            val lastSync = prefs.getLong("last_sync_timestamp", 0L)
            if (lastSync == 0L) {
                prefs.edit().putLong("last_sync_timestamp", currentMills).apply()
                return Result.success()
            }

            val effectiveLastSync = maxOf(lastSync, installTime)
            var maxTimestampSeen = effectiveLastSync

            val processedIds = mutableSetOf<String>()
            val processedTitles = mutableSetOf<String>()

            // 1. Sync new blog_posts
            val blogSnapshot = firestore.collection("blog_posts")
                .get()
                .await()

            for (doc in blogSnapshot.documents) {
                val ts = extractTimestamp(doc)
                if (ts > effectiveLastSync) {
                    val title = doc.getString("title") ?: doc.getString("name") ?: ""
                    val content = doc.getString("content") ?: doc.getString("text") ?: doc.getString("body") ?: ""
                    if ((title.isNotBlank() || content.isNotBlank()) && !processedIds.contains(doc.id) && !processedTitles.contains(title)) {
                        processedIds.add(doc.id)
                        if (title.isNotBlank()) processedTitles.add(title)

                        val blogPost = BlogPost(
                            id = doc.id,
                            title = title.ifBlank { "নতুন ইসলামিক পোস্ট" },
                            content = content,
                            author = doc.getString("author") ?: "ইসলামিক এডমিন",
                            category = doc.getString("category") ?: "সাধারণ",
                            imageUrl = doc.getString("imageUrl") ?: doc.getString("image") ?: "",
                            readTime = doc.getString("readTime") ?: "২ মিনিট",
                            timestamp = ts
                        )
                        PostNotificationHelper.showBlogPostNotification(appContext, blogPost)
                        if (ts > maxTimestampSeen) maxTimestampSeen = ts
                    }
                }
            }

            // 2. Sync new articles (skip if already processed)
            val articleSnapshot = firestore.collection("articles")
                .get()
                .await()

            for (doc in articleSnapshot.documents) {
                val ts = extractTimestamp(doc)
                if (ts > effectiveLastSync) {
                    val title = doc.getString("title") ?: doc.getString("name") ?: ""
                    val content = doc.getString("content") ?: doc.getString("text") ?: doc.getString("body") ?: ""
                    if ((title.isNotBlank() || content.isNotBlank()) && !processedIds.contains(doc.id) && !processedTitles.contains(title)) {
                        processedIds.add(doc.id)
                        if (title.isNotBlank()) processedTitles.add(title)

                        val blogPost = BlogPost(
                            id = doc.id,
                            title = title.ifBlank { "নতুন নিবন্ধ" },
                            content = content,
                            author = doc.getString("author") ?: "ইসলামিক এডমিন",
                            category = doc.getString("category") ?: "সাধারণ",
                            imageUrl = doc.getString("imageUrl") ?: doc.getString("image") ?: "",
                            readTime = doc.getString("readTime") ?: "২ মিনিট",
                            timestamp = ts
                        )
                        PostNotificationHelper.showBlogPostNotification(appContext, blogPost)
                        if (ts > maxTimestampSeen) maxTimestampSeen = ts
                    }
                }
            }

            // 3. Sync new short_posts
            val shortSnapshot = firestore.collection("short_posts")
                .get()
                .await()

            for (doc in shortSnapshot.documents) {
                val ts = extractTimestamp(doc)
                if (ts > effectiveLastSync) {
                    val text = doc.getString("text") ?: doc.getString("content") ?: doc.getString("title") ?: ""
                    if (text.isNotBlank() && !processedIds.contains(doc.id) && !processedTitles.contains(text)) {
                        processedIds.add(doc.id)
                        processedTitles.add(text)

                        val shortPost = ShortPost(
                            id = doc.id,
                            text = text,
                            reference = doc.getString("reference") ?: doc.getString("ref") ?: "",
                            category = doc.getString("category") ?: "দৈনিক নসীহত",
                            author = doc.getString("author") ?: "ইসলামিক স্কলার",
                            timestamp = ts
                        )
                        PostNotificationHelper.showPhotoCardNotification(appContext, shortPost)
                        if (ts > maxTimestampSeen) maxTimestampSeen = ts
                    }
                }
            }

            // 4. Sync new notifications collection
            val notifSnapshot = firestore.collection("notifications")
                .get()
                .await()

            for (doc in notifSnapshot.documents) {
                val ts = extractTimestamp(doc)
                if (ts > effectiveLastSync) {
                    val title = doc.getString("title") ?: doc.getString("name") ?: ""
                    val content = doc.getString("content") ?: doc.getString("text") ?: doc.getString("body") ?: ""
                    if ((title.isNotBlank() || content.isNotBlank()) && !processedIds.contains(doc.id) && !processedTitles.contains(title)) {
                        processedIds.add(doc.id)
                        if (title.isNotBlank()) processedTitles.add(title)

                        val blogPost = BlogPost(
                            id = doc.id,
                            title = title.ifBlank { "নতুন নোটিফিকেশন" },
                            content = content,
                            author = doc.getString("author") ?: "ইসলামিক এডমিন",
                            category = doc.getString("category") ?: "নোটিফিকেশন",
                            imageUrl = doc.getString("imageUrl") ?: doc.getString("image") ?: "",
                            readTime = doc.getString("readTime") ?: "১ মিনিট",
                            timestamp = ts
                        )
                        PostNotificationHelper.showBlogPostNotification(appContext, blogPost)
                        if (ts > maxTimestampSeen) maxTimestampSeen = ts
                    }
                }
            }

            // Update last sync time
            prefs.edit().putLong("last_sync_timestamp", maxOf(maxTimestampSeen, currentMills)).apply()

            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    private fun extractTimestamp(doc: DocumentSnapshot): Long {
        return try {
            val raw = doc.get("timestamp") ?: doc.get("createdAt") ?: doc.get("date")
            when (raw) {
                is Timestamp -> raw.toDate().time
                is Number -> {
                    val t = raw.toLong()
                    if (t in 1L..99999999999L) t * 1000 else t
                }
                is String -> {
                    val t = raw.toLongOrNull() ?: 0L
                    if (t in 1L..99999999999L) t * 1000 else t
                }
                else -> 0L
            }
        } catch (e: Exception) {
            0L
        }
    }
}
