package com.mediaplayer.android.playback

/**
 * What one poll of a long-running backend job means for the UI.
 */
internal sealed interface JobPollOutcome {
    data object Done : JobPollOutcome
    data class Failed(val message: String) : JobPollOutcome
    data object StillRunning : JobPollOutcome
}

/**
 * Schedule and status reading for the two backend jobs the player polls:
 * video download and video re-initialisation. Both kick off a yt-dlp run
 * that can take minutes, so the client fires the request and then asks for
 * status on a widening interval rather than sitting on one HTTP call long
 * enough to hit OkHttp's read timeout.
 *
 * The cap matters as much as the backoff. A backend that never reaches a
 * terminal status — network flap, 502, a job the user navigated away from
 * — would otherwise leave the loop polling for the life of the process,
 * burning radio wakeups on a screen nobody is looking at. After
 * [MAX_ATTEMPTS] the caller reports a timeout and stops.
 */
internal object VideoJobPolling {

    /** First gap, short enough that quick jobs still feel immediate. */
    const val INITIAL_DELAY_MS = 2_000L

    /** Ceiling on the gap, so a slow job doesn't drift into minutes. */
    const val MAX_DELAY_MS = 30_000L

    /** Hard stop. With the schedule below this is roughly 13 minutes. */
    const val MAX_ATTEMPTS = 30

    /** Double until the ceiling. */
    fun nextDelayMs(currentMs: Long): Long = (currentMs * 2).coerceAtMost(MAX_DELAY_MS)

    /**
     * Total wall time the loop can burn before giving up — the sum of the
     * whole schedule. Useful to assert the cap is a sane bound rather than
     * an arbitrary count.
     */
    fun budgetMs(): Long {
        var wait = INITIAL_DELAY_MS
        var total = 0L
        repeat(MAX_ATTEMPTS) {
            total += wait
            wait = nextDelayMs(wait)
        }
        return total
    }

    /**
     * Read one status response. Anything that isn't a terminal status means
     * the job is still going — including statuses this build doesn't know
     * about, which must not be mistaken for success.
     *
     * @param fallbackMessage shown when the backend reports a failure with
     *   no explanation attached.
     */
    fun outcomeOf(status: String, error: String, fallbackMessage: String): JobPollOutcome =
        when (status) {
            STATUS_DONE -> JobPollOutcome.Done
            STATUS_ERROR -> JobPollOutcome.Failed(error.ifBlank { fallbackMessage })
            else -> JobPollOutcome.StillRunning
        }

    private const val STATUS_DONE = "DONE"
    private const val STATUS_ERROR = "ERROR"
}
