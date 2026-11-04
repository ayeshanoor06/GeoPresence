package com.ayesha.geopresence.ui.map

import android.location.Location
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.location.LocationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayesha.geopresence.data.model.Department
import com.ayesha.geopresence.geofence.GeofenceManager
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.ui.permissions.rememberPermissionState
import com.ayesha.geopresence.util.PermissionUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapScreen(
    onBack: () -> Unit,
    viewModel: CampusMapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Re-checks automatically when the user returns from Settings
    val permissionState = rememberPermissionState()
    val hasLocation = permissionState.status.foregroundLocation

    // Live location, only while this screen is visible (saves battery)
    var userLocation by remember { mutableStateOf<Location?>(null) }
    LaunchedEffect(hasLocation) {
        if (!hasLocation) {
            userLocation = null
            return@LaunchedEffect
        }
        val lifecycle = (context as? ComponentActivity)?.lifecycle
        if (lifecycle != null) {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                LocationTracker.updates(context).collect { userLocation = it }
            }
        } else {
            LocationTracker.updates(context).collect { userLocation = it }
        }
    }

    Scaffold(
        containerColor = colors.surface,
        topBar = {
            TopAppBar(
                title = { Text("Campus map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                CampusMapUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is CampusMapUiState.Error -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ErrorBanner(s.message)
                    Button(onClick = { viewModel.load() }) { Text("Try again") }
                }

                is CampusMapUiState.Success -> {
                    if (!s.department.hasGeofence) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp)
                        ) {
                            ErrorBanner(
                                "Campus location is not set yet. Add geofenceLat and " +
                                        "geofenceLng to departments/${s.department.id} in Firestore."
                            )
                        }
                    } else {
                        GeofenceMap(
                            department = s.department,
                            showMyLocation = hasLocation,
                            userLocation = userLocation,
                            modifier = Modifier.fillMaxSize()
                        )
                        CampusBottomCard(
                            department = s.department,
                            hasLocation = hasLocation,
                            userLocation = userLocation,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────── Map ─────────────────────────

@Composable
private fun GeofenceMap(
    department: Department,
    showMyLocation: Boolean,
    userLocation: Location?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    val center = LatLng(department.geofenceLat, department.geofenceLng)
    val zoom = remember(department) { zoomForRadius(department.radiusMeters, department.geofenceLat) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, zoom)
    }
    val mapProperties = remember(dark, showMyLocation) {
        MapProperties(
            isMyLocationEnabled = showMyLocation,
            mapStyleOptions = if (dark) MapStyleOptions(DARK_MAP_STYLE) else null
        )
    }
    val uiSettings = remember {
        MapUiSettings(
            zoomControlsEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false
        )
    }

    Box(modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = uiSettings
        ) {
            Circle(
                center = center,
                radius = department.radiusMeters,
                fillColor = colors.primary.copy(alpha = 0.20f),
                strokeColor = colors.primary,
                strokeWidth = 4f
            )
            Marker(
                state = rememberUpdatedMarkerState(position = center),
                title = department.name,
                snippet = "Campus geofence center",
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallFloatingActionButton(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, zoom))
                    }
                },
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            ) {
                Icon(Icons.Filled.LocationOn, contentDescription = "Go to campus")
            }

            if (showMyLocation) {
                SmallFloatingActionButton(
                    onClick = {
                        userLocation?.let { loc ->
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(loc.latitude, loc.longitude), 17f
                                    )
                                )
                            }
                        }
                    },
                    containerColor = colors.surfaceContainerLowest,
                    contentColor = colors.primary
                ) {
                    MyLocationGlyph(tint = colors.primary)
                }
            }
        }
    }
}

/** A small crosshair icon drawn in code, so no extra icon library is needed. */
@Composable
private fun MyLocationGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val tick = size.minDimension / 5f
        val c = center
        drawCircle(tint, radius = size.minDimension / 2f - stroke * 2f, style = Stroke(stroke))
        drawCircle(tint, radius = size.minDimension / 6f)
        drawLine(tint, Offset(c.x, 0f), Offset(c.x, tick), stroke)
        drawLine(tint, Offset(c.x, size.height - tick), Offset(c.x, size.height), stroke)
        drawLine(tint, Offset(0f, c.y), Offset(tick, c.y), stroke)
        drawLine(tint, Offset(size.width - tick, c.y), Offset(size.width, c.y), stroke)
    }
}

// ───────────────────────── Bottom card ─────────────────────────

private sealed interface LocationStatus {
    data object NoPermission : LocationStatus
    data object Locating : LocationStatus
    data object Mocked : LocationStatus
    data object Inside : LocationStatus
    data class Outside(val metersFromBoundary: Float) : LocationStatus
}

@Composable
private fun CampusBottomCard(
    department: Department,
    hasLocation: Boolean,
    userLocation: Location?,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current

    // Same radius the geofence is registered with
    val radius = GeofenceManager.effectiveRadius(department.radiusMeters)

    val status = remember(hasLocation, userLocation, department) {
        when {
            !hasLocation -> LocationStatus.NoPermission
            userLocation == null -> LocationStatus.Locating
            LocationCompat.isMock(userLocation) -> LocationStatus.Mocked
            else -> {
                val result = FloatArray(1)
                Location.distanceBetween(
                    userLocation.latitude, userLocation.longitude,
                    department.geofenceLat, department.geofenceLng,
                    result
                )
                if (result[0] <= radius) LocationStatus.Inside
                else LocationStatus.Outside((result[0] - radius).toFloat())
            }
        }
    }

    val lateUntil = remember(department) {
        runCatching {
            LocalTime.parse(department.firstClassStart)
                .plusMinutes(department.graceMinutes.toLong()).toString()
        }.getOrDefault("--")
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLow,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            StatusRow(
                status = status,
                onOpenSettings = { PermissionUtils.openAppSettings(context) }
            )

            userLocation?.let { loc ->
                Text(
                    text = "Location accuracy ±${loc.accuracy.roundToInt()} m",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }

            HorizontalDivider(color = colors.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Radius", value = "${radius.toInt()} m")
                StatItem(label = "First class", value = department.firstClassStart)
                StatItem(label = "Grace", value = "${department.graceMinutes} min")
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    text = "Present ≤ ${department.firstClassStart}",
                    container = colors.primaryContainer,
                    content = colors.onPrimaryContainer
                )
                StatusPill(
                    text = "Late ≤ $lateUntil",
                    container = colors.tertiaryContainer,
                    content = colors.onTertiaryContainer
                )
            }
        }
    }
}

private data class StatusLook(
    val container: Color,
    val content: Color,
    val icon: ImageVector?,
    val title: String,
    val subtitle: String
)

@Composable
private fun StatusRow(status: LocationStatus, onOpenSettings: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    val look = when (status) {
        LocationStatus.Inside -> StatusLook(
            colors.primary, colors.onPrimary, Icons.Filled.Check,
            "You are inside the campus", "Attendance will be marked automatically."
        )
        is LocationStatus.Outside -> StatusLook(
            colors.secondaryContainer, colors.onSecondaryContainer, Icons.Filled.LocationOn,
            "You are outside the campus",
            "${formatDistance(status.metersFromBoundary)} from the campus boundary"
        )
        LocationStatus.Locating -> StatusLook(
            colors.primaryContainer, colors.onPrimaryContainer, null,
            "Finding your location…", "Make sure your phone’s GPS is on."
        )
        LocationStatus.NoPermission -> StatusLook(
            colors.tertiaryContainer, colors.onTertiaryContainer, Icons.Filled.Warning,
            "Location access needed", "Allow location to see where you are on the map."
        )
        LocationStatus.Mocked -> StatusLook(
            colors.error, colors.onError, Icons.Filled.Warning,
            "Fake GPS detected",
            "Attendance can’t be recorded while a mock location is active."
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = look.container,
            contentColor = look.content,
            modifier = Modifier.size(44.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (look.icon != null) {
                    Icon(look.icon, contentDescription = null)
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = look.content
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = look.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = look.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        if (status == LocationStatus.NoPermission) {
            TextButton(onClick = onOpenSettings) { Text("Settings") }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatusPill(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ───────────────────────── Helpers ─────────────────────────

private fun formatDistance(meters: Float): String =
    if (meters >= 1000f) String.format("%.1f km", meters / 1000f)
    else "${meters.roundToInt()} m"

/** Zoom level so the geofence circle fills roughly 60% of a phone screen width. */
private fun zoomForRadius(radiusMeters: Double, latitude: Double): Float {
    val metersPerDp = (radiusMeters * 2) / 230.0
    val zoom = ln(156543.03392 * cos(Math.toRadians(latitude)) / metersPerDp) / ln(2.0)
    return zoom.toFloat().coerceIn(12f, 19f)
}

private const val DARK_MAP_STYLE = """
[
 {"elementType":"geometry","stylers":[{"color":"#212121"}]},
 {"elementType":"labels.icon","stylers":[{"visibility":"off"}]},
 {"elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},
 {"elementType":"labels.text.stroke","stylers":[{"color":"#212121"}]},
 {"featureType":"administrative","elementType":"geometry","stylers":[{"color":"#757575"}]},
 {"featureType":"poi","elementType":"labels.text.fill","stylers":[{"color":"#757575"}]},
 {"featureType":"poi.park","elementType":"geometry","stylers":[{"color":"#181818"}]},
 {"featureType":"road","elementType":"geometry.fill","stylers":[{"color":"#2c2c2c"}]},
 {"featureType":"road","elementType":"labels.text.fill","stylers":[{"color":"#8a8a8a"}]},
 {"featureType":"road.highway","elementType":"geometry","stylers":[{"color":"#3c3c3c"}]},
 {"featureType":"transit","elementType":"geometry","stylers":[{"color":"#2f3948"}]},
 {"featureType":"water","elementType":"geometry","stylers":[{"color":"#000000"}]},
 {"featureType":"water","elementType":"labels.text.fill","stylers":[{"color":"#3d3d3d"}]}
]
"""