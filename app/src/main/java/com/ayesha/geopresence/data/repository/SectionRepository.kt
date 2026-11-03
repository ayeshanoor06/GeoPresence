package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.util.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SectionRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Students of one section (needs the section in the teacher's assignedSectionKeys). */
    fun observeStudents(sectionKey: String): Flow<Result<List<AppUser>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.USERS)
            .whereEqualTo("role", "STUDENT")
            .whereEqualTo("sectionKey", sectionKey)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val students = snapshot?.documents
                    ?.mapNotNull { AppUser.fromSnapshot(it) }
                    .orEmpty()
                    .sortedBy { it.name.lowercase() }
                trySend(Result.success(students))
            }
        awaitClose { registration.remove() }
    }

    /** Attendance records of one section on one day (yyyy-MM-dd). */
    fun observeAttendance(sectionKey: String, dateKey: String): Flow<Result<List<AttendanceRecord>>> =
        callbackFlow {
            val registration = db.collection(FirestorePaths.ATTENDANCE)
                .whereEqualTo("sectionKey", sectionKey)
                .whereEqualTo("date", dateKey)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        trySend(Result.failure(error))
                        return@addSnapshotListener
                    }
                    val records = snapshot?.documents
                        ?.mapNotNull { AttendanceRecord.fromSnapshot(it) }
                        .orEmpty()
                    trySend(Result.success(records))
                }
            awaitClose { registration.remove() }
        }
}