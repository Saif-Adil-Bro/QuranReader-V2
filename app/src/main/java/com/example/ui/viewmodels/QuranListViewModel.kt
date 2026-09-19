package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.QuranData
import com.example.data.model.Surah
import com.example.data.repository.QuranRepository
import com.example.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuranListViewModel(
    private val repository: QuranRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Surah>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Surah>>> = _uiState.asStateFlow()

    private var allSurahs: List<Surah> = emptyList()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    init {
        loadSurahs()
    }

    fun loadSurahs() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val surahs = repository.getSurahs()
                allSurahs = surahs
                _uiState.value = UiState.Success(surahs)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "An unknown error occurred")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterSurahs(query)
    }

    private fun filterSurahs(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.value = UiState.Success(allSurahs)
        } else {
            val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670-\\u06D6\\u06DC-\\u06ED]")
            val normalizedQuery = trimmedQuery.replace(diacriticsRegex, "").lowercase()

            val filtered = allSurahs.filter { surah ->
                val bengaliPair = QuranData.surahNames.find { it.first == surah.number }?.second
                val bengaliName = bengaliPair?.first ?: ""
                val bengaliMeaning = bengaliPair?.second ?: ""
                val arabicNameRaw = surah.name ?: ""
                val normalizedArabicName = arabicNameRaw.replace(diacriticsRegex, "")

                surah.englishName.contains(trimmedQuery, ignoreCase = true) ||
                surah.englishNameTranslation.contains(trimmedQuery, ignoreCase = true) ||
                arabicNameRaw.contains(trimmedQuery, ignoreCase = true) ||
                (normalizedQuery.isNotEmpty() && normalizedArabicName.lowercase().contains(normalizedQuery)) ||
                bengaliName.contains(trimmedQuery, ignoreCase = true) ||
                bengaliMeaning.contains(trimmedQuery, ignoreCase = true) ||
                surah.number.toString().contains(trimmedQuery) ||
                com.example.utils.DateUtil.toBengaliNumerals(surah.number).contains(trimmedQuery)
            }
            _uiState.value = UiState.Success(filtered)
        }
    }
}
