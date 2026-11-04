package com.ayesha.geopresence.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.data.repository.ManualAttendanceRepository
import com.ayesha.geopresence.data.repository.SectionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate

data class StudentRow(val student: AppUser, val record: AttendanceRecord?) {
    val status: AttendanceStatus get() = record?.status ?: AttendanceStatus.ABSENT
    val onCampusNow: Boolean
        get() = record != null && record.entryTime != null &&
                record.exitTime == null && record.status != AttendanceStatus.ABSENT
}

sealed interface ClassDetailUiState {
    data object Loading : ClassDetailUiState
    data class Error(val message: String) : ClassDetailUiState
    data class Ready(val date: LocalDate, val rows: List<StudentRow>) : ClassDetailUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class ClassDetailViewModel : ViewModel() {

    private val sectionRepository = SectionRepository()
    private val manualRepository = ManualAttendanceRepository()

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _uiState = MutableStateFlow<ClassDetailUiState>(ClassDetailUiState.Loading)
    val uiState: StateFlow<ClassDetailUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private var started = false
    private var teacher: AppUser? = null

    fun start(classInfo: ClassInfo, teacher: AppUser) {
        if (started) return
        started = true
        this.teacher = teacher

        viewModelScope.launch {
            combine(
                sectionRepository.observeStudents(classInfo.sectionKey),
                _date.flatMapLatest {
                    sectionRepository.observeAttendance(classInfo.sectionKey, it.toString())
                },
                _date
            ) { students, attendance, date ->
                val error = students.exceptionOrNull() ?: attendance.exceptionOrNull()
                if (error != null) {
                    ClassDetailUiState.Error(
                        error.localizedMessage ?: "Could not load this class."
                    )
                } else {
                    val byUser = attendance.getOrNull().orEmpty().associateBy { it.userId }
                    val rows = students.getOrNull().orEmpty()
                        .map { StudentRow(it, byUser[it.uid]) }
                    ClassDetailUiState.Ready(date, rows)
                }
            }.collect { _uiState.value = it }
        }
    }

    fun previousDay() { _date.value = _date.value.minusDays(1) }

    fun nextDay() {
        if (_date.value.isBefore(LocalDate.now())) _date.value = _date.value.plusDays(1)
    }

    fun override(row: StudentRow, status: AttendanceStatus, reason: String) {
        val t = teacher ?: return
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val result = manualRepository.setStatus(
                teacher = t,
                student = row.student,
                dateKey = _date.value.toString(),
                existing = row.record,
                status = status,
                reason = reason
            )
            _saving.value = false
            val label = status.name.lowercase().replaceFirstChar { it.uppercase() }
            _message.value = result.fold(
                onSuccess = { saved ->
                    "${row.student.name} marked $label" + if (saved) "" else " (will sync when online)"
                },
                onFailure = { "Could not save: ${it.localizedMessage ?: "unknown error"}" }
            )
        }
    }

    fun messageShown() { _message.value = null }
}