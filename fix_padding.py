import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

# Fix Box padding
text = text.replace(
    '.padding(horizontal = 10.dp, vertical = 4.dp) // tighter padding',
    '.padding(horizontal = 12.dp, vertical = 8.dp)'
)
text = text.replace(
    '.padding(horizontal = 10.dp, vertical = 4.dp)',
    '.padding(horizontal = 12.dp, vertical = 8.dp)'
)

# Fix Arrangement.spacedBy((-5).dp) to (-1).dp to prevent overlaps but keep them close
text = text.replace(
    'Arrangement.spacedBy((-5).dp)',
    'Arrangement.spacedBy((-1).dp)'
)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)
