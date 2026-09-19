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
}
