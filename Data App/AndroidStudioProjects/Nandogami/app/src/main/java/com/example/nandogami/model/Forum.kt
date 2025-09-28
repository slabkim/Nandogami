package com.example.nandogami.model

data class Forum(
    var id: String = "",
    val title: String = "",
    val description: String = "",
    val topic: String = "", // e.g., "Rekomendasi Manga Romance"
    val creatorId: String = "",
    val creatorName: String = "",
    val creatorPhotoUrl: String = "",
    val members: List<String> = emptyList(), // List user IDs
    val memberCount: Int = 0,
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageSenderId: String = "",
    val lastMessageSenderName: String = "",
    val createdAt: Long = 0,
    val isPublic: Boolean = true,
    val tags: List<String> = emptyList() // e.g., ["romance", "shoujo", "recommendation"]
)

data class ForumMessage(
    var id: String = "",
    val forumId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderPhotoUrl: String = "",
    val message: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList(), // List user IDs yang sudah baca
    val likes: List<String> = emptyList(), // List user IDs yang like
    val replyTo: String? = null // ID message yang di-reply, null jika bukan reply
)

