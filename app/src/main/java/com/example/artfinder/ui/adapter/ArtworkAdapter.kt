package com.example.artfinder.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.artfinder.R
import com.example.artfinder.data.model.ArticArtworkData
import com.example.artfinder.databinding.ItemArtworkBinding

class ArtworkAdapter(
    private val onArtworkClick: (ArticArtworkData) -> Unit,
    private val onFavoriteClick: (ArticArtworkData) -> Unit
) : ListAdapter<ArticArtworkData, ArtworkAdapter.ArtworkViewHolder>(ArtworkDiffCallback()) {

    private var favoriteIds: Set<Long> = emptySet()

    fun setFavoriteIds(ids: Set<Long>) {
        this.favoriteIds = ids
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtworkViewHolder {
        val binding = ItemArtworkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArtworkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtworkViewHolder, position: Int) {
        val item = getItem(position)
        val isFavorite = favoriteIds.contains(item.id)
        holder.bind(item, isFavorite)
    }

    inner class ArtworkViewHolder(private val binding: ItemArtworkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ArticArtworkData, isFavorite: Boolean) {
            binding.tvTitle.text = item.title ?: "Untitled"
            binding.tvArtist.text = item.artistDisplay ?: "Unknown Artist"

            binding.ivArtwork.load(item.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_dialog_alert)
            }

            val iconRes = if (isFavorite) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            }
            binding.btnFavorite.setImageResource(iconRes)

            binding.root.setOnClickListener {
                onArtworkClick(item)
            }

            binding.btnFavorite.setOnClickListener {
                onFavoriteClick(item)
            }
        }
    }

    class ArtworkDiffCallback : DiffUtil.ItemCallback<ArticArtworkData>() {
        override fun areItemsTheSame(oldItem: ArticArtworkData, newItem: ArticArtworkData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ArticArtworkData, newItem: ArticArtworkData): Boolean {
            return oldItem == newItem
        }
    }
}
