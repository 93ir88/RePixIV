// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import com.repixiv.app.pixiv.Follows
import top.yukonga.miuix.kmp.icon.extended.Copy
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.repixiv.app.pixiv.CommentPreviews
import com.repixiv.app.pixiv.Likes
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.icon.extended.FavoritesFill
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalAppState
import com.repixiv.app.LocalNavigator
import com.repixiv.app.Screen
import com.repixiv.app.pixiv.Downloader
import com.repixiv.app.LoginActivity
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.Session
import com.repixiv.app.pixiv.UserDetail
import com.repixiv.app.util.AdaptiveTopAppBar
import com.repixiv.app.util.BlurredBar
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.pageContentPadding
import com.repixiv.app.util.pageScrollModifiers
import com.repixiv.app.util.rememberBlurBackdrop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Image
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PageScaffold(
    title: String,
    padding: PaddingValues,
    actions: @Composable RowScope.() -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    bottomContent: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    overlay: Boolean = false,
    alwaysShowBar: Boolean = false,
    content: @Composable (PaddingValues, Modifier, ScrollBehavior) -> Unit,
) {
    val appState = LocalAppState.current
    val showBar = overlay || alwaysShowBar || appState.showTopAppBar
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, scrollBehavior) {
                AdaptiveTopAppBar(
                    title = title,
                    showTopAppBar = showBar,
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    navigationIcon = navigationIcon,
                    bottomContent = bottomContent,
                    actions = actions,
                )
            }
        },
        bottomBar = bottomBar,
    ) { inner ->
        val contentPadding = pageContentPadding(inner, if (overlay) inner else padding)
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            content(
                contentPadding,
                Modifier.pageScrollModifiers(appState.enableScrollEndHaptic, showBar, scrollBehavior),
                scrollBehavior,
            )
        }
    }
}

// ---------------------------------------------------------------- shared pieces

@Composable
internal fun LoginPrompt(message: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showToken by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf(TextFieldValue("")) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        Text(
            message,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = { context.startActivity(Intent(context, LoginActivity::class.java)) },
            colors = ButtonDefaults.buttonColorsPrimary(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log in with pixiv", color = MiuixTheme.colorScheme.onPrimary)
        }
        TextButton(
            text = if (showToken) "Hide refresh token login" else "Use a refresh token instead",
            onClick = { showToken = !showToken },
            modifier = Modifier.fillMaxWidth(),
        )
        if (showToken) {
            TextField(
                value = token,
                onValueChange = { token = it },
                label = "Refresh token",
                useLabelAsPlaceholder = true,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = {
                    if (token.text.isBlank() || busy) return@Button
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            Pixiv.loginWithRefreshToken(token.text)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            error = e.message ?: "Login failed"
                        } finally {
                            busy = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (busy) "Signing in…" else "Sign in")
            }
        }
        error?.let { Text(it, color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center) }
    }
}

internal fun fmt(n: Int): String = when {
    n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)}M"
    n >= 1_000 -> "${"%.1f".format(n / 1_000.0)}K"
    else -> n.toString()
}

@Composable
internal fun TapOverlay(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )
}

@Composable
internal fun LikeButton(a: Illust, modifier: Modifier = Modifier, idleTint: Color = MiuixTheme.colorScheme.onSurface) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val liked = Likes.isLiked(a)
    val pop = remember { Animatable(1f) }
    var previous by remember { mutableStateOf(liked) }
    LaunchedEffect(liked) {
        if (liked && !previous) {
            pop.snapTo(0.6f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 380f))
        }
        previous = liked
    }
    IconButton(
        modifier = modifier,
        onClick = {
            scope.launch {
                try {
                    Likes.toggle(a)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Could not update like", Toast.LENGTH_SHORT).show()
                }
            }
        },
    ) {
        Icon(
            imageVector = if (liked) MiuixIcons.FavoritesFill else MiuixIcons.Favorites,
            contentDescription = if (liked) "Unlike" else "Like",
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
            tint = if (liked) Color(0xFFFF4060) else idleTint,
        )
    }
}

@Composable
private fun CommentsPreview(a: Illust) {
    val nav = LocalNavigator.current
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    LaunchedEffect(a.id) { if (a.totalComments > 0) CommentPreviews.load(a.id) }
    val preview = CommentPreviews.cache[a.id]
    Card(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        onClick = { nav.push(Screen.Comments(a)) },
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Comments", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    a.totalComments.toString(),
                    fontSize = 12.sp,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .background(MiuixTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                )
            }
            if (preview != null) {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    RemoteImage(preview.userAvatar, true, Modifier.size(24.dp).clip(CircleShape))
                    Text(
                        if (preview.stampUrl != null) "[stamp]" else preview.text,
                        color = summary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else if (a.totalComments == 0) {
                Text("Be the first to comment", color = summary, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
internal fun IllustCard(a: Illust, onNotInterested: () -> Unit) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val tint = MiuixTheme.colorScheme.onSurface
    var showMenu by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape)) {
                RemoteImage(a.userAvatar, true, Modifier.fillMaxSize())
                TapOverlay(Modifier.matchParentSize()) { nav.openUser(a.userId) }
            }
            Text(
                a.userName,
                style = MiuixTheme.textStyles.body1,
                fontSize = 15.sp,
                maxLines = 1,
                modifier = Modifier.padding(start = 10.dp).weight(1f).clickable { nav.openUser(a.userId) },
            )
            PostMenuButton(a, onNotInterested)
        }

        Box(Modifier.fillMaxWidth()) {
            RemoteImage(
                a.imageUrl,
                false,
                Modifier.fillMaxWidth().aspectRatio((a.width.toFloat() / a.height).coerceIn(0.4f, 3f)),
                crop = false,
            )
            TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Post(a)) }
            if (a.pageCount > 1) {
                Text(
                    "1/${a.pageCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f).padding(top = 6.dp)) {
                Text(a.title, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (a.tags.isNotEmpty()) {
                    Text(
                        a.tags.take(5).joinToString("  ") { "#$it" },
                        color = summary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    "${fmt(Likes.count(a))} likes  ·  ${fmt(a.views)} views",
                    color = summary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            LikeButton(a)
        }
        CommentsPreview(a)
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(MiuixTheme.colorScheme.dividerLine))
    }
}

@Composable
private fun CollectionTile(work: Illust, modifier: Modifier) {
    val nav = LocalNavigator.current
    Box(modifier.clip(RoundedCornerShape(12.dp))) {
        RemoteImage(work.imageUrl, false, Modifier.fillMaxSize(), crop = true)
        TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Post(work)) }
    }
}

/** "Collections by X": a mosaic of the artist's other works (like pixiv's home feed). */
@Composable
internal fun CollectionBlock(userId: Long, userName: String) {
    val nav = LocalNavigator.current
    var works by remember(userId) { mutableStateOf<List<Illust>>(emptyList()) }
    LaunchedEffect(userId) {
        try {
            works = Pixiv.userIllusts(userId).items.take(5)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }
    if (works.size < 3) return
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(
            "Collections by $userName",
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().clickable { nav.openUser(userId) }.padding(horizontal = 12.dp, vertical = 12.dp),
        )
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth().height(250.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CollectionTile(works[0], Modifier.weight(2f).fillMaxHeight())
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CollectionTile(works[1], Modifier.weight(1f).fillMaxWidth())
                    CollectionTile(works[2], Modifier.weight(1f).fillMaxWidth())
                }
            }
            if (works.size >= 5) {
                Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CollectionTile(works[3], Modifier.weight(1f).fillMaxHeight())
                    CollectionTile(works[4], Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(0.5.dp).background(MiuixTheme.colorScheme.dividerLine))
    }
}

internal fun shareIllust(context: android.content.Context, a: Illust) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${a.title} by ${a.userName}\nhttps://www.pixiv.net/artworks/${a.id}")
    }
    context.startActivity(Intent.createChooser(send, "Share"))
}

/** Three-dots menu shared by feed cards and the post page. */
@Composable
internal fun PostMenuButton(a: Illust, onNotInterested: () -> Unit) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tint = MiuixTheme.colorScheme.onSurface
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(imageVector = MiuixIcons.More, contentDescription = "More", tint = tint)
        }
        val entries = listOf<Pair<DropdownItem, () -> Unit>>(
            DropdownItem(
                text = "Download",
                icon = { m -> Icon(imageVector = MiuixIcons.Download, contentDescription = null, modifier = m, tint = tint) },
            ) to {
                Toast.makeText(context, "Downloading…", Toast.LENGTH_SHORT).show()
                scope.launch { Toast.makeText(context, Downloader.save(context, a), Toast.LENGTH_LONG).show() }
                Unit
            },
            DropdownItem(
                text = "View full screen",
                icon = { m -> Icon(imageVector = MiuixIcons.Image, contentDescription = null, modifier = m, tint = tint) },
            ) to { nav.push(Screen.Viewer(a)) },
            DropdownItem(
                text = "Not interested",
                icon = { m -> Icon(imageVector = MiuixIcons.Hide, contentDescription = null, modifier = m, tint = tint) },
            ) to {
                onNotInterested()
                Toast.makeText(context, "Got it, you'll see less like this", Toast.LENGTH_SHORT).show()
            },
            DropdownItem(
                text = "Share",
                icon = { m -> Icon(imageVector = MiuixIcons.Share, contentDescription = null, modifier = m, tint = tint) },
            ) to { shareIllust(context, a) },
        )
        OverlayListPopup(
            show = showMenu,
            alignment = PopupPositionProvider.Align.End,
            onDismissRequest = { showMenu = false },
        ) {
            ListPopupColumn {
                entries.forEachIndexed { index, (item, action) ->
                    DropdownImpl(
                        item = item,
                        optionSize = entries.size,
                        isSelected = false,
                        index = index,
                        onSelectedIndexChange = {
                            showMenu = false
                            action()
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun FollowButton(
    userId: Long,
    defaultFollowed: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    if (Pixiv.session?.userId == userId) return
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val following = Follows.isFollowing(userId, defaultFollowed)
    var busy by remember { mutableStateOf(false) }
    Button(
        onClick = {
            if (busy) return@Button
            busy = true
            scope.launch {
                try {
                    Follows.toggle(userId, following)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Toast.makeText(context, e.message ?: "Request failed", Toast.LENGTH_SHORT).show()
                } finally {
                    busy = false
                }
            }
        },
        modifier = modifier,
        cornerRadius = 50.dp,
        minHeight = if (compact) 32.dp else 44.dp,
        insideMargin = PaddingValues(horizontal = if (compact) 14.dp else 24.dp, vertical = 0.dp),
        colors = if (following) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary(),
    ) {
        Text(
            if (following) "Following" else "Follow",
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (following) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
internal fun PagedFooter(
    loading: Boolean,
    error: String?,
    hasNext: Boolean,
    emptyAndLoaded: Boolean,
    emptyText: String,
    nextKey: Any?,
    onLoadMore: suspend () -> Unit,
    onRetry: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    if (hasNext && error == null) {
        LaunchedEffect(nextKey) { onLoadMore() }
    }
    Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
        when {
            loading -> InfiniteProgressIndicator()
            error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(error, color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center)
                TextButton(text = "Retry", onClick = { scope.launch { onRetry() } })
            }
            emptyAndLoaded -> Text(emptyText, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}

@Composable
internal fun FeedFooter(feed: FeedState, emptyText: String) {
    PagedFooter(
        loading = feed.loading,
        error = feed.error,
        hasNext = feed.nextUrl != null,
        emptyAndLoaded = feed.loaded && feed.items.isEmpty(),
        emptyText = emptyText,
        nextKey = feed.nextUrl,
        onLoadMore = { feed.loadMore() },
        onRetry = { if (feed.items.isEmpty()) feed.refresh() else feed.loadMore() },
    )
}

/** Square-ish grid tile with R-18 / page-count badges and a heart, like pixiv's grids. */
@Composable
internal fun GridTile(a: Illust, modifier: Modifier = Modifier) {
    val nav = LocalNavigator.current
    Box(modifier.aspectRatio(0.8f)) {
        RemoteImage(a.imageUrl, false, Modifier.fillMaxSize(), crop = true)
        TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Post(a)) }
        if (a.xRestrict > 0) {
            Text(
                if (a.xRestrict > 1) "R-18G" else "R-18",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
                    .background(Color(0xFFE84A5F), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
        }
        if (a.pageCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = MiuixIcons.Copy, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White)
                Text(a.pageCount.toString(), color = Color.White, fontSize = 11.sp, modifier = Modifier.padding(start = 3.dp))
            }
        }
        LikeButton(a, Modifier.align(Alignment.BottomEnd), idleTint = Color.White)
    }
}

internal fun LazyListScope.illustGrid(feed: FeedState, emptyText: String, keyPrefix: String) {
    items(feed.items.chunked(2), key = { "$keyPrefix-${it.first().id}" }) { row ->
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            row.forEach { GridTile(it, Modifier.weight(1f)) }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    item(key = "$keyPrefix-footer") { FeedFooter(feed, emptyText) }
}

internal fun LazyListScope.illustItems(feed: FeedState, emptyText: String, collections: Boolean = false) {
    itemsIndexed(feed.items, key = { _, it -> it.id }) { index, a ->
        Column {
            IllustCard(a, onNotInterested = { feed.notInterested(a) })
            if (collections && (index + 1) % 5 == 0) CollectionBlock(a.userId, a.userName)
        }
    }
    item(key = "footer") { FeedFooter(feed, emptyText) }
}

// ---------------------------------------------------------------- Profile

@Composable
fun ProfilePage(padding: PaddingValues) {
    val session = Pixiv.session
    val nav = LocalNavigator.current
    PageScaffold(
        title = "Profile",
        padding = padding,
        actions = {
            if (session != null) {
                IconButton(onClick = { nav.push(Screen.AccountSettings) }) {
                    Icon(MiuixIcons.Settings, "Account settings", tint = MiuixTheme.colorScheme.onSurface)
                }
            }
        },
    ) { contentPadding, scrollModifier, _ ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                LoginPrompt("Log in to view your pixiv profile")
            }
        } else {
            ProfileContent(session, contentPadding, scrollModifier)
        }
    }
}

@Composable
private fun ProfileContent(session: Session, contentPadding: PaddingValues, scrollModifier: Modifier) {
    var detail by remember(session.userId) { mutableStateOf<UserDetail?>(null) }
    var detailError by remember(session.userId) { mutableStateOf<String?>(null) }
    LaunchedEffect(session.userId) {
        try {
            detail = Pixiv.userDetail(session.userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            detailError = e.message
        }
    }
    val bookmarks = remember(session.userId) { FeedState { Pixiv.bookmarks(session.userId, it) } }
    LaunchedEffect(bookmarks) { bookmarks.refresh() }
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary

    LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
        item(key = "profileCard") {
            Card(modifier = Modifier.padding(12.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RemoteImage(detail?.avatar ?: session.avatar, true, Modifier.size(80.dp).clip(CircleShape))
                    Text(
                        detail?.name ?: session.name,
                        style = MiuixTheme.textStyles.title3,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text("@${detail?.account ?: session.account}  ·  ID ${session.userId}", color = summary, fontSize = 13.sp)
                    detail?.let { d ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                        ) {
                            Stat("Following", d.following)
                            Stat("Illusts", d.illusts)
                            Stat("Bookmarks", d.bookmarks)
                        }
                        if (d.comment.isNotBlank()) {
                            Text(d.comment, color = summary, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
                        }
                    }
                    detailError?.let { Text(it, color = MiuixTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }
        item(key = "logout") {
            Button(
                onClick = { Pixiv.logout() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            ) { Text("Log out") }
        }
        item(key = "bookmarksTitle") { SmallTitle("Public bookmarks") }
        illustItems(bookmarks, "No public bookmarks")
    }
}

@Composable
internal fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(fmt(value), style = MiuixTheme.textStyles.title4)
        Text(label, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
    }
}
