package com.example.ui.screens.mosque

import android.Manifest
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Mosque
import com.example.data.model.MosqueFilterTab
import com.example.data.model.PlaceSearchResult
import com.example.data.model.QuickCity
import com.example.data.model.UserLocation
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.NearbyMosqueViewModel
import com.example.utils.DateUtil
import java.util.UUID

enum class MosqueDisplayMode {
    SPLIT, // Half Map + Half List
    MAP_ONLY, // Full Map with markers
    LIST_ONLY // Traditional list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyMosqueScreen(
    onBackClick: () -> Unit,
    viewModel: NearbyMosqueViewModel = viewModel()
) {
    val context = LocalContext.current
    val userLocation by viewModel.userLocation.collectAsState()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedRadius by viewModel.selectedRadius.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allMosques by viewModel.allMosques.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val globalSearchResults by viewModel.globalSearchResults.collectAsState()
    val isSearchingGlobal by viewModel.isSearchingGlobal.collectAsState()

    var displayMode by remember { mutableStateOf(MosqueDisplayMode.SPLIT) }
    var selectedMosqueOnMap by remember { mutableStateOf<Mosque?>(null) }
    var showSearchField by remember { mutableStateOf(false) }
    var showAddMosqueDialog by remember { mutableStateOf(false) }
    var showLocationSwitcherDialog by remember { mutableStateOf(false) }
    var selectedMosqueForDetail by remember { mutableStateOf<Mosque?>(null) }

    // Compass Sensor listener for device azimuth
    var deviceAzimuth by remember { mutableFloatStateOf(0f) }
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrix = FloatArray(9)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    val azimuthInDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    deviceAzimuth = (azimuthInDeg + 360) % 360
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            if (rotationSensor != null) {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    // Permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "লাইভ জিপিএস চালু হয়েছে। বর্তমান অবস্থানের মসজিদ খোঁজা হচ্ছে...", Toast.LENGTH_SHORT).show()
            viewModel.detectCurrentLocationAndFetch()
        } else {
            Toast.makeText(context, "জিপিএস পারমিশন দেওয়া হয়নি। নেটওয়ার্ক লোকেশন ব্যবহার করা হচ্ছে।", Toast.LENGTH_SHORT).show()
            viewModel.detectCurrentLocationAndFetch()
        }
    }

    LaunchedEffect(Unit) {
        if (!com.example.utils.DeviceLocationProvider.hasLocationPermission(context)) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            viewModel.detectCurrentLocationAndFetch()
        }
    }

    val isDark = isSystemInDarkTheme()
    val filteredMosques = remember(allMosques, activeTab, searchQuery, globalSearchResults) {
        viewModel.getFilteredMosques()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "নিকটবর্তী মসজিদ ও ম্যাপ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = userLocation.address.ifEmpty { "শনাক্তকৃত অবস্থান" },
                            fontSize = 11.5.sp,
                            color = if (userLocation.isDefault) Color(0xFFEAB308) else PrimaryGreen,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "পিছনে যান",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // Toggle Display Mode (Split / Full Map / List)
                    IconButton(
                        onClick = {
                            displayMode = when (displayMode) {
                                MosqueDisplayMode.SPLIT -> MosqueDisplayMode.MAP_ONLY
                                MosqueDisplayMode.MAP_ONLY -> MosqueDisplayMode.LIST_ONLY
                                MosqueDisplayMode.LIST_ONLY -> MosqueDisplayMode.SPLIT
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (displayMode) {
                                MosqueDisplayMode.SPLIT -> Icons.Default.Layers
                                MosqueDisplayMode.MAP_ONLY -> Icons.Default.Map
                                MosqueDisplayMode.LIST_ONLY -> Icons.Default.FormatListBulleted
                            },
                            contentDescription = "ভিউ মোড পরিবর্তন",
                            tint = PrimaryGreen
                        )
                    }

                    // Global Location Switcher
                    IconButton(onClick = { showLocationSwitcherDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "বিশ্বব্যাপী শহর বা এলাকা খুঁজুন",
                            tint = PrimaryGreen
                        )
                    }

                    // Search Toggle
                    IconButton(onClick = {
                        showSearchField = !showSearchField
                        if (!showSearchField) {
                            viewModel.setSearchQuery("")
                        }
                    }) {
                        Icon(
                            imageVector = if (showSearchField) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "সার্চ",
                            tint = PrimaryGreen
                        )
                    }

                    // GPS Refresh
                    IconButton(
                        onClick = {
                            if (!com.example.utils.DeviceLocationProvider.hasLocationPermission(context)) {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            } else {
                                viewModel.detectCurrentLocationAndFetch()
                                Toast.makeText(context, "লাইভ জিপিএস অবস্থান রিফ্রেশ করা হচ্ছে...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        } else {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "বর্তমান অবস্থান রিফ্রেশ",
                                tint = PrimaryGreen
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search field & Global suggestions
            AnimatedVisibility(visible = showSearchField) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 3.dp
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("যেকোনো শহর, দেশ বা মসজিদের নাম লিখুন...", fontSize = 13.5.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen)
                            },
                            trailingIcon = {
                                if (isSearchingGlobal) {
                                    CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                } else if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        // Online global results dropdown
                        if (globalSearchResults.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1F2937) else Color(0xFFF1F5F9)
                                ),
                                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                                        Icon(Icons.Default.Public, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "অনলাইনে পাওয়া আন্তর্জাতিক স্থানসমূহ (ক্লিক করে সেখানে যান):",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryGreen
                                        )
                                    }
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = PrimaryGreen.copy(alpha = 0.2f))
                                    globalSearchResults.forEach { place ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    viewModel.selectGlobalPlace(place)
                                                    showSearchField = false
                                                    Toast.makeText(context, "${place.name} এর চারপাশের মসজিদ লোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = place.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = place.displayName,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // View Mode Switcher (Symmetrical, elegant 3-tab segmented bar)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ViewModeSegmentButton(
                        title = "স্প্লিট ভিউ",
                        icon = Icons.Default.Layers,
                        isSelected = displayMode == MosqueDisplayMode.SPLIT,
                        onClick = { displayMode = MosqueDisplayMode.SPLIT },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                    ViewModeSegmentButton(
                        title = "ম্যাপ ভিউ",
                        icon = Icons.Default.Map,
                        isSelected = displayMode == MosqueDisplayMode.MAP_ONLY,
                        onClick = { displayMode = MosqueDisplayMode.MAP_ONLY },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                    ViewModeSegmentButton(
                        title = "লিস্ট ভিউ",
                        icon = Icons.Default.FormatListBulleted,
                        isSelected = displayMode == MosqueDisplayMode.LIST_ONLY,
                        onClick = { displayMode = MosqueDisplayMode.LIST_ONLY },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                }
            }

            // Radius Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryGreen.copy(alpha = 0.12f),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("দূরত্ব:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                    }
                }
                RadiusSelectorSection(
                    selectedRadius = selectedRadius,
                    onRadiusSelected = { viewModel.setRadius(it) },
                    isDark = isDark,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tabs Selector
            TabSelectorSection(
                activeTab = activeTab,
                allCount = allMosques.size,
                favoritesCount = allMosques.count { it.isFavorite },
                customCount = allMosques.count { it.isCustomAdded },
                onTabSelected = { viewModel.setActiveTab(it) },
                isDark = isDark
            )

            // Location Info & City Switcher Banner
            LocationInfoBanner(
                userLocation = userLocation,
                mosqueCount = filteredMosques.size,
                isLoading = isLoading || isRefreshing,
                onOpenLocationSwitcher = { showLocationSwitcherDialog = true },
                onDetectLocation = {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                isDark = isDark
            )

            // Main Content Area depending on display mode
            Box(modifier = Modifier.fillMaxSize()) {
                when (displayMode) {
                    MosqueDisplayMode.SPLIT -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top half: Interactive Map with markers
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                            ) {
                                InteractiveMosqueMapView(
                                    userLocation = userLocation,
                                    mosques = filteredMosques,
                                    selectedMosque = selectedMosqueOnMap,
                                    isDark = isDark,
                                    onMosqueClick = { m ->
                                        selectedMosqueOnMap = m
                                        selectedMosqueForDetail = m
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Overlay Mosque count badge on map
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                        shape = RoundedCornerShape(10.dp),
                                        shadowElevation = 4.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("🕌", fontSize = 12.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "ম্যাপে ${DateUtil.toBengaliNumerals(filteredMosques.size)} টি মসজিদ পিন",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryGreen
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom half: List of Mosques
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.1f)
                            ) {
                                if (isLoading && allMosques.isEmpty()) {
                                    LoadingMosquesView(userLocation)
                                } else if (filteredMosques.isEmpty()) {
                                    EmptyMosqueView(
                                        activeTab = activeTab,
                                        onIncreaseRadius = { viewModel.setRadius(5000) },
                                        onOpenLocationSwitcher = { showLocationSwitcherDialog = true },
                                        onAddMosque = { showAddMosqueDialog = true }
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(filteredMosques, key = { it.id }) { mosque ->
                                            MosqueCard(
                                                mosque = mosque,
                                                deviceAzimuth = deviceAzimuth,
                                                isDark = isDark,
                                                onFavoriteToggle = { viewModel.toggleFavorite(mosque) },
                                                onCardClick = {
                                                    selectedMosqueOnMap = mosque
                                                    selectedMosqueForDetail = mosque
                                                },
                                                onNavigationClick = { openGoogleMapsNavigation(context, mosque) },
                                                onShareClick = { shareMosque(context, mosque) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MosqueDisplayMode.MAP_ONLY -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            InteractiveMosqueMapView(
                                userLocation = userLocation,
                                mosques = filteredMosques,
                                selectedMosque = selectedMosqueOnMap,
                                isDark = isDark,
                                onMosqueClick = { m ->
                                    selectedMosqueOnMap = m
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Mosque Count Badge on map
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(10.dp),
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🕌", fontSize = 12.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "ম্যাপে ${DateUtil.toBengaliNumerals(filteredMosques.size)} টি মসজিদ",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryGreen
                                    )
                                }
                            }

                            // Floating Selected Mosque Card at the bottom of the map
                            if (selectedMosqueOnMap != null) {
                                Card(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .clickable { selectedMosqueForDetail = selectedMosqueOnMap },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("🕌", fontSize = 20.sp)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = selectedMosqueOnMap!!.name,
                                                    fontSize = 14.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${formatDistance(selectedMosqueOnMap!!.distanceMeters)} • ${selectedMosqueOnMap!!.address}",
                                                    fontSize = 11.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Button(
                                            onClick = { openGoogleMapsNavigation(context, selectedMosqueOnMap!!) },
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Directions, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("রুট", fontSize = 11.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MosqueDisplayMode.LIST_ONLY -> {
                        if (isLoading && allMosques.isEmpty()) {
                            LoadingMosquesView(userLocation)
                        } else if (filteredMosques.isEmpty()) {
                            EmptyMosqueView(
                                activeTab = activeTab,
                                onIncreaseRadius = { viewModel.setRadius(5000) },
                                onOpenLocationSwitcher = { showLocationSwitcherDialog = true },
                                onAddMosque = { showAddMosqueDialog = true }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredMosques, key = { it.id }) { mosque ->
                                    MosqueCard(
                                        mosque = mosque,
                                        deviceAzimuth = deviceAzimuth,
                                        isDark = isDark,
                                        onFavoriteToggle = { viewModel.toggleFavorite(mosque) },
                                        onCardClick = {
                                            selectedMosqueOnMap = mosque
                                            selectedMosqueForDetail = mosque
                                        },
                                        onNavigationClick = { openGoogleMapsNavigation(context, mosque) },
                                        onShareClick = { shareMosque(context, mosque) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Mosque Detail Modal Dialog
    if (selectedMosqueForDetail != null) {
        MosqueDetailDialog(
            mosque = selectedMosqueForDetail!!,
            deviceAzimuth = deviceAzimuth,
            isDark = isDark,
            onDismiss = { selectedMosqueForDetail = null },
            onFavoriteToggle = {
                viewModel.toggleFavorite(selectedMosqueForDetail!!)
                selectedMosqueForDetail = selectedMosqueForDetail!!.copy(isFavorite = !selectedMosqueForDetail!!.isFavorite)
            },
            onDeleteCustom = {
                viewModel.deleteCustomMosque(selectedMosqueForDetail!!.id)
                selectedMosqueForDetail = null
            },
            onNavigationClick = { openGoogleMapsNavigation(context, selectedMosqueForDetail!!) },
            onShareClick = { shareMosque(context, selectedMosqueForDetail!!) }
        )
    }

    // Add Custom Mosque Dialog
    if (showAddMosqueDialog) {
        AddCustomMosqueDialog(
            defaultLat = userLocation.latitude,
            defaultLon = userLocation.longitude,
            onDismiss = { showAddMosqueDialog = false },
            onSave = { newMosque ->
                viewModel.addCustomMosque(newMosque)
                showAddMosqueDialog = false
                Toast.makeText(context, "নতুন মসজিদ যুক্ত করা হয়েছে!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Global Location Switcher Dialog (Explore Worldwide)
    if (showLocationSwitcherDialog) {
        GlobalLocationSwitcherDialog(
            currentLocation = userLocation,
            quickCities = viewModel.quickCities,
            globalSearchResults = globalSearchResults,
            isSearching = isSearchingGlobal,
            onSearchQuery = { viewModel.searchGlobalPlaces(it) },
            onSelectPlace = { place ->
                viewModel.selectGlobalPlace(place)
                showLocationSwitcherDialog = false
                Toast.makeText(context, "${place.name} এর আশপাশের মসজিদ খোঁজা হচ্ছে...", Toast.LENGTH_SHORT).show()
            },
            onSelectQuickCity = { city ->
                viewModel.selectQuickCity(city)
                showLocationSwitcherDialog = false
                Toast.makeText(context, "${city.nameBn} এর আশপাশের মসজিদ লোড হচ্ছে...", Toast.LENGTH_SHORT).show()
            },
            onResetToLiveGps = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
                showLocationSwitcherDialog = false
            },
            onDismiss = {
                showLocationSwitcherDialog = false
                viewModel.clearGlobalSearchResults()
            }
        )
    }
}

@Composable
fun LoadingMosquesView(userLocation: UserLocation) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "বিশ্বব্যাপী রিয়েল-টাইম মসজিদ খোঁজা হচ্ছে...",
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = userLocation.address,
                fontSize = 12.sp,
                color = PrimaryGreen,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ViewModeSegmentButton(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PrimaryGreen else Color.Transparent,
        modifier = modifier.height(36.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 1
            )
        }
    }
}

@Composable
fun RadiusSelectorSection(
    selectedRadius: Int,
    onRadiusSelected: (Int) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val radiusOptions = listOf(
        500 to "৫০০ মি.",
        1000 to "১ কিমি",
        2000 to "২ কিমি",
        3000 to "৩ কিমি",
        5000 to "৫ কিমি",
        10000 to "১০ কিমি",
        25000 to "২৫ কিমি"
    )

    LazyRow(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(radiusOptions) { (meters, label) ->
            val isSelected = selectedRadius == meters
            Surface(
                onClick = { onRadiusSelected(meters) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryGreen else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) PrimaryGreen else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TabSelectorSection(
    activeTab: MosqueFilterTab,
    allCount: Int,
    favoritesCount: Int,
    customCount: Int,
    onTabSelected: (MosqueFilterTab) -> Unit,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MosqueFilterTab.values().forEach { tab ->
            val isSelected = activeTab == tab
            val countStr = when (tab) {
                MosqueFilterTab.ALL -> " (${DateUtil.toBengaliNumerals(allCount)})"
                MosqueFilterTab.NEARBY -> ""
                MosqueFilterTab.FAVORITES -> " (${DateUtil.toBengaliNumerals(favoritesCount)})"
                MosqueFilterTab.CUSTOM -> " (${DateUtil.toBengaliNumerals(customCount)})"
            }

            Surface(
                onClick = { onTabSelected(tab) },
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else if (isDark) Color(0xFF13231B) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) PrimaryGreen else if (isDark) Color(0xFF22382C) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tab.title}$countStr",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LocationInfoBanner(
    userLocation: UserLocation,
    mosqueCount: Int,
    isLoading: Boolean,
    onOpenLocationSwitcher: () -> Unit,
    onDetectLocation: () -> Unit,
    isDark: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF132A1F) else Color(0xFFEDF8F2)
        ),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1E4633) else Color(0xFFB7E4C7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (userLocation.isDefault) Icons.Default.LocationOff else Icons.Default.Place,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (isLoading) "অনুসন্ধান চলছে..." else "আশপাশে ${DateUtil.toBengaliNumerals(mosqueCount)} টি মসজিদ পাওয়া গেছে",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "📍 ${userLocation.address.ifEmpty { "শনাক্তকৃত এলাকা" }}",
                        fontSize = 10.5.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Surface(
                onClick = onOpenLocationSwitcher,
                shape = RoundedCornerShape(8.dp),
                color = PrimaryGreen,
                modifier = Modifier.height(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("শহর বদলান", fontSize = 10.5.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun MosqueCard(
    mosque: Mosque,
    deviceAzimuth: Float,
    isDark: Boolean,
    onFavoriteToggle: () -> Unit,
    onCardClick: () -> Unit,
    onNavigationClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val relativeBearing = (mosque.bearing - deviceAzimuth + 360) % 360

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF192520) else Color(0xFFFFFFFF)
        ),
        border = BorderStroke(
            1.dp,
            if (mosque.isFavorite) Color(0xFFEAB308).copy(alpha = 0.5f)
            else if (isDark) Color(0xFF22362C)
            else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Leading compass/badge + Mosque Details + Distance & Favorite
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Leading Compass & Bearing Badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = if (isDark) Color(0xFF0F3826) else Color(0xFFE8F5E9),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .border(
                            1.dp,
                            PrimaryGreen.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "দিকনির্দেশক",
                            tint = PrimaryGreen,
                            modifier = Modifier
                                .size(17.dp)
                                .rotate(relativeBearing)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Middle Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = mosque.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (mosque.nameEn.isNotEmpty() && mosque.nameEn != mosque.name) {
                        Text(
                            text = mosque.nameEn,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = mosque.address,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right Side: Distance Pill & Favorite Button
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PrimaryGreen.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = formatDistance(mosque.distanceMeters),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }
                    }

                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = if (mosque.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "ফেভারিট",
                            tint = if (mosque.isFavorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Facilities & Live Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (mosque.rating > 0.0) {
                    FacilityTag("⭐ ${mosque.rating}", isDark, isHighlight = true)
                }
                if (mosque.isOpenNow == true) {
                    FacilityTag("🟢 খোলা", isDark)
                } else if (mosque.isOpenNow == false) {
                    FacilityTag("🔴 বন্ধ", isDark)
                }
                if (mosque.isJumaMosque) {
                    FacilityTag("🕌 জুমা", isDark)
                }
                if (mosque.hasAblution) {
                    FacilityTag("💧 ওযু", isDark)
                }
                if (mosque.hasAc) {
                    FacilityTag("❄️ AC", isDark)
                }
                if (mosque.hasFemalePrayerSpace) {
                    FacilityTag("🧕 মহিলা কর্নার", isDark)
                }
                if (mosque.isCustomAdded) {
                    FacilityTag("➕ কাস্টম", isDark, isHighlight = true)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (mosque.distanceMeters < 5000) Icons.Default.DirectionsWalk else Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = calculateWalkTime(mosque.distanceMeters),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onShareClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "শেয়ার",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Button(
                        onClick = onNavigationClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Directions,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ম্যাপে রুট",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FacilityTag(text: String, isDark: Boolean, isHighlight: Boolean = false) {
    Surface(
        color = if (isHighlight) Color(0xFFF59E0B).copy(alpha = 0.15f)
                else if (isDark) Color(0xFF1E293B)
                else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            0.5.dp,
            if (isHighlight) Color(0xFFF59E0B)
            else if (isDark) Color(0xFF334155)
            else Color(0xFFCBD5E1)
        )
    ) {
        Text(
            text = text,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isHighlight) Color(0xFFD97706) else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun EmptyMosqueView(
    activeTab: MosqueFilterTab,
    onIncreaseRadius: () -> Unit,
    onOpenLocationSwitcher: () -> Unit,
    onAddMosque: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(PrimaryGreen.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val msg = when (activeTab) {
                MosqueFilterTab.FAVORITES -> "কোনো প্রিয় মসজিদ সংরক্ষণ করা হয়নি।"
                MosqueFilterTab.CUSTOM -> "আপনি এখনো কোনো নতুন মসজিদ যুক্ত করেননি।"
                else -> "এই দূরত্বের মধ্যে কোনো মসজিদ খুঁজে পাওয়া যায়নি।"
            }

            Text(
                text = msg,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "অনুসন্ধানের পরিধি বাড়াতে পারেন অথবা বিশ্বব্যাপী অন্য যেকোনো শহর নির্বাচন করতে পারেন।",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onIncreaseRadius,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("রেডিয়াস ৫ কিমি করুন", fontSize = 12.sp)
                }

                Button(
                    onClick = onOpenLocationSwitcher,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("শহর পরিবর্তন", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun GlobalLocationSwitcherDialog(
    currentLocation: UserLocation,
    quickCities: List<QuickCity>,
    globalSearchResults: List<PlaceSearchResult>,
    isSearching: Boolean,
    onSearchQuery: (String) -> Unit,
    onSelectPlace: (PlaceSearchResult) -> Unit,
    onSelectQuickCity: (QuickCity) -> Unit,
    onResetToLiveGps: () -> Unit,
    onDismiss: () -> Unit
) {
    var searchInput by remember { mutableStateOf("") }
    val isDark = isSystemInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(PrimaryGreen.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Public, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "বিশ্বব্যাপী অবস্থান নির্বাচন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "যেকোনো দেশ, শহর বা ঠিকানার মসজিদ খুঁজুন",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search input
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = {
                        searchInput = it
                        onSearchQuery(it)
                    },
                    placeholder = { Text("শহর, দেশ বা এলাকার নাম (যেমন: London, Makkah, Dubai)...", fontSize = 12.5.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryGreen)
                    },
                    trailingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else if (searchInput.isNotEmpty()) {
                            IconButton(onClick = {
                                searchInput = ""
                                onSearchQuery("")
                            }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reset to Live GPS Button
                Surface(
                    onClick = onResetToLiveGps,
                    shape = RoundedCornerShape(12.dp),
                    color = PrimaryGreen.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "আমার বর্তমান লাইভ জিপিএস লোকেশন",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                            Text(
                                text = "ডিভাইসের আসল অবস্থান অনুযায়ী মসজিদ দেখাবে",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Online search results
                if (searchInput.trim().length >= 2 && globalSearchResults.isNotEmpty()) {
                    Text(
                        text = "অনলাইন অনুসন্ধানের ফলাফল:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(globalSearchResults) { place ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectPlace(place) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Place, contentDescription = null, tint = PrimaryGreen)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = place.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = place.displayName,
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = PrimaryGreen)
                                }
                            }
                        }
                    }
                } else {
                    // Quick Iconic International Cities
                    Text(
                        text = "প্রধান ও ঐতিহাসিক শহরসমূহ:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(quickCities) { city ->
                            Surface(
                                onClick = { onSelectQuickCity(city) },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(city.flag, fontSize = 18.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = city.nameBn,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = city.nameEn.split(",").firstOrNull() ?: "",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MosqueDetailDialog(
    mosque: Mosque,
    deviceAzimuth: Float,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDeleteCustom: () -> Unit,
    onNavigationClick: () -> Unit,
    onShareClick: () -> Unit
) {
    val relativeBearing = (mosque.bearing - deviceAzimuth + 360) % 360

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PrimaryGreen.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = PrimaryGreen,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(relativeBearing)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = mosque.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (mosque.nameEn.isNotEmpty()) {
                                Text(
                                    text = mosque.nameEn,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(onClick = onFavoriteToggle) {
                        Icon(
                            imageVector = if (mosque.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (mosque.isFavorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Address & Distance Bar
                Surface(
                    color = if (isDark) Color(0xFF132A1F) else Color(0xFFEDF8F2),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mosque.address,
                                fontSize = 12.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formatDistance(mosque.distanceMeters),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Jamat Times Schedule Table
                Text(
                    text = "জামাতের সময়সূচি",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                val waqts = listOf("ফজর", "যোহর", "আসর", "মাগরিব", "ইশা", "জুমা")
                Surface(
                    color = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        waqts.chunked(3).forEach { rowWaqts ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowWaqts.forEach { w ->
                                    val time = mosque.jamatTimes[w] ?: "নির্দিষ্ট নয়"
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = w, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                        Text(text = time, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Facilities List
                Text(
                    text = "সুযোগ-সুবিধা",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FacilityTag(if (mosque.hasAblution) "💧 ওযুখানা: আছে" else "💧 ওযুখানা: নেই", isDark)
                    FacilityTag(if (mosque.hasAc) "❄️ এসি: আছে" else "❄️ এসি: নেই", isDark)
                    FacilityTag(if (mosque.hasFemalePrayerSpace) "🧕 মহিলা কর্নার: আছে" else "🧕 মহিলা কর্নার: নেই", isDark)
                }

                if (mosque.notes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "📝 নোট: ${mosque.notes}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (mosque.isCustomAdded) {
                        OutlinedButton(
                            onClick = onDeleteCustom,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("মুছুন", fontSize = 13.sp)
                        }
                    }

                    OutlinedButton(
                        onClick = onShareClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("শেয়ার", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onNavigationClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("গুগল ম্যাপে যান", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomMosqueDialog(
    defaultLat: Double,
    defaultLon: Double,
    onDismiss: () -> Unit,
    onSave: (Mosque) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var fajrTime by remember { mutableStateOf("৫:১৫ AM") }
    var dhuhrTime by remember { mutableStateOf("১:৩০ PM") }
    var asrTime by remember { mutableStateOf("৫:০০ PM") }
    var maghribTime by remember { mutableStateOf("সূর্যাস্তের পর") }
    var ishaTime by remember { mutableStateOf("৮:০০ PM") }
    var jumaTime by remember { mutableStateOf("১:৩০ PM") }
    var hasAblution by remember { mutableStateOf(true) }
    var hasAc by remember { mutableStateOf(true) }
    var hasFemale by remember { mutableStateOf(false) }
    var isJuma by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "নতুন মসজিদ যোগ করুন",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("মসজিদের নাম *") },
                        placeholder = { Text("যেমন: বায়তুল আমান জামে মসজিদ") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("ঠিকানা বা এলাকা") },
                        placeholder = { Text("যেমন: ধানমন্ডি ৭/এ, ঢাকা") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Text(
                        text = "জামাতের সময়সূচি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fajrTime,
                            onValueChange = { fajrTime = it },
                            label = { Text("ফজর") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = dhuhrTime,
                            onValueChange = { dhuhrTime = it },
                            label = { Text("যোহর") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = asrTime,
                            onValueChange = { asrTime = it },
                            label = { Text("আসর") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maghribTime,
                            onValueChange = { maghribTime = it },
                            label = { Text("মাগরিব") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ishaTime,
                            onValueChange = { ishaTime = it },
                            label = { Text("ইশা") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = jumaTime,
                            onValueChange = { jumaTime = it },
                            label = { Text("জুমা") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }

                    Text(
                        text = "সুযোগ-সুবিধা",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasAblution = !hasAblution }
                    ) {
                        Checkbox(checked = hasAblution, onCheckedChange = { hasAblution = it })
                        Text("ওযুখখানার ব্যবস্থা আছে", fontSize = 13.5.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasAc = !hasAc }
                    ) {
                        Checkbox(checked = hasAc, onCheckedChange = { hasAc = it })
                        Text("শীতাতপ নিয়ন্ত্রিত (AC)", fontSize = 13.5.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hasFemale = !hasFemale }
                    ) {
                        Checkbox(checked = hasFemale, onCheckedChange = { hasFemale = it })
                        Text("মহিলাদের নামাজের জায়গা আছে", fontSize = 13.5.sp)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isJuma = !isJuma }
                    ) {
                        Checkbox(checked = isJuma, onCheckedChange = { isJuma = it })
                        Text("জুমার জামাত অনুষ্ঠিত হয়", fontSize = 13.5.sp)
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("অতিরিক্ত নোট (ঐচ্ছিক)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        val customMosque = Mosque(
                            id = "custom_" + UUID.randomUUID().toString(),
                            name = name.trim(),
                            latitude = defaultLat,
                            longitude = defaultLon,
                            address = address.trim().ifEmpty { "কাস্টম অবস্থান" },
                            isJumaMosque = isJuma,
                            hasAblution = hasAblution,
                            hasAc = hasAc,
                            hasFemalePrayerSpace = hasFemale,
                            isCustomAdded = true,
                            jamatTimes = mapOf(
                                "ফজর" to fajrTime,
                                "যোহর" to dhuhrTime,
                                "আসর" to asrTime,
                                "মাগরিব" to maghribTime,
                                "ইশা" to ishaTime,
                                "জুমা" to jumaTime
                            ),
                            notes = notes.trim()
                        )
                        onSave(customMosque)
                    },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("সংরক্ষণ করুন", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

private fun formatDistance(distanceMeters: Float): String {
    return if (distanceMeters < 1000) {
        "${DateUtil.toBengaliNumerals(distanceMeters.toInt())} মি."
    } else if (distanceMeters < 100000) {
        val km = String.format(java.util.Locale.US, "%.1f", distanceMeters / 1000f)
        "${DateUtil.toBengaliNumerals(km)} কিমি"
    } else {
        val kmInt = (distanceMeters / 1000f).toInt()
        val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(kmInt)
        "${DateUtil.toBengaliNumerals(formatted)} কিমি"
    }
}

private fun calculateWalkTime(distanceMeters: Float): String {
    return if (distanceMeters < 3000) {
        val minutes = (distanceMeters / 80f).toInt().coerceAtLeast(1)
        "🚶 ${DateUtil.toBengaliNumerals(minutes)} মিনিট হাঁটা"
    } else if (distanceMeters < 50000) {
        val driveMinutes = (distanceMeters / 500f).toInt().coerceAtLeast(1)
        if (driveMinutes < 60) {
            "🚗 ${DateUtil.toBengaliNumerals(driveMinutes)} মিনিট ড্রাইভ"
        } else {
            val hours = driveMinutes / 60
            val remMin = driveMinutes % 60
            "🚗 ${DateUtil.toBengaliNumerals(hours)} ঘণ্টা ${DateUtil.toBengaliNumerals(remMin)} মিনিট ড্রাইভ"
        }
    } else {
        "✈️ আন্তর্জাতিক / দূরবর্তী"
    }
}

private fun openGoogleMapsNavigation(context: Context, mosque: Mosque) {
    try {
        val uri = Uri.parse("google.navigation:q=${mosque.latitude},${mosque.longitude}&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${mosque.latitude},${mosque.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    } catch (e: Exception) {
        try {
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${mosque.latitude},${mosque.longitude}")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        } catch (ex: Exception) {
            Toast.makeText(context, "ম্যাপ ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }
}

private fun shareMosque(context: Context, mosque: Mosque) {
    val mapsUrl = "https://www.google.com/maps/search/?api=1&query=${mosque.latitude},${mosque.longitude}"
    val shareText = """
        🕌 ${mosque.name}
        📍 ঠিকানা: ${mosque.address}
        🗺️ গুগল ম্যাপ লিংক: $mapsUrl
        
        📱 ❝কুরআন রিডার❞ অ্যাপ থেকে শেয়ার করা হয়েছে।
    """.trimIndent()

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    context.startActivity(Intent.createChooser(intent, "মসজিদের তথ্য শেয়ার করুন"))
}
