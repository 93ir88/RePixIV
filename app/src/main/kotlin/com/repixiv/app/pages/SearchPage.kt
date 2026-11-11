// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.LocalLists
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.pixiv.TagCovers
import com.repixiv.app.pixiv.UserPreview
import com.repixiv.app.pixiv.UserSearchState
import com.repixiv.app.util.RemoteImage
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Recent
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.overlay.OverlayListPopup
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val SearchTabs = listOf("Illustrations and Manga", "Users")

@Composable
internal fun SectionRow(title: String, action: String?, onAction: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                fontSize = 14.sp,
                modifier = Modifier.clickable { onAction() }.padding(4.dp),
            )
        }
    }
}

@Composable
fun SearchPage(padding: PaddingValues) {
    val session = Pixiv.session
    val uid = session?.userId
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var query by remember { mutableStateOf(TextFieldValue("")) }
    var submitted by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var focused by remember { mutableStateOf(false) }
    var addingTag by remember { mutableStateOf(false) }
    var newTag by remember { mutableStateOf(TextFieldValue("")) }
    var editFavorites by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val illustFeed = remember(submitted, uid) { FeedState(applyFilters = true) { Pixiv.search(submitted, it) } }
    val userFeed = remember(submitted, uid) { UserSearchState { Pixiv.searchUsers(submitted, it) } }
    LaunchedEffect(illustFeed, userFeed, tab) {
        if (submitted.isNotBlank() && uid != null) {
            if (tab == 0) illustFeed.ensureLoaded() else userFeed.ensureLoaded()
        }
    }

    val clearIcon: @Composable () -> Unit = {
        Icon(
            imageVector = MiuixIcons.Close,
            contentDescription = "Clear",
            modifier = Modifier
                .padding(end = 12.dp)
                .size(20.dp)
                .clickable {
                    query = TextFieldValue("")
                    submitted = ""
                },
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }

    fun runSearch(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        query = TextFieldValue(w)
        submitted = w
        LocalLists.addHistory(w)
        focusManager.clearFocus()
        keyboard?.hide()
    }

    PageScaffold(
        title = "Search",
        padding = padding,
        actions = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(imageVector = MiuixIcons.More, contentDescription = "More", tint = MiuixTheme.colorScheme.onSurface)
                }
                val tint = MiuixTheme.colorScheme.onSurface
                val entries = listOf<Pair<DropdownItem, () -> Unit>>(
                    DropdownItem(
                        text = "Clear history",
                        icon = { m -> Icon(imageVector = MiuixIcons.Delete, contentDescription = null, modifier = m, tint = tint) },
                    ) to { LocalLists.clearHistory() },
                )
                OverlayListPopup(
                    show = menuOpen,
                    alignment = PopupPositionProvider.Align.End,
                    onDismissRequest = { menuOpen = false },
                ) {
                    ListPopupColumn {
                        entries.forEachIndexed { index, (item, action) ->
                            DropdownImpl(
                                item = item,
                                optionSize = entries.size,
                                isSelected = false,
                                index = index,
                                onSelectedIndexChange = {
                                    menuOpen = false
                                    action()
                                },
                            )
                        }
                    }
                }
            }
        },
    ) { contentPadding, scrollModifier, _ ->
        if (session == null) {
            Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
                LoginPrompt("Log in to search pixiv")
            }
        } else {
            LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
                item(key = "field") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            label = "Search pixiv",
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
                            trailingIcon = if (query.text.isNotEmpty()) clearIcon else null,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { runSearch(query.text) }),
                            modifier = Modifier.weight(1f).onFocusChanged { focused = it.isFocused },
                        )
                    }
                }
                item(key = "tabs") {
                    TabRowWithContour(
                        tabs = SearchTabs,
                        selectedTabIndex = tab,
                        onTabSelected = { tab = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }

                // history only while the search bar is focused
                if (focused) {
                    item(key = "historyHeader") {
                        SectionRow("Search history", "Clear history") { LocalLists.clearHistory() }
                    }
                    if (LocalLists.history.isEmpty()) {
                        item(key = "historyEmpty") {
                            Text(
                                "No search history yet",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            )
                        }
                    } else {
                        items(LocalLists.history.toList(), key = { "h-$it" }) { word ->
                            Row(
                                Modifier.fillMaxWidth().clickable { runSearch(word) }.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = MiuixIcons.Recent,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                                Text(word, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
                                Icon(
                                    imageVector = MiuixIcons.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp).clickable { LocalLists.removeHistory(word) },
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                }

                if (submitted.isBlank()) {
                    // ---- favorite tags
                    item(key = "favHeader") {
                        SectionRow("Your favorite tags", if (editFavorites) "Done" else "Favorite Tags Settings") {
                            editFavorites = !editFavorites
                        }
                    }
                    item(key = "addTag") {
                        Column(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                            if (!addingTag) {
                                Button(onClick = { addingTag = true }, cornerRadius = 12.dp) {
                                    Icon(imageVector = MiuixIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp), tint = MiuixTheme.colorScheme.onSurface)
                                    Text("Add a tag", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp))
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextField(
                                        value = newTag,
                                        onValueChange = { newTag = it },
                                        label = "Tag name",
                                        useLabelAsPlaceholder = true,
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Button(
                                        onClick = {
                                            LocalLists.addFavoriteTag(newTag.text)
                                            newTag = TextFieldValue("")
                                            addingTag = false
                                        },
                                        modifier = Modifier.padding(start = 8.dp),
                                        cornerRadius = 12.dp,
                                    ) { Text("Add", fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                    if (LocalLists.favoriteTags.isEmpty()) {
                        item(key = "favEmpty") {
                            Text(
                                "Add tags you like to find new works faster.",
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        item(key = "favRow") {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(LocalLists.favoriteTags.toList(), key = { it }) { tag ->
                                    FavoriteTagTile(tag, editFavorites, onOpen = { runSearch(tag) })
                                }
                            }
                        }
                        items(LocalLists.favoriteTags.take(3), key = { "sec-$it" }) { tag ->
                            FavoriteTagSection(tag, onViewAll = { runSearch(tag) })
                        }
                    }
                } else if (tab == 0) {
                    illustGrid(illustFeed, "No results for “$submitted”", "results")
                } else {
                    items(userFeed.items, key = { "u-${it.id}" }) { u -> UserRow(u) }
                    item(key = "u-footer") {
                        PagedFooter(
                            loading = userFeed.loading,
                            error = userFeed.error,
                            hasNext = userFeed.nextUrl != null,
                            emptyAndLoaded = userFeed.loaded && userFeed.items.isEmpty(),
                            emptyText = "No users found",
                            nextKey = userFeed.nextUrl,
                            onLoadMore = { userFeed.loadMore() },
                            onRetry = { if (userFeed.items.isEmpty()) userFeed.refresh() else userFeed.loadMore() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteTagTile(tag: String, editing: Boolean, onOpen: () -> Unit) {
    LaunchedEffect(tag) { TagCovers.load(tag) }
    val cover = TagCovers.cache[tag]
    Box(
        Modifier
            .size(width = 140.dp, height = 140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
    ) {
        if (cover != null) RemoteImage(cover, false, Modifier.fillMaxSize(), crop = true)
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        Text(
            "#$tag",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
        )
        TapOverlay(Modifier.matchParentSize()) { if (!editing) onOpen() }
        if (editing) {
            Icon(
                imageVector = MiuixIcons.Close,
                contentDescription = "Remove",
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { LocalLists.removeFavoriteTag(tag) }
                    .padding(5.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun FavoriteTagSection(tag: String, onViewAll: () -> Unit) {
    val feed = remember(tag) { FeedState(applyFilters = true) { Pixiv.search(tag, it) } }
    LaunchedEffect(tag) { feed.ensureLoaded() }
    Column(Modifier.fillMaxWidth()) {
        SectionRow("#$tag", "View all", onViewAll)
        feed.items.take(4).chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(bottom = 2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                row.forEach { GridTile(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun UserRow(u: UserPreview) {
    val nav = LocalNavigator.current
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(48.dp).clip(CircleShape)) {
                RemoteImage(u.avatar, true, Modifier.fillMaxSize())
                TapOverlay(Modifier.matchParentSize()) { nav.openUser(u.id) }
            }
            Text(
                u.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clickable { nav.openUser(u.id) }.padding(horizontal = 12.dp),
            )
            FollowButton(u.id, u.isFollowed)
        }
        if (u.thumbs.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                u.thumbs.take(3).forEach { t ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        RemoteImage(t, false, Modifier.fillMaxSize(), crop = true)
                        TapOverlay(Modifier.matchParentSize()) { nav.openUser(u.id) }
                    }
                }
                repeat(3 - u.thumbs.take(3).size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
