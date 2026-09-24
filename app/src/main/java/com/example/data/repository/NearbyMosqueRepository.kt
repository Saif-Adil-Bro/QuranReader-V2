package com.example.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.example.data.api.GeoapifyService
import com.example.data.model.Mosque
import com.example.data.model.PlaceSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class NearbyMosqueRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("nearby_mosques_prefs", Context.MODE_PRIVATE)
    private val FAVORITES_KEY = "favorite_mosque_ids"
    private val CUSTOM_MOSQUES_KEY = "custom_mosques_json"

    val geoapifyService = GeoapifyService(context)

    private val overpassEndpoints = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.kumi.systems/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
    )

    fun getFavoriteIds(): Set<String> {
        return prefs.getStringSet(FAVORITES_KEY, emptySet()) ?: emptySet()
    }

    fun toggleFavorite(mosqueId: String): Boolean {
        val current = getFavoriteIds().toMutableSet()
        val isNowFav = if (current.contains(mosqueId)) {
            current.remove(mosqueId)
            false
        } else {
            current.add(mosqueId)
            true
        }
        prefs.edit().putStringSet(FAVORITES_KEY, current).apply()
        return isNowFav
    }

    fun getCustomMosques(): List<Mosque> {
        val jsonStr = prefs.getString(CUSTOM_MOSQUES_KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<Mosque>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val jamatObj = obj.optJSONObject("jamatTimes")
                val jamatMap = mutableMapOf<String, String>()
                if (jamatObj != null) {
                    val keys = jamatObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        jamatMap[k] = jamatObj.optString(k, "")
                    }
                }

                list.add(
                    Mosque(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        nameEn = obj.optString("nameEn", ""),
                        latitude = obj.getDouble("latitude"),
                        longitude = obj.getDouble("longitude"),
                        address = obj.optString("address", ""),
                        isJumaMosque = obj.optBoolean("isJumaMosque", true),
                        hasAblution = obj.optBoolean("hasAblution", true),
                        hasAc = obj.optBoolean("hasAc", true),
                        hasFemalePrayerSpace = obj.optBoolean("hasFemalePrayerSpace", false),
                        isCustomAdded = true,
                        contactPhone = obj.optString("contactPhone", ""),
                        jamatTimes = jamatMap,
                        notes = obj.optString("notes", ""),
                        addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis())
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveCustomMosque(mosque: Mosque) {
        val currentList = getCustomMosques().toMutableList()
        val existingIndex = currentList.indexOfFirst { it.id == mosque.id }
        if (existingIndex >= 0) {
            currentList[existingIndex] = mosque
        } else {
            currentList.add(0, mosque)
        }

        val arr = JSONArray()
        for (m in currentList) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("nameEn", m.nameEn)
                put("latitude", m.latitude)
                put("longitude", m.longitude)
                put("address", m.address)
                put("isJumaMosque", m.isJumaMosque)
                put("hasAblution", m.hasAblution)
                put("hasAc", m.hasAc)
                put("hasFemalePrayerSpace", m.hasFemalePrayerSpace)
                put("contactPhone", m.contactPhone)
                put("notes", m.notes)
                put("addedTimestamp", m.addedTimestamp)
                val jObj = JSONObject()
                m.jamatTimes.forEach { (k, v) -> jObj.put(k, v) }
                put("jamatTimes", jObj)
            }
            arr.put(obj)
        }
        prefs.edit().putString(CUSTOM_MOSQUES_KEY, arr.toString()).apply()
    }

    fun deleteCustomMosque(mosqueId: String) {
        val currentList = getCustomMosques().filter { it.id != mosqueId }
        val arr = JSONArray()
        for (m in currentList) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("nameEn", m.nameEn)
                put("latitude", m.latitude)
                put("longitude", m.longitude)
                put("address", m.address)
                put("isJumaMosque", m.isJumaMosque)
                put("hasAblution", m.hasAblution)
                put("hasAc", m.hasAc)
                put("hasFemalePrayerSpace", m.hasFemalePrayerSpace)
                put("contactPhone", m.contactPhone)
                put("notes", m.notes)
                put("addedTimestamp", m.addedTimestamp)
                val jObj = JSONObject()
                m.jamatTimes.forEach { (k, v) -> jObj.put(k, v) }
                put("jamatTimes", jObj)
            }
            arr.put(obj)
        }
        prefs.edit().putString(CUSTOM_MOSQUES_KEY, arr.toString()).apply()
    }

    /**
     * Globally search for places, cities, towns, or mosques using Geoapify + Photon (Komoot) + Nominatim (100% Free)
     */
    suspend fun searchPlacesGlobally(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val results = mutableListOf<PlaceSearchResult>()

        try {
            // 1. First attempt Photon / Geoapify Service
            val geoResults = geoapifyService.searchPlacesGlobally(query)
            if (geoResults.isNotEmpty()) {
                results.addAll(geoResults)
            }

            // 2. Also check system Geocoder
            if (Geocoder.isPresent()) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(query, 5)
                    if (!addresses.isNullOrEmpty()) {
                        for ((idx, addr) in addresses.withIndex()) {
                            val name = addr.featureName ?: addr.locality ?: addr.adminArea ?: query
                            val disp = listOfNotNull(addr.subLocality, addr.locality, addr.adminArea, addr.countryName)
                                .filter { it.isNotBlank() }
                                .joinToString(", ")
                            if (results.none { calculateDistance(it.latitude, it.longitude, addr.latitude, addr.longitude) < 500 }) {
                                results.add(
                                    PlaceSearchResult(
                                        placeId = "geo_${addr.latitude}_${addr.longitude}_$idx",
                                        name = name,
                                        displayName = if (disp.isNotEmpty()) disp else addr.getAddressLine(0) ?: name,
                                        latitude = addr.latitude,
                                        longitude = addr.longitude,
                                        country = addr.countryName ?: ""
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    /**
     * Reverse geocodes coordinates anywhere in the world to a human-readable location
     */
    suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val parts = listOfNotNull(addr.subLocality, addr.locality, addr.adminArea, addr.countryName)
                        .filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        return@withContext parts.distinct().joinToString(", ")
                    }
                    val line = addr.getAddressLine(0)
                    if (!line.isNullOrBlank()) return@withContext line
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Online Reverse Geocode fallback via Nominatim
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("User-Agent", "QawmiManager-Android-GlobalMosqueFinder/1.0")
            }
            if (conn.responseCode == 200) {
                val jsonText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val obj = JSONObject(jsonText)
                val addressObj = obj.optJSONObject("address")
                if (addressObj != null) {
                    val city = addressObj.optString("city", "").ifEmpty {
                        addressObj.optString("town", "").ifEmpty {
                            addressObj.optString("village", "").ifEmpty {
                                addressObj.optString("state", "")
                            }
                        }
                    }
                    val country = addressObj.optString("country", "")
                    val parts = listOf(city, country).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) return@withContext parts.joinToString(", ")
                }
                val disp = obj.optString("display_name", "")
                if (disp.isNotEmpty()) return@withContext disp.split(",").take(3).joinToString(", ")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        "শনাক্তকৃত এলাকা"
    }

    /**
     * Real-time global mosque fetcher using Overpass API with multiple mirrors and Nominatim fallback
     */
    suspend fun fetchNearbyMosques(
        userLat: Double,
        userLon: Double,
        radiusMeters: Int = 3000
    ): List<Mosque> = withContext(Dispatchers.IO) {
        val favoriteIds = getFavoriteIds()
        val customMosques = getCustomMosques()
        val resultList = mutableListOf<Mosque>()
        val seenIds = mutableSetOf<String>()

        // 1. Add user custom mosques within radius
        for (cm in customMosques) {
            val dist = calculateDistance(userLat, userLon, cm.latitude, cm.longitude)
            val bearing = calculateBearing(userLat, userLon, cm.latitude, cm.longitude)
            val updated = cm.copy(
                distanceMeters = dist,
                bearing = bearing,
                isFavorite = favoriteIds.contains(cm.id)
            )
            resultList.add(updated)
            seenIds.add(cm.id)
        }

        // 2. Fetch from Geoapify Places API (if configured)
        try {
            val geoMosques = geoapifyService.searchNearbyMosques(userLat, userLon, radiusMeters)
            for (gm in geoMosques) {
                val isFav = favoriteIds.contains(gm.id)
                val updated = gm.copy(isFavorite = isFav)
                resultList.add(updated)
                seenIds.add(gm.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. Fetch from OpenStreetMap Overpass API (tries primary + secondary mirrors)
        val overpassQuery = """
            [out:json][timeout:15];
            (
              node["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$userLat,$userLon);
              way["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$userLat,$userLon);
              relation["amenity"="place_of_worship"]["religion"="muslim"](around:$radiusMeters,$userLat,$userLon);
              node["building"="mosque"](around:$radiusMeters,$userLat,$userLon);
              way["building"="mosque"](around:$radiusMeters,$userLat,$userLon);
              relation["building"="mosque"](around:$radiusMeters,$userLat,$userLon);
            );
            out center tags;
        """.trimIndent()

        var fetchSuccess = false
        for (endpoint in overpassEndpoints) {
            if (fetchSuccess) break
            try {
                val url = URL(endpoint)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                    setRequestProperty("User-Agent", "QawmiManager-Android-GlobalMosqueFinder/1.0")
                }

                val postData = "data=" + URLEncoder.encode(overpassQuery, "UTF-8")
                OutputStreamWriter(connection.outputStream).use { it.write(postData) }

                if (connection.responseCode == 200) {
                    val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    val root = JSONObject(responseText)
                    val elements = root.optJSONArray("elements") ?: JSONArray()

                    for (i in 0 until elements.length()) {
                        val elem = elements.getJSONObject(i)
                        val id = "osm_" + elem.optString("type", "node") + "_" + elem.optLong("id")
                        if (seenIds.contains(id)) continue

                        val lat = if (elem.has("lat")) elem.getDouble("lat") else elem.optJSONObject("center")?.optDouble("lat") ?: continue
                        val lon = if (elem.has("lon")) elem.getDouble("lon") else elem.optJSONObject("center")?.optDouble("lon") ?: continue
                        if (resultList.any { calculateDistance(it.latitude, it.longitude, lat, lon) < 35 }) continue

                        val tags = elem.optJSONObject("tags") ?: JSONObject()
                        val nameBn = tags.optString("name:bn", "").ifEmpty { tags.optString("name", "") }
                        val nameEn = tags.optString("name:en", "").ifEmpty { tags.optString("name", "") }
                        val nameAr = tags.optString("name:ar", "")
                        val rawName = if (nameBn.isNotEmpty()) nameBn else if (nameEn.isNotEmpty()) nameEn else if (nameAr.isNotEmpty()) nameAr else "Masjid"

                        val street = tags.optString("addr:street", "")
                        val suburb = tags.optString("addr:suburb", "").ifEmpty {
                            tags.optString("addr:city", "").ifEmpty { tags.optString("addr:district", "") }
                        }
                        val addressParts = listOf(street, suburb).filter { it.isNotBlank() }
                        val address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "নিকটবর্তী এলাকা"

                        val hasAblution = tags.optString("toilet", "").isNotEmpty() || tags.optString("wudu", "") == "yes" || true
                        val hasAc = tags.optString("air_conditioning", "") == "yes" || true
                        val hasFemale = tags.optString("female", "") == "yes" || tags.optString("women", "") == "yes"
                        val isJuma = tags.optString("denomination", "").contains("sunni", ignoreCase = true) || true

                        val dist = calculateDistance(userLat, userLon, lat, lon)
                        val bearing = calculateBearing(userLat, userLon, lat, lon)

                        val mosque = Mosque(
                            id = id,
                            name = formatMosqueName(rawName),
                            nameEn = if (nameEn.isNotEmpty()) nameEn else nameBn,
                            latitude = lat,
                            longitude = lon,
                            address = address,
                            distanceMeters = dist,
                            bearing = bearing,
                            isJumaMosque = isJuma,
                            hasAblution = hasAblution,
                            hasAc = hasAc,
                            hasFemalePrayerSpace = hasFemale,
                            isFavorite = favoriteIds.contains(id),
                            isCustomAdded = false,
                            contactPhone = tags.optString("phone", tags.optString("contact:phone", "")),
                            jamatTimes = getDefaultJamatTimes(),
                            notes = tags.optString("description", "")
                        )
                        resultList.add(mosque)
                        seenIds.add(id)
                    }
                    if (elements.length() > 0) {
                        fetchSuccess = true
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint mirror
            }
        }

        // 3. If Overpass yielded no results (or failed in remote location), search Nominatim for mosques around this location
        if (resultList.isEmpty()) {
            try {
                val delta = (radiusMeters / 111000.0) // 1 deg ~ 111 km
                val left = userLon - delta
                val top = userLat + delta
                val right = userLon + delta
                val bottom = userLat - delta
                val nomUrl = URL("https://nominatim.openstreetmap.org/search?q=mosque&format=json&viewbox=$left,$top,$right,$bottom&bounded=1&limit=25")
                val conn = (nomUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("User-Agent", "QawmiManager-Android-GlobalMosqueFinder/1.0")
                }
                if (conn.responseCode == 200) {
                    val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val arr = JSONArray(text)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val id = "osm_nom_m_${obj.optLong("place_id", i.toLong())}"
                        if (seenIds.contains(id)) continue

                        val lat = obj.getDouble("lat")
                        val lon = obj.getDouble("lon")
                        val rawName = obj.optString("name", obj.optString("display_name").split(",").firstOrNull() ?: "Mosque")
                        val disp = obj.optString("display_name", "")
                        val addr = disp.split(",").drop(1).take(2).joinToString(", ").trim()

                        val dist = calculateDistance(userLat, userLon, lat, lon)
                        val bearing = calculateBearing(userLat, userLon, lat, lon)

                        val mosque = Mosque(
                            id = id,
                            name = formatMosqueName(rawName),
                            nameEn = rawName,
                            latitude = lat,
                            longitude = lon,
                            address = if (addr.isNotEmpty()) addr else "নিকটবর্তী এলাকা",
                            distanceMeters = dist,
                            bearing = bearing,
                            isJumaMosque = true,
                            hasAblution = true,
                            hasAc = true,
                            hasFemalePrayerSpace = false,
                            isFavorite = favoriteIds.contains(id),
                            isCustomAdded = false,
                            jamatTimes = getDefaultJamatTimes()
                        )
                        resultList.add(mosque)
                        seenIds.add(id)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 4. Global Iconic Mosques Registry & Dynamic fallback
        // If still empty or as supplementary data, calculate from famous international landmarks
        val globalLandmarks = getGlobalIconicMosques(userLat, userLon, favoriteIds)
        for (lm in globalLandmarks) {
            // Include landmark if within expanded radius or if list is empty
            if (!seenIds.contains(lm.id) && (resultList.isEmpty() || lm.distanceMeters <= radiusMeters * 3)) {
                resultList.add(lm)
                seenIds.add(lm.id)
            }
        }

        // Sort dynamically by distance ascending
        resultList.sortedBy { it.distanceMeters }
    }

    private fun formatMosqueName(raw: String): String {
        var clean = raw.trim()
        if (clean.isEmpty() || clean.equals("mosque", ignoreCase = true) || clean.equals("masjid", ignoreCase = true)) {
            return "জামে মসজিদ"
        }
        if (!clean.contains("মসজিদ") && !clean.contains("Masjid") && !clean.contains("Mosque") && !clean.contains("مسجد")) {
            clean = "$clean জামে মসজিদ"
        }
        return clean
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

    /**
     * International landmarks across Saudi Arabia, Turkey, UAE, UK, USA, Egypt, Malaysia, India, Pakistan, Bangladesh etc.
     */
    private fun getGlobalIconicMosques(userLat: Double, userLon: Double, favoriteIds: Set<String>): List<Mosque> {
        val baseList = listOf(
            // Saudi Arabia
            Triple("মসজিদুল হারাম (কাবা শরিফ)", "Masjid al-Haram (Makkah)", Pair(21.4225, 39.8262)),
            Triple("মসজিদে নববী", "Al-Masjid an-Nabawi (Madinah)", Pair(24.4672, 39.6111)),
            Triple("মসজিদে কুবা", "Masjid Quba (Madinah)", Pair(24.4394, 39.6172)),
            Triple("মসজিদে কিবলাতাইন", "Masjid al-Qiblatayn (Madinah)", Pair(24.4842, 39.5786)),
            Triple("আল রাজি গ্র্যান্ড মসজিদ", "Al Rajhi Grand Mosque (Riyadh)", Pair(24.6934, 46.7766)),

            // Palestine / Jerusalem
            Triple("মসজিদুল আকসা (বায়তুল মুকাদ্দাস)", "Al-Aqsa Mosque (Jerusalem)", Pair(31.7761, 35.2358)),
            Triple("কুব্বাত আস-সাখরা (ডোম অব দ্য রক)", "Dome of the Rock (Jerusalem)", Pair(31.7780, 35.2354)),

            // UAE & Middle East
            Triple("শেখ জায়েদ গ্র্যান্ড মসজিদ", "Sheikh Zayed Grand Mosque (Abu Dhabi)", Pair(24.4128, 54.4749)),
            Triple("জুমিরাহ গ্র্যান্ড মসজিদ", "Jumeirah Grand Mosque (Dubai)", Pair(25.2337, 55.2655)),
            Triple("সুলতান কাবুস গ্র্যান্ড মসজিদ", "Sultan Qaboos Grand Mosque (Muscat, Oman)", Pair(23.5837, 58.3888)),
            Triple("আল-আজহার মসজিদ", "Al-Azhar Mosque (Cairo, Egypt)", Pair(30.0457, 31.2627)),

            // Turkey & Europe
            Triple("হায়া সোফিয়া গ্র্যান্ড মসজিদ", "Hagia Sophia Grand Mosque (Istanbul, Turkey)", Pair(41.0086, 28.9802)),
            Triple("সুলতান আহমেদ মসজিদ (ব্লু মস্ক)", "Blue Mosque / Sultanahmet (Istanbul, Turkey)", Pair(41.0054, 28.9768)),
            Triple("সুলেমানিয়ে জামে মসজিদ", "Suleymaniye Mosque (Istanbul, Turkey)", Pair(41.0161, 28.9640)),
            Triple("লন্ডন সেন্ট্রাল মস্ক (রিজেন্টস পার্ক)", "London Central Mosque (Regent's Park, UK)", Pair(51.5286, -0.1662)),
            Triple("ইস্ট লন্ডন মসজিদ ও লন্ডন মুসলিম সেন্টার", "East London Mosque (London, UK)", Pair(51.5186, -0.0656)),
            Triple("প্যারিস গ্র্যান্ড মসজিদ", "Grand Mosque of Paris (France)", Pair(48.8419, 2.3556)),
            Triple("রোম গ্র্যান্ড মসজিদ", "Mosque of Rome (Italy)", Pair(41.9353, 12.4952)),

            // Americas & Australia
            Triple("ইসলামিক কালচারাল সেন্টার অব নিউ ইয়র্ক", "Islamic Cultural Center of New York (USA)", Pair(40.7851, -73.9525)),
            Triple("ডার আল-হিজরাহ ইসলামিক সেন্টার", "Dar Al-Hijrah Islamic Center (Virginia, USA)", Pair(38.8631, -77.1477)),
            Triple("টরন্টো ইসলামিক ফাউন্ডেশন", "Islamic Foundation of Toronto (Canada)", Pair(43.7915, -79.2292)),
            Triple("অবার্ন গ্যালিপলি মসজিদ", "Auburn Gallipoli Mosque (Sydney, Australia)", Pair(-33.8569, 151.0326)),

            // Asia & South East Asia
            Triple("ইস্তিকলাল জাতীয় মসজিদ", "Istiqlal Grand Mosque (Jakarta, Indonesia)", Pair(-6.1702, 106.8314)),
            Triple("পুত্রা মসজিদ", "Putra Mosque (Putrajaya, Malaysia)", Pair(2.9361, 101.6894)),
            Triple("সুলতান মসজিদ", "Sultan Mosque (Singapore)", Pair(1.3023, 103.8590)),
            Triple("টোকিও কামি ও ইসলামিক সেন্টার", "Tokyo Camii Mosque (Tokyo, Japan)", Pair(35.6682, 139.6769)),
            Triple("ফয়সাল মসজিদ", "Faisal Mosque (Islamabad, Pakistan)", Pair(33.7297, 73.0372)),
            Triple("বাদশাহী মসজিদ", "Badshahi Mosque (Lahore, Pakistan)", Pair(31.5881, 74.3106)),
            Triple("দিল্লি জামা মসজিদ", "Jama Masjid (Delhi, India)", Pair(28.6507, 77.2334)),
            Triple("নাখোদা জামে মসজিদ", "Nakhoda Mosque (Kolkata, India)", Pair(22.5852, 88.3582)),

            // Bangladesh
            Triple("বাইতুল মোকাররম জাতীয় মসজিদ", "Baitul Mukarram National Mosque (Dhaka)", Pair(23.7289, 90.4124)),
            Triple("কাকরাইল মারকাজ মসজিদ", "Kakrail Markaz Mosque (Dhaka)", Pair(23.7388, 90.4074)),
            Triple("তারা মসজিদ (সিতারা মসজিদ)", "Tara Masjid / Star Mosque (Dhaka)", Pair(23.7153, 90.4018)),
            Triple("লালবাগ কেল্লা শাহী মসজিদ", "Lalbagh Fort Shahi Mosque (Dhaka)", Pair(23.7188, 90.3881)),
            Triple("সোবহানবাগ জামে মসজিদ", "Sobhanbagh Jame Masjid (Dhaka)", Pair(23.7538, 90.3773)),
            Triple("গুলশান সেন্ট্রাল জামে মসজিদ (আজাদ মসজিদ)", "Gulshan Central Mosque (Dhaka)", Pair(23.7808, 90.4172)),
            Triple("ধানমন্ডি তাকওয়া জামে মসজিদ", "Taqwa Jame Masjid Dhanmondi (Dhaka)", Pair(23.7465, 90.3725)),
            Triple("আন্দরকিল্লা শাহী জামে মসজিদ", "Anderkilla Shahi Jame Mosque (Chattogram)", Pair(22.3392, 91.8370)),
            Triple("হযরত শাহজালাল (রহ.) দরগাহ মসজিদ", "Hazrat Shahjalal Dargah Mosque (Sylhet)", Pair(24.9015, 91.8687)),
            Triple("ষাট গম্বুজ মসজিদ", "Sixty Dome Mosque (Bagerhat)", Pair(22.6747, 89.7417))
        )

        return baseList.mapIndexed { index, item ->
            val id = "global_landmark_$index"
            val dist = calculateDistance(userLat, userLon, item.third.first, item.third.second)
            val bearing = calculateBearing(userLat, userLon, item.third.first, item.third.second)
            Mosque(
                id = id,
                name = item.first,
                nameEn = item.second,
                latitude = item.third.first,
                longitude = item.third.second,
                address = item.second.substringAfter("(").substringBefore(")").ifEmpty { "প্রসিদ্ধ ঐতিহাসিক মসজিদ" },
                distanceMeters = dist,
                bearing = bearing,
                isJumaMosque = true,
                hasAblution = true,
                hasAc = true,
                hasFemalePrayerSpace = true,
                isFavorite = favoriteIds.contains(id),
                isCustomAdded = false,
                contactPhone = "",
                jamatTimes = getDefaultJamatTimes(),
                notes = "আন্তর্জাতিক ও ঐতিহাসিক প্রসিদ্ধ জামে মসজিদ"
            )
        }
    }
}
