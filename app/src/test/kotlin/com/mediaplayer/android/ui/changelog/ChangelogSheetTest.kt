package com.mediaplayer.android.ui.changelog

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.AppVersion
import com.mediaplayer.android.data.Changelog
import com.mediaplayer.android.data.ChangelogPreferences
import com.mediaplayer.android.ui.ScreenTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The changelog sheet auto-opens once per version, which only works if it
 * records the version it showed. If that write is lost the sheet reopens
 * on every cold start.
 */
class ChangelogSheetTest : ScreenTest() {

    private fun screen(onDismiss: () -> Unit = {}) {
        setScreen { ChangelogSheet(onDismiss = onDismiss) }
    }

    @Test
    fun `the latest release is the one shown`() {
        val latest = Changelog.entries.first()

        screen()

        awaitText(latest.title, substring = true)
    }

    @Test
    fun `the version is labelled`() {
        val latest = Changelog.entries.first()

        screen()

        awaitText("v${latest.version}", substring = true)
    }

    @Test
    fun `the highlights are listed`() {
        val firstHighlight = Changelog.entries.first().highlights.first()

        screen()

        awaitText(firstHighlight.take(30), substring = true)
    }

    /**
     * Opening the sheet marks the version seen. Without this the sheet
     * reopens on every launch, which is how a "what's new" turns into a
     * nuisance.
     */
    @Test
    fun `opening the sheet records the version as seen`() {
        screen()
        awaitText("Continua")

        val seen = runBlocking { ChangelogPreferences.instance.lastSeenVersion() }
        assertEquals(AppVersion.VERSION, seen)
    }

    @Test
    fun `continue dismisses`() {
        var dismissed = false

        screen(onDismiss = { dismissed = true })
        awaitText("Continua")
        compose.onNodeWithText("Continua").performClick()
        compose.waitForIdle()

        assertEquals(true, dismissed)
    }
}
