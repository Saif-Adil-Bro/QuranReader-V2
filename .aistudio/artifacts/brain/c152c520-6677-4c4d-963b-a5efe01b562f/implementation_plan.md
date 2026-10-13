# Implementation Plan - Ayah Detail Modal Fullscreen & Padding Fix

Fix the top margin/padding and hide background elements in the newly created Ayah Detail Modal, as well as hiding the bottom audio player layer while the modal is open.

## User Requirements & Decisions
1. **Top Area:** Fullscreen display that completely covers the main app bar ("কুরআন রিডার") and extends cleanly across the status bar area without extra top margin/padding.
2. **Bottom Area:** Remove extra footer padding and hide the bottom floating audio player layer whenever the detail modal is visible.

---

## Proposed Changes

### 1. `app/src/main/java/com/example/ui/components/AyahTafsirDetailModal.kt`
- Configure `DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)` so the modal window covers 100% of the screen.
- Set the top-level container background to full opaque surface color so the underlying main screen top bar does not show through.
- Remove redundant top status bar padding / margin in the top navigation row (`Surah` & `Ayah` selector header bar).
- Clean up bottom navigation bar inset and footer padding.

### 2. `app/src/main/java/com/example/ui/screens/SurahDetailScreen.kt`
- Condition the rendering/visibility of the bottom floating audio player (`FloatingAudioPlayer`) so that it is hidden when `selectedAyahDetailIndex != null` (modal is open).

---

## Verification Plan
1. Run `compile_applet` to ensure zero compilation or build errors.
2. Verify that opening the Ayah detail modal covers the top bar completely and removes unnecessary padding at top and bottom.
3. Confirm that the bottom audio player is hidden when the detail modal is open.
