package com.repixiv.app.pixiv

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException

/** Optimistic like (bookmark) state shared by every list and the viewer. */
object Likes {
    private val overrides = mutableStateMapOf<Long, Boolean>()

    fun isLiked(a: Illust): Boolean = overrides[a.id] ?: a.isBookmarked

    fun count(a: Illust): Int =
        (a.bookmarks + (if (isLiked(a)) 1 else 0) - (if (a.isBookmarked) 1 else 0)).coerceAtLeast(0)

    suspend fun toggle(a: Illust) {
        val target = !isLiked(a)
        overrides[a.id] = target
        try {
            Pixiv.setBookmark(a.id, target)
        } catch (e: Exception) {
            overrides[a.id] = !target
            if (e is CancellationException) throw e
            throw e
        }
    }
}

/** First comment of a post (shown as a preview under it) plus the real comment count reported by the server. */
object CommentPreviews {
    val cache = mutableStateMapOf<Long, Comment>()
    val counts = mutableStateMapOf<Long, Int>()
    private val inFlight = HashSet<Long>()

    /** Comment count to display: the server-reported one when we have it, else whatever the feed said. */
    fun count(a: Illust): Int = counts[a.id] ?: a.totalComments

    fun update(illustId: Long, first: Comment?, total: Int) {
        if (first != null) cache[illustId] = first else cache.remove(illustId)
        if (total >= 0) counts[illustId] = total
    }

    suspend fun load(illustId: Long) {
        if ((illustId in cache && illustId in counts) || !inFlight.add(illustId)) return
        try {
            val p = Pixiv.comments(illustId)
            update(illustId, p.items.firstOrNull(), p.total.takeIf { it >= 0 } ?: p.items.size)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        } finally {
            inFlight.remove(illustId)
        }
    }
}
