package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CombinedAyah
import com.example.domain.usecase.GetSurahDetailsUseCase
import com.example.data.repository.SettingsRepository
import com.example.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReadingModeViewModel(
    private val getSurahDetailsUseCase: GetSurahDetailsUseCase,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<CombinedAyah>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<CombinedAyah>>> = _uiState.asStateFlow()

    val arabicFontSize: StateFlow<Float> = settingsRepository.arabicFontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 18f)

    val arabicFontName: StateFlow<String> = settingsRepository.arabicFontNameFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "Me Quran")

    val bengaliFontSize: StateFlow<Float> = settingsRepository.bengaliFontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 16f)

    val tanzilTextStyle: StateFlow<String> = settingsRepository.tanzilTextStyleFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "default-indopak")

    val theme: StateFlow<String> = settingsRepository.themeFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, "Light")

    val autoScrollSpeed: StateFlow<Float> = settingsRepository.autoScrollSpeedFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 1f)

    val showWaqfSigns: StateFlow<Boolean> = settingsRepository.showWaqfSignsFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    val arabicLineSpacing: StateFlow<Float> = settingsRepository.arabicLineSpacingFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 2.5f)

    fun loadSurah(surahNumber: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val style = settingsRepository.tanzilTextStyleFlow.first()
                val ayahs = getSurahDetailsUseCase(surahNumber, style)
                _uiState.value = UiState.Success(ayahs)
                settingsRepository.setLastReadSurah(surahNumber)
                settingsRepository.setLastReadMode("READING")
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load Surah details")
            }
        }
    }

    fun setTanzilTextStyle(style: String) {
        viewModelScope.launch { settingsRepository.setTanzilTextStyle(style) }
    }

    fun setArabicFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.setArabicFontSize(size) }
    }

    fun setBengaliFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.setBengaliFontSize(size) }
    }

    fun setTheme(newTheme: String) {
        viewModelScope.launch { settingsRepository.setTheme(newTheme) }
    }

    fun setAutoScrollSpeed(speed: Float) {
        viewModelScope.launch { settingsRepository.setAutoScrollSpeed(speed) }
    }

    fun setShowWaqfSigns(show: Boolean) {
        viewModelScope.launch { settingsRepository.setShowWaqfSigns(show) }
    }

    fun setArabicLineSpacing(spacing: Float) {
        viewModelScope.launch { settingsRepository.setArabicLineSpacing(spacing) }
    }
}
