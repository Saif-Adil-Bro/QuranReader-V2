package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.QuranRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SplashViewModel(
    private val quranRepository: QuranRepository
) : ViewModel() {

    private val _loadingState = MutableStateFlow<SplashLoadingState>(SplashLoadingState.Logo)
    val loadingState: StateFlow<SplashLoadingState> = _loadingState

    init {
        startLoading()
    }

    private fun startLoading() {
        viewModelScope.launch {
            // Phase 1: Show Logo/Splash for 1.5 seconds
            delay(1500)
            
            // Phase 2: Change state to Loading Animation
            _loadingState.value = SplashLoadingState.Loading
            
            val startTime = System.currentTimeMillis()
            try {
                // Fetch surahs list to cache in memory/files
                quranRepository.getSurahs()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // Ensure loading animation is visible for at least 1.2s to prevent jarring transitions
                val elapsedTime = System.currentTimeMillis() - startTime
                val remaining = 1200L - elapsedTime
                if (remaining > 0) {
                    delay(remaining)
                }
                // Phase 3: Loading Complete, ready to show main screen
                _loadingState.value = SplashLoadingState.Complete
            }
        }
    }
}

sealed class SplashLoadingState {
    object Logo : SplashLoadingState()
    object Loading : SplashLoadingState()
    object Complete : SplashLoadingState()
}
