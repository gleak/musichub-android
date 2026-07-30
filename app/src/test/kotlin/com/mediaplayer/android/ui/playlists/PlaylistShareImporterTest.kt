package com.mediaplayer.android.ui.playlists

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.SharePreviewDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * What the recipient of a share link sees. It opens over whatever the user
 * was doing, so it must not accept anything on its own — except in the one
 * case where the playlist is already theirs, when asking would be asking
 * them to join something they are already in.
 */
class PlaylistShareImporterTest : ScreenTest() {

    private fun preview(
        songCount: Int = 12,
        ownerName: String = "Giulia",
        alreadyAccessible: Boolean = false,
    ) = SharePreviewDto(
        token = "abc123def",
        playlistName = "Corsa",
        songCount = songCount,
        ownerName = ownerName,
        alreadyAccessible = alreadyAccessible,
    )

    private fun detail(id: Long = 7L, name: String = "Corsa"): PlaylistDetailDto =
        PlaylistDetailDto(
            id = id,
            name = name,
            songs = emptyList(),
            createdAt = "2026-01-01T00:00:00",
            updatedAt = "2026-01-01T00:00:00",
        )

    private fun importer(
        token: String = "abc123def",
        onDismiss: () -> Unit = {},
        onImported: (Long, String) -> Unit = { _, _ -> },
    ) {
        setScreen {
            PlaylistShareImporter(
                token = token,
                onDismiss = onDismiss,
                onImported = onImported,
            )
        }
    }

    @Test
    fun `the preview says whose playlist it is and how big`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview(songCount = 12, ownerName = "Giulia")

        importer()

        awaitText("Corsa")
        compose.onNodeWithText("Playlist collaborativa di Giulia").assertIsDisplayed()
        compose.onNodeWithText("12 BRANI").assertIsDisplayed()
    }

    @Test
    fun `nothing is imported until the user accepts`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview()

        importer()
        awaitText("Corsa")

        coVerify(exactly = 0) { api.acceptPlaylistShare(any()) }
    }

    @Test
    fun `accepting imports the playlist and reports it back`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview()
        coEvery { api.acceptPlaylistShare(any()) } returns detail(id = 7L, name = "Corsa")
        var imported: Pair<Long, String>? = null

        importer(token = "abc123def", onImported = { id, name -> imported = id to name })
        awaitText("Corsa")
        compose.onNodeWithText("Aggiungi alla mia libreria").performClick()
        compose.waitForIdle()

        coVerify(exactly = 1) { api.acceptPlaylistShare("abc123def") }
        assertEquals(7L to "Corsa", imported)
    }

    /**
     * Re-opening a link the user already accepted must land them in the
     * playlist rather than asking them to add a second copy of it.
     */
    @Test
    fun `a playlist the user already has is opened without asking`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview(alreadyAccessible = true)
        coEvery { api.acceptPlaylistShare(any()) } returns detail(id = 7L)
        var imported: Pair<Long, String>? = null

        importer(onImported = { id, name -> imported = id to name })
        compose.waitForIdle()

        assertEquals(7L to "Corsa", imported)
    }

    @Test
    fun `a broken link reports why and offers nothing to accept`() {
        coEvery { api.previewPlaylistShare(any()) } throws IOException("offline")

        importer()

        awaitText("Server non raggiungibile. Controlla la connessione.")
        compose.onNodeWithText("Aggiungi alla mia libreria").assertIsNotEnabled()
    }

    @Test
    fun `a failed import keeps the sheet up with the reason`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview()
        coEvery { api.acceptPlaylistShare(any()) } throws IOException("offline")
        var imported = 0

        importer(onImported = { _, _ -> imported++ })
        awaitText("Corsa")
        compose.onNodeWithText("Aggiungi alla mia libreria").performClick()

        awaitText("Server non raggiungibile. Controlla la connessione.")
        assertEquals(0, imported)
    }

    @Test
    fun `closing before accepting imports nothing`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview()
        var dismissed = 0

        importer(onDismiss = { dismissed++ })
        awaitText("Corsa")
        compose.onNodeWithContentDescription("Chiudi").performClick()

        assertEquals(1, dismissed)
        coVerify(exactly = 0) { api.acceptPlaylistShare(any()) }
    }

    @Test
    fun `the footer shows a truncated form of the link`() {
        coEvery { api.previewPlaylistShare(any()) } returns preview()

        importer(token = "abc123def456")

        awaitText("abc123d…", substring = true)
    }
}
