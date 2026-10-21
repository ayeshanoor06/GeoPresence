package com.ayesha.geopresence.ui.student

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.ui.components.AppBackground
import com.ayesha.geopresence.ui.profile.ProfileScreen
import kotlinx.coroutines.launch

private enum class StudentTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    HISTORY("History", Icons.Filled.DateRange),
    PROFILE("Profile", Icons.Filled.Person)
}

@Composable
fun StudentShell(
    user: AppUser?,
    onOpenMap: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableStateOf(StudentTab.HOME) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = tab != StudentTab.HOME) { tab = StudentTab.HOME }

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(containerColor = colors.surfaceContainerLowest) {
                    StudentTab.entries.forEach { item ->
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
                StudentTab.HOME -> StudentHomeScreen(
                    user = user,
                    onOpenHistory = { tab = StudentTab.HISTORY },
                    onOpenMap = onOpenMap,
                    onOpenProfile = { tab = StudentTab.PROFILE },
                    onComingSoon = { message ->
                        scope.launch { snackbarHostState.showSnackbar(message) }
                    },
                    modifier = contentModifier
                )

                StudentTab.HISTORY -> ComingSoonPane(
                    title = "Attendance History",
                    message = "Your daily records with All / Present / Absent filters arrive in Step 8.",
                    modifier = contentModifier
                )

                StudentTab.PROFILE -> ProfileScreen(
                    user = user,
                    onSignOut = onSignOut,
                    modifier = contentModifier
                )
            }
        }
    }
}

@Composable
private fun ComingSoonPane(title: String, message: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
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
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}