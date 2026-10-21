package com.ayesha.geopresence.ui.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayesha.geopresence.data.model.Department
import com.ayesha.geopresence.ui.auth.ErrorBanner
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampusMapScreen(
    onBack: () -> Unit,
    viewModel: CampusMapViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.surface,
        topBar = {
            TopAppBar(
                title = { Text("Campus geofence") },
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
                CampusMapUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

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
                        GeofenceMap(department = s.department, modifier = Modifier.fillMaxSize())
                        GeofenceInfoCard(
                            department = s.department,
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

@Composable
private fun GeofenceMap(department: Department, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()

    val center = LatLng(department.geofenceLat, department.geofenceLng)
    val zoom = remember(department) { zoomForRadius(department.radiusMeters, department.geofenceLat) }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, zoom)
    }
    val mapProperties = remember(dark) {
        MapProperties(mapStyleOptions = if (dark) MapStyleOptions(DARK_MAP_STYLE) else null)
    }
    val uiSettings = remember {
        MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false)
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

        SmallFloatingActionButton(
            onClick = {
                scope.launch {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(center, zoom))
                }
            },
            containerColor = colors.primaryContainer,
            contentColor = colors.onPrimaryContainer,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.LocationOn, contentDescription = "Recenter on campus")
        }
    }
}

@Composable
private fun GeofenceInfoCard(department: Department, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
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
            Text(
                text = department.name.ifBlank { "Campus" },
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Radius", value = "${department.radiusMeters.toInt()} m")
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
private fun StatusPill(
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

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