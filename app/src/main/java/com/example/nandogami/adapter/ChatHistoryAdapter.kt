package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.R
import com.example.nandogami.databinding.ItemChatHistoryBinding
import com.example.nandogami.model.ChatHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatHistoryAdapter(private val onItemClicked: (ChatHistory) -> Unit) :
    ListAdapter<ChatHistory, ChatHistoryAdapter.ChatHistoryViewHolder>(ChatHistoryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatHistoryViewHolder {
        val binding =
            ItemChatHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatHistoryViewHolder(binding, onItemClicked)
    }

    override fun onBindViewHolder(holder: ChatHistoryViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(currentItem)
    }

    class ChatHistoryViewHolder(
        private val binding: ItemChatHistoryBinding,
        private val onItemClicked: (ChatHistory) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentChatHistory: ChatHistory? = null

        init {
            binding.root.setOnClickListener {
                currentChatHistory?.let {
                    onItemClicked(it)
                }
            }
        }

        fun bind(chatHistoryItem: ChatHistory) {
            currentChatHistory = chatHistoryItem
            binding.tvUserName.text = chatHistoryItem.otherUserName
            binding.tvLastMessage.text = chatHistoryItem.lastMessage
            Glide.with(itemView.context)
                .load(chatHistoryItem.otherUserProfileUrl)
                .placeholder(R.drawable.ic_user_profile)
                .into(binding.ivProfile)

            chatHistoryItem.timestamp?.let {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                binding.tvTimestamp.text = sdf.format(Date(it))
            }
        }
    }

    class ChatHistoryDiffCallback : DiffUtil.ItemCallback<ChatHistory>() {
        override fun areItemsTheSame(oldItem: ChatHistory, newItem: ChatHistory): Boolean {
            return oldItem.otherUserId == newItem.otherUserId
        }

        override fun areContentsTheSame(oldItem: ChatHistory, newItem: ChatHistory): Boolean {
            return oldItem == newItem
        }
    }
}