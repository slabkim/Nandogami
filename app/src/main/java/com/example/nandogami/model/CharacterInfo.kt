package com.example.nandogami.model

data class CharacterInfo(
    val name: String = "",
    val imageUrl: String = "", // URL gambar karakter
    val positionX: Float = 0.5f, // Posisi relatif X (0.0 - 1.0)
    val positionY: Float = 0.5f, // Posisi relatif Y (0.0 - 1.0)
    val rotation: Float = 0f, // Rotasi gambar dalam derajat
    val widthRatio: Float = 0.4f, // Rasio lebar terhadap container (0.0 - 1.0)
    val heightRatio: Float = 0.6f, // Rasio tinggi terhadap container (0.0 - 1.0)
    val zIndex: Int = 0 // Untuk z-ordering (yang di depan punya zIndex lebih tinggi)
)