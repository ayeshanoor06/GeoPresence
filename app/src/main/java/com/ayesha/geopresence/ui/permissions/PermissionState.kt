package com.ayesha.geopresence.ui.permissions

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ayesha.geopresence.util.PermissionUtils

data class PermissionStatus(
    val foregroundLocation: Boolean,
    val backgroundLocation: Boolean,
    val notifications: Boolean
) {
    val allGranted: Boolean get() = foregroundLocation && backgroundLocation && notifications

    companion object {
        fun read(context: Context) = PermissionStatus(
            foregroundLocation = PermissionUtils.hasForegroundLocation(context),
            backgroundLocation = PermissionUtils.hasBackgroundLocation(context),
            notifications = PermissionUtils.hasNotifications(context)
        )
    }
}

class PermissionState(private val context: Context) {
    var status by mutableStateOf(PermissionStatus.read(context))
        private set

    fun refresh() {
        status = PermissionStatus.read(context)
    }
}

@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    val state = remember { PermissionState(context) }

    DisposableEffect(context) {
        val owner = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) state.refresh()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    return state
}