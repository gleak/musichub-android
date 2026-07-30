package com.mediaplayer.android.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.sync.EventQueue
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The liked cache is what every heart in the app renders from, and it applies
 * the user's tap optimistically before the backend has agreed. These tests pin
 * the optimistic contract: the tap is visible immediately, it survives being
 * asked again, and signing out leaves nothing of the previous user behind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LikedSongsCacheTest {

    private lateinit var api: MediaPlayerApi

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        Network.apiOverride = api
        // A toggle enqueues the change locally, and the cache treats a failure
        // to enqueue as a reason to undo the optimistic update. Without a real
        // queue behind it every toggle would silently revert here.
        EventQueue.init(ApplicationProvider.getApplicationContext())
        LikedSongsCache.clear()
    }

    @After
    fun tearDown() {
        LikedSongsCache.clear()
        Network.apiOverride = null
    }

    @Test
    fun `a fresh cache likes nothing`() {
        assertFalse(LikedSongsCache.isLiked(1L))
        assertTrue(LikedSongsCache.likedIds.value.isEmpty())
    }

    @Test
    fun `toggling on is visible immediately`() {
        val nowLiked = LikedSongsCache.toggle(42L)

        assertTrue("toggle reports the new state", nowLiked)
        assertTrue(LikedSongsCache.isLiked(42L))
        assertTrue(42L in LikedSongsCache.likedIds.value)
    }

    @Test
    fun `toggling twice returns to the starting state`() {
        LikedSongsCache.toggle(42L)

        val nowLiked = LikedSongsCache.toggle(42L)

        assertFalse(nowLiked)
        assertFalse(LikedSongsCache.isLiked(42L))
    }

    @Test
    fun `markLiked sets an absolute state rather than flipping`() {
        LikedSongsCache.markLiked(7L, true)
        LikedSongsCache.markLiked(7L, true)

        assertTrue("repeated marks must not cancel out", LikedSongsCache.isLiked(7L))

        LikedSongsCache.markLiked(7L, false)

        assertFalse(LikedSongsCache.isLiked(7L))
    }

    @Test
    fun `several songs are tracked independently`() {
        LikedSongsCache.toggle(1L)
        LikedSongsCache.toggle(2L)
        LikedSongsCache.toggle(1L)

        assertFalse(LikedSongsCache.isLiked(1L))
        assertTrue(LikedSongsCache.isLiked(2L))
    }

    /**
     * A tap the user has already made must not be undone by a later server
     * read — that is what made hearts flicker back off after a refresh.
     */
    @Test
    fun `priming does not overwrite a song the user just toggled`() = runBlocking {
        LikedSongsCache.toggle(5L)
        coEvery { api.getLikedStatus(any()) } returns emptyList()

        LikedSongsCache.prime(listOf(5L))

        assertTrue("the user's own tap was reverted by a server read", LikedSongsCache.isLiked(5L))
    }

    @Test
    fun `priming an empty collection is a no-op`() = runBlocking {
        LikedSongsCache.toggle(5L)

        LikedSongsCache.prime(emptyList())

        assertTrue(LikedSongsCache.isLiked(5L))
    }

    /** Local tracks carry negative ids and have no backend liked state. */
    @Test
    fun `priming ignores non-positive ids`() = runBlocking {
        LikedSongsCache.prime(listOf(0L, -1L, -2L))

        assertFalse(LikedSongsCache.isLiked(-1L))
    }

    /**
     * Now reachable because the repositories resolve their client per call
     * instead of capturing it at construction — the cache builds its own
     * repository inside an object initialiser, so a captured client could
     * never be redirected.
     */
    @Test
    fun `the server's answer applies to songs the user has not touched`() = runBlocking {
        coEvery { api.getLikedStatus(any()) } returns listOf(8L)

        LikedSongsCache.prime(listOf(8L, 9L))

        assertTrue("the server said 8 is liked", LikedSongsCache.isLiked(8L))
        assertFalse("the server did not mention 9", LikedSongsCache.isLiked(9L))
    }

    @Test
    fun `a failing status call leaves the cache untouched`() = runBlocking {
        coEvery { api.getLikedStatus(any()) } throws java.io.IOException("offline")

        LikedSongsCache.prime(listOf(11L))

        assertFalse(LikedSongsCache.isLiked(11L))
    }

    @Test
    fun `clear empties the cache`() {
        LikedSongsCache.toggle(1L)
        LikedSongsCache.toggle(2L)

        LikedSongsCache.clear()

        assertTrue(LikedSongsCache.likedIds.value.isEmpty())
    }

    @Test
    fun `the flow reflects every change`() {
        val seen = mutableListOf<Int>()
        seen += LikedSongsCache.likedIds.value.size

        LikedSongsCache.toggle(1L)
        seen += LikedSongsCache.likedIds.value.size
        LikedSongsCache.toggle(2L)
        seen += LikedSongsCache.likedIds.value.size
        LikedSongsCache.toggle(1L)
        seen += LikedSongsCache.likedIds.value.size

        assertEquals(listOf(0, 1, 2, 1), seen)
    }

    private fun song(id: Long) = SongDto(
        id = id,
        title = "Brano $id",
        artist = "Artista",
        album = null,
        durationMs = 1000L,
        hasCoverArt = false,
    )
}
