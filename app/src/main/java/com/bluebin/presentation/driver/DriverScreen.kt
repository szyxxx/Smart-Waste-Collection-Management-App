package com.bluebin.presentation.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.data.model.Schedule
import com.bluebin.data.model.ScheduleStatus
import com.bluebin.presentation.auth.AuthViewModel
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import kotlin.math.roundToInt
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DriverScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    driverViewModel: DriverViewModel = hiltViewModel()
) {
    val authUiState by authViewModel.uiState.collectAsState()
    val uiState by driverViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    val tabs = listOf("Dashboard", "Active Route", "Collections", "Performance")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    // Navigation state management
    var showNavigationScreen by remember { mutableStateOf(false) }

    // Show NavigationScreen when navigation is active
    val currentRoute = uiState.currentRoute
    val currentSchedule = uiState.currentSchedule
    if (showNavigationScreen && currentRoute != null && currentSchedule != null) {
        NavigationScreen(
            currentRoute = currentRoute,
            currentSchedule = currentSchedule,
            onNavigationComplete = {
                driverViewModel.completeNavigation()
                showNavigationScreen = false
            },
            onBackPressed = {
                driverViewModel.stopNavigation()
                showNavigationScreen = false
            },
            driverViewModel = driverViewModel
        )
        return
    }

    // Error handling
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_LONG).show()
            driverViewModel.clearError()
        }
    }

    // Success messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            driverViewModel.clearMessage()
        }
    }

    LaunchedEffect(Unit) {
        driverViewModel.refreshData()
    }

    if (authUiState.isLoading || authUiState.user == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ModernLoadingIndicator(message = "Loading driver dashboard...")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header with gradient
        ModernDriverHeader(
            driverName = authUiState.user?.name ?: "Driver",
            isLocationTracking = uiState.isLocationTracking,
            onRefresh = { driverViewModel.refreshData() },
            onLogout = { authViewModel.signOut() }
        )

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

        // Swipeable Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoadingIndicator()
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> DashboardTab(uiState)
                    1 -> ActiveRouteTab(uiState, driverViewModel) {
                        showNavigationScreen = true
                    }
                    2 -> CollectionsTab(uiState)
                    3 -> PerformanceTab(uiState)
                }
            }
        }
    }
}

@Composable
private fun ModernDriverHeader(
    driverName: String,
    isLocationTracking: Boolean,
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
            Spacer(modifier = Modifier.height(32.dp))
            
            // Title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "🚛",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Column {
                        Text(
                            text = "Driver Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Collection Routes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Refresh, 
                            contentDescription = "Refresh",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), 
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp, 
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Status indicator row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Welcome back, $driverName!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                
                if (isLocationTracking) {
                    Surface(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
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
                                text = "Live Tracking",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardTab(uiState: DriverUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "Today's Overview",
                subtitle = "Your current progress and status"
            )
        }

        // Current Status Card
        item {
            ModernCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
            Box(
                modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(16.dp)
                            ),
                contentAlignment = Alignment.Center
            ) {
                        Icon(
                            Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current Status",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val currentRoute = uiState.currentRoute
                        if (currentRoute != null) {
                            Text(
                                text = "Route ${currentRoute.routeId} - ${currentRoute.status}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currentRoute.stops.count { it.isCompleted }}/${currentRoute.totalStops} stops completed",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
        } else {
                            Text(
                                text = "No active route assigned",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Quick Stats
        item {
            ModernSectionHeader(
                title = "Performance Metrics",
                subtitle = "Your daily statistics"
            )
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Routes Today",
                    value = "${uiState.dailyStats.totalRoutes}",
                    subtitle = "Completed routes",
                    icon = Icons.Default.Route,
                    color = InfoColor
                )
                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Efficiency",
                    value = "${uiState.dailyStats.efficiency}%",
                    subtitle = "Performance rate",
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    color = SuccessColor
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Stops Done",
                    value = "${uiState.dailyStats.stopsCompleted}",
                    subtitle = "Collections made",
                    icon = Icons.Default.CheckCircle,
                    color = WarningColor
                )
                ModernMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Hours Today",
                    value = "${uiState.dailyStats.hoursWorked}h",
                    subtitle = "Working time",
                    icon = Icons.Default.AccessTime,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun ActiveRouteTab(uiState: DriverUiState, viewModel: DriverViewModel, onNavigateClicked: () -> Unit) {
    val currentRoute = uiState.currentRoute
    val currentSchedule = uiState.currentSchedule
    
    if (currentRoute == null || currentSchedule == null) {
        ModernEmptyState(
            icon = Icons.Default.Route,
            title = "No Active Route",
            subtitle = "Your assigned route will appear here"
        )
        return
    }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
            .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Route Header
            item {
            ModernRouteHeaderCard(currentRoute, currentSchedule, viewModel)
            }

            // Route Progress
            item {
            ModernRouteProgressCard(currentRoute)
            }

            // Navigation Card
        if (currentRoute.status == RouteStatus.IN_PROGRESS) {
            item {
                ModernNavigationCard(currentRoute, viewModel, onNavigateClicked)
            }
            }

            // Route Stops
            item {
            ModernSectionHeader(
                title = "Collection Points",
                subtitle = "${currentRoute.stops.count { it.isCompleted }}/${currentRoute.stops.size} completed"
                )
            }

            itemsIndexed(currentRoute.stops) { index, stop ->
            ModernRouteStopCard(stop, index, viewModel)
        }
    }
}

@Composable
fun CollectionsTab(uiState: DriverUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "Collection Schedules",
                subtitle = "${uiState.assignedSchedules.size} scheduled collections"
            )
        }

        items(uiState.assignedSchedules) { schedule ->
            ExpandableScheduleCard(schedule)
        }

        if (uiState.assignedSchedules.isEmpty()) {
            item {
                ModernEmptyState(
                    icon = Icons.Default.Schedule,
                    title = "No Schedules Assigned",
                    subtitle = "Your assigned collection schedules will appear here"
                )
            }
        }
        
        // Collection History Section
        if (uiState.collectionRecords.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                ModernSectionHeader(
                    title = "Collection History",
                    subtitle = "${uiState.collectionRecords.size} completed collections"
                )
            }

            items(uiState.collectionRecords) { record ->
                ModernCollectionRecordCard(record)
            }
        }
    }
}

@Composable
fun PerformanceTab(uiState: DriverUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "Performance Overview",
                subtitle = "Track your collection efficiency and metrics"
            )
        }

        item {
            ModernDailyPerformanceCard(uiState.dailyStats)
        }

        item {
            ModernWeeklyOverviewCard(uiState.dailyStats)
        }

        item {
            ModernEfficiencyMetricsCard(uiState.dailyStats)
        }
    }
}

// Supporting Composables
@Composable
fun ModernRouteHeaderCard(route: RouteAssignment, schedule: Schedule, viewModel: DriverViewModel) {
    ModernCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
                        color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${route.totalStops} stops • ${route.estimatedDuration}",
                            style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        RouteStatus.ASSIGNED -> WarningColor
                        RouteStatus.IN_PROGRESS -> InfoColor
                        RouteStatus.COMPLETED -> SuccessColor
                        RouteStatus.CANCELLED -> ErrorColor
                    }
                )
            }

            if (route.status == RouteStatus.ASSIGNED) {
                Spacer(modifier = Modifier.height(12.dp))
                ModernPrimaryButton(
                    text = "Start Route",
                    onClick = { viewModel.startSchedule() },
                    icon = Icons.Default.PlayArrow
                )
            }
        }
    }
}

@Composable
fun ModernRouteProgressCard(route: RouteAssignment) {
    ModernCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                        .size(40.dp)
                                .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                        Icons.Default.TrendingUp,
                                contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                    text = "Route Progress",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            LinearProgressIndicator(
                progress = { route.progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${route.stops.count { it.isCompleted }} of ${route.totalStops} completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${route.progress}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ModernNavigationCard(route: RouteAssignment, viewModel: DriverViewModel, onNavigateClicked: () -> Unit) {
    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                )
                }
                Text(
                    text = "In-App Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            val nextStop = route.stops.firstOrNull { !it.isCompleted }
            if (nextStop != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Next Stop: ${nextStop.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = nextStop.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                ModernPrimaryButton(
                    text = "Open Navigation",
                    onClick = { 
                        viewModel.startNavigation()
                        onNavigateClicked()
                    },
                    icon = Icons.Default.Map,
                    trailingIcon = Icons.Default.ArrowForward
                )
            } else {
                ModernMessageCard(
                    message = "All stops completed!",
                    type = MessageType.SUCCESS,
                    onDismiss = { }
                )
            }
        }
    }
}

@Composable
fun ModernRouteStopCard(stop: RouteStop, index: Int, viewModel: DriverViewModel) {
    var showProofDialog by remember { mutableStateOf(false) }
    var showIssueDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (stop.isCompleted) 
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (stop.isCompleted) 
                            Color(0xFF4CAF50)
                        else Color(0xFF9E9E9E),
                        modifier = Modifier.size(32.dp)
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
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = stop.order.toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stop.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = stop.address,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                        if (stop.isCompleted && stop.completedAt != null) {
                            Text(
                                text = "Completed at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(stop.completedAt))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF4CAF50),
                                fontWeight = FontWeight.Medium
                            )
                        }
                }
            }

            if (!stop.isCompleted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                        onClick = { showProofDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Camera,
                                contentDescription = "Complete collection",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                        onClick = { showIssueDialog = true },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFFF9800), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Report issue",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProofDialog) {
        ProofOfCollectionDialog(
            onDismiss = { showProofDialog = false },
            onConfirm = { proof ->
                viewModel.completeCollection(index, notes = proof)
                showProofDialog = false
            }
        )
    }

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
fun ProofOfCollectionDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var proof by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Confirm Collection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Please confirm the collection and add any notes:",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextField(
                    value = proof,
                    onValueChange = { proof = it },
                    label = { Text("Collection notes (optional)") },
                    placeholder = { Text("e.g., Bin was full, collected 2 bags...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(proof) },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Complete Collection")
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
fun IssueReportDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var issue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "Report Issue",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Please describe the issue with this collection point:",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextField(
                    value = issue,
                    onValueChange = { issue = it },
                    label = { Text("Issue description") },
                    placeholder = { Text("e.g., Access blocked, bin missing, contamination...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(issue) },
                enabled = issue.isNotBlank(),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Report Issue")
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
fun ModernCollectionRecordCard(record: CollectionRecord) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header - Schedule Date and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                SuccessColor.copy(alpha = 0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    
                    Column {
                        Text(
                            text = dateFormatter.format(record.schedule.date.toDate()),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${record.totalStops} TPS locations • ${record.duration}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = record.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = SuccessColor
                        )
                    }
                    
                    // Expand/Collapse button
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Hide Details" else "Show Details",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Expandable Route Details
            if (isExpanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Route Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // TPS Route Stops (limit to first 3)
                    val visibleStops = record.schedule.tpsRoute.take(3)
                    
                    visibleStops.forEachIndexed { index, tpsId ->
                        val tps = record.tpsStops.find { it.tpsId == tpsId }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(
                                        SuccessColor.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessColor
                                )
                            }
                            
                            Text(
                                text = tps?.name ?: "TPS Stop ${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                modifier = Modifier.size(12.dp),
                                tint = SuccessColor
                            )
                        }
                    }
                    
                    if (record.schedule.tpsRoute.size > 3) {
                        Text(
                            text = "+ ${record.schedule.tpsRoute.size - 3} more stops",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 26.dp)
                        )
                    }
                    
                    // Completion info
                    Text(
                        text = "Completed at ${timeFormatter.format(Date(record.schedule.completedAt ?: System.currentTimeMillis()))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ModernDailyPerformanceCard(stats: DailyStats) {
    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                        .size(40.dp)
                            .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                    text = "Today's Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ModernPerformanceMetric(
                    label = "Routes",
                    value = "${stats.routesCompleted}/${stats.totalRoutes}"
                )
                ModernPerformanceMetric(
                    label = "Efficiency",
                    value = "${stats.efficiency}%"
                )
                ModernPerformanceMetric(
                    label = "Hours",
                    value = "${stats.hoursWorked}h"
                )
            }
        }
    }
}

@Composable
fun ModernWeeklyOverviewCard(stats: DailyStats) {
    ModernCard {
                Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                        .size(40.dp)
                            .background(
                            InfoColor.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = InfoColor,
                        modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                    text = "Weekly Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ModernPerformanceMetric(
                    label = "Total Routes",
                    value = "${stats.totalCompletedRoutes}"
                )
                ModernPerformanceMetric(
                    label = "Avg Efficiency",
                    value = "${stats.efficiency}%"
                )
                ModernPerformanceMetric(
                    label = "Total Hours",
                    value = "${(stats.hoursWorked * 7).roundToInt()}h"
                )
            }
        }
    }
}

@Composable
fun ModernEfficiencyMetricsCard(stats: DailyStats) {
    ModernCard {
                Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                        .size(40.dp)
                            .background(
                            SuccessColor.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = SuccessColor,
                        modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                    text = "Performance Metrics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            // Efficiency bar
            ModernMetricBar(
                label = "Efficiency",
                progress = stats.efficiency / 100f,
                value = "${stats.efficiency}%",
                color = if (stats.efficiency >= 80) SuccessColor else WarningColor
            )
            
            // Completion rate
            val completionRate = if (stats.totalRoutes > 0) (stats.routesCompleted * 100) / stats.totalRoutes else 0
            ModernMetricBar(
                label = "Completion",
                progress = completionRate / 100f,
                value = "${completionRate}%",
                color = if (completionRate >= 90) SuccessColor else WarningColor
            )
        }
    }
}

@Composable
private fun ModernPerformanceMetric(
    label: String,
    value: String
            ) {
                Column {
                    Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun WeeklyMetric(
    label: String,
    value: String
) {
                Column {
                    Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666)
                    )
                    Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun MetricBar(
    label: String,
    progress: Float,
    value: String,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
                )
                Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }
        LinearProgressIndicator(
            progress = { progress },
                modifier = Modifier.fillMaxWidth(),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
fun ExpandableScheduleCard(schedule: Schedule) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    ModernCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header - Schedule Date and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = dateFormatter.format(schedule.date.toDate()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Show assigned date if different from schedule date
                    if (schedule.assignedDate != null) {
                        val assignedDateStr = dateFormatter.format(schedule.assignedDate.toDate())
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Start Date: $assignedDateStr",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (schedule.isRecurring) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = "Recurring",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Text(
                        text = "${schedule.tpsRoute.size} TPS locations • ${timeFormatter.format(schedule.date.toDate())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ModernStatusChip(
                        text = when (schedule.status) {
                            ScheduleStatus.PENDING -> "Pending"
                            ScheduleStatus.PENDING_APPROVAL -> "Pending Approval"
                            ScheduleStatus.APPROVED -> "Approved"
                            ScheduleStatus.ASSIGNED -> "Assigned"
                            ScheduleStatus.IN_PROGRESS -> "Active"
                            ScheduleStatus.COMPLETED -> "Completed"
                            ScheduleStatus.CANCELLED -> "Cancelled"
                        },
                        color = when (schedule.status) {
                            ScheduleStatus.PENDING -> Color(0xFF9E9E9E)
                            ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
                            ScheduleStatus.APPROVED -> InfoColor
                            ScheduleStatus.ASSIGNED -> WarningColor
                            ScheduleStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                            ScheduleStatus.COMPLETED -> SuccessColor
                            ScheduleStatus.CANCELLED -> ErrorColor
                        }
                    )
                    
                    // Expand/Collapse button
                    TextButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Hide Details" else "Show Details",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Icon(
                            if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Expandable Route Details
            if (isExpanded) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Route Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // TPS Route Stops
                    schedule.tpsRoute.forEachIndexed { index, tpsId ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Stop number
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // TPS Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "TPS Stop ${index + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ID: ${tpsId.take(8)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // Stop status indicator
                            if (schedule.status == ScheduleStatus.COMPLETED) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Completed",
                                    modifier = Modifier.size(20.dp),
                                    tint = SuccessColor
                                )
                            } else if (schedule.status == ScheduleStatus.IN_PROGRESS && index == 0) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = "Current",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Schedule Metadata
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (schedule.assignedDate != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Assigned Date:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(schedule.assignedDate.toDate()),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        
                        if (schedule.isRecurring) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Recurrence:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Repeat,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Weekly",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            if (schedule.nextOccurrence != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Next Occurrence:",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = dateFormatter.format(schedule.nextOccurrence.toDate()),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        
                        if (schedule.completedAt != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Completed:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = dateFormatter.format(Date(schedule.completedAt)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
} 