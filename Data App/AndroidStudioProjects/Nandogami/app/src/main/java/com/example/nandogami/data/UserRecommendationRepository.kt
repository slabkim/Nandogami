package com.example.nandogami.data

import com.example.nandogami.model.UserRecommendation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRecommendationRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun sendRecommendation(toUserId: String, titleId: String, message: String): Result<UserRecommendation> {
        return try {
            val fromUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            if (fromUserId == toUserId) {
                return Result.failure(Exception("Cannot recommend to yourself"))
            }
            
            val recommendation = UserRecommendation(
                id = db.collection("user_recommendations").document().id,
                fromUserId = fromUserId,
                toUserId = toUserId,
                titleId = titleId,
                message = message,
                recommendationDate = System.currentTimeMillis(),
                isRead = false
            )
            
            db.collection("user_recommendations").document(recommendation.id).set(recommendation).await()
            Result.success(recommendation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReceivedRecommendations(userId: String): List<UserRecommendation> {
        return try {
            val snapshot = db.collection("user_recommendations")
                .whereEqualTo("toUserId", userId)
                .orderBy("recommendationDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.toObjects(UserRecommendation::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSentRecommendations(userId: String): List<UserRecommendation> {
        return try {
            val snapshot = db.collection("user_recommendations")
                .whereEqualTo("fromUserId", userId)
                .orderBy("recommendationDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.toObjects(UserRecommendation::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markRecommendationAsRead(recommendationId: String): Result<Unit> {
        return try {
            db.collection("user_recommendations").document(recommendationId).update(
                mapOf(
                    "isRead" to true,
                    "readDate" to System.currentTimeMillis()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRecommendation(recommendationId: String): Result<Unit> {
        return try {
            db.collection("user_recommendations").document(recommendationId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadRecommendationsCount(userId: String): Int {
        return try {
            val snapshot = db.collection("user_recommendations")
                .whereEqualTo("toUserId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
} 