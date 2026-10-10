# Implementation Plan - Hero Section Waqt Synchronization with Full-Page Prayer Timetable

## Summary of Goal
Synchronize the Hero Section's running prayer time card (`DarkEmeraldPrayerStatusPanel`) with the exact sequence, calculation, and display logic used in the full-page prayer timetable screen (`PrayerSunPathCard.kt`). Specifically, when sunrise finishes, it will display "চাশত ও দুহা" (Chasht & Duha / Ishraq & Duha) along with its exact time range (`duhaRange`), and during forbidden periods (Sunrise Makruh, Zawal Makruh, Sunset Makruh), it will display the specific Makruh reason and time range.

---

## Code Analysis & Implementation Details

### 1. Calculations in `FullWidthPrayerHeroSection.kt`
- Extract `sunriseMinutes`, `dhuhrMinutes`, `duhaStartMinutes` (sunrise + 16 min), and `duhaEndMinutes` (dhuhr - 4 min / zawal start).
- Determine current state:
  1. **Sunrise Makruh (`isSunriseMakruhNow`):**
     - `isForbiddenTime` = true (Red crimson theme)
     - Title: "সূর্যোদয় (মাকরূহ)" / "Sunrise (Makruh)"
     - Time Range: `schedule.forbiddenMorningRange`
     - Countdown: Remaining time until Chasht starts
  2. **Chasht & Duha (`isDuhaNow`):**
     - `isForbiddenTime` = false (Emerald green theme)
     - Title: "চাশত ও দুহা" / "Chasht & Duha"
     - Time Range: `schedule.duhaRange` (e.g. "০৬:১৬ - ১:৩৪")
     - Countdown: Remaining time until Zawal / Dhuhr
  3. **Zawal Makruh (`isZawalMakruhNow`):**
     - `isForbiddenTime` = true (Red crimson theme)
     - Title: "দ্বিপ্রহর (জাওয়াল)" / "Midday (Zawal)"
     - Time Range: `schedule.forbiddenNoonRange`
     - Countdown: Remaining time until Dhuhr starts
  4. **Sunset Makruh (`isSunsetMakruhNow`):**
     - `isForbiddenTime` = true (Red crimson theme)
     - Title: "সূর্যাস্ত (মাকরূহ)" / "Sunset (Makruh)"
     - Time Range: `schedule.forbiddenEveningRange`
     - Countdown: Remaining time until Maghrib
  5. **Regular Prayers (Fajr, Dhuhr, Asr, Maghrib, Isha):**
     - `isForbiddenTime` = false (Emerald green theme)
     - Title: Current prayer name
     - Time Range: Prayer start to end time
     - Countdown: Time remaining in current waqt or until next prayer

### 2. UI Updates in `DarkEmeraldPrayerStatusPanel`
- Update `DarkEmeraldPrayerStatusPanel` to accept and render these synchronized titles, time ranges, and countdown indicators.

---

## Verification Strategy
- Compile the applet using `compile_applet` to verify no syntax or compilation issues exist.
- Test time state transitions (Fajr -> Sunrise Makruh -> Chasht & Duha -> Zawal Makruh -> Dhuhr/Jumuah -> Asr -> Sunset Makruh -> Maghrib -> Isha) to ensure display parity with the full-page timetable.
