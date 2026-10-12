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
}
