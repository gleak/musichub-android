package com.mediaplayer.android.data.local

import android.net.Uri

/**
 * One on-device audio file surfaced from MediaStore (or a SAF-pinned tree).
 *
 * The [id] is the MediaStore `_ID` for items resolved through the standard
 * provider, or a stable hash of the document URI for SAF-pinned items
 * (negative-flagged below the SAF threshold). Either way it stays unique
 * within the app session — every local track is broadcast through Media3 as
 * `mediaId = "-$id"` so the playback layer can detect it without colliding
 * with backend song ids (which are always positive).
 */
data class LocalTrack(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val albumId: Long?,
    val albumArtUri: Uri?,
    val folderName: String,
    val folderPath: String,
    val dateAddedMs: Long,
    val source: Source = Source.MediaStore,
) {
    enum class Source { MediaStore, SafTree }
}
