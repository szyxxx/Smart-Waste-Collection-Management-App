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
        Log.d(TAG, "DriverViewModel initialized - loading driver data")
        loadDriverData()
    }

    fun loadDriverData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentUserId = authRepository.getCurrentUserId()
                Log.d(TAG, "Loading driver data for user: $currentUserId")
                
                if (currentUserId != null) {
                    // Load driver's assigned schedules using the working method
                    Log.d(TAG, "Loading schedules directly by driver ID...")
                    val driverSchedules = try {
                        scheduleRepository.getSchedulesByDriver(currentUserId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error loading schedules by driver ID", e)
                        emptyList<Schedule>()
                    }
                    
                    Log.d(TAG, "Found ${driverSchedules.size} schedules for driver $currentUserId")
                    
                    // Debug: Print driver schedules
                    driverSchedules.forEachIndexed { index, schedule ->
                        Log.d(TAG, "Driver Schedule $index: ID=${schedule.scheduleId}, status=${schedule.status}, date=${schedule.date}")
                        Log.d(TAG, "  - TPS Route size: ${schedule.tpsRoute.size}, Generation type: ${schedule.generationType}")
                    }
                    
                    // Get current assigned schedule (ASSIGNED status)
                    val assignedSchedule = driverSchedules.find { it.status == ScheduleStatus.ASSIGNED }
                    Log.d(TAG, "Assigned schedule: ${assignedSchedule?.scheduleId ?: "None"}")
                    
                    // Get current active route (IN_PROGRESS status)
                    val activeSchedule = driverSchedules.find { it.status == ScheduleStatus.IN_PROGRESS }
                    Log.d(TAG, "Active schedule: ${activeSchedule?.scheduleId ?: "None"}")
                    
                    // Check for APPROVED schedules too (might be approved but not assigned yet)
                    val approvedSchedules = driverSchedules.filter { it.status == ScheduleStatus.APPROVED }
                    Log.d(TAG, "Approved schedules: ${approvedSchedules.size}")
                    
                    // Get today's schedules
                    val today = Calendar.getInstance()
                    val todaySchedules = driverSchedules.filter { schedule ->
                        val scheduleDate = Calendar.getInstance().apply { 
                            time = schedule.date.toDate() 
                        }
                        today.get(Calendar.YEAR) == scheduleDate.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == scheduleDate.get(Calendar.DAY_OF_YEAR)
                    }
                    Log.d(TAG, "Found ${todaySchedules.size} schedules for today")
                    
                    // Filter assigned schedules for UI - include APPROVED, ASSIGNED, and IN_PROGRESS
                    val assignedSchedulesForUI = driverSchedules.filter { 
                        it.status == ScheduleStatus.APPROVED ||
                        it.status == ScheduleStatus.ASSIGNED || 
                        it.status == ScheduleStatus.IN_PROGRESS 
                    }
                    Log.d(TAG, "Assigned schedules for UI: ${assignedSchedulesForUI.size}")
                    
                    // Load TPS locations for route details
                    val allTPS = tpsRepository.getAllTPS().getOrNull() ?: emptyList()
                    Log.d(TAG, "Loaded ${allTPS.size} TPS locations")
                    
                    // Generate route assignment from current schedule
                    val currentSchedule = activeSchedule ?: assignedSchedule ?: approvedSchedules.firstOrNull()
                    val routeAssignment = currentSchedule?.let { schedule ->
                        Log.d(TAG, "Generating route assignment for schedule: ${schedule.scheduleId}")
                        generateRouteAssignment(schedule, allTPS)
                    }
                    
                    Log.d(TAG, "Current route assignment: ${routeAssignment?.routeId}")
                    
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
                    
                    Log.d(TAG, "Successfully loaded driver data - Current schedule: ${currentSchedule?.scheduleId}, Assigned schedules: ${assignedSchedulesForUI.size}")
                } else {
                    Log.e(TAG, "Current user ID is null - user not authenticated")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading driver data", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load driver data"
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
                            message = "Schedule started successfully!"
                        )
                        
                        Log.d(TAG, "Schedule started successfully")
                        
                        // Start location tracking
                        startLocationTracking()
                        
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to start schedule"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting schedule", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to start schedule"
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
                    Log.d(TAG, "Completing collection at stop $stopIndex")
                    
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
                        currentRoute = updatedRoute
                    )
                    
                    // If all stops completed, finish the schedule
                    if (updatedRoute.status == RouteStatus.COMPLETED) {
                        finishSchedule(currentSchedule)
                    }
                    
                    Log.d(TAG, "Collection completed - Progress: ${updatedRoute.progress}%")
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
                    message = "Schedule completed successfully!"
                )
                
                // Stop location tracking
                stopLocationTracking()
                
                // Reload data to get new assignments
                loadDriverData()
                
                Log.d(TAG, "Schedule finished successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finishing schedule", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to complete schedule: ${e.message}"
            )
        }
    }

    fun reportIssue(stopIndex: Int, issue: String) {
        viewModelScope.launch {
            try {
                val currentRoute = _uiState.value.currentRoute
                if (currentRoute != null && stopIndex < currentRoute.stops.size) {
                    Log.d(TAG, "Reporting issue at stop $stopIndex: $issue")
                    
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
                    
                    // TODO: Send issue report to admin/backend
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
                    Log.d(TAG, "Updating location: lat=$latitude, lng=$longitude")
                    
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
                        Log.d(TAG, "Location updated successfully in database")
                    } else {
                        Log.e(TAG, "Failed to update location in database")
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
        // TODO: Stop GPS location tracking
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
        Log.d(TAG, "Navigation completed - returning to dashboard")
        // Refresh data to reflect route completion
        loadDriverData()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun generateRouteAssignment(schedule: Schedule, allTPS: List<TPS>): RouteAssignment {
        val stops = schedule.tpsRoute.mapIndexed { index, tpsId ->
            val tps = allTPS.find { it.tpsId == tpsId }
            RouteStop(
                id = tpsId,
                name = tps?.name ?: "TPS $tpsId",
                address = tps?.address ?: "Unknown Address",
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

    fun refreshData() {
        Log.d(TAG, "Manual refresh requested")
        loadDriverData()
    }

    fun debugDriverInfo() {
        viewModelScope.launch {
            val currentUserId = authRepository.getCurrentUserId()
            Log.d(TAG, "=== DRIVER DEBUG INFO ===")
            Log.d(TAG, "Current User ID: $currentUserId")
            Log.d(TAG, "Is User Logged In: ${authRepository.isUserLoggedIn()}")
            
            if (currentUserId != null) {
                // Check if user exists in Firestore
                val userResult = userRepository.getUserById(currentUserId)
                userResult.fold(
                    onSuccess = { user ->
                        Log.d(TAG, "User found in Firestore: ${user?.name}, Role: ${user?.role}, Approved: ${user?.approved}")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "User not found in Firestore", error)
                    }
                )
                
                // Check schedules directly
                val allSchedules = scheduleRepository.getAllSchedules()
                val assignedToDriver = allSchedules.filter { it.driverId == currentUserId }
                Log.d(TAG, "Total schedules: ${allSchedules.size}, Assigned to driver: ${assignedToDriver.size}")
                
                assignedToDriver.forEach { schedule ->
                    Log.d(TAG, "Assigned Schedule: ${schedule.scheduleId}, Status: ${schedule.status}, Date: ${schedule.date}")
                }
            }
            Log.d(TAG, "=== END DEBUG INFO ===")
        }
    }

    fun testDriverScheduleAccess() {
        viewModelScope.launch {
            Log.d(TAG, "=== COMPREHENSIVE DRIVER SCHEDULE TEST ===")
            
            // 1. Check current user authentication
            val currentUserId = authRepository.getCurrentUserId()
            val isLoggedIn = authRepository.isUserLoggedIn()
            Log.d(TAG, "1. Authentication - User ID: $currentUserId, Logged in: $isLoggedIn")
            
            if (currentUserId == null) {
                Log.e(TAG, "ERROR: User not authenticated!")
                _uiState.value = _uiState.value.copy(error = "User not authenticated")
                return@launch
            }
            
            // 2. Check user profile and approval status
            try {
                val userResult = userRepository.getUserById(currentUserId)
                userResult.fold(
                    onSuccess = { user ->
                        Log.d(TAG, "2. User Profile - Name: ${user?.name}, Role: ${user?.role}, Approved: ${user?.approved}")
                        if (user?.approved != true) {
                            Log.e(TAG, "ERROR: User not approved!")
                            _uiState.value = _uiState.value.copy(error = "User account not approved by admin")
                            return@launch
                        }
                        if (user.role != UserRole.DRIVER) {
                            Log.e(TAG, "ERROR: User is not a driver! Role: ${user.role}")
                            _uiState.value = _uiState.value.copy(error = "User is not a driver")
                            return@launch
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "ERROR: Cannot get user profile", error)
                        _uiState.value = _uiState.value.copy(error = "Cannot access user profile: ${error.message}")
                        return@launch
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Exception getting user profile", e)
                _uiState.value = _uiState.value.copy(error = "Error accessing user profile")
                return@launch
            }
            
            // 3. Test direct schedule access by driver ID
            try {
                Log.d(TAG, "3. Testing direct schedule access by driver ID...")
                val driverSchedules = scheduleRepository.getSchedulesByDriver(currentUserId)
                Log.d(TAG, "3. Direct query returned ${driverSchedules.size} schedules")
                
                driverSchedules.forEachIndexed { index, schedule ->
                    Log.d(TAG, "   Schedule $index: ${schedule.scheduleId}, Status: ${schedule.status}, Date: ${schedule.date}")
                }
                
                if (driverSchedules.isNotEmpty()) {
                    Log.d(TAG, "SUCCESS: Found schedules with direct query!")
                    
                    // Update UI with found schedules
                    val assignedSchedules = driverSchedules.filter { 
                        it.status == ScheduleStatus.APPROVED ||
                        it.status == ScheduleStatus.ASSIGNED || 
                        it.status == ScheduleStatus.IN_PROGRESS 
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        assignedSchedules = assignedSchedules,
                        currentSchedule = assignedSchedules.firstOrNull(),
                        error = null,
                        message = "Found ${assignedSchedules.size} schedules using direct query"
                    )
                    
                    Log.d(TAG, "=== TEST COMPLETED SUCCESSFULLY ===")
                    return@launch
                }
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: Direct schedule query failed", e)
            }
            
            // 4. Test general schedule access (admin-level)
            try {
                Log.d(TAG, "4. Testing general schedule access...")
                val allSchedules = scheduleRepository.getAllSchedules()
                Log.d(TAG, "4. General query returned ${allSchedules.size} total schedules")
                
                // Look for schedules that should belong to this driver
                val shouldBeDriverSchedules = allSchedules.filter { it.driverId == currentUserId }
                Log.d(TAG, "4. Found ${shouldBeDriverSchedules.size} schedules with matching driver ID")
                
                shouldBeDriverSchedules.forEachIndexed { index, schedule ->
                    Log.d(TAG, "   Matching Schedule $index: ${schedule.scheduleId}, Status: ${schedule.status}")
                    Log.d(TAG, "     Driver ID: ${schedule.driverId} (matches: ${schedule.driverId == currentUserId})")
                }
                
                if (shouldBeDriverSchedules.isNotEmpty()) {
                    Log.d(TAG, "POTENTIAL ISSUE: Schedules exist but driver query doesn't return them!")
                    Log.d(TAG, "This suggests a Firestore security rules issue.")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "ERROR: General schedule query failed", e)
                Log.e(TAG, "This confirms Firestore permission issues")
            }
            
            // 5. Check if there are any schedules in the database at all
            try {
                Log.d(TAG, "5. Checking if any schedules exist...")
                // This would require admin access to see all schedules
                // If this fails, it means there are no schedules or permission issues
            } catch (e: Exception) {
                Log.e(TAG, "5. Cannot access schedule database", e)
            }
            
            Log.d(TAG, "=== COMPREHENSIVE TEST COMPLETED ===")
            _uiState.value = _uiState.value.copy(
                error = "No schedules found. Check admin dashboard to ensure schedules are assigned to driver ID: $currentUserId"
            )
        }
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