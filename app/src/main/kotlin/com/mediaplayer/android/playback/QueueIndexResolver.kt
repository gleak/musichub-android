package com.mediaplayer.android.playback

import kotlin.math.abs

/**
 * Resolves the live timeline index for a queue row the user tapped.
 *
 * The index baked into a queue row goes stale between the snapshot the sheet
 * rendered and the tap: the service front-prunes played history on every
 * transition, which shifts every index down, and the endless engine appends a
 * fresh pass of the whole pool, which means the same song legitimately appears
 * in the timeline many times over.
 *
 * Extracted from the view model so this arithmetic can be exercised on its own
 * — acting on the wrong occurrence means seeking backwards into history, or
 * deleting a row the user is not looking at.
 */
internal object QueueIndexResolver {

    /**
     * @param expectedSongId the song the tapped row was showing
     * @param hintIndex the index that row carried when the sheet was built
     * @param timelineIds resolved song ids of the live timeline, in order
     * @return the index to act on, or `null` when the song is no longer present
     */
    fun resolve(expectedSongId: Long, hintIndex: Int, timelineIds: List<Long?>): Int? {
        // The hint is right the overwhelming majority of the time; trust it
        // when it still points at the expected song.
        if (hintIndex in timelineIds.indices && timelineIds[hintIndex] == expectedSongId) {
            return hintIndex
        }
        var best: Int? = null
        for (i in timelineIds.indices) {
            if (timelineIds[i] != expectedSongId) continue
            if (best == null || abs(i - hintIndex) < abs(best - hintIndex)) best = i
        }
        return best
    }
}
