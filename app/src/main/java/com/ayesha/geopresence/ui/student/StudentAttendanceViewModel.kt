package com.ayesha.geopresence.ui.student

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.repository.AttendanceRepository
import com.ayesha.geopresence.data.repository.DepartmentRepository
import com.ayesha.geopresence.util.AppConstants
import com.ayesha.geopresence.util.AttendanceStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.ZoneId

sealed interface StudentAttendanceUiState {
    data object Loading : StudentAttendanceUiState
    data class Error(val message: String) : StudentAttendanceUiState
    data class Ready(
        val summary: AttendanceStats.Summary,
        val history: List<AttendanceStats.DayEntry>,
        val today: AttendanceStats.DayEntry?,
        val threshold: Int
    ) : StudentAttendanceUiState
}

class StudentAttendanceViewModel : ViewModel() {

    private val attendanceRepository = AttendanceRepository()
    private val departmentRepository = DepartmentRepository()

    private val _uiState = MutableStateFlow<StudentAttendanceUiState>(StudentAttendanceUiState.Loading)
    val uiState: StateFlow<StudentAttendanceUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private var startedFor: String? = null

    fun start(user: AppUser) {
        if (startedFor == user.uid) return
        startedFor = user.uid
        job?.cancel()
        _uiState.value = StudentAttendanceUiState.Loading

        job = viewModelScope.launch {
            val department = withTimeoutOrNull(4_000L) {
                departmentRepository
                    .getDepartment(user.departmentId.ifBlank { AppConstants.DEPARTMENT_ID })
                    .getOrNull()
            }
            val joined = user.createdAtMillis?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            attendanceRepository.observeUserRecords(user.uid).collect { result ->
                result
                    .onSuccess { records ->
                        val computed = AttendanceStats.compute(
                            records = records,
                            semesterStart = department?.semesterStart,
                            holidays = department?.holidays.orEmpty(),
                            notBefore = joined
                        )
                        _uiState.value = StudentAttendanceUiState.Ready(
                            summary = computed.summary,
                            history = computed.history,
                            today = computed.today,
                            threshold = department?.lowAttendanceThreshold ?: 75
                        )
                    }
                    .onFailure {
                        _uiState.value = StudentAttendanceUiState.Error(
                            it.localizedMessage ?: "Could not load your attendance."
                        )
                    }
            }
        }
    }

    fun retry(user: AppUser) {
        startedFor = null
        start(user)
    }
}