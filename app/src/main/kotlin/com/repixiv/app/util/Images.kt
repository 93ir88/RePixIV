package com.repixiv.app.util

import android.widget.ImageView
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

@Composable
fun RemoteImage(url: String, circle: Boolean, modifier: Modifier, crop: Boolean = true) {
    val cap = lowResCap(LocalAppState.current.lowRes)
    AndroidView(
        modifier = modifier,
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { view ->
            if (url.isNotBlank()) {
                view.scaleType = if (crop || circle) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
                val base = Glide.with(view).load(pixivGlideUrl(url))
                val req = if (cap > 0 && !circle) base.override(cap, cap) else base
                when {
                    circle -> req.circleCrop()
                    crop -> req.centerCrop()
                    else -> req.fitCenter()
                }.into(view)
            }
        },
    )
}
