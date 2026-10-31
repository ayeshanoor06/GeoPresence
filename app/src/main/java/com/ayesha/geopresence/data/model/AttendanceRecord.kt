package com.ayesha.geopresence.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

enum class AttendanceStatus { PRESENT, LATE, ABSENT }
enum class AttendanceSource { AUTO, MANUAL }

data class AttendanceRecord(
    val userId: String = "",
    val userName: String = "",
    val universityId: String = "",
    val role: UserRole = UserRole.STUDENT,
    val departmentId: String = "",
    val sectionKey: String? = null,      // null for teachers
    val date: String = "",               // yyyy-MM-dd
    val entryTime: Timestamp? = null,
    val exitTime: Timestamp? = null,
    val durationMinutes: Int = 0,
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val source: AttendanceSource = AttendanceSource.AUTO,
    val overriddenBy: String? = null,    // teacher uid for manual overrides
    val note: String? = null
) {
    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): AttendanceRecord? {
            val date = doc.getString("date") ?: return null
            val status = AttendanceStatus.entries
                .firstOrNull { it.name == doc.getString("status") } ?: return null
            return AttendanceRecord(
                userId = doc.getString("userId").orEmpty(),
                userName = doc.getString("userName").orEmpty(),
                universityId = doc.getString("universityId").orEmpty(),
                role = UserRole.fromString(doc.getString("role")) ?: UserRole.STUDENT,
                departmentId = doc.getString("departmentId").orEmpty(),
                sectionKey = doc.getString("sectionKey"),
                date = date,
                entryTime = doc.getTimestamp("entryTime"),
                exitTime = doc.getTimestamp("exitTime"),
                durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                status = status,
                source = AttendanceSource.entries
                    .firstOrNull { it.name == doc.getString("source") } ?: AttendanceSource.AUTO,
                overriddenBy = doc.getString("overriddenBy"),
                note = doc.getString("note")
            )
        }
    }
}