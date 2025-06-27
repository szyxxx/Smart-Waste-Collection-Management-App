package com.bluebin.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.model.*
import com.bluebin.data.repository.*
import com.bluebin.presentation.driver.DriverLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminDashboardUiState(
    val isLoading: Boolean = false,
    val users: List<User> = emptyList(),
    val tpsLocations: List<TPS> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val activeDriverLocations: List<DriverLocation> = emptyList(),
    val error: String? = null,
    val message: String? = null,
    val stats: DashboardStats = DashboardStats()
)

data class DashboardStats(
    val totalUsers: Int = 0,
    val pendingApprovals: Int = 0,
    val totalTPS: Int = 0,
    val fullTPS: Int = 0,
    val activeSchedules: Int = 0,
    val completedToday: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tpsRepository: TPSRepository,
    private val scheduleRepository: ScheduleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminDashboardUiState())
    val uiState: StateFlow<AdminDashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // Load all data in parallel
                android.util.Log.d("AdminDashboard", "Starting to load dashboard data...")
                
                val usersResult = userRepository.getAllUsers()
                android.util.Log.d("AdminDashboard", "Users result: ${usersResult.isSuccess}")
                
                val tpsResult = tpsRepository.getAllTPS()
                val schedulesResult = scheduleRepository.getAllSchedules()
                val activeDriverLocations = scheduleRepository.getAllActiveDriverLocations()
                
                val users = usersResult.getOrNull() ?: emptyList()
                val tpsLocations = tpsResult.getOrNull() ?: emptyList()
                val schedules = schedulesResult
                
                android.util.Log.d("AdminDashboard", "Final users count: ${users.size}")
                users.forEach { user ->
                    android.util.Log.d("AdminDashboard", "User: ${user.name} (${user.role}) - Approved: ${user.approved}")
                }
                
                if (usersResult.isFailure) {
                    android.util.Log.e("AdminDashboard", "Users loading failed", usersResult.exceptionOrNull())
                }
                
                val stats = calculateStats(users, tpsLocations, schedules)
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    users = users,
                    tpsLocations = tpsLocations,
                    schedules = schedules,
                    activeDriverLocations = activeDriverLocations,
                    stats = stats,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load dashboard data: ${e.message}"
                )
            }
        }
    }

    fun refreshDriverLocations() {
        viewModelScope.launch {
            try {
                val activeDriverLocations = scheduleRepository.getAllActiveDriverLocations()
                _uiState.value = _uiState.value.copy(
                    activeDriverLocations = activeDriverLocations
                )
            } catch (e: Exception) {
                // Silently fail for location updates to not disrupt UI
            }
        }
    }

    fun approveUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = userRepository.approveUser(userId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "User approved successfully",
                        isLoading = false
                    )
                    loadDashboardData() // Refresh data
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to approve user: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun rejectUser(userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = userRepository.deleteUser(userId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "User rejected and removed",
                        isLoading = false
                    )
                    loadDashboardData() // Refresh data
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to reject user: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun updateTPSStatus(tpsId: String, status: TPSStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            android.util.Log.d("AdminDashboard", "Updating TPS status - ID: '$tpsId', Status: ${status.name}")
            
            if (tpsId.isBlank()) {
                android.util.Log.e("AdminDashboard", "TPS ID is blank, cannot update status")
                _uiState.value = _uiState.value.copy(
                    error = "Invalid TPS ID - cannot update status",
                    isLoading = false
                )
                return@launch
            }
            
            val result = tpsRepository.updateTPSStatus(tpsId, status)
            result.fold(
                onSuccess = {
                    android.util.Log.d("AdminDashboard", "TPS status updated successfully for ID: $tpsId")
                    _uiState.value = _uiState.value.copy(
                        message = "TPS status updated successfully",
                        isLoading = false
                    )
                    loadDashboardData() // Refresh data
                },
                onFailure = { error ->
                    android.util.Log.e("AdminDashboard", "Failed to update TPS status for ID: $tpsId", error)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to update TPS status: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun registerTPS(tps: TPS) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = tpsRepository.createTPS(tps)
            result.fold(
                onSuccess = { tpsId ->
                    _uiState.value = _uiState.value.copy(
                        message = "TPS registered successfully with ID: $tpsId",
                        isLoading = false
                    )
                    loadDashboardData() // Refresh data
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to register TPS: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun assignOfficerToTPS(tpsId: String, officerId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val result = tpsRepository.assignOfficerToTPS(tpsId, officerId)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        message = "Officer assigned successfully",
                        isLoading = false
                    )
                    loadDashboardData() // Refresh data
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to assign officer: ${error.message}",
                        isLoading = false
                    )
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, error = null)
    }



    private fun calculateStats(users: List<User>, tpsLocations: List<TPS>, schedules: List<Schedule>): DashboardStats {
        val totalUsers = users.size
        val pendingApprovals = users.count { !it.approved }
        val totalTPS = tpsLocations.size
        val fullTPS = tpsLocations.count { it.status == TPSStatus.PENUH }
        val activeSchedules = schedules.count { it.status == ScheduleStatus.IN_PROGRESS }
        
        // Calculate completed today (last 24 hours)
        val oneDayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        val completedToday = schedules.count { 
            it.status == ScheduleStatus.COMPLETED && 
            (it.completedAt ?: 0) > oneDayAgo 
        }
        
        return DashboardStats(
            totalUsers = totalUsers,
            pendingApprovals = pendingApprovals,
            totalTPS = totalTPS,
            fullTPS = fullTPS,
            activeSchedules = activeSchedules,
            completedToday = completedToday
        )
    }
} 