package com.repixiv.app.player

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.repixiv.app.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

class GoogleAuthException(message: String, val code: String = "") : Exception(message)

/**
 * Google sign-in for the Player tab. Standard OAuth 2.0 authorization-code flow with PKCE, run in the
 * device's own browser (never an in-app WebView; Google blocks those anyway). The result is a token that
 * can read the account's YouTube data (liked videos, search), used by [YouTubeMusic].
 *
 * The OAuth client id is public (it ships inside every APK) and is set at build time, see README.
 * Create it in Google Cloud Console as an "Android" client with package name com.repixiv.app and make
 * sure "Enable custom URI scheme" is on under Advanced settings.
 */
object GoogleAuth {
    private const val AUTH = "https://accounts.google.com/o/oauth2/v2/auth"
    private const val TOKEN = "https://oauth2.googleapis.com/token"
    private const val REVOKE = "https://oauth2.googleapis.com/revoke"
    private const val SCOPE = "https://www.googleapis.com/auth/youtube.readonly"
    private const val ID_SUFFIX = ".apps.googleusercontent.com"

    private val clientId: String get() = BuildConfig.GOOGLE_CLIENT_ID

    /** True when this build was given a Google OAuth client id. */
    val isConfigured: Boolean get() = clientId.endsWith(ID_SUFFIX) && clientId.length > ID_SUFFIX.length

    /** Android-type OAuth clients use the package name as the redirect scheme: com.repixiv.app:/oauth2redirect */
    private const val redirectScheme = "com.repixiv.app"
    private val redirectUri: String get() = "$redirectScheme:/oauth2redirect"

    var signedIn by mutableStateOf(false)
        private set

    private var prefs: SharedPreferences? = null
    private var access: String = ""
    private var expiresAt: Long = 0L

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.applicationContext.getSharedPreferences("repixiv_google", Context.MODE_PRIVATE)
        prefs = p
        access = p.getString("access", "") ?: ""
        expiresAt = p.getLong("expires", 0L)
        signedIn = !p.getString("refresh", null).isNullOrEmpty()
    }

    fun isRedirect(uri: Uri): Boolean = isConfigured && uri.scheme == redirectScheme

    // ---------- login ----------

    /** Opens Google's sign-in page in the device browser. */
    fun startLogin(context: Context) {
        if (!isConfigured) throw GoogleAuthException("This build has no Google client id (see README)")
        val verifier = random(32)
        val state = random(16)
        prefs?.edit()?.putString("verifier", verifier)?.putString("state", state)?.apply()
        val challenge = b64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        val url = Uri.parse(AUTH).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPE)
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .appendQueryParameter("access_type", "offline")
            .appendQueryParameter("prompt", "select_account consent")
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, url))
    }

    /** Called with the redirect from the browser. Blocking (network): run off the main thread. */
    fun completeLoginBlocking(uri: Uri) {
        uri.getQueryParameter("error")?.let { throw GoogleAuthException("Google sign-in cancelled ($it)") }
        val code = uri.getQueryParameter("code") ?: throw GoogleAuthException("Google sign-in returned no code")
        val p = prefs ?: throw GoogleAuthException("Not initialised")
        val verifier = p.getString("verifier", null)
        val state = p.getString("state", null)
        if (verifier == null || state == null || state != uri.getQueryParameter("state")) {
            throw GoogleAuthException("Login attempt expired, please try again")
        }
        val j = post(
            TOKEN,
            mapOf(
                "client_id" to clientId,
                "code" to code,
                "code_verifier" to verifier,
                "grant_type" to "authorization_code",
                "redirect_uri" to redirectUri,
            ),
        )
        store(j)
        p.edit().remove("verifier").remove("state").apply()
        signedIn = true
    }

    // ---------- tokens ----------

    /** A valid access token, refreshing it when needed. Blocking (network): run off the main thread. */
    @Synchronized
    fun accessTokenBlocking(forceRefresh: Boolean = false): String {
        val refresh = prefs?.getString("refresh", null)
        if (refresh.isNullOrEmpty()) throw GoogleAuthException("Sign in with Google first")
        if (!forceRefresh && access.isNotEmpty() && System.currentTimeMillis() < expiresAt - 60_000) return access
        try {
            store(
                post(
                    TOKEN,
                    mapOf("client_id" to clientId, "refresh_token" to refresh, "grant_type" to "refresh_token"),
                ),
            )
        } catch (e: GoogleAuthException) {
            if (e.code == "invalid_grant") {
                signOutLocal()
                throw GoogleAuthException("Your Google session expired, please sign in again")
            }
            throw e
        }
        return access
    }

    private fun store(j: JSONObject) {
        access = j.getString("access_token")
        expiresAt = System.currentTimeMillis() + j.optLong("expires_in", 3600L) * 1000L
        val e = prefs?.edit()
        e?.putString("access", access)?.putLong("expires", expiresAt)
        // Google only sends a refresh token on the first exchange; keep the old one on refreshes.
        j.optString("refresh_token").takeIf { it.isNotEmpty() }?.let { e?.putString("refresh", it) }
        e?.apply()
    }

    fun signOutLocal() {
        access = ""
        expiresAt = 0L
        prefs?.edit()?.clear()?.apply()
        signedIn = false
    }

    /** Signs out and asks Google to revoke the token (best effort). Blocking: run off the main thread. */
    fun signOutBlocking() {
        val token = prefs?.getString("refresh", null)
        signOutLocal()
        if (!token.isNullOrEmpty()) runCatching { post(REVOKE, mapOf("token" to token)) }
    }

    // ---------- http ----------

    private fun post(url: String, params: Map<String, String>): JSONObject {
        val body = params.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = "POST"
            c.doOutput = true
            c.connectTimeout = 15_000
            c.readTimeout = 25_000
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            c.outputStream.use { it.write(body.toByteArray()) }
            val code = c.responseCode
            val text = (if (code >= 400) c.errorStream else c.inputStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
            if (code !in 200..299) {
                val err = json.optString("error")
                throw GoogleAuthException(json.optString("error_description").ifEmpty { err.ifEmpty { "HTTP $code" } }, err)
            }
            return json
        } finally {
            c.disconnect()
        }
    }

    private fun random(bytes: Int): String = b64(ByteArray(bytes).also { SecureRandom().nextBytes(it) })

    private fun b64(b: ByteArray): String =
        Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
