package com.mediaplayer.android.data.local

import android.net.Uri
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory bridge from the synthetic negative `mediaId` we hand to Media3
 * back to the source [LocalTrack]. The catalog lives in MediaStore — we
 * never persist anything here, just keep the latest scan available so
 * SongCover, the now-playing chrome, and the playback timeline can resolve
 * artwork URIs and stream URIs without re-querying the provider per row.
 *
 * Negative-id convention: backend SongDto ids are always positive Longs;
 * local items round-trip as the negation of their MediaStore `_ID` so a
 * single SongDto carrier covers both worlds with no schema change.
 */
object LocalMediaResolver {
    private val map = ConcurrentHashMap<Long, LocalTrack>()

    fun register(track: LocalTrack) { map[track.id] = track }

    fun registerAll(tracks: Collection<LocalTrack>) {
        tracks.forEach { map[it.id] = it }
    }

    fun replaceAll(tracks: Collection<LocalTrack>) {
        map.clear()
        registerAll(tracks)
    }

    /** Look up by the synthetic *signed* id (the negative form we hand Media3). */
    fun get(syntheticId: Long): LocalTrack? =
        if (syntheticId < 0) map[-syntheticId] else null

    fun getByLocalId(localId: Long): LocalTrack? = map[localId]

    fun isLocal(songId: Long): Boolean = songId < 0L

    fun streamUri(syntheticId: Long): Uri? = get(syntheticId)?.uri

    fun artworkUri(syntheticId: Long): Uri? = get(syntheticId)?.albumArtUri
}
