import re

with open('app/src/main/java/com/example/ui/components/PrayerSunPathCard.kt', 'r') as f:
    text = f.read()

# 1. Change height(96.dp) to height(115.dp)
text = re.sub(r'\.height\(96\.dp\)', '.height(115.dp)', text)

# 2. Change curve equations
# In nodes
text = re.sub(r'val y = \(oneMinusT \* oneMinusT \* 0\.85f\) \+ \(2f \* oneMinusT \* t \* -0\.30f\) \+ \(t \* t \* 0\.85f\)',
              r'val y = (oneMinusT * oneMinusT * 0.70f) + (2f * oneMinusT * t * -0.40f) + (t * t * 0.70f)', text)

# In drawPath
text = re.sub(r'val pathStart = Offset\(size\.width \* 0\.05f, size\.height \* 0\.85f\)',
              r'val pathStart = Offset(size.width * 0.05f, size.height * 0.70f)', text)
text = re.sub(r'val pathEnd = Offset\(size\.width \* 0\.95f, size\.height \* 0\.85f\)',
              r'val pathEnd = Offset(size.width * 0.95f, size.height * 0.70f)', text)
text = re.sub(r'val peak = Offset\(size\.width \* 0\.50f, size\.height \* -0\.30f\)',
              r'val peak = Offset(size.width * 0.50f, size.height * -0.40f)', text)

# In sunY
text = re.sub(r'val sunY = size\.height \* \(\(oneMinusT \* oneMinusT \* 0\.85f\) \+ \(2f \* oneMinusT \* sunT \* -0\.30f\) \+ \(sunT \* sunT \* 0\.85f\)\)',
              r'val sunY = size.height * ((oneMinusT * oneMinusT * 0.70f) + (2f * oneMinusT * sunT * -0.40f) + (sunT * sunT * 0.70f))', text)

# Tahajjud relY
text = re.sub(r'val relY = \(\(oneMinusT \* oneMinusT \* 0\.85f\) \+ \(2f \* oneMinusT \* t \* -0\.30f\) \+ \(t \* t \* 0\.85f\)\)',
              r'val relY = ((oneMinusT * oneMinusT * 0.70f) + (2f * oneMinusT * t * -0.40f) + (t * t * 0.70f))', text)


# 3. Change column ordering and verticalY
# For regular nodes:
# Match from val verticalY = (height * relativeOffset.y) - 34.dp to the end of the Box
pattern_node = r'(val verticalY = \(height \* relativeOffset\.y\) - 34\.dp\s*Column\(\s*modifier = Modifier\s*\.offset\(x = xOffset - 32\.dp, y = verticalY\) // -32dp centers the 64dp wide column over X\s*\.width\(64\.dp\),\s*horizontalAlignment = Alignment\.CenterHorizontally\s*\) \{)(.*?)(// Minimal icon circle\s*Box\([^}]*\} \{[^}]*Icon\([^}]*\)\s*\})'
# We need a better regex. Let's just use Python string manipulation for the nodes.
