package com.ayesha.geopresence.ui.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceSource
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.ui.components.AttendanceRing
import com.ayesha.geopresence.util.TimeFormat
import java.time.LocalDate
import kotlin.math.roundToInt

private enum class StatusFilter(val label: String) {
    ALL("All"), PRESENT("Present"), LATE("Late"), ABSENT("Absent")
}

private fun AttendanceStatus.label(): String =
    name.lowercase().replaceFirstChar { it.uppercase() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailScreen(
    classInfo: ClassInfo,
    teacher: AppUser,
    onBack: () -> Unit
) {
    val viewModel: ClassDetailViewModel = viewModel(key = classInfo.id)
    LaunchedEffect(classInfo.id) { viewModel.start(classInfo, teacher) }

    val state by viewModel.uiState.collectAsState()
    val date by viewModel.date.collectAsState()
    val message by viewModel.message.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val colors = MaterialTheme.colorScheme

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    var filter by rememberSaveable { mutableStateOf(StatusFilter.ALL) }
    var onCampusOnly by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StudentRow?>(null) }

    val isToday = date == LocalDate.now()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(classInfo.title, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${classInfo.sectionLabel} · ${classInfo.timeLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val s = state) {
                ClassDetailUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                is ClassDetailUiState.Error -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                ) {
                    ErrorBanner(
                        s.message + "\nCheck that this section is in your assignedSectionKeys."
                    )
                }

                is ClassDetailUiState.Ready -> {
                    val rows = remember(s, filter, onCampusOnly, isToday) {
                        s.rows.filter { row ->
                            val statusOk = when (filter) {
                                StatusFilter.ALL -> true
                                StatusFilter.PRESENT -> row.status == AttendanceStatus.PRESENT
                                StatusFilter.LATE -> row.status == AttendanceStatus.LATE
                                StatusFilter.ABSENT -> row.status == AttendanceStatus.ABSENT
                            }
                            statusOk && (!(onCampusOnly && isToday) || row.onCampusNow)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item { SummaryCard(s.rows) }

                        item {
                            DateStepper(
                                date = date,
                                onPrevious = viewModel::previousDay,
                                onNext = viewModel::nextDay
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                    StatusFilter.entries.forEachIndexed { index, item ->
                                        SegmentedButton(
                                            selected = filter == item,
                                            onClick = { filter = item },
                                            shape = SegmentedButtonDefaults.itemShape(
                                                index = index,
                                                count = StatusFilter.entries.size
                                            ),
                                            colors = SegmentedButtonDefaults.colors(
                                                activeContainerColor = colors.primary,
                                                activeContentColor = colors.onPrimary,
                                                inactiveContainerColor =
                                                    colors.surfaceContainerLowest.copy(alpha = 0.6f)
                                            ),
                                            label = { Text(item.label) }
                                        )
                                    }
                                }
                                FilterChip(
                                    selected = onCampusOnly && isToday,
                                    onClick = { onCampusOnly = !onCampusOnly },
                                    enabled = isToday,
                                    label = { Text("On campus now") }
                                )
                            }
                        }

                        if (s.rows.isEmpty()) {
                            item {
                                Text(
                                    "No students are registered in this section yet.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        } else if (rows.isEmpty()) {
                            item {
                                Text(
                                    "No students match this filter.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        } else {
                            items(rows, key = { it.student.uid }) { row ->
                                StudentRowCard(
                                    row = row,
                                    isToday = isToday,
                                    onClick = { editing = row }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { row ->
        OverrideDialog(
            row = row,
            dateLabel = TimeFormat.dayTitle(date),
            saving = saving,
            onDismiss = { editing = null },
            onConfirm = { status, reason ->
                viewModel.override(row, status, reason)
                editing = null
            }
        )
    }
}

// ───────────────────────── Pieces ─────────────────────────

@Composable
private fun SummaryCard(rows: List<StudentRow>) {
    val colors = MaterialTheme.colorScheme
    val present = rows.count { it.status == AttendanceStatus.PRESENT }
    val late = rows.count { it.status == AttendanceStatus.LATE }
    val absent = rows.count { it.status == AttendanceStatus.ABSENT }
    val onCampus = rows.count { it.onCampusNow }
    val percent = if (rows.isEmpty()) 0 else (((present + late) * 100.0) / rows.size).roundToInt()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AttendanceRing(
                percent = percent,
                trackColor = colors.onPrimary.copy(alpha = 0.25f),
                progressColor = colors.onPrimary,
                textColor = colors.onPrimary,
                diameter = 96.dp,
                strokeWidth = 10.dp,
                textStyle = MaterialTheme.typography.titleMedium
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(Modifier.fillMaxWidth()) {
                    CountItem("Present", present, Modifier.weight(1f))
                    CountItem("Late", late, Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth()) {
                    CountItem("Absent", absent, Modifier.weight(1f))
                    CountItem("On campus", onCampus, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CountItem(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun DateStepper(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
        }
        Text(
            text = TimeFormat.dayTitle(date),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext, enabled = date.isBefore(LocalDate.now())) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
        }
    }
}

@Composable
private fun StudentRowCard(row: StudentRow, isToday: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val record = row.record
    val entry = record?.entryTime?.toDate()?.time
    val exit = record?.exitTime?.toDate()?.time

    val timeText = when {
        record == null -> "No campus entry"
        entry == null -> "Marked manually"
        exit != null -> "In ${TimeFormat.clock(entry)} · Out ${TimeFormat.clock(exit)}"
        else -> "In ${TimeFormat.clock(entry)}"
    }

    val (chipText, chipContainer, chipContent) = when (row.status) {
        AttendanceStatus.PRESENT -> Triple("Present", colors.primaryContainer, colors.onPrimaryContainer)
        AttendanceStatus.LATE -> Triple("Late", colors.tertiaryContainer, colors.onTertiaryContainer)
        AttendanceStatus.ABSENT -> Triple("Absent", colors.errorContainer, colors.onErrorContainer)
    }

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceContainerLowest,
            contentColor = colors.onSurface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = row.student.name.trim().firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = row.student.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = row.student.universityId,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                if (isToday && row.onCampusNow) {
                    Text(
                        text = "On campus now",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (record?.source == AttendanceSource.MANUAL) {
                    Text(
                        text = record.note ?: "Marked manually",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Surface(shape = CircleShape, color = chipContainer, contentColor = chipContent) {
                Text(
                    text = chipText,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun OverrideDialog(
    row: StudentRow,
    dateLabel: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (AttendanceStatus, String) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var status by remember { mutableStateOf(row.status) }
    var reason by remember { mutableStateOf("") }
    val options = listOf(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.ABSENT)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark ${row.student.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    options.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = status == option,
                            onClick = { status = option },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = colors.primary,
                                activeContentColor = colors.onPrimary
                            ),
                            label = { Text(option.label()) }
                        )
                    }
                }
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason (optional)") },
                    placeholder = { Text("e.g. GPS failure, medical leave") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(status, reason.trim()) },
                enabled = !saving
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}