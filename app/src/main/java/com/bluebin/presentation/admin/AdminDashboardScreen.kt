package com.bluebin.presentation.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.presentation.auth.AuthViewModel
import com.bluebin.presentation.driver.DriverLocation
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
// Note: TPS management screens will be implemented as admin-only features

// Only 4 required management tools
enum class AdminDestination(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String
) {
    ANALYTICS("Analytics & Reports", Icons.Default.Analytics, "View system analytics and reports"),
    USER_MANAGEMENT("User Management", Icons.Default.People, "Approve users and manage roles"),
    TPS_MANAGEMENT("TPS Management", Icons.Default.LocationOn, "Monitor and manage TPS locations"),
    SCHEDULE_MANAGEMENT("Schedule Management", Icons.Default.Schedule, "Create and manage collection schedules")
}

@Composable
fun AdminDashboardScreen(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf<AdminDestination?>(null) }
    
    // Auto-refresh driver locations every 10 seconds for real-time tracking
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000L) // 10 seconds
            dashboardViewModel.refreshDriverLocations()
        }
    }

    // Handle navigation to sub-screens
    currentDestination?.let { destination ->
        when (destination) {
            AdminDestination.ANALYTICS -> {
                ReportsAnalyticsScreen(
                    onBackClick = { currentDestination = null }
                )
            }
            AdminDestination.USER_MANAGEMENT -> {
                UserManagementScreen(
                    onNavigateBack = { currentDestination = null },
                    viewModel = dashboardViewModel
                )
            }
            AdminDestination.TPS_MANAGEMENT -> {
                TPSManagementScreen(
                    onNavigateBack = { currentDestination = null },
                    viewModel = dashboardViewModel
                )
            }
            AdminDestination.SCHEDULE_MANAGEMENT -> {
                ScheduleManagementScreen(
                    onBackClick = { currentDestination = null }
                )
            }
        }
        return
    }

    Scaffold(
        bottomBar = {
            AdminBottomNavigationBar(
                onDestinationSelected = { destination ->
                    currentDestination = destination
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            // Modern Header with gradient
            ModernAdminHeader(
                userName = authState.user?.name ?: "Admin",
                onRefresh = { dashboardViewModel.loadDashboardData() },
                onLogout = { authViewModel.signOut() }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Real-time tracking section
                item {
                    ModernSectionHeader(
                        title = "Real-time Driver Tracking",
                        subtitle = "Monitor active collection routes"
                    )
                }

                item {
                    RealTimeDriverTrackingMap(
                        dashboardState = dashboardState,
                        onRefresh = { 
                            dashboardViewModel.loadDashboardData()
                            dashboardViewModel.refreshDriverLocations()
                        }
                    )
                }

                // System overview section
                item {
                    ModernSectionHeader(
                        title = "System Overview",
                        subtitle = "Key performance metrics"
                    )
                }

                if (dashboardState.isLoading) {
                    item {
                        ModernLoadingIndicator(message = "Loading dashboard data...")
                    }
                } else {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            item {
                                ModernMetricCard(
                                    title = "Active Drivers",
                                    value = dashboardState.stats.activeSchedules.toString(),
                                    subtitle = "Currently on routes",
                                    icon = Icons.Default.LocalShipping,
                                    color = MaterialTheme.colorScheme.primary,
                                    onClick = { currentDestination = AdminDestination.SCHEDULE_MANAGEMENT }
                                )
                            }
                            item {
                                ModernMetricCard(
                                    title = "TPS Locations",
                                    value = dashboardState.stats.totalTPS.toString(),
                                    subtitle = "${dashboardState.stats.fullTPS} at capacity",
                                    icon = Icons.Default.LocationOn,
                                    color = if (dashboardState.stats.fullTPS > 0) ErrorColor else SuccessColor,
                                    onClick = { currentDestination = AdminDestination.TPS_MANAGEMENT }
                                )
                            }
                            item {
                                ModernMetricCard(
                                    title = "Total Users",
                                    value = dashboardState.stats.totalUsers.toString(),
                                    subtitle = "${dashboardState.stats.pendingApprovals} pending approval",
                                    icon = Icons.Default.People,
                                    color = InfoColor,
                                    onClick = { currentDestination = AdminDestination.USER_MANAGEMENT }
                                )
                            }
                            item {
                                ModernMetricCard(
                                    title = "Completed Today",
                                    value = dashboardState.stats.completedToday.toString(),
                                    subtitle = "Collection routes",
                                    icon = Icons.Default.CheckCircle,
                                    color = SuccessColor,
                                    onClick = { currentDestination = AdminDestination.ANALYTICS }
                                )
                            }
                        }
                    }
                }

                // Alerts section
                if (dashboardState.stats.pendingApprovals > 0 || dashboardState.stats.fullTPS > 0) {
                    item {
                        ModernSectionHeader(
                            title = "Requires Attention",
                            subtitle = "Critical system alerts"
                        )
                    }
                    
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (dashboardState.stats.pendingApprovals > 0) {
                                ModernAlertCard(
                                    title = "User Approvals Pending",
                                    message = "${dashboardState.stats.pendingApprovals} users are waiting for approval",
                                    alertType = AlertType.WARNING,
                                    onClick = { currentDestination = AdminDestination.USER_MANAGEMENT }
                                )
                            }
                            if (dashboardState.stats.fullTPS > 0) {
                                ModernAlertCard(
                                    title = "TPS at Full Capacity",
                                    message = "${dashboardState.stats.fullTPS} locations need immediate collection",
                                    alertType = AlertType.ERROR,
                                    onClick = { currentDestination = AdminDestination.TPS_MANAGEMENT }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernAdminHeader(
    userName: String,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            )
    ) {
        Column {
            // Status bar spacer
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Admin Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f)
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ExitToApp, 
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            
            // Welcome section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back, $userName",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "Real-time system monitoring and management",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🗺️", fontSize = 24.sp)
                }
            }
        }
    }
}

@Composable
private fun AdminBottomNavigationBar(
    onDestinationSelected: (AdminDestination) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary,
        modifier = Modifier.height(80.dp)
    ) {
        AdminDestination.values().forEach { destination ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        destination.icon, 
                        contentDescription = destination.title,
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                label = { 
                    Text(
                        text = when (destination) {
                            AdminDestination.ANALYTICS -> "Analytics"
                            AdminDestination.USER_MANAGEMENT -> "Users"
                            AdminDestination.TPS_MANAGEMENT -> "TPS"
                            AdminDestination.SCHEDULE_MANAGEMENT -> "Schedule"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        fontWeight = FontWeight.Medium
                    ) 
                },
                selected = false,
                onClick = { onDestinationSelected(destination) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
private fun RealTimeDriverTrackingMap(
    dashboardState: AdminDashboardUiState,
    onRefresh: () -> Unit
) {
    // Default camera position (Bandung, Indonesia)
    val defaultPosition = LatLng(-6.9175, 107.6191)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    ModernCard(
        modifier = Modifier.height(400.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Map header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live Driver Locations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                                    Text(
                    text = "${dashboardState.activeDriverLocations.size} drivers live • ${dashboardState.stats.activeSchedules} active routes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Live indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(SuccessColor, CircleShape)
                    )
                    Text(
                        "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = SuccessColor
                    )
                    
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Google Map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        mapType = MapType.NORMAL,
                        isMyLocationEnabled = false
                    ),
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = false,
                        compassEnabled = true,
                        myLocationButtonEnabled = false
                    )
                ) {
                    // Add TPS markers with different icons based on status
                    dashboardState.tpsLocations.forEach { tps ->
                        val isFull = tps.status.name == "PENUH"
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    tps.location.latitude,
                                    tps.location.longitude
                                )
                            ),
                            title = tps.name,
                            snippet = "Status: ${tps.status.name}${if (isFull) " ⚠️" else " ✅"}",
                            icon = MapMarkerUtils.getTpsMarkerIcon(isFull)
                        )
                    }
                    
                    // Add real driver markers with driver icon
                    dashboardState.activeDriverLocations.forEach { driverLocation ->
                        // Calculate how long ago the location was updated
                        val minutesAgo = (System.currentTimeMillis() - driverLocation.timestamp) / (60 * 1000)
                        val timeAgo = when {
                            minutesAgo < 1 -> "Just now"
                            minutesAgo < 60 -> "${minutesAgo}m ago"
                            else -> "${minutesAgo / 60}h ago"
                        }
                        
                        // Find driver name from users list
                        val driverUser = dashboardState.users.find { it.uid == driverLocation.driverId }
                        val driverName = driverUser?.name ?: "Driver ${driverLocation.driverId.take(6)}"
                        
                        Marker(
                            state = MarkerState(
                                position = LatLng(
                                    driverLocation.latitude,
                                    driverLocation.longitude
                                )
                            ),
                            title = "🚛 $driverName",
                            snippet = "Live tracking • Updated $timeAgo${if (driverLocation.speed > 0) " • ${String.format("%.1f", driverLocation.speed)} km/h" else ""}",
                            icon = MapMarkerUtils.getDriverMarkerIcon()
                        )
                    }
                }
                
                // Empty state overlay
                if (dashboardState.activeDriverLocations.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ModernCard(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.LocationOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "No Active Drivers",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "No active drivers with live location tracking",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}