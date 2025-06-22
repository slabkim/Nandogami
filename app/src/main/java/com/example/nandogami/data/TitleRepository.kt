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
            val snapshot = titlesCollection
                .whereGreaterThanOrEqualTo("title", query)
                .whereLessThanOrEqualTo("title", query + '\uf8ff')
                .get()
                .await()

            // =================== BAGIAN YANG DIPERBAIKI ===================
            // Ubah dari snapshot.toObjects ke pemetaan manual untuk mendapatkan ID
            snapshot.mapNotNull { document ->
                try {
                    val title = document.toObject(Title::class.java)
                    // Ini adalah baris kunci: kita set ID dari dokumen ke objek
                    title.id = document.id
                    title
                } catch (e: Exception) {
                    Log.e("TitleRepository", "Gagal memetakan dokumen: ${document.id}", e)
                    null
                }
            }
            // =============================================================

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