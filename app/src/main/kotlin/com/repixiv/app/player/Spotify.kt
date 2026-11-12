package com.repixiv.app.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val image: String,
    val uri: String,
)

data class PlaybackSnapshot(
    val isPlaying: Boolean,
    val track: Track?,
    val repeat: String,
    val volume: Int?,
)

class SpotifyException(message: String, val httpCode: Int = 0, val reason: String = "") : Exception(message)

/**
 * Spotify Web API client (official API, OAuth with PKCE). Playback is controlled on the Spotify app,
 * which needs to be installed on this phone, and requires Spotify Premium. The user supplies their own
 * app Client ID (created at developer.spotify.com/dashboard).
 */
object Spotify {
    const val REDIRECT_URI = "repixiv://spotify-callback"
    private const val SCOPES = "user-top-read user-read-playback-state user-modify-playback-state"
    private const val API = "https://api.spotify.com/v1"
    private const val TOKEN_URL = "https://accounts.spotify.com/api/token"

    var clientId by mutableStateOf("")
        private set
    var connected by mutableStateOf(false)
        private set
    var authError by mutableStateOf<String?>(null)

    private var prefs: SharedPreferences? = null
    private var access = ""
    private var refresh = ""
    private var expiresAt = 0L

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_spotify", Context.MODE_PRIVATE)
        prefs = p
        clientId = p.getString("client_id", "") ?: ""
        access = p.getString("access", "") ?: ""
        refresh = p.getString("refresh", "") ?: ""
        expiresAt = p.getLong("expires", 0L)
        connected = refresh.isNotEmpty()
    }

    fun setClientId(id: String) {
        clientId = id.trim()
        prefs?.edit()?.putString("client_id", clientId)?.apply()
    }

    fun disconnect() {
        access = ""
        refresh = ""
        expiresAt = 0L
        connected = false
        prefs?.edit()?.remove("access")?.remove("refresh")?.remove("expires")?.apply()
    }

    /** Opens Spotify's login page; the result comes back through the repixiv://spotify-callback redirect. */
    fun startLogin(context: Context) {
        authError = null
        if (clientId.isBlank()) {
            authError = "Enter your Spotify Client ID first"
            return
        }
        val verifier = b64(ByteArray(32).also { SecureRandom().nextBytes(it) })
        val challenge = b64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        prefs?.edit()?.putString("verifier", verifier)?.apply()
        val url = "https://accounts.spotify.com/authorize?response_type=code" +
            "&client_id=${enc(clientId)}&scope=${enc(SCOPES)}&redirect_uri=${enc(REDIRECT_URI)}" +
            "&code_challenge_method=S256&code_challenge=$challenge"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun handleRedirect(uri: Uri) {
        if (uri.scheme != "repixiv" || uri.host != "spotify-callback") return
        val error = uri.getQueryParameter("error")
        if (error != null) {
            authError = "Spotify login failed: $error"
            return
        }
        val code = uri.getQueryParameter("code") ?: return
        val verifier = prefs?.getString("verifier", null) ?: return
        Thread {
            try {
                tokenRequest(
                    mapOf(
                        "grant_type" to "authorization_code",
                        "code" to code,
                        "redirect_uri" to REDIRECT_URI,
                        "client_id" to clientId,
                        "code_verifier" to verifier,
                    ),
                )
                authError = null
            } catch (e: Exception) {
                authError = e.message ?: "Spotify login failed"
            }
        }.start()
    }

    // ---------------------------------------------------------------- API

    suspend fun topTracks(): List<Track> = withContext(Dispatchers.IO) {
        val j = json(call("GET", "/me/top/tracks?limit=30&time_range=medium_term"))
        tracks(j.optJSONArray("items"))
    }

    suspend fun search(query: String): List<Track> = withContext(Dispatchers.IO) {
        val j = json(call("GET", "/search?q=${enc(query)}&type=track&limit=30"))
        tracks(j.optJSONObject("tracks")?.optJSONArray("items"))
    }

    suspend fun playbackState(): PlaybackSnapshot? = withContext(Dispatchers.IO) {
        val (code, text) = rawCall("GET", "/me/player", null)
        if (code == 204 || text.isBlank()) return@withContext null
        if (code !in 200..299) throw fail(code, text)
        val j = JSONObject(text)
        PlaybackSnapshot(
            isPlaying = j.optBoolean("is_playing"),
            track = j.optJSONObject("item")?.let { runCatching { track(it) }.getOrNull() },
            repeat = j.optString("repeat_state", "off"),
            volume = j.optJSONObject("device")?.takeIf { it.has("volume_percent") && !it.isNull("volume_percent") }
                ?.optInt("volume_percent"),
        )
    }

    /** Starts [uri], or resumes the current track when null. */
    suspend fun play(uri: String?) = withContext(Dispatchers.IO) {
        val body = if (uri != null) "{\"uris\":[\"$uri\"]}" else null
        call("PUT", "/me/player/play", body)
        Unit
    }

    suspend fun pause() = withContext(Dispatchers.IO) {
        call("PUT", "/me/player/pause", null)
        Unit
    }

    suspend fun seek(positionMs: Long) = withContext(Dispatchers.IO) {
        call("PUT", "/me/player/seek?position_ms=$positionMs", null)
        Unit
    }

    /** [state] is "off", "context" (repeat all) or "track" (repeat one). */
    suspend fun setRepeat(state: String) = withContext(Dispatchers.IO) {
        call("PUT", "/me/player/repeat?state=$state", null)
        Unit
    }

    suspend fun setVolume(percent: Int) = withContext(Dispatchers.IO) {
        call("PUT", "/me/player/volume?volume_percent=${percent.coerceIn(0, 100)}", null)
        Unit
    }

    // ---------------------------------------------------------------- internals

    private fun tracks(arr: org.json.JSONArray?): List<Track> {
        val out = ArrayList<Track>()
        if (arr != null) for (i in 0 until arr.length()) runCatching { out += track(arr.getJSONObject(i)) }
        return out.distinctBy { it.id }
    }

    private fun track(o: JSONObject): Track {
        val artists = o.optJSONArray("artists")
        val names = ArrayList<String>()
        if (artists != null) for (i in 0 until artists.length()) names += artists.getJSONObject(i).optString("name")
        val images = o.optJSONObject("album")?.optJSONArray("images")
        val image = when {
            images == null || images.length() == 0 -> ""
            images.length() > 1 -> images.getJSONObject(1).optString("url")
            else -> images.getJSONObject(0).optString("url")
        }
        return Track(
            id = o.getString("id"),
            title = o.optString("name"),
            artist = names.joinToString(", "),
            image = image,
            uri = o.optString("uri"),
        )
    }

    private fun json(res: Pair<Int, String>): JSONObject = if (res.second.isBlank()) JSONObject() else JSONObject(res.second)

    private fun call(method: String, path: String, body: String?): Pair<Int, String> {
        val res = rawCall(method, path, body)
        if (res.first !in 200..299) throw fail(res.first, res.second)
        return res
    }

    private fun rawCall(method: String, path: String, body: String?): Pair<Int, String> {
        if (!connected) throw SpotifyException("Spotify isn't connected")
        var token = validToken()
        var res = http(method, "$API$path", mapOf("Authorization" to "Bearer $token"), body)
        if (res.first == 401) {
            token = forceRefresh()
            res = http(method, "$API$path", mapOf("Authorization" to "Bearer $token"), body)
        }
        return res
    }

    @Synchronized
    private fun validToken(): String {
        if (access.isNotEmpty() && System.currentTimeMillis() < expiresAt - 60_000) return access
        return forceRefresh()
    }

    @Synchronized
    private fun forceRefresh(): String {
        try {
            tokenRequest(mapOf("grant_type" to "refresh_token", "refresh_token" to refresh, "client_id" to clientId))
        } catch (e: SpotifyException) {
            if (e.httpCode in 400..401) disconnect()
            throw e
        }
        return access
    }

    private fun tokenRequest(form: Map<String, String>) {
        val (code, text) = http(
            "POST",
            TOKEN_URL,
            mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            form.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" },
        )
        if (code !in 200..299) throw fail(code, text)
        val j = JSONObject(text)
        access = j.getString("access_token")
        j.optString("refresh_token").takeIf { it.isNotBlank() }?.let { refresh = it }
        expiresAt = System.currentTimeMillis() + j.optLong("expires_in", 3600L) * 1000L
        connected = refresh.isNotEmpty()
        prefs?.edit()?.putString("access", access)?.putString("refresh", refresh)?.putLong("expires", expiresAt)?.apply()
    }

    private fun http(method: String, url: String, headers: Map<String, String>, body: String?): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (method == "PUT" || method == "POST") {
                c.doOutput = true
                if (body != null && !headers.containsKey("Content-Type")) c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write((body ?: "").toByteArray()) }
            }
            val code = c.responseCode
            val stream = if (code >= 400) c.errorStream else c.inputStream
            return code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            c.disconnect()
        }
    }

    private fun fail(code: Int, text: String): SpotifyException {
        var reason = ""
        var message = ""
        runCatching {
            val j = JSONObject(text)
            val e = j.optJSONObject("error")
            if (e != null) {
                reason = e.optString("reason")
                message = e.optString("message")
            } else {
                message = j.optString("error_description").ifBlank { j.optString("error") }
            }
        }
        return SpotifyException(if (message.isNotBlank()) "$message (HTTP $code)" else "Spotify request failed (HTTP $code)", code, reason)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
}
