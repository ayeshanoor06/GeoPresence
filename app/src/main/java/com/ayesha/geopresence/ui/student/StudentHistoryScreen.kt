package com.ayesha.geopresence.ui.student

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.util.AttendanceStats
import com.ayesha.geopresence.util.TimeFormat
import java.time.LocalDate

private enum class HistoryFilter(val label: String) {
    ALL("All"), PRESENT("Present"), LATE("Late"), ABSENT("Absent")
}

@Composable
fun StudentHistoryScreen(
    state: StudentAttendanceUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var filter by rememberSaveable { mutableStateOf(HistoryFilter.ALL) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Attendance History",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurface
        )
        Spacer(Modifier.height(14.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            HistoryFilter.entries.forEachIndexed { index, item ->
                SegmentedButton(
                    selected = filter == item,
                    onClick = { filter = item },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = HistoryFilter.entries.size
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primary,
                        activeContentColor = colors.onPrimary,
                        inactiveContainerColor = colors.surfaceContainerLowest.copy(alpha = 0.6f)
                    ),
                    label = { Text(item.label) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        when (state) {
            StudentAttendanceUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            is StudentAttendanceUiState.Error -> Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBanner(state.message)
                Button(onClick = onRetry) { Text("Try again") }
            }

            is StudentAttendanceUiState.Ready -> {
                val items = remember(state, filter) {
                    state.history.filter {
                        when (filter) {
                            HistoryFilter.ALL -> true
                            HistoryFilter.PRESENT -> it.status == AttendanceStatus.PRESENT
                            HistoryFilter.LATE -> it.status == AttendanceStatus.LATE
                            HistoryFilter.ABSENT -> it.status == AttendanceStatus.ABSENT
                        }
                    }
                }

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (filter == HistoryFilter.ALL)
                                "No attendance records yet.\nThey appear here automatically when you arrive on campus."
                            else "No ${filter.label.lowercase()} days.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = colors.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(items, key = { it.date.toString() }) { entry ->
                            HistoryRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: AttendanceStats.DayEntry) {
    val colors = MaterialTheme.colorScheme

    val range = when {
        !entry.recorded -> "No campus entry recorded"
        entry.entryMillis == null -> "Marked manually by a teacher"
        entry.exitMillis != null ->
            "${TimeFormat.clock(entry.entryMillis)} – ${TimeFormat.clock(entry.exitMillis)}"
        entry.date == LocalDate.now() -> "${TimeFormat.clock(entry.entryMillis)} – on campus"
        else -> "${TimeFormat.clock(entry.entryMillis)} – exit not recorded"
    }

    val (chipText, chipContainer, chipContent) = when (entry.status) {
        AttendanceStatus.PRESENT -> Triple("Present", colors.primaryContainer, colors.onPrimaryContainer)
        AttendanceStatus.LATE -> Triple("Late", colors.tertiaryContainer, colors.onTertiaryContainer)
        AttendanceStatus.ABSENT -> Triple("Absent", colors.errorContainer, colors.onErrorContainer)
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLowest,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = TimeFormat.dayTitle(entry.date),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = range,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                if (entry.durationMinutes > 0) {
                    Text(
                        text = "On campus ${TimeFormat.duration(entry.durationMinutes)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
                entry.note?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
            StatusChip(chipText, chipContainer, chipContent)
        }
    }
}

@Composable
private fun StatusChip(text: String, container: Color, content: Color) {
    Surface(shape = CircleShape, color = container, contentColor = content) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}