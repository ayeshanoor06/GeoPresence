package com.ayesha.geopresence.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.ui.components.AppBackground
import com.ayesha.geopresence.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

private enum class AdminTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    REPORTS("Reports", Icons.AutoMirrored.Filled.ListIcon),
    PROFILE("Profile", Icons.Filled.Person)
}

private enum class AdminSubScreen { NONE, USERS, MOCK_LOG, GEOFENCE, CLASSES }

@Composable
fun AdminShell(
    user: AppUser?,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableStateOf(AdminTab.HOME) }
    var subScreen by rememberSaveable { mutableStateOf(AdminSubScreen.NONE) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val dashboardViewModel: AdminDashboardViewModel = viewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { dashboardViewModel.start() }

    BackHandler(enabled = subScreen != AdminSubScreen.NONE || tab != AdminTab.HOME) {
        if (subScreen != AdminSubScreen.NONE) subScreen = AdminSubScreen.NONE
        else tab = AdminTab.HOME
    }

    AppBackground {
        when (subScreen) {
            AdminSubScreen.USERS -> UserManagementScreen(
                onBack = { subScreen = AdminSubScreen.NONE }
            )

            AdminSubScreen.MOCK_LOG -> MockGpsLogScreen(
                onBack = { subScreen = AdminSubScreen.NONE }
            )

            AdminSubScreen.GEOFENCE -> GeofenceEditorScreen(
                onBack = { subScreen = AdminSubScreen.NONE }
            )

            AdminSubScreen.CLASSES -> ClassManagementScreen(
                onBack = { subScreen = AdminSubScreen.NONE }
            )

            AdminSubScreen.NONE -> Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(containerColor = colors.surfaceContainerLowest) {
                        AdminTab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.onPrimaryContainer,
                                    selectedTextColor = colors.primary,
                                    indicatorColor = colors.primaryContainer
                                )
                            )
                        }
                    }
                }
            ) { innerPadding ->
                val contentModifier = Modifier.padding(innerPadding)
                when (tab) {
                    AdminTab.HOME -> AdminHomeScreen(
                        user = user,
                        state = dashboardState,
                        onRetry = dashboardViewModel::retry,
                        onOpenUsers = { subScreen = AdminSubScreen.USERS },
                        onOpenMockLog = { subScreen = AdminSubScreen.MOCK_LOG },
                        onOpenGeofence = { subScreen = AdminSubScreen.GEOFENCE },
                        onOpenClasses = { subScreen = AdminSubScreen.CLASSES },
                        onComingSoon = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        modifier = contentModifier
                    )

                    AdminTab.REPORTS -> ReportsPlaceholder(modifier = contentModifier)

                    AdminTab.PROFILE -> ProfileScreen(
                        user = user,
                        onSignOut = onSignOut,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportsPlaceholder(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colors.surfaceContainerLowest,
                contentColor = colors.onSurface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Attendance Reports", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Daily, weekly and monthly reports with PDF export arrive in Step 10C.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}