package com.mediaplayer.android.playback

import com.mediaplayer.android.data.local.LocalMediaResolver

/**
 * What to report to the backend about one finished listen, or `null` when the
 * listen should not be reported at all.
 *
 * [completionRatio] is null when the duration was unknown, so the backend can
 * tell "no signal" apart from "played nothing".
 */
internal data class PlayRecord(
    val countsAsFullPlay: Boolean,
    val completionRatio: Double?,
)

/**
 * Decides whether a listen is worth recording and how it should be described.
 *
 * Extracted from the view model because this is the signal the recommender is
 * trained on: report too eagerly and rapid skipping floods it with noise,
 * report too little and genuine listens vanish. Neither failure is visible from
 * the app, which is exactly why the rules belong somewhere they can be checked.
 */
internal object PlayRecordingPolicy {

    /** A listen at or beyond this counts as a full play regardless of length. */
    const val LISTEN_THRESHOLD_MS = 30_000L

    /**
     * Floor below which nothing is reported. Sub-second listens are a user
     * mashing skip: one request per item, no useful signal.
     */
    const val MIN_RECORD_MS = 1_500L

    /**
     * @param songId the resolved song id; local tracks carry negative ids and
     *   never reach the backend's history, recommender or auto-download paths
     * @param durationMs the track length, or non-positive when still unknown
     */
    fun evaluate(songId: Long, listenedMs: Long, durationMs: Long): PlayRecord? {
        if (LocalMediaResolver.isLocal(songId)) return null
        if (listenedMs < MIN_RECORD_MS) return null

        // Half the track is the other way to earn a full play, so short songs
        // aren't permanently disqualified by the absolute threshold.
        val countsAsFullPlay = listenedMs >= LISTEN_THRESHOLD_MS ||
            (durationMs > 0 && listenedMs * 2 >= durationMs)

        val ratio = if (durationMs > 0) {
            // Seeking back and replaying can exceed the length; cap it.
            (listenedMs.toDouble() / durationMs).coerceAtMost(1.0)
        } else {
            null
        }
        return PlayRecord(countsAsFullPlay = countsAsFullPlay, completionRatio = ratio)
    }
}
