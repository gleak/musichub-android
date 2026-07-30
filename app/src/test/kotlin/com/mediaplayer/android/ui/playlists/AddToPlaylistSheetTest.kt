package com.mediaplayer.android.ui.playlists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.playlist
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The track kebab. It doubles as the add-to-playlist picker, and it has to
 * behave differently for a local file: a song stored on the phone has a
 * negative id the backend has never heard of, so the backend-only actions
 * would fail on an id that doesn't exist.
 */
class AddToPlaylistSheetTest : ScreenTest() {

    private fun stubPlaylists(vararg user: PlaylistDto) {
        coEvery { api.listPlaylists(kind = "auto") } returns emptyList()
        coEvery { api.listPlaylists(kind = null) } returns user.toList()
    }

    private fun sheet(
        songId: Long = 42L,
        songTitle: String = "Breed",
        allowBackendActions: Boolean = true,
        onPlayNext: (() -> Unit)? = {},
        onAddToQueue: (() -> Unit)? = {},
        onDismiss: () -> Unit = {},
        onAdded: (String) -> Unit = {},
    ) {
        setScreen {
            AddToPlaylistSheet(
                songTitle = songTitle,
                songId = songId,
                songArtist = "Nirvana",
                allowBackendActions = allowBackendActions,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onDismiss = onDismiss,
                onAdded = onAdded,
            )
        }
        compose.waitForIdle()
    }

    @Test
    fun `the song being acted on is named`() {
        stubPlaylists()

        sheet(songTitle = "Breed")

        compose.onNodeWithText("Breed").assertIsDisplayed()
    }

    @Test
    fun `playlists are offered as targets`() {
        stubPlaylists(playlist(1L, name = "Road Trip"))

        sheet()

        awaitText("Road Trip")
    }

    @Test
    fun `an account with no playlists is invited to create one`() {
        stubPlaylists()

        sheet()

        awaitText("Nessuna playlist")
    }

    @Test
    fun `creating a new playlist is always offered`() {
        stubPlaylists(playlist(1L, name = "Road Trip"))

        sheet()

        awaitText("Crea nuova playlist")
    }

    /**
     * Local files carry a negated MediaStore id the backend has never seen.
     * Offering to add one to a server-side playlist would post an id that
     * doesn't exist.
     */
    @Test
    fun `a local track is not offered server-side playlists`() {
        stubPlaylists(playlist(1L, name = "Road Trip"))

        sheet(songId = -17L, allowBackendActions = false)

        // The sheet title stays ("Le mie playlist" is the header); what must
        // not appear is a playlist row the track can't actually be added to.
        compose.onNodeWithText("Road Trip").assertIsNotDisplayed()
        compose.onNodeWithText("Crea nuova playlist").assertIsNotDisplayed()
    }

    @Test
    fun `queue actions are offered when the caller supports them`() {
        stubPlaylists()
        var queued = false

        sheet(onAddToQueue = { queued = true })
        compose.onNodeWithText("Aggiungi alla coda").performClick()

        assertEquals(true, queued)
    }

    /** A caller that can't queue must not show a button that does nothing. */
    @Test
    fun `queue actions are absent when the caller does not support them`() {
        stubPlaylists()

        sheet(onPlayNext = null, onAddToQueue = null)

        compose.onNodeWithText("Aggiungi alla coda").assertIsNotDisplayed()
    }
}
