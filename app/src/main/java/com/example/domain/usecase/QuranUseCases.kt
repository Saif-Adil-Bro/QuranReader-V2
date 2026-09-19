package com.example.domain.usecase

import com.example.data.model.CombinedAyah
import com.example.data.model.Surah
import com.example.data.repository.QuranRepository

class GetSurahsUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(): List<Surah> {
        return repository.getSurahs()
    }
}

class GetSurahDetailsUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(surahNumber: Int, arabicEdition: String = "quran-uthmani"): List<CombinedAyah> {
        return repository.getSurahDetailsCombined(surahNumber, arabicEdition)
    }
}

class GetPageDetailsUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(pageNumber: Int): List<CombinedAyah> {
        return repository.getPageCombined(pageNumber)
    }
}

class GetJuzDetailsUseCase(private val repository: QuranRepository) {
    suspend operator fun invoke(juzNumber: Int): List<CombinedAyah> {
        return repository.getJuzCombined(juzNumber)
    }
}
