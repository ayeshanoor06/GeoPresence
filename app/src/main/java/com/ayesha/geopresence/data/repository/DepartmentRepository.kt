package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.Department
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.awaitTask
import com.google.firebase.firestore.FirebaseFirestore

class DepartmentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    suspend fun getDepartment(id: String): Result<Department> = runCatching {
        val snapshot = db.collection(FirestorePaths.DEPARTMENTS).document(id).get().awaitTask()
        Department.fromSnapshot(snapshot)
            ?: error("Department settings not found. Create departments/$id in Firestore.")
    }
}