package com.ayesha.geopresence.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import androidx.core.location.LocationManagerCompat
import com.ayesha.geopresence.util.PermissionUtils
import com.ayesha.geopresence.util.awaitTask
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(context: Context) {

    private val appContext = context.applicationContext
    private val client: GeofencingClient = LocationServices.getGeofencingClient(appContext)

    // Must be MUTABLE on Android 12+ so Google Play services can attach the event data.
    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(appContext, GeofenceBroadcastReceiver::class.java)
        val mutableFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0
        PendingIntent.getBroadcast(
            appContext, REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or mutableFlag
        )
    }

    @SuppressLint("MissingPermission")
    suspend fun register(config: CampusConfig): Result<Unit> {
        if (!PermissionUtils.hasForegroundLocation(appContext) ||
            !PermissionUtils.hasBackgroundLocation(appContext)
        ) {
            return Result.failure(
                IllegalStateException("Location permission (“Allow all the time”) is required.")
            )
        }
        if (!isLocationEnabled()) {
            return Result.failure(
                IllegalStateException("Turn on your phone’s location to enable automatic attendance.")
            )
        }

        return try {
            val geofence = Geofence.Builder()
                .setRequestId(requestId(config.departmentId))
                .setCircularRegion(
                    config.latitude,
                    config.longitude,
                    effectiveRadius(config.radiusMeters).toFloat()
                )
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(
                    Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
                )
                .build()

            // If the user is already inside when we register, fire ENTER straight away.
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()

            // Same request ID replaces the old geofence, so changed settings take effect.
            client.addGeofences(request, pendingIntent).awaitTask()
            Result.success(Unit)
        } catch (e: ApiException) {
            Result.failure(IllegalStateException(friendlyMessage(e.statusCode), e))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Stop watching the campus (used on sign-out). */
    fun stop() {
        client.removeGeofences(pendingIntent)
        LocalAttendanceStore(appContext).clearUser()
        CampusConfigStore(appContext).clear()
    }

    private fun isLocationEnabled(): Boolean {
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(manager)
    }

    private fun friendlyMessage(code: Int): String = when (code) {
        GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE ->
            "Location is off or unavailable. Turn on location and “Google Location Accuracy”."
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES ->
            "Too many geofences are registered on this phone."
        GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS ->
            "Too many geofence requests. Please try again."
        else -> "Could not start geofencing (${GeofenceStatusCodes.getStatusCodeString(code)})."
    }

    companion object {
        private const val REQUEST_CODE = 4101
        const val MIN_RADIUS_METERS = 100.0

        fun requestId(departmentId: String) = "campus_$departmentId"

        /** Android recommends at least 100 m for reliable detection. */
        fun effectiveRadius(radius: Double) = radius.coerceAtLeast(MIN_RADIUS_METERS)
    }
}