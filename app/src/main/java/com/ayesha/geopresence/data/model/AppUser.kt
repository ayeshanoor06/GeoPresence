package com.ayesha.geopresence.data.model

import com.google.firebase.firestore.DocumentSnapshot

enum class UserStatus { ACTIVE, PENDING, DISABLED }

data class AppUser(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val universityId: String = "",
    val role: UserRole = UserRole.STUDENT,
    val status: UserStatus = UserStatus.ACTIVE,
    val departmentId: String = "",
    val semester: Int? = null,
    val section: String? = null,
    val batch: String? = null,
    val sectionKey: String? = null,
    val assignedSectionKeys: List<String> = emptyList()
) {
    companion object {
        fun fromSnapshot(doc: DocumentSnapshot): AppUser? {
            if (!doc.exists()) return null
            val role = UserRole.fromString(doc.getString("role")) ?: return null
            val status = UserStatus.entries.firstOrNull { it.name == doc.getString("status") }
                ?: UserStatus.ACTIVE
            return AppUser(
                uid = doc.id,
                name = doc.getString("name").orEmpty(),
                email = doc.getString("email").orEmpty(),
                universityId = doc.getString("universityId").orEmpty(),
                role = role,
                status = status,
                departmentId = doc.getString("departmentId").orEmpty(),
                semester = doc.getLong("semester")?.toInt(),
                section = doc.getString("section"),
                batch = doc.getString("batch"),
                sectionKey = doc.getString("sectionKey"),
                assignedSectionKeys = (doc.get("assignedSectionKeys") as? List<*>)
                    ?.filterIsInstance<String>().orEmpty()
            )
        }
    }
}

/** Data collected on the Register screen. */
data class RegistrationForm(
    val name: String,
    val email: String,
    val password: String,
    val role: UserRole,
    val universityId: String,
    val semester: Int? = null,
    val section: String? = null,
    val batch: String? = null
)