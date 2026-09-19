package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SearchMatch
import com.example.data.model.Surah
import com.example.data.model.Edition
import com.example.data.repository.QuranRepository
import com.example.data.repository.SettingsRepository
import com.example.ui.state.UiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class SearchResultItemType {
    data class SurahItem(
        val surah: Surah,
        val banglaName: String,
        val banglaMeaning: String
    ) : SearchResultItemType()

    data class AyahItem(
        val match: SearchMatch,
        val arabicText: String? = null,
        val banglaText: String? = null
    ) : SearchResultItemType()
}

class SearchViewModel(
    private val repository: QuranRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<UiState<List<SearchResultItemType>>>(UiState.Success(emptyList()))
    val uiState: StateFlow<UiState<List<SearchResultItemType>>> = _uiState.asStateFlow()

    val theme: StateFlow<String> = settingsRepository.themeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "Light"
        )

    val arabicFont: StateFlow<String> = settingsRepository.arabicFontNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = "Me Quran"
        )

    init {
        setupSearch()
    }

    fun onQueryChange(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _uiState.value = UiState.Success(emptyList())
        }
    }

    @OptIn(FlowPreview::class)
    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(500)
                .distinctUntilChanged()
                .filter { it.isNotBlank() }
                .collect { query ->
                    performSearch(query)
                }
        }
    }

    private fun convertBanglaDigitsToEnglish(input: String): String {
        val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
        for (i in 0..9) {
            result = result.replace(banglaDigits[i], englishDigits[i])
        }
        return result
    }

    private fun tryParseAyahQuery(query: String): Pair<Int, Int>? {
        val regex = Regex("""^(\d+)\s*[:/\-\.\s]\s*(\d+)$""")
        val matchResult = regex.find(query) ?: return null
        val surahNum = matchResult.groupValues[1].toIntOrNull() ?: return null
        val ayahNum = matchResult.groupValues[2].toIntOrNull() ?: return null
        if (surahNum in 1..114) {
            return Pair(surahNum, ayahNum)
        }
        return null
    }

    private fun containsArabic(text: String): Boolean {
        return text.any { char ->
            val block = Character.UnicodeBlock.of(char)
            block == Character.UnicodeBlock.ARABIC ||
            block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A ||
            block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B ||
            block == Character.UnicodeBlock.ARABIC_SUPPLEMENT
        }
    }

    private fun cleanArabicText(text: String): String {
        // Remove Arabic diacritics/tashkeel and waqf/pause signs
        val diacriticsRegex = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED]")
        return text.replace(diacriticsRegex, "")
    }

    private fun matchesSurah(surah: Surah, query: String, banglaName: String, banglaMeaning: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return false
        
        if (surah.englishName.lowercase().contains(q) || 
            surah.englishNameTranslation.lowercase().contains(q)) {
            return true
        }
        
        val cleanEnglishName = surah.englishName.lowercase()
            .replace("al-", "")
            .replace("ar-", "")
            .replace("an-", "")
            .replace("at-", "")
            .replace("ash-", "")
            .replace("az-", "")
            .replace("ad-", "")
            .replace(" ", "")
            .replace("-", "")
        val cleanQuery = q
            .replace("al-", "")
            .replace("ar-", "")
            .replace("an-", "")
            .replace("at-", "")
            .replace("ash-", "")
            .replace("az-", "")
            .replace("ad-", "")
            .replace(" ", "")
            .replace("-", "")
            
        if (cleanEnglishName.contains(cleanQuery) && cleanQuery.length >= 2) {
            return true
        }
        
        if (banglaName.contains(q) || banglaMeaning.contains(q)) {
            return true
        }
        
        val cleanBanglaName = banglaName
            .replace("আল ", "")
            .replace("আর ", "")
            .replace("আন ", "")
            .replace("আত ", "")
            .replace("আশ ", "")
            .replace("আয ", "")
            .replace("আদ ", "")
            .replace("-", "")
            .replace(" ", "")
        val cleanBanglaQuery = q
            .replace("আল ", "")
            .replace("আর ", "")
            .replace("আন ", "")
            .replace("আত ", "")
            .replace("আশ ", "")
            .replace("আয ", "")
            .replace("আদ ", "")
            .replace("-", "")
            .replace(" ", "")
            
        if (cleanBanglaName.contains(cleanBanglaQuery) && cleanBanglaQuery.length >= 2) {
            return true
        }
        
        return false
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val results = mutableListOf<SearchResultItemType>()
                val convertedQuery = convertBanglaDigitsToEnglish(query.trim())
                
                // 1. Check if the query is a specific Ayah query (e.g. 2:25, ২:২৫)
                val isAyahQuery = tryParseAyahQuery(convertedQuery)
                if (isAyahQuery != null) {
                    val (surahNum, ayahNum) = isAyahQuery
                    val surahs = repository.getSurahs()
                    val matchedSurah = surahs.find { it.number == surahNum }
                    if (matchedSurah != null && ayahNum in 1..matchedSurah.numberOfAyahs) {
                        val surahDetails = repository.getSurahDetailsCombined(surahNum)
                        val matchedAyah = surahDetails.find { it.numberInSurah == ayahNum }
                        if (matchedAyah != null) {
                            val searchMatch = SearchMatch(
                                number = matchedAyah.number,
                                text = matchedAyah.bengaliText,
                                edition = Edition(
                                    identifier = "bn.bengali",
                                    language = "bn",
                                    name = "Bengali",
                                    englishName = "Bengali",
                                    format = "text",
                                    type = "translation"
                                ),
                                surah = matchedSurah,
                                numberInSurah = matchedAyah.numberInSurah
                            )
                            results.add(SearchResultItemType.AyahItem(
                                match = searchMatch,
                                arabicText = matchedAyah.arabicText,
                                banglaText = matchedAyah.bengaliText
                            ))
                        }
                    }
                } else {
                    // 2. Search Surah Names
                    val surahs = repository.getSurahs()
                    for (surah in surahs) {
                        val surahPair = com.example.data.QuranData.surahNames.find { it.first == surah.number }
                        val banglaSurahName = surahPair?.second?.first ?: ""
                        val banglaSurahMeaning = surahPair?.second?.second ?: ""
                        
                        if (matchesSurah(surah, query, banglaSurahName, banglaSurahMeaning)) {
                            results.add(SearchResultItemType.SurahItem(surah, banglaSurahName, banglaSurahMeaning))
                        }
                    }
                    
                    // If the query is a single number (e.g. "2" or "২"), find and add that Surah
                    val singleSurahNum = query.trim().toIntOrNull() ?: convertedQuery.toIntOrNull()
                    if (singleSurahNum != null && singleSurahNum in 1..114) {
                        val matchedSurah = surahs.find { it.number == singleSurahNum }
                        if (matchedSurah != null) {
                            val alreadyAdded = results.any { 
                                it is SearchResultItemType.SurahItem && it.surah.number == singleSurahNum 
                            }
                            if (!alreadyAdded) {
                                val surahPair = com.example.data.QuranData.surahNames.find { it.first == singleSurahNum }
                                val banglaSurahName = surahPair?.second?.first ?: ""
                                val banglaSurahMeaning = surahPair?.second?.second ?: ""
                                results.add(SearchResultItemType.SurahItem(matchedSurah, banglaSurahName, banglaSurahMeaning))
                            }
                        }
                    }

                    // 3. Search verses via API for text matching
                    try {
                        val tanzilStyle = settingsRepository.tanzilTextStyleFlow.first()
                        val isArabicQuery = containsArabic(query)
                        val response = if (isArabicQuery) {
                            val cleanedQuery = cleanArabicText(query)
                            repository.searchQuran(cleanedQuery, "quran-simple-clean")
                        } else {
                            repository.searchQuran(query)
                        }

                        // For better performance, we don't load the full combined surah details for search results.
                        response.matches.forEach { match ->
                            results.add(SearchResultItemType.AyahItem(
                                match = match,
                                arabicText = if (isArabicQuery) match.text else null,
                                banglaText = if (!isArabicQuery) match.text else null
                            ))
                        }
                    } catch (e: Exception) {
                        if (results.isEmpty()) {
                            throw e
                        }
                    }
                }
                
                _uiState.value = UiState.Success(results)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Search failed")
            }
        }
    }
}
