package com.ayesha.geopresence.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.ClassDraft
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.data.model.Room
import com.ayesha.geopresence.data.model.UserRole
import com.ayesha.geopresence.data.model.UserStatus
import com.ayesha.geopresence.data.repository.AdminRepository
import com.ayesha.geopresence.data.repository.ClassAdminRepository
import com.ayesha.geopresence.data.repository.RoomRepository
import com.ayesha.geopresence.util.AppConstants
import com.ayesha.geopresence.util.Keys
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ClassManagementUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val classes: List<ClassInfo> = emptyList(),
    val teachers: List<AppUser> = emptyList(),     // approved teachers only
    val rooms: List<Room> = emptyList()
)

class ClassManagementViewModel : ViewModel() {

    private val adminRepository = AdminRepository()
    private val classRepository = ClassAdminRepository()
    private val roomRepository = RoomRepository()

    private val _state = MutableStateFlow(ClassManagementUiState())
    val state: StateFlow<ClassManagementUiState> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    fun load() {
        job?.cancel()
        _state.value = ClassManagementUiState()
        job = viewModelScope.launch {
            combine(
                adminRepository.observeClasses(),
                adminRepository.observeUsers(),
                roomRepository.observeRooms()
            ) { classes, users, rooms ->
                val error = classes.exceptionOrNull()
                    ?: users.exceptionOrNull()
                    ?: rooms.exceptionOrNull()
                if (error != null) {
                    ClassManagementUiState(
                        loading = false,
                        error = error.localizedMessage ?: "Could not load classes."
                    )
                } else {
                    ClassManagementUiState(
                        loading = false,
                        classes = classes.getOrNull().orEmpty()
                            .sortedWith(compareBy({ it.semester }, { it.section }, { it.startTime })),
                        teachers = users.getOrNull().orEmpty()
                            .filter { it.role == UserRole.TEACHER && it.status == UserStatus.ACTIVE }
                            .sortedBy { it.name.lowercase() },
                        rooms = rooms.getOrNull().orEmpty()
                    )
                }
            }.collect { _state.value = it }
        }
    }

    fun save(existingId: String?, draft: ClassDraft) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val result = classRepository.saveClass(existingId, draft)
            _saving.value = false

            val key = Keys.sectionKey(AppConstants.DEPARTMENT_ID, draft.semester, draft.section)
            _message.value = result.fold(
                onSuccess = { saved ->
                    "Class saved. ${draft.teacher.name} can now see section $key" +
                            if (saved) "" else " (will sync when online)"
                },
                onFailure = { "Could not save: ${it.localizedMessage ?: "unknown error"}" }
            )
        }
    }

    fun delete(info: ClassInfo) {
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val result = classRepository.deleteClass(info.id)
            _saving.value = false
            _message.value = result.fold(
                onSuccess = { "${info.title} deleted" },
                onFailure = { "Could not delete: ${it.localizedMessage ?: "unknown error"}" }
            )
        }
    }

    fun messageShown() {
        _message.value = null
    }
}