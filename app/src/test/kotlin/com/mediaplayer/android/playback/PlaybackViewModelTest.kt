package com.mediaplayer.android.playback

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.LikedSongsCache
import com.mediaplayer.android.data.RecentsCache
import com.mediaplayer.android.data.local.LocalTrack
import com.mediaplayer.android.ui.song
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
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
        // Shared singleton: a like left behind by another test flips the
        // heart the wrong way on the first tap here.
        LikedSongsCache.clear()
        // Shuffle is persisted, so a shuffle-play in one test would still be
        // on when the next one connects and would reorder its queue.
        kotlinx.coroutines.runBlocking {
            PlaybackPrefs.setShuffle(context, false)
            PlaybackPrefs.setRepeat(context, androidx.media3.common.Player.REPEAT_MODE_OFF)
        }
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

    /**
     * Build the view model with no controller published. Every play entry
     * point has to cope with this: it is the state during a cold launch and
     * after a failed bind.
     */
    private fun viewModelWithoutController(): PlaybackViewModel = ViewModelProvider(
        store,
        ViewModelProvider.AndroidViewModelFactory.getInstance(
            ApplicationProvider.getApplicationContext(),
        ),
    )[PlaybackViewModel::class.java]

    private fun localTrack(
        id: Long = 7L,
        title: String = "Breed",
        artist: String = "Nirvana",
    ) = LocalTrack(
        id = id,
        uri = android.net.Uri.parse("content://media/external/audio/media/$id"),
        title = title,
        artist = artist,
        album = "Nevermind",
        durationMs = 200_000L,
        albumId = null,
        albumArtUri = null,
        folderName = "Nirvana",
        folderPath = "Music/Nirvana",
        dateAddedMs = 0L,
    )

    // ---------- play entry points ----------

    @Test
    fun `playing a song hands it to the controller and starts it`() {
        connect()

        viewModel.play(song(1L, title = "Bohemian"))

        verify { controller.setMediaItem(any()) }
        verify { controller.prepare() }
        verify { controller.playWhenReady = true }
    }

    /**
     * A play tap before the controller has bound used to return silently,
     * which is exactly what "I can't start any song" looked like in the car.
     * It has to say something.
     */
    @Test
    fun `playing before the player is ready reports it instead of doing nothing`() {
        val vm = viewModelWithoutController()

        vm.play(song(1L))

        val error = vm.playbackError.value
        assertEquals("PLAYER_NOT_READY", error?.errorCodeName)
    }

    @Test
    fun `playing a playlist starts on the tapped track, in the original order`() {
        connect()
        val items = slot<List<MediaItem>>()
        val index = slot<Int>()
        every { controller.setMediaItems(capture(items), capture(index), any()) } returns Unit

        viewModel.playPlaylist(
            listOf(song(1L), song(2L), song(3L)),
            startIndex = 2,
        )

        assertEquals(listOf("1", "2", "3"), items.captured.map { it.mediaId })
        assertEquals(2, index.captured)
    }

    /** Shuffle is app-level, so shuffle-play turns the app's flag on. */
    @Test
    fun `shuffle-play turns shuffle on`() {
        connect()

        viewModel.playPlaylistShuffled(listOf(song(1L), song(2L)))

        assertTrue(viewModel.shuffleEnabled.value)
    }

    @Test
    fun `an empty playlist is not handed to the player at all`() {
        connect()

        viewModel.playPlaylist(emptyList())

        verify(exactly = 0) { controller.setMediaItems(any(), any(), any()) }
    }

    @Test
    fun `playing a playlist records where it came from`() {
        connect()

        viewModel.playPlaylist(listOf(song(1L)), sourceKey = "playlist:42")

        assertEquals("playlist:42", viewModel.activeSourceKey.value)
    }

    /** A single song is not a collection, so it clears the source. */
    @Test
    fun `playing one song clears the active source`() {
        connect()
        viewModel.playPlaylist(listOf(song(1L)), sourceKey = "playlist:42")

        viewModel.play(song(9L))

        assertNull(viewModel.activeSourceKey.value)
    }

    // ---------- on-device tracks ----------

    /**
     * Local tracks are broadcast with a negated id so the playback layer can
     * tell them from catalogue songs, whose ids are always positive.
     */
    @Test
    fun `a local track is published under a negated id`() {
        connect()
        val item = slot<MediaItem>()
        every { controller.setMediaItem(capture(item)) } returns Unit

        viewModel.playLocal(localTrack(id = 7L))

        assertEquals("-7", item.captured.mediaId)
    }

    @Test
    fun `queueing a local track next inserts it after the current one`() {
        connect(item("1"), item("2"))
        every { controller.currentMediaItemIndex } returns 0
        every { controller.mediaItemCount } returns 2
        val index = slot<Int>()
        every { controller.addMediaItem(capture(index), any()) } returns Unit

        viewModel.playNextLocal(localTrack())

        assertEquals(1, index.captured)
    }

    @Test
    fun `playing a local library keeps the given order`() {
        connect()
        val items = slot<List<MediaItem>>()
        every { controller.setMediaItems(capture(items), any(), any()) } returns Unit

        viewModel.playLocalAll(
            listOf(localTrack(1L, "Breed"), localTrack(2L, "Lithium")),
            startIndex = 1,
        )

        assertEquals(listOf("-1", "-2"), items.captured.map { it.mediaId })
    }

    @Test
    fun `an empty local library is not handed to the player`() {
        connect()

        viewModel.playLocalAll(emptyList())

        verify(exactly = 0) { controller.setMediaItems(any(), any(), any()) }
    }

    // ---------- sleep timer ----------

    @Test
    fun `arming the sleep timer sets the remaining time`() {
        connect()

        viewModel.setSleepTimer(15)

        assertTrue(viewModel.sleepTimerActive.value)
        assertEquals(15 * 60_000L, viewModel.sleepTimerRemainingMs.value)
        assertFalse(viewModel.sleepTimerEndOfTrack.value)
    }

    @Test
    fun `the end-of-track timer has no countdown`() {
        connect()

        viewModel.setEndOfTrackSleepTimer()

        assertTrue(viewModel.sleepTimerActive.value)
        assertTrue(viewModel.sleepTimerEndOfTrack.value)
        assertEquals(0L, viewModel.sleepTimerRemainingMs.value)
    }

    @Test
    fun `cancelling the sleep timer clears every part of it`() {
        connect()
        viewModel.setSleepTimer(15)

        viewModel.cancelSleepTimer()

        assertFalse(viewModel.sleepTimerActive.value)
        assertFalse(viewModel.sleepTimerEndOfTrack.value)
        assertEquals(0L, viewModel.sleepTimerRemainingMs.value)
    }

    @Test
    fun `arming an end-of-track timer replaces a minute timer`() {
        connect()
        viewModel.setSleepTimer(15)

        viewModel.setEndOfTrackSleepTimer()

        assertEquals(0L, viewModel.sleepTimerRemainingMs.value)
        assertTrue(viewModel.sleepTimerEndOfTrack.value)
    }

    // ---------- playback errors ----------

    /**
     * A failure has to say which song and why. The older surface was a toast
     * that said neither and then disappeared on its own.
     */
    @Test
    fun `a playback failure names the track and the reason`() {
        connect(item("42", title = "Breed"))

        listener.onPlayerError(
            PlaybackException(
                "boom",
                null,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            ),
        )
        shadowMainLooper().idle()

        val error = viewModel.playbackError.value
        assertEquals("Breed", error?.songTitle)
        assertEquals(
            "Nessuna connessione di rete o server irraggiungibile.",
            error?.reason,
        )
    }

    @Test
    fun `a rejected stream is reported as a server problem`() {
        connect(item("42", title = "Breed"))

        listener.onPlayerError(
            PlaybackException("boom", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS),
        )
        shadowMainLooper().idle()

        assertEquals(
            "Il server ha rifiutato la richiesta dello stream (HTTP error).",
            viewModel.playbackError.value?.reason,
        )
    }

    @Test
    fun `dismissing the error clears it`() {
        connect(item("42"))
        listener.onPlayerError(
            PlaybackException("boom", null, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS),
        )
        shadowMainLooper().idle()

        viewModel.dismissPlaybackError()

        assertNull(viewModel.playbackError.value)
    }

    // ---------- alarm export ----------

    /** There is no backend copy of an on-device file to export. */
    @Test
    fun `a local track cannot be saved as an alarm sound`() {
        connect(item("-7", title = "Breed"))

        viewModel.saveCurrentAsAlarmSound()
        shadowMainLooper().idle()

        val state = viewModel.alarmExportState.value
        assertTrue("was $state", state is PlaybackViewModel.AlarmExportState.Failure)
    }

    // ---------- actions that need a backend copy ----------

    /**
     * These four all re-fetch something from the backend, and none of them
     * mean anything for a file that lives on the phone. Each says so rather
     * than spinning on a request that can't exist.
     */
    @Test
    fun `re-downloading a local track is refused with a reason`() {
        connect(item("-7", title = "Breed"))

        viewModel.redownloadCurrent()

        assertEquals("Non disponibile per i brani locali", viewModel.redownloadError.value)
    }

    @Test
    fun `downloading a video for a local track is refused with a reason`() {
        connect(item("-7", title = "Breed"))

        viewModel.downloadVideoForCurrent()

        assertEquals("Non disponibile per i brani locali", viewModel.videoDownloadError.value)
    }

    @Test
    fun `re-initialising the video of a local track is refused with a reason`() {
        connect(item("-7", title = "Breed"))

        viewModel.reinitializeVideoForCurrent()

        assertEquals(
            "Non disponibile per i brani locali",
            viewModel.videoReinitializeError.value,
        )
    }

    @Test
    fun `re-downloading asks the backend for a fresh copy`() {
        connect(item("42", title = "Breed"))

        viewModel.redownloadCurrent()
        shadowMainLooper().idle()

        coVerify(exactly = 1) { api.redownloadSong(42L) }
    }

    @Test
    fun `a failed re-download is reported`() {
        coEvery { api.redownloadSong(any()) } throws java.io.IOException("offline")
        connect(item("42", title = "Breed"))

        viewModel.redownloadCurrent()
        waitUntil { viewModel.redownloadError.value != null }

        assertFalse(viewModel.redownloading.value)
    }

    @Test
    fun `dismissing a re-download error clears it`() {
        connect(item("-7"))
        viewModel.redownloadCurrent()

        viewModel.consumeRedownloadError()

        assertNull(viewModel.redownloadError.value)
    }

    @Test
    fun `dismissing a video download error clears it`() {
        connect(item("-7"))
        viewModel.downloadVideoForCurrent()

        viewModel.consumeVideoDownloadError()

        assertNull(viewModel.videoDownloadError.value)
    }

    @Test
    fun `dismissing a video re-initialise error clears it`() {
        connect(item("-7"))
        viewModel.reinitializeVideoForCurrent()

        viewModel.consumeVideoReinitializeError()

        assertNull(viewModel.videoReinitializeError.value)
    }

    // ---------- reporting a wrong track ----------

    /**
     * Flagging tells the backend the audio doesn't match the metadata, then
     * tells the service to drop it from the pool the endless queue refills
     * from — otherwise a wrap re-appends the song that was just tombstoned.
     */
    @Test
    fun `flagging a track drops it from the source pool too`() {
        connect(item("42", title = "Breed"))

        viewModel.flagWrong(42L)
        shadowMainLooper().idle()

        coVerify(exactly = 1) { api.flagSongWrong(42L) }
        verify { controller.sendCustomCommand(any(), any()) }
    }

    /** Local files and the sentinel zero have nothing to report. */
    @Test
    fun `flagging a local track is a no-op`() {
        connect(item("42"))

        viewModel.flagWrong(-7L)
        shadowMainLooper().idle()

        coVerify(exactly = 0) { api.flagSongWrong(any()) }
    }

    @Test
    fun `flagging the sentinel id is a no-op`() {
        connect(item("42"))

        viewModel.flagWrong(0L)
        shadowMainLooper().idle()

        coVerify(exactly = 0) { api.flagSongWrong(any()) }
    }

    // ---------- the heart ----------

    /**
     * The heart flips from what it is showing, not from the session extras:
     * those arrive unreliably for in-process controllers and used to carry
     * the previous track's value across a transition, inverting the first
     * tap after every skip.
     */
    @Test
    fun `liking flips from what the heart shows`() {
        connect(item("42", title = "Breed"))
        assertFalse(viewModel.currentLiked.value)

        viewModel.toggleCurrentLike()

        assertTrue(viewModel.currentLiked.value)
    }

    @Test
    fun `liking asks the service rather than the backend directly`() {
        connect(item("42", title = "Breed"))

        viewModel.toggleCurrentLike()
        shadowMainLooper().idle()

        verify { controller.sendCustomCommand(any(), any()) }
    }

    // ---------- play history ----------

    /**
     * The flush runs when the app goes to the background, and it only emits
     * a play that actually counts as one — a partial listen waits for the
     * next transition so it can be reported as the skip it was.
     */
    @Test
    fun `flushing with nothing tracked sends nothing`() {
        connect()

        kotlinx.coroutines.runBlocking { viewModel.flushPlayHistoryAwait() }

        coVerify(exactly = 0) { api.recordPlay(any()) }
    }
}
