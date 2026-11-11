package com.repixiv.app.util

import android.text.format.DateUtils
import androidx.core.text.HtmlCompat
import com.repixiv.app.pixiv.parseIsoMillis
import java.text.DateFormat
import java.util.Date

/** pixiv captions are small HTML snippets. */
fun htmlToText(html: String): String =
    HtmlCompat.fromHtml(html.replace("<br />", "\n").replace("<br>", "\n"), HtmlCompat.FROM_HTML_MODE_COMPACT)
        .toString().trim()

fun formatPostDate(iso: String): String {
    val ms = parseIsoMillis(iso) ?: return iso
    return DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT).format(Date(ms))
}

fun relativeTime(iso: String): String {
    val ms = parseIsoMillis(iso) ?: return iso.take(10)
    return DateUtils.getRelativeTimeSpanString(ms, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
}
