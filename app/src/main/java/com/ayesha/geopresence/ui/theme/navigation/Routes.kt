package com.ayesha.geopresence.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomePlaceholderScreen(
    roleLabel: String,
    onSignOut: () -> Unit,
    onOpenMap: (() -> Unit)? = null
) {
    val email = FirebaseAuth.getInstance().currentUser?.email.orEmpty()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = colors.primaryContainer,
                contentColor = colors.onPrimaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("$roleLabel dashboard", style = MaterialTheme.typography.headlineSmall)
                Text("Signed in as $email", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "The real dashboard arrives in a later step.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (onOpenMap != null) {
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(onClick = onOpenMap, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("View campus map")
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
            Text("Sign out")
        }
    }
}