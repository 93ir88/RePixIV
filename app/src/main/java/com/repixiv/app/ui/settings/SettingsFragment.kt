package com.repixiv.app.ui.settings

import android.app.Dialog
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

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(i, c, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Bottom padding so settings aren't hidden behind nav bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            v.updatePadding(bottom = resources.getDimensionPixelSize(R.dimen.nav_bar_height) + bottom)
            insets
        }
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val savedMode = prefs.getInt(MainActivity.KEY_COLOR_MODE, 0).coerceIn(colorModes.indices)
        binding.prefColorModeValue.text = colorModes[savedMode]
        binding.rowColorMode.setOnClickListener { showColorModeDialog() }
    }

    private fun isDarkMode(): Boolean {
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        return when (prefs.getInt(MainActivity.KEY_COLOR_MODE, 0)) {
            2, 5 -> true
            1, 4 -> false
            else -> (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }

    private fun showColorModeDialog() {
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val selected = prefs.getInt(MainActivity.KEY_COLOR_MODE, 0).coerceIn(colorModes.indices)
        val dark = isDarkMode()

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_color_mode)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Miuix OverlayDropdownPreference popup: compact rounded panel centered
        // over the screen, with a checkmark on the currently selected option.
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setDimAmount(0.40f)
        dialog.setCancelable(true)
        dialog.setCanceledOnTouchOutside(true)

        val container = dialog.findViewById<LinearLayout>(R.id.containerOptions)
        val selectedBlue = Color.parseColor("#277AF7")
        val unselectedColor = if (dark) Color.parseColor("#F2F2F2") else Color.parseColor("#1A1A1A")

        colorModes.forEachIndexed { idx, name ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_color_option, container, false)
            row.findViewById<TextView>(R.id.textOption).apply {
                text = name
                setTextColor(if (idx == selected) selectedBlue else unselectedColor)
            }
            row.findViewById<android.widget.ImageView>(R.id.iconCheck).visibility =
                if (idx == selected) android.view.View.VISIBLE else android.view.View.GONE
            row.setOnClickListener {
                prefs.edit().putInt(MainActivity.KEY_COLOR_MODE, idx).apply()
                binding.prefColorModeValue.text = name
                dialog.dismiss()
                binding.root.postDelayed({
                    AppCompatDelegate.setDefaultNightMode(MainActivity.nightModeFor(idx))
                    requireActivity().recreate()
                }, 100)
            }
            container.addView(row)
        }
        dialog.show()
        val popupW = (resources.displayMetrics.widthPixels * 0.62f).toInt()
        dialog.window?.setLayout(popupW, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
