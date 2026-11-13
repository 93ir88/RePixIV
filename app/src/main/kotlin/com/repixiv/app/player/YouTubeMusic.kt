package com.repixiv.app.player

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A playable item. [id] is a YouTube video id. */
data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val image: String,
    val uri: String = "",
)

/** One page of results; [next] is the token for the following page (null at the end). */
data class TrackPage(val tracks: List<Track>, val next: String?)

/**
 * Music from the signed-in Google account, through the official YouTube Data API v3.
 * (YouTube Music itself has no public API; its songs and videos are YouTube videos in the "Music" category.)
 * Only embeddable videos are returned, because playback uses YouTube's official embedded player ([AppPlayer]).
 */
object YouTubeMusic {
    private const val API = "https://www.googleapis.com/youtube/v3"
    private const val MUSIC_CATEGORY = "10"

    private val ARABIC = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

    /** True when the track's title or artist is written in Arabic script. */
    fun isArabic(t: Track): Boolean = ARABIC.containsMatchIn(t.title) || ARABIC.containsMatchIn(t.artist)

    private var regionQueue: List<String> = emptyList()
    private var regionIdx = 0

    /** Music chart regions that aren't Arabic-speaking. */
    private val RegionPool = listOf("US", "GB", "CA", "AU", "DE", "FR", "BR", "MX", "KR", "JP", "IN", "ES", "IT", "SE", "NL")

    /** "For You" feed, first page: music you liked on YouTube / YouTube Music, then what's popular in music. */
    suspend fun forYou(): TrackPage = withContext(Dispatchers.IO) {
        val liked = runCatching {
            parse(
                get("$API/videos?part=snippet,status&myRating=like&maxResults=50"),
                musicOnly = true,
                hideArabic = true,
            ).tracks
        }.getOrDefault(emptyList())
        val pop = popular(null)
        // shuffled so every refresh shows a different mix
        TrackPage((liked.shuffled() + pop.tracks.shuffled()).distinctBy { it.id }, pop.next)
    }

    /** Next page of the popular-music chart. */
    suspend fun morePopular(token: String): TrackPage = withContext(Dispatchers.IO) { popular(token) }

    private const val REGION_PREFIX = "region:"

    /**
     * Pages through the music charts of several regions one after another, so the feed doesn't end when one
     * chart does. A page token of the form "region:XX" means "start the chart of region XX".
     */
    private fun popular(token: String?): TrackPage {
        if (token == null) {
            regionQueue = RegionPool.shuffled()
            regionIdx = 0
        }
        var pageToken: String? = null
        if (token != null) {
            if (token.startsWith(REGION_PREFIX)) {
                val idx = regionQueue.indexOf(token.removePrefix(REGION_PREFIX))
                if (idx < 0) return TrackPage(emptyList(), null)
                regionIdx = idx
            } else {
                pageToken = token
            }
        }
        var lastError: Throwable? = null
        while (regionIdx < regionQueue.size) {
            val r = regionQueue[regionIdx]
            val page = if (pageToken != null) "&pageToken=${enc(pageToken)}" else ""
            val res = runCatching {
                parse(
                    get("$API/videos?part=snippet,status&chart=mostPopular&videoCategoryId=$MUSIC_CATEGORY&maxResults=50&regionCode=$r$page"),
                    musicOnly = false,
                    hideArabic = true,
                )
            }
            val p = res.getOrNull()
            if (p == null) {
                lastError = res.exceptionOrNull()
                // a failed later page keeps its token so Retry works; a region without a chart is skipped
                if (pageToken != null) throw lastError!!
                regionIdx++
                continue
            }
            if (p.next != null) return p
            // this region's chart is finished: continue with the next region's chart
            regionIdx++
            val nextToken = regionQueue.getOrNull(regionIdx)?.let { REGION_PREFIX + it }
            if (p.tracks.isEmpty() && nextToken != null) {
                pageToken = null
                continue
            }
            return TrackPage(p.tracks, nextToken)
        }
        if (lastError != null && token == null) throw lastError
        return TrackPage(emptyList(), null)
    }

    /** Search music; pass the previous page's [TrackPage.next] as [token] for more. */
    suspend fun search(query: String, token: String? = null): TrackPage = withContext(Dispatchers.IO) {
        val page = if (token != null) "&pageToken=${enc(token)}" else ""
        parse(
            get(
                "$API/search?part=snippet&type=video&videoCategoryId=$MUSIC_CATEGORY&videoEmbeddable=true" +
                    "&maxResults=30&q=${enc(query)}$page",
            ),
            musicOnly = false,
        )
    }

    private fun parse(text: String, musicOnly: Boolean, hideArabic: Boolean = false): TrackPage {
        val root = JSONObject(text)
        val next = root.optString("nextPageToken").ifEmpty { null }
        val arr: JSONArray = root.optJSONArray("items") ?: return TrackPage(emptyList(), next)
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
            if (hideArabic) {
                val lang = sn.optString("defaultAudioLanguage").ifEmpty { sn.optString("defaultLanguage") }
                if (lang.startsWith("ar", ignoreCase = true)) return@runCatching
            }
            val th = sn.optJSONObject("thumbnails")
            val image = listOf("high", "medium", "default")
                .firstNotNullOfOrNull { th?.optJSONObject(it)?.optString("url")?.takeIf { u -> u.isNotEmpty() } }
                .orEmpty()
            val track = Track(
                id = id,
                title = clean(sn.optString("title")),
                artist = clean(sn.optString("channelTitle")).removeSuffix(" - Topic"),
                image = image,
            )
            if (hideArabic && isArabic(track)) return@runCatching
            out += track
        }
        return TrackPage(out.distinctBy { it.id }, next)
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
