// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.Screen
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.UserDetail
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.cardUrl
import kotlinx.coroutines.CancellationException
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.min

private val UserTabs = listOf("Home", "Illustrations", "Bookmarks")

@Composable
fun UserScreen(userId: Long) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    var detail by remember(userId) { mutableStateOf<UserDetail?>(null) }
    var error by remember(userId) { mutableStateOf<String?>(null) }
    var tab by remember(userId) { mutableIntStateOf(0) }
    var bioExpanded by remember(userId) { mutableStateOf(false) }

    val home = remember(userId) { FeedState { Pixiv.userIllusts(userId, it, type = null) } }
    val illusts = remember(userId) { FeedState { Pixiv.userIllusts(userId, it, type = "illust") } }
    val bookmarks = remember(userId) { FeedState { Pixiv.bookmarks(userId, it) } }

    LaunchedEffect(userId) {
        try {
            detail = Pixiv.userDetail(userId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message
        }
    }
    LaunchedEffect(userId, tab) {
        when (tab) {
            0 -> home.ensureLoaded()
            1 -> illusts.ensureLoaded()
            else -> bookmarks.ensureLoaded()
        }
    }

    PageScaffold(
        title = detail?.name ?: "Artist",
        padding = PaddingValues(0.dp),
        overlay = true,
        navigationIcon = { BackButton() },
        actions = {
            IconButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "${detail?.name.orEmpty()}\nhttps://www.pixiv.net/users/$userId")
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                },
            ) {
                Icon(imageVector = MiuixIcons.Share, contentDescription = "Share", tint = MiuixTheme.colorScheme.onSurface)
            }
        },
    ) { contentPadding, scrollModifier, _ ->
        LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
            item(key = "header") {
                val d = detail
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth()) {
                        Box(Modifier.fillMaxWidth().height(150.dp).background(MiuixTheme.colorScheme.surfaceContainer)) {
                            if (!d?.background.isNullOrBlank()) {
                                RemoteImage(d?.background.orEmpty(), false, Modifier.fillMaxSize(), crop = true)
                            }
                        }
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = 44.dp)
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(MiuixTheme.colorScheme.surface)
                                .padding(3.dp)
                                .clip(CircleShape),
                        ) {
                            RemoteImage(d?.avatar.orEmpty(), true, Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(52.dp))
                    if (d != null) {
                        if (d.isPremium) {
                            Text(
                                "premium",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(bottom = 6.dp)
                                    .background(Color(0xFFFF9800), RoundedCornerShape(50))
                                    .padding(horizontal = 10.dp, vertical = 2.dp),
                            )
                        }
                        Text(d.name, style = MiuixTheme.textStyles.title3, fontWeight = FontWeight.Bold)
                        FollowButton(
                            userId,
                            d.isFollowed,
                            Modifier.fillMaxWidth(0.62f).padding(top = 14.dp),
                            compact = false,
                        )
                        Row(Modifier.padding(top = 14.dp)) {
                            Text(fmt(d.following), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(" Following", color = summary, fontSize = 14.sp)
                        }
                        if (d.webpage.isNotBlank() || d.twitterUrl.isNotBlank()) {
                            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (d.webpage.isNotBlank()) {
                                    IconButton(onClick = { uriHandler.openUri(d.webpage) }) {
                                        Icon(imageVector = MiuixIcons.Home, contentDescription = "Website", tint = MiuixTheme.colorScheme.onSurface)
                                    }
                                }
                                if (d.twitterUrl.isNotBlank()) {
                                    IconButton(onClick = { uriHandler.openUri(d.twitterUrl) }) {
                                        Text("𝕏", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        if (d.comment.isNotBlank()) {
                            Text(
                                d.comment,
                                fontSize = 14.sp,
                                maxLines = if (bioExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(top = 10.dp),
                            )
                            Text(
                                if (bioExpanded) "Hide profile" else "View profile",
                                color = summary,
                                fontSize = 13.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { bioExpanded = !bioExpanded }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    } else if (error != null) {
                        Text(error ?: "", color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    }
                    TabRowWithContour(
                        tabs = UserTabs,
                        selectedTabIndex = tab,
                        onTabSelected = { tab = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                    )
                }
            }

            when (tab) {
                0 -> {
                    if (home.items.isNotEmpty()) {
                        item(key = "carousel") { FeaturedCarousel(home) }
                    }
                    item(key = "worksHeader") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Illustrations/Manga", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            if ((detail?.totalWorks ?: 0) > 0) {
                                Text(
                                    detail?.totalWorks.toString(),
                                    fontSize = 12.sp,
                                    modifier = Modifier
                                        .padding(start = 10.dp)
                                        .background(MiuixTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    illustGrid(home, "No works yet", "home")
                }
                1 -> illustGrid(illusts, "No illustrations yet", "illusts")
                else -> illustGrid(bookmarks, "No public bookmarks", "bookmarks")
            }
        }
    }
}

/** Swipeable featured works at the top of an artist's Home tab; keeps loading as you swipe. */
@Composable
private fun FeaturedCarousel(feed: FeedState) {
    val nav = LocalNavigator.current
    val pagerState = rememberPagerState(pageCount = { feed.items.size.coerceAtLeast(1) })
    LaunchedEffect(pagerState.currentPage, feed.items.size) {
        if (pagerState.currentPage >= feed.items.size - 3) feed.loadMore()
    }
    Column(Modifier.fillMaxWidth()) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)) { page ->
            val a = feed.items.getOrNull(page)
            if (a != null) {
                Box(Modifier.fillMaxSize()) {
                    RemoteImage(a.cardUrl(), false, Modifier.fillMaxSize(), crop = true)
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))),
                    )
                    TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Post(a)) }
                    Column(Modifier.align(Alignment.BottomStart).padding(16.dp).padding(end = 48.dp)) {
                        Text(
                            if (a.type == "manga") "Manga" else "Illustrations",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                        )
                        Text(a.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    LikeButton(a, Modifier.align(Alignment.BottomEnd), idleTint = Color.White)
                }
            }
        }
        val total = feed.items.size
        if (total > 1) {
            val count = min(5, total)
            val start = (pagerState.currentPage - 2).coerceIn(0, total - count)
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                for (i in start until start + count) {
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == pagerState.currentPage) MiuixTheme.colorScheme.onSurface
                                else MiuixTheme.colorScheme.onSurfaceVariantSummary.copy(alpha = 0.4f),
                            ),
                    )
                }
            }
        }
    }
}
