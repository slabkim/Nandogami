package com.example.nandogami.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.nandogami.databinding.ItemChatListBinding
import com.example.nandogami.model.User

class UserSearchAdapter(
    private var users: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<UserSearchAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemChatListBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.binding.tvUserName.text = user.username
        holder.binding.tvLastMessage.text = "@${user.handle}" // tampilkan handle di bawah username
        holder.binding.tvLastMessageTime.text = ""
        Glide.with(holder.binding.ivUserPhoto.context)
            .load(user.photoUrl)
            .placeholder(com.example.nandogami.R.drawable.ic_user_profile)
            .into(holder.binding.ivUserPhoto)
        holder.itemView.setOnClickListener { onUserClick(user) }
    }

    override fun getItemCount() = users.size

    fun updateData(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
} 