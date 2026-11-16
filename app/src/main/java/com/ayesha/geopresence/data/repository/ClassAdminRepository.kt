package com.ayesha.geopresence.data.repository

import com.ayesha.geopresence.data.model.ClassDraft
import com.ayesha.geopresence.util.AppConstants
import com.ayesha.geopresence.util.FirestorePaths
import com.ayesha.geopresence.util.Keys
import com.ayesha.geopresence.util.awaitTask
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

class ClassAdminRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Creates (existingId == null) or updates a class, and adds the class's sectionKey to the
     * teacher's assignedSectionKeys in the same batch, so the two can never disagree.
     */
    suspend fun saveClass(existingId: String?, draft: ClassDraft): Result<Boolean> {
        val classes = db.collection(FirestorePaths.CLASSES)
        val ref = if (existingId != null) classes.document(existingId) else classes.document()

        val sectionKey = Keys.sectionKey(AppConstants.DEPARTMENT_ID, draft.semester, draft.section)
        val fields = hashMapOf<String, Any>(
            "departmentId" to AppConstants.DEPARTMENT_ID,
            "subjectName" to draft.subjectName.trim(),
            "subjectCode" to draft.subjectCode.trim().uppercase(),
            "teacherId" to draft.teacher.uid,
            "teacherName" to draft.teacher.name,
            "semester" to draft.semester,
            "section" to draft.section.trim().uppercase(),
            "sectionKey" to sectionKey,
            "days" to draft.days,
            "startTime" to draft.startTime,
            "endTime" to draft.endTime,
            "roomId" to (draft.room?.id ?: ""),
            "roomName" to (draft.room?.name ?: ""),
            "checkDelayMinutes" to draft.checkDelayMinutes
        )

        val batch = db.batch()
        batch.set(ref, fields)
        batch.update(
            db.collection(FirestorePaths.USERS).document(draft.teacher.uid),
            "assignedSectionKeys",
            FieldValue.arrayUnion(sectionKey)
        )
        return commit(batch.commit())
    }

    /** Deletes the class only. Sections stay on the teacher; remove them in User Management. */
    suspend fun deleteClass(id: String): Result<Boolean> =
        commit(db.collection(FirestorePaths.CLASSES).document(id).delete())

    private suspend fun commit(task: Task<Void>): Result<Boolean> {
        var completed = false
        return try {
            withTimeoutOrNull(6_000L) {
                task.awaitTask()
                completed = true
            }
            Result.success(completed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}