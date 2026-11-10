// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
private fun PageScaffold(
    title: String,
    padding: PaddingValues,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues, Modifier) -> Unit,
) {
    val appState = LocalAppState.current
    val backdrop = rememberBlurBackdrop()
    val blurActive = backdrop != null
    val barColor = if (blurActive) Color.Transparent else MiuixTheme.colorScheme.surface
    val scrollBehavior = MiuixScrollBehavior()
    Scaffold(
        topBar = {
            BlurredBar(backdrop, blurActive, scrollBehavior) {
                AdaptiveTopAppBar(
                    title = title,
                    showTopAppBar = appState.showTopAppBar,
                    scrollBehavior = scrollBehavior,
                    color = barColor,
                    actions = actions,
                )
            }
        },
    ) { inner ->
        val contentPadding = pageContentPadding(PaddingValues(top = inner.calculateTopPadding()), padding)
        Box(modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier) {
            content(
                contentPadding,
                Modifier.pageScrollModifiers(
                    appState.enableScrollEndHaptic,
                    appState.showTopAppBar,
                    scrollBehavior,
                ),
            )
        }
    }
}

// ---------------------------------------------------------------- shared pieces

@Composable
private fun LoginPrompt(message: String, modifier: Modifier = Modifier) {
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

private fun fmt(n: Int): String = when {
    n >= 1_000_000 -> "${"%.1f".format(n / 1_000_000.0)}M"
    n >= 1_000 -> "${"%.1f".format(n / 1_000.0)}K"
    else -> n.toString()
}

@Composable
private fun IllustCard(a: Illust) {
    val uriHandler = LocalUriHandler.current
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Card(
        modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp),
        onClick = { uriHandler.openUri("https://www.pixiv.net/artworks/${a.id}") },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteImage(a.userAvatar, true, Modifier.size(40.dp).clip(CircleShape))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(a.userName, style = MiuixTheme.textStyles.body1, fontSize = 15.sp, maxLines = 1)
                    Text(a.title, color = summary, fontSize = 13.sp, maxLines = 1)
                }
            }
            RemoteImage(
                a.imageUrl,
                false,
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .aspectRatio((a.width.toFloat() / a.height).coerceIn(0.5f, 2f))
                    .clip(RoundedCornerShape(12.dp)),
            )
            if (a.tags.isNotEmpty()) {
                Text(
                    a.tags.take(6).joinToString("  ") { "#$it" },
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(MiuixIcons.Favorites, null, Modifier.size(16.dp), summary)
                Text(fmt(a.bookmarks), color = summary, fontSize = 13.sp)
                Icon(MiuixIcons.Show, null, Modifier.padding(start = 8.dp).size(16.dp), summary)
                Text(fmt(a.views), color = summary, fontSize = 13.sp)
            }
        }
    }
}

private fun LazyListScope.illustItems(feed: FeedState, emptyText: String) {
    items(feed.items, key = { it.id }) { IllustCard(it) }
    item(key = "footer") {
        val scope = rememberCoroutineScope()
        val next = feed.nextUrl
        if (next != null && feed.error == null) {
            LaunchedEffect(next) { feed.loadMore() }
        }
        Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            when {
                feed.loading -> InfiniteProgressIndicator()
                feed.error != null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(feed.error ?: "", color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center)
                    TextButton(
                        text = "Retry",
                        onClick = {
                            scope.launch { if (feed.items.isEmpty()) feed.refresh() else feed.loadMore() }
                        },
                    )
                }
                feed.loaded && feed.items.isEmpty() -> Text(emptyText, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    }
}

// ---------------------------------------------------------------- Home

@Composable
fun FeedPage(padding: PaddingValues) {
    val session = Pixiv.session
    val scope = rememberCoroutineScope()
    val feed = remember(session?.userId) { FeedState { Pixiv.recommended(it) } }
    LaunchedEffect(feed) { if (Pixiv.session != null) feed.refresh() }

    PageScaffold(
        title = "Home",
        padding = padding,
        actions = {
            if (session != null) {
                IconButton(onClick = { scope.launch { feed.refresh() } }) {
                    Icon(MiuixIcons.Refresh, "Refresh", tint = MiuixTheme.colorScheme.onSurface)
                }
            }
        },
    ) { contentPadding, scrollModifier ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                LoginPrompt("Log in to see your pixiv recommendations")
            }
        } else {
            LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
                illustItems(feed, "Nothing to show yet")
            }
        }
    }
}

// ---------------------------------------------------------------- Search

@Composable
fun SearchPage(padding: PaddingValues) {
    val session = Pixiv.session
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var submitted by remember { mutableStateOf("") }
    val feed = remember(submitted, session?.userId) { FeedState { Pixiv.search(submitted, it) } }
    LaunchedEffect(feed) { if (submitted.isNotBlank() && Pixiv.session != null) feed.refresh() }
    val keyboard = LocalSoftwareKeyboardController.current

    PageScaffold(title = "Search", padding = padding) { contentPadding, scrollModifier ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                LoginPrompt("Log in to search pixiv")
            }
        } else {
            LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
                item(key = "searchField") {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        label = "Search artworks, tags…",
                        useLabelAsPlaceholder = true,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                submitted = query.text.trim()
                                keyboard?.hide()
                            },
                        ),
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    )
                }
                if (submitted.isNotBlank()) {
                    illustItems(feed, "No results for “$submitted”")
                } else {
                    item(key = "hint") {
                        Text(
                            "Search by tag or keyword",
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

// ---------------------------------------------------------------- Profile

@Composable
fun ProfilePage(padding: PaddingValues) {
    val session = Pixiv.session
    PageScaffold(title = "Profile", padding = padding) { contentPadding, scrollModifier ->
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
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(fmt(value), style = MiuixTheme.textStyles.title4)
        Text(label, color = MiuixTheme.colorScheme.onSurfaceVariantSummary, fontSize = 12.sp)
    }
}
