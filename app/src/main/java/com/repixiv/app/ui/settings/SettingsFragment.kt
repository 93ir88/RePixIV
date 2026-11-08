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
        binding.rowColorMode.setOnClickListener { showColorModeDialog() }
    }

    private fun showColorModeDialog() {
        val prefs = requireContext().getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val selected = prefs.getInt(MainActivity.KEY_COLOR_MODE, 0)

        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_color_mode)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // miuix popup width = ~72% of screen, centered
        val screenW = resources.displayMetrics.widthPixels
        dialog.window?.setLayout((screenW * 0.72).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        dialog.window?.setGravity(android.view.Gravity.CENTER)

        val container = dialog.findViewById<LinearLayout>(R.id.containerOptions)
        colorModes.forEachIndexed { idx, name ->
            val row = LayoutInflater.from(requireContext()).inflate(R.layout.item_color_option, container, false)
            val textOpt = row.findViewById<TextView>(R.id.textOption)
            val iconCheck = row.findViewById<ImageView>(R.id.iconCheck)
            textOpt.text = name
            if (idx == selected) {
                // miuix: selected = primary #277AF7
                textOpt.setTextColor(Color.parseColor("#277AF7"))
                iconCheck.visibility = View.VISIBLE
            } else {
                // miuix: unselected = onSurface ~ #F2F2F2 in dark
                textOpt.setTextColor(Color.parseColor("#F2F2F2"))
                iconCheck.visibility = View.GONE
            }
            row.setOnClickListener {
                prefs.edit().putInt(MainActivity.KEY_COLOR_MODE, idx).apply()
                binding.prefColorModeValue.text = name
                dialog.dismiss()
                // Apply and recreate — must post to avoid dismissal race
                binding.root.postDelayed({
                    AppCompatDelegate.setDefaultNightMode(MainActivity.nightModeFor(idx))
                    requireActivity().recreate()
                }, 100)
            }
            container.addView(row)
        }
        dialog.show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
