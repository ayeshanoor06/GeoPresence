
package com.ayesha.geopresence.data.model

import com.google.firebase.Timestamp

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
)