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

/** First comment of a post, shown as a preview under it. */
object CommentPreviews {
    val cache = mutableStateMapOf<Long, Comment>()
    private val inFlight = HashSet<Long>()

    suspend fun load(illustId: Long) {
        if (illustId in cache || !inFlight.add(illustId)) return
        try {
            Pixiv.comments(illustId).items.firstOrNull()?.let { cache[illustId] = it }
        } catch (e: CancellationException) {
            inFlight.remove(illustId)
            throw e
        } catch (_: Exception) {
        }
    }
}
