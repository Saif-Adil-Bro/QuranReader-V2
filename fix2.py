with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

import re
# Replace whatever is between 'text = "রাতের' and 'শেষাংশ",' with '\n'
text = re.sub(r'text = "রাতের.*শেষাংশ",', 'text = "রাতের\\\\nশেষাংশ",', text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)
