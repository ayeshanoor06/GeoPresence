package com.ayesha.geopresence.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.ui.auth.ErrorBanner


@Composable
fun TeacherClassesScreen(
    dashboard: TeacherDashboardUiState,
    onOpenClass: (ClassInfo) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "My Classes",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(14.dp))

        when (dashboard) {
            TeacherDashboardUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is TeacherDashboardUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBanner(dashboard.message)
                Button(onClick = onRetry) { Text("Try again") }
            }

            is TeacherDashboardUiState.Ready -> {
                if (dashboard.classes.isEmpty()) {
                    Text(
                        "No classes are assigned to you yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(dashboard.classes, key = { it.id }) { info ->
                            ClassAttendanceCard(
                                info = info,
                                stat = dashboard.stats[info.sectionKey],
                                showDays = true,
                                onClick = { onOpenClass(info) }
                            )
                        }
                    }
                }
            }
        }
    }
}