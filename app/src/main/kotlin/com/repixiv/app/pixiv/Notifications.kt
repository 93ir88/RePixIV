package com.repixiv.app.pixiv

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/** "X replied to your comment" entry. */
data class NotificationItem(
    val id: Long,
    val illustId: Long,
    val rootId: Long,
    val userId: Long,
    val userName: String,
    val userAvatar: String,
    val text: String,
    val stampUrl: String?,
    val date: String,
    val seen: Boolean = false,
)

/**
 * Reply notifications.
 *
 * pixiv's app API has no notification feed we can call, so this watches the comment threads you take part in:
 * every comment you post (or that is found under your account when you open a post's comments) is remembered,
 * and the replies under those threads are re-checked in the background. New replies from other people become
 * notifications and drive the badge on the bell.
 */
object Notifications {
    private const val MAX_THREADS = 40
    private const val MAX_ITEMS = 100

    private class Watched(
        val illustId: Long,
        val rootId: Long,
        val known: MutableSet<Long>,
        var baselined: Boolean,
    )

    private var prefs: SharedPreferences? = null
    private val threads = LinkedHashMap<Long, Watched>()
    private val lock = Mutex()

    val items = mutableStateListOf<NotificationItem>()
    var refreshing by mutableStateOf(false)
        private set

    /** Number shown in the badge. Reads snapshot state, so composables recompose when it changes. */
    val unreadCount: Int get() = items.count { !it.seen }

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_notifications", Context.MODE_PRIVATE)
        prefs = p
        load()
    }

    private fun owner(): Long = Pixiv.session?.userId ?: 0L

    private fun load() {
        val p = prefs ?: return
        val me = owner()
        if (p.getLong("owner", 0L) != me) {
            // Different account (or logged out): start clean.
            p.edit().clear().putLong("owner", me).apply()
            return
        }
        runCatching {
            JSONArray(p.getString("threads", "[]")).let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val known = HashSet<Long>()
                    o.optJSONArray("known")?.let { k -> for (j in 0 until k.length()) known += k.getLong(j) }
                    threads[o.getLong("root")] = Watched(o.getLong("illust"), o.getLong("root"), known, o.optBoolean("base"))
                }
            }
            JSONArray(p.getString("items", "[]")).let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    items += NotificationItem(
                        id = o.getLong("id"),
                        illustId = o.getLong("illust"),
                        rootId = o.getLong("root"),
                        userId = o.optLong("uid"),
                        userName = o.optString("name"),
                        userAvatar = o.optString("avatar"),
                        text = o.optString("text"),
                        stampUrl = o.optString("stamp").takeIf { it.isNotBlank() },
                        date = o.optString("date"),
                        seen = o.optBoolean("seen"),
                    )
                }
            }
        }
    }

    private fun save() {
        val p = prefs ?: return
        val tArr = JSONArray()
        threads.values.forEach { t ->
            tArr.put(
                JSONObject()
                    .put("illust", t.illustId).put("root", t.rootId).put("base", t.baselined)
                    .put("known", JSONArray(t.known.toList().takeLast(300))),
            )
        }
        val iArr = JSONArray()
        items.forEach { n ->
            iArr.put(
                JSONObject()
                    .put("id", n.id).put("illust", n.illustId).put("root", n.rootId).put("uid", n.userId)
                    .put("name", n.userName).put("avatar", n.userAvatar).put("text", n.text)
                    .put("stamp", n.stampUrl.orEmpty()).put("date", n.date).put("seen", n.seen),
            )
        }
        p.edit().putLong("owner", owner()).putString("threads", tArr.toString()).putString("items", iArr.toString()).apply()
    }

    /**
     * Start watching a thread. [known] are replies the user has already seen. With [baselined] false, the first
     * check just records what is there (used for threads found by scanning, whose replies may predate us).
     */
    fun trackThread(illustId: Long, rootId: Long, known: Collection<Long> = emptyList(), baselined: Boolean = true) {
        if (rootId <= 0 || Pixiv.session == null) return
        val existing = threads[rootId]
        if (existing != null) {
            existing.known.addAll(known)
            return
        }
        threads[rootId] = Watched(illustId, rootId, known.toMutableSet(), baselined)
        while (threads.size > MAX_THREADS) threads.remove(threads.keys.first())
        save()
    }

    /** Remember the threads of every comment on [illustId] that was written by the signed-in user. */
    fun trackFromComments(illustId: Long, comments: List<Comment>) {
        val me = owner()
        if (me == 0L) return
        comments.forEach { c ->
            if (c.userId == me && !c.pending && c.id > 0) trackThread(illustId, c.id, baselined = false)
        }
    }

    /** A reply of the user's own was found/loaded under [rootId]: watch that thread. */
    fun trackFromReplies(illustId: Long, rootId: Long, replies: List<Comment>) {
        val me = owner()
        if (me == 0L || rootId <= 0) return
        if (replies.any { it.userId == me && !it.pending }) {
            trackThread(illustId, rootId, replies.filter { !it.pending && it.id > 0 }.map { it.id }, baselined = true)
        }
    }

    suspend fun refresh() {
        val me = owner()
        if (me == 0L || prefs == null) return
        if (!lock.tryLock()) return
        refreshing = true
        try {
            val snapshot = threads.values.toList()
            val results = coroutineScope {
                snapshot.chunked(5).flatMap { chunk ->
                    chunk.map { t ->
                        async {
                            t to runCatching {
                                val all = ArrayList<Comment>()
                                var next: String? = null
                                var pages = 0
                                do {
                                    val p = Pixiv.commentReplies(t.rootId, next)
                                    all += p.items
                                    next = p.nextUrl
                                    pages++
                                } while (next != null && pages < 3)
                                all
                            }.getOrNull()
                        }
                    }.awaitAll()
                }
            }
            var changed = false
            val fresh = ArrayList<NotificationItem>()
            for ((t, replies) in results) {
                if (replies == null) continue
                if (!t.baselined) {
                    t.known.addAll(replies.map { it.id })
                    t.baselined = true
                    changed = true
                    continue
                }
                for (r in replies) {
                    if (!t.known.add(r.id)) continue
                    changed = true
                    if (r.userId == me) continue
                    fresh += NotificationItem(
                        id = r.id, illustId = t.illustId, rootId = t.rootId, userId = r.userId,
                        userName = r.userName, userAvatar = r.userAvatar, text = r.text,
                        stampUrl = r.stampUrl, date = r.date,
                    )
                }
            }
            if (fresh.isNotEmpty()) {
                fresh.sortedBy { parseIsoMillis(it.date) ?: 0L }.forEach { n ->
                    if (items.none { it.id == n.id }) items.add(0, n)
                }
                while (items.size > MAX_ITEMS) items.removeAt(items.lastIndex)
            }
            if (changed) save()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        } finally {
            refreshing = false
            lock.unlock()
        }
    }

    fun markAllSeen() {
        var any = false
        for (i in items.indices) {
            if (!items[i].seen) {
                items[i] = items[i].copy(seen = true)
                any = true
            }
        }
        if (any) save()
    }

    /** Forget everything (logout). */
    fun reset() {
        threads.clear()
        items.clear()
        prefs?.edit()?.clear()?.apply()
    }

    fun clear() {
        items.clear()
        save()
    }
}
