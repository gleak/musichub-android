package com.mediaplayer.android.data.sync

import kotlin.math.min

/** What the drainer should do with a queue row whose dispatch did not succeed. */
internal enum class EventDispatchAction {
    /** The row can never succeed. Remove it and move on. */
    DROP,

    /**
     * The row may still succeed. Bump its attempt count, push
     * `next_attempt_at` into the future, and stop draining this batch.
     */
    RETRY_LATER,
}

/**
 * The decision table for a failed dispatch, kept separate from [EventQueue] so
 * it can be exercised without a database, a network stack or a real token.
 *
 * The rule that matters most here is that RETRY_LATER always comes with a
 * backoff. An auth failure is answered by the server, not by the transport, so
 * connectivity still looks healthy and the outer drain loop will not block on
 * it; without a backoff the same row is re-read and re-sent immediately, which
 * turns one expired token into a continuous stream of requests for as long as
 * the process lives.
 */
internal object EventDispatchPolicy {

    private const val MAX_BACKOFF_MS = 5 * 60_000L

    /**
     * Shift ceiling for the exponential backoff. Nine rather than eight so the
     * final step (512 s) actually exceeds [MAX_BACKOFF_MS] and the cap becomes
     * reachable — clamped at eight the series topped out at 256 s and the cap
     * was dead code.
     */
    private const val MAX_BACKOFF_SHIFT = 9

    /**
     * [httpCode] is the HTTP status when the failure was an HTTP error, `null`
     * when it was a transport failure (no response at all).
     */
    fun actionFor(httpCode: Int?, isPoisonPill: Boolean): EventDispatchAction = when {
        // Malformed payload: no amount of retrying reshapes it.
        isPoisonPill -> EventDispatchAction.DROP

        // The token is dead or not yet refreshed. The user's action is still
        // valid, so it is never destroyed — it waits for a usable token.
        httpCode == 401 || httpCode == 403 -> EventDispatchAction.RETRY_LATER

        // Throttling: the server is explicitly asking us to come back later.
        httpCode == 408 || httpCode == 429 -> EventDispatchAction.RETRY_LATER

        // Any other 4xx is a permanent rejection; keeping it would wedge every
        // row queued behind it.
        httpCode != null && httpCode in 400..499 -> EventDispatchAction.DROP

        // 5xx and transport failures are transient by definition.
        else -> EventDispatchAction.RETRY_LATER
    }

    /** Exponential backoff in milliseconds for a row that has failed [attempts] times. */
    fun backoffMsFor(attempts: Int): Long =
        min(MAX_BACKOFF_MS, 1_000L * (1L shl min(attempts, MAX_BACKOFF_SHIFT)))
}
