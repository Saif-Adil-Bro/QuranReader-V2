import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    content = f.read()

def replace_between(start_str, end_str, new_str, text):
    start_idx = text.find(start_str)
    end_idx = text.find(end_str)
    if start_idx == -1 or end_idx == -1:
        print("Could not find bounds")
        return text
    return text[:start_idx] + new_str + text[end_idx:]

# 1. Update Fixed nodes drawing
start_fixed = """            // Fixed nodes
            nodes.forEachIndexed { index, relativeOffset ->"""
end_fixed = """            // Calculate dynamic sun position"""

new_fixed = """            // Fixed nodes
            nodes.forEachIndexed { index, relativeOffset ->
                // Hide middle nodes during Isha (night)
                if (currentIndex == prayers.lastIndex && index in 1 until prayers.lastIndex) return@forEachIndexed

                val x = size.width * relativeOffset.x
                val y = size.height * relativeOffset.y
                val isCurrent = index == currentIndex

                drawCircle(
                    color = if (isCurrent) GoldBright else Color.White.copy(alpha = 0.8f),
                    radius = if (isCurrent) 4.5.dp.toPx() else 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }
            
            // Draw Tahajjud node during Isha
            if (currentIndex == prayers.lastIndex && prayers.isNotEmpty()) {
                val t = 0.5f
                val oneMinusT = 1f - t
                val relX = (oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * t * 0.50f) + (t * t * 0.95f)
                val relY = (oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * t * -0.30f) + (t * t * 0.85f)
                drawCircle(
                    color = Color.White.copy(alpha = 0.8f),
                    radius = 3.dp.toPx(),
                    center = Offset(size.width * relX, size.height * relY)
                )
            }

"""
content = replace_between(start_fixed, end_fixed, new_fixed, content)


# 2. Update Labels & Icons drawing
start_labels = """        // Labels & Icons along the path
        nodes.forEachIndexed { index, relativeOffset ->"""
end_labels = """    }
}"""

new_labels = """        // Labels & Icons along the path
        nodes.forEachIndexed { index, relativeOffset ->
            // Hide middle labels during Isha (night)
            if (currentIndex == prayers.lastIndex && index in 1 until prayers.lastIndex) return@forEachIndexed

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
        
        // Tahajjud Label & Icon during Isha
        if (currentIndex == prayers.lastIndex && prayers.isNotEmpty()) {
            val t = 0.5f
            val oneMinusT = 1f - t
            val relX = (oneMinusT * oneMinusT * 0.05f) + (2f * oneMinusT * t * 0.50f) + (t * t * 0.95f)
            val relY = (oneMinusT * oneMinusT * 0.85f) + (2f * oneMinusT * t * -0.30f) + (t * t * 0.85f)
            
            val xOffset = width * relX
            val verticalY = (height * relY) - 34.dp
            
            Column(
                modifier = Modifier
                    .offset(x = xOffset - 32.dp, y = verticalY)
                    .width(64.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "তাহাজ্জুদ",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "রাতের\nশেষাংশ",
                    color = SoftWhite.copy(alpha = 0.75f),
                    fontSize = 6.5.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 7.5.sp,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(3.dp))
                // Minimal icon circle
                Box(
                    modifier = Modifier
                        .size(13.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(
                            width = 0.8.dp,
                            color = Color.White.copy(alpha = 0.35f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }
"""
content = replace_between(start_labels, end_labels, new_labels, content)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(content)
