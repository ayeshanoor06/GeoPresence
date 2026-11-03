package com.ayesha.geopresence.data.model

import com.google.firebase.firestore.DocumentSnapshot
import java.time.DayOfWeek

data class ClassInfo(
    val id: String = "",
    val departmentId: String = "",
    val subjectName: String = "",
    val subjectCode: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val semester: Int = 0,
    val section: String = "",
    val sectionKey: String = "",
    val days: List<String> = emptyList(),   // "MON", "TUE", ...
    val startTime: String = "09:00",
    val endTime: String = "10:00"
) {
    val title: String get() = subjectName.ifBlank { "Class" }
    val sectionLabel: String
        get() = if (semester > 0) "Semester $semester · Section $section" else "Section $section"
    val timeLabel: String get() = "$startTime – $endTime"
    val daysLabel: String
        get() = days.joinToString(", ") { d -> d.lowercase().replaceFirstChar { it.uppercase() } }

    fun runsOn(day: DayOfWeek): Boolean =
        days.any { it.equals(day.name.take(3), ignoreCase = true) }

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): ClassInfo? {
            if (!doc.exists()) return null
            return ClassInfo(
                id = doc.id,
                departmentId = doc.getString("departmentId").orEmpty(),
                subjectName = doc.getString("subjectName").orEmpty(),
                subjectCode = doc.getString("subjectCode").orEmpty(),
                teacherId = doc.getString("teacherId").orEmpty(),
                teacherName = doc.getString("teacherName").orEmpty(),
                semester = doc.getLong("semester")?.toInt() ?: 0,
                section = doc.getString("section").orEmpty(),
                sectionKey = doc.getString("sectionKey").orEmpty(),
                days = (doc.get("days") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                startTime = doc.getString("startTime") ?: "09:00",
                endTime = doc.getString("endTime") ?: "10:00"
            )
        }
    }
}