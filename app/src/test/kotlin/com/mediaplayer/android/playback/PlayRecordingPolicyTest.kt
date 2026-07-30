package com.mediaplayer.android.playback

import com.mediaplayer.android.playback.PlayRecordingPolicy.LISTEN_THRESHOLD_MS
import com.mediaplayer.android.playback.PlayRecordingPolicy.MIN_RECORD_MS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * This is the signal the recommender learns from, and both ways of getting it
 * wrong are invisible from the app: report too eagerly and rapid skipping
 * floods it with noise, report too little and real listens disappear.
 */
class PlayRecordingPolicyTest {

    @Test
    fun `a micro-skip is not reported at all`() {
        assertNull(evaluate(listenedMs = 0L))
        assertNull(evaluate(listenedMs = MIN_RECORD_MS - 1))
    }

    @Test
    fun `the floor is inclusive`() {
        assertNotNull(evaluate(listenedMs = MIN_RECORD_MS))
    }

    /** Local tracks have no backend counterpart; reporting them pollutes taste. */
    @Test
    fun `local tracks are never reported`() {
        assertNull(PlayRecordingPolicy.evaluate(songId = -5L, listenedMs = 60_000L, durationMs = 200_000L))
    }

    @Test
    fun `thirty seconds is a full play whatever the length`() {
        val record = evaluate(listenedMs = LISTEN_THRESHOLD_MS, durationMs = 10 * 60_000L)

        assertTrue(record!!.countsAsFullPlay)
    }

    @Test
    fun `a long track abandoned early is not a full play`() {
        val record = evaluate(listenedMs = 20_000L, durationMs = 10 * 60_000L)

        assertFalse(record!!.countsAsFullPlay)
    }

    /**
     * Half the track is the other route to a full play, so a short song is not
     * permanently disqualified by the absolute threshold.
     */
    @Test
    fun `half of a short track is a full play`() {
        val record = evaluate(listenedMs = 10_000L, durationMs = 20_000L)

        assertTrue(record!!.countsAsFullPlay)
    }

    @Test
    fun `just under half of a short track is not a full play`() {
        val record = evaluate(listenedMs = 9_000L, durationMs = 20_000L)

        assertFalse(record!!.countsAsFullPlay)
    }

    @Test
    fun `the completion ratio reflects how much was heard`() {
        val record = evaluate(listenedMs = 50_000L, durationMs = 200_000L)

        assertEquals(0.25, record!!.completionRatio!!, 0.0001)
    }

    /** Seeking back and replaying can exceed the length; the ratio still caps at one. */
    @Test
    fun `the completion ratio never exceeds one`() {
        val record = evaluate(listenedMs = 400_000L, durationMs = 200_000L)

        assertEquals(1.0, record!!.completionRatio!!, 0.0001)
    }

    /**
     * A null ratio means "no signal", which the backend must be able to tell
     * apart from "played nothing" — hence null rather than zero.
     */
    @Test
    fun `an unknown duration yields no ratio rather than zero`() {
        assertNull(evaluate(listenedMs = 60_000L, durationMs = 0L)!!.completionRatio)
        assertNull(evaluate(listenedMs = 60_000L, durationMs = -1L)!!.completionRatio)
    }

    @Test
    fun `an unknown duration can still earn a full play on time alone`() {
        assertTrue(evaluate(listenedMs = LISTEN_THRESHOLD_MS, durationMs = 0L)!!.countsAsFullPlay)
        assertFalse(evaluate(listenedMs = 5_000L, durationMs = 0L)!!.countsAsFullPlay)
    }

    /**
     * The flush path used to compute the full-play rule inline, without the
     * micro-skip floor. A bogus duration — which the player reports as a
     * matter of course before the track is prepared — then let a sub-second
     * listen through as a full play, recording it in history and triggering
     * an auto-download of a song the user skipped past instantly.
     */
    @Test
    fun `a tiny duration cannot turn a micro-skip into a full play`() {
        assertNull(evaluate(listenedMs = 200L, durationMs = 100L))
        assertNull(evaluate(listenedMs = 1L, durationMs = 1L))
    }

    @Test
    fun `a display label joins what is present`() {
        assertEquals("Song — Artist", PlayRecordingPolicy.displayLabel("Song", "Artist"))
        assertEquals("Song", PlayRecordingPolicy.displayLabel("Song", null))
        assertEquals("Artist", PlayRecordingPolicy.displayLabel(null, "Artist"))
    }

    /** An empty label would render as a blank history row, so use null. */
    @Test
    fun `a display label with nothing to say is null`() {
        assertNull(PlayRecordingPolicy.displayLabel(null, null))
        assertNull(PlayRecordingPolicy.displayLabel("", "   "))
    }

    private fun evaluate(
        listenedMs: Long,
        durationMs: Long = 200_000L,
        songId: Long = 42L,
    ) = PlayRecordingPolicy.evaluate(songId = songId, listenedMs = listenedMs, durationMs = durationMs)
}
