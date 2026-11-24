package com.ayesha.geopresence.geofence

import android.location.Location

/** A burst of fixes taken during one room check. */
data class FixSample(
    val acceptable: List<Location>,   // accurate enough and not mocked
    val sawMock: Boolean,
    val rejected: Int
)

object RoomCheckRules {

    const val MAX_ACCURACY_METERS = 30f      // reject any fix worse than this
    const val FIXES_PER_CHECK = 3            // best of 3 fixes...
    const val CHECK_SPAN_MS = 20_000L        // ...over about 20 seconds
    const val RETRY_MINUTES = 3L             // retry every 3 minutes
    const val WINDOW_MINUTES = 30L           // ...until class start + 30 minutes
    const val WINDOW_GRACE_MS = 60_000L      // WorkManager can run a little late
    const val MAX_FIX_AGE_MS = 10_000L       // ignore stale cached fixes

    sealed interface Decision {
        data class Inside(val distanceMeters: Float, val accuracyMeters: Float) : Decision
        data class Outside(val distanceMeters: Float) : Decision
        data class Inconclusive(val reason: String) : Decision
        data object MockLocation : Decision
    }

    fun evaluate(
        sample: FixSample,
        centerLat: Double,
        centerLng: Double,
        radiusMeters: Float
    ): Decision {
        if (sample.sawMock) return Decision.MockLocation

        val best = sample.acceptable.minByOrNull { it.accuracy }
            ?: return Decision.Inconclusive(
                "no fix better than ±${MAX_ACCURACY_METERS.toInt()} m (${sample.rejected} rejected)"
            )

        val result = FloatArray(1)
        Location.distanceBetween(centerLat, centerLng, best.latitude, best.longitude, result)
        val distance = result[0]

        return when {
            distance <= radiusMeters -> Decision.Inside(distance, best.accuracy)
            distance > radiusMeters + best.accuracy -> Decision.Outside(distance)
            else -> Decision.Inconclusive(
                "at the room edge (${distance.toInt()} m from centre, ±${best.accuracy.toInt()} m)"
            )
        }
    }
}