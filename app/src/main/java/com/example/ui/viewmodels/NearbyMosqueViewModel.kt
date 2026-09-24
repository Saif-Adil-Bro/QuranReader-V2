package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Mosque
import com.example.data.model.MosqueFilterTab
import com.example.data.model.PlaceSearchResult
import com.example.data.model.QuickCity
import com.example.data.model.UserLocation
import com.example.data.repository.NearbyMosqueRepository
import com.example.utils.DeviceLocationProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NearbyMosqueViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NearbyMosqueRepository(application.applicationContext)

    private val _userLocation = MutableStateFlow(
        UserLocation(
            latitude = 23.8103, // Initial placeholder before GPS/IP resolves
            longitude = 90.4125,
            address = "অবস্থান সনাক্ত করা হচ্ছে...",
            isDefault = true
        )
    )
    val userLocation: StateFlow<UserLocation> = _userLocation.asStateFlow()

    private val _isDetectingLocation = MutableStateFlow(false)
    val isDetectingLocation: StateFlow<Boolean> = _isDetectingLocation.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _selectedRadius = MutableStateFlow(3000) // 3 km default
    val selectedRadius: StateFlow<Int> = _selectedRadius.asStateFlow()

    private val _activeTab = MutableStateFlow(MosqueFilterTab.ALL)
    val activeTab: StateFlow<MosqueFilterTab> = _activeTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _allMosques = MutableStateFlow<List<Mosque>>(emptyList())
    val allMosques: StateFlow<List<Mosque>> = _allMosques.asStateFlow()

    private val _globalSearchResults = MutableStateFlow<List<PlaceSearchResult>>(emptyList())
    val globalSearchResults: StateFlow<List<PlaceSearchResult>> = _globalSearchResults.asStateFlow()

    private val _isSearchingGlobal = MutableStateFlow(false)
    val isSearchingGlobal: StateFlow<Boolean> = _isSearchingGlobal.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var searchJob: Job? = null

    val quickCities = listOf(
        QuickCity("মক্কা মুকাররমা", "Makkah, Saudi Arabia", "🕋", 21.4225, 39.8262),
        QuickCity("মদিনা মুনাওয়ারা", "Madinah, Saudi Arabia", "🕌", 24.4672, 39.6111),
        QuickCity("বায়তুল মুকাদ্দাস", "Jerusalem, Palestine", "🕌", 31.7761, 35.2358),
        QuickCity("ক্যালিফোর্নিয়া", "California, USA", "🇺🇸", 37.3861, -122.0839),
        QuickCity("নিউ ইয়র্ক", "New York, USA", "🇺🇸", 40.7128, -74.0060),
        QuickCity("লন্ডন", "London, UK", "🇬🇧", 51.5074, -0.1278),
        QuickCity("ইস্তাম্বুল", "Istanbul, Turkey", "🇹🇷", 41.0082, 28.9784),
        QuickCity("দুবাই", "Dubai, UAE", "🇦🇪", 25.2048, 55.2708),
        QuickCity("রিয়াদ", "Riyadh, Saudi Arabia", "🇸🇦", 24.7136, 46.6753),
        QuickCity("কুয়ালালামপুর", "Kuala Lumpur, Malaysia", "🇲🇾", 3.1390, 101.6869),
        QuickCity("কায়রো", "Cairo, Egypt", "🇪🇬", 30.0444, 31.2357),
        QuickCity("টোকিও", "Tokyo, Japan", "🇯🇵", 35.6762, 139.6503),
        QuickCity("দিল্লি", "Delhi, India", "🇮🇳", 28.6139, 77.2090),
        QuickCity("ঢাকা", "Dhaka, Bangladesh", "🇧🇩", 23.8103, 90.4125),
        QuickCity("চট্টগ্রাম", "Chattogram, Bangladesh", "🇧🇩", 22.3569, 91.7832),
        QuickCity("সিলেট", "Sylhet, Bangladesh", "🇧🇩", 24.8949, 91.8687)
    )

    init {
        detectCurrentLocationAndFetch()
    }

    fun detectCurrentLocationAndFetch() {
        viewModelScope.launch {
            _isDetectingLocation.value = true
            val context = getApplication<Application>().applicationContext
            val locResult = DeviceLocationProvider.resolveBestLocation(context)
            _userLocation.value = UserLocation(
                latitude = locResult.latitude,
                longitude = locResult.longitude,
                address = locResult.address,
                isDefault = !locResult.isRealTime
            )
            _isDetectingLocation.value = false
            fetchMosques(showLoader = true)
        }
    }

    fun updateLocation(lat: Double, lon: Double, address: String = "") {
        _userLocation.value = UserLocation(
            latitude = lat,
            longitude = lon,
            address = address.ifEmpty { "শনাক্তকৃত অবস্থান" },
            isDefault = false
        )
        viewModelScope.launch {
            if (address.isBlank() || address.startsWith("কাস্টম")) {
                val resolved = repository.reverseGeocode(lat, lon)
                _userLocation.value = _userLocation.value.copy(address = resolved)
            }
        }
        fetchMosques(showLoader = true)
    }

    fun selectQuickCity(city: QuickCity) {
        _searchQuery.value = ""
        _globalSearchResults.value = emptyList()
        updateLocation(city.latitude, city.longitude, "${city.nameBn} (${city.nameEn})")
    }

    fun selectGlobalPlace(place: PlaceSearchResult) {
        _searchQuery.value = ""
        _globalSearchResults.value = emptyList()
        updateLocation(place.latitude, place.longitude, place.displayName)
    }

    fun searchGlobalPlaces(query: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _globalSearchResults.value = emptyList()
            _isSearchingGlobal.value = false
            return
        }

        searchJob = viewModelScope.launch {
            _isSearchingGlobal.value = true
            delay(350) // Debounce typing
            try {
                val results = repository.searchPlacesGlobally(query)
                _globalSearchResults.value = results
            } catch (e: Exception) {
                _globalSearchResults.value = emptyList()
            } finally {
                _isSearchingGlobal.value = false
            }
        }
    }

    fun clearGlobalSearchResults() {
        _globalSearchResults.value = emptyList()
        _isSearchingGlobal.value = false
    }

    fun fetchMosques(showLoader: Boolean = true) {
        viewModelScope.launch {
            if (showLoader) _isLoading.value = true else _isRefreshing.value = true
            _errorMessage.value = null

            try {
                val currentLoc = _userLocation.value
                val list = repository.fetchNearbyMosques(
                    userLat = currentLoc.latitude,
                    userLon = currentLoc.longitude,
                    radiusMeters = _selectedRadius.value
                )
                _allMosques.value = list
            } catch (e: Exception) {
                _errorMessage.value = "মসজিদের তালিকা লোড করতে সমস্যা হয়েছে: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
                _isRefreshing.value = false
            }
        }
    }

    fun setRadius(meters: Int) {
        _selectedRadius.value = meters
        fetchMosques(showLoader = true)
    }

    fun setActiveTab(tab: MosqueFilterTab) {
        _activeTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isNotBlank() && query.trim().length >= 2) {
            searchGlobalPlaces(query)
        } else {
            clearGlobalSearchResults()
        }
    }

    fun toggleFavorite(mosque: Mosque) {
        val newFav = repository.toggleFavorite(mosque.id)
        _allMosques.value = _allMosques.value.map {
            if (it.id == mosque.id) it.copy(isFavorite = newFav) else it
        }
    }

    fun addCustomMosque(mosque: Mosque) {
        repository.saveCustomMosque(mosque)
        fetchMosques(showLoader = false)
    }

    fun deleteCustomMosque(mosqueId: String) {
        repository.deleteCustomMosque(mosqueId)
        _allMosques.value = _allMosques.value.filter { it.id != mosqueId }
    }

    fun getFilteredMosques(): List<Mosque> {
        val query = _searchQuery.value.trim().lowercase()
        val currentTab = _activeTab.value
        val list = _allMosques.value

        var filtered = when (currentTab) {
            MosqueFilterTab.ALL -> list
            MosqueFilterTab.NEARBY -> list.sortedBy { it.distanceMeters }
            MosqueFilterTab.FAVORITES -> list.filter { it.isFavorite }
            MosqueFilterTab.CUSTOM -> list.filter { it.isCustomAdded }
        }

        if (query.isNotEmpty() && _globalSearchResults.value.isEmpty()) {
            filtered = filtered.filter {
                it.name.lowercase().contains(query) ||
                it.nameEn.lowercase().contains(query) ||
                it.address.lowercase().contains(query)
            }
        }

        return filtered
    }
}
