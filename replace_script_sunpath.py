import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    content = f.read()

def replace_between(start_str, end_str, new_str, text):
    start_idx = text.find(start_str)
    end_idx = text.find(end_str)
    if start_idx == -1 or end_idx == -1:
        print(f"Could not find bounds: {start_str[:20]}... or {end_str[:20]}...")
        return text
    return text[:start_idx] + new_str + text[end_idx:]

# 1. Update Box background colors and next Waqt layout
start_str = '    Box(\n        modifier = modifier\n            .fillMaxWidth()'
end_str = '@Composable\nprivate fun VisualSunPathSection'

new_component_body = """    val dynamicColors = remember(currentIndex) {
        when (currentIndex) {
            0, 1 -> listOf(Color(0xFF0C2B3C), Color(0xFF071922)) // Fajr/Sunrise: Dawn Blues
            2 -> listOf(Color(0xFF084B5B), Color(0xFF04262E)) // Dhuhr: Bright Midday
            3 -> listOf(Color(0xFF4A3415), Color(0xFF2B1D0B)) // Asr: Afternoon Warm
            4 -> listOf(Color(0xFF441818), Color(0xFF220A0A)) // Maghrib: Sunset Deep Orange/Red
            else -> listOf(Color(0xFF0B1120), Color(0xFF060912)) // Isha/Night: Midnight
        }
    }

    // Toggle states for notification
    var isNotificationOn by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = dynamicColors
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDetailsClick
            )
    ) {
        // High-Fidelity 3D-Illuminated Mosque Silhouette Background
        MosqueSilhouetteBackground(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Header (Date + Location)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top-Left: Golden Calendar Icon + Bangla Date Capsule
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentDate,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = hijriDate,
                        color = SoftWhite.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 22.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Top-Right: Golden Location Pin + District Capsule
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedLocation,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Visual Sun/Moon Path
            VisualSunPathSection(
                prayers = prayers,
                currentIndex = currentIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Current Waqt Center Display
            if (currentIndex in 0 until prayers.size) {
                val current = prayers[currentIndex]
                val next = prayers.getOrNull((currentIndex + 1) % prayers.size) ?: prayers.first()
                
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

                // "বর্তমান ওয়াক্ত" pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "বর্তমান ওয়াক্ত",
                        color = SoftWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Current Prayer Name & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = current.bengaliName,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "•",
                        color = GoldBright,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = current.timeString,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Progress Block (Start -> Progress -> Remaining)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Started time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "শুরু হয়েছে",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${formatDurationBangla(elapsedMinutes)} আগে",
                                    color = GoldBright,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))
                        
                        // Center: Progress Bar
                        Column(
                            modifier = Modifier.weight(1.5f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                // Progress fill
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(GoldAccent, Color(0xFF34D399))
                                            )
                                        )
                                )
                                // Thumb indicator
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .offset(x = 5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                            .border(1.5.dp, Color(0xFF0F3E29), CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${DateUtil.toBengaliNumerals((progress * 100).toInt())}% সম্পন্ন",
                                color = Color(0xFF34D399),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(10.dp))

                        // Right: Remaining time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "পরবর্তী ওয়াক্ত",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${formatDurationBangla(totalMinutes - elapsedMinutes)} বাকি",
                                    color = GoldBright,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 5. Next Prayer Details Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Next Prayer
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "পরবর্তী ওয়াক্ত",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = "${next.bengaliName} • ${next.timeString}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Center Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Middle: Azaan Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "আজানের সময়",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp
                                )
                                Text(
                                    text = next.timeString,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Center Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(26.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Right: Notification Toggle
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { 
                                isNotificationOn = !isNotificationOn
                                onNotificationClick()
                            }
                        ) {
                            Text(
                                text = "নোটিফিকেশন",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            // Custom Pill Toggle
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isNotificationOn) Color(0xFF34D399).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, if (isNotificationOn) Color(0xFF34D399) else Color.White.copy(alpha = 0.3f), RoundedCornerShape(50))
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(if (isNotificationOn) Color(0xFF34D399) else Color.White.copy(alpha = 0.5f))
                                        .align(if (isNotificationOn) Alignment.CenterEnd else Alignment.CenterStart)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SoftWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "সময়সূচী: ইসলামিক ফাউন্ডেশন (বাংলাদেশ) অনুযায়ী",
                    color = SoftWhite.copy(alpha = 0.75f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
"""

content = replace_between(start_str, end_str, new_component_body, content)


# 2. Update VisualSunPathSection
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

        // Calculate exact node positions mathematically to align perfectly
        val nodes = remember(prayers, size) {
            prayers.mapIndexed { index, _ ->
                val fraction = index.toFloat() / (prayers.size - 1).coerceAtLeast(1)
                // Curve equation matching the quadratic bezier
                val curve = 4f * (1f - fraction) * fraction
                val x = 0.05f + (0.95f - 0.05f) * fraction // relative to width
                // Lowering the peak and shifting ends down to make arch bigger
                val y = 0.85f - (0.85f - 0.00f) * curve // relative to height
                Offset(x, y)
            }
        }

        // Arc & Nodes Drawing
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (prayers.size < 2) return@Canvas

            val pathStart = Offset(x = size.width * 0.05f, y = size.height * 0.85f)
            val pathEnd = Offset(x = size.width * 0.95f, y = size.height * 0.85f)
            // A higher peak creates a steeper arch. 
            // Control point Y needs to be negative to pull the curve up high enough.
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

            // Points along the arc
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
