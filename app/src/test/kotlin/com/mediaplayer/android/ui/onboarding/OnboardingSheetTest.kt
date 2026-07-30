package com.mediaplayer.android.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.ui.ScreenTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The first thing a new install shows. It has exactly one way out, so the
 * button has to work — a sheet the user cannot dismiss is an app they
 * cannot use.
 */
class OnboardingSheetTest : ScreenTest() {

    private fun sheet(onDismiss: () -> Unit = {}) {
        setScreen { OnboardingSheet(onDismiss = onDismiss) }
        compose.waitForIdle()
    }

    @Test
    fun `the welcome sheet introduces the app`() {
        sheet()

        // Eyebrows render prefixed and uppercased.
        compose.onNodeWithText("// BENVENUTO IN MUSICHUB").assertIsDisplayed()
        compose.onNodeWithText("La tua libreria,\nil tuo ritmo.").assertIsDisplayed()
    }

    @Test
    fun `the three things the app is for are listed`() {
        sheet()

        compose.onNodeWithText("La tua libreria, ovunque").assertIsDisplayed()
        compose.onNodeWithText("Daily Mix che impara").assertIsDisplayed()
        compose.onNodeWithText("Offline · senza interruzioni").assertIsDisplayed()
    }

    @Test
    fun `starting dismisses the sheet`() {
        var dismissed = 0

        sheet(onDismiss = { dismissed++ })
        compose.onNodeWithText("Inizia").performClick()

        assertEquals(1, dismissed)
    }
}
