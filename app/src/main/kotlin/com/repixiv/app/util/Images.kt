package com.repixiv.app.util

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.repixiv.app.LocalAppState
import com.repixiv.app.pixiv.Illust
import com.repixiv.app.pixiv.Pixiv

/** pixiv's image CDN (i.pximg.net) rejects requests without a pixiv Referer. */
fun pixivGlideUrl(url: String) = GlideUrl(
    url,
    LazyHeaders.Builder()
        .addHeader("Referer", Pixiv.REFERER)
        .addHeader("User-Agent", Pixiv.USER_AGENT)
        .build(),
)

/** Low-res mode: 0 off, 1 = 144p, 2 = 480p, 3 = 720p (longest edge in px). */
fun lowResCap(mode: Int): Int = when (mode) {
    1 -> 144
    2 -> 480
    3 -> 720
    else -> 0
}

/** Feed/grid image for [this], using a smaller server rendition when low-res mode is on. */
@Composable
fun Illust.cardUrl(): String {
    val cap = lowResCap(LocalAppState.current.lowRes)
    return when {
        cap == 0 -> imageUrl
        cap <= 144 -> squareUrl.ifEmpty { imageUrl }
        cap <= 480 -> mediumUrl.ifEmpty { imageUrl }
        else -> imageUrl
    }
}

/** Page image for the post carousel. */
fun Illust.pageUrl(page: Int, cap: Int): String {
    val large = largeUrls.getOrElse(page) { imageUrl }
    return if (cap in 1..480) mediumUrls.getOrElse(page) { large } else large
}

/** Full-screen viewer image: original quality unless low-res mode is on. */
fun Illust.fullUrl(page: Int, cap: Int): String =
    if (cap == 0) originalUrls.getOrElse(page) { largeUrls.getOrElse(page) { imageUrl } } else pageUrl(page, cap)

/**
 * True while the screen/tab hosting this content is fully hidden under an opaque screen. Covered content stays
 * composed (so its state and scroll position survive) but releases its decoded bitmaps; they're reloaded from
 * Glide's memory/disk cache the moment it becomes visible again, at full quality.
 */
val LocalCovered = compositionLocalOf<State<Boolean>?> { null }

@Composable
fun RemoteImage(url: String, circle: Boolean, modifier: Modifier, crop: Boolean = true, plain: Boolean = false) {
    val cap = lowResCap(LocalAppState.current.lowRes)
    val covered = LocalCovered.current
    AndroidView(
        modifier = modifier,
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { view ->
            if (covered?.value == true) {
                // Hidden under another screen: hand the bitmap back to Glide's pool instead of pinning it.
                Glide.with(view).clear(view)
                view.setImageDrawable(null)
            } else if (url.isNotBlank()) {
                view.scaleType = if (crop || circle) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                val base = if (plain) Glide.with(view).load(url) else Glide.with(view).load(pixivGlideUrl(url))
                val req = if (cap > 0 && !circle) base.override(cap, cap) else base
                when {
                    circle -> req.circleCrop()
                    crop -> req.centerCrop()
                    else -> req.fitCenter()
                }.into(view)
            }
        },
        // Scrolled off / screen closed: release the bitmap right away so Glide can recycle it, rather than
        // waiting for the GC. Keeps memory flat during long scrolling sessions.
        onReset = { view ->
            Glide.with(view).clear(view)
            view.setImageDrawable(null)
        },
        onRelease = { view ->
            Glide.with(view).clear(view)
            view.setImageDrawable(null)
        },
    )
}

/** Loads [url] into a Compose bitmap (used for album art that needs custom drawing). */
@Composable
fun rememberBitmap(url: String, size: Int = 320): ImageBitmap? {
    val context = LocalContext.current
    var bmp by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    DisposableEffect(url) {
        val target = object : CustomTarget<Bitmap>(size, size) {
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                bmp = resource.asImageBitmap()
            }

            override fun onLoadCleared(placeholder: Drawable?) {}
        }
        if (url.isNotBlank()) Glide.with(context).asBitmap().load(url).into(target)
        onDispose { Glide.with(context).clear(target) }
    }
    return bmp
}
