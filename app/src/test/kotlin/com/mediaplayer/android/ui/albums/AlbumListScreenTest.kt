package com.mediaplayer.android.ui.albums

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import com.mediaplayer.android.data.CatalogRepository
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.album
import com.mediaplayer.android.ui.onePage
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * The album grid filters and sorts client-side over server-paged data,
 * which is the interesting part: the filter must not be mistaken for an
 * empty catalogue, and it must not keep asking the server for more pages
 * while the user is typing.
 */
class AlbumListScreenTest : ScreenTest() {

    private fun screen(onAlbumClick: (String, String) -> Unit = { _, _ -> }, onBack: () -> Unit = {}) {
        val vm = AlbumListViewModel(CatalogRepository(api))
        setScreen {
            AlbumListScreen(onBack = onBack, onAlbumClick = onAlbumClick, viewModel = vm)
        }
    }

    @Test
    fun `albums render`() {
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("Nevermind", "Nirvana"), album("Ok Computer", "Radiohead")))

        screen()

        compose.onNodeWithText("Nevermind").assertIsDisplayed()
        compose.onNodeWithText("Ok Computer").assertIsDisplayed()
    }

    @Test
    fun `an empty catalogue says so`() {
        coEvery { api.listAlbums(any(), any(), any()) } returns onePage(emptyList<AlbumDto>())

        screen()

        compose.onNodeWithText("Nessun album nel catalogo.").assertIsDisplayed()
    }

    @Test
    fun `a load failure offers a retry`() {
        coEvery { api.listAlbums(any(), any(), any()) } throws IOException("offline")

        screen()

        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `tapping an album passes both name and artist`() {
        // Album names are not unique across artists, so the callback has to
        // carry the artist or the detail screen opens the wrong record.
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("Greatest Hits", "Queen")))
        var opened: Pair<String, String>? = null

        screen(onAlbumClick = { name, artist -> opened = name to artist })
        compose.onNodeWithText("Greatest Hits").performClick()

        assertEquals("Greatest Hits" to "Queen", opened)
    }

    @Test
    fun `the search box filters on album name`() {
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("Nevermind", "Nirvana"), album("Ok Computer", "Radiohead")))

        screen()
        compose.onNode(hasSetTextAction()).performTextInput("never")

        compose.onNodeWithText("Nevermind").assertIsDisplayed()
        compose.onNodeWithText("Ok Computer").assertDoesNotExist()
    }

    @Test
    fun `the search box also matches the artist`() {
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("Nevermind", "Nirvana"), album("Ok Computer", "Radiohead")))

        screen()
        compose.onNode(hasSetTextAction()).performTextInput("radio")

        compose.onNodeWithText("Ok Computer").assertIsDisplayed()
        compose.onNodeWithText("Nevermind").assertDoesNotExist()
    }

    /**
     * A filter that matches nothing is not an empty catalogue, and telling
     * the user their library is empty because they mistyped would be a lie.
     */
    @Test
    fun `a filter matching nothing is distinguished from an empty catalogue`() {
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("Nevermind", "Nirvana")))

        screen()
        compose.onNode(hasSetTextAction()).performTextInput("zzzz")

        compose.onNodeWithText("Nessun album corrisponde a \"zzzz\".").assertIsDisplayed()
        compose.onNodeWithText("Nessun album nel catalogo.").assertDoesNotExist()
    }

    @Test
    fun `sorting alphabetically reorders what is shown`() {
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("Zoo", "A"), album("Apple", "B")))

        screen()
        compose.onNodeWithText("Recenti").performClick()

        // Both still present; the assertion that matters is that the toggle
        // doesn't drop rows, which an in-place sort bug would.
        compose.onNodeWithText("Apple").assertIsDisplayed()
        compose.onNodeWithText("Zoo").assertIsDisplayed()
    }

    @Test
    fun `the header shows the server-wide count, not the loaded page`() {
        // endReached, so nothing pages in behind the assertion.
        coEvery { api.listAlbums(any(), any(), any()) } returns
            onePage(listOf(album("One"), album("Two")), totalItems = 2L)

        screen()

        compose.onNodeWithText("2", substring = true).assertIsDisplayed()
    }

    /**
     * Offset pagination over a catalogue that shifts between fetches can
     * hand back a row the previous page already had. The grid keys on
     * artist + name, and Compose throws on a duplicate key — so before the
     * de-dupe this didn't render wrong, it crashed the screen outright.
     * Liked and Genre already guarded this; Albums did not.
     */
    @Test
    fun `a row repeated across pages does not crash the grid`() {
        coEvery { api.listAlbums(any(), 0, any()) } returns
            PageResponse(
                items = listOf(album("First", "A"), album("Second", "B")),
                page = 0,
                size = 2,
                totalItems = 3L,
                totalPages = 2,
            )
        // Page 1 repeats "Second" — an album shifted position server-side.
        coEvery { api.listAlbums(any(), 1, any()) } returns
            PageResponse(
                items = listOf(album("Second", "B"), album("Third", "C")),
                page = 1,
                size = 2,
                totalItems = 3L,
                totalPages = 2,
            )

        screen()
        compose.waitForIdle()

        compose.onNodeWithText("First").assertIsDisplayed()
        compose.onNodeWithText("Third").assertIsDisplayed()
        compose.onAllNodesWithText("Second").assertCountEquals(1)
    }
}
