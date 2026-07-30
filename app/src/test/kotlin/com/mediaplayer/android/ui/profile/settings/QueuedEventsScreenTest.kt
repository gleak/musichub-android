package com.mediaplayer.android.ui.profile.settings

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.ui.ScreenTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * The queue diagnostic is what a user is pointed at when a like or a play
 * hasn't shown up server-side. It has to tell the truth about an empty
 * queue as clearly as a full one.
 */
class QueuedEventsScreenTest : ScreenTest() {

    @Before
    fun emptyTheQueue() {
        runBlocking { EventQueue.clear() }
    }

    private fun screen(onBack: () -> Unit = {}) {
        setScreen { QueuedEventsScreen(onBack = onBack) }
    }

    @Test
    fun `an empty queue says everything is synced`() {
        screen()

        awaitText("Tutto sincronizzato", substring = true)
    }

    @Test
    fun `a queued like shows up in the breakdown`() {
        runBlocking { EventQueue.enqueueLike(songId = 1L, displayLabel = "Breed") }

        screen()

        awaitText("Breed", substring = true)
    }

    @Test
    fun `several queued events are counted`() {
        runBlocking {
            EventQueue.enqueueLike(songId = 1L, displayLabel = "Breed")
            EventQueue.enqueueLike(songId = 2L, displayLabel = "Lithium")
        }

        screen()

        awaitText("Breed", substring = true)
        compose.onNodeWithText("Lithium", substring = true).assertExists()
    }

    @Test
    fun `back is wired`() {
        var backed = false

        screen(onBack = { backed = true })
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(true, backed)
    }
}
