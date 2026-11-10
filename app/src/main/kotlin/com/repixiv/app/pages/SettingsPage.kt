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
import androidx.compose.foundation.lazy.LazyColumn
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
private val BlurStyleOptions = listOf("Gaussian", "Progressive")
private val PagerGestureModeOptions = listOf("Default", "Cross-Axis", "iOS-like")

private const val MIUIX_URL = "https://github.com/compose-miuix-ui/miuix"

@Composable
fun SettingsPage(padding: PaddingValues) {
    val appState = LocalAppState.current
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val topAppBarScrollBehavior = MiuixScrollBehavior()

    val context = LocalContext.current
    val versionText = remember(context) {
        runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "v${info.versionName} (${PackageInfoCompat.getLongVersionCode(info)})"
        }.getOrDefault("")
    }

    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, topAppBarScrollBehavior) {
                AdaptiveTopAppBar(
                    title = "Settings",
                    showTopAppBar = appState.showTopAppBar,
                    scrollBehavior = topAppBarScrollBehavior,
                    color = barColor,
                    subtitle = versionText,
                )
            }
        },
    ) { innerPadding ->
        SettingsContent(
            padding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding(),
            ),
            topAppBarScrollBehavior = topAppBarScrollBehavior,
            backdrop = backdrop,
        )
    }
}

@Composable
private fun SettingsContent(
    padding: PaddingValues,
    topAppBarScrollBehavior: ScrollBehavior,
    backdrop: LayerBackdrop?,
) {
    val appState = LocalAppState.current
    val updateAppState = LocalUpdateAppState.current
    val uriHandler = LocalUriHandler.current
    val lazyListState = rememberLazyListState()

    val contentPadding = pageContentPadding(padding, padding)
    Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.pageScrollModifiers(
                appState.enableScrollEndHaptic,
                appState.showTopAppBar,
                topAppBarScrollBehavior,
            ),
            contentPadding = contentPadding,
        ) {
            item(key = "settingsUi") {
                Card(
                    modifier = Modifier.padding(12.dp),
                ) {
                    SwitchPreference(
                        title = "Show FPS Monitor",
                        checked = appState.showFPSMonitor,
                        onCheckedChange = { updateAppState { state -> state.copy(showFPSMonitor = it) } },
                    )
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
                    AnimatedVisibility(visible = appState.showNavigationBar) {
                        OverlayDropdownPreference(
                            title = "NavigationBar Mode",
                            items = NavigationBarDisplayModeOptions,
                            selectedIndex = appState.navigationBarMode,
                            onSelectedIndexChange = { updateAppState { state -> state.copy(navigationBarMode = it) } },
                        )
                    }
                }
            }
            item(key = "settingsAbout") {
                SmallTitle("Other")
                Card(
                    modifier = Modifier.padding(horizontal = 12.dp),
                ) {
                    ArrowPreference(
                        title = "UI design credits",
                        summary = "Design & components by Miuix (compose-miuix-ui), Apache-2.0",
                        onClick = { uriHandler.openUri(MIUIX_URL) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
        VerticalScrollBar(
            adapter = rememberScrollBarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
            trackPadding = contentPadding,
        )
    }
}
