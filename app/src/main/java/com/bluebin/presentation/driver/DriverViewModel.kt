package com.bluebin.presentation.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.repository.ScheduleRepository
import com.bluebin.data.repository.AuthRepository
import com.bluebin.data.repository.UserRepository
import com.bluebin.data.repository.TPSRepository
import com.bluebin.data.storage.CloudStorageService
import com.bluebin.data.model.*
import com.google.firebase.firestore.GeoPoint
import android.net.Uri
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
    private val tpsRepository: TPSRepository,
    private val cloudStorageService: CloudStorageService
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
                    val collectionRecords = generateCollectionRecords(
                        driverSchedules.filter { 
                            it.status == ScheduleStatus.COMPLETED 
                        }.take(10),
                        allTPS
                    )
                    
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
                    
                    // Check if driver can start the schedule based on assigned date
                    val canStart = scheduleRepository.canDriverStartSchedule(currentSchedule.scheduleId)
                    
                    if (!canStart) {
                        val assignedDate = currentSchedule.assignedDate?.toDate()
                        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val dateStr = assignedDate?.let { formatter.format(it) } ?: "the assigned date"
                        
                        _uiState.value = _uiState.value.copy(
                            error = "You can only start this schedule on or after $dateStr. Please wait until the assigned date."
                        )
                        return@launch
                    }
                    
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
                    val completedStop = currentRoute.stops[stopIndex]
                    
                    // Upload photo to Firebase Storage if provided
                    var cloudPhotoUrl: String? = null
                    if (!proofPhoto.isNullOrEmpty() && proofPhoto != "content://") {
                        Log.d(TAG, "Attempting to upload photo: $proofPhoto")
                        try {
                            val photoUri = Uri.parse(proofPhoto)
                            val uploadResult = cloudStorageService.uploadProofPhoto(
                                photoUri = photoUri,
                                driverId = currentSchedule.driverId,
                                scheduleId = currentSchedule.scheduleId,
                                tpsId = completedStop.id
                            )
                            
                            if (uploadResult.isSuccess) {
                                cloudPhotoUrl = uploadResult.getOrNull()
                                Log.d(TAG, "Photo processed successfully (temporary local storage): $cloudPhotoUrl")
                            } else {
                                Log.w(TAG, "Failed to process photo: ${uploadResult.exceptionOrNull()?.message}")
                                Log.d(TAG, "Continuing collection without photo storage")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception during photo processing", e)
                            // Continue without photo if processing completely fails
                            Log.d(TAG, "Continuing collection without photo storage due to exception")
                        }
                    } else {
                        Log.d(TAG, "No photo provided or invalid photo URI")
                    }
                    
                    val completedAt = System.currentTimeMillis()
                    val updatedStops = currentRoute.stops.toMutableList()
                    updatedStops[stopIndex] = updatedStops[stopIndex].copy(
                        isCompleted = true,
                        completedAt = completedAt,
                        proofPhoto = cloudPhotoUrl,
                        notes = notes
                    )
                    
                    // Store completion data in the schedule collection
                    val stopCompletion = RouteStopCompletion(
                        tpsId = completedStop.id,
                        completedAt = completedAt,
                        proofPhotoUrl = cloudPhotoUrl,
                        notes = notes,
                        hasIssue = false,
                        driverLocation = mapOf(
                            "latitude" to completedStop.latitude,
                            "longitude" to completedStop.longitude
                        )
                    )
                    
                    try {
                        val scheduleUpdateSuccess = scheduleRepository.updateScheduleStopCompletion(
                            currentSchedule.scheduleId, 
                            stopCompletion
                        )
                        if (scheduleUpdateSuccess) {
                            Log.d(TAG, "✅ Successfully stored completion data in schedule collection")
                        } else {
                            Log.w(TAG, "⚠️ Failed to store completion data in schedule collection")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error storing completion data in schedule collection", e)
                    }
                    
                    // Update TPS status to TIDAK_PENUH after collection
                    Log.d(TAG, "=== TPS STATUS UPDATE DEBUG ===")
                    Log.d(TAG, "TPS ID to update: ${completedStop.id}")
                    Log.d(TAG, "TPS Name: ${completedStop.name}")
                    Log.d(TAG, "TPS Address: ${completedStop.address}")
                    Log.d(TAG, "Stop Index: $stopIndex / ${updatedStops.size}")
                    
                    try {
                        val updateResult = tpsRepository.updateTPSStatus(completedStop.id, TPSStatus.TIDAK_PENUH)
                        if (updateResult.isSuccess) {
                            Log.d(TAG, "✅ SUCCESS: TPS ${completedStop.id} status updated to TIDAK_PENUH (available)")
                            
                            // Verify the update by fetching the TPS
                            viewModelScope.launch {
                                try {
                                    val tpsResult = tpsRepository.getTPSById(completedStop.id)
                                    if (tpsResult.isSuccess) {
                                        val tpsDoc = tpsResult.getOrNull()
                                        if (tpsDoc != null) {
                                            Log.d(TAG, "✅ VERIFICATION: TPS ${completedStop.id} current status: ${tpsDoc.status}")
                                        } else {
                                            Log.w(TAG, "⚠️ VERIFICATION: TPS ${completedStop.id} document not found")
                                        }
                                    } else {
                                        Log.e(TAG, "❌ VERIFICATION: Failed to fetch TPS ${completedStop.id}: ${tpsResult.exceptionOrNull()?.message}")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ VERIFICATION ERROR: ${e.message}", e)
                                }
                            }
                        } else {
                            val error = updateResult.exceptionOrNull()
                            Log.e(TAG, "❌ FAILED: Could not update TPS ${completedStop.id} status. Error: ${error?.message}", error)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ EXCEPTION: Error updating TPS ${completedStop.id} status", e)
                    }
                    Log.d(TAG, "=== END TPS STATUS UPDATE DEBUG ===")
                    
                    val completedCount = updatedStops.count { it.isCompleted }
                    val progress = (completedCount * 100) / updatedStops.size
                    
                    val updatedRoute = currentRoute.copy(
                        stops = updatedStops,
                        progress = progress,
                        status = if (completedCount == updatedStops.size) RouteStatus.COMPLETED else RouteStatus.IN_PROGRESS
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        currentRoute = updatedRoute,
                        message = "Collection point completed! TPS marked as available."
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
                // Add to collection records using current route's TPS data
                val currentRoute = _uiState.value.currentRoute
                val tpsStops = currentRoute?.stops?.map { routeStop ->
                    // Convert RouteStop back to TPS for collection record
                    TPS(
                        tpsId = routeStop.id,
                        name = routeStop.name,
                        address = routeStop.address,
                        location = GeoPoint(routeStop.latitude, routeStop.longitude)
                    )
                } ?: emptyList()
                
                val newRecord = CollectionRecord(
                    id = "record_${System.currentTimeMillis()}",
                    schedule = schedule,
                    tpsStops = tpsStops,
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
    
    suspend fun uploadDriverPhoto(photoUri: Uri): Result<String> {
        return try {
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId != null) {
                cloudStorageService.uploadDriverPhoto(photoUri, currentUserId)
            } else {
                Result.failure(Exception("User not authenticated"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading driver photo", e)
            Result.failure(e)
        }
    }

    fun startNavigation() {
        _uiState.value = _uiState.value.copy(isNavigationActive = true)
        Log.d(TAG, "Navigation started")
        // Verify TPS documents for debugging
        verifyTPSDocuments()
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
    
    // Debug method to test TPS status update
    fun testTPSStatusUpdate(tpsId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Testing TPS status update for ID: $tpsId")
            try {
                val updateResult = tpsRepository.updateTPSStatus(tpsId, TPSStatus.TIDAK_PENUH)
                if (updateResult.isSuccess) {
                    Log.d(TAG, "TEST SUCCESS: TPS $tpsId status updated to TIDAK_PENUH")
                } else {
                    val error = updateResult.exceptionOrNull()
                    Log.e(TAG, "TEST FAILED: Could not update TPS $tpsId status. Error: ${error?.message}", error)
                }
            } catch (e: Exception) {
                Log.e(TAG, "TEST EXCEPTION: Error updating TPS $tpsId status", e)
            }
        }
    }
    
    // Debug method to verify TPS document existence
    fun verifyTPSDocuments() {
        viewModelScope.launch {
            Log.d(TAG, "=== VERIFYING TPS DOCUMENTS ===")
            try {
                val allTPS = tpsRepository.getAllTPS().getOrNull() ?: emptyList()
                Log.d(TAG, "Found ${allTPS.size} TPS documents in database:")
                allTPS.forEach { tps ->
                    Log.d(TAG, "  - TPS ID: ${tps.tpsId}, Name: ${tps.name}, Status: ${tps.status}")
                }
                
                val currentSchedule = _uiState.value.currentSchedule
                if (currentSchedule != null) {
                    Log.d(TAG, "Current schedule TPS route: ${currentSchedule.tpsRoute}")
                    currentSchedule.tpsRoute.forEach { scheduleTPSId ->
                        val found = allTPS.find { it.tpsId == scheduleTPSId }
                        if (found != null) {
                            Log.d(TAG, "  ✓ Schedule TPS ID $scheduleTPSId matches database TPS: ${found.name}")
                        } else {
                            Log.e(TAG, "  ✗ Schedule TPS ID $scheduleTPSId NOT FOUND in database!")
                        }
                    }
                } else {
                    Log.d(TAG, "No current schedule to verify")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error verifying TPS documents", e)
            }
            Log.d(TAG, "=== END TPS VERIFICATION ===")
        }
    }

    private fun generateRouteAssignment(schedule: Schedule, allTPS: List<TPS>): RouteAssignment {
        Log.d(TAG, "Generating route assignment for schedule: ${schedule.scheduleId}")
        Log.d(TAG, "Schedule TPS route: ${schedule.tpsRoute}")
        Log.d(TAG, "Available TPS in database: ${allTPS.map { "${it.tpsId} (${it.name})" }}")
        
        val stops = schedule.tpsRoute.mapIndexed { index, tpsId ->
            val tps = allTPS.find { it.tpsId == tpsId }
            if (tps == null) {
                Log.w(TAG, "TPS not found in database for ID: $tpsId")
            } else {
                Log.d(TAG, "Found TPS for ID $tpsId: ${tps.name}")
            }
            
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

    private fun generateCollectionRecords(completedSchedules: List<Schedule>, allTPS: List<TPS>): List<CollectionRecord> {
        return completedSchedules.map { schedule ->
            val tpsStops = schedule.tpsRoute.mapNotNull { tpsId ->
                allTPS.find { it.tpsId == tpsId }
            }
            
            CollectionRecord(
                id = schedule.scheduleId,
                schedule = schedule,
                tpsStops = tpsStops,
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
    val schedule: Schedule,
    val tpsStops: List<TPS> = emptyList(),
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