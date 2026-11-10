package com.repixiv.app.pixiv

import android.content.Context
import android.content.SharedPreferences

/**
 * Local "Not interested" filter. pixiv's app API has no public "not interested" call, so this keeps
 * hidden posts and the tags of posts you dismissed on-device, and hides similar posts.
 */
object Filters {
    private var prefs: SharedPreferences? = null
    private val hidden = LinkedHashSet<Long>()
    private val tagCounts = HashMap<String, Int>()
    private val generic = setOf("original", "オリジナル")

    val hiddenCount: Int get() = hidden.size

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_filters", Context.MODE_PRIVATE)
        prefs = p
        p.getString("hidden", "")?.split(',')?.mapNotNullTo(hidden) { it.toLongOrNull() }
        p.getString("tags", "")?.lineSequence()?.forEach { line ->
            val parts = line.split('\t')
            if (parts.size == 2) parts[1].toIntOrNull()?.let { tagCounts[parts[0]] = it }
        }
    }

    fun dislike(i: Illust) {
        hidden.add(i.id)
        while (hidden.size > 3000) hidden.remove(hidden.first())
        i.tags.filter { it !in generic }.forEach { tagCounts[it] = (tagCounts[it] ?: 0) + 1 }
        persist()
    }

    /** Hidden if dismissed directly, if it has a tag dismissed 3+ times, or shares 3+ dismissed tags. */
    fun isHidden(i: Illust): Boolean {
        if (i.id in hidden) return true
        var matched = 0
        for (t in i.tags) {
            val c = tagCounts[t] ?: continue
            if (t in generic) continue
            if (c >= 3) return true
            matched++
        }
        return matched >= 3
    }

    fun reset() {
        hidden.clear()
        tagCounts.clear()
        persist()
    }

    private fun persist() {
        prefs?.edit()
            ?.putString("hidden", hidden.joinToString(","))
            ?.putString("tags", tagCounts.entries.joinToString("\n") { "${it.key}\t${it.value}" })
            ?.apply()
    }
}
