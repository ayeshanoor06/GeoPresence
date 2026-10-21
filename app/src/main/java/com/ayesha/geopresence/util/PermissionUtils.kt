package com.ayesha.geopresence.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {

    private fun granted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasForegroundLocation(context: Context): Boolean =
        granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    /** Below Android 10 there is no separate background permission. */
    fun hasBackgroundLocation(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) hasForegroundLocation(context)
        else granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    /** Below Android 13 notifications need no runtime permission. */
    fun hasNotifications(context: Context): Boolean =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
        else granted(context, Manifest.permission.POST_NOTIFICATIONS)

    val needsBackgroundStep: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val needsNotificationStep: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}