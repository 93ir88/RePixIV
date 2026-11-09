package com.repixiv.app.ui.settings

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.repixiv.app.MainActivity
import com.repixiv.app.R
import com.repixiv.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val colorModes = listOf("System","Light","Dark","MonetSystem","MonetLight","MonetDark")
    private var popup: PopupWindow? = null

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = resources.getDimensionPixelSize(R.dimen.nav_bar_height) + bottom)
            insets
        }
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        binding.prefColorModeValue.text = colorModes[prefs.getInt(MainActivity.KEY_COLOR_MODE, 0).coerceIn(0, 5)]
        binding.rowColorMode.setOnClickListener { showColorModePopup() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun isDark(): Boolean {
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getInt(MainActivity.KEY_COLOR_MODE, 0)) {
            2, 5 -> true
            1, 4 -> false
            else -> (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun showColorModePopup() {
        popup?.dismiss()
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val selected = prefs.getInt(MainActivity.KEY_COLOR_MODE, 0).coerceIn(0, 5)
        val dark = isDark()

        // miuix DropdownDefaults colors
        // selectedContentColor = primary = #277AF7
        // contentColor = onSurfaceContainer = dark:#E6FFFFFF, light:#E6000000
        val selectedColor = Color.parseColor("#277AF7")
        val unselectedColor = if (dark) Color.parseColor("#E0E0E0") else Color.parseColor("#1A1A1A")

        // Inflate popup view
        val popupView = LayoutInflater.from(requireContext()).inflate(R.layout.popup_color_mode, null)
        val container = popupView.findViewById<LinearLayout>(R.id.containerOptions)

        colorModes.forEachIndexed { idx, name ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_color_option, container, false)
            val textOpt = row.findViewById<TextView>(R.id.textOption)
            val iconCheck = row.findViewById<ImageView>(R.id.iconCheck)
            textOpt.text = name
            textOpt.setTextColor(if (idx == selected) selectedColor else unselectedColor)
            iconCheck.visibility = if (idx == selected) View.VISIBLE else View.GONE
            row.setOnClickListener {
                prefs.edit().putInt(MainActivity.KEY_COLOR_MODE, idx).apply()
                binding.prefColorModeValue.text = name
                popup?.dismiss()
                binding.root.postDelayed({
                    AppCompatDelegate.setDefaultNightMode(MainActivity.nightModeFor(idx))
                    requireActivity().recreate()
                }, 80)
            }
            container.addView(row)
        }

        // miuix ListPopupColumn: width auto-sizes between 200dp–288dp
        // Using 288dp (max) since we have 6 items with similar text lengths
        val popupWidth = dp(288)

        val pw = PopupWindow(popupView, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        pw.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        pw.elevation = dp(8).toFloat()
        pw.isOutsideTouchable = true
        pw.inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED

        // miuix Align.End: right-aligned to anchor, 8dp vertical gap below
        // xoff = anchor.width - popupWidth → right edge of popup aligns with right of anchor
        val xoff = binding.rowColorMode.width - popupWidth
        pw.showAsDropDown(binding.rowColorMode, xoff, dp(8))
        popup = pw
    }

    override fun onDestroyView() {
        popup?.dismiss()
        super.onDestroyView()
        _binding = null
    }
}
