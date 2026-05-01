package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.RecordPlayRequest
import com.mediaplayer.android.data.dto.SongDto

class HistoryRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    suspend fun record(
        songId: Long,
        durationListenedMs: Long,
        completionRatio: Double? = null,
        wasSkipped: Boolean? = null,
    ) = api.recordPlay(
        RecordPlayRequest(
            songId = songId,
            durationListenedMs = durationListenedMs,
            completionRatio = completionRatio,
            wasSkipped = wasSkipped,
        )
    )

    suspend fun recent(limit: Int = 20): List<SongDto> =
        api.recentSongs(limit)
}
