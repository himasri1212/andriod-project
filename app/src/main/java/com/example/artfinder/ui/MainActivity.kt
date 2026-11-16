package com.example.artfinder.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.example.artfinder.R
import com.example.artfinder.data.local.AppDatabase
import com.example.artfinder.data.model.ArticArtworkData
import com.example.artfinder.data.remote.RetrofitClient
import com.example.artfinder.data.repository.ArtworkRepository
import com.example.artfinder.databinding.ActivityMainBinding
import com.example.artfinder.ui.adapter.ArtworkAdapter
import com.example.artfinder.ui.viewmodel.MainViewModel
import com.example.artfinder.ui.viewmodel.ViewMode
import com.example.artfinder.ui.viewmodel.ViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

open class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var artworkAdapter: ArtworkAdapter

    private var firebaseAuth: FirebaseAuth? = null
    private var googleSignInClient: GoogleSignInClient? = null

    // Camera Launcher
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            binding.ivSelectedPhoto.visibility = View.VISIBLE
            binding.ivSelectedPhoto.setImageBitmap(bitmap)
            viewModel.processBitmapForLabels(bitmap)
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Gallery Picker Launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = getBitmapFromUri(uri)
            if (bitmap != null) {
                binding.ivSelectedPhoto.visibility = View.VISIBLE
                binding.ivSelectedPhoto.setImageBitmap(bitmap)
                viewModel.processBitmapForLabels(bitmap)
            }
        } else {
            Toast.makeText(this, "Image selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Google Sign-In Launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null) {
                firebaseAuthWithGoogle(account)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebaseAuth()
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupFirebaseAuth() {
        try {
            firebaseAuth = FirebaseAuth.getInstance()

            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build()
            googleSignInClient = GoogleSignIn.getClient(this, gso)

            updateAuthUI()
        } catch (e: Exception) {
            binding.tvAuthStatus.text = "Auth not configured"
        }
    }

    private fun updateAuthUI() {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            binding.tvAuthStatus.text = "Signed in as: ${user.email ?: user.displayName ?: "User"}"
            binding.btnAuthAction.text = "Sign Out"
        } else {
            binding.tvAuthStatus.text = "Not Signed In"
            binding.btnAuthAction.text = "Sign In"
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val idToken = account.idToken
        if (idToken != null) {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth?.signInWithCredential(credential)
                ?.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                        updateAuthUI()
                    } else {
                        Toast.makeText(this, "Authentication failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // Fallback for demo sign in display
            Toast.makeText(this, "Signed in as ${account.email}", Toast.LENGTH_SHORT).show()
            updateAuthUI()
        }
    }

    private fun handleAuthButtonClick() {
        val user = firebaseAuth?.currentUser
        if (user != null) {
            firebaseAuth?.signOut()
            googleSignInClient?.signOut()
            updateAuthUI()
            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
        } else {
            googleSignInClient?.signInIntent?.let { intent ->
                googleSignInLauncher.launch(intent)
            } ?: run {
                Toast.makeText(this, "Google Sign-In client not initialized", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViewModel() {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ArtworkRepository(RetrofitClient.apiService, database.artworkDao())
        val factory = ViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]
    }

    private fun setupRecyclerView() {
        artworkAdapter = ArtworkAdapter(
            onArtworkClick = { artwork -> openDetailScreen(artwork) },
            onFavoriteClick = { artwork -> viewModel.toggleFavorite(artwork) }
        )
        binding.rvArtworks.layoutManager = GridLayoutManager(this, 2)
        binding.rvArtworks.adapter = artworkAdapter
    }

    private fun setupClickListeners() {
        binding.btnAuthAction.setOnClickListener {
            handleAuthButtonClick()
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhotoLauncher.launch(null)
        }

        binding.btnPickImage.setOnClickListener {
            pickImageLauncher.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        binding.btnManualSearch.setOnClickListener {
            performManualSearch()
        }

        binding.etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performManualSearch()
                true
            } else false
        }

        binding.toggleGroupViewMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnTabSearch -> viewModel.setViewMode(ViewMode.SEARCH)
                    R.id.btnTabFavorites -> viewModel.setViewMode(ViewMode.FAVORITES)
                }
            }
        }
    }

    private fun performManualSearch() {
        val query = binding.etSearchQuery.text?.toString().orEmpty().trim()
        if (query.isNotEmpty()) {
            viewModel.searchArtworks(query)
        } else {
            Toast.makeText(this, "Please enter a search term", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeViewModel() {
        viewModel.top3Labels.observe(this) { labels ->
            renderChips(labels)
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.favoriteIds.observe(this) { favoriteSet ->
            artworkAdapter.setFavoriteIds(favoriteSet)
        }

        viewModel.viewMode.observe(this) { mode ->
            updateListForMode(mode)
        }

        viewModel.searchResults.observe(this) { list ->
            if (viewModel.viewMode.value == ViewMode.SEARCH) {
                updateListContent(list, "Search Results (${list.size})")
            }
        }

        viewModel.favoriteArtworks.observe(this) { favoritesList ->
            if (viewModel.viewMode.value == ViewMode.FAVORITES) {
                updateListContent(favoritesList, "Saved Favorites (${favoritesList.size})")
            }
        }
    }

    private fun updateListForMode(mode: ViewMode) {
        when (mode) {
            ViewMode.SEARCH -> {
                binding.toggleGroupViewMode.check(R.id.btnTabSearch)
                val searchList = viewModel.searchResults.value ?: emptyList()
                updateListContent(searchList, "Search Results (${searchList.size})")
            }
            ViewMode.FAVORITES -> {
                binding.toggleGroupViewMode.check(R.id.btnTabFavorites)
                val favList = viewModel.favoriteArtworks.value ?: emptyList()
                updateListContent(favList, "Saved Favorites (${favList.size})")
            }
        }
    }

    private fun updateListContent(list: List<ArticArtworkData>, headerText: String) {
        binding.tvResultsHeader.text = headerText
        artworkAdapter.submitList(list)

        if (list.isEmpty()) {
            binding.tvEmptyMessage.visibility = View.VISIBLE
            binding.rvArtworks.visibility = View.GONE
        } else {
            binding.tvEmptyMessage.visibility = View.GONE
            binding.rvArtworks.visibility = View.VISIBLE
        }
    }

    private fun renderChips(labels: List<String>) {
        binding.chipGroupLabels.removeAllViews()
        if (labels.isEmpty()) {
            binding.tvTopLabelsHeader.visibility = View.GONE
            return
        }

        binding.tvTopLabelsHeader.visibility = View.VISIBLE
        labels.forEach { labelText ->
            val chip = Chip(this).apply {
                text = labelText
                isCheckable = false
                isClickable = true
                setOnClickListener {
                    binding.etSearchQuery.setText(labelText)
                    viewModel.searchArtworks(labelText)
                }
            }
            binding.chipGroupLabels.addView(chip)
        }
    }

    private fun openDetailScreen(artwork: ArticArtworkData) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra(DetailActivity.EXTRA_ID, artwork.id)
            putExtra(DetailActivity.EXTRA_TITLE, artwork.title)
            putExtra(DetailActivity.EXTRA_ARTIST, artwork.artistDisplay)
            putExtra(DetailActivity.EXTRA_DATE, artwork.dateDisplay)
            putExtra(DetailActivity.EXTRA_IMAGE_ID, artwork.imageId)
            putExtra(DetailActivity.EXTRA_IMAGE_URL, artwork.imageUrl)
        }
        startActivity(intent)
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
