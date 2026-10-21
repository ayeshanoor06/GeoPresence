package com.ayesha.geopresence.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.Department
import com.ayesha.geopresence.data.repository.DepartmentRepository
import com.ayesha.geopresence.util.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CampusMapUiState {
    data object Loading : CampusMapUiState
    data class Error(val message: String) : CampusMapUiState
    data class Success(val department: Department) : CampusMapUiState
}

class CampusMapViewModel(
    private val repository: DepartmentRepository = DepartmentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<CampusMapUiState>(CampusMapUiState.Loading)
    val uiState: StateFlow<CampusMapUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.value = CampusMapUiState.Loading
            repository.getDepartment(AppConstants.DEPARTMENT_ID)
                .onSuccess { _uiState.value = CampusMapUiState.Success(it) }
                .onFailure {
                    _uiState.value = CampusMapUiState.Error(
                        it.localizedMessage ?: "Could not load campus settings."
                    )
                }
        }
    }
}