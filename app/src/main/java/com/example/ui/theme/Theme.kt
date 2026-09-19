package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    onPrimary = White,
    primaryContainer = DarkGreen,
    onPrimaryContainer = White,
    secondary = OrangeAccent,
    onSecondary = DarkText,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = OffWhite,
    background = DarkBackground,
    onBackground = OffWhite,
    surface = DarkSurface,
    onSurface = OffWhite,
    surfaceVariant = DarkCard,
    onSurfaceVariant = White
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = White,
    primaryContainer = BackgroundGreen,
    onPrimaryContainer = DarkGreen,
    secondary = OrangeAccent,
    onSecondary = White,
    secondaryContainer = OffWhite,
    onSecondaryContainer = DarkText,
    background = OffWhite,
    onBackground = DarkText,
    surface = White,
    onSurface = DarkText,
    surfaceVariant = White,
    onSurfaceVariant = DarkText
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  arabicFontName: String = "Me Quran",
  bengaliFontName: String = "SolaimanLipi",
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = Color.Transparent.toArgb()
      window.navigationBarColor = Color.Transparent.toArgb()
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !darkTheme
      insetsController.isAppearanceLightNavigationBars = !darkTheme
    }
  }

  val bengaliFont = remember(bengaliFontName) { getBengaliFont(bengaliFontName) }
  val arabicFont = remember(arabicFontName) { getArabicFont(arabicFontName) }
  val customTypography = remember(bengaliFont) { getTypographyForBengaliFont(bengaliFont) }

  CompositionLocalProvider(
      LocalBengaliFont provides bengaliFont,
      LocalArabicFont provides arabicFont
  ) {
      MaterialTheme(colorScheme = colorScheme, typography = customTypography, content = content)
  }
}
