package com.example.nandogami.model

import com.google.firebase.database.PropertyName // Pastikan untuk mengimpor ini

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

    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val readBy: List<String> = emptyList() // List user IDs yang sudah baca
)

enum class MessageType {
    TEXT,
    IMAGE,
    GIF,
    MANGA_LINK // Link ke manga
}