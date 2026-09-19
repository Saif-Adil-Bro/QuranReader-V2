import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    content = f.read()

# 1. Update the call site in PrayerSunPathCard
target_call = """            // 2. Visual Sun/Moon Path
            VisualSunPathSection(
                prayers = prayers,
                currentIndex = currentIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )"""

replacement_call = """            // 2. Visual Sun/Moon Path
            VisualSunPathSection(
                prayers = prayers,
                currentIndex = currentIndex,
                currentMinutes = currentMinutes,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )"""
content = content.replace(target_call, replacement_call)

# 2. Update the function signature
target_sig = """@Composable
private fun VisualSunPathSection(
    prayers: List<VisualPrayerPoint>,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {"""

replacement_sig = """@Composable
private fun VisualSunPathSection(
    prayers: List<VisualPrayerPoint>,
    currentIndex: Int,
    currentMinutes: Int,
    modifier: Modifier = Modifier
) {"""
content = content.replace(target_sig, replacement_sig)

# 3. Add dynamic sun position calculation inside Canvas
# Find:
#             val path = Path().apply {
#                 moveTo(pathStart.x, pathStart.y)
#                 quadraticBezierTo(peak.x, peak.y, pathEnd.x, pathEnd.y)
#             }
# And add dynamic progress calc right after or before drawing points.

def replace_between(start_str, end_str, new_str, text):
    start_idx = text.find(start_str)
    end_idx = text.find(end_str)
    if start_idx == -1 or end_idx == -1:
        print("Could not find bounds")
        return text
    return text[:start_idx] + new_str + text[end_idx:]

start_draw = """            // Points along the arc using calculated nodes
            nodes.forEachIndexed { index, relativeOffset ->"""
end_draw = """        // Labels & Icons along the path"""

new_draw = """            // Fixed nodes
            nodes.forEachIndexed { index, relativeOffset ->
                val x = size.width * relativeOffset.x
                val y = size.height * relativeOffset.y
                val isCurrent = index == currentIndex

                drawCircle(
                    color = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.8f),
                    radius = if (isCurrent) 4.5.dp.toPx() else 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Calculate dynamic sun position
            if (prayers.isNotEmpty() && currentIndex in prayers.indices) {
                val current = prayers[currentIndex]
                val nextIndex = (currentIndex + 1) % prayers.size
                val next = prayers[nextIndex]
                
                val totalMinutes = if (next.timeMinutes > current.timeMinutes) {
                    next.timeMinutes - current.timeMinutes
                } else {
                    (24 * 60 - current.timeMinutes) + next.timeMinutes
                }
                val elapsedMinutes = if (currentMinutes >= current.timeMinutes) {
                    currentMinutes - current.timeMinutes
                } else {
                    (24 * 60 - current.timeMinutes) + currentMinutes
                }
                val progress = (elapsedMinutes.toFloat() / totalMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
                
                val currentT = currentIndex.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                // If nextIndex is 0 (wrapping around to Fajr), its T would naturally be 1.0 + something.
                // But in our arch, 0 to 5 maps to T 0.0 to 1.0. 
                // Wrapping around means from Isha (T=1.0) to Fajr (T=0.0). We shouldn't visually wrap back across the whole screen.
                // It looks better if we just clamp it, or if it's Isha->Fajr, let it slowly move towards the end or disappear.
                // Let's implement wrap-around visually: from Isha to Fajr, the sun could move off-screen or jump back.
                // But on the arch, Isha is at T=1.0 and Fajr is at T=0.0.
                val nextT = if (nextIndex == 0) 1.2f else nextIndex.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                
                var sunT = currentT + (nextT - currentT) * progress
                // If sunT > 1.0 (Isha to Fajr), we can map it back to 0.0 gradually if we wanted, 
                // or just let it follow an imaginary extended curve (which nextT=1.2f does roughly).
                // Actually, wrapping around from right to left smoothly might look weird.
                // Let's just let it slide back smoothly from 1.0 to 0.0 over the night.
                if (nextIndex == 0) {
                    sunT = currentT + (0.0f - currentT) * progress // slides backwards during night
                }
                
                val oneMinusT = 1f - sunT
                val sunX = size.width * ((oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * sunT * 0.50f) + (sunT * sunT * 0.95f))
                val sunY = size.height * ((oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * sunT * -0.30f) + (sunT * sunT * 0.85f))
                
                // Draw dynamic glowing sun
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.35f),
                    radius = 8.dp.toPx() * pulseScale,
                    center = Offset(sunX, sunY)
                )
                drawCircle(
                    color = GoldBright,
                    radius = 4.5.dp.toPx(),
                    center = Offset(sunX, sunY)
                )
            }
        }

"""
content = replace_between(start_draw, end_draw, new_draw, content)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(content)
