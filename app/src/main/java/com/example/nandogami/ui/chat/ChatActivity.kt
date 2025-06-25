package com.example.nandogami.ui.chat

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.databinding.ActivityChatBinding
import com.example.nandogami.model.Chat
import com.example.nandogami.model.ChatMessage
import com.example.nandogami.model.MessageType
import com.example.nandogami.data.ChatRealtimeRepository
import com.example.nandogami.adapter.ChatMessageAdapter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle

class ChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatBinding
    private val chatRepository = ChatRealtimeRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    
    private var chatId: String = ""
    private var otherUserId: String = ""
    private var otherUserName: String = ""
    private var otherUserPhoto: String = ""
    private var chat: Chat? = null
    private lateinit var chatMessageAdapter: ChatMessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        otherUserId = intent.getStringExtra("otherUserId") ?: ""
        if (otherUserId.isEmpty()) {
            Toast.makeText(this, "User ID is missing", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        loadOtherUserInfo()
        setupMessageInput()
        createOrGetChat()
    }

    private fun setupToolbar() {
        binding.chatToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        chatMessageAdapter = ChatMessageAdapter(emptyList(), auth.currentUser?.uid ?: "")
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true // Messages start from bottom
            }
            adapter = chatMessageAdapter
        }
    }

    private fun loadOtherUserInfo() {
        db.collection("users").document(otherUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    otherUserName = document.getString("username") ?: "Unknown User"
                    otherUserPhoto = document.getString("photoUrl") ?: ""
                    
                    binding.chatToolbar.title = otherUserName
                    
                    if (otherUserPhoto.isNotEmpty()) {
                        Glide.with(this)
                            .load(otherUserPhoto)
                            .placeholder(R.drawable.ic_user_profile)
                            .into(binding.ivOtherUserPhoto)
                    } else {
                        binding.ivOtherUserPhoto.setImageResource(R.drawable.ic_user_profile)
                    }
                }
            }
    }

    private fun setupMessageInput() {
        binding.btnSendMessage.setOnClickListener {
            val messageText = binding.etMessageInput.text.toString().trim()
            if (messageText.isNotEmpty() && chatId.isNotEmpty()) {
                sendMessage(messageText)
                binding.etMessageInput.text.clear()
            }
        }
    }

    private fun createOrGetChat() {
        // Check authentication first
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("ChatActivity", "User not authenticated")
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Check Google Play Services
        try {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                Log.w("ChatActivity", "Google Play Services not available: $resultCode")
                // Continue anyway, Firebase might still work
            }
        } catch (e: Exception) {
            Log.w("ChatActivity", "Error checking Google Play Services", e)
            // Continue anyway
        }
        
        Log.d("ChatActivity", "Current user: ${currentUser.uid}")
        Log.d("ChatActivity", "Other user: $otherUserId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = chatRepository.createOrGetChat(otherUserId)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { chat ->
                            this@ChatActivity.chat = chat
                            chatId = chat.id
                            Log.d("ChatActivity", "Chat created/retrieved: $chatId")
                            setupMessageListener()
                        },
                        onFailure = { exception ->
                            Log.e("ChatActivity", "Error creating chat", exception)
                            Toast.makeText(this@ChatActivity, "Error: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error in createOrGetChat", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupMessageListener() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                chatRepository.listenToChatMessages(chatId).collectLatest { messages ->
                    Log.d("ChatActivity", "Received ${messages.size} messages")
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

    private fun sendMessage(messageText: String) {
        if (chatId.isEmpty()) {
            Log.e("ChatActivity", "Cannot send message: chatId is empty")
            Toast.makeText(this, "Chat not ready", Toast.LENGTH_SHORT).show()
            return
        }
        
        Log.d("ChatActivity", "Sending message: $messageText to chat: $chatId")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = chatRepository.sendMessage(chatId, messageText, MessageType.TEXT)
                withContext(Dispatchers.Main) {
                    result.fold(
                        onSuccess = { message ->
                            // Message sent successfully
                            Log.d("ChatActivity", "Message sent successfully: ${message.message}")
                        },
                        onFailure = { exception ->
                            Log.e("ChatActivity", "Error sending message", exception)
                            Toast.makeText(this@ChatActivity, "Error sending message: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Exception in sendMessage", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
} 