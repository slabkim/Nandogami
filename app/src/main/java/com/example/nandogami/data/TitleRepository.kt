package com.example.nandogami.data

import android.util.Log
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
            // Ambil semua data dan filter di aplikasi untuk case-insensitive search
            val snapshot = titlesCollection.get().await()
            
            // Filter data dengan case-insensitive search
            val filteredTitles = snapshot.mapNotNull { document ->
                try {
                    val title = document.toObject(Title::class.java)
                    title?.id = document.id
                    title
                } catch (e: Exception) {
                    Log.e("TitleRepository", "Gagal memetakan dokumen: ${document.id}", e)
                    null
                }
            }.filter { title ->
                // Case-insensitive search pada title
                title.title.contains(query, ignoreCase = true) ||
                // Juga search pada author
                title.author.contains(query, ignoreCase = true) ||
                // Dan search pada categories
                title.categories.any { it.contains(query, ignoreCase = true) }
            }
            
            filteredTitles

        } catch (e: Exception) {
            Log.e("TitleRepository", "Error saat mencari judul", e)
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