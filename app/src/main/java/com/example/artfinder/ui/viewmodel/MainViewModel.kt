package com.example.artfinder.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.artfinder.data.model.ArticArtworkData
import com.example.artfinder.data.repository.ArtworkRepository
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

enum class ViewMode {
    SEARCH, FAVORITES
}

class MainViewModel(
    private val repository: ArtworkRepository
) : ViewModel() {

    private val _selectedImageBitmap = MutableLiveData<Bitmap?>()
    val selectedImageBitmap: LiveData<Bitmap?> = _selectedImageBitmap

    private val _top3Labels = MutableLiveData<List<String>>(emptyList())
    val top3Labels: LiveData<List<String>> = _top3Labels

    private val _currentQuery = MutableLiveData<String>("")
    val currentQuery: LiveData<String> = _currentQuery

    private val _searchResults = MutableLiveData<List<ArticArtworkData>>(emptyList())
    val searchResults: LiveData<List<ArticArtworkData>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    private val _viewMode = MutableLiveData<ViewMode>(ViewMode.SEARCH)
    val viewMode: LiveData<ViewMode> = _viewMode

    val favoriteArtworks: LiveData<List<ArticArtworkData>> = repository.favoriteArtworks.asLiveData()
    val favoriteIds: LiveData<Set<Long>> = repository.favoriteIds.map { it.toSet() }.asLiveData()

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
    }

    fun processBitmapForLabels(bitmap: Bitmap) {
        _selectedImageBitmap.value = bitmap
        val image = InputImage.fromBitmap(bitmap, 0)
        runImageLabeler(image)
    }

    private fun runImageLabeler(image: InputImage) {
        _isLoading.value = true
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(image)
            .addOnSuccessListener { labels ->
                _isLoading.value = false
                // Extract top 3 labels by confidence
                val top3 = labels.sortedByDescending { it.confidence }
                    .take(3)
                    .map { it.text }
                _top3Labels.value = top3

                // Automatically search for the top 1 label if available
                if (top3.isNotEmpty()) {
                    searchArtworks(top3.first())
                }
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _errorMessage.value = "Image labeling failed: ${e.localizedMessage}"
            }
    }

    fun searchArtworks(query: String) {
        if (query.isBlank()) return
        _currentQuery.value = query
        _viewMode.value = ViewMode.SEARCH
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.searchArtworks(query)
            _isLoading.value = false
            result.onSuccess { list ->
                _searchResults.value = list
            }.onFailure { error ->
                _errorMessage.value = "Search failed: ${error.localizedMessage}"
            }
        }
    }

    fun toggleFavorite(artwork: ArticArtworkData) {
        viewModelScope.launch {
            repository.toggleFavorite(artwork)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
