package com.example.artfinder.data.repository

import com.example.artfinder.data.local.ArtworkDao
import com.example.artfinder.data.local.ArtworkEntity
import com.example.artfinder.data.model.ArticArtworkData
import com.example.artfinder.data.remote.ArticApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ArtworkRepository(
    private val apiService: ArticApiService,
    private val artworkDao: ArtworkDao
) {
    val favoriteArtworks: Flow<List<ArticArtworkData>> = artworkDao.getAllFavorites().map { entities ->
        entities.map { it.toArticArtworkData() }
    }

    val favoriteIds: Flow<List<Long>> = artworkDao.getFavoriteIds()

    suspend fun searchArtworks(query: String): Result<List<ArticArtworkData>> {
        return try {
            val response = apiService.searchArtworks(query = query)
            val rawList = response.data ?: emptyList()
            // Filter out items with missing image_id as per requirements
            val filtered = rawList.filter { !it.imageId.isNullOrBlank() }
            Result.success(filtered)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(artwork: ArticArtworkData) {
        val isFav = artworkDao.isFavorite(artwork.id)
        if (isFav) {
            artworkDao.deleteFavoriteById(artwork.id)
        } else {
            ArtworkEntity.fromArticArtworkData(artwork)?.let { entity ->
                artworkDao.insertFavorite(entity)
            }
        }
    }

    suspend fun isFavorite(id: Long): Boolean {
        return artworkDao.isFavorite(id)
    }
}
