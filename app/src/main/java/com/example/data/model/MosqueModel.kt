package com.example.data.model

data class Mosque(
    val id: String,
    val name: String,
    val nameEn: String = "",
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val distanceMeters: Float = 0f,
    val bearing: Float = 0f, // Compass direction in degrees (0 - 360)
    val isJumaMosque: Boolean = true,
    val hasAblution: Boolean = true,
    val hasAc: Boolean = true,
    val hasFemalePrayerSpace: Boolean = false,
    val isFavorite: Boolean = false,
    val isCustomAdded: Boolean = false,
    val contactPhone: String = "",
    val jamatTimes: Map<String, String> = emptyMap(),
    val notes: String = "",
    val addedTimestamp: Long = System.currentTimeMillis(),
    val rating: Double = 0.0,
    val userRatingsTotal: Int = 0,
    val isOpenNow: Boolean? = null,
    val photoUrl: String = "",
    val placeId: String = "",
    val source: String = "osm" // "google_places", "custom", "osm"
)

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val isDefault: Boolean = false,
    val cityName: String = "",
    val countryName: String = ""
)

data class PlaceSearchResult(
    val placeId: String,
    val name: String,
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val type: String = "city",
    val country: String = ""
)

data class QuickCity(
    val nameBn: String,
    val nameEn: String,
    val flag: String,
    val latitude: Double,
    val longitude: Double
)

enum class MosqueFilterTab(val title: String) {
    ALL("সকল মসজিদ"),
    NEARBY("সবচেয়ে কাছে"),
    FAVORITES("পছন্দের ★"),
    CUSTOM("আমার যুক্ত করা ➕")
}
