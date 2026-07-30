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
        onArtistClick: (String) -> Unit = {},
    ) {
        val vm = HomeViewModel()
        setScreen {
            HomeScreen(
                onSongClick = onSongClick,
                onPlaylistClick = onPlaylistClick,
                onArtistClick = onArtistClick,
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

    // ---------- the filter chips ----------

    /**
     * Home is four views over the same caches. Each filter has its own empty
     * state, because "no playlists" and "nothing played yet" call for
     * different things from the user.
     */
    @Test
    fun `the music filter lists recently played songs`() {
        stub(recents = listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))

        screen()
        awaitText("Musica")
        compose.onNodeWithText("Musica").performClick()

        awaitText("Brani recenti")
        compose.onAllNodesWithText("Breed").onFirst().assertIsDisplayed()
    }

    @Test
    fun `the music filter with nothing played says how to fill it`() {
        stub()

        screen()
        compose.onNodeWithText("Musica").performClick()

        awaitText("Nessun brano recente", substring = true)
    }

    @Test
    fun `the playlists filter separates the generated ones from your own`() {
        coEvery { api.recentSongs(any()) } returns emptyList()
        coEvery { api.listPlaylists(kind = "auto") } returns
            listOf(playlist(7L, name = "Discover Daily", kind = "auto"))
        coEvery { api.listPlaylists(kind = null) } returns listOf(playlist(1L, name = "Corsa"))

        screen()
        compose.onNodeWithText("Playlist").performClick()

        awaitText("Le tue playlist di oggi")
        compose.onAllNodesWithText("Discover Daily").onFirst().assertIsDisplayed()
        compose.onAllNodesWithText("Corsa").onFirst().assertIsDisplayed()
    }

    @Test
    fun `the playlists filter with none says so`() {
        stub()

        screen()
        compose.onNodeWithText("Playlist").performClick()

        awaitText("Nessuna playlist", substring = true)
    }

    @Test
    fun `tapping a playlist row reports it`() {
        coEvery { api.recentSongs(any()) } returns emptyList()
        coEvery { api.listPlaylists(kind = "auto") } returns emptyList()
        coEvery { api.listPlaylists(kind = null) } returns listOf(playlist(1L, name = "Corsa"))
        var opened: PlaylistDto? = null

        screen(onPlaylistClick = { opened = it })
        compose.onNodeWithText("Playlist").performClick()
        awaitText("Le tue playlist")
        compose.onAllNodesWithText("Corsa").onFirst().performClick()

        assertEquals(1L, opened?.id)
    }

    /** The artist list is derived from what has been played, not fetched. */
    @Test
    fun `the artists filter lists who you have been listening to`() {
        stub(recents = listOf(song(1L, artist = "Nirvana"), song(2L, artist = "Queen")))

        screen()
        compose.onNodeWithText("Artisti").performClick()

        awaitText("Artisti", substring = true)
        compose.onAllNodesWithText("Nirvana").onFirst().assertIsDisplayed()
    }

    @Test
    fun `the artists filter with nothing played says how to fill it`() {
        stub()

        screen()
        compose.onNodeWithText("Artisti").performClick()

        awaitText("Nessun artista", substring = true)
    }

    @Test
    fun `tapping an artist row reports the name`() {
        stub(recents = listOf(song(1L, artist = "Nirvana")))
        var opened: String? = null

        screen(onArtistClick = { opened = it })
        compose.onNodeWithText("Artisti").performClick()
        awaitText("Nirvana")
        compose.onAllNodesWithText("Nirvana").onFirst().performClick()

        assertEquals("Nirvana", opened)
    }

    @Test
    fun `the liked shortcut is offered under the music filter too`() {
        stub()

        screen()
        awaitText("Musica")
        compose.onNodeWithText("Musica").performClick()

        awaitText("Brani che mi piacciono")
    }
}
