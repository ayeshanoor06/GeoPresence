package com.ayesha.geopresence.data.model

/** What the admin enters in the class form, before it is saved to Firestore. */
data class ClassDraft(
    val subjectName: String,
    val subjectCode: String,
    val teacher: AppUser,
    val semester: Int,
    val section: String,
    val days: List<String>,
    val startTime: String,
    val endTime: String,
    val room: Room? = null,
    val checkDelayMinutes: Int = 15
)