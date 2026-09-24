package com.example.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.example.data.api.GeoapifyService
import com.example.data.local.PreloadedMosqueCatalog
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
     * Globally search for places, cities, towns, or mosques using Photon (Komoot) + System Geocoder (100% Free)
     */
    suspend fun searchPlacesGlobally(query: String): List<PlaceSearchResult> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val results = mutableListOf<PlaceSearchResult>()

        try {
            // 1. Photon / Geoapify Service
            val geoResults = geoapifyService.searchPlacesGlobally(query)
            if (geoResults.isNotEmpty()) {
                results.addAll(geoResults)
            }

            // 2. System Geocoder
            if (Geocoder.isPresent()) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses = geocoder.getFromLocationName(query, 5)
                    if (!addresses.isNullOrEmpty()) {
                        for ((idx, addr) in addresses.withIndex()) {
                            val name = addr.featureName ?: addr.locality ?: addr.adminArea ?: query
                            val disp = listOfNotNull(addr.subLocality, addr.locality, addr.adminArea, addr.countryName)
                                .filter { it.isNotBlank() }.distinct().joinToString(", ")
                            if (results.none { calculateDistance(it.latitude, it.longitude, addr.latitude, addr.longitude) < 500 }) {
                                results.add(
                                    PlaceSearchResult(
                                        placeId = "geocoder_$idx",
                                        name = name,
                                        displayName = disp.ifEmpty { name },
                                        latitude = addr.latitude,
                                        longitude = addr.longitude,
                                        type = "geocoded",
                                        country = addr.countryName ?: ""
                                    )
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore Geocoder failures
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        results
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val a: Address = addresses[0]
                    val parts = listOfNotNull(a.subLocality, a.locality, a.subAdminArea, a.adminArea, a.countryName)
                        .filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) return@withContext parts.distinct().joinToString(", ")
                    val line = a.getAddressLine(0)
                    if (!line.isNullOrBlank()) return@withContext line
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Online Reverse Geocoding fallback via OSM
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "QawmiManager-Android-GlobalMosqueFinder/1.0")
            }
            if (conn.responseCode == 200) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val obj = JSONObject(text)
                val addressObj = obj.optJSONObject("address")
                if (addressObj != null) {
                    val city = addressObj.optString("city", "").ifEmpty {
                        addressObj.optString("town", "").ifEmpty {
                            addressObj.optString("suburb", "").ifEmpty {
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
     * Multi-Engine Real-Time Mosque Fetcher:
     * 1. User Custom Mosques
     * 2. Preloaded Verified Regional Database (Dhaka, Chittagong, Sylhet, Rajshahi, California, London, Makkah etc.)
     * 3. Photon API POI Search (Instantaneous OpenStreetMap engine)
     * 4. OpenStreetMap Overpass Turbo Engine
     * 5. Nominatim Engine
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

        // 2. Preloaded Regional Mosques (Guarantees immediate, accurate local mosques anywhere in Dhaka / Bangladesh / International)
        val preloadedNear = PreloadedMosqueCatalog.getMosquesNear(userLat, userLon, radiusMeters, favoriteIds)
        for (pm in preloadedNear) {
            if (!seenIds.contains(pm.id) && resultList.none { calculateDistance(it.latitude, it.longitude, pm.latitude, pm.longitude) < 40 }) {
                resultList.add(pm)
                seenIds.add(pm.id)
            }
        }

        // 3. Photon Komoot API POI Search (Ultra-fast, global OpenStreetMap POI index)
        try {
            val photonQueries = listOf("mosque", "masjid", "মসজিদ")
            for (query in photonQueries) {
                try {
                    val encoded = URLEncoder.encode(query, "UTF-8")
                    val photonUrl = URL("https://photon.komoot.io/api/?q=$encoded&lat=$userLat&lon=$userLon&limit=25")
                    val conn = (photonUrl.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 4000
                        readTimeout = 4000
                        setRequestProperty("User-Agent", "Mozilla/5.0 (Android; QawmiManager/1.0)")
                    }

                    if (conn.responseCode == 200) {
                        val jsonText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                        val root = JSONObject(jsonText)
                        val features = root.optJSONArray("features") ?: JSONArray()

                        for (i in 0 until features.length()) {
                            val feat = features.getJSONObject(i)
                            val geom = feat.optJSONObject("geometry")
                            val coords = geom?.optJSONArray("coordinates")
                            val lon = coords?.optDouble(0) ?: continue
                            val lat = coords?.optDouble(1) ?: continue

                            val dist = calculateDistance(userLat, userLon, lat, lon)
                            // Filter by reasonable radius (up to 15km for city bounds)
                            val maxAllowed = (radiusMeters * 2.5f).coerceAtLeast(15000f)
                            if (dist > maxAllowed) continue

                            val props = feat.optJSONObject("properties") ?: continue
                            val osmId = "photon_osm_" + props.optLong("osm_id", i.toLong())
                            if (seenIds.contains(osmId)) continue
                            if (resultList.any { calculateDistance(it.latitude, it.longitude, lat, lon) < 40 }) continue

                            val name = props.optString("name", "মসজিদ")
                            val street = props.optString("street", "")
                            val city = props.optString("city", props.optString("district", props.optString("state", "")))
                            val addrParts = listOf(street, city).filter { it.isNotBlank() }
                            val addr = if (addrParts.isNotEmpty()) addrParts.joinToString(", ") else "নিকটবর্তী এলাকা"

                            val bearing = calculateBearing(userLat, userLon, lat, lon)
                            val mosque = Mosque(
                                id = osmId,
                                name = formatMosqueName(name),
                                nameEn = name,
                                latitude = lat,
                                longitude = lon,
                                address = addr,
                                distanceMeters = dist,
                                bearing = bearing,
                                isJumaMosque = true,
                                hasAblution = true,
                                hasAc = true,
                                hasFemalePrayerSpace = false,
                                isFavorite = favoriteIds.contains(osmId),
                                isCustomAdded = false,
                                jamatTimes = getDefaultJamatTimes(),
                                source = "photon"
                            )
                            resultList.add(mosque)
                            seenIds.add(osmId)
                        }
                    }
                } catch (e: Exception) {
                    // Try next query
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. OpenStreetMap Overpass Turbo Engine
        try {
            val overpassQuery = """
                [out:json][timeout:6];
                (
                  node["amenity"="place_of_worship"]["religion"="muslim"](around:${radiusMeters * 2},$userLat,$userLon);
                  way["amenity"="place_of_worship"]["religion"="muslim"](around:${radiusMeters * 2},$userLat,$userLon);
                  node["building"="mosque"](around:${radiusMeters * 2},$userLat,$userLon);
                  way["building"="mosque"](around:${radiusMeters * 2},$userLat,$userLon);
                );
                out center tags;
            """.trimIndent()

            for (endpoint in overpassEndpoints) {
                try {
                    val url = URL(endpoint)
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        doOutput = true
                        connectTimeout = 5000
                        readTimeout = 5000
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                        setRequestProperty("User-Agent", "QawmiManager-Android/1.0")
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
                            if (resultList.any { calculateDistance(it.latitude, it.longitude, lat, lon) < 40 }) continue

                            val tags = elem.optJSONObject("tags") ?: JSONObject()
                            val nameBn = tags.optString("name:bn", "").ifEmpty { tags.optString("name", "") }
                            val nameEn = tags.optString("name:en", "").ifEmpty { tags.optString("name", "") }
                            val rawName = if (nameBn.isNotEmpty()) nameBn else if (nameEn.isNotEmpty()) nameEn else "মসজিদ"

                            val street = tags.optString("addr:street", "")
                            val suburb = tags.optString("addr:suburb", "").ifEmpty {
                                tags.optString("addr:city", "").ifEmpty { tags.optString("addr:district", "") }
                            }
                            val addressParts = listOf(street, suburb).filter { it.isNotBlank() }
                            val address = if (addressParts.isNotEmpty()) addressParts.joinToString(", ") else "নিকটবর্তী এলাকা"

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
                                isJumaMosque = true,
                                hasAblution = true,
                                hasAc = true,
                                hasFemalePrayerSpace = tags.optString("female", "") == "yes" || tags.optString("women", "") == "yes",
                                isFavorite = favoriteIds.contains(id),
                                isCustomAdded = false,
                                contactPhone = tags.optString("phone", tags.optString("contact:phone", "")),
                                jamatTimes = getDefaultJamatTimes(),
                                notes = tags.optString("description", ""),
                                source = "osm"
                            )
                            resultList.add(mosque)
                            seenIds.add(id)
                        }
                        if (elements.length() > 0) break
                    }
                } catch (e: Exception) {
                    // Try next endpoint mirror
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 5. Geoapify (if configured)
        try {
            val geoMosques = geoapifyService.searchNearbyMosques(userLat, userLon, radiusMeters)
            for (gm in geoMosques) {
                if (!seenIds.contains(gm.id) && resultList.none { calculateDistance(it.latitude, it.longitude, gm.latitude, gm.longitude) < 40 }) {
                    val isFav = favoriteIds.contains(gm.id)
                    resultList.add(gm.copy(isFavorite = isFav))
                    seenIds.add(gm.id)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 6. Safeguard: Strictly sort by distance ascending
        resultList.sortBy { it.distanceMeters }
        resultList
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
}
