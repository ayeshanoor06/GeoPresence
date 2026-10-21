package com.ayesha.geopresence.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.RegistrationForm
import com.ayesha.geopresence.data.model.UserRole

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegistered: (AppUser) -> Unit,
    onBackToLogin: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    var role by rememberSaveable { mutableStateOf(UserRole.STUDENT) }
    var name by rememberSaveable { mutableStateOf("") }
    var universityId by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var semester by rememberSaveable { mutableStateOf("") }
    var section by rememberSaveable { mutableStateOf("") }
    var batch by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.clearError() }

    val isStudent = role == UserRole.STUDENT
    val semesterNumber = semester.toIntOrNull()

    val nameOk = name.trim().length >= 2
    val idOk = universityId.trim().length >= 3
    val emailOk = isValidEmail(email)
    val semesterOk = semesterNumber != null && semesterNumber in 1..8
    val sectionOk = section.trim().length in 1..2
    val batchOk = batch.trim().length >= 2
    val passwordOk = password.length >= 6
    val confirmOk = confirm == password

    val formValid = nameOk && idOk && emailOk && passwordOk && confirmOk &&
            (!isStudent || (semesterOk && sectionOk && batchOk))

    fun submit() {
        submitted = true
        if (formValid) {
            focusManager.clearFocus()
            viewModel.register(
                RegistrationForm(
                    name = name,
                    email = email,
                    password = password,
                    role = role,
                    universityId = universityId,
                    semester = if (isStudent) semesterNumber else null,
                    section = if (isStudent) section else null,
                    batch = if (isStudent) batch else null
                ),
                onRegistered
            )
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
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

            if (!isStudent) {
                Surface(
                    color = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Teacher accounts must be approved by the department admin " +
                                "before you can open your dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            state.errorMessage?.let { ErrorBanner(it) }

            AuthTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full name",
                icon = Icons.Filled.Person,
                errorText = if (submitted && !nameOk) "Enter your full name" else null
            )
            AuthTextField(
                value = universityId,
                onValueChange = { universityId = it },
                label = "University ID (e.g. UL-BSITM-B-23-06)",
                icon = Icons.Filled.AccountBox,
                errorText = if (submitted && !idOk) "Enter your university ID" else null
            )
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                label = "University email",
                icon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                errorText = if (submitted && !emailOk) "Enter a valid email address" else null
            )

            if (isStudent) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AuthTextField(
                        value = semester,
                        onValueChange = { semester = it.filter(Char::isDigit).take(1) },
                        label = "Semester",
                        keyboardType = KeyboardType.Number,
                        errorText = if (submitted && !semesterOk) "1–8" else null,
                        modifier = Modifier.weight(1f)
                    )
                    AuthTextField(
                        value = section,
                        onValueChange = { section = it.filter(Char::isLetter).take(2).uppercase() },
                        label = "Section",
                        errorText = if (submitted && !sectionOk) "e.g. A" else null,
                        modifier = Modifier.weight(1f)
                    )
                    AuthTextField(
                        value = batch,
                        onValueChange = { batch = it.filter(Char::isDigit).take(4) },
                        label = "Batch",
                        keyboardType = KeyboardType.Number,
                        errorText = if (submitted && !batchOk) "e.g. 2023" else null,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            PasswordField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                visible = showPassword,
                onToggleVisible = { showPassword = !showPassword },
                errorText = if (submitted && !passwordOk) "At least 6 characters" else null
            )
            PasswordField(
                value = confirm,
                onValueChange = { confirm = it },
                label = "Confirm password",
                visible = showPassword,
                onToggleVisible = { showPassword = !showPassword },
                imeAction = ImeAction.Done,
                errorText = if (submitted && !confirmOk) "Passwords do not match" else null,
                onDone = { submit() }
            )

            AuthButton(text = "Create account", isLoading = state.isLoading, onClick = { submit() })

            TextButton(onClick = onBackToLogin) {
                Text("Already have an account? Sign in", fontWeight = FontWeight.Medium)
            }
        }
    }
}