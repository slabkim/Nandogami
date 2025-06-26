package com.example.nandogami.ui.chat

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.adapter.ChatMessageAdapter
import com.example.nandogami.data.ChatRealtimeRepository
import com.example.nandogami.databinding.ActivityChatBinding
import com.example.nandogami.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val chatRepository = ChatRealtimeRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private var chatId: String = ""
    private var otherUserId: String = ""
    private var currentUser: User? = null
    private var otherUser: User? = null
    private lateinit var chatMessageAdapter: ChatMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        if (otherUserId.isEmpty()) {
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        setupMessageInput()
        loadUsersAndStartChat()
    }

    private fun loadUsersAndStartChat() {
        lifecycleScope.launch(Dispatchers.IO) { // Berjalan di background thread
            // Panggilan pertama ke Firestore
            currentUser = fetchUser(auth.currentUser?.uid)
            Log.d("ChatActivity", "Fetched currentUser: $currentUser (UID: ${auth.currentUser?.uid})")

            // Panggilan kedua ke Firestore
            otherUser = fetchUser(otherUserId) // otherUserId berasal dari Intent
            Log.d("ChatActivity", "Fetched otherUser: $otherUser (ID: $otherUserId)")

            withContext(Dispatchers.Main) { // Kembali ke Main thread untuk update UI dan Toast
                if (currentUser == null || otherUser == null) {
                    // SALAH SATU ATAU KEDUA USER GAGAL DIMUAT
                    var errorMessage = "Failed to load user data."
                    if (currentUser == null) {
                        errorMessage += " (Current user not found)"
                        Log.e("ChatActivity", "Failed to load currentUser. UID was: ${auth.currentUser?.uid}")
                    }
                    if (otherUser == null) {
                        errorMessage += " (Other user not found for ID: $otherUserId)"
                        Log.e("ChatActivity", "Failed to load otherUser. otherUserId was: $otherUserId")
                    }
                    Toast.makeText(this@ChatActivity, errorMessage, Toast.LENGTH_LONG).show()
                    finish() // Menutup ChatActivity karena data penting tidak ada
                    return@withContext
                }
                // Jika kedua user berhasil dimuat:
                Log.d("ChatActivity", "Both users loaded successfully. Updating toolbar and creating/getting chat.")
                updateToolbarUI()
                createOrGetChat()
            }
        }
    }

    private suspend fun fetchUser(uid: String?): User? {
        if (uid == null || uid.isEmpty()) { // Tambahkan pengecekan isEmpty
            Log.w("ChatActivity", "fetchUser called with null or empty UID.")
            return null
        }
        return try {
            Log.d("ChatActivity", "Fetching user from Firestore: users/$uid")
            val document = db.collection("users").document(uid).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                Log.d("ChatActivity", "User found in Firestore: $user")
                user
            } else {
                Log.w("ChatActivity", "User document users/$uid does not exist in Firestore.")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatActivity", "Error fetching user users/$uid from Firestore", e)
            null
        }
    }

    private fun updateToolbarUI() {
        binding.chatToolbar.title = otherUser?.username ?: "Unknown User"
        val photoUrl = otherUser?.photoUrl ?: ""
        Glide.with(this).load(photoUrl).placeholder(R.drawable.ic_user_profile).into(binding.ivOtherUserPhoto)
    }

    private fun createOrGetChat() {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = chatRepository.createOrGetChat(otherUserId)
            withContext(Dispatchers.Main) {
                result.fold(
                    onSuccess = { chat ->
                        chatId = chat.id
                        setupMessageListener()
                    },
                    onFailure = { Log.e("ChatActivity", "Error creating chat", it) }
                )
            }
        }
    }

    private fun setupMessageInput() {
        binding.btnSendMessage.setOnClickListener {
            val messageText = binding.etMessageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
                binding.etMessageInput.text.clear()
            }
        }
    }

    private fun sendMessage(messageText: String) {
        val fromUser = this.currentUser
        val toUser = this.otherUser

        if (chatId.isEmpty() || fromUser == null || toUser == null) {
            Toast.makeText(this, "Cannot send message. Data not ready.", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            chatRepository.sendMessage(chatId, messageText, fromUser, toUser)
        }
    }

    private fun setupToolbar() {
        binding.chatToolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun setupRecyclerView() {
        chatMessageAdapter = ChatMessageAdapter(emptyList(), auth.currentUser?.uid ?: "")
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = chatMessageAdapter
        }
    }

    private fun setupMessageListener() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatRepository.listenToChatMessages(chatId).collectLatest { messages ->
                    chatMessageAdapter.updateMessages(messages)
                    scrollToBottom()
                }
            }
        }
    }

    private fun scrollToBottom() {
        binding.rvMessages.post {
            if (chatMessageAdapter.itemCount > 0) {
                binding.rvMessages.smoothScrollToPosition(chatMessageAdapter.itemCount - 1)
            }
        }
    }
}