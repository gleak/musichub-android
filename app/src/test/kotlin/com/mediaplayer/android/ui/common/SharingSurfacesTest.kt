package com.mediaplayer.android.ui.common

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.ui.playlists.PlaylistShareDialog
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The three small surfaces around sharing and reporting. Each is a single
 * decision put in front of the user, and each has a wording that carries
 * the decision: what the link does to the recipient, what reporting a track
 * takes away, and whether the playlist you are looking at is yours.
 */
class SharingSurfacesTest : ScreenTest() {

    // ---------- the share dialog ----------

    @Test
    fun `the share dialog shows the playlist and its link`() {
        setScreen {
            PlaylistShareDialog(
                playlistName = "Corsa",
                link = "https://q-musichub.duckdns.org/share/abc123",
                onDismiss = {},
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Corsa").assertIsDisplayed()
        compose.onNodeWithText("https://q-musichub.duckdns.org/share/abc123").assertIsDisplayed()
    }

    /**
     * This link hands over a copy, not a seat in the same playlist — the
     * opposite of the collaborative share sheet, and the user has to be able
     * to tell them apart before they send it.
     */
    @Test
    fun `the share dialog says the recipient gets a copy`() {
        setScreen {
            PlaylistShareDialog(playlistName = "Corsa", link = "https://x/share/a", onDismiss = {})
        }
        compose.waitForIdle()

        compose.onNodeWithText("riceve una copia", substring = true).assertIsDisplayed()
    }

    @Test
    fun `copying puts the link on the clipboard`() {
        setScreen {
            PlaylistShareDialog(
                playlistName = "Corsa",
                link = "https://q-musichub.duckdns.org/share/abc123",
                onDismiss = {},
            )
        }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("Copia").performClick()
        compose.waitForIdle()

        val app = ApplicationProvider.getApplicationContext<Application>()
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        assertEquals(
            "https://q-musichub.duckdns.org/share/abc123",
            clipboard.primaryClip?.getItemAt(0)?.text,
        )
    }

    @Test
    fun `closing the share dialog hands back to the caller`() {
        var dismissed = 0
        setScreen {
            PlaylistShareDialog(playlistName = "Corsa", link = "https://x/share/a") { dismissed++ }
        }
        compose.waitForIdle()

        compose.onNodeWithText("Chiudi").performClick()

        assertEquals(1, dismissed)
    }

    // ---------- the flag-wrong confirmation ----------

    /**
     * Reporting a track removes it everywhere, and there is no undo — so the
     * dialog spells out the consequence rather than asking "are you sure?".
     */
    @Test
    fun `the report dialog names the track and what reporting costs`() {
        setScreen {
            FlagWrongConfirmDialog(
                songId = 1L,
                songTitle = "Breed",
                songArtist = "Nirvana",
                hasCoverArt = false,
                onConfirm = {},
                onDismiss = {},
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Brano sbagliato?").assertIsDisplayed()
        compose.onNodeWithText("Breed", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Verrà rimosso da ricerca", substring = true).assertIsDisplayed()
        compose.onNodeWithText("// SEGNALA · DEFINITIVO").assertIsDisplayed()
    }

    @Test
    fun `reporting is confirmed only on the report button`() {
        var confirmed = 0
        var dismissed = 0
        setScreen {
            FlagWrongConfirmDialog(
                songId = 1L,
                songTitle = "Breed",
                songArtist = "Nirvana",
                hasCoverArt = false,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Segnala").performClick()

        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun `cancelling the report reports nothing`() {
        var confirmed = 0
        var dismissed = 0
        setScreen {
            FlagWrongConfirmDialog(
                songId = 1L,
                songTitle = "Breed",
                songArtist = "Nirvana",
                hasCoverArt = false,
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Annulla").performClick()

        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    // ---------- the members strip ----------

    /** The owner manages the members; everyone else can only look. */
    @Test
    fun `the owner is offered the manage button`() {
        setScreen {
            MembersStripCard(
                isOwner = true,
                ownerName = "Antonio",
                memberCount = 3,
                onManage = {},
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Condivisa con 3 persone").assertIsDisplayed()
        compose.onNodeWithText("3 collaboratori attivi").assertIsDisplayed()
        compose.onNodeWithText("Gestisci").assertIsDisplayed()
    }

    @Test
    fun `a member sees who shared it and no manage button`() {
        setScreen {
            MembersStripCard(
                isOwner = false,
                ownerName = "Giulia",
                memberCount = 2,
                onManage = {},
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Condivisa da Giulia").assertIsDisplayed()
        compose.onNodeWithText("2 membri").assertIsDisplayed()
        compose.onAllNodesWithText("Gestisci").assertCountEquals(0)
    }

    @Test
    fun `one collaborator is counted in the singular`() {
        setScreen {
            MembersStripCard(isOwner = true, ownerName = "Antonio", memberCount = 1, onManage = {})
        }
        compose.waitForIdle()

        compose.onNodeWithText("1 collaboratore attivo").assertIsDisplayed()
    }

    @Test
    fun `an unknown owner is named rather than left blank`() {
        setScreen {
            MembersStripCard(isOwner = false, ownerName = null, memberCount = 1, onManage = {})
        }
        compose.waitForIdle()

        compose.onNodeWithText("Condivisa da Sconosciuto").assertIsDisplayed()
    }

    @Test
    fun `tapping the strip opens the member list`() {
        var managed = 0
        setScreen {
            MembersStripCard(
                isOwner = true,
                ownerName = "Antonio",
                memberCount = 2,
                onManage = { managed++ },
            )
        }
        compose.waitForIdle()

        compose.onNodeWithText("Gestisci").performClick()

        assertEquals(1, managed)
    }
}
