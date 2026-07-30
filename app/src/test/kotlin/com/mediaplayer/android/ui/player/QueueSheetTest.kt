package com.mediaplayer.android.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * The queue sheet shows what comes next, not what came before — Spotify's
 * split, and the thing users check when they've queued something and want
 * to know it landed.
 */
class QueueSheetTest : PlayerSheetTest() {

    private fun sheet(vararg queue: androidx.media3.common.MediaItem, currentIndex: Int = 0) {
        val vm = connectPlayer(queue.toList(), currentIndex = currentIndex)
        setScreen { QueueSheet(viewModel = vm, onDismiss = {}) }
        compose.waitForIdle()
    }

    @Test
    fun `the current track is labelled as playing`() {
        sheet(track("1", title = "Breed"), track("2", title = "Lithium"))

        compose.onNodeWithText("In riproduzione").assertIsDisplayed()
        compose.onNodeWithText("Breed").assertIsDisplayed()
    }

    @Test
    fun `upcoming tracks are listed`() {
        sheet(track("1", title = "Breed"), track("2", title = "Lithium"))

        compose.onNodeWithText("Lithium").assertIsDisplayed()
    }

    /**
     * Everything before the current item is hidden. Showing history in a
     * "what's next" list is what makes a queue unreadable.
     */
    @Test
    fun `already played tracks are not shown`() {
        sheet(
            track("1", title = "Played"),
            track("2", title = "Current"),
            track("3", title = "Upcoming"),
            currentIndex = 1,
        )

        compose.onNodeWithText("Current").assertIsDisplayed()
        compose.onNodeWithText("Upcoming").assertIsDisplayed()
        compose.onNodeWithText("Played").assertIsNotDisplayed()
    }

    @Test
    fun `an empty queue explains itself`() {
        sheet()

        compose.onNodeWithText("Coda vuota").assertIsDisplayed()
    }

    @Test
    fun `tapping an upcoming track jumps to it`() {
        sheet(track("1", title = "Breed"), track("2", title = "Lithium"))

        compose.onNodeWithText("Lithium").performClick()
        shadowMainLooper().idle()

        verify { controller.seekTo(1, 0L) }
    }
}
