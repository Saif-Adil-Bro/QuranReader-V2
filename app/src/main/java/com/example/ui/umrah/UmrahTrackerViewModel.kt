package com.example.ui.umrah

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.umrah.UmrahChecklistEntity
import com.example.data.umrah.UmrahRepository
import com.example.data.umrah.UmrahRoundLogEntity
import com.example.data.umrah.UmrahSessionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UmrahUiState(
    val activeSession: UmrahSessionEntity? = null,
    val checklistItems: List<UmrahChecklistEntity> = emptyList(),
    val roundLogs: List<UmrahRoundLogEntity> = emptyList(),
    val isLoading: Boolean = false
)

class UmrahTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UmrahRepository.getInstance(application)

    private val _uiState = MutableStateFlow(UmrahUiState(isLoading = true))
    val uiState: StateFlow<UmrahUiState> = _uiState.asStateFlow()

    val completedSessions: StateFlow<List<UmrahSessionEntity>> = repository.allCompletedSessionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        observeActiveSession()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observeActiveSession() {
        viewModelScope.launch {
            repository.latestSessionFlow.flatMapLatest { session ->
                if (session != null) {
                    combine(
                        repository.getChecklistForSession(session.id),
                        repository.getRoundLogsForSession(session.id)
                    ) { checklist, logs ->
                        UmrahUiState(
                            activeSession = session,
                            checklistItems = checklist,
                            roundLogs = logs,
                            isLoading = false
                        )
                    }
                } else {
                    flowOf(
                        UmrahUiState(
                            activeSession = null,
                            checklistItems = emptyList(),
                            roundLogs = emptyList(),
                            isLoading = false
                        )
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun startUmrah() {
        viewModelScope.launch {
            repository.startNewUmrah()
        }
    }

    fun toggleChecklistItem(stepKey: String, itemIndex: Int, isChecked: Boolean) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.updateChecklistItem(session.id, stepKey, itemIndex, isChecked)
        }
    }

    fun completeIhramAndGoToTawaf() {
        val session = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateSession(
                session.copy(
                    currentStep = "TAWAF",
                    currentTawafRound = 1,
                    ihramEndTime = now,
                    tawafStartTime = now
                )
            )
        }
    }

    fun setTawafRound(round: Int) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.updateSession(session.copy(currentTawafRound = round.coerceIn(1, 7)))
        }
    }

    fun completeTawafRound(round: Int) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.logRoundCompletion(session.id, "TAWAF", round)
            if (round < 7) {
                repository.updateSession(session.copy(currentTawafRound = round + 1))
            }
        }
    }

    fun completeTawafAndGoToSai() {
        val session = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.logRoundCompletion(session.id, "TAWAF", 7)
            repository.updateSession(
                session.copy(
                    currentStep = "SAI",
                    currentSaiRound = 0,
                    tawafEndTime = now,
                    saiStartTime = now
                )
            )
        }
    }

    fun setSaiRound(round: Int) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            repository.updateSession(session.copy(currentSaiRound = round.coerceIn(0, 7)))
        }
    }

    fun completeSaiRound(round: Int) {
        val session = _uiState.value.activeSession ?: return
        viewModelScope.launch {
            if (round in 1..7) {
                repository.logRoundCompletion(session.id, "SAI", round)
            }
            if (round < 7) {
                repository.updateSession(session.copy(currentSaiRound = round + 1))
            }
        }
    }

    fun completeSaiAndGoToHalq() {
        val session = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.logRoundCompletion(session.id, "SAI", 7)
            repository.updateSession(
                session.copy(
                    currentStep = "HALQ",
                    saiEndTime = now,
                    halqStartTime = now
                )
            )
        }
    }

    fun completeUmrah() {
        val session = _uiState.value.activeSession ?: return
        val now = System.currentTimeMillis()
        viewModelScope.launch {
            repository.updateSession(
                session.copy(
                    currentStep = "COMPLETED",
                    isCompleted = true,
                    endTime = now,
                    halqEndTime = now
                )
            )
        }
    }

    fun resetOrStartNewUmrah() {
        viewModelScope.launch {
            repository.startNewUmrah()
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
}
