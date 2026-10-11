# Live Radar Dot Indicator for Active Waqt in Prayer Times Detail Sheet

Replace the text-based `"চলমান"` (running) badge in the prayer times details sheet with a sleek, distraction-free pulsing radar live dot animation matching the home screen's hero section.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the visual styling: **"Subtle pulsing green radar dot matching the home screen"** without any text label.

- **Confirmed Decision 1**: Remove the rounded pill container and text label (`"চলমান"` / `"Active"`) completely from the active waqt row in `PrayerTimesDetailSheet.kt`.
- **Confirmed Decision 2**: Introduce a dedicated `LiveIndicatorDot` with a continuous radar pulse effect (soft expanding green ripple ring + glowing ambient halo + solid core dot) beside the active waqt name.
- **Confirmed Decision 3**: Retain identical 3-column weight distribution (`1.2f` left, `0.6f` middle bell icon, `1.2f` right time text) so the bell icon and timing text alignment never shifts.

---

### 1. Overview & Core Concept

- **What It Does**: During prayer times inspection, the currently ongoing prayer (e.g., Dhuhr, Asr, Isha) is marked with a subtle, radiant live pulsing radar dot in emerald green next to its title. This replaces the bulkier pill badge containing the word "চলমান", offering a much cleaner, more refined, and spiritual Islamic aesthetic.
- **Target Audience / Persona**: Devout Muslim users seeking quick, serene visual feedback on which prayer waqt is presently active without reading clutter.
- **Key Value**: Visual consistency across the application—matching the aesthetic radar dot in the home screen's prayer hero card while preserving rock-solid layout alignment.

---

### 2. User Experience & Visual Design

- **Visual Identity & Theme**:
  - *Dot Core*: Solid emerald dot (`Color(0xFF34D399)` / `Color(0xFF4ADE80)`), `5.dp` diameter.
  - *Ambient Halo*: Glowing middle ring (`8.dp` diameter) with `25%` opacity oscillating softly.
  - *Radar Ripple*: Liquid outer ripple scaling smoothly from `0.6x` to `1.5x` while fading opacity from `0.5` to `0.0` over a `1400ms` loop with `FastOutSlowInEasing`.
  - *Container*: Compact `14.dp` Box positioned immediately adjacent to the waqt name with `6.dp` horizontal spacing.
- **Row Composition & Layout Stability**:
  - The left section containing `Icon + Waqt Name + LiveIndicatorDot` maintains `weight(1.2f)`.
  - The middle section containing the Notification Bell icon remains strictly centered with `weight(0.6f)`.
  - The right section containing the start and end prayer times remains right-aligned with `weight(1.2f)`.
  - Inactive prayer rows display only the Waqt name without any dot or text badge.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: No Text Label Alongside Dot**
  - *Chosen Approach*: Display only the pulsing live radar dot beside the active prayer name; no `"চলমান"` text badge.
  - *Why*: Eliminates redundant visual noise, preserves horizontal room on compact devices, and matches the user's explicit preference.
- **Decision 2: Reusable Local Live Dot Component**
  - *Chosen Approach*: Add `LiveIndicatorDot` inside `PrayerTimesDetailSheet.kt` matching the exact timing and parameters from `FullWidthPrayerHeroSection.kt`.
  - *Why*: Decouples the component cleanly while ensuring identical 60fps Compose transition performance across both screens.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                 PrayerTimesDetailSheet                      │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                 PrayerDetailRow                     │   │
│   │                                                     │   │
│   │  ┌───────────────┐ ┌──────────────┐ ┌─────────────┐ │   │
│   │  │ Left (1.2f)   │ │ Center (0.6f)│ │ Right (1.2f)│ │   │
│   │  │ Icon + Name + │ │ Notification │ │ Waqt Time   │ │   │
│   │  │ LiveDot (●)   │ │ Bell Icon    │ │ Range       │ │   │
│   │  └───────────────┘ └──────────────┘ └─────────────┘ │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

- **Component Implementation**:
  - `LiveIndicatorDot` uses `rememberInfiniteTransition` animating `pulseProgress` (0f to 1f over 1400ms) and `coreAlpha` (0.7f to 1f over 700ms).
  - In `PrayerDetailRow`, replace the `Box(RoundedCornerShape(100.dp))` text badge block with `LiveIndicatorDot(color = Color(0xFF34D399))`.
  - Update any parameter or call site cleanly without breaking existing preview or sheet callers.

---

### Verification Plan

1. Verify build succeeds with `compile_applet`.
2. Inspect `PrayerDetailRow` rendering:
   - When `isCurrentWaqt == true`, verify emerald live radar dot pulses smoothly next to the prayer name.
   - When `isCurrentWaqt == false`, verify no indicator or badge is shown.
   - Verify bell icon and time text remain in exact horizontal alignment with inactive rows.
