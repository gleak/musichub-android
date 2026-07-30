package com.mediaplayer.android.ui.artists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.ArtistDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.album
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ArtistScreenTest : ScreenTest() {

    private fun screen(
        onPlayFromIndex: (List<SongDto>, Int) -> Unit = { _, _ -> },
        onShufflePlay: (List<SongDto>) -> Unit = {},
        onAlbumClick: (String, String) -> Unit = { _, _ -> },
        onBack: () -> Unit = {},
    ) {
        setScreen {
            ArtistScreen(
                artistName = "Nirvana",
                onBack = onBack,
                onPlayFromIndex = onPlayFromIndex,
                onShufflePlay = onShufflePlay,
                onAlbumClick = onAlbumClick,
            )
        }
    }

    private fun detail(
        albums: List<com.mediaplayer.android.data.dto.AlbumDto> = emptyList(),
        songs: List<SongDto> = emptyList(),
    ) = ArtistDetailDto(name = "Nirvana", albums = albums, songs = songs)

    @Test
    fun `the artist name and their tracks render`() {
        coEvery { api.getArtist(any()) } returns detail(songs = listOf(song(1L, title = "Breed")))

        screen()

        compose.onNodeWithText("Nirvana").assertIsDisplayed()
        compose.onNodeWithText("Breed").assertIsDisplayed()
    }

    @Test
    fun `the discography section lists albums`() {
        coEvery { api.getArtist(any()) } returns
            detail(albums = listOf(album("Nevermind", "Nirvana")), songs = listOf(song(1L)))

        screen()

        compose.onNodeWithText("Album").assertIsDisplayed()
        compose.onNodeWithText("Nevermind").assertIsDisplayed()
    }

    /** An artist with no albums shouldn't render an empty "Discografia" heading. */
    @Test
    fun `the discography section is absent when there are no albums`() {
        coEvery { api.getArtist(any()) } returns detail(songs = listOf(song(1L)))

        screen()

        compose.onNodeWithText("Discografia").assertIsNotDisplayed()
    }

    @Test
    fun `tapping an album carries the artist with it`() {
        coEvery { api.getArtist(any()) } returns
            detail(albums = listOf(album("Nevermind", "Nirvana")), songs = listOf(song(1L)))
        var opened: Pair<String, String>? = null

        screen(onAlbumClick = { name, artist -> opened = name to artist })
        compose.onNodeWithText("Nevermind").performClick()

        assertEquals("Nevermind" to "Nirvana", opened)
    }

    @Test
    fun `a load failure offers a retry`() {
        coEvery { api.getArtist(any()) } throws IOException("offline")

        screen()

        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `tapping a track plays from its position`() {
        coEvery { api.getArtist(any()) } returns
            detail(songs = listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))
        var index: Int? = null

        screen(onPlayFromIndex = { _, i -> index = i })
        compose.onNodeWithText("Lithium").performClick()

        assertEquals(1, index)
    }

    /** An artist page with no tracks must not start an empty queue. */
    @Test
    fun `an artist with no tracks does not start playback`() {
        coEvery { api.getArtist(any()) } returns detail()
        var played = false

        screen(onPlayFromIndex = { _, _ -> played = true }, onShufflePlay = { played = true })
        compose.onNodeWithContentDescription("Riproduci").performClick()
        compose.onNodeWithContentDescription("Casuale").performClick()

        assertEquals(false, played)
    }
}
