package com.ayesha.geopresence.geofence

import com.ayesha.geopresence.data.model.AttendanceStatus
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

object AttendanceRules {

    /** FR-03-002: ENTER events within 30 minutes of the last entry are duplicates. */
    const val DUPLICATE_WINDOW_MILLIS = 30 * 60 * 1000L

    data class Classification(val status: AttendanceStatus, val note: String?)

    /** FR-02-003 */
    fun classify(
        entryMillis: Long,
        firstClassStart: String,
        graceMinutes: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Classification {
        val entry = Instant.ofEpochMilli(entryMillis).atZone(zone).toLocalTime()
        val start = runCatching { LocalTime.parse(firstClassStart) }
            .getOrDefault(LocalTime.of(8, 30))
        val graceEnd = start.plusMinutes(graceMinutes.toLong())

        return when {
            !entry.isAfter(start) -> Classification(AttendanceStatus.PRESENT, null)
            !entry.isAfter(graceEnd) -> Classification(AttendanceStatus.LATE, null)
            else -> Classification(AttendanceStatus.LATE, "Arrived after the grace period")
        }
    }

    fun isDuplicate(lastEnterMillis: Long, nowMillis: Long): Boolean =
        lastEnterMillis > 0L && (nowMillis - lastEnterMillis) < DUPLICATE_WINDOW_MILLIS
}