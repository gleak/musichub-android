package com.mediaplayer.android.data

import com.mediaplayer.android.data.dto.RecordPlayRequest
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.serializer

class HistoryRepository(
    private val api: MediaPlayerApi = Network.api,
) {
    /**
     * Persists the play event to the offline queue and returns immediately.
     * The drainer coroutine ships it to `/api/history` once the network is
     * up — callers no longer have to wrap this in a try/catch for IOException
     * because the queue owns the retry loop.
     */
    suspend fun record(
        songId: Long,
        durationListenedMs: Long,
        completionRatio: Double? = null,
        wasSkipped: Boolean? = null,
    ) = EventQueue.enqueuePlay(
        RecordPlayRequest(
            songId = songId,
            durationListenedMs = durationListenedMs,
            completionRatio = completionRatio,
            wasSkipped = wasSkipped,
        )
    )

    /**
     * Direct POST with queue fallback. Use only when the caller needs
     * the backend to have the play recorded *before* it does its next
     * read (e.g. resume-flush → /recent refresh on Home). Offline the
     * row still lands in the queue so we don't drop the play.
     */
    suspend fun recordImmediate(
        songId: Long,
        durationListenedMs: Long,
        completionRatio: Double? = null,
        wasSkipped: Boolean? = null,
    ) {
        val req = RecordPlayRequest(
            songId = songId,
            durationListenedMs = durationListenedMs,
            completionRatio = completionRatio,
            wasSkipped = wasSkipped,
        )
        try {
            api.recordPlay(req)
        } catch (_: java.io.IOException) {
            EventQueue.enqueuePlay(req)
        }
    }

    suspend fun recent(limit: Int = 20): List<SongDto> {
        return try {
            val fresh = api.recentSongs(limit)
            ReadCache.putJson(ReadCache.Keys.HISTORY_RECENT, fresh, ListSerializer(serializer()))
            fresh
        } catch (_: java.io.IOException) {
            ReadCache.getOrNull(ReadCache.Keys.HISTORY_RECENT, ListSerializer(serializer<SongDto>())) ?: emptyList()
        }
    }
}
