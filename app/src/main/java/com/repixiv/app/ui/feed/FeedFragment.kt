package com.repixiv.app.ui.feed

import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.repixiv.app.R
import com.repixiv.app.databinding.FragmentFeedBinding
import com.repixiv.app.model.Artwork

class FeedFragment : Fragment() {
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var feedAdapter: FeedAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentFeedBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        feedAdapter = FeedAdapter()
        binding.recyclerFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = feedAdapter
            setHasFixedSize(true)
        }
        // Add bottom padding so last card clears the nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.recyclerFeed) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = resources.getDimensionPixelSize(R.dimen.nav_bar_height) + bottom)
            insets
        }
        feedAdapter.submitList(mockArtworks())
    }

    private fun mockArtworks() = listOf(
        Artwork(1L,"Cherry Blossom at Dusk","sakura_hana","https://picsum.photos/seed/av1/80/80","https://picsum.photos/seed/pixiv1/720/480",4821,23100,listOf("original","landscape","fantasy")),
        Artwork(2L,"Neon City Dreams","neon_brush","https://picsum.photos/seed/av2/80/80","https://picsum.photos/seed/pixiv2/720/480",7340,51200,listOf("cyberpunk","cityscape","night")),
        Artwork(3L,"Forest Spirit","mori_draws","https://picsum.photos/seed/av3/80/80","https://picsum.photos/seed/pixiv3/720/480",2910,18400,listOf("original","nature","spirit")),
        Artwork(4L,"Winter Wanderer","yuki_paints","https://picsum.photos/seed/av4/80/80","https://picsum.photos/seed/pixiv4/720/480",5500,30200,listOf("original","winter","character")),
        Artwork(5L,"Ocean Reflection","wave_artist","https://picsum.photos/seed/av5/80/80","https://picsum.photos/seed/pixiv5/720/480",3100,14700,listOf("original","ocean","seascape")),
        Artwork(6L,"Starfall Cathedral","astral_ink","https://picsum.photos/seed/av6/80/80","https://picsum.photos/seed/pixiv6/720/480",9120,72300,listOf("fantasy","architecture","night"))
    )

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
