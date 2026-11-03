package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.ClassInfo
import com.ayesha.geopresence.util.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ClassRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    fun observeTeacherClasses(teacherUid: String): Flow<Result<List<ClassInfo>>> = callbackFlow {
        val registration = db.collection(FirestorePaths.CLASSES)
            .whereEqualTo("teacherId", teacherUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Result.failure(error))
                    return@addSnapshotListener
                }
                val classes = snapshot?.documents
                    ?.mapNotNull { ClassInfo.fromSnapshot(it) }
                    .orEmpty()
                    .sortedWith(compareBy({ it.startTime }, { it.subjectName }))
                trySend(Result.success(classes))
            }
        awaitClose { registration.remove() }
    }
}