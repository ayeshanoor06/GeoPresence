package com.ayesha.geopresence.data.model

data class Department(
    val id: String = "",
    val name: String = "",
    val geofenceLat: Double = 0.0,
    val geofenceLng: Double = 0.0,
    val radiusMeters: Double = 150.0,
    val firstClassStart: String = "08:30",
    val graceMinutes: Int = 15,
    val lowAttendanceThreshold: Int = 75
)