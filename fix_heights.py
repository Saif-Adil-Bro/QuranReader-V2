import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

# Fix Progress Block
text = re.sub(
    r'\.background\(Color\.Black\.copy\(alpha = 0\.35f\)\) // Slightly darker for contrast\s*\.border\(0\.8\.dp, Color\.White\.copy\(alpha = 0\.15f\), RoundedCornerShape\(16\.dp\)\)\s*\.padding\(horizontal = 12\.dp, vertical = 8\.dp\)\s*\) \{\s*Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*verticalAlignment = Alignment\.CenterVertically',
    r'''.background(Color.Black.copy(alpha = 0.35f)) // Slightly darker for contrast
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .height(64.dp)
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically''',
    text
)

# Fix Next Prayer Block
text = re.sub(
    r'\.background\(Color\.Black\.copy\(alpha = 0\.35f\)\)\s*\.border\(0\.8\.dp, Color\.White\.copy\(alpha = 0\.15f\), RoundedCornerShape\(16\.dp\)\)\s*\.padding\(horizontal = 12\.dp, vertical = 8\.dp\)\s*\) \{\s*Row\(\s*modifier = Modifier\.fillMaxWidth\(\),\s*verticalAlignment = Alignment\.CenterVertically,',
    r'''.background(Color.Black.copy(alpha = 0.35f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .height(64.dp)
                        .padding(horizontal = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,''',
    text
)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)
