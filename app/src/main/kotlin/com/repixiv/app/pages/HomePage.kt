// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.Screen
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.TrendTag
import com.repixiv.app.util.RemoteImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val HomeTabs = listOf("Home", "Illustrations", "Manga", "Discover")

@Composable
fun FeedPage(padding: PaddingValues) {
    val session = Pixiv.session
    val uid = session?.userId
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }

    val latest = remember(uid) { FeedState { Pixiv.followFeed(it) } }
    val recommended = remember(uid) { FeedState(applyFilters = true) { Pixiv.recommended(it) } }
    val illustRanking = remember(uid) { FeedState { Pixiv.ranking("day", it) } }
    val mangaRanking = remember(uid) { FeedState { Pixiv.ranking("day_manga", it) } }
    val manga = remember(uid) { FeedState(applyFilters = true) { Pixiv.mangaRecommended(it) } }
    val weekly = remember(uid) { FeedState { Pixiv.ranking("week", it) } }
    var tags by remember(uid) { mutableStateOf<List<TrendTag>>(emptyList()) }
    val listStates = remember(uid) { List(HomeTabs.size) { LazyListState() } }
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()

    suspend fun loadTags() {
        try {
            tags = Pixiv.trendingTags()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(uid, tab) {
        if (uid == null) return@LaunchedEffect
        coroutineScope {
            when (tab) {
                0 -> { launch { latest.ensureLoaded() }; launch { recommended.ensureLoaded() } }
                1 -> { launch { illustRanking.ensureLoaded() }; launch { latest.ensureLoaded() } }
                2 -> { launch { mangaRanking.ensureLoaded() }; launch { manga.ensureLoaded() } }
                else -> { launch { if (tags.isEmpty()) loadTags() }; launch { weekly.ensureLoaded() } }
            }
        }
    }

    fun refreshCurrent() {
        refreshing = true
        scope.launch {
            try {
                coroutineScope {
                    when (tab) {
                        0 -> { launch { latest.refresh() }; launch { recommended.refresh() } }
                        1 -> { launch { illustRanking.refresh() }; launch { latest.refresh() } }
                        2 -> { launch { mangaRanking.refresh() }; launch { manga.refresh() } }
                        else -> { launch { loadTags() }; launch { weekly.refresh() } }
                    }
                }
            } finally {
                refreshing = false
            }
        }
    }

    val nav = LocalNavigator.current

    PageScaffold(
        title = "Home",
        padding = padding,
        alwaysShowBar = true,
        bottomContent = {
            if (session != null) {
                TabRowWithContour(
                    tabs = HomeTabs,
                    selectedTabIndex = tab,
                    onTabSelected = { tab = it },
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = 6.dp),
                )
            }
        },
    ) { contentPadding, scrollModifier, scrollBehavior ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                LoginPrompt("Log in to see your pixiv home")
            }
        } else {
            PullToRefresh(
                isRefreshing = refreshing,
                onRefresh = { refreshCurrent() },
                pullToRefreshState = pullState,
                contentPadding = contentPadding,
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    label = "homeTab",
                ) { t ->
                    LazyColumn(
                        state = listStates[t],
                        modifier = scrollModifier,
                        contentPadding = contentPadding,
                    ) {
                        when (t) {
                            0 -> {
                                item(key = "latestHeader") {
                                    SectionHeader("Latest works for you") {
                                        nav.push(Screen.FeedList("Latest works for you", "follow"))
                                    }
                                }
                                item(key = "latestRow") { WorkRow(latest, "No new works from people you follow") }
                                illustItems(recommended, "No recommendations yet", collections = true)
                            }
                            1 -> {
                                item(key = "rankingHeader") {
                                    SectionHeader("Daily ranking") { nav.push(Screen.FeedList("Daily ranking", "ranking:day")) }
                                }
                                item(key = "rankingRow") { WorkRow(illustRanking, "Ranking is unavailable") }
                                item(key = "followingHeader") { SectionHeader("Following", null) }
                                illustItems(latest, "No new works from people you follow")
                            }
                            2 -> {
                                item(key = "mangaRankingHeader") {
                                    SectionHeader("Daily manga ranking") {
                                        nav.push(Screen.FeedList("Daily manga ranking", "ranking:day_manga"))
                                    }
                                }
                                item(key = "mangaRankingRow") { WorkRow(mangaRanking, "Ranking is unavailable") }
                                item(key = "mangaHeader") { SectionHeader("Recommended manga", null) }
                                illustItems(manga, "No manga recommendations yet", collections = true)
                            }
                            else -> {
                                item(key = "tagsHeader") { SectionHeader("Trending tags", null) }
                                if (tags.isEmpty()) {
                                    item(key = "tagsLoading") {
                                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                            InfiniteProgressIndicator()
                                        }
                                    }
                                } else {
                                    items(tags.chunked(2), key = { it.first().tag }) { pair ->
                                        Row(
                                            Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            pair.forEach { TagTile(it, Modifier.weight(1f)) }
                                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                                item(key = "weeklyHeader") {
                                    SectionHeader("Rising this week") { nav.push(Screen.FeedList("Rising this week", "ranking:week")) }
                                }
                                illustItems(weekly, "Ranking is unavailable")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onShowAll: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (onShowAll != null) {
            Text(
                "Show all",
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onShowAll() }.padding(4.dp),
            )
        }
    }
}

@Composable
private fun WorkRow(feed: FeedState, emptyText: String) {
    if (feed.items.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
            when {
                feed.loading -> InfiniteProgressIndicator()
                feed.error != null -> Text(feed.error ?: "", color = MiuixTheme.colorScheme.error, textAlign = TextAlign.Center)
                else -> Text(emptyText, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 12.dp),
        ) {
            items(feed.items.take(15), key = { it.id }) { WorkTile(it) }
        }
    }
}

@Composable
private fun WorkTile(a: Illust) {
    val nav = LocalNavigator.current
    Column(Modifier.width(170.dp)) {
        Box(Modifier.size(170.dp).clip(RoundedCornerShape(12.dp))) {
            RemoteImage(a.imageUrl, false, Modifier.fillMaxSize(), crop = true)
            TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Viewer(a)) }
            LikeButton(a, Modifier.align(Alignment.BottomEnd), idleTint = Color.White)
        }
        Text(
            a.title,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 6.dp),
        )
        Row(
            Modifier.fillMaxWidth().clickable { nav.openUser(a.userId) }.padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RemoteImage(a.userAvatar, true, Modifier.size(22.dp).clip(CircleShape))
            Text(
                a.userName,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun TagTile(t: TrendTag, modifier: Modifier) {
    val nav = LocalNavigator.current
    Box(modifier.aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) {
        RemoteImage(t.imageUrl, false, Modifier.fillMaxSize(), crop = true)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
            Text("#${t.tag}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.translated.isNotBlank()) {
                Text(t.translated, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.FeedList("#${t.tag}", "search:${t.tag}")) }
    }
}
