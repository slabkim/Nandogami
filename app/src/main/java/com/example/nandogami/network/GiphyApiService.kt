package com.example.nandogami.network

import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyApiService {
    @GET("v1/gifs/search")
    suspend fun searchGifs(
        @Query("api_key") apiKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 25
    ): GiphyResponse
}

// Model response minimal
data class GiphyResponse(
    val data: List<GifData>
)
data class GifData(
    val id: String,
    val images: GifImages
)
data class GifImages(
    val fixed_height: GifImage
)
data class GifImage(
    val url: String
) 