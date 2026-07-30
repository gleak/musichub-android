package com.mediaplayer.android.ui

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.SingletonImageLoader
import coil3.test.FakeImageLoaderEngine
import coil3.ImageLoader
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.DislikedSongsCache
import com.mediaplayer.android.data.LikedSongsCache
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.RecentsCache
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.theme.MediaPlayerTheme
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Base for screen tests. They run in the ordinary JVM suite under
 * Robolectric — no emulator, no instrumentation — which is possible because
 * every screen takes its ViewModel as a parameter and every ViewModel takes
 * its repositories, so a test can push a fake [com.mediaplayer.android.data.MediaPlayerApi]
 * through the real chain and assert on what renders.
 *
 * What these tests are for: state contracts and interaction wiring. Does an
 * empty list produce the empty state rather than a blank screen, does the
 * error branch offer a retry that actually retries, does tapping a row hand
 * back the right song. Not layout, not pixels — those need a device and
 * break on every design tweak.
 *
 * [Application] replaces `MediaPlayerApp` deliberately: the real one starts
 * auth, opens the write queue and spins up playback on create, none of which
 * a screen test should drag in.
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [34],
    application = Application::class,
    // Pin a phone-sized window. Compose skips composing what doesn't fit,
    // so on Robolectric's default tiny screen a LazyColumn would report far
    // fewer rows than the test asked for and assertions would drift.
    qualifiers = "w411dp-h891dp-xhdpi",
)
abstract class ScreenTest {

    @get:Rule
    val compose: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity> =
        createAndroidComposeRule<ComponentActivity>()

    /**
     * The backend, for every screen in the test. Everything reaches the
     * network through `Network.api`, so overriding it covers screens that
     * build their own ViewModel internally as well as those that take one
     * as a parameter.
     *
     * Relaxed: a screen touches more endpoints than any one test cares
     * about, and stubbing all of them would bury the case under setup.
     */
    lateinit var api: MediaPlayerApi
        private set

    /**
     * The app-wide preference singletons resolve their Context through
     * `MediaPlayerApp`. Point that at Robolectric's plain Application
     * instead, and swap the backend for a mock while we're here.
     */
    @Before
    fun wireAppSingletons() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        MediaPlayerApp.contextOverride = context
        ReadCache.init(context)
        // Mutations (like, dislike, playlist edits) are written through the
        // offline queue, so screens that mutate need it open.
        EventQueue.init(context)
        // The app-wide caches are objects, so state survives between tests
        // in the same JVM. Reset them or one test's fixture leaks into the
        // next one's assertions.
        runBlocking {
            ReadCache.clearAll()
            PlaylistsCache.clear()
        }
        LikedSongsCache.clear()
        DislikedSongsCache.clear()
        RecentsCache.clear()
        api = mockk(relaxed = true)
        Network.apiOverride = api
    }

    @After
    fun clearOverrides() {
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
    }

    /**
     * Cover art must never reach the network here. Coil's singleton is
     * swapped for an engine that answers every request with a solid colour,
     * which keeps the suite hermetic and fast.
     */
    @OptIn(ExperimentalCoilApi::class)
    @Before
    fun installFakeImageLoader() {
        val engine = FakeImageLoaderEngine.Builder()
            .default(ColorImage(0xFF444444.toInt()))
            .build()
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context).components { add(engine) }.build()
        }
    }

    /** Render [content] inside the app theme, as the activity would. */
    fun setScreen(content: @Composable () -> Unit) {
        compose.setContent { MediaPlayerTheme { content() } }
    }

    /**
     * Wait until [text] appears. Needed wherever a screen debounces or
     * hops threads before it can render — `waitForIdle` only settles
     * Compose's own work, and a ViewModel sitting on a `delay` is invisible
     * to it. Fails the test on timeout rather than asserting on a screen
     * that never got there.
     */
    fun awaitText(text: String, substring: Boolean = false, timeoutMs: Long = 5_000) {
        compose.waitUntil(timeoutMs) {
            compose.onAllNodes(hasText(text, substring = substring))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }
}

/** Fixture song. Fields are only interesting where a test says they are. */
fun song(
    id: Long,
    title: String = "Song $id",
    artist: String = "Artist $id",
    album: String? = "Album $id",
    durationMs: Long = 200_000L,
    hasCoverArt: Boolean = false,
    hasVideo: Boolean = false,
    playable: Boolean = true,
): SongDto = SongDto(
    id = id,
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    hasCoverArt = hasCoverArt,
    hasVideo = hasVideo,
    playable = playable,
)

fun album(
    name: String,
    artist: String = "Artist",
    songCount: Int = 10,
    totalDurationMs: Long = 2_000_000L,
    year: Int? = 2020,
    coverSongId: Long? = null,
): AlbumDto = AlbumDto(
    name = name,
    artist = artist,
    songCount = songCount,
    totalDurationMs = totalDurationMs,
    year = year,
    coverSongId = coverSongId,
)

fun artist(
    name: String,
    albumCount: Int = 3,
    songCount: Int = 30,
    coverSongId: Long? = null,
): ArtistDto = ArtistDto(
    name = name,
    albumCount = albumCount,
    songCount = songCount,
    coverSongId = coverSongId,
)

fun playlist(
    id: Long,
    name: String = "Playlist $id",
    songCount: Int = 5,
    kind: String = "USER",
    coverSongId: Long? = null,
): PlaylistDto = PlaylistDto(
    id = id,
    name = name,
    songCount = songCount,
    createdAt = "2026-01-01T00:00:00",
    updatedAt = "2026-01-01T00:00:00",
    coverSongId = coverSongId,
    kind = kind,
)

/** Single-page response holding [items], as a backend with nothing more would send. */
fun <T> onePage(items: List<T>, totalItems: Long = items.size.toLong()): PageResponse<T> =
    PageResponse(
        items = items,
        page = 0,
        size = items.size.coerceAtLeast(1),
        totalItems = totalItems,
        totalPages = 1,
    )
