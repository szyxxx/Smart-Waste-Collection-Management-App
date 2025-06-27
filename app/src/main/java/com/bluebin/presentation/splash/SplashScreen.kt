package com.bluebin.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.presentation.auth.AuthViewModel
import com.bluebin.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    
    var showLogo by remember { mutableStateOf(false) }
    var showTitle by remember { mutableStateOf(false) }
    var showSubtitle by remember { mutableStateOf(false) }
    var showLoading by remember { mutableStateOf(false) }
    var splashComplete by remember { mutableStateOf(false) }
    
    // Animation sequence
    LaunchedEffect(Unit) {
        delay(100) // Small initial delay
        showLogo = true
        delay(600)
        showTitle = true
        delay(400)
        showSubtitle = true
        delay(500)
        showLoading = true
        delay(1500) // Minimum splash time to show all animations
        splashComplete = true
    }
    
    // Navigation logic - only trigger after splash animations complete
    LaunchedEffect(splashComplete, authState.isLoading, authState.isAuthenticated, authState.user) {
        if (splashComplete) {
            when {
                // If still loading auth state, wait a bit more
                authState.isLoading -> {
                    delay(500)
                }
                // User is authenticated and approved
                authState.isAuthenticated && authState.user?.approved == true -> {
                    onNavigateToMain()
                }
                // User needs authentication or approval
                else -> {
                    onNavigateToAuth()
                }
            }
        }
    }
    
    // Animated logo scale
    val logoScale by animateFloatAsState(
        targetValue = if (showLogo) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated App Logo
            AnimatedVisibility(
                visible = showLogo,
                enter = fadeIn(animationSpec = tween(800, easing = EaseOutQuart)) + 
                       scaleIn(animationSpec = tween(800, easing = EaseOutQuart)),
                exit = fadeOut() + scaleOut()
            ) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(logoScale)
                        .background(
                            Color.White,
                            shape = RoundedCornerShape(35.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(25.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🗑️",
                            fontSize = 56.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated Title
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(animationSpec = tween(800, easing = EaseOutQuart)) + 
                       scaleIn(animationSpec = tween(800, easing = EaseOutQuart)),
                exit = fadeOut()
            ) {
                Text(
                    text = "BlueBin",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = 2.sp
                )
            }
            
            // Animated Subtitle
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(animationSpec = tween(800, easing = EaseOutQuart)),
                exit = fadeOut()
            ) {
                Text(
                    text = "Smart Waste Management",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(64.dp))
            
            // Animated Loading Indicator
            AnimatedVisibility(
                visible = showLoading,
                enter = fadeIn(animationSpec = tween(600, easing = EaseOutQuart)),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Custom loading indicator
                    LoadingDots()
                    
                    Text(
                        text = when {
                            !splashComplete -> "Initializing..."
                            authState.isLoading -> "Checking authentication..."
                            authState.user?.approved == false -> "Account pending approval"
                            else -> "Setting up your workspace..."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        
        // Version info at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showSubtitle,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 1000))
            ) {
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun LoadingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingDots")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.4f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOutQuart),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot$index"
            )
            
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = EaseInOutQuart),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "alpha$index"
            )
            
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scale)
                    .background(
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = alpha),
                        CircleShape
                    )
            )
        }
    }
} 