package com.example.nandogami.model

data class Title(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val imageUrl: String = "",
    val type: String = "",
    val rating: Float = 0f,
    val isFeatured: Boolean = false,
    val isPopular: Boolean = false,
    val isNewRelease: Boolean = false,
    val categories: List<String> = emptyList()
)