package com.bluebin.presentation.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    val authState by authViewModel.authState.collectAsState()
    val uiState by driverViewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Dashboard", "Active Route", "Collections", "Performance")

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

    if (authState.isLoading || authState.user == null) {
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
            driverName = authState.user?.name ?: "Driver",
            isLocationTracking = uiState.isLocationTracking,
            onRefresh = { driverViewModel.refreshData() },
            onLogout = { authViewModel.signOut() }
        )

        // Tab Navigation
        ModernTabRow(
            selectedTabIndex = selectedTab,
            tabs = tabs,
            onTabSelected = { selectedTab = it }
        )

        // Content
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoadingIndicator()
            }
        } else {
            when (selectedTab) {
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
            Spacer(modifier = Modifier.height(24.dp))
            
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                    .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "🚛",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            text = "Driver Dashboard",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                        Text(
                        text = "Welcome back, $driverName!",
                            style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    // Live tracking indicator
                    if (isLocationTracking) {
                        Surface(
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SuccessColor, CircleShape)
                                )
                                Text(
                                    text = "Live Tracking",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Action buttons
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
                            Icons.AutoMirrored.Filled.ExitToApp, 
                                contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
                title = "Collection History",
                subtitle = "${uiState.collectionRecords.size} completed collections"
            )
        }

        items(uiState.collectionRecords) { record ->
            ModernCollectionRecordCard(record)
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
    ModernCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
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
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = record.routeId,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = record.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Stops: ${record.completedStops}/${record.totalStops} • Duration: ${record.duration}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            ModernStatusChip(
                text = record.status,
                color = SuccessColor
            )
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