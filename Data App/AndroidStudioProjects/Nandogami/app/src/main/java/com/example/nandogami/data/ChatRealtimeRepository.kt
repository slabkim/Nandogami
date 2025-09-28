package com.example.nandogami.data

import android.util.Log
import com.example.nandogami.model.Chat
import com.example.nandogami.model.ChatMessage
import com.example.nandogami.model.MessageType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRealtimeRepository {
    private val database = FirebaseDatabase.getInstance("https://nandogami-45016-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val auth = FirebaseAuth.getInstance()

    suspend fun createOrGetChat(otherUserId: String): Result<Chat> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            Log.d("ChatRealtimeRepository", "Creating chat between $currentUserId and $otherUserId")
            
            if (currentUserId == otherUserId) {
                return Result.failure(Exception("Cannot chat with yourself"))
            }
            
            // Create chat ID by sorting user IDs to ensure consistency
            val chatId = if (currentUserId < otherUserId) {
                "${currentUserId}_${otherUserId}"
            } else {
                "${otherUserId}_${currentUserId}"
            }
            
            Log.d("ChatRealtimeRepository", "Chat ID: $chatId")
            
            // Check if chat exists
            val chatRef = database.getReference("chats").child(chatId)
            val chatSnapshot = chatRef.get().await()
            
            if (chatSnapshot.exists()) {
                val chat = chatSnapshot.getValue(Chat::class.java)
                chat?.id = chatId
                Log.d("ChatRealtimeRepository", "Existing chat found")
                return Result.success(chat ?: Chat())
            }
            
            // Create new chat
            val chat = Chat(
                id = chatId,
                participants = listOf(currentUserId, otherUserId),
                lastMessage = "",
                lastMessageTime = 0,
                lastMessageSenderId = "",
                unreadCount = mapOf(otherUserId to 0),
                createdAt = System.currentTimeMillis()
            )
            
            Log.d("ChatRealtimeRepository", "Creating new chat")
            chatRef.setValue(chat).await()
            Log.d("ChatRealtimeRepository", "Chat created successfully")
            Result.success(chat)
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error creating chat", e)
            Result.failure(e)
        }
    }

    suspend fun sendMessage(chatId: String, message: String, messageType: MessageType = MessageType.TEXT): Result<ChatMessage> {
        return try {
            val senderId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val messageId = database.getReference("chat_messages").child(chatId).push().key
                ?: return Result.failure(Exception("Failed to generate message ID"))
            
            val chatMessage = ChatMessage(
                id = messageId,
                chatId = chatId,
                senderId = senderId,
                message = message,
                messageType = messageType,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                readBy = listOf(senderId)
            )
            
            // Save message
            database.getReference("chat_messages").child(chatId).child(messageId).setValue(chatMessage).await()
            
            // Update chat's last message
            updateChatLastMessage(chatId, message, senderId)
            
            Result.success(chatMessage)
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error sending message", e)
            Result.failure(e)
        }
    }

    private suspend fun updateChatLastMessage(chatId: String, message: String, senderId: String) {
        try {
            val updates = mapOf(
                "lastMessage" to message,
                "lastMessageTime" to System.currentTimeMillis(),
                "lastMessageSenderId" to senderId
            )
            database.getReference("chats").child(chatId).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error updating chat last message", e)
        }
    }

    suspend fun getChatMessages(chatId: String): List<ChatMessage> {
        return try {
            val snapshot = database.getReference("chat_messages").child(chatId).get().await()
            val messages = mutableListOf<ChatMessage>()
            
            for (childSnapshot in snapshot.children) {
                val message = childSnapshot.getValue(ChatMessage::class.java)
                message?.let {
                    it.id = childSnapshot.key ?: ""
                    messages.add(it)
                }
            }
            
            messages.sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error getting chat messages", e)
            emptyList()
        }
    }

    suspend fun getUserChats(userId: String): List<Chat> {
        return try {
            val snapshot = database.getReference("chats").get().await()
            val chats = mutableListOf<Chat>()
            
            for (childSnapshot in snapshot.children) {
                val chat = childSnapshot.getValue(Chat::class.java)
                chat?.let {
                    if (it.participants.contains(userId)) {
                        it.id = childSnapshot.key ?: ""
                        chats.add(it)
                    }
                }
            }
            
            chats.sortedByDescending { it.lastMessageTime }
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error getting user chats", e)
            emptyList()
        }
    }

    suspend fun markMessagesAsRead(chatId: String): Result<Unit> {
        return try {
            val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in"))
            
            val messagesRef = database.getReference("chat_messages").child(chatId)
            val snapshot = messagesRef.get().await()
            
            val updates = mutableMapOf<String, Any>()
            for (childSnapshot in snapshot.children) {
                val message = childSnapshot.getValue(ChatMessage::class.java)
                if (message != null && message.senderId != currentUserId && !message.isRead) {
                    val readBy = message.readBy.toMutableList()
                    if (!readBy.contains(currentUserId)) {
                        readBy.add(currentUserId)
                        updates["${childSnapshot.key}/readBy"] = readBy
                        updates["${childSnapshot.key}/isRead"] = true
                    }
                }
            }
            
            if (updates.isNotEmpty()) {
                messagesRef.updateChildren(updates).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error marking messages as read", e)
            Result.failure(e)
        }
    }

    // Real-time listener for chat messages using Flow
    fun listenToChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = database.getReference("chat_messages").child(chatId)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<ChatMessage>()
                
                for (childSnapshot in snapshot.children) {
                    val message = childSnapshot.getValue(ChatMessage::class.java)
                    message?.let {
                        it.id = childSnapshot.key ?: ""
                        messages.add(it)
                    }
                }
                
                val sortedMessages = messages.sortedBy { it.timestamp }
                trySend(sortedMessages)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRealtimeRepository", "Error listening to messages", error.toException())
                close(error.toException())
            }
        }
        
        messagesRef.addValueEventListener(listener)
        
        awaitClose {
            messagesRef.removeEventListener(listener)
        }
    }

    // Real-time listener for user chats
    fun listenToUserChats(userId: String): Flow<List<Chat>> = callbackFlow {
        val chatsRef = database.getReference("chats")
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = mutableListOf<Chat>()
                
                for (childSnapshot in snapshot.children) {
                    val chat = childSnapshot.getValue(Chat::class.java)
                    chat?.let {
                        if (it.participants.contains(userId)) {
                            it.id = childSnapshot.key ?: ""
                            chats.add(it)
                        }
                    }
                }
                
                val sortedChats = chats.sortedByDescending { it.lastMessageTime }
                trySend(sortedChats)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRealtimeRepository", "Error listening to chats", error.toException())
                close(error.toException())
            }
        }
        
        chatsRef.addValueEventListener(listener)
        
        awaitClose {
            chatsRef.removeEventListener(listener)
        }
    }

    suspend fun getUnreadChatsCount(userId: String): Int {
        return try {
            val chats = getUserChats(userId)
            var totalUnread = 0
            
            for (chat in chats) {
                val messages = getChatMessages(chat.id)
                totalUnread += messages.count { it.senderId != userId && !it.isRead }
            }
            
            totalUnread
        } catch (e: Exception) {
            Log.e("ChatRealtimeRepository", "Error getting unread count", e)
            0
        }
    }
} 