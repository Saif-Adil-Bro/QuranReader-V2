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
    val ishaFixedIntervalMinutes: Int? = null,
    val divisionEn: String = ""
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
    IFTAR("iftar", "ইফতার", "Iftar", "✨"),
    MAKRUH_SUNRISE("makruh_sunrise", "মাকরূহ: সূর্যোদয়", "Makruh Sunrise", "⚠️"),
    MAKRUH_ZAWAL("makruh_zawal", "মাকরূহ: দ্বিপ্রহর (জাওয়াল)", "Makruh Zawal", "⚠️"),
    MAKRUH_SUNSET("makruh_sunset", "মাকরূহ: সূর্যাস্ত", "Makruh Sunset", "⚠️");

    val isMakruh: Boolean
        get() = this == MAKRUH_SUNRISE || this == MAKRUH_ZAWAL || this == MAKRUH_SUNSET

    fun getDisplayName(isFriday: Boolean = false, isEn: Boolean = false): String {
        if (this == DHUHR && isFriday) {
            return if (isEn) "Jumu'ah" else "জুমুআ"
        }
        return if (isEn) nameEn else nameBn
    }
}

enum class AlertCategory(val id: String, val titleBn: String) {
    NOTIFICATION("notification", "নোটিফিকেশন"),
    ALARM("alarm", "অ্যালার্ম ও আযান");

    fun getTitle(isEn: Boolean): String = if (isEn) {
        when (this) {
            NOTIFICATION -> "Notification"
            ALARM -> "Alarm & Azan"
        }
    } else titleBn
}

enum class PrayerAlarmSoundType(
    val id: String, 
    val titleBn: String, 
    val subtitleBn: String,
    val category: AlertCategory
) {
    // Notification category
    SILENT("silent", "নিঃশব্দ", "কোনো শব্দ হবে না", AlertCategory.NOTIFICATION),
    BEEP("beep", "মৃদু বিপ", "সংক্ষিপ্ত হালকা বিপ টোন", AlertCategory.NOTIFICATION),
    RING("ring", "মৃদু রিং", "মধুর সুরের সংক্ষিপ্ত রিংটোন", AlertCategory.NOTIFICATION),
    VOICE_NAME("voice_name", "ওয়াক্তের নাম", "বাংলায় ওয়াক্তের নাম ঘোষণা", AlertCategory.NOTIFICATION),
    NOTIFICATION("notification", "নোটিফিকেশন", "ডিফল্ট নোটিফিকেশন টিউন", AlertCategory.NOTIFICATION),

    // Alarm category
    AZAN_MECCA("azan_mecca", "মক্কা মুকাররমা আজান", "মক্কার সুমধুর আজান ধ্বনি", AlertCategory.ALARM),
    AZAN_MADINA("azan_madina", "মদিনা মুনাওয়ারা আজান", "মদিনার হৃদয়স্পর্শী আজান", AlertCategory.ALARM),
    AZAN_FAJR("azan_fajr", "ফজর স্পেশাল আজান", "আস-সালাতু খাইরুম মিনান নাওম সহ", AlertCategory.ALARM),
    CUSTOM_RINGTONE("custom_ringtone", "ফোনের রিংটোন", "ডিভাইসের নিজস্ব রিংটোন তালিকা থেকে নির্বাচন", AlertCategory.ALARM);

    val isAlarm: Boolean get() = category == AlertCategory.ALARM
    val isNotification: Boolean get() = category == AlertCategory.NOTIFICATION

    fun getTitle(isEn: Boolean): String = if (isEn) {
        when (this) {
            SILENT -> "Silent"
            BEEP -> "Gentle Beep"
            RING -> "Short Ringtone"
            VOICE_NAME -> "Waqt Name Voice"
            NOTIFICATION -> "Default Notification"
            AZAN_MECCA -> "Makkah Mukarramah Azan"
            AZAN_MADINA -> "Madinah Munawwarah Azan"
            AZAN_FAJR -> "Fajr Special Azan"
            CUSTOM_RINGTONE -> "Device Ringtone"
        }
    } else titleBn

    fun getSubtitle(isEn: Boolean): String = if (isEn) {
        when (this) {
            SILENT -> "No sound will play"
            BEEP -> "Short gentle beep tone"
            RING -> "Pleasant short melody"
            VOICE_NAME -> "Voice announcement of prayer name"
            NOTIFICATION -> "Default notification tone"
            AZAN_MECCA -> "Soulful Azan from Makkah"
            AZAN_MADINA -> "Heartwarming Azan from Madinah"
            AZAN_FAJR -> "Includes As-Salatu Khairum Minan Nawm"
            CUSTOM_RINGTONE -> "Select from device ringtone list"
        }
    } else subtitleBn
}

data class WaqtAlarmConfig(
    val prayerName: PrayerName,
    val isEnabled: Boolean = true,
    val offsetMinutes: Int = 0, // -30 min to +30 min
    val soundType: PrayerAlarmSoundType = PrayerAlarmSoundType.AZAN_MECCA,
    val customRingtoneUri: String? = null,
    val customRingtoneTitle: String? = null,
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
    val timeRangeFormatted: String = "", // e.g. "৪:১৬ AM - ৫:৩২ AM"
    val isFriday: Boolean = false
) {
    fun getDisplayName(isEn: Boolean = false): String {
        return name.getDisplayName(isFriday = isFriday, isEn = isEn)
    }

    val displayNameBn: String
        get() = name.getDisplayName(isFriday = isFriday, isEn = false)

    val displayNameEn: String
        get() = name.getDisplayName(isFriday = isFriday, isEn = true)
}

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
    val iftarTimeDigits: String = "",
    val isFriday: Boolean = false
)
