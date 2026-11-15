package com.repixiv.app.pages

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.repixiv.app.LocalNavigator
import com.repixiv.app.pixiv.Comment
import com.repixiv.app.pixiv.CommentPreviews
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Notifications
import com.repixiv.app.pixiv.Pixiv
import com.repixiv.app.util.RemoteImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    /** [silent] skips the spinner (used for background refreshes right after posting). */
    suspend fun load(rootId: Long, reset: Boolean, illustId: Long = 0L, silent: Boolean = false) {
        if (loading) return
        loading = !silent
        error = null
        try {
            val p = Pixiv.commentReplies(rootId, if (reset) null else next)
            if (reset) items.clear()
            val seen = items.mapTo(HashSet()) { it.id }
            items.addAll(p.items.filter { seen.add(it.id) })
            next = p.nextUrl
            loaded = true
            if (illustId != 0L) Notifications.trackFromReplies(illustId, rootId, items)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!silent) error = e.message ?: "Could not load replies"
        } finally {
            loading = false
        }
    }
}

// ---------------------------------------------------------------- posting (optimistic)

/** Posts outlive the screen that started them, so leaving the page mid-send doesn't drop the comment. */
private val postScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/** pixiv reports comment times in JST. */
private fun nowPixivIso(): String =
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+09:00'", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("Asia/Tokyo") }
        .format(java.util.Date())

/**
 * Shows the comment/reply immediately (dimmed, "pending") and sends it in the background; on success it is
 * swapped for the real one in place, on failure it is removed again and [onFailed] gets the text back.
 * Nothing is reloaded, so there is no wait.
 */
fun postComment(
    context: Context,
    illust: Illust,
    text: String,
    target: ReplyTarget?,
    threads: SnapshotStateMap<Long, ReplyThread>,
    insertTop: (Comment) -> Unit,
    resolveTop: (tempKey: Long, real: Comment?) -> Unit,
    reloadTop: suspend () -> Unit,
    onFailed: () -> Unit,
) {
    val appContext = context.applicationContext
    val me = Pixiv.session
    if (me == null) {
        Toast.makeText(appContext, "Not logged in", Toast.LENGTH_LONG).show()
        onFailed()
        return
    }
    val tempId = -System.nanoTime()
    val temp = Comment(
        id = tempId,
        text = text,
        date = nowPixivIso(),
        userId = me.userId,
        userName = me.name,
        userAvatar = me.avatar,
        stampUrl = null,
        parentId = target?.root?.id ?: 0L,
        pending = true,
        tempKey = tempId,
    )
    CommentPreviews.bump(illust.id, 1, CommentPreviews.count(illust))
    val thread = target?.let { threads.getOrPut(it.root.id) { ReplyThread() } }
    if (thread == null) {
        insertTop(temp)
    } else {
        thread.expanded = true
        thread.items.add(temp)
    }

    postScope.launch {
        try {
            val newId = Pixiv.addComment(illust.id, text, target?.root?.id)
            val real = newId?.let { temp.copy(id = it, pending = false) }
            if (thread == null) {
                if (real != null) {
                    resolveTop(temp.tempKey, real)
                    Notifications.trackThread(illust.id, real.id)
                } else {
                    reloadTop()
                }
            } else {
                val rootId = target!!.root.id
                val i = thread.items.indexOfFirst { it.tempKey == temp.tempKey }
                if (real != null && i >= 0) thread.items[i] = real
                if (real == null || !thread.loaded) thread.load(rootId, reset = true, illustId = illust.id, silent = true)
                Notifications.trackThread(
                    illust.id, rootId,
                    thread.items.filter { !it.pending && it.id > 0 }.map { it.id },
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (thread == null) resolveTop(temp.tempKey, null) else thread.items.removeAll { it.tempKey == temp.tempKey }
            CommentPreviews.bump(illust.id, -1, CommentPreviews.count(illust))
            Toast.makeText(appContext, e.message ?: "Could not post comment", Toast.LENGTH_LONG).show()
            onFailed()
        }
    }
}

// ---------------------------------------------------------------- shared pieces

/** Fades/expands content in the first time it appears when [animate] is true; static otherwise. */
@Composable
internal fun EnterAnimated(animate: Boolean, content: @Composable () -> Unit) {
    val state = remember { MutableTransitionState(!animate).apply { targetState = true } }
    AnimatedVisibility(
        visibleState = state,
        enter = fadeIn(tween(220)) + expandVertically(spring(dampingRatio = 1f, stiffness = 380f)),
        exit = fadeOut(tween(140)) + shrinkVertically(spring(dampingRatio = 1f, stiffness = 380f)),
    ) { content() }
}

/** Pill with the comment count; the number cross-fades when it changes. */
@Composable
internal fun CountChip(count: Int, modifier: Modifier = Modifier, fontSize: Int = 12) {
    AnimatedContent(
        targetState = count,
        modifier = modifier,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(140)) },
        label = "commentCount",
    ) { n ->
        Text(
            n.toString(),
            fontSize = fontSize.sp,
            modifier = Modifier
                .background(MiuixTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 1.dp),
        )
    }
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
    val shownAlpha by animateFloatAsState(if (c.pending) 0.55f else 1f, tween(220), label = "commentAlpha")
    Row(Modifier.fillMaxWidth().graphicsLayer { alpha = shownAlpha }) {
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
                Text(if (c.pending) "Sending…" else dateText, color = summary, fontSize = 12.sp)
                if (!c.pending) {
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
}

/**
 * One top-level comment with its "See replies" toggle and (when expanded) its replies.
 * [threads] lives in the parent screen so expanded state survives scrolling.
 */
@Composable
internal fun CommentThread(
    illustId: Long,
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
                                    if (!thread.loaded) scope.launch { thread.load(c.id, reset = true, illustId = illustId) }
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            },
        )
        AnimatedVisibility(
            visible = thread.expanded,
            enter = fadeIn(tween(220)) + expandVertically(spring(dampingRatio = 1f, stiffness = 380f)),
            exit = fadeOut(tween(140)) + shrinkVertically(spring(dampingRatio = 1f, stiffness = 380f)),
        ) {
            Column(Modifier.padding(start = avatarSize / 2 + 10.dp, top = 6.dp)) {
                thread.items.forEach { r ->
                    androidx.compose.runtime.key(r.key) {
                        EnterAnimated(animate = r.tempKey != 0L) {
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
                            .clickable { scope.launch { thread.load(c.id, reset = !thread.loaded, illustId = illustId) } }
                            .padding(vertical = 8.dp),
                    )
                    thread.next != null -> Text(
                        "More replies",
                        color = MiuixTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { scope.launch { thread.load(c.id, reset = false, illustId = illustId) } }
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
