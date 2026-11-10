package com.repixiv.app.pixiv

import android.content.Context
import android.content.SharedPreferences
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class Illust(
    val id: Long,
    val title: String,
    val userName: String,
    val userAvatar: String,
    val imageUrl: String,
    val width: Int,
    val height: Int,
    val bookmarks: Int,
    val views: Int,
    val tags: List<String>,
    val pageCount: Int,
    val userId: Long,
    val largeUrls: List<String>,
    val originalUrls: List<String>,
)

data class IllustPage(val items: List<Illust>, val nextUrl: String?)

data class UserDetail(
    val name: String,
    val account: String,
    val avatar: String,
    val comment: String,
    val following: Int,
    val illusts: Int,
    val bookmarks: Int,
    val isFollowed: Boolean,
)

data class Session(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val userId: Long,
    val name: String,
    val account: String,
    val avatar: String,
)

class PixivException(message: String, val httpCode: Int = 0) : Exception(message)

/**
 * Minimal client for pixiv's (unofficial) app API, the same one the official mobile app uses.
 * OAuth + PKCE login happens in [com.repixiv.app.LoginActivity].
 */
object Pixiv {
    // Public mobile-app client credentials (also used by pixivpy and other open-source clients).
    private const val CLIENT_ID = "MOBrBDS8blbauoSck0ZfDbtuzpyT"
    private const val CLIENT_SECRET = "lsACyCD94FhDUtGTXi3QzcFE2uU1hqtDaKeqrdwj"
    private const val HASH_SECRET = "28c1fdd170a5204386cb1313c7077b34f83e4aaf4aa829ce78c231e05b0bae2c"
    private const val API = "https://app-api.pixiv.net"
    private const val AUTH_URL = "https://oauth.secure.pixiv.net/auth/token"
    private const val REDIRECT_URI = "https://app-api.pixiv.net/web/v1/users/auth/pixiv/callback"

    const val USER_AGENT = "PixivAndroidApp/5.0.234 (Android 11; Pixel 5)"
    const val REFERER = "https://app-api.pixiv.net/"

    var session by mutableStateOf<Session?>(null)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_session", Context.MODE_PRIVATE)
        prefs = p
        val refresh = p.getString("refresh", null) ?: return
        session = Session(
            accessToken = p.getString("access", "") ?: "",
            refreshToken = refresh,
            expiresAt = p.getLong("expires", 0L),
            userId = p.getLong("uid", 0L),
            name = p.getString("name", "") ?: "",
            account = p.getString("account", "") ?: "",
            avatar = p.getString("avatar", "") ?: "",
        )
    }

    private fun save(s: Session) {
        session = s
        prefs?.edit()
            ?.putString("access", s.accessToken)
            ?.putString("refresh", s.refreshToken)
            ?.putLong("expires", s.expiresAt)
            ?.putLong("uid", s.userId)
            ?.putString("name", s.name)
            ?.putString("account", s.account)
            ?.putString("avatar", s.avatar)
            ?.apply()
    }

    fun logout() {
        session = null
        prefs?.edit()?.clear()?.apply()
    }

    // ---------- PKCE login ----------

    /** Starts a login attempt and returns the URL to load in a WebView. */
    fun newLoginUrl(): String {
        val bytes = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val verifier = b64(bytes)
        val challenge = b64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        prefs?.edit()?.putString("verifier", verifier)?.apply()
        return "$API/web/v1/login?code_challenge=$challenge&code_challenge_method=S256&client=pixiv-android"
    }

    fun exchangeCodeBlocking(code: String) {
        val verifier = prefs?.getString("verifier", null)
            ?: throw PixivException("Login attempt expired, please try again")
        val s = requestToken(
            mapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "code_verifier" to verifier,
                "redirect_uri" to REDIRECT_URI,
                "include_policy" to "true",
            ),
        )
        save(s)
    }

    fun loginWithRefreshTokenBlocking(token: String) {
        val s = requestToken(mapOf("grant_type" to "refresh_token", "refresh_token" to token.trim()))
        save(s)
    }

    suspend fun loginWithRefreshToken(token: String) = withContext(Dispatchers.IO) { loginWithRefreshTokenBlocking(token) }

    // ---------- API ----------

    suspend fun recommended(nextUrl: String? = null): IllustPage = withContext(Dispatchers.IO) {
        page(get(nextUrl ?: "$API/v1/illust/recommended?filter=for_android&include_ranking_illusts=false"))
    }

    suspend fun search(word: String, nextUrl: String? = null): IllustPage = withContext(Dispatchers.IO) {
        page(
            get(
                nextUrl ?: "$API/v1/search/illust?filter=for_android&search_target=partial_match_for_tags" +
                    "&sort=date_desc&merge_plain_keyword_results=true&include_translated_tag_results=true" +
                    "&word=${enc(word)}",
            ),
        )
    }

    suspend fun bookmarks(userId: Long, nextUrl: String? = null): IllustPage = withContext(Dispatchers.IO) {
        page(get(nextUrl ?: "$API/v1/user/bookmarks/illust?user_id=$userId&restrict=public"))
    }

    suspend fun userDetail(userId: Long): UserDetail = withContext(Dispatchers.IO) {
        val j = get("$API/v1/user/detail?user_id=$userId&filter=for_android")
        val u = j.getJSONObject("user")
        val p = j.optJSONObject("profile")
        UserDetail(
            name = u.optString("name"),
            account = u.optString("account"),
            avatar = u.optJSONObject("profile_image_urls")?.optString("medium").orEmpty(),
            comment = u.optString("comment"),
            following = p?.optInt("total_follow_users") ?: 0,
            illusts = p?.optInt("total_illusts") ?: 0,
            bookmarks = p?.optInt("total_illust_bookmarks_public") ?: 0,
            isFollowed = u.optBoolean("is_followed"),
        )
    }

    suspend fun userIllusts(userId: Long, nextUrl: String? = null): IllustPage = withContext(Dispatchers.IO) {
        page(get(nextUrl ?: "$API/v1/user/illusts?user_id=$userId&type=illust&filter=for_android"))
    }

    suspend fun setFollow(userId: Long, follow: Boolean) = withContext(Dispatchers.IO) {
        if (follow) {
            call("POST", "$API/v1/user/follow/add", mapOf("user_id" to userId.toString(), "restrict" to "public"))
        } else {
            call("POST", "$API/v1/user/follow/delete", mapOf("user_id" to userId.toString()))
        }
        Unit
    }

    suspend fun restrictedMode(): Boolean = withContext(Dispatchers.IO) {
        get("$API/v1/user/restricted-mode-settings").optBoolean("is_restricted_mode_enabled")
    }

    suspend fun setRestrictedMode(enabled: Boolean) = withContext(Dispatchers.IO) {
        call("POST", "$API/v1/user/restricted-mode-settings", mapOf("is_restricted_mode_enabled" to enabled.toString()))
        Unit
    }

    suspend fun showAi(): Boolean = withContext(Dispatchers.IO) {
        get("$API/v1/user/ai-show-settings").optBoolean("show_ai", true)
    }

    suspend fun setShowAi(show: Boolean) = withContext(Dispatchers.IO) {
        call("POST", "$API/v1/user/ai-show-settings/edit", mapOf("show_ai" to show.toString()))
        Unit
    }

    /** Best-effort read of account flags (only the keys pixiv actually returns are included). */
    suspend fun accountState(): Map<String, Boolean> = withContext(Dispatchers.IO) {
        val j = get("$API/v1/user/me/state")
        val st = j.optJSONObject("user_state") ?: j
        val out = LinkedHashMap<String, Boolean>()
        for (k in listOf("has_mail_address", "mail_authorized", "has_password", "is_premium")) {
            if (st.has(k)) out[k] = st.optBoolean(k)
        }
        out
    }

    // ---------- internals ----------

    private fun page(j: JSONObject): IllustPage {
        val arr = j.optJSONArray("illusts")
        val items = ArrayList<Illust>()
        if (arr != null) for (i in 0 until arr.length()) runCatching { items += parseIllust(arr.getJSONObject(i)) }
        val next = j.optString("next_url").takeIf { it.isNotBlank() && it != "null" }
        return IllustPage(items, next)
    }

    private fun parseIllust(o: JSONObject): Illust {
        val img = o.getJSONObject("image_urls")
        val user = o.getJSONObject("user")
        val tagsArr = o.optJSONArray("tags")
        val tags = ArrayList<String>()
        if (tagsArr != null) for (i in 0 until tagsArr.length()) tags += tagsArr.getJSONObject(i).optString("name")
        val imageUrl = img.optString("large").ifEmpty { img.optString("medium") }
        val large = ArrayList<String>()
        val orig = ArrayList<String>()
        val metaPages = o.optJSONArray("meta_pages")
        if (metaPages != null && metaPages.length() > 0) {
            for (i in 0 until metaPages.length()) {
                val urls = metaPages.getJSONObject(i).getJSONObject("image_urls")
                val l = urls.optString("large").ifEmpty { urls.optString("medium") }
                large += l
                orig += urls.optString("original").ifEmpty { l }
            }
        } else {
            large += imageUrl
            orig += (o.optJSONObject("meta_single_page")?.optString("original_image_url").orEmpty()).ifEmpty { imageUrl }
        }
        return Illust(
            id = o.getLong("id"),
            title = o.optString("title"),
            userName = user.optString("name"),
            userAvatar = user.optJSONObject("profile_image_urls")?.optString("medium").orEmpty(),
            imageUrl = imageUrl,
            width = o.optInt("width", 1).coerceAtLeast(1),
            height = o.optInt("height", 1).coerceAtLeast(1),
            bookmarks = o.optInt("total_bookmarks"),
            views = o.optInt("total_view"),
            tags = tags,
            pageCount = o.optInt("page_count", 1),
            userId = user.optLong("id"),
            largeUrls = large,
            originalUrls = orig,
        )
    }

    private fun get(url: String): JSONObject = call("GET", url)

    private fun call(method: String, url: String, form: Map<String, String>? = null): JSONObject {
        var s = session ?: throw PixivException("Not logged in")
        if (System.currentTimeMillis() > s.expiresAt - 60_000) s = refresh(s)
        var res = http(method, url, baseHeaders() + ("Authorization" to "Bearer ${s.accessToken}"), form)
        if (res.first == 400 || res.first == 401) {
            s = refresh(s)
            res = http(method, url, baseHeaders() + ("Authorization" to "Bearer ${s.accessToken}"), form)
        }
        if (res.first !in 200..299) throw PixivException(errorMessage(res.second, res.first), res.first)
        return if (res.second.isBlank()) JSONObject() else JSONObject(res.second)
    }

    @Synchronized
    private fun refresh(old: Session): Session {
        val current = session
        // Another thread may already have refreshed.
        if (current != null && current.accessToken != old.accessToken) return current
        return try {
            requestToken(mapOf("grant_type" to "refresh_token", "refresh_token" to old.refreshToken)).also { save(it) }
        } catch (e: PixivException) {
            if (e.httpCode == 400 || e.httpCode == 401) {
                logout()
                throw PixivException("Session expired, please log in again", e.httpCode)
            }
            throw e
        }
    }

    private fun requestToken(form: Map<String, String>): Session {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+00:00", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val time = fmt.format(Date())
        val headers = baseHeaders() + mapOf("X-Client-Time" to time, "X-Client-Hash" to md5(time + HASH_SECRET))
        val body = form + mapOf("client_id" to CLIENT_ID, "client_secret" to CLIENT_SECRET, "get_secure_url" to "1")
        val (code, text) = http("POST", AUTH_URL, headers, body)
        if (code !in 200..299) throw PixivException(errorMessage(text, code), code)
        val root = JSONObject(text)
        val r = root.optJSONObject("response") ?: root
        val u = r.getJSONObject("user")
        return Session(
            accessToken = r.getString("access_token"),
            refreshToken = r.getString("refresh_token"),
            expiresAt = System.currentTimeMillis() + r.optLong("expires_in", 3600L) * 1000L,
            userId = u.optString("id").toLong(),
            name = u.optString("name"),
            account = u.optString("account"),
            avatar = u.optJSONObject("profile_image_urls")?.let { it.optString("px_170x170").ifEmpty { it.optString("medium") } }.orEmpty(),
        )
    }

    private fun baseHeaders() = mapOf(
        "User-Agent" to USER_AGENT,
        "App-OS" to "android",
        "App-OS-Version" to "11",
        "App-Version" to "5.0.234",
        "Accept-Language" to "en-US",
    )

    private fun http(method: String, url: String, headers: Map<String, String>, form: Map<String, String>? = null): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            if (form != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                val data = form.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
                c.outputStream.use { it.write(data.toByteArray()) }
            }
            val code = c.responseCode
            val stream = if (code >= 400) c.errorStream else c.inputStream
            return code to (stream?.bufferedReader()?.use { it.readText() } ?: "")
        } finally {
            c.disconnect()
        }
    }

    private fun errorMessage(body: String, code: Int): String {
        val msg = runCatching {
            val j = JSONObject(body)
            j.optJSONObject("error")?.let { e ->
                e.optString("user_message").ifBlank { e.optString("message") }.ifBlank { e.optString("reason") }
            }.orEmpty().ifBlank { j.optString("message") }.ifBlank { j.optString("error_description") }
        }.getOrDefault("")
        return if (msg.isNotBlank()) "$msg (HTTP $code)" else "Request failed (HTTP $code)"
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
}
