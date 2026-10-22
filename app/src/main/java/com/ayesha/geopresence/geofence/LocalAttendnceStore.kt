package com.ayesha.geopresence.geofence

import android.content.Context
import com.ayesha.geopresence.data.model.AppUser

class LocalAttendanceStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("attendance_local", Context.MODE_PRIVATE)

    data class CachedUser(
        val uid: String,
        val name: String,
        val universityId: String,
        val role: String,
        val departmentId: String,
        val sectionKey: String?
    )

    data class DayState(
        val uid: String,
        val dateKey: String,
        val firstEntryMillis: Long,
        val lastEnterMillis: Long,
        val currentEnterMillis: Long,   // 0 when the user is outside
        val accumulatedMillis: Long
    )

    fun saveUser(user: AppUser) {
        prefs.edit()
            .putString("u_uid", user.uid)
            .putString("u_name", user.name)
            .putString("u_univ_id", user.universityId)
            .putString("u_role", user.role.name)
            .putString("u_dept", user.departmentId)
            .putString("u_section", user.sectionKey)
            .apply()
    }

    fun loadUser(): CachedUser? {
        val uid = prefs.getString("u_uid", null) ?: return null
        return CachedUser(
            uid = uid,
            name = prefs.getString("u_name", "").orEmpty(),
            universityId = prefs.getString("u_univ_id", "").orEmpty(),
            role = prefs.getString("u_role", "STUDENT").orEmpty(),
            departmentId = prefs.getString("u_dept", "").orEmpty(),
            sectionKey = prefs.getString("u_section", null)
        )
    }

    fun clearUser() {
        prefs.edit()
            .remove("u_uid").remove("u_name").remove("u_univ_id")
            .remove("u_role").remove("u_dept").remove("u_section")
            .apply()
    }

    fun saveDay(d: DayState) {
        prefs.edit()
            .putString("d_uid", d.uid)
            .putString("d_date", d.dateKey)
            .putLong("d_first", d.firstEntryMillis)
            .putLong("d_last_enter", d.lastEnterMillis)
            .putLong("d_current_enter", d.currentEnterMillis)
            .putLong("d_accumulated", d.accumulatedMillis)
            .apply()
    }

    fun loadDay(): DayState? {
        val uid = prefs.getString("d_uid", null) ?: return null
        val date = prefs.getString("d_date", null) ?: return null
        return DayState(
            uid = uid,
            dateKey = date,
            firstEntryMillis = prefs.getLong("d_first", 0L),
            lastEnterMillis = prefs.getLong("d_last_enter", 0L),
            currentEnterMillis = prefs.getLong("d_current_enter", 0L),
            accumulatedMillis = prefs.getLong("d_accumulated", 0L)
        )
    }

    fun clearDay() {
        prefs.edit()
            .remove("d_uid").remove("d_date").remove("d_first")
            .remove("d_last_enter").remove("d_current_enter").remove("d_accumulated")
            .apply()
    }
}