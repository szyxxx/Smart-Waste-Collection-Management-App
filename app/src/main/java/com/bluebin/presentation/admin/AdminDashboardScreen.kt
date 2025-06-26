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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.presentation.auth.AuthViewModel
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf<AdminDestination?>(null) }

    // Handle navigation back to main dashboard
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
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            // Modern Header with full top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4CAF50),
                                Color(0xFF45A049)
                            )
                        )
                    )
                    .statusBarsPadding()
            ) {
                Column {
                    // Top status bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Admin Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { dashboardViewModel.loadDashboardData() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                            
                            IconButton(
                                onClick = { authViewModel.signOut() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp, 
                                    contentDescription = "Logout",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    
                    // Welcome section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome back, ${authState.user?.name ?: "Admin"}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Real-time driver tracking and system monitoring",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🗺️", fontSize = 24.sp)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Real-time Driver Tracking Map
                item {
                    Text(
                        text = "Real-time Driver Tracking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }

                item {
                    RealTimeDriverTrackingMap(
                        dashboardState = dashboardState,
                        onRefresh = { dashboardViewModel.loadDashboardData() }
                    )
                }

                // Quick Stats
                if (dashboardState.isLoading) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Loading dashboard data...",
                                color = Color(0xFF666666)
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "System Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            item {
                                ModernStatCard(
                                    title = "Active Drivers",
                                    value = dashboardState.stats.activeSchedules.toString(),
                                    subtitle = "Currently on routes",
                                    icon = Icons.Default.LocalShipping,
                                    color = Color(0xFF4CAF50),
                                    onClick = { currentDestination = AdminDestination.SCHEDULE_MANAGEMENT }
                                )
                            }
                            item {
                                ModernStatCard(
                                    title = "TPS Locations",
                                    value = dashboardState.stats.totalTPS.toString(),
                                    subtitle = "${dashboardState.stats.fullTPS} at capacity",
                                    icon = Icons.Default.LocationOn,
                                    color = if (dashboardState.stats.fullTPS > 0) Color(0xFFFF5722) else Color(0xFF4CAF50),
                                    onClick = { currentDestination = AdminDestination.TPS_MANAGEMENT }
                                )
                            }
                            item {
                                ModernStatCard(
                                    title = "Total Users",
                                    value = dashboardState.stats.totalUsers.toString(),
                                    subtitle = "${dashboardState.stats.pendingApprovals} pending approval",
                                    icon = Icons.Default.People,
                                    color = Color(0xFF2196F3),
                                    onClick = { currentDestination = AdminDestination.USER_MANAGEMENT }
                                )
                            }
                            item {
                                ModernStatCard(
                                    title = "Completed Today",
                                    value = dashboardState.stats.completedToday.toString(),
                                    subtitle = "Collection routes",
                                    icon = Icons.Default.CheckCircle,
                                    color = Color(0xFF9C27B0),
                                    onClick = { currentDestination = AdminDestination.ANALYTICS }
                                )
                            }
                        }
                    }
                }

                // Alerts section
                if (dashboardState.stats.pendingApprovals > 0 || dashboardState.stats.fullTPS > 0) {
                    item {
                        Text(
                            text = "Requires Attention",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
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
private fun AdminBottomNavigationBar(
    onDestinationSelected: (AdminDestination) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        contentColor = Color(0xFF4CAF50),
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
                    selectedIconColor = Color(0xFF4CAF50),
                    selectedTextColor = Color(0xFF4CAF50),
                    unselectedIconColor = Color(0xFF666666),
                    unselectedTextColor = Color(0xFF666666),
                    indicatorColor = Color(0xFF4CAF50).copy(alpha = 0.12f)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Map header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live Driver Locations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "${dashboardState.stats.activeSchedules} drivers active",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Live indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    )
                    Text(
                        "LIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFF666666)
                        )
                    }
                }
            }

            // Google Map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 0.dp)
                    .padding(bottom = 20.dp)
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
                                         // Add TPS markers
                     dashboardState.tpsLocations.forEach { tps ->
                         Marker(
                             state = MarkerState(
                                 position = LatLng(
                                     tps.location.latitude,
                                     tps.location.longitude
                                 )
                             ),
                             title = tps.name,
                             snippet = "Status: ${tps.status.name}"
                         )
                     }
                                         
                     // Add sample driver markers (in a real app, this would be real-time driver locations)
                     if (dashboardState.stats.activeSchedules > 0) {
                         // Sample driver positions for demonstration
                         val sampleDriverPositions = listOf(
                             LatLng(-6.9147, 107.6098) to "Driver 1",
                             LatLng(-6.9200, 107.6150) to "Driver 2",
                             LatLng(-6.9100, 107.6250) to "Driver 3"
                         ).take(dashboardState.stats.activeSchedules)
                         
                         sampleDriverPositions.forEach { (position, driverName) ->
                             Marker(
                                 state = MarkerState(position = position),
                                 title = driverName,
                                 snippet = "On active route"
                             )
                         }
                     }
                }
                
                // Map overlay with driver info
                if (dashboardState.stats.activeSchedules == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White.copy(alpha = 0.9f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.LocationOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = Color(0xFF666666)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No Active Drivers",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    "All drivers have completed their routes",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                                 }
             }
         }
     }
}

// ... existing code for other composables (ModernStatCard, ModernAlertCard, etc.) ...

@Composable
private fun ModernStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(color.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = color
                    )
                }
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun ModernAlertCard(
    title: String,
    message: String,
    alertType: AlertType,
    onClick: (() -> Unit)? = null
) {
    val (backgroundColor, iconColor, icon) = when (alertType) {
        AlertType.WARNING -> Triple(Color(0xFFFFF3E0), Color(0xFFFF9800), Icons.Default.Warning)
        AlertType.ERROR -> Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.Error)
        AlertType.SUCCESS -> Triple(Color(0xFFE8F5E8), Color(0xFF4CAF50), Icons.Default.CheckCircle)
        AlertType.INFO -> Triple(Color(0xFFE3F2FD), Color(0xFF2196F3), Icons.Default.Info)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = iconColor
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
            
            if (onClick != null) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Go",
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF999999)
                )
            }
        }
    }
}

 