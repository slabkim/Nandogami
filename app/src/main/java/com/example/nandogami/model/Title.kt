package com.example.nandogami.model

import com.google.firebase.firestore.PropertyName

// Data class tambahan untuk menangani objek di dalam array "adaptations"
data class Adaptation(
    @get:PropertyName("studio") @set:PropertyName("studio")
    var studio: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "",

    // TIPE DATA HARUS LONG agar cocok dengan angka di database
    @get:PropertyName("year") @set:PropertyName("year")
    var year: Long = 0
)

data class Title(
    // ID dokumen, akan kita isi secara manual
    var id: String = "",

    @get:PropertyName("author") @set:PropertyName("author")
    var author: String = "",

    @get:PropertyName("categories") @set:PropertyName("categories")
    var categories: List<String> = emptyList(),

    @get:PropertyName("chapters") @set:PropertyName("chapters")
    var chapters: Long = 0,

    @get:PropertyName("format") @set:PropertyName("format")
    var format: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("isFeatured") @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,

    @get:PropertyName("isNewRelease") @set:PropertyName("isNewRelease")
    var isNewRelease: Boolean = false,

    @get:PropertyName("isPopular") @set:PropertyName("isPopular")
    var isPopular: Boolean = false,

    @get:PropertyName("rating") @set:PropertyName("rating")
    var rating: Double = 0.0,

    @get:PropertyName("release_year") @set:PropertyName("release_year")
    var release_year: Long = 0,

    @get:PropertyName("synopsis") @set:PropertyName("synopsis")
    var synopsis: String = "",

    @get:PropertyName("theme") @set:PropertyName("theme")
    var theme: List<String> = emptyList(),

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("title_lowercase") @set:PropertyName("title_lowercase")
    var title_lowercase: String = "",

    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "",

    // Menggunakan nama variabel yang Anda minta
    @get:PropertyName("alternative titles") @set:PropertyName("alternative titles")
    var alternativesTitles: List<String> = emptyList(),

    @get:PropertyName("adaptations") @set:PropertyName("adaptations")
    var adaptations: List<Adaptation> = emptyList()
) {
    // Konstruktor kosong ini WAJIB ada untuk Firestore
    constructor() : this(
        id = "",
        author = "",
        categories = emptyList(),
        chapters = 0,
        format = "",
        imageUrl = "",
        isFeatured = false,
        isNewRelease = false,
        isPopular = false,
        rating = 0.0,
        release_year = 0,
        synopsis = "",
        theme = emptyList(),
        title = "",
        type = "",
        alternativesTitles = emptyList(),
        adaptations = emptyList()
    )
}
