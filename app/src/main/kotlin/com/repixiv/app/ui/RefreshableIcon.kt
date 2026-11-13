package com.repixiv.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.ceil
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Refresh

/** Shows [icon]; while [spinning] it fades into Miuix's Refresh icon, which rotates, and fades back when done. */
@Composable
fun RefreshableIcon(
    icon: ImageVector,
    spinning: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(spinning) {
        if (spinning) {
            while (true) {
                angle.animateTo(angle.value + 360f, tween(900, easing = LinearEasing))
            }
        } else {
            // finish the current turn so the icon fades out upright
            val end = ceil(angle.value / 360f) * 360f
            if (end > angle.value) angle.animateTo(end, tween(250, easing = LinearEasing))
            angle.snapTo(0f)
        }
    }
    Crossfade(targetState = spinning, animationSpec = tween(300), modifier = modifier, label = "refreshIcon") { s ->
        if (s) {
            Icon(
                imageVector = MiuixIcons.Refresh,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = angle.value },
                tint = tint,
            )
        } else {
            Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
        }
    }
}
