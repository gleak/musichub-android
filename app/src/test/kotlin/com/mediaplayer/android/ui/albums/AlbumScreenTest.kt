package com.mediaplayer.android.ui.albums

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.AlbumDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * The album detail screen builds its own ViewModel from the route
 * arguments, so these go through the shared `Network.api` override rather
 * than injecting a repository.
 */
class AlbumScreenTest : ScreenTest() {

    private fun screen(
        onPlayFromIndex: (List<SongDto>, Int) -> Unit = { _, _ -> },
        onShufflePlay: (List<SongDto>) -> Unit = {},
        onBack: () -> Unit = {},
        onArtistClick: (String) -> Unit = {},
    ) {
        setScreen {
            AlbumScreen(
                albumName = "Nevermind",
                albumArtist = "Nirvana",
                onBack = onBack,
                onPlayFromIndex = onPlayFromIndex,
                onShufflePlay = onShufflePlay,
                onArtistClick = onArtistClick,
            )
        }
    }

    private fun detail(vararg songs: SongDto) =
        AlbumDetailDto(name = "Nevermind", artist = "Nirvana", songs = songs.toList())

    @Test
    fun `the album header names the artist and counts the tracks`() {
        coEvery { api.getAlbum(any(), any()) } returns detail(song(1L), song(2L))

        screen()

        compose.onNodeWithText("Nevermind").assertIsDisplayed()
        compose.onNodeWithText("Album · Nirvana · 2 brani").assertIsDisplayed()
    }

    @Test
    fun `tracks render`() {
        coEvery { api.getAlbum(any(), any()) } returns
            detail(song(1L, title = "Breed"), song(2L, title = "Lithium"))

        screen()

        compose.onNodeWithText("Breed").assertIsDisplayed()
        compose.onNodeWithText("Lithium").assertIsDisplayed()
    }

    @Test
    fun `a load failure offers a retry`() {
        coEvery { api.getAlbum(any(), any()) } throws IOException("offline")

        screen()

        compose.onNodeWithText("Couldn't load album.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `tapping a track plays the album from that position`() {
        coEvery { api.getAlbum(any(), any()) } returns
            detail(song(1L, title = "Breed"), song(2L, title = "Lithium"))
        var index: Int? = null
        var queue: List<SongDto>? = null

        screen(onPlayFromIndex = { list, i -> queue = list; index = i })
        compose.onNodeWithText("Lithium").performClick()

        assertEquals(1, index)
        assertEquals(listOf(1L, 2L), queue!!.map { it.id })
    }

    @Test
    fun `the header play button starts from the top`() {
        coEvery { api.getAlbum(any(), any()) } returns detail(song(1L), song(2L))
        var index: Int? = null

        screen(onPlayFromIndex = { _, i -> index = i })
        compose.onNodeWithContentDescription("Riproduci").performClick()

        assertEquals(0, index)
    }

    /**
     * Shuffle has to go through the app-level shuffle rather than a one-off
     * shuffled list, so the toggle in the player reflects what's happening.
     * The screen hands the queue over untouched and lets the service order it.
     */
    @Test
    fun `shuffle hands over the album in its natural order`() {
        coEvery { api.getAlbum(any(), any()) } returns detail(song(1L), song(2L), song(3L))
        var shuffled: List<SongDto>? = null

        screen(onShufflePlay = { shuffled = it })
        compose.onNodeWithContentDescription("Casuale").performClick()

        assertEquals(listOf(1L, 2L, 3L), shuffled!!.map { it.id })
    }

    /** An album with no playable tracks must not start an empty queue. */
    @Test
    fun `an empty album does not start playback`() {
        coEvery { api.getAlbum(any(), any()) } returns detail()
        var played = false

        screen(onPlayFromIndex = { _, _ -> played = true }, onShufflePlay = { played = true })
        compose.onNodeWithContentDescription("Riproduci").performClick()
        compose.onNodeWithContentDescription("Casuale").performClick()

        assertEquals(false, played)
    }

    @Test
    fun `back is wired`() {
        coEvery { api.getAlbum(any(), any()) } returns detail(song(1L))
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(true, backed)
    }
}
