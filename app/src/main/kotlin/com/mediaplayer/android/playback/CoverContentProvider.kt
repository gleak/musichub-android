package com.mediaplayer.android.playback

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.mediaplayer.android.data.Network
import okhttp3.Request
import java.io.File
import java.util.UUID

/**
 * Exposes song cover bytes to other apps via
 * `content://com.mediaplayer.android.covers/{songId}`. Primary client is
 * Android Auto's gearhead process: it fetches browse-tile artwork URIs
 * from its own process, so an `https://backend/...` URI cannot carry our
 * `X-Api-Key` + `Bearer` headers and gets 401-ed — covers stay blank.
 *
 * Proxying through this provider lets AA resolve the URI locally; the
 * OkHttp fetch happens in our process where the auth interceptor injects
 * headers. Bytes are cached on disk under `cacheDir/covers/{songId}` so
 * AA's repeated tile pulls during grid scroll skip the backend.
 */
class CoverContentProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val songId = uri.lastPathSegment?.toLongOrNull() ?: return null
        val ctx = context ?: return null
        val dir = File(ctx.cacheDir, "covers").apply { mkdirs() }
        val target = File(dir, songId.toString())
        if (!target.exists() || target.length() == 0L) {
            if (!download(songId, target, dir)) return null
        }
        return ParcelFileDescriptor.open(target, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    /**
     * Atomic-rename download: writes to a tmp file then renames so a
     * concurrent reader never sees a half-written cover. Returns false on
     * any failure so the caller (`openFile`) bubbles a null PFD up to AA,
     * which then renders its own placeholder.
     */
    private fun download(songId: Long, target: File, dir: File): Boolean {
        val tmp = File(dir, "$songId.tmp.${UUID.randomUUID()}")
        return try {
            val request = Request.Builder().url(Network.coverUrl(songId)).build()
            Network.okHttp.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                response.body.byteStream().use { input ->
                    tmp.outputStream().use { output -> input.copyTo(output) }
                }
            }
            if (!tmp.renameTo(target)) {
                tmp.copyTo(target, overwrite = true)
                tmp.delete()
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }

    override fun getType(uri: Uri): String = "image/jpeg"

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "com.mediaplayer.android.covers"

        fun uriFor(songId: Long): Uri = Uri.parse("content://$AUTHORITY/$songId")
    }
}
