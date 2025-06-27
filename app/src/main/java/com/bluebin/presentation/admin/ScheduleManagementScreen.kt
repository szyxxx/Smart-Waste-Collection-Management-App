package com.bluebin.presentation.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.data.model.*
import com.bluebin.data.repository.ScheduleStats
import com.bluebin.ui.components.ModernCard
import com.bluebin.ui.components.ModernSectionHeader
import com.bluebin.ui.components.ModernStatusChip
import com.bluebin.ui.components.ModernTabRow
import com.bluebin.ui.components.ModernCalendarDialog
import com.bluebin.ui.theme.SuccessColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleManagementScreen(
    onBackClick: () -> Unit,
    onNavigateToRouteDetails: (String) -> Unit = {},
    viewModel: ScheduleManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf("Generate", "Pending", "All", "Analytics")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadSchedules()
    }

    // Handle messages and errors
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            // Clear message after 3 seconds
            kotlinx.coroutines.delay(3000)
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header with gradient
        ModernScheduleHeader(
            onBackClick = onBackClick,
            onRefresh = { viewModel.loadSchedules() }
        )

        // Success/Error Messages
        uiState.message?.let { message ->
            ModernMessageCard(
                message = message,
                isError = false,
                onDismiss = { viewModel.clearError() }
            )
        }

        uiState.error?.let { error ->
            ModernMessageCard(
                message = error,
                isError = true,
                onDismiss = { viewModel.clearError() }
            )
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

        // Swipeable Content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> AutoGenerateTab(
                    isLoading = uiState.isGenerating,
                    availableDrivers = emptyList(), // Load drivers separately if needed
                    tpsNeedingCollection = emptyList(), // Load TPS separately if needed
                    scheduleStats = uiState.stats,
                    onGenerateSchedules = viewModel::generateOptimizedSchedule,
                    generatedSchedules = uiState.pendingOptimizedSchedules + uiState.optimizedSchedules
                )
                1 -> PendingSchedulesTab(
                    schedules = uiState.pendingOptimizedSchedules,
                    onApproveSchedule = viewModel::approveOptimizedSchedule,
                    onRejectSchedule = viewModel::deleteOptimizedSchedule,
                    isLoading = uiState.isLoading,
                    viewModel = viewModel,
                    onNavigateToRouteDetails = onNavigateToRouteDetails
                )
                2 -> AllSchedulesTab(
                    schedules = uiState.optimizedSchedules.filter { 
                        it.status != ScheduleStatus.PENDING_APPROVAL 
                    } + uiState.schedules,
                    isLoading = uiState.isLoading,
                    viewModel = viewModel,
                    onNavigateToRouteDetails = onNavigateToRouteDetails
                )
                3 -> ScheduleAnalyticsTab(
                    schedules = uiState.schedules,
                    stats = uiState.stats,
                    isLoading = uiState.isLoading
                )
            }
        }
    }
}

@Composable
private fun ModernScheduleHeader(
    onBackClick: () -> Unit,
    onRefresh: () -> Unit
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
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), 
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "📅",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Column {
                        Text(
                            text = "Schedule Management",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Smart Scheduling",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Action buttons
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
                    text = "AI-powered route optimization",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                
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
                            text = "Schedule Control",
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

@Composable
private fun ModernMessageCard(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    val (backgroundColor, iconColor, icon) = if (isError) {
        Triple(Color(0xFFFFEBEE), Color(0xFFD32F2F), Icons.Default.Error)
    } else {
        Triple(Color(0xFFE8F5E8), Color(0xFF4CAF50), Icons.Default.CheckCircle)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor
            )
            Text(
                message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF1A1A1A)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun AutoGenerateTab(
    isLoading: Boolean,
    availableDrivers: List<User>,
    tpsNeedingCollection: List<TPS>,
    scheduleStats: ScheduleStats,
    onGenerateSchedules: () -> Unit,
    generatedSchedules: List<Schedule> = emptyList()
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ModernSectionHeader(
                title = "AI-Powered Route Optimization",
                subtitle = "Generate optimal collection schedules based on TPS status and route efficiency"
            )
        }

        item {
            ModernCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(30.dp),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Smart Schedule Generation",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                "Automatically creates optimized routes for full TPS locations using AI algorithms",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF666666)
                            )
                        }
                    }

                    // Features list
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FeatureItem(
                            icon = Icons.Default.GpsFixed,
                            title = "Real-time TPS Status",
                            description = "Only includes TPS locations that are currently full"
                        )
                        FeatureItem(
                            icon = Icons.Default.Route,
                            title = "Optimized Routing",
                            description = "Uses advanced algorithms to minimize travel time and distance"
                        )
                        FeatureItem(
                            icon = Icons.Default.Schedule,
                            title = "Priority-Based",
                            description = "Assigns priority levels based on number of full TPS locations"
                        )
                    }

                    Button(
                        onClick = onGenerateSchedules,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generating Route...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Generate Optimized Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Statistics Overview
        item {
            ModernCard {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "System Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatisticCard(
                            title = "Available Drivers",
                            value = availableDrivers.size.toString(),
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f)
                        )
                        StatisticCard(
                            title = "TPS Needing Collection",
                            value = tpsNeedingCollection.size.toString(),
                            icon = Icons.Default.LocationOn,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        // Recently Generated Schedules
        if (generatedSchedules.isNotEmpty()) {
            item {
                ModernSectionHeader(
                    title = "Recently Generated Schedules",
                    subtitle = "${generatedSchedules.size} AI-optimized schedules waiting for approval"
                )
            }
            
            items(generatedSchedules.take(3)) { schedule ->
                GeneratedSchedulePreviewCard(schedule)
            }
        }
    }
}

@Composable
private fun PendingSchedulesTab(
    schedules: List<Schedule>,
    onApproveSchedule: (String) -> Unit,
    onRejectSchedule: (String) -> Unit,
    isLoading: Boolean,
    viewModel: ScheduleManagementViewModel = hiltViewModel(),
    onNavigateToRouteDetails: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAvailableDrivers()
    }
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    } else if (schedules.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "No Pending Schedules",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "All schedules have been processed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(schedules) { schedule ->
                ModernScheduleCard(
                    schedule = schedule,
                    onStatusUpdate = { scheduleId, status ->
                        viewModel.updateScheduleStatus(scheduleId, status)
                    },
                    onDelete = { scheduleId ->
                        onRejectSchedule(scheduleId)
                    },
                    showStatusActions = true,
                    onAssignDriver = { driverId ->
                        // Assign the driver to the schedule
                        viewModel.assignDriverToSchedule(schedule.scheduleId, driverId)
                    },
                    availableDrivers = uiState.availableDrivers,
                    viewModel = viewModel,
                    onNavigateToRouteDetails = onNavigateToRouteDetails
                )
            }
        }
    }
}

@Composable
private fun OptimizedScheduleCard(
    schedule: Schedule,
    onApprove: () -> Unit,
    onDelete: () -> Unit,
    onAssignDriver: ((String) -> Unit)? = null,
    showActions: Boolean = false,
    viewModel: ScheduleManagementViewModel? = null
) {
    var showDriverDialog by remember { mutableStateOf(false) }
    var availableDrivers by remember { mutableStateOf(listOf<User>()) }

    ModernCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Optimized Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        "Generated ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(schedule.generatedAt ?: schedule.createdAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }

                ModernStatusChip(
                    text = schedule.status.name.replace("_", " "),
                    color = when (schedule.status) {
                        ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
                        ScheduleStatus.APPROVED -> Color(0xFF2196F3)
                        ScheduleStatus.ASSIGNED -> Color(0xFF4CAF50)
                        ScheduleStatus.PENDING -> Color(0xFFFF9800)
                        ScheduleStatus.IN_PROGRESS -> Color(0xFF2196F3)
                        ScheduleStatus.COMPLETED -> Color(0xFF4CAF50)
                        ScheduleStatus.CANCELLED -> Color(0xFF666666)
                    }
                )
            }

            // Route summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryCard(
                    title = "TPS Locations",
                    value = schedule.tpsRoute.size.toString(),
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Total Distance",
                    value = "${schedule.totalDistance.roundToInt()} km",
                    icon = Icons.Default.Route,
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Est. Duration",
                    value = "${schedule.estimatedDuration.roundToInt()} min",
                    icon = Icons.Default.Schedule,
                    modifier = Modifier.weight(1f)
                )
            }

            // TPS locations
            Text(
                "TPS Locations",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(schedule.tpsRoute) { tpsId ->
                    ModernStatusChip(
                        text = "TPS $tpsId",
                        color = Color(0xFFE53935)
                    )
                }
            }

            // Route segments preview
            schedule.optimizationData?.let { optimizationData ->
                if (optimizationData.routeSegments.isNotEmpty()) {
                Text(
                    "Optimized Route",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        optimizationData.routeSegments.take(3).forEach { segment ->
                        RouteSegmentItem(segment)
                    }
                        if (optimizationData.routeSegments.size > 3) {
                        Text(
                                "+${optimizationData.routeSegments.size - 3} more segments",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            modifier = Modifier.padding(start = 32.dp)
                        )
                        }
                    }
                }
            }

            // Actions
            if (showActions) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (schedule.status == ScheduleStatus.PENDING_APPROVAL) {
                        Button(
                            onClick = { 
                                viewModel?.loadAvailableDrivers { drivers ->
                                    availableDrivers = drivers
                                    showDriverDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve & Assign")
                        }
                    } else if (schedule.status == ScheduleStatus.APPROVED && onAssignDriver != null) {
                        Button(
                            onClick = { 
                                viewModel?.loadAvailableDrivers { drivers ->
                                    availableDrivers = drivers
                                    showDriverDialog = true
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2196F3)
                            )
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Assign Driver")
                        }
                    }
                    
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }

    // Driver selection and approval dialog
    if (showDriverDialog) {
        ModernDriverApprovalDialog(
            drivers = availableDrivers,
            onApproveAndAssign = { driver, selectedDate, isRecurring ->
                // Use the new combined approve and assign function with date
                viewModel?.approveAndAssignDriverWithDate(schedule.scheduleId, driver.uid, selectedDate, isRecurring)
                showDriverDialog = false
            },
            onDismiss = { showDriverDialog = false }
        )
    }
}

@Composable
private fun ModernDriverApprovalDialog(
    drivers: List<User>,
    onApproveAndAssign: (User, LocalDate, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDriver by remember { mutableStateOf<User?>(null) }
    var showCalendar by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var isRecurring by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(min = 500.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Approve & Assign Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Driver Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Select Driver:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    LazyColumn(
                        modifier = Modifier.height(200.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(drivers) { driver ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDriver = driver },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDriver == driver) 
                                        MaterialTheme.colorScheme.primaryContainer
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ),
                                border = if (selectedDriver == driver) 
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary) 
                                else null,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(
                                                if (selectedDriver == driver) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.primaryContainer, 
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            driver.name.first().uppercase(),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectedDriver == driver)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            driver.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            driver.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (selectedDriver == driver) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                // Date Selection Section
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Schedule Date:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    OutlinedButton(
                        onClick = { showCalendar = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selectedDate != null) 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                Color.Transparent
                        )
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            selectedDate?.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy")) 
                                ?: "Select Date",
                            fontWeight = if (selectedDate != null) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDriver?.let { driver ->
                        selectedDate?.let { date ->
                            onApproveAndAssign(driver, date, isRecurring)
                        }
                    }
                },
                enabled = selectedDriver != null && selectedDate != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Approve & Assign")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Calendar Dialog
    if (showCalendar) {
        ModernCalendarDialog(
            onDateSelected = { date, recurring ->
                selectedDate = date
                isRecurring = recurring
                showCalendar = false
            },
            onDismiss = { showCalendar = false },
            title = "Select Assignment Date"
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF4CAF50)
            )
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF4CAF50)
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RouteSegmentItem(segment: SimpleRouteSegment) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color(0xFF4CAF50)
        )
        
        Text(
            "${segment.from} → ${segment.to}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.weight(1f)
        )
        
        Text(
            "${segment.distanceKm.roundToInt()}km",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
        
        Text(
            "${segment.estimatedTimeMinutes.roundToInt()}min",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF666666)
        )
    }
}

@Composable
private fun AllSchedulesTab(
    schedules: List<Schedule>,
    isLoading: Boolean,
    viewModel: ScheduleManagementViewModel = hiltViewModel(),
    onNavigateToRouteDetails: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadAvailableDrivers()
    }
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
        }
    } else if (schedules.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    "No Schedules Found",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Create your first collection schedule to get started",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(schedules) { schedule ->
                ModernScheduleCard(
                    schedule = schedule,
                    onStatusUpdate = { scheduleId, status ->
                        viewModel.updateScheduleStatus(scheduleId, status)
                    },
                    onDelete = if (schedule.status == ScheduleStatus.PENDING_APPROVAL) { scheduleId ->
                        viewModel.deleteOptimizedSchedule(scheduleId)
                    } else null,
                    showStatusActions = true,
                    onAssignDriver = { driverId ->
                        // Assign the driver to the schedule
                        viewModel.assignDriverToSchedule(schedule.scheduleId, driverId)
                    },
                    availableDrivers = uiState.availableDrivers,
                    viewModel = viewModel,
                    onNavigateToRouteDetails = onNavigateToRouteDetails
                )
            }
        }
    }
}

@Composable
private fun ScheduleAnalyticsTab(
    schedules: List<Schedule>,
    stats: ScheduleStats,
    isLoading: Boolean
) {
    if (isLoading) {
        ModernLoadingIndicator()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ModernSectionHeader(
                    title = "Schedule Analytics",
                    subtitle = "Comprehensive overview of schedule performance and optimization metrics"
                )
            }

            // Combined Schedule Overview
            item {
                ModernCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Color(0xFF2196F3).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF2196F3)
                                )
                            }
                            
                            Column {
                                Text(
                                    "Schedule Overview",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    "Regular and AI-optimized schedule analytics",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                        
                        // Regular Schedules Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF2196F3)
                                )
                                Text(
                                    "Regular Schedules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnalyticsCard(
                                    title = "Total",
                                    value = stats.totalSchedules.toString(),
                                    icon = Icons.Default.Schedule,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsCard(
                                    title = "Active",
                                    value = stats.activeSchedules.toString(),
                                    icon = Icons.Default.PlayArrow,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsCard(
                                    title = "Completed",
                                    value = stats.completedSchedules.toString(),
                                    icon = Icons.Default.CheckCircle,
                                    color = Color(0xFF8BC34A),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                        
                        // Performance Metrics Section
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                                Text(
                                    "AI-Optimized Schedules",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1A1A1A)
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnalyticsCard(
                                    title = "Generated",
                                    value = stats.totalOptimizedSchedules.toString(),
                                    icon = Icons.Default.AutoAwesome,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsCard(
                                    title = "Assigned",
                                    value = stats.assignedOptimizedSchedules.toString(),
                                    icon = Icons.Default.Assignment,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                                AnalyticsCard(
                                    title = "Completed",
                                    value = stats.completedOptimizedSchedules.toString(),
                                    icon = Icons.Default.TaskAlt,
                                    color = Color(0xFF8BC34A),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Performance Metrics
            item {
                ModernCard {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        Color(0xFFFF5722).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFFFF5722)
                                )
                            }
                            
                            Column {
                                Text(
                                    "Performance Metrics",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    "Efficiency and completion statistics",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                        
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricRow(
                                label = "Completed Today",
                                value = "${stats.completedToday} schedules",
                                icon = Icons.Default.Today
                            )
                            MetricRow(
                                label = "AI Schedules Today",
                                value = "${stats.optimizedSchedulesToday} generated",
                                icon = Icons.Default.AutoAwesome
                            )
                            MetricRow(
                                label = "Regular Completion Rate",
                                value = if (stats.totalSchedules > 0) {
                                    "${(stats.completedSchedules * 100 / stats.totalSchedules)}%"
                                } else "0%",
                                icon = Icons.Default.TrendingUp
                            )
                            MetricRow(
                                label = "AI Approval Rate",
                                value = if (stats.totalOptimizedSchedules > 0) {
                                    "${((stats.approvedOptimizedSchedules + stats.assignedOptimizedSchedules + stats.completedOptimizedSchedules) * 100 / stats.totalOptimizedSchedules)}%"
                                } else "0%",
                                icon = Icons.Default.ThumbUp
                            )
                        }
                    }
                }
            }

            // Route Optimization Metrics
            if (stats.totalOptimizedSchedules > 0) {
                item {
                    ModernCard {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Color(0xFF9C27B0).copy(alpha = 0.1f),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Route,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp),
                                        tint = Color(0xFF9C27B0)
                                    )
                                }
                                
                                Column {
                                    Text(
                                        "Route Optimization",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A1A1A)
                                    )
                                    Text(
                                        "AI-generated route efficiency data",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                            
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                MetricRow(
                                    label = "Total Distance Optimized",
                                    value = "${String.format("%.1f", stats.totalOptimizedDistance)} km",
                                    icon = Icons.Default.Straighten
                                )
                                MetricRow(
                                    label = "Average Route Distance",
                                    value = "${String.format("%.1f", stats.averageOptimizedDistance)} km",
                                    icon = Icons.Default.Timeline
                                )
                                MetricRow(
                                    label = "Average TPS per Route",
                                    value = "${stats.averageTpsPerRoute} locations",
                                    icon = Icons.Default.LocationOn
                                )
                                MetricRow(
                                    label = "Efficiency Score",
                                    value = if (stats.averageOptimizedDistance > 0) "Optimized" else "N/A",
                                    icon = Icons.Default.Speed
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
private fun ModernScheduleCard(
    schedule: Schedule,
    onStatusUpdate: (String, ScheduleStatus) -> Unit,
    onDelete: ((String) -> Unit)? = null,
    showStatusActions: Boolean = false,
    onAssignDriver: ((String) -> Unit)? = null,
    availableDrivers: List<User> = emptyList(),
    viewModel: ScheduleManagementViewModel? = null,
    onNavigateToRouteDetails: (String) -> Unit = {}
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDriverAssignment by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            when (schedule.status) {
                                ScheduleStatus.PENDING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                ScheduleStatus.IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                ScheduleStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                ScheduleStatus.CANCELLED -> Color(0xFFD32F2F).copy(alpha = 0.1f)
                                ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800).copy(alpha = 0.1f)
                                ScheduleStatus.APPROVED -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                ScheduleStatus.ASSIGNED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        when (schedule.status) {
                            ScheduleStatus.PENDING -> Icons.Default.Schedule
                            ScheduleStatus.IN_PROGRESS -> Icons.Default.PlayArrow
                            ScheduleStatus.COMPLETED -> Icons.Default.CheckCircle
                            ScheduleStatus.CANCELLED -> Icons.Default.Cancel
                            ScheduleStatus.PENDING_APPROVAL -> Icons.Default.PendingActions
                            ScheduleStatus.APPROVED -> Icons.Default.CheckCircle
                            ScheduleStatus.ASSIGNED -> Icons.Default.Assignment
                        },
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = when (schedule.status) {
                            ScheduleStatus.PENDING -> Color(0xFFFF9800)
                            ScheduleStatus.IN_PROGRESS -> Color(0xFF2196F3)
                            ScheduleStatus.COMPLETED -> Color(0xFF4CAF50)
                            ScheduleStatus.CANCELLED -> Color(0xFFD32F2F)
                            ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
                            ScheduleStatus.APPROVED -> Color(0xFF2196F3)
                            ScheduleStatus.ASSIGNED -> Color(0xFF4CAF50)
                        }
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Schedule ${schedule.scheduleId.take(8)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = if (schedule.driverId.isEmpty() || schedule.driverId == "Not Assigned") 
                            "Driver: Not Assigned" 
                        else 
                            "Driver: ${schedule.driverId.take(8)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (schedule.driverId.isEmpty() || schedule.driverId == "Not Assigned") 
                            Color(0xFFFF9800) 
                        else 
                            Color(0xFF4CAF50),
                        fontWeight = if (schedule.driverId.isEmpty() || schedule.driverId == "Not Assigned") 
                            FontWeight.Medium 
                        else 
                            FontWeight.Normal
                    )
                    Text(
                        text = "Date: ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(schedule.date.toDate())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Text(
                        text = "TPS Count: ${schedule.tpsRoute.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (schedule.status) {
                        ScheduleStatus.PENDING -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        ScheduleStatus.IN_PROGRESS -> Color(0xFF2196F3).copy(alpha = 0.1f)
                        ScheduleStatus.COMPLETED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        ScheduleStatus.CANCELLED -> Color(0xFFD32F2F).copy(alpha = 0.1f)
                        ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        ScheduleStatus.APPROVED -> Color(0xFF2196F3).copy(alpha = 0.1f)
                        ScheduleStatus.ASSIGNED -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                    }
                ) {
                    Text(
                        text = schedule.status.name.replace("_", " "),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = when (schedule.status) {
                            ScheduleStatus.PENDING -> Color(0xFFFF9800)
                            ScheduleStatus.IN_PROGRESS -> Color(0xFF2196F3)
                            ScheduleStatus.COMPLETED -> Color(0xFF4CAF50)
                            ScheduleStatus.CANCELLED -> Color(0xFFD32F2F)
                            ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
                            ScheduleStatus.APPROVED -> Color(0xFF2196F3)
                            ScheduleStatus.ASSIGNED -> Color(0xFF4CAF50)
                        }
                    )
                }
            }

            if (schedule.completedAt != null) {
                Text(
                    "Completed: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(schedule.completedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
            }
            
            // Route Completion Data Section
            if (schedule.routeCompletionData.isNotEmpty()) {
                HorizontalDivider(color = Color(0xFFE0E0E0))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Collection Details",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1A1A1A)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(schedule.routeCompletionData) { completion ->
                            RouteCompletionCard(completion)
                        }
                    }
                }
            }
            
            // Action Buttons with proper responsive layout
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First row - Primary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // View Details Button
                    Button(
                        onClick = { onNavigateToRouteDetails(schedule.scheduleId) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "View Details",
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    
                    // Driver Assignment Button
                    if (onAssignDriver != null && 
                        (schedule.driverId.isEmpty() || 
                         schedule.driverId == "Not Assigned" || 
                         schedule.status == ScheduleStatus.PENDING_APPROVAL ||
                         schedule.status == ScheduleStatus.APPROVED)) {
                        Button(
                            onClick = { showDriverAssignment = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Assign Driver",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Second row - Secondary actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showStatusActions || schedule.status != ScheduleStatus.COMPLETED) {
                        OutlinedButton(
                            onClick = { showStatusMenu = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Update Status",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                    
                    // Delete button if needed
                    if (onDelete != null) {
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFD32F2F)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Delete",
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

            }
        }
    }

    // Enhanced Driver Assignment Dialog
    if (showDriverAssignment) {
        DriverAssignmentDialog(
            schedule = schedule,
            availableDrivers = availableDrivers,
            onDismiss = { showDriverAssignment = false },
            onAssignDriver = { driverId, assignmentDate, isRecurring ->
                // Use the enhanced assignment method with the passed viewModel
                viewModel?.assignDriverWithSchedule(schedule.scheduleId, driverId, assignmentDate, isRecurring)
                    ?: onAssignDriver?.invoke(driverId)
                showDriverAssignment = false
            }
        )
    }

    // Status Update Menu
    if (showStatusMenu) {
        AlertDialog(
            onDismissRequest = { showStatusMenu = false },
            title = { Text("Update Status") },
            text = {
                Column {
                    ScheduleStatus.values().forEach { status ->
                        TextButton(
                            onClick = {
                                onStatusUpdate(schedule.scheduleId, status)
                                showStatusMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(status.name.replace("_", " "))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusMenu = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Schedule") },
            text = { Text("Are you sure you want to delete this schedule? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(schedule.scheduleId)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ModernLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color(0xFF4CAF50),
                strokeWidth = 2.dp
            )
            Text(
                "Loading schedules...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                        RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color(0xFF4CAF50)
                )
            }
            
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
            
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    Color(0xFF4CAF50).copy(alpha = 0.1f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Color(0xFF4CAF50)
            )
        }
        
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF666666),
            modifier = Modifier.weight(1f)
        )
        
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun GeneratedSchedulePreviewCard(schedule: Schedule) {
    ModernCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Schedule ${schedule.scheduleId.takeLast(8)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "${schedule.tpsRoute.size} TPS locations • ${schedule.totalDistance.roundToInt()}km • ${schedule.estimatedDuration.roundToInt()}min",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
                if (schedule.generatedAt != null) {
                    Text(
                        text = "Generated ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(schedule.generatedAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }
            }
            
            ModernStatusChip(
                text = when (schedule.status) {
                    ScheduleStatus.PENDING_APPROVAL -> "Pending Approval"
                    ScheduleStatus.APPROVED -> "Approved"
                    ScheduleStatus.ASSIGNED -> "Assigned"
                    else -> schedule.status.name
                },
                color = when (schedule.status) {
                    ScheduleStatus.PENDING_APPROVAL -> Color(0xFFFF9800)
                    ScheduleStatus.APPROVED -> Color(0xFF2196F3)
                    ScheduleStatus.ASSIGNED -> Color(0xFF4CAF50)
                    else -> Color(0xFF666666)
                }
            )
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RouteCompletionCard(completion: RouteStopCompletion) {
    var showPhotoDialog by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.width(200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TPS Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF4CAF50)
                )
                Text(
                    "TPS ${completion.tpsId}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            }
            
            // Completion Time
            if (completion.completedAt != null) {
                Text(
                    "Completed: ${dateFormatter.format(Date(completion.completedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666)
                )
            }
            
            // Photo Evidence
            if (!completion.proofPhotoUrl.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF2196F3)
                    )
                    TextButton(
                        onClick = { showPhotoDialog = true },
                        modifier = Modifier.padding(0.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            "View Photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2196F3)
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFF999999)
                    )
                    Text(
                        "No Photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }
            }
            
            // Notes
            if (completion.notes.isNotEmpty()) {
                Text(
                    completion.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF666666),
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
            
            // Issue Indicator
            if (completion.hasIssue) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFFF9800)
                    )
                    Text(
                        "Issue Reported",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Photo Dialog
    if (showPhotoDialog && !completion.proofPhotoUrl.isNullOrEmpty()) {
        PhotoViewDialog(
            photoUrl = completion.proofPhotoUrl,
            onDismiss = { showPhotoDialog = false }
        )
    }
}

@Composable
private fun DriverAssignmentDialog(
    schedule: Schedule,
    availableDrivers: List<User>,
    onDismiss: () -> Unit,
    onAssignDriver: (driverId: String, assignmentDate: LocalDate, isRecurring: Boolean) -> Unit
) {
    var selectedDriver by remember { mutableStateOf<User?>(null) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var isRecurring by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    "Assign Driver & Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Driver Selection
                Text(
                    "Select Driver:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (availableDrivers.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonOff,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "No drivers available",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDrivers) { driver ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedDriver = driver },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDriver?.uid == driver.uid) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else 
                                        MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
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
                                            driver.name.first().uppercase(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            driver.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            driver.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    if (selectedDriver?.uid == driver.uid) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Date Selection
                Text(
                    "Assignment Date:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Start Date",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                selectedDate.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Weekly Repeat Option
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isRecurring = !isRecurring }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = isRecurring,
                            onCheckedChange = { isRecurring = it }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Repeat Weekly",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Schedule will repeat every week on the same day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Repeat,
                            contentDescription = null,
                            tint = if (isRecurring) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDriver?.let { driver ->
                        onAssignDriver(driver.uid, selectedDate, isRecurring)
                    }
                },
                enabled = selectedDriver != null,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Assign Schedule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        ModernCalendarDialog(
            initialDate = selectedDate,
            onDateSelected = { date, recurring ->
                selectedDate = date
                isRecurring = recurring
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
            title = "Select Assignment Date"
        )
    }
}

@Composable
private fun PhotoViewDialog(
    photoUrl: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Collection Proof Photo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Photo URL:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5)
                    )
                ) {
                    Text(
                        photoUrl,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
                Text(
                    "Note: Photo viewing functionality can be enhanced to display actual images using an image loading library like Coil.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}