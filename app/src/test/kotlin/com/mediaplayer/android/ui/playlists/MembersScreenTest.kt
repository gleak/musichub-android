package com.mediaplayer.android.ui.playlists

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.PlaylistMemberDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Who can see a shared playlist. Removing someone is irreversible from the
 * app's side — the only way back in is a fresh link — so it asks first, and
 * the owner's own row must never offer the button at all.
 */
class MembersScreenTest : ScreenTest() {

    private fun member(
        userId: Long,
        name: String,
        owner: Boolean = false,
    ) = PlaylistMemberDto(
        userId = userId,
        name = name,
        joinedAt = "2026-01-01T00:00:00",
        owner = owner,
    )

    private fun screen(
        playlistId: Long = 7L,
        isOwnerView: Boolean = true,
        onBack: () -> Unit = {},
    ) {
        setScreen {
            MembersScreen(
                playlistId = playlistId,
                isOwnerView = isOwnerView,
                onBack = onBack,
            )
        }
    }

    @Test
    fun `members are listed with their role`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(
            member(1L, "Antonio", owner = true),
            member(2L, "Giulia"),
        )

        screen()

        awaitText("Antonio")
        compose.onNodeWithText("Proprietario").assertIsDisplayed()
        compose.onNodeWithText("Giulia").assertIsDisplayed()
        compose.onNodeWithText("Membro").assertIsDisplayed()
    }

    @Test
    fun `a failed load offers a retry`() {
        coEvery { api.listPlaylistMembers(any()) } throws IOException("offline")

        screen()

        awaitText("Server non raggiungibile. Controlla la connessione.")
    }

    @Test
    fun `retrying reloads the list`() {
        coEvery { api.listPlaylistMembers(any()) } throws IOException("offline")

        screen()
        awaitText("Server non raggiungibile. Controlla la connessione.")
        coEvery { api.listPlaylistMembers(any()) } returns listOf(member(2L, "Giulia"))
        compose.onNodeWithText("Riprova").performClick()

        awaitText("Giulia")
    }

    /** The owner cannot remove themselves. */
    @Test
    fun `the owner row has no remove button`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(
            member(1L, "Antonio", owner = true),
            member(2L, "Giulia"),
        )

        screen(isOwnerView = true)
        awaitText("Antonio")

        compose.onAllNodesWithContentDescription("Rimuovi membro").assertCountEquals(1)
    }

    /** A guest viewing the list can see who else is in, and nothing more. */
    @Test
    fun `a non-owner is offered no remove buttons at all`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(
            member(1L, "Antonio", owner = true),
            member(2L, "Giulia"),
        )

        screen(isOwnerView = false)
        awaitText("Antonio")

        compose.onAllNodesWithContentDescription("Rimuovi membro").assertCountEquals(0)
    }

    @Test
    fun `removing a member asks for confirmation first`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(
            member(1L, "Antonio", owner = true),
            member(2L, "Giulia"),
        )

        screen()
        awaitText("Giulia")
        compose.onNodeWithContentDescription("Rimuovi membro").performClick()

        awaitText("Rimuovere Giulia?")
        coVerify(exactly = 0) { api.kickPlaylistMember(any(), any()) }
    }

    @Test
    fun `confirming removes the member and reloads`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(
            member(1L, "Antonio", owner = true),
            member(2L, "Giulia"),
        )

        screen(playlistId = 7L)
        awaitText("Giulia")
        compose.onNodeWithContentDescription("Rimuovi membro").performClick()
        awaitText("Rimuovere Giulia?")
        coEvery { api.listPlaylistMembers(any()) } returns listOf(member(1L, "Antonio", owner = true))
        compose.onNodeWithText("Rimuovi").performClick()

        compose.waitForIdle()
        coVerify(exactly = 1) { api.kickPlaylistMember(7L, 2L) }
    }

    @Test
    fun `cancelling leaves the member in place`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(
            member(1L, "Antonio", owner = true),
            member(2L, "Giulia"),
        )

        screen()
        awaitText("Giulia")
        compose.onNodeWithContentDescription("Rimuovi membro").performClick()
        awaitText("Rimuovere Giulia?")
        compose.onNodeWithText("Annulla").performClick()

        compose.waitForIdle()
        coVerify(exactly = 0) { api.kickPlaylistMember(any(), any()) }
        compose.onNodeWithText("Giulia").assertIsDisplayed()
    }

    @Test
    fun `back is wired to the caller`() {
        coEvery { api.listPlaylistMembers(any()) } returns listOf(member(1L, "Antonio", owner = true))
        var backs = 0

        screen(onBack = { backs++ })
        awaitText("Antonio")
        compose.onNodeWithContentDescription("Indietro").performClick()

        assertEquals(1, backs)
    }
}
