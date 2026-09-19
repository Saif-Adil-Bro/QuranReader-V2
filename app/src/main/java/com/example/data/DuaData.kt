package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DuaSegment(
    val arabic: String = "",
    val transliteration: String = "",
    val translation: String = "",
    val bottom: String = "",
    val reference: String = ""
)

data class DuaItem(
    val id: Int,
    val title: String,
    val segments: List<DuaSegment>
)

object DuaData {
    private var loadedRichDuas: List<DuaItem>? = null
    private var loadedMorningEveningDuas: List<DuaItem>? = null

    val richDuas: List<DuaItem>
        get() = loadedRichDuas ?: fallbackRichDuas

    val morningEveningDuas: List<DuaItem>
        get() = loadedMorningEveningDuas ?: emptyList()

    val dailyDuas: List<Pair<String, String>>
        get() = richDuas.map { item ->
            val desc = item.segments.joinToString("\n\n-------------------\n\n") { segment ->
                buildString {
                    if (segment.arabic.isNotEmpty() && segment.arabic != "null") {
                        append(segment.arabic).append("\n\n")
                    }
                    if (segment.transliteration.isNotEmpty() && segment.transliteration != "null") {
                        append("উচ্চারণ: ").append(segment.transliteration).append("\n\n")
                    }
                    if (segment.translation.isNotEmpty() && segment.translation != "null") {
                        append("অর্থ: ").append(segment.translation)
                    }
                    val ref = segment.reference.ifEmpty { segment.bottom }
                    if (ref.isNotEmpty() && ref != "null") {
                        append("\n(").append(ref).append(")")
                    }
                }
            }
            Pair(item.title, desc)
        }

    private val fallbackRichDuas = listOf(
        DuaItem(
            id = 1,
            title = "দুনিয়া ও পরকালের কল্যাণের দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "রব্বানা আতিনা ফিদ্দুনিয়া হাসানাতাওঁ ওয়া ফিল আখিরতি হাসানাতাওঁ ওয়াক্বিনা আযাবান্নার। (সুরা বাকারা: ২০১)\nঅর্থ: হে আমাদের রব! আমাদের দুনিয়া ও আখেরাতের কল্যাণ দান করুন এবং জাহান্নামের শাস্তি থেকে আমাদের বাঁচান।"
                )
            )
        ),
        DuaItem(
            id = 2,
            title = "ঈমানের ওপর অবিচল থাকার দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "রব্বানা লা তুযিগ ক্বুলুবানা বা'দা ইয হাদাইতানা ওয়াহাব লানা মিল্লাদুনকা রহমাহ, ইন্নাকা আনতাল ওয়াহহাব। (সুরা আল ইমরান: ৮)\nঅর্থ: হে আমাদের রব! আমাদের সরল পথ প্রদর্শনের পর আপনি আমাদের অন্তরকে সত্যলংঘনপ্রবণ করবেন না এবং আপনার পক্ষ থেকে অনুগ্রহ দান করুন।"
                )
            )
        ),
        DuaItem(
            id = 3,
            title = "জ্ঞান ও বক্ষ প্রশস্ত করার দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "রব্বিশ রাহলি সদরি ওয়া ইয়াসসির লি আমরি ওয়াহলুল উকদাতাম মিল লিসানি ইয়াফক্বাহু ক্বওলি। (সুরা তাহা: ২৫-২৮)\nঅর্থ: হে আমার রব! আমার বক্ষ প্রশস্ত করে দিন এবং আমার কাজ সহজ করুন এবং আমার জিহ্বার জড়তা দূর করুন যেন তারা আমার কথা বুঝতে পারে।"
                )
            )
        ),
        DuaItem(
            id = 4,
            title = "সৎ স্ত্রী ও সন্তান লাভের দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "রব্বানা হাবলানা মিন আযওয়াজিনা ওয়া যুররিয়্যাতিনা কুররতা আ'ইয়ুনিওঁ ওয়াজআলনা লিল মুত্তাক্বিনা ইমামা। (সুরা ফুরকান: ৭৪)\nঅর্থ: হে আমাদের রব! আমাদের জন্য এমন স্ত্রী ও সন্তান দান করুন যারা আমাদের চক্ষু শীতল করবে এবং আমাদের মুত্তাকীদের ইমাম করে দিন।"
                )
            )
        ),
        DuaItem(
            id = 5,
            title = "ক্ষমা ও রহমত কামনার দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "রব্বানা যালামনা আনফুসানা ওয়া ইল্লাম তাগফির লানা ওয়া তারহামনা লানাকুনান্না MINAL খাসিরিন। (সুরা আরাফ: ২৩)\nঅর্থ: হে আমাদের রব! আমরা নিজেদের প্রতি জুলুম করেছি, আর আপনি যদি আমাদের ক্ষমা না করেন এবং আমাদের প্রতি রহম না করেন, তবে অবশ্যই আমরা ক্ষতিগ্রস্তদের অন্তর্ভুক্ত হব।"
                )
            )
        ),
        DuaItem(
            id = 6,
            title = "বিপদ মুক্তির দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "লা ইলাহা ইল্লা আনতা সুবহানাকা ইন্নি কুনতু মিনায যালিমিন। (সুরা আম্বিয়া: ৮৭)\nঅর্থ: আপনি ছাড়া কোনো ইলাহ নেই, আপনি পবিত্র মহান। নিশ্চয়ই আমি জালিমদের অন্তর্ভুক্ত।"
                )
            )
        ),
        DuaItem(
            id = 7,
            title = "উপযোগী রিজিক ও আমলের দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "আল্লাহুম্মা ইন্নি আসআলুকাহু ইলমান নাফিআ, ওয়া রিযকান ত্বায়্যিবা, ওয়া আমালান মুতাকাব্বালা। (ইবনে মাজাহ: ৯২৫)\nঅর্থ: হে আল্লাহ! আমি আপনার কাছে উপকারী জ্ঞান, পবিত্র রিজিক এবং কবুলযোগ্য আমল প্রার্থনা করছি।"
                )
            )
        ),
        DuaItem(
            id = 8,
            title = "অন্টারকে দীনের ওপর স্থির রাখার দুআ",
            segments = listOf(
                DuaSegment(
                    translation = "ইয়া মুক্বাল্লিবাল কুলুব, ছাব্বিত ক্বলবি আলা দীনিক। (তিরমিযি: ২১৪০)\nঅর্থ: হে অন্তরসমূহ পরিবর্তনকারী! আমার অন্তরকে আপনার দীনের ওপর অবিচল রাখুন।"
                )
            )
        )
    )

    private val fallbackDuas = listOf(
        Pair("দুনিয়া ও পরকালের কল্যাণের দুআ", "রব্বানা আতিনা ফিদ্দুনিয়া হাসানাতাওঁ ওয়া ফিল আখিরতি হাসানাতাওঁ ওয়াক্বিনা আযাবান্নার। (সুরা বাকারা: ২০১)\nঅর্থ: হে আমাদের রব! আমাদের দুনিয়া ও আখেরাতের কল্যাণ দান করুন এবং জাহান্নামের শাস্তি থেকে আমাদের বাঁচান।"),
        Pair("ঈমানের ওপর অবিচল থাকার দুআ", "রব্বানা লা তুযিগ ক্বুলুবানা বা'দা ইয হাদাইতানা ওয়াহাব লানা মিল্লাদুনকা রহমাহ, ইন্নাকা আনতাল ওয়াহহাব। (সুরা আল ইমরান: ৮)\nঅর্থ: হে আমাদের রব! আমাদের সরল পথ প্রদর্শনের পর আপনি আমাদের অন্তরকে সত্যলংঘনপ্রবণ করবেন না এবং আপনার পক্ষ থেকে অনুগ্রহ দান করুন।"),
        Pair("জ্ঞান ও বক্ষ প্রশস্ত করার দুআ", "রব্বিশ রাহলি সদরি ওয়া ইয়াসসির লি আমরি ওয়াহলুল উকদাতাম মিল লিসানি ইয়াফক্বাহু ক্বওলি। (সুরা তাহা: ২৫-২৮)\nঅর্থ: হে আমার রব! আমার বক্ষ প্রশস্ত করে দিন এবং আমার কাজ সহজ করুন এবং আমার জিহ্বার জড়তা দূর করুন যেন তারা আমার কথা বুঝতে পারে।"),
        Pair("সৎ স্ত্রী ও সন্তান লাভের দুআ", "রব্বানা হাবলানা মিন আযওয়াজিনা ওয়া যুররিয়্যাতিনা কুররতা আ'ইয়ুনিওঁ ওয়াজআলনা লিল মুত্তাক্বিনা ইমামা। (সুরা ফুরকান: ৭৪)\nঅর্থ: হে আমাদের রব! আমাদের জন্য এমন স্ত্রী ও সন্তান দান করুন যারা আমাদের চক্ষু শীতল করবে এবং আমাদের মুত্তাকীদের ইমাম করে দিন।"),
        Pair("ক্ষমা ও রহমত কামনার দুআ", "রব্বানা যালামনা আনফুসানা ওয়া ইল্লাম তাগফির লানা ওয়া তারহামনা লানাকুনান্না MINAL খাসিরিন। (সুরা আরাফ: ২৩)\nঅর্থ: হে আমাদের রব! আমরা নিজেদের প্রতি জুলুম করেছি, আর আপনি যদি আমাদের ক্ষমা না করেন এবং আমাদের প্রতি রহম না করেন, তবে অবশ্যই আমরা ক্ষতিগ্রস্তদের অন্তর্ভুক্ত হব।"),
        Pair("বিপদ মুক্তির দুআ", "লা ইলাহা ইল্লা আনতা সুবহানাকা ইন্নি কুনতু মিনায যালিমিন। (সুরা আম্বিয়া: ৮৭)\nঅর্থ: আপনি ছাড়া কোনো ইলাহ নেই, আপনি পবিত্র মহান। নিশ্চয়ই আমি জালিমদের অন্তর্ভুক্ত।"),
        Pair("উপযোগী রিজিক ও আমলের দুআ", "আল্লাহুম্মা ইন্নি আসআলুকাহু ইলমান নাফিআ, ওয়া রিযকান ত্বায়্যিবা, ওয়া আমালান মুতাকাব্বালা। (ইবনে মাজাহ: ৯২৫)\nঅর্থ: হে আল্লাহ! আমি আপনার কাছে উপকারী জ্ঞান, পবিত্র রিজিক এবং কবুলযোগ্য আমল প্রার্থনা করছি।"),
        Pair("অন্টারকে দীনের ওপর স্থির রাখার দুআ", "ইয়া মুক্বাল্লিবাল কুলুব, ছাব্বিত ক্বলবি আলা দীনিক। (তিরমিযি: ২১৪০)\nঅর্থ: হে অন্তরসমূহ পরিবর্তনকারী! আমার অন্তরকে আপনার দীনের ওপর অবিচল রাখুন।")
    )

    fun initialize(context: Context) {
        val quranic = loadDuasFromAsset(context, "duas.json")
        if (quranic.isNotEmpty()) {
            loadedRichDuas = quranic
        }
        val morningEvening = loadDuasFromAsset(context, "morning_evening_duas.json")
        if (morningEvening.isNotEmpty()) {
            loadedMorningEveningDuas = morningEvening
        }
    }

    private fun loadDuasFromAsset(context: Context, fileName: String): List<DuaItem> {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }.trim()
            val richList = mutableListOf<DuaItem>()
            
            if (jsonString.startsWith("{")) {
                val jsonObject = JSONObject(jsonString)
                val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val title = obj.optString("duaname", "").ifEmpty { obj.optString("chapname", "") }
                    val globalId = obj.optInt("dua_global_id", i)
                    
                    val segmentList = mutableListOf<DuaSegment>()
                    val segments = obj.optJSONArray("segments")
                    if (segments != null && segments.length() > 0) {
                        for (j in 0 until segments.length()) {
                            val segment = segments.getJSONObject(j)
                            val arabic = segment.optString("arabic", "")
                            val transliteration = segment.optString("transliteration", "")
                            val translations = segment.optString("translations", "")
                            val bottom = segment.optString("bottom", "")
                            val reference = segment.optString("reference", "")
                            
                            segmentList.add(DuaSegment(
                                arabic = arabic,
                                transliteration = transliteration,
                                translation = translations,
                                bottom = bottom,
                                reference = reference
                            ))
                        }
                    } else {
                        val arabic = obj.optString("arabic", "")
                        val transliteration = obj.optString("transliteration", "")
                        val translation = obj.optString("translation", "")
                        val reference = obj.optString("reference", "")
                        segmentList.add(DuaSegment(
                            arabic = arabic,
                            transliteration = transliteration,
                            translation = translation,
                            reference = reference
                        ))
                    }
                    if (title.isNotEmpty()) {
                        richList.add(DuaItem(id = globalId, title = title, segments = segmentList))
                    }
                }
            } else if (jsonString.startsWith("[")) {
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optString("title", "")
                    val arabic = obj.optString("arabic", "")
                    val transliteration = obj.optString("transliteration", "")
                    val translation = obj.optString("translation", "").ifEmpty { obj.optString("desc", "") }
                    val reference = obj.optString("reference", "")
                    
                    val segment = DuaSegment(
                        arabic = arabic,
                        transliteration = transliteration,
                        translation = translation,
                        reference = reference
                    )
                    richList.add(DuaItem(id = i, title = title, segments = listOf(segment)))
                }
            }
            return richList
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    fun getDuaOfTheDay(): Pair<String, String> {
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val list = dailyDuas
        val index = dayOfYear % list.size
        return list[index]
    }

    fun getDuaItemOfTheDay(): DuaItem {
        val calendar = java.util.Calendar.getInstance()
        val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
        val list = richDuas
        val index = dayOfYear % list.size
        return list[index]
    }
}
