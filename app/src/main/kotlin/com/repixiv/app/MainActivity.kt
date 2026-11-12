// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
import com.repixiv.app.player.PlayerStore
import com.repixiv.app.pixiv.Filters
import com.repixiv.app.pixiv.LocalLists
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.ui.AppTheme
import com.repixiv.app.ui.keyColorFor
import top.yukonga.miuix.kmp.squircle.LocalSquircleEnabled

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Pixiv.init(applicationContext)
        Filters.init(applicationContext)
        LocalLists.init(applicationContext)
        PlayerStore.init(applicationContext)

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
