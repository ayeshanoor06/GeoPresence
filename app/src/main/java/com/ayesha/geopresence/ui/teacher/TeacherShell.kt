package com.ayesha.geopresence.ui.teacher

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.geofence.GeofenceViewModel
import com.ayesha.geopresence.ui.components.AppBackground
import com.ayesha.geopresence.ui.permissions.rememberPermissionState
import com.ayesha.geopresence.ui.profile.ProfileScreen
import com.ayesha.geopresence.ui.student.StudentAttendanceUiState
import com.ayesha.geopresence.ui.student.StudentAttendanceViewModel
import com.ayesha.geopresence.ui.student.StudentHistoryScreen
import com.ayesha.geopresence.util.AttendanceStats
import com.ayesha.geopresence.util.TimeFormat
import kotlinx.coroutines.launch
import java.time.YearMonth

private enum class TeacherTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Filled.Home),
    CLASSES("Classes", Icons.Filled.DateRange),
    PROFILE("Profile", Icons.Filled.Person)
}

@Composable
fun TeacherShell(
    user: AppUser?,
    onOpenMap: () -> Unit,
    onOpenPermissions: () -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var tab by rememberSaveable { mutableStateOf(TeacherTab.HOME) }
    var selectedClassId by rememberSaveable { mutableStateOf<String?>(null) }
    var showMyAttendance by rememberSaveable { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Teachers are tracked by the same geofence as students
    val permissionState = rememberPermissionState()
    val geofenceViewModel: GeofenceViewModel = viewModel()
    val geofenceState by geofenceViewModel.state.collectAsState()
    val lastEvent by geofenceViewModel.lastEvent.collectAsState()
    val geofencePermissionsReady = permissionState.status.foregroundLocation &&
            permissionState.status.backgroundLocation
    LaunchedEffect(geofencePermissionsReady, user?.uid) {
        if (geofencePermissionsReady) geofenceViewModel.ensureRegistered(user)
    }

    val dashboardViewModel: TeacherDashboardViewModel = viewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsState()
    val attendanceViewModel: StudentAttendanceViewModel = viewModel()
    val myAttendance by attendanceViewModel.uiState.collectAsState()
    LaunchedEffect(user?.uid) {
        user?.let {
            dashboardViewModel.start(it.uid)
            attendanceViewModel.start(it)
        }
    }

    val selectedClass = (dashboardState as? TeacherDashboardUiState.Ready)
        ?.classes?.firstOrNull { it.id == selectedClassId }

    BackHandler(enabled = selectedClassId != null || showMyAttendance || tab != TeacherTab.HOME) {
        when {
            selectedClassId != null -> selectedClassId = null
            showMyAttendance -> showMyAttendance = false
            else -> tab = TeacherTab.HOME
        }
    }

    AppBackground {
        when {
            selectedClass != null && user != null -> ClassDetailScreen(
                classInfo = selectedClass,
                teacher = user,
                onBack = { selectedClassId = null }
            )

            showMyAttendance -> MyAttendanceScreen(
                state = myAttendance,
                onRetry = { user?.let { attendanceViewModel.retry(it) } },
                onBack = { showMyAttendance = false }
            )

            else -> Scaffold(
                containerColor = Color.Transparent,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(containerColor = colors.surfaceContainerLowest) {
                        TeacherTab.entries.forEach { item ->
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
                    TeacherTab.HOME -> TeacherHomeScreen(
                        user = user,
                        dashboard = dashboardState,
                        myAttendance = myAttendance,
                        permissionsComplete = permissionState.status.allGranted,
                        onSetupPermissions = onOpenPermissions,
                        geofenceState = geofenceState,
                        lastEvent = lastEvent,
                        onRetryGeofence = { geofenceViewModel.ensureRegistered(user) },
                        onRetryClasses = { user?.let { dashboardViewModel.retry(it.uid) } },
                        onOpenClass = { selectedClassId = it.id },
                        onOpenClasses = { tab = TeacherTab.CLASSES },
                        onOpenMyAttendance = { showMyAttendance = true },
                        onOpenMap = onOpenMap,
                        onComingSoon = { message ->
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        modifier = contentModifier
                    )

                    TeacherTab.CLASSES -> TeacherClassesScreen(
                        dashboard = dashboardState,
                        onOpenClass = { selectedClassId = it.id },
                        onRetry = { user?.let { dashboardViewModel.retry(it.uid) } },
                        modifier = contentModifier
                    )

                    TeacherTab.PROFILE -> ProfileScreen(
                        user = user,
                        onSignOut = onSignOut,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyAttendanceScreen(
    state: StudentAttendanceUiState,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("My attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            (state as? StudentAttendanceUiState.Ready)?.let { MonthSummaryCard(it.history) }
            StudentHistoryScreen(
                state = state,
                onRetry = onRetry,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MonthSummaryCard(history: List<AttendanceStats.DayEntry>) {
    val colors = MaterialTheme.colorScheme
    val month = YearMonth.now()
    val days = history.filter { YearMonth.from(it.date) == month }
    val attended = days.count { it.status != AttendanceStatus.ABSENT }
    val minutes = days.sumOf { it.durationMinutes }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column {
                Text("This month", style = MaterialTheme.typography.labelMedium)
                Text(
                    "$attended day${if (attended == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text("Time on campus", style = MaterialTheme.typography.labelMedium)
                Text(
                    TimeFormat.duration(minutes),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}