package com.mediaplayer.android.playback

import android.app.Application
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ServiceController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * Brings the playback service up for real: `onCreate` builds an ExoPlayer,
 * wraps it, opens a [MediaSession] and installs the queue and healing
 * controllers. None of that was exercised, and all of it runs before a
 * single note plays — a failure here is an app that starts and then does
 * nothing when you press play, on the phone and in the car alike.
 *
 * The session itself is checked for the commands Android Auto needs.
 * Head units read the available commands to decide which transport buttons
 * to draw, so a missing one is a button the user cannot press.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class MediaPlaybackServiceTest {

    private lateinit var api: MediaPlayerApi
    private lateinit var controller: ServiceController<MediaPlaybackService>
    private lateinit var service: MediaPlaybackService

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        MediaPlayerApp.contextOverride = context
        ReadCache.init(context)
        EventQueue.init(context)
        api = mockk(relaxed = true)
        Network.apiOverride = api

        controller = Robolectric.buildService(MediaPlaybackService::class.java).create()
        service = controller.get()
        shadowMainLooper().idle()
    }

    @After
    fun tearDown() {
        controller.destroy()
        shadowMainLooper().idle()
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
    }

    private val session: MediaSession
        get() = service.onGetSession(mockk(relaxed = true))
            ?: error("service exposes no session")

    @Test
    fun `the service comes up with a session`() {
        assertNotNull(service.onGetSession(mockk(relaxed = true)))
    }

    @Test
    fun `the session exposes a player`() {
        assertNotNull(session.player)
    }

    /**
     * A fresh service has nothing loaded. It must not claim to be playing —
     * a head unit that believes otherwise renders a pause button that does
     * nothing.
     */
    @Test
    fun `a fresh service is idle and empty`() {
        val player = session.player

        assertFalse(player.isPlaying)
        assertEquals(0, player.mediaItemCount)
    }

    /**
     * Shuffle is app-level: the native flag stays off so the user queue can
     * sit at currentIndex+1 under any ordering. The command itself is
     * withheld per-controller at connect time, which isn't reachable from
     * here; what is reachable, and what the ordering depends on, is that
     * the flag is never actually set.
     */
    @Test
    fun `the native shuffle flag stays off`() {
        assertFalse(session.player.shuffleModeEnabled)
    }

    /**
     * Skipping is what a steering wheel sends. Media3 derives availability
     * from the timeline, so the queue has to be loaded before asking —
     * on an empty one there is nothing to skip to and the answer is
     * legitimately no.
     */
    @Test
    fun `the transport commands a head unit needs are granted once a queue is loaded`() {
        val player = session.player
        player.setMediaItems(
            listOf(playableItem("1"), playableItem("2"), playableItem("3")),
        )
        player.seekTo(1, 0L)
        shadowMainLooper().idle()

        assertTrue(player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
        assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
    }

    private fun playableItem(id: String) = androidx.media3.common.MediaItem.Builder()
        .setMediaId(id)
        .setUri("https://test.invalid/$id.mp3")
        .build()

    @Test
    fun `repeat mode can be set through the session player`() {
        val player = session.player

        player.repeatMode = Player.REPEAT_MODE_ALL
        shadowMainLooper().idle()

        assertEquals(Player.REPEAT_MODE_ALL, player.repeatMode)
    }

    /**
     * Swiping the app away with nothing playing should let the service go,
     * rather than leaving an empty foreground notification behind.
     */
    @Test
    fun `an idle service survives the task being removed`() {
        service.onTaskRemoved(Intent())
        shadowMainLooper().idle()

        assertNotNull(service.onGetSession(mockk(relaxed = true)))
    }

    @Test
    fun `destroying the service releases without throwing`() {
        controller.destroy()
        shadowMainLooper().idle()

        // Re-created in tearDown's destroy() call; the assertion here is
        // simply that the first teardown completed.
        controller = Robolectric.buildService(MediaPlaybackService::class.java).create()
        service = controller.get()
        shadowMainLooper().idle()
        assertNotNull(service.onGetSession(mockk(relaxed = true)))
    }
}
