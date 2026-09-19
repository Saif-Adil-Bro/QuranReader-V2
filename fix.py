with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

# Fix the broken text
text = text.replace('text = "রাতের\\n\\nশেষাংশ",', 'text = "রাতের\\\\nশেষাংশ",')
text = text.replace('text = "রাতের\\nশেষাংশ",', 'text = "রাতের\\\\nশেষাংশ",')
text = text.replace('text = "রাতের\\n\\nশেষাংশ",', 'text = "রাতের\\\\nশেষাংশ",')

# Also wait, did the python script do: text = "রাতের\nশেষাংশ" -> actual newline?
# Let's just fix it carefully
import re
text = re.sub(r'text = "রাতের\n+শেষাংশ",', 'text = "রাতের\\\\nশেষাংশ",', text)
text = re.sub(r'text = "রাতের\\\\n\n+শেষাংশ",', 'text = "রাতের\\\\nশেষাংশ",', text)

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'w') as f:
    f.write(text)
