package com.mediaplayer.android.ui.liked

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.LikedRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.onePage
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Liked is the screen users open most, and the one with the most branches:
 * loading, error, empty, and a header whose play button is supposed to span
 * the whole collection rather than the pages that happen to be loaded.
 */
class LikedScreenTest : ScreenTest() {

    private fun screen(
        onPlayFromIndex: (List<SongDto>, Int) -> Unit = { _, _ -> },
        onShufflePlay: (List<SongDto>) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        val vm = LikedViewModel(LikedRepository(api))
        setScreen {
            LikedScreen(
                onBack = onBack,
                onPlayFromIndex = onPlayFromIndex,
                onShufflePlay = onShufflePlay,
                viewModel = vm,
            )
        }
    }

    @Test
    fun `liked songs render with their artists`() {
        coEvery { api.getLikedSongs(any(), any()) } returns
            onePage(listOf(song(1L, title = "Bohemian"), song(2L, title = "Innuendo")))

        screen()

        compose.onNodeWithText("Bohemian").assertIsDisplayed()
        compose.onNodeWithText("Innuendo").assertIsDisplayed()
    }

    @Test
    fun `the header counts what the server says, not what is loaded`() {
        // One page loaded out of a much larger collection: the count is
        // server-authoritative so the user isn't told they have 2 liked songs.
        coEvery { api.getLikedSongs(any(), any()) } returns
            onePage(listOf(song(1L), song(2L)), totalItems = 284L)

        screen()

        compose.onNodeWithText("284 brani", substring = true).assertIsDisplayed()
    }

    @Test
    fun `a single liked song is not pluralised`() {
        coEvery { api.getLikedSongs(any(), any()) } returns
            onePage(listOf(song(1L)), totalItems = 1L)

        screen()

        compose.onNodeWithText("1 brano", substring = true).assertIsDisplayed()
    }

    @Test
    fun `an empty collection explains itself instead of showing a blank list`() {
        coEvery { api.getLikedSongs(any(), any()) } returns onePage(emptyList<SongDto>())

        screen()

        compose.onNodeWithText("Nessun brano che ti piace").assertIsDisplayed()
    }

    @Test
    fun `a failed load offers a retry`() {
        coEvery { api.getLikedSongs(any(), any()) } throws IOException("offline")

        screen()

        compose.onNodeWithText("Couldn't load liked songs.", substring = true).assertIsDisplayed()
    }

    @Test
    fun `tapping a row plays from that row's position`() {
        coEvery { api.getLikedSongs(any(), any()) } returns
            onePage(listOf(song(1L, title = "First"), song(2L, title = "Second")))
        var playedFrom: Int? = null
        var playedList: List<SongDto>? = null

        screen(onPlayFromIndex = { list, index -> playedList = list; playedFrom = index })
        compose.onNodeWithText("Second").performClick()

        assertEquals(1, playedFrom)
        assertEquals(listOf(1L, 2L), playedList!!.map { it.id })
    }

    /**
     * The header's play button is documented to span the entire liked
     * collection, not the pages scrolled so far — otherwise shuffling a
     * 300-song library would only ever draw from the first 30.
     */
    @Test
    fun `playing from the header spans the whole collection`() {
        coEvery { api.getLikedSongs(any(), any()) } returns
            onePage(listOf(song(1L), song(2L)), totalItems = 100L)
        coEvery { api.getAllLikedSongs() } returns (1L..100L).map { song(it) }
        var played: List<SongDto>? = null

        screen(onPlayFromIndex = { list, _ -> played = list })
        compose.onNodeWithContentDescription("Riproduci").performClick()
        compose.waitForIdle()

        assertEquals(100, played!!.size)
    }

    /** If the full fetch fails the button still has to do something sensible. */
    @Test
    fun `a failed full fetch falls back to the loaded pages`() {
        coEvery { api.getLikedSongs(any(), any()) } returns
            onePage(listOf(song(1L), song(2L)), totalItems = 100L)
        coEvery { api.getAllLikedSongs() } throws IOException("offline")
        var played: List<SongDto>? = null

        screen(onPlayFromIndex = { list, _ -> played = list })
        compose.onNodeWithContentDescription("Riproduci").performClick()
        compose.waitForIdle()

        assertEquals(listOf(1L, 2L), played!!.map { it.id })
    }

    @Test
    fun `back is wired`() {
        coEvery { api.getLikedSongs(any(), any()) } returns onePage(listOf(song(1L)))
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(true, backed)
    }
}
