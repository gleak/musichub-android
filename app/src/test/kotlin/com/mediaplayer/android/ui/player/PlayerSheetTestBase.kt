package com.mediaplayer.android.ui.player

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.MediaController
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.playback.PlaybackViewModel
import com.mediaplayer.android.playback.PlayerConnection
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * Shared setup for the sheets that render off the player. Publishes a
 * stand-in [MediaController] through [PlayerConnection], lets the real
 * [PlaybackViewModel] adopt it, and hands the view model back.
 *
 * The view model comes from a [ViewModelStore] that is cleared after each
 * test: [PlayerConnection] is a singleton, so one left alive keeps
 * collecting from it and every later test carries the earlier ones along.
 */
abstract class PlayerSheetTest : ScreenTest() {

    protected lateinit var controller: MediaController
        private set

    private lateinit var store: ViewModelStore
    private var connected = false

    @After
    fun disconnectController() {
        PlayerConnection.publishForTest(null)
        if (::store.isInitialized) store.clear()
        shadowMainLooper().idle()
    }

    /**
     * Build a controller holding [queue], publish it, and return the view
     * model once it has finished adopting it.
     *
     * [stub] runs before publication so a test can override any part of the
     * controller's behaviour.
     */
    protected fun connectPlayer(
        queue: List<MediaItem>,
        currentIndex: Int = 0,
        stub: (MediaController) -> Unit = {},
    ): PlaybackViewModel {
        controller = mockk(relaxed = true)
        connected = false
        every { controller.shuffleModeEnabled = false } answers { connected = true }
        every { controller.mediaItemCount } returns queue.size
        queue.forEachIndexed { i, mi -> every { controller.getMediaItemAt(i) } returns mi }
        every { controller.currentMediaItemIndex } returns currentIndex
        every { controller.currentMediaItem } returns queue.getOrNull(currentIndex)
        every { controller.duration } returns 200_000L
        every { controller.currentPosition } returns 0L
        every { controller.hasNextMediaItem() } returns (currentIndex < queue.lastIndex)
        every { controller.hasPreviousMediaItem() } returns (currentIndex > 0)
        stub(controller)

        store = ViewModelStore()
        val vm = ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(
                ApplicationProvider.getApplicationContext(),
            ),
        )[PlaybackViewModel::class.java]

        PlayerConnection.publishForTest(controller)
        // The adopt path suspends on a DataStore read before it populates
        // any state, so idling the looper once isn't enough.
        waitUntil { connected }
        return vm
    }

    protected fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowMainLooper().idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("condition still false after ${timeoutMs}ms")
    }

    protected fun track(
        id: String,
        title: String = "Title $id",
        artist: String = "Artist $id",
    ): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri("https://test.invalid/$id.mp3")
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(title).setArtist(artist).build(),
        )
        .build()
}
