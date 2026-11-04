package com.ayesha.geopresence.ui.teacher

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.data.repository.ClassRepository
import com.ayesha.geopresence.data.repository.SectionRepository
import com.ayesha.geopresence.util.Keys
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class ClassStat(
    val total: Int,
    val present: Int,
    val late: Int,
    val error: String? = null
) {
    val attended: Int get() = present + late
    val percent: Int get() = if (total == 0) 0 else ((attended * 100.0) / total).roundToInt()
}

sealed interface TeacherDashboardUiState {
    data object Loading : TeacherDashboardUiState
    data class Error(val message: String) : TeacherDashboardUiState
    data class Ready(
        val classes: List<ClassInfo>,
        val stats: Map<String, ClassStat>     // keyed by sectionKey
    ) : TeacherDashboardUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TeacherDashboardViewModel : ViewModel() {

    private val classRepository = ClassRepository()
    private val sectionRepository = SectionRepository()

    private val _uiState = MutableStateFlow<TeacherDashboardUiState>(TeacherDashboardUiState.Loading)
    val uiState: StateFlow<TeacherDashboardUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private var startedFor: String? = null

    fun start(teacherUid: String) {
        if (startedFor == teacherUid) return
        startedFor = teacherUid
        job?.cancel()
        _uiState.value = TeacherDashboardUiState.Loading
        val today = Keys.dateKey()

        job = viewModelScope.launch {
            classRepository.observeTeacherClasses(teacherUid)
                .flatMapLatest { result ->
                    val classes = result.getOrNull()
                    if (classes != null) {
                        statsFlow(classes, today)
                    } else {
                        flowOf<TeacherDashboardUiState>(
                            TeacherDashboardUiState.Error(
                                result.exceptionOrNull()?.localizedMessage
                                    ?: "Could not load your classes."
                            )
                        )
                    }
                }
                .collect { _uiState.value = it }
        }
    }

    fun retry(teacherUid: String) {
        startedFor = null
        start(teacherUid)
    }

    private fun statsFlow(classes: List<ClassInfo>, today: String): Flow<TeacherDashboardUiState> {
        val keys = classes.map { it.sectionKey }.filter { it.isNotBlank() }.distinct()
        if (keys.isEmpty()) return flowOf(TeacherDashboardUiState.Ready(classes, emptyMap()))

        val perSection = keys.map { key ->
            combine(
                sectionRepository.observeStudents(key),
                sectionRepository.observeAttendance(key, today)
            ) { students, attendance -> key to computeStat(students, attendance) }
        }
        return combine(perSection) { pairs ->
            TeacherDashboardUiState.Ready(classes, pairs.toMap())
        }
    }

    private fun computeStat(
        students: Result<List<AppUser>>,
        attendance: Result<List<AttendanceRecord>>
    ): ClassStat {
        val error = students.exceptionOrNull() ?: attendance.exceptionOrNull()
        if (error != null) return ClassStat(0, 0, 0, error.localizedMessage ?: "Could not load")

        val ids = students.getOrNull().orEmpty().map { it.uid }.toSet()
        val records = attendance.getOrNull().orEmpty().filter { it.userId in ids }
        return ClassStat(
            total = ids.size,
            present = records.count { it.status == AttendanceStatus.PRESENT },
            late = records.count { it.status == AttendanceStatus.LATE }
        )
    }
}