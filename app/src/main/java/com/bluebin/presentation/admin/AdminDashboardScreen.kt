package com.bluebin.presentation.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import com.bluebin.data.model.*
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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
    onNavigateToRouteDetails: (String) -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel(),
    dashboardViewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
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
                    onBackClick = { currentDestination = null },
                    onNavigateToRouteDetails = onNavigateToRouteDetails
                )
            }
        }
        return
    }

    val tabs = listOf("Overview", "Users", "TPS", "Schedules")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        // Update any tab-specific logic when page changes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header with gradient
        ModernGradientHeader(
            title = "Admin Dashboard",
            subtitle = "System Management Hub",
            emoji = "🛠️",
            actions = {
                ModernIconButton(
                    onClick = { dashboardViewModel.loadDashboardData() },
                    icon = Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
                ModernIconButton(
                    onClick = { authViewModel.signOut() },
                    icon = Icons.Default.ExitToApp,
                    contentDescription = "Logout"
                )
            }
        )

        // Welcome message
        AnimatedVisibility(
            visible = uiState.user != null,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Welcome back, ${uiState.user?.name ?: "Admin"}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "System running smoothly • All services online",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Surface(
                        color = SuccessColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(SuccessColor, CircleShape)
                            )
                            Text(
                                text = "LIVE",
                                style = MaterialTheme.typography.labelMedium,
                                color = SuccessColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Tab Navigation
        ModernTabRow(
            selectedTabIndex = pagerState.currentPage,
            tabs = tabs,
            onTabSelected = { index ->
                coroutineScope.launch {
                    pagerState.animateScrollToPage(index)
                }
            }
        )

        // Content based on loading state
        if (dashboardState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoadingIndicator(message = "Loading dashboard data...")
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> OverviewTab(
                        dashboardState = dashboardState,
                        dashboardViewModel = dashboardViewModel
                    )
                    1 -> UsersTab(
                        dashboardState = dashboardState,
                        onManageUsers = { currentDestination = AdminDestination.USER_MANAGEMENT }
                    )
                    2 -> TPSTab(
                        dashboardState = dashboardState,
                        onManageTPS = { currentDestination = AdminDestination.TPS_MANAGEMENT }
                    )
                    3 -> SchedulesTab(
                        dashboardState = dashboardState,
                        onNavigateToRouteDetails = onNavigateToRouteDetails,
                        onManageSchedules = { currentDestination = AdminDestination.SCHEDULE_MANAGEMENT }
                    )
                }
            }
        }
    }

    // Error handling
    dashboardState.error?.let { error ->
        LaunchedEffect(error) {
            // Show error snackbar or handle error
        }
    }
}

@Composable
private fun OverviewTab(
    dashboardState: AdminDashboardUiState,
    dashboardViewModel: AdminDashboardViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Quick Stats Section
        item {
            ModernSectionHeader(
                title = "System Overview",
                subtitle = "Real-time monitoring and key metrics"
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernStatsCard(
                        title = "Active Drivers",
                        value = dashboardState.stats.activeSchedules.toString(),
                        subtitle = "Currently on routes",
                        icon = Icons.Default.LocalShipping,
                        trend = if (dashboardState.stats.activeSchedules > 0) TrendType.POSITIVE else TrendType.NEUTRAL,
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatsCard(
                        title = "TPS Status",
                        value = "${dashboardState.stats.totalTPS - dashboardState.stats.fullTPS}/${dashboardState.stats.totalTPS}",
                        subtitle = "Available locations",
                        icon = Icons.Default.LocationOn,
                        trend = if (dashboardState.stats.fullTPS == 0) TrendType.POSITIVE else TrendType.NEGATIVE,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernStatsCard(
                        title = "Total Users",
                        value = dashboardState.stats.totalUsers.toString(),
                        subtitle = "${dashboardState.stats.pendingApprovals} pending approval",
                        icon = Icons.Default.People,
                        trend = if (dashboardState.stats.pendingApprovals == 0) TrendType.POSITIVE else TrendType.NEUTRAL,
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatsCard(
                        title = "Today's Progress",
                        value = dashboardState.stats.completedToday.toString(),
                        subtitle = "Completed today",
                        icon = Icons.Default.CheckCircle,
                        trend = TrendType.POSITIVE,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }



        // Live Driver Tracking
        item {
            ModernSectionHeader(
                title = "Live Driver Tracking",
                subtitle = "Real-time collection vehicle monitoring"
            )
        }

        item {
            LiveDriverTrackingCard(
                dashboardState = dashboardState,
                onRefresh = { 
                    dashboardViewModel.loadDashboardData()
                    dashboardViewModel.refreshDriverLocations()
                }
            )
        }

        // System Alerts
        if (dashboardState.stats.pendingApprovals > 0 || dashboardState.stats.fullTPS > 0) {
            item {
                ModernSectionHeader(
                    title = "System Alerts",
                    subtitle = "Items requiring immediate attention"
                )
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dashboardState.stats.pendingApprovals > 0) {
                        ModernAlertCard(
                            title = "User Approvals Pending",
                            message = "${dashboardState.stats.pendingApprovals} users are waiting for approval",
                            alertType = AlertType.WARNING,
                            onClick = { /* Navigate to user management */ }
                        )
                    }
                    if (dashboardState.stats.fullTPS > 0) {
                        ModernAlertCard(
                            title = "TPS at Full Capacity",
                            message = "${dashboardState.stats.fullTPS} locations need immediate attention",
                            alertType = AlertType.ERROR,
                            onClick = { /* Navigate to TPS management */ }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsersTab(
    dashboardState: AdminDashboardUiState,
    onManageUsers: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "User Management",
                subtitle = "${dashboardState.users.size} total users • ${dashboardState.stats.pendingApprovals} pending approval"
            )
        }

        item {
            ModernPrimaryButton(
                text = "Manage All Users",
                onClick = onManageUsers,
                icon = Icons.Default.People
            )
        }

        item {
            ModernSectionHeader(
                title = "Recent User Activity",
                subtitle = "Latest registered users"
            )
        }

        items(dashboardState.users.take(5)) { user ->
            ModernUserPreviewCard(user)
        }

        if (dashboardState.users.size > 5) {
            item {
                TextButton(
                    onClick = onManageUsers,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View All ${dashboardState.users.size} Users")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TPSTab(
    dashboardState: AdminDashboardUiState,
    onManageTPS: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "TPS Monitoring",
                subtitle = "${dashboardState.tpsLocations.size} locations • ${dashboardState.stats.fullTPS} at capacity"
            )
        }

        item {
            ModernPrimaryButton(
                text = "Manage TPS Locations",
                onClick = onManageTPS,
                icon = Icons.Default.LocationOn
            )
        }

        item {
            ModernSectionHeader(
                title = "TPS Status Overview",
                subtitle = "Current status of waste collection points"
            )
        }

        items(dashboardState.tpsLocations.take(5)) { tps ->
            ModernTPSPreviewCard(tps)
        }

        if (dashboardState.tpsLocations.size > 5) {
            item {
                TextButton(
                    onClick = onManageTPS,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View All ${dashboardState.tpsLocations.size} TPS Locations")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SchedulesTab(
    dashboardState: AdminDashboardUiState,
    onNavigateToRouteDetails: (String) -> Unit,
    onManageSchedules: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "Schedule Management",
                subtitle = "${dashboardState.schedules.size} total schedules • ${dashboardState.stats.activeSchedules} active"
            )
        }

        item {
            ModernPrimaryButton(
                text = "Manage Schedules",
                onClick = onManageSchedules,
                icon = Icons.Default.Schedule
            )
        }

        item {
            ModernSectionHeader(
                title = "Recent Schedules",
                subtitle = "Latest collection schedules"
            )
        }

        items(dashboardState.schedules.take(5)) { schedule ->
            ModernSchedulePreviewCard(schedule, onNavigateToRouteDetails)
        }

        if (dashboardState.schedules.size > 5) {
            item {
                TextButton(
                    onClick = onManageSchedules,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View All ${dashboardState.schedules.size} Schedules")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveDriverTrackingCard(
    dashboardState: AdminDashboardUiState,
    onRefresh: () -> Unit
) {
    val defaultPosition = LatLng(-6.9175, 107.6191)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultPosition, 12f)
    }

    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        text = "${dashboardState.activeDriverLocations.size} drivers online • Auto-refresh every 10s",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = SuccessColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(SuccessColor, CircleShape)
                            )
                            Text(
                                "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = SuccessColor
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Map
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
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
                    // TPS markers
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
                    
                    // Driver markers
                    dashboardState.activeDriverLocations.forEach { driverLocation ->
                        val minutesAgo = (System.currentTimeMillis() - driverLocation.timestamp) / (60 * 1000)
                        val timeAgo = when {
                            minutesAgo < 1 -> "Just now"
                            minutesAgo < 60 -> "${minutesAgo}m ago"
                            else -> "${minutesAgo / 60}h ago"
                        }
                        
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
            }
        }
    }
}

@Composable
private fun ModernUserPreviewCard(user: User) {
    ModernCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.first().uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${user.role} • ${if (user.approved) "Approved" else "Pending Approval"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            ModernStatusChip(
                text = if (user.approved) "ACTIVE" else "PENDING",
                color = if (user.approved) SuccessColor else WarningColor
            )
        }
    }
}

@Composable
private fun ModernTPSPreviewCard(tps: TPS) {
    ModernCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (tps.status.name == "PENUH") ErrorColor.copy(alpha = 0.12f) else SuccessColor.copy(alpha = 0.12f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (tps.status.name == "PENUH") ErrorColor else SuccessColor
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tps.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = tps.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            ModernStatusChip(
                text = if (tps.status.name == "PENUH") "FULL" else "AVAILABLE",
                color = if (tps.status.name == "PENUH") ErrorColor else SuccessColor
            )
        }
    }
}

@Composable
private fun ModernSchedulePreviewCard(
    schedule: Schedule,
    onNavigateToRouteDetails: (String) -> Unit
) {
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    ModernCard(
        onClick = { onNavigateToRouteDetails(schedule.scheduleId) }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Route ${schedule.scheduleId.takeLast(6)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${schedule.tpsRoute.size} stops • ${dateFormatter.format(schedule.date.toDate())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ModernStatusChip(
                    text = schedule.status.name.replace("_", " "),
                    color = when (schedule.status.name) {
                        "COMPLETED" -> SuccessColor
                        "IN_PROGRESS" -> InfoColor
                        "ASSIGNED" -> WarningColor
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${schedule.totalDistance.toInt()} km • ${schedule.estimatedDuration.toInt()} min",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}