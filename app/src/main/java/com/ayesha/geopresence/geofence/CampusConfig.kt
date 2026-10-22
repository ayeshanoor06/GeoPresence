package com.ayesha.geopresence.geofence

import android.content.Context
import android.content.SharedPreferences
import com.ayesha.geopresence.data.model.Department

data class CampusConfig(
    val departmentId: String,
    val departmentName: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
    val firstClassStart: String,
    val graceMinutes: Int
) {
    companion object {
        fun from(d: Department) = CampusConfig(
            departmentId = d.id,
            departmentName = d.name,
            latitude = d.geofenceLat,
            longitude = d.geofenceLng,
            radiusMeters = d.radiusMeters,
            firstClassStart = d.firstClassStart,
            graceMinutes = d.graceMinutes
        )
    }
}

data class GeofenceEvent(val type: String, val timeMillis: Long, val result: String? = null)

/** Small on-device cache so geofencing works after a reboot and without internet. */
class CampusConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("campus_config", Context.MODE_PRIVATE)

    fun save(config: CampusConfig) {
        prefs.edit()
            .putString(K_DEPT_ID, config.departmentId)
            .putString(K_DEPT_NAME, config.departmentName)
            .putString(K_LAT, config.latitude.toString())
            .putString(K_LNG, config.longitude.toString())
            .putString(K_RADIUS, config.radiusMeters.toString())
            .putString(K_FIRST_CLASS, config.firstClassStart)
            .putInt(K_GRACE, config.graceMinutes)
            .apply()
    }

    fun load(): CampusConfig? {
        val id = prefs.getString(K_DEPT_ID, null) ?: return null
        val lat = prefs.getString(K_LAT, null)?.toDoubleOrNull() ?: return null
        val lng = prefs.getString(K_LNG, null)?.toDoubleOrNull() ?: return null
        val radius = prefs.getString(K_RADIUS, null)?.toDoubleOrNull() ?: return null
        return CampusConfig(
            departmentId = id,
            departmentName = prefs.getString(K_DEPT_NAME, "").orEmpty(),
            latitude = lat,
            longitude = lng,
            radiusMeters = radius,
            firstClassStart = prefs.getString(K_FIRST_CLASS, "08:30").orEmpty(),
            graceMinutes = prefs.getInt(K_GRACE, 15)
        )
    }

    fun saveLastEvent(type: String, timeMillis: Long, result: String? = null) {
        prefs.edit()
            .putString(K_EVENT_TYPE, type)
            .putLong(K_EVENT_TIME, timeMillis)
            .putString(K_EVENT_RESULT, result)
            .apply()
    }

    fun setInside(inside: Boolean) {
        prefs.edit().putBoolean(K_INSIDE, inside).apply()
    }

    fun isInside(): Boolean = prefs.getBoolean(K_INSIDE, false)

    fun loadLastEvent(): GeofenceEvent? {
        val type = prefs.getString(K_EVENT_TYPE, null) ?: return null
        return GeofenceEvent(
            type = type,
            timeMillis = prefs.getLong(K_EVENT_TIME, 0L),
            result = prefs.getString(K_EVENT_RESULT, null)
        )
    }

    fun clear() = prefs.edit().clear().apply()

    fun registerListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(l)

    fun unregisterListener(l: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(l)

    private companion object {
        const val K_DEPT_ID = "dept_id"
        const val K_DEPT_NAME = "dept_name"
        const val K_LAT = "lat"
        const val K_LNG = "lng"
        const val K_RADIUS = "radius"
        const val K_FIRST_CLASS = "first_class"
        const val K_GRACE = "grace"
        const val K_EVENT_TYPE = "event_type"
        const val K_EVENT_TIME = "event_time"

        const val K_INSIDE = "inside"

        const val K_EVENT_RESULT = "event_result"
    }
}