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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onNavigateToMain: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.DRIVER) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }
    var screenVisible by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current

    // Show screen with animation
    LaunchedEffect(Unit) {
        delay(100)
        screenVisible = true
    }

    // Handle auth state changes
    LaunchedEffect(authState.user, authState.isAuthenticated) {
        if (authState.user != null && authState.isAuthenticated) {
            onNavigateToMain()
        }
    }

    // Clear validations when switching modes
    LaunchedEffect(isLogin) {
        authViewModel.clearValidationErrors()
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
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        AnimatedVisibility(
            visible = screenVisible,
            enter = fadeIn(
                animationSpec = tween(800, easing = EaseOutQuart)
            ) + slideInVertically(
                animationSpec = tween(800, easing = EaseOutQuart),
                initialOffsetY = { it / 4 }
            ),
            exit = fadeOut() + slideOutVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // Header with smooth entrance
                AnimatedVisibility(
                    visible = screenVisible,
                    enter = fadeIn(
                        animationSpec = tween(600, delayMillis = 200, easing = EaseOutQuart)
                    ) + slideInVertically(
                        animationSpec = tween(600, delayMillis = 200, easing = EaseOutQuart),
                        initialOffsetY = { -it / 3 }
                    )
                ) {
                    ModernAuthHeader(isLogin = isLogin)
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Main content card with staggered animation
                AnimatedVisibility(
                    visible = screenVisible,
                    enter = fadeIn(
                        animationSpec = tween(600, delayMillis = 400, easing = EaseOutQuart)
                    ) + slideInVertically(
                        animationSpec = tween(600, delayMillis = 400, easing = EaseOutQuart),
                        initialOffsetY = { it / 3 }
                    )
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Auth Mode Toggle with smooth transition
                        AnimatedContent(
                            targetState = isLogin,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400, easing = EaseInOutQuart)) togetherWith
                                fadeOut(animationSpec = tween(400, easing = EaseInOutQuart))
                            },
                            label = "authToggle"
                        ) { isLoginMode ->
                            ModernAuthToggle(
                                isLogin = isLoginMode,
                                onToggle = { 
                                    isLogin = it
                                    authViewModel.setSignUpMode(!it)
                                }
                            )
                        }

                        // Main Auth Form with content animation
                        ModernCard {
                            AnimatedContent(
                                targetState = isLogin,
                                transitionSpec = {
                                    (slideInHorizontally(
                                        animationSpec = tween(500, easing = EaseInOutQuart),
                                        initialOffsetX = { if (targetState) -it else it }
                                    ) + fadeIn(animationSpec = tween(500, easing = EaseInOutQuart))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(500, easing = EaseInOutQuart),
                                        targetOffsetX = { if (targetState) it else -it }
                                    ) + fadeOut(animationSpec = tween(500, easing = EaseInOutQuart)))
                                },
                                label = "authForm"
                            ) { isLoginMode ->
                                AuthFormContent(
                                    isLogin = isLoginMode,
                                    authState = authState,
                                    email = email,
                                    onEmailChange = { email = it },
                                    password = password,
                                    onPasswordChange = { password = it },
                                    confirmPassword = confirmPassword,
                                    onConfirmPasswordChange = { confirmPassword = it },
                                    name = name,
                                    onNameChange = { name = it },
                                    selectedRole = selectedRole,
                                    onRoleSelected = { selectedRole = it },
                                    passwordVisible = passwordVisible,
                                    onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                                    confirmPasswordVisible = confirmPasswordVisible,
                                    onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                                    onForgotPasswordClick = { showForgotPassword = true },
                                    focusManager = focusManager,
                                    onSubmit = {
                                        if (isLoginMode) {
                                            authViewModel.signIn(email, password)
                                        } else {
                                            authViewModel.signUp(email, password, confirmPassword, name, selectedRole)
                                        }
                                    }
                                )
                            }
                        }

                        // Messages with entrance animation
                        AnimatedVisibility(
                            visible = authState.error != null,
                            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                        ) {
                            authState.error?.let { error ->
                                ModernMessageCard(
                                    message = error,
                                    type = MessageType.ERROR,
                                    onDismiss = { authViewModel.clearError() }
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = authState.successMessage != null,
                            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
                        ) {
                            authState.successMessage?.let { message ->
                                ModernMessageCard(
                                    message = message,
                                    type = MessageType.SUCCESS,
                                    onDismiss = { authViewModel.clearMessage() }
                                )
                            }
                        }

                        // Additional info for registration with smooth entrance
                        AnimatedVisibility(
                            visible = !isLogin,
                            enter = expandVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                                   fadeIn(animationSpec = tween(400, easing = EaseOutQuart)),
                            exit = shrinkVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                                  fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
                        ) {
                            ModernAlertCard(
                                title = "Account Approval Required",
                                message = "New accounts require admin approval before access is granted. You will be notified once your account is approved.",
                                alertType = AlertType.INFO
                            )
                        }
                    }
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
private fun ModernAuthHeader(isLogin: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Logo with subtle animation
        val logoScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "logoScale"
        )
        
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🗑️",
                fontSize = 40.sp,
                modifier = Modifier.scale(logoScale)
            )
        }
        
        // Dynamic title with smooth transition
        AnimatedContent(
            targetState = isLogin,
            transitionSpec = {
                fadeIn(tween(400, easing = EaseInOutQuart)) togetherWith
                fadeOut(tween(400, easing = EaseInOutQuart))
            },
            label = "headerTitle"
        ) { isLoginMode ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "BlueBin",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
                
                Text(
                    text = if (isLoginMode) 
                        "Welcome back to your dashboard" 
                    else 
                        "Join our waste management team",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ModernAuthToggle(
    isLogin: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToggleButton(
                text = "Sign In",
                isSelected = isLogin,
                onClick = { onToggle(true) },
                modifier = Modifier.weight(1f)
            )
            
            ToggleButton(
                text = "Sign Up",
                isSelected = !isLogin,
                onClick = { onToggle(false) },
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
        animationSpec = tween(300, easing = EaseInOutQuart),
        label = "backgroundColor"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(300, easing = EaseInOutQuart),
        label = "textColor"
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
private fun AuthFormContent(
    isLogin: Boolean,
    authState: AuthUiState,
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
    focusManager: androidx.compose.ui.focus.FocusManager,
    onSubmit: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Form Title with animation
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = if (isLogin) "Welcome Back" else "Create Account",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = if (isLogin) 
                    "Sign in to continue to your dashboard" 
                else 
                    "Join BlueBin waste management system",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Form fields with staggered animations
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Name field (register only)
            AnimatedVisibility(
                visible = !isLogin,
                enter = expandVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                       fadeIn(animationSpec = tween(400, easing = EaseOutQuart)),
                exit = shrinkVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                      fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModernTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = "Full Name",
                        leadingIcon = Icons.Default.Person,
                        isError = authState.nameValidation != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )
                    
                    authState.nameValidation?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Email field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    label = "Email Address",
                    leadingIcon = Icons.Default.Email,
                    isError = authState.emailValidation != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )
                
                authState.emailValidation?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Password field
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ModernTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = "Password",
                    leadingIcon = Icons.Default.Lock,
                    trailingIcon = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    onTrailingIconClick = onPasswordVisibilityToggle,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    isError = authState.passwordValidation != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = if (isLogin) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) },
                        onDone = { if (isLogin) onSubmit() }
                    )
                )
                
                authState.passwordValidation?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Confirm password field (register only)
            AnimatedVisibility(
                visible = !isLogin,
                enter = expandVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                       fadeIn(animationSpec = tween(400, easing = EaseOutQuart)),
                exit = shrinkVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                      fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
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

            // Role selection (register only)
            AnimatedVisibility(
                visible = !isLogin,
                enter = expandVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                       fadeIn(animationSpec = tween(400, easing = EaseOutQuart)),
                exit = shrinkVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                      fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
            ) {
                RoleSelectionCard(
                    selectedRole = selectedRole,
                    onRoleSelected = onRoleSelected
                )
            }

            // Forgot password (login only)
            AnimatedVisibility(
                visible = isLogin,
                enter = expandVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                       fadeIn(animationSpec = tween(400, easing = EaseOutQuart)),
                exit = shrinkVertically(animationSpec = tween(400, easing = EaseOutQuart)) + 
                      fadeOut(animationSpec = tween(400, easing = EaseOutQuart))
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
                         (isLogin || (name.isNotBlank() && confirmPassword.isNotBlank()))
        
        ModernPrimaryButton(
            text = if (isLogin) "Sign In" else "Create Account",
            onClick = onSubmit,
            isLoading = authState.isLoading,
            enabled = isFormValid,
            icon = if (isLogin) Icons.Default.Login else Icons.Default.PersonAdd,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun RoleSelectionCard(
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
                val isSelected = selectedRole == role
                val backgroundColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    animationSpec = tween(300, easing = EaseInOutQuart),
                    label = "backgroundColor"
                )
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRoleSelected(role) },
                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isSelected) 2.dp else 0.dp
                    )
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
                            onClick = { onRoleSelected(role) }
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
                                    UserRole.DRIVER -> "Collect waste and manage collection routes"
                                    UserRole.TPS_OFFICER -> "Monitor TPS status and manage collections"
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
                    text = "Enter your email address and we'll send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = resetEmail,
                    onValueChange = { resetEmail = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp)
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