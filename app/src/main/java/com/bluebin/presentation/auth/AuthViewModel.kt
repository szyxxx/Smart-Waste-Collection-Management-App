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
    val successMessage: String? = null,
    val isAuthenticated: Boolean = false,
    val isSignUpMode: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Form state
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.DRIVER)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.currentUser.collect { firebaseUser ->
                if (firebaseUser != null) {
                    loadCurrentUser(firebaseUser.uid)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        user = null
                    )
                }
            }
        }
    }

    private suspend fun loadCurrentUser(uid: String) {
        if (_uiState.value.user?.uid == uid) return
        
        _uiState.value = _uiState.value.copy(isLoading = true)
        
        userRepository.getCurrentUser().fold(
            onSuccess = { user ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isAuthenticated = user?.approved == true,
                    error = if (user?.approved == false) "Account pending admin approval" else null,
                    successMessage = if (user?.approved == true) "Welcome back, ${user.name}!" else null
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load user profile: ${error.message}",
                    isAuthenticated = false,
                    user = null
                )
            }
        )
    }

    // Form field updates
    fun updateEmail(email: String) {
        _email.value = email
        clearMessages()
    }

    fun updatePassword(password: String) {
        _password.value = password
        clearMessages()
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _confirmPassword.value = confirmPassword
        clearMessages()
    }

    fun updateName(name: String) {
        _name.value = name
        clearMessages()
    }

    fun updateSelectedRole(role: UserRole) {
        _selectedRole.value = role
    }

    fun toggleAuthMode() {
        _uiState.value = _uiState.value.copy(
            isSignUpMode = !_uiState.value.isSignUpMode
        )
        clearForm()
        clearMessages()
    }

    fun signIn() {
        viewModelScope.launch {
            val email = _email.value.trim()
            val password = _password.value

            // Validate inputs
            val validation = validateSignInInputs(email, password)
            if (!validation.isValid) {
                _uiState.value = _uiState.value.copy(error = validation.message)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.signInWithEmailPassword(email, password).fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        loadCurrentUser(firebaseUser.uid)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Sign in failed. Please try again."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = getAuthErrorMessage(error)
                    )
                }
            )
        }
    }

    fun signUp() {
        viewModelScope.launch {
            val email = _email.value.trim()
            val password = _password.value
            val confirmPassword = _confirmPassword.value
            val name = _name.value.trim()
            val role = _selectedRole.value

            // Validate inputs
            val validation = validateSignUpInputs(email, password, confirmPassword, name)
            if (!validation.isValid) {
                _uiState.value = _uiState.value.copy(error = validation.message)
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            authRepository.signUpWithEmailPassword(email, password, name, role).fold(
                onSuccess = { firebaseUser ->
                    if (firebaseUser != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            successMessage = "Account created successfully! Please wait for admin approval."
                        )
                        loadCurrentUser(firebaseUser.uid)
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Account creation failed. Please try again."
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = getAuthErrorMessage(error)
                    )
                }
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            authRepository.signOut()
            clearForm()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                successMessage = "Signed out successfully"
            )
        }
    }

    fun resetPassword(resetEmail: String) {
        viewModelScope.launch {
            val email = resetEmail.trim()
            
            if (!isValidEmail(email)) {
                _uiState.value = _uiState.value.copy(error = "Please enter a valid email address")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            authRepository.resetPassword(email).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Password reset email sent to $email"
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = getAuthErrorMessage(error)
                    )
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    private fun clearForm() {
        _email.value = ""
        _password.value = ""
        _confirmPassword.value = ""
        _name.value = ""
        _selectedRole.value = UserRole.DRIVER
    }

    // Validation helpers
    private fun validateSignInInputs(email: String, password: String): ValidationResult {
        return when {
            email.isEmpty() -> ValidationResult(false, "Email is required")
            password.isEmpty() -> ValidationResult(false, "Password is required")
            !isValidEmail(email) -> ValidationResult(false, "Please enter a valid email address")
            password.length < 6 -> ValidationResult(false, "Password must be at least 6 characters")
            else -> ValidationResult(true)
        }
    }

    private fun validateSignUpInputs(
        email: String, 
        password: String, 
        confirmPassword: String, 
        name: String
    ): ValidationResult {
        return when {
            name.isEmpty() -> ValidationResult(false, "Name is required")
            name.length < 2 -> ValidationResult(false, "Name must be at least 2 characters")
            email.isEmpty() -> ValidationResult(false, "Email is required")
            !isValidEmail(email) -> ValidationResult(false, "Please enter a valid email address")
            password.isEmpty() -> ValidationResult(false, "Password is required")
            password.length < 6 -> ValidationResult(false, "Password must be at least 6 characters")
            confirmPassword.isEmpty() -> ValidationResult(false, "Please confirm your password")
            password != confirmPassword -> ValidationResult(false, "Passwords do not match")
            else -> ValidationResult(true)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun getAuthErrorMessage(error: Throwable): String {
        return when {
            error.message?.contains("user-not-found") == true -> 
                "No account found with this email address"
            error.message?.contains("wrong-password") == true -> 
                "Incorrect password. Please try again."
            error.message?.contains("email-already-in-use") == true -> 
                "An account with this email already exists"
            error.message?.contains("weak-password") == true -> 
                "Password is too weak. Please choose a stronger password."
            error.message?.contains("invalid-email") == true -> 
                "Invalid email address format"
            error.message?.contains("too-many-requests") == true -> 
                "Too many failed attempts. Please try again later."
            error.message?.contains("network") == true -> 
                "Network error. Please check your internet connection."
            else -> error.message ?: "An error occurred. Please try again."
        }
    }

    data class ValidationResult(
        val isValid: Boolean,
        val message: String? = null
    )
} 