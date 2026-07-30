package com.mediaplayer.android.ui.local

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.local.LocalTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * On-device likes live in their own store, keyed by MediaStore id, because
 * the backend's liked table only knows about catalogue songs. The screen is
 * the join between that id set and the current scan — an id whose file is
 * gone must not produce a row, and unliking has to empty the screen it was
 * shown on.
 */
class LocalLikedScreenTest : LocalScreenTest() {

    private fun screen(
        onBack: () -> Unit = {},
        onPlay: (LocalTrack, List<LocalTrack>) -> Unit = { _, _ -> },
        onShuffle: (List<LocalTrack>) -> Unit = {},
        onPlayNext: (LocalTrack) -> Unit = {},
        onAddToQueue: (LocalTrack) -> Unit = {},
    ) {
        setScreen {
            LocalLikedScreen(
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
    fun `only liked tracks are listed`() {
        library()
        runBlocking { localLikes().setLiked(2L, true) }

        screen()

        awaitText("Lithium")
        compose.onAllNodesWithText("Breed").assertCountEquals(0)
        compose.onNodeWithText("1 brani").assertIsDisplayed()
    }

    @Test
    fun `nothing liked yet reads as an invitation, not a failure`() {
        library()

        screen()

        awaitText("Nessun brano locale tra i preferiti")
        compose.onNodeWithText(
            "Tocca il cuore su un brano del dispositivo per aggiungerlo qui.",
        ).assertIsDisplayed()
    }

    /**
     * A like outlives the file it points at — the store keeps the id even
     * after the track is deleted from the phone.
     */
    @Test
    fun `a like whose file is gone produces no row`() {
        library()
        runBlocking { localLikes().setLiked(999L, true) }

        screen()

        awaitText("Nessun brano locale tra i preferiti")
    }

    @Test
    fun `liked tracks are listed alphabetically`() {
        library()
        runBlocking {
            localLikes().setLiked(3L, true)
            localLikes().setLiked(1L, true)
        }

        screen()
        awaitText("Breed")

        val breed = compose.onNodeWithText("Breed").fetchSemanticsNode().positionInRoot.y
        val polly = compose.onNodeWithText("Polly").fetchSemanticsNode().positionInRoot.y
        assertTrue("Breed should sit above Polly", breed < polly)
    }

    @Test
    fun `playing a row queues the liked list`() {
        library()
        runBlocking {
            localLikes().setLiked(1L, true)
            localLikes().setLiked(2L, true)
        }
        var played: Pair<LocalTrack, List<LocalTrack>>? = null

        screen(onPlay = { t, q -> played = t to q })
        awaitText("Breed")
        compose.onNodeWithText("Breed").performClick()

        assertEquals("Breed", played?.first?.title)
        assertEquals(2, played?.second?.size)
    }

    @Test
    fun `shuffle hands over the liked list`() {
        library()
        runBlocking { localLikes().setLiked(1L, true) }
        var shuffled: List<LocalTrack>? = null

        screen(onShuffle = { shuffled = it })
        awaitText("Breed")
        compose.onNodeWithText("Riproduzione casuale").performClick()

        assertEquals(1, shuffled?.size)
    }

    @Test
    fun `unliking from the row menu empties the screen`() {
        library()
        runBlocking { localLikes().setLiked(1L, true) }

        screen()
        awaitText("Breed")
        compose.onNodeWithContentDescription("Altre opzioni").performClick()
        compose.onNodeWithText("Togli dai preferiti").performClick()

        awaitText("Nessun brano locale tra i preferiti")
        assertTrue(runBlocking { localLikes().liked.first() }.isEmpty())
    }

    @Test
    fun `the row menu can queue a track next or last`() {
        library()
        runBlocking { localLikes().setLiked(1L, true) }
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

    @Test
    fun `back is wired to the caller`() {
        library()
        runBlocking { localLikes().setLiked(1L, true) }
        var backs = 0

        screen(onBack = { backs++ })
        awaitText("Breed")
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(1, backs)
    }
}
