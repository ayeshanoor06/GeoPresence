package com.ayesha.geopresence.ui.navigation

import com.ayesha.geopresence.data.model.UserRole

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val STUDENT_HOME = "student_home"
    const val TEACHER_HOME = "teacher_home"
    const val ADMIN_HOME = "admin_home"

    fun homeFor(role: UserRole): String = when (role) {
        UserRole.STUDENT -> STUDENT_HOME
        UserRole.TEACHER -> TEACHER_HOME
        UserRole.ADMIN -> ADMIN_HOME
    }
}