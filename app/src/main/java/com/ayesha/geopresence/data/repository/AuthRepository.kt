package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.UserRole
import com.ayesha.geopresence.util.awaitTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    suspend fun login(email: String, password: String): Result<UserRole> = runCatching {
        auth.signInWithEmailAndPassword(email, password).awaitTask()
        val uid = auth.currentUser?.uid ?: error("Sign-in failed. Please try again.")
        fetchRole(uid)
    }.onFailure {
        if (auth.currentUser != null) auth.signOut()
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<UserRole> = runCatching {
        auth.createUserWithEmailAndPassword(email, password).awaitTask()
        val uid = auth.currentUser?.uid ?: error("Registration failed. Please try again.")

        val profile = hashMapOf(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role.name,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            db.collection("users").document(uid).set(profile).awaitTask()
        } catch (e: Exception) {
            // Don't leave an account without a profile behind
            auth.currentUser?.delete()
            throw e
        }
        role
    }

    /** Returns the saved user's role, or null when nobody is signed in. */
    suspend fun currentUserRole(): UserRole? {
        val user = auth.currentUser ?: return null
        return runCatching { fetchRole(user.uid) }.getOrNull()
    }

    fun signOut() = auth.signOut()

    private suspend fun fetchRole(uid: String): UserRole {
        val snapshot = db.collection("users").document(uid).get().awaitTask()
        return UserRole.fromString(snapshot.getString("role"))
            ?: error("No profile found for this account.")
    }
}