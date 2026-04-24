package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.SongDto

class LikedRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    suspend fun likedSongs(page: Int = 0, size: Int = 20): PageResponse<SongDto> =
        api.getLikedSongs(page, size)

    suspend fun like(songId: Long) = api.likeSong(songId)

    suspend fun unlike(songId: Long) = api.unlikeSong(songId)

    suspend fun status(ids: List<Long>): Set<Long> =
        api.getLikedStatus(ids).toHashSet()
}
