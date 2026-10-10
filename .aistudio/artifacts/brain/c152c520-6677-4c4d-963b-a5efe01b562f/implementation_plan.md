# Implementation Plan: Full-Month Horizontally Scrollable Date Strip with Centered Active Date

## Problem Analysis
The previous change used a static `Row` with 7 fixed days to keep today in the middle position. Because it was a fixed `Row`, users could not scroll left or right to view other days of the month.

## Proposed Solution
Convert the date selector strip into a horizontally scrollable `LazyRow` encompassing all days of the selected month (1st to last day of the month), while ensuring that today (or the active selected date) is centered in the exact horizontal middle of the viewport on initial load and when clicked.

---

## Detailed Changes

### `PrayerTimesDetailSheet.kt`
1. **Full-Month Date Generation**:
   - Generate all dates in the active month (`1..yearMonth.lengthOfMonth()`).
2. **Centered Viewport with `BoxWithConstraints`**:
   - Wrap the `LazyRow` in `BoxWithConstraints` to determine `maxWidth`.
   - Set symmetric content padding: `PaddingValues(horizontal = (maxWidth / 2 - 24.dp).coerceAtLeast(0.dp))` so that any date item (width `48.dp`) scrolled into view rests precisely at the horizontal center of the card.
3. **Smooth Centered Auto-Scroll**:
   - Use `LaunchedEffect(selectedDate.year, selectedDate.monthValue)` or when opening the sheet to automatically scroll today's index (`today.dayOfMonth - 1`) directly into the center.
   - When a user taps any date in the row, update `selectedDate` and animate scroll that tapped date into the center.
4. **Preserve Centered Header**:
   - Retain the centered weekday and date header in the middle of the card header.
5. **Bidirectional Free Scrolling**:
   - Allow free manual left-and-right swiping/scrolling across all days of the month.

---

## Verification
- Run `compile_applet` to confirm successful build.
- Verify that on initial load, today's date sits right in the middle with preceding days on the left and future days on the right.
- Verify smooth left and right horizontal scrolling across all days of the month.
- Verify that clicking any date animates it to the center and updates prayer times.
