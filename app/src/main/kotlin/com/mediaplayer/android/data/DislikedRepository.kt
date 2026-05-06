package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.serializer
import java.io.IOException

/**
 * Mirror of [LikedRepository] for the negative-signal side: per-user
 * dislikes for songs and artists. All mutations go through [EventQueue]
 * so the toggle survives offline → online transitions; reads cache
 * page 0 / artist list so the "Don't recommend" screen renders without
 * network.
 */
class DislikedRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    suspend fun dislikedSongs(page: Int = 0, size: Int = 20): PageResponse<SongDto> {
        if (page != 0) return api.getDislikedSongs(page, size)
        return try {
            val fresh = api.getDislikedSongs(page, size)
            ReadCache.putJson(
                ReadCache.Keys.DISLIKED_SONGS_PAGE0,
                fresh,
                serializer<PageResponse<SongDto>>(),
            )
            fresh
        } catch (e: IOException) {
            ReadCache.getOrNull(
                ReadCache.Keys.DISLIKED_SONGS_PAGE0,
                serializer<PageResponse<SongDto>>(),
            ) ?: throw e
        }
    }

    suspend fun dislikedArtists(): List<String> {
        return try {
            val fresh = api.getDislikedArtists()
            ReadCache.putJson(
                ReadCache.Keys.DISLIKED_ARTISTS,
                fresh,
                ListSerializer(String.serializer()),
            )
            fresh
        } catch (e: IOException) {
            ReadCache.getOrNull(
                ReadCache.Keys.DISLIKED_ARTISTS,
                ListSerializer(String.serializer()),
            ) ?: throw e
        }
    }

    /** Toggles dedupe per songId (dislike then undislike cancels). */
    suspend fun dislikeSong(songId: Long, displayLabel: String? = null) =
        EventQueue.enqueueDislikeSong(songId, displayLabel)

    suspend fun undislikeSong(songId: Long, displayLabel: String? = null) =
        EventQueue.enqueueUndislikeSong(songId, displayLabel)

    suspend fun dislikeArtist(artist: String) = EventQueue.enqueueDislikeArtist(artist)

    suspend fun undislikeArtist(artist: String) = EventQueue.enqueueUndislikeArtist(artist)

    suspend fun dislikedSongStatus(ids: List<Long>): Set<Long> =
        if (ids.isEmpty()) emptySet() else api.getDislikedSongStatus(ids).toHashSet()
}
