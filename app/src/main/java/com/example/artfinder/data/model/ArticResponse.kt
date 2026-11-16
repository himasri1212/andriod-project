package com.example.artfinder.data.model

import com.google.gson.annotations.SerializedName

data class ArticSearchResponse(
    @SerializedName("data")
    val data: List<ArticArtworkData>?
)

data class ArticArtworkData(
    @SerializedName("id")
    val id: Long,
    @SerializedName("title")
    val title: String?,
    @SerializedName("artist_display")
    val artistDisplay: String?,
    @SerializedName("date_display")
    val dateDisplay: String?,
    @SerializedName("image_id")
    val imageId: String?
) {
    val imageUrl: String?
        get() = if (!imageId.isNullOrBlank()) {
            "https://www.artic.edu/iiif/2/$imageId/full/843,/0/default.jpg"
        } else null
}
