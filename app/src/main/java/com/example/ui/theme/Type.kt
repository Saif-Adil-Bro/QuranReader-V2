package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.font.Font as ResourceFont
import androidx.compose.ui.text.googlefonts.Font as GoogleFontFile
import com.example.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)


val meQuranFont = FontFamily(ResourceFont(R.font.me_quran))
val pdmsSaleemFont = FontFamily(ResourceFont(R.font.pdms_saleem))
val noorehiraFont = FontFamily(ResourceFont(R.font.noorehira))


val uthmanTahaFont = FontFamily(
    ResourceFont(R.font.uthman_taha)
)

val amiriQuranFont = FontFamily(
    ResourceFont(R.font.amiri_quran),
    GoogleFontFile(googleFont = GoogleFont("Amiri Quran"), fontProvider = provider)
)

val amiriFont = FontFamily(
    ResourceFont(R.font.amiri_regular),
    GoogleFontFile(googleFont = GoogleFont("Amiri"), fontProvider = provider)
)

val scheherazadeFont = FontFamily(
    ResourceFont(R.font.scheherazade_new),
    GoogleFontFile(googleFont = GoogleFont("Scheherazade New"), fontProvider = provider)
)

val lateefFont = FontFamily(
    ResourceFont(R.font.lateef_regular),
    GoogleFontFile(googleFont = GoogleFont("Lateef"), fontProvider = provider)
)

val almaraiFont = FontFamily(
    ResourceFont(R.font.almarai_regular),
    GoogleFontFile(googleFont = GoogleFont("Almarai"), fontProvider = provider)
)

val tajawalFont = FontFamily(
    ResourceFont(R.font.tajawal_regular),
    GoogleFontFile(googleFont = GoogleFont("Tajawal"), fontProvider = provider)
)




val solaimanLipiFont = FontFamily(
    ResourceFont(R.font.solaimanlipi, FontWeight.Normal),
    ResourceFont(R.font.solaimanlipi_bold, FontWeight.Bold)
)

val hindSiliguriFont = FontFamily(
    ResourceFont(R.font.hind_siliguri, FontWeight.Normal)
)

val shorifShishirFont = FontFamily(
    ResourceFont(R.font.shorif_shishir, FontWeight.Normal)
)

val LocalBengaliFont = staticCompositionLocalOf { solaimanLipiFont }
val LocalArabicFont = staticCompositionLocalOf { meQuranFont }

val bengaliFontsList = listOf(
    "SolaimanLipi",
    "Hind Siliguri",
    "Shorif Shishir"
)

fun getBengaliFont(name: String): FontFamily {
    return when (name) {
        "SolaimanLipi", "সলাইমান লিপি" -> solaimanLipiFont
        "Hind Siliguri", "হিন্দ শিলিগুড়ি" -> hindSiliguriFont
        "Shorif Shishir", "শরীফ শিশির" -> shorifShishirFont
        else -> solaimanLipiFont
    }
}

val arabicFontsList = listOf(
    "Scheherazade New",
    "PDMS Saleem",
    "Amiri",
    "Me Quran",
    "Noorehira",
    "Uthman Taha",
    "Amiri Quran",
    "Lateef",
    "Almarai",
    "Tajawal"
)

fun getArabicFont(name: String): FontFamily {
    return when (name) {
        "Uthman Taha", "উছমান তাহা নাসখ" -> uthmanTahaFont
        "Amiri Quran", "আমিরি কুরআন" -> amiriQuranFont
        "Amiri", "আমিরি" -> amiriFont
        "Scheherazade New", "শাহরাজাদ", "আল কালাম কুরআন মাজীদ", "আল কালাম" -> scheherazadeFont
        "Lateef", "লতীফ" -> lateefFont
        "Almarai", "আল মারাই" -> almaraiFont
        "Tajawal", "তাজাওয়াল" -> tajawalFont
        "Me Quran", "মি কুরআন" -> meQuranFont
        "PDMS Saleem", "মলভি এ-এম", "সেলিম" -> pdmsSaleemFont
        "Noorehira", "নুরে হুদা", "নূরে হেরা" -> noorehiraFont
        else -> meQuranFont
    }
}

fun getArabicFontForTajweed(name: String): FontFamily {
    return getArabicFont(name)
}

fun getTypographyForBengaliFont(bengaliFontFamily: FontFamily): Typography {
    val defaultStyle = TextStyle(fontFamily = bengaliFontFamily)
    return Typography(
        displayLarge = defaultStyle.copy(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
        displayMedium = defaultStyle.copy(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
        displaySmall = defaultStyle.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
        headlineLarge = defaultStyle.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
        headlineMedium = defaultStyle.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
        headlineSmall = defaultStyle.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
        titleLarge = defaultStyle.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleMedium = defaultStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp, fontWeight = FontWeight.Medium),
        titleSmall = defaultStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
        bodyLarge = defaultStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
        bodyMedium = defaultStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
        bodySmall = defaultStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
        labelLarge = defaultStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
        labelMedium = defaultStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
        labelSmall = defaultStyle.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
    )
}

// Set of Material typography styles to start with
private val defaultTextStyle = TextStyle(
    fontFamily = FontFamily.Default
)

val Typography = Typography(
    displayLarge = defaultTextStyle.copy(fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium = defaultTextStyle.copy(fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = 0.sp),
    displaySmall = defaultTextStyle.copy(fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp),
    headlineLarge = defaultTextStyle.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp),
    headlineMedium = defaultTextStyle.copy(fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp),
    headlineSmall = defaultTextStyle.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp),
    titleLarge = defaultTextStyle.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
    titleMedium = defaultTextStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp, fontWeight = FontWeight.Medium),
    titleSmall = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    bodyLarge = defaultTextStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall = defaultTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp, fontWeight = FontWeight.Medium),
    labelMedium = defaultTextStyle.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
    labelSmall = defaultTextStyle.copy(fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp, fontWeight = FontWeight.Medium),
)
