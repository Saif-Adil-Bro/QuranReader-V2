package com.example.data.umrah

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UmrahRepository(context: Context) {
    private val database = UmrahDatabase.getDatabase(context)
    private val dao = database.umrahDao()

    val activeSessionFlow: Flow<UmrahSessionEntity?> = dao.getActiveSessionFlow()
    val allCompletedSessionsFlow: Flow<List<UmrahSessionEntity>> = dao.getAllCompletedSessions()

    fun getSessionById(sessionId: Long): Flow<UmrahSessionEntity?> = dao.getSessionById(sessionId)
    fun getChecklistForSession(sessionId: Long): Flow<List<UmrahChecklistEntity>> = dao.getChecklistForSession(sessionId)
    fun getRoundLogsForSession(sessionId: Long): Flow<List<UmrahRoundLogEntity>> = dao.getRoundLogsForSession(sessionId)

    suspend fun getActiveSessionOnce(): UmrahSessionEntity? = withContext(Dispatchers.IO) {
        dao.getActiveSessionOnce()
    }

    suspend fun getSessionByIdOnce(sessionId: Long): UmrahSessionEntity? = withContext(Dispatchers.IO) {
        dao.getSessionByIdOnce(sessionId)
    }

    suspend fun getChecklistForSessionOnce(sessionId: Long): List<UmrahChecklistEntity> = withContext(Dispatchers.IO) {
        dao.getChecklistForSessionOnce(sessionId)
    }

    suspend fun getRoundLogsForSessionOnce(sessionId: Long): List<UmrahRoundLogEntity> = withContext(Dispatchers.IO) {
        dao.getRoundLogsForSessionOnce(sessionId)
    }

    suspend fun startNewUmrah(): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val session = UmrahSessionEntity(
            startTime = now,
            currentStep = "IHRAM",
            currentTawafRound = 1,
            currentSaiRound = 0,
            isCompleted = false,
            ihramStartTime = now
        )
        val sessionId = dao.insertSession(session)

        // Pre-populate checklist items for Ihram, Tawaf, Sa'i, and Halq
        val initialItems = mutableListOf<UmrahChecklistEntity>()
        UmrahContentData.IHRAM_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "IHRAM", itemIndex = idx, itemTextBn = text))
        }
        UmrahContentData.TAWAF_PREP_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "TAWAF_PREP", itemIndex = idx, itemTextBn = text))
        }
        UmrahContentData.TAWAF_ROUND_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "TAWAF_ROUND", itemIndex = idx, itemTextBn = text))
        }
        UmrahContentData.TAWAF_FINAL_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "TAWAF_FINAL", itemIndex = idx, itemTextBn = text))
        }
        UmrahContentData.SAI_PREP_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "SAI_PREP", itemIndex = idx, itemTextBn = text))
        }
        UmrahContentData.SAI_ROUND_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "SAI_ROUND", itemIndex = idx, itemTextBn = text))
        }
        UmrahContentData.SAI_FINAL_CHECKLIST.forEachIndexed { idx, text ->
            initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "SAI_FINAL", itemIndex = idx, itemTextBn = text))
        }
        initialItems.add(UmrahChecklistEntity(sessionId = sessionId, stepKey = "HALQ", itemIndex = 0, itemTextBn = "হলক / কসর সম্পন্ন করা হয়েছে"))

        dao.insertChecklistItems(initialItems)
        sessionId
    }

    suspend fun updateChecklistItem(sessionId: Long, stepKey: String, itemIndex: Int, isChecked: Boolean) = withContext(Dispatchers.IO) {
        val checkedAt = if (isChecked) System.currentTimeMillis() else null
        dao.updateChecklistItem(sessionId, stepKey, itemIndex, isChecked, checkedAt)
    }

    suspend fun updateSession(session: UmrahSessionEntity) = withContext(Dispatchers.IO) {
        dao.updateSession(session)
    }

    suspend fun logRoundCompletion(sessionId: Long, ritualType: String, roundNumber: Int) = withContext(Dispatchers.IO) {
        dao.insertRoundLog(
            UmrahRoundLogEntity(
                sessionId = sessionId,
                ritualType = ritualType,
                roundNumber = roundNumber,
                completedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        dao.deleteSession(sessionId)
    }

    companion object {
        @Volatile
        private var INSTANCE: UmrahRepository? = null

        fun getInstance(context: Context): UmrahRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = UmrahRepository(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
