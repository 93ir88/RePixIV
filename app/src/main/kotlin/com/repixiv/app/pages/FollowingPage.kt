// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.UserSearchState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Tune
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val FollowingTabs = listOf("All", "Users")
private val RestrictLabels = listOf("All", "Public", "Private")
private val RestrictValues = listOf("all", "public", "private")

/** pixiv's Following feed: new works from people you follow, plus the list of who you follow. */
@Composable
fun FollowingPage(padding: PaddingValues) {
    val session = Pixiv.session
    val uid = session?.userId
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var restrictIndex by remember { mutableIntStateOf(0) }
    var filterOpen by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    val pullState = rememberPullToRefreshState()
    val restrict = RestrictValues[restrictIndex]

    val posts = remember(uid, restrict) { FeedState { Pixiv.followFeed(it, restrict) } }
    val users = remember(uid, restrict) {
        UserSearchState { Pixiv.following(uid ?: 0L, if (restrict == "private") "private" else "public", it) }
    }
    val listStates = remember(uid, restrict) { List(FollowingTabs.size) { LazyListState() } }

    LaunchedEffect(posts, users, tab) {
        if (uid != null) {
            if (tab == 0) posts.ensureLoaded() else users.ensureLoaded()
        }
    }

    PageScaffold(
        title = "Following",
        padding = padding,
        alwaysShowBar = true,
        bottomContent = {
            if (session != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp).padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TabRowWithContour(
                        tabs = FollowingTabs,
                        selectedTabIndex = tab,
                        onTabSelected = { tab = it },
                        modifier = Modifier.weight(1f),
                    )
                    Box {
                        IconButton(onClick = { filterOpen = true }) {
                            Icon(imageVector = MiuixIcons.Tune, contentDescription = "Filter", tint = MiuixTheme.colorScheme.onSurface)
                        }
                        OverlayListPopup(
                            show = filterOpen,
                            alignment = PopupPositionProvider.Align.End,
                            onDismissRequest = { filterOpen = false },
                        ) {
                            ListPopupColumn {
                                RestrictLabels.forEachIndexed { index, label ->
                                    DropdownImpl(
                                        item = DropdownItem(text = label),
                                        optionSize = RestrictLabels.size,
                                        isSelected = index == restrictIndex,
                                        index = index,
                                        onSelectedIndexChange = {
                                            filterOpen = false
                                            restrictIndex = it
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
    ) { contentPadding, scrollModifier, scrollBehavior ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                LoginPrompt("Log in to see who you follow")
            }
        } else {
            PullToRefresh(
                isRefreshing = refreshing,
                onRefresh = {
                    refreshing = true
                    scope.launch {
                        try {
                            if (tab == 0) posts.refresh() else users.refresh()
                        } finally {
                            refreshing = false
                        }
                    }
                },
                pullToRefreshState = pullState,
                contentPadding = contentPadding,
                topAppBarScrollBehavior = scrollBehavior,
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
                    label = "followingTab",
                ) { t ->
                    LazyColumn(state = listStates[t], modifier = scrollModifier, contentPadding = contentPadding) {
                        if (t == 0) {
                            illustItems(posts, "No new works from people you follow", posted = true)
                        } else {
                            items(users.items, key = { "f-${it.id}" }) { u -> UserRow(u) }
                            item(key = "f-footer") {
                                PagedFooter(
                                    loading = users.loading,
                                    error = users.error,
                                    hasNext = users.nextUrl != null,
                                    emptyAndLoaded = users.loaded && users.items.isEmpty(),
                                    emptyText = "You aren't following anyone yet",
                                    nextKey = users.nextUrl,
                                    onLoadMore = { users.loadMore() },
                                    onRetry = { if (users.items.isEmpty()) users.refresh() else users.loadMore() },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
