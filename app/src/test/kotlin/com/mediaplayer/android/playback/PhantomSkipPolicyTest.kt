package com.mediaplayer.android.playback

import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.playback.PhantomSkipPolicy.PREMATURE_EOS_MARGIN_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Plain JVM tests — [PhantomSkipPolicy] deliberately has no Android
 * dependency, so no Robolectric sandbox is needed here.
 *
 * These pin down when the healer is allowed to delete a song's offline copy.
 * That reaction is expensive to get wrong: the replacement download is gated on
 * unmetered network, so in a car the song simply loses its offline copy.
 */
@UnstableApi
class PhantomSkipPolicyTest {

    @Test
    fun `detects a song that ended well before its duration`() {
        val detected = detect(endedAtMs = 30_000L, durationMs = 200_000L)

        assertNotNull(detected)
        assertEquals(42L, detected!!.songId)
        assertEquals(170_000L, detected.earlyByMs)
    }

    @Test
    fun `ignores a song that played to the end`() {
        assertNull(detect(endedAtMs = 200_000L, durationMs = 200_000L))
    }

    @Test
    fun `ignores a gap one millisecond under the margin`() {
        val duration = 200_000L
        val margin = PhantomSkipPolicy.marginMsFor(duration)

        assertNull(detect(endedAtMs = duration - margin + 1, durationMs = duration))
    }

    /** The boundary is inclusive: a gap of exactly the margin counts. */
    @Test
    fun `detects a gap of exactly the margin`() {
        val duration = 200_000L
        val margin = PhantomSkipPolicy.marginMsFor(duration)

        assertNotNull(detect(endedAtMs = duration - margin, durationMs = duration))
    }

    /**
     * Short tracks fall back to the absolute floor, where a percentage would be
     * too small to survive ordinary rounding.
     */
    @Test
    fun `short tracks use the absolute floor`() {
        assertEquals(PREMATURE_EOS_MARGIN_MS, PhantomSkipPolicy.marginMsFor(30_000L))
        assertEquals(PREMATURE_EOS_MARGIN_MS, PhantomSkipPolicy.marginMsFor(0L))
    }

    /**
     * Long tracks scale, because the duration they are compared against is
     * itself an estimate that drifts with length.
     */
    @Test
    fun `long tracks scale the margin with duration`() {
        assertEquals(60_000L, PhantomSkipPolicy.marginMsFor(600_000L))
        assertEquals(20_000L, PhantomSkipPolicy.marginMsFor(200_000L))
    }

    /**
     * The regression this scaling exists for: on a ten-minute track a 30 s gap
     * is well within duration-estimate error, and healing it would delete a
     * healthy offline copy.
     */
    @Test
    fun `ignores a gap that would have fired under a flat margin`() {
        assertNull(detect(endedAtMs = 570_000L, durationMs = 600_000L))
    }

    /**
     * A Bluetooth or steering-wheel NEXT arrives as a seek, not an auto
     * transition. Healing on those would delete offline copies of perfectly
     * good songs every time the user skips.
     */
    @Test
    fun `ignores discontinuities that are not auto transitions`() {
        val reasons = listOf(
            Player.DISCONTINUITY_REASON_SEEK,
            Player.DISCONTINUITY_REASON_SEEK_ADJUSTMENT,
            Player.DISCONTINUITY_REASON_REMOVE,
            Player.DISCONTINUITY_REASON_SKIP,
            Player.DISCONTINUITY_REASON_INTERNAL,
        )
        for (reason in reasons) {
            assertNull(
                "reason $reason should not be treated as a phantom skip",
                detect(endedAtMs = 0L, durationMs = 200_000L, reason = reason),
            )
        }
    }

    @Test
    fun `ignores an unknown duration`() {
        assertNull(detect(endedAtMs = 0L, durationMs = 0L))
        assertNull(detect(endedAtMs = 0L, durationMs = -1L))
    }

    @Test
    fun `accepts the song prefixed media id used by Android Auto`() {
        val detected = detect(endedAtMs = 0L, durationMs = 200_000L, mediaId = "song:7")

        assertEquals(7L, detected?.songId)
    }

    @Test
    fun `ignores a media id that is missing or not a song`() {
        assertNull(detect(endedAtMs = 0L, durationMs = 200_000L, mediaId = null))
        assertNull(detect(endedAtMs = 0L, durationMs = 200_000L, mediaId = ""))
        assertNull(detect(endedAtMs = 0L, durationMs = 200_000L, mediaId = "qu:3|9"))
    }

    /** Local tracks carry negative ids; the policy reports them and the healer filters them. */
    @Test
    fun `reports local negative ids and leaves the filtering to the caller`() {
        assertEquals(-5L, detect(endedAtMs = 0L, durationMs = 200_000L, mediaId = "-5")?.songId)
    }

    /**
     * The asymmetry that motivates the whole remedy split: the streaming cache
     * refills itself, an offline copy deleted on mobile data does not.
     */
    @Test
    fun `only touches the offline copy when the replacement download can run`() {
        assertEquals(
            PhantomSkipRemedy.EVICT_AND_REDOWNLOAD,
            PhantomSkipPolicy.remedyFor(canRedownloadNow = true),
        )
        assertEquals(
            PhantomSkipRemedy.EVICT_STREAM_CACHE_ONLY,
            PhantomSkipPolicy.remedyFor(canRedownloadNow = false),
        )
    }

    private fun detect(
        endedAtMs: Long,
        durationMs: Long,
        reason: Int = Player.DISCONTINUITY_REASON_AUTO_TRANSITION,
        mediaId: String? = "42",
    ): PrematureEndOfStream? = PhantomSkipPolicy.detect(
        discontinuityReason = reason,
        knownDurationMs = durationMs,
        endedAtMs = endedAtMs,
        mediaId = mediaId,
    )
}
