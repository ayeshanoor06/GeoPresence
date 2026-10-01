package com.ayesha.geopresence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.ui.components.AttendanceStatCard
import com.ayesha.geopresence.ui.theme.GeoPresenceTheme
import android.util.Log
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        Log.d("GeoPresence", "Firebase project: ${FirebaseApp.getInstance().options.projectId}")
        enableEdgeToEdge()
        setContent {
            GeoPresenceTheme {
                ThemeCheckScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCheckScreen() {
    val colors = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colors.surface,
        topBar = {
            TopAppBar(
                title = { Text("GeoPresence") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.surface,
                    titleContentColor = colors.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Today's attendance",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )

            // Hero card — primaryContainer
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = colors.primaryContainer,
                    contentColor = colors.onPrimaryContainer
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("You are on campus", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Attendance marked automatically",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AttendanceStatCard(
                    label = "Entry time",
                    value = "8:42 AM",
                    badgeText = "On time",
                    badgeContainerColor = colors.primaryContainer,
                    badgeContentColor = colors.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatCard(
                    label = "Exit time",
                    value = "—",
                    badgeText = "Still inside",
                    badgeContainerColor = colors.secondaryContainer,
                    badgeContentColor = colors.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AttendanceStatCard(
                    label = "On campus",
                    value = "3h 20m",
                    badgeText = "Live",
                    badgeContainerColor = colors.tertiaryContainer,
                    badgeContentColor = colors.onTertiaryContainer,
                    modifier = Modifier.weight(1f)
                )
                AttendanceStatCard(
                    label = "Attendance",
                    value = "68%",
                    badgeText = "Below 75%",
                    badgeContainerColor = colors.errorContainer,
                    badgeContentColor = colors.onErrorContainer,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { }) { Text("Primary") }
                FilledTonalButton(onClick = { }) { Text("Tonal") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ThemeCheckPreview() {
    GeoPresenceTheme { ThemeCheckScreen() }
}