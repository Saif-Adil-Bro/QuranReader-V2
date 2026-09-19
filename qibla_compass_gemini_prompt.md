# Qibla Compass Jetpack Compose - Gemini Prompt

Hello Gemini, act as an Expert Android Jetpack Compose Developer. I want you to build a fully functional "Qibla Compass" screen for my Android app. Please use the specifications, math formulas, and UI descriptions below to recreate this feature perfectly.

## 1. Theme & Colors (CRITICAL)
- The compass UI **MUST** adapt to the app's Light and Dark modes.
- Use `MaterialTheme.colorScheme.background`, `onBackground`, `surface`, `onSurface`, etc., for texts, cards, and backgrounds.
- For the compass dial and primary accent colors, use an elegant Green (e.g., `#2A7C5D` or `#389E6E`), but ensure contrast is maintained in dark mode. For example, inner circles should use `MaterialTheme.colorScheme.surface` or a dark variant instead of hardcoded white.
- Do not hardcode colors like `Color.White` or `Color.Black` for text or main backgrounds. Always rely on MaterialTheme tokens or calculate luminance to pick appropriate text colors.

## 2. Sensors & Permissions
- Request location permissions (`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`) using `rememberLauncherForActivityResult`.
- Use `LocationManager` to fetch coordinates. If location is unavailable or permission is denied, fallback to Dhaka's coordinates: Latitude `23.8103`, Longitude `90.4125`.
- Use `SensorManager` with `Sensor.TYPE_ACCELEROMETER` and `Sensor.TYPE_MAGNETIC_FIELD` to calculate device orientation.
- Apply a low-pass filter to smooth the azimuth, pitch, and roll values (e.g., `current = current + 0.2f * (newValue - current)`).
- Add **Haptic Feedback** (`Vibrator` or `VibratorManager`) that triggers a brief vibration when the device aligns perfectly with the Qibla (tolerance of ±3 degrees).

## 3. Math & Core Logic
Include these specific functions for calculation:

```kotlin
fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
    val meccaLat = Math.toRadians(21.422487)
    val meccaLng = Math.toRadians(39.826206)
    val userLat = Math.toRadians(latitude)
    val userLng = Math.toRadians(longitude)
    
    val dLng = meccaLng - userLng
    
    val y = kotlin.math.sin(dLng) * kotlin.math.cos(meccaLat)
    val x = kotlin.math.cos(userLat) * kotlin.math.sin(meccaLat) - kotlin.math.sin(userLat) * kotlin.math.cos(meccaLat) * kotlin.math.cos(dLng)
    
    var qibla = Math.toDegrees(kotlin.math.atan2(y, x))
    if (qibla < 0) qibla += 360.0
    return qibla
}

fun calculateDistanceToMecca(latitude: Double, longitude: Double): Double {
    val meccaLat = 21.422487
    val meccaLng = 39.826206
    val earthRadius = 6371.0 // Radius of the earth in km
    val dLat = Math.toRadians(meccaLat - latitude)
    val dLng = Math.toRadians(meccaLng - longitude)
    val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
            kotlin.math.cos(Math.toRadians(latitude)) * kotlin.math.cos(Math.toRadians(meccaLat)) *
            kotlin.math.sin(dLng / 2) * kotlin.math.sin(dLng / 2)
    val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
    return earthRadius * c
}
```

## 4. Helper Functions for Bengali Localization
Use these for formatting strings:
```kotlin
fun formatToBanglaNumber(num: Int): String {
    val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return num.toString().map { char ->
        if (char.isDigit()) banglaDigits[char - '0'] else char
    }.joinToString("")
}

fun getBanglaDirection(azimuth: Float): String {
    val normalized = (azimuth % 360 + 360) % 360
    return when {
        normalized < 22.5 || normalized >= 337.5 -> "উ"
        normalized < 67.5 -> "উ-পূ"
        normalized < 112.5 -> "পূ"
        normalized < 157.5 -> "দ-পূ"
        normalized < 202.5 -> "দ"
        normalized < 247.5 -> "দ-প"
        normalized < 292.5 -> "প"
        else -> "উ-প"
    }
}
```

## 5. UI Requirements (Canvas & Compose)
- **Top Section**: Show an Error/Warning Card if location permission is missing, prompting the user to grant it.
- **Turn Instruction**: Display text like "বামে 15° ঘুরুন", "ডানে 10° ঘুরুন", or "আপনি সঠিক দিকে আছেন" based on `turnAngle = (qiblaBearing - currentAzimuth + 360) % 360`.
- **The Compass Canvas**:
  - Draw a circular base with a green accent ring and an inner circle that adapts to the theme.
  - Draw tick marks for 360 degrees.
  - Draw N, E, S, W text labels rotating based on `-azimuth`.
  - Draw a Compass Needle (North pointing green, South pointing grey).
  - Draw a **Kaaba Icon** (A small dark rectangle with a gold band) representing the Qibla direction. It should rotate using `animatedAzimuth + qiblaBearing` relative to the center.
- **Bubble Level (Tilt Indicator)**:
  - Add a small circular Canvas floating at the bottom right of the main compass.
  - Map the `pitch` and `roll` values to a moving "bubble" inside this circle to act as a leveler, helping the user hold the phone flat. If flat, make the bubble green; if tilted, yellow/orange.
- **Bottom Section**:
  - Display the current degree and direction (e.g., `120° পূ`).
  - Display the total distance to Mecca (e.g., `4520 কিমি`).

Please output the complete Kotlin code for `QiblaCompassScreen.kt` adhering to these instructions.
