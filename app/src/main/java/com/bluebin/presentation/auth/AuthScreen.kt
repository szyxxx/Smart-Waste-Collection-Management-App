package com.bluebin.presentation.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.data.model.UserRole
import com.bluebin.ui.components.*
import com.bluebin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    onNavigateToMain: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    val email by authViewModel.email.collectAsState()
    val password by authViewModel.password.collectAsState()
    val confirmPassword by authViewModel.confirmPassword.collectAsState()
    val name by authViewModel.name.collectAsState()
    val selectedRole by authViewModel.selectedRole.collectAsState()
    
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var screenVisible by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    // Screen entrance animation
    LaunchedEffect(Unit) {
        delay(100)
        screenVisible = true
    }

    // Handle successful authentication
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onNavigateToMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                animationSpec = tween(600),
                initialOffsetY = { it / 4 }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo and Header
                AppHeader(isSignUp = uiState.isSignUpMode)
                
                Spacer(modifier = Modifier.height(40.dp))

                // Auth Toggle
                AuthModeToggle(
                    isSignUp = uiState.isSignUpMode,
                    onToggle = { authViewModel.toggleAuthMode() }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Main Auth Form
                ModernCard {
                    AuthForm(
                        uiState = uiState,
                        email = email,
                        onEmailChange = authViewModel::updateEmail,
                        password = password,
                        onPasswordChange = authViewModel::updatePassword,
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = authViewModel::updateConfirmPassword,
                        name = name,
                        onNameChange = authViewModel::updateName,
                        selectedRole = selectedRole,
                        onRoleSelected = authViewModel::updateSelectedRole,
                        passwordVisible = passwordVisible,
                        onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                        confirmPasswordVisible = confirmPasswordVisible,
                        onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        onForgotPasswordClick = { showForgotPassword = true },
                        onSubmit = {
                            if (uiState.isSignUpMode) {
                                authViewModel.signUp()
                            } else {
                                authViewModel.signIn()
                            }
                        },
                        focusManager = focusManager
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error and Success Messages
                uiState.error?.let { error ->
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ModernMessageCard(
                            message = error,
                            type = MessageType.ERROR,
                            onDismiss = { authViewModel.clearMessages() }
                        )
                    }
                }

                uiState.successMessage?.let { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ModernMessageCard(
                            message = message,
                            type = MessageType.SUCCESS,
                            onDismiss = { authViewModel.clearMessages() }
                        )
                    }
                }

                // Signup info
                AnimatedVisibility(
                    visible = uiState.isSignUpMode,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ModernAlertCard(
                        title = "Account Approval Required",
                        message = "New accounts require admin approval before access is granted. You will be notified once approved.",
                        alertType = AlertType.INFO
                    )
                }
            }
        }

        // Forgot Password Dialog
        if (showForgotPassword) {
            ForgotPasswordDialog(
                onDismiss = { showForgotPassword = false },
                onSendResetEmail = { resetEmail ->
                    authViewModel.resetPassword(resetEmail)
                    showForgotPassword = false
                }
            )
        }
    }
}

@Composable
private fun AppHeader(isSignUp: Boolean) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // App Icon and Title Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🗑️",
                    fontSize = 32.sp
                )
            }
            
            // App Name
            Text(
                text = "BlueBin",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Dynamic subtitle
        AnimatedContent(
            targetState = isSignUp,
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            }
        ) { isSignUpMode ->
            Text(
                text = if (isSignUpMode) 
                    "Join our waste management system" 
                else 
                    "Welcome back to your dashboard",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
    }
}

@Composable
private fun AuthModeToggle(
    isSignUp: Boolean,
    onToggle: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleButton(
                text = "Sign In",
                isSelected = !isSignUp,
                onClick = { if (isSignUp) onToggle() },
                modifier = Modifier.weight(1f)
            )
            
            ToggleButton(
                text = "Sign Up",
                isSelected = isSignUp,
                onClick = { if (!isSignUp) onToggle() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
        animationSpec = tween(300)
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300)
    )
    
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun AuthForm(
    uiState: AuthUiState,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityToggle: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onSubmit: () -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Form Header
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (uiState.isSignUpMode) "Create Account" else "Welcome Back",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = if (uiState.isSignUpMode) 
                    "Join the BlueBin waste management system" 
                else 
                    "Sign in to continue to your dashboard",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Form Fields
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field (signup only)
            AnimatedVisibility(
                visible = uiState.isSignUpMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ModernTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }

            // Email field
            ModernTextField(
                value = email,
                onValueChange = onEmailChange,
                label = "Email Address",
                leadingIcon = Icons.Default.Email,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            // Password field
            ModernTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = "Password",
                leadingIcon = Icons.Default.Lock,
                trailingIcon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                onTrailingIconClick = onPasswordVisibilityToggle,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (uiState.isSignUpMode) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { if (!uiState.isSignUpMode) onSubmit() }
                )
            )

            // Confirm password field (signup only)
            AnimatedVisibility(
                visible = uiState.isSignUpMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ModernTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = "Confirm Password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    onTrailingIconClick = onConfirmPasswordVisibilityToggle,
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
            }

            // Role selection (signup only)
            AnimatedVisibility(
                visible = uiState.isSignUpMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                RoleSelectionSection(
                    selectedRole = selectedRole,
                    onRoleSelected = onRoleSelected
                )
            }

            // Forgot password (login only)
            AnimatedVisibility(
                visible = !uiState.isSignUpMode,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Forgot Password?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { onForgotPasswordClick() }
                    )
                }
            }
        }

        // Submit button
        val isFormValid = email.isNotBlank() && password.isNotBlank() && 
                         (!uiState.isSignUpMode || (name.isNotBlank() && confirmPassword.isNotBlank()))
        
        ModernPrimaryButton(
            text = if (uiState.isSignUpMode) "Create Account" else "Sign In",
            onClick = onSubmit,
            isLoading = uiState.isLoading,
            enabled = isFormValid,
            icon = if (uiState.isSignUpMode) Icons.Default.PersonAdd else Icons.AutoMirrored.Filled.Login,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoleSelectionSection(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Select Your Role",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UserRole.values().forEach { role ->
                RoleCard(
                    role = role,
                    isSelected = selectedRole == role,
                    onSelected = { onRoleSelected(role) }
                )
            }
        }
    }
}

@Composable
private fun RoleCard(
    role: UserRole,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300)
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() },
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelected
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = role.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (role) {
                        UserRole.DRIVER -> "Collect waste and manage routes"
                        UserRole.TPS_OFFICER -> "Monitor TPS and manage collections"
                        UserRole.ADMIN -> "System administration and oversight"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = when (role) {
                    UserRole.DRIVER -> Icons.Default.LocalShipping
                    UserRole.TPS_OFFICER -> Icons.Default.BusinessCenter
                    UserRole.ADMIN -> Icons.Default.AdminPanelSettings
                },
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSendResetEmail: (String) -> Unit
) {
    var resetEmail by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Enter your email address and we'll send you a reset link.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                ModernTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendResetEmail(resetEmail) },
                enabled = resetEmail.isNotBlank(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Send Reset Link")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
} 