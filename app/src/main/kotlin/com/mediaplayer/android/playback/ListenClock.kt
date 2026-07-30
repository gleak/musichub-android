package com.mediaplayer.android.playback

/**
 * Tracks how long the current track has actually been audible, as opposed
 * to how long it has been loaded. Playback pauses, the user backgrounds
 * the app, a call comes in — none of that should count towards the listen
 * that gets reported to the backend.
 *
 * The clock banks a running interval on every pause and every track
 * transition, and [bankedMs] is what [PlayRecordingPolicy] then judges.
 *
 * Extracted from the view model because the arithmetic is easy to get
 * subtly wrong and impossible to notice when it is: a pause that forgets
 * to close the open interval silently loses the listen, and a resume that
 * forgets to move the mark counts the same seconds twice. Neither shows up
 * in the app — they surface weeks later as skewed recommendations.
 *
 * Not thread-safe. Driven entirely from `Player.Listener` callbacks, which
 * Media3 delivers on the application main looper.
 *
 * @param now injectable wall clock; tests drive it instead of sleeping.
 */
internal class ListenClock(private val now: () -> Long = System::currentTimeMillis) {

    private var accumulatedMs: Long = 0L
    private var startedAt: Long = NOT_RUNNING

    /**
     * Listening time closed off so far, excluding any interval still open.
     * Callers bank before reading — see the call sites in the view model —
     * so this is the full listen for the track that just ended.
     */
    val bankedMs: Long get() = accumulatedMs

    /** Whether an interval is currently open. */
    val isRunning: Boolean get() = startedAt != NOT_RUNNING

    /** Open an interval. No-op when one is already running. */
    fun resume() {
        if (startedAt == NOT_RUNNING) startedAt = now()
    }

    /** Close the open interval, adding it to the total. No-op when idle. */
    fun bank() {
        if (startedAt == NOT_RUNNING) return
        accumulatedMs += now() - startedAt
        startedAt = NOT_RUNNING
    }

    /**
     * Close the open interval and immediately reopen it when playback is
     * carrying on into the next track — the transition case, where the
     * outgoing track's total has to be read out before [reset] clears it
     * but the clock must not lose the seconds in between.
     *
     * Does nothing when no interval is open: a transition while paused
     * hasn't accrued anything, and starting the clock here would count
     * silence.
     */
    fun bankAndContinue(stillPlaying: Boolean) {
        if (startedAt == NOT_RUNNING) return
        accumulatedMs += now() - startedAt
        startedAt = if (stillPlaying) now() else NOT_RUNNING
    }

    /**
     * Zero the banked total for a new track, leaving any open interval
     * alone — after a transition the clock is already running against the
     * incoming track and restarting it here would drop the head of it.
     */
    fun reset() {
        accumulatedMs = 0L
    }

    private companion object {
        const val NOT_RUNNING = -1L
    }
}
