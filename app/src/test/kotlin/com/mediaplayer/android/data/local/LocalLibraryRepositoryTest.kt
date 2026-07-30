package com.mediaplayer.android.data.local

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The on-device scan reads MediaStore, and every quirk it handles comes
 * from a real device: `<unknown>` where a tag is missing, an album id of
 * zero meaning "no album", a folder that has to be derived from whichever
 * of RELATIVE_PATH / DATA / DISPLAY_NAME the OS version populates.
 *
 * A fake cursor stands in for the provider so those branches are exercised
 * without a device full of music.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class LocalLibraryRepositoryTest {

    private lateinit var app: Application

    private val projection = arrayOf(
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

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        grantAudioPermission()
        publish()
        Robolectric.setupContentProvider(
            FakeMediaProvider::class.java,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.authority,
        )
    }

    private fun grantAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        shadowOf(app).grantPermissions(permission)
    }

    private fun revokeAudioPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        shadowOf(app).denyPermissions(permission)
    }

    /**
     * Publish [rows] as the MediaStore audio table. A real (if tiny)
     * ContentProvider stands in for the media provider, so the repository's
     * own query — projection, selection and sort order included — runs
     * exactly as it does on a device.
     */
    private fun publish(vararg rows: Array<Any?>) {
        FakeMediaProvider.rows = rows.toList()
        FakeMediaProvider.columns = projection
    }

    private fun row(
        id: Long = 1L,
        title: String = "Breed",
        artist: String? = "Nirvana",
        album: String? = "Nevermind",
        albumId: Long = 10L,
        durationMs: Long = 200_000L,
        dateAddedSeconds: Long = 1_700_000_000L,
        data: String = "/storage/emulated/0/Music/Nirvana/breed.mp3",
        relativePath: String = "Music/Nirvana/",
        displayName: String = "breed.mp3",
    ): Array<Any?> = arrayOf(
        id, title, artist, album, albumId, durationMs, dateAddedSeconds,
        data, relativePath, displayName,
    )

    private fun scan(): List<LocalTrack> = runBlocking { LocalLibraryRepository.scan(app) }

    @Test
    fun `an indexed track is read out in full`() {
        publish(row())

        val track = scan().single()

        assertEquals(1L, track.id)
        assertEquals("Breed", track.title)
        assertEquals("Nirvana", track.artist)
        assertEquals("Nevermind", track.album)
        assertEquals(200_000L, track.durationMs)
        assertEquals(10L, track.albumId)
    }

    /** DATE_ADDED is seconds in MediaStore; the app works in milliseconds. */
    @Test
    fun `the added timestamp is converted to milliseconds`() {
        publish(row(dateAddedSeconds = 1_700_000_000L))

        assertEquals(1_700_000_000_000L, scan().single().dateAddedMs)
    }

    /**
     * MediaStore writes the literal string `<unknown>` for a missing tag.
     * Rendered as-is it shows up in the UI as an artist called "<unknown>".
     */
    @Test
    fun `an unknown artist becomes blank rather than the literal placeholder`() {
        publish(row(artist = "<unknown>"))

        assertEquals("", scan().single().artist)
    }

    @Test
    fun `an unknown album becomes null`() {
        publish(row(album = "<unknown>"))

        assertNull(scan().single().album)
    }

    @Test
    fun `a blank album becomes null`() {
        publish(row(album = "   "))

        assertNull(scan().single().album)
    }

    /** Album id zero is MediaStore's "no album", not album number zero. */
    @Test
    fun `album id zero is treated as absent`() {
        publish(row(albumId = 0L))

        val track = scan().single()
        assertNull(track.albumId)
        assertNull(track.albumArtUri)
    }

    @Test
    fun `the folder is derived from the relative path`() {
        publish(row(relativePath = "Music/Nirvana/", displayName = "breed.mp3"))

        assertEquals("Nirvana", scan().single().folderName)
    }

    /**
     * RELATIVE_PATH only exists from Q. On older rows it comes back empty
     * and the folder has to come out of the absolute DATA path instead.
     */
    @Test
    fun `the folder falls back to the absolute path when relative path is empty`() {
        publish(
            row(
                relativePath = "",
                data = "/storage/emulated/0/Music/Queen/bohemian.mp3",
                displayName = "bohemian.mp3",
            ),
        )

        assertEquals("Queen", scan().single().folderName)
    }

    @Test
    fun `several tracks all come back`() {
        publish(
            row(id = 1L, title = "Breed"),
            row(id = 2L, title = "Lithium"),
            row(id = 3L, title = "Polly"),
        )

        assertEquals(listOf("Breed", "Lithium", "Polly"), scan().map { it.title })
    }

    @Test
    fun `an empty device yields an empty library rather than an error`() {
        publish()

        assertTrue(scan().isEmpty())
    }

    /**
     * Without the audio permission MediaStore isn't read at all. The scan
     * must return the SAF-pinned trees (none here) instead of throwing —
     * the screen shows a permission prompt, not a crash.
     */
    @Test
    fun `a revoked permission yields an empty scan rather than a failure`() {
        publish(row())
        revokeAudioPermission()

        assertTrue(scan().isEmpty())
    }

    @Test
    fun `permission state is reported`() {
        assertTrue(LocalLibraryRepository.hasPermission(app))

        revokeAudioPermission()

        assertEquals(false, LocalLibraryRepository.hasPermission(app))
    }

    @Test
    fun `each track carries a playable content uri`() {
        publish(row(id = 42L))

        val uri = scan().single().uri.toString()
        assertTrue("unexpected uri: $uri", uri.endsWith("/42"))
    }
}

/**
 * Minimal stand-in for the system media provider. Serves whatever rows the
 * test published, projected onto the columns the repository asked for.
 */
class FakeMediaProvider : android.content.ContentProvider() {

    companion object {
        var columns: Array<String> = emptyArray()
        var rows: List<Array<Any?>> = emptyList()
    }

    override fun onCreate(): Boolean = true

    override fun query(
        uri: android.net.Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): android.database.Cursor {
        val requested = projection?.toList() ?: columns.toList()
        val cursor = android.database.MatrixCursor(requested.toTypedArray())
        rows.forEach { row ->
            cursor.addRow(requested.map { column -> row[columns.indexOf(column)] })
        }
        return cursor
    }

    override fun getType(uri: android.net.Uri): String? = null
    override fun insert(uri: android.net.Uri, values: android.content.ContentValues?) = null
    override fun delete(
        uri: android.net.Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
    override fun update(
        uri: android.net.Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0
}
