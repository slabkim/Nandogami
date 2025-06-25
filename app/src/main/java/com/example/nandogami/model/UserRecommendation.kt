package com.example.nandogami.model

data class UserRecommendation(
    val id: String = "",
    val fromUserId: String = "", // User yang merekomendasikan
    val toUserId: String = "",   // User yang direkomendasikan
    val titleId: String = "",    // Manga yang direkomendasikan
    val message: String = "",    // Pesan rekomendasi
    val recommendationDate: Long = 0,
    val isRead: Boolean = false, // Apakah sudah dibaca oleh penerima
    val readDate: Long = 0
) 