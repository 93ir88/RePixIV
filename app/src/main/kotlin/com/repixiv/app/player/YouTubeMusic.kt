package com.repixiv.app.player

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** A playable item. [id] is a YouTube video id. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val image: String,
    val uri: String = "",
)

/**
 * Music from the signed-in Google account, through the official YouTube Data API v3.
 * (YouTube Music itself has no public API; its songs and videos are YouTube videos in the "Music" category.)
 * Only embeddable videos are returned, because playback uses YouTube's official embedded player ([AppPlayer]).
 */
object YouTubeMusic {
    private const val API = "https://www.googleapis.com/youtube/v3"
    private const val MUSIC_CATEGORY = "10"

    /** "For You" feed: music you liked on YouTube / YouTube Music, then what's popular in music. */
    suspend fun forYou(): List<Track> = withContext(Dispatchers.IO) {
        val liked = runCatching {
            parse(
                get("$API/videos?part=snippet,status&myRating=like&maxResults=50"),
                musicOnly = true,
            )
        }.getOrDefault(emptyList())
        (liked + popular()).distinctBy { it.id }
    }

    private fun popular(): List<Track> {
        val region = Locale.getDefault().country.ifEmpty { "US" }
        // Not every region has a music chart, so fall back to the US one.
        for (r in listOf(region, "US").distinct()) {
            val t = runCatching {
                parse(
                    get("$API/videos?part=snippet,status&chart=mostPopular&videoCategoryId=$MUSIC_CATEGORY&maxResults=40&regionCode=$r"),
                    musicOnly = false,
                )
            }
            t.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
            if (r == "US") t.exceptionOrNull()?.let { throw it }
        }
        return emptyList()
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        parse(
            get(
                "$API/search?part=snippet&type=video&videoCategoryId=$MUSIC_CATEGORY&videoEmbeddable=true" +
                    "&maxResults=30&q=${enc(query)}",
            ),
            musicOnly = false,
        )
    }

    private fun parse(text: String, musicOnly: Boolean): List<Track> {
        val arr: JSONArray = JSONObject(text).optJSONArray("items") ?: return emptyList()
        val out = ArrayList<Track>()
        for (i in 0 until arr.length()) runCatching {
            val o = arr.getJSONObject(i)
            // videos.list has "id" as a string, search.list has {"videoId": ...}
            val id = o.optJSONObject("id")?.optString("videoId") ?: o.optString("id")
            if (id.isEmpty()) return@runCatching
            val sn = o.getJSONObject("snippet")
            if (musicOnly && sn.optString("categoryId") != MUSIC_CATEGORY) return@runCatching
            val status = o.optJSONObject("status")
            if (status != null && !status.optBoolean("embeddable", true)) return@runCatching
            val th = sn.optJSONObject("thumbnails")
            val image = listOf("high", "medium", "default")
                .firstNotNullOfOrNull { th?.optJSONObject(it)?.optString("url")?.takeIf { u -> u.isNotEmpty() } }
                .orEmpty()
            out += Track(
                id = id,
                title = clean(sn.optString("title")),
                artist = clean(sn.optString("channelTitle")).removeSuffix(" - Topic"),
                image = image,
            )
        }
        return out.distinctBy { it.id }
    }

    /** The API returns HTML entities (&amp;, &#39;) in titles. */
    private fun clean(s: String): String = Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString().trim()

    private fun get(url: String, retry: Boolean = true): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "GET"
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            c.setRequestProperty("Authorization", "Bearer ${GoogleAuth.accessTokenBlocking()}")
            val code = c.responseCode
            val text = (if (code >= 400) c.errorStream else c.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code == 401 && retry) {
                GoogleAuth.accessTokenBlocking(forceRefresh = true)
                return get(url, retry = false)
            }
            if (code !in 200..299) throw IllegalStateException(errorMessage(code, text))
            return text
        } finally {
            c.disconnect()
        }
    }

    private fun errorMessage(code: Int, body: String): String {
        val err = runCatching { JSONObject(body).getJSONObject("error") }.getOrNull()
        val reason = err?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
        return when (reason) {
            "quotaExceeded", "rateLimitExceeded" -> "YouTube's daily API quota for this app is used up, try again tomorrow"
            "accessNotConfigured", "SERVICE_DISABLED" -> "YouTube Data API v3 is not enabled for this Google Cloud project"
            else -> err?.optString("message")?.takeIf { it.isNotEmpty() } ?: "YouTube request failed (HTTP $code)"
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
