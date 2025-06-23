package com.example.nandogami.model

data class Comment(
    val id: String = "",
    val titleId: String = "", // Tambahkan ini untuk link ke Title
    val userId: String = "",    // Tambahkan ini untuk link ke User
    val userName: String = "",
    val userAvatar: String = "",
    val commentText: String = "",
    val gifUrl: String = "", // URL GIF jika ada
    val timestamp: Long = 0,
    val likeCount: Int = 0,
    val replies: List<Comment> = emptyList(),
    val parentId: String = "" // Untuk reply bertingkat
)