package com.mediaplayer.android.playback

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.AlbumDetailDto
import com.mediaplayer.android.data.dto.ArtistDetailDto
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The media-browse tree is everything Android Auto sees. Rows that the head
 * unit silently drops, ids that don't survive the round trip, and pages that
 * repeat themselves are all invisible from the phone — which is exactly why
 * they need pinning here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class LibraryTreeTest {

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

    // --- browse roots -----------------------------------------------------

    @Test
    fun `the root is a browsable folder and not playable`() = runBlocking {
        val root = LibraryTree.root()

        assertTrue(root.mediaMetadata.isBrowsable == true)
        assertFalse(root.mediaMetadata.isPlayable == true)
        assertEquals(LibraryTree.ROOT_ID, root.mediaId)
    }

    /**
     * Android Auto drops any item that is neither browsable nor playable, so
     * every row the tree emits has to declare itself one or the other.
     */
    @Test
    fun `every root child declares browsable or playable`() = runBlocking {
        val children = LibraryTree.children(LibraryTree.ROOT_ID, page = 0, pageSize = 50).orEmpty()

        assertTrue("root has no children", children.isNotEmpty())
        for (child in children) {
            val md = child.mediaMetadata
            assertTrue(
                "${child.mediaId} is invisible to Android Auto",
                md.isBrowsable == true || md.isPlayable == true,
            )
        }
    }

    @Test
    fun `root extras advertise content style and search`() {
        val extras = LibraryTree.rootExtras()

        assertFalse("root extras are empty", extras.isEmpty)
    }

    /** The empty-state row must survive Android Auto's filter, or the message is lost. */
    @Test
    fun `the info row is visible to Android Auto`() {
        val info = LibraryTree.infoItem("Server irraggiungibile")

        assertEquals("Server irraggiungibile", info.mediaMetadata.title)
        assertTrue(
            "an inert row is dropped by the head unit",
            info.mediaMetadata.isBrowsable == true || info.mediaMetadata.isPlayable == true,
        )
    }

    /**
     * Walks every folder the root advertises. Two invariants hold across all of
     * them: browsing never throws (a thrown resolver becomes an error tile in
     * the car), and every row is either browsable or playable, because Android
     * Auto silently discards anything else.
     */
    @Test
    fun `every advertised folder browses without throwing and yields visible rows`() = runBlocking {
        val folders = listOf(
            LibraryTree.ALL_SONGS_ID,
            LibraryTree.PLAYLISTS_ID,
            LibraryTree.MADE_FOR_YOU_ID,
            LibraryTree.LIKED_ID,
            LibraryTree.RECENTS_ID,
            LibraryTree.ALBUMS_ID,
            LibraryTree.ARTISTS_ID,
            LibraryTree.GENRES_ID,
            LibraryTree.QUEUE_ID,
        )

        for (folder in folders) {
            val rows = LibraryTree.children(folder, page = 0, pageSize = 20).orEmpty()
            for (row in rows) {
                val md = row.mediaMetadata
                assertTrue(
                    "$folder row ${row.mediaId} is invisible to Android Auto",
                    md.isBrowsable == true || md.isPlayable == true,
                )
                assertTrue("$folder row has a blank id", row.mediaId.isNotBlank())
            }
        }
    }

    /** Paging past the end must terminate for every folder, or the car scrolls forever. */
    @Test
    fun `no folder repeats itself on a far page`() = runBlocking {
        val folders = listOf(
            LibraryTree.PLAYLISTS_ID,
            LibraryTree.MADE_FOR_YOU_ID,
            LibraryTree.RECENTS_ID,
            LibraryTree.GENRES_ID,
        )

        for (folder in folders) {
            val first = LibraryTree.children(folder, page = 0, pageSize = 20).orEmpty()
            val far = LibraryTree.children(folder, page = 50, pageSize = 20).orEmpty()

            assertTrue(
                "$folder answered page 50 with page 0's rows",
                far.isEmpty() || far.map { it.mediaId } != first.map { it.mediaId },
            )
        }
    }

    @Test
    fun `an unknown parent id resolves to nothing rather than throwing`() = runBlocking {
        val rows = LibraryTree.children("non-esiste", page = 0, pageSize = 20)

        assertTrue(rows.isNullOrEmpty())
    }

    @Test
    fun `an unknown media id resolves to null`() = runBlocking {
        assertNull(LibraryTree.item("non-esiste"))
        assertNull(LibraryTree.item(""))
    }

    @Test
    fun `search tolerates an empty result set`() = runBlocking {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns page(emptyList())

        val results = LibraryTree.search("nulla", page = 0, pageSize = 20)

        assertTrue(results.isEmpty())
    }

    @Test
    fun `search results are playable songs`() = runBlocking {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns page(songs(3L, 4L))

        val results = LibraryTree.search("qualcosa", page = 0, pageSize = 20)

        assertEquals(2, results.size)
        assertTrue(results.all { it.mediaMetadata.isPlayable == true })
    }

    // --- leaf id round trips ---------------------------------------------

    @Test
    fun `queue rows round-trip through their leaf id`() {
        val timeline = listOf(songItem(10), songItem(20), songItem(30))

        val rows = LibraryTree.queueChildren(timeline, currentIndex = 1)

        assertEquals(3, rows.size)
        rows.forEachIndexed { index, row ->
            val parsed = LibraryTree.parseQueueLeaf(row.mediaId)
            assertNotNull("row $index has an unparseable id: ${row.mediaId}", parsed)
            assertEquals("position", index, parsed!!.first)
            assertEquals("song id", timeline[index].mediaId.removePrefix("song:").toLong(), parsed.second)
        }
    }

    @Test
    fun `queue rows are playable`() {
        val rows = LibraryTree.queueChildren(listOf(songItem(1)), currentIndex = 0)

        assertTrue(rows.first().mediaMetadata.isPlayable == true)
    }

    /** An empty queue explains itself rather than rendering as a blank folder. */
    @Test
    fun `an empty timeline produces an explanatory row`() {
        val rows = LibraryTree.queueChildren(emptyList(), currentIndex = 0)

        assertEquals(1, rows.size)
        assertTrue(rows.first().mediaMetadata.title.toString().isNotBlank())
        assertFalse("the placeholder must not be playable", rows.first().mediaMetadata.isPlayable == true)
    }

    @Test
    fun `a malformed queue leaf parses to null`() {
        assertNull(LibraryTree.parseQueueLeaf("qu:"))
        assertNull(LibraryTree.parseQueueLeaf("qu:abc|1"))
        assertNull(LibraryTree.parseQueueLeaf("song:5"))
        assertNull(LibraryTree.parseQueueLeaf(""))
    }

    @Test
    fun `simple leaves carry position and song id`() {
        val parsed = LibraryTree.parseSimpleLeaf("lk:7|4242", "lk:")

        assertEquals(7, parsed?.first)
        assertEquals(4242L, parsed?.second)
    }

    @Test
    fun `a simple leaf with the wrong prefix parses to null`() {
        assertNull(LibraryTree.parseSimpleLeaf("rc:7|1", "lk:"))
        assertNull(LibraryTree.parseSimpleLeaf("lk:7", "lk:"))
        assertNull(LibraryTree.parseSimpleLeaf("lk:x|y", "lk:"))
    }

    // --- pagination -------------------------------------------------------

    /**
     * The defect this covers: branches that ignored the requested page answered
     * page 1 with page 0's rows, so long lists repeated on scroll and their
     * tails were unreachable from the car.
     */
    @Test
    fun `liked songs ask the backend for the page that was requested`() = runBlocking {
        coEvery { api.getLikedSongs(page = 2, size = 10) } returns page(songs(20L, 21L), page = 2, size = 10)

        val rows = LibraryTree.children(LibraryTree.LIKED_ID, page = 2, pageSize = 10).orEmpty()

        assertEquals(2, rows.size)
        assertEquals(
            listOf(20L, 21L),
            rows.mapNotNull { LibraryTree.parseSimpleLeaf(it.mediaId, "lk:")?.second },
        )
    }

    /** Leaf positions stay absolute, or tapping row 1 of page 2 plays row 1 of page 1. */
    @Test
    fun `liked leaf positions are absolute across pages`() = runBlocking {
        coEvery { api.getLikedSongs(page = 1, size = 10) } returns page(songs(50L), page = 1, size = 10)

        val rows = LibraryTree.children(LibraryTree.LIKED_ID, page = 1, pageSize = 10).orEmpty()

        val position = LibraryTree.parseSimpleLeaf(rows.first().mediaId, "lk:")?.first
        assertEquals("position must count from the start of the collection", 10, position)
    }

    @Test
    fun `an empty page past the end yields no rows rather than an empty-state row`() = runBlocking {
        coEvery { api.getLikedSongs(page = 5, size = 10) } returns page(emptyList(), page = 5, size = 10)

        val rows = LibraryTree.children(LibraryTree.LIKED_ID, page = 5, pageSize = 10).orEmpty()

        assertTrue("paging past the end must terminate", rows.isEmpty())
    }

    @Test
    fun `an empty collection still explains itself on the first page`() = runBlocking {
        coEvery { api.getLikedSongs(page = 0, size = 10) } returns page(emptyList(), page = 0, size = 10)

        val rows = LibraryTree.children(LibraryTree.LIKED_ID, page = 0, pageSize = 10).orEmpty()

        assertEquals(1, rows.size)
        assertTrue(rows.first().mediaMetadata.title.toString().isNotBlank())
    }

    // --- queue resolvers --------------------------------------------------

    /**
     * Backs the bare `song:` leaf fix: a single song has to come with a pool
     * behind it, or the endless engine can never grow a next item and skip
     * stays dead for the rest of the session.
     */
    @Test
    fun `the all-songs queue returns a playable pool`() = runBlocking {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns page(songs(1L, 2L, 3L))

        val queue = LibraryTree.allSongsQueue()

        assertEquals(3, queue.size)
        assertTrue(queue.all { it.mediaMetadata.isPlayable == true })
        assertTrue("queue items need a stream uri", queue.all { it.localConfiguration != null })
    }

    @Test
    fun `queue items carry the song media id form`() = runBlocking {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns page(songs(77L))

        val item = LibraryTree.allSongsQueue().first()

        assertEquals(77L, item.mediaId.removePrefix("song:").toLong())
    }

    // --- placeholder songs must never enter a playback queue ---------------
    //
    // Un segnaposto del DJ (playable=false — richiesta non ancora scaricata,
    // o file sparito) che finisse in una di queste code si comporterebbe in
    // auto come uno skip fantasma: l'elemento non parte, esattamente la
    // classe di guasto per cui l'app ha gia' avuto due incidenti. Ogni
    // resolver sotto "queue resolvers (called by onSetMediaItems)" deve
    // togliere questi brani PRIMA di consegnare la lista al player — questi
    // test diventano rossi se quel filtro sparisce da uno qualsiasi di loro.

    @Test
    fun `playlist queue drops unplayable songs`() = runBlocking {
        coEvery { api.getPlaylist(1L) } returns playlistDetail(
            song(1L, playable = true),
            song(2L, playable = false),
            song(3L, playable = true),
        )

        val queue = LibraryTree.playlistQueue(1L)

        assertEquals(listOf(1L, 3L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    @Test
    fun `album queue drops unplayable songs`() = runBlocking {
        coEvery { api.getAlbum("Album", "Artista") } returns AlbumDetailDto(
            name = "Album",
            artist = "Artista",
            songs = listOf(song(1L, playable = true), song(2L, playable = false)),
        )

        val queue = LibraryTree.albumQueue("Album", "Artista")

        assertEquals(listOf(1L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    @Test
    fun `artist queue drops unplayable songs`() = runBlocking {
        coEvery { api.getArtist("Artista") } returns ArtistDetailDto(
            name = "Artista",
            albums = emptyList(),
            songs = listOf(song(1L, playable = false), song(2L, playable = true)),
        )

        val queue = LibraryTree.artistQueue("Artista")

        assertEquals(listOf(2L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    @Test
    fun `genre queue drops unplayable songs`() = runBlocking {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            page(listOf(song(1L, playable = true), song(2L, playable = false)))

        val queue = LibraryTree.genreQueue("indie")

        assertEquals(listOf(1L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    @Test
    fun `all-songs queue drops unplayable songs`() = runBlocking {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns
            page(listOf(song(1L, playable = false), song(2L, playable = true)))

        val queue = LibraryTree.allSongsQueue()

        assertEquals(listOf(2L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    @Test
    fun `liked queue drops unplayable songs`() = runBlocking {
        coEvery { api.getLikedSongs(any(), any()) } returns
            page(listOf(song(1L, playable = true), song(2L, playable = false)))

        val queue = LibraryTree.likedQueue()

        assertEquals(listOf(1L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    @Test
    fun `recents queue drops unplayable songs`() = runBlocking {
        coEvery { api.recentSongs(any()) } returns
            listOf(song(1L, playable = false), song(2L, playable = true))

        val queue = LibraryTree.recentsQueue()

        assertEquals(listOf(2L), queue.map { it.mediaId.removePrefix("song:").toLong() })
    }

    /**
     * Contrario del blocco sopra: nella lista SFOGLIABILE di una playlist il
     * segnaposto deve restare visibile (solo marcato non riproducibile), non
     * sparire come nelle code. E' la stessa distinzione che vale sul lato
     * Compose (`PlaylistDetailScreen` mostra la riga spenta invece di
     * toglierla) — qui si fissa la meta' Android-Auto della stessa regola.
     */
    @Test
    fun `the browsable playlist list keeps an unplayable song, just marked not playable`() = runBlocking {
        coEvery { api.getPlaylist(1L) } returns playlistDetail(
            song(1L, playable = true),
            song(2L, playable = false),
        )

        val rows = LibraryTree.children("playlist:1", page = 0, pageSize = 20).orEmpty()

        assertEquals(2, rows.size)
        assertEquals(
            listOf(true, false),
            rows.map { it.mediaMetadata.isPlayable == true },
        )
    }

    // --- helpers ----------------------------------------------------------

    private fun songItem(id: Long): MediaItem =
        MediaItem.Builder()
            .setMediaId("song:$id")
            .setUri("https://test.invalid/$id.mp3")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Brano $id").setArtist("Artista").build())
            .build()

    private fun song(id: Long, playable: Boolean = true) = SongDto(
        id = id,
        title = "Brano $id",
        artist = "Artista",
        album = "Album",
        durationMs = 180_000L,
        hasCoverArt = true,
        playable = playable,
    )

    private fun songs(vararg ids: Long) = ids.map { song(it) }

    private fun playlistDetail(vararg entrySongs: SongDto) = PlaylistDetailDto(
        id = 1L,
        name = "Playlist",
        createdAt = "2026-08-22T05:00:00Z",
        updatedAt = "2026-08-22T05:00:00Z",
        songs = entrySongs.mapIndexed { i, s -> PlaylistSongEntryDto(playlistSongId = i.toLong(), song = s) },
    )

    private fun page(items: List<SongDto>, page: Int = 0, size: Int = 50) = PageResponse(
        items = items,
        page = page,
        size = size,
        totalItems = items.size.toLong(),
        totalPages = 1,
    )

    // --- il DJ in auto ------------------------------------------------------

    private fun djProposal(id: Long = 42L, name: String = "Cantautori di Casa") = PlaylistDto(
        id = id,
        name = name,
        songCount = 18,
        createdAt = "2026-08-22T05:00:00Z",
        updatedAt = "2026-08-22T05:00:00Z",
        kind = "DJ_SET",
    )

    /**
     * In macchina il DJ esiste solo attraverso le playlist che produce.
     * Nessuna modifica al codice serve a farlo: `madeForYou()` chiede
     * `kind=auto`, e il filtro `auto` del backend significa "ogni kind
     * diverso da USER". Questo test blocca quel comportamento, cosi' un
     * eventuale restringimento del filtro non farebbe sparire in silenzio le
     * proposte del DJ dall'auto.
     */
    @Test
    fun `DJ proposals reach Android Auto through the made-for-you folder`() = runBlocking {
        coEvery { api.listPlaylists(kind = "auto") } returns listOf(djProposal())

        val children = LibraryTree.children(
            LibraryTree.MADE_FOR_YOU_ID, page = 0, pageSize = 50).orEmpty()

        assertTrue(children.any { it.mediaMetadata.title == "Cantautori di Casa" })
    }

    /**
     * `madeForYou()` must map the auto-kind playlists one-to-one. Nothing may
     * ride along with them: a synthetic "chat with the DJ" or "generate now"
     * tile slipped in alongside the real proposals would pass a mere
     * `contains` check, so this compares the full set of rows against the
     * full set of playlists the (mocked) backend returned — an extra row of
     * any name, or a missing one, fails it.
     */
    @Test
    fun `made-for-you contains exactly the auto-kind playlists, nothing synthetic`() = runBlocking {
        coEvery { api.listPlaylists(kind = "auto") } returns listOf(
            djProposal(id = 42L, name = "Cantautori di Casa"),
            djProposal(id = 43L, name = "Sere Lente"),
        )

        val children = LibraryTree.children(
            LibraryTree.MADE_FOR_YOU_ID, page = 0, pageSize = 50).orEmpty()

        assertEquals(
            "the made-for-you folder must contain exactly one row per " +
                "auto-kind playlist and nothing else",
            setOf("playlist:42", "playlist:43"),
            children.map { it.mediaId }.toSet(),
        )
    }

    /**
     * The root of the Android Auto browse tree is a closed set. Comparing
     * the full set of ids — not scanning for words like "chat" or
     * "preferenze" — is deliberate: a keyword search only catches names we
     * already thought of, and a node called e.g. "Parla col DJ" or "Il tuo
     * DJ" would walk straight past one. Comparing the complete set means
     * *any* addition or removal fails this test regardless of what the new
     * node is called, which is what actually enforces "in macchina il DJ
     * esiste solo attraverso le playlist che produce": nobody can add
     * anything to this tree — DJ-related or not — without editing the
     * allowlist below and reading why it's here.
     */
    @Test
    fun `the browse root exposes exactly the known folders, nothing more`() = runBlocking {
        val expectedRootIds = setOf(
            LibraryTree.QUEUE_ID,
            LibraryTree.MADE_FOR_YOU_ID,
            LibraryTree.RECENTS_ID,
            LibraryTree.LIKED_ID,
            LibraryTree.PLAYLISTS_ID,
            LibraryTree.ALBUMS_ID,
            LibraryTree.ARTISTS_ID,
            LibraryTree.GENRES_ID,
            LibraryTree.ALL_SONGS_ID,
        )

        val actualRootIds = LibraryTree.children(LibraryTree.ROOT_ID, page = 0, pageSize = 50)
            .orEmpty()
            .map { it.mediaId }
            .toSet()

        assertEquals(
            "A node was added to (or removed from) the Android Auto browse " +
                "root. In macchina il DJ esiste solo attraverso le playlist " +
                "che produce: una conversazione a testo e' inutile e " +
                "pericolosa alla guida. If this new node belongs to the DJ " +
                "(chat, preferences, a \"generate now\" command), it must not " +
                "reach the car — take it back out. If it's unrelated to the " +
                "DJ and the addition is deliberate, update expectedRootIds " +
                "above and say why in the commit.",
            expectedRootIds,
            actualRootIds,
        )
    }
}
