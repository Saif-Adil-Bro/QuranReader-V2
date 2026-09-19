with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    lines = f.readlines()

depth = 0
for i in range(52, 526):
    line = lines[i]
    cleaned = line.split('//')[0]
    depth += cleaned.count('{')
    depth -= cleaned.count('}')
    print(f"{i+1:3d}: {depth} | {line.strip()}")
