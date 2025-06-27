package com.bluebin.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bluebin.data.model.User
import com.bluebin.data.model.UserRole
import com.bluebin.data.repository.AuthRepository
import com.bluebin.data.repository.UserRepository
import com.bluebin.util.AuthDebugUtil
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
    val isAuthenticated: Boolean = false,
    val successMessage: String? = null,
    val isSignUp: Boolean = false,
    val emailValidation: String? = null,
    val passwordValidation: String? = null,
    val nameValidation: String? = null
)

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
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
                    error = if (user?.approved == false) "Account pending approval by admin" else null,
                    successMessage = if (user?.approved == true) "Welcome back, ${user.name}!" else null
                )
                AuthDebugUtil.logAuthState(_authState.value, _currentUser.value, "UserLoaded")
            },
            onFailure = { error ->
                AuthDebugUtil.logUserLoad(uid, false)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Failed to load user profile: ${error.message}",
                    isAuthenticated = false,
                    user = null
                )
                _currentUser.value = null
                AuthDebugUtil.logAuthState(_authState.value, _currentUser.value, "UserLoadFailed")
            }
        )
    }

    // Validation functions
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult(false, "Email is required")
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationResult(false, "Please enter a valid email address")
            else -> ValidationResult(true)
        }
    }

    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult(false, "Password is required")
            password.length < 6 -> ValidationResult(false, "Password must be at least 6 characters")
            else -> ValidationResult(true)
        }
    }

    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult(false, "Name is required")
            name.length < 2 -> ValidationResult(false, "Name must be at least 2 characters")
            else -> ValidationResult(true)
        }
    }

    fun validatePasswordMatch(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult(false, "Please confirm your password")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            // Clear previous states
            _authState.value = _authState.value.copy(
                isLoading = true, 
                error = null, 
                successMessage = null,
                emailValidation = null,
                passwordValidation = null
            )

            // Validate inputs
            val emailValidation = validateEmail(email)
            val passwordValidation = validatePassword(password)

            if (!emailValidation.isValid || !passwordValidation.isValid) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    emailValidation = emailValidation.errorMessage,
                    passwordValidation = passwordValidation.errorMessage
                )
                return@launch
            }

            val result = authRepository.signInWithEmailPassword(email, password)
            result.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        loadCurrentUser(firebaseUser.uid)
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = "Sign in failed. Please try again."
                        )
                    }
                },
                onFailure = { error ->
                    val userFriendlyError = when {
                        error.message?.contains("user-not-found") == true -> 
                            "No account found with this email address"
                        error.message?.contains("wrong-password") == true -> 
                            "Incorrect password. Please try again."
                        error.message?.contains("invalid-email") == true -> 
                            "Invalid email address format"
                        error.message?.contains("too-many-requests") == true -> 
                            "Too many failed attempts. Please try again later."
                        error.message?.contains("network") == true -> 
                            "Network error. Please check your internet connection."
                        else -> error.message ?: "Sign in failed. Please try again."
                    }
                    
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = userFriendlyError
                    )
                }
            )
        }
    }

    fun signUp(email: String, password: String, confirmPassword: String, name: String, role: UserRole) {
        viewModelScope.launch {
            // Clear previous states
            _authState.value = _authState.value.copy(
                isLoading = true, 
                error = null, 
                successMessage = null,
                emailValidation = null,
                passwordValidation = null,
                nameValidation = null
            )

            // Validate inputs
            val emailValidation = validateEmail(email)
            val passwordValidation = validatePassword(password)
            val nameValidation = validateName(name)
            val passwordMatchValidation = validatePasswordMatch(password, confirmPassword)

            if (!emailValidation.isValid || !passwordValidation.isValid || 
                !nameValidation.isValid || !passwordMatchValidation.isValid) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    emailValidation = emailValidation.errorMessage,
                    passwordValidation = passwordValidation.errorMessage ?: passwordMatchValidation.errorMessage,
                    nameValidation = nameValidation.errorMessage
                )
                return@launch
            }

            val result = authRepository.signUpWithEmailPassword(email, password, name, role)
            result.fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        loadCurrentUser(firebaseUser.uid)
                        _authState.value = _authState.value.copy(
                            successMessage = "Account created successfully! Please wait for admin approval."
                        )
                    } else {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = "Account creation failed. Please try again."
                        )
                    }
                },
                onFailure = { error ->
                    val userFriendlyError = when {
                        error.message?.contains("email-already-in-use") == true -> 
                            "An account with this email already exists"
                        error.message?.contains("weak-password") == true -> 
                            "Password is too weak. Please choose a stronger password."
                        error.message?.contains("invalid-email") == true -> 
                            "Invalid email address format"
                        error.message?.contains("network") == true -> 
                            "Network error. Please check your internet connection."
                        else -> error.message ?: "Account creation failed. Please try again."
                    }
                    
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = userFriendlyError
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true)
            authRepository.signOut()
            _authState.value = _authState.value.copy(
                isLoading = false,
                successMessage = "Signed out successfully"
            )
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun clearMessage() {
        _authState.value = _authState.value.copy(successMessage = null)
    }

    fun clearValidationErrors() {
        _authState.value = _authState.value.copy(
            emailValidation = null,
            passwordValidation = null,
            nameValidation = null
        )
    }

    fun resetPassword(email: String) {
        viewModelScope.launch {
            val emailValidation = validateEmail(email)
            if (!emailValidation.isValid) {
                _authState.value = _authState.value.copy(
                    error = emailValidation.errorMessage
                )
                return@launch
            }

            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.resetPassword(email)
            result.fold(
                onSuccess = {
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        successMessage = "Password reset email sent to $email"
                    )
                },
                onFailure = { error ->
                    val userFriendlyError = when {
                        error.message?.contains("user-not-found") == true -> 
                            "No account found with this email address"
                        error.message?.contains("invalid-email") == true -> 
                            "Invalid email address format"
                        else -> "Failed to send reset email. Please try again."
                    }
                    
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = userFriendlyError
                    )
                }
            )
        }
    }

    fun setSignUpMode(isSignUp: Boolean) {
        _authState.value = _authState.value.copy(
            isSignUp = isSignUp,
            error = null,
            successMessage = null,
            emailValidation = null,
            passwordValidation = null,
            nameValidation = null
        )
    }
} 