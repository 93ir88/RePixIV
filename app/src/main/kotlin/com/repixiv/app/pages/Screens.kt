// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import com.repixiv.app.pixiv.Comment
import com.repixiv.app.pixiv.CommentPreviews
import top.yukonga.miuix.kmp.basic.TextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.repixiv.app.LocalAppState
import com.repixiv.app.LocalNavigator
import com.repixiv.app.Screen
import com.repixiv.app.pixiv.Downloader
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Filters
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.UserDetail
import com.repixiv.app.util.AdaptiveTopAppBar
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.pageScrollModifiers
import com.repixiv.app.util.fullUrl
import com.repixiv.app.util.lowResCap
import com.repixiv.app.util.pageUrl
import com.repixiv.app.util.pixivGlideUrl
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.BasicComponent
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
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Download
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

// ---------------------------------------------------------------- Full-screen viewer

@Composable
fun ViewerScreen(illust: Illust, startPage: Int) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pageCount = illust.largeUrls.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(initialPage = startPage.coerceIn(0, pageCount - 1), pageCount = { pageCount })
    var zoomed by remember { mutableStateOf(false) }
    val cap = lowResCap(LocalAppState.current.lowRes)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                previewUrl = illust.pageUrl(page, if (cap == 0) 720 else cap),
                fullUrl = illust.fullUrl(page, cap),
                maxPx = if (cap == 0) 2560 else cap,
                onZoomChange = { zoomed = it },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { nav.pop() }) {
                Icon(imageVector = MiuixIcons.Back, contentDescription = "Back", tint = Color.White)
            }
            Text(
                illust.title,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
            )
            if (pageCount > 1) {
                Text("${pagerState.currentPage + 1}/$pageCount", color = Color.White, fontSize = 14.sp)
            }
            LikeButton(illust, idleTint = Color.White)
            IconButton(onClick = { nav.push(Screen.Comments(illust)) }) {
                Icon(imageVector = MiuixIcons.Messages, contentDescription = "Comments", tint = Color.White)
            }
            IconButton(
                onClick = {
                    Toast.makeText(context, "Downloading…", Toast.LENGTH_SHORT).show()
                    scope.launch { Toast.makeText(context, Downloader.save(context, illust), Toast.LENGTH_LONG).show() }
                },
            ) {
                Icon(imageVector = MiuixIcons.Download, contentDescription = "Download", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ZoomableImage(previewUrl: String, fullUrl: String, maxPx: Int, onZoomChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(fullUrl) { mutableStateOf<ImageBitmap?>(null) }
    var fullLoaded by remember(fullUrl) { mutableStateOf(false) }

    DisposableEffect(previewUrl) {
        val target = object : CustomTarget<Bitmap>() {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                if (!fullLoaded) bitmap = resource.asImageBitmap()
            }
            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
        }
        Glide.with(context).asBitmap().load(pixivGlideUrl(previewUrl)).into(target)
        onDispose { Glide.with(context).clear(target) }
    }
    DisposableEffect(fullUrl) {
        val target = object : CustomTarget<Bitmap>(maxPx, maxPx) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                fullLoaded = true
                bitmap = resource.asImageBitmap()
            }
            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
        }
        Glide.with(context).asBitmap().load(pixivGlideUrl(fullUrl)).into(target)
        onDispose { Glide.with(context).clear(target) }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        val newScale = (scale * zoom).coerceIn(1f, 5f)
        val maxX = size.width * (newScale - 1f) / 2f
        val maxY = size.height * (newScale - 1f) / 2f
        offset = Offset(
            (offset.x + pan.x).coerceIn(-maxX, maxX),
            (offset.y + pan.y).coerceIn(-maxY, maxY),
        )
        scale = newScale
    }
    LaunchedEffect(scale) { onZoomChange(scale > 1.01f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .transformable(transformState, canPan = { scale > 1f }),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp == null) {
            InfiniteProgressIndicator()
        } else {
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
            )
        }
    }
}

// ---------------------------------------------------------------- Shared bits


// ---------------------------------------------------------------- Generic feed list ("Show all", ranking, tag search)

@Composable
fun FeedListScreen(title: String, kind: String) {
    val feed = remember(kind) {
        FeedState(applyFilters = kind != "follow") { next ->
            when {
                kind == "follow" -> Pixiv.followFeed(next)
                kind.startsWith("ranking:") -> Pixiv.ranking(kind.removePrefix("ranking:"), next)
                kind.startsWith("search:") -> Pixiv.search(kind.removePrefix("search:"), next)
                else -> Pixiv.recommended(next)
            }
        }
    }
    LaunchedEffect(feed) { feed.refresh() }
    PageScaffold(
        title = title,
        padding = PaddingValues(0.dp),
        overlay = true,
        navigationIcon = { BackButton() },
    ) { contentPadding, scrollModifier, _ ->
        LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
            illustItems(feed, "Nothing here yet")
        }
    }
}

// ---------------------------------------------------------------- Comments

@Composable
fun CommentsScreen(illust: Illust) {
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val list = remember(illust.id) { mutableStateListOf<Comment>() }
    var next by remember(illust.id) { mutableStateOf<String?>(null) }
    var loading by remember(illust.id) { mutableStateOf(false) }
    var loaded by remember(illust.id) { mutableStateOf(false) }
    var error by remember(illust.id) { mutableStateOf<String?>(null) }
    var input by remember(illust.id) { mutableStateOf(TextFieldValue("")) }
    var sending by remember(illust.id) { mutableStateOf(false) }

    suspend fun load(reset: Boolean) {
        if (loading) return
        loading = true
        error = null
        try {
            val p = Pixiv.comments(illust.id, if (reset) null else next)
            if (reset) list.clear()
            val seen = list.mapTo(HashSet()) { it.id }
            list.addAll(p.items.filter { seen.add(it.id) })
            next = p.nextUrl
            loaded = true
            CommentPreviews.cache.remove(illust.id)
            list.firstOrNull()?.let { CommentPreviews.cache[illust.id] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load comments"
        } finally {
            loading = false
        }
    }
    LaunchedEffect(illust.id) { load(true) }

    fun send() {
        val text = input.text.trim()
        if (text.isEmpty() || sending) return
        sending = true
        scope.launch {
            try {
                Pixiv.addComment(illust.id, text)
                input = TextFieldValue("")
                load(true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Could not post comment", Toast.LENGTH_LONG).show()
            } finally {
                sending = false
            }
        }
    }

    PageScaffold(
        title = "Comments",
        padding = PaddingValues(0.dp),
        overlay = true,
        navigationIcon = { BackButton() },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MiuixTheme.colorScheme.surface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    label = "Add a comment…",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { send() }, enabled = !sending && input.text.isNotBlank()) {
                    Icon(
                        imageVector = MiuixIcons.Send,
                        contentDescription = "Send",
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
        },
    ) { contentPadding, scrollModifier, _ ->
        LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
            items(list, key = { it.id }) { c ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Box(Modifier.size(40.dp).clip(CircleShape)) {
                        RemoteImage(c.userAvatar, true, Modifier.fillMaxSize())
                        TapOverlay(Modifier.matchParentSize()) { nav.openUser(c.userId) }
                    }
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(c.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                            Text(
                                c.date.replace('T', ' ').take(16),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        if (c.stampUrl != null) {
                            RemoteImage(c.stampUrl, false, Modifier.padding(top = 6.dp).size(72.dp), crop = false)
                        } else {
                            Text(c.text, fontSize = 15.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
            item(key = "footer") {
                val n = next
                if (n != null && error == null) LaunchedEffect(n) { load(false) }
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    when {
                        loading -> InfiniteProgressIndicator()
                        error != null -> Text(error ?: "", color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center)
                        loaded && list.isEmpty() -> Text("No comments yet", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Account settings

private val AccountFlagLabels = mapOf(
    "has_mail_address" to "Email address added",
    "mail_authorized" to "Email verified",
    "has_password" to "Password set",
    "is_premium" to "pixiv Premium",
)

@Composable
fun AccountSettingsScreen() {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = Pixiv.session

    var restricted by remember { mutableStateOf<Boolean?>(null) }
    var showAi by remember { mutableStateOf<Boolean?>(null) }
    var flags by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var hiddenCount by remember { mutableIntStateOf(Filters.hiddenCount) }

    LaunchedEffect(session?.userId) {
        if (session == null) return@LaunchedEffect
        try { restricted = Pixiv.restrictedMode() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message }
        try { showAi = Pixiv.showAi() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message }
        try { flags = Pixiv.accountState() } catch (e: CancellationException) { throw e } catch (e: Exception) { /* optional info */ }
    }

    PageScaffold(
        title = "Settings",
        padding = PaddingValues(0.dp),
        overlay = true,
        navigationIcon = { BackButton() },
    ) { contentPadding, scrollModifier, _ ->
        LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
            if (session != null) {
                item(key = "account") {
                    Column {
                        SmallTitle("Account")
                        Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                            BasicComponent(title = "Name", summary = session.name)
                            BasicComponent(title = "pixiv ID", summary = session.account)
                            BasicComponent(title = "User ID", summary = session.userId.toString())
                            AccountFlagLabels.forEach { (key, label) ->
                                flags[key]?.let { BasicComponent(title = label, summary = if (it) "Yes" else "No") }
                            }
                        }
                    }
                }
                item(key = "privacy") {
                    Column {
                        SmallTitle("Content & privacy")
                        Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                            SwitchPreference(
                                title = "Restricted mode",
                                summary = "Hide potentially mature content",
                                checked = restricted ?: false,
                                enabled = restricted != null,
                                onCheckedChange = { v ->
                                    val old = restricted
                                    restricted = v
                                    error = null
                                    scope.launch {
                                        try {
                                            Pixiv.setRestrictedMode(v)
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            restricted = old
                                            error = e.message ?: "Could not update setting"
                                        }
                                    }
                                },
                            )
                            SwitchPreference(
                                title = "Show AI-generated works",
                                summary = "Include AI-generated works in feeds and search",
                                checked = showAi ?: true,
                                enabled = showAi != null,
                                onCheckedChange = { v ->
                                    val old = showAi
                                    showAi = v
                                    error = null
                                    scope.launch {
                                        try {
                                            Pixiv.setShowAi(v)
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            showAi = old
                                            error = e.message ?: "Could not update setting"
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
            }
            item(key = "device") {
                Column {
                    SmallTitle("On this device")
                    Card(modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                        ArrowPreference(
                            title = "Reset “Not interested”",
                            summary = "$hiddenCount hidden post(s). Tap to show them again.",
                            onClick = {
                                Filters.reset()
                                hiddenCount = 0
                                Toast.makeText(context, "Filters reset", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
            }
            if (session != null) {
                item(key = "logout") {
                    Column {
                        error?.let {
                            Text(
                                it,
                                color = MiuixTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                            )
                        }
                        Button(
                            onClick = {
                                Pixiv.logout()
                                nav.pop()
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        ) { Text("Log out") }
                        Text(
                            "Email, password and pixiv ID changes aren't available through pixiv's app API.",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                        )
                    }
                }
            }
            repixivSettingsItems()
            item(key = "bottomSpace") { Spacer(Modifier.height(24.dp)) }
        }
    }
}
