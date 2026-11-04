package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.AttendanceRecord
import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.data.model.MockGpsLog
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.awaitTask
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull

/** Everything the admin screens read or change. Firestore rules restrict this to ADMIN users. */
class AdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Every user in the department (live). */
    fun observeUsers(): Flow<Result<List<AppUser>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val users = snapshot?.documents
                    ?.mapNotNull { AppUser.fromSnapshot(it) }
                    .orEmpty()
                trySend(Result.success(users))
            }
        awaitClose { registration.remove() }
    }

    /** Every class in the department (live). */
    fun observeClasses(): Flow<Result<List<ClassInfo>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.CLASSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val classes = snapshot?.documents
                    ?.mapNotNull { ClassInfo.fromSnapshot(it) }
                    .orEmpty()
                trySend(Result.success(classes))
            }
        awaitClose { registration.remove() }
    }

    /** All attendance records of one day (yyyy-MM-dd), students and teachers. */
    fun observeAttendance(dateKey: String): Flow<Result<List<AttendanceRecord>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.ATTENDANCE)
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

    /** Most recent rejected fake-GPS attempts, newest first. */
    fun observeMockLogs(limit: Long = 100): Flow<Result<List<MockGpsLog>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.MOCK_GPS_LOGS)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents
                    ?.mapNotNull { MockGpsLog.fromSnapshot(it) }
                    .orEmpty()
                trySend(Result.success(logs))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Updates some fields of one user document.
     * Result value: true = saved on the server, false = saved on the phone and will sync.
     */
    suspend fun updateUser(uid: String, fields: Map<String, Any>): Result<Boolean> {
        val task = db.collection(FirestorePaths.USERS).document(uid).update(fields)
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