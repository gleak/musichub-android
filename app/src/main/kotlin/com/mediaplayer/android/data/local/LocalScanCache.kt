package com.mediaplayer.android.data.local

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Disk-backed snapshot of the last successful local-library scan.
 *
 * Why: cold-start scanning the SAF tree calls MediaMetadataRetriever per file
 * (header parse, ~50ms each), and even MediaStore-only users with thousands of
 * files watch a spinner for noticeable seconds. The cache lets [observe] emit
 * the previous result instantly while the fresh scan runs in the background.
 *
 * Format: JSON list serialised to `filesDir/local_scan_cache.json`. Atomic
 * writes via tmp + rename. Parse failures degrade to "no cache" — the worst
 * outcome is one slow boot.
 */
object LocalScanCache {

    private const val FILE_NAME = "local_scan_cache.json"
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(CachedTrack.serializer())

    suspend fun read(context: Context): List<LocalTrack> = withContext(Dispatchers.IO) {
        val f = File(context.filesDir, FILE_NAME)
        if (!f.exists()) return@withContext emptyList()
        runCatching {
            json.decodeFromString(serializer, f.readText()).map { it.toLocalTrack() }
        }.getOrDefault(emptyList())
    }

    suspend fun write(context: Context, tracks: List<LocalTrack>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val target = File(context.filesDir, FILE_NAME)
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            val payload = json.encodeToString(serializer, tracks.map { it.toCached() })
            runCatching {
                tmp.writeText(payload)
                if (target.exists()) target.delete()
                tmp.renameTo(target)
            }
        }
    }

    @Serializable
    private data class CachedTrack(
        val id: Long,
        val uri: String,
        val title: String,
        val artist: String,
        val album: String? = null,
        val durationMs: Long,
        val albumId: Long? = null,
        val albumArtUri: String? = null,
        val folderName: String,
        val folderPath: String,
        val dateAddedMs: Long,
        val source: String,
    ) {
        fun toLocalTrack() = LocalTrack(
            id = id,
            uri = Uri.parse(uri),
            title = title,
            artist = artist,
            album = album,
            durationMs = durationMs,
            albumId = albumId,
            albumArtUri = albumArtUri?.let(Uri::parse),
            folderName = folderName,
            folderPath = folderPath,
            dateAddedMs = dateAddedMs,
            source = runCatching { LocalTrack.Source.valueOf(source) }
                .getOrDefault(LocalTrack.Source.MediaStore),
        )
    }

    private fun LocalTrack.toCached() = CachedTrack(
        id = id,
        uri = uri.toString(),
        title = title,
        artist = artist,
        album = album,
        durationMs = durationMs,
        albumId = albumId,
        albumArtUri = albumArtUri?.toString(),
        folderName = folderName,
        folderPath = folderPath,
        dateAddedMs = dateAddedMs,
        source = source.name,
    )
}
