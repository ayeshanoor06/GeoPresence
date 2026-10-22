package com.ayesha.geopresence.geofence

import android.content.Context
import android.location.Location
import android.util.Log
import androidx.core.location.LocationCompat
import com.ayesha.geopresence.data.model.AttendanceSource
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.geofence.LocalAttendanceStore.CachedUser
import com.ayesha.geopresence.geofence.LocalAttendanceStore.DayState
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.Keys
import com.ayesha.geopresence.util.awaitTask
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Date

class AttendanceLogger(context: Context) {

    private val appContext = context.applicationContext
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val configStore = CampusConfigStore(appContext)
    private val local = LocalAttendanceStore(appContext)

    private enum class WriteResult { SAVED, QUEUED, FAILED }

    // ───────────────────────── ENTER ─────────────────────────

    suspend fun handleEnter(location: Location?) {
        val now = System.currentTimeMillis()
        val user = signedInUser() ?: run {
            Log.w(TAG, "ENTER ignored: no signed-in user")
            return
        }

        // FR-03-001: reject spoofed GPS and warn the admin
        if (location != null && LocationCompat.isMock(location)) {
            Log.w(TAG, "ENTER rejected: mock location detected")
            configStore.saveLastEvent("ENTER", now, "Rejected: fake GPS detected")
            writeMockLog(user, now)
            return
        }
        if (location == null) {
            Log.w(TAG, "No location available to verify, mock check skipped")
        }

        val today = Keys.dateKey()
        val day = local.loadDay()?.takeIf { it.uid == user.uid && it.dateKey == today }

        if (day == null) {
            firstEntryToday(user, today, now)
            return
        }

        when {
            // Already inside (e.g. the geofence was re-registered): nothing to do
            day.currentEnterMillis > 0L -> {
                configStore.setInside(true)
                Log.d(TAG, "ENTER ignored: already inside")
            }

            // FR-03-002: boundary flicker / duplicate event
            AttendanceRules.isDuplicate(day.lastEnterMillis, now) -> {
                local.saveDay(day.copy(currentEnterMillis = now))
                configStore.setInside(true)
                configStore.saveLastEvent("ENTER", now, "Duplicate entry ignored (within 30 min)")
            }

            // Genuine return later in the day: still ONE record per day (NR-02-002)
            else -> {
                local.saveDay(day.copy(lastEnterMillis = now, currentEnterMillis = now))
                configStore.setInside(true)
                configStore.saveLastEvent("ENTER", now, "Back on campus (today’s attendance already recorded)")
            }
        }
    }

    private suspend fun firstEntryToday(user: CachedUser, dateKey: String, now: Long) {
        val config = configStore.load()
        val result = AttendanceRules.classify(
            entryMillis = now,
            firstClassStart = config?.firstClassStart ?: "08:30",
            graceMinutes = config?.graceMinutes ?: 15
        )

        // Save local state FIRST so a timeout can never cause a second record.
        local.saveDay(DayState(user.uid, dateKey, now, now, now, 0L))
        configStore.setInside(true)

        val write = createRecord(user, dateKey, now, result.status, result.note)
        val label = result.status.label()
        val text = when (write) {
            WriteResult.SAVED -> "Marked $label"
            WriteResult.QUEUED -> "Marked $label (will sync when online)"
            WriteResult.FAILED -> {
                local.clearDay()   // allow a retry on the next ENTER
                "Could not save attendance"
            }
        }
        configStore.saveLastEvent("ENTER", now, text)
    }

    // ───────────────────────── EXIT ─────────────────────────

    suspend fun handleExit() {
        val now = System.currentTimeMillis()
        val user = signedInUser() ?: return

        val day = local.loadDay()?.takeIf { it.uid == user.uid }
        if (day == null || day.currentEnterMillis == 0L) {
            Log.d(TAG, "EXIT ignored: no matching ENTER")
            return
        }

        val total = day.accumulatedMillis + (now - day.currentEnterMillis).coerceAtLeast(0L)
        local.saveDay(day.copy(currentEnterMillis = 0L, accumulatedMillis = total))
        configStore.setInside(false)

        val write = updateExit(user, day.dateKey, now, (total / 60_000L).toInt())
        val text = if (write == WriteResult.FAILED) {
            "Could not save exit time"
        } else {
            "Time on campus today: ${formatDuration(total)}"
        }
        configStore.saveLastEvent("EXIT", now, text)
    }

    // ───────────────────────── Firestore ─────────────────────────

    private suspend fun createRecord(
        user: CachedUser,
        dateKey: String,
        entryMillis: Long,
        status: AttendanceStatus,
        note: String?
    ): WriteResult {
        val data = hashMapOf<String, Any?>(
            "userId" to user.uid,
            "userName" to user.name,
            "universityId" to user.universityId,
            "role" to user.role,
            "departmentId" to user.departmentId,
            "sectionKey" to user.sectionKey,          // null for teachers
            "date" to dateKey,
            "entryTime" to Timestamp(Date(entryMillis)),
            "exitTime" to null,
            "durationMinutes" to 0,
            "status" to status.name,
            "source" to AttendanceSource.AUTO.name,
            "overriddenBy" to null,
            "note" to note
        )
        val docId = Keys.attendanceDocId(user.uid, dateKey)
        val task = db.collection(FirestorePaths.ATTENDANCE).document(docId).set(data)
        return awaitWrite(task, "create attendance")
    }

    private suspend fun updateExit(
        user: CachedUser,
        dateKey: String,
        exitMillis: Long,
        minutes: Int
    ): WriteResult {
        val docId = Keys.attendanceDocId(user.uid, dateKey)
        val task = db.collection(FirestorePaths.ATTENDANCE).document(docId).update(
            mapOf(
                "exitTime" to Timestamp(Date(exitMillis)),
                "durationMinutes" to minutes
            )
        )
        return awaitWrite(task, "update exit")
    }

    private suspend fun writeMockLog(user: CachedUser, now: Long) {
        val task = db.collection(FirestorePaths.MOCK_GPS_LOGS).add(
            mapOf(
                "userId" to user.uid,
                "userName" to user.name,
                "universityId" to user.universityId,
                "departmentId" to user.departmentId,
                "timestamp" to Timestamp(Date(now)),
                "reason" to "Mock location provider detected on campus ENTER"
            )
        )
        awaitWrite(task, "mock gps log")
    }

    /**
     * Firestore keeps a write on the phone when offline and uploads it later.
     * So if the server does not answer in time we treat it as QUEUED, not failed.
     */
    private suspend fun awaitWrite(task: Task<*>, what: String): WriteResult {
        var completed = false
        return try {
            withTimeoutOrNull(WRITE_TIMEOUT_MS) {
                task.awaitTask()
                completed = true
            }
            if (completed) WriteResult.SAVED else WriteResult.QUEUED
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "$what failed: ${e.message}")
            WriteResult.FAILED
        }
    }

    // ───────────────────────── Helpers ─────────────────────────

    private fun signedInUser(): CachedUser? {
        val firebaseUser = auth.currentUser ?: return null
        val cached = local.loadUser() ?: return null
        return if (cached.uid == firebaseUser.uid) cached else null
    }

    private fun AttendanceStatus.label(): String =
        name.lowercase().replaceFirstChar { it.uppercase() }

    private fun formatDuration(millis: Long): String {
        val hours = millis / 3_600_000L
        val minutes = (millis / 60_000L) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "$minutes min"
    }

    private companion object {
        const val TAG = "GeoPresenceAttendance"
        const val WRITE_TIMEOUT_MS = 3_500L
    }
}