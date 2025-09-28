package com.example.nandogami.model

data class Chat(
    var id: String = "",
    val participants: List<String> = emptyList(), // List user IDs
    val lastMessage: String = "",
    val lastMessageTime: Long = 0,
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap(), // userId -> unread count
    val createdAt: Long = 0
)

data class ChatMessage(
    var id: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val message: String = "",
    val messageType: MessageType = MessageType.TEXT,
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val readBy: List<String> = emptyList() // List user IDs yang sudah baca
)
