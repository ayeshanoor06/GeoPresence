package com.ayesha.geopresence.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.UserStatus

@Composable
fun PendingApprovalScreen(
    viewModel: AuthViewModel,
    onApproved: (AppUser) -> Unit,
    onSignOut: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    var message by remember { mutableStateOf<String?>(null) }

    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthHeader(subtitle = "Almost there")

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = colors.tertiaryContainer,
                    contentColor = colors.onTertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Awaiting approval", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Your teacher account has been created. The department admin " +
                                "needs to approve it before you can use the dashboard.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    message?.let {
                        Text(it, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.checkSession { user ->
                        if (user != null && user.status == UserStatus.ACTIVE) {
                            onApproved(user)
                        } else {
                            message = "Still waiting for approval."
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Check again") }

            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}