package com.ayesha.geopresence.util

import java.time.LocalDate

object AppConstants {
    const val DEPARTMENT_ID = "it"
}

object FirestorePaths {
    const val USERS = "users"
    const val DEPARTMENTS = "departments"
    const val CLASSES = "classes"
    const val ATTENDANCE = "attendance"
    const val MOCK_GPS_LOGS = "mockGpsLogs"
}

object Keys {
    /** e.g. it_6_A */
    fun sectionKey(departmentId: String, semester: Int, section: String): String =
        "${departmentId}_${semester}_${section.trim().uppercase()}"

    /** Date as yyyy-MM-dd */
    fun dateKey(date: LocalDate = LocalDate.now()): String = date.toString()

    /** One attendance document per user per day */
    fun attendanceDocId(uid: String, dateKey: String): String = "${uid}_$dateKey"
}