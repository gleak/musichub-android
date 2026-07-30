package com.mediaplayer.android.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.dto.AppUpdateDto
import com.mediaplayer.android.ui.ScreenTest
import com.mediaplayer.android.update.AppUpdateChecker
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
}