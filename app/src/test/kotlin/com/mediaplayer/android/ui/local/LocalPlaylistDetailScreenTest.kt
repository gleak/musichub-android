package com.mediaplayer.android.ui.local

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.local.LocalTrack
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * An on-device playlist is just a list of MediaStore ids, so the screen has
 * to survive ids that no longer resolve (file deleted between scans) and it
 * has to preserve the user's ordering rather than the library's.
 *
 * Delete is the one destructive action here; it goes through a confirmation
 * and must leave the screen afterwards, because what it was showing no
 * longer exists.
 */
class LocalPlaylistDetailScreenTest : LocalScreenTest() {

    private fun screen(
        playlistId: String,
        onBack: () -> Unit = {},
        onPlay: (LocalTrack, List<LocalTrack>) -> Unit = { _, _ -> },
        onShuffle: (List<LocalTrack>) -> Unit = {},
        onPlayNext: (LocalTrack) -> Unit = {},
        onAddToQueue: (LocalTrack) -> Unit = {},
    ) {
        setScreen {
            LocalPlaylistDetailScreen(
                playlistId = playlistId,
                onBack = onBack,
                onPlay = onPlay,
                onShuffle = onShuffle,
                onPlayNext = onPlayNext,
                onAddToQueue = onAddToQueue,
            )
        }
    }

    private fun library() = publishTracks(
        trackRow(id = 1L, title = "Breed"),
        trackRow(id = 2L, title = "Lithium"),
        trackRow(id = 3L, title = "Polly"),
    )

    @Test
    fun `the playlist name and its tracks are shown`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L, 2L)) }

        screen(created.id)

        awaitText("Corsa")
        compose.onNodeWithText("Breed").assertIsDisplayed()
        compose.onNodeWithText("Lithium").assertIsDisplayed()
        compose.onNodeWithText("2 brani").assertIsDisplayed()
    }

    /** The user's ordering is the point of a playlist. */
    @Test
    fun `tracks keep the playlist order, not the library order`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(3L, 1L)) }

        screen(created.id)
        awaitText("Polly")

        val polly = compose.onNodeWithText("Polly").fetchSemanticsNode().positionInRoot.y
        val breed = compose.onNodeWithText("Breed").fetchSemanticsNode().positionInRoot.y
        assertTrue("Polly should sit above Breed", polly < breed)
    }

    /** A deleted or renamed playlist can still be navigated to from a back stack. */
    @Test
    fun `an unknown playlist says so instead of rendering blank`() {
        library()

        screen("no-such-id")

        awaitText("Playlist non trovata.")
    }

    @Test
    fun `an empty playlist explains how to fill it`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa") }

        screen(created.id)

        awaitText("Nessun brano qui")
    }

    /**
     * Ids whose file is gone simply drop out — the alternative is a row that
     * plays nothing.
     */
    @Test
    fun `ids that no longer resolve are dropped`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L, 999L)) }

        screen(created.id)

        awaitText("Breed")
        compose.onNodeWithText("1 brani").assertIsDisplayed()
    }

    @Test
    fun `play starts at the top of the playlist`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(2L, 1L)) }
        var played: Pair<LocalTrack, List<LocalTrack>>? = null

        screen(created.id, onPlay = { t, q -> played = t to q })
        awaitText("Lithium")
        compose.onNodeWithText("Riproduci").performClick()

        assertEquals("Lithium", played?.first?.title)
        assertEquals(2, played?.second?.size)
    }

    @Test
    fun `shuffle hands over the playlist`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L, 2L)) }
        var shuffled: List<LocalTrack>? = null

        screen(created.id, onShuffle = { shuffled = it })
        awaitText("Breed")
        compose.onNodeWithText("Casuale").performClick()

        assertEquals(2, shuffled?.size)
    }

    @Test
    fun `a row can be removed from the playlist`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L, 2L)) }

        screen(created.id)
        awaitText("Breed")
        // Index 0 is the playlist's own menu in the header; the rows follow.
        compose.onAllNodesWithContentDescription("Altre opzioni")[1].performClick()
        compose.onNodeWithText("Rimuovi dalla playlist").performClick()

        awaitPlaylists { it.single().trackIds == listOf(2L) }
    }

    @Test
    fun `the row menu can queue a track next`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }
        var next: LocalTrack? = null

        screen(created.id, onPlayNext = { next = it })
        awaitText("Breed")
        compose.onAllNodesWithContentDescription("Altre opzioni")[1].performClick()
        compose.onNodeWithText("Riproduci dopo").performClick()

        assertEquals(1L, next?.id)
    }

    @Test
    fun `renaming opens a dialog seeded with the current name`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }

        screen(created.id)
        awaitText("Corsa")
        compose.onAllNodesWithContentDescription("Altre opzioni")[0].performClick()
        openDialog("Rinomina")

        compose.onNodeWithText("Rinomina playlist").assertIsDisplayed()
    }

    /**
     * Deleting throws away something the user built by hand, so it asks
     * first — and the confirmation names the playlist.
     */
    @Test
    fun `deleting asks before it throws the playlist away`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }

        screen(created.id)
        awaitText("Corsa")
        compose.onAllNodesWithContentDescription("Altre opzioni")[0].performClick()
        compose.onNodeWithText("Elimina playlist").performClick()

        awaitText("Vuoi eliminare", substring = true)
        assertEquals(1, runBlocking { localPlaylists().snapshot() }.size)
    }

    @Test
    fun `confirming the delete removes it and leaves the screen`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }
        var backs = 0

        screen(created.id, onBack = { backs++ })
        awaitText("Corsa")
        compose.onAllNodesWithContentDescription("Altre opzioni")[0].performClick()
        compose.onNodeWithText("Elimina playlist").performClick()
        awaitText("Vuoi eliminare", substring = true)
        compose.onNodeWithText("Elimina").performClick()

        awaitPlaylists { it.isEmpty() }
        assertEquals(1, backs)
    }

    @Test
    fun `cancelling the delete keeps the playlist`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }
        var backs = 0

        screen(created.id, onBack = { backs++ })
        awaitText("Corsa")
        compose.onAllNodesWithContentDescription("Altre opzioni")[0].performClick()
        compose.onNodeWithText("Elimina playlist").performClick()
        awaitText("Vuoi eliminare", substring = true)
        compose.onNodeWithText("Annulla").performClick()

        assertEquals(1, runBlocking { localPlaylists().snapshot() }.size)
        assertEquals(0, backs)
    }

    @Test
    fun `back is wired to the caller`() {
        library()
        val created = runBlocking { localPlaylists().create("Corsa", listOf(1L)) }
        var backs = 0

        screen(created.id, onBack = { backs++ })
        awaitText("Corsa")
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(1, backs)
    }
}
