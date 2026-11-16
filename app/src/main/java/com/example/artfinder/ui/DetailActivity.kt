package com.example.artfinder.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.artfinder.data.local.AppDatabase
import com.example.artfinder.data.model.ArticArtworkData
import com.example.artfinder.data.remote.RetrofitClient
import com.example.artfinder.data.repository.ArtworkRepository
import com.example.artfinder.databinding.ActivityDetailBinding
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding
    private lateinit var repository: ArtworkRepository

    private var artworkId: Long = -1L
    private var title: String? = null
    private var artistDisplay: String? = null
    private var dateDisplay: String? = null
    private var imageId: String? = null
    private var imageUrl: String? = null

    private var isFavorite = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val db = AppDatabase.getDatabase(applicationContext)
        repository = ArtworkRepository(RetrofitClient.apiService, db.artworkDao())

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        extractIntentData()
        setupUI()
        observeFavoriteStatus()
    }

    private fun extractIntentData() {
        artworkId = intent.getLongExtra(EXTRA_ID, -1L)
        title = intent.getStringExtra(EXTRA_TITLE)
        artistDisplay = intent.getStringExtra(EXTRA_ARTIST)
        dateDisplay = intent.getStringExtra(EXTRA_DATE)
        imageId = intent.getStringExtra(EXTRA_IMAGE_ID)
        imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)
    }

    private fun setupUI() {
        binding.tvDetailTitle.text = title ?: "Untitled Artwork"
        binding.tvDetailArtist.text = artistDisplay ?: "Unknown Artist"
        binding.tvDetailDate.text = if (!dateDisplay.isNullOrBlank()) "Date: $dateDisplay" else "Date: N/A"

        binding.ivDetailArtwork.load(imageUrl) {
            crossfade(true)
            placeholder(android.R.drawable.ic_menu_gallery)
            error(android.R.drawable.ic_dialog_alert)
        }

        binding.btnShare.setOnClickListener {
            shareArtwork()
        }

        binding.btnFavoriteDetail.setOnClickListener {
            toggleFavorite()
        }
    }

    private fun observeFavoriteStatus() {
        if (artworkId == -1L) return
        lifecycleScope.launch {
            repository.favoriteIds.collect { ids ->
                isFavorite = ids.contains(artworkId)
                updateFavoriteButtonUI()
            }
        }
    }

    private fun updateFavoriteButtonUI() {
        if (isFavorite) {
            binding.btnFavoriteDetail.setText("Remove Favorite")
            binding.btnFavoriteDetail.setIconResource(android.R.drawable.btn_star_big_on)
        } else {
            binding.btnFavoriteDetail.setText("Save Favorite")
            binding.btnFavoriteDetail.setIconResource(android.R.drawable.btn_star_big_off)
        }
    }

    private fun toggleFavorite() {
        val artwork = ArticArtworkData(
            id = artworkId,
            title = title,
            artistDisplay = artistDisplay,
            dateDisplay = dateDisplay,
            imageId = imageId
        )
        lifecycleScope.launch {
            repository.toggleFavorite(artwork)
        }
    }

    private fun shareArtwork() {
        val shareText = buildString {
            append("Check out this artwork from the Art Institute of Chicago!\n\n")
            append("Title: ${title ?: "Untitled"}\n")
            if (!artistDisplay.isNullOrBlank()) {
                append("Artist: $artistDisplay\n")
            }
            if (!dateDisplay.isNullOrBlank()) {
                append("Date: $dateDisplay\n")
            }
            if (!imageUrl.isNullOrBlank()) {
                append("\nImage: $imageUrl")
            }
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Artwork")
        startActivity(shareIntent)
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_IMAGE_ID = "extra_image_id"
        const val EXTRA_IMAGE_URL = "extra_image_url"
    }
}
