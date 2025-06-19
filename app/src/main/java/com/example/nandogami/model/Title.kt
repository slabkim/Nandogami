package com.example.nandogami.model

data class Title(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val imageUrl: String = "",
    val type: String = "",
    val categories: List<String> = emptyList(),
    val rating: Double = 0.0,
    val isFeatured: Boolean = false,
    val isNewRelease: Boolean = false,
    val isPopular: Boolean = false
)
