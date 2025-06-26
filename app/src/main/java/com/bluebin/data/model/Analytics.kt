package com.bluebin.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.*

data class SystemAnalytics(
    // User Analytics
    val totalUsers: Int = 0,
    val activeUsers: Int = 0,
    val newUsersThisMonth: Int = 0,
    val pendingApprovals: Int = 0,
    val userRoleDistribution: Map<String, Int> = emptyMap(),
    
    // TPS Analytics
    val totalTPS: Int = 0,
    val activeTPS: Int = 0,
    val fullTPS: Int = 0,
    val averageUtilization: Int = 0,
    val tpsStatusDistribution: Map<String, Int> = emptyMap(),
    val averageCollectionsPerDay: Int = 0,
    val onTimeCollectionRate: Int = 0,
    val peakUsageHours: String = "08:00-10:00",
    
    // Schedule Analytics
    val totalSchedules: Int = 0,
    val schedulesToday: Int = 0,
    val completedToday: Int = 0,
    val schedulesThisWeek: Int = 0,
    val completedThisWeek: Int = 0,
    val completionRate: Int = 0,
    val scheduleStatusDistribution: Map<String, Int> = emptyMap(),
    val averageCompletionTime: Int = 0,
    val onTimeCompletionRate: Int = 0,
    val cancelledRate: Int = 0,
    
    // Performance Metrics
    val efficiencyRate: Int = 0,
    val overallPerformanceScore: Int = 0,
    val collectionEfficiency: Int = 0,
    val scheduleAdherence: Int = 0,
    val userSatisfaction: Int = 0,
    val systemReliability: Int = 0,
    val responseTime: Int = 0,
    
    // System Health
    val systemHealth: String = "Good",
    val recentActivity: List<ActivityLog> = emptyList(),
    val recommendations: List<String> = emptyList()
)

data class ActivityLog(
    val id: String = "",
    val description: String = "",
    val timestamp: Date = Date(),
    val icon: ImageVector = Icons.Default.Info,
    val type: ActivityType = ActivityType.INFO
)

enum class ActivityType {
    INFO, WARNING, ERROR, SUCCESS
}

data class PerformanceMetric(
    val name: String,
    val value: Int,
    val unit: String = "%",
    val trend: String = "neutral", // up, down, neutral
    val description: String = ""
)

data class ReportFilter(
    val dateRange: DateRange = DateRange.LAST_30_DAYS,
    val userRole: UserRole? = null,
    val tpsStatus: TPSStatus? = null,
    val scheduleStatus: ScheduleStatus? = null
)

enum class DateRange(val displayName: String, val days: Int) {
    LAST_7_DAYS("Last 7 Days", 7),
    LAST_30_DAYS("Last 30 Days", 30),
    LAST_90_DAYS("Last 90 Days", 90),
    LAST_YEAR("Last Year", 365),
    ALL_TIME("All Time", -1)
}

data class ExportOptions(
    val format: ExportFormat = ExportFormat.PDF,
    val includeCharts: Boolean = true,
    val includeRawData: Boolean = false,
    val dateRange: DateRange = DateRange.LAST_30_DAYS
)

enum class ExportFormat {
    PDF, CSV, XLSX
} 