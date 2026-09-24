package com.example.ui.screens.mosque

import android.annotation.SuppressLint
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Mosque
import com.example.data.model.UserLocation
import com.example.ui.theme.PrimaryGreen
import org.json.JSONArray
import org.json.JSONObject

enum class MosqueMapLayer(val title: String, val icon: String) {
    STREET("স্ট্রিট", "🗺️"),
    SATELLITE("স্যাটেলাইট", "🛰️"),
    DARK("ডার্ক", "🌙"),
    TOPO("টেরেন", "🏔️")
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InteractiveMosqueMapView(
    userLocation: UserLocation,
    mosques: List<Mosque>,
    selectedMosque: Mosque?,
    isDark: Boolean,
    onMosqueClick: (Mosque) -> Unit,
    modifier: Modifier = Modifier,
    initialLayer: MosqueMapLayer = if (isDark) MosqueMapLayer.DARK else MosqueMapLayer.STREET
) {
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var currentLayer by remember { mutableStateOf(initialLayer) }
    var showLayerMenu by remember { mutableStateOf(false) }

    fun getMosquesBase64(): String {
        val array = JSONArray()
        for (m in mosques) {
            val obj = JSONObject().apply {
                put("id", m.id)
                put("name", m.name)
                put("nameEn", m.nameEn)
                put("lat", m.latitude)
                put("lon", m.longitude)
                put("address", m.address)
                put("distanceMeters", m.distanceMeters)
                put("isJuma", m.isJumaMosque)
                put("hasAc", m.hasAc)
                put("hasAblution", m.hasAblution)
                put("hasFemale", m.hasFemalePrayerSpace)
                put("isFavorite", m.isFavorite)
                put("isCustom", m.isCustomAdded)
            }
            array.put(obj)
        }
        val raw = array.toString()
        return Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    LaunchedEffect(userLocation.latitude, userLocation.longitude, mosques.size, selectedMosque?.id) {
        val targetWebView = webViewRef.value ?: return@LaunchedEffect
        val b64 = getMosquesBase64()
        val selId = selectedMosque?.id ?: ""
        val js = """
            if (window.updateMapDataEncoded) {
                window.updateMapDataEncoded(${userLocation.latitude}, ${userLocation.longitude}, '$b64', '$selId');
            }
        """.trimIndent()
        targetWebView.evaluateJavascript(js, null)
    }

    LaunchedEffect(currentLayer) {
        val targetWebView = webViewRef.value ?: return@LaunchedEffect
        val layerKey = when (currentLayer) {
            MosqueMapLayer.STREET -> "street"
            MosqueMapLayer.SATELLITE -> "satellite"
            MosqueMapLayer.DARK -> "dark"
            MosqueMapLayer.TOPO -> "topo"
        }
        val js = "if (window.setMapLayer) { window.setMapLayer('$layerKey'); }"
        targetWebView.evaluateJavascript(js, null)
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        cacheMode = WebSettings.LOAD_DEFAULT
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        builtInZoomControls = false
                        displayZoomControls = false
                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile; QawmiManager/1.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                    }
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            val b64 = getMosquesBase64()
                            val selId = selectedMosque?.id ?: ""
                            val layerKey = when (currentLayer) {
                                MosqueMapLayer.STREET -> "street"
                                MosqueMapLayer.SATELLITE -> "satellite"
                                MosqueMapLayer.DARK -> "dark"
                                MosqueMapLayer.TOPO -> "topo"
                            }
                            val js = """
                                if (window.initAppMapEncoded) {
                                    window.initAppMapEncoded(${userLocation.latitude}, ${userLocation.longitude}, '$b64', '$selId', '$layerKey');
                                }
                            """.trimIndent()
                            view?.evaluateJavascript(js, null)
                        }
                    }

                    addJavascriptInterface(
                        MosqueMapJsBridge(
                            mosquesProvider = { mosques },
                            onMosqueSelected = { m ->
                                post { onMosqueClick(m) }
                            }
                        ),
                        "AndroidBridge"
                    )

                    val htmlContent = buildMapEngineHtml()
                    loadDataWithBaseURL("https://server.arcgisonline.com", htmlContent, "text/html", "UTF-8", null)
                    webViewRef.value = this
                }
            },
            update = {
                webViewRef.value = it
            }
        )

        // Floating Map Controls on Top Right
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Map Layer Selector Button
            Surface(
                onClick = { showLayerMenu = true },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "ম্যাপ লেয়ার",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = showLayerMenu,
                onDismissRequest = { showLayerMenu = false }
            ) {
                MosqueMapLayer.values().forEach { layer ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(layer.icon, fontSize = 16.sp)
                                Text(
                                    layer.title,
                                    fontWeight = if (currentLayer == layer) FontWeight.Bold else FontWeight.Normal,
                                    color = if (currentLayer == layer) PrimaryGreen else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        onClick = {
                            currentLayer = layer
                            showLayerMenu = false
                        }
                    )
                }
            }

            // Re-center to User Location
            Surface(
                onClick = {
                    val js = "if (window.centerOnUser) { window.centerOnUser(); }"
                    webViewRef.value?.evaluateJavascript(js, null)
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "আমার অবস্থান",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Zoom Controls
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.3f))
            ) {
                Column {
                    IconButton(
                        onClick = {
                            val js = "if (window.zoomIn) { window.zoomIn(); }"
                            webViewRef.value?.evaluateJavascript(js, null)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    }
                    HorizontalDivider(modifier = Modifier.width(36.dp), color = Color.Gray.copy(alpha = 0.2f))
                    IconButton(
                        onClick = {
                            val js = "if (window.zoomOut) { window.zoomOut(); }"
                            webViewRef.value?.evaluateJavascript(js, null)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

class MosqueMapJsBridge(
    private val mosquesProvider: () -> List<Mosque>,
    private val onMosqueSelected: (Mosque) -> Unit
) {
    @JavascriptInterface
    fun onMarkerClicked(mosqueId: String) {
        val list = mosquesProvider()
        val found = list.firstOrNull { it.id == mosqueId }
        if (found != null) {
            onMosqueSelected(found)
        }
    }
}

private fun buildMapEngineHtml(): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                    background: #111827;
                    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                }
                .leaflet-control-attribution {
                    display: none !important;
                }
                .dark-tile {
                    filter: brightness(0.6) invert(1) contrast(3) hue-rotate(200deg) saturate(0.3) brightness(0.7) !important;
                }
                .user-gps-marker {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                }
                .pulse-dot {
                    width: 16px;
                    height: 16px;
                    background: #2563eb;
                    border: 3px solid #ffffff;
                    border-radius: 50%;
                    box-shadow: 0 0 12px rgba(37, 99, 235, 0.9);
                    animation: pulse 2s infinite;
                }
                @keyframes pulse {
                    0% {
                        box-shadow: 0 0 0 0 rgba(37, 99, 235, 0.7);
                    }
                    70% {
                        box-shadow: 0 0 0 16px rgba(37, 99, 235, 0);
                    }
                    100% {
                        box-shadow: 0 0 0 0 rgba(37, 99, 235, 0);
                    }
                }
                .mosque-pin {
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    width: 36px;
                    height: 36px;
                    border-radius: 50% 50% 50% 0;
                    transform: rotate(-45deg);
                    box-shadow: 0 4px 10px rgba(0,0,0,0.35);
                    cursor: pointer;
                    transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
                }
                .mosque-pin:hover, .mosque-pin.active {
                    transform: rotate(-45deg) scale(1.22);
                    border: 2.5px solid #fde047 !important;
                    box-shadow: 0 6px 14px rgba(0,0,0,0.5);
                }
                .mosque-pin span {
                    transform: rotate(45deg);
                    font-size: 16px;
                    color: white;
                    user-select: none;
                }
                .mosque-label {
                    background: rgba(16, 185, 129, 0.95);
                    border: none;
                    border-radius: 6px;
                    color: white;
                    font-weight: bold;
                    font-size: 11px;
                    padding: 3px 7px;
                    box-shadow: 0 3px 7px rgba(0,0,0,0.3);
                }
                .leaflet-tooltip-top:before {
                    border-top-color: rgba(16, 185, 129, 0.95);
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([23.8103, 90.4125], 14);

                var layers = {
                    street: L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19 }),
                    satellite: L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19 }),
                    dark: L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile/{z}/{y}/{x}', { maxZoom: 19, className: 'dark-tile' }),
                    topo: L.tileLayer('https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png', { maxZoom: 17 })
                };

                var currentTileLayer = layers.street.addTo(map);

                window.setMapLayer = function(layerKey) {
                    if (layers[layerKey]) {
                        map.removeLayer(currentTileLayer);
                        currentTileLayer = layers[layerKey].addTo(map);
                    }
                };

                var userMarker = null;
                var markersGroup = L.layerGroup().addTo(map);
                var currentUserLat = 23.8103;
                var currentUserLon = 90.4125;

                function createUserIcon() {
                    return L.divIcon({
                        className: 'user-gps-marker',
                        html: '<div class="pulse-dot"></div>',
                        iconSize: [24, 24],
                        iconAnchor: [12, 12]
                    });
                }

                function createMosqueIcon(isFav, isSelected) {
                    var border = isSelected ? '#fde047' : '#ffffff';
                    var bg = isSelected ? '#047857' : (isFav ? '#059669' : '#10b981');
                    return L.divIcon({
                        className: 'custom-icon',
                        html: '<div class="mosque-pin ' + (isSelected ? 'active' : '') + '" style="background:' + bg + '; border: 2px solid ' + border + ';"><span>🕌</span></div>',
                        iconSize: [36, 36],
                        iconAnchor: [18, 36],
                        tooltipAnchor: [0, -36]
                    });
                }

                function decodeUtf8Base64(b64) {
                    try {
                        var bin = atob(b64);
                        var bytes = new Uint8Array(bin.length);
                        for (var i = 0; i < bin.length; i++) {
                            bytes[i] = bin.charCodeAt(i);
                        }
                        var decoder = new TextDecoder('utf-8');
                        return decoder.decode(bytes);
                    } catch(e) {
                        return atob(b64);
                    }
                }

                window.initAppMapEncoded = function(userLat, userLon, b64Data, selectedId, layerKey) {
                    if (layerKey && layers[layerKey]) {
                        window.setMapLayer(layerKey);
                    }
                    window.updateMapDataEncoded(userLat, userLon, b64Data, selectedId);
                };

                window.updateMapDataEncoded = function(userLat, userLon, b64Data, selectedId) {
                    var jsonStr = decodeUtf8Base64(b64Data);
                    window.updateMapData(userLat, userLon, jsonStr, selectedId);
                };

                window.updateMapData = function(userLat, userLon, mosquesJsonStr, selectedId) {
                    currentUserLat = userLat;
                    currentUserLon = userLon;

                    var mosques = [];
                    try {
                        mosques = JSON.parse(mosquesJsonStr);
                    } catch(e) {
                        mosques = [];
                    }

                    // Update User GPS Marker
                    if (!userMarker) {
                        userMarker = L.marker([userLat, userLon], { icon: createUserIcon() }).addTo(map);
                    } else {
                        userMarker.setLatLng([userLat, userLon]);
                    }

                    // Clear old mosque markers
                    markersGroup.clearLayers();

                    var selectedPoint = null;

                    mosques.forEach(function(m) {
                        var isSel = (m.id === selectedId);
                        var marker = L.marker([m.lat, m.lon], {
                            icon: createMosqueIcon(m.isFavorite, isSel)
                        });

                        marker.bindTooltip(m.name, {
                            permanent: isSel,
                            direction: 'top',
                            className: 'mosque-label'
                        });

                        marker.on('click', function() {
                            if (window.AndroidBridge && window.AndroidBridge.onMarkerClicked) {
                                window.AndroidBridge.onMarkerClicked(m.id);
                            }
                        });

                        markersGroup.addLayer(marker);

                        if (isSel) {
                            selectedPoint = [m.lat, m.lon];
                        }
                    });

                    if (selectedPoint) {
                        map.flyTo(selectedPoint, 16, { animate: true, duration: 0.8 });
                    } else if (mosques.length > 0) {
                        var bounds = L.latLngBounds([[userLat, userLon]]);
                        mosques.forEach(function(m) {
                            bounds.extend([m.lat, m.lon]);
                        });
                        map.fitBounds(bounds, { padding: [40, 40], maxZoom: 16 });
                    } else {
                        map.setView([userLat, userLon], 14);
                    }
                };

                window.centerOnUser = function() {
                    map.flyTo([currentUserLat, currentUserLon], 15, { animate: true, duration: 0.8 });
                };

                window.zoomIn = function() {
                    map.zoomIn();
                };

                window.zoomOut = function() {
                    map.zoomOut();
                };
            </script>
        </body>
        </html>
    """.trimIndent()
}
