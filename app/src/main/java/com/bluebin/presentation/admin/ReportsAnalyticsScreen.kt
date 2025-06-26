package com.bluebin.presentation.admin

import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsAnalyticsScreen(
    onBackClick: () -> Unit,
    viewModel: ReportsAnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "User Analytics", "TPS Analytics", "Schedule Reports", "Performance")

    LaunchedEffect(Unit) {
        viewModel.loadAnalyticsData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Modern Header with status bar padding
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
                    
                    Text(
                        text = "Reports & Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.loadAnalyticsData() },
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
                            onClick = { viewModel.exportReport() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFF5F5F5), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.FileDownload,
                                contentDescription = "Export",
                                tint = Color(0xFF666666)
                            )
                        }
                    }
                }
            }
        }

        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> OverviewTab(
                analytics = uiState.analytics,
                isLoading = uiState.isLoading
            )
            1 -> UserAnalyticsTab(
                analytics = uiState.analytics,
                users = uiState.users,
                isLoading = uiState.isLoading
            )
            2 -> TPSAnalyticsTab(
                analytics = uiState.analytics,
                tpsLocations = uiState.tpsLocations,
                isLoading = uiState.isLoading
            )
            3 -> ScheduleReportsTab(
                analytics = uiState.analytics,
                schedules = uiState.schedules,
                isLoading = uiState.isLoading
            )
            4 -> PerformanceTab(
                analytics = uiState.analytics,
                isLoading = uiState.isLoading
            )
        }
    }
}

@Composable
private fun OverviewTab(
    analytics: SystemAnalytics,
    isLoading: Boolean
) {
    if (isLoading) {
        LoadingContent()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(1) { _ ->
                Text(
                    "System Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Key Metrics Cards
            items(1) { _ ->
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(1) { _ ->
                        MetricCard(
                            title = "Total Users",
                            value = analytics.totalUsers.toString(),
                            subtitle = "+${analytics.newUsersThisMonth} this month",
                            icon = Icons.Default.People,
                            trend = if (analytics.newUsersThisMonth > 0) "up" else "neutral"
                        )
                    }
                    items(1) { _ ->
                        MetricCard(
                            title = "TPS Locations",
                            value = analytics.totalTPS.toString(),
                            subtitle = "${analytics.activeTPS} active",
                            icon = Icons.Default.LocationOn,
                            trend = "neutral"
                        )
                    }
                    items(1) { _ ->
                        MetricCard(
                            title = "Schedules Today",
                            value = analytics.schedulesToday.toString(),
                            subtitle = "${analytics.completedToday} completed",
                            icon = Icons.Default.Schedule,
                            trend = if (analytics.completedToday > analytics.schedulesToday / 2) "up" else "down"
                        )
                    }
                    items(1) { _ ->
                        MetricCard(
                            title = "Efficiency Rate",
                            value = "${analytics.efficiencyRate}%",
                            subtitle = "Last 30 days",
                            icon = Icons.Default.TrendingUp,
                            trend = if (analytics.efficiencyRate > 80) "up" else if (analytics.efficiencyRate > 60) "neutral" else "down"
                        )
                    }
                }
            }

            // Recent Activity
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Recent Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        analytics.recentActivity.forEach { activity ->
                            ActivityItem(activity = activity)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // System Health
            items(1) { _ ->
                SystemHealthCard(analytics = analytics)
            }
        }
    }
}

@Composable
private fun UserAnalyticsTab(
    analytics: SystemAnalytics,
    users: List<User>,
    isLoading: Boolean
) {
    if (isLoading) {
        LoadingContent()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(1) { _ ->
                Text(
                    "User Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // User Statistics
            items(1) { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard(
                        title = "Total Users",
                        value = analytics.totalUsers.toString(),
                        description = "All registered users",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = "Active Users",
                        value = analytics.activeUsers.toString(),
                        description = "Active this month",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            items(1) { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard(
                        title = "New Users",
                        value = analytics.newUsersThisMonth.toString(),
                        description = "This month",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = "Pending Approval",
                        value = analytics.pendingApprovals.toString(),
                        description = "Awaiting approval",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // User Role Distribution
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "User Role Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        analytics.userRoleDistribution.forEach { (role, count) ->
                            RoleDistributionItem(role = role, count = count, total = analytics.totalUsers)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // User Growth Chart (placeholder)
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "User Growth Trend",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Placeholder for chart
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "User Growth Chart",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Chart visualization would appear here",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
private fun TPSAnalyticsTab(
    analytics: SystemAnalytics,
    tpsLocations: List<TPS>,
    isLoading: Boolean
) {
    if (isLoading) {
        LoadingContent()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(1) { _ ->
                Text(
                    "TPS Analytics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // TPS Overview
            items(1) { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard(
                        title = "Total TPS",
                        value = analytics.totalTPS.toString(),
                        description = "All locations",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = "Active TPS",
                        value = analytics.activeTPS.toString(),
                        description = "Currently active",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            items(1) { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard(
                        title = "Full TPS",
                        value = analytics.fullTPS.toString(),
                        description = "At capacity",
                        modifier = Modifier.weight(1f),
                        isAlert = analytics.fullTPS > 0
                    )
                    AnalyticsCard(
                        title = "Utilization",
                        value = "${analytics.averageUtilization}%",
                        description = "Average capacity",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Collection Performance
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Collection Performance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Average Collections/Day:")
                            Text(
                                analytics.averageCollectionsPerDay.toString(),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("On-Time Collection Rate:")
                            Text(
                                "${analytics.onTimeCollectionRate}%",
                                fontWeight = FontWeight.Bold,
                                color = if (analytics.onTimeCollectionRate > 80) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Peak Usage Hours:")
                            Text(
                                analytics.peakUsageHours,
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
private fun ScheduleReportsTab(
    analytics: SystemAnalytics,
    schedules: List<Schedule>,
    isLoading: Boolean
) {
    if (isLoading) {
        LoadingContent()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(1) { _ ->
                Text(
                    "Schedule Reports",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Schedule Overview
            items(1) { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard(
                        title = "Total Schedules",
                        value = analytics.totalSchedules.toString(),
                        description = "All time",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = "Today",
                        value = analytics.schedulesToday.toString(),
                        description = "${analytics.completedToday} completed",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            items(1) { _ ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnalyticsCard(
                        title = "This Week",
                        value = analytics.schedulesThisWeek.toString(),
                        description = "${analytics.completedThisWeek} completed",
                        modifier = Modifier.weight(1f)
                    )
                    AnalyticsCard(
                        title = "Completion Rate",
                        value = "${analytics.completionRate}%",
                        description = "Last 30 days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Performance Metrics
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Performance Metrics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Average Completion Time:")
                            Text(
                                "${analytics.averageCompletionTime} hours",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("On-Time Completion:")
                            Text(
                                "${analytics.onTimeCompletionRate}%",
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Cancelled Rate:")
                            Text(
                                "${analytics.cancelledRate}%",
                                fontWeight = FontWeight.Bold,
                                color = if (analytics.cancelledRate > 10) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PerformanceTab(
    analytics: SystemAnalytics,
    isLoading: Boolean
) {
    if (isLoading) {
        LoadingContent()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(1) { _ ->
                Text(
                    "System Performance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Overall Performance Score
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Overall Performance Score",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "${analytics.overallPerformanceScore}",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "out of 100",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Performance Breakdown
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Performance Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        PerformanceMetricItem("Collection Efficiency", analytics.collectionEfficiency)
                        PerformanceMetricItem("Schedule Adherence", analytics.scheduleAdherence)
                        PerformanceMetricItem("User Satisfaction", analytics.userSatisfaction)
                        PerformanceMetricItem("System Reliability", analytics.systemReliability)
                        PerformanceMetricItem("Response Time", analytics.responseTime)
                    }
                }
            }

            // Recommendations
            items(1) { _ ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Recommendations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        analytics.recommendations.forEach { recommendation ->
                            RecommendationItem(recommendation = recommendation)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

// Helper Composables
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    trend: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(180.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                when (trend) {
                    "up" -> Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    "down" -> Icon(
                        Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun AnalyticsCard(
    title: String,
    value: String,
    description: String,
    modifier: Modifier = Modifier,
    isAlert: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) 
                MaterialTheme.colorScheme.errorContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ActivityItem(activity: ActivityLog) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            activity.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                activity.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(activity.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SystemHealthCard(analytics: SystemAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "System Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when (analytics.systemHealth) {
                        "Excellent" -> MaterialTheme.colorScheme.primary
                        "Good" -> MaterialTheme.colorScheme.tertiary
                        "Fair" -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        analytics.systemHealth,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "All systems operational. Performance within normal parameters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RoleDistributionItem(role: String, count: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(role.replace("_", " "))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "(${if (total > 0) (count * 100 / total) else 0}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun PerformanceMetricItem(name: String, score: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$score%",
                fontWeight = FontWeight.Bold,
                color = when {
                    score >= 80 -> MaterialTheme.colorScheme.primary
                    score >= 60 -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.error
                }
            )
        }
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun RecommendationItem(recommendation: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            recommendation,
            style = MaterialTheme.typography.bodyMedium
        )
    }
} 