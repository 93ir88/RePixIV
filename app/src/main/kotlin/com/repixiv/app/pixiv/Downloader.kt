package com.repixiv.app.pixiv

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object Downloader {
    /** Saves every page of [illust] in original quality. Returns a message for the user. */
    suspend fun save(context: Context, illust: Illust): String = withContext(Dispatchers.IO) {
        try {
            var saved = 0
            illust.originalUrls.forEachIndexed { index, url ->
                val ext = url.substringAfterLast('.', "jpg").substringBefore('?').lowercase()
                val name = "pixiv_${illust.id}_p$index.$ext"
                val bytes = fetch(url)
                store(context, name, ext, bytes)
                saved++
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                "Saved $saved image(s) to Pictures/RePixIV"
            } else {
                "Saved $saved image(s) to the app's Pictures folder"
            }
        } catch (e: Exception) {
            "Download failed: ${e.message ?: "unknown error"}"
        }
    }

    private fun fetch(url: String): ByteArray {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.setRequestProperty("Referer", Pixiv.REFERER)
            c.setRequestProperty("User-Agent", Pixiv.USER_AGENT)
            c.connectTimeout = 15_000
            c.readTimeout = 60_000
            if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode}")
            return c.inputStream.use { it.readBytes() }
        } finally {
            c.disconnect()
        }
    }

    private fun store(context: Context, name: String, ext: String, bytes: ByteArray) {
        val mime = when (ext) {
            "png" -> "image/png"
            "gif" -> "image/gif"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/RePixIV")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create file")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "RePixIV").apply { mkdirs() }
            File(dir, name).writeBytes(bytes)
        }
    }
}
