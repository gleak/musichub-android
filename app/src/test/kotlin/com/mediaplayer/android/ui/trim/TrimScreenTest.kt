package com.mediaplayer.android.ui.trim

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.player.PlayerSheetTest
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * The trim editor. Its preview runs on the host player rather than a second
 * one, so every transport control here has to reach the same MediaController
 * the lockscreen and the car are driving — a jump-to-IN that seeks the wrong
 * player looks like a dead button.
 *
 * The waveform is decoded from the audio in the background. When that can't
 * happen the screen falls back to a synthetic curve, which is what makes it
 * renderable here at all: the editor stays usable while the decode runs, or
 * fails.
 */
@UnstableApi
class TrimScreenTest : PlayerSheetTest() {

    private val source = song(
        1L,
        title = "Bohemian",
        artist = "Queen",
        durationMs = 200_000L,
    )

    private fun screen(
        song: SongDto = source,
        onClose: () -> Unit = {},
        onSaved: (SongDto) -> Unit = {},
    ) {
        val playbackVm = connectPlayer(listOf(track("1")))
        setScreen {
            TrimScreen(
                song = song,
                playbackVm = playbackVm,
                onClose = onClose,
                onSaved = onSaved,
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun `the editor names the track and both of its steps`() {
        screen()

        compose.onNodeWithText("Bohemian").assertIsDisplayed()
        compose.onNodeWithText("// 01 · ASCOLTO · SCRUB LIBERO").assertIsDisplayed()
        compose.onNodeWithText("// 02 · TAGLIO · SPOSTA I PUNTI IN / OUT").assertIsDisplayed()
    }

    @Test
    fun `the markers open inside the track`() {
        screen()

        // Eyebrow labels render prefixed; the editor is taller than the
        // viewport, so the marker boxes exist below the fold.
        compose.onNodeWithText("// IN").assertExists()
        compose.onNodeWithText("// OUT").assertExists()
        // IN opens 5s in, OUT 5s before the end of a 200s track.
        compose.onNodeWithText("00:05.0").assertExists()
        compose.onNodeWithText("03:15.0").assertExists()
    }

    /**
     * The result line is the answer to "what am I about to save?" — the kept
     * window and how much of the original it drops.
     */
    @Test
    fun `the result card reports the window and what it cuts`() {
        screen()

        compose.onNodeWithText("// RISULTATO").assertIsDisplayed()
        compose.onNodeWithText("dall'originale", substring = true).assertIsDisplayed()
    }

    @Test
    fun `jumping to IN seeks the host player`() {
        screen()

        // The label sits under the button rather than on it, so the tap goes
        // to the glyph inside the clickable circle.
        compose.onNodeWithText("⇤").performClick()

        verify { controller.seekTo(any<Long>()) }
    }

    @Test
    fun `jumping to OUT seeks the host player`() {
        screen()

        compose.onNodeWithText("⇥").performClick()

        verify { controller.seekTo(any<Long>()) }
    }

    @Test
    fun `the nudge buttons seek by five seconds`() {
        screen()

        compose.onNodeWithText("+5").performClick()

        verify { controller.seekTo(5_000L) }
    }

    /** Nudging back from the start clamps rather than seeking negative. */
    @Test
    fun `nudging back from the start stays at zero`() {
        screen()

        compose.onNodeWithText("−5").performClick()

        verify { controller.seekTo(0L) }
    }

    @Test
    fun `the fade pill toggles`() {
        screen()

        compose.onNodeWithText("Fade in/out").assertIsDisplayed()
        compose.onNodeWithText("Fade in/out").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Fade in/out").assertIsDisplayed()
    }

    @Test
    fun `snapping to silence is offered`() {
        screen()

        compose.onNodeWithText("Aggancia al silenzio").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("// IN").assertExists()
        compose.onNodeWithText("// OUT").assertExists()
    }

    /** The fine-nudge buttons are the only way to place a marker exactly. */
    @Test
    fun `nudging IN by a second moves the marker`() {
        screen()

        // Two identical nudge rows on screen; the first belongs to IN.
        compose.onAllNodesWithText("+1s")[0].performClick()
        compose.waitForIdle()

        compose.onNodeWithText("00:06.0").assertExists()
    }

    @Test
    fun `nudging IN by a tenth moves the marker`() {
        screen()

        compose.onAllNodesWithText("+.1")[0].performClick()
        compose.waitForIdle()

        compose.onNodeWithText("00:05.1").assertExists()
    }

    @Test
    fun `closing hands back to the caller`() {
        var closed = 0

        screen(onClose = { closed++ })
        compose.onNodeWithContentDescription("Chiudi").performClick()

        assertEquals(1, closed)
    }

    /**
     * A saved cut is a new song, and the question that follows is whether it
     * should take the original's place in the user's playlists.
     */
    @Test
    fun `saving offers to replace the original in playlists`() {
        val cut = song(99L, title = "Bohemian (cut)")
        coEvery { api.cutSong(any(), any()) } returns cut

        screen()
        compose.onNodeWithText("Salva").performClick()

        awaitText("Salvato come · Bohemian (cut)")
        compose.onNodeWithText("Versione locale · sostituirà l'originale nelle playlist?")
            .assertIsDisplayed()
    }

    @Test
    fun `declining the replacement finishes with the new song`() {
        val cut = song(99L, title = "Bohemian (cut)")
        coEvery { api.cutSong(any(), any()) } returns cut
        var saved: SongDto? = null

        screen(onSaved = { saved = it })
        compose.onNodeWithText("Salva").performClick()
        awaitText("Salvato come · Bohemian (cut)")
        compose.onNodeWithText("No").performClick()

        assertEquals(99L, saved?.id)
    }

    @Test
    fun `accepting the replacement swaps it across the playlists`() {
        val cut = song(99L, title = "Bohemian (cut)")
        coEvery { api.cutSong(any(), any()) } returns cut
        coEvery { api.replaceSongInPlaylists(any()) } returns
            com.mediaplayer.android.data.dto.ReplaceSongResponse(updated = 3)
        var saved: SongDto? = null

        screen(onSaved = { saved = it })
        compose.onNodeWithText("Salva").performClick()
        awaitText("Salvato come · Bohemian (cut)")
        compose.onNodeWithText("Sì").performClick()
        compose.waitForIdle()

        assertEquals(99L, saved?.id)
    }

    @Test
    fun `a failed save is reported on the editor rather than swallowed`() {
        coEvery { api.cutSong(any(), any()) } throws IOException("Unable to resolve host")
        var saved: SongDto? = null

        screen(onSaved = { saved = it })
        compose.onNodeWithText("Salva").performClick()

        awaitText("Salvataggio non riuscito")
        assertEquals(null, saved)
    }

    @Test
    fun `dismissing the error leaves the editor open to retry`() {
        coEvery { api.cutSong(any(), any()) } throws IOException("Unable to resolve host")

        screen()
        compose.onNodeWithText("Salva").performClick()
        awaitText("Salvataggio non riuscito")
        compose.onNodeWithText("Salvataggio non riuscito").performClick()
        compose.waitForIdle()

        compose.onAllNodesWithText("Salvataggio non riuscito").assertCountEquals(0)
        compose.onNodeWithText("Salva").assertIsDisplayed()
    }

    /**
     * A track shorter than the minimum window has nothing valid to cut, and
     * the editor says so locally instead of spending a round trip on it.
     */
    @Test
    fun `a track too short to cut is refused without a request`() {
        screen(song = song(2L, title = "Beep", durationMs = 500L))

        compose.onNodeWithText("Salva").performClick()

        awaitText("Salvataggio non riuscito")
        io.mockk.coVerify(exactly = 0) { api.cutSong(any(), any()) }
    }
}
