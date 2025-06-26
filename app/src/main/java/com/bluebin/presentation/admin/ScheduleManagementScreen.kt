package com.bluebin.presentation.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.data.model.*
import com.bluebin.data.repository.ScheduleStats
import com.bluebin.ui.components.ModernCard
import com.bluebin.ui.components.ModernSectionHeader
import com.bluebin.ui.components.ModernStatusChip
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleManagementScreen(
    onBackClick: () -> Unit,
    viewModel: ScheduleManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Auto Generate", "Pending Approval", "All Schedules", "Analytics")

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
            .background(Color(0xFFF8F9FA))
    ) {
        // Modern Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.White,
            shadowElevation = 2.dp
        ) {
            Column {
                // Status bar spacer
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF5F5F5), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF666666)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Smart Schedule Management",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "AI-powered route optimization for waste collection",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { viewModel.loadSchedules() },
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

        // Success/Error Messages
        uiState.message?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE8F5E8)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        message,
                        color = Color(0xFF2E7D32),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        uiState.error?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFEBEE)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFD32F2F)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        error,
                        color = Color(0xFFD32F2F),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> AutoGenerateTab(uiState, viewModel)
            1 -> PendingApprovalTab(uiState, viewModel)
            2 -> AllSchedulesTab(
                schedules = uiState.schedules,
                optimizedSchedules = uiState.optimizedSchedules,
                isLoading = uiState.isLoading,
                onStatusUpdate = viewModel::updateScheduleStatus,
                onDeleteSchedule = viewModel::deleteSchedule,
                onDeleteOptimizedSchedule = viewModel::deleteOptimizedSchedule
            )
            3 -> ScheduleAnalyticsTab(
                stats = uiState.stats,
                schedules = uiState.schedules,
                optimizedSchedules = uiState.optimizedSchedules,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
private fun AutoGenerateTab(
    uiState: ScheduleManagementUiState,
    viewModel: ScheduleManagementViewModel
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
                        onClick = { viewModel.generateOptimizedSchedule() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (uiState.isGenerating) {
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

        // Recent generated schedule
        uiState.lastGeneratedSchedule?.let { schedule ->
            item {
                Text(
                    "Last Generated Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }
            
            item {
                OptimizedScheduleCard(
                    schedule = schedule,
                    onApprove = { viewModel.approveOptimizedSchedule(schedule.scheduleId) },
                    onDelete = { viewModel.deleteOptimizedSchedule(schedule.scheduleId) },
                    onAssignDriver = { driverId -> 
                        viewModel.assignDriverToOptimizedSchedule(schedule.scheduleId, driverId) 
                    },
                    showActions = schedule.status == ScheduleStatus.PENDING_APPROVAL,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun PendingApprovalTab(
    uiState: ScheduleManagementUiState,
    viewModel: ScheduleManagementViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ModernSectionHeader(
                        title = "Pending Approval",
                        subtitle = if (uiState.isLoading) {
                            "Loading schedules..."
                        } else {
                            "${uiState.pendingOptimizedSchedules.size} schedules awaiting your approval"
                        }
                    )
                }
                
                IconButton(
                    onClick = { viewModel.refreshAllData() }
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = Color(0xFF4CAF50)
                    )
                }
                
                // Debug button (remove in production)
                IconButton(
                    onClick = { viewModel.debugOptimizedSchedules() }
                ) {
                    Icon(
                        Icons.Default.BugReport,
                        contentDescription = "Debug",
                        tint = Color(0xFFFF9800)
                    )
                }
                
                // Test button (remove in production)
                IconButton(
                    onClick = { viewModel.testFirestoreDirectAccess() }
                ) {
                    Icon(
                        Icons.Default.Science,
                        contentDescription = "Test",
                        tint = Color(0xFF9C27B0)
                    )
                }
            }
        }

        if (uiState.isLoading) {
            item {
                ModernLoadingIndicator()
            }
        } else if (uiState.pendingOptimizedSchedules.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.PendingActions,
                    title = "No Pending Schedules",
                    description = "All generated schedules have been processed. Generate new optimized schedules to see them here."
                )
            }
        } else {
            items(uiState.pendingOptimizedSchedules) { schedule ->
                OptimizedScheduleCard(
                    schedule = schedule,
                    onApprove = { viewModel.approveOptimizedSchedule(schedule.scheduleId) },
                    onDelete = { viewModel.deleteOptimizedSchedule(schedule.scheduleId) },
                    onAssignDriver = { driverId -> 
                        viewModel.assignDriverToOptimizedSchedule(schedule.scheduleId, driverId) 
                    },
                    showActions = true,
                    viewModel = viewModel
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
        DriverApprovalDialog(
            drivers = availableDrivers,
            onApproveAndAssign = { driver, selectedDate ->
                // Use the combined approve and assign function from ViewModel
                viewModel?.approveAndAssignDriverToSchedule(schedule.scheduleId, driver.uid, selectedDate)
                showDriverDialog = false
            },
            onDismiss = { showDriverDialog = false }
        )
    }
}

@Composable
private fun DriverApprovalDialog(
    drivers: List<User>,
    onApproveAndAssign: (User, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDriver by remember { mutableStateOf<User?>(null) }
    var selectedDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Generate date options (today + next 7 days)
    val dateOptions = remember {
        (0..7).map { daysToAdd ->
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            val displayDate = SimpleDateFormat("MMM dd, yyyy (EEEE)", Locale.getDefault()).format(calendar.time)
            date to displayDate
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Approve Schedule",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Select Driver:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(drivers) { driver ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDriver = driver },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDriver == driver) 
                                Color(0xFF4CAF50).copy(alpha = 0.1f) 
                            else 
                                Color(0xFFF5F5F5)
                        ),
                        border = if (selectedDriver == driver) 
                            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF4CAF50)) 
                        else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    driver.name.first().uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    driver.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    driver.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF666666)
                                )
                            }
                            
                            if (selectedDriver == driver) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Select Schedule Date:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                items(dateOptions) { (dateValue, displayDate) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDate = dateValue },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedDate == dateValue) 
                                Color(0xFF2196F3).copy(alpha = 0.1f) 
                            else 
                                Color(0xFFF5F5F5)
                        ),
                        border = if (selectedDate == dateValue) 
                            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF2196F3)) 
                        else null
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                displayDate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1A1A1A)
                            )
                            
                            if (selectedDate == dateValue) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedDriver?.let { driver ->
                        if (selectedDate.isNotEmpty()) {
                            onApproveAndAssign(driver, selectedDate)
                        }
                    }
                },
                enabled = selectedDriver != null && selectedDate.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Text("Approve & Assign")
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
    optimizedSchedules: List<Schedule>,
    isLoading: Boolean,
    onStatusUpdate: (String, ScheduleStatus) -> Unit,
    onDeleteSchedule: (String) -> Unit,
    onDeleteOptimizedSchedule: (String) -> Unit
) {
    if (isLoading) {
        ModernLoadingIndicator()
    } else if (schedules.isEmpty() && optimizedSchedules.isEmpty()) {
        EmptyStateCard(
            icon = Icons.Default.Schedule,
            title = "No Schedules Found",
            description = "Create your first collection schedule to get started"
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show optimized schedules first
            if (optimizedSchedules.isNotEmpty()) {
                item {
                    Text(
                        "Generated Optimized Schedules (${optimizedSchedules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(optimizedSchedules) { optimizedSchedule ->
                    OptimizedScheduleCard(
                        schedule = optimizedSchedule,
                        onApprove = { /* Handle in approval tab */ },
                        onDelete = { onDeleteOptimizedSchedule(optimizedSchedule.scheduleId) },
                        onAssignDriver = null,
                        showActions = false, // Don't show actions in all schedules tab
                        viewModel = null
                    )
                }
                
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
            
            // Show regular schedules
            if (schedules.isNotEmpty()) {
                item {
                    Text(
                        "Regular Schedules (${schedules.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(schedules) { schedule ->
                    ModernScheduleCard(
                        schedule = schedule,
                        onStatusUpdate = onStatusUpdate,
                        onDelete = onDeleteSchedule
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleAnalyticsTab(
    stats: ScheduleStats,
    schedules: List<Schedule>,
    optimizedSchedules: List<Schedule>,
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

            // Regular Schedules Overview
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
                                        Color(0xFF2196F3).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF2196F3)
                                )
                            }
                            
                            Column {
                                Text(
                                    "Regular Schedules",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    "Manual and converted schedule tracking",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                            }
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
                                title = "Pending",
                                value = stats.pendingSchedules.toString(),
                                icon = Icons.Default.PendingActions,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
                }
            }

            // Optimized Schedules Overview
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
                                        Color(0xFF4CAF50).copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color(0xFF4CAF50)
                                )
                            }
                            
                            Column {
                                Text(
                                    "AI-Optimized Schedules",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1A1A1A)
                                )
                                Text(
                                    "Generated and processed AI schedules",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                            }
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
                                title = "Pending",
                                value = stats.pendingOptimizedSchedules.toString(),
                                icon = Icons.Default.HourglassEmpty,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
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
    showStatusActions: Boolean = false
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                        text = "Driver: ${schedule.driverId}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
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
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showStatusActions || schedule.status != ScheduleStatus.COMPLETED) {
                    OutlinedButton(
                        onClick = { showStatusMenu = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Update Status", fontWeight = FontWeight.Medium)
                    }
                }
                
                if (onDelete != null) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F).copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
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