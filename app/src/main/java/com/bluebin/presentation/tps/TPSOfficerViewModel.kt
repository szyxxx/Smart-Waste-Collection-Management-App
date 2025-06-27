package com.bluebin.presentation.tps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.repository.TPSRepository
import com.bluebin.data.repository.AuthRepository
import com.bluebin.data.repository.UserRepository
import com.bluebin.data.model.TPS
import com.bluebin.data.model.TPSStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*

@HiltViewModel
class TPSOfficerViewModel @Inject constructor(
    private val tpsRepository: TPSRepository,
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TPSOfficerUiState())
    val uiState: StateFlow<TPSOfficerUiState> = _uiState.asStateFlow()

    init {
        loadTPSAssignment()
    }

    fun loadTPSAssignment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    // Use real-time flow to get TPS assignments
                    tpsRepository.getTPSByOfficer(currentUserId).collectLatest { tpsList ->
                        if (tpsList.isNotEmpty()) {
                            val assignedTPS = tpsList.first() // Assume officer is assigned to one TPS
                            
                            val tpsAssignment = AssignedTPS(
                                id = assignedTPS.tpsId,
                                name = assignedTPS.name,
                                address = assignedTPS.address,
                                status = assignedTPS.status,
                                lastUpdated = formatTimestamp(assignedTPS.lastUpdated),
                                latitude = assignedTPS.location.latitude,
                                longitude = assignedTPS.location.longitude
                            )
                            
                            // Load status history
                            val statusHistory = loadStatusHistory(assignedTPS.tpsId)
                            
                            _uiState.value = _uiState.value.copy(
                                assignedTPS = tpsAssignment,
                                statusHistory = statusHistory,
                                isLoading = false,
                                error = null
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                assignedTPS = null,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load TPS assignment"
                )
            }
        }
    }

    fun updateTPSStatus(newStatus: TPSStatus) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            
            try {
                val assignedTPS = _uiState.value.assignedTPS
                if (assignedTPS != null) {
                    val result = tpsRepository.updateTPSStatus(assignedTPS.id, newStatus)
                    
                    if (result.isSuccess) {
                        // Update local state immediately for better UX
                        val updatedTPS = assignedTPS.copy(
                            status = newStatus,
                            lastUpdated = formatTimestamp(System.currentTimeMillis())
                        )
                        
                        // Add to status history
                        val newUpdate = StatusUpdate(
                            status = newStatus,
                            timestamp = formatTimestamp(System.currentTimeMillis()),
                            officerName = "You"
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            assignedTPS = updatedTPS,
                            statusHistory = listOf(newUpdate) + _uiState.value.statusHistory.take(9), // Keep last 10 updates
                            isUpdating = false,
                            error = null,
                            successMessage = "TPS status updated successfully"
                        )
                        
                        // Clear success message after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(successMessage = null)
                        
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isUpdating = false,
                            error = result.exceptionOrNull()?.message ?: "Failed to update TPS status"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        error = "No TPS assignment found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isUpdating = false,
                    error = e.message ?: "Failed to update TPS status"
                )
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = enabled
                )
                
                if (enabled) {
                    scheduleStatusReminder()
                } else {
                    cancelStatusReminder()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update notification settings"
                )
            }
        }
    }

    fun refreshAssignment() {
        loadTPSAssignment()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }



    private suspend fun loadStatusHistory(tpsId: String): List<StatusUpdate> {
        return try {
            val tps = tpsRepository.getTPSById(tpsId).getOrNull()
            val updates = mutableListOf<StatusUpdate>()
            
            if (tps != null) {
                // Add current status as most recent update
                updates.add(
                    StatusUpdate(
                        status = tps.status,
                        timestamp = formatTimestamp(tps.lastUpdated),
                        officerName = "System"
                    )
                )
                
                // Generate some mock historical data for demonstration
                // In a real app, you would store and load actual history
                val mockHistory = generateMockStatusHistory(tps.status)
                updates.addAll(mockHistory)
            }
            
            updates.take(10) // Limit to last 10 updates
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun generateMockStatusHistory(currentStatus: TPSStatus): List<StatusUpdate> {
        val history = mutableListOf<StatusUpdate>()
        val calendar = Calendar.getInstance()
        
        // Generate last few days of status changes
        for (i in 1..5) {
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val status = if (i % 2 == 0) TPSStatus.PENUH else TPSStatus.TIDAK_PENUH
            
            history.add(
                StatusUpdate(
                    status = status,
                    timestamp = formatTimestamp(calendar.timeInMillis),
                    officerName = "Officer"
                )
            )
        }
        
        return history.reversed() // Show in chronological order
    }

    private fun scheduleStatusReminder() {
        // TODO: Implement WorkManager scheduling for status reminders
        // This would schedule a daily notification to remind officers to update status
    }

    private fun cancelStatusReminder() {
        // TODO: Cancel WorkManager tasks
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp)
        val today = Calendar.getInstance()
        val timestampCal = Calendar.getInstance().apply { time = date }
        
        return when {
            today.get(Calendar.DAY_OF_YEAR) == timestampCal.get(Calendar.DAY_OF_YEAR) -> {
                "Today, ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            }
            today.get(Calendar.DAY_OF_YEAR) - 1 == timestampCal.get(Calendar.DAY_OF_YEAR) -> {
                "Yesterday, ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)}"
            }
            else -> {
                SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(date)
            }
        }
    }
}

data class TPSOfficerUiState(
    val assignedTPS: AssignedTPS? = null,
    val statusHistory: List<StatusUpdate> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val isUpdating: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

data class AssignedTPS(
    val id: String,
    val name: String,
    val address: String,
    val status: TPSStatus,
    val lastUpdated: String,
    val latitude: Double,
    val longitude: Double
)

data class StatusUpdate(
    val status: TPSStatus,
    val timestamp: String,
    val officerName: String
) 