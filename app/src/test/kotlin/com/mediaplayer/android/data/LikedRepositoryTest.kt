package com.mediaplayer.android.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/**
 * The liked repository is the app's offline story for the screen users open
 * most. Page 0 is mirrored into the read cache so it survives a cold start with
 * no network; the rest is live. These tests pin which half is which, because
 * getting it wrong shows either a stale list or an error screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LikedRepositoryTest {

    private lateinit var api: MediaPlayerApi
    private lateinit var repository: LikedRepository

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        repository = LikedRepository(api)
        ReadCache.init(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `the first page is served from the network when it answers`() = runBlocking {
        coEvery { api.getLikedSongs(0, 20) } returns page(songs(1L, 2L))

        val result = repository.likedSongs(page = 0, size = 20)

        assertEquals(listOf(1L, 2L), result.items.map { it.id })
    }

    /**
     * Only page 0 is cached: it is what the screen lands on, and caching deeper
     * pages would let the user scroll into a snapshot that no longer lines up
     * with the live list.
     */
    @Test
    fun `a later page goes straight to the network with no caching`() = runBlocking {
        coEvery { api.getLikedSongs(2, 20) } returns page(songs(9L), page = 2)

        val result = repository.likedSongs(page = 2, size = 20)

        assertEquals(listOf(9L), result.items.map { it.id })
        coVerify(exactly = 1) { api.getLikedSongs(2, 20) }
    }

    /** Offline on the landing page falls back to the last snapshot instead of erroring. */
    @Test
    fun `the first page falls back to the cache when the network is down`() = runBlocking {
        coEvery { api.getLikedSongs(0, 20) } returns page(songs(1L, 2L))
        repository.likedSongs(page = 0, size = 20)

        coEvery { api.getLikedSongs(0, 20) } throws IOException("offline")
        val cached = repository.likedSongs(page = 0, size = 20)

        assertEquals(listOf(1L, 2L), cached.items.map { it.id })
    }

    /** With nothing cached there is nothing to fall back to, and the caller must know. */
    @Test
    fun `an offline first page with no cache surfaces the failure`() = runBlocking {
        ReadCache.clearAll()
        coEvery { api.getLikedSongs(0, 20) } throws IOException("offline")

        assertThrows(IOException::class.java) {
            runBlocking { repository.likedSongs(page = 0, size = 20) }
        }
        Unit
    }

    /** A deep page has no cache by design, so its failure always propagates. */
    @Test
    fun `an offline later page surfaces the failure`() = runBlocking {
        coEvery { api.getLikedSongs(3, 20) } throws IOException("offline")

        assertThrows(IOException::class.java) {
            runBlocking { repository.likedSongs(page = 3, size = 20) }
        }
        Unit
    }

    @Test
    fun `status resolves the ids the backend reports as liked`() = runBlocking {
        coEvery { api.getLikedStatus(listOf(1L, 2L, 3L)) } returns listOf(1L, 3L)

        val liked = repository.status(listOf(1L, 2L, 3L))

        assertEquals(setOf(1L, 3L), liked)
    }

    @Test
    fun `status of an empty answer is an empty set`() = runBlocking {
        coEvery { api.getLikedStatus(any()) } returns emptyList()

        assertEquals(emptySet<Long>(), repository.status(listOf(1L)))
    }

    @Test
    fun `shuffle-all pulls the whole collection uncached`() = runBlocking {
        coEvery { api.getAllLikedSongs() } returns songs(1L, 2L, 3L)

        val all = repository.allLikedSongs()

        assertEquals(3, all.size)
        coVerify(exactly = 1) { api.getAllLikedSongs() }
    }

    private fun song(id: Long) = SongDto(
        id = id,
        title = "Brano $id",
        artist = "Artista",
        album = null,
        durationMs = 1000L,
        hasCoverArt = false,
    )

    private fun songs(vararg ids: Long) = ids.map { song(it) }

    private fun page(items: List<SongDto>, page: Int = 0) = PageResponse(
        items = items,
        page = page,
        size = 20,
        totalItems = items.size.toLong(),
        totalPages = 1,
    )
}
