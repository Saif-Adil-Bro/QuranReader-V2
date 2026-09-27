package com.example.data.umrah

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "umrah_sessions")
data class UmrahSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val currentStep: String = "NOT_STARTED", // NOT_STARTED, IHRAM, TAWAF, SAI, HALQ, COMPLETED
    val currentTawafRound: Int = 1,          // 1..7
    val currentSaiRound: Int = 0,            // 0 = ready, 1..7
    val isCompleted: Boolean = false,
    val ihramStartTime: Long? = null,
    val ihramEndTime: Long? = null,
    val tawafStartTime: Long? = null,
    val tawafEndTime: Long? = null,
    val saiStartTime: Long? = null,
    val saiEndTime: Long? = null,
    val halqStartTime: Long? = null,
    val halqEndTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "umrah_checklist_items")
data class UmrahChecklistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val stepKey: String, // IHRAM, TAWAF_PREP, TAWAF_ROUND, TAWAF_FINAL, SAI_PREP, SAI_ROUND, SAI_FINAL, HALQ
    val itemIndex: Int,
    val itemTextBn: String,
    val isChecked: Boolean = false,
    val checkedAt: Long? = null
)

@Entity(tableName = "umrah_round_logs")
data class UmrahRoundLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sessionId: Long,
    val ritualType: String, // TAWAF, SAI
    val roundNumber: Int,   // 1..7
    val completedAt: Long = System.currentTimeMillis()
)
