package com.mediaplayer.android.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The clock decides how much of a track the user actually heard, which is
 * the input to every history and recommendation decision. These tests
 * drive it through the sequences the player produces — pause/resume,
 * transition-while-playing, transition-while-paused, teardown — with a
 * hand-cranked wall clock instead of real time.
 */
class ListenClockTest {

    /** Wall clock the test moves by hand. */
    private var wall = 1_000L
    private val clock = ListenClock { wall }

    private fun advance(ms: Long) {
        wall += ms
    }

    @Test
    fun `banks nothing before playback starts`() {
        assertEquals(0L, clock.bankedMs)
        assertFalse(clock.isRunning)
    }

    @Test
    fun `time only accrues once an interval is banked`() {
        clock.resume()
        advance(5_000)
        // Still open: callers bank before reading, so an unbanked interval
        // must not leak into the total.
        assertEquals(0L, clock.bankedMs)
        clock.bank()
        assertEquals(5_000L, clock.bankedMs)
    }

    @Test
    fun `paused time does not count`() {
        clock.resume()
        advance(3_000)
        clock.bank()
        advance(60_000) // paused
        clock.resume()
        advance(2_000)
        clock.bank()
        assertEquals(5_000L, clock.bankedMs)
    }

    @Test
    fun `resume while already running does not move the mark`() {
        clock.resume()
        advance(4_000)
        clock.resume() // spurious onIsPlayingChanged(true)
        advance(1_000)
        clock.bank()
        assertEquals(5_000L, clock.bankedMs)
    }

    @Test
    fun `bank on an idle clock is a no-op`() {
        clock.bank()
        clock.bank()
        assertEquals(0L, clock.bankedMs)
        assertFalse(clock.isRunning)
    }

    @Test
    fun `transition while playing keeps the clock running`() {
        clock.resume()
        advance(30_000)
        clock.bankAndContinue(stillPlaying = true)
        assertEquals(30_000L, clock.bankedMs)
        assertTrue(clock.isRunning)

        // The outgoing track's total is read here, then cleared for the
        // incoming one — which is already accruing.
        clock.reset()
        advance(7_000)
        clock.bank()
        assertEquals(7_000L, clock.bankedMs)
    }

    @Test
    fun `transition into a stopped player closes the interval`() {
        clock.resume()
        advance(12_000)
        clock.bankAndContinue(stillPlaying = false)
        assertEquals(12_000L, clock.bankedMs)
        assertFalse(clock.isRunning)
    }

    @Test
    fun `transition while paused accrues nothing`() {
        clock.resume()
        advance(8_000)
        clock.bank()
        advance(30_000) // paused on the last track
        // A skip taken while paused must not bill the silence, even if the
        // player reports itself as playing a moment later.
        clock.bankAndContinue(stillPlaying = true)
        assertEquals(8_000L, clock.bankedMs)
        assertFalse(clock.isRunning)
    }

    @Test
    fun `reset clears the total but leaves a running interval alone`() {
        clock.resume()
        advance(10_000)
        clock.bankAndContinue(stillPlaying = true)
        clock.reset()
        assertEquals(0L, clock.bankedMs)
        assertTrue(clock.isRunning)
        advance(3_000)
        clock.bank()
        // The head of the new track is not dropped by the reset.
        assertEquals(3_000L, clock.bankedMs)
    }

    @Test
    fun `full session sums every audible stretch`() {
        clock.resume()
        advance(10_000)
        clock.bank() // pause
        advance(5_000)
        clock.resume()
        advance(10_000)
        clock.bank() // pause
        advance(120_000)
        clock.resume()
        advance(15_000)
        clock.bank() // onCleared
        assertEquals(35_000L, clock.bankedMs)
    }

    @Test
    fun `a listen assembled from pauses still clears the full-play bar`() {
        // Three short bursts that individually look like skips add up to a
        // real listen — the clock is what makes that distinction possible.
        repeat(3) {
            clock.resume()
            advance(11_000)
            clock.bank()
            advance(2_000)
        }
        val record = PlayRecordingPolicy.evaluate(
            songId = 42L,
            listenedMs = clock.bankedMs,
            durationMs = 200_000L,
        )
        assertTrue(record!!.countsAsFullPlay)
    }
}
