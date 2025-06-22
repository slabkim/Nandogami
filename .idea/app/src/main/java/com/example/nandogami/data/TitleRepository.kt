package com.example.nandogami.data

import com.example.nandogami.model.Title
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TitleRepository {

    private val db = FirebaseFirestore.getInstance()
    private val titlesCollection = db.collection("titles")

    suspend fun searchTitles(query: String): List<Title> {
        if (query.isEmpty()) {
            return emptyList()
        }
        return try {
            val snapshot = titlesCollection
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
                .get()
                .await()
            snapshot.toObjects(Title::class.java)
        } catch (e: Exception) {
            // Handle error, e.g., log it or return an empty list
            emptyList()
        }
    }

    suspend fun getPopularTitles(): List<Title> {
        return try {
            val snapshot = titlesCollection
                .orderBy("rating") // Assuming higher rating is more popular
                .limit(10)
                .get()
                .await()
            snapshot.toObjects(Title::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
} 