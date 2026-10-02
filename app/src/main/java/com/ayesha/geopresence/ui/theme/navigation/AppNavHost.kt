package com.ayesha.geopresence.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.ui.auth.AuthViewModel
import com.ayesha.geopresence.ui.auth.LoginScreen
import com.ayesha.geopresence.ui.auth.PendingApprovalScreen
import com.ayesha.geopresence.ui.auth.RegisterScreen
import com.ayesha.geopresence.ui.auth.SplashScreen
import com.ayesha.geopresence.ui.components.HomePlaceholderScreen

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    fun goHome(user: AppUser) {
        navController.navigate(Routes.homeFor(user)) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    fun signOut() {
        authViewModel.signOut()
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        modifier = modifier
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                viewModel = authViewModel,
                onResult = { user ->
                    if (user == null) {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    } else {
                        goHome(user)
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoggedIn = { user -> goHome(user) },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                viewModel = authViewModel,
                onRegistered = { user -> goHome(user) },
                onBackToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.PENDING_APPROVAL) {
            PendingApprovalScreen(
                viewModel = authViewModel,
                onApproved = { user -> goHome(user) },
                onSignOut = { signOut() }
            )
        }
        composable(Routes.STUDENT_HOME) {
            HomePlaceholderScreen(roleLabel = "Student", onSignOut = { signOut() })
        }
        composable(Routes.TEACHER_HOME) {
            HomePlaceholderScreen(roleLabel = "Teacher", onSignOut = { signOut() })
        }
        composable(Routes.ADMIN_HOME) {
            HomePlaceholderScreen(roleLabel = "Admin", onSignOut = { signOut() })
        }
    }
}