import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    content = f.read()

target = """        // High-Fidelity 3D-Illuminated Mosque Silhouette Background - Pinned to bottom
        MosqueSilhouetteBackground(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .align(Alignment.BottomCenter)
        )"""

replacement = """        // Dynamic Sun/Moon Background
        DynamicSkyBackground(
            currentIndex = currentIndex,
            modifier = Modifier.matchParentSize()
        )"""

content = content.replace(target, replacement)

new_func = """
@Composable
private fun DynamicSkyBackground(
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    val isNight = currentIndex == 5 || currentIndex == 0 // Isha or Fajr
    val isSunset = currentIndex == 4
    
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        if (isNight) {
            // Moon
            val moonCenter = Offset(w * 0.85f, h * 0.25f)
            // Moon glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFE2E8F0).copy(alpha = 0.25f), Color.Transparent),
                    center = moonCenter,
                    radius = w * 0.4f
                ),
                radius = w * 0.4f,
                center = moonCenter
            )
            // Moon body
            drawCircle(
                color = Color(0xFFF8FAFC).copy(alpha = 0.95f),
                radius = 18.dp.toPx(),
                center = moonCenter
            )
            // Crescent effect (dark circle masking the moon)
            val skyColor = if (currentIndex == 0) Color(0xFF0C2B3C) else Color(0xFF0B1120) // Match dawn or midnight sky
            drawCircle(
                color = skyColor, 
                radius = 15.dp.toPx(),
                center = Offset(moonCenter.x - 6.dp.toPx(), moonCenter.y - 4.dp.toPx())
            )
        } else {
            // Sun
            val sunCenter = Offset(w * 0.85f, h * 0.25f)
            val sunCore = if (isSunset) Color(0xFFFF8A65) else Color(0xFFFFD54F)
            val sunGlow = if (isSunset) Color(0xFFE64A19) else Color(0xFFFFA000)
            
            // Sun glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(sunGlow.copy(alpha = 0.4f), Color.Transparent),
                    center = sunCenter,
                    radius = w * 0.45f
                ),
                radius = w * 0.45f,
                center = sunCenter
            )
            // Sun body
            drawCircle(
                color = sunCore.copy(alpha = 0.95f),
                radius = 22.dp.toPx(),
                center = sunCenter
            )
        }
        
        // Add some stars if it's night
        if (isNight) {
            val random = java.util.Random(42) // Fixed seed for stable stars
            for (i in 0..15) {
                val x = random.nextFloat() * w
                val y = random.nextFloat() * (h * 0.6f)
                val starRadius = (random.nextFloat() * 1.5f + 0.5f).dp.toPx()
                val alpha = random.nextFloat() * 0.5f + 0.2f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = starRadius,
                    center = Offset(x, y)
                )
            }
        } else {
            // Add some soft clouds if it's day
            val cloudColor = Color.White.copy(alpha = 0.08f)
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.1f, h * 0.15f),
                size = androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.04f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f)
            )
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.15f, h * 0.13f),
                size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.05f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f)
            )
            
            drawRoundRect(
                color = cloudColor,
                topLeft = Offset(w * 0.65f, h * 0.35f),
                size = androidx.compose.ui.geometry.Size(w * 0.15f, h * 0.03f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(50f, 50f)
            )
        }
    }
}
"""

content = content + new_func

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(content)
