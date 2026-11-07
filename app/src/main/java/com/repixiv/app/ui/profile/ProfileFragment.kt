package com.repixiv.app.ui.profile
import android.os.Bundle; import android.view.*; import androidx.fragment.app.Fragment
import com.repixiv.app.databinding.FragmentProfileBinding
class ProfileFragment : Fragment() {
    private var _b: FragmentProfileBinding? = null; private val b get() = _b!!
    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View { _b = FragmentProfileBinding.inflate(i, c, false); return b.root }
    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
