package com.bluebin.presentation.admin

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.model.*
import com.bluebin.data.repository.ScheduleRepository
import com.bluebin.data.repository.TPSRepository
import com.bluebin.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReportsAnalyticsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tpsRepository: TPSRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsAnalyticsUiState())
    val uiState: StateFlow<ReportsAnalyticsUiState> = _uiState.asStateFlow()

    fun loadAnalyticsData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Load all data
                val usersResult = userRepository.getAllUsers()
                val tpsResult = tpsRepository.getAllTPS()
                val schedulesResult = scheduleRepository.getAllSchedules()
                
                val users = usersResult.getOrNull() ?: emptyList()
                val tpsLocations = tpsResult.getOrNull() ?: emptyList()
                val schedules = schedulesResult
                
                // Calculate analytics
                val analytics = calculateSystemAnalytics(users, tpsLocations, schedules)
                
                _uiState.value = _uiState.value.copy(
                    analytics = analytics,
                    users = users,
                    tpsLocations = tpsLocations,
                    schedules = schedules,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load analytics data"
                )
            }
        }
    }

    fun exportReport() {
        viewModelScope.launch {
            try {
                // TODO: Implement report export functionality
                _uiState.value = _uiState.value.copy(
                    message = "Report export feature coming soon"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to export report: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }

    private fun calculateSystemAnalytics(
        users: List<User>,
        tpsLocations: List<TPS>,
        schedules: List<Schedule>
    ): SystemAnalytics {
        val now = System.currentTimeMillis()
        val dayInMillis = 24 * 60 * 60 * 1000
        val weekInMillis = 7 * dayInMillis
        val monthInMillis = 30 * dayInMillis
        
        val todayStart = now - (now % dayInMillis)
        val weekStart = now - weekInMillis
        val monthStart = now - monthInMillis

        // User Analytics
        val totalUsers = users.size
        val activeUsers = users.count { it.approved }
        val newUsersThisMonth = users.count { it.createdAt > monthStart }
        val pendingApprovals = users.count { !it.approved }
        
        val userRoleDistribution = users.groupBy { it.role.name }
            .mapValues { it.value.size }

        // TPS Analytics
        val totalTPS = tpsLocations.size
        val activeTPS = tpsLocations.count { it.status == TPSStatus.TIDAK_PENUH }
        val fullTPS = tpsLocations.count { it.status == TPSStatus.PENUH }
        val averageUtilization = if (totalTPS > 0) {
            (fullTPS * 100 / totalTPS)
        } else 0
        
        val tpsStatusDistribution = tpsLocations.groupBy { it.status.name }
            .mapValues { it.value.size }

        // Schedule Analytics
        val totalSchedules = schedules.size
        val schedulesToday = schedules.count { 
            it.date.toDate().time > todayStart 
        }
        val completedToday = schedules.count { 
            it.status == ScheduleStatus.COMPLETED && 
            (it.completedAt ?: 0) > todayStart 
        }
        val schedulesThisWeek = schedules.count { 
            it.date.toDate().time > weekStart 
        }
        val completedThisWeek = schedules.count { 
            it.status == ScheduleStatus.COMPLETED && 
            (it.completedAt ?: 0) > weekStart 
        }
        val completionRate = if (totalSchedules > 0) {
            schedules.count { it.status == ScheduleStatus.COMPLETED } * 100 / totalSchedules
        } else 0
        
        val scheduleStatusDistribution = schedules.groupBy { it.status.name }
            .mapValues { it.value.size }
        
        val completedSchedules = schedules.filter { it.status == ScheduleStatus.COMPLETED }
        val averageCompletionTime = if (completedSchedules.isNotEmpty()) {
            completedSchedules.mapNotNull { schedule ->
                schedule.completedAt?.let { completed ->
                    val created = schedule.createdAt
                    ((completed - created) / (1000 * 60 * 60)).toInt() // Hours
                }
            }.average().toInt()
        } else 0
        
        // Calculate on-time completion rate based on actual data
        val onTimeCompletionRate = if (completedSchedules.isNotEmpty()) {
            // For now, consider a completion "on-time" if completed within expected duration
            // This could be enhanced with actual deadline tracking
            val onTimeCompletions = completedSchedules.count { schedule ->
                val completionTime = schedule.completedAt ?: 0
                val creationTime = schedule.createdAt
                val duration = (completionTime - creationTime) / (1000 * 60 * 60) // Hours
                duration <= 8 // Assume 8 hours is the expected completion time
            }
            (onTimeCompletions * 100) / completedSchedules.size
        } else 85 // Default if no data available
        val cancelledRate = schedules.count { it.status == ScheduleStatus.CANCELLED } * 100 / 
            if (totalSchedules > 0) totalSchedules else 1

        // Performance Metrics
        val efficiencyRate = ((completionRate + onTimeCompletionRate + (100 - cancelledRate)) / 3)
        val collectionEfficiency = efficiencyRate
        val scheduleAdherence = onTimeCompletionRate
        // These would come from actual user feedback and system monitoring in a real app
        val userSatisfaction = if (completionRate > 80) 90 else if (completionRate > 60) 75 else 60
        val systemReliability = if (cancelledRate < 5) 95 else if (cancelledRate < 10) 85 else 75
        val responseTime = if (averageCompletionTime < 6) 95 else if (averageCompletionTime < 8) 85 else 75
        val overallPerformanceScore = (collectionEfficiency + scheduleAdherence + 
            userSatisfaction + systemReliability + responseTime) / 5

        // Recent Activity
        val recentActivity = generateRecentActivity(users, schedules, tpsLocations)
        
        // System Health
        val systemHealth = when {
            overallPerformanceScore >= 90 -> "Excellent"
            overallPerformanceScore >= 75 -> "Good"
            overallPerformanceScore >= 60 -> "Fair"
            else -> "Poor"
        }
        
        // Recommendations
        val recommendations = generateRecommendations(
            pendingApprovals, fullTPS, cancelledRate, onTimeCompletionRate
        )

        return SystemAnalytics(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            newUsersThisMonth = newUsersThisMonth,
            pendingApprovals = pendingApprovals,
            userRoleDistribution = userRoleDistribution,
            totalTPS = totalTPS,
            activeTPS = activeTPS,
            fullTPS = fullTPS,
            averageUtilization = averageUtilization,
            tpsStatusDistribution = tpsStatusDistribution,
            averageCollectionsPerDay = if (totalSchedules > 0) (totalSchedules / 30) else 0, // Average over 30 days
            onTimeCollectionRate = onTimeCompletionRate,
            peakUsageHours = "08:00-10:00",
            totalSchedules = totalSchedules,
            schedulesToday = schedulesToday,
            completedToday = completedToday,
            schedulesThisWeek = schedulesThisWeek,
            completedThisWeek = completedThisWeek,
            completionRate = completionRate,
            scheduleStatusDistribution = scheduleStatusDistribution,
            averageCompletionTime = averageCompletionTime,
            onTimeCompletionRate = onTimeCompletionRate,
            cancelledRate = cancelledRate,
            efficiencyRate = efficiencyRate,
            overallPerformanceScore = overallPerformanceScore,
            collectionEfficiency = collectionEfficiency,
            scheduleAdherence = scheduleAdherence,
            userSatisfaction = userSatisfaction,
            systemReliability = systemReliability,
            responseTime = responseTime,
            systemHealth = systemHealth,
            recentActivity = recentActivity,
            recommendations = recommendations
        )
    }

    private fun generateRecentActivity(
        users: List<User>,
        schedules: List<Schedule>,
        tpsLocations: List<TPS>
    ): List<ActivityLog> {
        val activities = mutableListOf<ActivityLog>()
        
        // Recent user registrations
        users.filter { it.createdAt > System.currentTimeMillis() - (24 * 60 * 60 * 1000) }
            .take(3)
            .forEach { user ->
                activities.add(
                    ActivityLog(
                        id = "user_${user.uid}",
                        description = "New user registered: ${user.name}",
                        timestamp = Date(user.createdAt),
                        icon = Icons.Default.PersonAdd,
                        type = ActivityType.INFO
                    )
                )
            }
        
        // Recent schedule completions
        schedules.filter { 
            it.status == ScheduleStatus.COMPLETED && 
            (it.completedAt ?: 0) > System.currentTimeMillis() - (24 * 60 * 60 * 1000) 
        }
            .take(3)
            .forEach { schedule ->
                activities.add(
                    ActivityLog(
                        id = "schedule_${schedule.scheduleId}",
                        description = "Schedule completed by driver ${schedule.driverId}",
                        timestamp = Date(schedule.completedAt ?: 0),
                        icon = Icons.Default.CheckCircle,
                        type = ActivityType.SUCCESS
                    )
                )
            }
        
        // Full TPS alerts
        tpsLocations.filter { it.status == TPSStatus.PENUH }
            .take(2)
            .forEach { tps ->
                activities.add(
                    ActivityLog(
                        id = "tps_${tps.tpsId}",
                        description = "TPS ${tps.name} is at full capacity",
                        timestamp = Date(),
                        icon = Icons.Default.Warning,
                        type = ActivityType.WARNING
                    )
                )
            }
        
        return activities.sortedByDescending { it.timestamp }.take(10)
    }

    private fun generateRecommendations(
        pendingApprovals: Int,
        fullTPS: Int,
        cancelledRate: Int,
        onTimeRate: Int
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (pendingApprovals > 5) {
            recommendations.add("Process ${pendingApprovals} pending user approvals to improve system adoption")
        }
        
        if (fullTPS > 0) {
            recommendations.add("Schedule immediate collection for ${fullTPS} TPS locations at full capacity")
        }
        
        if (cancelledRate > 10) {
            recommendations.add("Investigate high cancellation rate (${cancelledRate}%) and improve scheduling reliability")
        }
        
        if (onTimeRate < 80) {
            recommendations.add("Improve on-time completion rate (${onTimeRate}%) through better route optimization")
        }
        
        recommendations.add("Consider implementing predictive analytics for better capacity planning")
        recommendations.add("Regular system health monitoring is recommended for optimal performance")
        
        return recommendations
    }
}

data class ReportsAnalyticsUiState(
    val analytics: SystemAnalytics = SystemAnalytics(),
    val users: List<User> = emptyList(),
    val tpsLocations: List<TPS> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val selectedFilter: ReportFilter = ReportFilter()
) 