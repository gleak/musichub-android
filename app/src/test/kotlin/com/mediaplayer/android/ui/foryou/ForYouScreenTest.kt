package com.mediaplayer.android.ui.foryou

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.playlist
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * "Per te" is entirely server-generated: if the auto-playlist call fails
 * the screen has nothing of its own to fall back on, so the failure has to
 * be visible rather than an empty page that looks like a bare account.
 */
class ForYouScreenTest : ScreenTest() {

    private fun screen(onPlaylistClick: (PlaylistDto) -> Unit = {}, onProfileClick: () -> Unit = {}) {
        setScreen {
            ForYouScreen(
                onPlaylistClick = onPlaylistClick,
                onProfileClick = onProfileClick,
                viewModel = ForYouViewModel(),
            )
        }
    }

    @Test
    fun `auto playlists render`() {
        coEvery { api.listPlaylists(kind = "auto") } returns
            listOf(playlist(1L, name = "Discover Daily", kind = "DISCOVER"))

        screen()

        compose.onNodeWithText("Discover Daily").assertIsDisplayed()
    }

    /** Context rows carry their length; the "in poi" family renders as a row. */
    @Test
    fun `the song count is shown on a context row`() {
        coEvery { api.listPlaylists(kind = "auto") } returns
            listOf(playlist(1L, name = "In poi", songCount = 42, kind = "NEXT"))

        screen()

        compose.onNodeWithText("42 brani").assertIsDisplayed()
    }

    /**
     * An unrecognised kind falls back to the daily-mix family rather than
     * vanishing — a new server-side playlist type must still show up.
     */
    @Test
    fun `an unknown playlist kind still renders`() {
        coEvery { api.listPlaylists(kind = "auto") } returns
            listOf(playlist(1L, name = "Brand New Thing", kind = "SOMETHING_NEW"))

        screen()

        compose.onNodeWithText("Brand New Thing").assertIsDisplayed()
    }

    @Test
    fun `a failure is surfaced rather than shown as an empty feed`() {
        coEvery { api.listPlaylists(kind = "auto") } throws IOException("offline")

        screen()

        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `tapping a playlist hands the whole record to the caller`() {
        val target = playlist(7L, name = "Time Capsule", kind = "TIME_CAPSULE")
        coEvery { api.listPlaylists(kind = "auto") } returns listOf(target)
        var opened: PlaylistDto? = null

        screen(onPlaylistClick = { opened = it })
        compose.onNodeWithText("Time Capsule").performClick()

        assertEquals(7L, opened?.id)
    }

    @Test
    fun `the profile shortcut is wired`() {
        coEvery { api.listPlaylists(kind = "auto") } returns emptyList()
        var profiled = false

        screen(onProfileClick = { profiled = true })
        compose.onNodeWithContentDescription("Profilo").performClick()

        assertEquals(true, profiled)
    }
}