package com.example.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.dao.BookmarkDao
import com.example.data.local.dao.MemorizedPageDao
import com.example.data.repository.AudioRepository
import com.example.data.repository.QuranRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.AiRepository
import com.example.domain.usecase.GetPageDetailsUseCase
import com.example.domain.usecase.GetSurahDetailsUseCase

/**
 * Factory for creating ViewModels with constructor parameters.
 */
class AppViewModelFactory(
    private val quranRepository: QuranRepository,
    private val settingsRepository: SettingsRepository,
    private val audioRepository: AudioRepository,
    private val aiRepository: AiRepository,
    private val bookmarkDao: BookmarkDao,
    private val memorizedPageDao: MemorizedPageDao,
    private val mushafRepository: com.example.data.repository.MushafRepository,
    private val postsRepository: com.example.data.repository.PostsRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(QuranListViewModel::class.java) -> {
                QuranListViewModel(quranRepository) as T
            }
            modelClass.isAssignableFrom(SurahDetailViewModel::class.java) -> {
                SurahDetailViewModel(quranRepository, settingsRepository, aiRepository, audioRepository, bookmarkDao) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(settingsRepository, bookmarkDao, quranRepository, audioRepository) as T
            }
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> {
                SearchViewModel(quranRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> {
                HomeViewModel(
                    settingsRepository = settingsRepository,
                    quranRepository = quranRepository,
                    mushafRepository = mushafRepository,
                    bookmarkDao = bookmarkDao,
                    audioRepository = audioRepository
                ) as T
            }
            modelClass.isAssignableFrom(ReadingModeViewModel::class.java) -> {
                ReadingModeViewModel(GetSurahDetailsUseCase(quranRepository), settingsRepository) as T
            }
            modelClass.isAssignableFrom(HafeziModeViewModel::class.java) -> {
                HafeziModeViewModel(
                    getPageDetailsUseCase = GetPageDetailsUseCase(quranRepository),
                    audioRepository = audioRepository,
                    settingsRepository = settingsRepository,
                    bookmarkDao = bookmarkDao,
                    memorizedPageDao = memorizedPageDao
                ) as T
            }
            modelClass.isAssignableFrom(TajweedModeViewModel::class.java) -> {
                TajweedModeViewModel(
                    getPageDetailsUseCase = GetPageDetailsUseCase(quranRepository),
                    audioRepository = audioRepository,
                    settingsRepository = settingsRepository,
                    bookmarkDao = bookmarkDao,
                    memorizedPageDao = memorizedPageDao
                ) as T
            }
            modelClass.isAssignableFrom(MushafSelectionViewModel::class.java) -> {
                MushafSelectionViewModel(mushafRepository, settingsRepository) as T
            }
            modelClass.isAssignableFrom(MushafViewerViewModel::class.java) -> {
                MushafViewerViewModel(mushafRepository, settingsRepository, bookmarkDao, quranRepository, audioRepository) as T
            }
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> {
                SplashViewModel(quranRepository) as T
            }
            modelClass.isAssignableFrom(PostsViewModel::class.java) -> {
                val repo = postsRepository ?: throw IllegalArgumentException("postsRepository must not be null for PostsViewModel")
                PostsViewModel(repo) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
