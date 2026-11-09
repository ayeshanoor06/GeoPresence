package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.Department
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.awaitTask
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class DepartmentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getDepartment(id: String): Result<Department> = runCatching {
        val snapshot = db.collection(FirestorePaths.DEPARTMENTS).document(id).get().awaitTask()
        Department.fromSnapshot(snapshot)
            ?: error("Department settings not found. Create departments/$id in Firestore.")
    }

    /**
     * Saves the geofence and attendance settings (admin only, enforced by Firestore rules).
     * Result value: true = saved on the server, false = saved on the phone and will sync.
     */
    suspend fun saveSettings(id: String, d: Department): Result<Boolean> {
        val fields = hashMapOf<String, Any>(
            "geofenceLat" to d.geofenceLat,
            "geofenceLng" to d.geofenceLng,
            "radiusMeters" to d.radiusMeters,
            "firstClassStart" to d.firstClassStart,
            "graceMinutes" to d.graceMinutes,
            "lowAttendanceThreshold" to d.lowAttendanceThreshold,
            "holidays" to d.holidays,
            "semesterStart" to (d.semesterStart ?: FieldValue.delete())
        )
        val task = db.collection(FirestorePaths.DEPARTMENTS).document(id).update(fields)

        var completed = false
        return try {
            withTimeoutOrNull(6_000L) {
                task.awaitTask()
                completed = true
            }
            Result.success(completed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}