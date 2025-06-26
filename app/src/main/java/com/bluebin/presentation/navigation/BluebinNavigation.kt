package com.bluebin.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bluebin.data.model.UserRole
import com.bluebin.presentation.auth.AuthScreen
import com.bluebin.presentation.auth.AuthViewModel
import com.bluebin.presentation.admin.AdminDashboardScreen
import com.bluebin.presentation.tps.TPSOfficerScreen
import com.bluebin.presentation.driver.DriverScreen
import com.bluebin.presentation.splash.SplashScreen

@Composable
fun BluebinNavigation(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Handle sign out navigation from any screen
    LaunchedEffect(authState.isAuthenticated, currentUser) {
        val currentRoute = navController.currentDestination?.route
        // Only navigate to auth if user explicitly signed out
        // and we're not already on auth or splash screen
        if (!authState.isAuthenticated && currentUser == null && 
            currentRoute != Screen.Auth.route && currentRoute != Screen.Splash.route) {
            navController.navigate(Screen.Auth.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = { userRole ->
                    val route = when (userRole) {
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                        UserRole.TPS -> Screen.TPSOfficer.route
                        UserRole.DRIVER -> Screen.Driver.route
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Auth.route) {
            AuthScreen(
                onNavigateToMain = { userRole ->
                    val route = when (userRole) {
                        UserRole.ADMIN -> Screen.AdminDashboard.route
                        UserRole.TPS -> Screen.TPSOfficer.route
                        UserRole.DRIVER -> Screen.Driver.route
                    }
                    navController.navigate(route) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboardScreen(
                onNavigateToAuth = {
                    // This callback is kept for explicit logout action
                    // but navigation is primarily handled by LaunchedEffect above
                }
            )
        }

        composable(Screen.TPSOfficer.route) {
            TPSOfficerScreen(
                onNavigateToAuth = {
                    // This callback is kept for explicit logout action
                    // but navigation is primarily handled by LaunchedEffect above
                }
            )
        }

        composable(Screen.Driver.route) {
            DriverScreen(
                onNavigateToAuth = {
                    // This callback is kept for explicit logout action
                    // but navigation is primarily handled by LaunchedEffect above
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Auth : Screen("auth")
    object AdminDashboard : Screen("admin_dashboard")
    object TPSOfficer : Screen("tps_officer")
    object Driver : Screen("driver")
} 