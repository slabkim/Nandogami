package com.example.nandogami.data

import android.util.Log
import com.example.nandogami.model.Chat
import com.example.nandogami.model.ChatHistory
import com.example.nandogami.model.ChatMessage
import com.example.nandogami.model.User // Pastikan User memiliki field uid, username, photoUrl
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue // Untuk Realtime Database timestamp
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FieldValue // Untuk Firestore timestamp
import com.google.firebase.firestore.FirebaseFirestore
// import com.google.firebase.firestore.Query // Query tidak digunakan secara langsung di sini sekarang
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.lang.Exception

class ChatRealtimeRepository {
    private val dbRealtime = FirebaseDatabase.getInstance("https://nandogami-45016-default-rtdb.asia-southeast1.firebasedatabase.app/")
    private val dbFirestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ChatRepository" // Tag Log konsisten
    }

    suspend fun createOrGetChat(otherUserUid: String): Result<Chat> {
        val currentUserId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in to create or get chat."))
        if (currentUserId == otherUserUid) return Result.failure(Exception("Cannot create chat with yourself."))

        // Buat ID chat yang konsisten dengan mengurutkan UID
        val chatId = if (currentUserId < otherUserUid) "${currentUserId}_${otherUserUid}" else "${otherUserUid}_${currentUserId}"
        val chatRef = dbRealtime.getReference("chats").child(chatId)

        return try {
            val chatSnapshot = chatRef.get().await()
            if (chatSnapshot.exists()) {
                val chat = chatSnapshot.getValue(Chat::class.java)
                chat?.id = chatId // Pastikan ID disetel jika diambil dari DB
                Result.success(chat ?: Chat(id = chatId, participants = listOf(currentUserId, otherUserUid))) // Fallback jika deserialisasi null
            } else {
                val newChat = Chat(
                    id = chatId,
                    participants = listOf(currentUserId, otherUserUid),
                    createdAt = System.currentTimeMillis() // Atau ServerValue.TIMESTAMP jika model Chat di RTDB
                    // dan Anda ingin RTDB yang mengaturnya
                )
                chatRef.setValue(newChat).await()
                Result.success(newChat)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating or getting chat for chatId: $chatId", e)
            Result.failure(Exception("Failed to create or get chat: ${e.message}", e))
        }
    }

    suspend fun sendMessage(chatId: String, messageText: String, fromUser: User, toUser: User): Result<ChatMessage> {
        val senderAuthId = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in to send message."))
        // Pastikan fromUser.uid adalah senderAuthId
        if (fromUser.uid != senderAuthId) {
            return Result.failure(Exception("Sender ID mismatch. Message not sent."))
        }

        val messagesRef = dbRealtime.getReference("chat_messages").child(chatId)
        val messageId = messagesRef.push().key ?: return Result.failure(Exception("Failed to generate message ID."))

        val chatMessage = ChatMessage(
            id = messageId,
            chatId = chatId,
            senderId = senderAuthId,
            message = messageText,
            timestamp = System.currentTimeMillis() // Akan diisi ServerValue.TIMESTAMP oleh RTDB
        )

        // Buat objek untuk RTDB yang menyertakan ServerValue.TIMESTAMP
        val messageMap = mapOf(
            "id" to chatMessage.id,
            "chatId" to chatMessage.chatId,
            "senderId" to chatMessage.senderId,
            "message" to chatMessage.message,
            "timestamp" to ServerValue.TIMESTAMP // Gunakan ServerValue.TIMESTAMP
        )

        return try {
            Log.d(TAG, "Attempting to send message. ChatId: $chatId, Message: $messageText, SenderAuthId: $senderAuthId")
            messagesRef.child(messageId).setValue(messageMap).await()
            Log.d(TAG, "Message sent to RTDB. Updating last message and history.")

            // Update last message di metadata chat RTDB
            updateChatLastMessageRTDB(chatId, messageText, senderAuthId)

            // Update riwayat chat di Firestore (hanya untuk pengirim jika tidak pakai Cloud Function)
            updateChatHistoryForSender(chatId, messageText, fromUser, toUser)

            Result.success(chatMessage.copy(timestamp = System.currentTimeMillis())) // Timestamp lokal untuk kembalian, RTDB punya server time
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message to chatId: $chatId", e)
            Result.failure(Exception("Failed to send message: ${e.message}", e))
        }
    }

    // Mengganti nama agar lebih jelas ini untuk RTDB
    private suspend fun updateChatLastMessageRTDB(chatId: String, message: String, senderId: String) {
        try {
            val updates = mapOf(
                "lastMessage" to message,
                "lastMessageTimestamp" to ServerValue.TIMESTAMP, // Gunakan ServerValue.TIMESTAMP
                "lastMessageSenderId" to senderId
            )
            dbRealtime.getReference("chats").child(chatId).updateChildren(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating last message in RTDB for chatId: $chatId", e)
        }
    }

    // Fungsi ini diganti namanya dan hanya untuk pengirim jika tidak ada Cloud Function
    private suspend fun updateChatHistoryForSender(chatIdFromRTDB: String, lastMessage: String, sender: User, receiver: User) {
        val currentUserUid = auth.currentUser?.uid ?: run {
            Log.e(TAG, "Cannot update chat history: User not logged in.")
            return
        }
        // Pastikan sender.uid adalah currentUserUid
        if (sender.uid != currentUserUid) {
            Log.e(TAG, "Sender UID mismatch, cannot update chat history for sender.")
            return
        }

        // Data untuk riwayat chat pengirim
        val historyDataForSender = mapOf(
            "otherUserId" to receiver.uid, // Gunakan UID
            "otherUserName" to receiver.username,
            "otherUserPhotoUrl" to receiver.photoUrl,
            "lastMessage" to lastMessage,
            "timestamp" to FieldValue.serverTimestamp(), // Gunakan Firestore Server Timestamp
            "unreadCount" to 0 // Pengirim tidak memiliki pesan belum dibaca dari dirinya sendiri di chat ini
            // "actualChatId" to chatIdFromRTDB // Jika Anda perlu menyimpan ID chat dari RTDB
        )

        try {
            // Path: /chat_history/{senderUid}/chats/{receiverUid}
            dbFirestore.collection("chat_history").document(sender.uid)
                .collection("chats").document(receiver.uid)
                .set(historyDataForSender).await()
            Log.d(TAG, "Chat history updated for sender: ${sender.uid} with receiver: ${receiver.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update chat history for sender ${sender.uid}: ${e.message}", e)
            // Error ini kemungkinan karena PERMISSION_DENIED jika aturan tidak benar
        }

        // PENTING: Untuk memperbarui riwayat chat penerima (`/chat_history/{receiver.uid}/chats/{sender.uid}`),
        // idealnya gunakan Cloud Function yang terpicu oleh pesan baru di RTDB.
        // Jika Anda mencoba melakukannya dari klien, kemungkinan besar akan gagal karena PERMISSION_DENIED.
        // Contoh jika tetap ingin mencoba dari klien (TIDAK DISARANKAN untuk entri penerima):
        /*
        val historyDataForReceiver = mapOf(
            "otherUserId" to sender.uid,
            "otherUserName" to sender.username,
            "otherUserPhotoUrl" to sender.photoUrl,
            "lastMessage" to lastMessage,
            "timestamp" to FieldValue.serverTimestamp(),
            "unreadCount" to 1 // Atau logika untuk increment
        )
        try {
            dbFirestore.collection("chat_history").document(receiver.uid)
                .collection("chats").document(sender.uid)
                .set(historyDataForReceiver).await()
            Log.d(TAG, "Chat history (attempted) for receiver: ${receiver.uid} with sender: ${sender.uid}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update chat history for receiver ${receiver.uid}: ${e.message}", e)
            // Error ini SANGAT MUNGKIN terjadi (PERMISSION_DENIED)
        }
        */
    }


    // Fungsi `updateChatHistory` lama Anda tampaknya tidak dipanggil dari `sendMessage`.
    // Jika masih diperlukan, pastikan path dan datanya sesuai.
    // Saya akan mengomentarinya untuk saat ini karena `updateChatHistoryForSender` lebih relevan
    // dengan alur `sendMessage`.
    /*
    suspend fun updateChatHistoryFirestore(chatHistoryDocumentId: String, data: Map<String, Any>): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(Exception("User not authenticated."))

        // Asumsi chatHistoryDocumentId adalah UID pengguna yang riwayatnya ingin diupdate.
        // Dan 'data' adalah Map untuk satu entri chat dengan otherUser.
        // Anda perlu menentukan path yang benar. Jika 'data' adalah untuk chat spesifik:
        // val chatDocRef = dbFirestore.collection("chat_history").document(currentUser.uid)
        // .collection("chats").document(otherUserIdFromData).set(data)

        // Contoh jika `chatHistoryDocumentId` adalah UID pengguna, dan `data` adalah keseluruhan
        // dokumen untuk pengguna tersebut (kurang umum untuk satu pesan):
        val docRef = dbFirestore.collection("chat_history").document(chatHistoryDocumentId)

        Log.d(TAG, "Attempting to update Firestore chat history. Path: ${docRef.path}, Data: $data, User UID: ${currentUser.uid}")
        return try {
            docRef.set(data).await() // Atau .update(data)
            Log.d(TAG, "Firestore chat history successfully updated!")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Firestore chat history at ${docRef.path}", e)
            Result.failure(Exception("Failed to update Firestore chat history: ${e.message}", e))
        }
    }
    */

    fun listenToChatMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val messagesRef = dbRealtime.getReference("chat_messages").child(chatId)
        val listener = messagesRef.orderByChild("timestamp") // Urutkan berdasarkan timestamp di RTDB
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d(TAG, "listenToChatMessages - onDataChange for chatId $chatId. Snapshot exists: ${snapshot.exists()}")
                    val messages = snapshot.children.mapNotNull {
                        // Perlu penanganan khusus jika timestamp adalah Long atau Map dari ServerValue
                        val message = it.getValue(ChatMessage::class.java)
                        // Jika timestamp adalah Long setelah dikonversi dari ServerValue.TIMESTAMP
                        message?.apply {
                            // Jika 'timestamp' di RTDB adalah Long, tidak perlu konversi.
                            // Jika ada masalah deserialisasi timestamp, perlu dilihat struktur data di RTDB.
                        }
                    }
                    Log.d(TAG, "listenToChatMessages - Parsed messages: $messages")
                    trySend(messages) // Tidak perlu sorting lagi jika sudah diorderByChild
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "listenToChatMessages - onCancelled for chatId $chatId", error.toException())
                    close(error.toException())
                }
            })
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun getChatHistoryForUser(userUid: String): Result<List<ChatHistory>> {
        val currentUserUid = auth.currentUser?.uid ?: return Result.failure(Exception("User not logged in to get chat history."))
        if (userUid != currentUserUid) {
            // Atau sesuaikan jika admin bisa mengambil riwayat orang lain (dengan aturan keamanan yang sesuai)
            Log.w(TAG, "Attempting to get chat history for a different user: $userUid. Current user: $currentUserUid")
            // return Result.failure(Exception("Cannot fetch chat history for another user."))
        }

        return try {
            Log.d(TAG, "Attempting to fetch chat history for user: $userUid")
            val snapshot = dbFirestore.collection("chat_history").document(userUid)
                .collection("chats")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Aktifkan pengurutan
                .get().await()

            if (snapshot.isEmpty) {
                Log.w(TAG, "No chat history documents found for user $userUid at path chat_history/$userUid/chats")
            } else {
                Log.d(TAG, "Found ${snapshot.size()} chat history documents for user $userUid")
            }

            // Hati-hati dengan deserialisasi jika field tidak cocok atau ada tipe yang salah
            val chatHistories = snapshot.documents.mapNotNull { doc ->
                try {
                    // Contoh jika Anda perlu mengonversi Firestore Timestamp ke Long manual
                    val history = doc.toObject(ChatHistory::class.java)
                    history?.apply {
                        // Jika ChatHistory Anda memiliki field timestamp bertipe Long,
                        // dan di Firestore adalah Timestamp, Anda mungkin perlu konversi manual atau @ServerTimestamp
                        // pada model Anda jika menggunakan annotation.
                        // Misalnya, jika model ChatHistory.timestamp adalah Long:
                        // this.timestamp = (doc.get("timestamp") as? com.google.firebase.Timestamp)?.toDate()?.time ?: 0L
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error deserializing chat history document ${doc.id}", e)
                    null
                }
            }
            Log.d(TAG, "Parsed chat histories: $chatHistories")
            Result.success(chatHistories)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching chat history for $userUid", e)
            Result.failure(Exception("Failed to fetch chat history: ${e.message}", e))
        }
    }
}