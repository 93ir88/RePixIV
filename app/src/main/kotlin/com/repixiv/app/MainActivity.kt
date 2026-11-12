// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.repixiv.app.player.AppPlayer
import com.repixiv.app.player.GoogleAuth
import com.repixiv.app.player.PlayerStore
import com.repixiv.app.pixiv.Filters
import com.repixiv.app.pixiv.LocalLists
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.ui.AppTheme
import com.repixiv.app.ui.keyColorFor
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled

class MainActivity : ComponentActivity() {
    /** Logins happen in the device browser, which redirects back here (pixiv:// and Google's reversed-client-id scheme). */
    private fun handleLoginRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        when {
            uri.scheme == "pixiv" -> {
                val code = uri.getQueryParameter("code") ?: return
                runLogin { Pixiv.exchangeCodeBlocking(code) }
            }
            GoogleAuth.isRedirect(uri) -> runLogin { GoogleAuth.completeLoginBlocking(uri) }
            else -> return
        }
        intent.data = null // handle each redirect once
    }

    private fun runLogin(block: () -> Unit) {
        Toast.makeText(this, "Signing in…", Toast.LENGTH_SHORT).show()
        Thread {
            try {
                block()
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, e.message ?: "Login failed", Toast.LENGTH_LONG).show() }
            }
        }.start()
    }

    /**
     * The YouTube player's WebView must stay attached to a window to keep playing, so it lives here as a
     * 2dp view in the corner instead of a black box in the Player page.
     */
    private fun attachPlayerView() {
        val view = AppPlayer.webView(this)
        (view.parent as? ViewGroup)?.removeView(view)
        val px = (2 * resources.displayMetrics.density).toInt().coerceAtLeast(2)
        addContentView(view, FrameLayout.LayoutParams(px, px))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLoginRedirect(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Pixiv.init(applicationContext)
        Filters.init(applicationContext)
        LocalLists.init(applicationContext)
        PlayerStore.init(applicationContext)
        GoogleAuth.init(applicationContext)
        attachPlayerView()
        if (savedInstanceState == null) handleLoginRedirect(intent)

        setContent {
            var appState by remember { mutableStateOf(AppStateStore.load(this@MainActivity)) }
            val updateAppState: ((AppState) -> AppState) -> Unit = remember {
                { transform ->
                    appState = transform(appState)
                    AppStateStore.save(this@MainActivity, appState)
                }
            }

            val darkMode = when (appState.colorMode) {
                2, 5 -> true
                0, 3 -> isSystemInDarkTheme()
                else -> false
            }

            DisposableEffect(darkMode) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkMode },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                onDispose {}
            }

            AppTheme(
                colorMode = appState.colorMode,
                keyColor = keyColorFor(appState.seedIndex),
                paletteStyle = appState.paletteStyle,
                colorSpec = appState.colorSpec,
            ) {
                CompositionLocalProvider(
                    LocalAppState provides appState,
                    LocalUpdateAppState provides updateAppState,
                    LocalSquircleEnabled provides appState.enableSquircle,
                ) {
                    AppContent(padding = PaddingValues(0.dp))
                }
            }
        }
    }
}
