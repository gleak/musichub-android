package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.SongDto

/**
 * Thin wrapper over [MediaPlayerApi]. Exists so the ViewModel can depend on
 * an interface we control rather than Retrofit's generated one — makes it
 * trivial to swap for a fake in unit tests (M5+).
 */
class SongRepository(
    private val api: MediaPlayerApi = Network.api,
) {

    suspend fun listSongs(
        query: String?,
        page: Int = 0,
        size: Int = 20,
    ): PageResponse<SongDto> {
        // Backend treats an empty q and a missing q the same way; normalise
        // to null so we don't litter the query string with `?q=`.
        val normalized = query?.trim()?.takeIf { it.isNotEmpty() }
        return api.listSongs(query = normalized, page = page, size = size)
    }

    suspend fun redownload(songId: Long): SongDto = api.redownloadSong(songId)

    suspend fun downloadVideo(songId: Long): SongDto = api.downloadVideo(songId)

    suspend fun reinitializeVideo(songId: Long) = api.reinitializeVideo(songId)

    suspend fun getReinitializeStatus(songId: Long) = api.getReinitializeStatus(songId)
}
