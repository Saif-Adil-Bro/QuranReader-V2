with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

# I will add a } just before "Spacer(modifier = Modifier.height(14.dp))"
import re
text = text.replace('            Spacer(modifier = Modifier.height(14.dp))', '            }\n\n            Spacer(modifier = Modifier.height(14.dp))', 1)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)
