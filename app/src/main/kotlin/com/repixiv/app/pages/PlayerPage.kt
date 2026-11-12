// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV. Glass controls adapted from AndroidLiquidGlass (Kyant0, Apache-2.0).

package com.repixiv.app.pages

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.player.PlayerController
import com.repixiv.app.player.PlayerState
import com.repixiv.app.player.PlayerStore
import com.repixiv.app.player.AppPlayer
import com.repixiv.app.player.Audius
import com.repixiv.app.player.Track
import com.repixiv.app.ui.bakasu.LiquidGlassButton
import com.repixiv.app.ui.bakasu.LiquidGlassSlider
import com.repixiv.app.util.rememberBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Pause
import top.yukonga.miuix.kmp.icon.extended.Play
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.RotateLeft
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.icon.extended.VolumeUp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private val PlayerTabs = listOf("For You", "Favorites")
private val RepeatCycle = listOf("off", "context", "track")

private val StarFilled: ImageVector by lazy {
    ImageVector.Builder("StarFilled", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2.5f); lineTo(14.9f, 8.7f); lineTo(21.7f, 9.5f); lineTo(16.6f, 14.1f)
            lineTo(18f, 20.8f); lineTo(12f, 17.4f); lineTo(6f, 20.8f); lineTo(7.4f, 14.1f)
            lineTo(2.3f, 9.5f); lineTo(9.1f, 8.7f); close()
        }
    }.build()
}

private val StarOutline: ImageVector by lazy {
    ImageVector.Builder("StarOutline", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineJoin = StrokeJoin.Round) {
            moveTo(12f, 2.5f); lineTo(14.9f, 8.7f); lineTo(21.7f, 9.5f); lineTo(16.6f, 14.1f)
            lineTo(18f, 20.8f); lineTo(12f, 17.4f); lineTo(6f, 20.8f); lineTo(7.4f, 14.1f)
            lineTo(2.3f, 9.5f); lineTo(9.1f, 8.7f); close()
        }
    }.build()
}

@Composable
fun PlayerPage(padding: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var submitted by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Track>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    fun toast(msg: String) = Toast.makeText(context, msg, Toast.LENGTH_LONG).show()

    suspend fun loadTrending() {
        PlayerState.trendingLoading = true
        try {
            val t = Audius.trending()
            PlayerState.trending.clear()
            PlayerState.trending.addAll(t)
            PlayerState.trendingError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            PlayerState.trendingError = e.message ?: "Could not load tracks"
        } finally {
            PlayerState.trendingLoading = false
        }
    }

    fun runSearch() {
        val q = query.text.trim()
        if (q.isEmpty()) return
        submitted = q
        searching = true
        searchError = null
        focusManager.clearFocus()
        keyboard?.hide()
        scope.launch {
            try {
                results = Audius.search(q)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                searchError = e.message ?: "Search failed"
            } finally {
                searching = false
            }
        }
    }

    // "For You": what you play most, then trending tracks to fill it up
    val forYou = (PlayerStore.mostPlayed(30) + PlayerState.trending).distinctBy { it.id }

    fun onTrackTap(t: Track, list: List<Track>) {
        if (PlayerState.current?.id == t.id) {
            PlayerState.optionsOpen = !PlayerState.optionsOpen
            return
        }
        PlayerState.optionsOpen = false
        scope.launch { PlayerController.play(context, t, list)?.let { toast(it) } }
    }

    LaunchedEffect(Unit) {
        if (PlayerState.trending.isEmpty()) loadTrending()
    }
    PlayerState.error?.let { err ->
        LaunchedEffect(err) {
            toast(err)
            PlayerState.error = null
        }
    }

    PageScaffold(
        title = "Player",
        padding = padding,
        alwaysShowBar = true,
        bottomContent = {
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 6.dp)) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    label = "Search music",
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = MiuixIcons.Search,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 12.dp).size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                    modifier = Modifier.fillMaxWidth(),
                )
                TabRowWithContour(
                    tabs = PlayerTabs,
                    selectedTabIndex = tab,
                    onTabSelected = {
                        tab = it
                        submitted = ""
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
    ) { contentPadding, scrollModifier, scrollBehavior ->
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch {
                    try {
                        loadTrending()
                    } finally {
                        refreshing = false
                    }
                }
            },
            pullToRefreshState = pullState,
            contentPadding = contentPadding,
            topAppBarScrollBehavior = scrollBehavior,
        ) {
            LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
                if (submitted.isNotBlank()) {
                    item(key = "resultsHeader") {
                        SectionRow("Results for “$submitted”", "Clear") {
                            submitted = ""
                            query = TextFieldValue("")
                        }
                    }
                    items(results, key = { "res-${it.id}" }) { t -> TrackRow(t) { onTrackTap(t, results) } }
                    item(key = "resultsFooter") {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            when {
                                searching -> InfiniteProgressIndicator()
                                searchError != null -> Text(searchError ?: "", color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center)
                                results.isEmpty() -> Text("No tracks found", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            }
                        }
                    }
                } else if (tab == 0) {
                    items(forYou, key = { "fy-${it.id}" }) { t -> TrackRow(t) { onTrackTap(t, forYou) } }
                    item(key = "forYouFooter") {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            when {
                                PlayerState.trendingLoading -> InfiniteProgressIndicator()
                                PlayerState.trendingError != null -> Text(
                                    PlayerState.trendingError ?: "",
                                    color = MiuixTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                )
                                forYou.isEmpty() -> Text(
                                    "Nothing to play yet. Pull down to refresh.",
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    val favs = PlayerStore.favorites.toList()
                    items(favs, key = { "fav-${it.id}" }) { t -> TrackRow(t) { onTrackTap(t, favs) } }
                    if (favs.isEmpty()) {
                        item(key = "favEmpty") {
                            Text(
                                "Tap the star next to a track to save it here",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- track row

@Composable
private fun TrackRow(track: Track, onTap: () -> Unit) {
    val current = PlayerState.current?.id == track.id
    val playing = current && PlayerState.isPlaying
    val fav = PlayerStore.isFavorite(track.id)
    val nameColor by animateColorAsState(
        if (current) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface,
        tween(700),
        label = "trackName",
    )

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp)) {
                AlbumDisk(track.image, current, playing, Modifier.fillMaxSize())
                TapOverlay(Modifier.matchParentSize(), onTap)
            }
            Column(
                Modifier.weight(1f).clickable { onTap() }.padding(horizontal = 12.dp),
            ) {
                Text(
                    track.title,
                    color = nameColor,
                    fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = { PlayerStore.toggle(track) }) {
                Icon(
                    imageVector = if (fav) StarFilled else StarOutline,
                    contentDescription = if (fav) "Remove from favorites" else "Add to favorites",
                    tint = if (fav) Color(0xFFFFC107) else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        AnimatedVisibility(
            visible = current && PlayerState.optionsOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            PlayerOptions(track)
        }
    }
}

/** Rounded-square cover that turns into a spinning disc with an empty centre while the track is current. */
@Composable
private fun AlbumDisk(url: String, current: Boolean, playing: Boolean, modifier: Modifier) {
    val bmp = rememberBitmap(url)
    val corner by animateFloatAsState(if (current) 50f else 14f, tween(450), label = "diskCorner")
    val hole by animateFloatAsState(if (current) 1f else 0f, tween(600), label = "diskHole")
    val angle = remember { Animatable(0f) }
    LaunchedEffect(current, playing) {
        if (!current) {
            angle.snapTo(0f)
        } else if (playing) {
            while (true) {
                angle.animateTo(angle.value + 360f, tween(8000, easing = LinearEasing))
                angle.snapTo(angle.value % 360f)
            }
        }
    }
    Box(
        modifier
            .graphicsLayer {
                rotationZ = angle.value
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .clip(RoundedCornerShape(percent = corner.roundToInt()))
            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
            .drawWithContent {
                drawContent()
                if (hole > 0.01f) {
                    val r = size.minDimension * 0.12f * hole
                    drawCircle(Color.Black.copy(alpha = 0.25f * hole), radius = r * 1.8f)
                    drawCircle(Color.Black, radius = r, blendMode = BlendMode.Clear)
                }
            },
    ) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

// ---------------------------------------------------------------- options (liquid glass)

private val SpeedSteps = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Composable
private fun PlayerOptions(track: Track) {
    val backdrop = rememberLayerBackdrop()
    val art = rememberBitmap(track.image)
    var volume by remember { mutableFloatStateOf(PlayerState.volume) }
    val playing = PlayerState.isPlaying
    val repeat = PlayerState.repeat
    val speed = PlayerState.speed

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            art?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LiquidGlassButton(
                    onClick = { AppPlayer.togglePause() },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    horizontalPadding = 6.dp,
                ) {
                    Icon(imageVector = if (playing) MiuixIcons.Pause else MiuixIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(if (playing) "Stop" else "Play", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                LiquidGlassButton(
                    onClick = {
                        val next = RepeatCycle[(RepeatCycle.indexOf(repeat).coerceAtLeast(0) + 1) % RepeatCycle.size]
                        AppPlayer.setRepeat(next)
                    },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    tint = if (repeat != "off") MiuixTheme.colorScheme.primary else Color.Unspecified,
                    height = 44.dp,
                    horizontalPadding = 6.dp,
                ) {
                    Icon(imageVector = MiuixIcons.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text(
                        when (repeat) {
                            "track" -> "One"
                            "context" -> "All"
                            else -> "Repeat"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
                LiquidGlassButton(
                    onClick = {
                        val i = SpeedSteps.indexOfFirst { it >= speed - 0.001f }.coerceAtLeast(0)
                        AppPlayer.setSpeed(SpeedSteps[(i + 1) % SpeedSteps.size])
                    },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    tint = if (speed != 1f) MiuixTheme.colorScheme.primary else Color.Unspecified,
                    height = 44.dp,
                    horizontalPadding = 6.dp,
                ) {
                    Icon(imageVector = MiuixIcons.Stopwatch, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text("${"%.2f".format(speed).trimEnd('0').trimEnd('.')}x", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
                LiquidGlassButton(
                    onClick = { AppPlayer.restart() },
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f),
                    height = 44.dp,
                    horizontalPadding = 6.dp,
                ) {
                    Icon(imageVector = MiuixIcons.RotateLeft, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Text("Restart", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = MiuixIcons.VolumeOff, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
                LiquidGlassSlider(
                    value = { volume },
                    onValueChange = {
                        volume = it
                        AppPlayer.setVolume(it)
                    },
                    onValueChangeFinished = { v ->
                        volume = v
                        AppPlayer.setVolume(v)
                    },
                    valueRange = 0f..100f,
                    visibilityThreshold = 0.1f,
                    backdrop = backdrop,
                    modifier = Modifier.weight(1f).padding(horizontal = 14.dp),
                )
                Icon(imageVector = MiuixIcons.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color.White)
            }
        }
    }
}
