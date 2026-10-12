# Implementation Plan: Bangladesh Real Hijri Date Alignment for Default (0) Offset

Calibrate the core Hijri calendar calculation in `HijriCalendarUtil` so that the default zero offset (`০ দিন স্বাভাবিক`) accurately displays Bangladesh's real Islamic lunar date (**২৯ রবিউস সানী ১৪৪৮**) instead of the astronomical ICU civil calendar's lagging date (**২৮ রবিউস সানী**).

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The user confirmed the following requirements:
> 1. **স্বাভাবিক (০) অফসেটের তারিখ:** বাংলাদেশের বর্তমান বাস্তব তারিখ **২৯ রবিউস সানী** হতে হবে।
> 2. **পপআপ ডায়ালগের ডিফল্ট নির্বাচন:** পপআপ ডায়ালগ খোলার সময় সবসময় **স্বাভাবিক (০)** অফসেট নির্বাচিত থাকবে এবং এটি শুরুতেই রিয়েল তারিখ (২৯ রবিউস সানী) প্রদর্শন করবে।

- **Confirmed Decision 1**: Calibrate the base Hijri calendar calculation in `HijriCalendarUtil.kt` by applying Bangladesh's standard local lunar alignment (`+1` day base offset against ICU's astronomical civil computation).
- **Confirmed Decision 2**: Ensure that when offset is `0` (স্বাভাবিক), the date is **২৯ রবিউস সানী ১৪৪৮**, giving users the accurate date out-of-the-box without requiring manual adjustments.
- **Confirmed Decision 3**: Retain full manual adjustment range (`-2, -1, 0, +1, +2`) in `ModernHijriAdjustmentDialog` and Settings so users can still adapt to any local crescent sighting variations.

---

### 1. Overview & Root Cause Analysis

- **Why was 'স্বাভাবিক (০)' showing ২৮ রবিউস সানী?**
  - Android's `android.icu.util.IslamicCalendar()` computes dates using astronomical civil moon conjunctions (Umm al-Qura / astronomical standard).
  - In Bangladesh and the Indian Subcontinent, the Islamic calendar is strictly established by Islamic Foundation Bangladesh (জাতীয় চাঁদ দেখা কমিটি) via physical crescent sighting, which occurs approximately 1 day later than astronomical conjunction.
  - Consequently, raw ICU calculation without regional calibration produced **২৮ রবিউস সানী** on Sunday, 11 October 2026, while the real date in Bangladesh was **২৯ রবিউস সানী**.
  - Users previously had to manually apply a `+1` offset in Settings just to see the real date.
- **The Solution**:
  - Introduce `BASE_BANGLADESH_OFFSET = 1` into `HijriCalendarUtil.calculateHijriDate`.
  - When `offsetDays = 0` (স্বাভাবিক), the effective offset is aligned to Bangladesh's calendar, yielding **২৯ রবিউস সানী**.
  - The dialog opens with `০ দিন (স্বাভাবিক)` selected, immediately showing **২৯ রবিউস সানী ১৪৪৮ হিজরী**.

---

### 2. User Experience & Visual Design

- **ModernHijriAdjustmentDialog Preview at 0 Offset**:
  - *Gregorian Date*: `রবিবার, ১১ অক্টোবর ২০২৬`
  - *Hijri Date Display*: `২৯ রবিউস সানী ১৪৪৮ হিজরী` (Arabic: `29 ربيع الثاني 1448 هـ`)
  - *Selected Pill*: `স্বাভাবিক (০)`
  - *Stepper Display*: `০ দিন (স্বাভাবিক)`
- **Interactive Adjustments**:
  - If user selects `-১ দিন`: shows `২৮ রবিউস সানী`
  - If user selects `০ দিন (স্বাভাবিক)`: shows `২৯ রবিউস সানী` (Real current date)
  - If user selects `+১ দিন`: shows `৩০ রবিউস সানী`
- **Application-wide Consistency**:
  - Home screen hero waqt card, top bar calendar indicator, prayer times sheet, and monthly grid will all show the real Bangladesh date by default.

---

### 3. Key Technical Decisions & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                      HijriCalendarUtil                      │
│                                                             │
│   LocalDate (e.g. 2026-10-11) + User Offset (0 / +/-)       │
│                             │                               │
│                             ▼                               │
│   Total Offset = User Offset + BASE_BANGLADESH_OFFSET (1)   │
│                             │                               │
│                             ▼                               │
│        Output: 29 Rabi us-Sani 1448 AH (Real Date)          │
└─────────────────────────────────────────────────────────────┘
```

- **File to Modify**: `app/src/main/java/com/example/utils/HijriCalendarUtil.kt`
  - In `calculateHijriDate(date: LocalDate, offsetDays: Int)`:
    - Apply `val totalOffset = offsetDays + BASE_BANGLADESH_OFFSET` (where `BASE_BANGLADESH_OFFSET = 1`).
    - Pass `totalOffset` to both `IslamicCalendar` and fallback `HijrahDate`.
- **Dialog & Settings**:
  - `ModernHijriAdjustmentDialog.kt` automatically reflects the calibrated date preview when `selectedOffset == 0`.
  - Settings screen item shows the calibrated date accurately when offset is reset to 0.

---

### Verification Plan

1. **Compilation Check**:
   - Run `compile_applet` to ensure error-free compilation.
2. **Date Verification**:
   - Verify that for `offsetDays = 0`, `HijriCalendarUtil.getHijriDate(LocalDate.of(2026, 10, 11), 0)` outputs `hijriDay == 29`, `hijriMonth == 4` (রবিউস সানী), `hijriYear == 1448`.
   - Verify that `ModernHijriAdjustmentDialog` shows "২৯ রবিউস সানী ১৪৪৮ হিজরী" when `০ দিন (স্বাভাবিক)` is selected.
