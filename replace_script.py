import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    content = f.read()

# Add outlined icons import if not there
if 'import androidx.compose.material.icons.outlined.*' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.*', 'import androidx.compose.material.icons.filled.*\nimport androidx.compose.material.icons.outlined.*')

start_str = '@Composable\nfun PrayerSunPathCard'
end_str = '@Composable\nprivate fun VisualSunPathSection'

start_idx = content.find(start_str)
end_idx = content.find(end_str)

if start_idx == -1 or end_idx == -1:
    print("Could not find bounds")
    exit(1)

new_component = """@Composable
fun PrayerSunPathCard(
    schedule: DailyPrayerSchedule,
    modifier: Modifier = Modifier,
    onDetailsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val currentDate = remember(schedule) { schedule.dateStrBn }
    val hijriDate = remember { DateUtil.getTodayHijriDateStr(0) }

    val selectedLocation = if (schedule.district.countryBn == "বাংলাদেশ") {
        schedule.district.nameBn
    } else {
        "${schedule.district.nameBn}, ${schedule.district.countryBn}"
    }

    val prayers = remember(schedule) {
        val list = mutableListOf<VisualPrayerPoint>()
        schedule.prayers.forEach { prayerTime ->
            if (prayerTime.name != PrayerName.SAHRI && prayerTime.name != PrayerName.IFTAR) {
                val cal = Calendar.getInstance().apply { timeInMillis = prayerTime.timestampMillis }
                val minutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                
                list.add(
                    VisualPrayerPoint(
                        name = prayerTime.name.name,
                        bengaliName = prayerTime.name.nameBn,
                        timeMinutes = minutes,
                        timeString = "${prayerTime.timeDigits} ${prayerTime.amPm}",
                        iconType = prayerTime.name
                    )
                )
            }
        }
        list.sortedBy { it.timeMinutes }
    }

    val currentMinutes by rememberCurrentMinutes()
    val currentIndex = findCurrentPrayerIndex(prayers, currentMinutes)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F3E29), // Deep Emerald Green
                        Color(0xFF0A2B1C)
                    )
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Current Prayer Name & Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = current.bengaliName,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = GoldBright,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = current.timeString,
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 4. Progress Block (Start -> Progress -> Remaining)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "শুরু হয়েছে",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${formatDurationBangla(elapsedMinutes)} আগে",
                                    color = GoldBright,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        // Center: Progress Bar
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
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
                                            .size(12.dp)
                                            .offset(x = 6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34D399))
                                            .border(2.dp, Color(0xFF0F3E29), CircleShape)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${DateUtil.toBengaliNumerals((progress * 100).toInt())}% সম্পন্ন",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(12.dp))

                        // Right: Remaining time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.HourglassEmpty,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "পরবর্তী ওয়াক্ত",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${formatDurationBangla(totalMinutes - elapsedMinutes)} বাকি",
                                    color = GoldBright,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Next Prayer Details Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "পরবর্তী ওয়াক্ত",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "${next.bengaliName} • ${next.timeString}",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Center Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Middle: Azaan Time
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = GoldBright,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "আজানের সময়",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = next.timeString,
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Center Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(30.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Right: Notification Status
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNotificationClick() }) {
                            Icon(
                                imageVector = Icons.Outlined.VolumeUp,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "নোটিফিকেশন",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "চালু আছে",
                                    color = Color(0xFF34D399),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 6. Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = SoftWhite.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "সময়সূচী: ইসলামিক ফাউন্ডেশন (বাংলাদেশ) অনুযায়ী",
                    color = SoftWhite.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
"""

new_content = content[:start_idx] + new_component + content[end_idx:]

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(new_content)
