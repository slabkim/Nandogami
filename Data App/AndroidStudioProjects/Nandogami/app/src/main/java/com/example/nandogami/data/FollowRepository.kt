package com.example.nandogami.data

import com.example.nandogami.model.Follow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

class FollowRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun followUser(followingId: String): Result<Follow> {
        return try {
            val followerId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            if (followerId == followingId) {
                return Result.failure(Exception("Cannot follow yourself"))
            }
            
            // Check if already following (any status)
            val existingFollow = db.collection("follows")
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .get()
                .await()
            
            if (!existingFollow.isEmpty) {
                val doc = existingFollow.documents.first()
                val isActive = doc.getBoolean("isActive") ?: false
                if (isActive) {
                    return Result.failure(Exception("Already following this user"))
                } else {
                    // Reactivate follow
                    db.collection("follows").document(doc.id).update(
                        mapOf(
                            "isActive" to true,
                            "followDate" to System.currentTimeMillis()
                        )
                    ).await()
                    updateUserCounts(followerId, followingId, true)
                    val follow = doc.toObject(Follow::class.java)?.copy(isActive = true) ?: Follow()
                    return Result.success(follow)
                }
            }
            
            val follow = Follow(
                id = db.collection("follows").document().id,
                followerId = followerId,
                followingId = followingId,
                followDate = System.currentTimeMillis(),
                isActive = true
            )
            
            db.collection("follows").document(follow.id).set(follow).await()
            
            // Update user counts
            updateUserCounts(followerId, followingId, true)
            
            Result.success(follow)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followingId: String): Result<Unit> {
        return try {
            val followerId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val followDoc = db.collection("follows")
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            if (!followDoc.isEmpty) {
                db.collection("follows").document(followDoc.documents.first().id).update(
                    mapOf("isActive" to false)
                ).await()
                updateUserCounts(followerId, followingId, false)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateUserCounts(followerId: String, followingId: String, isFollowing: Boolean) {
        try {
            val increment = if (isFollowing) 1 else -1
            
            // Update follower's following count
            try {
                db.collection("users").document(followerId).update(
                    "followingCount", 
                    com.google.firebase.firestore.FieldValue.increment(increment.toLong())
                ).await()
            } catch (e: Exception) {
                // If document doesn't exist, create it with initial count
                if (isFollowing) {
                    db.collection("users").document(followerId).set(
                        mapOf("followingCount" to 1), 
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                }
            }
            
            // Update following's follower count
            try {
                db.collection("users").document(followingId).update(
                    "followersCount", 
                    com.google.firebase.firestore.FieldValue.increment(increment.toLong())
                ).await()
            } catch (e: Exception) {
                // If document doesn't exist, create it with initial count
                if (isFollowing) {
                    db.collection("users").document(followingId).set(
                        mapOf("followersCount" to 1), 
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                }
            }
        } catch (e: Exception) {
            Log.e("FollowRepository", "Error updating user counts", e)
        }
    }

    suspend fun isFollowing(followingId: String): Boolean {
        return try {
            val followerId = auth.currentUser?.uid ?: return false
            
            val followDoc = db.collection("follows")
                .whereEqualTo("followerId", followerId)
                .whereEqualTo("followingId", followingId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            !followDoc.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getFollowers(userId: String): List<String> {
        return try {
            val snapshot = db.collection("follows")
                .whereEqualTo("followingId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.getString("followerId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowing(userId: String): List<String> {
        return try {
            val snapshot = db.collection("follows")
                .whereEqualTo("followerId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { it.getString("followingId") }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getFollowersCount(userId: String): Int {
        return try {
            val snapshot = db.collection("follows")
                .whereEqualTo("followingId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }

    suspend fun getFollowingCount(userId: String): Int {
        return try {
            val snapshot = db.collection("follows")
                .whereEqualTo("followerId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            snapshot.size()
        } catch (e: Exception) {
            0
        }
    }
} 