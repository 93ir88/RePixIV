package com.repixiv.app.player

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * In-app audio engine: YouTube's official embedded player (IFrame Player API) inside a WebView.
 * The WebView is created once and attached to the main window as a tiny view (see MainActivity), so
 * playback continues across tabs. Play/pause, repeat, speed, restart and volume all drive the embedded player.
 */
object AppPlayer {
    /** Identifies this app to YouTube's embed (it expects an origin / referrer). */
    private const val ORIGIN = "https://com.repixiv.app"
    private val ID_REGEX = Regex("^[A-Za-z0-9_-]{6,20}$")

    private var web: WebView? = null
    private var ready = false
    private var pendingId: String? = null
    private var queue: List<Track> = emptyList()
    private val main = Handler(Looper.getMainLooper())

    private val HTML = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}#p{width:100%;height:100%}</style>
        </head><body><div id="p"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
        var player=null;
        function onYouTubeIframeAPIReady(){
          player=new YT.Player('p',{width:'100%',height:'100%',
            playerVars:{playsinline:1,rel:0,controls:1,fs:0,enablejsapi:1,origin:'__ORIGIN__'},
            events:{
              onReady:function(){Native.onReady();},
              onStateChange:function(e){Native.onState(e.data);},
              onError:function(e){Native.onError(e.data);}
            }});
        }
        function load(id,v){player.setVolume(v);player.loadVideoById(id);}
        function play(){player.playVideo();}
        function pause(){player.pauseVideo();}
        function seek0(){player.seekTo(0,true);player.playVideo();}
        function vol(v){player.setVolume(v);}
        function rate(r){player.setPlaybackRate(r);}
        </script></body></html>
    """.trimIndent().replace("__ORIGIN__", ORIGIN)

    /** The player view. Must be called on the main thread. Detach it from any old parent before re-attaching. */
    @SuppressLint("SetJavaScriptEnabled")
    fun webView(context: Context): WebView {
        web?.let { return it }
        val w = WebView(context.applicationContext)
        w.settings.javaScriptEnabled = true
        w.settings.domStorageEnabled = true
        w.settings.mediaPlaybackRequiresUserGesture = false
        w.setBackgroundColor(Color.BLACK)
        // Never navigate away from the player (e.g. "Watch on YouTube" links).
        w.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        w.addJavascriptInterface(Bridge(), "Native")
        w.loadDataWithBaseURL("$ORIGIN/", HTML, "text/html", "utf-8", null)
        web = w
        return w
    }

    suspend fun play(context: Context, track: Track, list: List<Track>) = withContext(Dispatchers.Main) {
        if (!ID_REGEX.matches(track.id)) throw IllegalArgumentException("Invalid track id")
        webView(context)
        queue = list.ifEmpty { listOf(track) }
        startTrack(track)
    }

    private fun startTrack(track: Track) {
        PlayerState.current = track
        PlayerState.loading = true
        PlayerState.isPlaying = false
        if (ready) js("load('${track.id}',${PlayerState.volume.toInt()})") else pendingId = track.id
    }

    fun togglePause() {
        if (!ready) return
        if (PlayerState.isPlaying) {
            js("pause()")
            PlayerState.isPlaying = false
        } else {
            js("play()")
        }
    }

    fun restart() {
        if (ready && PlayerState.current != null) js("seek0()")
    }

    /** [mode] is "off", "context" (repeat all) or "track" (repeat one). */
    fun setRepeat(mode: String) {
        PlayerState.repeat = mode
    }

    fun setVolume(percent: Float) {
        PlayerState.volume = percent
        if (ready) js("vol(${percent.toInt().coerceIn(0, 100)})")
    }

    fun setSpeed(speed: Float) {
        PlayerState.speed = speed
        if (ready) js("rate($speed)")
    }

    /** Stops playback and forgets the current track (used when signing out). */
    fun stop() {
        if (ready) js("pause()")
        PlayerState.current = null
        PlayerState.isPlaying = false
        PlayerState.loading = false
        PlayerState.optionsOpen = false
    }

    private fun js(code: String) {
        web?.evaluateJavascript(code, null)
    }

    // ---------- events from the embedded player ----------

    private class Bridge {
        @JavascriptInterface
        fun onReady() {
            main.post {
                ready = true
                pendingId?.let { js("load('$it',${PlayerState.volume.toInt()})") }
                pendingId = null
            }
        }

        @JavascriptInterface
        fun onState(state: Int) {
            main.post { handleState(state) }
        }

        @JavascriptInterface
        fun onError(code: Int) {
            main.post { handleError(code) }
        }
    }

    private fun handleState(state: Int) {
        when (state) {
            1 -> { // playing
                PlayerState.isPlaying = true
                PlayerState.loading = false
                // a newly loaded video resets the playback rate
                if (PlayerState.speed != 1f) js("rate(${PlayerState.speed})")
            }
            2 -> { // paused
                PlayerState.isPlaying = false
                PlayerState.loading = false
            }
            3 -> PlayerState.loading = true // buffering
            0 -> { // ended
                PlayerState.isPlaying = false
                onCompleted()
            }
        }
    }

    private fun handleError(code: Int) {
        PlayerState.isPlaying = false
        PlayerState.loading = false
        when (code) {
            // 100 not found / private, 101 and 150: the owner doesn't allow embedding
            100, 101, 150 -> {
                if (!playNext()) PlayerState.error = "This track can't be played here"
                else PlayerState.error = "That track can't be played in other apps, skipped it"
            }
            153 -> PlayerState.error = "YouTube refused the embedded player (error 153)"
            else -> PlayerState.error = "Playback error ($code)"
        }
    }

    private fun onCompleted() {
        if (PlayerState.repeat == "track") {
            js("seek0()")
            return
        }
        playNext()
    }

    /** Moves to the next track in the queue (wrapping when "repeat all"). Returns false if there is none. */
    private fun playNext(): Boolean {
        val current = PlayerState.current ?: return false
        val i = queue.indexOfFirst { it.id == current.id }
        val next = when {
            i in 0 until queue.size - 1 -> queue[i + 1]
            PlayerState.repeat == "context" && queue.size > 1 -> queue[0]
            else -> null
        } ?: return false
        PlayerStore.recordPlay(next)
        startTrack(next)
        return true
    }
}
