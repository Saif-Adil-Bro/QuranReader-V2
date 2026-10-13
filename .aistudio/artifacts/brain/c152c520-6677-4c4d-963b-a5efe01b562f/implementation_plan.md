# Implementation Plan - Ayah Detail Modal UI Fixes, Full-Width Cards, Collapsible Tafsirs & Multiple Tafsirs

Address the bottom action bar clipping issue, expand cards to 100% full width, implement collapsible accordion Tafsir cards, and render all selected Tafsirs sequentially for the active Ayah.

## User Requirements
1. **Fix Bottom Action Bar Clipping:**
   - Ensure "তিলাওয়াত শুনুন", Bookmark, Copy, Share, and Photo Card buttons are completely visible and well-padded above the system navigation/gesture bar without clipping or extending off-screen.
2. **Full-Width Cards (100% Width):**
   - Stretch the Ayah Header Card, Bengali Translation Card, and Tafsir Cards to 100% container width (`fillMaxWidth()`) with clean edge alignment.
3. **Collapsible Accordion Tafsir Cards:**
   - Display each Tafsir in its own collapsible accordion card with an expand/collapse toggle header ("তাফসীর [নাম]", arrow indicator, character count or preview).
4. **Multiple Selected Tafsirs Support:**
   - Retrieve and render all user-selected Tafsirs sequentially for the current Ayah so users can read multiple commentary sources side-by-side.

## Proposed Changes

### 1. `AyahTafsirDetailModal.kt` - Layout & Inset Fix
- Update `Column` structure inside `Dialog`:
  - `Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding())`
  - Ensure `HorizontalPager` uses `Modifier.weight(1f).fillMaxWidth()`.
  - Fix bottom action bar `Surface`: apply `navigationBarsPadding()` on the container surface and reduce button height slightly (e.g., 44.dp - 46.dp) with `contentPadding` to prevent text truncation ("তিলাওয়াত শুনুন").

### 2. `AyahTafsirDetailModal.kt` - Full Width & Accordion Cards
- Modify `AyahTafsirPageContent`:
  - Update card padding to use `fillMaxWidth()` with consistent screen margins (e.g., `padding(horizontal = 12.dp)`).
  - Create `CollapsibleTafsirCard` composable:
    - Displays Tafsir Title (e.g., "তাফসীর আল-মুয়াসসার", "তাফসীর ইবনে কাসীর").
    - Includes expand/collapse state (`var isExpanded by remember { mutableStateOf(true) }`).
    - Header row with expand icon (`KeyboardArrowUp` / `KeyboardArrowDown`).
    - Smooth `AnimatedVisibility` for expanding/collapsing Tafsir body text.

### 3. Multiple Tafsirs Integration (`SurahDetailViewModel.kt` / `AyahTafsirDetailModal.kt`)
- Pass all selected Tafsir IDs / names into `AyahTafsirDetailModal`.
- Load Tafsir content for each selected Tafsir ID asynchronously or from cache/database.
- Loop over all selected Tafsir items to render a stack of `CollapsibleTafsirCard` components for the active Ayah.

## Verification Plan
1. Open Ayah Detail Modal from any Ayah card.
2. Verify bottom action bar ("তিলাওয়াত শুনুন", Bookmark, Copy, Share) is fully visible above system navigation bar without cutoff.
3. Verify cards take full width (100%).
4. Verify each Tafsir card collapses and expands smoothly when clicked.
5. Select multiple Tafsirs in Settings and verify all selected Tafsirs render sequentially in individual collapsible accordion cards.
