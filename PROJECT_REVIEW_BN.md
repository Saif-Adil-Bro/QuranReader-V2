# 🔍 QuranReader-V2 — সম্পূর্ণ প্রজেক্ট রিভিউ

**রিভিউয়ার পারস্পেক্টিভ:** Professional Android Developer (Kotlin/Compose) + Islamic Content Specialist + Bug Hunter
**তারিখ:** ২০ অক্টোবর ২০২৬ | **ব্রাঞ্চ:** `arena/a8c7572a-quranreader-v2`
**স্কোপ:** 185টি Kotlin ফাইল (~85,000 লাইন), 21MB Quran ডেটাবেজ, 4টা JSON কনটেন্ট ফাইল, CI, সিকিউরিটি কনফিগ

---

## 📊 সারসংক্ষেপ (Executive Summary)

| বিষয় | রেটিং | মন্তব্য |
|---|---|---|
| Islamic কনটেন্ট ডেটা (কুরআন/দোয়া/মানযিল) | ⭐⭐⭐⭐⭐ | 6236 আয়াত ১০০% ভেরিফাইড, সব রেফারেন্স ভ্যালিড |
| নামাজের সময় হিসাব (বাংলাদেশ) | ⭐⭐⭐⭐ | NOAA/Meeus ইঞ্জিন অতি নির্ভুল, কিন্তু হাই-ল্যাটিচিউডে ভাঙা |
| কিবলা কম্পাস | ⭐⭐⭐ | ফর্মুলা সঠিক, কিন্তু ফিল্টারে র‍্যাপ-অ্যারাউন্ড বাগ |
| হিজরি ক্যালেন্ডার | ⭐⭐⭐⭐ | ICU-based, অ্যাডজাস্টমেন্ট আছে |
| বাংলা ক্যালেন্ডার (বঙ্গাব্দ) | ⭐⭐ | **১ দিনের ভুল** (মধ্য-অক্টোবর → মধ্য-এপ্রিল) |
| Architecture | ⭐⭐⭐ | Manual DI ঠিক আছে, কিন্তু god-files |
| Security | ⭐⭐ | ক্রিটিক্যাল সমস্যা আছে (নিচে দেখুন) |
| Testing | ⭐ | শুধু টেমপ্লেট টেস্ট, কোনো রিয়েল টেস্ট নেই |

**মোট ইমপ্রেশন:** একটা ফিচার-সমৃদ্ধ, বাংলা-ফার্স্ট ইসলামিক অ্যাপ যার **ডেটা কোয়ালিটি ও নামাজের সময়ের কোর ম্যাথ genuinely ভালো**। কিন্তু ৩টা ক্রিটিক্যাল সিকিউরিটি ইস্যু, ২-৩টা ইসলামিক-কারেক্টনেস বাগ, আর রিলায়েবিলিটি ঝুঁকি ঠিক না করলে প্রোডাকশনে যাওয়া ঝুঁকিপূর্ণ।

---

## ✅ যা সত্যিই ভালো হয়েছে (এগুলো প্রশংসার দাবিদার)

1. **কুরআন ডেটাবেজ নির্ভুল** — `quran.db` ভেরিফাই করেছি:
   - 6236 আয়াত, 114 সূরা — প্রতিটা সূরার আয়াত-সংখ্যা Tanzil স্ট্যান্ডার্ডের সাথে **100% ম্যাচ** (আল-ফাতিহা ৭, আল-বাকারা ২৮৬... আন-নাস ৬)
   - 30 পারা, 604 পৃষ্ঠা (মাদানি লেআউট) ✓
   - 9:128–129 সম্পূর্ণ আছে ✓, 9:1-এ বিসমিল্লাহ নেই ✓, 27:1 = طسم ✓
   - Bengali অনুবাদ (মুহিউদ্দীন খান স্টাইল) + English (Saheeh International) প্রিলোডেড
2. **নামাজের সময়ের সৌর গণিত প্রায় নিখুঁত** — আমি কোডটা রিপ্লিকেট করে যাচাই করেছি:
   - ঢাকা, ২০ অক্টোবর ২০২৬: ফজর ৪:৪২ / সূর্যোদয় ৫:৫৮ / যোহর ১১:৪৪ / আসর (হানাফি) ৩:৫১ / মাগরিব ৫:২৯ / ইশা ৬:৪৪ — ইসলামিক ফাউন্ডেশনের ক্যালেন্ডারের সাথে ১-২ মিনিটের মধ্যে ম্যাচ
   - মক্কা (উম্মুল কুরা): 5:02/6:18/12:06/4:15/5:54/7:24 ✓
   - NOAA/Meeus ephemeris + EoT সঠিকভাবে ইমপ্লিমেন্ট করা; Asr-এর `atan(1/(factor + tan(|lat−dec|)))` ফর্মুলা শুদ্ধ; হানাফি ডিফল্ট ✓
   - সব ক্যালকুলেশন মেথডের অ্যাঙ্গেল (MWL 18/17, Karachi 18/18, Egypt 19.5/17.5, Umm al-Qura 18.5+90min, ISNA 15/15, Singapore 20/18, Kuwait 18/17.5, Qatar 18+90min) — সব স্ট্যান্ডার্ড ভ্যালু ✓
3. **কনটেন্ট রেফারেন্স ভ্যালিডিটি** — manzil.json (79), subjectwise_topics.json (737), সবগুলো সূরা:আয়াত রেফারেন্স ডেটাবেজের সীমার ভেতরে — **একটাও ইনভ্যালিড নেই**
4. **দোয়া কনটেন্ট** — হিসনুল মুসলিম (বাংলা) স্টাইলের 77 + 51টা দোয়া, আরবি টেক্সট + উচ্চারণ + অনুবাদ + রেফারেন্স সহ — চমৎকার স্ট্রাকচার
5. **ওমরাহ কনটেন্ট** — হানাফি ফিকহ্‌ অনুযায়ী নির্ভুল (ইহরামের নিষিদ্ধ কাজ, তালবিয়া, তাওয়াফ, সায়ী — সব ঠিক)
6. **কিবলা বিয়ারিং ফর্মুলা** সঠিক (ঢাকা → ২৭৭.৫° WNW ভেরিফাইড) + বাবল-লেভেল দিয়ে ফোন সমতল চেক — চমৎকার UX ডিটেইল
7. **অফলাইন-ফার্স্ট আর্কিটেকচার** — প্রার্থনার সময় সম্পূর্ণ অফলাইন ক্যালকুলেট হয় (কোনো API নির্ভরতা নেই); কুরআন প্রি-লোডেড
8. **Media3 MediaSessionService** সঠিকভাবে ব্যবহার (audio focus, becoming-noisy, wake mode)
9. **Bismillah হ্যান্ডলিং** — সূরা ১ ও ৯ বাদে হেডার দেখায় ✓
10. **বাংলা লোকালাইজেশন** — values/strings এর 166টা কী bn ও ar দুই জায়গাতেই সম্পূর্ণ

---

## 🔴 CRITICAL — এগুলো আগে ঠিক করুন

### C1. Release Keystore রিপোজিটরিতে কমিট করা আছে
```
app/my-release-key.jks   (2,772 bytes)
```
**সমস্যা:** আপনার রিলিজ-সাইনিং কী পাবলিক। এটা দিয়ে যে কেউ একই আইডেন্টিটির APK সাইন করতে পারবে। বিপজ্জনক দিকটা হলো `app/build.gradle.kts`-এ fallback:
```kotlin
storePassword = System.getenv("STORE_PASSWORD") ?: "android"
keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
```
যদি আসল পাসওয়ার্ড এগুলোই হয় (বা ডিক্টেড/ব্রুট-ফোর্স করা সম্ভব হয়), Play Store আপডেট-চেইন কম্প্রোমাইজড।
**ফিক্স:** ফাইলটা git থেকে সরান + `git filter-repo` দিয়ে history purge + **নতুন keystore বানিয়ে key reset** করুন + `.gitignore`-এ `*.jks` যোগ করুন।

### C2. Firebase API Key হার্ডকোডেড (2 জায়গায়)
- `QuranApplication.kt:40` → `.setApiKey("AIzaSyAFud_Pg8hJ_WjLSsf6HYNA7zzSzibermc")`
- `.github/workflows/appetizeConnecter.yml` → google-services.json fallback-এ একই key

Firebase Web API key কঠোরভাবে সিক্রেট না হলেও, পাবলিক রিপোতে কমিট করা + খোলা security rules থাকলে abuse (Firestore read/write spam, quota drain) সম্ভব। **ফিক্স:** Firebase Console থেকে key rotate করুন, CI-তে `secrets.GOOGLE_SERVICES_JSON` ব্যবহার করুন, fallback হটকোড মুছুন।

### C3. User Data DB-তে `fallbackToDestructiveMigration()`
`data/local/QuranDatabase.kt` — এই DB-তে **ব্যবহারকারীর বুকমার্ক ও মুখস্থ করা পৃষ্ঠা** (MemorizedPageEntity) থাকে। ভবিষ্যতে ভার্সন বাম্প করলে (v2→v3) **সবার সব ডেটা মুছে যাবে**। কনটেন্ট DB-র (quran.db) জন্য এটা ঠিক আছে, কিন্তু user DB-র জন্য প্রপার `Migration` লিখুন।

### C4. AI-জেনারেটেড "তাফসির" — দ্বিমুখী সমস্যা
`AiRepository.kt:10`:
```kotlin
modelName = "gemini-1.5-flash"
```
- **প্রযুক্তিগত:** gemini-1.5-flash ডিপ্রিকেটেড/রিটায়ার্ড — এখন কল করলে fail করবে। `gemini-2.0-flash`+ এ আপগ্রেড করুন।
- **শরয়ী:** LLM-এর তৈরি করা তাফসির ভুল/hallucination হতে পারে — ইসলামি দৃষ্টিতে কুরআনের ব্যাখ্যায় ভুল মারাত্মক বিষয়। অ্যাপে যেহেতু প্রমাণিত তাফসির (tafsir_cache — Ibn Kathir, Tafsir Jalalayn ইত্যাদি) আছে, **AI উত্তরকে স্পষ্টভাবে "AI পরামর্শ — সহীহ তাফসির নয়" লেবেল দিন**, নাম বদলে "আয়াত সম্পর্কে প্রশ্ন" করুন।

---

## 🟠 HIGH — Islamic Correctness বাগ

### H1. হাই-ল্যাটিচিউডে ফজর/ইশা সময় ভীষণ ভুল (গ্রীষ্মকালে)
`PrayerTimesCalculator.kt:263`:
```kotlin
return if (cosH > 1.0) 0.0 else if (cosH < -1.0) Math.PI else acos(cosH)
```
আপনার INTERNATIONAL_CITIES-এ **London, Birmingham, Manchester, Leeds, Berlin, Toronto, Montreal** আছে। গ্রীষ্মের solstice-এ লন্ডনে (51.5°N) সূর্য কখনো −18°-এ নামেই না (নিচে নামে মাত্র ~−15°)। তখন `cosH < -1` → PI → **ফজর দেখায় ১:০২ AM, ইশাও ১:০২ AM** — বাস্তবে মসজিদগুলো night-middle রুলে ফজর দেখায় ~২:৩০-৩:০০। আমি গাণিতিকভাবে ভেরিফাই করেছি — এটা নিশ্চিত বাগ।
**ফিক্স:** |lat| > 48.5°-এর জন্য high-latitude rule যোগ করুন (Angle-Based 1/7th বা Night-Middle — MWL recommendation)।

### H2. ইশা ইন্টারভালের প্রায়োরিটি ভুল
`PrayerTimesCalculator.kt:188`:
```kotlin
val effectiveIshaInterval = calculationMethod.ishaFixedIntervalMinutes ?: district.ishaFixedIntervalMinutes
```
মক্কা/দোহা/সৌদি সিটি সিলেক্ট করে ইউজার যদি **Karachi (18°)** বা **MWL** মেথড বেছে নেয়, তারপরও ডিস্ট্রিক্টের 90-মিনিট ইন্টারভাল প্রযোজ্য হয় — ইউজারের স্পষ্ট পছন্দ ওভাররাইট হয়ে যায়। ডিস্ট্রিক্ট-ইন্টারভাল শুধু "default/auto" মেথডে প্রযোজ্য হওয়া উচিত।

### H3. বাংলা ক্যালেন্ডার (বঙ্গাব্দ) ১ দিন পিছিয়ে — নিশ্চিত ভুল
`HijriCalendarUtil.kt:165`:
```kotlin
val monthDays = if (isLeapYear) intArrayOf(31,31,31,31,31,31,30,30,30,30,31,30)
                else intArrayOf(31,31,31,31,31,31,30,30,30,30,30,30)
```
২০১৯ সালের সংস্কারিত (ফসলী) বাংলা ক্যালেন্ডার অনুযায়ী: **বৈশাখ–ভাদ্র = ৫ মাস × ৩১ দিন**, আশ্বিন–চৈত্র = ৭ মাস × ৩০, গ্রেগরিয়ান লিপইয়ারে ফাল্গুন ৩১। আপনার অ্যারেতে **৬টা 31** আছে (আশ্বিনে অতিরিক্ত ১ দিন) — বছর 366/367 দিনের হয়ে যায়।
**আমার সিমুলেশনে প্রমাণিত:** আজ ২০ অক্টোবর ২০২৬-এ অ্যাপ দেখায় **৪ কার্তিক ১৪৩৩**, সরকারি ক্যালেন্ডারে **৫ কার্তিক ১৪৩৩**। মধ্য-অক্টোবর থেকে মধ্য-এপ্রিল পর্যন্ত সব তারিখ ১ দিন পিছিয়ে থাকে (যেমন ১৩ এপ্রিল দেখায় ২৯ চৈত্র, হবে ৩০ চৈত্র)।
এছাড়া `date.isLeapYear` দিয়ে ফাল্গুন-লিপ চেক ভুল — এপ্রিল-ডিসেম্বর পর্যন্ত তারিখের ক্ষেত্রে ফাল্গুন পরের গ্রেগরিয়ান বছরের ফেব্রুয়ারিতে পড়ে। স্প্যানে Feb 29 আছে কিনা দেখুন।
**ফিক্স:** `intArrayOf(31,31,31,31,31,30,30,30,30,30, if (falgunLeap) 31 else 30, 30)`

### H4. দুইটা ভিন্ন মুশাফ পেজ-লেআউট একসাথে মেশানো → ভুল পেজে নেভিগেশন
- `HafeziQuranData` = **610-পৃষ্ঠার** হাফেজি লেআউট (আন-নাবা পৃ. 586-এ, পারা-30 = 25 পৃষ্ঠা)
- `quran.db`-র `page` কলাম = **604-পৃষ্ঠার** মাদানি লেআউট (আন-নাবা পৃ. 582-এ)
- `MushafRepository`-র "indopak" স্টাইল = android.quran.com-এর **604-পৃষ্ঠার ইমেজ**, কিন্তু `totalPages` ডিফল্ট 604 রেখে juz-নেভিগেশনে `HafeziQuranData.getParaStartPage()` (610-লেআউট) **নির্দ্বিধায়** ব্যবহার হয় (`MushafViewerScreen.kt:740`)

**ফল:** Indo-Pak ইমেজ মুশাফে পারা ৩০ সিলেক্ট করলে ইউজার পড়বে ৪ পৃষ্ঠা আগের কনটেন্ট; মাঝের পারাগুলোতেও ১-৪ পৃষ্ঠার ড্রিফট। এছাড়া `totalPages = 611` (2টা PDF) vs ম্যাপে 610 এন্ট্রি — পেজ 611-এ টেক্সট রেঞ্জ পাওয়া যায় না।
**ফিক্স:** প্রতিটা MushafStyle-এ `pageLayout: HAFEZI_610 | MADANI_604` ফিল্ড রাখুন; নেভিগেশন ও টেক্সট-ফেচ সেই অনুযায়ী হোক।

### H5. কিবলা কম্পাসের লো-পাস ফিল্টার wrap-around বাগ
`QiblaCompassScreen.kt:158`:
```kotlin
currentAzimuth = currentAzimuth + 0.2f * (azimuthInDegrees - currentAzimuth)
```
অ্যাজিমুথ 359° → 1° ঘোরার সময় diff = −358°, ফলে কম্পাস প্রায় পূর্ণ বৃত্ত ঘুরে যায় (স্পিন-গ্লিচ)। `animateFloatAsState(-azimuth)`-ও একই সমস্যায় পড়ে।
**ফিক্স:** shortest-angular-difference ব্যবহার করুন:
```kotlin
var diff = (azimuthInDegrees - currentAzimuth + 540f) % 360f - 180f
currentAzimuth = (currentAzimuth + 0.2f * diff + 360f) % 360f
```

---

## 🟡 MEDIUM — Android/Reliability ইস্যু

### M1. `GeoapifyService` কখনোই কাজ করবে না (dead code)
`GeoapifyService.kt:22` — `System.getenv("GEOAPIFY_API_KEY")` অ্যান্ড্রয়েড রানটাইমে সবসময় খালি (env vars প্রসেসে inject হয় না)। এটা `.env.example`-এও নেই। ফলে Geoapify পাথ সবসময় skip হয়ে OSM-এ ফলব্যাক করে। BuildConfig/secrets plugin দিয়ে নিন।

### M2. Main thread-এ `runBlocking` — ANR ঝুঁকি
- `PrayerNotificationHelper.kt:253,258` (runBlocking + DataStore read)
- `PrayerNotificationReceiver.kt:126-127`
এগুলো `QuranApplication.onCreate()` থেকে **কোল্ড স্টার্টেই মেইন থ্রেডে** চলে — DataStore-এর প্রথম ডিস্ক রিড + সম্ভবত সিরিয়ালাইজড I/O = স্টার্টআপ jank, কম মেমোরির ডিভাইসে ANR। **ফিক্স:** BroadcastReceiver-এ `goAsync()` + coroutine, অ্যাপ স্টার্টে async।

### M3. `GlobalScope` ব্যবহার (7 জায়গায়)
`DailyMessageReceiver`, `IslamicEventReceiver`, `MyFirebaseMessagingService`, `HijriNewMonthNotificationHelper` ইত্যাদিতে। প্রসেস ডাই করলে কাজ হারায়, lifecycle নেই। `goAsync()`/WorkManager ব্যবহার করুন।

### M4. সব ওয়াক্তের অ্যালার্মে `setAlarmClock()`
`PrayerNotificationHelper.kt:500` — 12টা অ্যালার্মের (ফজর থেকে মাকরূহ পর্যন্ত) প্রতিটাতে `setAlarmClock`। এতে স্ট্যাটাস বারে **সবসময় অ্যালার্ম-ক্লক আইকন** দেখা যায় এবং সিস্টেম এগুলোকে ইউজারের "পরবর্তী অ্যালার্ম" হিসেবে ট্রিট করে। Doze-proof নির্ভরযোগ্যতার জন্য ভালো চয়েস (বাংলাদেশের aggressive OEM battery killers সামলাতে), কিন্তু মাকরূহ-নোটিফিকেশনের মতো low-priority আইটেমে এটা overkill — শুধু আসল ওয়াক্ত + সাহরি/ইফতারে রাখুন।

### M5. `USE_EXACT_ALARM` + `SCHEDULE_EXACT_ALARM` দুটোই ডিক্লেয়ার
`AndroidManifest.xml:15-16` — `USE_EXACT_ALARM` Play Store policy অনুযায়ী শুধু alarm-clock/calendar অ্যাপের জন্য অনুমোদিত; প্রার্থনার অ্যাপে ব্যবহারে **Play review-তে রিজেকশন ঝুঁকি**। আপনি যেহেতু `setAlarmClock` ব্যবহার করছেন (একটাও exact-alarm permission লাগে না), দুটোই সরানো যায়।

### M6. FCM নোটিফিকেশনে `R.mipmap.ic_launcher` small icon
`MyFirebaseMessagingService.kt:132` — mipmap launcher আইকন status bar-এ সাদা/ধূসর বক্স হয়ে রেন্ডার হয়। alpha-only ছোট ড্রয়েবল (monochrome) বানান।

### M7. `allowBackup="true"` + template backup rules — 21MB ডেটাবেজ ক্লাউড ব্যাকআপে
`backup_rules.xml` / `data_extraction_rules.xml` ফাঁকা TODO টেমপ্লেট। ডিফল্টভাবে Room DB ব্যাকআপে যায় — `offline_quran_database_v13` (~13MB) + `quran_wbw` (~8MB) = **25MB auto-backup লিমিট ছুঁইছুঁই**; ব্যাকআপ fail বা বেফাজুল ক্লাউড ব্যবহার হতে পারে। `<exclude domain="database" path="..."/>` যোগ করুন (এগুলো assets থেকে আবার তৈরি হয়)।

### M8. FileProvider অতিরিক্ত খোলা
`file_paths.xml`-এ `<external-path path="."/>` ও `<files-path path="."/>` — পুরো স্টোরেজ এক্সপোজ। শুধু `shared_images/`, `shared_posts/` ফোল্ডারে সীমাবদ্ধ করুন।

### M9. CI সাপ্লাই-চেইন হাইজিন
`appetizeConnecter.yml` — `validate-wrappers: false` + `gradle wrapper` দিয়ে wrapper regenerate (PATH-এর unpinned gradle দিয়ে)। এর চেয়ে বরং repo-তে সঠিক wrapper কমিট করুন, validation চালু রাখুন। এছাড়া রিলিজ APK/AAB artifact হিসেবে আপলোড হচ্ছে — সেগুলো C1-এর কম্প্রোমাইজড কী দিয়ে সাইন হচ্ছে।

### M10. `MushafDownloader`-এ thread-safety
`activeDownloads = mutableMapOf<String, Job>()` (line 29) — UI scope ও IO coroutine থেকে concurrent mutation হয়, sync নেই → বিরল `ConcurrentModificationException`। `ConcurrentHashMap` করুন। এছাড়া `cancelDownload()` পুরো ডিরেক্টরি ডিলিট করে — আংশিক ডাউনলোড থাকলেও।

### M11. পুরনো ডিপেন্ডেন্সি
OkHttp **4.10.0** (2022 — CVE fixes মিস হচ্ছে, 4.12.x আসুন), Compose BOM 2024.09.00 (compileSdk 36-এর সাথে নতুন), `androidx.compose.ui:ui-text-google-fonts:1.7.0` hardcoded। এছাড়া `build.gradle.kts`-এ `compileSdk { version = release(36) {...} }` **দুইবার** লেখা — একটা মুছুন।

---

## 🔵 LOW — Code Quality / রক্ষণাবেক্ষণ

1. **God files:** `SettingsScreen.kt` = 6,796 লাইন, `HomeScreen.kt` = 5,225, `SurahDetailScreen.kt` = 2,899, `PrayerTimesDetailSheet.kt` = 2,870। ভাঙুন — settings-এর জন্য আলাদা feature packages।
2. **এরর হ্যান্ডলিং:** 359টা `catch (e: Exception)` (বেশিরভাগ swallow), 194টা `printStackTrace()`, 46টা `!!`। Crashlytics ডিপেন্ডেন্সি আছে অথচ কোথাও non-fatal logging নেই — `FirebaseCrashlytics.getInstance().recordException(e)` অন্তত critical path-গুলোতে দিন।
3. **কোনো রিয়েল টেস্ট নেই:** `ExampleUnitTest`/`DaoTest`/`RepoTest` সব টেমপ্লেট। যেটা সবচেয়ে দরকার — **`PrayerTimesCalculator`-এর unit test** (জানা তারিখের জন্য Islamic Foundation/UMQ-র প্রকাশিত সময়ের সাথে মিলিয়ে golden-file test), HijriCalendarUtil, Bangla calendar, IndoPakTajweedParser।
4. **রিপো হাইজিন:** `fix_scope.py` (debug স্ক্রিপ্ট), খালি root-level `assets/` ফোল্ডার, `.aistudio/` artifacts, `rootProject.name = "My Application"` — সরান/ঠিক করুন।
5. **`SimpleDateFormat`** 9 জায়গায় — `java.time.format.DateTimeFormatter` ব্যবহার করুন (minSdk 24 + desugaring আছে কি না দেখুন; `java.time` আগেই ব্যবহার করছেন তাই coreLibraryDesugaring নিশ্চিত করুন — build.gradle-এ **desugaring দেখিনি**, minSdk 24-এ `java.time` ব্যবহার **API 26-এর নিচে ক্র্যাশ করবে যদি না** `coreLibraryDesugaringEnabled = true` থাকে! → **এটা সম্ভবত আরেকটা HIGH বাগ, নিচে দেখুন**)।
6. `QuranApplication.onCreate`-এ সব হেল্পার সিরিয়ালি চলে — ঠিক আছে, কিন্তু M2-এর সাথে মিলিয়ে ঠিক করুন।
7. ইন্টারসেপ্টরে Chrome User-Agent spoof — দরকার হলে version-string রাখুন।

### ⚠️ সম্ভাব্য HIGH #2: Java 8+ Date/Time API vs minSdk 24
পুরো কোডবেজ `java.time.LocalDate/LocalTime/ZoneId` ব্যবহার করে, কিন্তু `minSdk = 24` এবং `build.gradle.kts`-এ `coreLibraryDesugaring` **কনফিগার করা নেই** (compileOptions শুধু Java 11 source/target)। Android 7.0 (API 24) ডিভাইসে `java.time` ক্লাস নেই → **NoClassDefFoundError ক্র্যাশ**। যদি কেউ API 24/25 ডিভাইসে চালায় এটা তাৎক্ষণিক ক্র্যাশ। **ফিক্স:** `isCoreLibraryDesugaringEnabled = true` + `coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.x")` যোগ করুন, অথবা minSdk 26 করুন। (বাংলাদেশে API 24-25 ডিভাইস এখনো আছে — এটা গুরুত্বপূর্ণ।)

---

## 🕌 ইসলামিক কনটেন্ট — বিস্তারিত অডিট নোট

| বিষয় | ফলাফল |
|---|---|
| আয়াত সংখ্যা (সব 114 সূরা) | ✅ 100% শুদ্ধ |
| 9:128-129 (কুরআনের শেষ দুই আয়াত) | ✅ উপস্থিত |
| বিসমিল্লাহ লজিক (সূরা ১/৯ বাদ) | ✅ সঠিক |
| আসর হানাফি (shadow ×2) | ✅ ডিফল্ট চালু |
| মাকরূহ সময় (সূর্যোদয়+১৫, জাওয়াল−১২, সূর্যাস্ত−১৫) | ✅ মূলধারার ফিকহ্‌-সম্মত |
| তাহাজ্জুদ শেষ = ফজর−১৫মি; শেষ তৃতীয়াংশ হিসাব | ✅ সঠিক |
| ইসলামিক মধ্যরাত = মাগরিব+অর্ধরাত | ✅ সঠিক |
| লাইলাতুল কদর (বিজোড় রাত ২১-২৯ + ২৭) | ✅ সঠিক উপস্থাপনা |
| আইয়ামে বীজ (১৩-১৪-১৫) + সোম/বৃহস্পতি রোজা | ✅ সঠিক |
| আরাফাহ রোজা, আশুরা (৯+১০), তাশরীক | ✅ সঠিক |
| সাহরি শেষ = ফজর−৩মি | ✅ সতর্কতামূলক, গ্রহণযোগ্য |
| দোহর +১মি / মাগরিব +১মি | ✅ BD প্রচলিত রীতি |
| Waqf চিহ্ন (খ-গ-জ-ল-ম-ص-لا) | ✅ আছে; ❌ ۝ (আয়াত-শেষ) ও সিজদাহ-চিহ্ন WAQF_CHARS-এ নেই |
| শবে বরাত/মেরাজ/ঈদে মিলাদুন্নবী ইভেন্ট | ⚠️ বাংলাদেশি প্রচলিত; কোনো কোনো মতবাদে বিতর্কিত — "বহুল প্রচলিত দিবস" হিসেবে উপস্থাপন করা ভালো |
| AI তাফসির | ❌ উপরে C4 |
| হিজরি (ICU civil) vs চাঁদ দেখা | ✅ ইউজার-অ্যাডজাস্টমেন্ট ডায়ালগ আছে (ভালো সিদ্ধান্ত) |
| তালবিয়া, ইহরাম-নিষেধ, তাওয়াফ-সায়ী নির্দেশনা | ✅ হানাফি ফিকহ্‌ অনুযায়ী নির্ভুল |
| কিবলা ফর্মুলা | ✅ নির্ভুল (ঢাকা 277.5°) |

---

## 🛠️ প্রায়োরিটি রোডম্যাপ

**এই সপ্তাহে (Release ব্লকার):**
1. Keystore রিমুভ + history purge + কী রোটেট (C1)
2. Firebase API key রোটেট + CI থেকে হটকোড সরানো (C2)
3. Java time desugaring চেক/ঠিক করা — API 24/25 ক্র্যাশ (⚠️)
4. বাংলা ক্যালেন্ডার monthDays ফিক্স (H3) — ৫ লাইনের পরিবর্তন
5. হাই-ল্যাটিচিউড নামাজের সময় fallback (H1)

**এই মাসে:**
6. ইশা ইন্টারভাল প্রায়োরিটি + মুশাফ পেজ-লেআউট ম্যাপিং (H2, H4)
7. কম্পাস ফিল্টার wrap-around (H5)
8. User DB-র জন্য প্রপার Room Migration (C3)
9. `runBlocking`/`GlobalScope` → `goAsync()`/WorkManager (M2, M3)
10. AI তাফসির disclaimers + মডেল আপগ্রেড (C4)

**পরবর্তী স্প্রিন্ট:**
11. PrayerTimesCalculator-এর golden-file unit tests (ঢাকা/মক্কা/লন্ডনের প্রকাশিত সময়)
12. Geoapify BuildConfig ফিক্স (M1), FCM small icon (M6), backup rules (M7)
13. ডিপেন্ডেন্সি আপডেট + god-file ভাঙা

---

## 🏁 চূড়ান্ত রায়

এই অ্যাপের **কুরআন-ডেটা লেয়ার ও নামাজের সময়ের কোর ইঞ্জিন বাজারের অনেক অ্যাপের চেয়ে ভালো** — ডেটা ভেরিফিকেশনে একটাও ভুল পাইনি, যা এই ধরনের অ্যাপে বিরল। কিন্তু **সিকিউরিটি স্তম্ভটা (keystore + API key) এখনই ঠিক করা জরুরি**, আর ইসলামিক-কারেক্টনেসের দিক থেকে হাই-ল্যাটিচিউড নামাজের সময় (H1) ও বাংলা ক্যালেন্ডার (H3) ইউজারদের কাছে বিশ্বাসযোগ্যতা নষ্ট করবে। উপরের ৫টা release-blocker ঠিক করলে এটা সত্যিকারের প্রোডাকশন-রেডি অ্যাপ হবে ইনশাআল্লাহ।

*রিভিউ শেষ। কোনো ইস্যুতে fix-PR চাইলে বলুন — আমি এই ব্রাঞ্চেই ঠিক করে দিতে পারি।*
