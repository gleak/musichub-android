package com.mediaplayer.android.ui.playlists

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.data.dto.ShareLinkDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * The owner's side of sharing. The sheet mints a token as soon as it opens,
 * shows the URL itself — the point of the redesign was that the user can
 * see and copy it — and offers a revoke that closes the link for anyone who
 * hasn't opened it yet.
 */
class PlaylistShareSheetTest : ScreenTest() {

    private val link = ShareLinkDto(token = "abc123def", url = "https://q-musichub.duckdns.org/p/abc123def")

    private fun sheet(
        playlistId: Long = 7L,
        playlistName: String = "Corsa",
        memberCount: Int = 0,
        onDismiss: () -> Unit = {},
    ) {
        setScreen {
            PlaylistShareSheet(
                playlistId = playlistId,
                playlistName = playlistName,
                memberCount = memberCount,
                onDismiss = onDismiss,
            )
        }
    }

    @Test
    fun `the link is minted on open and shown in full`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet(playlistId = 7L)

        awaitText(link.url)
        coVerify(exactly = 1) { api.createPlaylistShare(7L) }
    }

    @Test
    fun `the sheet names the playlist being shared`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet(playlistName = "Corsa")

        awaitText("Corsa")
        compose.onNodeWithText("// CONDIVIDI · COLLABORATIVA").assertIsDisplayed()
    }

    /**
     * Sharing invites collaborators into the same playlist rather than
     * handing out copies, which is the one thing the user has to understand
     * before they send the link.
     */
    @Test
    fun `the sheet says the link shares rather than copies`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet()

        awaitText("non viene creata una copia", substring = true)
    }

    /**
     * The failure the user actually hits is being offline, and what they
     * need to read is that — not the transport's own words for it.
     */
    @Test
    fun `a failed mint reports why instead of showing a dead link box`() {
        coEvery { api.createPlaylistShare(any()) } throws IOException("Unable to resolve host")

        sheet()

        awaitText("Server non raggiungibile. Controlla la connessione.")
    }

    @Test
    fun `copying puts the url on the clipboard`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet()
        awaitText(link.url)
        compose.onNodeWithText("Copia").performClick()

        awaitText("Copiato")
        val app = ApplicationProvider.getApplicationContext<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(link.url, clipboard.primaryClip?.getItemAt(0)?.text)
    }

    @Test
    fun `the member count is spelled out`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet(memberCount = 1)

        awaitText("1 membro attivo", substring = true)
    }

    @Test
    fun `several members are counted in the plural`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet(memberCount = 3)

        awaitText("3 membri attivi", substring = true)
    }

    @Test
    fun `an unshared playlist says nobody is in yet`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet(memberCount = 0)

        awaitText("Nessun membro attivo")
    }

    /** There is nothing to revoke until a link exists. */
    @Test
    fun `revoke is dead until the link is minted`() {
        coEvery { api.createPlaylistShare(any()) } throws IOException("offline")

        sheet()
        awaitText("Server non raggiungibile. Controlla la connessione.")

        compose.onNodeWithText("Revoca link").assertIsNotEnabled()
    }

    @Test
    fun `revoking asks first`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet()
        awaitText(link.url)
        compose.onNodeWithText("Revoca link").performClick()

        awaitText("Revocare il link?")
        coVerify(exactly = 0) { api.revokePlaylistShares(any()) }
    }

    @Test
    fun `confirming the revoke closes the link and says so`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet(playlistId = 7L)
        awaitText(link.url)
        compose.onNodeWithText("Revoca link").performClick()
        awaitText("Revocare il link?")
        compose.onNodeWithText("Revoca").performClick()

        awaitText("Link revocato. Genera un nuovo link riaprendo questa schermata.")
        coVerify(exactly = 1) { api.revokePlaylistShares(7L) }
    }

    @Test
    fun `cancelling the revoke leaves the link alone`() {
        coEvery { api.createPlaylistShare(any()) } returns link

        sheet()
        awaitText(link.url)
        compose.onNodeWithText("Revoca link").performClick()
        awaitText("Revocare il link?")
        compose.onNodeWithText("Annulla").performClick()

        compose.waitForIdle()
        coVerify(exactly = 0) { api.revokePlaylistShares(any()) }
        compose.onNodeWithText(link.url).assertIsDisplayed()
    }

    @Test
    fun `a failed revoke reports the reason`() {
        coEvery { api.createPlaylistShare(any()) } returns link
        coEvery { api.revokePlaylistShares(any()) } throws IOException("offline")

        sheet()
        awaitText(link.url)
        compose.onNodeWithText("Revoca link").performClick()
        awaitText("Revocare il link?")
        compose.onNodeWithText("Revoca").performClick()

        compose.waitForIdle()
        compose.onNodeWithText(
            "Link revocato. Genera un nuovo link riaprendo questa schermata.",
        ).assertDoesNotExist()
    }
}
