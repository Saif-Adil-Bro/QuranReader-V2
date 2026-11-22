package com.example.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class DuaSegment(
    val top: String = "",
    val arabic: String = "",
    val transliteration: String = "",
    val translation: String = "",
    val bottom: String = "",
    val reference: String = ""
)

data class DuaItem(
    val id: Int,
    val title: String,
    val segments: List<DuaSegment>,
    val recommendedCount: Int = 1
)

enum class MorningEveningSection(val key: String) {
    FAZILAT("fazilat"),
    MORNING("morning"),
    EVENING("evening")
}

object DuaData {
    private var loadedRichDuas: List<DuaItem>? = null
    private var loadedMorningEveningDuas: List<DuaItem>? = null
    private var loadedMorningDuas: List<DuaItem>? = null
    private var loadedEveningDuas: List<DuaItem>? = null
    private var loadedFazilatDuas: List<DuaItem>? = null

    val richDuas: List<DuaItem>
        get() = loadedRichDuas ?: fallbackRichDuas

    val morningDuas: List<DuaItem>
        get() = loadedMorningDuas ?: emptyList()

    val eveningDuas: List<DuaItem>
        get() = loadedEveningDuas ?: emptyList()

    val fazilatDuas: List<DuaItem>
        get() = loadedFazilatDuas ?: emptyList()

    val morningEveningDuas: List<DuaItem>
        get() = loadedMorningEveningDuas ?: emptyList()

    fun getDuasForSection(section: MorningEveningSection): List<DuaItem> = when (section) {
        MorningEveningSection.FAZILAT -> fazilatDuas
        MorningEveningSection.MORNING -> morningDuas
        MorningEveningSection.EVENING -> eveningDuas
    }

    fun findMorningEveningSectionAndDua(duaId: Int): Pair<MorningEveningSection, DuaItem>? {
        morningDuas.find { it.id == duaId }?.let { return MorningEveningSection.MORNING to it }
        eveningDuas.find { it.id == duaId }?.let { return MorningEveningSection.EVENING to it }
        fazilatDuas.find { it.id == duaId }?.let { return MorningEveningSection.FAZILAT to it }
        return null
    }

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
        if (loadedRichDuas == null) {
            val quranic = loadDuasFromAsset(context, "duas.json")
            if (quranic.isNotEmpty()) {
                loadedRichDuas = quranic
            }
        }
        if (loadedMorningEveningDuas == null) {
            loadMorningEveningDuasFromAsset(context, "morning_evening_duas.json")
        }
    }

    private fun JSONObject.optCleanString(key: String): String {
        if (isNull(key)) return ""
        val value = optString(key, "")
        return if (value == "null") "" else value
    }

    /**
     * Infers the Sunnah repetition count (e.g., 3, 4, 7, 10, 100) from Bengali cue text.
     * Defaults to 1 when no explicit repetition count is mentioned.
     */
    fun parseRecommendedCount(title: String, segments: List<DuaSegment>): Int {
        val combined = buildString {
            append(title).append(' ')
            segments.forEach { seg ->
                if (seg.top.isNotBlank()) append(seg.top).append(' ')
                if (seg.bottom.isNotBlank()) append(seg.bottom).append(' ')
            }
        }
        return when {
            Regex("(?:১০০|100|একশত?|একশো)\\s*বার").containsMatchIn(combined) -> 100
            Regex("(?:১০|10|দশ)\\s*বার").containsMatchIn(combined) -> 10
            Regex("(?:৭|7|সাত)\\s*বার").containsMatchIn(combined) -> 7
            Regex("(?:৪|4|চার)\\s*বার").containsMatchIn(combined) -> 4
            Regex("(?:৩|3|তিন)\\s*বার").containsMatchIn(combined) -> 3
            else -> 1
        }
    }

    private fun parseDuaArray(dataArray: JSONArray): List<DuaItem> {
        val list = mutableListOf<DuaItem>()
        for (i in 0 until dataArray.length()) {
            val obj = dataArray.getJSONObject(i)
            val title = obj.optCleanString("duaname").ifEmpty { obj.optCleanString("chapname") }
            val globalId = obj.optInt("dua_global_id", i)

            val segmentList = mutableListOf<DuaSegment>()
            val segments = obj.optJSONArray("segments")
            if (segments != null && segments.length() > 0) {
                for (j in 0 until segments.length()) {
                    val segment = segments.getJSONObject(j)
                    segmentList.add(
                        DuaSegment(
                            top = segment.optCleanString("top"),
                            arabic = segment.optCleanString("arabic"),
                            transliteration = segment.optCleanString("transliteration"),
                            translation = segment.optCleanString("translations"),
                            bottom = segment.optCleanString("bottom"),
                            reference = segment.optCleanString("reference")
                        )
                    )
                }
            } else {
                segmentList.add(
                    DuaSegment(
                        top = obj.optCleanString("top"),
                        arabic = obj.optCleanString("arabic"),
                        transliteration = obj.optCleanString("transliteration"),
                        translation = obj.optCleanString("translation"),
                        reference = obj.optCleanString("reference")
                    )
                )
            }
            if (title.isNotEmpty()) {
                val count = parseRecommendedCount(title, segmentList)
                list.add(
                    DuaItem(
                        id = globalId,
                        title = title,
                        segments = segmentList,
                        recommendedCount = count
                    )
                )
            }
        }
        return list
    }

    private fun loadMorningEveningDuasFromAsset(context: Context, fileName: String) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }.trim()
            if (!jsonString.startsWith("{")) return
            val jsonObject = JSONObject(jsonString)

            val morningArr = jsonObject.optJSONArray("morning_duas")
            val eveningArr = jsonObject.optJSONArray("evening_duas")
            val fazilatArr = jsonObject.optJSONArray("fazilat")

            if (morningArr != null || eveningArr != null || fazilatArr != null) {
                val morning = morningArr?.let { parseDuaArray(it) } ?: emptyList()
                val evening = eveningArr?.let { parseDuaArray(it) } ?: emptyList()
                val fazilat = fazilatArr?.let { parseDuaArray(it) } ?: emptyList()

                loadedMorningDuas = morning
                loadedEveningDuas = evening
                loadedFazilatDuas = fazilat

                // Combined deduplicated view for backward-compatible bookmark lookups
                val seenIds = mutableSetOf<Int>()
                val combined = mutableListOf<DuaItem>()
                (morning + evening + fazilat).forEach { item ->
                    if (seenIds.add(item.id)) {
                        combined.add(item)
                    }
                }
                loadedMorningEveningDuas = combined
            } else {
                // Legacy flat "data" array fallback
                val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
                val legacy = parseDuaArray(dataArray)
                loadedMorningEveningDuas = legacy
                loadedMorningDuas = legacy
                loadedEveningDuas = legacy
                loadedFazilatDuas = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadDuasFromAsset(context: Context, fileName: String): List<DuaItem> {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }.trim()
            if (jsonString.startsWith("{")) {
                val jsonObject = JSONObject(jsonString)
                val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
                return parseDuaArray(dataArray)
            } else if (jsonString.startsWith("[")) {
                val richList = mutableListOf<DuaItem>()
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val title = obj.optCleanString("title")
                    val segment = DuaSegment(
                        arabic = obj.optCleanString("arabic"),
                        transliteration = obj.optCleanString("transliteration"),
                        translation = obj.optCleanString("translation").ifEmpty { obj.optCleanString("desc") },
                        reference = obj.optCleanString("reference")
                    )
                    richList.add(DuaItem(id = i, title = title, segments = listOf(segment)))
                }
                return richList
            }
            return emptyList()
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
