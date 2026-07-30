package com.mediaplayer.android.ui.local

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The picker that "Aggiungi a playlist locale" opens. Two ways out — an
 * existing playlist or a new one — and both have to report the name back to
 * the caller, which is what the confirmation toast shows.
 */
class AddToLocalPlaylistSheetTest : LocalScreenTest() {

    private fun sheet(
        trackIds: List<Long> = listOf(7L),
        suggestedName: String? = null,
        onDismiss: () -> Unit = {},
        onAdded: (String) -> Unit = {},
    ) {
        setScreen {
            AddToLocalPlaylistSheet(
                trackIds = trackIds,
                onDismiss = onDismiss,
                onAdded = onAdded,
                suggestedName = suggestedName,
            )
        }
    }

    @Test
    fun `existing playlists are listed with their sizes`() {
        runBlocking {
            localPlaylists().create("Corsa", listOf(1L, 2L))
            localPlaylists().create("Studio")
        }

        sheet()

        awaitText("Corsa")
        compose.onNodeWithText("2 brani").assertIsDisplayed()
        compose.onNodeWithText("Studio").assertIsDisplayed()
        compose.onNodeWithText("0 brani").assertIsDisplayed()
    }

    @Test
    fun `picking a playlist adds the track and closes the sheet`() {
        runBlocking { localPlaylists().create("Corsa") }
        var added: String? = null
        var dismissed = 0

        sheet(trackIds = listOf(7L), onAdded = { added = it }, onDismiss = { dismissed++ })
        awaitText("Corsa")
        compose.onNodeWithText("Corsa").performClick()

        awaitPlaylists { it.single().trackIds == listOf(7L) }
        assertEquals("Corsa", added)
        assertEquals(1, dismissed)
    }

    /**
     * The store de-duplicates, so adding a track that is already there is a
     * no-op rather than a second row for the same file.
     */
    @Test
    fun `adding a track that is already in the playlist changes nothing`() {
        runBlocking { localPlaylists().create("Corsa", listOf(7L)) }

        sheet(trackIds = listOf(7L))
        awaitText("Corsa")
        compose.onNodeWithText("Corsa").performClick()

        awaitPlaylists { it.single().trackIds == listOf(7L) }
    }

    @Test
    fun `a whole folder can be added at once`() {
        runBlocking { localPlaylists().create("Corsa") }

        sheet(trackIds = listOf(1L, 2L, 3L))
        awaitText("Corsa")
        compose.onNodeWithText("Corsa").performClick()

        awaitPlaylists { it.single().trackIds == listOf(1L, 2L, 3L) }
    }

    /**
     * Coming from a folder, the new-playlist dialog opens pre-filled with
     * the folder's name so the user can accept it as-is.
     */
    @Test
    fun `the create dialog is seeded with the suggested name`() {
        sheet(suggestedName = "Nirvana")

        awaitText("Crea nuova playlist")
        openDialog("Crea nuova playlist")

        compose.onNodeWithText("Nuova playlist").assertIsDisplayed()
        compose.onNodeWithText("Nirvana").assertIsDisplayed()
    }

    @Test
    fun `accepting the suggested name creates the playlist with the tracks`() {
        var added: String? = null
        var dismissed = 0

        sheet(
            trackIds = listOf(4L, 5L),
            suggestedName = "Nirvana",
            onAdded = { added = it },
            onDismiss = { dismissed++ },
        )
        awaitText("Crea nuova playlist")
        openDialog("Crea nuova playlist")
        compose.onNodeWithText("Crea").performClick()
        pumpFrames()
        compose.mainClock.autoAdvance = true

        awaitPlaylists { it.singleOrNull()?.name == "Nirvana" }
        assertEquals(listOf(4L, 5L), runBlocking { localPlaylists().snapshot() }.single().trackIds)
        assertEquals("Nirvana", added)
        assertEquals(1, dismissed)
    }

    @Test
    fun `cancelling the create dialog leaves the sheet open and creates nothing`() {
        var dismissed = 0

        sheet(suggestedName = "Nirvana", onDismiss = { dismissed++ })
        awaitText("Crea nuova playlist")
        openDialog("Crea nuova playlist")
        compose.onNodeWithText("Annulla").performClick()
        pumpFrames()
        compose.mainClock.autoAdvance = true

        assertTrue(runBlocking { localPlaylists().snapshot() }.isEmpty())
        assertEquals(0, dismissed)
    }
}
