package com.example.data.model

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.data.model.CombinedAyah

/**
 * Aspect Ratios for Quran Video Creator
 */
enum class VideoAspectRatio(val label: String, val width: Int, val height: Int, val ratioFloat: Float) {
    PORTRAIT_9_16("9:16 (Story / Reels)", 720, 1280, 9f / 16f),
    SQUARE_1_1("1:1 (Square Post)", 720, 720, 1f),
    LANDSCAPE_16_9("16:9 (Landscape)", 1280, 720, 16f / 9f)
}

/**
 * Text Animation Styles for Quran Video Creator
 */
enum class TextAnimationStyle(val label: String) {
    NONE("কোনো অ্যানিমেশন নেই"),
    FADE_IN("ফেড ইন (Fade In)"),
    FADE_OUT("ফেড আউট (Fade Out)"),
    SLIDE_UP("স্লাইড আপ (Slide Up)"),
    SLIDE_DOWN("স্লাইড ডাউন (Slide Down)")
}

/**
 * Background Overlay Level
 */
enum class BackgroundOverlay(val label: String, val alpha: Float) {
    NONE("কোনোটি নয়", 0.0f),
    LIGHT("হালকা (Light)", 0.25f),
    MEDIUM("মাঝারি (Medium)", 0.50f),
    DARK("গাঢ় (Dark)", 0.75f)
}

/**
 * Predefined Video Templates for Phase 1
 */
enum class QuranVideoTemplate(
    val id: String,
    val title: String,
    val description: String,
    val gradientColors: List<Color>,
    val arabicColor: Color,
    val translationColor: Color,
    val referenceColor: Color,
    val accentColor: Color,
    val defaultOverlay: BackgroundOverlay,
    val defaultAnimation: TextAnimationStyle
) {
    EMERALD_QURAN(
        id = "emerald_quran",
        title = "Emerald Quran",
        description = "পান্না সবুজ ও সোনালী আভার ক্লাসিক ইসলামিক থিম",
        gradientColors = listOf(Color(0xFF062B1D), Color(0xFF0F4D35), Color(0xFF041911)),
        arabicColor = Color(0xFFFAF9F5),
        translationColor = Color(0xFFE2E8F0),
        referenceColor = Color(0xFFF6D884),
        accentColor = Color(0xFF10B981),
        defaultOverlay = BackgroundOverlay.MEDIUM,
        defaultAnimation = TextAnimationStyle.FADE_IN
    ),
    ROYAL_NIGHT(
        id = "royal_night",
        title = "Royal Night",
        description = "রয়্যাল নেভি ব্লু ও চাঁদের স্নিগ্ধ গভীর থিম",
        gradientColors = listOf(Color(0xFF091428), Color(0xFF132448), Color(0xFF030712)),
        arabicColor = Color(0xFFFFFFFF),
        translationColor = Color(0xFFCBD5E1),
        referenceColor = Color(0xFF60A5FA),
        accentColor = Color(0xFF3B82F6),
        defaultOverlay = BackgroundOverlay.MEDIUM,
        defaultAnimation = TextAnimationStyle.FADE_IN
    ),
    CLASSIC_ISLAMIC(
        id = "classic_islamic",
        title = "Classic Islamic",
        description = "ঐতিহ্যবাহী গাঢ় কালার ও ভিন্টেজ গোল্ডেন বর্ডার",
        gradientColors = listOf(Color(0xFF181512), Color(0xFF2C241D), Color(0xFF0D0B09)),
        arabicColor = Color(0xFFFFFBEB),
        translationColor = Color(0xFFFEF3C7),
        referenceColor = Color(0xFFFBBF24),
        accentColor = Color(0xFFD97706),
        defaultOverlay = BackgroundOverlay.MEDIUM,
        defaultAnimation = TextAnimationStyle.FADE_IN
    ),
    MINIMAL_QURAN(
        id = "minimal_quran",
        title = "Minimal Quran",
        description = "মিনিমালিস্টিক ক্লীন ডার্ক ও উচ্চ পঠনযোগ্যতা",
        gradientColors = listOf(Color(0xFF111827), Color(0xFF1F2937), Color(0xFF030712)),
        arabicColor = Color(0xFFFFFFFF),
        translationColor = Color(0xFFE5E7EB),
        referenceColor = Color(0xFF9CA3AF),
        accentColor = Color(0xFF6B7280),
        defaultOverlay = BackgroundOverlay.LIGHT,
        defaultAnimation = TextAnimationStyle.SLIDE_UP
    ),
    GOLDEN_QURAN(
        id = "golden_quran",
        title = "Golden Quran",
        description = "মহিমান্বিত স্বর্ণালী আভা ও রাজকীয় ইসলামিক লুক",
        gradientColors = listOf(Color(0xFF2E1A03), Color(0xFF5A3406), Color(0xFF190E02)),
        arabicColor = Color(0xFFFFFDF5),
        translationColor = Color(0xFFFEF9C3),
        referenceColor = Color(0xFFFACC15),
        accentColor = Color(0xFFEAB308),
        defaultOverlay = BackgroundOverlay.MEDIUM,
        defaultAnimation = TextAnimationStyle.FADE_IN
    ),
    NATURE_QURAN(
        id = "nature_quran",
        title = "Nature Quran",
        description = "শান্ত ও মনোরম টিল-প্রকৃতির সবুজ আবহ",
        gradientColors = listOf(Color(0xFF042F2E), Color(0xFF0F766E), Color(0xFF021E1D)),
        arabicColor = Color(0xFFF0FDFA),
        translationColor = Color(0xFFCCFBF1),
        referenceColor = Color(0xFF5EEAD4),
        accentColor = Color(0xFF14B8A6),
        defaultOverlay = BackgroundOverlay.LIGHT,
        defaultAnimation = TextAnimationStyle.FADE_IN
    )
}

/**
 * Prepared Ayah Audio Model holding verified local file and duration
 */
data class PreparedAyahAudio(
    val ayahNumber: Int,
    val numberInSurah: Int,
    val localFile: java.io.File,
    val durationMs: Long,
    val isReady: Boolean = true,
    val arabicPreview: String = ""
)

/**
 * Audio Preparation State for Step 2
 */
data class AudioPreparationState(
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val progress: Float = 0f,
    val currentAyahNumber: Int = 0,
    val preparedAudios: List<PreparedAyahAudio> = emptyList(),
    val totalDurationMs: Long = 0L,
    val error: String? = null
) {
    val isPreparing: Boolean get() = isDownloading
    val isPrepared: Boolean get() = isDownloaded
    val errorMessage: String? get() = error
}

/**
 * Quran Video Payload for Creation & Rendering
 */
data class QuranVideoConfig(
    val surahNumber: Int = 1,
    val surahName: String = "আল-ফাতিহা",
    val ayahStart: Int = 1,
    val ayahEnd: Int = 1,
    val selectedAyahs: List<CombinedAyah> = emptyList(),
    val qariId: String = "mishari_alafasy",
    val qariName: String = "মিশারি রাশিদ আল-আফাসী",
    val template: QuranVideoTemplate = QuranVideoTemplate.EMERALD_QURAN,
    val aspectRatio: VideoAspectRatio = VideoAspectRatio.PORTRAIT_9_16,
    val backgroundPresetName: String? = null,
    val customImageUri: String? = null,
    val overlay: BackgroundOverlay = BackgroundOverlay.MEDIUM,
    val overlayColor: Color = Color.Black,
    val arabicFontSize: Float = 28f,
    val arabicLineSpacing: Float = 1.8f,
    val translationFontSize: Float = 16f,
    val translationLineSpacing: Float = 1.3f,
    val arabicFontName: String = "Noorehira",
    val bengaliFontName: String = "SolaimanLipi",
    val showBanglaTranslation: Boolean = true,
    val showReference: Boolean = true,
    val showLogo: Boolean = true,
    val showCredit: Boolean = true,
    val showWaqfSigns: Boolean = false,
    val includeBismillah: Boolean = false,
    val animationStyle: TextAnimationStyle = TextAnimationStyle.FADE_IN,
    val logoText: String = "Quran READER",
    val creditText: String = "MuslimsLibrary"
)
