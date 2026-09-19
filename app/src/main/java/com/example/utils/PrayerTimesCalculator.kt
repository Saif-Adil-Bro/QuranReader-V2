package com.example.utils

import com.example.data.model.DistrictInfo
import com.example.data.model.DailyPrayerSchedule
import com.example.data.model.ForbiddenPrayerInterval
import com.example.data.model.PrayerName
import com.example.data.model.SinglePrayerTime
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.*

object PrayerTimesCalculator {

    // 64 Districts of Bangladesh with accurate Coordinates
    val BANGLADESH_DISTRICTS = listOf(
        DistrictInfo("dhaka", "Dhaka", "ঢাকা", "ঢাকা", 23.8103, 90.4125),
        DistrictInfo("chattogram", "Chattogram", "চট্টগ্রাম", "চট্টগ্রাম", 22.3569, 91.7832),
        DistrictInfo("sylhet", "Sylhet", "সিলেট", "সিলেট", 24.8949, 91.8687),
        DistrictInfo("rajshahi", "Rajshahi", "রাজশাহী", "রাজশাহী", 24.3636, 88.6241),
        DistrictInfo("khulna", "Khulna", "খুলনা", "খুলনা", 22.8456, 89.5403),
        DistrictInfo("barishal", "Barishal", "বরিশাল", "বরিশাল", 22.7010, 90.3535),
        DistrictInfo("rangpur", "Rangpur", "রংপুর", "রংপুর", 25.7439, 89.2752),
        DistrictInfo("mymensingh", "Mymensingh", "ময়মনসিংহ", "ময়মনসিংহ", 24.7471, 90.4203),
        DistrictInfo("cumilla", "Cumilla", "কুমিল্লা", "চট্টগ্রাম", 23.4682, 91.1788),
        DistrictInfo("gazipur", "Gazipur", "গাজীপুর", "ঢাকা", 24.0023, 90.4267),
        DistrictInfo("narayanganj", "Narayanganj", "নারায়ণগঞ্জ", "ঢাকা", 23.6238, 90.5000),
        DistrictInfo("bogra", "Bogura", "বগুড়া", "রাজশাহী", 24.8465, 89.3777),
        DistrictInfo("dinajpur", "Dinajpur", "দিনাজপুর", "রংপুর", 25.6217, 88.6355),
        DistrictInfo("faridpur", "Faridpur", "ফরিদপুর", "ঢাকা", 23.6071, 89.8429),
        DistrictInfo("jashore", "Jashore", "যশোর", "খুলনা", 23.1664, 89.2081),
        DistrictInfo("coxsbazar", "Cox's Bazar", "কক্সবাজার", "চট্টগ্রাম", 21.4272, 92.0058),
        DistrictInfo("noakhali", "Noakhali", "নোয়াখালী", "চট্টগ্রাম", 22.8696, 91.0998),
        DistrictInfo("feni", "Feni", "ফেনী", "চট্টগ্রাম", 23.0186, 91.3966),
        DistrictInfo("brahmanbaria", "Brahmanbaria", "ব্রাহ্মণবাড়িয়া", "চট্টগ্রাম", 23.9571, 91.1115),
        DistrictInfo("chandpur", "Chandpur", "চাঁদপুর", "চট্টগ্রাম", 23.2333, 90.6667),
        DistrictInfo("lakshmipur", "Lakshmipur", "লক্ষ্মীপুর", "চট্টগ্রাম", 22.9425, 90.8412),
        DistrictInfo("tangail", "Tangail", "টাঙ্গাইল", "ঢাকা", 24.2513, 89.9167),
        DistrictInfo("kishoreganj", "Kishoreganj", "কিশোরগঞ্জ", "ঢাকা", 24.4449, 90.7766),
        DistrictInfo("manikganj", "Manikganj", "মানিকগঞ্জ", "ঢাকা", 23.8617, 90.0003),
        DistrictInfo("munshiganj", "Munshiganj", "মুন্সিগঞ্জ", "ঢাকা", 23.5422, 90.5305),
        DistrictInfo("narsingdi", "Narsingdi", "নরসিংদী", "ঢাকা", 23.9322, 90.7154),
        DistrictInfo("gopalganj", "Gopalganj", "গোপালগঞ্জ", "ঢাকা", 23.0051, 89.8266),
        DistrictInfo("madaripur", "Madaripur", "মাদারীপুর", "ঢাকা", 23.1641, 90.1897),
        DistrictInfo("shariatpur", "Shariatpur", "শরীয়তপুর", "ঢাকা", 23.2423, 90.4348),
        DistrictInfo("rajbari", "Rajbari", "রাজবাড়ী", "ঢাকা", 23.7574, 89.6445),
        DistrictInfo("netrokona", "Netrokona", "নেত্রকোণা", "ময়মনসিংহ", 24.8709, 90.7279),
        DistrictInfo("jamalpur", "Jamalpur", "জামালপুর", "ময়মনসিংহ", 24.9375, 89.9378),
        DistrictInfo("sherpur", "Sherpur", "শেরপুর", "ময়মনসিংহ", 25.0205, 90.0153),
        DistrictInfo("habiganj", "Habiganj", "হবিগঞ্জ", "সিলেট", 24.3749, 91.4155),
        DistrictInfo("moulvibazar", "Moulvibazar", "মৌলভীবাজার", "সিলেট", 24.4829, 91.7774),
        DistrictInfo("sunamganj", "Sunamganj", "সুনামগঞ্জ", "সিলেট", 25.0658, 91.3950),
        DistrictInfo("sirajganj", "Sirajganj", "সিরাজগঞ্জ", "রাজশাহী", 24.4534, 89.7008),
        DistrictInfo("pabna", "Pabna", "পাবনা", "রাজশাহী", 24.0064, 89.2372),
        DistrictInfo("natore", "Natore", "নাটোর", "রাজশাহী", 24.4206, 88.9320),
        DistrictInfo("naogaon", "Naogaon", "নওগাঁ", "রাজশাহী", 24.7936, 88.9318),
        DistrictInfo("chapainawabganj", "Chapai Nawabganj", "চাঁপাইনবাবগঞ্জ", "রাজশাহী", 24.5965, 88.2775),
        DistrictInfo("joypurhat", "Joypurhat", "জয়পুরহাট", "রাজশাহী", 25.1015, 89.0277),
        DistrictInfo("kushtia", "Kushtia", "কুষ্টিয়া", "খুলনা", 23.9013, 89.1205),
        DistrictInfo("meherpur", "Meherpur", "মেহেরপুর", "খুলনা", 23.7622, 88.6318),
        DistrictInfo("chuadanga", "Chuadanga", "চুয়াডাঙ্গা", "খুলনা", 23.6402, 88.8418),
        DistrictInfo("jhenaidah", "Jhenaidah", "ঝিনাইদহ", "খুলনা", 23.5450, 89.1726),
        DistrictInfo("magura", "Magura", "মাগুরা", "খুলনা", 23.4873, 89.4199),
        DistrictInfo("narail", "Narail", "নড়াইল", "খুলনা", 23.1725, 89.5127),
        DistrictInfo("satkhira", "Satkhira", "সাতক্ষীরা", "খুলনা", 22.7185, 89.0705),
        DistrictInfo("bagerhat", "Bagerhat", "বাগেরহাট", "খুলনা", 22.6516, 89.7859),
        DistrictInfo("patuakhali", "Patuakhali", "পটুয়াখালী", "বরিশাল", 22.3596, 90.3299),
        DistrictInfo("bhola", "Bhola", "ভোলা", "বরিশাল", 22.6859, 90.6481),
        DistrictInfo("pirojpur", "Pirojpur", "পিরোজপুর", "বরিশাল", 22.5841, 89.9720),
        DistrictInfo("jhalokathi", "Jhalokathi", "ঝালকাঠি", "বরিশাল", 22.6406, 90.1987),
        DistrictInfo("barguna", "Barguna", "বরগুনা", "বরিশাল", 22.0953, 90.1121),
        DistrictInfo("gaibandha", "Gaibandha", "গাইবান্ধা", "রংপুর", 25.3288, 89.5408),
        DistrictInfo("kurigram", "Kurigram", "কুড়িগ্রাম", "রংপুর", 25.8054, 89.6362),
        DistrictInfo("lalmonirhat", "Lalmonirhat", "লালমনিরহাট", "রংপুর", 25.9923, 89.2847),
        DistrictInfo("nilphamari", "Nilphamari", "নীলফামারী", "রংপুর", 25.9318, 88.8560),
        DistrictInfo("panchagarh", "Panchagarh", "পঞ্চগড়", "রংপুর", 26.3411, 88.5542),
        DistrictInfo("thakurgaon", "Thakurgaon", "ঠাকুরগাঁও", "রংপুর", 26.0337, 88.4617),
        DistrictInfo("khagrachhari", "Khagrachhari", "খাগড়াছড়ি", "চট্টগ্রাম", 23.1193, 91.9847),
        DistrictInfo("rangamati", "Rangamati", "রাঙ্গামাটি", "চট্টগ্রাম", 22.7324, 92.2985),
        DistrictInfo("bandarban", "Bandarban", "বান্দরবান", "চট্টগ্রাম", 22.1953, 92.2184)
    )

    val INTERNATIONAL_CITIES = listOf(
        // Saudi Arabia
        DistrictInfo("makkah", "Makkah", "মক্কা", "মক্কা প্রদেশ", 21.4225, 39.8262, "সৌদি আরব", "Saudi Arabia", 3.0, "Asia/Riyadh", 18.5, 18.0, ishaFixedIntervalMinutes = 90),
        DistrictInfo("madinah", "Madinah", "মদিনা", "মদিনা প্রদেশ", 24.5247, 39.5692, "সৌদি আরব", "Saudi Arabia", 3.0, "Asia/Riyadh", 18.5, 18.0, ishaFixedIntervalMinutes = 90),
        DistrictInfo("riyadh", "Riyadh", "রিয়াদ", "রিয়াদ প্রদেশ", 24.7136, 46.6753, "সৌদি আরব", "Saudi Arabia", 3.0, "Asia/Riyadh", 18.5, 18.0, ishaFixedIntervalMinutes = 90),
        DistrictInfo("jeddah", "Jeddah", "জেদ্দা", "মক্কা প্রদেশ", 21.4858, 39.1925, "সৌদি আরব", "Saudi Arabia", 3.0, "Asia/Riyadh", 18.5, 18.0, ishaFixedIntervalMinutes = 90),
        DistrictInfo("dammam", "Dammam", "দাম্মাম", "পূর্ব প্রদেশ", 26.4207, 50.0888, "সৌদি আরব", "Saudi Arabia", 3.0, "Asia/Riyadh", 18.5, 18.0, ishaFixedIntervalMinutes = 90),
        
        // United Arab Emirates
        DistrictInfo("dubai", "Dubai", "দুবাই", "দুবাই আমিরাত", 25.2048, 55.2708, "সংযুক্ত আরব আমিরাত", "UAE", 4.0, "Asia/Dubai", 18.2, 18.2),
        DistrictInfo("abu_dhabi", "Abu Dhabi", "আবুধাবি", "আবুধাবি আমিরাত", 24.4539, 54.3773, "সংযুক্ত আরব আমিরাত", "UAE", 4.0, "Asia/Dubai", 18.2, 18.2),
        DistrictInfo("sharjah", "Sharjah", "শারজাহ", "শারজাহ আমিরাত", 25.3463, 55.4209, "সংযুক্ত আরব আমিরাত", "UAE", 4.0, "Asia/Dubai", 18.2, 18.2),

        // Qatar, Kuwait, Oman, Bahrain
        DistrictInfo("doha", "Doha", "দোহা", "দোহা", 25.2854, 51.5310, "কাতার", "Qatar", 3.0, "Asia/Qatar", 18.0, 18.0, ishaFixedIntervalMinutes = 90),
        DistrictInfo("kuwait_city", "Kuwait City", "কুয়েত সিটি", "আল আসিমা", 29.3759, 47.9774, "কুয়েত", "Kuwait", 3.0, "Asia/Kuwait", 18.0, 17.5),
        DistrictInfo("muscat", "Muscat", "মাস্কাট", "মাস্কাট প্রদেশ", 23.5880, 58.3829, "ওমান", "Oman", 4.0, "Asia/Muscat", 18.0, 18.0),
        DistrictInfo("manama", "Manama", "মানামা", "ক্যাপিটাল", 26.2285, 50.5860, "বাহরাইন", "Bahrain", 3.0, "Asia/Bahrain", 18.0, 18.0),

        // Malaysia & Singapore
        DistrictInfo("kuala_lumpur", "Kuala Lumpur", "কুয়ালালামপুর", "ফেডারেল টেরিটরি", 3.1390, 101.6869, "মালয়েশিয়া", "Malaysia", 8.0, "Asia/Kuala_Lumpur", 20.0, 18.0),
        DistrictInfo("penang", "Penang", "পেনাং", "পেনাং", 5.4164, 100.3327, "মালয়েশিয়া", "Malaysia", 8.0, "Asia/Kuala_Lumpur", 20.0, 18.0),
        DistrictInfo("johor_bahru", "Johor Bahru", "জোহর বাহরু", "জোহর", 1.4927, 103.7414, "মালয়েশিয়া", "Malaysia", 8.0, "Asia/Kuala_Lumpur", 20.0, 18.0),
        DistrictInfo("singapore", "Singapore", "সিঙ্গাপুর", "সেন্ট্রাল", 1.3521, 103.8198, "সিঙ্গাপুর", "Singapore", 8.0, "Asia/Singapore", 20.0, 18.0),

        // United Kingdom
        DistrictInfo("london", "London", "লন্ডন", "গ্রেটার লন্ডন", 51.5074, -0.1278, "যুক্তরাজ্য", "United Kingdom", 0.0, "Europe/London", 15.0, 15.0),
        DistrictInfo("birmingham", "Birmingham", "বার্মিংহাম", "ওয়েস্ট মিডল্যান্ডস", 52.4862, -1.8904, "যুক্তরাজ্য", "United Kingdom", 0.0, "Europe/London", 15.0, 15.0),
        DistrictInfo("manchester", "Manchester", "ম্যানচেস্টার", "গ্রেটার ম্যানচেস্টার", 53.4808, -2.2426, "যুক্তরাজ্য", "United Kingdom", 0.0, "Europe/London", 15.0, 15.0),
        DistrictInfo("leeds", "Leeds", "লিডস", "ওয়েস্ট ইয়র্কশায়ার", 53.8008, -1.5491, "যুক্তরাজ্য", "United Kingdom", 0.0, "Europe/London", 15.0, 15.0),

        // United States & Canada
        DistrictInfo("new_york", "New York", "নিউ ইয়র্ক", "নিউ ইয়র্ক", 40.7128, -74.0060, "যুক্তরাষ্ট্র", "USA", -5.0, "America/New_York", 15.0, 15.0),
        DistrictInfo("paterson", "Paterson", "প্যাটারসন", "নিউ জার্সি", 40.9168, -74.1718, "যুক্তরাষ্ট্র", "USA", -5.0, "America/New_York", 15.0, 15.0),
        DistrictInfo("chicago", "Chicago", "শিকাগো", "ইলিনয়", 41.8781, -87.6298, "যুক্তরাষ্ট্র", "USA", -6.0, "America/Chicago", 15.0, 15.0),
        DistrictInfo("los_angeles", "Los Angeles", "লস অ্যাঞ্জেলেস", "ক্যালিফোর্নিয়া", 34.0522, -118.2437, "যুক্তরাষ্ট্র", "USA", -8.0, "America/Los_Angeles", 15.0, 15.0),
        DistrictInfo("houston", "Houston", "হিউস্টন", "টেক্সাস", 29.7604, -95.3698, "যুক্তরাষ্ট্র", "USA", -6.0, "America/Chicago", 15.0, 15.0),
        DistrictInfo("toronto", "Toronto", "টরন্টো", "অন্টারিও", 43.6532, -79.3832, "কানাডা", "Canada", -5.0, "America/Toronto", 15.0, 15.0),
        DistrictInfo("montreal", "Montreal", "মন্ট্রিল", "ক্যুবেক", 45.5017, -73.5673, "কানাডা", "Canada", -5.0, "America/Montreal", 15.0, 15.0),

        // Italy & Germany & France
        DistrictInfo("rome", "Rome", "রোম", "লাজিও", 41.9028, 12.4964, "ইতালি", "Italy", 1.0, "Europe/Rome", 15.0, 15.0),
        DistrictInfo("milan", "Milan", "মিলান", "লম্বার্ডি", 45.4642, 9.1900, "ইতালি", "Italy", 1.0, "Europe/Rome", 15.0, 15.0),
        DistrictInfo("berlin", "Berlin", "বার্লিন", "বার্লিন", 52.5200, 13.4050, "জার্মানি", "Germany", 1.0, "Europe/Berlin", 15.0, 15.0),
        DistrictInfo("paris", "Paris", "প্যারিস", "ইল-দ্য-ফ্রঁস", 48.8566, 2.3522, "ফ্রান্স", "France", 1.0, "Europe/Paris", 15.0, 15.0),

        // India & Pakistan
        DistrictInfo("kolkata", "Kolkata", "কলকাতা", "পশ্চিমবঙ্গ", 22.5726, 88.3639, "ভারত", "India", 5.5, "Asia/Kolkata", 18.0, 18.0),
        DistrictInfo("delhi", "Delhi", "দিল্লি", "দিল্লি", 28.6139, 77.2090, "ভারত", "India", 5.5, "Asia/Kolkata", 18.0, 18.0),
        DistrictInfo("mumbai", "Mumbai", "মুম্বাই", "মহারাষ্ট্র", 19.0760, 72.8777, "ভারত", "India", 5.5, "Asia/Kolkata", 18.0, 18.0),
        DistrictInfo("karachi", "Karachi", "করাচি", "সিন্ধু", 24.8607, 67.0011, "পাকিস্তান", "Pakistan", 5.0, "Asia/Karachi", 18.0, 18.0),
        DistrictInfo("lahore", "Lahore", "লাহোর", "পাঞ্জাব", 31.5204, 74.3587, "পাকিস্তান", "Pakistan", 5.0, "Asia/Karachi", 18.0, 18.0),

        // Australia, Japan, South Korea, Turkey, Egypt
        DistrictInfo("sydney", "Sydney", "সিডনি", "নিউ সাউথ ওয়েলস", -33.8688, 151.2093, "অস্ট্রেলিয়া", "Australia", 10.0, "Australia/Sydney", 15.0, 15.0),
        DistrictInfo("melbourne", "Melbourne", "মেলবোর্ন", "ভিক্টোরিয়া", -37.8136, 144.9631, "অস্ট্রেলিয়া", "Australia", 10.0, "Australia/Melbourne", 15.0, 15.0),
        DistrictInfo("tokyo", "Tokyo", "টোকিও", "কান্তো", 35.6762, 139.6503, "জাপান", "Japan", 9.0, "Asia/Tokyo", 18.0, 18.0),
        DistrictInfo("seoul", "Seoul", "সিউল", "সিউল ক্যাপিটাল", 37.5665, 126.9780, "দক্ষিণ কোরিয়া", "South Korea", 9.0, "Asia/Seoul", 18.0, 18.0),
        DistrictInfo("istanbul", "Istanbul", "ইস্তাম্বুল", "মারমারা", 41.0082, 28.9784, "তুরস্ক", "Turkey", 3.0, "Europe/Istanbul", 18.0, 17.0),
        DistrictInfo("cairo", "Cairo", "কায়রো", "কায়রো", 30.0444, 31.2357, "মিশর", "Egypt", 2.0, "Africa/Cairo", 19.5, 17.5)
    )

    val ALL_LOCATIONS: List<DistrictInfo> = BANGLADESH_DISTRICTS + INTERNATIONAL_CITIES

    fun getDefaultDistrict(): DistrictInfo = BANGLADESH_DISTRICTS.first() // Dhaka

    fun findDistrictById(id: String): DistrictInfo {
        return ALL_LOCATIONS.find { it.id.equals(id, ignoreCase = true) } ?: getDefaultDistrict()
    }

    /**
     * Finds the closest district or city from ALL_LOCATIONS given GPS latitude and longitude.
     */
    fun findClosestDistrict(latitude: Double, longitude: Double): DistrictInfo {
        return ALL_LOCATIONS.minByOrNull { location ->
            val dLat = Math.toRadians(location.latitude - latitude)
            val dLon = Math.toRadians(location.longitude - longitude)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(latitude)) * cos(Math.toRadians(location.latitude)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            c // relative spherical distance
        } ?: getDefaultDistrict()
    }

    /**
     * Calculates the daily prayer schedule using astronomical solar formulas.
     */
    fun calculatePrayerSchedule(
        date: LocalDate = LocalDate.now(),
        district: DistrictInfo = getDefaultDistrict(),
        isHanafi: Boolean = true,
        fajrAngle: Double = district.fajrAngle,
        ishaAngle: Double = district.ishaAngle,
        sahriOffsetMinutes: Int = -3,
        iftarOffsetMinutes: Int = 0
    ): DailyPrayerSchedule {
        val lat = district.latitude
        val lng = district.longitude

        val zoneId = try {
            ZoneId.of(district.timeZoneId)
        } catch (e: Exception) {
            ZoneId.of("Asia/Dhaka")
        }
        val zonedDateTime = java.time.ZonedDateTime.of(date, LocalTime.NOON, zoneId)
        val timeZone = zonedDateTime.offset.totalSeconds / 3600.0

        val dayOfYear = date.dayOfYear

        // Solar Declination & Equation of Time
        val b = 2.0 * Math.PI * (dayOfYear - 81) / 365.0
        val eot = 9.87 * sin(2 * b) - 7.53 * cos(b) - 1.5 * sin(b) // minutes
        val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 81))) // degrees

        // Solar Noon (Dhuhr) in local time hours
        val solarNoon = 12.0 + (timeZone * 15.0 - lng) / 15.0 - (eot / 60.0)

        // Helper to calculate Hour Angle
        fun getHourAngle(angle: Double): Double {
            val latRad = Math.toRadians(lat)
            val decRad = Math.toRadians(declination)
            val angRad = Math.toRadians(angle)

            val cosH = (sin(angRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            return if (cosH > 1.0) 0.0 else if (cosH < -1.0) Math.PI else acos(cosH)
        }

        // Sunrise & Sunset angle is typically -0.833° (atmospheric refraction + sun disk radius)
        val sunriseHourAngle = Math.toDegrees(getHourAngle(-0.833)) / 15.0
        val fajrHourAngle = Math.toDegrees(getHourAngle(-fajrAngle)) / 15.0
        val ishaHourAngle = Math.toDegrees(getHourAngle(-ishaAngle)) / 15.0

        // Asr Angle calculation (Hanafi shadow = 2, Shafi'i/Standard shadow = 1)
        val asrFactor = if (isHanafi) 2.0 else 1.0
        val latMinusDec = Math.abs(lat - declination)
        val asrAltitude = Math.toDegrees(atan(1.0 / (asrFactor + tan(Math.toRadians(latMinusDec)))))
        val asrHourAngle = Math.toDegrees(getHourAngle(asrAltitude)) / 15.0

        val fajrDecimal = solarNoon - fajrHourAngle
        val sunriseDecimal = solarNoon - sunriseHourAngle
        val dhuhrDecimal = solarNoon + (2.0 / 60.0) // 2 minutes added after zawal for safety
        val asrDecimal = solarNoon + asrHourAngle
        val maghribDecimal = solarNoon + sunriseHourAngle + (2.0 / 60.0) // 2 minutes safety margin for sunset
        val ishaDecimal = if (district.ishaFixedIntervalMinutes != null) {
            maghribDecimal + (district.ishaFixedIntervalMinutes.toDouble() / 60.0)
        } else {
            solarNoon + ishaHourAngle
        }

        // Format to LocalTime
        fun decimalToLocalTime(decimal: Double): LocalTime {
            var hours = decimal.toInt()
            var minutes = ((decimal - hours) * 60.0).roundToInt()
            if (minutes >= 60) {
                hours += 1
                minutes -= 60
            }
            if (hours >= 24) hours %= 24
            if (hours < 0) hours = (hours + 24) % 24
            return LocalTime.of(hours, minutes)
        }

        val fajrTime = decimalToLocalTime(fajrDecimal)
        val sunriseTime = decimalToLocalTime(sunriseDecimal)
        val dhuhrTime = decimalToLocalTime(dhuhrDecimal)
        val asrTime = decimalToLocalTime(asrDecimal)
        val maghribTime = decimalToLocalTime(maghribDecimal)
        val ishaTime = decimalToLocalTime(ishaDecimal)

        // Extra times
        val sahriEndTime = fajrTime.plusMinutes(sahriOffsetMinutes.toLong()) // Precaution before/around Fajr (default -3 mins)
        val iftarTime = maghribTime.plusMinutes(iftarOffsetMinutes.toLong()) // Precaution before/around Maghrib
        val tahajjudEndTime = fajrTime.minusMinutes(15)
        val ishraqStartTime = sunriseTime.plusMinutes(15)

        // Convert to millisecond timestamps for selected location's timezone
        fun toMillis(lt: LocalTime): Long {
            return LocalDateTime.of(date, lt).atZone(zoneId).toInstant().toEpochMilli()
        }

        val now = LocalDateTime.now(zoneId)
        val nowMillis = java.time.ZonedDateTime.now(zoneId).toInstant().toEpochMilli()

        // Check forbidden prayer times (মাকরূহ সময়)
        // 1. Sunrise (Sunrise to Sunrise + 15 min)
        // 2. Solar Noon / Zawal (12 min before Dhuhr to Dhuhr)
        // 3. Sunset (15 min before Maghrib till Maghrib)
        val sunriseForbiddenEnd = sunriseTime.plusMinutes(15)

        fun createSinglePrayer(name: PrayerName, time: LocalTime, endTime: LocalTime? = null): SinglePrayerTime {
            val endDigits = endTime?.let { formatTimeDigits(it) } ?: ""
            val endFormatted = endTime?.let { formatTime12h(it) } ?: ""
            val rangeFormatted = if (endTime != null) {
                "${formatTime12h(time)} - ${formatTime12h(endTime)}"
            } else {
                formatTime12h(time)
            }
            return SinglePrayerTime(
                name = name,
                timeDigits = formatTimeDigits(time),
                amPm = formatAmPm(time),
                timeFormatted = formatTimeBn(time),
                timestampMillis = toMillis(time),
                endTimeDigits = endDigits,
                endTimeFormatted = endFormatted,
                timeRangeFormatted = rangeFormatted
            )
        }

        val rawPrayers = listOf(
            createSinglePrayer(PrayerName.FAJR, fajrTime, sunriseTime),
            createSinglePrayer(PrayerName.SUNRISE, sunriseTime, sunriseForbiddenEnd),
            createSinglePrayer(PrayerName.DHUHR, dhuhrTime, asrTime),
            createSinglePrayer(PrayerName.ASR, asrTime, maghribTime),
            createSinglePrayer(PrayerName.MAGHRIB, maghribTime, ishaTime),
            createSinglePrayer(PrayerName.ISHA, ishaTime, fajrTime)
        )

        // Determine current & next prayer
        val isToday = (date == LocalDate.now(zoneId))
        var currentPrayer: SinglePrayerTime? = null
        var nextPrayer: SinglePrayerTime? = null
        var remainingMillis = 0L

        if (isToday) {
            val fMillis = toMillis(fajrTime)
            val sMillis = toMillis(sunriseTime)
            val dMillis = toMillis(dhuhrTime)
            val aMillis = toMillis(asrTime)
            val mMillis = toMillis(maghribTime)
            val iMillis = toMillis(ishaTime)

            when {
                nowMillis < fMillis -> {
                    // Before Fajr (Night time)
                    currentPrayer = null
                    nextPrayer = rawPrayers[0] // Fajr
                    remainingMillis = fMillis - nowMillis
                }
                nowMillis < sMillis -> {
                    // Fajr
                    currentPrayer = rawPrayers[0]
                    nextPrayer = rawPrayers[1] // Sunrise
                    remainingMillis = sMillis - nowMillis
                }
                nowMillis < dMillis -> {
                    // Sunrise to Dhuhr (Ishraq/Chasht)
                    currentPrayer = rawPrayers[1]
                    nextPrayer = rawPrayers[2] // Dhuhr
                    remainingMillis = dMillis - nowMillis
                }
                nowMillis < aMillis -> {
                    // Dhuhr
                    currentPrayer = rawPrayers[2]
                    nextPrayer = rawPrayers[3] // Asr
                    remainingMillis = aMillis - nowMillis
                }
                nowMillis < mMillis -> {
                    // Asr
                    currentPrayer = rawPrayers[3]
                    nextPrayer = rawPrayers[4] // Maghrib
                    remainingMillis = mMillis - nowMillis
                }
                nowMillis < iMillis -> {
                    // Maghrib
                    currentPrayer = rawPrayers[4]
                    nextPrayer = rawPrayers[5] // Isha
                    remainingMillis = iMillis - nowMillis
                }
                else -> {
                    // After Isha
                    currentPrayer = rawPrayers[5]
                    // Next is tomorrow's Fajr
                    val tomorrowFajrMillis = LocalDateTime.of(date.plusDays(1), fajrTime).atZone(zoneId).toInstant().toEpochMilli()
                    nextPrayer = SinglePrayerTime(
                        name = PrayerName.FAJR,
                        timeDigits = formatTimeDigits(fajrTime),
                        amPm = formatAmPm(fajrTime),
                        timeFormatted = formatTimeBn(fajrTime),
                        timestampMillis = tomorrowFajrMillis,
                        endTimeDigits = formatTimeDigits(sunriseTime),
                        endTimeFormatted = formatTime12h(sunriseTime),
                        timeRangeFormatted = "${formatTime12h(fajrTime)} - ${formatTime12h(sunriseTime)}"
                    )
                    remainingMillis = tomorrowFajrMillis - nowMillis
                }
            }
        }

        val markedPrayers = rawPrayers.map { p ->
            p.copy(
                isCurrent = isToday && (currentPrayer?.name == p.name),
                isNext = isToday && (nextPrayer?.name == p.name)
            )
        }

        // Check forbidden prayer times (মাকরূহ সময়)
        // 1. Sunrise (Sunrise to Sunrise + 15 min)
        // 2. Solar Noon / Zawal (12 min before Dhuhr to Dhuhr)
        // 3. Sunset (15 min before Maghrib till Maghrib)
        val zawalForbiddenStart = dhuhrTime.minusMinutes(12)
        val sunsetForbiddenStart = maghribTime.minusMinutes(15)

        val forbiddenSunriseStr = "${formatTimeBn(sunriseTime)} - ${formatTimeBn(sunriseForbiddenEnd)}"
        val forbiddenMiddayStr = "${formatTimeBn(zawalForbiddenStart)} - ${formatTimeBn(dhuhrTime)}"
        val forbiddenSunsetStr = "${formatTimeBn(sunsetForbiddenStart)} - ${formatTimeBn(maghribTime)}"

        val forbiddenList = listOf(
            ForbiddenPrayerInterval(
                titleBn = "সূর্যোদয়ের সময়",
                timeRangeBn = forbiddenSunriseStr,
                subtitleBn = "সূর্য ওঠা শুরু থেকে ১৫ মিনিট পর্যন্ত"
            ),
            ForbiddenPrayerInterval(
                titleBn = "দ্বিপ্রহরের সময় (জাওয়াল)",
                timeRangeBn = forbiddenMiddayStr,
                subtitleBn = "ঠিক দুপুরে সূর্য মধ্যাকাশে অবস্থানকালে (যোহরের পূর্ববর্তী ১২ মিনিট)"
            ),
            ForbiddenPrayerInterval(
                titleBn = "সূর্যাস্তের সময়",
                timeRangeBn = forbiddenSunsetStr,
                subtitleBn = "সূর্যাস্তের পূর্ববর্তী ১৫ মিনিট থেকে মাগরিব পর্যন্ত"
            )
        )

        var isForbidden = false
        var forbiddenReason: String? = null

        if (isToday) {
            val currentTime = now.toLocalTime()

            if (currentTime.isAfter(sunriseTime) && currentTime.isBefore(sunriseForbiddenEnd)) {
                isForbidden = true
                forbiddenReason = "সূর্যোদয়ের নিষিদ্ধ সময় (মাকরূহ)"
            } else if (currentTime.isAfter(zawalForbiddenStart) && currentTime.isBefore(dhuhrTime)) {
                isForbidden = true
                forbiddenReason = "দ্বিপ্রহরের নিষিদ্ধ সময় (মাকরূহ)"
            } else if (currentTime.isAfter(sunsetForbiddenStart) && currentTime.isBefore(maghribTime)) {
                isForbidden = true
                forbiddenReason = "সূর্যাস্তের নিষিদ্ধ সময় (মাকরূহ)"
            }
        }

        // Calculate night milestones (for Isha, Tahajjud, etc.)
        val maghribMinutes = maghribTime.hour * 60 + maghribTime.minute
        val fajrMinutes = fajrTime.hour * 60 + fajrTime.minute
        val nightMinutesTotal = (fajrMinutes + 24 * 60 - maghribMinutes) % (24 * 60)
        val oneThirdNight = nightMinutesTotal / 3
        val halfNight = nightMinutesTotal / 2

        val ishaUttomEndMinutes = (maghribMinutes + oneThirdNight) % (24 * 60)
        val ishaUttomEndTime = LocalTime.of(ishaUttomEndMinutes / 60, ishaUttomEndMinutes % 60)

        val midnightMinutes = (maghribMinutes + halfNight) % (24 * 60)
        val midnightTime = LocalTime.of(midnightMinutes / 60, midnightMinutes % 60)

        val lastThirdMinutes = (fajrMinutes + 24 * 60 - oneThirdNight) % (24 * 60)
        val lastThirdTime = LocalTime.of(lastThirdMinutes / 60, lastThirdMinutes % 60)

        val duhaStartTime = sunriseTime.plusMinutes(16)
        val duhaEndTime = zawalForbiddenStart.minusMinutes(1)

        val fMorningRange = "${formatTimeDigits(sunriseTime)} - ${formatTimeDigits(sunriseForbiddenEnd)}"
        val fNoonRange = "${formatTimeDigits(zawalForbiddenStart)} - ${formatTimeDigits(dhuhrTime)}"
        val fEveRange = "${formatTimeDigits(sunsetForbiddenStart)} - ${formatTimeDigits(maghribTime.minusMinutes(1))}"

        val fajrRangeStr = "${formatTimeDigits(fajrTime)} - ${formatTimeDigits(sunriseTime)}"
        val dhuhrRangeStr = "${formatTimeDigits(dhuhrTime)} - ${formatTimeDigits(asrTime.minusMinutes(1))}"
        val asrRangeStr = "${formatTimeDigits(asrTime)} - ${formatTimeDigits(maghribTime.minusMinutes(1))}"
        val asrMakruhStr = formatTimeDigits(sunsetForbiddenStart)
        val maghribRangeStr = "${formatTimeDigits(maghribTime)} - ${formatTimeDigits(ishaTime.minusMinutes(1))}"
        val ishaRangeStr = "${formatTimeDigits(ishaTime)} - ${formatTimeDigits(fajrTime.minusMinutes(1))}"
        val ishaUttomStr = formatTimeDigits(ishaUttomEndTime)
        val ishaMakruhStr = formatTimeDigits(midnightTime)

        val duhaRangeStr = "${formatTimeDigits(duhaStartTime)} - ${formatTimeDigits(duhaEndTime)}"
        val zawalStartStr = formatTimeDigits(dhuhrTime.minusMinutes(4))
        val awwabinRangeStr = "মাগরিবের পর - ${formatTimeDigits(ishaTime.minusMinutes(1))}"
        val tahajjudRangeStr = "ইশার পর - ${formatTimeDigits(fajrTime.minusMinutes(1))}"
        val tahajjudLastThirdStr = formatTimeDigits(lastThirdTime)

        return DailyPrayerSchedule(
            dateStrBn = DateUtil.formatDateStr(date),
            district = district,
            prayers = markedPrayers,
            currentPrayer = currentPrayer,
            nextPrayer = nextPrayer,
            remainingTimeToNextFormatted = formatRemainingTimeBn(remainingMillis),
            isForbiddenTimeNow = isForbidden,
            forbiddenTimeReason = forbiddenReason,
            sahriEndTimeFormatted = formatTimeBn(sahriEndTime),
            iftarTimeFormatted = formatTimeBn(iftarTime),
            tahajjudEndTimeFormatted = formatTimeBn(tahajjudEndTime),
            ishraqStartTimeFormatted = formatTimeBn(ishraqStartTime),
            forbiddenSunriseFormatted = forbiddenSunriseStr,
            forbiddenMiddayFormatted = forbiddenMiddayStr,
            forbiddenSunsetFormatted = forbiddenSunsetStr,
            forbiddenTimesList = forbiddenList,
            fajrRange = fajrRangeStr,
            dhuhrRange = dhuhrRangeStr,
            asrRange = asrRangeStr,
            asrMakruhTime = asrMakruhStr,
            maghribRange = maghribRangeStr,
            ishaRange = ishaRangeStr,
            ishaUttomTime = ishaUttomStr,
            ishaMakruhTime = ishaMakruhStr,
            duhaRange = duhaRangeStr,
            zawalStartTime = zawalStartStr,
            awwabinRange = awwabinRangeStr,
            tahajjudRange = tahajjudRangeStr,
            tahajjudLastThirdStart = tahajjudLastThirdStr,
            forbiddenMorningRange = fMorningRange,
            forbiddenNoonRange = fNoonRange,
            forbiddenEveningRange = fEveRange,
            sunriseTimeDigits = formatTimeDigits(sunriseTime),
            sunsetTimeDigits = formatTimeDigits(maghribTime),
            sahriTimeDigits = formatTimeDigits(sahriEndTime),
            iftarTimeDigits = formatTimeDigits(iftarTime)
        )
    }

    fun formatTimeDigits(time: LocalTime): String {
        var hour = time.hour
        val minute = time.minute
        if (hour > 12) hour -= 12
        if (hour == 0) hour = 12

        val hourStr = toBengaliDigits(hour).padStart(2, '০')
        val minuteStr = toBengaliDigits(minute).padStart(2, '০')
        return "$hourStr:$minuteStr"
    }

    fun formatAmPm(time: LocalTime): String {
        return if (time.hour >= 12) "PM" else "AM"
    }

    fun formatTime12h(time: LocalTime): String {
        var hour = time.hour
        val minute = time.minute
        val amPm = if (hour >= 12) "PM" else "AM"
        if (hour > 12) hour -= 12
        if (hour == 0) hour = 12

        val hourStr = toBengaliDigits(hour)
        val minuteStr = toBengaliDigits(minute).padStart(2, '০')
        return "$hourStr:$minuteStr $amPm"
    }

    fun formatTimeBn(time: LocalTime): String {
        var hour = time.hour
        val minute = time.minute
        val isPm = hour >= 12
        val amPmBn = if (hour in 0..3) "রাত" else if (hour in 4..11) "সকাল" else if (hour in 12..15) "দুপুর" else if (hour in 16..17) "বিকাল" else if (hour in 18..19) "সন্ধ্যা" else "রাত"

        if (hour > 12) hour -= 12
        if (hour == 0) hour = 12

        val hourStr = toBengaliDigits(hour).padStart(2, '০')
        val minuteStr = toBengaliDigits(minute).padStart(2, '০')

        return "$hourStr:$minuteStr $amPmBn"
    }

    fun formatRemainingTimeBn(millis: Long): String {
        if (millis <= 0) return "ওয়াক্ত শুরু হয়েছে"
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60

        return when {
            hours > 0 && mins > 0 -> "${toBengaliDigits(hours.toInt())} ঘণ্টা ${toBengaliDigits(mins.toInt())} মিনিট বাকি"
            hours > 0 -> "${toBengaliDigits(hours.toInt())} ঘণ্টা বাকি"
            mins > 0 -> "${toBengaliDigits(mins.toInt())} মিনিট বাকি"
            else -> "কয়েক সেকেন্ড বাকি"
        }
    }

    fun toBengaliDigits(number: Int): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        return number.toString().map { if (it.isDigit()) bnDigits[it - '0'] else it }.joinToString("")
    }
}
