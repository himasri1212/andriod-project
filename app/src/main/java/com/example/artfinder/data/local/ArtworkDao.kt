package com.example.artfinder.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArtworkDao {
    @Query("SELECT * FROM favorite_artworks")
    fun getAllFavorites(): Flow<List<ArtworkEntity>>

    @Query("SELECT id FROM favorite_artworks")
    fun getFavoriteIds(): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_artworks WHERE id = :id)")
    suspend fun isFavorite(id: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(artwork: ArtworkEntity)

    @Delete
    suspend fun deleteFavorite(artwork: ArtworkEntity)

    @Query("DELETE FROM favorite_artworks WHERE id = :id")
    suspend fun deleteFavoriteById(id: Long)
}
