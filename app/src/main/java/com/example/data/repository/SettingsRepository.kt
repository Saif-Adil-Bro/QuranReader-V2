package com.example.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Repository for handling app-wide settings like translation toggles.
 */
class SettingsRepository(val context: Context) {

    private val SHOW_TRANSLATION_KEY = booleanPreferencesKey("show_translation")
    private val SHOW_TRANSLITERATION_KEY = booleanPreferencesKey("show_transliteration")
    private val SHOW_TAJWEED_KEY = booleanPreferencesKey("show_tajweed")
    private val LAST_READ_SURAH_KEY = intPreferencesKey("last_read_surah")
    private val LAST_READ_PAGE_KEY = intPreferencesKey("last_read_page")
    private val LAST_READ_MUSHAF_ID_KEY = stringPreferencesKey("last_read_mushaf_id")
    private val LAST_READ_MUSHAF_PAGE_KEY = intPreferencesKey("last_read_mushaf_page")
    private val LAST_READ_MODE_KEY = stringPreferencesKey("last_read_mode")
    private val LAST_READ_AYAH_KEY = intPreferencesKey("last_read_ayah")
    
    // Reading Mode Settings
    private val ARABIC_FONT_SIZE_KEY = floatPreferencesKey("arabic_font_size")
    private val BENGALI_FONT_SIZE_KEY = floatPreferencesKey("bengali_font_size")
    private val THEME_KEY = stringPreferencesKey("theme")
    private val AUTO_SCROLL_SPEED_KEY = floatPreferencesKey("auto_scroll_speed")
    private val ARABIC_FONT_NAME_KEY = stringPreferencesKey("arabic_font_name")
    private val BENGALI_FONT_NAME_KEY = stringPreferencesKey("bengali_font_name")
    private val TANZIL_TEXT_STYLE_KEY = stringPreferencesKey("tanzil_text_style")
    private val ARABIC_LINE_SPACING_KEY = floatPreferencesKey("arabic_line_spacing")
    
    // Hafezi Mode Settings
    private val REPEAT_COUNT_KEY = intPreferencesKey("repeat_count")
    private val SHOW_WAQF_SIGNS_KEY = booleanPreferencesKey("show_waqf_signs")
    private val HAS_ASKED_DOWNLOAD_PROMPT_KEY = booleanPreferencesKey("has_asked_download_prompt")
    private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
    
    // Tafsir Settings
    private val SELECTED_TAFSIR_IDS_KEY = stringSetPreferencesKey("selected_tafsir_ids")
    private val SELECTED_TRANSLATION_IDS_KEY = stringSetPreferencesKey("selected_translation_ids")
    
    // Qari Settings
    private val SELECTED_QARI_ID_KEY = stringPreferencesKey("selected_qari_id")
    private val MUSHAF_SCROLL_DIRECTION_KEY = stringPreferencesKey("mushaf_scroll_direction")
    private val MUSHAF_PAGE_HEIGHT_SCALE_KEY = floatPreferencesKey("mushaf_page_height_scale")
    private val DEFAULT_MUSHAF_ID_KEY = stringPreferencesKey("default_mushaf_id")
    private val HIJRI_OFFSET_KEY = intPreferencesKey("hijri_offset")
    private val SAHRI_OFFSET_KEY = intPreferencesKey("sahri_offset_minutes")
    private val IFTAR_OFFSET_KEY = intPreferencesKey("iftar_offset_minutes")

    val showTranslationFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_TRANSLATION_KEY] ?: true }

    val showTransliterationFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_TRANSLITERATION_KEY] ?: false }
    val showTajweedFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_TAJWEED_KEY] ?: true }

    val lastReadSurahFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LAST_READ_SURAH_KEY] ?: 1 }

    val lastReadPageFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LAST_READ_PAGE_KEY] ?: 1 }

    val lastReadMushafIdFlow: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[LAST_READ_MUSHAF_ID_KEY] }

    val lastReadMushafPageFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LAST_READ_MUSHAF_PAGE_KEY] ?: 1 }

    val lastReadModeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[LAST_READ_MODE_KEY] ?: "DETAIL" }

    val lastReadAyahFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[LAST_READ_AYAH_KEY] ?: 1 }

    val arabicFontSizeFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[ARABIC_FONT_SIZE_KEY] ?: 20f }

    val bengaliFontSizeFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[BENGALI_FONT_SIZE_KEY] ?: 16f }

    val arabicFontNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[ARABIC_FONT_NAME_KEY] ?: "Me Quran" }

    val bengaliFontNameFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[BENGALI_FONT_NAME_KEY] ?: "SolaimanLipi" }

    val tanzilTextStyleFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[TANZIL_TEXT_STYLE_KEY] ?: "default-indopak" }

    val arabicLineSpacingFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> (preferences[ARABIC_LINE_SPACING_KEY] ?: 2.0f).coerceAtLeast(2.0f) }

    val themeFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[THEME_KEY] ?: "System" }

    val autoScrollSpeedFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[AUTO_SCROLL_SPEED_KEY] ?: 1f }

    val repeatCountFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[REPEAT_COUNT_KEY] ?: 1 }

    val showWaqfSignsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[SHOW_WAQF_SIGNS_KEY] ?: true }

    val hasAskedDownloadPromptFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[HAS_ASKED_DOWNLOAD_PROMPT_KEY] ?: false }

    val selectedTafsirIdsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_TAFSIR_IDS_KEY] ?: setOf("164") }

    val selectedTranslationIdsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_TRANSLATION_IDS_KEY] ?: setOf("161") }

    val selectedQariIdFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[SELECTED_QARI_ID_KEY] ?: "ar.alafasy" }

    val mushafScrollDirectionFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[MUSHAF_SCROLL_DIRECTION_KEY] ?: "Horizontal" }

    val mushafPageHeightScaleFlow: Flow<Float> = context.dataStore.data
        .map { preferences -> preferences[MUSHAF_PAGE_HEIGHT_SCALE_KEY] ?: 1.2f }

    val defaultMushafIdFlow: Flow<String> = context.dataStore.data
        .map { preferences -> preferences[DEFAULT_MUSHAF_ID_KEY] ?: "imdadia_hafezi" }

    val hijriOffsetFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[HIJRI_OFFSET_KEY] ?: 0 }

    val keepScreenOnFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[KEEP_SCREEN_ON_KEY] ?: false }

    val sahriOffsetFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[SAHRI_OFFSET_KEY] ?: -3 }

    val iftarOffsetFlow: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[IFTAR_OFFSET_KEY] ?: 0 }

    private val _globalHijriOffsetFlow = MutableStateFlow(0)
    val globalHijriOffsetFlow: StateFlow<Int> = _globalHijriOffsetFlow.asStateFlow()
    
    val combinedHijriOffsetFlow: Flow<Int> = hijriOffsetFlow.combine(globalHijriOffsetFlow) { local, global ->
        local + global
    }

    init {
        fetchGlobalHijriOffset()
    }

    private fun fetchGlobalHijriOffset() {
        try {
            val remoteConfig = Firebase.remoteConfig
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = 3600
            }
            remoteConfig.setConfigSettingsAsync(configSettings)
            remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val globalOffset = remoteConfig.getLong("global_hijri_offset").toInt()
                    _globalHijriOffsetFlow.value = globalOffset
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun setShowTranslation(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[SHOW_TRANSLATION_KEY] = show }
    }

    suspend fun setShowTransliteration(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[SHOW_TRANSLITERATION_KEY] = show }
    }
    suspend fun setShowTajweed(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[SHOW_TAJWEED_KEY] = show }
    }

    suspend fun setLastReadSurah(surahNumber: Int) {
        context.dataStore.edit { preferences -> preferences[LAST_READ_SURAH_KEY] = surahNumber }
    }

    suspend fun setLastReadPage(pageNumber: Int) {
        context.dataStore.edit { preferences -> preferences[LAST_READ_PAGE_KEY] = pageNumber }
    }

    suspend fun setLastReadMushaf(mushafId: String, pageNumber: Int) {
        context.dataStore.edit { preferences ->
            preferences[LAST_READ_MUSHAF_ID_KEY] = mushafId
            preferences[LAST_READ_MUSHAF_PAGE_KEY] = pageNumber
        }
    }

    suspend fun setLastReadMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_READ_MODE_KEY] = mode
        }
    }

    suspend fun setLastReadAyah(ayahNumber: Int) {
        context.dataStore.edit { preferences -> preferences[LAST_READ_AYAH_KEY] = ayahNumber }
    }

    suspend fun setArabicFontSize(size: Float) {
        context.dataStore.edit { preferences -> preferences[ARABIC_FONT_SIZE_KEY] = size }
    }

    suspend fun setBengaliFontSize(size: Float) {
        context.dataStore.edit { preferences -> preferences[BENGALI_FONT_SIZE_KEY] = size }
    }

    suspend fun setArabicFontName(fontName: String) {
        context.dataStore.edit { preferences -> preferences[ARABIC_FONT_NAME_KEY] = fontName }
    }

    suspend fun setBengaliFontName(fontName: String) {
        context.dataStore.edit { preferences -> preferences[BENGALI_FONT_NAME_KEY] = fontName }
    }

    suspend fun setTanzilTextStyle(style: String) {
        context.dataStore.edit { preferences -> preferences[TANZIL_TEXT_STYLE_KEY] = style }
    }

    suspend fun setArabicLineSpacing(spacing: Float) {
        context.dataStore.edit { preferences -> preferences[ARABIC_LINE_SPACING_KEY] = spacing.coerceAtLeast(2.0f) }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences -> preferences[THEME_KEY] = theme }
    }

    suspend fun setAutoScrollSpeed(speed: Float) {
        context.dataStore.edit { preferences -> preferences[AUTO_SCROLL_SPEED_KEY] = speed }
    }

    suspend fun setRepeatCount(count: Int) {
        context.dataStore.edit { preferences -> preferences[REPEAT_COUNT_KEY] = count }
    }

    suspend fun setShowWaqfSigns(show: Boolean) {
        context.dataStore.edit { preferences -> preferences[SHOW_WAQF_SIGNS_KEY] = show }
    }

    suspend fun setHasAskedDownloadPrompt(hasAsked: Boolean) {
        context.dataStore.edit { preferences -> preferences[HAS_ASKED_DOWNLOAD_PROMPT_KEY] = hasAsked }
    }

    suspend fun setSelectedTafsirIds(tafsirIds: Set<String>) {
        context.dataStore.edit { preferences -> preferences[SELECTED_TAFSIR_IDS_KEY] = tafsirIds }
    }

    suspend fun setSelectedQariId(qariId: String) {
        context.dataStore.edit { preferences -> preferences[SELECTED_QARI_ID_KEY] = qariId }
    }

    suspend fun setMushafScrollDirection(direction: String) {
        context.dataStore.edit { preferences -> preferences[MUSHAF_SCROLL_DIRECTION_KEY] = direction }
    }

    suspend fun setMushafPageHeightScale(scale: Float) {
        context.dataStore.edit { preferences -> preferences[MUSHAF_PAGE_HEIGHT_SCALE_KEY] = scale }
    }

    suspend fun setDefaultMushafId(mushafId: String) {
        context.dataStore.edit { preferences -> preferences[DEFAULT_MUSHAF_ID_KEY] = mushafId }
    }

    suspend fun setHijriOffset(offset: Int) {
        context.dataStore.edit { preferences -> preferences[HIJRI_OFFSET_KEY] = offset }
    }

    suspend fun setKeepScreenOn(keep: Boolean) {
        context.dataStore.edit { preferences -> preferences[KEEP_SCREEN_ON_KEY] = keep }
    }

    suspend fun setSahriOffset(offsetMinutes: Int) {
        context.dataStore.edit { preferences -> preferences[SAHRI_OFFSET_KEY] = offsetMinutes }
    }

    suspend fun setIftarOffset(offsetMinutes: Int) {
        context.dataStore.edit { preferences -> preferences[IFTAR_OFFSET_KEY] = offsetMinutes }
    }

    fun getMushafOffset(mushafId: String): Flow<Int?> {
        return context.dataStore.data.map { preferences ->
            preferences[intPreferencesKey("offset_$mushafId")]
        }
    }

    suspend fun setMushafOffset(mushafId: String, offset: Int) {
        context.dataStore.edit { preferences ->
            preferences[intPreferencesKey("offset_$mushafId")] = offset
        }

    }
    suspend fun setSelectedTranslationIds(ids: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_TRANSLATION_IDS_KEY] = ids
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
