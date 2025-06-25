package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.nandogami.databinding.ItemChatMessageBinding
import com.example.nandogami.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth

class ChatMessageAdapter(
    private var messages: List<ChatMessage>,
    private val currentUserId: String
) : RecyclerView.Adapter<ChatMessageAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatMessageBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message = messages[position]
        val isMyMessage = message.senderId == currentUserId

        if (isMyMessage) {
            // My message - align to right
            holder.binding.layoutMessage.visibility = android.view.View.GONE
            holder.binding.layoutMyMessage.visibility = android.view.View.VISIBLE
            holder.binding.tvMyMessage.text = message.message
            holder.binding.tvMyMessageTime.text = formatTime(message.timestamp)
        } else {
            // Other's message - align to left
            holder.binding.layoutMessage.visibility = android.view.View.VISIBLE
            holder.binding.layoutMyMessage.visibility = android.view.View.GONE
            holder.binding.tvMessage.text = message.message
            holder.binding.tvMessageTime.text = formatTime(message.timestamp)
        }
    }

    override fun getItemCount() = messages.size

    fun updateMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    fun addMessage(message: ChatMessage) {
        val newList = messages.toMutableList()
        newList.add(message)
        messages = newList
        notifyItemInserted(messages.size - 1)
    }

    private fun formatTime(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
} 