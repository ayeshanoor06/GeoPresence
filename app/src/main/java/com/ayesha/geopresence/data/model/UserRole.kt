package com.ayesha.geopresence.data.model

enum class UserRole(val label: String) {
    STUDENT("Student"),
    TEACHER("Teacher"),
    ADMIN("Admin");

    companion object {
        fun fromString(value: String?): UserRole? = entries.firstOrNull { it.name == value }
    }
}