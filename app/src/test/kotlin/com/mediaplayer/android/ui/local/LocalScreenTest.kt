package com.mediaplayer.android.ui.local

import android.app.Application
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.data.local.FakeMediaProvider
import com.mediaplayer.android.data.local.LocalLibraryRepository
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.local.LocalLikedStore
import com.mediaplayer.android.data.local.LocalPlaylist
import com.mediaplayer.android.data.local.LocalPlaylistStore
import com.mediaplayer.android.ui.ScreenTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper.shadowMainLooper
import java.io.File

/**
 * Base for the on-device library screens. Where [ScreenTest] fakes the
 * backend, this fakes the phone: a stand-in media provider serves whatever
 * rows a test publishes, and the audio permission is granted so the scan
 * actually runs.
 *
 * The two on-device stores (playlists, likes) are DataStore-backed
 * singletons, so their contents survive from one test to the next inside
 * the same JVM. Both are emptied here — otherwise a playlist created in one
 * test shows up in the next one's assertions.
 */
abstract class LocalScreenTest : ScreenTest() {

    @Before
    fun wireLocalLibrary() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).grantPermissions(LocalLibraryRepository.requiredPermission())
        publishTracks()
        Robolectric.setupContentProvider(
            FakeMediaProvider::class.java,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.authority,
        )
        // The scan cache is emitted before the fresh scan, so a stale one
        // would show tracks a test never published.
        File(app.filesDir, "local_scan_cache.json").delete()
        runBlocking {
            val playlists = LocalPlaylistStore.instance(app)
            playlists.snapshot().forEach { playlists.delete(it.id) }
            val liked = LocalLikedStore.instance(app)
            liked.liked.first().forEach { liked.setLiked(it, false) }
        }
    }

    /** Serve [rows] as the device's audio library. */
    protected fun publishTracks(vararg rows: Array<Any?>) {
        FakeMediaProvider.columns = PROJECTION
        FakeMediaProvider.rows = rows.toList()
    }

    protected fun revokeAudioPermission() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(app).denyPermissions(LocalLibraryRepository.requiredPermission())
    }

    protected fun localPlaylists() = LocalPlaylistStore.instance(
        ApplicationProvider.getApplicationContext<Application>(),
    )

    /**
     * Playlist edits are fire-and-forget: the composable hands them to the
     * ViewModel, which writes to DataStore off the main thread. Waiting for
     * Compose to be idle says nothing about that write, so poll the store.
     */
    protected fun awaitPlaylists(predicate: (List<LocalPlaylist>) -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        var last: List<LocalPlaylist> = emptyList()
        while (System.currentTimeMillis() < deadline) {
            last = runBlocking { localPlaylists().snapshot() }
            if (predicate(last)) return
            compose.waitForIdle()
            Thread.sleep(20)
        }
        throw AssertionError("local playlists never reached the expected state: $last")
    }

    /**
     * Open a dialog by clicking [label], with the frame clock frozen first.
     *
     * A dialog window reports itself focused under Robolectric, which starts
     * the blinking-cursor animation in any text field it contains. That
     * animation keeps the recomposer permanently busy, so the test rule's
     * automatic "wait until idle" never returns while the dialog is up. The
     * clock is driven by hand from here instead. For the same reason, don't
     * type into a dialog field in these tests — focusing it wedges every
     * later assertion.
     */
    protected fun openDialog(label: String) {
        compose.mainClock.autoAdvance = false
        // Whatever is already on screen — a dropdown menu, say — lives in a
        // window of its own too, and stops being reachable the moment the
        // rule stops running the looper for us.
        pumpFrames()
        compose.onNodeWithText(label).performClick()
        pumpFrames()
    }

    /**
     * Advance the frozen frame clock far enough for a recomposition to
     * settle. The main looper is pumped alongside it because a dialog
     * arrives in a window of its own, which Robolectric only attaches when
     * the looper runs.
     */
    protected fun pumpFrames(count: Int = 20) {
        repeat(count) {
            compose.mainClock.advanceTimeByFrame()
            shadowMainLooper().idle()
        }
    }

    protected fun localLikes() = LocalLikedStore.instance(
        ApplicationProvider.getApplicationContext<Application>(),
    )

    /** One MediaStore row. Defaults describe an ordinary indexed track. */
    protected fun trackRow(
        id: Long = 1L,
        title: String = "Breed",
        artist: String? = "Nirvana",
        album: String? = "Nevermind",
        albumId: Long = 10L,
        durationMs: Long = 200_000L,
        dateAddedSeconds: Long = 1_700_000_000L,
        folder: String = "Nirvana",
    ): Array<Any?> = arrayOf(
        id, title, artist, album, albumId, durationMs, dateAddedSeconds,
        "/storage/emulated/0/Music/$folder/${title.lowercase()}.mp3",
        "Music/$folder/",
        "${title.lowercase()}.mp3",
    )

    protected companion object {
        val PROJECTION = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DISPLAY_NAME,
        )
    }
}
