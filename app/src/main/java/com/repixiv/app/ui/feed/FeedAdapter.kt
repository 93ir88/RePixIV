package com.repixiv.app.ui.feed

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.repixiv.app.R
import com.repixiv.app.databinding.ItemArtworkBinding
import com.repixiv.app.model.Artwork

class FeedAdapter : ListAdapter<Artwork, FeedAdapter.ArtworkViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtworkViewHolder {
        val binding = ItemArtworkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ArtworkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArtworkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArtworkViewHolder(
        private val binding: ItemArtworkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(artwork: Artwork) = with(binding) {
            textAuthorName.text = artwork.authorName
            textTitle.text = artwork.title
            textLikeCount.text = formatCount(artwork.likeCount)
            textViewCount.text = formatCount(artwork.viewCount)
            textTags.text = artwork.tags.joinToString("  ") { "#$it" }

            Glide.with(imageAvatar.context)
                .load(artwork.authorAvatarUrl)
                .transform(CircleCrop())
                .placeholder(R.drawable.ic_nav_profile)
                .into(imageAvatar)

            Glide.with(imageArtwork.context)
                .load(artwork.imageUrl)
                .centerCrop()
                .transform(RoundedCorners(24))
                .placeholder(R.drawable.ic_launcher_background)
                .into(imageArtwork)

            buttonLike.setOnClickListener {
                // TODO: wire to PixivRepository.toggleLike(artwork.id)
            }
        }

        private fun formatCount(count: Int): String = when {
            count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M"
            count >= 1_000     -> "${"%.1f".format(count / 1_000.0)}K"
            else               -> count.toString()
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Artwork>() {
        override fun areItemsTheSame(a: Artwork, b: Artwork) = a.id == b.id
        override fun areContentsTheSame(a: Artwork, b: Artwork) = a == b
    }
}
