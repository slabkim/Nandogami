package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.databinding.ItemChatListBinding
import com.example.nandogami.model.Chat

class ChatListAdapter(
    private var chats: List<Chat>,
    private val currentUserId: String,
    private val onChatClick: (Chat, String) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemChatListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chat = chats[position]
        val otherUserId = chat.participants.firstOrNull { it != currentUserId } ?: ""
        holder.binding.tvUserName.text = "User"
        holder.binding.tvLastMessage.text = chat.lastMessage
        holder.binding.tvLastMessageTime.text = formatTime(chat.lastMessageTime)
        // Foto profil: nanti diisi di Activity pakai Glide setelah ambil data user
        holder.binding.ivUserPhoto.setImageResource(R.drawable.ic_user_profile)
        holder.itemView.setOnClickListener {
            onChatClick(chat, otherUserId)
        }
    }

    override fun getItemCount() = chats.size

    fun updateChats(newChats: List<Chat>) {
        chats = newChats
        notifyDataSetChanged()
    }

    private fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
} 