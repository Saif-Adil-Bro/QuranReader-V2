# Fix Ayah Share Crash & Restore Floating Mushaf Tab

## Title & Summary
This plan resolves two targeted issues:
1. Fixes an application crash when clicking the "Share" button inside the Ayah Tafsir Detail Modal by safely launching `ACTION_SEND` with `FLAG_ACTIVITY_NEW_TASK`.
2. Restores the missing "মুসহাফ" (Mushaf) tab to the floating navigation toggle bar that appears while scrolling in `SurahDetailScreen`.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Both reported issues have clear, non-breaking solutions identified directly from the code and crash log:
> - **Share Crash Resolution**: The crash is an `AndroidRuntimeException` caused by calling `startActivity` without `Intent.FLAG_ACTIVITY_NEW_TASK` from a Dialog context. Adding `FLAG_ACTIVITY_NEW_TASK` to the chooser intent immediately resolves the crash.
> - **Floating Menu Tab Restoration**: The sticky floating toggle bar in `SurahDetailScreen` previously had 3 tabs (`লিস্ট`, `শব্দার্থ`, `তাফসির`), missing the 4th tab `মুসহাফ` (`ViewMode.MUSHAF`), whereas the top static bar had all 4 tabs. We will add the Mushaf toggle button to the floating bar and link it to scroll to `ViewMode.MUSHAF`.

---

## 1. Overview & Core Concept
- **Ayah Share Intent**: Users can seamlessly share an Ayah with its Arabic script, Bengali translation, Tafsir text, Surah name, and Ayah number to any messaging or social app without triggering runtime crashes.
- **Continuous Reading Mode Navigation**: When scrolling down through long Surahs, users maintain quick one-tap access to all 4 reading modes (`লিস্ট`, `শব্দার্থ`, `তাফসির`, `মুসহাফ`) via the floating pill menu.

---

## 2. User Experience & Visual Design

### Floating Mode Switcher (Scroll State)
The pill-shaped floating bar at the top of `SurahDetailScreen` will contain all four modes with their icons and localized labels:
1. **লিস্ট (List)** (`Icons.Default.List`)
2. **শব্দার্থ (Word-by-word)** (`Icons.Outlined.Book`)
3. **তাফসির (Tafsir)** (`Icons.Outlined.Info`)
4. **মুসহাফ (Mushaf)** (`Icons.Outlined.MenuBook`)

Selecting any tab triggers smooth animated scrolling to the corresponding view mode page.

### Share Button Interaction
- Tapping the share icon (bottom right in `AyahTafsirDetailModal`) builds formatted text containing:
  - Arabic verse text
  - Bengali translation
  - Tafsir text (HTML tags cleaned)
  - Reference: `[সূরা <Surah Name> : আয়াত <Ayah No>]`
- Opens the native Android share sheet with `Intent.FLAG_ACTIVITY_NEW_TASK` safely handled within a `try/catch` guard.

---

## 3. Key Product Decisions & Trade-Offs

| Decision | Chosen Approach | Rationale |
|---|---|---|
| **Share Intent Context Safety** | `chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)` + `try/catch` | Resolves `AndroidRuntimeException` when `startActivity` is invoked from a Dialog/wrapped context. |
| **Floating Menu Layout** | 4-item pill `Row` with `Arrangement.SpaceBetween` | Matches the static header bar layout and ensures consistent UX across scrolled and non-scrolled states. |
| **Paging Synchronization** | `pagerState.animateScrollToPage(pageOrder.indexOf(ViewMode.MUSHAF))` | Maintains identical swipe & tap synchronization with the horizontal pager. |

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   SurahDetailScreen                    │
├──────────────────────────┬─────────────────────────────┤
│ Top Static Bar (4 tabs)  │ Sticky Floating Bar (4 tabs)│
│  [List][Words][Tafsir]   │  [List][Words][Tafsir]      │
│  [Mushaf]                │  [Mushaf] <── Restored!     │
└─────────────┬────────────┴───────────────┬─────────────┘
              ▼                            ▼
┌────────────────────────────────────────────────────────┐
│                 HorizontalPager State                  │
│       (List / Reading / Tafsir / Mushaf Pages)         │
└────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────┐
│               AyahTafsirDetailModal                    │
├────────────────────────────────────────────────────────┤
│ Bottom Action Bar: [Play] [Bookmark] [Copy] [Share]    │
│                                               │        │
│                                               ▼        │
│                                 Share Intent Chooser   │
│                             + FLAG_ACTIVITY_NEW_TASK   │
│                               (Crash-free invocation)  │
└────────────────────────────────────────────────────────┘
```

### Proposed Changes:

1. **`app/src/main/java/com/example/ui/components/AyahTafsirDetailModal.kt`**:
   - In the `Share` `IconButton` `onClick` handler (around line 471):
   - Add `addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)` to the `Intent.createChooser(intent, ...)` intent.
   - Wrap the `context.startActivity(...)` invocation in a `try/catch` block for defensive robustness.

2. **`app/src/main/java/com/example/ui/screens/SurahDetailScreen.kt`**:
   - In the floating bar `Row` (around lines 740–753):
   - Add `ViewModeToggle(stringResource(R.string.tab_mushaf), Icons.Outlined.MenuBook, currentMode == ViewMode.MUSHAF)` with click handler `coroutineScope.launch { pagerState.animateScrollToPage(pageOrder.indexOf(ViewMode.MUSHAF)) }`.
