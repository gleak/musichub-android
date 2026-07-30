package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.LyricLineDto

class LyricsRepository {
    /** Resolved per call so the client can be swapped after construction. */
    private val api: MediaPlayerApi get() = Network.api

    suspend fun getLyrics(songId: Long): List<LyricLineDto> = api.getLyrics(songId)

    /** Trigger an on-demand lyric download for a single song. */
    suspend fun importLyrics(songId: Long): List<LyricLineDto> = api.importLyrics(songId)
}
