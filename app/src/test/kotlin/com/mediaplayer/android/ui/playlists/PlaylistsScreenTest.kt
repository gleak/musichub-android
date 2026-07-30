package com.mediaplayer.android.ui.playlists

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
 * The library landing. Its state comes from the shared PlaylistsCache
 * rather than a per-screen fetch, so a create or delete performed anywhere
 * else in the app shows up here without a refetch.
 */
class PlaylistsScreenTest : ScreenTest() {

    /**
     * The repository asks for auto playlists and user playlists in two
     * separate calls, so a single catch-all stub would hand the same rows
     * back twice.
     */
    private fun stubPlaylists(vararg user: PlaylistDto) {
        coEvery { api.listPlaylists(kind = "auto") } returns emptyList()
        coEvery { api.listPlaylists(kind = null) } returns user.toList()
    }

    private fun screen(
        onPlaylistClick: (PlaylistDto) -> Unit = {},
        onLikedSongsClick: () -> Unit = {},
        onProfileClick: () -> Unit = {},
        onLocalLibraryClick: () -> Unit = {},
    ) {
        val vm = PlaylistsViewModel()
        setScreen {
            PlaylistsScreen(
                viewModel = vm,
                onPlaylistClick = onPlaylistClick,
                onLikedSongsClick = onLikedSongsClick,
                onProfileClick = onProfileClick,
                onLocalLibraryClick = onLocalLibraryClick,
            )
        }
    }

    @Test
    fun `playlists render`() {
        stubPlaylists(playlist(1L, name = "Road Trip"), playlist(2L, name = "Focus"))

        screen()

        awaitText("Road Trip")
        compose.onNodeWithText("Focus").assertIsDisplayed()
    }

    @Test
    fun `an account with no playlists is told how to start`() {
        stubPlaylists()

        screen()

        awaitText("No playlists yet")
    }

    /**
     * Going offline falls back to the last cached list rather than erroring.
     * With nothing cached that means the empty state — worth knowing, because
     * a first run with no network reads as "you have no playlists" rather
     * than "you're offline".
     */
    @Test
    fun `offline with an empty cache shows the empty state, not an error`() {
        coEvery { api.listPlaylists(any()) } throws IOException("offline")

        screen()

        awaitText("No playlists yet")
    }

    @Test
    fun `a non-network failure surfaces as an error with a retry`() {
        coEvery { api.listPlaylists(any()) } throws IllegalStateException("boom")

        screen()

        awaitText("Riprova")
    }

    @Test
    fun `tapping a playlist hands the record back`() {
        stubPlaylists(playlist(9L, name = "Road Trip"))
        var opened: PlaylistDto? = null

        screen(onPlaylistClick = { opened = it })
        awaitText("Road Trip")
        compose.onNodeWithText("Road Trip").performClick()

        assertEquals(9L, opened?.id)
    }

    /** Liked songs are a fixed entry above the list, not a playlist row. */
    @Test
    fun `the liked songs shortcut is wired`() {
        stubPlaylists()
        var liked = false

        screen(onLikedSongsClick = { liked = true })
        compose.onNodeWithText("Brani preferiti").performClick()

        assertEquals(true, liked)
    }

    @Test
    fun `the on-device library shortcut is wired`() {
        stubPlaylists()
        var local = false

        screen(onLocalLibraryClick = { local = true })
        compose.onNodeWithText("Sul tuo dispositivo").performClick()

        assertEquals(true, local)
    }

    @Test
    fun `the profile shortcut is wired`() {
        stubPlaylists()
        var profiled = false

        screen(onProfileClick = { profiled = true })
        compose.onNodeWithContentDescription("Profilo").performClick()

        assertEquals(true, profiled)
    }
}
