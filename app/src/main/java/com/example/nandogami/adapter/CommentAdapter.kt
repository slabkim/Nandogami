package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ItemCommentBinding
import com.example.nandogami.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val comments: List<Comment>
) : RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    class CommentViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        
        holder.binding.tvUserName.text = comment.userName
        holder.binding.tvCommentText.text = comment.commentText
        holder.binding.tvLikeCount.text = "👍 ${comment.likeCount}"
        
        // Format timestamp
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeAgo = getTimeAgo(comment.timestamp)
        holder.binding.tvCommentTime.text = timeAgo
        
        // Load user avatar
        if (comment.userAvatar.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(comment.userAvatar)
                .into(holder.binding.ivUserAvatar)
        }
        
        // Handle reply button click
        holder.binding.tvReplyButton.setOnClickListener {
            // TODO: Implement reply functionality
        }
    }

    override fun getItemCount(): Int = comments.size

    private fun getTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60 * 1000 -> "Just now"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }
} 