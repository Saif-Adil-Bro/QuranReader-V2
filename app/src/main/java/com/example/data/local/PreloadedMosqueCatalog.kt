package com.example.data.local

import android.location.Location
import com.example.data.model.Mosque

object PreloadedMosqueCatalog {

    data class RawMosque(
        val id: String,
        val nameBn: String,
        val nameEn: String,
        val lat: Double,
        val lon: Double,
        val addressBn: String,
        val cityOrDivision: String,
        val country: String = "Bangladesh",
        val isJuma: Boolean = true,
        val hasAblution: Boolean = true,
        val hasAc: Boolean = true,
        val hasFemale: Boolean = false,
        val contact: String = "",
        val notes: String = ""
    )

    val allPreloadedMosques = listOf(
        // ==========================================
        // DHAKA: KALACHANDPUR, KURMITOLA, BARIDHARA, GULSHAN, NIKUNJA, AIRPORT
        // ==========================================
        RawMosque(
            id = "pre_dhaka_kalachandpur_1",
            nameBn = "কালাচাঁদপুর বায়তুল জান্নাত জামে মসজিদ",
            nameEn = "Kalachandpur Baitul Jannat Jame Masjid",
            lat = 23.8055,
            lon = 90.4190,
            addressBn = "কালাচাঁদপুর প্রধান সড়ক, বারিধারা, ঢাকা ১২১২",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_kalachandpur_2",
            nameBn = "কালাচাঁদপুর কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Kalachandpur Central Jame Masjid",
            lat = 23.8038,
            lon = 90.4182,
            addressBn = "কালাচাঁদপুর, গুলশান/বারিধারা লিংক রোড, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_kurmitola_1",
            nameBn = "কুর্মিটোলা হাই স্কুল ও বায়তুল ফালাহ জামে মসজিদ",
            nameEn = "Kurmitola High School Jame Masjid",
            lat = 23.8122,
            lon = 90.4140,
            addressBn = "কুর্মিটোলা, বিমানবন্দর সড়ক সংলগ্ন, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_kurmitola_hosp",
            nameBn = "কুর্মিটোলা জেনারেল হাসপাতাল মসজিদ",
            nameEn = "Kurmitola General Hospital Masjid",
            lat = 23.8180,
            lon = 90.4075,
            addressBn = "কুর্মিটোলা জেনারেল হাসপাতাল প্রাঙ্গণ, বিমানবন্দর সড়ক",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_baridhara_dohs",
            nameBn = "বারিধারা ডিওএইচএস কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Baridhara DOHS Central Mosque",
            lat = 23.8085,
            lon = 90.4135,
            addressBn = "রোড ২, বারিধারা ডিওএইচএস, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_baridhara_j",
            nameBn = "বারিধারা কেন্দ্রীয় জামে মসজিদ ও মাদ্রাসা",
            nameEn = "Baridhara Central Jame Mosque & Madrasa",
            lat = 23.7995,
            lon = 90.4220,
            addressBn = "পার্ক রোড, ব্লক জে, বারিধারা কূটনৈতিক এলাকা, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_gulshan_society",
            nameBn = "গুলশান সোসাইটি জামে মসজিদ",
            nameEn = "Gulshan Society Jame Masjid",
            lat = 23.7936,
            lon = 90.4168,
            addressBn = "রোড ৬৩, গুলশান-২, ঢাকা ১২১২",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_gulshan_azad",
            nameBn = "গুলশান সেন্ট্রাল আজাদ মসজিদ",
            nameEn = "Gulshan Central Azad Mosque",
            lat = 23.7785,
            lon = 90.4160,
            addressBn = "রোড ২৪, গুলশান-১, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_banani_central",
            nameBn = "বনানী কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Banani Central Mosque",
            lat = 23.7932,
            lon = 90.4045,
            addressBn = "রোড ১১, ব্লক ডি, বনানী, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_banani_dohs",
            nameBn = "বনানী ডিওএইচএস জামে মসজিদ",
            nameEn = "Banani DOHS Jame Masjid",
            lat = 23.7890,
            lon = 90.3995,
            addressBn = "রোড ২, বনানী ডিওএইচএস, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_nikunja_2",
            nameBn = "নিকুঞ্জ-২ কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Nikunja-2 Central Jame Masjid",
            lat = 23.8245,
            lon = 90.4170,
            addressBn = "রোড ৯, নিকুঞ্জ-২, খিলক্ষেত, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_khilkhet_bottola",
            nameBn = "খিলক্ষেত বটতলা জামে মসজিদ",
            nameEn = "Khilkhet Bottola Jame Masjid",
            lat = 23.8290,
            lon = 90.4185,
            addressBn = "খিলক্ষেত বাজার রোড, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_bashundhara_central",
            nameBn = "বসুন্ধরা আবাসিক কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Bashundhara R/A Central Mosque",
            lat = 23.8190,
            lon = 90.4365,
            addressBn = "ব্লক ডি, বসুন্ধরা আবাসিক এলাকা, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_bashundhara_irc",
            nameBn = "ইসলামিক রিসার্চ সেন্টার জামে মসজিদ",
            nameEn = "Islamic Research Center Mosque",
            lat = 23.8130,
            lon = 90.4310,
            addressBn = "ব্লক সি, বসুন্ধরা আ/এ, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_airport",
            nameBn = "হযরত শাহজালাল আন্তর্জাতিক বিমানবন্দর জামে মসজিদ",
            nameEn = "HSIA Airport Terminal Jame Masjid",
            lat = 23.8435,
            lon = 90.4005,
            addressBn = "টার্মিনাল ১ সংলগ্ন, হযরত শাহজালাল আন্তর্জাতিক বিমানবন্দর, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_mohakhali_gausul",
            nameBn = "মহাখালী গাউসুল আজম জামে মসজিদ কমপ্লেক্স",
            nameEn = "Mohakhali Gausul Azam Mosque Complex",
            lat = 23.7770,
            lon = 90.4005,
            addressBn = "ওয়্যারলেস গেট, মহাখালী, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_mohakhali_dohs",
            nameBn = "মহাখালী ডিওএইচএস জামে মসজিদ",
            nameEn = "Mohakhali DOHS Jame Masjid",
            lat = 23.7820,
            lon = 90.3910,
            addressBn = "মহাখালী ডিওএইচএস রোড, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),

        // ==========================================
        // DHAKA: UTTARA, MIRPUR, MOHAMMADPUR, DHANMONDI
        // ==========================================
        RawMosque(
            id = "pre_dhaka_uttara_sec3",
            nameBn = "উত্তরা সেক্টর ৩ জামে মসজিদ",
            nameEn = "Uttara Sector 3 Jame Masjid",
            lat = 23.8685,
            lon = 90.3980,
            addressBn = "রোড ৭, সেক্টর ৩, উত্তরা, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_uttara_sec7",
            nameBn = "উত্তরা সেক্টর ৭ কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Uttara Sector 7 Central Mosque",
            lat = 23.8730,
            lon = 90.3990,
            addressBn = "সোনারগাঁও জনপদ মোড়, সেক্টর ৭, উত্তরা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_uttara_sec13",
            nameBn = "উত্তরা সেক্টর ১৩ গাউসুল আজম জামে মসজিদ",
            nameEn = "Uttara Sec 13 Gausul Azam Mosque",
            lat = 23.8765,
            lon = 90.3875,
            addressBn = "গাউসুল আজম এভিনিউ, সেক্টর ১৩, উত্তরা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_mirpur_1",
            nameBn = "মিরপুর ১ মুক্তবাংলা শাহী জামে মসজিদ",
            nameEn = "Mirpur-1 Muktobangla Shahi Mosque",
            lat = 23.7950,
            lon = 90.3540,
            addressBn = "মিরপুর ১ নম্বর গোলচত্বর সংলগ্ন, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_mirpur_2",
            nameBn = "মিরপুর ২ বায়তুল মোকাররম সোসাইটি জামে মসজিদ",
            nameEn = "Mirpur 2 Baitul Mukarram Mosque",
            lat = 23.8050,
            lon = 90.3605,
            addressBn = "ব্লক ডি, মিরপুর ২, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_mirpur_10",
            nameBn = "মিরপুর ১০ ফলপট্টি কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Mirpur 10 Central Mosque",
            lat = 23.8070,
            lon = 90.3685,
            addressBn = "মিরপুর ১০ নম্বর গোলচত্বর, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_mirpur_12",
            nameBn = "মিরপুর ১২ বায়তুল ফালাহ জামে মসজিদ",
            nameEn = "Mirpur 12 Baitul Falah Mosque",
            lat = 23.8260,
            lon = 90.3640,
            addressBn = "ব্লক ডি, মিরপুর ১২ বাস স্ট্যান্ড সংলগ্ন",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_pallabi",
            nameBn = "পল্লবী কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Pallabi Central Jame Mosque",
            lat = 23.8195,
            lon = 90.3610,
            addressBn = "সেকশন ১২, পল্লবী, মিরপুর, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_dhanmondi_27",
            nameBn = "সোবহানবাগ জামে মসজিদ",
            nameEn = "Sobhanbagh Jame Mosque",
            lat = 23.7525,
            lon = 90.3755,
            addressBn = "ধানমন্ডি ২৭ (মিরপুর রোড), ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_dhanmondi_eidgah",
            nameBn = "ধানমন্ডি ঈদগাহ জামে মসজিদ",
            nameEn = "Dhanmondi Eidgah Jame Mosque",
            lat = 23.7420,
            lon = 90.3780,
            addressBn = "রোড ৬/এ, ধানমন্ডি, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_dhanmondi_taqwa",
            nameBn = "তাকওয়া মসজিদ",
            nameEn = "Taqwa Mosque (Dhanmondi Lake)",
            lat = 23.7490,
            lon = 90.3735,
            addressBn = "লেকভিউ রোড, ধানমন্ডি ১১/এ, ঢাকা",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_sat_gambuj",
            nameBn = "মোহাম্মদপুর ঐতিহাসিক সাত গম্বুজ মসজিদ",
            nameEn = "Sat Gambuj Mosque Mohammadpur",
            lat = 23.7555,
            lon = 90.3585,
            addressBn = "সাত গম্বুজ রোড, মোহাম্মদপুর, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_mohammadpur_townhall",
            nameBn = "মোহাম্মদপুর টাউন হল জামে মসজিদ",
            nameEn = "Mohammadpur Town Hall Mosque",
            lat = 23.7580,
            lon = 90.3630,
            addressBn = "টাউন হল বাজার মোড়, মোহাম্মদপুর, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),

        // ==========================================
        // DHAKA: PALTAN, MOTIJHEEL, RAMNA, OLD DHAKA
        // ==========================================
        RawMosque(
            id = "pre_dhaka_baitul_mukarram",
            nameBn = "জাতীয় মসজিদ বায়তুল মোকাররম",
            nameEn = "Baitul Mukarram National Mosque",
            lat = 23.7289,
            lon = 90.4126,
            addressBn = "পল্টন, ঢাকা ১০০০",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_kakrail_markaz",
            nameBn = "কাকরাইল মারকাজ জামে মসজিদ",
            nameEn = "Kakrail Markaz Mosque",
            lat = 23.7385,
            lon = 90.4085,
            addressBn = "কাকরাইল, রমনা পার্ক সংলগ্ন, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_du_central",
            nameBn = "ঢাকা বিশ্ববিদ্যালয় কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Dhaka University Central Mosque",
            lat = 23.7335,
            lon = 90.3930,
            addressBn = "টিএসসি সংলগ্ন, ঢাকা বিশ্ববিদ্যালয় ক্যাম্পাস",
            cityOrDivision = "ঢাকা",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_dhaka_buet_central",
            nameBn = "বুয়েট কেন্দ্রীয় জামে মসজিদ",
            nameEn = "BUET Central Mosque",
            lat = 23.7265,
            lon = 90.3920,
            addressBn = "পলাশী মোড় সংলগ্ন, বুয়েট ক্যাম্পাস, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_lalbagh_shahi",
            nameBn = "লালবাগ কেল্লা শাহী জামে মসজিদ",
            nameEn = "Lalbagh Fort Shahi Mosque",
            lat = 23.7190,
            lon = 90.3880,
            addressBn = "লালবাগ কেল্লা কমপ্লেক্স, পুরান ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_tara_masjid",
            nameBn = "ঐতিহাসিক তারা মসজিদ (সিতারা মসজিদ)",
            nameEn = "Star Mosque (Tara Masjid)",
            lat = 23.7150,
            lon = 90.4020,
            addressBn = "আরমানিটোলা, আবুল খায়রাত রোড, পুরান ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_chawkbazar_shahi",
            nameBn = "চকবাজার শাহী জামে মসজিদ",
            nameEn = "Chawkbazar Shahi Mosque",
            lat = 23.7160,
            lon = 90.3955,
            addressBn = "চকবাজার মোড়, পুরান ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_moghbazar",
            nameBn = "মগবাজার কাজী অফিস জামে মসজিদ",
            nameEn = "Moghbazar Kazi Office Mosque",
            lat = 23.7510,
            lon = 90.4045,
            addressBn = "মগবাজার ওয়্যারলেস মোড়, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_malibagh_circle",
            nameBn = "মালিবাগ মোড় কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Malibagh Circle Central Mosque",
            lat = 23.7495,
            lon = 90.4165,
            addressBn = "মালিবাগ মোড়, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_shantinagar",
            nameBn = "শান্তিনগর বাজার জামে মসজিদ",
            nameEn = "Shantinagar Bazar Jame Mosque",
            lat = 23.7405,
            lon = 90.4150,
            addressBn = "শান্তিনগর প্রধান সড়ক, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_khilgaon_matir",
            nameBn = "খিলগাঁও মাটির জামে মসজিদ",
            nameEn = "Khilgaon Matir Mosque",
            lat = 23.7520,
            lon = 90.4245,
            addressBn = "তালতলা মার্কেট সংলগ্ন, খিলগাঁও, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),
        RawMosque(
            id = "pre_dhaka_motijheel_sonali",
            nameBn = "সোনালী ব্যাংক কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Sonali Bank Central Jame Mosque",
            lat = 23.7295,
            lon = 90.4190,
            addressBn = "মতিঝিল বাণিজ্যিক এলাকা, ঢাকা",
            cityOrDivision = "ঢাকা"
        ),

        // ==========================================
        // OTHER DIVISIONS & DISTRICTS OF BANGLADESH
        // ==========================================
        RawMosque(
            id = "pre_ctg_anderkilla",
            nameBn = "আন্দরকিল্লা শাহী জামে মসজিদ",
            nameEn = "Anderkilla Shahi Jame Mosque",
            lat = 22.3395,
            lon = 91.8360,
            addressBn = "আন্দরকিল্লা মোড়, কোতোয়ালি, চট্টগ্রাম",
            cityOrDivision = "চট্টগ্রাম",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_ctg_jamiatul_falah",
            nameBn = "জমিয়তুল ফালাহ জাতীয় মসজিদ",
            nameEn = "Jamiatul Falah National Mosque",
            lat = 22.3550,
            lon = 91.8220,
            addressBn = "দামপাড়া, ওয়াসার মোড় সংলগ্ন, চট্টগ্রাম",
            cityOrDivision = "চট্টগ্রাম",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_ctg_chandanpura",
            nameBn = "চন্দনপুরা তাজ জামে মসজিদ",
            nameEn = "Chandanpura Taj Mosque",
            lat = 22.3480,
            lon = 91.8400,
            addressBn = "সিরাজউদ্দৌলা রোড, চন্দনপুরা, চট্টগ্রাম",
            cityOrDivision = "চট্টগ্রাম"
        ),
        RawMosque(
            id = "pre_sylhet_shahjalal",
            nameBn = "হযরত শাহজালাল (রহ.) দরগাহ জামে মসজিদ",
            nameEn = "Hazrat Shahjalal (R) Dargah Mosque",
            lat = 24.9015,
            lon = 91.8680,
            addressBn = "দরগাহ মহল্লা, আম্বরখানা, সিলেট",
            cityOrDivision = "সিলেট",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_sylhet_shahparan",
            nameBn = "হযরত শাহপরান (রহ.) মাজার মসজিদ",
            nameEn = "Hazrat Shah Paran (R) Shrine Mosque",
            lat = 24.8975,
            lon = 91.9320,
            addressBn = "শাহপরান থানা, সিলেট",
            cityOrDivision = "সিলেট"
        ),
        RawMosque(
            id = "pre_sylhet_kudrat_ullah",
            nameBn = "কুদরত উল্লাহ জামে মসজিদ",
            nameEn = "Kudrat Ullah Jame Masjid",
            lat = 24.8920,
            lon = 91.8685,
            addressBn = "বন্দর বাজার, সিলেট",
            cityOrDivision = "সিলেট"
        ),
        RawMosque(
            id = "pre_raj_saheb_bazar",
            nameBn = "সাহেব বাজার বড় জামে মসজিদ",
            nameEn = "Saheb Bazar Central Mosque",
            lat = 24.3680,
            lon = 88.6010,
            addressBn = "সাহেব বাজার জিরো পয়েন্ট, রাজশাহী",
            cityOrDivision = "রাজশাহী"
        ),
        RawMosque(
            id = "pre_raj_bagha_shahi",
            nameBn = "ঐতিহাসিক বাঘা শাহী মসজিদ",
            nameEn = "Historical Bagha Shahi Mosque",
            lat = 24.1915,
            lon = 88.8350,
            addressBn = "বাঘা, রাজশাহী",
            cityOrDivision = "রাজশাহী"
        ),
        RawMosque(
            id = "pre_khulna_town",
            nameBn = "খুলনা টাউন জামে মসজিদ",
            nameEn = "Khulna Town Jame Mosque",
            lat = 22.8120,
            lon = 89.5645,
            addressBn = "ডাকবাংলা মোড় সংলগ্ন, খুলনা",
            cityOrDivision = "খুলনা"
        ),
        RawMosque(
            id = "pre_khulna_sixty_dome",
            nameBn = "ঐতিহাসিক ষাট গম্বুজ মসজিদ",
            nameEn = "Sixty Dome Mosque (Shat Gombuj)",
            lat = 22.6740,
            lon = 89.7420,
            addressBn = "বাগেরহাট সদর, খুলনা বিভাগ",
            cityOrDivision = "খুলনা"
        ),
        RawMosque(
            id = "pre_barisal_guthia",
            nameBn = "বায়তুল আমান জামে মসজিদ (গুঠিয়া মসজিদ)",
            nameEn = "Baitul Aman Jame Mosque (Guthia Mosque)",
            lat = 22.7530,
            lon = 90.2780,
            addressBn = "গুঠিয়া, উজিরপুর, বরিশাল",
            cityOrDivision = "বরিশাল",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_barisal_sadar",
            nameBn = "বরিশাল মডেল মসজিদ ও ইসলামিক সাংস্কৃতিক কেন্দ্র",
            nameEn = "Barishal Model Mosque & Islamic Center",
            lat = 22.7010,
            lon = 90.3700,
            addressBn = "বান্দ রোড, বরিশাল",
            cityOrDivision = "বরিশাল",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_rangpur_central",
            nameBn = "রংপুর কেন্দ্রীয় বড় জামে মসজিদ",
            nameEn = "Rangpur Central Grand Jame Mosque",
            lat = 25.7460,
            lon = 89.2510,
            addressBn = "জাহাজ কোম্পানি মোড়, রংপুর",
            cityOrDivision = "রংপুর"
        ),
        RawMosque(
            id = "pre_mym_boro_masjid",
            nameBn = "ময়মনসিংহ বড় জামে মসজিদ",
            nameEn = "Mymensingh Boro Jame Mosque",
            lat = 24.7570,
            lon = 90.4070,
            addressBn = "বড় বাজার মোড়, ময়মনসিংহ",
            cityOrDivision = "ময়মনসিংহ"
        ),
        RawMosque(
            id = "pre_cumilla_shah_shuja",
            nameBn = "শাহ সুজা ঐতিহাসিক জামে মসজিদ",
            nameEn = "Shah Shuja Historical Mosque",
            lat = 23.4610,
            lon = 91.1830,
            addressBn = "মুঘলটুলী, কুমিল্লা",
            cityOrDivision = "কুমিল্লা"
        ),
        RawMosque(
            id = "pre_gazipur_chourasta",
            nameBn = "গাজীপুর চান্দনা চৌরাস্তা কেন্দ্রীয় জামে মসজিদ",
            nameEn = "Gazipur Chandana Chowrasta Central Mosque",
            lat = 23.9980,
            lon = 90.3800,
            addressBn = "চান্দনা চৌরাস্তা, জয়দেবপুর, গাজীপুর",
            cityOrDivision = "গাজীপুর"
        ),
        RawMosque(
            id = "pre_gazipur_iut",
            nameBn = "আইইউটি কেন্দ্রীয় জামে মসজিদ",
            nameEn = "IUT Central Jame Mosque",
            lat = 23.9480,
            lon = 90.3820,
            addressBn = "ইসলামিক ইউনিভার্সিটি অব টেকনোলজি, বোর্ড বাজার, গাজীপুর",
            cityOrDivision = "গাজীপুর"
        ),
        RawMosque(
            id = "pre_narayanganj_chashara",
            nameBn = "চাষাড়া বাগে জান্নাত জামে মসজিদ",
            nameEn = "Chashara Bage Jannat Mosque",
            lat = 23.6235,
            lon = 90.4990,
            addressBn = "চাষাড়া গোলচত্বর সংলগ্ন, নারায়ণগঞ্জ",
            cityOrDivision = "নারায়ণগঞ্জ"
        ),

        // ==========================================
        // USA (CALIFORNIA, NEW YORK, TEXAS, MICHIGAN)
        // ==========================================
        RawMosque(
            id = "pre_usa_mca_santaclara",
            nameBn = "এমসিএ ইসলামিক সেন্টার (সান্টা ক্লারা)",
            nameEn = "Muslim Community Association (MCA)",
            lat = 37.3828,
            lon = -121.9680,
            addressBn = "3003 Scott Blvd, Santa Clara, CA 95054, USA",
            cityOrDivision = "California",
            country = "USA",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_usa_sbia_sanjose",
            nameBn = "এসবিআইএ ইসলামিক সেন্টার (সান হোসে)",
            nameEn = "South Bay Islamic Association (SBIA)",
            lat = 37.4080,
            lon = -121.8980,
            addressBn = "2345 Harris Way, San Jose, CA 95131, USA",
            cityOrDivision = "California",
            country = "USA",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_usa_mountainview",
            nameBn = "ইয়াসিন ফাউন্ডেশন ইসলামিক সেন্টার (মাউন্টেন ভিউ)",
            nameEn = "Yaseen Foundation / Mountain View Mosque",
            lat = 37.3861,
            lon = -122.0839,
            addressBn = "Mountain View, CA 94041, USA",
            cityOrDivision = "California",
            country = "USA",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_usa_king_fahad_la",
            nameBn = "কিং ফাহাদ মসজিদ (লস অ্যাঞ্জেলেস)",
            nameEn = "King Fahad Mosque (Culver City / LA)",
            lat = 34.0085,
            lon = -118.4060,
            addressBn = "10980 Washington Blvd, Culver City, CA 90232, USA",
            cityOrDivision = "California",
            country = "USA",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_usa_ny_icc",
            nameBn = "ইসলামিক কালচারাল সেন্টার অব নিউ ইয়র্ক",
            nameEn = "Islamic Cultural Center of New York (96th St)",
            lat = 40.7851,
            lon = -73.9525,
            addressBn = "1711 3rd Ave, New York, NY 10029, USA",
            cityOrDivision = "New York",
            country = "USA",
            hasFemale = true
        ),

        // ==========================================
        // INTERNATIONAL & HOLY LANDMARKS
        // ==========================================
        RawMosque(
            id = "pre_sa_haram_makkah",
            nameBn = "মসজিদুল হারাম (কাবা শরিফ)",
            nameEn = "Masjid al-Haram (Kaaba Sharif)",
            lat = 21.4225,
            lon = 39.8262,
            addressBn = "মক্কা মুকাররমা, সৌদি আরব",
            cityOrDivision = "Makkah",
            country = "Saudi Arabia",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_sa_nabawi_madinah",
            nameBn = "মসজিদে নববী (রওজা মোবারক)",
            nameEn = "Al-Masjid an-Nabawi (Madinah)",
            lat = 24.4672,
            lon = 39.6111,
            addressBn = "মদিনা মুনাওয়ারা, সৌদি আরব",
            cityOrDivision = "Madinah",
            country = "Saudi Arabia",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_sa_quba",
            nameBn = "মসজিদে কুবা",
            nameEn = "Masjid Quba (First Mosque in Islam)",
            lat = 24.4394,
            lon = 39.6172,
            addressBn = "আল হিজরা রোড, মদিনা মুনাওয়ারা",
            cityOrDivision = "Madinah",
            country = "Saudi Arabia",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_sa_qiblatayn",
            nameBn = "মসজিদে কিবলাতাইন",
            nameEn = "Masjid al-Qiblatayn",
            lat = 24.4842,
            lon = 39.5786,
            addressBn = "মদিনা মুনাওয়ারা, সৌদি আরব",
            cityOrDivision = "Madinah",
            country = "Saudi Arabia"
        ),
        RawMosque(
            id = "pre_palestine_aqsa",
            nameBn = "মসজিদুল আকসা (বায়তুল মুকাদ্দাস)",
            nameEn = "Al-Aqsa Mosque (Jerusalem)",
            lat = 31.7761,
            lon = 35.2358,
            addressBn = "ওল্ড সিটি, বায়তুল মুকাদ্দাস, ফিলিস্তিন",
            cityOrDivision = "Jerusalem",
            country = "Palestine",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_uae_sheikh_zayed",
            nameBn = "শেখ জায়েদ গ্র্যান্ড মসজিদ",
            nameEn = "Sheikh Zayed Grand Mosque",
            lat = 24.4128,
            lon = 54.4749,
            addressBn = "আল রাওয়াদাহ, আবুধাবি, সংযুক্ত আরব আমিরাত",
            cityOrDivision = "Abu Dhabi",
            country = "UAE",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_turkey_hagia_sophia",
            nameBn = "হায়া সোফিয়া গ্র্যান্ড মসজিদ",
            nameEn = "Hagia Sophia Grand Mosque",
            lat = 41.0086,
            lon = 28.9802,
            addressBn = "সুলতানাহমেত স্কয়ার, ফাতিহ, ইস্তাম্বুল, তুরস্ক",
            cityOrDivision = "Istanbul",
            country = "Turkey",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_turkey_blue_mosque",
            nameBn = "সুলতান আহমেদ মসজিদ (ব্লু মস্ক)",
            nameEn = "Blue Mosque (Sultanahmet)",
            lat = 41.0054,
            lon = 28.9768,
            addressBn = "ইস্তাম্বুল, তুরস্ক",
            cityOrDivision = "Istanbul",
            country = "Turkey",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_uk_east_london",
            nameBn = "ইস্ট লন্ডন মসজিদ ও লন্ডন মুসলিম সেন্টার",
            nameEn = "East London Mosque & London Muslim Centre",
            lat = 51.5186,
            lon = -0.0656,
            addressBn = "82-92 Whitechapel Rd, London E1 1JQ, UK",
            cityOrDivision = "London",
            country = "UK",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_uk_london_central",
            nameBn = "লন্ডন সেন্ট্রাল মস্ক (রিজেন্টস পার্ক)",
            nameEn = "London Central Mosque (Regent's Park)",
            lat = 51.5286,
            lon = -0.1662,
            addressBn = "146 Park Rd, London NW8 7RG, UK",
            cityOrDivision = "London",
            country = "UK",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_my_putra_mosque",
            nameBn = "পুত্রা মসজিদ (গোলাপি মসজিদ)",
            nameEn = "Putra Mosque (Pink Mosque)",
            lat = 2.9361,
            lon = 101.6892,
            addressBn = "পুত্রাজায়া, মালয়েশিয়া",
            cityOrDivision = "Putrajaya",
            country = "Malaysia",
            hasFemale = true
        ),
        RawMosque(
            id = "pre_id_istiqlal",
            nameBn = "মসজিদ ইস্তিকলাল (জাকার্তা)",
            nameEn = "Istiqlal Mosque Jakarta",
            lat = -6.1702,
            lon = 106.8314,
            addressBn = "সেন্ট্রাল জাকার্তা, ইন্দোনেশিয়া",
            cityOrDivision = "Jakarta",
            country = "Indonesia",
            hasFemale = true
        )
    )

    fun getMosquesNear(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int,
        favoriteIds: Set<String>
    ): List<Mosque> {
        val list = mutableListOf<Mosque>()

        for (raw in allPreloadedMosques) {
            val dist = calculateDistance(userLat, userLon, raw.lat, raw.lon)
            val bearing = calculateBearing(userLat, userLon, raw.lat, raw.lon)

            // If within specified search radius or within maximum neighborhood radius (up to 25km for city/metro)
            val maxAllowedRadius = (radiusMeters * 2.5f).coerceAtLeast(15000f)
            if (dist <= maxAllowedRadius) {
                list.add(
                    Mosque(
                        id = raw.id,
                        name = raw.nameBn,
                        nameEn = raw.nameEn,
                        latitude = raw.lat,
                        longitude = raw.lon,
                        address = raw.addressBn,
                        distanceMeters = dist,
                        bearing = bearing,
                        isJumaMosque = raw.isJuma,
                        hasAblution = raw.hasAblution,
                        hasAc = raw.hasAc,
                        hasFemalePrayerSpace = raw.hasFemale,
                        isFavorite = favoriteIds.contains(raw.id),
                        isCustomAdded = false,
                        contactPhone = raw.contact,
                        jamatTimes = getDefaultJamatTimes(),
                        notes = raw.notes,
                        source = "preloaded"
                    )
                )
            }
        }

        return list.sortedBy { it.distanceMeters }
    }

    private fun calculateDistance(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(startLat, startLon, endLat, endLon, results)
        return results[0]
    }

    private fun calculateBearing(startLat: Double, startLon: Double, endLat: Double, endLon: Double): Float {
        val loc1 = Location("").apply {
            latitude = startLat
            longitude = startLon
        }
        val loc2 = Location("").apply {
            latitude = endLat
            longitude = endLon
        }
        val b = loc1.bearingTo(loc2)
        return (b + 360) % 360
    }

    private fun getDefaultJamatTimes(): Map<String, String> {
        return mapOf(
            "ফজর" to "৫:১৫ AM",
            "যোহর" to "১:৩০ PM",
            "আসর" to "৫:০০ PM",
            "মাগরিব" to "সূর্যাস্তের ৫ মিনিট পর",
            "ইশা" to "৮:০০ PM",
            "জুমা" to "১:৩০ PM"
        )
    }
}
