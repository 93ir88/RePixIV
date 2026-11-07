package com.repixiv.app.ui.settings

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.repixiv.app.MainActivity
import com.repixiv.app.R
import com.repixiv.app.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val colorModes = listOf("System", "Light", "Dark", "MonetSystem", "MonetLight", "MonetDark")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val current = prefs.getInt(MainActivity.KEY_COLOR_MODE, 0)
        binding.prefColorModeValue.text = colorModes[current]

        binding.rowColorMode.setOnClickListener {
            showColorModeDialog(prefs.getInt(MainActivity.KEY_COLOR_MODE, 0))
        }
    }

    private fun showColorModeDialog(selected: Int) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_color_mode)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.72).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val container = dialog.findViewById<LinearLayout>(R.id.containerOptions)
        colorModes.forEachIndexed { idx, name ->
            val row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_color_option, container, false)
            val textOpt = row.findViewById<TextView>(R.id.textOption)
            val iconCheck = row.findViewById<ImageView>(R.id.iconCheck)
            textOpt.text = name
            if (idx == selected) {
                textOpt.setTextColor(Color.parseColor("#3F7FFF"))
                iconCheck.visibility = View.VISIBLE
            } else {
                textOpt.setTextColor(Color.parseColor("#CCCCDD"))
                iconCheck.visibility = View.GONE
            }
            row.setOnClickListener {
                val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putInt(MainActivity.KEY_COLOR_MODE, idx).apply()
                AppCompatDelegate.setDefaultNightMode(MainActivity.nightModeFor(idx))
                binding.prefColorModeValue.text = colorModes[idx]
                dialog.dismiss()
                requireActivity().recreate()
            }
            container.addView(row)
        }
        dialog.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
