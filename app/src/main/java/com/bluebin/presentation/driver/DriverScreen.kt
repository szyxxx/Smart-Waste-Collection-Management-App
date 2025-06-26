package com.bluebin.presentation.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.data.model.Schedule
import com.bluebin.data.model.ScheduleStatus
import com.bluebin.presentation.auth.AuthViewModel
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    onNavigateToAuth: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    driverViewModel: DriverViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val uiState by driverViewModel.uiState.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Route", "Collections", "Reports")

    // Handle errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // Show snackbar or handle error
            driverViewModel.clearError()
        }
    }

    // Handle success messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Show snackbar for success messages
            driverViewModel.clearMessage()
        }
    }

    // Navigation logic - show NavigationScreen when navigation is active
    val currentRoute = uiState.currentRoute
    val currentSchedule = uiState.currentSchedule
    
    if (uiState.isNavigationActive && currentRoute != null && currentSchedule != null) {
        NavigationScreen(
            currentRoute = currentRoute,
            currentSchedule = currentSchedule,
            onNavigationComplete = {
                driverViewModel.completeNavigation()
            },
            onBackPressed = {
                driverViewModel.stopNavigation()
            },
            driverViewModel = driverViewModel
        )
    } else {
        // Regular driver dashboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
        ) {
            // Modern Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Driver Dashboard",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Welcome back, ${authState.user?.name ?: "Driver"}!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                                
                                // Location tracking indicator
                                if (uiState.isLocationTracking) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFF4CAF50), CircleShape)
                                    )
                                    Text(
                                        text = "Live",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF4CAF50),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Debug button
                            IconButton(
                                onClick = { driverViewModel.debugDriverInfo() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFE3F2FD), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Info, 
                                    contentDescription = "Debug Info",
                                    tint = Color(0xFF1976D2)
                                )
                            }
                            
                            // Test button
                            IconButton(
                                onClick = { driverViewModel.testDriverScheduleAccess() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFFFF3E0), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.BugReport, 
                                    contentDescription = "Test Schedule Access",
                                    tint = Color(0xFFFF9800)
                                )
                            }
                            
                            IconButton(
                                onClick = { driverViewModel.refreshData() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF5F5F5), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Refresh, 
                                    contentDescription = "Refresh",
                                    tint = Color(0xFF666666)
                                )
                            }
                            
                            IconButton(
                                onClick = { authViewModel.signOut() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFFFEBEE), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.ExitToApp, 
                                    contentDescription = "Logout",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }

                    // Modern Tab Row
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.White,
                        contentColor = Color(0xFF4CAF50)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { 
                                    Text(
                                        title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedTab == index) Color(0xFF4CAF50) else Color(0xFF666666)
                                    ) 
                                }
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ModernLoadingState("Loading driver data...")
                }
            } else {
                // Tab Content
                when (selectedTab) {
                    0 -> DashboardTab(uiState, driverViewModel)
                    1 -> RouteTab(uiState, driverViewModel)
                    2 -> CollectionsTab(uiState)
                    3 -> ReportsTab(uiState)
                }
            }
        }
    }
}

@Composable
fun DashboardTab(uiState: DriverUiState, viewModel: DriverViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Section Header
        item {
            ModernSectionHeader(
                title = "Today's Performance",
                subtitle = if (uiState.currentSchedule != null) {
                    when (uiState.currentSchedule.status) {
                        ScheduleStatus.ASSIGNED -> "You have a schedule assigned - ready to start"
                        ScheduleStatus.IN_PROGRESS -> "Route in progress - ${uiState.currentRoute?.progress ?: 0}% completed"
                        else -> "No active schedule"
                    }
                } else {
                    "No schedule assigned today"
                }
            )
        }
        
        // Quick Stats Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatCard(
                    title = "Routes Today",
                    value = "${uiState.dailyStats.routesCompleted}/${uiState.dailyStats.totalRoutes}",
                    icon = Icons.Default.Route,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                ModernStatCard(
                    title = "Efficiency",
                    value = "${uiState.dailyStats.efficiency}%",
                    icon = Icons.Default.TrendingUp,
                    color = if (uiState.dailyStats.efficiency >= 80) 
                        Color(0xFF4CAF50) 
                    else 
                        Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernStatCard(
                    title = "Stops Completed",
                    value = "${uiState.dailyStats.stopsCompleted}",
                    icon = Icons.Default.LocationOn,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                ModernStatCard(
                    title = "Hours Worked",
                    value = "${uiState.dailyStats.hoursWorked.roundToInt()}h",
                    icon = Icons.Default.AccessTime,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Current Schedule Card
        item {
            ModernSectionHeader(
                title = "Current Schedule",
                subtitle = uiState.currentSchedule?.let { schedule ->
                    "Schedule ID: ${schedule.scheduleId.takeLast(6)} • ${schedule.tpsRoute.size} stops"
                } ?: "No active schedule"
            )
        }
        
        item {
            CurrentScheduleCard(uiState.currentSchedule, uiState.currentRoute, viewModel)
        }

        // Debug: Show all assigned schedules
        if (uiState.assignedSchedules.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = "All Assigned Schedules",
                    subtitle = "${uiState.assignedSchedules.size} schedules assigned to you"
                )
            }
            
            items(uiState.assignedSchedules) { schedule ->
                ModernCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Schedule ${schedule.scheduleId.takeLast(8)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            ModernStatusChip(
                                text = schedule.status.name,
                                color = when (schedule.status) {
                                    ScheduleStatus.ASSIGNED -> Color(0xFF2196F3)
                                    ScheduleStatus.IN_PROGRESS -> Color(0xFF4CAF50)
                                    ScheduleStatus.COMPLETED -> Color(0xFF4CAF50)
                                    else -> Color(0xFF666666)
                                }
                            )
                        }
                        
                        Text(
                            text = "TPS Locations: ${schedule.tpsRoute.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        
                        Text(
                            text = "Date: ${schedule.date}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        
                        if (schedule.status == ScheduleStatus.ASSIGNED) {
                            Button(
                                onClick = { 
                                    viewModel.startSchedule()
                                    viewModel.startNavigation()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50)
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Start Navigation")
                            }
                        }
                    }
                }
            }
        } else {
            item {
                ModernCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Assignment,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color(0xFF666666)
                        )
                        Text(
                            text = "No Schedules Assigned",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Wait for admin to assign you a route or check back later",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }

        // Recent Collections
        if (uiState.collectionRecords.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = "Recent Collections",
                    subtitle = "${uiState.collectionRecords.size} completed routes"
                )
            }

            items(uiState.collectionRecords.take(3)) { record ->
                CollectionRecordCard(record)
            }
        }
    }
}

@Composable
private fun ModernStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ModernCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
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
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
fun RouteTab(uiState: DriverUiState, viewModel: DriverViewModel) {
    val currentRoute = uiState.currentRoute
    val currentSchedule = uiState.currentSchedule

    if (currentRoute == null || currentSchedule == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ModernEmptyState(
                icon = Icons.Default.Route,
                title = "No Active Route",
                subtitle = if (uiState.assignedSchedules.isNotEmpty()) {
                    "Start your assigned schedule to begin navigation"
                } else {
                    "You will be assigned a route by the admin"
                }
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Route Header
            item {
                RouteHeaderCard(currentRoute, currentSchedule, viewModel)
            }

            // Route Progress
            item {
                RouteProgressCard(currentRoute)
            }

            // Navigation Card
            if (currentRoute.status == RouteStatus.IN_PROGRESS) {
                item {
                    NavigationCard(currentRoute)
                }
            }

            // Route Stops
            item {
                ModernSectionHeader(
                    title = "Route Stops",
                    subtitle = "${currentRoute.stops.count { it.isCompleted }}/${currentRoute.stops.size} completed"
                )
            }

            itemsIndexed(currentRoute.stops) { index, stop ->
                RouteStopCard(stop, index, viewModel)
            }
        }
    }
}

@Composable
fun CollectionsTab(uiState: DriverUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernSectionHeader(
                    title = "Collection History",
                    subtitle = "${uiState.collectionRecords.size} completed collections"
                )
            }
        }

        items(uiState.collectionRecords) { record ->
            CollectionRecordCard(record, showDetails = true)
        }

        if (uiState.collectionRecords.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.History,
                    title = "No Collections Yet",
                    subtitle = "Your completed collections will appear here"
                )
            }
        }
    }
}

@Composable
fun ReportsTab(uiState: DriverUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "Performance Reports",
                subtitle = "Track your daily and weekly performance metrics"
            )
        }

        item {
            DailyReportCard(uiState.dailyStats)
        }

        item {
            ModernSectionHeader(
                title = "Weekly Summary",
                subtitle = "Performance overview for this week"
            )
        }

        item {
            WeeklyReportCard(uiState.dailyStats)
        }

        item {
            ModernSectionHeader(
                title = "Performance Metrics",
                subtitle = "Detailed efficiency and completion metrics"
            )
        }

        item {
            PerformanceMetricsCard(uiState.dailyStats)
        }
    }
}

@Composable
fun CurrentScheduleCard(
    schedule: Schedule?,
    route: RouteAssignment?,
    viewModel: DriverViewModel
) {
    ModernCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (schedule != null && route != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Route ${route.routeId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "${route.totalStops} stops • ${route.estimatedDuration}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    ModernStatusChip(
                        text = when (schedule.status) {
                            ScheduleStatus.ASSIGNED -> "Ready to Start"
                            ScheduleStatus.IN_PROGRESS -> "In Progress"
                            ScheduleStatus.COMPLETED -> "Completed"
                            else -> schedule.status.name.replace("_", " ")
                        },
                        color = when (schedule.status) {
                            ScheduleStatus.ASSIGNED -> Color(0xFFFF9800)
                            ScheduleStatus.IN_PROGRESS -> Color(0xFF2196F3)
                            ScheduleStatus.COMPLETED -> Color(0xFF4CAF50)
                            else -> Color(0xFF666666)
                        }
                    )
                }

                // Show progress for in-progress routes
                if (schedule.status == ScheduleStatus.IN_PROGRESS && route.progress > 0) {
                    ModernProgressIndicator(
                        label = "Route Progress",
                        progress = route.progress / 100f,
                        progressText = "${route.progress}% completed"
                    )
                }

                // Action button
                when (schedule.status) {
                    ScheduleStatus.ASSIGNED -> {
                        Button(
                            onClick = { 
                                viewModel.startSchedule()
                                viewModel.startNavigation()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Start Navigation",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    ScheduleStatus.IN_PROGRESS -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { /* TODO: Pause functionality */ },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause")
                            }
                            Button(
                                onClick = { viewModel.startNavigation() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF2196F3)
                                )
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate")
                            }
                        }
                    }
                    ScheduleStatus.COMPLETED -> {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Schedule Completed",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                        }
                    }
                    else -> {}
                }
            } else {
                ModernEmptyState(
                    icon = Icons.Default.Schedule,
                    title = "No Schedule Assigned",
                    subtitle = "Wait for admin to assign you a collection schedule"
                )
            }
        }
    }
}

@Composable
fun RouteHeaderCard(route: RouteAssignment, schedule: Schedule, viewModel: DriverViewModel) {
    ModernCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Route ${route.routeId}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "${route.totalStops} stops • ${route.estimatedDuration}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }

                ModernStatusChip(
                    text = when (route.status) {
                        RouteStatus.ASSIGNED -> "Ready"
                        RouteStatus.IN_PROGRESS -> "Active"
                        RouteStatus.COMPLETED -> "Done"
                        RouteStatus.CANCELLED -> "Cancelled"
                    },
                    color = when (route.status) {
                        RouteStatus.ASSIGNED -> Color(0xFFFF9800)
                        RouteStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        RouteStatus.COMPLETED -> Color(0xFF4CAF50)
                        RouteStatus.CANCELLED -> Color(0xFFD32F2F)
                    }
                )
            }

            if (route.status == RouteStatus.ASSIGNED) {
                Button(
                    onClick = { viewModel.startSchedule() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Start Route")
                }
            }
        }
    }
}

@Composable
fun RouteProgressCard(route: RouteAssignment) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Progress",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LinearProgressIndicator(
                progress = { route.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${route.stops.count { it.isCompleted }} of ${route.totalStops} completed",
                    fontSize = 14.sp
                )
                Text(
                    text = "${route.progress}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NavigationCard(route: RouteAssignment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Navigation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val nextStop = route.stops.firstOrNull { !it.isCompleted }
            if (nextStop != null) {
                Text(
                    text = "Next Stop: ${nextStop.name}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = nextStop.address,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Button(
                    onClick = { /* TODO: Implement navigation integration */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.Map, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in Maps")
                }
            } else {
                Text(
                    text = "All stops completed!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun RouteStopCard(stop: RouteStop, index: Int, viewModel: DriverViewModel) {
    var showProofDialog by remember { mutableStateOf(false) }
    var showIssueDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (stop.isCompleted) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (stop.isCompleted) 
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (stop.isCompleted) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = stop.order.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = stop.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stop.address,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        if (stop.isCompleted && stop.completedAt != null) {
                            Text(
                                text = "Completed at ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(stop.completedAt))}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (stop.hasIssue) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Issue reported",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (!stop.isCompleted) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showProofDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Complete")
                    }
                    OutlinedButton(
                        onClick = { showIssueDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Report Issue")
                    }
                }
            }

            if (stop.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Note: ${stop.notes}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }

    // Proof of Collection Dialog
    if (showProofDialog) {
        ProofOfCollectionDialog(
            onDismiss = { showProofDialog = false },
            onConfirm = { notes ->
                viewModel.completeCollection(index, notes = notes)
                showProofDialog = false
            }
        )
    }

    // Issue Report Dialog
    if (showIssueDialog) {
        IssueReportDialog(
            onDismiss = { showIssueDialog = false },
            onConfirm = { issue ->
                viewModel.reportIssue(index, issue)
                showIssueDialog = false
            }
        )
    }
}

@Composable
fun ProofOfCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Complete Collection") },
        text = {
            Column {
                Text("Mark this stop as completed")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(notes) }
            ) {
                Text("Complete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun IssueReportDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var issue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Issue") },
        text = {
            Column {
                Text("Describe the issue at this location")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = issue,
                    onValueChange = { issue = it },
                    label = { Text("Issue description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(issue) },
                enabled = issue.isNotBlank()
            ) {
                Text("Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun CollectionRecordCard(record: CollectionRecord, showDetails: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = record.routeId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = record.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (showDetails) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stops: ${record.completedStops}/${record.totalStops}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Duration: ${record.duration}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun DailyReportCard(stats: DailyStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Today's Performance",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stats.routesCompleted}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Routes\nCompleted",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                StatusCompleted.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stats.efficiency}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusCompleted
                        )
                    }
                    Text(
                        text = "Efficiency",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                StatusInProgress.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${stats.hoursWorked.roundToInt()}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = StatusInProgress
                        )
                    }
                    Text(
                        text = "Hours\nWorked",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyReportCard(stats: DailyStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "This Week",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Routes",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${stats.totalCompletedRoutes}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = "Avg Efficiency",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${stats.efficiency}%",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Column {
                    Text(
                        text = "Total Hours",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "${(stats.hoursWorked * 7).roundToInt()}h",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PerformanceMetricsCard(stats: DailyStats) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Insights",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Efficiency bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Efficiency",
                    modifier = Modifier.width(80.dp),
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    progress = { stats.efficiency / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (stats.efficiency >= 80) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${stats.efficiency}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Completion rate
            val completionRate = if (stats.totalRoutes > 0) (stats.routesCompleted * 100) / stats.totalRoutes else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Completion",
                    modifier = Modifier.width(80.dp),
                    fontSize = 14.sp
                )
                LinearProgressIndicator(
                    progress = { completionRate / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (completionRate >= 90) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${completionRate}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
} 