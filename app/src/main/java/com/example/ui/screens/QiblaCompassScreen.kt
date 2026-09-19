package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.PrimaryGreen
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt
import kotlin.math.abs

fun formatToBanglaNumber(num: Int): String {
    val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return num.toString().map { char ->
        if (char.isDigit()) banglaDigits[char - '0'] else char
    }.joinToString("")
}

fun getBanglaDirection(azimuth: Float): String {
    val normalized = (azimuth % 360 + 360) % 360
    return when {
        normalized < 22.5 || normalized >= 337.5 -> "উ"
        normalized < 67.5 -> "উ-পূ"
        normalized < 112.5 -> "পূ"
        normalized < 157.5 -> "দ-পূ"
        normalized < 202.5 -> "দ"
        normalized < 247.5 -> "দ-প"
        normalized < 292.5 -> "প"
        else -> "উ-প"
    }
}

@Composable
fun QiblaDialogContent() {
    val context = LocalContext.current
    var qiblaBearing by remember { mutableFloatStateOf(0f) }
    var currentAzimuth by remember { mutableFloatStateOf(0f) }
    var currentPitch by remember { mutableFloatStateOf(0f) }
    var currentRoll by remember { mutableFloatStateOf(0f) }
    var hasLocationPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    // Default to Dhaka if location is not available
    var latitude by remember { mutableDoubleStateOf(23.8103) }
    var longitude by remember { mutableDoubleStateOf(90.4125) }

    LaunchedEffect(latitude, longitude) {
        qiblaBearing = calculateQiblaBearing(latitude, longitude).toFloat()
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        var lastAccelerometer = FloatArray(3)
        var lastMagnetometer = FloatArray(3)
        var lastAccelerometerSet = false
        var lastMagnetometerSet = false

        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
                    lastAccelerometerSet = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
                    lastMagnetometerSet = true
                }

                if (lastAccelerometerSet && lastMagnetometerSet) {
                    val rotationMatrix = FloatArray(9)
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer)) {
                        val orientation = FloatArray(3)
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        val azimuthInRadians = orientation[0]
                        var azimuthInDegrees = (Math.toDegrees(azimuthInRadians.toDouble()) + 360).toFloat() % 360
                        
                        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
                        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
                        
                        // Low pass filter for smoother compass
                        currentAzimuth = currentAzimuth + 0.2f * (azimuthInDegrees - currentAzimuth)
                        currentPitch = currentPitch + 0.2f * (pitch - currentPitch)
                        currentRoll = currentRoll + 0.2f * (roll - currentRoll)
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(sensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(sensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    DisposableEffect(hasLocationPermission) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                latitude = location.latitude
                longitude = location.longitude
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (hasLocationPermission) {
            try {
                val lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) 
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (lastKnown != null) {
                    latitude = lastKnown.latitude
                    longitude = lastKnown.longitude
                }
                
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        10000L,
                        5f,
                        locationListener
                    )
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        10000L,
                        5f,
                        locationListener
                    )
                }
            } catch (e: SecurityException) {}
        }

        onDispose {
            if (hasLocationPermission) {
                try {
                    locationManager.removeUpdates(locationListener)
                } catch (e: SecurityException) {}
            }
        }
    }

    val distanceToMecca = calculateDistanceToMecca(latitude, longitude).roundToInt()
    var turnAngle = (qiblaBearing - currentAzimuth + 360) % 360
    if (turnAngle > 180) turnAngle -= 360
    
    val isAligned = abs(turnAngle) <= 3f
    var hasVibratedForCurrentAlignment by remember { mutableStateOf(false) }

    LaunchedEffect(isAligned) {
        if (isAligned && !hasVibratedForCurrentAlignment) {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(150L, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(150L)
                }
            }
            hasVibratedForCurrentAlignment = true
        } else if (!isAligned) {
            hasVibratedForCurrentAlignment = false
        }
    }
    
    val turnDirectionText = when {
        abs(turnAngle) <= 2 -> "আপনি সঠিক দিকে আছেন"
        turnAngle < 0 -> "বামে ${formatToBanglaNumber(abs(turnAngle.roundToInt()))}° ঘুরুন"
        else -> "ডানে ${formatToBanglaNumber(abs(turnAngle.roundToInt()))}° ঘুরুন"
    }

    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!hasLocationPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "লোকেশন পারমিশন প্রয়োজন",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                            )
                        }
                    ) {
                        Text("পারমিশন")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Turn instruction
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.ScreenRotation,
                contentDescription = null,
                tint = Color(0xFF2A7C5D),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = turnDirectionText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF2A7C5D)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        QiblaCompassUI(
            azimuth = currentAzimuth,
            qiblaBearing = qiblaBearing,
            pitch = currentPitch,
            roll = currentRoll
        )

        Spacer(modifier = Modifier.weight(1f))
        
        // Compass Info (Degrees)
        Text(
            text = "${formatToBanglaNumber(currentAzimuth.roundToInt())}° ${getBanglaDirection(currentAzimuth)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF2D3748)
        )
        Text(
            text = "কম্পাস",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
        
        // Distance Info
        Text(
            text = "${formatToBanglaNumber(distanceToMecca)} কিমি",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White else Color(0xFF2D3748)
        )
        Text(
            text = "মক্কা থেকে দূরত্ব",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun QiblaCompassUI(azimuth: Float, qiblaBearing: Float, pitch: Float = 0f, roll: Float = 0f) {
    val animatedAzimuth by animateFloatAsState(
        targetValue = -azimuth,
        animationSpec = tween(durationMillis = 300),
        label = "compassRotation"
    )
    
    val textMeasurer = rememberTextMeasurer()
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .size(340.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            val center = size.center
            val radius = size.width / 2
            
            // Draw Compass Base
            // Outer shadow and green ring
            drawCircle(
                color = Color(0xFF389E6E),
                radius = radius,
                style = Fill
            )
            // Inner white circle
            drawCircle(
                color = if (isDark) Color(0xFF1E2923) else Color.White,
                radius = radius - 20.dp.toPx(),
                style = Fill
            )
            
            // Draw Compass Dial (Rotating)
            rotate(degrees = animatedAzimuth, pivot = center) {
                
                // Tick marks on the green ring
                for (i in 0 until 360 step 5) {
                    val angle = Math.toRadians(i.toDouble() - 90)
                    val isCardinal = i % 90 == 0
                    
                    val tickLength = if (isCardinal) 12.dp.toPx() else 8.dp.toPx()
                    val tickWidth = if (isCardinal) 2.dp.toPx() else 1.dp.toPx()
                    
                    val startX = center.x + (radius - tickLength) * cos(angle).toFloat()
                    val startY = center.y + (radius - tickLength) * sin(angle).toFloat()
                    
                    val endX = center.x + radius * cos(angle).toFloat()
                    val endY = center.y + radius * sin(angle).toFloat()
                    
                    drawLine(
                        color = Color.White.copy(alpha = if (isCardinal) 1f else 0.5f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickWidth
                    )
                }

                // Inner decorative lines (sunburst effect inside white area)
                for (i in 0 until 360 step 30) {
                    val angle = Math.toRadians(i.toDouble() - 90)
                    val lineRadius = radius - 50.dp.toPx()
                    val startX = center.x + 15.dp.toPx() * cos(angle).toFloat()
                    val startY = center.y + 15.dp.toPx() * sin(angle).toFloat()
                    val endX = center.x + lineRadius * cos(angle).toFloat()
                    val endY = center.y + lineRadius * sin(angle).toFloat()
                    
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.2f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Draw Text (N, E, S, W, NE, SE, SW, NW)
                val labels = listOf(
                    0 to "N", 45 to "NE", 90 to "E", 135 to "SE", 
                    180 to "S", 225 to "SW", 270 to "W", 315 to "NW"
                )
                labels.forEach { (degree, text) ->
                    val angle = Math.toRadians(degree.toDouble() - 90)
                    val isN = degree == 0
                    
                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = TextStyle(
                            color = if (isN) Color(0xFF2A7C5D) else Color.Gray.copy(alpha = 0.8f),
                            fontSize = if (degree % 90 == 0) 18.sp else 14.sp,
                            fontWeight = if (isN) FontWeight.ExtraBold else FontWeight.Bold
                        )
                    )
                    
                    val textRadius = radius - 45.dp.toPx()
                    val textCenter = Offset(
                        center.x + textRadius * cos(angle).toFloat(),
                        center.y + textRadius * sin(angle).toFloat()
                    )
                    
                    rotate(degrees = degree.toFloat(), pivot = textCenter) {
                        drawText(
                            textLayoutResult = textLayoutResult,
                            topLeft = Offset(
                                textCenter.x - textLayoutResult.size.width / 2,
                                textCenter.y - textLayoutResult.size.height / 2
                            )
                        )
                    }
                }
            }
            
            // Draw Needle (Rotating with azimuth)
            rotate(degrees = animatedAzimuth + qiblaBearing, pivot = center) {
                // North pointing half (Green)
                drawPath(
                    path = Path().apply {
                        moveTo(center.x, center.y - radius + 70.dp.toPx()) // Tip
                        lineTo(center.x - 12.dp.toPx(), center.y) // Base left
                        lineTo(center.x, center.y) // Center
                        close()
                    },
                    color = Color(0xFF2A7C5D)
                )
                drawPath(
                    path = Path().apply {
                        moveTo(center.x, center.y - radius + 70.dp.toPx()) // Tip
                        lineTo(center.x + 12.dp.toPx(), center.y) // Base right
                        lineTo(center.x, center.y) // Center
                        close()
                    },
                    color = Color(0xFF389E6E) // Slightly lighter green for 3D effect
                )
                
                // South pointing half (Grey)
                drawPath(
                    path = Path().apply {
                        moveTo(center.x, center.y + radius - 70.dp.toPx()) // Tip
                        lineTo(center.x - 12.dp.toPx(), center.y) // Base left
                        lineTo(center.x, center.y) // Center
                        close()
                    },
                    color = Color(0xFF9E9E9E)
                )
                drawPath(
                    path = Path().apply {
                        moveTo(center.x, center.y + radius - 70.dp.toPx()) // Tip
                        lineTo(center.x + 12.dp.toPx(), center.y) // Base right
                        lineTo(center.x, center.y) // Center
                        close()
                    },
                    color = Color(0xFFBDBDBD)
                )
                
                // Center pin
                drawCircle(
                    color = Color.White,
                    radius = 12.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color(0xFF389E6E),
                    radius = 8.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = center
                )
            }

            // Draw Qibla pointer (rotating based on azimuth AND Qibla bearing)
            rotate(degrees = animatedAzimuth + qiblaBearing, pivot = center) {
                // Kaaba representation at the edge of the white circle
                val kaabaDistance = radius - 20.dp.toPx()
                val kaabaCenter = Offset(center.x, center.y - kaabaDistance)
                
                // White circle background for Kaaba icon to pop out
                drawCircle(
                    color = if (isDark) Color(0xFF1E2923) else Color.White,
                    radius = 16.dp.toPx(),
                    center = kaabaCenter
                )
                
                // Box representing Kaaba
                val kaabaSize = 18.dp.toPx()
                drawRect(
                    color = Color(0xFF333333), // Greyish black
                    topLeft = Offset(kaabaCenter.x - kaabaSize/2, kaabaCenter.y - kaabaSize/2),
                    size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize)
                )
                drawRect(
                    color = Color(0xFFD4AF37), // Gold band
                    topLeft = Offset(kaabaCenter.x - kaabaSize/2, kaabaCenter.y - kaabaSize/6),
                    size = androidx.compose.ui.geometry.Size(kaabaSize, 3.dp.toPx())
                )
            }
        }

        // Bubble level / tilt indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-10).dp)
                .size(44.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(if (isDark) MaterialTheme.colorScheme.surface else Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = size.center
                val radius = size.width / 2f
                
                // Draw background circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.2f),
                    radius = radius - 4.dp.toPx(),
                    center = center
                )
                
                // Draw center crosshairs
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(center.x, center.y - 8.dp.toPx()),
                    end = Offset(center.x, center.y + 8.dp.toPx()),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(center.x - 8.dp.toPx(), center.y),
                    end = Offset(center.x + 8.dp.toPx(), center.y),
                    strokeWidth = 1.dp.toPx()
                )
                
                // Draw center target circle
                drawCircle(
                    color = Color.Gray.copy(alpha = 0.5f),
                    radius = 4.dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
                
                val maxOffset = radius - 10.dp.toPx()
                val sensitivity = maxOffset / 30f // 30 degrees to reach edge
                
                var bubbleX = center.x - roll * sensitivity
                var bubbleY = center.y + pitch * sensitivity
                
                val dx = bubbleX - center.x
                val dy = bubbleY - center.y
                val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                if (dist > maxOffset) {
                    val ratio = maxOffset / dist
                    bubbleX = center.x + dx * ratio
                    bubbleY = center.y + dy * ratio
                }
                
                val isFlat = dist < 4.dp.toPx()
                val bubbleColor = if (isFlat) Color(0xFF10B981) else Color(0xFFFBBF24)
                
                drawCircle(
                    color = bubbleColor,
                    radius = 6.dp.toPx(),
                    center = Offset(bubbleX, bubbleY)
                )
            }
        }
    }
}

fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
    val meccaLat = Math.toRadians(21.422487)
    val meccaLng = Math.toRadians(39.826206)
    val userLat = Math.toRadians(latitude)
    val userLng = Math.toRadians(longitude)
    
    val dLng = meccaLng - userLng
    
    val y = sin(dLng) * cos(meccaLat)
    val x = cos(userLat) * sin(meccaLat) - sin(userLat) * cos(meccaLat) * cos(dLng)
    
    var qibla = Math.toDegrees(atan2(y, x))
    if (qibla < 0) {
        qibla += 360.0
    }
    return qibla
}

fun calculateDistanceToMecca(latitude: Double, longitude: Double): Double {
    val meccaLat = 21.422487
    val meccaLng = 39.826206
    val earthRadius = 6371.0 // Radius of the earth in km
    val dLat = Math.toRadians(meccaLat - latitude)
    val dLng = Math.toRadians(meccaLng - longitude)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(meccaLat)) *
            sin(dLng / 2) * sin(dLng / 2)
    val c = 2 * atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return earthRadius * c
}
