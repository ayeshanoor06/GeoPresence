package com.ayesha.geopresence.geofence

import android.app.Application
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.UserRole
import com.ayesha.geopresence.data.model.UserStatus
import com.ayesha.geopresence.data.repository.DepartmentRepository
import com.ayesha.geopresence.util.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GeofenceUiState {
    data object Idle : GeofenceUiState
    data object Registering : GeofenceUiState
    data class Active(val departmentName: String, val radiusMeters: Double) : GeofenceUiState
    data class Error(val message: String) : GeofenceUiState
}

class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val manager = GeofenceManager(appContext)
    private val store = CampusConfigStore(appContext)
    private val departmentRepository = DepartmentRepository()

    private val _state = MutableStateFlow<GeofenceUiState>(GeofenceUiState.Idle)
    val state: StateFlow<GeofenceUiState> = _state.asStateFlow()

    private val _lastEvent = MutableStateFlow(store.loadLastEvent())
    val lastEvent: StateFlow<GeofenceEvent?> = _lastEvent.asStateFlow()

    // Updates the card live when the receiver stores a new ENTER/EXIT.
    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        _lastEvent.value = store.loadLastEvent()
    }

    init {
        store.registerListener(prefsListener)
    }

    override fun onCleared() {
        store.unregisterListener(prefsListener)
    }

    fun ensureRegistered(user: AppUser?) {
        if (user == null || user.status != UserStatus.ACTIVE || user.role == UserRole.ADMIN) return
        LocalAttendanceStore(appContext).saveUser(user)
        if (_state.value is GeofenceUiState.Registering) return

        viewModelScope.launch {
            _state.value = GeofenceUiState.Registering

            val deptId = user.departmentId.ifBlank { AppConstants.DEPARTMENT_ID }
            val fresh = departmentRepository.getDepartment(deptId)
                .getOrNull()
                ?.takeIf { it.hasGeofence }
                ?.let { CampusConfig.from(it) }

            // Fall back to the cached copy when offline.
            val config = fresh ?: store.load()
            if (config == null) {
                _state.value = GeofenceUiState.Error(
                    "Campus location isn’t available yet. Check your internet and try again."
                )
                return@launch
            }

            manager.register(config)
                .onSuccess {
                    store.save(config)
                    _state.value = GeofenceUiState.Active(
                        departmentName = config.departmentName.ifBlank { "Campus" },
                        radiusMeters = GeofenceManager.effectiveRadius(config.radiusMeters)
                    )
                }
                .onFailure {
                    _state.value = GeofenceUiState.Error(
                        it.localizedMessage ?: "Could not start geofencing."
                    )
                }
        }
    }
}