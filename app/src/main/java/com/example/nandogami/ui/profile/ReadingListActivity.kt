package com.example.nandogami.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.adapter.TitleAdapter
import com.example.nandogami.databinding.ActivityReadingListBinding
import com.example.nandogami.model.Title
import com.example.nandogami.model.ReadingStatus
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ReadingListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReadingListBinding
    private lateinit var adapter: TitleAdapter
    private val db = FirebaseFirestore.getInstance()
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId") ?: ""
        adapter = TitleAdapter(emptyList()) { title ->
            val intent = Intent(this, com.example.nandogami.ui.detail.DetailActivity::class.java)
            intent.putExtra("titleId", title.id)
            startActivity(intent)
        }
        binding.rvReadingList.layoutManager = LinearLayoutManager(this)
        binding.rvReadingList.adapter = adapter

        if (userId.isEmpty()) {
            Toast.makeText(this, "Invalid user", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadReadingList()
    }

    private fun loadReadingList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Ambil reading status user
                val snapshot = db.collection("reading_status")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("status", "reading")
                    .get().await()
                val titleIds = snapshot.documents.mapNotNull { it.getString("titleId") }
                val titles = if (titleIds.isNotEmpty()) getTitlesByIds(titleIds) else emptyList()
                runOnUiThread {
                    adapter.updateData(titles)
                    binding.tvEmpty.text = if (titles.isEmpty()) "No reading titles found" else ""
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ReadingListActivity, "Failed to load reading list", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun getTitlesByIds(ids: List<String>): List<Title> {
        if (ids.isEmpty()) return emptyList()
        val chunks = ids.chunked(10)
        val titles = mutableListOf<Title>()
        for (chunk in chunks) {
            val snapshot = db.collection("titles")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get().await()
            titles += snapshot.documents.mapNotNull { doc ->
                val title = doc.toObject(Title::class.java)
                if (title != null) title.copy(id = doc.id) else null
            }
        }
        return titles
    }
} 