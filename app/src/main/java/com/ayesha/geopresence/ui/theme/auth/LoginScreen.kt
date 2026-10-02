package com.ayesha.geopresence.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.UserRole

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: (UserRole) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearError() }

    val emailError = if (submitted && !isValidEmail(email)) "Enter a valid email address" else null
    val passwordError = if (submitted && password.length < 6) "Password must be at least 6 characters" else null

    fun submit() {
        submitted = true
        if (isValidEmail(email) && password.length >= 6) {
            focusManager.clearFocus()
            viewModel.login(email, password, onLoggedIn)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        AuthHeader(subtitle = "Automatic attendance, powered by your location")
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Welcome back",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        state.errorMessage?.let { ErrorBanner(it) }

        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            icon = Icons.Filled.Email,
            keyboardType = KeyboardType.Email,
            errorText = emailError
        )
        PasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            visible = showPassword,
            onToggleVisible = { showPassword = !showPassword },
            imeAction = ImeAction.Done,
            errorText = passwordError,
            onDone = { submit() }
        )

        AuthButton(text = "Sign in", isLoading = state.isLoading, onClick = { submit() })

        TextButton(onClick = onNavigateToRegister) {
            Text("New to GeoPresence? Create an account")
        }
    }
}