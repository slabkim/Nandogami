package com.example.nandogami.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.adapter.UserSearchAdapter
import com.example.nandogami.databinding.ActivityUserListBinding
import com.example.nandogami.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserListBinding
    private lateinit var adapter: UserSearchAdapter
    private val db = FirebaseFirestore.getInstance()
    private var userId: String = ""
    private var listType: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userId = intent.getStringExtra("userId") ?: ""
        listType = intent.getStringExtra("listType") ?: ""

        adapter = UserSearchAdapter(emptyList()) { user ->
            val intent = Intent(this, OtherProfileActivity::class.java)
            intent.putExtra("userId", user.id)
            startActivity(intent)
        }
        binding.rvUserList.layoutManager = LinearLayoutManager(this)
        binding.rvUserList.adapter = adapter

        if (userId.isEmpty() || listType.isEmpty()) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadUserList()
    }

    private fun loadUserList() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userIds = when (listType) {
                    "followers" -> getFollowers(userId)
                    "following" -> getFollowing(userId)
                    else -> emptyList()
                }
                val users = if (userIds.isNotEmpty()) getUsersByIds(userIds) else emptyList()
                runOnUiThread {
                    adapter.updateData(users)
                    binding.tvTitle.text = if (listType == "followers") "Followers" else "Following"
                    binding.tvEmpty.text = if (users.isEmpty()) "No users found" else ""
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@UserListActivity, "Failed to load users", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun getFollowers(userId: String): List<String> {
        val snapshot = db.collection("follows")
            .whereEqualTo("followingId", userId)
            .whereEqualTo("isActive", true)
            .get().await()
        return snapshot.documents.mapNotNull { it.getString("followerId") }
    }

    private suspend fun getFollowing(userId: String): List<String> {
        val snapshot = db.collection("follows")
            .whereEqualTo("followerId", userId)
            .whereEqualTo("isActive", true)
            .get().await()
        return snapshot.documents.mapNotNull { it.getString("followingId") }
    }

    private suspend fun getUsersByIds(ids: List<String>): List<User> {
        if (ids.isEmpty()) return emptyList()
        val chunks = ids.chunked(10) // Firestore whereIn max 10
        val users = mutableListOf<User>()
        for (chunk in chunks) {
            val snapshot = db.collection("users")
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get().await()
            users += snapshot.documents.mapNotNull { doc ->
                val user = doc.toObject(User::class.java)
                if (user != null) user.copy(id = doc.id) else null
            }
        }
        return users
    }
} 