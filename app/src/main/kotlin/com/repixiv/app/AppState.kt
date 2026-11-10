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
    val showFPSMonitor: Boolean = false,
    val showTopAppBar: Boolean = true,
    val showNavigationBar: Boolean = true,
    val showNavigationBarBadge: Boolean = false,
    val navigationBarMode: Int = 0,
    val enablePageUserScroll: Boolean = true,
    val enableScrollEndHaptic: Boolean = true,
    val pagerInterceptionMode: Int = 1,
    val enableSquircle: Boolean = true,
    val enableBlur: Boolean = true,
)

val LocalAppState = compositionLocalOf<AppState> { error("No AppState provided!") }
val LocalUpdateAppState = staticCompositionLocalOf<((AppState) -> AppState) -> Unit> { error("No AppState updater provided!") }

object AppStateStore {
    private const val PREFS = "repixiv_prefs_v2"

    fun load(context: Context): AppState {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val d = AppState()
        return AppState(
            colorMode = p.getInt("colorMode", d.colorMode),
            seedIndex = p.getInt("seedIndex", d.seedIndex),
            paletteStyle = p.getInt("paletteStyle", d.paletteStyle),
            colorSpec = p.getInt("colorSpec", d.colorSpec),
            showFPSMonitor = p.getBoolean("showFPSMonitor", d.showFPSMonitor),
            showTopAppBar = p.getBoolean("showTopAppBar", d.showTopAppBar),
            showNavigationBar = p.getBoolean("showNavigationBar", d.showNavigationBar),
            showNavigationBarBadge = p.getBoolean("showNavigationBarBadge", d.showNavigationBarBadge),
            navigationBarMode = p.getInt("navigationBarMode", d.navigationBarMode),
            enablePageUserScroll = p.getBoolean("enablePageUserScroll", d.enablePageUserScroll),
            enableScrollEndHaptic = p.getBoolean("enableScrollEndHaptic", d.enableScrollEndHaptic),
            pagerInterceptionMode = p.getInt("pagerInterceptionMode", d.pagerInterceptionMode),
            enableSquircle = p.getBoolean("enableSquircle", d.enableSquircle),
            enableBlur = p.getBoolean("enableBlur", d.enableBlur),
        )
    }

    fun save(context: Context, s: AppState) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt("colorMode", s.colorMode)
            .putInt("seedIndex", s.seedIndex)
            .putInt("paletteStyle", s.paletteStyle)
            .putInt("colorSpec", s.colorSpec)
            .putBoolean("showFPSMonitor", s.showFPSMonitor)
            .putBoolean("showTopAppBar", s.showTopAppBar)
            .putBoolean("showNavigationBar", s.showNavigationBar)
            .putBoolean("showNavigationBarBadge", s.showNavigationBarBadge)
            .putInt("navigationBarMode", s.navigationBarMode)
            .putBoolean("enablePageUserScroll", s.enablePageUserScroll)
            .putBoolean("enableScrollEndHaptic", s.enableScrollEndHaptic)
            .putInt("pagerInterceptionMode", s.pagerInterceptionMode)
            .putBoolean("enableSquircle", s.enableSquircle)
            .putBoolean("enableBlur", s.enableBlur)
            .apply()
    }
}
