package com.mediaplayer.android.data.local

import android.app.Application
import android.net.Uri
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
 * Local tracks reach Media3 as the negation of their MediaStore id, so a single
 * song carrier covers both the backend catalog and the device library. Getting
 * that sign convention wrong makes a queued local track unresolvable, which
 * throws while building its media item — hence the emphasis here on the
 * boundary cases rather than the happy path.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalMediaResolverTest {

    @Before
    fun reset() {
        // The resolver is a process-wide object; force it to a known state
        // without tripping the empty-scan guard.
        LocalMediaResolver.replaceAll(listOf(track(999)))
        LocalMediaResolver.replaceAll(emptyList())
    }

    @Test
    fun `lookup takes the synthetic negative id`() {
        LocalMediaResolver.register(track(7))

        assertEquals(7L, LocalMediaResolver.get(-7L)?.id)
    }

    /** A positive id belongs to the backend catalog and must never resolve here. */
    @Test
    fun `a positive id never resolves`() {
        LocalMediaResolver.register(track(7))

        assertNull(LocalMediaResolver.get(7L))
        assertNull(LocalMediaResolver.get(0L))
    }

    @Test
    fun `locality is decided by the sign`() {
        assertTrue(LocalMediaResolver.isLocal(-1L))
        assertFalse(LocalMediaResolver.isLocal(0L))
        assertFalse(LocalMediaResolver.isLocal(1L))
    }

    @Test
    fun `unsigned lookup uses the raw MediaStore id`() {
        LocalMediaResolver.register(track(7))

        assertEquals(7L, LocalMediaResolver.getByLocalId(7L)?.id)
        assertNull(LocalMediaResolver.getByLocalId(-7L))
    }

    /**
     * MediaStore returns zero rows transiently during a reindex — after a
     * reboot, an SD remount or a permission re-grant. Letting that wipe the
     * bridge makes whatever is playing unresolvable mid-track.
     */
    @Test
    fun `an empty scan does not clobber a populated bridge`() {
        LocalMediaResolver.replaceAll(listOf(track(1), track(2)))

        LocalMediaResolver.replaceAll(emptyList())

        assertEquals(1L, LocalMediaResolver.get(-1L)?.id)
        assertEquals(2L, LocalMediaResolver.get(-2L)?.id)
    }

    @Test
    fun `a non-empty scan replaces the previous one`() {
        LocalMediaResolver.replaceAll(listOf(track(1), track(2)))

        LocalMediaResolver.replaceAll(listOf(track(3)))

        assertNull("stale entry survived the rescan", LocalMediaResolver.get(-1L))
        assertEquals(3L, LocalMediaResolver.get(-3L)?.id)
    }

    @Test
    fun `registerAll adds without dropping what is already there`() {
        LocalMediaResolver.register(track(1))

        LocalMediaResolver.registerAll(listOf(track(2), track(3)))

        assertEquals(1L, LocalMediaResolver.get(-1L)?.id)
        assertEquals(3L, LocalMediaResolver.get(-3L)?.id)
    }

    @Test
    fun `re-registering the same id overwrites it`() {
        LocalMediaResolver.register(track(1, title = "vecchio"))

        LocalMediaResolver.register(track(1, title = "nuovo"))

        assertEquals("nuovo", LocalMediaResolver.get(-1L)?.title)
    }

    @Test
    fun `uris are exposed through the synthetic id`() {
        LocalMediaResolver.register(track(4))

        assertEquals(Uri.parse("content://media/4"), LocalMediaResolver.streamUri(-4L))
        assertEquals(Uri.parse("content://art/4"), LocalMediaResolver.artworkUri(-4L))
    }

    @Test
    fun `uris of an unknown track are null rather than a crash`() {
        assertNull(LocalMediaResolver.streamUri(-404L))
        assertNull(LocalMediaResolver.artworkUri(-404L))
    }

    private fun track(id: Long, title: String = "Traccia $id") = LocalTrack(
        id = id,
        uri = Uri.parse("content://media/$id"),
        title = title,
        artist = "Artista",
        album = "Album",
        durationMs = 180_000L,
        albumId = id,
        albumArtUri = Uri.parse("content://art/$id"),
        folderName = "Musica",
        folderPath = "/storage/emulated/0/Music",
        dateAddedMs = 0L,
    )
}
