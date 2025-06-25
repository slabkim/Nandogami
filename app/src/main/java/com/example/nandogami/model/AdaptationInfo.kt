package com.example.nandogami.model

data class AdaptationInfo(
    val studio: String = "",
    val title: String = "",
    val type: String = "", // Misal "Manga", "Anime", "Movie"
    val year: Int = 0 // Tahun rilis
)