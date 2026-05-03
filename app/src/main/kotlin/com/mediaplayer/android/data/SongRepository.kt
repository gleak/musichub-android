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
        genre: String? = null,
        page: Int = 0,
        size: Int = 20,
    ): PageResponse<SongDto> {
        // Backend treats an empty q/genre and a missing one the same way;
        // normalise to null so we don't litter the query string with `?q=`.
        val normalizedQ = query?.trim()?.takeIf { it.isNotEmpty() }
        val normalizedGenre = genre?.trim()?.takeIf { it.isNotEmpty() }
        return api.listSongs(
            query = normalizedQ,
            genre = normalizedGenre,
            page = page,
            size = size,
        )
    }

    suspend fun redownload(songId: Long): SongDto = api.redownloadSong(songId)

    /** Report a song as "wrong"; backend wipes the file + references. Irreversible. */
    suspend fun flagWrong(songId: Long) = api.flagSongWrong(songId)

    suspend fun downloadVideo(songId: Long) = api.downloadVideo(songId)

    suspend fun getDownloadVideoStatus(songId: Long) = api.getDownloadVideoStatus(songId)

    suspend fun reinitializeVideo(songId: Long) = api.reinitializeVideo(songId)

    suspend fun getReinitializeStatus(songId: Long) = api.getReinitializeStatus(songId)
}
