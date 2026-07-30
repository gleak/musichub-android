package com.mediaplayer.android.playback

import android.app.Application
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.AlbumDetailDto
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.ArtistDetailDto
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.data.dto.PageResponse
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.data.dto.PlaylistSongEntryDto
import com.mediaplayer.android.data.dto.SongDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The browse tree with content in it. The existing suite pins the shape of
 * the tree — what is browsable, what pages terminate — against an empty
 * backend; this drills into the folders with songs actually in them.
 *
 * The leaf ids are the part that matters. A car tap sends back an id and
 * nothing else, so an id has to carry enough to rebuild the queue: which
 * collection, which position in it, and which song. Names go through the
 * id too, which is why the ones with separators in them are here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class LibraryTreeContentTest {

    private lateinit var api: MediaPlayerApi

    @Before
    fun setUp() {
        api = mockk(relaxed = true)
        Network.apiOverride = api
    }

    @After
    fun tearDown() {
        Network.apiOverride = null
    }

    private fun song(id: Long, title: String = "Song $id", playable: Boolean = true) = SongDto(
        id = id,
        title = title,
        artist = "Nirvana",
        album = "Nevermind",
        durationMs = 200_000L,
        hasCoverArt = false,
        playable = playable,
    )

    private fun entry(song: SongDto) =
        PlaylistSongEntryDto(playlistSongId = song.id * 10, song = song)

    private fun <T> page(items: List<T>) = PageResponse(
        items = items,
        page = 0,
        size = items.size.coerceAtLeast(1),
        totalItems = items.size.toLong(),
        totalPages = 1,
    )

    // ---------- playlists ----------

    @Test
    fun `a playlist folder lists its songs as playable leaves`() = runBlocking {
        coEvery { api.getPlaylist(42L) } returns PlaylistDetailDto(
            id = 42L,
            name = "Corsa",
            createdAt = "2026-01-01T00:00:00",
            updatedAt = "2026-01-01T00:00:00",
            songs = listOf(entry(song(1L, "Breed")), entry(song(2L, "Lithium"))),
        )

        val rows = LibraryTree.children("playlist:42", page = 0, pageSize = 20).orEmpty()

        assertEquals(listOf("Breed", "Lithium"), rows.map { it.mediaMetadata.title.toString() })
        assertTrue(rows.all { it.mediaMetadata.isPlayable == true })
    }

    /**
     * The same song can sit in a playlist twice, so the id has to carry the
     * position — otherwise a tap on the second copy starts the first.
     */
    @Test
    fun `a playlist leaf carries its position, not just the song`() = runBlocking {
        val breed = song(1L, "Breed")
        coEvery { api.getPlaylist(42L) } returns PlaylistDetailDto(
            id = 42L,
            name = "Corsa",
            createdAt = "2026-01-01T00:00:00",
            updatedAt = "2026-01-01T00:00:00",
            songs = listOf(entry(breed), entry(breed)),
        )

        val rows = LibraryTree.children("playlist:42", page = 0, pageSize = 20).orEmpty()

        val second = LibraryTree.parsePlaylistLeaf(rows[1].mediaId)
        assertEquals(42L, second?.first)
        assertEquals(1, second?.second)
        assertEquals(1L, second?.third)
    }

    @Test
    fun `playlist rows are numbered for the head unit`() = runBlocking {
        coEvery { api.getPlaylist(42L) } returns PlaylistDetailDto(
            id = 42L,
            name = "Corsa",
            createdAt = "2026-01-01T00:00:00",
            updatedAt = "2026-01-01T00:00:00",
            songs = listOf(entry(song(1L)), entry(song(2L))),
        )

        val rows = LibraryTree.children("playlist:42", page = 0, pageSize = 20).orEmpty()

        assertEquals(1, rows[0].mediaMetadata.trackNumber)
        assertEquals(2, rows[1].mediaMetadata.totalTrackCount)
    }

    /** The backend marks a song unplayable when its audio file is missing. */
    @Test
    fun `a song with no audio is shown but not playable`() = runBlocking {
        coEvery { api.getPlaylist(42L) } returns PlaylistDetailDto(
            id = 42L,
            name = "Corsa",
            createdAt = "2026-01-01T00:00:00",
            updatedAt = "2026-01-01T00:00:00",
            songs = listOf(entry(song(1L, "Breed", playable = false))),
        )

        val rows = LibraryTree.children("playlist:42", page = 0, pageSize = 20).orEmpty()

        assertEquals(false, rows.single().mediaMetadata.isPlayable)
    }

    @Test
    fun `the playlists folder tiles every playlist`() = runBlocking {
        coEvery { api.listPlaylists() } returns listOf(
            PlaylistDto(
                id = 42L, name = "Corsa", songCount = 5,
                createdAt = "2026-01-01T00:00:00", updatedAt = "2026-01-01T00:00:00",
            ),
        )

        val rows = LibraryTree.children(LibraryTree.PLAYLISTS_ID, page = 0, pageSize = 20).orEmpty()

        assertEquals("Corsa", rows.single().mediaMetadata.title.toString())
        assertEquals(true, rows.single().mediaMetadata.isBrowsable)
    }

    @Test
    fun `the made-for-you folder asks for the auto playlists`() = runBlocking {
        coEvery { api.listPlaylists(kind = "auto") } returns listOf(
            PlaylistDto(
                id = 7L, name = "Discover Daily", songCount = 30,
                createdAt = "2026-01-01T00:00:00", updatedAt = "2026-01-01T00:00:00",
                kind = "auto",
            ),
        )

        val rows = LibraryTree.children(LibraryTree.MADE_FOR_YOU_ID, page = 0, pageSize = 20)
            .orEmpty()

        assertEquals("Discover Daily", rows.single().mediaMetadata.title.toString())
    }

    // ---------- albums ----------

    @Test
    fun `an album folder lists its songs`() = runBlocking {
        coEvery { api.getAlbum("Nevermind", "Nirvana") } returns AlbumDetailDto(
            name = "Nevermind",
            artist = "Nirvana",
            songs = listOf(song(1L, "Breed"), song(2L, "Lithium")),
        )

        val rows = LibraryTree.children("album:Nevermind|Nirvana", page = 0, pageSize = 20)
            .orEmpty()

        assertEquals(listOf("Breed", "Lithium"), rows.map { it.mediaMetadata.title.toString() })
    }

    /**
     * The album key packs a name and an artist into one id with a separator
     * between them, so a name containing that separator has to survive.
     */
    @Test
    fun `an album leaf round-trips a name containing the separator`() = runBlocking {
        coEvery { api.getAlbum(any(), any()) } returns AlbumDetailDto(
            name = "Weird|Album",
            artist = "Odd|Artist",
            songs = listOf(song(1L, "Breed")),
        )

        val rows = LibraryTree.children("album:Nevermind|Nirvana", page = 0, pageSize = 20)
            .orEmpty()

        val parsed = LibraryTree.parseAlbumLeaf(rows.single().mediaId)
        assertEquals("Weird|Album", parsed?.a)
        assertEquals("Odd|Artist", parsed?.b)
        assertEquals(0, parsed?.c)
        assertEquals(1L, parsed?.d)
    }

    @Test
    fun `an album id that does not decode yields nothing`() = runBlocking {
        val rows = LibraryTree.children("album:no-separator-here", page = 0, pageSize = 20)

        assertTrue(rows.isNullOrEmpty())
    }

    @Test
    fun `the albums folder tiles what the backend returns`() = runBlocking {
        coEvery { api.listAlbums(any(), any(), any()) } returns page(
            listOf(
                AlbumDto(name = "Nevermind", artist = "Nirvana", songCount = 12,
                    totalDurationMs = 2_000_000L, year = 1991, coverSongId = 1L),
            ),
        )

        val rows = LibraryTree.children(LibraryTree.ALBUMS_ID, page = 0, pageSize = 20).orEmpty()

        assertEquals("Nevermind", rows.single().mediaMetadata.title.toString())
        assertEquals(true, rows.single().mediaMetadata.isBrowsable)
    }

    // ---------- artists ----------

    /** An artist folder is albums first, then loose songs. */
    @Test
    fun `an artist folder offers albums above songs`() = runBlocking {
        coEvery { api.getArtist("Nirvana") } returns ArtistDetailDto(
            name = "Nirvana",
            albums = listOf(
                AlbumDto(name = "Nevermind", artist = "Nirvana", songCount = 12,
                    totalDurationMs = 2_000_000L, year = 1991, coverSongId = 1L),
            ),
            songs = listOf(song(1L, "Breed")),
        )

        val rows = LibraryTree.children("artist:Nirvana", page = 0, pageSize = 20).orEmpty()

        assertEquals(true, rows.first().mediaMetadata.isBrowsable)
        assertEquals("Breed", rows.last().mediaMetadata.title.toString())
        assertEquals(true, rows.last().mediaMetadata.isPlayable)
    }

    @Test
    fun `an artist leaf round-trips a name with a slash in it`() = runBlocking {
        coEvery { api.getArtist(any()) } returns ArtistDetailDto(
            name = "AC/DC",
            albums = emptyList(),
            songs = listOf(song(1L, "Breed")),
        )

        val rows = LibraryTree.children("artist:AC%2FDC", page = 0, pageSize = 20).orEmpty()

        val parsed = LibraryTree.parseArtistLeaf(rows.single().mediaId)
        assertEquals("AC/DC", parsed?.first)
        assertEquals(0, parsed?.second)
        assertEquals(1L, parsed?.third)
    }

    @Test
    fun `the artists folder tiles what the backend returns`() = runBlocking {
        coEvery { api.listArtists(any(), any()) } returns page(
            listOf(ArtistDto(name = "Nirvana", albumCount = 3, songCount = 30, coverSongId = 1L)),
        )

        val rows = LibraryTree.children(LibraryTree.ARTISTS_ID, page = 0, pageSize = 20).orEmpty()

        assertEquals("Nirvana", rows.single().mediaMetadata.title.toString())
    }

    // ---------- genres ----------

    @Test
    fun `a genre folder asks the backend for that tag`() = runBlocking {
        coEvery { api.listSongs(query = null, genre = "rock", page = any(), size = any()) } returns
            page(listOf(song(1L, "Breed")))

        val rows = LibraryTree.children("genre:rock", page = 0, pageSize = 20).orEmpty()

        assertEquals("Breed", rows.single().mediaMetadata.title.toString())
    }

    @Test
    fun `a genre leaf carries its tag and position`() = runBlocking {
        coEvery { api.listSongs(query = null, genre = "rock", page = any(), size = any()) } returns
            page(listOf(song(1L), song(2L)))

        val rows = LibraryTree.children("genre:rock", page = 0, pageSize = 20).orEmpty()

        val parsed = LibraryTree.parseGenreLeaf(rows[1].mediaId)
        assertEquals("rock", parsed?.first)
        assertEquals(1, parsed?.second)
        assertEquals(2L, parsed?.third)
    }

    @Test
    fun `a genre with nothing in it explains itself`() = runBlocking {
        coEvery { api.listSongs(query = null, genre = "polka", page = any(), size = any()) } returns
            page(emptyList())

        val rows = LibraryTree.children("genre:polka", page = 0, pageSize = 20).orEmpty()

        assertEquals("Nessun brano per questo genere", rows.single().mediaMetadata.title.toString())
    }

    // ---------- recents ----------

    @Test
    fun `recents come back as playable songs`() = runBlocking {
        coEvery { api.recentSongs(any()) } returns listOf(song(1L, "Breed"))

        val rows = LibraryTree.children(LibraryTree.RECENTS_ID, page = 0, pageSize = 20).orEmpty()

        assertEquals("Breed", rows.single().mediaMetadata.title.toString())
        assertEquals(true, rows.single().mediaMetadata.isPlayable)
    }

    @Test
    fun `nothing played yet says so rather than showing an empty folder`() = runBlocking {
        coEvery { api.recentSongs(any()) } returns emptyList()

        val rows = LibraryTree.children(LibraryTree.RECENTS_ID, page = 0, pageSize = 20).orEmpty()

        assertEquals("Ancora nessun ascolto", rows.single().mediaMetadata.title.toString())
    }

    // ---------- resolving a tapped id back to audio ----------

    /**
     * A tapped browse row resolves to the canonical playable form — the
     * `song:{id}` shape the rest of the playback layer understands.
     */
    @Test
    fun `a leaf id resolves to the song it points at`() = runBlocking {
        coEvery { api.getPlaylist(42L) } returns PlaylistDetailDto(
            id = 42L,
            name = "Corsa",
            createdAt = "2026-01-01T00:00:00",
            updatedAt = "2026-01-01T00:00:00",
            songs = listOf(entry(song(1L, "Breed"))),
        )
        val leafId = LibraryTree.children("playlist:42", page = 0, pageSize = 20)
            .orEmpty().single().mediaId

        val item = LibraryTree.item(leafId)

        assertEquals("song:1", item?.mediaId)
        assertEquals(true, item?.mediaMetadata?.isPlayable)
    }

    @Test
    fun `an id from nowhere resolves to nothing`() = runBlocking {
        assertNull(LibraryTree.item("pl:not-an-id"))
    }
}
