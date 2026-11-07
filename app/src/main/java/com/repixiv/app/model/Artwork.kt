package com.repixiv.app.model

data class Artwork(
    val id: Long,
    val title: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val imageUrl: String,
    val likeCount: Int,
    val viewCount: Int,
    val tags: List<String>,
    val isLiked: Boolean = false
)
