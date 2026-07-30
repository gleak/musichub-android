package com.mediaplayer.android.playback

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.MediaPlayerApi
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.PageResponse
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

    // --- helpers ----------------------------------------------------------

    private fun songItem(id: Long): MediaItem =
        MediaItem.Builder()
            .setMediaId("song:$id")
            .setUri("https://test.invalid/$id.mp3")
            .setMediaMetadata(MediaMetadata.Builder().setTitle("Brano $id").setArtist("Artista").build())
            .build()

    private fun song(id: Long) = SongDto(
        id = id,
        title = "Brano $id",
        artist = "Artista",
        album = "Album",
        durationMs = 180_000L,
        hasCoverArt = true,
    )

    private fun songs(vararg ids: Long) = ids.map { song(it) }

    private fun page(items: List<SongDto>, page: Int = 0, size: Int = 50) = PageResponse(
        items = items,
        page = page,
        size = size,
        totalItems = items.size.toLong(),
        totalPages = 1,
    )
}
