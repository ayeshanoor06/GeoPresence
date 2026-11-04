package com.ayesha.geopresence.data.model

import com.google.firebase.firestore.DocumentSnapshot

/** One rejected fake-GPS attempt, written by a phone when it detects a mock location. */
data class MockGpsLog(
    val id: String,
    val userId: String,
    val userName: String,
    val universityId: String,
    val departmentId: String,
    val timestampMillis: Long,
    val reason: String
) {
    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): MockGpsLog? {
            if (!doc.exists()) return null
            return MockGpsLog(
                id = doc.id,
                userId = doc.getString("userId").orEmpty(),
                userName = doc.getString("userName").orEmpty(),
                universityId = doc.getString("universityId").orEmpty(),
                departmentId = doc.getString("departmentId").orEmpty(),
                timestampMillis = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L,
                reason = doc.getString("reason").orEmpty()
            )
        }
    }
}