package com.repixiv.app

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
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
import eightbitlab.com.blurview.RenderEffectBlur
import eightbitlab.com.blurview.RenderScriptBlur
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        // 1. Read mode from prefs
        val prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val mode = prefs.getInt(KEY_COLOR_MODE, 0)

        // 2. Apply night mode BEFORE super (standard AppCompat pattern)
        AppCompatDelegate.setDefaultNightMode(nightModeFor(mode))

        // 3. Wrap context with Monet dynamic colors for modes 3-5 on Android 12+
        //    Use try-catch — DynamicColors may not apply if theme is incompatible
        val ctx = try {
            if (mode >= 3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                DynamicColors.wrapContextIfAvailable(newBase) ?: newBase
            } else newBase
        } catch (e: Exception) { newBase }

        super.attachBaseContext(ctx)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge
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
            1, 4 -> false
            2, 5 -> true
            else -> (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        // miuix PuredThinGlass overlay — from GlassMaterials.kt
        val overlayColor = if (isDark) Color.argb(0xB8, 0x28, 0x28, 0x28)
                           else        Color.argb(0x88, 0xEE, 0xEE, 0xEE)

        val rootView = window.decorView.rootView as ViewGroup
        @Suppress("DEPRECATION")
        val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            RenderEffectBlur() else RenderScriptBlur(this)

        binding.navBlurView.setupWith(rootView, algorithm)
            .setBlurRadius(20f)
            .setOverlayColor(overlayColor)
            .setBlurAutoUpdate(true)

        binding.bottomNavigation.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val sysBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val navBarHeight = resources.getDimensionPixelSize(R.dimen.nav_bar_height)
            val total = navBarHeight + sysBottom
            binding.navBlurView.updateLayoutParams { height = total }
            binding.bottomNavigation.updateLayoutParams { height = total }
            binding.bottomNavigation.updatePadding(bottom = sysBottom)
            binding.navDivider.updateLayoutParams<android.widget.FrameLayout.LayoutParams> {
                bottomMargin = total
            }
            WindowInsetsCompat.CONSUMED
        }
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
