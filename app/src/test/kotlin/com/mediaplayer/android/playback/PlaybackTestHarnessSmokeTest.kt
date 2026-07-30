package com.mediaplayer.android.playback

import android.app.Application
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

/**
 * Proves the test harness itself works before any behavioural test relies on
 * it: a real [ExoPlayer] boots under Robolectric, timeline mutations land, and
 * a real [Bundle] round-trips through [MediaMetadata] extras.
 *
 * That last point is why Robolectric is here at all. `isUserQueued()` reads a
 * boolean out of `mediaMetadata.extras`, and under plain JVM stubs every
 * Bundle getter returns the default — a user-queued item would be
 * indistinguishable from a normal one, which is precisely the distinction the
 * queue tests turn on.
 */
// Plain Application on purpose: booting MediaPlayerApp would start the real
// auth bootstrap and bind a MediaController to the playback service, and
// Robolectric then delivers onServiceConnected with a null ComponentName the
// moment the looper idles. These tests exercise the queue engine, not startup.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class PlaybackTestHarnessSmokeTest {

    private lateinit var player: ExoPlayer

    @Before
    fun setUp() {
        player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()
    }

    @After
    fun tearDown() {
        player.release()
        shadowMainLooper().idle()
    }

    @Test
    fun `player accepts a timeline and reports it back`() {
        player.setMediaItems(listOf(item("1"), item("2"), item("3")))
        shadowMainLooper().idle()

        assertEquals(3, player.mediaItemCount)
        assertEquals("2", player.getMediaItemAt(1).mediaId)
    }

    @Test
    fun `timeline mutations dispatch listener callbacks`() {
        val transitions = mutableListOf<String?>()
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                transitions += mediaItem?.mediaId
            }
        })

        player.setMediaItems(listOf(item("1"), item("2")))
        shadowMainLooper().idle()

        assertTrue("expected at least one transition, got $transitions", transitions.isNotEmpty())
        assertEquals("1", transitions.first())
    }

    @Test
    fun `user-queued flag survives a real Bundle round-trip`() {
        assertTrue(userQueuedItem("42").isUserQueued())
        assertFalse(item("42").isUserQueued())
    }

    // A URI is mandatory: without one MediaItem.localConfiguration is null and
    // DefaultMediaSourceFactory rejects the item. Production items always
    // carry a stream URL, so this matches reality rather than working around it.
    private fun item(id: String): MediaItem =
        MediaItem.Builder().setMediaId(id).setUri("https://test.invalid/$id.mp3").build()

    private fun userQueuedItem(id: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setUri("https://test.invalid/$id.mp3")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setExtras(Bundle().apply { putBoolean(KEY_USER_QUEUED, true) })
                    .build()
            )
            .build()
}
