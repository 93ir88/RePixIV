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
)

val MockArtworks = listOf(
    Artwork(1L, "Cherry Blossom at Dusk", "sakura_hana", "https://picsum.photos/seed/av1/80/80", "https://picsum.photos/seed/pixiv1/720/480", 4821, 23100, listOf("original", "landscape", "fantasy")),
    Artwork(2L, "Neon City Dreams", "neon_brush", "https://picsum.photos/seed/av2/80/80", "https://picsum.photos/seed/pixiv2/720/480", 7340, 51200, listOf("cyberpunk", "cityscape", "night")),
    Artwork(3L, "Forest Spirit", "mori_draws", "https://picsum.photos/seed/av3/80/80", "https://picsum.photos/seed/pixiv3/720/480", 2910, 18400, listOf("original", "nature", "spirit")),
    Artwork(4L, "Winter Wanderer", "yuki_paints", "https://picsum.photos/seed/av4/80/80", "https://picsum.photos/seed/pixiv4/720/480", 5500, 30200, listOf("original", "winter", "character")),
    Artwork(5L, "Ocean Reflection", "wave_artist", "https://picsum.photos/seed/av5/80/80", "https://picsum.photos/seed/pixiv5/720/480", 3100, 14700, listOf("original", "ocean", "seascape")),
    Artwork(6L, "Starfall Cathedral", "astral_ink", "https://picsum.photos/seed/av6/80/80", "https://picsum.photos/seed/pixiv6/720/480", 9120, 72300, listOf("fantasy", "architecture", "night")),
)
