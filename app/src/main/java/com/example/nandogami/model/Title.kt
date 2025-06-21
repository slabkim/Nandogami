package com.example.nandogami.model

import com.google.firebase.firestore.PropertyName

data class Title(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("author") @set:PropertyName("author")
    var author: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("type") @set:PropertyName("type")
    var type: String = "",

    @get:PropertyName("rating") @set:PropertyName("rating")
    var rating: Float = 0f,

    @get:PropertyName("isFeatured") @set:PropertyName("isFeatured")
    var isFeatured: Boolean = false,

    @get:PropertyName("isPopular") @set:PropertyName("isPopular")
    var isPopular: Boolean = false,

    @get:PropertyName("isNewRelease") @set:PropertyName("isNewRelease")
    var isNewRelease: Boolean = false,

    @get:PropertyName("categories") @set:PropertyName("categories")
    var categories: List<String> = emptyList()
) {
    // Konstruktor kosong ini WAJIB ada untuk Firestore
    constructor() : this("", "", "", "", "", 0f, false, false, false, emptyList())
}