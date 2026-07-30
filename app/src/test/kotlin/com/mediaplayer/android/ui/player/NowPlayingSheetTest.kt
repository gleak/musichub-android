package com.mediaplayer.android.ui.player

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.playback.PlayerConnection
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * The player sheet is the only screen whose entire content comes from a
 * [MediaController]. A stand-in controller is published through
 * [PlayerConnection], the real view model adopts it, and the sheet renders
 * off that — so the transport buttons are checked against what they
 * actually ask the controller to do.
 */
class NowPlayingSheetTest : ScreenTest() {

    private lateinit var controller: MediaController
    private lateinit var store: ViewModelStore
    private var shuffleForcedOff = false

    @After
    fun disconnectController() {
        PlayerConnection.publishForTest(null)
        if (::store.isInitialized) store.clear()
        shadowMainLooper().idle()
    }

    private fun item(id: String, title: String, artist: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri("https://test.invalid/$id.mp3")
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(title).setArtist(artist).build(),
            )
            .build()

    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowMainLooper().idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("condition still false after ${timeoutMs}ms")
    }

    private fun sheet(
        title: String = "Breed",
        artist: String = "Nirvana",
        playing: Boolean = false,
        onDismiss: () -> Unit = {},
    ) {
        controller = mockk(relaxed = true)
        shuffleForcedOff = false
        val track = item("42", title, artist)
        every { controller.shuffleModeEnabled = false } answers { shuffleForcedOff = true }
        every { controller.mediaItemCount } returns 1
        every { controller.getMediaItemAt(0) } returns track
        every { controller.currentMediaItemIndex } returns 0
        every { controller.currentMediaItem } returns track
        every { controller.isPlaying } returns playing
        // The transport buttons are gated on queue availability; without
        // these the icons render disabled and clicks never reach the mock.
        every { controller.hasNextMediaItem() } returns true
        every { controller.hasPreviousMediaItem() } returns true
        every { controller.duration } returns 200_000L
        every { controller.currentPosition } returns 0L

        store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(
                ApplicationProvider.getApplicationContext(),
            ),
        )[PlaybackViewModel::class.java]

        PlayerConnection.publishForTest(controller)
        waitUntil { shuffleForcedOff }

        setScreen { NowPlayingSheet(viewModel = vm, onDismiss = onDismiss) }
        compose.waitForIdle()
    }

    @Test
    fun `the current track is named`() {
        sheet(title = "Breed", artist = "Nirvana")

        compose.onNodeWithText("Breed").assertIsDisplayed()
        // The artist shows in the header and again under the artwork.
        compose.onAllNodesWithText("Nirvana").onFirst().assertIsDisplayed()
    }

    @Test
    fun `next asks the controller to advance`() {
        sheet()

        compose.onNodeWithContentDescription("Successivo").performClick()
        shadowMainLooper().idle()

        verify { controller.seekToNextMediaItem() }
    }

    @Test
    fun `previous asks the controller to go back`() {
        sheet()

        compose.onNodeWithContentDescription("Precedente").performClick()
        shadowMainLooper().idle()

        verify { controller.seekToPrevious() }
    }

    @Test
    fun `play starts a paused controller`() {
        sheet(playing = false)

        compose.onNodeWithContentDescription("Riproduci").performClick()
        shadowMainLooper().idle()

        verify { controller.play() }
    }

    @Test
    fun `pause stops a playing controller`() {
        sheet(playing = true)

        compose.onNodeWithContentDescription("Pausa").performClick()
        shadowMainLooper().idle()

        verify { controller.pause() }
    }

    /**
     * Shuffle is app-level: the native flag stays off and the toggle goes
     * through the service. Setting `shuffleModeEnabled` here would be a
     * silent no-op, which is exactly how it used to fail.
     */
    @Test
    fun `shuffle does not touch the controller's native flag`() {
        sheet()

        compose.onNodeWithContentDescription("Casuale").performClick()
        shadowMainLooper().idle()

        verify(exactly = 0) { controller.shuffleModeEnabled = true }
    }

    @Test
    fun `the transport controls are all present`() {
        sheet()

        compose.onNodeWithContentDescription("Ripeti").assertIsDisplayed()
        compose.onNodeWithContentDescription("Coda").assertIsDisplayed()
        compose.onNodeWithContentDescription("Equalizzatore").assertIsDisplayed()
    }

    @Test
    fun `collapsing dismisses the sheet`() {
        var dismissed = false

        sheet(onDismiss = { dismissed = true })
        compose.onNodeWithContentDescription("Comprimi").performClick()

        assertEquals(true, dismissed)
    }
}
