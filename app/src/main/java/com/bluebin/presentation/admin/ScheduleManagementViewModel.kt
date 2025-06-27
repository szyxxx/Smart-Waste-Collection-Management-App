package com.bluebin.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.model.*
import com.bluebin.data.repository.ScheduleRepository
import com.bluebin.data.repository.ScheduleStats
import com.bluebin.data.repository.UserRepository
import com.bluebin.data.repository.TPSRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ScheduleManagementViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository,
    private val tpsRepository: TPSRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ScheduleManagementVM"
    }

    private val _uiState = MutableStateFlow(ScheduleManagementUiState())
    val uiState: StateFlow<ScheduleManagementUiState> = _uiState.asStateFlow()

    init {
        loadSchedules()
    }

    fun loadSchedules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Loading schedules...")
                
                // Load all schedules (unified collection)
                val allSchedules = scheduleRepository.getAllSchedules()
                Log.d(TAG, "Loaded ${allSchedules.size} total schedules")
                
                // Filter by type and status - avoid duplicates
                val regularSchedules = allSchedules.filter { 
                    it.generationType == ScheduleGenerationType.MANUAL
                }
                
                val optimizedSchedules = allSchedules.filter { 
                    it.generationType == ScheduleGenerationType.AI_GENERATED 
                }
                
                val pendingApprovalSchedules = allSchedules.filter { 
                    it.status == ScheduleStatus.PENDING_APPROVAL 
                }
                
                Log.d(TAG, "Filtered - Regular: ${regularSchedules.size}, Optimized: ${optimizedSchedules.size}, Pending: ${pendingApprovalSchedules.size}")
                
                // Calculate stats from unified data
                val stats = calculateStatsFromSchedules(allSchedules)
                Log.d(TAG, "Calculated stats - Total: ${stats.totalSchedules}, Pending approval: ${stats.pendingOptimizedSchedules}")

                // Update UI state with unified data
                _uiState.value = _uiState.value.copy(
                    schedules = regularSchedules,
                    optimizedSchedules = optimizedSchedules,
                    pendingOptimizedSchedules = pendingApprovalSchedules,
                    stats = stats,
                    isLoading = false,
                    error = null
                )
                
                Log.d(TAG, "Successfully loaded all schedule data - UI State updated")
                Log.d(TAG, "UI State - Regular: ${_uiState.value.schedules.size}, Optimized: ${_uiState.value.optimizedSchedules.size}, Pending: ${_uiState.value.pendingOptimizedSchedules.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading schedules", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load schedules"
                )
            }
        }
    }

    private fun calculateStatsFromSchedules(allSchedules: List<Schedule>): ScheduleStats {
        val today = System.currentTimeMillis()
        val todayStart = today - (today % (24 * 60 * 60 * 1000))
        val todayEnd = todayStart + (24 * 60 * 60 * 1000)
        
        // Regular schedules
        val regularSchedules = allSchedules.filter { 
            it.generationType == ScheduleGenerationType.MANUAL || it.status == ScheduleStatus.ASSIGNED 
        }
        
        // Optimized schedules  
        val optimizedSchedules = allSchedules.filter { 
            it.generationType == ScheduleGenerationType.AI_GENERATED 
        }
        
        return ScheduleStats(
            totalSchedules = regularSchedules.size,
            pendingSchedules = regularSchedules.count { it.status == ScheduleStatus.PENDING },
            activeSchedules = regularSchedules.count { it.status == ScheduleStatus.IN_PROGRESS },
            completedSchedules = regularSchedules.count { it.status == ScheduleStatus.COMPLETED },
            cancelledSchedules = regularSchedules.count { it.status == ScheduleStatus.CANCELLED },
            completedToday = regularSchedules.count { schedule ->
                schedule.status == ScheduleStatus.COMPLETED &&
                schedule.completedAt != null &&
                schedule.completedAt!! >= todayStart &&
                schedule.completedAt!! < todayEnd
            },
            totalOptimizedSchedules = optimizedSchedules.size,
            pendingOptimizedSchedules = optimizedSchedules.count { it.status == ScheduleStatus.PENDING_APPROVAL },
            approvedOptimizedSchedules = optimizedSchedules.count { it.status == ScheduleStatus.APPROVED },
            assignedOptimizedSchedules = optimizedSchedules.count { it.status == ScheduleStatus.ASSIGNED },
            completedOptimizedSchedules = optimizedSchedules.count { it.status == ScheduleStatus.COMPLETED },
            optimizedSchedulesToday = optimizedSchedules.count { schedule ->
                schedule.generatedAt != null && 
                schedule.generatedAt!! >= todayStart && 
                schedule.generatedAt!! < todayEnd
            },
            totalOptimizedDistance = optimizedSchedules.sumOf { it.totalDistance },
            averageOptimizedDistance = if (optimizedSchedules.isNotEmpty()) {
                optimizedSchedules.sumOf { it.totalDistance } / optimizedSchedules.size
            } else 0.0,
            averageTpsPerRoute = if (optimizedSchedules.isNotEmpty()) {
                optimizedSchedules.sumOf { it.tpsRoute.size } / optimizedSchedules.size
            } else 0
        )
    }

    fun generateOptimizedSchedule() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            
            try {
                Log.d(TAG, "Starting optimized schedule generation...")
                
                // Get all TPS locations with FULL status
                val allTPS = tpsRepository.getAllTPS().getOrNull() ?: emptyList()
                val fullTPS = allTPS.filter { it.status == TPSStatus.PENUH }
                
                Log.d(TAG, "Found ${fullTPS.size} full TPS locations out of ${allTPS.size} total")
                
                if (fullTPS.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = "No TPS locations are currently full. Cannot generate schedule."
                    )
                    return@launch
                }
                
                // Split full TPS into chunks of 2-3 TPS per schedule
                val tpsChunks = fullTPS.chunked(3) // Prefer 3 TPS per schedule
                var generatedCount = 0
                val errors = mutableListOf<String>()
                
                Log.d(TAG, "Generating ${tpsChunks.size} optimized schedules from ${fullTPS.size} full TPS")
                
                tpsChunks.forEachIndexed { index, tpsChunk ->
                    try {
                        Log.d(TAG, "Generating schedule ${index + 1} for ${tpsChunk.size} TPS locations")
                        
                        // Generate and save optimized schedule directly
                        val result = scheduleRepository.generateAndSaveOptimizedSchedule(tpsChunk)
                        
                        result.fold(
                            onSuccess = { scheduleId ->
                                        generatedCount++
                                Log.d(TAG, "Successfully generated and saved optimized schedule with ID: $scheduleId")
                            },
                            onFailure = { error ->
                                val errorMsg = "Failed to generate schedule ${index + 1}: ${error.message}"
                                Log.e(TAG, errorMsg, error)
                                errors.add(errorMsg)
                            }
                        )
                    } catch (e: Exception) {
                        val errorMsg = "Unexpected error for schedule ${index + 1}: ${e.message}"
                        Log.e(TAG, errorMsg, e)
                        errors.add(errorMsg)
                    }
                }
                
                if (generatedCount > 0) {
                    val message = "Generated $generatedCount optimized schedules from ${fullTPS.size} full TPS locations${if (errors.isNotEmpty()) ". Some schedules failed to generate." else ""}"
                    Log.d(TAG, message)
                    
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        message = message,
                        error = if (errors.isNotEmpty()) errors.joinToString("; ") else null
                    )
                    
                    // Reload all data to show new schedules
                    Log.d(TAG, "Reloading all schedule data to show new schedules")
                    loadSchedules()
                } else {
                    val errorMsg = "Failed to generate any schedules: ${errors.joinToString("; ")}"
                    Log.e(TAG, errorMsg)
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        error = errorMsg
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during schedule generation", e)
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun approveOptimizedSchedule(scheduleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            Log.d(TAG, "Approving schedule: $scheduleId")
            
            val result = scheduleRepository.approveSchedule(scheduleId)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Successfully approved schedule: $scheduleId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Schedule approved successfully!"
                    )
                    loadSchedules()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to approve schedule: $scheduleId", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to approve schedule: ${error.message}"
                    )
                }
            )
        }
    }

    fun assignDriverToOptimizedSchedule(scheduleId: String, driverId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            Log.d(TAG, "Assigning driver $driverId to schedule: $scheduleId")
            
            val result = scheduleRepository.assignDriverToSchedule(scheduleId, driverId)
            result.fold(
                onSuccess = {
                    Log.d(TAG, "Successfully assigned driver to schedule: $scheduleId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "Driver assigned successfully!"
                    )
                    loadSchedules()
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to assign driver to schedule: $scheduleId", error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to assign driver: ${error.message}"
                    )
                }
            )
        }
    }

    fun approveAndAssignDriverToSchedule(scheduleId: String, driverId: String, selectedDate: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                Log.d(TAG, "Approving and assigning driver to schedule: $scheduleId")
                
                // First approve the schedule
                val approveResult = scheduleRepository.approveSchedule(scheduleId)
                
                approveResult.fold(
                    onSuccess = {
                        Log.d(TAG, "Schedule approved, now assigning driver")
                        
                        // Then assign driver
                        val assignResult = scheduleRepository.assignDriverToSchedule(scheduleId, driverId)
                        
                        assignResult.fold(
                            onSuccess = {
                                Log.d(TAG, "Driver assigned successfully")
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        message = "Schedule approved and driver assigned successfully!"
                                    )
                                    loadSchedules()
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Failed to assign driver after approval", error)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Schedule approved but failed to assign driver: ${error.message}"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to approve schedule", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to approve schedule: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during approve and assign", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun approveAndAssignDriverWithDate(
        scheduleId: String, 
        driverId: String, 
        assignedDate: java.time.LocalDate,
        isRecurring: Boolean = false
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                Log.d(TAG, "Approving and assigning driver with date to schedule: $scheduleId")
                
                // Convert LocalDate to Timestamp
                val dateTimestamp = com.google.firebase.Timestamp(
                    assignedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond(),
                    0
                )
                
                // First approve the schedule
                val approveResult = scheduleRepository.approveSchedule(scheduleId)
                
                approveResult.fold(
                    onSuccess = {
                        Log.d(TAG, "Schedule approved, now assigning driver with date")
                        
                        // Then assign driver with date
                        val assignResult = scheduleRepository.assignDriverWithDate(
                            scheduleId, driverId, dateTimestamp, isRecurring
                        )
                        
                        assignResult.fold(
                            onSuccess = {
                                Log.d(TAG, "Driver assigned with date successfully")
                                val message = if (isRecurring) {
                                    "Schedule approved and assigned to driver! Will repeat weekly."
                                } else {
                                    "Schedule approved and assigned to driver for ${assignedDate.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy"))}!"
                                }
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    message = message
                                )
                                loadSchedules()
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Failed to assign driver with date after approval", error)
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Schedule approved but failed to assign driver: ${error.message}"
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to approve schedule", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to approve schedule: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during approve and assign with date", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun deleteOptimizedSchedule(scheduleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            Log.d(TAG, "Deleting schedule: $scheduleId")
            
            val result = scheduleRepository.deleteSchedule(scheduleId)
            if (result) {
                Log.d(TAG, "Successfully deleted schedule: $scheduleId")
                    
                    // Remove from both lists
                    val updatedPending = _uiState.value.pendingOptimizedSchedules.toMutableList()
                    val updatedAll = _uiState.value.optimizedSchedules.toMutableList()
                    
                    updatedPending.removeAll { it.scheduleId == scheduleId }
                    updatedAll.removeAll { it.scheduleId == scheduleId }
                    
                    // Also clear lastGeneratedSchedule if it matches
                    val updatedLastGenerated = if (_uiState.value.lastGeneratedSchedule?.scheduleId == scheduleId) {
                        null
                    } else {
                        _uiState.value.lastGeneratedSchedule
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                    message = "Schedule deleted successfully!",
                        pendingOptimizedSchedules = updatedPending,
                        optimizedSchedules = updatedAll,
                        lastGeneratedSchedule = updatedLastGenerated
                    )
                    
                    // Also reload to sync with database
                    loadSchedules()
            } else {
                Log.e(TAG, "Failed to delete schedule: $scheduleId")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                    error = "Failed to delete schedule"
                    )
                }
        }
    }

    fun updateScheduleStatus(scheduleId: String, status: ScheduleStatus) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Updating schedule status: $scheduleId to $status")
                val success = scheduleRepository.updateScheduleStatus(scheduleId, status)
                if (success) {
                    Log.d(TAG, "Successfully updated schedule status")
                    // Reload schedules to reflect the change
                    loadSchedules()
                } else {
                    Log.e(TAG, "Failed to update schedule status")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update schedule status"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating schedule status", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update schedule status"
                )
            }
        }
    }

    fun deleteSchedule(scheduleId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Deleting schedule: $scheduleId")
                val success = scheduleRepository.deleteSchedule(scheduleId)
                if (success) {
                    Log.d(TAG, "Successfully deleted schedule")
                    // Reload schedules to reflect the change
                    loadSchedules()
                } else {
                    Log.e(TAG, "Failed to delete schedule")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to delete schedule"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting schedule", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete schedule"
                )
            }
        }
    }

    fun createSchedule(schedule: Schedule) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Creating new schedule")
                val success = scheduleRepository.createSchedule(schedule)
                if (success) {
                    Log.d(TAG, "Successfully created schedule")
                    // Reload schedules to include the new one
                    loadSchedules()
                } else {
                    Log.e(TAG, "Failed to create schedule")
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to create schedule"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating schedule", e)
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to create schedule"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null, message = null)
    }
    
    fun refreshPendingSchedules() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Refreshing pending schedules")
                val pendingSchedules = scheduleRepository.getPendingApprovalSchedules()
                
                _uiState.value = _uiState.value.copy(
                    pendingOptimizedSchedules = pendingSchedules
                )
                Log.d(TAG, "Successfully refreshed ${pendingSchedules.size} pending schedules")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing pending schedules", e)
                // Handle error silently
            }
        }
    }

    fun refreshAllData() {
        Log.d(TAG, "Refreshing all schedule data")
        loadSchedules()
    }

    fun getSchedulesByStatus(status: ScheduleStatus): List<Schedule> {
        return _uiState.value.schedules.filter { it.status == status }
    }

    fun getSchedulesByDriver(driverId: String): List<Schedule> {
        return _uiState.value.schedules.filter { it.driverId == driverId }
    }

    fun loadAvailableDrivers() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading available drivers")
                val usersResult = userRepository.getAllUsers()
                val drivers = usersResult.getOrNull()
                    ?.filter { it.role == UserRole.DRIVER && it.approved }
                    ?: emptyList()
                
                _uiState.value = _uiState.value.copy(
                    availableDrivers = drivers
                )
                Log.d(TAG, "Successfully loaded ${drivers.size} drivers")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading drivers", e)
                // Don't show error for driver loading failure
            }
        }
    }

    fun assignDriverToSchedule(scheduleId: String, driverId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                Log.d(TAG, "Assigning driver $driverId to schedule $scheduleId")
                
                val result = scheduleRepository.assignDriverToSchedule(scheduleId, driverId)
                
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Successfully assigned driver to schedule")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = "Driver assigned successfully!"
                        )
                        // Reload schedules to reflect the change
                        loadSchedules()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to assign driver to schedule", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to assign driver: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during driver assignment", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    fun assignDriverWithSchedule(
        scheduleId: String, 
        driverId: String, 
        assignmentDate: java.time.LocalDate, 
        isRecurring: Boolean
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            Log.d(TAG, "Assigning driver $driverId to schedule $scheduleId with date $assignmentDate, recurring: $isRecurring")
            
            try {
                // Convert LocalDate to Timestamp
                val assignmentTimestamp = com.google.firebase.Timestamp(
                    assignmentDate.atStartOfDay(java.time.ZoneId.systemDefault()).toEpochSecond(),
                    0
                )
                
                val result = scheduleRepository.assignDriverWithDate(
                    scheduleId, 
                    driverId, 
                    assignmentTimestamp, 
                    isRecurring
                )
                
                result.fold(
                    onSuccess = {
                        val message = if (isRecurring) {
                            "Driver assigned successfully with weekly recurrence!"
                        } else {
                            "Driver assigned successfully for ${assignmentDate}!"
                        }
                        
                        Log.d(TAG, "Successfully assigned driver with schedule details")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            message = message
                        )
                        loadSchedules()
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to assign driver with schedule details", error)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Failed to assign driver: ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error assigning driver with schedule details", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    fun loadAvailableDrivers(onDriversLoaded: (List<User>) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading available drivers")
                val usersResult = userRepository.getAllUsers()
                val drivers = usersResult.getOrNull()
                    ?.filter { it.role == UserRole.DRIVER && it.approved }
                    ?: emptyList()
                Log.d(TAG, "Loaded ${drivers.size} available drivers")
                onDriversLoaded(drivers)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading available drivers", e)
                onDriversLoaded(emptyList())
            }
        }
    }

    fun debugOptimizedSchedules() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== DEBUG: Checking optimized schedules ===")
                
                // Check all optimized schedules
                val allSchedules = scheduleRepository.getOptimizedSchedules()
                Log.d(TAG, "DEBUG: Found ${allSchedules.size} total optimized schedules")
                allSchedules.forEachIndexed { index, schedule ->
                    Log.d(TAG, "DEBUG: Schedule $index - ID: ${schedule.scheduleId}, Status: ${schedule.status}, Generated: ${schedule.generatedAt}")
                }
                
                // Check pending schedules specifically
                val pendingSchedules = scheduleRepository.getPendingApprovalSchedules()
                Log.d(TAG, "DEBUG: Found ${pendingSchedules.size} pending optimized schedules")
                pendingSchedules.forEachIndexed { index, schedule ->
                    Log.d(TAG, "DEBUG: Pending Schedule $index - ID: ${schedule.scheduleId}, Status: ${schedule.status}")
                }
                
                // Check current UI state
                val currentState = _uiState.value
                Log.d(TAG, "DEBUG: Current UI State - Optimized: ${currentState.optimizedSchedules.size}, Pending: ${currentState.pendingOptimizedSchedules.size}")
                Log.d(TAG, "DEBUG: UI State loading: ${currentState.isLoading}, error: ${currentState.error}")
                
                Log.d(TAG, "=== DEBUG: End optimized schedules check ===")
            } catch (e: Exception) {
                Log.e(TAG, "DEBUG: Error during debug check", e)
            }
        }
    }

    fun testFirestoreDirectAccess() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "=== TESTING DIRECT FIRESTORE ACCESS ===")
                
                // Test basic connectivity
                val regularSchedules = scheduleRepository.getAllSchedules()
                Log.d(TAG, "TEST: Regular schedules: ${regularSchedules.size}")
                
                // Test optimized schedules collection access
                Log.d(TAG, "TEST: Attempting to access schedules collection for pending approval...")
                
                // Force reload of pending schedules
                val pendingSchedules = scheduleRepository.getPendingApprovalSchedules()
                Log.d(TAG, "TEST: Successfully loaded ${pendingSchedules.size} pending schedules")
                _uiState.value = _uiState.value.copy(
                    pendingOptimizedSchedules = pendingSchedules,
                    error = null
                )
                
                Log.d(TAG, "=== END FIRESTORE TEST ===")
            } catch (e: Exception) {
                Log.e(TAG, "TEST: Error during Firestore test", e)
                _uiState.value = _uiState.value.copy(
                    error = "Test failed: ${e.message}"
                )
            }
        }
    }
}

data class ScheduleManagementUiState(
    val schedules: List<Schedule> = emptyList(),
    val optimizedSchedules: List<Schedule> = emptyList(),
    val pendingOptimizedSchedules: List<Schedule> = emptyList(),
    val stats: ScheduleStats = ScheduleStats(),
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val lastGeneratedSchedule: Schedule? = null,
    val availableDrivers: List<User> = emptyList()
) 