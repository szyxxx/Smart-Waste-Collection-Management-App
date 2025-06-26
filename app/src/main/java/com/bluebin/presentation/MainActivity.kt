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

@Composable
fun BluebinApp(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    
    when {
        authState.isLoading -> {
            SplashScreen(
                onNavigateToAuth = { /* Will be handled by auth state change */ },
                onNavigateToMain = { /* Will be handled by auth state change */ }
            )
        }
        authState.user == null -> {
            AuthScreen(
                onNavigateToMain = { /* Will be handled by auth state change */ }
            )
        }
        else -> {
            // User is authenticated, route based on role
            when (authState.user?.role) {
                UserRole.ADMIN -> {
                    AdminDashboardScreen(
                        onNavigateToAuth = { authViewModel.signOut() }
                    )
                }
                UserRole.DRIVER -> {
                    DriverScreen()
                }
                UserRole.TPS_OFFICER -> {
                    TPSOfficerScreen(
                        onNavigateToAuth = { authViewModel.signOut() }
                    )
                }
                else -> {
                    // Fallback to auth screen for unknown roles
                    AuthScreen(
                        onNavigateToMain = { /* Will be handled by auth state change */ }
                    )
                }
            }
        }
    }
} 