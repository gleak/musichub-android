package com.mediaplayer.android.ui.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import com.mediaplayer.android.data.AppVersion
import com.mediaplayer.android.data.dto.StatsDto
import com.mediaplayer.android.ui.ScreenTest
import io.mockk.coEvery
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Settings and sign-out live here. The destructive action is behind a
 * confirmation, and the confirmation is the part worth pinning: an
 * accidental sign-out costs the user their session and their queue.
 */
class ProfileScreenTest : ScreenTest() {

    private fun screen(
        onSignOut: () -> Unit = {},
        onShowChangelog: () -> Unit = {},
        onOpenSetting: (String) -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        setScreen {
            ProfileScreen(
                onBack = onBack,
                onShowChangelog = onShowChangelog,
                onSignOut = onSignOut,
                onOpenSetting = onOpenSetting,
                statsViewModel = ProfileStatsViewModel(),
            )
        }
    }

    @Test
    fun `library stats render`() {
        coEvery { api.getStats() } returns StatsDto(songs = 412, playlists = 9, artists = 57)

        screen()

        awaitText("412")
        compose.onNodeWithText("57").assertIsDisplayed()
    }

    /** Stats that can't be fetched show a dash, not a zero. Zero is a lie. */
    @Test
    fun `unavailable stats are blank rather than zero`() {
        coEvery { api.getStats() } throws IOException("offline")

        screen()

        awaitText("—")
    }

    @Test
    fun `the installed version is shown`() {
        coEvery { api.getStats() } returns StatsDto(0, 0, 0)

        screen()

        compose.onNodeWithText("v${AppVersion.VERSION}").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the changelog shortcut is wired`() {
        coEvery { api.getStats() } returns StatsDto(0, 0, 0)
        var shown = false

        screen(onShowChangelog = { shown = true })
        compose.onNodeWithText("Cosa c'è di nuovo").performScrollTo().performClick()

        assertEquals(true, shown)
    }

    @Test
    fun `a settings row opens its sub-page by route`() {
        coEvery { api.getStats() } returns StatsDto(0, 0, 0)
        var route: String? = null

        screen(onOpenSetting = { route = it })
        compose.onNodeWithText("Crossfade").performScrollTo().performClick()

        assertEquals("profile/crossfade", route)
    }

    /** Sign-out asks first — one stray tap must not end the session. */
    @Test
    fun `signing out is confirmed before it happens`() {
        coEvery { api.getStats() } returns StatsDto(0, 0, 0)
        var signedOut = false

        screen(onSignOut = { signedOut = true })
        // The sign-out button sits at the bottom of a LazyColumn, so it
        // isn't composed until the list is scrolled to it.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Disconnetti"))
        compose.onAllNodesWithText("Disconnetti").onFirst().performClick()

        assertEquals(false, signedOut)
        compose.onNodeWithText("Disconnettersi da MusicHub?").assertIsDisplayed()
    }

    @Test
    fun `cancelling the confirmation keeps the session`() {
        coEvery { api.getStats() } returns StatsDto(0, 0, 0)
        var signedOut = false

        screen(onSignOut = { signedOut = true })
        // The sign-out button sits at the bottom of a LazyColumn, so it
        // isn't composed until the list is scrolled to it.
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Disconnetti"))
        compose.onAllNodesWithText("Disconnetti").onFirst().performClick()
        compose.onNodeWithText("Annulla").performClick()

        assertEquals(false, signedOut)
    }
}
