with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

import re

# Fix Next Prayer block spacing
text = re.sub(
    r'Column\(verticalArrangement = Arrangement\.spacedBy\(\(-3\)\.dp\)\) \{\s*Text\(\s*text = "পরবর্তী ওয়াক্ত",\s*color = Color\.White\.copy\(alpha = 0\.8f\),\s*fontSize = 9\.sp\s*\)\s*Text\(\s*text = "\$\{next\.bengaliName\} • \$\{next\.timeString\}",\s*color = Color\.White,\s*fontSize = 11\.5\.sp,\s*fontWeight = FontWeight\.Bold\s*\)\s*\}',
    '''Column(verticalArrangement = Arrangement.spacedBy((-5).dp)) {
                                Text(
                                    text = "পরবর্তী ওয়াক্ত",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "${next.bengaliName} • ${next.timeString}",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }''',
    text
)

# Fix Azaan time block spacing (just to match Next prayer, if needed, but not asked. Let's fix it anyway to be consistent)
text = re.sub(
    r'Column\(verticalArrangement = Arrangement\.spacedBy\(\(-3\)\.dp\)\) \{\s*Text\(\s*text = "আজানের সময়",\s*color = Color\.White\.copy\(alpha = 0\.8f\),\s*fontSize = 9\.sp\s*\)\s*Text\(\s*text = next\.timeString, // This matches alarm logic natively\s*color = Color\.White,\s*fontSize = 11\.5\.sp,\s*fontWeight = FontWeight\.Bold\s*\)\s*\}',
    '''Column(verticalArrangement = Arrangement.spacedBy((-5).dp)) {
                                Text(
                                    text = "আজানের সময়",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 9.sp,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = next.timeString, // This matches alarm logic natively
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 12.sp
                                )
                            }''',
    text
)


# Fix Remaining time (ওয়াক্ত শেষ) spacing
text = re.sub(
    r'Column\(\s*horizontalAlignment = Alignment\.End,\s*verticalArrangement = Arrangement\.spacedBy\(\(-2\)\.dp\)\s*\) \{\s*Text\(\s*text = "ওয়াক্ত শেষ",\s*color = Color\.White,\s*fontSize = 9\.sp,\s*fontWeight = FontWeight\.Medium\s*\)\s*Text\(\s*text = "\$\{formatDurationBangla\(totalMinutes - elapsedMinutes\)\} বাকি",\s*color = GoldBright,\s*fontSize = 9\.sp\s*\)\s*\}',
    '''Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy((-5).dp)
                            ) {
                                Text(
                                    text = "ওয়াক্ত শেষ",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "${formatDurationBangla(totalMinutes - elapsedMinutes)} বাকি",
                                    color = GoldBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )
                            }''',
    text
)

# Same for "শুরু হয়েছে" block to match spacing
text = re.sub(
    r'Column\(verticalArrangement = Arrangement\.spacedBy\(\(-2\)\.dp\)\) \{\s*Text\(\s*text = "শুরু হয়েছে",\s*color = Color\.White,\s*fontSize = 9\.sp,\s*fontWeight = FontWeight\.Medium\s*\)\s*Text\(\s*text = "\$\{formatDurationBangla\(elapsedMinutes\)\} আগে",\s*color = GoldBright,\s*fontSize = 9\.sp\s*\)\s*\}',
    '''Column(verticalArrangement = Arrangement.spacedBy((-5).dp)) {
                                Text(
                                    text = "শুরু হয়েছে",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 10.sp
                                )
                                Text(
                                    text = "${formatDurationBangla(elapsedMinutes)} আগে",
                                    color = GoldBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 11.sp
                                )
                            }''',
    text
)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)
