package com.bluebin.presentation.tps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.repository.TPSRepository
import com.bluebin.data.repository.AuthRepository
import com.bluebin.data.repository.UserRepository
import com.bluebin.data.model.TPSStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun loadTPSAssignment() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val currentUserId = authRepository.getCurrentUserId()
                if (currentUserId != null) {
                    val allTPS = tpsRepository.getAllTPS().getOrNull() ?: emptyList()
                    val assignedTPS = allTPS.find { it.assignedOfficerId == currentUserId }
                    
                    if (assignedTPS != null) {
                        val tpsAssignment = AssignedTPS(
                            id = assignedTPS.tpsId,
                            name = assignedTPS.name,
                            address = assignedTPS.address,
                            status = assignedTPS.status,
                            currentCapacity = 100, // Default capacity, as TPS model doesn't have capacity field
                            lastUpdated = formatTimestamp(assignedTPS.lastUpdated)
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
            try {
                val assignedTPS = _uiState.value.assignedTPS
                if (assignedTPS != null) {
                    val result = tpsRepository.updateTPSStatus(assignedTPS.id, newStatus)
                    
                    if (result.isSuccess) {
                        // Update local state
                        val updatedTPS = assignedTPS.copy(
                            status = newStatus,
                            lastUpdated = formatTimestamp(System.currentTimeMillis())
                        )
                        
                        // Add to status history
                        val newUpdate = StatusUpdate(
                            status = newStatus,
                            timestamp = formatTimestamp(System.currentTimeMillis())
                        )
                        
                        _uiState.value = _uiState.value.copy(
                            assignedTPS = updatedTPS,
                            statusHistory = listOf(newUpdate) + _uiState.value.statusHistory
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to update TPS status"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update TPS status"
                )
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // In a real implementation, this would:
                // 1. Save notification preference to SharedPreferences/DataStore
                // 2. Schedule/cancel WorkManager task for 8 PM reminders
                // 3. Update FCM subscription topics if needed
                
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = enabled
                )
                
                if (enabled) {
                    // Schedule daily reminder at 8 PM
                    scheduleStatusReminder()
                } else {
                    // Cancel scheduled reminders
                    cancelStatusReminder()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update notification settings"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private suspend fun loadStatusHistory(tpsId: String): List<StatusUpdate> {
        return try {
            // Load actual status history from TPS updates
            val tps = tpsRepository.getTPSById(tpsId).getOrNull()
            val updates = mutableListOf<StatusUpdate>()
            
            if (tps != null) {
                // Add current status as most recent update
                updates.add(
                    StatusUpdate(
                        status = tps.status,
                        timestamp = formatTimestamp(tps.lastUpdated)
                    )
                )
                
                // In a full implementation, you would load historical status changes
                // For now, we'll just show the current status
                // TODO: Implement historical status tracking in TPS model
            }
            
            updates
        } catch (e: Exception) {
            // Return empty list if loading fails
            emptyList()
        }
    }

    private fun scheduleStatusReminder() {
        // TODO: Implement WorkManager scheduling for 8 PM daily reminder
        // Example:
        // val reminderRequest = OneTimeWorkRequestBuilder<StatusReminderWorker>()
        //     .setInitialDelay(calculateDelayUntil8PM(), TimeUnit.MILLISECONDS)
        //     .build()
        // WorkManager.getInstance(context).enqueue(reminderRequest)
    }

    private fun cancelStatusReminder() {
        // TODO: Cancel WorkManager task
        // WorkManager.getInstance(context).cancelAllWorkByTag("status_reminder")
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
    val error: String? = null
)

data class AssignedTPS(
    val id: String,
    val name: String,
    val address: String,
    val status: TPSStatus,
    val currentCapacity: Int,
    val lastUpdated: String
)

data class StatusUpdate(
    val status: TPSStatus,
    val timestamp: String
) 