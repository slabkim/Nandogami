package com.example.nandogami.data

import com.example.nandogami.model.ReadingStatus
import com.example.nandogami.model.ReadingStatusType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ReadingStatusRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addReadingStatus(titleId: String, status: ReadingStatusType, currentChapter: Int = 0): Result<ReadingStatus> {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val readingStatus = ReadingStatus(
                id = db.collection("reading_status").document().id,
                userId = userId,
                titleId = titleId,
                status = status,
                currentChapter = currentChapter,
                startDate = if (status == ReadingStatusType.READING) System.currentTimeMillis() else 0,
                lastReadDate = System.currentTimeMillis()
            )
            
            db.collection("reading_status").document(readingStatus.id).set(readingStatus).await()
            Result.success(readingStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateReadingStatus(statusId: String, status: ReadingStatusType, currentChapter: Int? = null): Result<ReadingStatus> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status.name,
                "lastReadDate" to System.currentTimeMillis()
            )
            
            if (currentChapter != null) {
                updates["currentChapter"] = currentChapter
            }
            
            if (status == ReadingStatusType.COMPLETED) {
                updates["finishDate"] = System.currentTimeMillis()
            }
            
            db.collection("reading_status").document(statusId).update(updates).await()
            
            // Fetch updated document
            val doc = db.collection("reading_status").document(statusId).get().await()
            val readingStatus = doc.toObject(ReadingStatus::class.java)
            Result.success(readingStatus ?: ReadingStatus())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserReadingStatus(userId: String, status: ReadingStatusType? = null): List<ReadingStatus> {
        return try {
            var query = db.collection("reading_status").whereEqualTo("userId", userId)
            
            if (status != null) {
                query = query.whereEqualTo("status", status.name)
            }
            
            val snapshot = query.get().await()
            snapshot.toObjects(ReadingStatus::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getReadingStatusByTitle(titleId: String): ReadingStatus? {
        return try {
            val userId = auth.currentUser?.uid ?: return null
            val doc = db.collection("reading_status")
                .whereEqualTo("userId", userId)
                .whereEqualTo("titleId", titleId)
                .get()
                .await()
            
            if (!doc.isEmpty) {
                doc.documents.first().toObject(ReadingStatus::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteReadingStatus(statusId: String): Result<Unit> {
        return try {
            db.collection("reading_status").document(statusId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 