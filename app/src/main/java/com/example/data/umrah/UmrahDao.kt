package com.example.data.umrah

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UmrahDao {
    @Query("SELECT * FROM umrah_sessions WHERE isCompleted = 0 ORDER BY id DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<UmrahSessionEntity?>

    @Query("SELECT * FROM umrah_sessions WHERE isCompleted = 0 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveSessionOnce(): UmrahSessionEntity?

    @Query("SELECT * FROM umrah_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionById(sessionId: Long): Flow<UmrahSessionEntity?>

    @Query("SELECT * FROM umrah_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionByIdOnce(sessionId: Long): UmrahSessionEntity?

    @Query("SELECT * FROM umrah_sessions WHERE isCompleted = 1 ORDER BY endTime DESC")
    fun getAllCompletedSessions(): Flow<List<UmrahSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UmrahSessionEntity): Long

    @Update
    suspend fun updateSession(session: UmrahSessionEntity)

    @Query("DELETE FROM umrah_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Long)

    // Checklist queries
    @Query("SELECT * FROM umrah_checklist_items WHERE sessionId = :sessionId")
    fun getChecklistForSession(sessionId: Long): Flow<List<UmrahChecklistEntity>>

    @Query("SELECT * FROM umrah_checklist_items WHERE sessionId = :sessionId")
    suspend fun getChecklistForSessionOnce(sessionId: Long): List<UmrahChecklistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<UmrahChecklistEntity>)

    @Query("UPDATE umrah_checklist_items SET isChecked = :isChecked, checkedAt = :checkedAt WHERE sessionId = :sessionId AND stepKey = :stepKey AND itemIndex = :itemIndex")
    suspend fun updateChecklistItem(sessionId: Long, stepKey: String, itemIndex: Int, isChecked: Boolean, checkedAt: Long?)

    // Round Logs queries
    @Query("SELECT * FROM umrah_round_logs WHERE sessionId = :sessionId ORDER BY roundNumber ASC")
    fun getRoundLogsForSession(sessionId: Long): Flow<List<UmrahRoundLogEntity>>

    @Query("SELECT * FROM umrah_round_logs WHERE sessionId = :sessionId ORDER BY roundNumber ASC")
    suspend fun getRoundLogsForSessionOnce(sessionId: Long): List<UmrahRoundLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoundLog(log: UmrahRoundLogEntity): Long
}
