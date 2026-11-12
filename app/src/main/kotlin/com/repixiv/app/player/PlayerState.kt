package com.repixiv.app.player

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject

/** Favorite tracks, saved on this device. */
object PlayerStore {
    private var prefs: SharedPreferences? = null
    val favorites = mutableStateListOf<Track>()

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_player", Context.MODE_PRIVATE)
        prefs = p
        runCatching {
            val arr = JSONArray(p.getString("favorites", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                favorites += Track(o.getString("id"), o.optString("title"), o.optString("artist"), o.optString("image"), o.optString("uri"))
            }
        }
    }

    fun isFavorite(id: String) = favorites.any { it.id == id }

    fun toggle(track: Track) {
        val existing = favorites.indexOfFirst { it.id == track.id }
        if (existing >= 0) favorites.removeAt(existing) else favorites.add(0, track)
        val arr = JSONArray()
        favorites.forEach {
            arr.put(JSONObject().put("id", it.id).put("title", it.title).put("artist", it.artist).put("image", it.image).put("uri", it.uri))
        }
        prefs?.edit()?.putString("favorites", arr.toString())?.apply()
    }
}

/** What's playing right now (mirrors the Spotify app). */
object PlayerState {
    var current by mutableStateOf<Track?>(null)
    var isPlaying by mutableStateOf(false)
    var repeat by mutableStateOf("off")
    var volume by mutableFloatStateOf(50f)
    var optionsOpen by mutableStateOf(false)
    var lastUserPlayAt = 0L
    val forYou = mutableStateListOf<Track>()
    var forYouError by mutableStateOf<String?>(null)
    var forYouLoading by mutableStateOf(false)
}

object PlayerController {
    private fun friendly(e: SpotifyException): String = when {
        e.reason == "PREMIUM_REQUIRED" -> "Spotify Premium is required to control playback."
        e.reason == "NO_ACTIVE_DEVICE" || e.httpCode == 404 -> "Open the Spotify app and play anything once, then try again."
        else -> e.message ?: "Spotify error"
    }

    private fun wakeSpotify(context: Context) {
        context.packageManager.getLaunchIntentForPackage("com.spotify.music")?.let {
            it.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(it) }
        }
    }

    /** Plays [track] (or resumes when null). Returns an error message, or null on success. */
    suspend fun play(context: Context, track: Track?): String? {
        PlayerState.lastUserPlayAt = System.currentTimeMillis()
        return try {
            Spotify.play(track?.uri)
            PlayerState.isPlaying = true
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: SpotifyException) {
            if (e.httpCode == 404 || e.reason == "NO_ACTIVE_DEVICE") {
                wakeSpotify(context)
                delay(3000)
                try {
                    Spotify.play(track?.uri)
                    PlayerState.isPlaying = true
                    null
                } catch (e2: SpotifyException) {
                    friendly(e2)
                }
            } else {
                friendly(e)
            }
        } catch (e: Exception) {
            e.message ?: "Could not start playback"
        }
    }

    suspend fun run(block: suspend () -> Unit): String? = try {
        block()
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: SpotifyException) {
        friendly(e)
    } catch (e: Exception) {
        e.message ?: "Spotify error"
    }
}
