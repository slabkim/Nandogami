package com.example.nandogami.model

import com.google.firebase.firestore.PropertyName

data class ChatHistory(
    @get:PropertyName("otherUserId") @set:PropertyName("otherUserId")
    var otherUserId: String = "",

    @get:PropertyName("otherUserName") @set:PropertyName("otherUserName")
    var otherUserName: String = "",

    @get:PropertyName("otherUserProfileUrl") @set:PropertyName("otherUserProfileUrl")
    var otherUserProfileUrl: String = "",

    @get:PropertyName("lastMessage") @set:PropertyName("lastMessage")
    var lastMessage: String = "",

    @get:PropertyName("timestamp") @set:PropertyName("timestamp")
    var timestamp: Long? = null
) {
    // Konstruktor kosong diperlukan untuk Firestore
    constructor() : this("", "", "", "", 0)
}