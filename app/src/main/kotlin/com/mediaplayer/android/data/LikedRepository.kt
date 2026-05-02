package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import kotlinx.serialization.serializer
import java.io.IOException

class LikedRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    /**
     * Page 0 is cached because it's what the LikedScreen lands on.
     * Subsequent pages are pure pagination — never cached, never useful
     * offline (the user can't scroll past what they've already loaded).
     */
    suspend fun likedSongs(page: Int = 0, size: Int = 20): PageResponse<SongDto> {
        if (page != 0) return api.getLikedSongs(page, size)
        return try {
            val fresh = api.getLikedSongs(page, size)
            ReadCache.putJson(ReadCache.Keys.LIKED_PAGE0, fresh, serializer<PageResponse<SongDto>>())
            fresh
        } catch (e: IOException) {
            ReadCache.getOrNull(ReadCache.Keys.LIKED_PAGE0, serializer<PageResponse<SongDto>>()) ?: throw e
        }
    }

    /** Queued — toggles dedupe per songId (heart-on then heart-off cancels). */
    suspend fun like(songId: Long) = EventQueue.enqueueLike(songId)

    suspend fun unlike(songId: Long) = EventQueue.enqueueUnlike(songId)

    suspend fun status(ids: List<Long>): Set<Long> =
        api.getLikedStatus(ids).toHashSet()
}
