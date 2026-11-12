// Adapted from the AndroidLiquidGlass catalog (LiquidButton, LiquidSlider):
// https://github.com/Kyant0/AndroidLiquidGlass, Copyright Kyant, Apache-2.0.
// Ported to Miuix's blur backdrop (https://github.com/compose-miuix-ui/miuix, Apache-2.0) for RePixIV.

package com.repixiv.app.ui.bakasu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtMost
import androidx.compose.ui.util.fastCoerceIn
import androidx.compose.ui.util.fastRoundToInt
import androidx.compose.ui.util.lerp
import com.repixiv.app.ui.bakasu.animation.DampedDragAnimation
import com.repixiv.app.ui.bakasu.animation.InteractiveHighlight
import com.repixiv.app.ui.bakasu.liquid.lens
import com.repixiv.app.ui.bakasu.liquid.rememberCombinedBackdrop
import com.repixiv.app.ui.bakasu.liquid.vibrancy
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tanh

@Composable
fun LiquidGlassButton(
    onClick: () -> Unit,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
    height: Dp = 48.dp,
    horizontalPadding: Dp = 16.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val animationScope = rememberCoroutineScope()
    val interactiveHighlight = remember(animationScope) { InteractiveHighlight(animationScope = animationScope) }
    val glass = isRuntimeShaderSupported()
    val fallback = MiuixTheme.colorScheme.surfaceContainerHigh

    Row(
        modifier
            .then(
                if (glass) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(2.dp.toPx(), 2.dp.toPx())
                            lens(refractionHeight = 12.dp.toPx(), refractionAmount = 24.dp.toPx())
                        },
                        layerBlock = {
                            val width = size.width
                            val h = size.height
                            val progress = interactiveHighlight.pressProgress
                            val scale = lerp(1f, 1f + 4.dp.toPx() / size.height, progress)
                            val maxOffset = size.minDimension
                            val initialDerivative = 0.05f
                            val offset = interactiveHighlight.offset
                            translationX = maxOffset * tanh(initialDerivative * offset.x / maxOffset)
                            translationY = maxOffset * tanh(initialDerivative * offset.y / maxOffset)
                            val maxDragScale = 4.dp.toPx() / size.height
                            val offsetAngle = atan2(offset.y, offset.x)
                            scaleX = scale + maxDragScale * abs(cos(offsetAngle) * offset.x / size.maxDimension) *
                                (width / h).fastCoerceAtMost(1f)
                            scaleY = scale + maxDragScale * abs(sin(offsetAngle) * offset.y / size.maxDimension) *
                                (h / width).fastCoerceAtMost(1f)
                        },
                        onDrawSurface = {
                            if (tint.isSpecified) {
                                drawRect(tint, blendMode = BlendMode.Hue)
                                drawRect(tint.copy(alpha = 0.75f))
                            }
                            drawRect(Color.White.copy(alpha = 0.06f))
                        },
                    )
                } else {
                    Modifier.background(fallback, CircleShape)
                },
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .then(if (glass) interactiveHighlight.modifier.then(interactiveHighlight.gestureModifier) else Modifier)
            .height(height)
            .padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun LiquidGlassSlider(
    value: () -> Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    visibilityThreshold: Float,
    backdrop: Backdrop,
    modifier: Modifier = Modifier,
) {
    val accentColor = MiuixTheme.colorScheme.primary
    val trackColor = Color.White.copy(alpha = 0.3f)
    val trackBackdrop = rememberLayerBackdrop()
    val glass = isRuntimeShaderSupported()

    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        val trackWidth = constraints.maxWidth
        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        val animationScope = rememberCoroutineScope()
        var didDrag by remember { mutableStateOf(false) }
        val dampedDragAnimation = remember(animationScope) {
            DampedDragAnimation(
                animationScope = animationScope,
                initialValue = value(),
                valueRange = valueRange,
                visibilityThreshold = visibilityThreshold,
                initialScale = 1f,
                pressedScale = 1.5f,
                consumeDrag = true, // dragging the thumb must not swipe pages
                onDragStarted = {},
                onDragStopped = {
                    if (didDrag) onValueChangeFinished(targetValue)
                    didDrag = false
                },
                onDrag = { _, dragAmount ->
                    if (!didDrag) didDrag = dragAmount.x != 0f
                    val delta = (valueRange.endInclusive - valueRange.start) * (dragAmount.x / trackWidth)
                    onValueChange(
                        if (isLtr) (targetValue + delta).coerceIn(valueRange) else (targetValue - delta).coerceIn(valueRange),
                    )
                },
            )
        }
        LaunchedEffect(dampedDragAnimation) {
            snapshotFlow { value() }.collectLatest { v ->
                if (dampedDragAnimation.targetValue != v) dampedDragAnimation.updateValue(v)
            }
        }
        val progressFraction = {
            val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1e-6f)
            ((dampedDragAnimation.value - valueRange.start) / span).fastCoerceIn(0f, 1f)
        }

        Box(Modifier.layerBackdrop(trackBackdrop)) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(trackColor)
                    .pointerInput(animationScope) {
                        detectTapGestures { position ->
                            val delta = (valueRange.endInclusive - valueRange.start) * (position.x / trackWidth)
                            val target = (if (isLtr) valueRange.start + delta else valueRange.endInclusive - delta)
                                .coerceIn(valueRange)
                            dampedDragAnimation.animateToValue(target)
                            onValueChange(target)
                            onValueChangeFinished(target)
                        }
                    }
                    .height(6.dp)
                    .fillMaxWidth(),
            )
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(accentColor)
                    .height(6.dp)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = (constraints.maxWidth * progressFraction()).fastRoundToInt()
                        layout(width, placeable.height) { placeable.place(0, 0) }
                    },
            )
        }

        Box(
            Modifier
                .graphicsLayer {
                    translationX = (-size.width / 2f + trackWidth * progressFraction())
                        .fastCoerceIn(-size.width / 4f, trackWidth - size.width * 3f / 4f) * if (isLtr) 1f else -1f
                }
                .then(dampedDragAnimation.modifier)
                .then(
                    if (glass) {
                        Modifier.drawBackdrop(
                            backdrop = rememberCombinedBackdrop(backdrop, trackBackdrop),
                            shape = { CircleShape },
                            effects = {
                                val p = dampedDragAnimation.pressProgress
                                blur(8.dp.toPx() * (1f - p), 8.dp.toPx() * (1f - p))
                                lens(refractionHeight = 10.dp.toPx() * p, refractionAmount = 14.dp.toPx() * p)
                            },
                            layerBlock = {
                                scaleX = dampedDragAnimation.scaleX
                                scaleY = dampedDragAnimation.scaleY
                                val velocity = dampedDragAnimation.velocity / 10f
                                scaleX /= 1f - (velocity * 0.75f).fastCoerceIn(-0.2f, 0.2f)
                                scaleY *= 1f - (velocity * 0.25f).fastCoerceIn(-0.2f, 0.2f)
                            },
                            onDrawSurface = {
                                drawRect(Color.White.copy(alpha = 1f - dampedDragAnimation.pressProgress))
                            },
                        )
                    } else {
                        Modifier.background(Color.White, CircleShape)
                    },
                )
                .size(40.dp, 24.dp),
        )
    }
}
