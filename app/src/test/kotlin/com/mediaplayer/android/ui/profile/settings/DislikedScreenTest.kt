package com.mediaplayer.android.ui.profile.settings

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
 * The exclusion list is the only place a user can undo a "don't recommend
 * this". If it renders empty when it isn't, or silently swallows a load
 * failure, the exclusion becomes permanent from the user's point of view.
 */
class DislikedScreenTest : ScreenTest() {

    private fun screen(onBack: () -> Unit = {}) {
        setScreen { DislikedScreen(onBack = onBack) }
    }

    @Test
    fun `excluded songs render`() {
        coEvery { api.getDislikedSongs(any(), any()) } returns
            onePage(listOf(song(1L, title = "Breed")))
        coEvery { api.getDislikedArtists() } returns emptyList()

        screen()

        awaitText("Breed")
    }

    @Test
    fun `nothing excluded explains what the screen is for`() {
        coEvery { api.getDislikedSongs(any(), any()) } returns onePage(emptyList<SongDto>())
        coEvery { api.getDislikedArtists() } returns emptyList()

        screen()

        awaitText("Nessun brano escluso")
    }

    @Test
    fun `the artists tab lists excluded artists`() {
        coEvery { api.getDislikedSongs(any(), any()) } returns onePage(emptyList<SongDto>())
        coEvery { api.getDislikedArtists() } returns listOf("Nickelback")

        screen()
        awaitText("Artisti", substring = true)
        compose.onNodeWithText("Artisti", substring = true).performClick()

        awaitText("Nickelback")
    }

    @Test
    fun `an empty artists tab says so`() {
        coEvery { api.getDislikedSongs(any(), any()) } returns onePage(emptyList<SongDto>())
        coEvery { api.getDislikedArtists() } returns emptyList()

        screen()
        compose.onNodeWithText("Artisti", substring = true).performClick()

        awaitText("Nessun artista escluso")
    }

    /** A failed load must not read as "you've excluded nothing". */
    @Test
    fun `a load failure is surfaced`() {
        coEvery { api.getDislikedSongs(any(), any()) } throws IOException("offline")
        coEvery { api.getDislikedArtists() } returns emptyList()

        screen()

        awaitText("offline", substring = true)
    }

    @Test
    fun `restoring a song removes it from the list`() {
        coEvery { api.getDislikedSongs(any(), any()) } returns
            onePage(listOf(song(1L, title = "Breed")))
        coEvery { api.getDislikedArtists() } returns emptyList()

        screen()
        awaitText("Breed")
        compose.onNodeWithText("Ripristina").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Nessun brano escluso").assertIsDisplayed()
    }

    @Test
    fun `back is wired`() {
        coEvery { api.getDislikedSongs(any(), any()) } returns onePage(emptyList<SongDto>())
        coEvery { api.getDislikedArtists() } returns emptyList()
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(true, backed)
    }
}
