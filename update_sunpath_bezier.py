import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    content = f.read()

def replace_between(start_str, end_str, new_str, text):
    start_idx = text.find(start_str)
    end_idx = text.find(end_str)
    if start_idx == -1 or end_idx == -1:
        print(f"Could not find bounds")
        return text
    return text[:start_idx] + new_str + text[end_idx:]

start_str = '@Composable\nprivate fun VisualSunPathSection'
end_str = '@Composable\nprivate fun rememberCurrentMinutes()'

visual_sun_path_body = """@Composable
private fun VisualSunPathSection(
    prayers: List<VisualPrayerPoint>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val width = maxWidth
        val height = maxHeight

        val infiniteTransition = rememberInfiniteTransition(label = "sunPulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.90f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        // Exact node positions mathematically matching quadraticBezierTo to align perfectly
        val nodes = remember(prayers, width, height) {
            prayers.mapIndexed { index, _ ->
                val t = index.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                val oneMinusT = 1f - t
                // Exact Bezier curve equations mapping to our Canvas control points:
                // start(0.05, 0.85), peak(0.50, -0.30), end(0.95, 0.85)
                val x = (oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * t * 0.50f) + (t * t * 0.95f)
                val y = (oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * t * -0.30f) + (t * t * 0.85f)
                Offset(x, y)
            }
        }

        // Arc & Nodes Drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (prayers.size < 2) return@Canvas

            val pathStart = Offset(x = size.width * 0.05f, y = size.height * 0.85f)
            val pathEnd = Offset(x = size.width * 0.95f, y = size.height * 0.85f)
            val peak = Offset(x = size.width * 0.50f, y = -size.height * 0.30f)

            val path = Path().apply {
                moveTo(pathStart.x, pathStart.y)
                quadraticBezierTo(peak.x, peak.y, pathEnd.x, pathEnd.y)
            }

            // Glow path behind
            drawPath(
                path = path,
                color = GoldAccent.copy(alpha = 0.15f),
                style = Stroke(width = 6.dp.toPx())
            )

            // Solid celestial path (removed dashPathEffect)
            drawPath(
                path = path,
                color = GoldAccent.copy(alpha = 0.9f),
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Points along the arc using calculated nodes
            nodes.forEachIndexed { index, relativeOffset ->
                val x = size.width * relativeOffset.x
                val y = size.height * relativeOffset.y
                val isCurrent = index == currentIndex

                if (isCurrent) {
                    drawCircle(
                        color = GoldAccent.copy(alpha = 0.35f),
                        radius = 8.dp.toPx() * pulseScale,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = GoldBright,
                        radius = 4.5.dp.toPx(),
                        center = Offset(x, y)
                    )
                } else {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = 3.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }

        // Labels & Icons along the path
        nodes.forEachIndexed { index, relativeOffset ->
            val prayer = prayers[index]
            val isCurrent = index == currentIndex
            
            // X position is aligned exactly to the node center
            val xOffset = width * relativeOffset.x
            // Y position is placed just above the node center
            val verticalY = (height * relativeOffset.y) - 34.dp

            Column(
                modifier = Modifier
                    .offset(x = xOffset - 32.dp, y = verticalY) // -32dp centers the 64dp wide column over X
                    .width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = prayer.bengaliName,
                    color = if (isCurrent) GoldBright else Color.White,
                    fontSize = if (isCurrent) 8.5.sp else 7.5.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
                Text(
                    text = prayer.timeString.replace(" ", "\\n"),
                    color = if (isCurrent) Color.White else SoftWhite.copy(alpha = 0.85f),
                    fontSize = if (isCurrent) 7.5.sp else 6.5.sp,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    lineHeight = 8.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Minimal icon circle
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 16.dp else 13.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) GoldAccent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f))
                        .border(
                            width = if (isCurrent) 1.5.dp else 0.8.dp,
                            color = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val fallbackIcon = when (prayer.iconType) {
                        PrayerName.FAJR -> Icons.Default.Nightlight
                        PrayerName.SUNRISE -> Icons.Default.WbSunny
                        PrayerName.DHUHR -> Icons.Default.WbSunny
                        PrayerName.ASR -> Icons.Default.WbSunny
                        PrayerName.MAGHRIB -> Icons.Default.WbSunny
                        PrayerName.ISHA -> Icons.Default.Nightlight
                        else -> Icons.Default.Schedule
                    }
                    Icon(
                        imageVector = fallbackIcon,
                        contentDescription = null,
                        tint = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(if (isCurrent) 10.dp else 8.dp)
                    )
                }
            }
        }
    }
}
"""

content = replace_between(start_str, end_str, visual_sun_path_body, content)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(content)
