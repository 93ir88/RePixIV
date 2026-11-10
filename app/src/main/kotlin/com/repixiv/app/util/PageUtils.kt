// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.util

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.repixiv.app.LocalAppState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

fun Modifier.pageScrollModifiers(
    enableScrollEndHaptic: Boolean,
    showTopAppBar: Boolean,
    topAppBarScrollBehavior: ScrollBehavior,
): Modifier = this
    .then(if (enableScrollEndHaptic) Modifier.scrollEndHaptic() else Modifier)
    .overScrollVertical()
    .then(if (showTopAppBar) Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection) else Modifier)
    .fillMaxHeight()

@Composable
fun pageContentPadding(innerPadding: PaddingValues, outerPadding: PaddingValues): PaddingValues {
    val top = innerPadding.calculateTopPadding()
    val bottom = outerPadding.calculateBottomPadding()
    return remember(top, bottom) { PaddingValues(top = top, bottom = bottom) }
}

@Composable
fun AdaptiveTopAppBar(
    title: String,
    showTopAppBar: Boolean,
    scrollBehavior: ScrollBehavior,
    subtitle: String = "",
    color: Color = MiuixTheme.colorScheme.surface,
    actions: @Composable RowScope.() -> Unit = {},
) {
    if (!showTopAppBar) return
    TopAppBar(
        title = title,
        subtitle = subtitle,
        color = color,
        scrollBehavior = scrollBehavior,
        actions = actions,
    )
}

@Composable
fun rememberBlurBackdrop(): LayerBackdrop? {
    val appState = LocalAppState.current
    if (!appState.enableBlur || !isRuntimeShaderSupported()) return null
    val surfaceColor = MiuixTheme.colorScheme.surface
    return rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }
}

@Composable
fun BlurredBar(
    backdrop: LayerBackdrop?,
    blurEnabled: Boolean,
    scrollBehavior: ScrollBehavior? = null,
    content: @Composable () -> Unit,
) {
    val progressive = LocalAppState.current.blurStyle == 1
    val blurActive = blurEnabled && backdrop != null
    Box(
        modifier = if (blurActive && !progressive) {
            Modifier.textureBlur(
                backdrop = backdrop,
                shape = RectangleShape,
                blurRadius = 25f,
                colors = barBlurColors(),
            )
        } else {
            Modifier
        },
    ) {
        if (blurActive && progressive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = scrollBehavior?.state
                            ?.let { (-it.contentOffset / 48.dp.toPx()).coerceIn(0f, 1f) }
                            ?: 1f
                    }
                    .progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        gradient = ProgressiveBlur.Top.copy(curve = 2.2f),
                        blurRadius = 10f,
                        colors = barBlurColors(progressive = true),
                    ),
            )
        }
        content()
    }
}

@Composable
fun barBlurColors(progressive: Boolean = false): BlurColors = BlurDefaults.blurColors(
    blendColors = listOf(
        BlendColorEntry(color = MiuixTheme.colorScheme.surface.copy(if (progressive) 0.3f else 0.8f)),
    ),
)
