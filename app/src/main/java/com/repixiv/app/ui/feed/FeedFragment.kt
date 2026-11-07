package com.repixiv.app.ui.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.repixiv.app.databinding.FragmentFeedBinding
import com.repixiv.app.model.Artwork

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadMockData()
    }

    private fun setupRecyclerView() {
        feedAdapter = FeedAdapter()
        binding.recyclerFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            setHasFixedSize(true)
        }
    }

    private fun loadMockData() {
        val mockArtworks = listOf(
            Artwork(
                id = 1L,
                title = "Cherry Blossom at Dusk",
                authorName = "sakura_hana",
                authorAvatarUrl = "https://picsum.photos/seed/av1/80/80",
                imageUrl = "https://picsum.photos/seed/pixiv1/720/480",
                likeCount = 4821,
                viewCount = 23100,
                tags = listOf("original", "landscape", "fantasy")
            ),
            Artwork(
                id = 2L,
                title = "Neon City Dreams",
                authorName = "neon_brush",
                authorAvatarUrl = "https://picsum.photos/seed/av2/80/80",
                imageUrl = "https://picsum.photos/seed/pixiv2/720/480",
                likeCount = 7340,
                viewCount = 51200,
                tags = listOf("cyberpunk", "cityscape", "night")
            ),
            Artwork(
                id = 3L,
                title = "Forest Spirit",
                authorName = "mori_draws",
                authorAvatarUrl = "https://picsum.photos/seed/av3/80/80",
                imageUrl = "https://picsum.photos/seed/pixiv3/720/480",
                likeCount = 2910,
                viewCount = 18400,
                tags = listOf("original", "nature", "spirit")
            ),
            Artwork(
                id = 4L,
                title = "Winter Wanderer",
                authorName = "yuki_paints",
                authorAvatarUrl = "https://picsum.photos/seed/av4/80/80",
                imageUrl = "https://picsum.photos/seed/pixiv4/720/480",
                likeCount = 5500,
                viewCount = 30200,
                tags = listOf("original", "winter", "character")
            ),
            Artwork(
                id = 5L,
                title = "Ocean Reflection",
                authorName = "wave_artist",
                authorAvatarUrl = "https://picsum.photos/seed/av5/80/80",
                imageUrl = "https://picsum.photos/seed/pixiv5/720/480",
                likeCount = 3100,
                viewCount = 14700,
                tags = listOf("original", "ocean", "seascape")
            ),
            Artwork(
                id = 6L,
                title = "Starfall Cathedral",
                authorName = "astral_ink",
                authorAvatarUrl = "https://picsum.photos/seed/av6/80/80",
                imageUrl = "https://picsum.photos/seed/pixiv6/720/480",
                likeCount = 9120,
                viewCount = 72300,
                tags = listOf("fantasy", "architecture", "night")
            )
        )
        feedAdapter.submitList(mockArtworks)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
