package com.ayesha.geopresence.data.model

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
)