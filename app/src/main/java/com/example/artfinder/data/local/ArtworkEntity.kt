package com.example.artfinder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.artfinder.data.model.ArticArtworkData

@Entity(tableName = "favorite_artworks")
data class ArtworkEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artistDisplay: String?,
    val dateDisplay: String?,
    val imageId: String,
    val imageUrl: String
) {
    fun toArticArtworkData(): ArticArtworkData {
        return ArticArtworkData(
            id = id,
            title = title,
            artistDisplay = artistDisplay,
            dateDisplay = dateDisplay,
            imageId = imageId
        )
    }

    companion object {
        fun fromArticArtworkData(artwork: ArticArtworkData): ArtworkEntity? {
            val imageId = artwork.imageId ?: return null
            val imageUrl = artwork.imageUrl ?: return null
            return ArtworkEntity(
                id = artwork.id,
                title = artwork.title ?: "Untitled",
                artistDisplay = artwork.artistDisplay,
                dateDisplay = artwork.dateDisplay,
                imageId = imageId,
                imageUrl = imageUrl
            )
        }
    }
}
