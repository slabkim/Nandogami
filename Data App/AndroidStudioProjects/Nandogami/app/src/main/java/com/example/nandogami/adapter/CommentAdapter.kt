package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.EditText
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ItemCommentBinding
import com.example.nandogami.R
import com.example.nandogami.databinding.LayoutReplyInputBinding
import com.example.nandogami.model.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val comments: List<Comment>,
    private val onLikeClick: (Comment) -> Unit,
    private val onReplyClick: (Comment) -> Unit,
    private val replyingToCommentId: String?,
    private val onSendReply: (parentComment: Comment, replyText: String) -> Unit,
    private val isLikedSet: Set<String> = emptySet(),
    private val onUserClick: (userId: String) -> Unit
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
        
        // Tampilkan GIF jika ada
        if (comment.gifUrl.isNotEmpty()) {
            holder.binding.ivCommentGif.visibility = View.VISIBLE
            Glide.with(holder.itemView.context)
                .asGif()
                .load(comment.gifUrl)
                .into(holder.binding.ivCommentGif)
        } else {
            holder.binding.ivCommentGif.visibility = View.GONE
        }
        
        // Tampilkan status like (warna jempol)
        if (isLikedSet.contains(comment.id)) {
            holder.binding.tvLikeCount.setTextColor(holder.itemView.context.getColor(R.color.purple_accent))
        } else {
            holder.binding.tvLikeCount.setTextColor(holder.itemView.context.getColor(R.color.white_secondary))
        }
        
        // Handle like button click
        holder.binding.tvLikeCount.setOnClickListener {
            onLikeClick(comment)
        }
        
        // Handle reply button click
        if (comment.parentId.isEmpty()) {
            holder.binding.tvReplyButton.visibility = View.VISIBLE
            holder.binding.tvReplyButton.setOnClickListener {
                onReplyClick(comment)
            }
        } else {
            holder.binding.tvReplyButton.visibility = View.GONE
            holder.binding.tvReplyButton.setOnClickListener(null)
        }

        // Tampilkan input reply inline jika komentar ini sedang di-reply
        val container = holder.binding.root as ViewGroup
        // Hapus view reply input jika ada
        for (i in container.childCount - 1 downTo 0) {
            val v = container.getChildAt(i)
            if (v.tag == "reply_input") container.removeViewAt(i)
        }
        if (comment.id == replyingToCommentId) {
            val replyInput = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.layout_reply_input, container, false)
            replyInput.tag = "reply_input"
            val etReply = replyInput.findViewById<EditText>(R.id.etReplyInput)
            val btnSend = replyInput.findViewById<Button>(R.id.btnSendReply)
            btnSend.setOnClickListener {
                val text = etReply.text.toString().trim()
                if (text.isNotEmpty()) {
                    onSendReply(comment, text)
                }
            }
            container.addView(replyInput)
        }

        // Indentasi dan label reply
        val params = holder.binding.root.layoutParams as ViewGroup.MarginLayoutParams
        if (comment.parentId.isNotEmpty()) {
            // Ini reply
            params.marginStart = (32 * holder.itemView.context.resources.displayMetrics.density).toInt() // 32dp
            holder.binding.tvReplyLabel.visibility = View.VISIBLE
            holder.binding.tvReplyLabel.text = if (!comment.userNameParent.isNullOrEmpty()) {
                "Replying to: ${comment.userNameParent}"
            } else {
                "Reply"
            }
        } else {
            params.marginStart = 0
            holder.binding.tvReplyLabel.visibility = View.GONE
        }
        holder.binding.root.layoutParams = params

        holder.binding.tvUserName.setOnClickListener {
            if (comment.userId.isNotEmpty()) {
                onUserClick(comment.userId)
            } else {
                android.widget.Toast.makeText(holder.itemView.context, "User tidak ditemukan", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        holder.binding.ivUserAvatar.setOnClickListener {
            if (comment.userId.isNotEmpty()) {
                onUserClick(comment.userId)
            } else {
                android.widget.Toast.makeText(holder.itemView.context, "User tidak ditemukan", android.widget.Toast.LENGTH_SHORT).show()
            }
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