# Implementation Plan: Fix Alignment Shift in Highlighted Prayer Row

## 原因 (Causes of Alignment Shift)
1. **প্যাডিং পার্থক্য (Padding Discrepancy):** 
   রানিং ওয়াক্তের রো (Row)-তে প্যাডিং দেওয়া ছিল `horizontal = 10.dp`, কিন্তু অন্য রো-গুলোতে ছিল `horizontal = 4.dp`। এর ফলে কার্ডের ভেতরের ব্যাকগ্রাউন্ড প্যাডিং-এর পার্থক্যের কারণে চিহ্নিত রো-টি সরে যাচ্ছিল।

2. **ডায়নামিক উইডথ চেঞ্জ (Dynamic Width Shift):** 
   রানিং ওয়াক্তে নামের পাশে "• চলমান" ব্যাজ যুক্ত হওয়ার কারণে বামপাশের কলামের চওড়া (Width) বেড়ে যাচ্ছিল। ফলে মাঝখানের ফেক্সিবল স্পেস ডানদিকে চেপে বেল আইকনটিকে ডানে ঠেলে দিচ্ছিল।

---

## Proposed Fixes

### 1. `PrayerTimesDetailSheet.kt` (`PrayerDetailRow` Component)
- **ইউনিফর্ম হরাইজন্টাল প্যাডিং (Uniform Horizontal Padding):**
  - সকল রো (Row)-এর জন্য প্যাডিং একই রাখা হবে: `padding(horizontal = 8.dp, vertical = if (isCurrentWaqt) 8.dp else 6.dp)`।
- **ওয়েটেড থ্রি-কলাম লেআউট (Weighted 3-Column Layout):**
  - **বাম কলাম (`weight(1.2f)`)**: আইকন + ওয়াক্তের নাম + "চলমান" ব্যাজ ধারণ করবে।
  - **মাঝের কলাম (`weight(0.6f)`)**: বেল আইকন সংসংক্রান্ত `IconButton`-টি একদম স্ক্রিনের ৫০% নিখুঁত সেন্টারে লক করা থাকবে।
  - **ডান কলাম (`weight(1.2f)`)**: সালাতের সময়সূচী (Time Range) ডানে অ্যালাইনড থাকবে।

---

## Verification
- `compile_applet` চালিয়ে বিল্ড সফল কিনা তা যাচাই করা।
- রানিং ওয়াক্ত ("চলমান" ব্যাজ থাকা অবস্থায়) এবং সাধারণ ওয়াক্তের সময় বেল আইকনগুলো নিখুঁত একই লাইনে লক থাকে কিনা পরীক্ষা করা।
