// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

@file:OptIn(ExperimentalScrollBarApi::class)

package com.repixiv.app.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.repixiv.app.LocalAppState
import com.repixiv.app.LocalUpdateAppState
import com.repixiv.app.ui.KeyColors
import com.repixiv.app.util.AdaptiveTopAppBar
import com.repixiv.app.util.BlurredBar
import com.repixiv.app.util.pageContentPadding
import com.repixiv.app.util.pageScrollModifiers
import com.repixiv.app.util.rememberBlurBackdrop
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.VerticalScrollBar
import top.yukonga.miuix.kmp.basic.rememberScrollBarAdapter
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.interfaces.ExperimentalScrollBarApi
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle

private val NavigationBarDisplayModeOptions = listOf("IconAndText", "IconOnly", "IconWithSelectedLabel")
private val ColorModeOptions = listOf("System", "Light", "Dark", "MonetSystem", "MonetLight", "MonetDark")
private val PaletteStyleOptions = ThemePaletteStyle.entries.map { it.name }
private val ColorSpecOptions = ThemeColorSpec.entries.map { it.name }
private val KeyColorOptions = listOf("Default") + KeyColors.map { it.first }
private val FloatingNavigationBarPositionOptions = listOf("Center", "Start", "End")
private val BlurStyleOptions = listOf("Gaussian", "Progressive")
private val LowResOptions = listOf("Disabled", "144p (Ultra Low)", "480p", "720p")
private val PagerGestureModeOptions = listOf("Default", "Cross-Axis", "iOS-like")

private const val MIUIX_URL = "https://github.com/compose-miuix-ui/miuix"
private const val BAKASU_URL = "https://github.com/Baka-SU/BakaSU"
private const val LIQUID_GLASS_URL = "https://github.com/Kyant0/AndroidLiquidGlass"

/** The app-wide settings, shown as the "RePixIV settings" category inside Settings. */
internal fun LazyListScope.repixivSettingsItems() {
    item(key = "settingsTitle") { SmallTitle("RePixIV settings") }
    item(key = "settingsUi") {
        val appState = LocalAppState.current
        val updateAppState = LocalUpdateAppState.current
        Card(
            modifier = Modifier.padding(12.dp),
        ) {
            OverlayDropdownPreference(
                title = "Color Mode",
                items = ColorModeOptions,
                selectedIndex = appState.colorMode,
                onSelectedIndexChange = { updateAppState { state -> state.copy(colorMode = it) } },
            )
            AnimatedVisibility(visible = appState.colorMode in 3..5) {
                OverlayDropdownPreference(
                    title = "Key Color",
                    items = KeyColorOptions,
                    selectedIndex = appState.seedIndex,
                    onSelectedIndexChange = { updateAppState { state -> state.copy(seedIndex = it) } },
                )
            }
            AnimatedVisibility(visible = appState.colorMode in 3..5 && appState.seedIndex > 0) {
                Column {
                    OverlayDropdownPreference(
                        title = "Palette Style",
                        items = PaletteStyleOptions,
                        selectedIndex = appState.paletteStyle,
                        onSelectedIndexChange = { updateAppState { state -> state.copy(paletteStyle = it) } },
                    )
                    OverlayDropdownPreference(
                        title = "Color Spec",
                        items = ColorSpecOptions,
                        selectedIndex = appState.colorSpec,
                        onSelectedIndexChange = { updateAppState { state -> state.copy(colorSpec = it) } },
                    )
                }
            }
            AnimatedVisibility(visible = isRuntimeShaderSupported()) {
                SwitchPreference(
                    title = "Enable Squircle Shapes",
                    checked = appState.enableSquircle,
                    onCheckedChange = { updateAppState { state -> state.copy(enableSquircle = it) } },
                )
            }
            AnimatedVisibility(visible = isRuntimeShaderSupported()) {
                SwitchPreference(
                    title = "Enable Blur Effect",
                    checked = appState.enableBlur,
                    onCheckedChange = { updateAppState { state -> state.copy(enableBlur = it) } },
                )
            }
            SwitchPreference(
                title = "Enable Scroll End Haptic",
                checked = appState.enableScrollEndHaptic,
                onCheckedChange = { updateAppState { state -> state.copy(enableScrollEndHaptic = it) } },
            )
            SwitchPreference(
                title = "Enable Page User Scroll",
                checked = appState.enablePageUserScroll,
                onCheckedChange = { updateAppState { state -> state.copy(enablePageUserScroll = it) } },
            )
            AnimatedVisibility(visible = appState.enablePageUserScroll) {
                OverlayDropdownPreference(
                    title = "Pager Gesture Mode",
                    items = PagerGestureModeOptions,
                    selectedIndex = appState.pagerInterceptionMode,
                    onSelectedIndexChange = { updateAppState { state -> state.copy(pagerInterceptionMode = it) } },
                )
            }
            OverlayDropdownPreference(
                title = "Low-res mode",
                summary = "Caps image resolution to save data",
                items = LowResOptions,
                selectedIndex = appState.lowRes,
                onSelectedIndexChange = { updateAppState { state -> state.copy(lowRes = it) } },
            )
            SwitchPreference(
                title = "Show TopAppBar",
                checked = appState.showTopAppBar,
                onCheckedChange = { updateAppState { state -> state.copy(showTopAppBar = it) } },
            )
            AnimatedVisibility(visible = appState.showTopAppBar && appState.enableBlur && isRuntimeShaderSupported()) {
                OverlayDropdownPreference(
                    title = "TopAppBar Blur Style",
                    items = BlurStyleOptions,
                    selectedIndex = appState.blurStyle,
                    onSelectedIndexChange = { updateAppState { state -> state.copy(blurStyle = it) } },
                )
            }
            SwitchPreference(
                title = "Show NavigationBar",
                checked = appState.showNavigationBar,
                onCheckedChange = { updateAppState { state -> state.copy(showNavigationBar = it) } },
            )
            AnimatedVisibility(visible = appState.showNavigationBar) {
                SwitchPreference(
                    title = "Show Navigation Badge",
                    checked = appState.showNavigationBarBadge,
                    onCheckedChange = { updateAppState { state -> state.copy(showNavigationBarBadge = it) } },
                )
            }
            AnimatedVisibility(visible = appState.showNavigationBar && !appState.useFloatingNavigationBar) {
                OverlayDropdownPreference(
                    title = "NavigationBar Mode",
                    items = NavigationBarDisplayModeOptions,
                    selectedIndex = appState.navigationBarMode,
                    onSelectedIndexChange = { updateAppState { state -> state.copy(navigationBarMode = it) } },
                )
            }
            AnimatedVisibility(visible = appState.showNavigationBar) {
                Column {
                    SwitchPreference(
                        title = "Use FloatingNavigationBar",
                        summary = "Liquid-glass floating bar from BakaSU",
                        checked = appState.useFloatingNavigationBar,
                        onCheckedChange = { updateAppState { state -> state.copy(useFloatingNavigationBar = it) } },
                    )
                    AnimatedVisibility(visible = appState.useFloatingNavigationBar) {
                        OverlayDropdownPreference(
                            title = "FloatingNavigationBar Position",
                            items = FloatingNavigationBarPositionOptions,
                            selectedIndex = appState.floatingNavigationBarPosition,
                            onSelectedIndexChange = { updateAppState { state -> state.copy(floatingNavigationBarPosition = it) } },
                        )
                    }
                }
            }
        }
    }
    item(key = "settingsAbout") {
        val uriHandler = LocalUriHandler.current
        SmallTitle("Other")
        Card(
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            ArrowPreference(
                title = "UI design credits",
                summary = "Design & components by Miuix (compose-miuix-ui), Apache-2.0",
                onClick = { uriHandler.openUri(MIUIX_URL) },
            )
            ArrowPreference(
                title = "Player controls credits",
                summary = "Liquid-glass controls from Kyant's AndroidLiquidGlass (Apache-2.0)",
                onClick = { uriHandler.openUri(LIQUID_GLASS_URL) },
            )
            ArrowPreference(
                title = "Floating bar credits",
                summary = "Liquid-glass bar by BakaSU (GPL-3.0), based on Miuix and Kyant0's AndroidLiquidGlass",
                onClick = { uriHandler.openUri(BAKASU_URL) },
            )
        }
    }

}
