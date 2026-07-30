package com.mediaplayer.android.playback

import android.app.Application
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The phone UI is driven entirely by mapping timeline items back to songs, and
 * the two mediaId forms in circulation meet here: a bare number from the phone,
 * and `song:{id}` from Android Auto, the media library and playback resumption.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@UnstableApi
class MediaItemMappingTest {

    /**
     * The regression this pins: parsing the bare mediaId only meant every
     * car-, resumption- or voice-started session mapped to null, which blanked
     * the mini player, auto-closed the Now Playing sheet, emptied the queue
     * sheet and suppressed play history — while audio kept playing.
     */
    @Test
    fun `the Android Auto media id form resolves to a song`() {
        val song = item("song:42").toSongDto()

        assertEquals(42L, song?.id)
    }

    @Test
    fun `the phone media id form resolves to a song`() {
        assertEquals(42L, item("42").toSongDto()?.id)
    }

    /** Local tracks carry negative ids in both forms. */
    @Test
    fun `local negative ids survive both forms`() {
        assertEquals(-5L, item("-5").toSongDto()?.id)
        assertEquals(-5L, item("song:-5").toSongDto()?.id)
    }

    @Test
    fun `metadata is carried across`() {
        val song = MediaItem.Builder()
            .setMediaId("song:7")
            .setUri("https://test.invalid/7.mp3")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Titolo")
                    .setArtist("Artista")
                    .setAlbumTitle("Album")
                    .build()
            )
            .build()
            .toSongDto()

        assertEquals("Titolo", song?.title)
        assertEquals("Artista", song?.artist)
        assertEquals("Album", song?.album)
    }

    /** Browse-tree ids never reach the timeline, and must not be mistaken for songs. */
    @Test
    fun `non-song media ids resolve to null`() {
        assertNull(item("qu:3|9").toSongDto())
        assertNull(item("pl:12").toSongDto())
        assertNull(item("").toSongDto())
    }

    private fun item(id: String): MediaItem =
        MediaItem.Builder().setMediaId(id).setUri("https://test.invalid/x.mp3").build()
}
