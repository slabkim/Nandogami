package com.example.nandogami.data

import com.example.nandogami.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")

    suspend fun searchUsersByUsername(query: String): List<User> {
        if (query.isEmpty()) return emptyList()
        return try {
            val snapshot = usersCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null && user.username.contains(query, ignoreCase = true)) {
                    user.copy(id = doc.id)
                } else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
} 