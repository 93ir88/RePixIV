// Portions of this file are derived from Miuix (https://github.com/compose-miuix-ui/miuix)
// Copyright 2025, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
// Modified for RePixIV.

package com.repixiv.app.pages

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.Screen
import com.repixiv.app.pixiv.Comment
import com.repixiv.app.pixiv.CommentPreviews
import com.repixiv.app.pixiv.FeedState
import com.repixiv.app.pixiv.Filters
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Likes
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.formatPostDate
import com.repixiv.app.util.htmlToText
import com.repixiv.app.util.relativeTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun BackButton() {
    val nav = LocalNavigator.current
    IconButton(onClick = { nav.pop() }) {
        Icon(imageVector = MiuixIcons.Back, contentDescription = "Back", tint = MiuixTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(0.5.dp).background(MiuixTheme.colorScheme.dividerLine))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PostScreen(illust: Illust) {
    val nav = LocalNavigator.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary

    val related = remember(illust.id) { FeedState(applyFilters = true) { Pixiv.related(illust.id, it) } }
    val authorWorks = remember(illust.userId) { FeedState { Pixiv.userIllusts(illust.userId, it) } }
    var comments by remember(illust.id) { mutableStateOf<List<Comment>>(emptyList()) }
    var input by remember(illust.id) { mutableStateOf(TextFieldValue("")) }
    var replyTo by remember(illust.id) { mutableStateOf<Comment?>(null) }
    var sending by remember(illust.id) { mutableStateOf(false) }
    var captionExpanded by remember(illust.id) { mutableStateOf(false) }
    val captionText = remember(illust.id) { htmlToText(illust.caption) }
    val pageCount = illust.largeUrls.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pageCount })

    suspend fun loadComments() {
        try {
            comments = Pixiv.comments(illust.id).items.take(3)
            comments.firstOrNull()?.let { CommentPreviews.cache[illust.id] = it }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    LaunchedEffect(illust.id) {
        coroutineScope {
            launch { related.ensureLoaded() }
            launch { authorWorks.ensureLoaded() }
            launch { loadComments() }
        }
    }

    PageScaffold(
        title = illust.userName,
        padding = PaddingValues(0.dp),
        overlay = true,
        navigationIcon = { BackButton() },
        actions = {
            FollowButton(illust.userId, illust.userFollowed)
            Spacer(Modifier.width(12.dp))
        },
    ) { contentPadding, scrollModifier, _ ->
        LazyColumn(modifier = scrollModifier, contentPadding = contentPadding) {
            // ---- pages
            item(key = "pager") {
                val ratio = (illust.width.toFloat() / illust.height).coerceIn(0.4f, 3f)
                Box(Modifier.fillMaxWidth()) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().aspectRatio(ratio),
                    ) { page ->
                        Box(Modifier.fillMaxSize()) {
                            RemoteImage(illust.largeUrls.getOrElse(page) { illust.imageUrl }, false, Modifier.fillMaxSize(), crop = false)
                            TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Viewer(illust, page)) }
                        }
                    }
                    if (pageCount > 1) {
                        Text(
                            "${pagerState.currentPage + 1}/$pageCount",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        "Show all",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { nav.push(Screen.Viewer(illust, pagerState.currentPage)) }
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                    )
                }
            }

            // ---- like / share / more
            item(key = "actions") {
                val liked = Likes.isLiked(illust)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LikeButton(illust)
                    Text(if (liked) "Liked" else "Like", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { shareIllust(context, illust) }) {
                        Icon(imageVector = MiuixIcons.Share, contentDescription = "Share", tint = MiuixTheme.colorScheme.onSurface)
                    }
                    PostMenuButton(illust, onNotInterested = {
                        Filters.dislike(illust)
                        nav.pop()
                    })
                }
            }

            // ---- title, caption
            item(key = "caption") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(illust.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    if (captionText.isNotBlank()) {
                        Text(
                            captionText,
                            fontSize = 14.sp,
                            maxLines = if (captionExpanded) Int.MAX_VALUE else 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        if (!captionExpanded && (captionText.length > 90 || captionText.count { it == '\n' } >= 3)) {
                            Text(
                                "Continue reading",
                                color = summary,
                                fontSize = 13.sp,
                                modifier = Modifier.align(Alignment.End).clickable { captionExpanded = true }.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            // ---- tags
            if (illust.tagList.isNotEmpty()) {
                item(key = "tags") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        illust.tagList.forEach { t ->
                            Text(
                                "#${t.name}" + if (t.translated.isNotBlank()) " ${t.translated}" else "",
                                color = MiuixTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                modifier = Modifier.clickable { nav.push(Screen.FeedList("#${t.name}", "search:${t.name}")) },
                            )
                        }
                    }
                }
            }

            // ---- stats + date
            item(key = "stats") { Column {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatChip(MiuixIcons.Messages, illust.totalComments)
                        StatChip(MiuixIcons.Favorites, Likes.count(illust))
                        StatChip(MiuixIcons.Show, illust.views)
                    }
                    if (illust.createDate.isNotBlank()) {
                        Text(formatPostDate(illust.createDate), color = summary, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                    }
                }
                Divider()
            } }

            // ---- artist + their works
            item(key = "artist") { Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(40.dp).clip(CircleShape)) {
                        RemoteImage(illust.userAvatar, true, Modifier.fillMaxSize())
                        TapOverlay(Modifier.matchParentSize()) { nav.openUser(illust.userId) }
                    }
                    Text(
                        illust.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable { nav.openUser(illust.userId) }.padding(horizontal = 10.dp),
                    )
                    FollowButton(illust.userId, illust.userFollowed)
                }
                val others = authorWorks.items.filter { it.id != illust.id }
                if (others.isNotEmpty() || authorWorks.nextUrl != null) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(others, key = { it.id }) { w -> StripTile(w) }
                        item(key = "more") {
                            val next = authorWorks.nextUrl
                            if (next != null) {
                                LaunchedEffect(next) { authorWorks.loadMore() }
                                Box(Modifier.size(128.dp), contentAlignment = Alignment.Center) { InfiniteProgressIndicator() }
                            }
                        }
                    }
                }
                Button(
                    onClick = { nav.openUser(illust.userId) },
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    cornerRadius = 50.dp,
                ) { Text("Show all", fontWeight = FontWeight.Bold) }
                Divider()
            } }

            // ---- comments
            item(key = "comments") { Column {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Comments", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            illust.totalComments.toString(),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .background(MiuixTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                                .padding(horizontal = 8.dp, vertical = 1.dp),
                        )
                    }
                    replyTo?.let { r ->
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Replying to ${r.userName}", color = summary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("Cancel", color = MiuixTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.clickable { replyTo = null })
                        }
                    }
                    Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RemoteImage(Pixiv.session?.avatar.orEmpty(), true, Modifier.size(36.dp).clip(CircleShape))
                        TextField(
                            value = input,
                            onValueChange = { input = it },
                            label = "Leave a comment",
                            useLabelAsPlaceholder = true,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        IconButton(
                            enabled = !sending && input.text.isNotBlank(),
                            onClick = {
                                val text = input.text.trim()
                                sending = true
                                scope.launch {
                                    try {
                                        Pixiv.addComment(illust.id, text, replyTo?.id)
                                        input = TextFieldValue("")
                                        replyTo = null
                                        loadComments()
                                    } catch (e: CancellationException) {
                                        throw e
                                    } catch (e: Exception) {
                                        Toast.makeText(context, e.message ?: "Could not post comment", Toast.LENGTH_LONG).show()
                                    } finally {
                                        sending = false
                                    }
                                }
                            },
                        ) {
                            Icon(imageVector = MiuixIcons.Send, contentDescription = "Send", tint = MiuixTheme.colorScheme.primary)
                        }
                    }
                    comments.forEach { c ->
                        Row(Modifier.fillMaxWidth().padding(top = 14.dp)) {
                            Box(Modifier.size(36.dp).clip(CircleShape)) {
                                RemoteImage(c.userAvatar, true, Modifier.fillMaxSize())
                                TapOverlay(Modifier.matchParentSize()) { nav.openUser(c.userId) }
                            }
                            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(c.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                                if (c.stampUrl != null) {
                                    RemoteImage(c.stampUrl, false, Modifier.padding(top = 4.dp).size(72.dp), crop = false)
                                } else {
                                    Text(c.text, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                                Row(Modifier.padding(top = 4.dp)) {
                                    Text(relativeTime(c.date), color = summary, fontSize = 12.sp)
                                    Text(
                                        "Reply",
                                        color = MiuixTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(start = 12.dp).clickable { replyTo = c },
                                    )
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { nav.push(Screen.Comments(illust)) },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        cornerRadius = 50.dp,
                    ) { Text("See more", fontWeight = FontWeight.Bold) }
                }
                Divider()
            } }

            // ---- related
            item(key = "relatedHeader") {
                Text("Related work", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(12.dp))
            }
            illustGrid(related, "No related works", "related")
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, value: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        Text(fmt(value), fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun StripTile(w: Illust) {
    val nav = LocalNavigator.current
    Box(Modifier.size(128.dp).clip(RoundedCornerShape(10.dp))) {
        RemoteImage(w.imageUrl, false, Modifier.fillMaxSize(), crop = true)
        TapOverlay(Modifier.matchParentSize()) { nav.push(Screen.Post(w)) }
        if (w.pageCount > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = MiuixIcons.Copy, contentDescription = null, modifier = Modifier.size(10.dp), tint = Color.White)
                Text(w.pageCount.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(start = 3.dp))
            }
        }
        LikeButton(w, Modifier.align(Alignment.BottomEnd), idleTint = Color.White)
    }
}
