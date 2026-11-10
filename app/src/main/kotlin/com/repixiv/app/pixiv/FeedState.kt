package com.repixiv.app.pixiv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

/** Paged list of illusts. [fetch] receives the `next_url` (null for the first page). */
class FeedState(
    private val applyFilters: Boolean = false,
    private val fetch: suspend (String?) -> IllustPage,
) {
    private fun List<Illust>.visible() = if (applyFilters) filterNot { Filters.isHidden(it) } else this

    val items = mutableStateListOf<Illust>()
    var nextUrl by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set

    suspend fun ensureLoaded() {
        if (!loaded && !loading) refresh()
    }

    suspend fun refresh() {
        if (loading) return
        loading = true
        error = null
        try {
            val p = fetch(null)
            items.clear()
            items.addAll(p.items.visible().distinctBy { it.id })
            nextUrl = p.nextUrl
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
        }
    }

    suspend fun loadMore() {
        val next = nextUrl ?: return
        if (loading) return
        loading = true
        error = null
        try {
            val p = fetch(next)
            val seen = items.mapTo(HashSet()) { it.id }
            items.addAll(p.items.visible().filter { seen.add(it.id) })
            nextUrl = p.nextUrl
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
        }
    }

    /** Dismisses [illust] from this list and (when filtering) hides similar ones too. */
    fun notInterested(illust: Illust) {
        Filters.dislike(illust)
        items.removeAll { it.id == illust.id || (applyFilters && Filters.isHidden(it)) }
    }
}
