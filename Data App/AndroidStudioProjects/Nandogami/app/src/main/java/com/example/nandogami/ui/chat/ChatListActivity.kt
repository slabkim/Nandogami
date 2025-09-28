package com.example.nandogami.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nandogami.adapter.ChatListAdapter
import com.example.nandogami.data.ChatRealtimeRepository
import com.example.nandogami.databinding.ActivityChatListBinding
import com.example.nandogami.model.Chat
import com.example.nandogami.ui.chat.ChatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.nandogami.R

class ChatListActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatListBinding
    private lateinit var chatListAdapter: ChatListAdapter
    private val chatRepository = ChatRealtimeRepository()
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            android.widget.Toast.makeText(this, "Anda belum login!", android.widget.Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        chatListAdapter = ChatListAdapter(emptyList(), currentUserId) { chat, otherUserId ->
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("otherUserId", otherUserId)
            startActivity(intent)
        }
        binding.rvChatList.layoutManager = LinearLayoutManager(this)
        binding.rvChatList.adapter = chatListAdapter

        binding.chatListToolbar.title = getString(R.string.chat_list_title)
        binding.chatListToolbar.setNavigationOnClickListener { finish() }

        // Listener real-time untuk daftar chat
        CoroutineScope(Dispatchers.IO).launch {
            try {
                chatRepository.listenToUserChats(currentUserId).collect { chats ->
                    withContext(Dispatchers.Main) {
                        chatListAdapter.updateChats(chats)
                        // Ambil nama dan foto profil lawan chat
                        chats.forEachIndexed { index, chat ->
                            val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: return@forEachIndexed
                            db.collection("users").document(otherUserId).get()
                                .addOnSuccessListener { document ->
                                    val userName = document.getString("username") ?: "User"
                                    val photoUrl = document.getString("photoUrl") ?: ""
                                    val holder = binding.rvChatList.findViewHolderForAdapterPosition(index)
                                    if (holder is com.example.nandogami.adapter.ChatListAdapter.ViewHolder) {
                                        holder.binding.tvUserName.text = userName
                                        if (photoUrl.isNotEmpty()) {
                                            com.bumptech.glide.Glide.with(this@ChatListActivity)
                                                .load(photoUrl)
                                                .placeholder(com.example.nandogami.R.drawable.ic_user_profile)
                                                .into(holder.binding.ivUserPhoto)
                                        } else {
                                            holder.binding.ivUserPhoto.setImageResource(com.example.nandogami.R.drawable.ic_user_profile)
                                        }
                                    }
                                }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(this@ChatListActivity, "Gagal memuat chat: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }
} 