package com.ayesha.geopresence.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.ClassDraft
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.data.model.Room
import com.ayesha.geopresence.ui.auth.ErrorBanner
import com.ayesha.geopresence.util.AppConstants
import com.ayesha.geopresence.util.Keys

private val WEEK_DAYS = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
private val CHECK_DELAYS = listOf(10, 15, 20)

private sealed interface EditorTarget {
    data object New : EditorTarget
    data class Edit(val info: ClassInfo) : EditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassManagementScreen(
    onBack: () -> Unit,
    viewModel: ClassManagementViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
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

    var editor by remember { mutableStateOf<EditorTarget?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Class management") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            if (!state.loading && state.error == null) {
                ExtendedFloatingActionButton(
                    onClick = { editor = EditorTarget.New },
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Add class")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.error != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ErrorBanner(state.error.orEmpty())
                    Button(onClick = viewModel::load) { Text("Try again") }
                }

                state.classes.isEmpty() -> Text(
                    text = "No classes yet.\nTap “Add class” to create the first one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp)
                )

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp, top = 8.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.classes, key = { it.id }) { info ->
                        ClassRow(info = info, onClick = { editor = EditorTarget.Edit(info) })
                    }
                }
            }
        }
    }

    editor?.let { target ->
        val editing = (target as? EditorTarget.Edit)?.info
        ClassFormDialog(
            initial = editing,
            teachers = state.teachers,
            rooms = state.rooms,
            saving = saving,
            onDismiss = { editor = null },
            onSave = { draft ->
                viewModel.save(editing?.id, draft)
                editor = null
            },
            onDelete = editing?.let { info ->
                {
                    viewModel.delete(info)
                    editor = null
                }
            }
        )
    }
}

// ───────────────────────── List card ─────────────────────────

@Composable
private fun ClassRow(info: ClassInfo, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme

    ElevatedCard(
        onClick = onClick,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = info.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${info.sectionLabel} · ${info.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                if (info.days.isNotEmpty()) {
                    Text(
                        text = info.daysLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
                Text(
                    text = "Teacher: ${info.teacherName.ifBlank { "—" }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                if (info.hasRoom) {
                    Text(
                        text = "Room ${info.roomName} · auto-check after ${info.checkDelayMinutes} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                } else {
                    Text(
                        text = "No room · manual register only",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ───────────────────────── Form dialog ─────────────────────────

private enum class TimeTarget { START, END }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ClassFormDialog(
    initial: ClassInfo?,
    teachers: List<AppUser>,
    rooms: List<Room>,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ClassDraft) -> Unit,
    onDelete: (() -> Unit)?
) {
    val colors = MaterialTheme.colorScheme

    var subjectName by remember { mutableStateOf(initial?.subjectName.orEmpty()) }
    var subjectCode by remember { mutableStateOf(initial?.subjectCode.orEmpty()) }
    var teacherId by remember { mutableStateOf(initial?.teacherId) }
    var semester by remember {
        mutableStateOf(initial?.semester?.takeIf { it > 0 }?.toString().orEmpty())
    }
    var section by remember { mutableStateOf(initial?.section.orEmpty()) }
    var days by remember {
        mutableStateOf(
            initial?.days?.map { it.uppercase() }?.toSet()
                ?: setOf("MON", "TUE", "WED", "THU", "FRI")
        )
    }
    var start by remember { mutableStateOf(initial?.startTime ?: "09:00") }
    var end by remember { mutableStateOf(initial?.endTime ?: "10:30") }
    var roomId by remember { mutableStateOf(initial?.roomId?.takeIf { it.isNotBlank() }) }
    var delay by remember { mutableStateOf(initial?.checkDelayMinutes ?: 15) }
    var timePicker by remember { mutableStateOf<TimeTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val semesterNumber = semester.toIntOrNull()
    val teacher = teachers.firstOrNull { it.uid == teacherId }
    val room = rooms.firstOrNull { it.id == roomId }
    val semesterOk = semesterNumber != null && semesterNumber in 1..8
    val valid = subjectName.isNotBlank() && teacher != null && semesterOk &&
            section.isNotBlank() && days.isNotEmpty() && start < end

    val keyPreview = if (semesterOk && section.isNotBlank())
        Keys.sectionKey(AppConstants.DEPARTMENT_ID, semesterNumber!!, section) else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add class" else "Edit class") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = subjectName,
                    onValueChange = { subjectName = it },
                    label = { Text("Subject name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subjectCode,
                    onValueChange = { subjectCode = it },
                    label = { Text("Subject code (e.g. IT-601)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Teacher",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (teachers.isEmpty()) {
                    Text(
                        "No approved teachers yet. Approve one in User Management first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        teachers.forEach { t ->
                            FilterChip(
                                selected = teacherId == t.uid,
                                onClick = { teacherId = t.uid },
                                label = { Text(t.name) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = semester,
                        onValueChange = { semester = it.filter(Char::isDigit).take(1) },
                        label = { Text("Semester") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = submitted && !semesterOk,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = {
                            section = it.filter(Char::isLetter).take(2).uppercase()
                        },
                        label = { Text("Section") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (keyPreview != null) {
                    Text(
                        "Section key: $keyPreview. It is added to the teacher automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Text(
                    "Days",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WEEK_DAYS.forEach { day ->
                        FilterChip(
                            selected = day in days,
                            onClick = { days = if (day in days) days - day else days + day },
                            label = { Text(day) }
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { timePicker = TimeTarget.START },
                        modifier = Modifier.weight(1f)
                    ) { Text("Starts $start") }
                    OutlinedButton(
                        onClick = { timePicker = TimeTarget.END },
                        modifier = Modifier.weight(1f)
                    ) { Text("Ends $end") }
                }
                if (start >= end) {
                    Text(
                        "The class must end after it starts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }

                // ── Room (for the automatic room check) ──
                Text(
                    "Room",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (rooms.isEmpty()) {
                    Text(
                        "No rooms yet. Add rooms in Room Management to enable the automatic room check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            selected = roomId == null,
                            onClick = { roomId = null },
                            label = { Text("No room") }
                        )
                        rooms.forEach { r ->
                            FilterChip(
                                selected = roomId == r.id,
                                onClick = { roomId = r.id },
                                label = { Text(r.name) }
                            )
                        }
                    }
                }
                if (room != null) {
                    Text(
                        "Check attendance after",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CHECK_DELAYS.forEach { minutes ->
                            FilterChip(
                                selected = delay == minutes,
                                onClick = { delay = minutes },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                    Text(
                        "A student is marked present automatically if they are inside ${room.name} " +
                                "$delay minutes after the class starts. If the phone can't tell, " +
                                "the teacher fixes it with one tap in the register.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    if (!room.calibrated) {
                        Text(
                            "This room is not calibrated yet. Calibrate it in Room Management.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.error
                        )
                    }
                }

                if (submitted && !valid) {
                    Text(
                        "Fill in the subject, pick a teacher, semester (1–8), section and at least one day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.error
                    )
                }

                if (onDelete != null) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.error)
                    ) { Text("Delete this class") }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    submitted = true
                    if (valid && teacher != null && semesterNumber != null) {
                        onSave(
                            ClassDraft(
                                subjectName = subjectName,
                                subjectCode = subjectCode,
                                teacher = teacher,
                                semester = semesterNumber,
                                section = section,
                                days = WEEK_DAYS.filter { it in days },
                                startTime = start,
                                endTime = end,
                                room = room,
                                checkDelayMinutes = delay
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )

    when (timePicker) {
        TimeTarget.START -> ClockPickerDialog(
            initial = start,
            onConfirm = { start = it; timePicker = null },
            onDismiss = { timePicker = null }
        )

        TimeTarget.END -> ClockPickerDialog(
            initial = end,
            onConfirm = { end = it; timePicker = null },
            onDismiss = { timePicker = null }
        )

        null -> Unit
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this class?") },
            text = {
                Text(
                    "Students will no longer see it. The teacher keeps the section until you " +
                            "remove it in User Management."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.error,
                        contentColor = colors.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}