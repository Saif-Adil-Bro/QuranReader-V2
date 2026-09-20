package com.example.data.model

data class DistrictInfo(
    val id: String,
    val nameEn: String,
    val nameBn: String,
    val divisionBn: String,
    val latitude: Double,
    val longitude: Double,
    val countryBn: String = "বাংলাদেশ",
    val countryEn: String = "Bangladesh",
    val timeZone: Double = 6.0,
    val timeZoneId: String = "Asia/Dhaka",
    val fajrAngle: Double = 18.0,
    val ishaAngle: Double = 18.0,
    val ishaFixedIntervalMinutes: Int? = null
)

enum class PrayerName(val id: String, val nameBn: String, val nameEn: String, val icon: String) {
    FAJR("fajr", "ফজর", "Fajr", "🌅"),
    SUNRISE("sunrise", "সূর্যোদয়", "Sunrise", "☀️"),
    DHUHR("dhuhr", "যোহর", "Dhuhr", "☀️"),
    ASR("asr", "আসর", "Asr", "🌤️"),
    MAGHRIB("maghrib", "মাগরিব", "Maghrib", "🌇"),
    ISHA("isha", "এশা", "Isha", "🌙"),
    TAHAJJUD("tahajjud", "তাহাজ্জুদ", "Tahajjud", "🌌"),
    SAHRI("sahri", "সাহরি শেষ", "Sahri", "🌙"),
    IFTAR("iftar", "ইফতার", "Iftar", "✨")
}

enum class PrayerAlarmSoundType(val id: String, val titleBn: String, val subtitleBn: String) {
    SILENT("silent", "নিঃশব্দ", "কোনো শব্দ হবে না"),
    BEEP("beep", "বিপ", "মৃদু অ্যালার্ম বিপ টোন"),
    RING("ring", "রিং", "মধুর সুরের রিংটোন"),
    VOICE_NAME("voice_name", "ওয়াক্তের নাম", "বাংলায় ওয়াক্তের নাম ঘোষণা"),
    NOTIFICATION("notification", "নোটিফিকেশন", "ডিফল্ট নোটিফিকেশন টিউন"),
    AZAN_MECCA("azan_mecca", "মক্কা মুকাররমা আজান", "মক্কার সুমধুর আজান ধ্বনি"),
    AZAN_MADINA("azan_madina", "মদিনা মুনাওয়ারা আজান", "মদিনার হৃদয়স্পর্শী আজান")
}

data class WaqtAlarmConfig(
    val prayerName: PrayerName,
    val isEnabled: Boolean = true,
    val offsetMinutes: Int = 0, // -30 min to +30 min
    val soundType: PrayerAlarmSoundType = PrayerAlarmSoundType.NOTIFICATION,
    val isVibrationEnabled: Boolean = true
)


data class SinglePrayerTime(
    val name: PrayerName,
    val timeDigits: String,         // e.g. "৪:৩১"
    val amPm: String,               // "AM" or "PM"
    val timeFormatted: String,      // e.g. "০৪:২২ AM"
    val timestampMillis: Long,
    val isCurrent: Boolean = false,
    val isNext: Boolean = false,
    val endTimeDigits: String = "",
    val endTimeFormatted: String = "", // e.g. "৫:৩২ AM"
    val timeRangeFormatted: String = "" // e.g. "৪:১৬ AM - ৫:৩২ AM"
)

data class ForbiddenPrayerInterval(
    val titleBn: String,          // e.g. "সূর্যোদয়"
    val timeRangeBn: String,      // e.g. "সকাল ০৫:৩৬ - ০৫:৫৬"
    val subtitleBn: String,       // e.g. "সূর্য ওঠার পর থেকে ১৫-২০ মিনিট"
    val icon: String = "⚠️"
)

data class DailyPrayerSchedule(
    val dateStrBn: String,
    val district: DistrictInfo,
    val prayers: List<SinglePrayerTime>,
    val currentPrayer: SinglePrayerTime?,
    val nextPrayer: SinglePrayerTime?,
    val remainingTimeToNextFormatted: String, // e.g. "৩৫ মিনিট বাকি" or "১ ঘণ্টা ২০ মিনিট"
    val isForbiddenTimeNow: Boolean,
    val forbiddenTimeReason: String?,
    val sahriEndTimeFormatted: String,
    val iftarTimeFormatted: String,
    val tahajjudEndTimeFormatted: String,
    val ishraqStartTimeFormatted: String,
    val forbiddenSunriseFormatted: String = "",
    val forbiddenMiddayFormatted: String = "",
    val forbiddenSunsetFormatted: String = "",
    val forbiddenTimesList: List<ForbiddenPrayerInterval> = emptyList(),
    // Specific ranges matching the Islamic Calendar Design
    val fajrRange: String = "",
    val dhuhrRange: String = "",
    val asrRange: String = "",
    val asrMakruhTime: String = "",
    val maghribRange: String = "",
    val ishaRange: String = "",
    val ishaUttomTime: String = "",
    val ishaMakruhTime: String = "",
    val duhaRange: String = "",
    val zawalStartTime: String = "",
    val awwabinRange: String = "",
    val tahajjudRange: String = "",
    val tahajjudLastThirdStart: String = "",
    val forbiddenMorningRange: String = "",
    val forbiddenNoonRange: String = "",
    val forbiddenEveningRange: String = "",
    val sunriseTimeDigits: String = "",
    val sunsetTimeDigits: String = "",
    val sahriTimeDigits: String = "",
    val iftarTimeDigits: String = ""
)
