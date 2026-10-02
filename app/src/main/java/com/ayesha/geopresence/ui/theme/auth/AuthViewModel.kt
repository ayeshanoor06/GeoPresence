package com.ayesha.geopresence.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.UserRole
import com.ayesha.geopresence.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String, onSuccess: (UserRole) -> Unit) {
        viewModelScope.launch {
            _uiState.update { AuthUiState(isLoading = true) }
            repository.login(email.trim(), password)
                .onSuccess { role ->
                    _uiState.update { AuthUiState() }
                    onSuccess(role)
                }
                .onFailure { e ->
                    _uiState.update { AuthUiState(errorMessage = friendlyMessage(e)) }
                }
        }
    }

    fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole,
        onSuccess: (UserRole) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { AuthUiState(isLoading = true) }
            repository.register(name.trim(), email.trim(), password, role)
                .onSuccess { r ->
                    _uiState.update { AuthUiState() }
                    onSuccess(r)
                }
                .onFailure { e ->
                    _uiState.update { AuthUiState(errorMessage = friendlyMessage(e)) }
                }
        }
    }

    fun checkSession(onResult: (UserRole?) -> Unit) {
        viewModelScope.launch { onResult(repository.currentUserRole()) }
    }

    fun signOut() = repository.signOut()

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseNetworkException -> "No internet connection. Please check your network."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }
}