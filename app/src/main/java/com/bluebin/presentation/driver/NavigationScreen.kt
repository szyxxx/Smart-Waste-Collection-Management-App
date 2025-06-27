@file:OptIn(ExperimentalPermissionsApi::class)

package com.bluebin.presentation.driver

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
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
import kotlinx.coroutines.awaitCancellation
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
import com.bluebin.util.PhotoUtils
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
    
    // Permission handling - Location is required, camera is optional
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // Camera permission separate since it's optional
    val cameraPermission = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA
        )
    )
    
    // Check permissions when screen loads - only request if location permission not granted
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (!locationPermissions.allPermissionsGranted) {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }
    
    // Find current active stop
    val currentStop = currentRoute.stops.getOrNull(currentStopIndex)
    val nextStop = currentRoute.stops.getOrNull(currentStopIndex + 1)
    val isLastStop = currentStopIndex >= currentRoute.stops.size - 1
    
    // State for error handling  
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Simplified camera launcher using a temporary file
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("NavigationScreen", "Photo captured successfully: $capturedPhotoUri")
            // Verify the file exists and is readable
            try {
                val currentUri = capturedPhotoUri
                if (currentUri != null) {
                    // Test if we can read the photo data
                    val canRead = when (currentUri.scheme) {
                        "content" -> {
                            try {
                                context.contentResolver.openInputStream(currentUri)?.use { inputStream ->
                                    val bytes = inputStream.readBytes()
                                    Log.d("NavigationScreen", "Content URI photo verified: ${bytes.size} bytes")
                                    bytes.isNotEmpty()
                                } ?: false
                            } catch (e: Exception) {
                                Log.e("NavigationScreen", "Failed to read content URI", e)
                                false
                            }
                        }
                        "file" -> {
                            val photoFile = java.io.File(currentUri.path ?: "")
                            val exists = photoFile.exists() && photoFile.length() > 0
                            if (exists) {
                                Log.d("NavigationScreen", "File URI photo verified: ${photoFile.absolutePath} (${photoFile.length()} bytes)")
                            }
                            exists
                        }
                        else -> false
                    }
                    
                    if (canRead) {
                        errorMessage = null // Clear any previous errors
                    } else {
                        Log.w("NavigationScreen", "Photo file not found or empty after capture")
                        errorMessage = "Photo may not have been saved properly. You can continue without it or try again."
                    }
                } else {
                    Log.w("NavigationScreen", "Photo URI is null after capture")
                    errorMessage = "Photo URI is null. You can continue without it or try again."
                }
            } catch (e: Exception) {
                Log.e("NavigationScreen", "Error verifying photo file", e)
                errorMessage = "Error verifying photo: ${e.message}. You can continue without it or try again."
            }
        } else {
            Log.e("NavigationScreen", "Photo capture failed or cancelled")
            errorMessage = "Photo capture failed or was cancelled. You can continue without a photo."
            capturedPhotoUri = null
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (locationPermissions.allPermissionsGranted) {
            // Main Map View
            GoogleMapView(
                currentStop = currentStop,
                allStops = currentRoute.stops,
                currentStopIndex = currentStopIndex,
                driverViewModel = driverViewModel,
                locationPermissions = locationPermissions,
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
                onCapturePhoto = { 
                    try {
                        // Check if camera permission is granted
                        if (cameraPermission.allPermissionsGranted) {
                            try {
                                // Create a simple temporary file in cache directory
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                val photoFile = File(context.cacheDir, "temp_photo_${timeStamp}.jpg")
                                
                                // Create URI - try multiple approaches for compatibility
                                val photoUri = try {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        photoFile
                                    )
                                } catch (e: Exception) {
                                    Log.w("NavigationScreen", "FileProvider failed, using file URI: ${e.message}")
                                    Uri.fromFile(photoFile)
                                }
                                
                                Log.d("NavigationScreen", "Attempting to capture photo to: ${photoFile.absolutePath}")
                                
                                // Set the URI for the callback
                                capturedPhotoUri = photoUri
                                
                                // Launch camera
                                cameraLauncher.launch(photoUri)
                                
                            } catch (e: Exception) {
                                Log.e("NavigationScreen", "Camera setup failed", e)
                                errorMessage = "Camera setup failed. You can continue without a photo."
                            }
                        } else {
                            Log.w("NavigationScreen", "Camera permission not granted")
                            errorMessage = "Camera permission required. Tap here to grant permission, or continue without a photo."
                            // Re-request camera permission
                            cameraPermission.launchMultiplePermissionRequest()
                        }
                    } catch (e: Exception) {
                        Log.e("NavigationScreen", "Error setting up camera capture", e)
                        errorMessage = "Camera error: ${e.message}. You can continue without a photo."
                    }
                },
                onArrivedAtStop = {
                    // Allow completion with or without photo
                    val photoPath = if (PhotoUtils.isValidPhotoUri(capturedPhotoUri)) {
                        capturedPhotoUri.toString()
                    } else {
                        null // Use null instead of empty string for cleaner handling
                    }
                    
                    Log.d("NavigationScreen", "Completing collection - Stop: ${currentStopIndex + 1}, Photo: $photoPath, Notes: '$notes'")
                    
                    driverViewModel.completeCollection(
                        stopIndex = currentStopIndex,
                        proofPhoto = photoPath,
                        notes = notes
                    )
                    
                    if (isLastStop) {
                        Log.d("NavigationScreen", "Last stop completed - finishing route")
                        onNavigationComplete()
                    } else {
                        Log.d("NavigationScreen", "Moving to next stop: ${currentStopIndex + 1} -> ${currentStopIndex + 2}")
                        currentStopIndex++
                        capturedPhotoUri = null
                        notes = ""
                        errorMessage = null // Clear any error messages
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
        
        // Show error message if camera is not available
        errorMessage?.let { message ->
            LaunchedEffect(message) {
                // Auto-dismiss after 5 seconds
                kotlinx.coroutines.delay(5000)
                errorMessage = null
            }
            
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Camera Notice",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    IconButton(
                        onClick = { errorMessage = null }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoogleMapView(
    currentStop: RouteStop?,
    allStops: List<RouteStop>,
    currentStopIndex: Int,
    driverViewModel: DriverViewModel,
    locationPermissions: com.google.accompanist.permissions.MultiplePermissionsState,
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
    
    // Get user's current location and start real-time tracking
    LaunchedEffect(Unit) {
        try {
            val location = fusedLocationClient.lastLocation.await()
            location?.let {
                val newLocation = LatLng(it.latitude, it.longitude)
                userLocation = newLocation
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(newLocation, 15f),
                    1000
                )
                
                // Update driver location in database for admin tracking
                driverViewModel.updateLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    speed = if (it.hasSpeed()) it.speed.toDouble() * 3.6 else 0.0, // Convert m/s to km/h
                    heading = if (it.hasBearing()) it.bearing.toDouble() else 0.0
                )
            }
        } catch (e: Exception) {
            // Handle permission or location errors
        }
    }
    
    // Set up real-time location updates
    LaunchedEffect(currentStopIndex) {
        if (locationPermissions.allPermissionsGranted) {
            var locationCallback: LocationCallback? = null
            try {
                val locationRequest = LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5000L // Update every 5 seconds
                ).apply {
                    setMinUpdateIntervalMillis(2000L) // Fastest update every 2 seconds
                    setMaxUpdateDelayMillis(10000L) // Max delay 10 seconds
                }.build()
                
                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult) {
                        locationResult.lastLocation?.let { location ->
                            val newLocation = LatLng(location.latitude, location.longitude)
                            userLocation = newLocation
                            
                            // Update driver location in database for admin tracking
                            driverViewModel.updateLocation(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                speed = if (location.hasSpeed()) location.speed.toDouble() * 3.6 else 0.0,
                                heading = if (location.hasBearing()) location.bearing.toDouble() else 0.0
                            )
                        }
                    }
                }
                
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
                
                // Clean up when component is destroyed
                kotlinx.coroutines.awaitCancellation()
            } catch (e: kotlinx.coroutines.CancellationException) {
                locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
                throw e
            } catch (e: SecurityException) {
                Log.e("NavigationScreen", "Location permission denied", e)
                locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
            }
        }
    }
    
    // Get route directions when current stop or user location changes
    LaunchedEffect(currentStopIndex, userLocation) {
        if (userLocation != null && currentStop != null) {
            isLoadingRoute = true
            Log.d("NavigationScreen", "Getting directions from ${userLocation!!.latitude},${userLocation!!.longitude} to ${currentStop.latitude},${currentStop.longitude}")
            try {
                val directions = getDirections(
                    origin = userLocation!!,
                    destination = LatLng(currentStop.latitude, currentStop.longitude),
                    context = context
                )
                
                if (directions.isNotEmpty()) {
                    routePoints = directions
                    Log.d("NavigationScreen", "Route found with ${directions.size} points")
                    
                    // Auto-fit camera to show both user location and destination
                    val bounds = LatLngBounds.builder().apply {
                        include(userLocation!!)
                        include(LatLng(currentStop.latitude, currentStop.longitude))
                        directions.forEach { include(it) }
                    }.build()
                    
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(bounds, 100),
                        1500
                    )
                } else {
                    Log.w("NavigationScreen", "No route points returned, using straight line")
                    routePoints = listOf(userLocation!!, LatLng(currentStop.latitude, currentStop.longitude))
                }
            } catch (e: Exception) {
                Log.e("NavigationScreen", "Error getting directions, using straight line fallback", e)
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
                icon = MapMarkerUtils.getRouteStopMarkerIcon(isCompletedStop, isCurrentStop)
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
                    Icons.AutoMirrored.Filled.ArrowBack,
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
            
            HorizontalDivider(color = Color(0xFFE0E0E0))
            
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
                        containerColor = if (PhotoUtils.isValidPhotoUri(capturedPhotoUri)) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        if (PhotoUtils.isValidPhotoUri(capturedPhotoUri)) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (PhotoUtils.isValidPhotoUri(capturedPhotoUri)) "Retake" else "Capture")
                }
                
                // Complete/Next button
                Button(
                    onClick = onArrivedAtStop,
                    modifier = Modifier.weight(1f),
                    enabled = true, // Always enabled - photo is optional
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(
                        if (isLastStop) Icons.Default.Flag else Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isLastStop) "Complete" else "Next Stop")
                }
            }
            
            // Notes field (compact) - always show when photo is captured or when no camera is available
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
                    text = "Location Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "This app needs location access to provide navigation and track your route. Please grant location permission to continue.",
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
            Log.e("NavigationScreen", "Google Maps API key not found in manifest")
            return@withContext listOf(origin, destination)
        }
        
        Log.d("NavigationScreen", "Using API key: ${apiKey.take(10)}...")
        
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
        Log.d("NavigationScreen", "Directions API response code: $responseCode")
        
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { 
                it.readText() 
            }
            
            Log.d("NavigationScreen", "Directions API response: ${response.take(200)}...")
            
            val jsonResponse = JSONObject(response)
            val routes = jsonResponse.getJSONArray("routes")
            
            Log.d("NavigationScreen", "Found ${routes.length()} routes")
            
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
                        val decodedPoints = decodePolyline(points)
                        polylinePoints.addAll(decodedPoints)
                    }
                }
                
                Log.d("NavigationScreen", "Successfully decoded ${polylinePoints.size} route points")
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

 