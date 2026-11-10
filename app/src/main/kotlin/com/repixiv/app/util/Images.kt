package com.repixiv.app.util

import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import com.repixiv.app.pixiv.Pixiv

/** pixiv's image CDN (i.pximg.net) rejects requests without a pixiv Referer. */
private fun pixivUrl(url: String) = GlideUrl(
    url,
    LazyHeaders.Builder()
        .addHeader("Referer", Pixiv.REFERER)
        .addHeader("User-Agent", Pixiv.USER_AGENT)
        .build(),
)

@Composable
fun RemoteImage(url: String, circle: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx -> ImageView(ctx).apply { scaleType = ImageView.ScaleType.CENTER_CROP } },
        update = { view ->
            if (url.isNotBlank()) {
                val req = Glide.with(view).load(pixivUrl(url))
                (if (circle) req.circleCrop() else req.centerCrop()).into(view)
            }
        },
    )
}
