package com.bluebin.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.model.User
import com.bluebin.data.model.UserRole
import com.bluebin.data.repository.AuthRepository
import com.bluebin.data.repository.UserRepository
import com.bluebin.util.AuthDebugUtil
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { firebaseUser ->
                AuthDebugUtil.logFirebaseAuthChange(firebaseUser)
                
                if (firebaseUser != null) {
                    loadCurrentUser(firebaseUser.uid)
                } else {
                    // Clear user state when not authenticated
                    _authState.value = AuthUiState(
                        isLoading = false,
                        isAuthenticated = false,
                        user = null,
                        error = null
                    )
                    _currentUser.value = null
                    AuthDebugUtil.logAuthState(_authState.value, _currentUser.value, "AuthStateCleared")
                }
            }
        }
    }

    private suspend fun loadCurrentUser(uid: String) {
        // Don't reload if we already have the same user
        if (_currentUser.value?.uid == uid && _authState.value.user?.uid == uid) {
            return
        }
        
        // Set loading state
        _authState.value = _authState.value.copy(isLoading = true)
        
        val result = userRepository.getCurrentUser()
        result.fold(
            onSuccess = { user ->
                AuthDebugUtil.logUserLoad(uid, true, user)
                _currentUser.value = user
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = user != null && user.approved,
                    user = user,
                    error = if (user?.approved == false) "Account pending approval" else null
                )
                AuthDebugUtil.logAuthState(_authState.value, _currentUser.value, "UserLoaded")
            },
            onFailure = { error ->
                AuthDebugUtil.logUserLoad(uid, false)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = error.message,
                    isAuthenticated = false,
                    user = null
                )
                _currentUser.value = null
                AuthDebugUtil.logAuthState(_authState.value, _currentUser.value, "UserLoadFailed")
            }
        )
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.signInWithEmailPassword(email, password)
            result.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        loadCurrentUser(firebaseUser.uid)
                    }
                    _authState.value = _authState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, name: String, role: UserRole) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)

            val result = authRepository.signUpWithEmailPassword(email, password, name, role)
            result.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        loadCurrentUser(firebaseUser.uid)
                    }
                    _authState.value = _authState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    _authState.value = _authState.value.copy(
                        error = "Password reset email sent"
                    )
                },
                onFailure = { error ->
                    _authState.value = _authState.value.copy(
                        error = error.message
                    )
                }
            )
        }
    }
} 