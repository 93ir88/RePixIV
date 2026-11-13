// Dialog style adapted from the AndroidLiquidGlass catalog (DialogContent):
// https://github.com/Kyant0/AndroidLiquidGlass, Copyright Kyant, Apache-2.0.
// Ported to Miuix's blur backdrop for RePixIV.

package com.repixiv.app.ui.bakasu

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.player.AppPlayer
import com.repixiv.app.player.PlayerState
import com.repixiv.app.ui.bakasu.liquid.lens
import com.repixiv.app.ui.bakasu.liquid.vibrancy
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private val Accent = Color(0xFF0091FF)

private fun fmt(totalSec: Int): String = "%d:%02d".format(totalSec / 60, totalSec % 60)

/**
 * "Jump to" dialog for the player: a Backdrop-style glass card that fades in over a blurred, dimmed screen.
 * Shows the full track name, minute / second inputs with a Jump button, a glass seek slider and one long Close button.
 * Put it on top of everything that should be blurred, as a sibling of the content that owns [backdrop].
 */
@Composable
fun JumpToDialog(backdrop: Backdrop) {
    val track = PlayerState.current
    val open = PlayerState.jumpDialogOpen && track != null
    val progress by animateFloatAsState(if (open) 1f else 0f, tween(280), label = "jumpDialog")
    if (!open && progress <= 0.001f) return

    val close = { PlayerState.jumpDialogOpen = false }
    BackHandler(enabled = open) { close() }

    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    val contentColor = if (dark) Color.White else Color.Black
    val containerColor = if (dark) Color(0xFF121212).copy(0.4f) else Color(0xFFFAFAFA).copy(0.6f)
    val dimColor = if (dark) Color(0xFF121212).copy(0.56f) else Color(0xFF29293A).copy(0.23f)
    val glass = isRuntimeShaderSupported()

    // the sheet's state starts from where the song is now each time it opens
    val startSec = remember(track?.id) { PlayerState.positionSec.toInt() }
    val maxSec = PlayerState.durationSec.toInt().coerceAtLeast(1)
    var picked by remember { mutableFloatStateOf(startSec.toFloat().coerceIn(0f, maxSec.toFloat())) }
    var minText by remember { mutableStateOf((startSec / 60).toString()) }
    var secText by remember { mutableStateOf((startSec % 60).toString()) }

    fun setPicked(total: Int) {
        val t = total.coerceIn(0, maxSec)
        picked = t.toFloat()
        minText = (t / 60).toString()
        secText = (t % 60).toString()
    }

    fun typedTotal(): Int = (minText.toIntOrNull() ?: 0) * 60 + (secText.toIntOrNull() ?: 0)

    fun jump() {
        val t = typedTotal().coerceIn(0, maxSec)
        AppPlayer.seekTo(t.toFloat())
        close()
    }

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = progress },
    ) {
        // blurred + dimmed screen behind the dialog; tapping it closes the dialog
        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (glass) {
                        Modifier.drawBackdrop(
                            backdrop = backdrop,
                            shape = { RectangleShape },
                            effects = { blur(14.dp.toPx(), 14.dp.toPx()) },
                            onDrawSurface = { drawRect(dimColor) },
                        )
                    } else {
                        Modifier.background(dimColor.copy(alpha = 0.8f))
                    },
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = close,
                ),
        )

        Box(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .graphicsLayer {
                        val s = 0.92f + 0.08f * progress
                        scaleX = s
                        scaleY = s
                    }
                    .then(
                        if (glass) {
                            Modifier.drawBackdrop(
                                backdrop = backdrop,
                                shape = { RoundedCornerShape(40.dp) },
                                effects = {
                                    vibrancy()
                                    blur(if (dark) 8.dp.toPx() else 16.dp.toPx(), if (dark) 8.dp.toPx() else 16.dp.toPx())
                                    lens(refractionHeight = 24.dp.toPx(), refractionAmount = 48.dp.toPx())
                                },
                                onDrawSurface = { drawRect(containerColor) },
                            )
                        } else {
                            Modifier.background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(40.dp))
                        },
                    )
                    // swallow taps so they don't reach the scrim
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .fillMaxWidth(),
            ) {
                // full music name, never cut off
                Text(
                    track?.title.orEmpty(),
                    color = contentColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(28.dp, 24.dp, 28.dp, 4.dp),
                )
                if (!track?.artist.isNullOrBlank()) {
                    Text(
                        track?.artist.orEmpty(),
                        color = contentColor.copy(0.68f),
                        fontSize = 15.sp,
                        modifier = Modifier.padding(28.dp, 0.dp, 28.dp, 4.dp),
                    )
                }

                // minute box, second box, blue Jump button
                Row(
                    Modifier.padding(24.dp, 16.dp, 24.dp, 0.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeBox(
                        value = minText,
                        placeholder = "min",
                        color = contentColor,
                        modifier = Modifier.weight(1f),
                        onChange = {
                            minText = it
                            picked = typedTotal().coerceIn(0, maxSec).toFloat()
                        },
                        onDone = { jump() },
                    )
                    Text(":", color = contentColor, fontSize = 20.sp)
                    TimeBox(
                        value = secText,
                        placeholder = "sec",
                        color = contentColor,
                        modifier = Modifier.weight(1f),
                        onChange = {
                            secText = it
                            picked = typedTotal().coerceIn(0, maxSec).toFloat()
                        },
                        onDone = { jump() },
                    )
                    LiquidGlassButton(
                        onClick = { jump() },
                        backdrop = backdrop,
                        modifier = Modifier.weight(1.2f),
                        tint = Accent,
                        height = 48.dp,
                        horizontalPadding = 12.dp,
                    ) {
                        Text("Jump", color = Color.White, fontSize = 16.sp, maxLines = 1, softWrap = false)
                    }
                }

                // seek slider (glass), like a video time bar
                Column(Modifier.padding(24.dp, 20.dp, 24.dp, 0.dp)) {
                    LiquidGlassSlider(
                        value = { picked },
                        onValueChange = { setPicked(it.roundToInt()) },
                        onValueChangeFinished = { setPicked(it.roundToInt()) },
                        valueRange = 0f..maxSec.toFloat(),
                        visibilityThreshold = 0.5f,
                        backdrop = backdrop,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(fmt(picked.roundToInt()), color = contentColor.copy(0.68f), fontSize = 13.sp)
                        Text(
                            if (PlayerState.durationSec > 0f) fmt(maxSec) else "--:--",
                            color = contentColor.copy(0.68f),
                            fontSize = 13.sp,
                        )
                    }
                }

                // one long Close button
                LiquidGlassButton(
                    onClick = close,
                    backdrop = backdrop,
                    modifier = Modifier.padding(24.dp, 16.dp, 24.dp, 24.dp).fillMaxWidth(),
                    height = 48.dp,
                ) {
                    Text("Close", color = contentColor, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun TimeBox(
    value: String,
    placeholder: String,
    color: Color,
    modifier: Modifier,
    onChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    BasicTextField(
        value = value,
        onValueChange = { s -> onChange(s.filter { it.isDigit() }.take(3)) },
        singleLine = true,
        textStyle = TextStyle(color = color, fontSize = 16.sp, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                Modifier.height(48.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) Text(placeholder, color = color.copy(0.45f), fontSize = 16.sp)
                inner()
            }
        },
    )
}
