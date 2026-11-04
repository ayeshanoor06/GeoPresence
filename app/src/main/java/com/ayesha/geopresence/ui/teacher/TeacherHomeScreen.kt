package com.ayesha.geopresence.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.geofence.GeofenceEvent
import com.ayesha.geopresence.geofence.GeofenceUiState
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.ui.components.GeofenceStatusCard
import com.ayesha.geopresence.ui.components.GreetingHeader
import com.ayesha.geopresence.ui.components.QuickTile
import com.ayesha.geopresence.ui.permissions.PermissionBanner
import com.ayesha.geopresence.ui.student.StudentAttendanceUiState
import com.ayesha.geopresence.ui.student.TodayCards
import java.time.LocalDate

@Composable
fun TeacherHomeScreen(
    user: AppUser?,
    dashboard: TeacherDashboardUiState,
    myAttendance: StudentAttendanceUiState,
    permissionsComplete: Boolean,
    onSetupPermissions: () -> Unit,
    geofenceState: GeofenceUiState,
    lastEvent: GeofenceEvent?,
    onRetryGeofence: () -> Unit,
    onRetryClasses: () -> Unit,
    onOpenClass: (ClassInfo) -> Unit,
    onOpenClasses: () -> Unit,
    onOpenMyAttendance: () -> Unit,
    onOpenMap: () -> Unit,
    onComingSoon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val firstName = user?.name?.trim()?.substringBefore(' ').orEmpty().ifBlank { "Sir/Mam" }
    val todayWeekday = LocalDate.now().dayOfWeek
    val ready = dashboard as? TeacherDashboardUiState.Ready
    val todaysClasses = ready?.classes?.filter { it.runsOn(todayWeekday) }.orEmpty()
    val myToday = (myAttendance as? StudentAttendanceUiState.Ready)?.today

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GreetingHeader(
            name = user?.name.orEmpty(),
            title = "Hello, $firstName",
            subtitle = "Teacher",
            alertCount = 0,
            onBellClick = { onComingSoon("No new alerts") }
        )

        if (!permissionsComplete) {
            PermissionBanner(onSetup = onSetupPermissions)
        } else {
            GeofenceStatusCard(
                state = geofenceState,
                lastEvent = lastEvent,
                onRetry = onRetryGeofence
            )
        }

        // ── Today's classes summary ──
        ElevatedCard(
            onClick = onOpenClasses,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colors.surfaceContainerLowest,
                contentColor = colors.onSurface
            ),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Today’s Classes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = when {
                            ready == null -> "Loading…"
                            todaysClasses.isEmpty() -> "No classes today"
                            else -> "${todaysClasses.size} Class${if (todaysClasses.size == 1) "" else "es"}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = colors.primary
                )
            }
        }

        // ── My own attendance today ──
        TodayCards(today = myToday)

        // ── Class attendance ──
        Text(
            text = "Class Attendance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )

        when (dashboard) {
            TeacherDashboardUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is TeacherDashboardUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBanner(dashboard.message)
                Button(onClick = onRetryClasses) { Text("Try again") }
            }

            is TeacherDashboardUiState.Ready -> {
                if (dashboard.classes.isEmpty()) {
                    Text(
                        "No classes are assigned to you yet. Ask the department admin to assign your classes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                } else if (todaysClasses.isEmpty()) {
                    Text(
                        "No classes scheduled today. Open the Classes tab to see them all.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    todaysClasses.forEach { info ->
                        ClassAttendanceCard(
                            info = info,
                            stat = dashboard.stats[info.sectionKey],
                            showDays = false,
                            onClick = { onOpenClass(info) }
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickTile(
                label = "My Attendance",
                icon = Icons.Filled.CheckCircle,
                onClick = onOpenMyAttendance,
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                label = "Campus Map",
                icon = Icons.Filled.LocationOn,
                onClick = onOpenMap,
                modifier = Modifier.weight(1f)
            )
        }
    }
}