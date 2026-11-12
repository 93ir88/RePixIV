package com.repixiv.app.player

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val image: String,
    val uri: String = "",
)

/**
 * Audius (https://audius.co) public API: open music from independent artists, free to stream.
 * Only tracks the artist has made streamable are returned.
 */
object Audius {
    private const val BASE = "https://api.audius.co/v1"
    private const val APP = "RePixIV"

    suspend fun trending(): List<Track> = withContext(Dispatchers.IO) {
        parse(get("$BASE/tracks/trending?limit=40&time=week&app_name=$APP"))
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        parse(get("$BASE/tracks/search?query=${enc(query)}&limit=40&app_name=$APP"))
    }

    /** Direct audio URL for [id] (falls back to the redirecting stream endpoint). */
    suspend fun resolveStream(id: String): String = withContext(Dispatchers.IO) {
        val base = "$BASE/tracks/${enc(id)}/stream?app_name=$APP"
        runCatching {
            val j = JSONObject(get("$base&no_redirect=true"))
            j.optString("data").takeIf { it.startsWith("http") }
        }.getOrNull() ?: base
    }

    private fun parse(text: String): List<Track> {
        val arr: JSONArray = JSONObject(text).optJSONArray("data") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) runCatching {
            val o = arr.getJSONObject(i)
            if (o.has("is_streamable") && !o.optBoolean("is_streamable", true)) return@runCatching
            val art = o.optJSONObject("artwork")
            val image = art?.optString("480x480").orEmpty().ifEmpty { art?.optString("150x150").orEmpty() }
            out += Track(
                id = o.getString("id"),
                title = o.optString("title"),
                artist = o.optJSONObject("user")?.optString("name").orEmpty(),
                image = image,
            )
        }
        return out.distinctBy { it.id }
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            c.setRequestProperty("User-Agent", "RePixIV")
            val code = c.responseCode
            val text = (if (code >= 400) c.errorStream else c.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IllegalStateException("Audius request failed (HTTP $code)")
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
