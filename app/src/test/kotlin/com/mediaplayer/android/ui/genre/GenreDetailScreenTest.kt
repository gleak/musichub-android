package com.mediaplayer.android.ui.genre

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.onePage
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * A genre page is paged, and its play/shuffle buttons are meant to span
 * the whole genre rather than the pages scrolled — the same contract the
 * liked screen has.
 */
class GenreDetailScreenTest : ScreenTest() {

    private fun screen(
        onSongClick: (SongDto) -> Unit = {},
        onPlayAll: (List<SongDto>) -> Unit = {},
        onShufflePlay: (List<SongDto>) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        setScreen {
            GenreDetailScreen(
                tag = "rock",
                displayName = "Rock",
                onBack = onBack,
                onSongClick = onSongClick,
                onPlayAll = onPlayAll,
                onShufflePlay = onShufflePlay,
            )
        }
    }

    @Test
    fun `songs in the genre render`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L, title = "Enter Sandman"), song(2L, title = "Paranoid")))

        screen()

        compose.onNodeWithText("Enter Sandman").assertIsDisplayed()
        compose.onNodeWithText("Paranoid").assertIsDisplayed()
    }

    @Test
    fun `an empty genre names itself in the message`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns onePage(emptyList<SongDto>())

        screen()

        compose.onNodeWithText("Nessun brano in Rock.").assertIsDisplayed()
    }

    @Test
    fun `a load failure offers a retry`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } throws IOException("offline")

        screen()

        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `tapping a song hands that song back`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L, title = "Enter Sandman"), song(2L, title = "Paranoid")))
        var clicked: SongDto? = null

        screen(onSongClick = { clicked = it })
        compose.onNodeWithText("Paranoid").performClick()

        assertEquals(2L, clicked?.id)
    }

    /**
     * With pages left unloaded, "play all" fetches the genre in full first
     * — otherwise it would only ever play the first page.
     */
    @Test
    fun `play all spans the whole genre, not the loaded page`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L), song(2L)), totalItems = 80L)
        coEvery { api.getAllSongs(any(), any()) } returns (1L..80L).map { song(it) }
        var played: List<SongDto>? = null

        screen(onPlayAll = { played = it })
        compose.onNodeWithText("Riproduci tutti").performClick()
        compose.waitForIdle()

        assertEquals(80, played!!.size)
    }

    @Test
    fun `a failed full fetch falls back to what is loaded`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L), song(2L)), totalItems = 80L)
        coEvery { api.getAllSongs(any(), any()) } throws IOException("offline")
        var played: List<SongDto>? = null

        screen(onPlayAll = { played = it })
        compose.onNodeWithText("Riproduci tutti").performClick()
        compose.waitForIdle()

        assertEquals(listOf(1L, 2L), played!!.map { it.id })
    }

    @Test
    fun `shuffle spans the whole genre too`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L)), totalItems = 40L)
        coEvery { api.getAllSongs(any(), any()) } returns (1L..40L).map { song(it) }
        var shuffled: List<SongDto>? = null

        screen(onShufflePlay = { shuffled = it })
        compose.onNodeWithContentDescription("Casuale").performClick()
        compose.waitForIdle()

        assertEquals(40, shuffled!!.size)
    }

    /** The genre pill doubles as the way back to search. */
    @Test
    fun `clearing the genre pill goes back`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns onePage(listOf(song(1L)))
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Rimuovi filtro").performClick()

        assertEquals(true, backed)
    }
}
