package com.repixiv.app.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.pixiv.Comment
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.util.RemoteImage
import com.repixiv.app.util.relativeTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** What the comment box is currently replying to. [root] is the top-level comment pixiv attaches the reply to. */
data class ReplyTarget(val root: Comment, val target: Comment)

/** Loaded / expanded state of the replies under one top-level comment. */
class ReplyThread {
    val items = mutableStateListOf<Comment>()
    var next by mutableStateOf<String?>(null)
    var loading by mutableStateOf(false)
    var loaded by mutableStateOf(false)
    var expanded by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    suspend fun load(rootId: Long, reset: Boolean) {
        if (loading) return
        loading = true
        error = null
        try {
            val p = Pixiv.commentReplies(rootId, if (reset) null else next)
            if (reset) items.clear()
            val seen = items.mapTo(HashSet()) { it.id }
            items.addAll(p.items.filter { seen.add(it.id) })
            next = p.nextUrl
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load replies"
        } finally {
            loading = false
        }
    }
}

/** Reloads (and expands) the replies of [rootId], e.g. right after the user posted one. */
suspend fun SnapshotStateMap<Long, ReplyThread>.refreshReplies(rootId: Long) {
    val t = getOrPut(rootId) { ReplyThread() }
    t.expanded = true
    t.load(rootId, reset = true)
}

@Composable
private fun CommentBody(
    c: Comment,
    avatarSize: Dp,
    textSize: Int,
    dateText: String,
    onReply: () -> Unit,
    extraActions: @Composable () -> Unit = {},
) {
    val nav = LocalNavigator.current
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.size(avatarSize).clip(CircleShape)) {
            RemoteImage(c.userAvatar, true, Modifier.fillMaxSize())
            TapOverlay(Modifier.matchParentSize()) { nav.openUser(c.userId) }
        }
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text(c.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            if (c.stampUrl != null) {
                RemoteImage(c.stampUrl, false, Modifier.padding(top = 4.dp).size(72.dp), crop = false)
            } else {
                Text(c.text, fontSize = textSize.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(dateText, color = summary, fontSize = 12.sp)
                // Generous tap target: the old 12sp text link was very hard to hit.
                Text(
                    "Reply",
                    color = MiuixTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(onClick = onReply)
                        .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                )
                extraActions()
            }
        }
    }
}

/**
 * One top-level comment with its "View replies" toggle and (when expanded) its replies.
 * [threads] lives in the parent screen so expanded state survives scrolling.
 */
@Composable
internal fun CommentThread(
    c: Comment,
    threads: SnapshotStateMap<Long, ReplyThread>,
    avatarSize: Dp,
    textSize: Int,
    dateText: (Comment) -> String,
    onReply: (ReplyTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val summary = MiuixTheme.colorScheme.onSurfaceVariantSummary
    val thread = threads.getOrPut(c.id) { ReplyThread() }
    val hasReplies = c.hasReplies || thread.items.isNotEmpty()

    Column(modifier.fillMaxWidth()) {
        CommentBody(
            c = c,
            avatarSize = avatarSize,
            textSize = textSize,
            dateText = dateText(c),
            onReply = { onReply(ReplyTarget(root = c, target = c)) },
            extraActions = {
                if (hasReplies) {
                    Text(
                        when {
                            thread.expanded -> "Hide replies"
                            thread.loaded -> "See replies (${thread.items.size})"
                            else -> "See replies"
                        },
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable {
                                if (thread.expanded) {
                                    thread.expanded = false
                                } else {
                                    thread.expanded = true
                                    if (!thread.loaded) scope.launch { thread.load(c.id, reset = true) }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            },
        )
        if (thread.expanded) {
            Column(Modifier.padding(start = avatarSize / 2 + 10.dp, top = 6.dp)) {
                thread.items.forEach { r ->
                    Box(Modifier.padding(top = 8.dp)) {
                        CommentBody(
                            c = r,
                            avatarSize = 28.dp,
                            textSize = textSize - 1,
                            dateText = dateText(r),
                            onReply = { onReply(ReplyTarget(root = c, target = r)) },
                        )
                    }
                }
                when {
                    thread.loading -> Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        InfiniteProgressIndicator()
                    }
                    thread.error != null -> Text(
                        "${thread.error} — tap to retry",
                        color = MiuixTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { scope.launch { thread.load(c.id, reset = !thread.loaded) } }
                            .padding(vertical = 8.dp),
                    )
                    thread.next != null -> Text(
                        "More replies",
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { scope.launch { thread.load(c.id, reset = false) } }
                            .padding(vertical = 8.dp),
                    )
                    thread.loaded && thread.items.isEmpty() -> Text(
                        "No replies",
                        color = summary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
