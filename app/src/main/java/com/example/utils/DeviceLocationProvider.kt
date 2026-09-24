package com.example.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume

data class DeviceLocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val source: String, // "GPS", "FUSED", "NETWORK", "IP_GEO", "DEFAULT"
    val isRealTime: Boolean
)

object DeviceLocationProvider {

    private const val DEFAULT_LAT = 23.8103
    private const val DEFAULT_LON = 90.4125
    private const val DEFAULT_ADDRESS = "ঢাকা, বাংলাদেশ"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Resolves the real-time device location using multi-tiered fallback:
     * 1. Google FusedLocationProviderClient (High Accuracy)
     * 2. Android Native LocationManager (GPS + Network)
     * 3. Network IP Geolocation (Instantaneous worldwide location detection for WiFi / Emulators / US / UK etc.)
     * 4. Safe Default
     */
    suspend fun resolveBestLocation(context: Context): DeviceLocationResult = withContext(Dispatchers.IO) {
        val hasPermission = hasLocationPermission(context)

        // Tier 1: FusedLocationProviderClient (if permission granted)
        if (hasPermission) {
            val fusedLocation = getFusedLocation(context)
            if (fusedLocation != null) {
                val addr = reverseGeocode(context, fusedLocation.latitude, fusedLocation.longitude)
                return@withContext DeviceLocationResult(
                    latitude = fusedLocation.latitude,
                    longitude = fusedLocation.longitude,
                    address = addr,
                    source = "FUSED_GPS",
                    isRealTime = true
                )
            }

            // Tier 2: LocationManager (GPS / Network provider)
            val managerLocation = getLocationFromManager(context)
            if (managerLocation != null) {
                val addr = reverseGeocode(context, managerLocation.latitude, managerLocation.longitude)
                return@withContext DeviceLocationResult(
                    latitude = managerLocation.latitude,
                    longitude = managerLocation.longitude,
                    address = addr,
                    source = "NATIVE_GPS",
                    isRealTime = true
                )
            }
        }

        // Tier 3: IP-Based Geolocation (Crucial for emulators, devices with GPS off, or US/California real-time detection)
        val ipLocation = getIpGeolocation()
        if (ipLocation != null) {
            return@withContext ipLocation
        }

        // Tier 4: Fallback
        DeviceLocationResult(
            latitude = DEFAULT_LAT,
            longitude = DEFAULT_LON,
            address = DEFAULT_ADDRESS,
            source = "DEFAULT",
            isRealTime = false
        )
    }

    @SuppressLint("MissingPermission")
    private suspend fun getFusedLocation(context: Context): Location? = withTimeoutOrNull(5000) {
        suspendCancellableCoroutine { continuation ->
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val cts = CancellationTokenSource()

                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            if (continuation.isActive) continuation.resume(loc)
                        } else {
                            // Try lastLocation
                            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                                if (continuation.isActive) continuation.resume(lastLoc)
                            }.addOnFailureListener {
                                if (continuation.isActive) continuation.resume(null)
                            }
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) continuation.resume(null)
                    }

                continuation.invokeOnCancellation {
                    cts.cancel()
                }
            } catch (e: Exception) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLocationFromManager(context: Context): Location? = withTimeoutOrNull(4000) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withTimeoutOrNull null

        // First check last known locations
        var bestLoc: Location? = null
        try {
            val providers = locationManager.getProviders(true)
            for (p in providers) {
                val l = locationManager.getLastKnownLocation(p) ?: continue
                if (bestLoc == null || l.accuracy < bestLoc.accuracy) {
                    bestLoc = l
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (bestLoc != null) return@withTimeoutOrNull bestLoc

        // If last known is null, request a single update via listener
        suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    try {
                        locationManager.removeUpdates(this)
                    } catch (e: Exception) {}
                    if (cont.isActive) cont.resume(location)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }

            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener, Looper.getMainLooper())
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, listener, Looper.getMainLooper())
                } else {
                    if (cont.isActive) cont.resume(null)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }

            cont.invokeOnCancellation {
                try {
                    locationManager.removeUpdates(listener)
                } catch (e: Exception) {}
            }
        }
    }

    /**
     * Accurately detects city, state/region, and coordinates via IP geolocation.
     * Perfect for devices in California, UK, Saudi Arabia, etc.
     */
    private suspend fun getIpGeolocation(): DeviceLocationResult? = withContext(Dispatchers.IO) {
        // Try IP-API
        try {
            val url = URL("http://ip-api.com/json/?fields=status,country,regionName,city,lat,lon,timezone")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(text)
                if (json.optString("status") == "success") {
                    val lat = json.getDouble("lat")
                    val lon = json.getDouble("lon")
                    val city = json.optString("city", "")
                    val region = json.optString("regionName", "")
                    val country = json.optString("country", "")

                    val parts = listOf(city, region, country).filter { it.isNotBlank() }
                    val address = if (parts.isNotEmpty()) parts.distinct().joinToString(", ") else "বর্তমান এলাকা"

                    return@withContext DeviceLocationResult(
                        latitude = lat,
                        longitude = lon,
                        address = address,
                        source = "IP_GEO",
                        isRealTime = true
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore & try fallback
        }

        // Secondary IP API fallback (ipapi.co)
        try {
            val url = URL("https://ipapi.co/json/")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "QawmiManager-Location-Service/1.0")
            }
            if (conn.responseCode == 200) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(text)
                val lat = json.optDouble("latitude", Double.NaN)
                val lon = json.optDouble("longitude", Double.NaN)
                if (!lat.isNaN() && !lon.isNaN()) {
                    val city = json.optString("city", "")
                    val region = json.optString("region", "")
                    val country = json.optString("country_name", "")

                    val parts = listOf(city, region, country).filter { it.isNotBlank() }
                    val address = if (parts.isNotEmpty()) parts.distinct().joinToString(", ") else "বর্তমান এলাকা"

                    return@withContext DeviceLocationResult(
                        latitude = lat,
                        longitude = lon,
                        address = address,
                        source = "IP_GEO_SECONDARY",
                        isRealTime = true
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    /**
     * Resolves human readable address from coordinates
     */
    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
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
            // Fallback to online OSM
        }

        // Online Reverse Geocode fallback via OSM Nominatim
        try {
            val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "QawmiManager-LocationProvider/1.0")
            }
            if (conn.responseCode == 200) {
                val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val obj = JSONObject(text)
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

        "বর্তমান এলাকা"
    }
}
