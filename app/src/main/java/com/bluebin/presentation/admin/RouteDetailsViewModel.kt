package com.bluebin.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.model.*
import com.bluebin.data.repository.ScheduleRepository
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
class RouteDetailsViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val userRepository: UserRepository,
    private val tpsRepository: TPSRepository
) : ViewModel() {

    companion object {
        private const val TAG = "RouteDetailsVM"
    }

    private val _uiState = MutableStateFlow(RouteDetailsUiState())
    val uiState: StateFlow<RouteDetailsUiState> = _uiState.asStateFlow()

    fun loadRouteDetails(scheduleId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                Log.d(TAG, "Loading route details for schedule: $scheduleId")
                
                // Load the specific schedule
                val allSchedules = scheduleRepository.getAllSchedules()
                val schedule = allSchedules.find { it.scheduleId == scheduleId }
                
                if (schedule == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Schedule not found"
                    )
                    return@launch
                }
                
                Log.d(TAG, "Found schedule: ${schedule.scheduleId}")
                
                // Load driver details if assigned
                var driver: User? = null
                if (schedule.driverId.isNotEmpty() && schedule.driverId != "Not Assigned") {
                    val usersResult = userRepository.getAllUsers()
                    driver = usersResult.getOrNull()?.find { it.uid == schedule.driverId }
                    Log.d(TAG, "Found driver: ${driver?.name ?: "Unknown"}")
                }
                
                // Load TPS details for each stop
                val tpsResult = tpsRepository.getAllTPS()
                val allTPS = tpsResult.getOrNull() ?: emptyList()
                
                val tpsDetails = schedule.tpsRoute.mapNotNull { tpsId ->
                    allTPS.find { it.tpsId == tpsId }
                }.also { tpsList ->
                    Log.d(TAG, "Loaded ${tpsList.size} TPS details out of ${schedule.tpsRoute.size} route stops")
                }
                
                // Create route steps with completion data
                val routeSteps = schedule.tpsRoute.mapIndexed { index, tpsId ->
                    val tps = tpsDetails.find { it.tpsId == tpsId }
                    val completion = schedule.routeCompletionData.find { it.tpsId == tpsId }
                    
                    RouteStep(
                        stepNumber = index + 1,
                        tpsId = tpsId,
                        tpsName = tps?.name ?: "Unknown TPS",
                        tpsAddress = tps?.address ?: "Unknown Address",
                        isCompleted = completion != null,
                        completedAt = completion?.completedAt,
                        proofPhotoUrl = completion?.proofPhotoUrl,
                        notes = completion?.notes ?: "",
                        hasIssue = completion?.hasIssue ?: false,
                        estimatedArrivalTime = null, // Can be calculated based on route optimization
                        actualArrivalTime = completion?.completedAt
                    )
                }
                
                Log.d(TAG, "Created ${routeSteps.size} route steps, ${routeSteps.count { it.isCompleted }} completed")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    schedule = schedule,
                    driver = driver,
                    tpsDetails = tpsDetails,
                    routeSteps = routeSteps,
                    error = null
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading route details", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load route details: ${e.message}"
                )
            }
        }
    }

    fun refreshRouteDetails() {
        _uiState.value.schedule?.let { schedule ->
            loadRouteDetails(schedule.scheduleId)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class RouteDetailsUiState(
    val isLoading: Boolean = false,
    val schedule: Schedule? = null,
    val driver: User? = null,
    val tpsDetails: List<TPS> = emptyList(),
    val routeSteps: List<RouteStep> = emptyList(),
    val error: String? = null
)

data class RouteStep(
    val stepNumber: Int,
    val tpsId: String,
    val tpsName: String,
    val tpsAddress: String,
    val isCompleted: Boolean,
    val completedAt: Long? = null,
    val proofPhotoUrl: String? = null,
    val notes: String = "",
    val hasIssue: Boolean = false,
    val estimatedArrivalTime: Long? = null,
    val actualArrivalTime: Long? = null
) 