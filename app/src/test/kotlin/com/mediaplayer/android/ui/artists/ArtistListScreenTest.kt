package com.mediaplayer.android.ui.artists

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.CatalogRepository
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.artist
import com.mediaplayer.android.ui.onePage
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class ArtistListScreenTest : ScreenTest() {

    private fun screen(onArtistClick: (String) -> Unit = {}, onBack: () -> Unit = {}) {
        val vm = ArtistListViewModel(CatalogRepository(api))
        setScreen {
            ArtistListScreen(onBack = onBack, onArtistClick = onArtistClick, viewModel = vm)
        }
    }

    @Test
    fun `artists render`() {
        coEvery { api.listArtists(any(), any(), any()) } returns
            onePage(listOf(artist("Nirvana"), artist("Radiohead")))

        screen()

        compose.onNodeWithText("Nirvana").assertIsDisplayed()
        compose.onNodeWithText("Radiohead").assertIsDisplayed()
    }

    @Test
    fun `an empty catalogue says so`() {
        coEvery { api.listArtists(any(), any(), any()) } returns onePage(emptyList<ArtistDto>())

        screen()

        compose.onNodeWithText("Nessun artista nel catalogo.").assertIsDisplayed()
    }

    @Test
    fun `a load failure offers a retry`() {
        coEvery { api.listArtists(any(), any(), any()) } throws IOException("offline")

        screen()

        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `tapping an artist opens it by name`() {
        coEvery { api.listArtists(any(), any(), any()) } returns onePage(listOf(artist("Queen")))
        var opened: String? = null

        screen(onArtistClick = { opened = it })
        compose.onNodeWithText("Queen").performClick()

        assertEquals("Queen", opened)
    }

    /** Same duplicate-key crash the album grid had; the list keys on name. */
    @Test
    fun `a name repeated across pages does not crash the list`() {
        // Multi-letter names: the alphabet scrubber down the side renders
        // single letters, and a one-letter artist would collide with it.
        coEvery { api.listArtists(any(), 0, any()) } returns
            PageResponse(listOf(artist("Alpha"), artist("Bravo")), 0, 2, 3L, 2)
        coEvery { api.listArtists(any(), 1, any()) } returns
            PageResponse(listOf(artist("Bravo"), artist("Charlie")), 1, 2, 3L, 2)

        screen()
        compose.waitForIdle()

        compose.onNodeWithText("Alpha").assertIsDisplayed()
        compose.onNodeWithText("Charlie").assertIsDisplayed()
        compose.onAllNodesWithText("Bravo").assertCountEquals(1)
    }
}
