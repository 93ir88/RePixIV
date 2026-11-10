package com.repixiv.app.pixiv

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

/** Paged list of illusts. [fetch] receives the `next_url` (null for the first page). */
class FeedState(private val fetch: suspend (String?) -> IllustPage) {
    val items = mutableStateListOf<Illust>()
    var nextUrl by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loaded by mutableStateOf(false)
        private set

    suspend fun refresh() {
        if (loading) return
        loading = true
        error = null
        try {
            val p = fetch(null)
            items.clear()
            items.addAll(p.items.distinctBy { it.id })
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
            items.addAll(p.items.filter { seen.add(it.id) })
            nextUrl = p.nextUrl
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Network error"
        } finally {
            loading = false
        }
    }
}
