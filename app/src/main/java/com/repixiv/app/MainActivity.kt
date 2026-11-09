package com.repixiv.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.color.DynamicColors
import com.repixiv.app.databinding.ActivityMainBinding
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        // Apply Monet dynamic colors on API 31+ for Monet modes
        val prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getInt(KEY_COLOR_MODE, 0)
        // Monet is optional: a broken/unsupported dynamic-color overlay must never
        // prevent the app from starting. Keep the selected light/dark mode as fallback.
        val ctx = if (mode >= 3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { DynamicColors.wrapContextIfAvailable(newBase) }.getOrDefault(newBase)
        } else newBase
        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyColorMode()
        super.onCreate(savedInstanceState)

        // Edge-to-edge: content draws behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNavigation.setupWithNavController(navController)

        setupFrostedNavBar()
        setupWindowInsets()
    }

    private fun setupFrostedNavBar() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getInt(KEY_COLOR_MODE, 0)
        val isDark = when (mode) {
            1, 4 -> false   // Light, MonetLight
            2, 5 -> true    // Dark, MonetDark
            else -> (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }

        // miuix PuredThinGlass overlay colors (from GlassMaterials.kt)
        // Dark:  0x1A000000 PlusDarker + 0x66565656 Lum + 0x993F3F3F Overlay → ~#B8282828
        // Light: 0x05000000 PlusDarker + 0x99FFFFFF SoftLight + 0x66FFFFFF HardLight → ~#88EEEEEE
        val overlayColor = if (isDark)
            Color.argb(0xB8, 0x28, 0x28, 0x28)
        else
            Color.argb(0x88, 0xEE, 0xEE, 0xEE)

        val rootView = window.decorView.rootView as ViewGroup
        @Suppress("DEPRECATION")
        val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            RenderEffectBlur()
        } else {
            RenderScriptBlur(this)
        }

        // miuix PuredThinGlass blurRadius = 20.dp
        binding.navBlurView.setupWith(rootView, algorithm)
            .setBlurRadius(20f)
            .setOverlayColor(overlayColor)
            .setBlurAutoUpdate(true)

        // Bottom nav itself is transparent — blur view shows through
        binding.bottomNavigation.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val navBarHeight = resources.getDimensionPixelSize(R.dimen.nav_bar_height)
            val totalNavHeight = navBarHeight + sysBottom

            // Expand blur view to cover nav bar + system gesture area
            binding.navBlurView.updateLayoutParams { height = totalNavHeight }

            // Push BNV content up by system inset (gesture pill / button bar)
            binding.bottomNavigation.updateLayoutParams { height = totalNavHeight }
            binding.bottomNavigation.updatePadding(bottom = sysBottom)

            // Move divider to sit at top of blur area
            binding.navDivider.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
                bottomMargin = totalNavHeight
            }

            WindowInsetsCompat.CONSUMED
        }
    }

    private fun applyColorMode() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getInt(KEY_COLOR_MODE, 0)
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))
    }

    companion object {
        const val PREFS_NAME = "repixiv_prefs"
        const val KEY_COLOR_MODE = "color_mode"

        fun nightModeFor(mode: Int): Int = when (mode) {
            1, 4 -> AppCompatDelegate.MODE_NIGHT_NO
            2, 5 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
    }
}
