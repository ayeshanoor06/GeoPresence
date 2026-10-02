package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.AppUser
import com.ayesha.geopresence.data.model.RegistrationForm
import com.ayesha.geopresence.data.model.UserRole
import com.ayesha.geopresence.data.model.UserStatus
import com.ayesha.geopresence.util.AppConstants
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.Keys
import com.ayesha.geopresence.util.awaitTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun login(email: String, password: String): Result<AppUser> = runCatching {
        auth.signInWithEmailAndPassword(email, password).awaitTask()
        val uid = auth.currentUser?.uid ?: error("Sign-in failed. Please try again.")
        fetchProfile(uid)
    }.onFailure {
        if (auth.currentUser != null) auth.signOut()
    }

    suspend fun register(form: RegistrationForm): Result<AppUser> = runCatching {
        auth.createUserWithEmailAndPassword(form.email, form.password).awaitTask()
        val firebaseUser = auth.currentUser ?: error("Registration failed. Please try again.")
        val uid = firebaseUser.uid
        val isStudent = form.role == UserRole.STUDENT
        val departmentId = AppConstants.DEPARTMENT_ID

        val profile = mutableMapOf<String, Any>(
            "uid" to uid,
            "name" to form.name.trim(),
            "email" to firebaseUser.email.orEmpty(),
            "universityId" to form.universityId.trim().uppercase(),
            "role" to form.role.name,
            "status" to (if (isStudent) UserStatus.ACTIVE.name else UserStatus.PENDING.name),
            "departmentId" to departmentId,
            "assignedSectionKeys" to emptyList<String>(),
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (isStudent) {
            val semester = requireNotNull(form.semester)
            val section = requireNotNull(form.section).trim().uppercase()
            profile["semester"] = semester
            profile["section"] = section
            profile["batch"] = requireNotNull(form.batch).trim()
            profile["sectionKey"] = Keys.sectionKey(departmentId, semester, section)
        }

        try {
            db.collection(FirestorePaths.USERS).document(uid).set(profile).awaitTask()
        } catch (e: Exception) {
            auth.currentUser?.delete()   // don't leave an account without a profile
            throw e
        }
        fetchProfile(uid)
    }

    /** The signed-in user's profile, or null if nobody is signed in or it can't be read. */
    suspend fun currentUserProfile(): AppUser? {
        val user = auth.currentUser ?: return null
        return runCatching { fetchProfile(user.uid) }.getOrNull()
    }

    fun signOut() = auth.signOut()

    private suspend fun fetchProfile(uid: String): AppUser {
        val snapshot = db.collection(FirestorePaths.USERS).document(uid).get().awaitTask()
        val user = AppUser.fromSnapshot(snapshot) ?: error("No profile found for this account.")
        if (user.status == UserStatus.DISABLED) {
            error("This account has been disabled. Please contact your department admin.")
        }
        return user
    }
}