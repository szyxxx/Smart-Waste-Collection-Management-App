package com.bluebin.presentation.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.repository.ScheduleRepository
import com.bluebin.data.repository.AuthRepository
import com.bluebin.data.repository.UserRepository
import com.bluebin.data.repository.TPSRepository
import com.bluebin.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log

@HiltViewModel
class DriverViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val tpsRepository: TPSRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DriverViewModel"
    }

    private val _uiState = MutableStateFlow(DriverUiState())
    val uiState: StateFlow<DriverUiState> = _uiState.asStateFlow()

    init {
        Log.d(TAG, "DriverViewModel initialized")
        loadDriverData()
    }

    fun loadDriverData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentUserId = authRepository.getCurrentUserId()
                Log.d(TAG, "Loading driver data for user: $currentUserId")
                
                if (currentUserId != null) {
                    // Load driver's assigned schedules
                    val driverSchedules = try {
                        scheduleRepository.getSchedulesByDriver(currentUserId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading schedules", e)
                        emptyList<Schedule>()
                    }
                    
                    Log.d(TAG, "Found ${driverSchedules.size} schedules for driver")
                    
                    // Get current assigned schedule (ASSIGNED status)
                    val assignedSchedule = driverSchedules.find { it.status == ScheduleStatus.ASSIGNED }
                    
                    // Get current active route (IN_PROGRESS status)
                    val activeSchedule = driverSchedules.find { it.status == ScheduleStatus.IN_PROGRESS }
                    
                    // Get today's schedules
                    val today = Calendar.getInstance()
                    val todaySchedules = driverSchedules.filter { schedule ->
                        val scheduleDate = Calendar.getInstance().apply { 
                            time = schedule.date.toDate() 
                        }
                        today.get(Calendar.YEAR) == scheduleDate.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == scheduleDate.get(Calendar.DAY_OF_YEAR)
                    }
                    
                    // Filter assigned schedules for UI - include APPROVED, ASSIGNED, and IN_PROGRESS
                    val assignedSchedulesForUI = driverSchedules.filter { 
                        it.status == ScheduleStatus.APPROVED ||
                        it.status == ScheduleStatus.ASSIGNED || 
                        it.status == ScheduleStatus.IN_PROGRESS 
                    }
                    
                    // Load TPS locations for route details
                    val allTPS = tpsRepository.getAllTPS().getOrNull() ?: emptyList()
                    
                    // Generate route assignment from current schedule
                    val currentSchedule = activeSchedule ?: assignedSchedule
                    val routeAssignment = currentSchedule?.let { schedule ->
                        generateRouteAssignment(schedule, allTPS)
                    }
                    
                    // Generate daily stats
                    val dailyStats = generateDailyStats(todaySchedules, driverSchedules)
                    
                    // Generate collection records
                    val collectionRecords = generateCollectionRecords(driverSchedules.filter { 
                        it.status == ScheduleStatus.COMPLETED 
                    }.take(10))
                    
                    _uiState.value = _uiState.value.copy(
                        currentRoute = routeAssignment,
                        currentSchedule = currentSchedule,
                        assignedSchedules = assignedSchedulesForUI,
                        dailyStats = dailyStats,
                        collectionRecords = collectionRecords,
                        isLoading = false,
                        error = null
                    )
                    
                    Log.d(TAG, "Successfully loaded driver data")
                } else {
                    Log.e(TAG, "User not authenticated")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please login to continue"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading driver data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load data"
                )
            }
        }
    }

    fun startSchedule() {
        viewModelScope.launch {
            try {
                val currentSchedule = _uiState.value.currentSchedule
                if (currentSchedule != null && currentSchedule.status == ScheduleStatus.ASSIGNED) {
                    Log.d(TAG, "Starting schedule: ${currentSchedule.scheduleId}")
                    
                    // Update schedule status to IN_PROGRESS
                    val success = scheduleRepository.updateScheduleStatus(currentSchedule.scheduleId, ScheduleStatus.IN_PROGRESS)
                    
                    if (success) {
                        // Update local state
                        val updatedSchedule = currentSchedule.copy(status = ScheduleStatus.IN_PROGRESS)
                        
                        // Update route assignment
                        val routeAssignment = _uiState.value.currentRoute
                        val updatedRoute = routeAssignment?.copy(
                            status = RouteStatus.IN_PROGRESS,
                            startTime = System.currentTimeMillis()
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            currentSchedule = updatedSchedule,
                            currentRoute = updatedRoute,
                            message = "Collection route started!"
                        )
                        
                        // Start location tracking
                        startLocationTracking()
                        
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to start route"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting schedule", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to start route"
                )
            }
        }
    }

    fun completeCollection(stopIndex: Int, proofPhoto: String? = null, notes: String = "") {
        viewModelScope.launch {
            try {
                val currentRoute = _uiState.value.currentRoute
                val currentSchedule = _uiState.value.currentSchedule
                
                if (currentRoute != null && currentSchedule != null && stopIndex < currentRoute.stops.size) {
                    val updatedStops = currentRoute.stops.toMutableList()
                    updatedStops[stopIndex] = updatedStops[stopIndex].copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis(),
                        proofPhoto = proofPhoto,
                        notes = notes
                    )
                    
                    val completedCount = updatedStops.count { it.isCompleted }
                    val progress = (completedCount * 100) / updatedStops.size
                    
                    val updatedRoute = currentRoute.copy(
                        stops = updatedStops,
                        progress = progress,
                        status = if (completedCount == updatedStops.size) RouteStatus.COMPLETED else RouteStatus.IN_PROGRESS
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        currentRoute = updatedRoute,
                        message = "Collection point completed!"
                    )
                    
                    // If all stops completed, finish the schedule
                    if (updatedRoute.status == RouteStatus.COMPLETED) {
                        finishSchedule(currentSchedule)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing collection", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to complete collection"
                )
            }
        }
    }

    private suspend fun finishSchedule(schedule: Schedule) {
        try {
            Log.d(TAG, "Finishing schedule: ${schedule.scheduleId}")
            
            // Update schedule status to COMPLETED
            val success = scheduleRepository.updateScheduleStatus(schedule.scheduleId, ScheduleStatus.COMPLETED)
            
            if (success) {
                // Add to collection records
                val currentRoute = _uiState.value.currentRoute
                val newRecord = CollectionRecord(
                    id = "record_${System.currentTimeMillis()}",
                    routeId = currentRoute?.routeId ?: "ROUTE_${schedule.tpsRoute.size}",
                    date = formatDate(System.currentTimeMillis()),
                    totalStops = schedule.tpsRoute.size,
                    completedStops = schedule.tpsRoute.size,
                    duration = calculateDuration(currentRoute?.startTime, System.currentTimeMillis()),
                    status = "Completed"
                )
                
                val updatedRecords = listOf(newRecord) + _uiState.value.collectionRecords
                
                _uiState.value = _uiState.value.copy(
                    collectionRecords = updatedRecords,
                    currentRoute = null,
                    currentSchedule = null,
                    message = "Route completed successfully!"
                )
                
                // Stop location tracking
                stopLocationTracking()
                
                // Reload data to get new assignments
                loadDriverData()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing schedule", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to complete route: ${e.message}"
            )
        }
    }

    fun reportIssue(stopIndex: Int, issue: String) {
        viewModelScope.launch {
            try {
                val currentRoute = _uiState.value.currentRoute
                if (currentRoute != null && stopIndex < currentRoute.stops.size) {
                    val updatedStops = currentRoute.stops.toMutableList()
                    updatedStops[stopIndex] = updatedStops[stopIndex].copy(
                        hasIssue = true,
                        notes = issue
                    )
                    
                    val updatedRoute = currentRoute.copy(stops = updatedStops)
                    _uiState.value = _uiState.value.copy(
                        currentRoute = updatedRoute,
                        message = "Issue reported successfully"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reporting issue", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to report issue"
                )
            }
        }
    }

    fun updateLocation(latitude: Double, longitude: Double, speed: Double = 0.0, heading: Double = 0.0) {
        viewModelScope.launch {
            try {
                val currentSchedule = _uiState.value.currentSchedule
                if (currentSchedule?.status == ScheduleStatus.IN_PROGRESS) {
                    val locationUpdate = DriverLocation(
                        driverId = currentSchedule.driverId,
                        scheduleId = currentSchedule.scheduleId,
                        latitude = latitude,
                        longitude = longitude,
                        timestamp = System.currentTimeMillis(),
                        speed = speed,
                        heading = heading
                    )
                    
                    // Update location in database for real-time admin tracking
                    val success = scheduleRepository.updateDriverLocation(locationUpdate)
                    
                    if (success) {
                        _uiState.value = _uiState.value.copy(
                            currentLocation = locationUpdate
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating location", e)
            }
        }
    }

    private fun startLocationTracking() {
        Log.d(TAG, "Starting location tracking")
        _uiState.value = _uiState.value.copy(isLocationTracking = true)
        // TODO: Implement GPS location tracking
    }

    private fun stopLocationTracking() {
        Log.d(TAG, "Stopping location tracking")
        
        viewModelScope.launch {
            try {
                val currentSchedule = _uiState.value.currentSchedule
                if (currentSchedule != null) {
                    // Clear driver location from database
                    scheduleRepository.clearDriverLocation(currentSchedule.driverId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing driver location", e)
            }
        }
        
        _uiState.value = _uiState.value.copy(
            isLocationTracking = false,
            currentLocation = null
        )
    }

    fun startNavigation() {
        _uiState.value = _uiState.value.copy(isNavigationActive = true)
        Log.d(TAG, "Navigation started")
    }
    
    fun stopNavigation() {
        _uiState.value = _uiState.value.copy(isNavigationActive = false)
        Log.d(TAG, "Navigation stopped")
    }
    
    fun completeNavigation() {
        _uiState.value = _uiState.value.copy(isNavigationActive = false)
        Log.d(TAG, "Navigation completed")
        // Refresh data to reflect route completion
        loadDriverData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun refreshData() {
        Log.d(TAG, "Refreshing driver data")
        loadDriverData()
    }

    private fun generateRouteAssignment(schedule: Schedule, allTPS: List<TPS>): RouteAssignment {
        val stops = schedule.tpsRoute.mapIndexed { index, tpsId ->
            val tps = allTPS.find { it.tpsId == tpsId }
            RouteStop(
                id = tpsId,
                name = tps?.name ?: "Collection Point $tpsId",
                address = tps?.address ?: "Address not available",
                latitude = tps?.location?.latitude ?: 0.0,
                longitude = tps?.location?.longitude ?: 0.0,
                estimatedTime = "10:${String.format("%02d", index * 15)}",
                isCompleted = false,
                order = index + 1
            )
        }
        
        val routeStatus = when (schedule.status) {
            ScheduleStatus.ASSIGNED -> RouteStatus.ASSIGNED
            ScheduleStatus.IN_PROGRESS -> RouteStatus.IN_PROGRESS
            ScheduleStatus.COMPLETED -> RouteStatus.COMPLETED
            else -> RouteStatus.ASSIGNED
        }
        
        return RouteAssignment(
            scheduleId = schedule.scheduleId,
            routeId = "ROUTE_${schedule.scheduleId.takeLast(6)}",
            assignedDate = schedule.date.toDate().time,
            totalStops = stops.size,
            estimatedDuration = "${(schedule.estimatedDuration.takeIf { it > 0 } ?: (stops.size * 30)).toInt()} min",
            status = routeStatus,
            stops = stops,
            progress = 0,
            startTime = if (schedule.status == ScheduleStatus.IN_PROGRESS) schedule.createdAt else null
        )
    }

    private fun generateDailyStats(todaySchedules: List<Schedule>, allSchedules: List<Schedule>): DailyStats {
        val completedToday = todaySchedules.count { it.status == ScheduleStatus.COMPLETED }
        val totalToday = todaySchedules.size
        val totalCompleted = allSchedules.count { it.status == ScheduleStatus.COMPLETED }
        val inProgressToday = todaySchedules.count { it.status == ScheduleStatus.IN_PROGRESS }
        
        return DailyStats(
            routesCompleted = completedToday,
            totalRoutes = totalToday,
            stopsCompleted = todaySchedules.filter { it.status == ScheduleStatus.COMPLETED }
                .sumOf { it.tpsRoute.size },
            hoursWorked = completedToday * 3.0, // Estimate 3 hours per route
            efficiency = if (totalToday > 0) (completedToday * 100) / totalToday else 100,
            totalCompletedRoutes = totalCompleted,
            activeRoutes = inProgressToday
        )
    }

    private fun generateCollectionRecords(completedSchedules: List<Schedule>): List<CollectionRecord> {
        return completedSchedules.map { schedule ->
            CollectionRecord(
                id = schedule.scheduleId,
                routeId = "ROUTE_${schedule.scheduleId.takeLast(6)}",
                date = formatDate(schedule.completedAt ?: schedule.createdAt),
                totalStops = schedule.tpsRoute.size,
                completedStops = schedule.tpsRoute.size,
                duration = calculateDuration(schedule.createdAt, schedule.completedAt ?: System.currentTimeMillis()),
                status = "Completed"
            )
        }
    }

    private fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    private fun calculateDuration(startTime: Long?, endTime: Long): String {
        if (startTime == null) return "0h 0m"
        val duration = endTime - startTime
        val hours = duration / (60 * 60 * 1000)
        val minutes = (duration % (60 * 60 * 1000)) / (60 * 1000)
        return "${hours}h ${minutes}m"
    }
}

data class DriverUiState(
    val currentRoute: RouteAssignment? = null,
    val currentSchedule: Schedule? = null,
    val assignedSchedules: List<Schedule> = emptyList(),
    val dailyStats: DailyStats = DailyStats(),
    val collectionRecords: List<CollectionRecord> = emptyList(),
    val currentLocation: DriverLocation? = null,
    val isLocationTracking: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isNavigationActive: Boolean = false
)

data class RouteAssignment(
    val scheduleId: String,
    val routeId: String,
    val assignedDate: Long,
    val totalStops: Int,
    val estimatedDuration: String,
    val status: RouteStatus,
    val stops: List<RouteStop>,
    val progress: Int = 0,
    val startTime: Long? = null
)

data class RouteStop(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val estimatedTime: String,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val proofPhoto: String? = null,
    val notes: String = "",
    val hasIssue: Boolean = false,
    val order: Int
)

enum class RouteStatus {
    ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED
}

data class DailyStats(
    val routesCompleted: Int = 0,
    val totalRoutes: Int = 0,
    val stopsCompleted: Int = 0,
    val hoursWorked: Double = 0.0,
    val efficiency: Int = 0,
    val totalCompletedRoutes: Int = 0,
    val activeRoutes: Int = 0
)

data class CollectionRecord(
    val id: String,
    val routeId: String,
    val date: String,
    val totalStops: Int,
    val completedStops: Int,
    val duration: String,
    val status: String
)

data class DriverLocation(
    val driverId: String,
    val scheduleId: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val speed: Double = 0.0,
    val heading: Double = 0.0
) 