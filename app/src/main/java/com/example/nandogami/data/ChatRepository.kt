package com.example.nandogami.data

import com.example.nandogami.model.Chat
import com.example.nandogami.model.ChatMessage
import com.example.nandogami.model.MessageType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import android.util.Log

class ChatRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun createOrGetChat(otherUserId: String): Result<Chat> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            if (currentUserId == otherUserId) {
                return Result.failure(Exception("Cannot chat with yourself"))
            }
            
            // Check if chat already exists
            val existingChat = db.collection("chats")
                .whereArrayContains("participants", currentUserId)
                .get()
                .await()
                .documents
                .find { doc ->
                    val participants = doc.get("participants") as? List<String>
                    participants?.contains(otherUserId) == true
                }
            
            if (existingChat != null) {
                val chat = existingChat.toObject(Chat::class.java)
                chat?.id = existingChat.id
                return Result.success(chat ?: Chat())
            }
            
            // Create new chat
            val chat = Chat(
                id = db.collection("chats").document().id,
                participants = listOf(currentUserId, otherUserId),
                lastMessage = "",
                lastMessageTime = 0,
                lastMessageSenderId = "",
                unreadCount = mapOf(otherUserId to 0),
                createdAt = System.currentTimeMillis()
            )
            
            db.collection("chats").document(chat.id).set(chat).await()
            Result.success(chat)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, message: String, messageType: MessageType = MessageType.TEXT): Result<ChatMessage> {
        return try {
            val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val chatMessage = ChatMessage(
                id = db.collection("chat_messages").document().id,
                chatId = chatId,
                senderId = senderId,
                message = message,
                messageType = messageType,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                readBy = listOf(senderId)
            )
            
            db.collection("chat_messages").document(chatMessage.id).set(chatMessage).await()
            
            // Update chat's last message
            updateChatLastMessage(chatId, message, senderId)
            
            Result.success(chatMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateChatLastMessage(chatId: String, message: String, senderId: String) {
        try {
            db.collection("chats").document(chatId).update(
                mapOf(
                    "lastMessage" to message,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "lastMessageSenderId" to senderId
                )
            ).await()
        } catch (e: Exception) {
            // Handle error silently
        }
    }

    suspend fun getChatMessages(chatId: String): List<ChatMessage> {
        return try {
            val snapshot = db.collection("chat_messages")
                .whereEqualTo("chatId", chatId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                val message = doc.toObject(ChatMessage::class.java)
                message?.let {
                    it.id = doc.id
                    it
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chat messages", e)
            emptyList()
        }
    }

    suspend fun getUserChats(userId: String): List<Chat> {
        return try {
            val snapshot = db.collection("chats")
                .whereArrayContains("participants", userId)
                .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.toObjects(Chat::class.java).map { chat ->
                chat.id = snapshot.documents.find { it.id == chat.id }?.id ?: ""
                chat
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val unreadMessages = db.collection("chat_messages")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("isRead", false)
                .whereNotIn("senderId", listOf(currentUserId))
                .get()
                .await()
            
            val batch = db.batch()
            unreadMessages.documents.forEach { doc ->
                val readBy = doc.get("readBy") as? List<String> ?: emptyList()
                if (!readBy.contains(currentUserId)) {
                    val updatedReadBy = readBy + currentUserId
                    batch.update(doc.reference, "readBy", updatedReadBy)
                    batch.update(doc.reference, "isRead", true)
                }
            }
            
            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUnreadChatsCount(userId: String): Int {
        return try {
            val chats = getUserChats(userId)
            var totalUnread = 0
            
            for (chat in chats) {
                val unreadMessages = db.collection("chat_messages")
                    .whereEqualTo("chatId", chat.id)
                    .whereEqualTo("isRead", false)
                    .whereNotIn("senderId", listOf(userId))
                    .get()
                    .await()
                
                totalUnread += unreadMessages.size()
            }
            
            totalUnread
        } catch (e: Exception) {
            0
        }
    }

    // Real-time listener for chat messages
    fun listenToChatMessages(chatId: String, onMessageReceived: (ChatMessage) -> Unit): ListenerRegistration {
        return db.collection("chat_messages")
            .whereEqualTo("chatId", chatId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ChatRepository", "Error listening to messages", e)
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    val message = change.document.toObject(ChatMessage::class.java)
                    message?.let { 
                        it.id = change.document.id
                        when (change.type) {
                            com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                                onMessageReceived(it)
                            }
                            com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                                // Handle message updates if needed
                                Log.d("ChatRepository", "Message modified: ${it.message}")
                            }
                            com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                                // Handle message deletion if needed
                                Log.d("ChatRepository", "Message removed: ${it.message}")
                            }
                        }
                    }
                }
            }
    }

    // Real-time listener for user chats
    fun listenToUserChats(userId: String, onChatUpdated: (Chat) -> Unit): ListenerRegistration {
        return db.collection("chats")
            .whereArrayContains("participants", userId)
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                
                snapshot?.documentChanges?.forEach { change ->
                    val chat = change.document.toObject(Chat::class.java)
                    chat?.let {
                        it.id = change.document.id
                        onChatUpdated(it)
                    }
                }
            }
    }
} 