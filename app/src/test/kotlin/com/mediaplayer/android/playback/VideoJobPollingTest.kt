package com.mediaplayer.android.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoJobPollingTest {

    @Test
    fun `the delay doubles until it hits the ceiling`() {
        val schedule = generateSequence(VideoJobPolling.INITIAL_DELAY_MS) {
            VideoJobPolling.nextDelayMs(it)
        }.take(6).toList()
        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L), schedule)
    }

    @Test
    fun `the ceiling holds no matter how long the job runs`() {
        var wait = VideoJobPolling.INITIAL_DELAY_MS
        repeat(VideoJobPolling.MAX_ATTEMPTS) { wait = VideoJobPolling.nextDelayMs(wait) }
        assertEquals(VideoJobPolling.MAX_DELAY_MS, wait)
    }

    /**
     * The cap is what keeps a wedged backend from polling for the life of
     * the process. Long enough for a real yt-dlp run, short enough that an
     * abandoned job stops waking the radio.
     */
    @Test
    fun `the attempt cap bounds the loop to a sane wall time`() {
        val budget = VideoJobPolling.budgetMs()
        assertTrue("budget was ${budget}ms", budget in 5 * 60_000L..20 * 60_000L)
    }

    @Test
    fun `DONE ends the poll`() {
        assertEquals(
            JobPollOutcome.Done,
            VideoJobPolling.outcomeOf("DONE", error = "", fallbackMessage = "boom"),
        )
    }

    @Test
    fun `ERROR carries the backend explanation`() {
        val outcome = VideoJobPolling.outcomeOf("ERROR", "yt-dlp exited 1", "boom")
        assertEquals(JobPollOutcome.Failed("yt-dlp exited 1"), outcome)
    }

    @Test
    fun `an unexplained failure still says something`() {
        assertEquals(
            JobPollOutcome.Failed("boom"),
            VideoJobPolling.outcomeOf("ERROR", error = "   ", fallbackMessage = "boom"),
        )
    }

    @Test
    fun `in-flight statuses keep the poll going`() {
        listOf("PENDING", "RUNNING", "QUEUED").forEach {
            assertEquals(JobPollOutcome.StillRunning, VideoJobPolling.outcomeOf(it, "", "boom"))
        }
    }

    /**
     * A status this build doesn't recognise must not read as success — the
     * UI would flip the song to "has video" for a job that never finished.
     */
    @Test
    fun `an unknown status is not success`() {
        assertEquals(
            JobPollOutcome.StillRunning,
            VideoJobPolling.outcomeOf("SOMETHING_NEW", "", "boom"),
        )
        assertEquals(JobPollOutcome.StillRunning, VideoJobPolling.outcomeOf("done", "", "boom"))
    }
}
