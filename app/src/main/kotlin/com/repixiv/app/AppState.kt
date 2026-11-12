package com.repixiv.app

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/** Settings state. Field set and defaults follow the Miuix example app's AppState. */
@Stable
data class AppState(
    val colorMode: Int = 0,
    val seedIndex: Int = 0,
    val paletteStyle: Int = 0,
    val colorSpec: Int = 0,
    val showTopAppBar: Boolean = true,
    val showNavigationBar: Boolean = true,
    val showNavigationBarBadge: Boolean = false,
    val navigationBarMode: Int = 0,
    val useFloatingNavigationBar: Boolean = true,
    val floatingNavigationBarPosition: Int = 0,
    val enablePageUserScroll: Boolean = true,
    val enableScrollEndHaptic: Boolean = true,
    val pagerInterceptionMode: Int = 0,
    val enableSquircle: Boolean = true,
    val enableBlur: Boolean = true,
    val blurStyle: Int = 1, // 0 = Gaussian, 1 = Progressive (default for a fresh install)
    val lowRes: Int = 0,
)

val LocalAppState = compositionLocalOf<AppState> { error("No AppState provided!") }
val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> { error("No AppState updater provided!") }

object AppStateStore {
    private const val PREFS = "repixiv_prefs_v2"

    fun load(context: Context): AppState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = AppState()
        val version = p.getInt("version", 1)
        return AppState(
            colorMode = p.getInt("colorMode", d.colorMode),
            seedIndex = p.getInt("seedIndex", d.seedIndex),
            paletteStyle = p.getInt("paletteStyle", d.paletteStyle),
            colorSpec = p.getInt("colorSpec", d.colorSpec),
            showTopAppBar = p.getBoolean("showTopAppBar", d.showTopAppBar),
            showNavigationBar = p.getBoolean("showNavigationBar", d.showNavigationBar),
            showNavigationBarBadge = p.getBoolean("showNavigationBarBadge", d.showNavigationBarBadge),
            navigationBarMode = p.getInt("navigationBarMode", d.navigationBarMode),
            useFloatingNavigationBar = p.getBoolean("useFloatingNavigationBar", d.useFloatingNavigationBar),
            floatingNavigationBarPosition = p.getInt("floatingNavigationBarPosition", d.floatingNavigationBarPosition),
            enablePageUserScroll = p.getBoolean("enablePageUserScroll", d.enablePageUserScroll),
            enableScrollEndHaptic = p.getBoolean("enableScrollEndHaptic", d.enableScrollEndHaptic),
            // v1 defaulted to Cross-Axis, which stole swipes from horizontal lists; reset it once.
            pagerInterceptionMode = if (version < 2) 0 else p.getInt("pagerInterceptionMode", d.pagerInterceptionMode),
            enableSquircle = p.getBoolean("enableSquircle", d.enableSquircle),
            enableBlur = p.getBoolean("enableBlur", d.enableBlur),
            blurStyle = p.getInt("blurStyle", d.blurStyle),
            lowRes = p.getInt("lowRes", d.lowRes),
        )
    }

    fun save(context: Context, s: AppState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("colorMode", s.colorMode)
            .putInt("seedIndex", s.seedIndex)
            .putInt("paletteStyle", s.paletteStyle)
            .putInt("colorSpec", s.colorSpec)
            .putBoolean("showTopAppBar", s.showTopAppBar)
            .putBoolean("showNavigationBar", s.showNavigationBar)
            .putBoolean("showNavigationBarBadge", s.showNavigationBarBadge)
            .putInt("navigationBarMode", s.navigationBarMode)
            .putBoolean("useFloatingNavigationBar", s.useFloatingNavigationBar)
            .putInt("floatingNavigationBarPosition", s.floatingNavigationBarPosition)
            .putBoolean("enablePageUserScroll", s.enablePageUserScroll)
            .putBoolean("enableScrollEndHaptic", s.enableScrollEndHaptic)
            .putInt("pagerInterceptionMode", s.pagerInterceptionMode)
            .putBoolean("enableSquircle", s.enableSquircle)
            .putBoolean("enableBlur", s.enableBlur)
            .putInt("blurStyle", s.blurStyle)
            .putInt("lowRes", s.lowRes)
            .putInt("version", 2)
            .apply()
    }
}
