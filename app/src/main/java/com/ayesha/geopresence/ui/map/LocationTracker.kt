package com.ayesha.geopresence.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import android.os.SystemClock

object LocationTracker {

    /** Live location while the collector is active. Needs location permission. */
    @SuppressLint("MissingPermission")
    fun updates(context: Context, intervalMs: Long = 3_000L): Flow<Location> = callbackFlow {
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { trySend(it) }
            }
        }

        // Show something quickly while the first live fix arrives
        // Only trust a cached location if it is fresh (under 2 minutes old)
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
                if (ageMs < 120_000L) trySend(location)
            }
        }

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close()
        }

        awaitClose { client.removeLocationUpdates(callback) }
    }
}