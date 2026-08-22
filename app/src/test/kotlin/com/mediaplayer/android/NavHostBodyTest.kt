package com.mediaplayer.android

import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.ui.player.PlayerSheetTest
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The signed-in navigation graph, driven directly rather than through the
 * auth gate. What lives here and nowhere else is the route table: the
 * patterns, the argument types, and the encoding of values that go into a
 * URL. An artist called "AC/DC" or a folder path with a slash in it are the
 * cases that quietly break a graph — the segment splits in two and the
 * destination either misses or arrives with half a name.
 */
@UnstableApi
class NavHostBodyTest : PlayerSheetTest() {

    private lateinit var navController: NavHostController
    private lateinit var playbackVm: PlaybackViewModel

    @Before
    fun stubCatalogue() {
        // The graph composes whichever destination it lands on, so every
        // screen behind a tested route needs something to render. Empty is
        // enough — this is about routing, not content.
        coEvery { api.recentSongs(any()) } returns emptyList()
        coEvery { api.listPlaylists(any()) } returns emptyList()
        coEvery { api.listAlbums(any(), any(), any()) } returns
            com.mediaplayer.android.ui.onePage(emptyList())
        coEvery { api.listArtists(any(), any()) } returns
            com.mediaplayer.android.ui.onePage(emptyList())
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            com.mediaplayer.android.ui.onePage(emptyList())
        coEvery { api.getLikedSongs(any(), any()) } returns
            com.mediaplayer.android.ui.onePage(emptyList())
        coEvery { djApi.status() } returns
            com.mediaplayer.android.data.dto.DjStatusDto()
        coEvery { djApi.chat(any()) } returns emptyList()
        coEvery { djApi.profile() } returns
            com.mediaplayer.android.data.dto.DjTasteProfileDto()
        coEvery { djApi.preferences() } returns
            com.mediaplayer.android.data.dto.DjPreferencesDto()
        coEvery { djApi.recentRuns() } returns emptyList()
    }

    private fun graph(onSignOut: () -> Unit = {}) {
        playbackVm = connectPlayer(emptyList())
        setScreen {
            navController = rememberNavController()
            NavHostBody(
                navController = navController,
                playbackVm = playbackVm,
                onSignOut = onSignOut,
                onShowChangelog = {},
                onCheckUpdates = {},
            )
        }
        compose.waitForIdle()
    }

    private fun navigateTo(route: String) {
        compose.runOnUiThread { navController.navigate(route) }
        compose.waitForIdle()
    }

    private val currentRoute: String?
        get() = navController.currentBackStackEntry?.destination?.route

    private fun arg(name: String): String? =
        navController.currentBackStackEntry?.arguments?.getString(name)

    /** Ids are typed as Long in the graph, so they don't come back as strings. */
    private fun longArg(name: String): Long? =
        navController.currentBackStackEntry?.arguments?.getLong(name)

    private fun boolArg(name: String): Boolean? =
        navController.currentBackStackEntry?.arguments?.getBoolean(name)

    @Test
    fun `the graph opens on home`() {
        graph()

        assertEquals(Routes.HOME, currentRoute)
    }

    @Test
    fun `the flat destinations are all reachable`() {
        graph()

        listOf(
            Routes.SEARCH,
            Routes.FOR_YOU,
            Routes.PROFILE,
            Routes.FIND,
            Routes.LIKED,
            Routes.ALBUM_LIST,
            Routes.ARTIST_LIST,
            Routes.LOCAL,
            Routes.LOCAL_LIKED,
        ).forEach { route ->
            navigateTo(route)
            assertEquals(route, currentRoute)
        }
    }

    @Test
    fun `the settings sub-screens are reachable`() {
        graph()

        listOf(
            Routes.SETTINGS_CROSSFADE,
            Routes.SETTINGS_DOWNLOAD,
            Routes.SETTINGS_THEME,
            Routes.SETTINGS_DISLIKED,
            Routes.SETTINGS_QUEUED_EVENTS,
        ).forEach { route ->
            navigateTo(route)
            assertEquals(route, currentRoute)
        }
    }

    @Test
    fun `a playlist id arrives as an argument`() {
        graph()

        navigateTo(Routes.playlistDetail(42L))

        assertEquals(Routes.PLAYLIST_DETAIL, currentRoute)
        assertEquals(42L, longArg("playlistId"))
    }

    /**
     * "AC/DC" is the canonical name that breaks a path segment. If the
     * encoding is wrong the route doesn't match at all, or matches with the
     * name cut at the slash.
     */
    @Test
    fun `an artist name containing a slash survives the round trip`() {
        graph()

        navigateTo(Routes.artistDetail("AC/DC"))

        assertEquals(Routes.ARTIST_DETAIL, currentRoute)
        assertEquals("AC/DC", arg("artistName"))
    }

    @Test
    fun `an album carries both its name and its artist`() {
        graph()

        navigateTo(Routes.albumDetail("Greatest Hits", "Queen"))

        assertEquals(Routes.ALBUM_DETAIL, currentRoute)
        assertEquals("Greatest Hits", arg("albumName"))
        assertEquals("Queen", arg("albumArtist"))
    }

    @Test
    fun `an album name with an ampersand keeps its query intact`() {
        graph()

        navigateTo(Routes.albumDetail("Hall & Oates", "Daryl Hall"))

        assertEquals("Hall & Oates", arg("albumName"))
        assertEquals("Daryl Hall", arg("albumArtist"))
    }

    @Test
    fun `a genre carries its tag and its display name separately`() {
        graph()

        navigateTo(Routes.genreDetail("indie-rock", "Indie Rock"))

        assertEquals(Routes.GENRE_DETAIL, currentRoute)
        assertEquals("indie-rock", arg("tag"))
        assertEquals("Indie Rock", arg("display"))
    }

    @Test
    fun `the members route remembers whether the caller owns the playlist`() {
        graph()

        navigateTo(Routes.playlistMembers(42L, owner = true))

        assertEquals(Routes.PLAYLIST_MEMBERS, currentRoute)
        assertEquals(42L, longArg("playlistId"))
        assertEquals(true, boolArg("owner"))
    }

    /** Folder paths are the other place a slash has to survive a segment. */
    @Test
    fun `a local folder path survives the round trip`() {
        graph()

        navigateTo(Routes.localFolder("Music/Nirvana"))

        assertEquals(Routes.LOCAL_FOLDER, currentRoute)
        assertEquals("Music/Nirvana", arg("path"))
    }

    @Test
    fun `a local album name survives the round trip`() {
        graph()

        navigateTo(Routes.localAlbum("Nevermind"))

        assertEquals(Routes.LOCAL_ALBUM, currentRoute)
        assertEquals("Nevermind", arg("name"))
    }

    @Test
    fun `a local playlist id survives the round trip`() {
        graph()

        navigateTo(Routes.localPlaylist("a-b-c"))

        assertEquals(Routes.LOCAL_PLAYLIST, currentRoute)
        assertEquals("a-b-c", arg("id"))
    }

    @Test
    fun `going back returns to the previous destination`() {
        graph()
        navigateTo(Routes.LIKED)

        compose.runOnUiThread { navController.popBackStack() }
        compose.waitForIdle()

        assertEquals(Routes.HOME, currentRoute)
    }

    @Test
    fun `the liked screen actually renders behind its route`() {
        graph()

        navigateTo(Routes.LIKED)

        compose.onNodeWithText("Brani che ti piacciono").assertIsDisplayed()
    }

    @Test
    fun `the DJ section is a destination of its own, not a child of the library`() {
        graph()

        navigateTo(Routes.DJ)

        assertEquals(Routes.DJ, currentRoute)
        // Se `dj` finisse in libraryPrefixes, aprire il DJ accenderebbe
        // "Libreria" nella barra in basso.
        org.junit.Assert.assertFalse(Routes.belongsToLibrary(currentRoute))
    }
}
