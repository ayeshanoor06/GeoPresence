package com.ayesha.geopresence.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.ui.components.AppBackground
import com.ayesha.geopresence.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val permissionState = rememberPermissionState()
    val status = permissionState.status
    var anyDenied by remember { mutableStateOf(false) }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionState.refresh()
        if (!PermissionUtils.hasForegroundLocation(context)) anyDenied = true
    }
    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionState.refresh()
        if (!PermissionUtils.hasBackgroundLocation(context)) anyDenied = true
    }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        permissionState.refresh()
        if (!PermissionUtils.hasNotifications(context)) anyDenied = true
    }

    val needsBackground = PermissionUtils.needsBackgroundStep
    val needsNotifications = PermissionUtils.needsNotificationStep
    val totalSteps = 1 + (if (needsBackground) 1 else 0) + (if (needsNotifications) 1 else 0)
    val doneSteps = (if (status.foregroundLocation) 1 else 0) +
            (if (needsBackground && status.backgroundLocation) 1 else 0) +
            (if (needsNotifications && status.notifications) 1 else 0)
    val allDone = doneSteps == totalSteps

    AppBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Automatic attendance") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Hero card ──
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = if (allDone) "You’re all set" else "Turn on automatic attendance",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (allDone)
                                "GeoPresence will mark your attendance when you arrive at campus."
                            else
                                "GeoPresence needs a few permissions to notice when you arrive at " +
                                        "or leave campus. Your location is used only for this.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(
                            progress = { doneSteps / totalSteps.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = colors.onPrimary,
                            trackColor = colors.onPrimary.copy(alpha = 0.25f)
                        )
                        Text(
                            text = "$doneSteps of $totalSteps done",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // ── Step 1: location ──
                PermissionStepCard(
                    number = 1,
                    title = "Location access",
                    description = "Lets GeoPresence know when you reach campus. When Android asks, " +
                            "choose “Precise” and “While using the app”.",
                    granted = status.foregroundLocation,
                    enabled = true,
                    buttonText = "Allow location",
                    onClick = {
                        foregroundLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )

                // ── Step 2: background location ──
                if (needsBackground) {
                    PermissionStepCard(
                        number = 2,
                        title = "Allow all the time",
                        description = if (status.foregroundLocation)
                            "Needed so attendance is marked even when the app is closed. " +
                                    "Settings will open: choose “Allow all the time”, then come back."
                        else
                            "Allow location first, then this step unlocks.",
                        granted = status.backgroundLocation,
                        enabled = status.foregroundLocation,
                        buttonText = "Allow all the time",
                        onClick = {
                            backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    )
                }

                // ── Step 3: notifications ──
                if (needsNotifications) {
                    PermissionStepCard(
                        number = if (needsBackground) 3 else 2,
                        title = "Notifications",
                        description = "Tells you when your attendance is marked and when it drops " +
                                "below the minimum.",
                        granted = status.notifications,
                        enabled = true,
                        buttonText = "Allow notifications",
                        onClick = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }

                // ── Blocked card ──
                if (anyDenied && !allDone) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = colors.errorContainer,
                            contentColor = colors.onErrorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Warning, contentDescription = null)
                                Text(
                                    "Permission blocked?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "After a few refusals Android stops asking. Open App settings, " +
                                        "then Permissions, then Location, and choose “Allow all the time”.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Button(
                                onClick = { PermissionUtils.openAppSettings(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.error,
                                    contentColor = colors.onError
                                )
                            ) { Text("Open app settings") }
                        }
                    }
                }

                // ── Battery tip ──
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = colors.surfaceContainerLowest,
                        contentColor = colors.onSurface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "Tip for Xiaomi, Oppo, Vivo and Samsung phones",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "These phones often stop apps in the background. In App settings, " +
                                    "open Battery and set GeoPresence to “Unrestricted”.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
                        TextButton(onClick = { PermissionUtils.openAppSettings(context) }) {
                            Text("Open app settings")
                        }
                    }
                }

                if (allDone) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) { Text("Done", style = MaterialTheme.typography.titleMedium) }
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun PermissionStepCard(
    number: Int,
    title: String,
    description: String,
    granted: Boolean,
    enabled: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
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
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = if (granted) colors.primary else colors.primaryContainer,
                contentColor = if (granted) colors.onPrimary else colors.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (granted) {
                        Icon(Icons.Filled.Check, contentDescription = "Allowed")
                    } else {
                        Text(
                            "$number",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                if (granted) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primaryContainer,
                        contentColor = colors.onPrimaryContainer
                    ) {
                        Text(
                            "Allowed",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Button(onClick = onClick, enabled = enabled) { Text(buttonText) }
                }
            }
        }
    }
}

/** Amber banner shown on the home screen while setup is incomplete. */
@Composable
fun PermissionBanner(onSetup: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.tertiaryContainer,
            contentColor = colors.onTertiaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Warning, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Automatic attendance is off",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Allow location all the time so you’re marked present when you arrive.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onSetup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.tertiary,
                    contentColor = colors.onTertiary
                )
            ) { Text("Set up") }
        }
    }
}