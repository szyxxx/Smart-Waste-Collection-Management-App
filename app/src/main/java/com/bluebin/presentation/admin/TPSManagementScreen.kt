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
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.delay
import com.bluebin.data.model.TPS
import com.bluebin.data.model.TPSStatus
import com.bluebin.data.model.User
import com.bluebin.data.model.UserRole
import com.bluebin.ui.components.ModernCard
import com.bluebin.ui.components.ModernSectionHeader
import com.bluebin.ui.components.ModernStatusChip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TPSManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("All TPS", "Register TPS", "Assign Officers", "Analytics")

    LaunchedEffect(Unit) {
        viewModel.clearMessage()
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
                        onClick = onNavigateBack,
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
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "TPS Management",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "Monitor and manage TPS locations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    IconButton(
                        onClick = { viewModel.loadDashboardData() },
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

        // Status Messages
        uiState.message?.let { message ->
            ModernMessageCard(
                message = message,
                isError = false,
                onDismiss = { viewModel.clearMessage() }
            )
        }

        uiState.error?.let { error ->
            ModernMessageCard(
                message = error,
                isError = true,
                onDismiss = { viewModel.clearMessage() }
            )
        }

        // Loading Indicator
        if (uiState.isLoading) {
            ModernLoadingIndicator()
        }

        // Tab Content
        when (selectedTab) {
            0 -> AllTPSTab(
                tpsLocations = uiState.tpsLocations,
                onUpdateStatus = viewModel::updateTPSStatus,
                isLoading = uiState.isLoading
            )
            1 -> RegisterTPSTab(
                onRegisterTPS = { tps ->
                    viewModel.registerTPS(tps)
                },
                isLoading = uiState.isLoading
            )
            2 -> AssignOfficersTab(
                tpsLocations = uiState.tpsLocations,
                onAssignOfficer = { tpsId, officerId ->
                    viewModel.assignOfficerToTPS(tpsId, officerId)
                },
                isLoading = uiState.isLoading,
                availableOfficers = uiState.users.filter { it.role == UserRole.TPS_OFFICER && it.approved },
                isLoadingOfficers = uiState.isLoading
            )
            3 -> TPSAnalyticsTab(
                tpsLocations = uiState.tpsLocations,
                stats = uiState.stats
            )
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
                "Processing...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
            )
        }
    }
}

@Composable
private fun AllTPSTab(
    tpsLocations: List<TPS>,
    onUpdateStatus: (String, TPSStatus) -> Unit,
    isLoading: Boolean
) {
    if (tpsLocations.isEmpty()) {
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
                        .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
                Text(
                    "No TPS Locations",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    "TPS locations will appear here once added to the system",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666),
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
            items(tpsLocations) { tps ->
                ModernTPSCard(
                    tps = tps,
                    onUpdateStatus = { status -> onUpdateStatus(tps.tpsId, status) },
                    isLoading = isLoading
                )
            }
        }
    }
}

@Composable
private fun StatusOverviewTab(
    tpsLocations: List<TPS>,
    stats: DashboardStats
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Status Overview",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }

        // Status Statistics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total TPS",
                    value = stats.totalTPS.toString(),
                    icon = Icons.Default.LocationOn,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "At Capacity",
                    value = stats.fullTPS.toString(),
                    icon = Icons.Default.Warning,
                    color = if (stats.fullTPS > 0) Color(0xFFD32F2F) else Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Available",
                    value = (stats.totalTPS - stats.fullTPS).toString(),
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Capacity Rate",
                    value = "${if (stats.totalTPS > 0) ((stats.totalTPS - stats.fullTPS) * 100 / stats.totalTPS) else 100}%",
                    icon = Icons.Default.Analytics,
                    color = Color(0xFF9C27B0),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // TPS by Status
        item {
            StatusDistributionCard(tpsLocations = tpsLocations)
        }

        // Critical Alerts
        if (stats.fullTPS > 0) {
            item {
                CriticalAlertsCard(fullTPSCount = stats.fullTPS)
            }
        }
    }
}

@Composable
private fun TPSAnalyticsTab(
    tpsLocations: List<TPS>,
    stats: DashboardStats
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "TPS Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
        }

        // Location Distribution
        item {
            LocationDistributionCard(tpsLocations = tpsLocations)
        }

        // Officer Assignment
        item {
            OfficerAssignmentCard(tpsLocations = tpsLocations)
        }

        // Recent Updates
        item {
            RecentUpdatesCard(tpsLocations = tpsLocations)
        }
    }
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun ModernTPSCard(
    tps: TPS,
    onUpdateStatus: (TPSStatus) -> Unit,
    isLoading: Boolean
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    
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
                            if (tps.status == TPSStatus.PENUH) 
                                Color(0xFFD32F2F).copy(alpha = 0.1f)
                            else 
                                Color(0xFF4CAF50).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (tps.status == TPSStatus.PENUH) Icons.Default.Warning else Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (tps.status == TPSStatus.PENUH) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tps.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        text = tps.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Coordinates: ${String.format("%.6f", tps.location.latitude)}, ${String.format("%.6f", tps.location.longitude)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF999999)
                    )
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (tps.status == TPSStatus.PENUH) 
                        Color(0xFFD32F2F).copy(alpha = 0.1f)
                    else 
                        Color(0xFF4CAF50).copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (tps.status == TPSStatus.PENUH) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (tps.status == TPSStatus.PENUH) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                        )
                        Text(
                            when (tps.status) {
                                TPSStatus.PENUH -> "Full"
                                TPSStatus.TIDAK_PENUH -> "Available"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (tps.status == TPSStatus.PENUH) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                        )
                    }
                }
            }
            
            // Officer Assignment
            if (!tps.assignedOfficerId.isNullOrEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color(0xFF666666)
                    )
                    Text(
                        "Assigned Officer: ${tps.assignedOfficerId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF666666)
                    )
                }
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Last updated: ${android.text.format.DateUtils.getRelativeTimeSpanString(tps.lastUpdated)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF999999)
                )
                
                Button(
                    onClick = { showStatusDialog = true },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
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
        }
    }
    
    // Status Update Dialog
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { 
                Text(
                    "Update TPS Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Select new status for ${tps.name}:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    
                    TPSStatus.values().forEach { status ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (tps.status == status) 
                                    Color(0xFF4CAF50).copy(alpha = 0.1f) 
                                else 
                                    Color(0xFFF5F5F5)
                            ),
                            onClick = { 
                                onUpdateStatus(status)
                                showStatusDialog = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = tps.status == status,
                                    onClick = { 
                                        onUpdateStatus(status)
                                        showStatusDialog = false
                                    }
                                )
                                Column {
                                    Text(
                                        when (status) {
                                            TPSStatus.PENUH -> "Full"
                                            TPSStatus.TIDAK_PENUH -> "Available"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        when (status) {
                                            TPSStatus.PENUH -> "Needs immediate collection"
                                            TPSStatus.TIDAK_PENUH -> "Has capacity for waste"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF666666)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun StatusDistributionCard(tpsLocations: List<TPS>) {
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
            Text(
                text = "Status Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            
            val statusGroups = tpsLocations.groupBy { it.status }
            
            TPSStatus.values().forEach { status ->
                val tpsWithStatus = statusGroups[status] ?: emptyList()
                val percentage = if (tpsLocations.isNotEmpty()) {
                    (tpsWithStatus.size * 100) / tpsLocations.size
                } else 0
                
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
                            if (status == TPSStatus.PENUH) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (status == TPSStatus.PENUH) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                        )
                        Text(
                            when (status) {
                                TPSStatus.PENUH -> "Full"
                                TPSStatus.TIDAK_PENUH -> "Available"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    
                    Text(
                        "${tpsWithStatus.size} ($percentage%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF666666)
                    )
                }
            }
        }
    }
}

@Composable
private fun CriticalAlertsCard(fullTPSCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
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
                    .background(Color(0xFFD32F2F).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFFD32F2F)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Critical Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = "$fullTPSCount TPS locations are at full capacity and need immediate collection.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun LocationDistributionCard(tpsLocations: List<TPS>) {
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
            Text(
                text = "Location Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            
            val locationGroups = tpsLocations.groupBy { 
                it.address.split(",").firstOrNull()?.trim() ?: "Unknown"
            }.toList().sortedByDescending { it.second.size }
            
            if (locationGroups.isNotEmpty()) {
                locationGroups.take(5).forEach { (area, tpsList) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            area,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            "${tpsList.size} locations",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF666666)
                        )
                    }
                }
            } else {
                Text(
                    "No location data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF999999)
                )
            }
        }
    }
}

@Composable
private fun OfficerAssignmentCard(tpsLocations: List<TPS>) {
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
            Text(
                text = "Officer Assignment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            
            val assignedCount = tpsLocations.count { !it.assignedOfficerId.isNullOrEmpty() }
            val unassignedCount = tpsLocations.size - assignedCount
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            assignedCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            "Assigned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            unassignedCount.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            "Unassigned",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentUpdatesCard(tpsLocations: List<TPS>) {
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
            Text(
                text = "Recent Updates",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )
            
            val recentlyUpdated = tpsLocations
                .sortedByDescending { it.lastUpdated }
                .take(5)
            
            if (recentlyUpdated.isEmpty()) {
                Text(
                    "No recent updates",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF999999)
                )
            } else {
                recentlyUpdated.forEach { tps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                tps.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                when (tps.status) {
                                    TPSStatus.PENUH -> "Full"
                                    TPSStatus.TIDAK_PENUH -> "Available"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (tps.status == TPSStatus.PENUH) Color(0xFFD32F2F) else Color(0xFF4CAF50)
                            )
                        }
                        Text(
                            android.text.format.DateUtils.getRelativeTimeSpanString(tps.lastUpdated).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF999999)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegisterTPSTab(
    onRegisterTPS: (TPS) -> Unit,
    isLoading: Boolean
) {
    var tpsName by remember { mutableStateOf("") }
    var tpsAddress by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var selectedAddress by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var showManualInput by remember { mutableStateOf(false) }
    
    // Step management
    var currentStep by remember { mutableStateOf(1) }
    val maxSteps = 2

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                // Header with step indicator
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernSectionHeader(
                        title = "Register New TPS",
                        subtitle = "Add a new TPS location to the system"
                    )
                    
                    // Step Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(maxSteps) { index ->
                            val stepNumber = index + 1
                            val isCurrentStep = stepNumber == currentStep
                            val isCompletedStep = stepNumber < currentStep
                            
                            // Step circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        when {
                                            isCompletedStep -> Color(0xFF4CAF50)
                                            isCurrentStep -> Color(0xFF2196F3)
                                            else -> Color(0xFFE0E0E0)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isCompletedStep) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color.White
                                    )
                                } else {
                                    Text(
                                        stepNumber.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrentStep) Color.White else Color(0xFF666666)
                                    )
                                }
                            }
                            
                            // Step connector line
                            if (index < maxSteps - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .height(2.dp)
                                        .background(
                                            if (stepNumber < currentStep) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                                        )
                                )
                            }
                        }
                    }
                    
                    // Step labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            "Basic Info",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (currentStep == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentStep >= 1) Color(0xFF1A1A1A) else Color(0xFF666666),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Location",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (currentStep == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (currentStep >= 2) Color(0xFF1A1A1A) else Color(0xFF666666),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            item {
                // Step 1: Basic Information
                if (currentStep == 1) {
                    TPSBasicInfoStep(
                        tpsName = tpsName,
                        onTpsNameChange = { tpsName = it },
                        tpsAddress = tpsAddress,
                        onTpsAddressChange = { tpsAddress = it }
                    )
                }
                
                // Step 2: Location Selection
                if (currentStep == 2) {
                    TPSLocationStep(
                        selectedAddress = selectedAddress,
                        latitude = latitude,
                        longitude = longitude,
                        showManualInput = showManualInput,
                        onShowMapPicker = { showMapPicker = true },
                        onShowManualInput = { showManualInput = it },
                        onLatitudeChange = { newLat ->
                            latitude = newLat
                            if (longitude.isNotBlank()) {
                                selectedAddress = "Custom coordinates location"
                            }
                        },
                        onLongitudeChange = { newLng ->
                            longitude = newLng
                            if (latitude.isNotBlank()) {
                                selectedAddress = "Custom coordinates location"
                            }
                        }
                    )
                }
            }




        }
        
        // Floating Action Button for Step Navigation/Submit
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // Back button (only show on step 2)
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep-- },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2196F3))
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back", color = Color(0xFF2196F3))
                }
            }
            
            // Next/Submit button
            val isStep1Valid = tpsName.isNotBlank() && (selectedAddress.isNotBlank() || tpsAddress.isNotBlank())
            val isStep2Valid = latitude.isNotBlank() && longitude.isNotBlank() && 
                              latitude.toDoubleOrNull() != null && longitude.toDoubleOrNull() != null
            
            val canProceed = when (currentStep) {
                1 -> isStep1Valid
                2 -> isStep2Valid
                else -> false
            }
            
            Button(
                onClick = {
                    if (currentStep < maxSteps && canProceed) {
                        currentStep++
                    } else if (currentStep == maxSteps && canProceed && !isLoading) {
                        // Submit form
                        val finalAddress = if (selectedAddress.isNotBlank()) selectedAddress else tpsAddress
                        try {
                            val lat = latitude.toDouble()
                            val lng = longitude.toDouble()
                            
                            val newTPS = TPS(
                                name = tpsName.trim(),
                                address = finalAddress.trim(),
                                location = com.google.firebase.firestore.GeoPoint(lat, lng),
                                status = TPSStatus.TIDAK_PENUH,
                                assignedOfficerId = null
                            )
                            onRegisterTPS(newTPS)
                            
                            // Reset form after successful submission
                            tpsName = ""
                            tpsAddress = ""
                            selectedAddress = ""
                            latitude = ""
                            longitude = ""
                            showManualInput = false
                            currentStep = 1
                        } catch (e: NumberFormatException) {
                            // Handle invalid coordinates
                        }
                    }
                },
                modifier = Modifier.weight(if (currentStep > 1) 2f else 1f),
                enabled = canProceed && !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentStep == maxSteps) Color(0xFF4CAF50) else Color(0xFF2196F3),
                    disabledContainerColor = Color(0xFFE0E0E0)
                ),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isLoading && currentStep == maxSteps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Registering...")
                } else {
                    val buttonText = if (currentStep == maxSteps) "Register TPS" else "Next"
                    val buttonIcon = if (currentStep == maxSteps) Icons.Default.Check else Icons.Default.ArrowForward
                    
                    Icon(
                        buttonIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        buttonText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            }
        }
    }
    
    // Google Maps Picker Dialog
    if (showMapPicker) {
        GoogleMapsPickerDialog(
            onLocationSelected = { lat, lng, address ->
                latitude = lat.toString()
                longitude = lng.toString()
                selectedAddress = address
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
            initialLatitude = if (latitude.isNotBlank()) latitude.toDoubleOrNull() ?: -6.9175 else -6.9175,
            initialLongitude = if (longitude.isNotBlank()) longitude.toDoubleOrNull() ?: 107.6191 else 107.6191
        )
    }
}

@Composable
private fun AssignOfficersTab(
    tpsLocations: List<TPS>,
    onAssignOfficer: (String, String) -> Unit,
    isLoading: Boolean,
    availableOfficers: List<User>,
    isLoadingOfficers: Boolean
) {

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModernSectionHeader(
                title = "Officer Assignment",
                subtitle = "Assign TPS officers to manage specific locations"
            )
        }

        if (isLoadingOfficers) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                ModernLoadingIndicator()
            }
        } else if (tpsLocations.isEmpty()) {
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
                            .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color(0xFF2196F3)
                        )
                    }
                    Text(
                        "No TPS Locations",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        "Register TPS locations first before assigning officers",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tpsLocations) { tps ->
                    TPSAssignmentCard(
                        tps = tps,
                        availableOfficers = availableOfficers,
                        onAssignOfficer = { officerId -> onAssignOfficer(tps.tpsId, officerId) },
                        isLoading = isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun TPSAssignmentCard(
    tps: TPS,
    availableOfficers: List<User>,
    onAssignOfficer: (String) -> Unit,
    isLoading: Boolean
) {
    var showOfficerDropdown by remember { mutableStateOf(false) }
    val assignedOfficer = availableOfficers.find { it.uid == tps.assignedOfficerId }

    ModernCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TPS Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color(0xFF2196F3).copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tps.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        tps.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
                
                ModernStatusChip(
                    text = when (tps.status) {
                        TPSStatus.PENUH -> "Full"
                        TPSStatus.TIDAK_PENUH -> "Available"
                    },
                    color = if (tps.status == TPSStatus.PENUH) 
                        Color(0xFFD32F2F) else Color(0xFF4CAF50)
                )
            }

            Divider(color = Color(0xFFE0E0E0))

            // Officer Assignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = Color(0xFF666666)
                )
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Assigned Officer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                    
                    if (assignedOfficer != null) {
                        Text(
                            assignedOfficer.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            assignedOfficer.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666)
                        )
                    } else {
                        Text(
                            "No officer assigned",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF999999),
                            fontStyle = FontStyle.Italic
                        )
                    }
                }
                
                Box {
                    OutlinedButton(
                        onClick = { showOfficerDropdown = !showOfficerDropdown },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF4CAF50)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50))
                    ) {
                        Icon(
                            if (assignedOfficer != null) Icons.Default.Edit else Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (assignedOfficer != null) "Change" else "Assign")
                    }
                    
                    DropdownMenu(
                        expanded = showOfficerDropdown,
                        onDismissRequest = { showOfficerDropdown = false },
                        modifier = Modifier.widthIn(min = 280.dp)
                    ) {
                        availableOfficers.forEach { officer ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            officer.name,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1A1A1A)
                                        )
                                        Text(
                                            officer.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF666666)
                                        )
                                    }
                                },
                                onClick = {
                                    onAssignOfficer(officer.uid)
                                    showOfficerDropdown = false
                                },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(Color(0xFF4CAF50).copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            officer.name.first().uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF4CAF50)
                                        )
                                    }
                                },
                                trailingIcon = if (officer.uid == tps.assignedOfficerId) {
                                    {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Currently assigned",
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF4CAF50)
                                        )
                                    }
                                } else null
                            )
                        }
                        
                        if (assignedOfficer != null) {
                            Divider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Remove Assignment",
                                        color = Color(0xFFD32F2F)
                                    )
                                },
                                onClick = {
                                    onAssignOfficer("")
                                    showOfficerDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.PersonRemove,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Color(0xFFD32F2F)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoogleMapsPickerDialog(
    onLocationSelected: (Double, Double, String) -> Unit,
    onDismiss: () -> Unit,
    initialLatitude: Double = -6.9175,
    initialLongitude: Double = 107.6191
) {
    var selectedLocation by remember { mutableStateOf<LatLng?>(LatLng(initialLatitude, initialLongitude)) }
    var selectedAddress by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf(listOf<Triple<Double, Double, String>>()) }
    var showSuggestions by remember { mutableStateOf(false) }
    
    // Camera position state for the map
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(initialLatitude, initialLongitude), 12f)
    }
    
    // Handle search functionality
    LaunchedEffect(isSearching) {
        if (isSearching && searchQuery.isNotBlank()) {
            delay(1000) // Simulate search delay
            val searchResults = performMockSearch(searchQuery)
            if (searchResults != null) {
                val newLocation = LatLng(searchResults.first, searchResults.second)
                selectedLocation = newLocation
                selectedAddress = searchResults.third
                cameraPositionState.move(
                    CameraUpdateFactory.newLatLngZoom(newLocation, 16f)
                )
            }
            isSearching = false
        }
    }
    
    // Handle search suggestions
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(300) // Debounce search
            searchSuggestions = getSearchSuggestions(searchQuery)
            showSuggestions = searchSuggestions.isNotEmpty()
        } else {
            showSuggestions = false
            searchSuggestions = emptyList()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Select TPS Location",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    Text(
                        "Tap on map to select location",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF666666)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF666666)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Search Bar with suggestions
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            if (it.isBlank()) {
                                showSuggestions = false
                            }
                        },
                        label = { Text("Search location") },
                        placeholder = { Text("e.g., Bandung Wetan, Bandung") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = if (searchQuery.isNotBlank()) {
                            {
                                Row {
                                    if (isSearching) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF4285F4)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            if (!isSearching) {
                                                // Perform async search
                                                isSearching = true
                                                showSuggestions = false
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (isSearching) Icons.Default.HourglassEmpty else Icons.Default.Search,
                                            contentDescription = if (isSearching) "Searching..." else "Search",
                                            tint = Color(0xFF4285F4)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            searchQuery = ""
                                            showSuggestions = false
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = Color(0xFF666666)
                                        )
                                    }
                                }
                            }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                    
                    // Search suggestions dropdown
                    if (showSuggestions && searchSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column {
                                searchSuggestions.take(5).forEach { suggestion ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val newLocation = LatLng(suggestion.first, suggestion.second)
                                                selectedLocation = newLocation
                                                selectedAddress = suggestion.third
                                                searchQuery = suggestion.third
                                                showSuggestions = false
                                                cameraPositionState.move(
                                                    CameraUpdateFactory.newLatLngZoom(newLocation, 16f)
                                                )
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = Color(0xFF666666)
                                        )
                                        Text(
                                            suggestion.third,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF1A1A1A)
                                        )
                                    }
                                    if (suggestion != searchSuggestions.take(5).last()) {
                                        Divider(
                                            modifier = Modifier.padding(horizontal = 12.dp),
                                            color = Color(0xFFE0E0E0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Google Map
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        onMapClick = { latLng ->
                            selectedLocation = latLng
                            selectedAddress = "Lat: ${String.format("%.6f", latLng.latitude)}, Lng: ${String.format("%.6f", latLng.longitude)}"
                        },
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        // Show marker for selected location
                        selectedLocation?.let { location ->
                            Marker(
                                state = MarkerState(position = location),
                                title = "Selected TPS Location",
                                snippet = selectedAddress.ifEmpty { 
                                    "Lat: ${String.format("%.6f", location.latitude)}, Lng: ${String.format("%.6f", location.longitude)}" 
                                }
                            )
                        }
                    }
                }
                
                // Selected location info
                selectedLocation?.let { location ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "📍 Selected Location",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF4CAF50)
                            )
                            Text(
                                "Latitude: ${String.format("%.6f", location.latitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1A1A1A)
                            )
                            Text(
                                "Longitude: ${String.format("%.6f", location.longitude)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF1A1A1A)
                            )
                        }
                    }
                }

                // Quick location buttons
                Text(
                    "Quick Locations:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1A1A1A)
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedLocation?.latitude == -6.9175 && selectedLocation?.longitude == 107.6191,
                            onClick = {
                                val newLocation = LatLng(-6.9175, 107.6191)
                                selectedLocation = newLocation
                                selectedAddress = "Bandung Wetan, Bandung"
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(newLocation, 15f)
                                )
                            },
                            label = { Text("Bandung Wetan", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedLocation?.latitude == -6.9147 && selectedLocation?.longitude == 107.6098,
                            onClick = {
                                val newLocation = LatLng(-6.9147, 107.6098)
                                selectedLocation = newLocation
                                selectedAddress = "Coblong, Bandung"
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(newLocation, 15f)
                                )
                            },
                            label = { Text("Coblong", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedLocation?.latitude == -6.9389 && selectedLocation?.longitude == 107.6233,
                            onClick = {
                                val newLocation = LatLng(-6.9389, 107.6233)
                                selectedLocation = newLocation
                                selectedAddress = "Antapani, Bandung"
                                cameraPositionState.move(
                                    CameraUpdateFactory.newLatLngZoom(newLocation, 15f)
                                )
                            },
                            label = { Text("Antapani", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // Instructions
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF2196F3).copy(alpha = 0.1f)
                    )
                ) {
                    Text(
                        "💡 Tap anywhere on the map to select a location, or use the quick location buttons above",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1565C0)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedLocation?.let { location ->
                        val finalAddress = selectedAddress.ifEmpty {
                            "Location at ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                        }
                        onLocationSelected(location.latitude, location.longitude, finalAddress)
                    }
                },
                enabled = selectedLocation != null,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Location")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Cancel", color = Color(0xFF666666))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

// Mock search function that simulates Google Places API search
private fun performMockSearch(query: String): Triple<Double, Double, String>? {
    val normalizedQuery = query.lowercase().trim()
    
    // Comprehensive Bandung locations database
    val locations = mapOf(
        // Bandung City Districts
        "bandung wetan" to Triple(-6.9175, 107.6191, "Bandung Wetan, Bandung"),
        "bandung kulon" to Triple(-6.9147, 107.5833, "Bandung Kulon, Bandung"),
        "bandung kidul" to Triple(-6.9389, 107.6233, "Bandung Kidul, Bandung"),
        "bojongloa kaler" to Triple(-6.9389, 107.5833, "Bojongloa Kaler, Bandung"),
        "bojongloa kidul" to Triple(-6.9508, 107.5833, "Bojongloa Kidul, Bandung"),
        "astana anyar" to Triple(-6.9319, 107.5889, "Astana Anyar, Bandung"),
        "regol" to Triple(-6.9319, 107.6056, "Regol, Bandung"),
        "lengkong" to Triple(-6.9319, 107.6191, "Lengkong, Bandung"),
        "bandung tengah" to Triple(-6.9175, 107.6056, "Bandung Tengah, Bandung"),
        "cibeunying kaler" to Triple(-6.9009, 107.6233, "Cibeunying Kaler, Bandung"),
        "cibeunying kidul" to Triple(-6.9175, 107.6233, "Cibeunying Kidul, Bandung"),
        "coblong" to Triple(-6.9147, 107.6098, "Coblong, Bandung"),
        "andir" to Triple(-6.9175, 107.5972, "Andir, Bandung"),
        "cicendo" to Triple(-6.9061, 107.5972, "Cicendo, Bandung"),
        "batununggal" to Triple(-6.9508, 107.6191, "Batununggal, Bandung"),
        "sukajadi" to Triple(-6.8915, 107.6098, "Sukajadi, Bandung"),
        "sukasari" to Triple(-6.8677, 107.6026, "Sukasari, Bandung"),
        "cidadap" to Triple(-6.8677, 107.5972, "Cidadap, Bandung"),
        "cihampelas" to Triple(-6.8915, 107.5972, "Cihampelas, Bandung"),
        "arcamanik" to Triple(-6.9147, 107.6636, "Arcamanik, Bandung"),
        "antapani" to Triple(-6.9389, 107.6233, "Antapani, Bandung"),
        "mandalajati" to Triple(-6.8915, 107.6636, "Mandalajati, Bandung"),
        "kiaracondong" to Triple(-6.9508, 107.6636, "Kiaracondong, Bandung"),
        "gedebage" to Triple(-6.9508, 107.7097, "Gedebage, Bandung"),
        "cinambo" to Triple(-6.8677, 107.6636, "Cinambo, Bandung"),
        "ujungberung" to Triple(-6.9175, 107.7097, "Ujungberung, Bandung"),
        "rancasari" to Triple(-6.9678, 107.6636, "Rancasari, Bandung"),
        "buahbatu" to Triple(-6.9678, 107.6233, "Buahbatu, Bandung"),
        "panyileukan" to Triple(-6.8677, 107.6833, "Panyileukan, Bandung"),
        
        // Popular Areas & Landmarks
        "dago" to Triple(-6.8740, 107.6098, "Dago, Bandung"),
        "lembang" to Triple(-6.8118, 107.6182, "Lembang, Bandung"),
        "cihampelas" to Triple(-6.8915, 107.5972, "Jl. Cihampelas, Bandung"),
        "braga" to Triple(-6.9175, 107.6098, "Jl. Braga, Bandung"),
        "asia afrika" to Triple(-6.9219, 107.6056, "Jl. Asia Afrika, Bandung"),
        "setiabudhi" to Triple(-6.8677, 107.5972, "Jl. Setiabudhi, Bandung"),
        "dipatiukur" to Triple(-6.8915, 107.6098, "Jl. Dipatiukur, Bandung"),
        "pasteur" to Triple(-6.8915, 107.5833, "Jl. Pasteur, Bandung"),
        "surapati" to Triple(-6.9009, 107.6098, "Jl. Surapati, Bandung"),
        "sudirman" to Triple(-6.9175, 107.6056, "Jl. Sudirman, Bandung"),
        
        // Shopping Centers & Malls
        "paris van java" to Triple(-6.8915, 107.5972, "Paris Van Java Mall, Bandung"),
        "cihampelas walk" to Triple(-6.8915, 107.5972, "Cihampelas Walk, Bandung"),
        "bandung indah plaza" to Triple(-6.9175, 107.6056, "Bandung Indah Plaza, Bandung"),
        "kings shopping center" to Triple(-6.9175, 107.6098, "Kings Shopping Center, Bandung"),
        "istana plaza" to Triple(-6.9175, 107.6056, "Istana Plaza, Bandung"),
        "festival citylink" to Triple(-6.9175, 107.6233, "Festival CityLink, Bandung"),
        "trans studio mall" to Triple(-6.9319, 107.6636, "Trans Studio Mall Bandung"),
        "taman super" to Triple(-6.9175, 107.6056, "Taman Super, Bandung"),
        "alun alun" to Triple(-6.9219, 107.6056, "Alun-Alun Bandung"),
        
        // Educational Institutions
        "itb bandung" to Triple(-6.8915, 107.6107, "Institut Teknologi Bandung"),
        "unpad" to Triple(-6.9175, 107.6191, "Universitas Padjadjaran, Bandung"),
        "unpas" to Triple(-6.9389, 107.6233, "Universitas Pasundan, Bandung"),
        "upi" to Triple(-6.8626, 107.6098, "Universitas Pendidikan Indonesia"),
        "itenas" to Triple(-6.9061, 107.5972, "Institut Teknologi Nasional"),
        "unisba" to Triple(-6.8915, 107.5972, "Universitas Islam Bandung"),
        "unikom" to Triple(-6.9319, 107.6098, "Universitas Komputer Indonesia"),
        "tel-u" to Triple(-6.9736, 107.6304, "Telkom University"),
        
        // Tourist Attractions
        "gedung sate" to Triple(-6.9009, 107.6191, "Gedung Sate, Bandung"),
        "tangkuban perahu" to Triple(-6.7597, 107.6098, "Tangkuban Perahu, Lembang"),
        "kawah putih" to Triple(-7.1661, 107.4028, "Kawah Putih, Ciwidey"),
        "farmhouse lembang" to Triple(-6.8118, 107.6182, "Farmhouse Susu Lembang"),
        "floating market" to Triple(-6.8118, 107.6182, "Floating Market Lembang"),
        "saung angklung udjo" to Triple(-6.9319, 107.6636, "Saung Angklung Udjo"),
        "taman hutan raya" to Triple(-6.8740, 107.6304, "Taman Hutan Raya Ir. H. Djuanda"),
        "taman film" to Triple(-6.9009, 107.6098, "Taman Film, Bandung"),
        "villa isola" to Triple(-6.8740, 107.6098, "Villa Isola, Bandung"),
        "cipaganti" to Triple(-6.8915, 107.6026, "Cipaganti, Bandung"),
        
        // Transportation Hubs
        "stasiun bandung" to Triple(-6.9175, 107.6056, "Stasiun Bandung (Hall)"),
        "stasiun cicalengka" to Triple(-6.9736, 107.6304, "Stasiun Cicalengka"),
        "terminal leuwi panjang" to Triple(-6.9678, 107.5833, "Terminal Leuwi Panjang"),
        "husein sastranegara" to Triple(-6.9009, 107.5763, "Husein Sastranegara Airport"),
        
        // Hospitals & Healthcare
        "rs hasan sadikin" to Triple(-6.9009, 107.6098, "RSUP Dr. Hasan Sadikin"),
        "rs borromeus" to Triple(-6.9175, 107.6056, "RS Borromeus, Bandung"),
        "rs advent" to Triple(-6.9009, 107.6098, "RS Advent, Bandung"),
        "rs al islam" to Triple(-6.9319, 107.6233, "RS Al Islam, Bandung")
    )
    
    // Search for exact matches first
    locations[normalizedQuery]?.let { return it }
    
    // Search for partial matches
    locations.entries.find { it.key.contains(normalizedQuery) || normalizedQuery.contains(it.key) }?.let { 
        return it.value 
    }
    
    // If no match found, return a random location around Bandung center
    return Triple(
        -6.9175 + (Math.random() - 0.5) * 0.1, // Random latitude within Bandung
        107.6191 + (Math.random() - 0.5) * 0.1, // Random longitude within Bandung
        "Search result: $query, Bandung"
    )
}

// Get search suggestions as user types
private fun getSearchSuggestions(query: String): List<Triple<Double, Double, String>> {
    val normalizedQuery = query.lowercase().trim()
    
    val locations = mapOf(
        // Bandung Districts
        "bandung wetan" to Triple(-6.9175, 107.6191, "Bandung Wetan, Bandung"),
        "bandung kulon" to Triple(-6.9147, 107.5833, "Bandung Kulon, Bandung"),
        "coblong" to Triple(-6.9147, 107.6098, "Coblong, Bandung"),
        "antapani" to Triple(-6.9389, 107.6233, "Antapani, Bandung"),
        "cicendo" to Triple(-6.9061, 107.5972, "Cicendo, Bandung"),
        "sukajadi" to Triple(-6.8915, 107.6098, "Sukajadi, Bandung"),
        
        // Popular Areas
        "dago" to Triple(-6.8740, 107.6098, "Dago, Bandung"),
        "lembang" to Triple(-6.8118, 107.6182, "Lembang, Bandung"),
        "cihampelas" to Triple(-6.8915, 107.5972, "Jl. Cihampelas, Bandung"),
        "braga" to Triple(-6.9175, 107.6098, "Jl. Braga, Bandung"),
        "asia afrika" to Triple(-6.9219, 107.6056, "Jl. Asia Afrika, Bandung"),
        "setiabudhi" to Triple(-6.8677, 107.5972, "Jl. Setiabudhi, Bandung"),
        "dipatiukur" to Triple(-6.8915, 107.6098, "Jl. Dipatiukur, Bandung"),
        
        // Shopping Centers
        "paris van java" to Triple(-6.8915, 107.5972, "Paris Van Java Mall, Bandung"),
        "cihampelas walk" to Triple(-6.8915, 107.5972, "Cihampelas Walk, Bandung"),
        "bandung indah plaza" to Triple(-6.9175, 107.6056, "Bandung Indah Plaza, Bandung"),
        "trans studio mall" to Triple(-6.9319, 107.6636, "Trans Studio Mall Bandung"),
        "festival citylink" to Triple(-6.9175, 107.6233, "Festival CityLink, Bandung"),
        "alun alun" to Triple(-6.9219, 107.6056, "Alun-Alun Bandung"),
        
        // Universities
        "itb bandung" to Triple(-6.8915, 107.6107, "Institut Teknologi Bandung"),
        "unpad" to Triple(-6.9175, 107.6191, "Universitas Padjadjaran, Bandung"),
        "upi" to Triple(-6.8626, 107.6098, "Universitas Pendidikan Indonesia"),
        "tel-u" to Triple(-6.9736, 107.6304, "Telkom University"),
        
        // Tourist Attractions
        "gedung sate" to Triple(-6.9009, 107.6191, "Gedung Sate, Bandung"),
        "tangkuban perahu" to Triple(-6.7597, 107.6098, "Tangkuban Perahu, Lembang"),
        "kawah putih" to Triple(-7.1661, 107.4028, "Kawah Putih, Ciwidey"),
        "farmhouse lembang" to Triple(-6.8118, 107.6182, "Farmhouse Susu Lembang"),
        "floating market" to Triple(-6.8118, 107.6182, "Floating Market Lembang")
    )
    
    return locations.entries
        .filter { it.key.contains(normalizedQuery) || it.value.third.lowercase().contains(normalizedQuery) }
        .map { it.value }
        .sortedBy { it.third }
        .take(8)
}

@Composable
private fun TPSBasicInfoStep(
    tpsName: String,
    onTpsNameChange: (String) -> Unit,
    tpsAddress: String,
    onTpsAddressChange: (String) -> Unit
) {
    ModernCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF2196F3)
                    )
                }
                Text(
                    "TPS Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            }
            
            OutlinedTextField(
                value = tpsName,
                onValueChange = onTpsNameChange,
                label = { Text("TPS Name") },
                placeholder = { Text("e.g., TPS Bandung Wetan") },
                leadingIcon = {
                    Icon(Icons.Default.LocationOn, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
            
            OutlinedTextField(
                value = tpsAddress,
                onValueChange = onTpsAddressChange,
                label = { Text("Complete Address") },
                placeholder = { Text("e.g., Jl. Braga No. 10, Bandung Wetan") },
                leadingIcon = {
                    Icon(Icons.Default.Place, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4CAF50),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                )
            )
        }
    }
}

@Composable
private fun TPSLocationStep(
    selectedAddress: String,
    latitude: String,
    longitude: String,
    showManualInput: Boolean,
    onShowMapPicker: () -> Unit,
    onShowManualInput: (Boolean) -> Unit,
    onLatitudeChange: (String) -> Unit,
    onLongitudeChange: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Google Maps Location Picker Card
        ModernCard {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                Color(0xFF4285F4).copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = Color(0xFF4285F4)
                        )
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Location Selection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            "Choose location on Google Maps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF666666)
                        )
                    }
                    
                    Button(
                        onClick = onShowMapPicker,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4285F4)
                        )
                    ) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Map")
                    }
                }
                
                // Display selected location info
                if (latitude.isNotBlank() && longitude.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.05f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    "Location Selected",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            
                            Text(
                                "Coordinates: $latitude, $longitude",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF1A1A1A)
                            )
                            
                            if (selectedAddress.isNotBlank()) {
                                Text(
                                    "Address: $selectedAddress",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF666666)
                                )
                            }
                        }
                    }
                }
                
                // Manual coordinates input (fallback)
                if (showManualInput) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Manual Input",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF666666)
                            )
                            TextButton(
                                onClick = { onShowManualInput(false) }
                            ) {
                                Text("Hide", color = Color(0xFF666666))
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = onLatitudeChange,
                                label = { Text("Latitude") },
                                placeholder = { Text("-6.9175") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                            
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = onLongitudeChange,
                                label = { Text("Longitude") },
                                placeholder = { Text("107.6191") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4CAF50),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                    }
                } else {
                    TextButton(
                        onClick = { onShowManualInput(true) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter coordinates manually")
                    }
                }
            }
        }
    }
}