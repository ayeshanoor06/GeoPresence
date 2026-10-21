package com.ayesha.geopresence.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ayesha.geopresence.ui.components.HomePlaceholderScreen
import com.ayesha.geopresence.ui.map.CampusMapScreen
import androidx.compose.runtime.collectAsState
import com.ayesha.geopresence.ui.student.StudentShell


@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    val currentUser by authViewModel.currentUser.collectAsState()

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

    // Auto-login: runs once per app launch (not on rotation, not after sign-out).
    var sessionChecked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!sessionChecked) {
            sessionChecked = true
            authViewModel.checkSession { user ->
                if (user != null && navController.currentDestination?.route == Routes.LOGIN) {
                    goHome(user)
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        modifier = modifier
    ) {
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
            StudentShell(
                user = currentUser,
                onOpenMap = { navController.navigate(Routes.CAMPUS_MAP) },
                onSignOut = { signOut() }
            )
        }
        composable(Routes.TEACHER_HOME) {
            HomePlaceholderScreen(
                roleLabel = "Teacher",
                onSignOut = { signOut() },
                onOpenMap = { navController.navigate(Routes.CAMPUS_MAP) }
            )
        }
        composable(Routes.ADMIN_HOME) {
            HomePlaceholderScreen(
                roleLabel = "Admin",
                onSignOut = { signOut() },
                onOpenMap = { navController.navigate(Routes.CAMPUS_MAP) }
            )
        }
        composable(Routes.CAMPUS_MAP) {
            CampusMapScreen(onBack = { navController.popBackStack() })
        }
    }
}