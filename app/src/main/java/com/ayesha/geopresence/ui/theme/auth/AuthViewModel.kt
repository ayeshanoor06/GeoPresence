package com.ayesha.geopresence.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.RegistrationForm
import com.ayesha.geopresence.data.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestoreException
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

    fun login(email: String, password: String, onSuccess: (AppUser) -> Unit) {
        viewModelScope.launch {
            _uiState.update { AuthUiState(isLoading = true) }
            repository.login(email.trim(), password)
                .onSuccess { user ->
                    _uiState.update { AuthUiState() }
                    onSuccess(user)
                }
                .onFailure { e ->
                    _uiState.update { AuthUiState(errorMessage = friendlyMessage(e)) }
                }
        }
    }

    fun register(form: RegistrationForm, onSuccess: (AppUser) -> Unit) {
        viewModelScope.launch {
            _uiState.update { AuthUiState(isLoading = true) }
            repository.register(form.copy(email = form.email.trim()))
                .onSuccess { user ->
                    _uiState.update { AuthUiState() }
                    onSuccess(user)
                }
                .onFailure { e ->
                    _uiState.update { AuthUiState(errorMessage = friendlyMessage(e)) }
                }
        }
    }

    fun checkSession(onResult: (AppUser?) -> Unit) {
        viewModelScope.launch { onResult(repository.currentUserProfile()) }
    }

    fun signOut() = repository.signOut()

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun friendlyMessage(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password is too weak. Use at least 6 characters."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException,
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseNetworkException -> "No internet connection. Please check your network."
        is FirebaseFirestoreException ->
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
                "Permission denied by security rules. Please check your details and try again."
            else e.localizedMessage ?: "Database error. Please try again."
        else -> e.localizedMessage ?: "Something went wrong. Please try again."
    }
}