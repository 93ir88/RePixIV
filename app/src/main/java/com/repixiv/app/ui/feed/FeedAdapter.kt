package com.repixiv.app.ui.feed

import android.view.*
import androidx.recyclerview.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.repixiv.app.R
import com.repixiv.app.databinding.ItemArtworkBinding
import com.repixiv.app.model.Artwork

class FeedAdapter : ListAdapter<Artwork, FeedAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(p: ViewGroup, t: Int) =
        ViewHolder(ItemArtworkBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) = h.bind(getItem(pos))

    inner class ViewHolder(private val b: ItemArtworkBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(a: Artwork) = with(b) {
            textAuthorName.text = a.authorName
            textTitle.text = a.title
            textLikeCount.text = fmt(a.likeCount)
            textViewCount.text = fmt(a.viewCount)
            textTags.text = a.tags.joinToString("  ") { "#$it" }
            Glide.with(imageAvatar.context).load(a.authorAvatarUrl).transform(CircleCrop())
                .placeholder(R.drawable.ic_nav_profile).into(imageAvatar)
            Glide.with(imageArtwork.context).load(a.imageUrl).centerCrop()
                .transform(RoundedCorners(24)).placeholder(R.drawable.ic_launcher_background).into(imageArtwork)
        }
        private fun fmt(n: Int) = when { n >= 1_000_000 -> "${"%.1f".format(n/1_000_000.0)}M"; n >= 1_000 -> "${"%.1f".format(n/1_000.0)}K"; else -> n.toString() }
    }

    private class Diff : DiffUtil.ItemCallback<Artwork>() {
        override fun areItemsTheSame(a: Artwork, b: Artwork) = a.id == b.id
        override fun areContentsTheSame(a: Artwork, b: Artwork) = a == b
    }
}
