package com.mediaplayer.android.ui.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.LyricLineDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Test
import java.io.IOException

/**
 * Timed lyrics. Most of the catalogue has none, so the empty case is the
 * common one and it has to offer a way out — a fetch the user can trigger —
 * rather than a blank panel.
 *
 * The one race worth pinning: the import outlives the song it was started
 * for. Skipping while it is in flight used to paint the previous song's
 * lyrics over the new one.
 */
class LyricsViewTest : ScreenTest() {

    private fun line(ms: Long, text: String) = LyricLineDto(positionMs = ms, text = text)

    private fun view(songId: Long = 1L, positionMs: Long = 0L) {
        setScreen { LyricsView(songId = songId, positionMs = positionMs) }
        compose.waitForIdle()
    }

    @Test
    fun `lyrics render in order`() {
        coEvery { api.getLyrics(1L) } returns listOf(
            line(0L, "I'm a negative creep"),
            line(5_000L, "And I'm stoned"),
        )

        view()

        awaitText("I'm a negative creep")
        compose.onNodeWithText("And I'm stoned").assertIsDisplayed()
    }

    @Test
    fun `the sheet is titled`() {
        coEvery { api.getLyrics(any()) } returns listOf(line(0L, "One"))

        view()

        compose.onNodeWithText("Testo").assertIsDisplayed()
    }

    /**
     * An empty result and a failed request mean the same thing to the user —
     * no lyrics — and both have to offer the fetch.
     */
    @Test
    fun `a song with no lyrics offers to fetch them`() {
        coEvery { api.getLyrics(any()) } returns emptyList()

        view()

        awaitText("Testo non disponibile")
        compose.onNodeWithText("Scarica testo").assertIsDisplayed()
    }

    @Test
    fun `a failed load also offers to fetch`() {
        coEvery { api.getLyrics(any()) } throws IOException("offline")

        view()

        awaitText("Testo non disponibile")
        compose.onNodeWithText("Scarica testo").assertIsDisplayed()
    }

    @Test
    fun `fetching lyrics puts them on screen`() {
        coEvery { api.getLyrics(any()) } returns emptyList()
        coEvery { api.importLyrics(1L) } returns listOf(line(0L, "I'm a negative creep"))

        view(songId = 1L)
        awaitText("Scarica testo")
        compose.onNodeWithText("Scarica testo").performClick()

        awaitText("I'm a negative creep")
        coVerify(exactly = 1) { api.importLyrics(1L) }
    }

    /**
     * A fetch that comes back with nothing is a different message from "not
     * fetched yet": the source has been asked and doesn't have it.
     */
    @Test
    fun `a fetch that finds nothing says so and offers a retry`() {
        coEvery { api.getLyrics(any()) } returns emptyList()
        coEvery { api.importLyrics(any()) } returns emptyList()

        view()
        awaitText("Scarica testo")
        compose.onNodeWithText("Scarica testo").performClick()

        awaitText("Testo non trovato")
        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `a failed fetch offers a retry`() {
        coEvery { api.getLyrics(any()) } returns emptyList()
        coEvery { api.importLyrics(any()) } throws IOException("offline")

        view()
        awaitText("Scarica testo")
        compose.onNodeWithText("Scarica testo").performClick()

        awaitText("Riprova")
    }

    /** The playhead decides which line is the current one. */
    @Test
    fun `the line for the current position is the one highlighted`() {
        coEvery { api.getLyrics(any()) } returns listOf(
            line(0L, "First"),
            line(5_000L, "Second"),
            line(10_000L, "Third"),
        )

        view(positionMs = 6_000L)

        awaitText("Second")
        compose.onAllNodesWithText("Third").assertCountEquals(1)
    }

    @Test
    fun `a position before the first line still shows the lyrics`() {
        coEvery { api.getLyrics(any()) } returns listOf(line(5_000L, "First"))

        view(positionMs = 0L)

        awaitText("First")
    }
}
