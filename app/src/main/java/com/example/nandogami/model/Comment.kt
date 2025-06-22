package com.example.nandogami.model

data class Comment(
    val id: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val commentText: String = "",
    val timestamp: Long = 0,
    val likeCount: Int = 0,
    val replies: List<Comment> = emptyList()
) 