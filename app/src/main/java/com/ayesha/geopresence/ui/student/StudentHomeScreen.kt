package com.ayesha.geopresence.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.geofence.GeofenceEvent
import com.ayesha.geopresence.geofence.GeofenceUiState
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.ui.components.AttendanceHeroCard
import com.ayesha.geopresence.ui.components.AttendanceStatCard
import com.ayesha.geopresence.ui.components.GeofenceStatusCard
import com.ayesha.geopresence.ui.components.GreetingHeader
import com.ayesha.geopresence.ui.components.QuickTile
import com.ayesha.geopresence.ui.permissions.PermissionBanner
import com.ayesha.geopresence.util.AttendanceStats
import com.ayesha.geopresence.util.TimeFormat

@Composable
fun StudentHomeScreen(
    user: AppUser?,
    attendance: StudentAttendanceUiState,
    permissionsComplete: Boolean,
    onSetupPermissions: () -> Unit,
    geofenceState: GeofenceUiState,
    lastEvent: GeofenceEvent?,
    onRetryGeofence: () -> Unit,
    onRetryAttendance: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenProfile: () -> Unit,
    onComingSoon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstName = user?.name?.trim()?.substringBefore(' ').orEmpty().ifBlank { "there" }
    val ready = attendance as? StudentAttendanceUiState.Ready
    val low = ready != null && ready.summary.total > 0 && ready.summary.percent < ready.threshold
    val alertCount = if (low) 1 else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GreetingHeader(
            name = user?.name.orEmpty(),
            title = "Hi, $firstName",
            subtitle = "Student",
            alertCount = alertCount,
            onBellClick = {
                onComingSoon(
                    if (low) "Your attendance is below ${ready?.threshold}%" else "No new alerts"
                )
            }
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

        when (attendance) {
            StudentAttendanceUiState.Loading -> LoadingCard()

            is StudentAttendanceUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBanner(attendance.message)
                Button(onClick = onRetryAttendance) { Text("Try again") }
            }

            is StudentAttendanceUiState.Ready -> {
                val s = attendance.summary
                val footnote = buildList {
                    if (s.total == 0) {
                        add("No attendance days recorded yet.")
                    } else {
                        if (s.late > 0) {
                            add("Includes ${s.late} late arrival${if (s.late == 1) "" else "s"}.")
                        }
                        if (low) add("Below ${attendance.threshold}%: risk of attendance shortage.")
                    }
                }.joinToString("\n").ifBlank { null }

                AttendanceHeroCard(
                    title = "Attendance Percentage",
                    percent = s.percent,
                    present = s.attended,
                    absent = s.absent,
                    total = s.total,
                    footnote = footnote,
                    warning = low
                )
                TodayCards(today = attendance.today)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickTile(
                label = "My Attendance",
                icon = Icons.Filled.CheckCircle,
                onClick = onOpenHistory,
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                label = "Alerts",
                icon = Icons.Filled.Notifications,
                onClick = {
                    onComingSoon(
                        if (low) "Your attendance is below ${ready?.threshold}%" else "No new alerts"
                    )
                },
                modifier = Modifier.weight(1f),
                badgeCount = alertCount
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            QuickTile(
                label = "Campus Map",
                icon = Icons.Filled.LocationOn,
                onClick = onOpenMap,
                modifier = Modifier.weight(1f)
            )
            QuickTile(
                label = "Profile",
                icon = Icons.Filled.Person,
                onClick = onOpenProfile,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    val colors = MaterialTheme.colorScheme
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLowest,
            contentColor = colors.onSurface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator() }
    }
}

private data class Badge(val text: String, val container: Color, val content: Color)

@Composable
internal fun TodayCards(today: AttendanceStats.DayEntry?) {
    val colors = MaterialTheme.colorScheme

    val entryBadge = when {
        today == null -> Badge("Not yet", colors.secondaryContainer, colors.onSecondaryContainer)
        today.status == AttendanceStatus.PRESENT ->
            Badge("On time", colors.primaryContainer, colors.onPrimaryContainer)
        today.status == AttendanceStatus.LATE ->
            Badge("Late", colors.tertiaryContainer, colors.onTertiaryContainer)
        else -> Badge("Absent", colors.errorContainer, colors.onErrorContainer)
    }

    val exitBadge = when {
        today == null -> Badge("Waiting", colors.secondaryContainer, colors.onSecondaryContainer)
        today.exitMillis == null ->
            Badge("On campus", colors.tertiaryContainer, colors.onTertiaryContainer)
        else -> Badge(
            "Total ${TimeFormat.duration(today.durationMinutes)}",
            colors.secondaryContainer, colors.onSecondaryContainer
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AttendanceStatCard(
            label = "Entry time",
            value = today?.entryMillis?.let { TimeFormat.clock(it) } ?: "—",
            badgeText = entryBadge.text,
            badgeContainerColor = entryBadge.container,
            badgeContentColor = entryBadge.content,
            modifier = Modifier.weight(1f)
        )
        AttendanceStatCard(
            label = "Exit time",
            value = today?.exitMillis?.let { TimeFormat.clock(it) } ?: "—",
            badgeText = exitBadge.text,
            badgeContainerColor = exitBadge.container,
            badgeContentColor = exitBadge.content,
            modifier = Modifier.weight(1f)
        )
    }
}