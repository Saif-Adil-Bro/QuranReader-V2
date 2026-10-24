package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LocalNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalNotificationDao {
    @Query("SELECT * FROM local_notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<LocalNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: LocalNotificationEntity)

    @Query("DELETE FROM local_notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    @Query("DELETE FROM local_notifications WHERE author IN ('চাঁদ দেখা বিজ্ঞপ্তি', 'জুমুআ মোবারক')")
    suspend fun cleanupDummyNotifications()

    @Query("""
        DELETE FROM local_notifications
        WHERE EXISTS (
            SELECT 1 FROM local_notifications AS newer
            WHERE newer.title = local_notifications.title
              AND newer.author = local_notifications.author
              AND newer.id > local_notifications.id
              AND ABS(newer.timestamp - local_notifications.timestamp) < 129600000
        )
    """)
    suspend fun cleanupDuplicateNotifications()

    @Query("""
        SELECT COUNT(*) FROM local_notifications
        WHERE title = :title
          AND author = :author
          AND ABS(:timestamp - timestamp) < 64800000
    """)
    suspend fun countRecentDuplicate(title: String, author: String, timestamp: Long): Int

    @androidx.room.Transaction
    suspend fun insertNotificationDeduped(notification: LocalNotificationEntity) {
        if (countRecentDuplicate(notification.title, notification.author, notification.timestamp) == 0) {
            insertNotification(notification)
        }
    }
}
