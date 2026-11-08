package com.repixiv.app.ui.profile
import android.os.Bundle; import android.view.*
import androidx.core.view.ViewCompat; import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding; import androidx.fragment.app.Fragment
import com.repixiv.app.R; import com.repixiv.app.databinding.FragmentProfileBinding
class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null; private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentProfileBinding.inflate(i, c, false)
        return b.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(b.root) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = resources.getDimensionPixelSize(R.dimen.nav_bar_height) + bottom)
            insets
        }
    }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
