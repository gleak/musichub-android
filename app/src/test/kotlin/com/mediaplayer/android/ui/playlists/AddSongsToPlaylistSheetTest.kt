package com.mediaplayer.android.ui.playlists

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.onePage
import com.mediaplayer.android.ui.song
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * The picker for adding catalogue songs to a playlist. There is no bulk
 * endpoint behind it, so one tap on the CTA fans out one request per
 * selected song — which means it has to cope with some of them failing.
 * Reporting a partial add as a clean success loses songs silently; as a
 * plain failure it makes the user add the successful ones twice.
 */
class AddSongsToPlaylistSheetTest : ScreenTest() {

    private fun sheet(
        playlistId: Long = 7L,
        playlistName: String = "Corsa",
        existingSongIds: Set<Long> = emptySet(),
        onDismiss: () -> Unit = {},
        onSongAdded: () -> Unit = {},
    ) {
        setScreen {
            AddSongsToPlaylistSheet(
                playlistId = playlistId,
                playlistName = playlistName,
                existingSongIds = existingSongIds,
                onDismiss = onDismiss,
                onSongAdded = onSongAdded,
            )
        }
    }

    private fun catalogue(vararg songs: SongDto) {
        coEvery { api.listSongs(any(), any(), any(), any()) } returns onePage(songs.toList())
    }

    private val detail: PlaylistDetailDto = mockk(relaxed = true)

    @Test
    fun `the catalogue is listed with artists and durations`() {
        catalogue(song(1L, title = "Bohemian", artist = "Queen"))

        sheet()

        awaitText("Bohemian")
        compose.onNodeWithText("Queen").assertIsDisplayed()
        compose.onNodeWithText("3:20").assertIsDisplayed()
    }

    @Test
    fun `the header names the playlist being added to`() {
        catalogue(song(1L))

        sheet(playlistName = "Corsa")

        awaitText("// AGGIUNGI A · CORSA")
    }

    @Test
    fun `nothing is selected to begin with`() {
        catalogue(song(1L))

        sheet()

        awaitText("Seleziona almeno un brano")
    }

    @Test
    fun `the call to action counts the selection`() {
        catalogue(song(1L, title = "Bohemian"), song(2L, title = "Innuendo"))

        sheet()
        awaitText("Bohemian")
        compose.onNodeWithText("Bohemian").performClick()
        awaitText("Aggiungi 1 brano")

        compose.onNodeWithText("Innuendo").performClick()

        awaitText("Aggiungi 2 brani")
    }

    @Test
    fun `tapping a selected song deselects it`() {
        catalogue(song(1L, title = "Bohemian"))

        sheet()
        awaitText("Bohemian")
        compose.onNodeWithText("Bohemian").performClick()
        awaitText("Aggiungi 1 brano")
        compose.onNodeWithText("Bohemian").performClick()

        awaitText("Seleziona almeno un brano")
    }

    /**
     * A song already in the playlist stays in the list — hiding it would
     * make the user wonder where it went — but it cannot be picked again.
     */
    @Test
    fun `a song already in the playlist cannot be selected`() {
        catalogue(song(1L, title = "Bohemian"))

        sheet(existingSongIds = setOf(1L))
        awaitText("Bohemian")
        compose.onNodeWithText("Bohemian").performClick()

        compose.onNodeWithText("Seleziona almeno un brano").assertIsDisplayed()
    }

    @Test
    fun `committing adds every selected song and closes the sheet`() {
        catalogue(song(1L, title = "Bohemian"), song(2L, title = "Innuendo"))
        coEvery { api.addSongToPlaylist(any(), any()) } returns detail
        var added = 0
        var dismissed = 0

        sheet(playlistId = 7L, onSongAdded = { added++ }, onDismiss = { dismissed++ })
        awaitText("Bohemian")
        compose.onNodeWithText("Bohemian").performClick()
        compose.onNodeWithText("Innuendo").performClick()
        compose.onNodeWithText("Aggiungi 2 brani").performClick()
        compose.waitForIdle()

        coVerify(exactly = 2) { api.addSongToPlaylist(7L, any()) }
        assertEquals(1, added)
        assertEquals(1, dismissed)
    }

    /**
     * Half the songs landed. Saying "failed" would hide that; saying
     * "done" would lose the rest. The sheet stays open with both numbers,
     * and only the failures remain selected so a retry re-sends just those.
     */
    @Test
    fun `a partial failure reports both halves and keeps the sheet open`() {
        catalogue(song(1L, title = "Bohemian"), song(2L, title = "Innuendo"))
        coEvery { api.addSongToPlaylist(any(), match { it.songId == 1L }) } returns detail
        coEvery { api.addSongToPlaylist(any(), match { it.songId == 2L }) } throws
            IOException("offline")
        var dismissed = 0

        sheet(onDismiss = { dismissed++ })
        awaitText("Bohemian")
        compose.onNodeWithText("Bohemian").performClick()
        compose.onNodeWithText("Innuendo").performClick()
        compose.onNodeWithText("Aggiungi 2 brani").performClick()

        awaitText("1 aggiunti, 1 non riusciti.")
        assertEquals(0, dismissed)
    }

    @Test
    fun `a total failure keeps the sheet open with the reason`() {
        catalogue(song(1L, title = "Bohemian"))
        coEvery { api.addSongToPlaylist(any(), any()) } throws IOException("offline")
        var dismissed = 0

        sheet(onDismiss = { dismissed++ })
        awaitText("Bohemian")
        compose.onNodeWithText("Bohemian").performClick()
        compose.onNodeWithText("Aggiungi 1 brano").performClick()
        compose.waitForIdle()

        assertEquals(0, dismissed)
    }

    @Test
    fun `an empty catalogue says so rather than showing a blank list`() {
        catalogue()

        sheet()

        awaitText("La tua libreria è vuota")
    }

    /**
     * An unreachable server is not an empty catalogue, and telling the user
     * their library is empty would send them off looking for the songs they
     * know are there.
     */
    @Test
    fun `a failed load shows the reason instead of an empty library`() {
        coEvery { api.listSongs(any(), any(), any(), any()) } throws IOException("offline")

        sheet()

        awaitText("Server non raggiungibile. Controlla la connessione.")
        compose.onAllNodesWithText("La tua libreria è vuota").assertCountEquals(0)
    }
}
