package com.repixiv.app.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** In-app audio engine (Android MediaPlayer): play, pause, repeat, speed, restart and volume all work here. */
object AppPlayer {
    private var mp: MediaPlayer? = null
    private var queue: List<Track> = emptyList()
    private var generation = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    suspend fun play(context: Context, track: Track, list: List<Track>) {
        queue = list.ifEmpty { listOf(track) }
        PlayerState.current = track
        PlayerState.loading = true
        PlayerState.isPlaying = false
        val gen = ++generation
        val url = Audius.resolveStream(track.id)
        if (gen != generation) return
        withContext(Dispatchers.Main) {
            if (gen != generation) return@withContext
            release()
            val player = MediaPlayer()
            mp = player
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            player.setWakeMode(context.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            player.setDataSource(url)
            player.isLooping = PlayerState.repeat == "track"
            player.setOnPreparedListener { p ->
                if (gen == generation) {
                    val v = PlayerState.volume / 100f
                    p.setVolume(v, v)
                    p.start()
                    applySpeed(p)
                    PlayerState.isPlaying = true
                    PlayerState.loading = false
                }
            }
            player.setOnCompletionListener { onCompleted(context) }
            player.setOnErrorListener { _, what, _ ->
                if (gen == generation) {
                    PlayerState.isPlaying = false
                    PlayerState.loading = false
                    PlayerState.error = "Playback error ($what)"
                }
                true
            }
            player.prepareAsync()
        }
    }

    fun togglePause() {
        val p = mp ?: return
        runCatching {
            if (p.isPlaying) {
                p.pause()
                PlayerState.isPlaying = false
            } else {
                p.start()
                applySpeed(p)
                PlayerState.isPlaying = true
            }
        }
    }

    fun restart() {
        val p = mp ?: return
        runCatching {
            p.seekTo(0)
            if (!p.isPlaying) {
                p.start()
                applySpeed(p)
                PlayerState.isPlaying = true
            }
        }
    }

    /** [mode] is "off", "context" (repeat all) or "track" (repeat one). */
    fun setRepeat(mode: String) {
        PlayerState.repeat = mode
        runCatching { mp?.isLooping = mode == "track" }
    }

    fun setVolume(percent: Float) {
        PlayerState.volume = percent
        val v = (percent / 100f).coerceIn(0f, 1f)
        runCatching { mp?.setVolume(v, v) }
    }

    fun setSpeed(speed: Float) {
        PlayerState.speed = speed
        mp?.let { applySpeed(it) }
    }

    private fun applySpeed(p: MediaPlayer) {
        // Setting params on a paused player would start it, so only apply while playing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching { if (p.isPlaying) p.playbackParams = p.playbackParams.setSpeed(PlayerState.speed) }
        }
    }

    private fun onCompleted(context: Context) {
        val current = PlayerState.current ?: return
        val i = queue.indexOfFirst { it.id == current.id }
        val next = when {
            i in 0 until queue.size - 1 -> queue[i + 1]
            PlayerState.repeat == "context" && queue.isNotEmpty() -> queue[0]
            else -> null
        }
        if (next != null) {
            scope.launch {
                try {
                    play(context, next, queue)
                } catch (e: Exception) {
                    PlayerState.isPlaying = false
                    PlayerState.loading = false
                    PlayerState.error = e.message ?: "Could not play the next track"
                }
            }
        } else {
            PlayerState.isPlaying = false
        }
    }

    private fun release() {
        runCatching { mp?.release() }
        mp = null
    }
}
