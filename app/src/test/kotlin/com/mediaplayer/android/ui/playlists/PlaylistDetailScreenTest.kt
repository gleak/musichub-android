package com.mediaplayer.android.ui.playlists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.PlaylistSongEntryDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * A playlist can be owned or joined, hand-made or server-generated, and
 * the screen changes shape for each: only owners get the auto-sync card
 * and the add button, only members get the "leave" call to action.
 */
class PlaylistDetailScreenTest : ScreenTest() {

    private fun screen(
        onPlayFromIndex: (List<SongDto>, Int) -> Unit = { _, _ -> },
        onShufflePlay: (List<SongDto>) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        setScreen {
            PlaylistDetailScreen(
                playlistId = 1L,
                onBack = onBack,
                onPlayFromIndex = onPlayFromIndex,
                onShufflePlay = onShufflePlay,
            )
        }
    }

    private fun detail(
        songs: List<SongDto> = emptyList(),
        name: String = "Road Trip",
        kind: String = "USER",
        isOwner: Boolean = true,
        ownerName: String? = null,
        memberCount: Int = 0,
    ) = PlaylistDetailDto(
        id = 1L,
        name = name,
        createdAt = "2026-01-01T00:00:00",
        updatedAt = "2026-01-01T00:00:00",
        songs = songs.mapIndexed { i, s ->
            PlaylistSongEntryDto(playlistSongId = (i + 1).toLong(), song = s)
        },
        kind = kind,
        isOwner = isOwner,
        ownerName = ownerName,
        memberCount = memberCount,
    )

    @Test
    fun `the playlist name and its songs render`() {
        coEvery { api.getPlaylist(1L) } returns
            detail(songs = listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))

        screen()

        awaitText("Road Trip")
        compose.onNodeWithText("Breed").assertIsDisplayed()
        compose.onNodeWithText("Lithium").assertIsDisplayed()
    }

    @Test
    fun `an empty playlist explains how to fill it`() {
        coEvery { api.getPlaylist(1L) } returns detail()

        screen()

        awaitText("Nessun brano")
        compose.onNodeWithText("Aggiungi brani dalla ricerca o tocca +.").assertIsDisplayed()
    }

    @Test
    fun `a load failure offers a retry`() {
        coEvery { api.getPlaylist(1L) } throws IOException("offline")

        screen()

        awaitText("Riprova")
    }

    @Test
    fun `tapping a song plays the playlist from that position`() {
        coEvery { api.getPlaylist(1L) } returns
            detail(songs = listOf(song(1L, title = "Breed"), song(2L, title = "Lithium")))
        var index: Int? = null

        screen(onPlayFromIndex = { _, i -> index = i })
        awaitText("Lithium")
        compose.onNodeWithText("Lithium").performClick()

        assertEquals(1, index)
    }

    /**
     * An auto-playlist is regenerated server-side, so hand-editing it would
     * be undone on the next refresh — the add button has no business here.
     */
    @Test
    fun `an auto playlist offers no way to add songs`() {
        coEvery { api.getPlaylist(1L) } returns
            detail(songs = listOf(song(1L)), kind = "DISCOVER_DAILY")

        screen()
        awaitText("Road Trip")

        compose.onNodeWithContentDescription("Aggiungi brani").assertIsNotDisplayed()
    }

    @Test
    fun `a user playlist can have songs added`() {
        coEvery { api.getPlaylist(1L) } returns detail(songs = listOf(song(1L)))

        screen()
        awaitText("Road Trip")

        compose.onNodeWithContentDescription("Aggiungi brani").assertIsDisplayed()
    }

    @Test
    fun `back is wired`() {
        coEvery { api.getPlaylist(1L) } returns detail(songs = listOf(song(1L)))
        var backed = false

        screen(onBack = { backed = true })
        awaitText("Road Trip")
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(true, backed)
    }
}
