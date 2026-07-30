package com.mediaplayer.android.ui.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import io.mockk.every
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The bar that sits above everything else whenever something is loaded.
 * It is the only playback control most sessions ever touch, and it has one
 * rule that matters more than the rest: with nothing loaded it must not be
 * there at all, or it eats a strip of every screen for no reason.
 */
@UnstableApi
class MiniPlayerTest : PlayerSheetTest() {

    private fun trackWithAlbum(
        id: String,
        title: String,
        artist: String,
        album: String?,
    ): MediaItem = MediaItem.Builder()
        .setMediaId(id)
        .setUri("https://test.invalid/$id.mp3")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(album)
                .build(),
        )
        .build()

    private fun miniPlayer(
        queue: List<MediaItem>,
        playing: Boolean = false,
        onExpand: () -> Unit = {},
    ) {
        val vm = connectPlayer(queue) { c ->
            every { c.isPlaying } returns playing
        }
        setScreen { MiniPlayer(viewModel = vm, onExpand = onExpand) }
        compose.waitForIdle()
    }

    @Test
    fun `the bar shows the loaded track`() {
        miniPlayer(listOf(track("1", title = "Bohemian", artist = "Queen")))

        compose.onNodeWithText("Bohemian").assertIsDisplayed()
        compose.onNodeWithText("Queen").assertIsDisplayed()
    }

    /** Nothing loaded, nothing drawn — the bar is not a permanent fixture. */
    @Test
    fun `an empty queue draws no bar`() {
        miniPlayer(emptyList())

        compose.onAllNodesWithText("Riproduci").assertCountEquals(0)
        compose.onAllNodesWithText("Pausa").assertCountEquals(0)
    }

    @Test
    fun `the subtitle pairs artist with album when there is one`() {
        miniPlayer(
            listOf(trackWithAlbum("1", "Bohemian", "Queen", "A Night at the Opera")),
        )

        compose.onNodeWithText("Queen · A Night at the Opera").assertIsDisplayed()
    }

    /** Missing album metadata is common; it must not render a dangling dot. */
    @Test
    fun `a track with no album shows the artist alone`() {
        miniPlayer(listOf(trackWithAlbum("1", "Bohemian", "Queen", null)))

        compose.onNodeWithText("Queen").assertIsDisplayed()
        compose.onAllNodesWithText("Queen · ", substring = true).assertCountEquals(0)
    }

    @Test
    fun `a blank album is treated as no album`() {
        miniPlayer(listOf(trackWithAlbum("1", "Bohemian", "Queen", "   ")))

        compose.onNodeWithText("Queen").assertIsDisplayed()
    }

    @Test
    fun `the button offers play while paused`() {
        miniPlayer(listOf(track("1")), playing = false)

        compose.onNodeWithContentDescription("Riproduci").assertIsDisplayed()
    }

    @Test
    fun `the button offers pause while playing`() {
        miniPlayer(listOf(track("1")), playing = true)

        compose.onNodeWithContentDescription("Pausa").assertIsDisplayed()
    }

    @Test
    fun `pressing play asks the player to start`() {
        miniPlayer(listOf(track("1")), playing = false)

        compose.onNodeWithContentDescription("Riproduci").performClick()

        verify { controller.play() }
    }

    @Test
    fun `pressing pause asks the player to stop`() {
        miniPlayer(listOf(track("1")), playing = true)

        compose.onNodeWithContentDescription("Pausa").performClick()

        verify { controller.pause() }
    }

    @Test
    fun `tapping the bar expands the player`() {
        var expanded = 0
        miniPlayer(listOf(track("1", title = "Bohemian")), onExpand = { expanded++ })

        compose.onNodeWithText("Bohemian").performClick()

        assertEquals(1, expanded)
    }
}
