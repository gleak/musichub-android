package com.mediaplayer.android.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.ui.ScreenTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Onboarding is the one screen a user cannot avoid, and its whole job is a
 * gate: pick enough genres or you don't get through. These pin the gate,
 * because a broken one either blocks everybody or seeds the recommender
 * with nothing.
 */
class OnboardingScreenTest : ScreenTest() {

    private fun screen(
        saving: Boolean = false,
        error: String? = null,
        onContinue: (List<String>) -> Unit = {},
        onSkip: () -> Unit = {},
    ) {
        setScreen {
            OnboardingScreen(
                saving = saving,
                error = error,
                onContinue = onContinue,
                onSkip = onSkip,
            )
        }
    }

    @Test
    fun `the prompt renders`() {
        screen()

        compose.onNodeWithText("Cosa ascolti?").assertIsDisplayed()
    }

    /** Nothing picked yet: the button says how many more are needed. */
    @Test
    fun `the continue button counts down the remaining picks`() {
        screen()

        compose.onNodeWithText("Scegli ancora 3").assertIsDisplayed()
    }

    @Test
    fun `continuing is refused until enough genres are picked`() {
        var picked: List<String>? = null

        screen(onContinue = { picked = it })
        compose.onAllNodesWithText("Rock").onFirst().performClick()
        compose.onNodeWithText("Scegli ancora 2").performClick()

        assertNull(picked)
    }

    @Test
    fun `picking enough genres unlocks continue and hands them over`() {
        var picked: List<String>? = null

        screen(onContinue = { picked = it })
        listOf("Rock", "Pop", "Jazz").forEach {
            compose.onAllNodesWithText(it).onFirst().performClick()
        }
        compose.onNodeWithText("Continua").performClick()

        assertEquals(3, picked?.size)
    }

    /** Tapping a picked genre again releases it and re-locks the gate. */
    @Test
    fun `unpicking a genre puts the counter back`() {
        screen()
        listOf("Rock", "Pop", "Jazz").forEach {
            compose.onAllNodesWithText(it).onFirst().performClick()
        }
        compose.onNodeWithText("Continua").assertIsDisplayed()

        compose.onAllNodesWithText("Jazz").onFirst().performClick()

        compose.onNodeWithText("Scegli ancora 1").assertIsDisplayed()
    }

    @Test
    fun `skipping is always available`() {
        var skipped = false

        screen(onSkip = { skipped = true })
        compose.onNodeWithText("Salta").performClick()

        assertEquals(true, skipped)
    }

    @Test
    fun `a save in flight shows progress instead of the action`() {
        screen(saving = true)

        compose.onNodeWithText("Salvo…").assertIsDisplayed()
    }

    @Test
    fun `a failed save explains itself and offers a retry`() {
        screen(error = "500")

        compose.onNodeWithText("Salvataggio non riuscito").assertIsDisplayed()
        compose.onNodeWithText("RIPROVA").assertIsDisplayed()
    }
}
