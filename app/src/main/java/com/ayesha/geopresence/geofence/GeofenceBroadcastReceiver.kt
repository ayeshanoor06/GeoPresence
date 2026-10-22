package com.ayesha.geopresence.geofence

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.util.Log
import com.ayesha.geopresence.util.awaitTask
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return

        if (event.hasError()) {
            Log.e(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }

        val transition = event.geofenceTransition
        if (transition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            transition != Geofence.GEOFENCE_TRANSITION_EXIT
        ) return

        val appContext = context.applicationContext
        val pending = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                withTimeoutOrNull(RECEIVER_BUDGET_MS) {
                    val logger = AttendanceLogger(appContext)
                    when (transition) {
                        Geofence.GEOFENCE_TRANSITION_ENTER -> {
                            Log.d(TAG, "ENTER received")
                            logger.handleEnter(resolveLocation(appContext, event))
                        }
                        Geofence.GEOFENCE_TRANSITION_EXIT -> {
                            Log.d(TAG, "EXIT received")
                            logger.handleExit()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Attendance handling failed", e)
            } finally {
                pending.finish()
            }
        }
    }

    /** The location that triggered the event, or the last known one as a fallback. */
    @SuppressLint("MissingPermission")
    private suspend fun resolveLocation(context: Context, event: GeofencingEvent): Location? {
        event.triggeringLocation?.let { return it }
        return try {
            withTimeoutOrNull(4_000L) {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.awaitTask()
            }
        } catch (e: Exception) {
            null
        }
    }

    private companion object {
        const val TAG = "GeoPresenceGeofence"
        const val RECEIVER_BUDGET_MS = 9_000L
    }
}