package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.data.model.AttendanceSource
import com.ayesha.geopresence.data.model.AttendanceStatus
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.Keys
import com.ayesha.geopresence.util.awaitTask
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class ManualAttendanceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Result value: true = saved on the server, false = saved on the phone and will sync. */
    suspend fun setStatus(
        teacher: AppUser,
        student: AppUser,
        dateKey: String,
        existing: AttendanceRecord?,
        status: AttendanceStatus,
        reason: String
    ): Result<Boolean> {
        val ref = db.collection(FirestorePaths.ATTENDANCE)
            .document(Keys.attendanceDocId(student.uid, dateKey))
        val note = if (reason.isBlank()) "Marked by ${teacher.name}"
        else "Marked by ${teacher.name}: $reason"

        val task: Task<Void> = if (existing != null) {
            ref.update(
                mapOf(
                    "status" to status.name,
                    "source" to AttendanceSource.MANUAL.name,
                    "overriddenBy" to teacher.uid,
                    "note" to note
                )
            )
        } else {
            ref.set(
                hashMapOf<String, Any?>(
                    "userId" to student.uid,
                    "userName" to student.name,
                    "universityId" to student.universityId,
                    "role" to student.role.name,
                    "departmentId" to student.departmentId,
                    "sectionKey" to student.sectionKey,
                    "date" to dateKey,
                    "entryTime" to null,
                    "exitTime" to null,
                    "durationMinutes" to 0,
                    "status" to status.name,
                    "source" to AttendanceSource.MANUAL.name,
                    "overriddenBy" to teacher.uid,
                    "note" to note
                )
            )
        }

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