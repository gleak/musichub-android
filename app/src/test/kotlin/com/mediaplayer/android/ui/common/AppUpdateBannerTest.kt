package com.mediaplayer.android.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.AppUpdateDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.update.AppUpdateChecker
import com.mediaplayer.android.update.AppUpdateInstaller
import io.mockk.coEvery
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The update prompt has two shapes, and picking the wrong one is what
 * makes an update either invisible or inescapable: an optional update is
 * an inline banner the user can ignore, a required one is a full-screen
 * overlay with no dismiss.
 */
class AppUpdateBannerTest : ScreenTest() {

    @After
    fun clearUpdateState() {
        AppUpdateChecker.consume()
        AppUpdateInstaller.publishProgressForTest(AppUpdateInstaller.DownloadProgress.Idle)
    }

    private suspend fun publish(version: String = "9.9.9", required: Boolean = false) {
        coEvery { api.latestAppUpdate() } returns retrofit2.Response.success(
            AppUpdateDto(
                version = version,
                versionCode = Int.MAX_VALUE,
                url = "https://example.invalid/app.apk",
                required = required,
            ),
        )
        AppUpdateChecker.forceCheck(
            com.mediaplayer.android.MediaPlayerApp.appContext,
        )
    }

    private fun hostScreen() {
        setScreen { AppUpdateBannerHost() }
    }

    private fun overlayScreen() {
        setScreen { AppUpdateRequiredOverlay() }
    }

    private fun nodeCount(text: String): Int =
        compose.onAllNodesWithText(text).fetchSemanticsNodes().size

    @Test
    fun `an optional update renders an inline banner`() {
        runBlocking { publish(version = "9.9.9") }

        hostScreen()

        compose.onNodeWithText("// AGGIORNAMENTO").assertIsDisplayed()
        compose.onNodeWithText("v9.9.9", substring = true).assertIsDisplayed()
    }

    /** No manifest means no banner — not an empty one. */
    @Test
    fun `nothing renders when there is no update`() {
        AppUpdateChecker.consume()

        hostScreen()

        assertEquals(0, nodeCount("// AGGIORNAMENTO"))
    }

    /**
     * A required update must not also draw the inline banner, or the user
     * sees the same prompt twice stacked.
     */
    @Test
    fun `a required update does not also render the inline banner`() {
        runBlocking { publish(required = true) }

        hostScreen()

        assertEquals(0, nodeCount("// AGGIORNAMENTO"))
    }

    @Test
    fun `a required update renders the blocking overlay`() {
        runBlocking { publish(required = true) }

        overlayScreen()

        compose.onNodeWithText("// AGGIORNAMENTO RICHIESTO").assertIsDisplayed()
        compose.onNodeWithText("Devi aggiornare per continuare.").assertIsDisplayed()
    }

    /** The overlay is only for required updates; an optional one must not block. */
    @Test
    fun `an optional update does not render the blocking overlay`() {
        runBlocking { publish(required = false) }

        overlayScreen()

        assertEquals(0, nodeCount("// AGGIORNAMENTO RICHIESTO"))
    }

    @Test
    fun `the banner offers an install action`() {
        runBlocking { publish() }

        hostScreen()

        compose.onNodeWithText("Installa").assertIsDisplayed()
    }

    // ---------- the download the user watches ----------

    /**
     * Once the APK is coming down the banner stops offering to install and
     * starts reporting progress instead — otherwise a second tap starts a
     * second download.
     */
    @Test
    fun `an in-flight download shows its progress instead of the install button`() {
        runBlocking { publish(version = "9.9.9") }
        AppUpdateInstaller.publishProgressForTest(
            AppUpdateInstaller.DownloadProgress.Active(
                percent = 42,
                bytesDownloaded = 21L * 1024 * 1024,
                totalBytes = 50L * 1024 * 1024,
            ),
        )

        hostScreen()

        compose.onNodeWithText("// SCARICAMENTO APK").assertIsDisplayed()
        compose.onNodeWithText("42%").assertIsDisplayed()
        assertEquals(0, nodeCount("Installa"))
    }

    @Test
    fun `the progress banner reports how much has come down`() {
        runBlocking { publish() }
        AppUpdateInstaller.publishProgressForTest(
            AppUpdateInstaller.DownloadProgress.Active(
                percent = 50,
                bytesDownloaded = 25L * 1024 * 1024,
                totalBytes = 50L * 1024 * 1024,
            ),
        )

        hostScreen()

        compose.onNodeWithText("MB", substring = true).assertIsDisplayed()
    }

    /**
     * A download that died mid-flight has to offer a retry: the update is
     * still available, only the transfer failed.
     */
    @Test
    fun `a failed download offers a retry`() {
        runBlocking { publish() }
        AppUpdateInstaller.publishProgressForTest(
            AppUpdateInstaller.DownloadProgress.Failed("network"),
        )

        hostScreen()

        compose.onNodeWithText("// AGGIORNAMENTO FALLITO").assertIsDisplayed()
        compose.onNodeWithText("Scaricamento interrotto").assertIsDisplayed()
        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }

    @Test
    fun `a required update in flight shows progress in the overlay too`() {
        runBlocking { publish(required = true) }
        AppUpdateInstaller.publishProgressForTest(
            AppUpdateInstaller.DownloadProgress.Active(
                percent = 10,
                bytesDownloaded = 5L * 1024 * 1024,
                totalBytes = 50L * 1024 * 1024,
            ),
        )

        overlayScreen()

        compose.onNodeWithText("10%").assertIsDisplayed()
    }

    @Test
    fun `a required update that failed to download can be retried`() {
        runBlocking { publish(required = true) }
        AppUpdateInstaller.publishProgressForTest(
            AppUpdateInstaller.DownloadProgress.Failed("network"),
        )

        overlayScreen()

        compose.onNodeWithText("Riprova").assertIsDisplayed()
    }
}
