package com.bluebin.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.bluebin.data.model.UserRole
import com.bluebin.presentation.admin.AdminDashboardScreen
import com.bluebin.presentation.auth.AuthScreen
import com.bluebin.presentation.auth.AuthViewModel
import com.bluebin.presentation.driver.DriverScreen
import com.bluebin.presentation.splash.SplashScreen
import com.bluebin.presentation.tps.TPSOfficerScreen
import com.bluebin.ui.theme.BluebinTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BluebinTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BluebinApp()
                }
            }
        }
    }
}

enum class AppState {
    SPLASH,
    AUTH,
    MAIN
}

@Composable
fun BluebinApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    var appState by remember { mutableStateOf(AppState.SPLASH) }
    var currentRouteDetailsId by remember { mutableStateOf<String?>(null) }
    
    // Handle app state transitions
    LaunchedEffect(authState.isLoading, authState.isAuthenticated, authState.user) {
        when {
            // Stay in splash while loading or for initial delay
            authState.isLoading -> {
                appState = AppState.SPLASH
            }
            // Navigate to auth if not authenticated
            !authState.isAuthenticated || authState.user == null -> {
                appState = AppState.AUTH
            }
            // Navigate to main if authenticated and approved
            authState.isAuthenticated && authState.user?.approved == true -> {
                appState = AppState.MAIN
            }
            // Stay in auth for unapproved users
            else -> {
                appState = AppState.AUTH
            }
        }
    }
    
    when (appState) {
        AppState.SPLASH -> {
            SplashScreen(
                onNavigateToAuth = { appState = AppState.AUTH },
                onNavigateToMain = { appState = AppState.MAIN }
            )
        }
        
        AppState.AUTH -> {
            AuthScreen(
                onNavigateToMain = { appState = AppState.MAIN }
            )
        }
        
        AppState.MAIN -> {
            // User is authenticated, route based on role
            when (authState.user?.role) {
                UserRole.ADMIN -> {
                    if (currentRouteDetailsId != null) {
                        // Show RouteDetailsScreen
                        com.bluebin.presentation.admin.RouteDetailsScreen(
                            scheduleId = currentRouteDetailsId!!,
                            onBackClick = { currentRouteDetailsId = null }
                        )
                    } else {
                        // Show AdminDashboardScreen
                        AdminDashboardScreen(
                            onNavigateToAuth = { 
                                authViewModel.signOut()
                                appState = AppState.AUTH
                            },
                            onNavigateToRouteDetails = { scheduleId ->
                                currentRouteDetailsId = scheduleId
                            }
                        )
                    }
                }
                UserRole.DRIVER -> {
                    DriverScreen()
                }
                UserRole.TPS_OFFICER -> {
                    TPSOfficerScreen(
                        onNavigateToAuth = { 
                            authViewModel.signOut()
                            appState = AppState.AUTH
                        }
                    )
                }
                else -> {
                    // Fallback to auth screen for unknown roles
                    appState = AppState.AUTH
                }
            }
        }
    }
} 