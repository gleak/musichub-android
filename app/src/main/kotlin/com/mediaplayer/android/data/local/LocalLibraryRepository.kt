package com.mediaplayer.android.data.local

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reads on-device audio from MediaStore. The provider already aggregates
 * every audio file the system has indexed — no recursive directory walk or
 * SAF dance is needed for the default library. SAF-pinned trees plug in via
 * a separate path (Phase C).
 *
 * Filters applied:
 *  - `IS_MUSIC = 1` — drops alarm tones, ringtones, notifications, system
 *    sounds. Without this the list fills with dialer beeps.
 *  - `DURATION >= 30s` — drops the same junk that doesn't carry IS_MUSIC
 *    (e.g. WhatsApp voice-note exports, app SFX). Tunable; 30s is what
 *    Spotify / Poweramp / VLC settle on.
 */
object LocalLibraryRepository {

    private val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

    /** Permission the user must grant before we can query MediaStore audio. */
    fun requiredPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, requiredPermission()
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Snapshot scan. Suspends on Dispatchers.IO. Returns whatever the
     * provider has indexed at call time. Pair with [observe] to refresh
     * automatically when the system rescans. Result is also persisted to
     * disk so the next [observe] subscription can emit instantly.
     */
    suspend fun scan(context: Context): List<LocalTrack> {
        val fresh = withContext(Dispatchers.IO) { scanInternal(context) }
        LocalScanCache.write(context, fresh)
        return fresh
    }

    /**
     * Cold flow:
     *  1. emits the cached snapshot from the previous run instantly (skipped
     *     when no cache exists yet — first launch still pays the full scan),
     *  2. emits a fresh scan and persists it,
     *  3. keeps emitting on every ContentObserver fire.
     *
     * The cache write after each scan is fire-and-forget on the producer
     * scope so it never blocks the emission. Use from a ViewModelScope;
     * cancellation tears down the observer.
     */
    fun observe(context: Context): Flow<List<LocalTrack>> = callbackFlow {
        val cached = LocalScanCache.read(context)
        if (cached.isNotEmpty()) trySend(cached)

        val initial = scanInternal(context)
        trySend(initial)
        // Guard: don't clobber a good cache with an empty cold scan. MediaStore
        // can return zero rows during a reindex (Xiaomi reboot, SD remount,
        // permission revoke-then-grant). The ContentObserver path still writes
        // empty results — that's the legitimate "user deleted everything" flow.
        if (initial.isNotEmpty() || cached.isEmpty()) {
            launch { LocalScanCache.write(context, initial) }
        }

        val resolver = context.contentResolver
        // ContentObserver(null) dispatches onChange on the binder thread; we
        // immediately hop to Dispatchers.IO so the MediaStore + SAF scan
        // (with per-file MediaMetadataRetriever calls) never blocks anything
        // visible. runCatching guards the system-thread callback so a transient
        // SecurityException from a revoked SAF permission can't crash the app.
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                launch(Dispatchers.IO) {
                    runCatching {
                        val updated = scanInternal(context)
                        trySend(updated)
                        LocalScanCache.write(context, updated)
                    }
                }
            }
        }
        resolver.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            /* notifyForDescendants */ true,
            observer,
        )
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.flowOn(Dispatchers.IO)

    private suspend fun scanInternal(context: Context): List<LocalTrack> {
        // SAF-pinned trees never need READ_MEDIA_AUDIO — the persistable URI
        // permission granted at pick time is enough. So scan them even when
        // the legacy permission isn't granted.
        val safTracks = scanSaf(context)
        if (!hasPermission(context)) return safTracks
        val mediaStoreTracks = scanMediaStore(context)
        return mergeDeduped(mediaStoreTracks, safTracks)
    }

    private fun mergeDeduped(
        a: List<LocalTrack>,
        b: List<LocalTrack>,
    ): List<LocalTrack> {
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a
        val seen = HashSet<String>(a.size + b.size)
        val out = ArrayList<LocalTrack>(a.size + b.size)
        for (t in a + b) {
            if (seen.add(t.uri.toString())) out += t
        }
        return out
    }

    private fun scanMediaStore(context: Context): List<LocalTrack> {
        val resolver: ContentResolver = context.contentResolver

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            // RELATIVE_PATH is Q+. On older devices DATA carries the absolute
            // path and we derive folder from there.
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 " +
            "AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_DURATION_MS.toString())
        val sortOrder =
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        val out = ArrayList<LocalTrack>()
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder,
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val relCol = cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol).orEmpty()
                val artist = cursor.getString(artistCol).orEmpty()
                    .let { if (it == "<unknown>") "" else it }
                val album = cursor.getString(albumCol).orEmpty()
                    .let { if (it.isBlank() || it == "<unknown>") null else it }
                val albumId = cursor.getLong(albumIdCol).takeIf { it != 0L }
                val duration = cursor.getLong(durationCol)
                val dateAdded = cursor.getLong(dateAddedCol) * 1000L

                val data = if (dataCol >= 0) cursor.getString(dataCol).orEmpty() else ""
                val relPath = if (relCol >= 0) cursor.getString(relCol).orEmpty() else ""
                val displayName = if (nameCol >= 0) cursor.getString(nameCol).orEmpty() else ""

                val (folderName, folderPath) = deriveFolder(relPath, data, displayName)

                val uri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id,
                )
                val albumArt = albumId?.let { ContentUris.withAppendedId(ALBUM_ART_URI, it) }

                out += LocalTrack(
                    id = id,
                    uri = uri,
                    title = title.ifBlank { displayName.substringBeforeLast('.') },
                    artist = artist,
                    album = album,
                    durationMs = duration,
                    albumId = albumId,
                    albumArtUri = albumArt,
                    folderName = folderName,
                    folderPath = folderPath,
                    dateAddedMs = dateAdded,
                    source = LocalTrack.Source.MediaStore,
                )
            }
        }
        return out
    }

    /**
     * Derive a `(displayName, fullPath)` pair for the folder this file
     * lives in. Q+ exposes RELATIVE_PATH (e.g. `Music/Album/`); pre-Q
     * gives only the absolute DATA path.
     */
    private fun deriveFolder(
        relPath: String,
        absPath: String,
        displayName: String,
    ): Pair<String, String> {
        if (relPath.isNotBlank()) {
            val trimmed = relPath.trimEnd('/')
            val name = trimmed.substringAfterLast('/').ifBlank { trimmed }
            return name to trimmed
        }
        if (absPath.isNotBlank()) {
            val parent = File(absPath).parent.orEmpty()
            val name = File(parent).name.ifBlank { parent }
            return name to parent
        }
        return "" to ""
    }

    private const val MIN_DURATION_MS = 30_000L

    /**
     * Walk every SAF tree the user has pinned via `OPEN_DOCUMENT_TREE` and
     * return the audio files inside. We use [DocumentsContract] directly
     * instead of `DocumentFile` to avoid the extra dependency — `DocumentFile`
     * is just a thin wrapper around the same provider calls.
     *
     * Audio metadata (title/artist/album/duration) is read via
     * [MediaMetadataRetriever] on the pinned URI; this is the same retriever
     * the local cover composable uses, so the cost has already been paid in
     * the cache for files the user actually plays.
     */
    private suspend fun scanSaf(context: Context): List<LocalTrack> {
        val prefs = LocalFolderPrefs.instance(context)
        val pinnedRaw = prefs.snapshot()
        if (pinnedRaw.isEmpty()) return emptyList()
        val resolver = context.contentResolver
        val out = ArrayList<LocalTrack>()
        for (raw in pinnedRaw) {
            val tree = runCatching { Uri.parse(raw) }.getOrNull() ?: continue
            // Re-validate the persistable permission. The user can revoke it
            // from system settings; once that happens we drop the tree from
            // the saved set so the UI stops showing a stale entry.
            val stillGranted = resolver.persistedUriPermissions.any {
                it.uri == tree && it.isReadPermission
            }
            if (!stillGranted) continue
            val rootDoc = DocumentsContract.getTreeDocumentId(tree)
            val rootChildrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, rootDoc)
            collectAudioFromTree(context, tree, rootChildrenUri, out)
        }
        return out
    }

    /**
     * Recursive depth-first walk of a SAF tree. Audio nodes get materialised
     * into [LocalTrack]; sub-directories are queued via the
     * `buildChildDocumentsUriUsingTree` helper.
     *
     * Iterative-with-stack rather than recursive-fn to keep the call frame
     * small on deeply nested folder hierarchies.
     */
    private fun collectAudioFromTree(
        context: Context,
        tree: Uri,
        rootChildren: Uri,
        out: MutableList<LocalTrack>,
    ) {
        val resolver = context.contentResolver
        val stack = ArrayDeque<Uri>()
        stack.addLast(rootChildren)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )
        while (stack.isNotEmpty()) {
            val childrenUri = stack.removeLast()
            resolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val docIdCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val modCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(docIdCol) ?: continue
                    val name = cursor.getString(nameCol).orEmpty()
                    val mime = cursor.getString(mimeCol).orEmpty()
                    val lastMod = cursor.getLong(modCol)
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        stack.addLast(
                            DocumentsContract.buildChildDocumentsUriUsingTree(tree, docId)
                        )
                    } else if (mime.startsWith("audio/")) {
                        val docUri = DocumentsContract.buildDocumentUriUsingTree(tree, docId)
                        val (title, artist, album, duration) = readSafMetadata(context, docUri)
                        if (duration in 1 until MIN_DURATION_MS) continue
                        // Synthetic id: stable hash of the document URI so the
                        // same SAF entry rehydrates with the same negative
                        // mediaId across scans. Bias into the Long.MAX/2..Long.MAX
                        // range so it never collides with MediaStore _IDs.
                        //
                        // Mix two String.hashCode() variants (forward + reversed)
                        // to lift entropy from 32 bits to ~56 bits — at 31 bits
                        // the birthday-paradox collision rate hit 50% around 54k
                        // SAF documents; with this mix the same risk needs ~250M
                        // documents. Masked to 56 bits so it sits between
                        // SAF_ID_BASE (2^40) and Long.MAX_VALUE without ever
                        // overflowing or going negative.
                        val s = docUri.toString()
                        val mix = s.hashCode().toLong() xor (s.reversed().hashCode().toLong() shl 31)
                        val safId = SAF_ID_BASE + (mix and 0x00FF_FFFF_FFFF_FFFFL)
                        out += LocalTrack(
                            id = safId,
                            uri = docUri,
                            title = title.ifBlank { name.substringBeforeLast('.') },
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            albumId = null,
                            albumArtUri = null,
                            folderName = "(cartella personalizzata)",
                            folderPath = tree.lastPathSegment.orEmpty(),
                            dateAddedMs = lastMod,
                            source = LocalTrack.Source.SafTree,
                        )
                    }
                }
            }
        }
    }

    private data class SafMeta(
        val title: String,
        val artist: String,
        val album: String?,
        val durationMs: Long,
    )

    private fun readSafMetadata(context: Context, uri: Uri): SafMeta {
        val mmr = MediaMetadataRetriever()
        return try {
            mmr.setDataSource(context, uri)
            val title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).orEmpty()
            val artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).orEmpty()
            val album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                ?.takeIf { it.isNotBlank() }
            val dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            SafMeta(title, artist, album, dur)
        } catch (_: Throwable) {
            SafMeta("", "", null, 0L)
        } finally {
            runCatching { mmr.release() }
        }
    }

    /**
     * SAF synthetic ids start at this base so their negatives never collide
     * with negated MediaStore `_ID`s, which start at 1 and grow monotonically.
     * 2^40 is comfortably above any realistic MediaStore ceiling.
     */
    private const val SAF_ID_BASE = 1L shl 40
}
