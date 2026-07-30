package com.mediaplayer.android.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.playlist
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Home is a combiner over the shared caches rather than a fetcher of its
 * own, so what it renders is a question of what's in RecentsCache and
 * PlaylistsCache — including the cold-start case where both are empty and
 * the screen has to offer a way out instead of showing nothing.
 */
class HomeScreenTest : ScreenTest() {

    private fun stub(recents: List<SongDto> = emptyList(), playlists: List<PlaylistDto> = emptyList()) {
        coEvery { api.recentSongs(any()) } returns recents
        coEvery { api.listPlaylists(kind = "auto") } returns emptyList()
        coEvery { api.listPlaylists(kind = null) } returns playlists
    }

    private fun screen(
        onSongClick: (SongDto) -> Unit = {},
        onPlaylistClick: (PlaylistDto) -> Unit = {},
        onLikedClick: () -> Unit = {},
        onFindClick: () -> Unit = {},
        onSpotifyImport: () -> Unit = {},
        onLocalLibraryClick: () -> Unit = {},
    ) {
        val vm = HomeViewModel()
        setScreen {
            HomeScreen(
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onLikedClick = onLikedClick,
                onFindClick = onFindClick,
                onSpotifyImport = onSpotifyImport,
                onLocalLibraryClick = onLocalLibraryClick,
                viewModel = vm,
            )
        }
    }

    @Test
    fun `recently played songs render`() {
        stub(recents = listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))

        screen()

        awaitText("Breed")
        // Home surfaces recents in more than one section, so assert the
        // song is present rather than that it appears exactly once.
        compose.onAllNodesWithText("Lithium").onFirst().assertIsDisplayed()
    }

    @Test
    fun `playlists render`() {
        stub(playlists = listOf(playlist(1L, name = "Road Trip")))

        screen()

        awaitText("Road Trip")
    }

    /**
     * A brand-new account has no recents and no playlists. Showing an empty
     * feed would leave the user with nowhere to go, so Home offers the two
     * ways to fill a library instead.
     */
    @Test
    fun `a cold start offers a way to fill the library`() {
        stub()

        screen()

        awaitText("Iniziamo")
        compose.onNodeWithText("Scopri musica").assertIsDisplayed()
        compose.onNodeWithText("Importa Spotify").assertIsDisplayed()
    }

    @Test
    fun `the cold start find shortcut is wired`() {
        stub()
        var found = false

        screen(onFindClick = { found = true })
        awaitText("Scopri musica")
        compose.onAllNodesWithText("Scopri musica").onFirst().performClick()

        assertEquals(true, found)
    }

    @Test
    fun `the cold start import shortcut is wired`() {
        stub()
        var imported = false

        screen(onSpotifyImport = { imported = true })
        awaitText("Importa Spotify")
        compose.onAllNodesWithText("Importa Spotify").onFirst().performClick()

        assertEquals(true, imported)
    }

    @Test
    fun `tapping a recent song hands it back`() {
        stub(recents = listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))
        var clicked: SongDto? = null

        screen(onSongClick = { clicked = it })
        awaitText("Lithium")
        compose.onAllNodesWithText("Lithium").onFirst().performClick()

        assertEquals(2L, clicked?.id)
    }

    @Test
    fun `tapping a playlist hands the record back`() {
        stub(playlists = listOf(playlist(4L, name = "Road Trip")))
        var opened: PlaylistDto? = null

        screen(onPlaylistClick = { opened = it })
        awaitText("Road Trip")
        compose.onAllNodesWithText("Road Trip").onFirst().performClick()

        assertEquals(4L, opened?.id)
    }

    @Test
    fun `the liked songs tile is wired`() {
        stub(recents = listOf(song(1L)))
        var liked = false

        screen(onLikedClick = { liked = true })
        awaitText("Brani che mi piacciono")
        compose.onAllNodesWithText("Brani che mi piacciono").onFirst().performClick()

        assertEquals(true, liked)
    }
}
