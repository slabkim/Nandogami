package com.example.nandogami.model

data class Follow(
    val id: String = "",
    val followerId: String = "", // User yang follow
    val followingId: String = "", // User yang di-follow
    val followDate: Long = 0,
    val isActive: Boolean = true
) 