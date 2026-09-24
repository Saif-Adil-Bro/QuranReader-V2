package com.example.ui.screens.mosque

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.UserLocation
import com.example.ui.theme.PrimaryGreen
import com.example.ui.viewmodels.NearbyMosqueViewModel
import com.example.utils.DateUtil

/**
 * Filter mode for mosques on the map
 */
enum class MapMosqueFilter(val title: String) {
    ALL("সকল মসজিদ"),
    OPEN_NOW("🟢 খোলা আছে"),
    TOP_RATED("⭐ ৪.০+ রেটিং"),
    JUMA("🕌 জুমা মসজিদ"),
    FAVORITES("★ পছন্দের")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MosqueMapScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NearbyMosqueViewModel = viewModel()
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    val userLocation by viewModel.userLocation.collectAsState()
    val isDetectingLocation by viewModel.isDetectingLocation.collectAsState()
    val allMosques by viewModel.allMosques.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val selectedRadius by viewModel.selectedRadius.collectAsState()
    val globalSearchResults by viewModel.globalSearchResults.collectAsState()
    val isSearchingGlobal by viewModel.isSearchingGlobal.collectAsState()

    var activeFilter by remember { mutableStateOf(MapMosqueFilter.ALL) }
    var selectedMosque by remember { mutableStateOf<Mosque?>(null) }
    var showDetailDialog by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }

    // Permission launcher for Real-Time GPS
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "লাইভ জিপিএস লোকেশন সনাক্ত করা হচ্ছে...", Toast.LENGTH_SHORT).show()
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

    // Filtered Mosques based on chips
    val filteredMosques = remember(allMosques, activeFilter) {
        when (activeFilter) {
            MapMosqueFilter.ALL -> allMosques
            MapMosqueFilter.OPEN_NOW -> allMosques.filter { it.isOpenNow == true }
            MapMosqueFilter.TOP_RATED -> allMosques.filter { it.rating >= 4.0 }
            MapMosqueFilter.JUMA -> allMosques.filter { it.isJumaMosque }
            MapMosqueFilter.FAVORITES -> allMosques.filter { it.isFavorite }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = {
                                searchInput = it
                                viewModel.searchGlobalPlaces(it)
                            },
                            placeholder = { Text("যেকোনো শহর, এলাকা বা মসজিদ খুঁজুন...", fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = Color.Transparent
                            ),
                            trailingIcon = {
                                if (searchInput.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchInput = ""
                                        viewModel.clearGlobalSearchResults()
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                    } else {
                        Column {
                            Text(
                                text = "🕌 মসজিদ ম্যাপ এক্সপ্লোরার",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF1E293B)
                            )
                            Text(
                                text = "${userLocation.address} (${DateUtil.toBengaliNumerals(filteredMosques.size)} টি মসজিদ)",
                                fontSize = 11.5.sp,
                                color = PrimaryGreen,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showSearchBar) {
                            showSearchBar = false
                            searchInput = ""
                            viewModel.clearGlobalSearchResults()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
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
                    }) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "আমার অবস্থান", tint = PrimaryGreen)
                        }
                    }
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                    IconButton(onClick = { viewModel.fetchMosques(showLoader = false) }) {
                        if (isRefreshing || isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Full Screen Interactive Free Map (MapLibre / OSM / ESRI Multi-layer)
            InteractiveMosqueMapView(
                userLocation = userLocation,
                mosques = filteredMosques,
                selectedMosque = selectedMosque,
                isDark = isDark,
                onMosqueClick = { m ->
                    selectedMosque = m
                },
                modifier = Modifier.fillMaxSize()
            )

            // Top Filter Chips Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MapMosqueFilter.values()) { filter ->
                        val isSelected = (activeFilter == filter)
                        Surface(
                            onClick = { activeFilter = filter },
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) PrimaryGreen else MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            shadowElevation = 3.dp,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryGreen else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = filter.title,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // Global Search Suggestions Dropdown Overlay
            if (showSearchBar && (globalSearchResults.isNotEmpty() || isSearchingGlobal)) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 55.dp)
                        .heightIn(max = 280.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
                ) {
                    if (isSearchingGlobal) {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = PrimaryGreen)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(globalSearchResults) { result ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectGlobalPlace(result)
                                            showSearchBar = false
                                            searchInput = ""
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(result.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text(result.displayName, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.15f))
                            }
                        }
                    }
                }
            }

            // Bottom Floating Mosque Detail Card
            if (selectedMosque != null) {
                val mosque = selectedMosque!!
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { showDetailDialog = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mosque.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = mosque.address,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { selectedMosque = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Features row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (mosque.isJumaMosque) {
                                Surface(shape = RoundedCornerShape(6.dp), color = PrimaryGreen.copy(alpha = 0.15f)) {
                                    Text("🕌 জুমা", fontSize = 10.5.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (mosque.hasAblution) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF0284C7).copy(alpha = 0.15f)) {
                                    Text("💧 ওজু", fontSize = 10.5.sp, color = Color(0xFF0284C7), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            if (mosque.hasAc) {
                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF6366F1).copy(alpha = 0.15f)) {
                                    Text("❄️ এসি", fontSize = 10.5.sp, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "দূরত্ব: ${formatDistance(mosque.distanceMeters)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreen
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    openUniversalNavigation(context, mosque)
                                },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ম্যাপে রুট দেখুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showDetailDialog = true },
                                modifier = Modifier.weight(1f).height(38.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, PrimaryGreen),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("বিস্তারিত সময়সূচী", fontSize = 12.sp, color = PrimaryGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Dialog
    if (showDetailDialog && selectedMosque != null) {
        val mosque = selectedMosque!!
        Dialog(
            onDismissRequest = { showDetailDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mosque.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showDetailDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Text(text = mosque.address, fontSize = 12.5.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "⏰ জামাত ও নামাজের সময়সূচি:", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            mosque.jamatTimes.forEach { (waqt, time) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(waqt, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text(time, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            showDetailDialog = false
                            openUniversalNavigation(context, mosque)
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("দিকনির্দেশনা ও রুট চালু করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun formatDistance(distanceMeters: Float): String {
    return if (distanceMeters < 1000) {
        "${DateUtil.toBengaliNumerals(distanceMeters.toInt())} মি."
    } else {
        val km = String.format(java.util.Locale.US, "%.1f", distanceMeters / 1000f)
        "${DateUtil.toBengaliNumerals(km)} কিমি"
    }
}

private fun openUniversalNavigation(context: Context, mosque: Mosque) {
    try {
        val uri = Uri.parse("geo:${mosque.latitude},${mosque.longitude}?q=${mosque.latitude},${mosque.longitude}(${Uri.encode(mosque.name)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val webUri = Uri.parse("https://www.openstreetmap.org/directions?engine=fossgis_osrm_car&route=%2C%3B${mosque.latitude}%2C${mosque.longitude}")
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            context.startActivity(webIntent)
        } catch (ex: Exception) {
            Toast.makeText(context, "ম্যাপ ওপেন করা যায়নি", Toast.LENGTH_SHORT).show()
        }
    }
}
