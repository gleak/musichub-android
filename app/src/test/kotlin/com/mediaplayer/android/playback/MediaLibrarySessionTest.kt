package com.mediaplayer.android.playback

import android.app.Application
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.AuthTokenHolder
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.coEvery
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
 * The session as a controller actually sees it. A real [MediaController]
 * connects to the live session in-process, so the connection handshake, the
 * command set it is granted and the custom commands it can send are all
 * exercised for real rather than asserted against the player underneath.
 *
 * That handshake is where the Android Auto defects lived: a head unit draws
 * the controls the session advertises, so a command granted or withheld by
 * mistake is a button that isn't there, or one that is there and dead.
 *
 * A controller built here runs in the app's own process, so it is always
 * the trusted, own-package branch. The untrusted branch needs a second
 * package and belongs to an instrumentation test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class MediaLibrarySessionTest {

    private lateinit var api: MediaPlayerApi
    private lateinit var serviceController: ServiceController<MediaPlaybackService>
    private lateinit var service: MediaPlaybackService
    private val connected = mutableListOf<MediaController>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        MediaPlayerApp.contextOverride = context
        ReadCache.init(context)
        EventQueue.init(context)
        api = mockk(relaxed = true)
        Network.apiOverride = api

        serviceController = Robolectric.buildService(MediaPlaybackService::class.java).create()
        service = serviceController.get()
        shadowMainLooper().idle()
    }

    @After
    fun tearDown() {
        connected.forEach { it.release() }
        connected.clear()
        shadowMainLooper().idle()
        AuthTokenHolder.idToken = null
        serviceController.destroy()
        shadowMainLooper().idle()
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
    }

    private val session
        get() = service.onGetSession(mockk(relaxed = true))
            ?: error("service exposes no session")

    /** Drain the looper until [future] settles, then hand back its value. */
    private fun <T> await(future: ListenableFuture<T>, timeoutMs: Long = 10_000): T {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline && !future.isDone) {
            shadowMainLooper().idle()
            Thread.sleep(5)
        }
        if (!future.isDone) throw AssertionError("future never completed")
        return future.get()
    }

    private fun connectController(): MediaController {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return await(MediaController.Builder(context, session.token).buildAsync())
            .also { connected += it }
    }

    private fun connectBrowser(): MediaBrowser {
        val context = ApplicationProvider.getApplicationContext<Application>()
        return await(MediaBrowser.Builder(context, session.token).buildAsync())
            .also { connected += it }
    }

    // ---------- the connection handshake ----------

    @Test
    fun `a controller connects to the session`() {
        val c = connectController()

        assertTrue(c.isConnected)
    }

    /**
     * The five custom commands are the app's own controls: the heart, the
     * sleep timer, shuffle, repeat and drop-from-source. A trusted
     * controller gets all of them.
     */
    @Test
    fun `a trusted controller is granted every custom command`() {
        val c = connectController()

        listOf(
            MediaPlaybackService.ACTION_TOGGLE_LIKE,
            MediaPlaybackService.ACTION_SLEEP_TIMER,
            MediaPlaybackService.ACTION_TOGGLE_SHUFFLE,
            MediaPlaybackService.ACTION_CYCLE_REPEAT,
            MediaPlaybackService.ACTION_DROP_FROM_SOURCE,
        ).forEach { action ->
            assertTrue(
                "$action was not granted",
                c.availableSessionCommands.contains(SessionCommand(action, Bundle.EMPTY)),
            )
        }
    }

    /**
     * Shuffle is app-level: the queue is reordered in the timeline. Granting
     * the native command as well would put a second, dead shuffle control on
     * the head unit, fighting the real one.
     */
    @Test
    fun `the native shuffle command is withheld from everyone`() {
        val c = connectController()

        assertFalse(c.isCommandAvailable(Player.COMMAND_SET_SHUFFLE_MODE))
    }

    /** Repeat is native, and the app's own controllers set it directly. */
    @Test
    fun `an own controller keeps the native repeat command`() {
        val c = connectController()

        assertTrue(c.isCommandAvailable(Player.COMMAND_SET_REPEAT_MODE))
    }

    @Test
    fun `the transport commands a head unit needs are granted`() {
        val c = connectController()

        assertTrue(c.isCommandAvailable(Player.COMMAND_PLAY_PAUSE))
        assertTrue(c.isCommandAvailable(Player.COMMAND_SET_MEDIA_ITEM))
    }

    // ---------- custom commands ----------

    @Test
    fun `the sleep timer command is accepted`() {
        val c = connectController()

        val result = await(
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, Bundle.EMPTY),
                Bundle().apply { putInt("minutes", 15) },
            ),
        )

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
    }

    @Test
    fun `cancelling the sleep timer is accepted`() {
        val c = connectController()
        await(
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, Bundle.EMPTY),
                Bundle().apply { putInt("minutes", 15) },
            ),
        )

        val result = await(
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_SLEEP_TIMER, Bundle.EMPTY),
                Bundle.EMPTY,
            ),
        )

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
    }

    @Test
    fun `the shuffle command is accepted with a queue loaded`() {
        val c = connectController()
        setQueue(c)

        val result = await(
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_TOGGLE_SHUFFLE, Bundle.EMPTY),
                Bundle.EMPTY,
            ),
        )

        assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
    }

    @Test
    fun `the repeat command cycles the player's repeat mode`() {
        val c = connectController()
        setQueue(c)
        val before = c.repeatMode

        await(
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_CYCLE_REPEAT, Bundle.EMPTY),
                Bundle.EMPTY,
            ),
        )
        shadowMainLooper().idle()

        assertFalse("repeat mode did not change", before == c.repeatMode)
    }

    /**
     * Liking needs a song id on the current item; with nothing playing there
     * is nothing to like, and the session says so rather than pretending.
     */
    @Test
    fun `liking with nothing playing is refused`() {
        val c = connectController()

        val result = await(
            c.sendCustomCommand(
                SessionCommand(MediaPlaybackService.ACTION_TOGGLE_LIKE, Bundle.EMPTY),
                Bundle.EMPTY,
            ),
        )

        assertFalse(result.resultCode == SessionResult.RESULT_SUCCESS)
    }

    // ---------- setting a queue ----------

    private fun setQueue(c: MediaController) {
        val items = listOf(playable("1"), playable("2"), playable("3"))
        shadowMainLooper().idle()
        c.setMediaItems(items, 0, 0L)
        shadowMainLooper().idle()
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline && c.mediaItemCount == 0) {
            shadowMainLooper().idle()
            Thread.sleep(5)
        }
    }

    private fun playable(id: String) = MediaItem.Builder()
        .setMediaId(id)
        .setUri("https://test.invalid/$id.mp3")
        .build()

    @Test
    fun `a queue set through the session reaches the player`() {
        val c = connectController()

        setQueue(c)

        assertEquals(3, c.mediaItemCount)
    }

    /**
     * Skipping is what a steering wheel sends, and Media3 derives its
     * availability from the timeline — so it only becomes true once there is
     * something on either side of the current track.
     */
    @Test
    fun `skipping becomes available once a queue is loaded`() {
        val c = connectController()
        setQueue(c)

        c.seekTo(1, 0L)
        shadowMainLooper().idle()
        awaitCondition { c.currentMediaItemIndex == 1 }

        assertTrue(c.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM))
        assertTrue(c.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM))
    }

    private fun awaitCondition(timeoutMs: Long = 10_000, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            shadowMainLooper().idle()
            if (condition()) return
            Thread.sleep(5)
        }
        throw AssertionError("condition still false after ${timeoutMs}ms")
    }

    // ---------- the browse tree ----------

    @Test
    fun `a browser gets a library root`() {
        coEvery { api.listPlaylists(any()) } returns emptyList()
        val browser = connectBrowser()

        val root = await(browser.getLibraryRoot(null))

        assertTrue(root.resultCode == androidx.media3.session.LibraryResult.RESULT_SUCCESS)
        assertEquals(LibraryTree.ROOT_ID, root.value?.mediaId)
    }

    /**
     * The head unit's home screen is this list. An empty one is a library
     * that looks broken from the driver's seat.
     */
    @Test
    fun `the root offers the browse sections`() {
        AuthTokenHolder.idToken = "id-token"
        coEvery { api.listPlaylists(any()) } returns emptyList()
        val browser = connectBrowser()

        val children = await(browser.getChildren(LibraryTree.ROOT_ID, 0, 50, null))

        val ids = children.value?.map { it.mediaId }.orEmpty()
        assertTrue("root was empty", ids.isNotEmpty())
        assertTrue("$ids", ids.contains(LibraryTree.LIKED_ID))
        assertTrue("$ids", ids.contains(LibraryTree.PLAYLISTS_ID))
        assertTrue("$ids", ids.contains(LibraryTree.QUEUE_ID))
    }

    /**
     * Android Auto can cold-start the service without the phone UI ever
     * running, so the token may be missing. Every backend call would then be
     * 401-ed and the head unit would show a blank panel with no explanation.
     * One info row says what to do instead.
     */
    @Test
    fun `a signed-out browse says to open the app rather than showing nothing`() {
        AuthTokenHolder.idToken = null
        val browser = connectBrowser()

        val children = await(browser.getChildren(LibraryTree.ROOT_ID, 0, 50, null))

        val titles = children.value?.map { it.mediaMetadata.title?.toString() }.orEmpty()
        assertEquals(listOf("Apri MusicHub sul telefono per accedere"), titles)
    }

    /**
     * The queue is the app's own timeline, not the backend's, so reviewing
     * what is playing has to work signed out and offline.
     */
    @Test
    fun `the queue folder is browsable without a token`() {
        AuthTokenHolder.idToken = null
        val c = connectController()
        setQueue(c)
        val browser = connectBrowser()

        val children = await(browser.getChildren(LibraryTree.QUEUE_ID, 0, 50, null))

        assertEquals(3, children.value?.size)
    }
}
