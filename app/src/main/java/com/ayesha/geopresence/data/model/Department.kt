package com.ayesha.geopresence.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class Department(
    val id: String = "",
    val name: String = "",
    val geofenceLat: Double = 0.0,
    val geofenceLng: Double = 0.0,
    val radiusMeters: Double = 150.0,
    val firstClassStart: String = "08:30",
    val graceMinutes: Int = 15,
    val lowAttendanceThreshold: Int = 75
) {
    /** False until the admin/you have entered real campus coordinates. */
    val hasGeofence: Boolean get() = geofenceLat != 0.0 || geofenceLng != 0.0

    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): Department? {
            if (!doc.exists()) return null
            return Department(
                id = doc.id,
                name = doc.getString("name").orEmpty(),
                geofenceLat = doc.getDouble("geofenceLat") ?: 0.0,
                geofenceLng = doc.getDouble("geofenceLng") ?: 0.0,
                radiusMeters = doc.getDouble("radiusMeters") ?: 150.0,
                firstClassStart = doc.getString("firstClassStart") ?: "08:30",
                graceMinutes = doc.getLong("graceMinutes")?.toInt() ?: 15,
                lowAttendanceThreshold = doc.getLong("lowAttendanceThreshold")?.toInt() ?: 75
            )
        }
    }
}