package com.bluebin.presentation.driver

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import com.bluebin.data.model.TPS
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NavigationScreen(
    currentRoute: RouteAssignment,
    currentSchedule: com.bluebin.data.model.Schedule,
    onNavigationComplete: () -> Unit,
    onBackPressed: () -> Unit,
    driverViewModel: DriverViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var currentStopIndex by remember { mutableIntStateOf(0) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var notes by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    // Permission handling
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        )
    )
    
    // Request permissions when screen loads
    LaunchedEffect(Unit) {
        locationPermissions.launchMultiplePermissionRequest()
    }
    
    // Find current active stop
    val currentStop = currentRoute.stops.getOrNull(currentStopIndex)
    val nextStop = currentRoute.stops.getOrNull(currentStopIndex + 1)
    val isLastStop = currentStopIndex >= currentRoute.stops.size - 1
    
    // Camera setup
    val photoFile = remember {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        File(context.cacheDir, "JPEG_${timeStamp}_proof.jpg")
    }
    
    val photoUri = remember {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoUri = photoUri
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissions.allPermissionsGranted) {
            // Main Map View
            GoogleMapView(
                currentStop = currentStop,
                allStops = currentRoute.stops,
                currentStopIndex = currentStopIndex,
                modifier = Modifier.fillMaxSize()
            )
            
            // Top Navigation Bar
            NavigationTopBar(
                currentStop = currentStop,
                currentStopIndex = currentStopIndex,
                totalStops = currentRoute.stops.size,
                onBackPressed = onBackPressed,
                modifier = Modifier.align(Alignment.TopStart)
            )
            
            // Bottom Actions Panel
            NavigationBottomPanel(
                currentStop = currentStop,
                nextStop = nextStop,
                isLastStop = isLastStop,
                capturedPhotoUri = capturedPhotoUri,
                notes = notes,
                onNotesChange = { notes = it },
                onCapturePhoto = { cameraLauncher.launch(photoUri) },
                onArrivedAtStop = {
                    if (capturedPhotoUri != null) {
                        driverViewModel.completeCollection(
                            stopIndex = currentStopIndex,
                            proofPhoto = capturedPhotoUri.toString(),
                            notes = notes
                        )
                        
                        if (isLastStop) {
                            onNavigationComplete()
                        } else {
                            currentStopIndex++
                            capturedPhotoUri = null
                            notes = ""
                        }
                    }
                },
                onShowDetails = { showBottomSheet = true },
                modifier = Modifier.align(Alignment.BottomStart)
            )
        } else {
            // Permission request UI
            PermissionRequestScreen(
                onRequestPermissions = { locationPermissions.launchMultiplePermissionRequest() }
            )
        }
    }
}

@Composable
private fun GoogleMapView(
    currentStop: RouteStop?,
    allStops: List<RouteStop>,
    currentStopIndex: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // State for user's current location
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    
    // Location client
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    // Default location (Jakarta)
    val defaultLocation = LatLng(-6.2088, 106.8456)
    
    // Current stop location
    val currentStopLocation = currentStop?.let { 
        LatLng(it.latitude, it.longitude) 
    } ?: defaultLocation
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: currentStopLocation, 15f)
    }
    
    // Get user's current location
    LaunchedEffect(Unit) {
        try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                userLocation = LatLng(it.latitude, it.longitude)
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15f),
                    1000
                )
            }
        } catch (e: Exception) {
            // Handle permission or location errors
        }
    }
    
    // Get route directions when current stop or user location changes
    LaunchedEffect(currentStopIndex, userLocation) {
        if (userLocation != null && currentStop != null) {
            isLoadingRoute = true
            try {
                val directions = getDirections(
                    origin = userLocation!!,
                    destination = LatLng(currentStop.latitude, currentStop.longitude),
                    context = context
                )
                routePoints = directions
                
                // Auto-fit camera to show both user location and destination
                if (directions.isNotEmpty()) {
                    val bounds = LatLngBounds.builder().apply {
                        include(userLocation!!)
                        include(LatLng(currentStop.latitude, currentStop.longitude))
                        directions.forEach { include(it) }
                    }.build()
                    
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100),
                        1500
                    )
                }
            } catch (e: Exception) {
                // Fallback to straight line if directions fail
                routePoints = listOf(userLocation!!, LatLng(currentStop.latitude, currentStop.longitude))
            } finally {
                isLoadingRoute = false
            }
        }
    }
    
    // Update camera when stop changes
    LaunchedEffect(currentStopIndex) {
        currentStop?.let {
            val newLocation = LatLng(it.latitude, it.longitude)
            if (userLocation != null) {
                // Show both user location and destination
                val bounds = LatLngBounds.builder().apply {
                    include(userLocation!!)
                    include(newLocation)
                }.build()
                
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngBounds(bounds, 150),
                    1000
                )
            } else {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(newLocation, 15f),
                    1000
                )
            }
        }
    }
    
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true,
            mapType = MapType.NORMAL
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true,
            compassEnabled = true,
            zoomControlsEnabled = false
        )
    ) {
        // Add markers for all stops
        allStops.forEachIndexed { index, stop ->
            val position = LatLng(stop.latitude, stop.longitude)
            val isCurrentStop = index == currentStopIndex
            val isCompletedStop = index < currentStopIndex
            
            Marker(
                state = MarkerState(position = position),
                title = stop.name,
                snippet = stop.address,
                icon = when {
                    isCurrentStop -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    isCompletedStop -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    else -> BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                }
            )
        }
        
        // Draw actual route directions from current location to current TPS
        if (routePoints.isNotEmpty() && !isLoadingRoute) {
            Polyline(
                points = routePoints,
                color = Color(0xFF2196F3),
                width = 8f
            )
        }
        
        // Show loading indicator for route
        if (isLoadingRoute && userLocation != null && currentStop != null) {
            Polyline(
                points = listOf(userLocation!!, LatLng(currentStop.latitude, currentStop.longitude)),
                color = Color(0xFF2196F3).copy(alpha = 0.5f),
                width = 4f
            )
        }
        
        // Draw completed routes in green
        for (i in 0 until currentStopIndex) {
            if (i + 1 < allStops.size) {
                val startStop = allStops[i]
                val endStop = allStops[i + 1]
                Polyline(
                    points = listOf(
                        LatLng(startStop.latitude, startStop.longitude),
                        LatLng(endStop.latitude, endStop.longitude)
                    ),
                    color = Color(0xFF4CAF50),
                    width = 6f
                )
            }
        }
    }
}

@Composable
private fun NavigationTopBar(
    currentStop: RouteStop?,
    currentStopIndex: Int,
    totalStops: Int,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackPressed,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1976D2)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Stop ${currentStopIndex + 1} of $totalStops",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                Text(
                    text = currentStop?.name ?: "Unknown Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
                )
            }
            
            // Progress indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF4CAF50), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${((currentStopIndex + 1) * 100 / totalStops)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun NavigationBottomPanel(
    currentStop: RouteStop?,
    nextStop: RouteStop?,
    isLastStop: Boolean,
    capturedPhotoUri: Uri?,
    notes: String,
    onNotesChange: (String) -> Unit,
    onCapturePhoto: () -> Unit,
    onArrivedAtStop: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Destination info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStop?.name ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2)
                    )
                    Text(
                        text = currentStop?.address ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                IconButton(onClick = onShowDetails) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "More details",
                        tint = Color(0xFF666666)
                    )
                }
            }
            
            // Next stop preview and route info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                nextStop?.let { next ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Flag,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next: ${next.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    }
                } ?: Spacer(modifier = Modifier.width(1.dp))
                
                // Navigation status
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Live Route",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Divider(color = Color(0xFFE0E0E0))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Photo capture button
                Button(
                    onClick = onCapturePhoto,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (capturedPhotoUri != null) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        if (capturedPhotoUri != null) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (capturedPhotoUri != null) "Retake" else "Capture")
                }
                
                // Complete/Next button
                Button(
                    onClick = onArrivedAtStop,
                    modifier = Modifier.weight(1f),
                    enabled = capturedPhotoUri != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (isLastStop) Icons.Default.Flag else Icons.Default.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLastStop) "Complete" else "Next Stop")
                }
            }
            
            // Notes field (compact)
            if (capturedPhotoUri != null) {
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2196F3),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    onRequestPermissions: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1976D2)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.padding(32.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF1976D2)
                )
                
                Text(
                    text = "Location & Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "To provide navigation and capture proof of collection, we need access to your location and camera.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
                
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

// Function to get directions from Google Directions API
suspend fun getDirections(
    origin: LatLng,
    destination: LatLng,
    context: android.content.Context
): List<LatLng> = withContext(Dispatchers.IO) {
    try {
        // Get API key from manifest
        val apiKey = context.packageManager
            .getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            .metaData
            .getString("com.google.android.geo.API_KEY") ?: ""
        
        if (apiKey.isEmpty()) {
            Log.e("NavigationScreen", "Google Maps API key not found")
            return@withContext listOf(origin, destination)
        }
        
        val originStr = "${origin.latitude},${origin.longitude}"
        val destinationStr = "${destination.latitude},${destination.longitude}"
        
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${URLEncoder.encode(originStr, "UTF-8")}&" +
                "destination=${URLEncoder.encode(destinationStr, "UTF-8")}&" +
                "mode=driving&" +
                "key=$apiKey"
        
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        
        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { 
                it.readText() 
            }
            
            val jsonResponse = JSONObject(response)
            val routes = jsonResponse.getJSONArray("routes")
            
            if (routes.length() > 0) {
                val route = routes.getJSONObject(0)
                val legs = route.getJSONArray("legs")
                val polylinePoints = mutableListOf<LatLng>()
                
                for (i in 0 until legs.length()) {
                    val leg = legs.getJSONObject(i)
                    val steps = leg.getJSONArray("steps")
                    
                    for (j in 0 until steps.length()) {
                        val step = steps.getJSONObject(j)
                        val polyline = step.getJSONObject("polyline")
                        val points = polyline.getString("points")
                        
                        // Decode polyline points
                        polylinePoints.addAll(decodePolyline(points))
                    }
                }
                
                return@withContext polylinePoints
            }
        } else {
            Log.e("NavigationScreen", "Directions API error: $responseCode")
        }
        
        connection.disconnect()
    } catch (e: Exception) {
        Log.e("NavigationScreen", "Error getting directions", e)
    }
    
    // Fallback to straight line
    return@withContext listOf(origin, destination)
}

// Function to decode Google's polyline encoding
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0
    
    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        
        val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(latLng)
    }
    
    return poly
}

  