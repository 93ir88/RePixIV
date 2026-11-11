package com.repixiv.app.pixiv

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Locale

/** Optimistic follow state shared by every follow button. */
object Follows {
    private val overrides = mutableStateMapOf<Long, Boolean>()

    fun isFollowing(userId: Long, default: Boolean): Boolean = overrides[userId] ?: default

    suspend fun toggle(userId: Long, current: Boolean) {
        val target = !current
        overrides[userId] = target
        try {
            Pixiv.setFollow(userId, target)
        } catch (e: Exception) {
            overrides[userId] = current
            throw e
        }
    }
}

/** On-device lists: favorite tags and search history. */
object LocalLists {
    private var prefs: SharedPreferences? = null
    val favoriteTags = mutableStateListOf<String>()
    val history = mutableStateListOf<String>()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_local", Context.MODE_PRIVATE)
        prefs = p
        p.getString("favoriteTags", "")?.split('\n')?.filter { it.isNotBlank() }?.let { favoriteTags.addAll(it) }
        p.getString("history", "")?.split('\n')?.filter { it.isNotBlank() }?.let { history.addAll(it) }
    }

    private fun save() {
        prefs?.edit()
            ?.putString("favoriteTags", favoriteTags.joinToString("\n"))
            ?.putString("history", history.joinToString("\n"))
            ?.apply()
    }

    fun addFavoriteTag(tag: String) {
        val t = tag.trim().removePrefix("#")
        if (t.isEmpty() || t in favoriteTags) return
        favoriteTags.add(0, t)
        save()
    }

    fun removeFavoriteTag(tag: String) {
        favoriteTags.remove(tag)
        save()
    }

    fun addHistory(word: String) {
        val w = word.trim()
        if (w.isEmpty()) return
        history.remove(w)
        history.add(0, w)
        while (history.size > 20) history.removeAt(history.lastIndex)
        save()
    }

    fun removeHistory(word: String) {
        history.remove(word)
        save()
    }

    fun clearHistory() {
        history.clear()
        save()
    }
}

/** Cover image for a favorite tag: the newest work that uses it. */
object TagCovers {
    val cache = mutableStateMapOf<String, String>()
    private val inFlight = HashSet<String>()

    suspend fun load(tag: String) {
        if (tag in cache || !inFlight.add(tag)) return
        try {
            Pixiv.search(tag).items.firstOrNull()?.imageUrl?.let { cache[tag] = it }
        } catch (e: CancellationException) {
            inFlight.remove(tag)
            throw e
        } catch (_: Exception) {
        }
    }
}

fun parseIsoMillis(iso: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(iso)?.time
}.getOrNull()
