package com.ayesha.geopresence.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegistered: (UserRole) -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var role by rememberSaveable { mutableStateOf(UserRole.STUDENT) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearError() }

    val nameError = if (submitted && name.trim().length < 2) "Enter your full name" else null
    val emailError = if (submitted && !isValidEmail(email)) "Enter a valid email address" else null
    val passwordError = if (submitted && password.length < 6) "Password must be at least 6 characters" else null
    val confirmError = if (submitted && confirm != password) "Passwords do not match" else null

    fun submit() {
        submitted = true
        val valid = name.trim().length >= 2 && isValidEmail(email) &&
                password.length >= 6 && confirm == password
        if (valid) {
            focusManager.clearFocus()
            viewModel.register(name, email, password, role, onRegistered)
        }
    }

    val selectableRoles = listOf(UserRole.STUDENT, UserRole.TEACHER)

    AuthBackground {
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
            AuthHeader(subtitle = "Create your account to get started")
            Spacer(Modifier.height(4.dp))

            Text(
                text = "I am a",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface.copy(alpha = 0.70f),
                modifier = Modifier.fillMaxWidth()
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                selectableRoles.forEachIndexed { index, r ->
                    SegmentedButton(
                        selected = role == r,
                        onClick = { role = r },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = selectableRoles.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = colors.primary,
                            activeContentColor = colors.onPrimary,
                            inactiveContainerColor = colors.surfaceContainerLowest.copy(alpha = 0.6f)
                        ),
                        label = { Text(r.label) }
                    )
                }
            }

            state.errorMessage?.let { ErrorBanner(it) }

            AuthTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full name",
                icon = Icons.Filled.Person,
                errorText = nameError
            )
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
                errorText = passwordError
            )
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirm password",
                visible = showPassword,
                onToggleVisible = { showPassword = !showPassword },
                imeAction = ImeAction.Done,
                errorText = confirmError,
                onDone = { submit() }
            )

            AuthButton(text = "Create account", isLoading = state.isLoading, onClick = { submit() })

            TextButton(onClick = onBackToLogin) {
                Text("Already have an account? Sign in", fontWeight = FontWeight.Medium)
            }
        }
    }
}