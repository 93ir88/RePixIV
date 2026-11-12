package com.repixiv.app.player

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

/** Favorites and play counts ("most heard"), saved on this device. */
object PlayerStore {
    private var prefs: SharedPreferences? = null
    val favorites = mutableStateListOf<Track>()
    private val counts = mutableStateMapOf<String, Int>()
    private val known = HashMap<String, Track>()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_player", Context.MODE_PRIVATE)
        prefs = p
        // Earlier builds stored Audius track ids, which don't exist on YouTube: start fresh once.
        if (p.getString("source", null) != "youtube") {
            p.edit().remove("favorites").remove("plays").putString("source", "youtube").apply()
        }
        runCatching {
            val arr = JSONArray(p.getString("favorites", "[]"))
            for (i in 0 until arr.length()) favorites += trackOf(arr.getJSONObject(i))
        }
        runCatching {
            val arr = JSONArray(p.getString("plays", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val t = trackOf(o)
                known[t.id] = t
                counts[t.id] = o.optInt("count", 1)
            }
        }
    }

    private fun trackOf(o: JSONObject) =
        Track(o.getString("id"), o.optString("title"), o.optString("artist"), o.optString("image"), o.optString("uri"))

    private fun JSONObject.fill(t: Track): JSONObject =
        put("id", t.id).put("title", t.title).put("artist", t.artist).put("image", t.image).put("uri", t.uri)

    fun isFavorite(id: String) = favorites.any { it.id == id }

    fun toggle(track: Track) {
        val existing = favorites.indexOfFirst { it.id == track.id }
        if (existing >= 0) favorites.removeAt(existing) else favorites.add(0, track)
        val arr = JSONArray()
        favorites.forEach { arr.put(JSONObject().fill(it)) }
        prefs?.edit()?.putString("favorites", arr.toString())?.apply()
    }

    fun recordPlay(track: Track) {
        known[track.id] = track
        counts[track.id] = (counts[track.id] ?: 0) + 1
        // keep the 200 most played
        if (counts.size > 200) {
            counts.entries.sortedBy { it.value }.take(counts.size - 200).forEach {
                counts.remove(it.key)
                known.remove(it.key)
            }
        }
        val arr = JSONArray()
        counts.forEach { (id, c) -> known[id]?.let { arr.put(JSONObject().fill(it).put("count", c)) } }
        prefs?.edit()?.putString("plays", arr.toString())?.apply()
    }

    /** Your most played tracks, most played first. */
    fun mostPlayed(limit: Int): List<Track> =
        counts.entries.sortedByDescending { it.value }.take(limit).mapNotNull { known[it.key] }
}

/** What's playing right now. */
object PlayerState {
    var current by mutableStateOf<Track?>(null)
    var isPlaying by mutableStateOf(false)
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var repeat by mutableStateOf("off")
    var speed by mutableFloatStateOf(1f)
    var volume by mutableFloatStateOf(80f)
    var optionsOpen by mutableStateOf(false)
    val trending = mutableStateListOf<Track>()
    var trendingError by mutableStateOf<String?>(null)
    var trendingLoading by mutableStateOf(false)
}

object PlayerController {
    /** Plays [track] within [queue] and counts it as a listen. Returns an error message or null. */
    suspend fun play(context: Context, track: Track, queue: List<Track>): String? = try {
        PlayerState.error = null
        AppPlayer.play(context, track, queue)
        PlayerStore.recordPlay(track)
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        PlayerState.loading = false
        PlayerState.isPlaying = false
        "Couldn't play this track: ${e.message ?: "unknown error"}"
    }
}
