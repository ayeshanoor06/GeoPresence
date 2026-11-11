package com.ayesha.geopresence.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.ui.components.AttendanceRing
import com.ayesha.geopresence.ui.components.AttendanceStatCard
import com.ayesha.geopresence.ui.components.GreetingHeader

@Composable
fun AdminHomeScreen(
    user: AppUser?,
    state: AdminDashboardUiState,
    onRetry: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenMockLog: () -> Unit,
    onOpenGeofence: () -> Unit,
    onOpenClasses: () -> Unit,
    onComingSoon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val firstName = user?.name?.trim()?.substringBefore(' ').orEmpty().ifBlank { "Admin" }
    val stats = (state as? AdminDashboardUiState.Ready)?.stats

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GreetingHeader(
            name = user?.name.orEmpty(),
            title = "Welcome, $firstName",
            subtitle = "Department Administrator",
            alertCount = stats?.pendingTeachers ?: 0,
            onBellClick = onOpenUsers
        )

        when (state) {
            AdminDashboardUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(40.dp),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is AdminDashboardUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBanner(state.message)
                Button(onClick = onRetry) { Text("Try again") }
            }

            is AdminDashboardUiState.Ready -> {
                val s = state.stats

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AttendanceStatCard(
                        label = "Total Students",
                        value = "${s.totalStudents}",
                        badgeText = "Active",
                        badgeContainerColor = colors.primaryContainer,
                        badgeContentColor = colors.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                    AttendanceStatCard(
                        label = "Total Teachers",
                        value = "${s.totalTeachers}",
                        badgeText = if (s.pendingTeachers > 0) "${s.pendingTeachers} pending"
                        else "All approved",
                        badgeContainerColor = if (s.pendingTeachers > 0) colors.tertiaryContainer
                        else colors.primaryContainer,
                        badgeContentColor = if (s.pendingTeachers > 0) colors.onTertiaryContainer
                        else colors.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }

                DepartmentStatsCard(s)

                ManagementCard(
                    stats = s,
                    onOpenUsers = onOpenUsers,
                    onOpenMockLog = onOpenMockLog,
                    onOpenGeofence = onOpenGeofence,
                    onOpenClasses = onOpenClasses,
                    onComingSoon = onComingSoon
                )
            }
        }
    }
}

@Composable
private fun DepartmentStatsCard(s: AdminStats) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLowest,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column {
                Text(
                    "Department Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Today · students who have arrived",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                AttendanceRing(
                    percent = s.studentPercent,
                    trackColor = colors.primaryContainer,
                    progressColor = colors.primary,
                    textColor = colors.onSurface,
                    diameter = 112.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LegendRow("Present", s.presentStudents, colors.primary)
                    LegendRow("Late", s.lateStudents, colors.tertiary)
                    LegendRow("Not arrived", s.notArrivedStudents, colors.outline)
                }
            }

            HorizontalDivider(color = colors.outlineVariant)

            SummaryLine("Teachers present", "${s.teachersPresent} / ${s.totalTeachers}")
            SummaryLine("On campus now", "${s.onCampusNow}")
        }
    }
}

@Composable
private fun LegendRow(label: String, value: Int, dot: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(dot, CircleShape)
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ManagementCard(
    stats: AdminStats,
    onOpenUsers: () -> Unit,
    onOpenMockLog: () -> Unit,
    onOpenGeofence: () -> Unit,
    onOpenClasses: () -> Unit,
    onComingSoon: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLowest,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            ManageRow(
                icon = Icons.Filled.LocationOn,
                title = "Manage Geofence",
                subtitle = "Edit the campus boundary on the map",
                onClick = onOpenGeofence
            )
            HorizontalDivider(color = colors.outlineVariant)
            ManageRow(
                icon = Icons.Filled.DateRange,
                title = "Class Management",
                subtitle = "Create classes and assign teachers",
                onClick = onOpenClasses
            )
            HorizontalDivider(color = colors.outlineVariant)
            ManageRow(
                icon = Icons.Filled.Person,
                title = "User Management",
                subtitle = "Approve teachers, assign sections",
                badge = if (stats.pendingTeachers > 0) "${stats.pendingTeachers} pending" else null,
                onClick = onOpenUsers
            )
            HorizontalDivider(color = colors.outlineVariant)
            ManageRow(
                icon = Icons.Filled.Warning,
                title = "Fake GPS attempts",
                subtitle = "Rejected attempts to cheat attendance",
                badge = if (stats.fakeGpsToday > 0) "${stats.fakeGpsToday} today" else null,
                badgeIsWarning = true,
                onClick = onOpenMockLog
            )
            HorizontalDivider(color = colors.outlineVariant)
            ManageRow(
                icon = Icons.AutoMirrored.Filled.ListIcon,
                title = "Reports (PDF Export)",
                subtitle = "Daily, weekly and monthly reports",
                onClick = { onComingSoon("Reports arrive in Step 10C") }
            )
        }
    }
}

@Composable
private fun ManageRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    badge: String? = null,
    badgeIsWarning: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            modifier = Modifier.size(44.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        if (badge != null) {
            Surface(
                shape = CircleShape,
                color = if (badgeIsWarning) colors.errorContainer else colors.tertiaryContainer,
                contentColor = if (badgeIsWarning) colors.onErrorContainer else colors.onTertiaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant
        )
    }
}