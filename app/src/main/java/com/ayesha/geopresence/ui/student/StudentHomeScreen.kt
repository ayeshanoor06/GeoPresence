package com.ayesha.geopresence.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.ui.components.AttendanceHeroCard
import com.ayesha.geopresence.ui.components.GreetingHeader
import com.ayesha.geopresence.ui.components.QuickTile
import com.ayesha.geopresence.ui.permissions.PermissionBanner
import kotlin.math.roundToInt

// Temporary sample numbers. Replaced with real attendance data in a later step.
private const val SAMPLE_PRESENT = 46
private const val SAMPLE_ABSENT = 4
private const val SAMPLE_ALERTS = 2

@Composable
fun StudentHomeScreen(
    user: AppUser?,
    permissionsComplete: Boolean,
    onSetupPermissions: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMap: () -> Unit,
    onOpenProfile: () -> Unit,
    onComingSoon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val firstName = user?.name?.trim()?.substringBefore(' ').orEmpty().ifBlank { "there" }
    val total = SAMPLE_PRESENT + SAMPLE_ABSENT
    val percent = ((SAMPLE_PRESENT * 100.0) / total).roundToInt()

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
            alertCount = SAMPLE_ALERTS,
            onBellClick = { onComingSoon("Alerts arrive in a later step") }
        )

        if (!permissionsComplete) {
            PermissionBanner(onSetup = onSetupPermissions)
        }

        AttendanceHeroCard(
            title = "Attendance Percentage",
            percent = percent,
            present = SAMPLE_PRESENT,
            absent = SAMPLE_ABSENT,
            total = total
        )

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
                onClick = { onComingSoon("Alerts arrive in a later step") },
                modifier = Modifier.weight(1f),
                badgeCount = SAMPLE_ALERTS
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