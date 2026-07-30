package com.mediaplayer.android.playback

import androidx.media3.common.Player

/**
 * A song that ended earlier than its known duration said it would.
 *
 * [earlyByMs] is the gap the decision was made on, carried along so callers can
 * log it: the size of the gap is what separates a genuinely truncated file
 * (typically many tens of seconds) from a duration estimate that was simply a
 * little optimistic.
 */
/** How far [PhantomSkipHealer] is allowed to go in reacting to a phantom skip. */
internal enum class PhantomSkipRemedy {
    /** Drop the streaming cache entry only, leaving any offline copy alone. */
    EVICT_STREAM_CACHE_ONLY,

    /** Also delete the offline copy and queue a fresh download of it. */
    EVICT_AND_REDOWNLOAD,
}

internal data class PrematureEndOfStream(
    val songId: Long,
    val endedAtMs: Long,
    val durationMs: Long,
) {
    val earlyByMs: Long get() = durationMs - endedAtMs
}

/**
 * Decides whether a position discontinuity looks like a truncated-bytes phantom
 * skip. Split out of [PhantomSkipHealer] so the decision can be exercised on its
 * own: the healer's reaction is to delete a song's offline copy and re-queue a
 * download, which on mobile data will not run, so a wrong verdict costs the user
 * their offline copy for the rest of the drive.
 *
 * Deliberately free of Android and of any singleton: everything it needs
 * arrives as a parameter.
 */
internal object PhantomSkipPolicy {

    /**
     * Floor for the gap that counts as truncation. Wide enough to ignore
     * gapless trims and rounding on short tracks, where a percentage would be
     * meaninglessly small.
     */
    const val PREMATURE_EOS_MARGIN_MS = 10_000L

    /**
     * The gap must also be this share of the track before it counts.
     *
     * The comparison is between two numbers of unequal quality: for a CBR/VBR
     * MP3 without an accurate seek header, `Player.duration` is derived from
     * bitrate and content length rather than measured, and that estimate drifts
     * proportionally to the length of the track. A fixed floor alone therefore
     * fires on long tracks purely because the estimate is off, which costs a
     * healthy offline copy.
     */
    const val PREMATURE_EOS_MARGIN_PERCENT = 10

    /**
     * How early a track must end to be considered truncated: the floor, or a
     * share of the track, whichever is larger.
     */
    fun marginMsFor(durationMs: Long): Long =
        maxOf(PREMATURE_EOS_MARGIN_MS, durationMs * PREMATURE_EOS_MARGIN_PERCENT / 100)

    /**
     * What to do about a detected phantom skip.
     *
     * The two remedies are not equally safe. Evicting the streaming cache is
     * free — it is a cache, and the bytes come back on the next play. Deleting
     * the offline copy is not: the replacement download only runs on an
     * unmetered network, so doing it in a car destroys the copy and cannot
     * rebuild it, leaving the song worse off than the skip that triggered this.
     * So the destructive remedy is only offered when the repair can actually
     * complete.
     */
    fun remedyFor(canRedownloadNow: Boolean): PhantomSkipRemedy =
        if (canRedownloadNow) PhantomSkipRemedy.EVICT_AND_REDOWNLOAD
        else PhantomSkipRemedy.EVICT_STREAM_CACHE_ONLY

    /**
     * Returns the detection when [discontinuityReason] is a genuine
     * auto-advance that landed more than [PREMATURE_EOS_MARGIN_MS] short of
     * [knownDurationMs], or `null` when there is nothing to act on.
     *
     * A controller-driven skip (Bluetooth or steering-wheel NEXT) arrives as a
     * seek rather than an auto transition, so it is never reported here — which
     * is what keeps the two causes distinguishable in the logs.
     *
     * [mediaId] accepts both the bare numeric form used by the phone UI and the
     * `song:{id}` form used by Android Auto, the media library and playback
     * resumption.
     */
    fun detect(
        discontinuityReason: Int,
        knownDurationMs: Long,
        endedAtMs: Long,
        mediaId: String?,
    ): PrematureEndOfStream? {
        if (discontinuityReason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION) return null
        if (knownDurationMs <= 0L) return null
        if (knownDurationMs - endedAtMs < marginMsFor(knownDurationMs)) return null
        val songId = mediaId?.removePrefix("song:")?.toLongOrNull() ?: return null
        return PrematureEndOfStream(
            songId = songId,
            endedAtMs = endedAtMs,
            durationMs = knownDurationMs,
        )
    }
}
