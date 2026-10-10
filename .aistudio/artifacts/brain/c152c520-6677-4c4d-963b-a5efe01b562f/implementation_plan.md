# Implementation Plan: Curved Sun Arc Path & Rounded Bottom Cards

## Proposed Changes

### 1. `PrayerSunPathCard.kt` (`VisualSunPathSection`)
- Increase the Bezier curve peak height factor in `VisualSunPathSection` (e.g. from `-0.30f` to `-0.65f`) so the Sun Arc Path is noticeably higher, rounder, and more prominently curved.
- Update node coordinate calculations to match the new curve peak height so the nodes sit precisely along the new rounder arc path.

### 2. `PrayerTimesDetailSheet.kt`
Restore rounded corner shapes and horizontal side margins for the bottom 3 components:
1. **Calculation Method Card (`গণনা পদ্ধতি`)**:
   - `shape = RoundedCornerShape(14.dp)`
   - `modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)`
2. **Asr Method Card (`আসরের পদ্ধতি / হানাফী-শাফেয়ী`)**:
   - `shape = RoundedCornerShape(14.dp)`
   - `modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)`
3. **Copy Prayer Times Button (`সময়সূচির টেক্সট কপি করুন`)**:
   - `shape = RoundedCornerShape(14.dp)`
   - `modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)`

---

## Verification
- Run `compile_applet` to verify compilation.
- Verify visual curvature of the Sun Arc path and the floating rounded appearance of the bottom 3 cards.
