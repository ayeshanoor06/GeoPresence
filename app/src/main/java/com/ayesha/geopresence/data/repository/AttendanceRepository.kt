package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.util.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AttendanceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /** Live list of one user's attendance records (also works from the offline cache). */
    fun observeUserRecords(uid: String): Flow<Result<List<AttendanceRecord>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.ATTENDANCE)
            .whereEqualTo("userId", uid)
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