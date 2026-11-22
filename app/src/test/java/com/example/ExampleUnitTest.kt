package com.example

import com.example.data.api.QuranApi
import com.example.data.api.QuranComApi
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ExampleUnitTest {
    @Test
    fun testQuranComApiAndCombine() {
        val okHttpClient = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }.build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.alquran.cloud/v1/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val quranComRetrofit = Retrofit.Builder()
            .baseUrl("https://api.quran.com/api/v4/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val quranApi = retrofit.create(QuranApi::class.java)
        val quranComApi = quranComRetrofit.create(QuranComApi::class.java)

        runBlocking {
            println("Fetching Surah 5...")
            val response = quranApi.getSurahWithTranslation(5)
            println("QuranCloud response code: ${response.code}")
            val quranComResponse = quranComApi.getSurahVerses(5)
            println("QuranCom response size: ${quranComResponse.verses.size}")
            
            val arabicEditionObj = response.data.find { it.edition.identifier == "quran-uthmani" }
            val arabicAyahs = arabicEditionObj!!.ayahs
            
            val targetAyahIndex = 2 // Ayah 2
            val targetAyah = arabicAyahs.find { it.numberInSurah == targetAyahIndex }
            if (targetAyah != null) {
                println("Ayah 2 text: ${targetAyah.text}")
                val quranComVerse = quranComResponse.verses.find { it.verseNumber == targetAyahIndex }
                if (quranComVerse != null) {
                    println("Ayah 2 has ${quranComVerse.words.size} words.")
                    assert(quranComVerse.words.isNotEmpty()) { "Ayah 2 word list should not be empty" }
                    quranComVerse.words.forEach { w ->
                        println("  Word Position: ${w.position}, Arabic: ${w.textUthmani}, Bengali Translation: ${w.translation?.text ?: "N/A"}, Audio URL: ${w.audioUrl}")
                    }
                } else {
                    println("DIAGNOSTIC: Could not find QuranComVerse for verseNumber = $targetAyahIndex")
                }
            } else {
                println("DIAGNOSTIC: Could not find Arabic Ayah 2 in QuranCloud response")
            }
            
            var wordsPrinted = 0
            for (arabicAyah in arabicAyahs) {
                val quranComVerse = quranComResponse.verses.find { it.verseNumber == arabicAyah.numberInSurah }
                if (quranComVerse == null) {
                    println("DIAGNOSTIC: Could not find QuranComVerse for verseNumber = ${arabicAyah.numberInSurah}")
                } else if (quranComVerse.words.isEmpty()) {
                    println("DIAGNOSTIC: QuranComVerse has empty words for verseNumber = ${arabicAyah.numberInSurah}")
                }
            }
            println("Finished mapping diagnostics.")
        }
    }

    @Test
    fun testHijriDateCalculation() {
        val testDate = java.time.LocalDate.of(2026, 10, 11)
        val defaultGlobalOffset = -1 // Bangladesh Remote Config calibrated base offset

        // Default 0 user offset + default global offset (-1) -> Must be 29 Rabi us-Sani (Bangladeshi real date)
        val defaultRes = com.example.utils.HijriCalendarUtil.getHijriDate(testDate, 0 + defaultGlobalOffset)
        org.junit.Assert.assertEquals(29, defaultRes.hijriDay)
        org.junit.Assert.assertEquals("রবিউস সানী", defaultRes.hijriMonthNameBn)
        org.junit.Assert.assertEquals(1448, defaultRes.hijriYear)

        // User selected -1 day offset -> 28 Rabi us-Sani
        val minusOneRes = com.example.utils.HijriCalendarUtil.getHijriDate(testDate, -1 + defaultGlobalOffset)
        org.junit.Assert.assertEquals(28, minusOneRes.hijriDay)

        // User selected +1 day offset -> 30 Rabi us-Sani
        val plusOneRes = com.example.utils.HijriCalendarUtil.getHijriDate(testDate, 1 + defaultGlobalOffset)
        org.junit.Assert.assertEquals(30, plusOneRes.hijriDay)
    }

    @Test
    fun testBanglaDateCalculation() {
        // Oct 20, 2026 must be 5 Kartik 1433 (was 4 Kartik due to the 6th 31-day month bug)
        val oct20 = java.time.LocalDate.of(2026, 10, 20)
        val banglaOct20 = com.example.utils.HijriCalendarUtil.getBanglaDateStr(oct20)
        org.junit.Assert.assertEquals("৫ কার্তিক ১৪৩৩ বঙ্গাব্দ", banglaOct20)

        // April 14, 2026 must be 1 Boishakh 1433
        val apr14 = java.time.LocalDate.of(2026, 4, 14)
        val banglaApr14 = com.example.utils.HijriCalendarUtil.getBanglaDateStr(apr14)
        org.junit.Assert.assertEquals("১ বৈশাখ ১৪৩৩ বঙ্গাব্দ", banglaApr14)

        // April 13, 2026 must be 30 Choitro 1432
        val apr13 = java.time.LocalDate.of(2026, 4, 13)
        val banglaApr13 = com.example.utils.HijriCalendarUtil.getBanglaDateStr(apr13)
        org.junit.Assert.assertEquals("৩০ চৈত্র ১৪৩২ বঙ্গাব্দ", banglaApr13)
    }

    @Test
    fun testPrayerTimesHighLatitudeSummer() {
        // London (lat 51.5) on Summer Solstice (June 21)
        val londonDistrict = com.example.data.model.DistrictInfo(
            id = "london",
            nameEn = "London",
            nameBn = "লন্ডন",
            divisionBn = "যুক্তরাজ্য",
            latitude = 51.5074,
            longitude = -0.1278,
            countryBn = "যুক্তরাজ্য",
            countryEn = "United Kingdom",
            timeZoneOffset = 1.0,
            timeZoneId = "Europe/London",
            fajrAngle = 18.0,
            ishaAngle = 17.0
        )
        val summerSolstice = java.time.LocalDate.of(2026, 6, 21)
        val schedule = com.example.utils.PrayerTimesCalculator.calculatePrayerSchedule(
            date = summerSolstice,
            district = londonDistrict,
            calculationMethod = com.example.data.model.PrayerCalculationMethod.MWL
        )

        // Fajr must be reasonable (around 02:00 - 03:00 AM) and not broken at midnight/1:02 AM
        val fajrHour = schedule.fajr.hour
        val ishaHour = schedule.isha.hour
        org.junit.Assert.assertTrue("Fajr should be between 2 AM and 3 AM in London summer: ${schedule.fajr}", fajrHour in 2..3)
        org.junit.Assert.assertTrue("Isha should be late night (22:00 or 23:00): ${schedule.isha}", ishaHour >= 22)
    }

    @Test
    fun testPrayerSafeRoundingCeilAndFloor() {
        // Ceiling tests for prayers (roundUp = true)
        val tCeil1 = com.example.utils.PrayerTimesCalculator.decimalToLocalTime(12.0 + (1.01 / 60.0), roundUp = true)
        org.junit.Assert.assertEquals("12:01.01 should ceil to 12:02", java.time.LocalTime.of(12, 2), tCeil1)

        val tCeil2 = com.example.utils.PrayerTimesCalculator.decimalToLocalTime(12.0 + (1.00 / 60.0), roundUp = true)
        org.junit.Assert.assertEquals("12:01.00 should remain 12:01", java.time.LocalTime.of(12, 1), tCeil2)

        val tCeilRollOver = com.example.utils.PrayerTimesCalculator.decimalToLocalTime(11.0 + (59.05 / 60.0), roundUp = true)
        org.junit.Assert.assertEquals("11:59.05 should ceil to 12:00", java.time.LocalTime.of(12, 0), tCeilRollOver)

        // Floor tests for sunrise (roundUp = false)
        val tFloor1 = com.example.utils.PrayerTimesCalculator.decimalToLocalTime(5.0 + (59.99 / 60.0), roundUp = false)
        org.junit.Assert.assertEquals("05:59.99 should floor to 05:59 so Fajr does not run past sunrise", java.time.LocalTime.of(5, 59), tFloor1)

        val tFloor2 = com.example.utils.PrayerTimesCalculator.decimalToLocalTime(5.0 + (59.00 / 60.0), roundUp = false)
        org.junit.Assert.assertEquals("05:59.00 should remain 05:59", java.time.LocalTime.of(5, 59), tFloor2)
    }

    @Test
    fun testDhuhrThreeMinuteBuffer() {
        val dhakaDistrict = com.example.utils.PrayerTimesCalculator.findDistrictById("dhaka")
        val date = java.time.LocalDate.of(2026, 3, 21) // Equinox
        val schedule = com.example.utils.PrayerTimesCalculator.calculatePrayerSchedule(
            date = date,
            district = dhakaDistrict
        )

        // Equinox solar noon is around 12:00 local time for Dhaka (90.41°E, +6 GMT = 90° reference)
        // With 3-min buffer + ceil rounding, Dhuhr should be around 12:02 - 12:04
        val dhuhr = schedule.dhuhr
        org.junit.Assert.assertEquals(12, dhuhr.hour)
        org.junit.Assert.assertTrue("Dhuhr minute should be at least 2 with 3 min buffer: ${dhuhr.minute}", dhuhr.minute >= 2)
    }

    @Test
    fun testExactGpsCoordinatesPassedToCalculator() {
        val dhaka = com.example.utils.PrayerTimesCalculator.findDistrictById("dhaka")
        val date = java.time.LocalDate.of(2026, 3, 21)

        val baseSchedule = com.example.utils.PrayerTimesCalculator.calculatePrayerSchedule(
            date = date,
            district = dhaka
        )

        // Exact coordinates 0.5 degrees west of Dhaka center:
        // 0.5 degrees of longitude = 2.0 minutes later solar noon
        val westLat = dhaka.latitude
        val westLng = dhaka.longitude - 0.5

        val exactDistrict = com.example.utils.PrayerTimesCalculator.createExactLocationDistrict(westLat, westLng)
        org.junit.Assert.assertEquals(westLat, exactDistrict.latitude, 0.0001)
        org.junit.Assert.assertEquals(westLng, exactDistrict.longitude, 0.0001)
        org.junit.Assert.assertEquals("dhaka", exactDistrict.id)

        val exactSchedule = com.example.utils.PrayerTimesCalculator.calculatePrayerSchedule(
            date = date,
            district = exactDistrict
        )

        org.junit.Assert.assertEquals(westLat, exactSchedule.district.latitude, 0.0001)
        org.junit.Assert.assertEquals(westLng, exactSchedule.district.longitude, 0.0001)

        // Dhuhr time should be 2 minutes later due to 0.5 deg west shift
        val baseDhuhrMinutes = baseSchedule.dhuhr.hour * 60 + baseSchedule.dhuhr.minute
        val exactDhuhrMinutes = exactSchedule.dhuhr.hour * 60 + exactSchedule.dhuhr.minute
        org.junit.Assert.assertEquals(2, exactDhuhrMinutes - baseDhuhrMinutes)
    }

    @Test
    fun testMorningEveningRecommendedCountParser() {
        val seg3 = listOf(com.example.data.DuaSegment(top = "সকালে ও সন্ধ্যায় তিনবার পড়বে"))
        org.junit.Assert.assertEquals(3, com.example.data.DuaData.parseRecommendedCount("নিরাপত্তার দোয়া", seg3))

        val seg4 = listOf(com.example.data.DuaSegment(bottom = "যে ব্যক্তি চার বার পাঠ করবে"))
        org.junit.Assert.assertEquals(4, com.example.data.DuaData.parseRecommendedCount("জাহান্নাম থেকে মুক্তি", seg4))

        val seg7 = listOf(com.example.data.DuaSegment(bottom = "সকালে ও সন্ধ্যায় ৭ বার পাঠ করলে"))
        org.junit.Assert.assertEquals(7, com.example.data.DuaData.parseRecommendedCount("দুশ্চিন্তা মুক্তির দোয়া", seg7))

        val seg10 = listOf(com.example.data.DuaSegment(top = "দশবার দরূদ পাঠ"))
        org.junit.Assert.assertEquals(10, com.example.data.DuaData.parseRecommendedCount("দরূদ", seg10))

        val seg100 = listOf(com.example.data.DuaSegment(bottom = "প্রতিদিন ১০০ বার পাঠ করলে"))
        org.junit.Assert.assertEquals(100, com.example.data.DuaData.parseRecommendedCount("সুবহানাল্লাহ", seg100))

        val seg1 = listOf(com.example.data.DuaSegment(top = "আয়াতুল কুরসি"))
        org.junit.Assert.assertEquals(1, com.example.data.DuaData.parseRecommendedCount("আয়াতুল কুরসি", seg1))
    }
}



