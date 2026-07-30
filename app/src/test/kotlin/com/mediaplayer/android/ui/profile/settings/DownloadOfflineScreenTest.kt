package com.mediaplayer.android.ui.profile.settings

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.data.dto.AutoPlaylistRefreshAllDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

/**
 * Settings → Download offline. Everything here spends the user's storage
 * or their mobile data, which is why both switches default to the cautious
 * side and why wiping the cache asks first — re-downloading a library over
 * mobile data is exactly what the screen exists to prevent.
 */
class DownloadOfflineScreenTest : ScreenTest() {

    @Before
    fun resetDownloadSettings() {
        runBlocking {
            PlayerSettings.instance.setDownloadWifiOnly(true)
            PlayerSettings.instance.setDownloadAuto(false)
        }
    }

    private fun screen(onBack: () -> Unit = {}) {
        setScreen { DownloadOfflineScreen(onBack = onBack) }
        compose.waitForIdle()
    }

    @Test
    fun `the screen is titled and can be left`() {
        var backs = 0

        screen(onBack = { backs++ })

        compose.onNodeWithText("Download offline").assertIsDisplayed()
        compose.onNodeWithContentDescription("Indietro").performClick()
        assertEquals(1, backs)
    }

    @Test
    fun `an empty cache reads as nothing downloaded`() {
        screen()

        awaitText("0 brani scaricati")
        // Nothing cached yet, so the gauge reads zero against the 1 GB cap.
        compose.onNodeWithText("0MB").assertIsDisplayed()
    }

    /**
     * Downloading over mobile data is the expensive mistake, so the switch
     * starts on the side that can't make it.
     */
    @Test
    fun `wi-fi only is on by default`() {
        screen()

        compose.onAllNodes(isToggleable())[0].assertIsOn()
    }

    @Test
    fun `automatic download is off by default`() {
        screen()

        compose.onAllNodes(isToggleable())[1].assertIsOff()
    }

    @Test
    fun `turning off wi-fi only is remembered`() {
        screen()

        compose.onAllNodes(isToggleable())[0].performClick()

        awaitSetting(expected = false) { PlayerSettings.instance.downloadWifiOnly.first() }
    }

    @Test
    fun `turning on automatic download is remembered`() {
        screen()

        compose.onAllNodes(isToggleable())[1].performClick()

        awaitSetting(expected = true) { PlayerSettings.instance.downloadAuto.first() }
    }

    @Test
    fun `regenerating the automatic playlists reports how many changed`() {
        coEvery { api.refreshAllAutoPlaylists() } returns AutoPlaylistRefreshAllDto(userId = 1L, refreshed = 42)

        screen()
        compose.onNodeWithText("Forza rigenerazione playlist Per te").performClick()

        awaitText("Aggiornate (42 brani)")
        coVerify(exactly = 1) { api.refreshAllAutoPlaylists() }
    }

    @Test
    fun `a failed regeneration says to try again`() {
        coEvery { api.refreshAllAutoPlaylists() } throws IOException("offline")

        screen()
        compose.onNodeWithText("Forza rigenerazione playlist Per te").performClick()

        awaitText("Errore — riprova")
    }

    /**
     * Wiping downloads costs whatever it took to fetch them, over whatever
     * connection is around next time. One stray tap must not do it.
     */
    @Test
    fun `wiping the downloads asks first`() {
        screen()

        compose.onNodeWithText("Cancella tutti i download").performClick()

        awaitText("Cancellare tutti i download?")
        compose.onNodeWithText("L'azione non può essere annullata.", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun `cancelling the wipe closes the dialog`() {
        screen()

        compose.onNodeWithText("Cancella tutti i download").performClick()
        awaitText("Cancellare tutti i download?")
        compose.onNodeWithText("Annulla").performClick()

        compose.onAllNodesWithText("Cancellare tutti i download?").assertCountEquals(0)
    }

    @Test
    fun `confirming the wipe empties the gauge`() {
        screen()

        compose.onNodeWithText("Cancella tutti i download").performClick()
        awaitText("Cancellare tutti i download?")
        compose.onNodeWithText("Cancella").performClick()

        awaitText("0 brani scaricati")
    }

    private fun awaitSetting(expected: Boolean, read: suspend () -> Boolean) {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (runBlocking { read() } == expected) return
            compose.waitForIdle()
            Thread.sleep(20)
        }
        throw AssertionError("setting never became $expected")
    }
}
