package com.mediaplayer.android.playback

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.IOException

/**
 * Saves the song's audio bytes into MediaStore with `IS_ALARM=1` so the
 * Android Clock app's "alarm sound" picker can use it. The file lives under
 * `Alarms/MediaPlayer/<artist> - <title>.mp3` on Android 10+; older devices
 * fall back to the legacy {@code EXTERNAL_CONTENT_URI} surface.
 *
 * Re-exporting the same song replaces the prior MediaStore entry instead of
 * accumulating duplicates.
 */
object RingtoneExporter {

    suspend fun exportAsAlarm(context: Context, song: SongDto): Uri = withContext(Dispatchers.IO) {
        val displayName = sanitizeFilename("${song.artist} - ${song.title}.mp3")
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION") MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        // Drop any prior export for this song so the user doesn't end up with
        // "Song.mp3 (1)", "Song.mp3 (2)" cluttering their alarm picker.
        deleteExisting(context, collection, displayName)

        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.TITLE, song.title)
            put(MediaStore.Audio.Media.ARTIST, song.artist)
            song.album?.let { put(MediaStore.Audio.Media.ALBUM, it) }
            put(MediaStore.Audio.Media.IS_ALARM, 1)
            put(MediaStore.Audio.Media.IS_RINGTONE, 0)
            put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
            put(MediaStore.Audio.Media.IS_MUSIC, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Audio.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_ALARMS + "/MediaPlayer",
                )
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("MediaStore insert returned null")

        try {
            // Pull the audio bytes from the backend stream endpoint. ExoPlayer's
            // download cache might already have them but its key/format isn't
            // public; a single OkHttp call keeps this simple and works whether
            // or not the song was previously downloaded.
            val request = Request.Builder().url(Network.streamUrl(song.id)).build()
            Network.okHttp.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IOException("HTTP ${resp.code} fetching ${song.id}")
                }
                val body = resp.body ?: throw IOException("Empty response body")
                resolver.openOutputStream(uri)?.use { out ->
                    body.byteStream().copyTo(out)
                } ?: throw IOException("Cannot open output stream for $uri")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val finalize = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                resolver.update(uri, finalize, null, null)
            }
            uri
        } catch (t: Throwable) {
            // Roll back the MediaStore row so a failed export doesn't leave a
            // 0-byte placeholder visible to the alarm picker.
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    private fun deleteExisting(context: Context, collection: Uri, displayName: String) {
        context.contentResolver.query(
            collection,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.DISPLAY_NAME}=?",
            arrayOf(displayName),
            null,
        )?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(0)
                runCatching {
                    context.contentResolver.delete(
                        ContentUris.withAppendedId(collection, id),
                        null,
                        null,
                    )
                }
            }
        }
    }

    private fun sanitizeFilename(name: String): String =
        name.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), "_").trim()
}
