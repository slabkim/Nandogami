package com.example.nandogami.model

data class ReadingStatus(
    val id: String = "",
    val userId: String = "",
    val titleId: String = "",
    val status: ReadingStatusType = ReadingStatusType.PLAN_TO_READ,
    val currentChapter: Int = 0,
    val totalChapters: Int = 0,
    val startDate: Long = 0,
    val finishDate: Long = 0,
    val lastReadDate: Long = 0,
    val rating: Float = 0f,
    val review: String = "",
    val isPrivate: Boolean = false
)

enum class ReadingStatusType {
    PLAN_TO_READ,    // Ingin dibaca
    READING,         // Sedang dibaca
    COMPLETED,       // Sudah selesai
    DROPPED,         // Ditinggalkan
    ON_HOLD         // Ditunda
} 