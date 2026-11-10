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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.repixiv.app.pixiv.Downloader
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Filters
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.UserDetail
import com.repixiv.app.util.AdaptiveTopAppBar
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.pageScrollModifiers
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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !zoomed,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomableImage(
                previewUrl = illust.largeUrls.getOrElse(page) { illust.imageUrl },
                fullUrl = illust.originalUrls.getOrElse(page) { illust.imageUrl },
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
private fun ZoomableImage(previewUrl: String, fullUrl: String, onZoomChange: (Boolean) -> Unit) {
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
        val target = object : CustomTarget<Bitmap>(2560, 2560) {
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

// ---------------------------------------------------------------- Artist page

@Composable
fun UserScreen(userId: Long) {
    val nav = LocalNavigator.current
    val appState = LocalAppState.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    var detail by remember(userId) { mutableStateOf<UserDetail?>(null) }
    var followed by remember(userId) { mutableStateOf(false) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    var busy by remember(userId) { mutableStateOf(false) }
    val feed = remember(userId) { FeedState { Pixiv.userIllusts(userId, it) } }

    LaunchedEffect(userId) {
        try {
            val d = Pixiv.userDetail(userId)
            detail = d
            followed = d.isFollowed
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(feed) { feed.refresh() }
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val isMe = Pixiv.session?.userId == userId

    Scaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = detail?.name ?: "Artist",
                showTopAppBar = true,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { nav.pop() }) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "Back", tint = MiuixTheme.colorScheme.onSurface)
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.pageScrollModifiers(appState.enableScrollEndHaptic, true, scrollBehavior),
            contentPadding = PaddingValues(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding()),
        ) {
            item(key = "userCard") {
                Card(modifier = Modifier.padding(12.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        RemoteImage(detail?.avatar.orEmpty(), true, Modifier.size(80.dp).clip(CircleShape))
                        detail?.let { d ->
                            Text(d.name, style = MiuixTheme.textStyles.title3, modifier = Modifier.padding(top = 12.dp))
                            Text("@${d.account}", color = summary, fontSize = 13.sp)
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
                            if (!isMe) {
                                Button(
                                    onClick = {
                                        if (busy) return@Button
                                        busy = true
                                        error = null
                                        scope.launch {
                                            try {
                                                Pixiv.setFollow(userId, !followed)
                                                followed = !followed
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                error = e.message ?: "Request failed"
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    },
                                    colors = if (followed) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColorsPrimary(),
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                ) {
                                    Text(
                                        if (followed) "Following" else "Follow",
                                        color = if (followed) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                        error?.let {
                            Text(it, color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
            illustItems(feed, "No illustrations yet")
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
    val appState = LocalAppState.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = MiuixScrollBehavior()
    val session = Pixiv.session

    var restricted by remember { mutableStateOf<Boolean?>(null) }
    var showAi by remember { mutableStateOf<Boolean?>(null) }
    var flags by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var hiddenCount by remember { mutableIntStateOf(Filters.hiddenCount) }

    LaunchedEffect(session == null) { if (session == null) nav.pop() }
    LaunchedEffect(Unit) {
        try { restricted = Pixiv.restrictedMode() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message }
        try { showAi = Pixiv.showAi() } catch (e: CancellationException) { throw e } catch (e: Exception) { error = e.message }
        try { flags = Pixiv.accountState() } catch (e: CancellationException) { throw e } catch (e: Exception) { /* optional info */ }
    }

    Scaffold(
        topBar = {
            AdaptiveTopAppBar(
                title = "Account settings",
                showTopAppBar = true,
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = { nav.pop() }) {
                        Icon(imageVector = MiuixIcons.Back, contentDescription = "Back", tint = MiuixTheme.colorScheme.onSurface)
                    }
                },
            )
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.pageScrollModifiers(appState.enableScrollEndHaptic, true, scrollBehavior),
            contentPadding = PaddingValues(top = inner.calculateTopPadding(), bottom = inner.calculateBottomPadding()),
        ) {
            if (session != null) {
                item(key = "account") {
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
            item(key = "device") {
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
            item(key = "footer") {
                error?.let {
                    Text(
                        it,
                        color = MiuixTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    )
                }
                Button(
                    onClick = { Pixiv.logout() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                ) { Text("Log out") }
                Text(
                    "Email, password and pixiv ID changes aren't available through pixiv's app API.",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                )
            }
        }
    }
}
