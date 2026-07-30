package com.mediaplayer.android.playback

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.RecentsCache
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * The view model is driven entirely by callbacks from a [MediaController]
 * it does not own, which is what made it hard to test and easy to break.
 * A stand-in controller is published through [PlayerConnection] and the
 * listener the view model registers on it is captured, then invoked
 * directly — the same sequence Media3 would deliver.
 *
 * These pin the parts that are invisible when wrong: whether a track
 * started from the car is recognised at all, and what counts as a play.
 *
 * The view model is created through a [ViewModelStore] so it can be
 * cleared between tests. It isn't tidiness: [PlayerConnection] is a
 * singleton, so a view model left alive keeps collecting from it and every
 * later test drags the previous ones along with it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackViewModelTest {

    private lateinit var api: MediaPlayerApi
    private lateinit var controller: MediaController
    private lateinit var listener: Player.Listener
    private lateinit var store: ViewModelStore
    private lateinit var viewModel: PlaybackViewModel

    /**
     * Flipped when the view model forces the controller's native shuffle
     * off. That write sits immediately after the view model's one suspending
     * step on connect (a DataStore read for the saved shuffle/repeat), so it
     * doubles as the signal that the connect block has finished and the
     * state flows are populated.
     */
    private var shuffleForcedOff = false

    @Before
    fun setUp() {
        // Unconfined: the view model launches its controller collector in
        // `init`, and the test needs it to have run by the time the
        // constructor returns rather than at some later scheduler tick.
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Application>()
        MediaPlayerApp.contextOverride = context
        ReadCache.init(context)
        EventQueue.init(context)
        RecentsCache.clear()
        api = mockk(relaxed = true)
        Network.apiOverride = api
        controller = mockk(relaxed = true)
        shuffleForcedOff = false
        store = ViewModelStore()
    }

    @After
    fun tearDown() {
        PlayerConnection.publishForTest(null)
        store.clear()
        shadowMainLooper().idle()
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
        Dispatchers.resetMain()
    }

    /**
     * Spin the Robolectric looper until [condition] holds. The view model
     * reads DataStore on a real IO thread during connect, so idling the
     * looper once isn't enough — there is genuine asynchrony to wait on.
     */
    private fun waitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowMainLooper().idle()
            if (condition()) return
            Thread.sleep(10)
        }
        throw AssertionError("condition still false after ${timeoutMs}ms")
    }

    /** Boot the view model against [controller] and capture its listener. */
    private fun connect(vararg queue: MediaItem) {
        val captured = slot<Player.Listener>()
        every { controller.addListener(capture(captured)) } returns Unit
        every { controller.shuffleModeEnabled = false } answers { shuffleForcedOff = true }
        every { controller.mediaItemCount } returns queue.size
        queue.forEachIndexed { i, item -> every { controller.getMediaItemAt(i) } returns item }
        every { controller.currentMediaItemIndex } returns 0
        every { controller.currentMediaItem } returns queue.firstOrNull()

        viewModel = ViewModelProvider(
            store,
            ViewModelProvider.AndroidViewModelFactory.getInstance(
                ApplicationProvider.getApplicationContext(),
            ),
        )[PlaybackViewModel::class.java]

        PlayerConnection.publishForTest(controller)
        waitUntil { shuffleForcedOff }
        listener = captured.captured
    }

    private fun item(
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

    @Test
    fun `connecting adopts the controller's current track`() {
        connect(item("42", title = "Breed"))

        assertEquals(42L, viewModel.currentSong.value?.id)
        assertEquals("Breed", viewModel.currentSong.value?.title)
    }

    /**
     * Android Auto, the media library and playback resumption all emit the
     * "song:{id}" form. Not stripping it returned null for every one of
     * those sessions, which blanked the mini player and suppressed history.
     */
    @Test
    fun `a car-started track is recognised despite its prefixed id`() {
        connect(item("song:42", title = "Breed"))

        assertEquals(42L, viewModel.currentSong.value?.id)
    }

    @Test
    fun `a track with an unparseable id is ignored rather than guessed at`() {
        connect(item("not-a-number"))

        assertNull(viewModel.currentSong.value)
    }

    @Test
    fun `the queue mirrors the controller timeline`() {
        connect(item("1"), item("2"), item("3"))

        val queue = viewModel.queue.value
        assertEquals(listOf(1L, 2L, 3L), queue.map { it.song.id })
        assertTrue(queue.first().isCurrent)
        assertFalse(queue[1].isCurrent)
    }

    @Test
    fun `play state follows the controller`() {
        connect(item("1"))

        listener.onIsPlayingChanged(true)
        assertTrue(viewModel.isPlaying.value)

        listener.onIsPlayingChanged(false)
        assertFalse(viewModel.isPlaying.value)
    }

    @Test
    fun `a track transition adopts the incoming track`() {
        connect(item("1"))

        listener.onMediaItemTransition(
            item("2", title = "Lithium"),
            Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
        )
        waitUntil { viewModel.currentSong.value?.id == 2L }

        assertEquals(2L, viewModel.currentSong.value?.id)
        assertEquals("Lithium", viewModel.currentSong.value?.title)
    }

    /**
     * A transition a moment after the track started is the user mashing
     * next. It must not land in recents, which is what Home and Search read.
     */
    @Test
    fun `a micro-skip does not count as a play`() {
        connect(item("1"))
        listener.onIsPlayingChanged(true)

        listener.onMediaItemTransition(item("2"), Player.MEDIA_ITEM_TRANSITION_REASON_SEEK)
        shadowMainLooper().idle()

        assertTrue(RecentsCache.recents.value.none { it.id == 1L })
    }

    @Test
    fun `repeat mode changes are surfaced`() {
        connect(item("1"))

        listener.onRepeatModeChanged(Player.REPEAT_MODE_ONE)

        assertEquals(Player.REPEAT_MODE_ONE, viewModel.repeatMode.value)
    }

    /**
     * The app owns shuffle: the native flag is pinned off so the user queue
     * stays at currentIndex+1. A controller arriving with it on must be
     * corrected, not adopted.
     */
    @Test
    fun `the native shuffle flag is forced off on connect`() {
        connect(item("1"))

        assertTrue(shuffleForcedOff)
    }

    @Test
    fun `queue availability reflects what the controller offers`() {
        every { controller.hasNextMediaItem() } returns true
        every { controller.hasPreviousMediaItem() } returns false
        connect(item("1"), item("2"))

        assertTrue(viewModel.hasNext.value)
        assertFalse(viewModel.hasPrevious.value)
    }

    /**
     * A transient rebind must not blank the mini player: the last known
     * track is kept when the controller goes away.
     */
    @Test
    fun `losing the controller keeps the last known track`() {
        connect(item("1"))

        PlayerConnection.publishForTest(null)
        shadowMainLooper().idle()

        assertEquals(1L, viewModel.currentSong.value?.id)
    }
}
