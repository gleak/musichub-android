package com.mediaplayer.android.ui.local

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.local.LocalTrack
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The drill-down used by both the folder and the album tiles. It filters
 * the same in-memory scan through a caller-supplied matcher, so the screen
 * itself has no idea whether it is showing a folder or an album — what it
 * must get right is the filtering, the queue it hands to playback, and the
 * "save as playlist" affordance that only folders pass in.
 */
class LocalFolderScreenTest : LocalScreenTest() {

    private fun screen(
        titlePrefix: String = "// CARTELLA",
        title: String = "Nirvana",
        matcher: (LocalTrack) -> Boolean = { it.folderName == "Nirvana" },
        onBack: () -> Unit = {},
        onPlay: (LocalTrack, List<LocalTrack>) -> Unit = { _, _ -> },
        onShuffle: (List<LocalTrack>) -> Unit = {},
        onPlayNext: (LocalTrack) -> Unit = {},
        onAddToQueue: (LocalTrack) -> Unit = {},
        onCreatePlaylist: ((String, List<LocalTrack>) -> Unit)? = null,
    ) {
        setScreen {
            LocalFolderOrAlbumScreen(
                titlePrefix = titlePrefix,
                title = title,
                matcher = matcher,
                onBack = onBack,
                onPlay = onPlay,
                onShuffle = onShuffle,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
                onCreatePlaylist = onCreatePlaylist,
            )
        }
    }

    @Test
    fun `only the tracks that match are listed`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Innuendo", folder = "Queen"),
        )

        screen()

        awaitText("Breed")
        compose.onAllNodesWithText("Innuendo").assertCountEquals(0)
    }

    @Test
    fun `the header names what is being shown`() {
        publishTracks(trackRow(folder = "Nirvana"))

        screen(title = "Nirvana")

        awaitText("Nirvana")
        compose.onNodeWithText("// CARTELLA").assertIsDisplayed()
    }

    @Test
    fun `the count reflects the filtered list, not the whole library`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Lithium", folder = "Nirvana"),
            trackRow(id = 3L, title = "Innuendo", folder = "Queen"),
        )

        screen()

        awaitText("2 brani")
    }

    /**
     * A matcher nobody satisfies happens whenever a folder is emptied
     * between two scans. It has to read as an empty folder, not a spinner
     * that never resolves.
     */
    @Test
    fun `a matcher that finds nothing says so`() {
        publishTracks(trackRow(folder = "Nirvana"))

        screen(matcher = { false })

        awaitText("Nessun brano qui.")
    }

    @Test
    fun `playing a row queues the filtered list, not the library`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Lithium", folder = "Nirvana"),
            trackRow(id = 3L, title = "Innuendo", folder = "Queen"),
        )
        var played: Pair<LocalTrack, List<LocalTrack>>? = null

        screen(onPlay = { t, queue -> played = t to queue })
        awaitText("Breed")
        compose.onNodeWithText("Breed").performClick()

        assertEquals("Breed", played?.first?.title)
        assertEquals(2, played?.second?.size)
    }

    @Test
    fun `shuffle hands over the filtered list`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Lithium", folder = "Nirvana"),
        )
        var shuffled: List<LocalTrack>? = null

        screen(onShuffle = { shuffled = it })
        awaitText("Breed")
        compose.onNodeWithText("Casuale").performClick()

        assertEquals(2, shuffled?.size)
    }

    @Test
    fun `the row menu can queue a track next or last`() {
        publishTracks(trackRow(id = 1L, title = "Breed", folder = "Nirvana"))
        var next: LocalTrack? = null
        var queued: LocalTrack? = null

        screen(onPlayNext = { next = it }, onAddToQueue = { queued = it })
        awaitText("Breed")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Riproduci dopo").performClick()
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Aggiungi alla coda").performClick()

        assertEquals(1L, next?.id)
        assertEquals(1L, queued?.id)
    }

    /**
     * Albums pass no callback, so the button must not be drawn — an album
     * has no folder name to seed a playlist with.
     */
    @Test
    fun `save as playlist is offered only when the caller supports it`() {
        publishTracks(trackRow(folder = "Nirvana"))

        screen(onCreatePlaylist = null)

        awaitText("Breed")
        compose.onAllNodesWithText("Salva come playlist").assertCountEquals(0)
    }

    @Test
    fun `save as playlist opens a naming dialog when supported`() {
        publishTracks(
            trackRow(id = 1L, title = "Breed", folder = "Nirvana"),
            trackRow(id = 2L, title = "Lithium", folder = "Nirvana"),
        )

        screen(onCreatePlaylist = { _, _ -> })
        awaitText("Breed")
        openDialog("Salva come playlist")

        compose.onNodeWithText("Crea una playlist locale con questi 2 brani.")
            .assertIsDisplayed()
    }

    @Test
    fun `back is wired to the caller`() {
        publishTracks(trackRow(folder = "Nirvana"))
        var backs = 0

        screen(onBack = { backs++ })
        awaitText("Breed")
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(1, backs)
    }
}
