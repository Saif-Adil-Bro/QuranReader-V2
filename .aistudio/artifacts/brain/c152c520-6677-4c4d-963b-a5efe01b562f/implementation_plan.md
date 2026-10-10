# Hero Section & Home Screen Layout Optimization

Remove individual prayer countdowns from the 5-column prayer timetable in the hero section, remove the overlapping search box from the home screen, and enlarge key sections and cards with enhanced typography and touch targets for superior readability and spiritual serenity.

## User Review & Critical Decisions

> [!IMPORTANT]
> The changes follow user preferences for distraction-free reading, visual comfort, and cleaner spacing across the home screen.

- **Confirmed Decision 1 (Prayer Timetable Cleanup)**: Remove the two-line countdown status (`statusLine1` & `statusLine2`) underneath each of the 5 prayers in the Hero Section timetable. Reinvest the freed vertical space into larger prayer icons (from 13dp to 17dp), bolder prayer names (10.5sp), and larger time digits (13.5sp), making the prayer times immediately legible at a glance.
- **Confirmed Decision 2 (Search Box Removal)**: Remove the floating `SearchSection` pill overlapping the hero card on the home screen. Provide clean, elegant access to Quran search through a top header action icon, preserving complete search capability without taking up vertical real estate.
- **Confirmed Decision 3 (Section & Content Enlargement)**: Scale up the home screen cards (Quick Action cards like Hafezi, Tajweed, Mushaf, Surah Reading, Daily Ayah/Hadith card, and grid tiles) with enhanced padding (from 12dp to 16dp), larger font sizes (+1.5sp to +2sp), and bolder icons to make all content easily readable without clutter.

---

## 1. Overview & Core Concept

- **What It Does**: Cleans up the top hero prayer timetable by eliminating cluttered live sub-minute countdowns under individual waqt columns. Eliminates the redundant large search pill from the home screen, allowing the Hero section and surrounding cards to breathe with generous padding, bigger fonts, and elevated visual hierarchy.
- **Target Audience / Persona**: Daily Quran reciters and Salah practitioners who value a calm, elegant, distraction-free Islamic interface that is comfortable on the eyes and effortless to navigate.
- **Key Value**: Reduces cognitive clutter, improves reading comfort with larger typography and icons, and reclaims vertical screen real estate for essential Islamic daily content.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Home Screen Glance**: The user opens the app and immediately sees the grand Islamic Emerald Hero card with the current prayer waqt, clean prayer times in the 5-column grid without distracting live countdown text underneath each column, and large legible time digits.
2. **Search Access**: When searching for Surahs, Juz, or Ayahs, the user taps the intuitive search icon in the top header, navigating seamlessly to the dedicated Search screen.
3. **Browsing Daily Cards**: Below the Hero section, the user experiences generously proportioned, beautifully padded cards for reading modes (Hafezi, Tajweed, Surah List), daily duas, and Islamic tools with clear, readable typography.

### Visual Identity & Theme
- **Aesthetic Direction**: Minimalist, serene, luxury Islamic emerald and gold.
- **Hero Timetable Redesign**:
  - Each of the 5 prayer columns (Fajr, Dhuhr, Asr, Maghrib, Isha) features:
    - Golden Vector Icon (`17.dp`, up from `13.dp`)
    - Prayer Name (`10.5.sp`, bold, light green highlight when current)
    - Prayer Time Digits (`13.5.sp`, bold, crisp white / mint green)
    - Clean, comfortable vertical padding without cramped sub-second countdown strings
- **Card Scaling**:
  - Quick action pills & cards: Enhanced height (e.g. `84.dp` from `72.dp`), increased padding (`16.dp`), larger Arabic & Bengali title typography (`15.sp - 17.sp`), and bigger icons (`24.dp - 28.dp`).
  - Hero Section bottom padding adjusted cleanly to remove the 26dp gap previously allocated for the overlapping search pill.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Removal of Individual Countdown Lines in Prayer Columns**
  - *Chosen Approach*: Omit `statusLine1` and `statusLine2` from `PrayerColumnItem` inside `FiveColumnPrayerTimetable`. Retain the primary running waqt countdown inside the bottom status panel (Capsule 1 & 2), where live time remaining is already prominently displayed.
  - *Why*: Having 5 separate micro-countdowns in tiny 7.2sp fonts inside narrow columns created extreme visual density and distraction. The bottom panel already provides the exact waqt remaining countdown.
  - *Alternatives Considered*: Showing abbreviated countdowns (e.g., "১০ মি.") — rejected because removing them completely creates a much cleaner, premium aesthetic.

- **Decision 2: Seamless Search Migration**
  - *Chosen Approach*: Remove the full-width search input card from the middle of the home feed; place a refined Search action in the top header bar next to notifications/calendar.
  - *Why*: A large persistent search bar takes up valuable vertical space. Moving it to the header maintains instant access while letting content expand.

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                       HomeScreen                            │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Top Header Bar (Location, Date, [🔍 Search], Settings)│  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ FullWidthPrayerHeroSection                            │  │
│  │  - Top Info Header (Date | Location | Sunrise/Sunset) │  │
│  │  - 5-Column Timetable (Larger Icons, Names, Digits)   │  │
│  │  - Bottom Emerald Prayer Status & Sehri/Iftar Panel   │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Enlarged Quick Action & Reading Cards                 │  │
│  │  - Hafezi Mode, Tajweed Mode, Surah List              │  │
│  │  - Daily Ayah, Hadith & Dua Cards                     │  │
│  └───────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Component & State Mapping
- `FullWidthPrayerHeroSection.kt`:
  - Simplify `PrayerColumnItem` to render icon, title, and digits with enhanced sizes (`17.dp` icon, `10.5.sp` name, `13.5.sp` digits).
  - Remove `statusLine1` and `statusLine2` parameters and calculations from `FiveColumnPrayerTimetable`.
  - Adjust container padding and vertical dividers.
- `HomeScreen.kt`:
  - Remove `SearchSection` and overlapping `Box`/`Spacer` structure.
  - Add search icon affordance in the top header or maintain top bar action for search navigation.
  - Adjust padding and dimensions of quick action cards and content sections for optimal legibility.
