package com.ayesha.geopresence.ui.navigation

import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.UserRole
import com.ayesha.geopresence.data.model.UserStatus

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val PENDING_APPROVAL = "pending_approval"
    const val STUDENT_HOME = "student_home"
    const val TEACHER_HOME = "teacher_home"
    const val ADMIN_HOME = "admin_home"

    fun homeFor(user: AppUser): String = when {
        user.status == UserStatus.PENDING -> PENDING_APPROVAL
        user.role == UserRole.STUDENT -> STUDENT_HOME
        user.role == UserRole.TEACHER -> TEACHER_HOME
        else -> ADMIN_HOME
    }
}