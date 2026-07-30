package com.mediaplayer.android.ui.search

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mediaplayer.android.data.SongRepository
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.onePage
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Search debounces the query by 300ms and cancels in-flight requests on
 * every keystroke, so the assertions here wait for the result rather than
 * for Compose to go idle — the ViewModel's `delay` is invisible to
 * `waitForIdle`.
 *
 * An empty query deliberately stays idle and shows the genre browser
 * instead of listing the whole catalogue.
 */
class SearchScreenTest : ScreenTest() {

    private fun screen(
        onSongClick: (SongDto) -> Unit = {},
        onGenreOpen: (String, String) -> Unit = { _, _ -> },
        onProfileClick: () -> Unit = {},
    ) {
        setScreen {
            SearchScreen(
                viewModel = SearchViewModel(SongRepository(api)),
                onSongClick = onSongClick,
                onGenreOpen = onGenreOpen,
                onProfileClick = onProfileClick,
            )
        }
    }

    private fun searchFor(text: String) {
        compose.onNode(hasSetTextAction()).performTextInput(text)
    }

    @Test
    fun `an empty query browses genres instead of listing everything`() {
        screen()

        compose.onNodeWithText("Tutti i generi").assertIsDisplayed()
    }

    @Test
    fun `typing a query searches for it`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))

        screen()
        searchFor("nirvana")

        awaitText("Breed")
        compose.onNodeWithText("Lithium").assertIsDisplayed()
    }

    @Test
    fun `a search that matches nothing says so`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns onePage(emptyList<SongDto>())

        screen()
        searchFor("zzzz")

        awaitText("Nessun brano corrisponde", substring = true)
    }

    @Test
    fun `a failed search offers a retry`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } throws IOException("offline")

        screen()
        searchFor("nirvana")

        awaitText("Riprova")
    }

    @Test
    fun `tapping a result hands the song back`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            onePage(listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))
        var clicked: SongDto? = null

        screen(onSongClick = { clicked = it })
        searchFor("nirvana")
        awaitText("Lithium")
        compose.onNodeWithText("Lithium").performClick()

        assertEquals(2L, clicked?.id)
    }

    @Test
    fun `the profile shortcut is wired`() {
        var profiled = false

        screen(onProfileClick = { profiled = true })
        compose.onNodeWithContentDescription("Profilo").performClick()

        assertEquals(true, profiled)
    }
}
