package com.example.util

object AudioUtils {
    private val SURAH_AYAH_COUNTS = intArrayOf(
        7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110, 98, 135,
        112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88, 75, 85,
        54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24, 13,
        14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
        29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11,
        11, 8, 3, 9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6
    )

    private fun getEveryAyahCode(globalAyahNumber: Int): String {
        var remaining = globalAyahNumber
        var surah = 1
        for (count in SURAH_AYAH_COUNTS) {
            if (remaining <= count) break
            remaining -= count
            surah++
        }
        val safeSurah = surah.coerceIn(1, 114)
        val safeAyah = remaining.coerceAtLeast(1)
        return String.format("%03d%03d", safeSurah, safeAyah)
    }

    fun getAudioUrl(qariId: String, ayahNumber: Int): String {
        val eaCode by lazy { getEveryAyahCode(ayahNumber) }

        return when (qariId) {
            // Islamic Network CDN Qaris
            "ar.alafasy" -> "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$ayahNumber.mp3"
            "ar.abdulbasitmurattal" -> "https://cdn.islamic.network/quran/audio/192/ar.abdulbasitmurattal/$ayahNumber.mp3"
            "ar.abdullahbasfar" -> "https://cdn.islamic.network/quran/audio/192/ar.abdullahbasfar/$ayahNumber.mp3"
            "ar.abdurrahmaansudais" -> "https://cdn.islamic.network/quran/audio/192/ar.abdurrahmaansudais/$ayahNumber.mp3"
            "ar.hudhaify" -> "https://cdn.islamic.network/quran/audio/128/ar.hudhaify/$ayahNumber.mp3"
            "ar.husary" -> "https://cdn.islamic.network/quran/audio/128/ar.husary/$ayahNumber.mp3"
            "ar.husarymujawwad" -> "https://cdn.islamic.network/quran/audio/128/ar.husarymujawwad/$ayahNumber.mp3"
            "ar.mahermuaiqly" -> "https://cdn.islamic.network/quran/audio/128/ar.mahermuaiqly/$ayahNumber.mp3"
            "ar.minshawi" -> "https://cdn.islamic.network/quran/audio/128/ar.minshawi/$ayahNumber.mp3"
            "ar.minshawimujawwad" -> "https://cdn.islamic.network/quran/audio/64/ar.minshawimujawwad/$ayahNumber.mp3"
            "ar.muhammadayyoub" -> "https://cdn.islamic.network/quran/audio/128/ar.muhammadayyoub/$ayahNumber.mp3"
            "ar.muhammadjibreel" -> "https://cdn.islamic.network/quran/audio/128/ar.muhammadjibreel/$ayahNumber.mp3"
            "ar.saoodshuraym" -> "https://cdn.islamic.network/quran/audio/64/ar.saoodshuraym/$ayahNumber.mp3"
            "ar.hanirifai" -> "https://cdn.islamic.network/quran/audio/192/ar.hanirifai/$ayahNumber.mp3"
            "ar.ahmedajamy" -> "https://cdn.islamic.network/quran/audio/128/ar.ahmedajamy/$ayahNumber.mp3"

            // EveryAyah CDN Qaris
            "ar.abdulbasitmujawwad" -> "https://everyayah.com/data/Abdul_Basit_Mujawwad_128kbps/$eaCode.mp3"
            "ar.parhizgar" -> "https://everyayah.com/data/Parhizgar_48kbps/$eaCode.mp3"
            "ar.yasserdosari" -> "https://everyayah.com/data/Yasser_Ad-Dussary_128kbps/$eaCode.mp3"
            "ar.nasseralqatami" -> "https://everyayah.com/data/Nasser_Alqatami_128kbps/$eaCode.mp3"
            "ar.khalifaaltunaiji" -> "https://everyayah.com/data/khalefa_al_tunaiji_64kbps/$eaCode.mp3"
            "ar.ibrahimakhdar" -> "https://everyayah.com/data/Ibrahim_Akhdar_32kbps/$eaCode.mp3"
            "ar.salahbudair" -> "https://everyayah.com/data/Salah_Al_Budair_128kbps/$eaCode.mp3"
            "ar.saadalgahmadi" -> "https://everyayah.com/data/Ghamadi_40kbps/$eaCode.mp3"
            "ar.faresabbad" -> "https://everyayah.com/data/Fares_Abbad_64kbps/$eaCode.mp3"

            else -> "https://cdn.islamic.network/quran/audio/128/ar.alafasy/$ayahNumber.mp3"
        }
    }
}

