package com.mediaplayer.android.data

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.ReadCache
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
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

/**
 * The playlist cache is what every screen reads, so a mutation that reaches
 * the backend but not the cache shows up as an edit that "didn't work" until
 * the next refresh — and one that reaches the cache but not the backend shows
 * up as an edit that comes back from the dead.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class PlaylistsCacheTest {

    private lateinit var api: MediaPlayerApi

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        MediaPlayerApp.contextOverride = context
        ReadCache.init(context)
        EventQueue.init(context)
        api = mockk(relaxed = true)
        Network.apiOverride = api
        runBlocking {
            ReadCache.clearAll()
            PlaylistsCache.clear()
        }
    }

    @After
    fun tearDown() {
        runBlocking { PlaylistsCache.clear() }
        Network.apiOverride = null
        MediaPlayerApp.contextOverride = null
    }

    private fun playlist(id: Long, name: String = "Corsa", songCount: Int = 3) = PlaylistDto(
        id = id,
        name = name,
        songCount = songCount,
        createdAt = "2026-01-01T00:00:00",
        updatedAt = "2026-01-01T00:00:00",
    )

    private fun detail(id: Long, name: String = "Corsa") = PlaylistDetailDto(
        id = id,
        name = name,
        createdAt = "2026-01-01T00:00:00",
        updatedAt = "2026-01-01T00:00:00",
        songs = emptyList(),
    )

    private fun stubList(vararg items: PlaylistDto) {
        coEvery { api.listPlaylists(kind = "auto") } returns emptyList()
        coEvery { api.listPlaylists(kind = null) } returns items.toList()
    }

    @Test
    fun `a refresh publishes what the backend has`() = runBlocking {
        stubList(playlist(1L, "Corsa"), playlist(2L, "Studio"))

        PlaylistsCache.refresh()

        assertEquals(listOf("Corsa", "Studio"), PlaylistsCache.playlists.value.map { it.name })
    }

    @Test
    fun `creating one adds it without a second round trip`() = runBlocking {
        stubList()
        PlaylistsCache.refresh()
        coEvery { api.createPlaylist(any()) } returns playlist(9L, "Nuova", songCount = 0)

        PlaylistsCache.create("Nuova")

        assertEquals(listOf("Nuova"), PlaylistsCache.playlists.value.map { it.name })
    }

    @Test
    fun `deleting removes it from the list every screen reads`() = runBlocking {
        stubList(playlist(1L, "Corsa"), playlist(2L, "Studio"))
        PlaylistsCache.refresh()

        PlaylistsCache.delete(1L)

        assertEquals(listOf("Studio"), PlaylistsCache.playlists.value.map { it.name })
    }

    @Test
    fun `renaming shows the new name straight away`() = runBlocking {
        stubList(playlist(1L, "Corsa"))
        PlaylistsCache.refresh()
        coEvery { api.renamePlaylist(any(), any()) } returns playlist(1L, "Corsa lunga")

        PlaylistsCache.rename(1L, "Corsa lunga")

        assertEquals(listOf("Corsa lunga"), PlaylistsCache.playlists.value.map { it.name })
    }

    @Test
    fun `toggling auto-sync keeps the flag on the cached row`() = runBlocking {
        stubList(playlist(1L, "Corsa"))
        PlaylistsCache.refresh()
        coEvery { api.setPlaylistAutoSync(any(), any()) } returns
            playlist(1L, "Corsa").copy(autoSync = true)

        PlaylistsCache.setAutoSync(1L, true)

        assertTrue(PlaylistsCache.playlists.value.single().autoSync)
    }

    /**
     * The song count on the tile is what the user checks after adding
     * something, so it has to move with the edit rather than wait for the
     * next full refresh.
     */
    @Test
    fun `adding a song bumps the count on the tile`() = runBlocking {
        stubList(playlist(1L, "Corsa", songCount = 3))
        PlaylistsCache.refresh()
        coEvery { api.addSongToPlaylist(any(), any()) } returns
            detail(1L).copy(songs = List(4) { mockk(relaxed = true) })

        PlaylistsCache.addSong(1L, 99L)

        assertEquals(4, PlaylistsCache.playlists.value.single().songCount)
    }

    @Test
    fun `removing a song lowers the count on the tile`() = runBlocking {
        stubList(playlist(1L, "Corsa", songCount = 3))
        PlaylistsCache.refresh()
        coEvery { api.removeSongFromPlaylist(any(), any()) } returns
            detail(1L).copy(songs = List(2) { mockk(relaxed = true) })

        PlaylistsCache.removeSong(1L, 99L)

        assertEquals(2, PlaylistsCache.playlists.value.single().songCount)
    }

    /**
     * The kebab shows a tick against the playlists a song is already in, and
     * that answer comes from the cached details rather than a request per
     * playlist.
     */
    @Test
    fun `the cache can say which playlists hold a song`() = runBlocking {
        stubList(playlist(1L, "Corsa"))
        PlaylistsCache.refresh()
        val entry = mockk<com.mediaplayer.android.data.dto.PlaylistSongEntryDto>(relaxed = true)
        coEvery { entry.song } returns
            com.mediaplayer.android.data.dto.SongDto(
                id = 99L, title = "Breed", artist = "Nirvana", album = null,
                durationMs = 1L, hasCoverArt = false,
            )
        coEvery { api.getPlaylist(1L) } returns detail(1L).copy(songs = listOf(entry))
        PlaylistsCache.refreshDetail(1L)

        assertEquals(setOf(1L), PlaylistsCache.playlistIdsContaining(99L))
        assertTrue(PlaylistsCache.playlistIdsContaining(1234L).isEmpty())
    }

    @Test
    fun `clearing empties the cache`() = runBlocking {
        stubList(playlist(1L, "Corsa"))
        PlaylistsCache.refresh()

        PlaylistsCache.clear()

        assertTrue(PlaylistsCache.playlists.value.isEmpty())
    }

    /** A deleted playlist must not leave its detail behind for a stale read. */
    @Test
    fun `deleting drops the cached detail too`() = runBlocking {
        stubList(playlist(1L, "Corsa"))
        PlaylistsCache.refresh()
        coEvery { api.getPlaylist(1L) } returns detail(1L)
        PlaylistsCache.refreshDetail(1L)

        PlaylistsCache.delete(1L)

        assertTrue(PlaylistsCache.playlistIdsContaining(99L).isEmpty())
        assertFalse(PlaylistsCache.playlists.value.any { it.id == 1L })
        assertNull(PlaylistsCache.playlists.value.firstOrNull { it.id == 1L })
    }
}
