package com.example.artfinder.data.remote

import com.example.artfinder.data.model.ArticSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ArticApiService {
    @GET("api/v1/artworks/search")
    suspend fun searchArtworks(
        @Query("q") query: String,
        @Query("fields") fields: String = "id,title,artist_display,date_display,image_id",
        @Query("limit") limit: Int = 30
    ): ArticSearchResponse
}
