package com.bluebin.presentation.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.R
import com.bluebin.data.model.UserRole
import com.bluebin.presentation.auth.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToMain: (UserRole) -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(authState.isAuthenticated, currentUser) {
        delay(2000) // Show splash for 2 seconds
        
        val user = currentUser
        when {
            // User is authenticated and approved
            authState.isAuthenticated && user != null && user.approved -> {
                onNavigateToMain(user.role)
            }
            // User exists but not approved or not authenticated
            user == null || !authState.isAuthenticated -> {
                onNavigateToAuth()
            }
            // User exists but not approved - still go to auth to show the message
            user != null && !user.approved -> {
                onNavigateToAuth()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Modern App Logo with card background
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(
                        Color.White,
                        shape = RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗑️",
                        fontSize = 64.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            Text(
                text = "BlueBin",
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                letterSpacing = 1.sp
            )
            
            Text(
                text = "Smart Waste Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF666666),
                letterSpacing = 0.5.sp
            )
            
            Spacer(modifier = Modifier.height(80.dp))
            
            // Modern loading indicator
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        Color.White,
                        shape = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Loading your dashboard...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF999999),
                fontWeight = FontWeight.Medium
            )
        }
    }
} 