package com.repixiv.app.ui.search
import android.os.Bundle; import android.view.*; import androidx.fragment.app.Fragment
import com.repixiv.app.databinding.FragmentSearchBinding
class SearchFragment : Fragment() {
    private var _b: FragmentSearchBinding? = null; private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _b = FragmentSearchBinding.inflate(i, c, false); return b.root }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
