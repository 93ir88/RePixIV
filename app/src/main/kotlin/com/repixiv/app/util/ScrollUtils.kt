package com.repixiv.app.util

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState

/** Scrolls to the very top in [durationMs] (0.5 s by default), however far down the list is. */
suspend fun LazyListState.scrollToTopFast(durationMs: Int = 500) {
    if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) return
    val visible = layoutInfo.visibleItemsInfo
    val avg = if (visible.isEmpty()) 600f else visible.sumOf { it.size }.toFloat() / visible.size
    var index = firstVisibleItemIndex
    var offset = firstVisibleItemScrollOffset
    if (index > 6) {
        // far away: jump close first so the 0.5 s animation covers a short, smooth distance
        scrollToItem(6)
        index = 6
        offset = 0
    }
    animateScrollBy(-(index * avg + offset), tween(durationMs, easing = FastOutSlowInEasing))
    // the size estimate can fall short, so make sure we're really at the top
    if (firstVisibleItemIndex != 0 || firstVisibleItemScrollOffset != 0) scrollToItem(0)
}
