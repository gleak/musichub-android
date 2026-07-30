package com.mediaplayer.android.ui.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.ui.ScreenTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.robolectric.shadows.ShadowBuild

/**
 * MIUI pauses background apps hard enough that the media service dies
 * mid-drive and the steering-wheel controls go quiet. This banner is the
 * only place the app explains that, so it has to appear on the phones that
 * suffer from it and stay out of the way everywhere else.
 *
 * It is also the app's one permanently-dismissable banner: once ignored it
 * must never come back, including after a restart.
 */
class MiuiRestrictionsBannerTest : ScreenTest() {

    @Before
    fun undismiss() {
        runBlocking { PlayerSettings.instance.setMiuiWarningDismissed(false) }
    }

    private fun onXiaomi() {
        ShadowBuild.setManufacturer("Xiaomi")
        ShadowBuild.setBrand("Redmi")
    }

    private fun banner() {
        setScreen { MiuiRestrictionsBannerHost() }
        compose.waitForIdle()
    }

    @Test
    fun `a xiaomi phone gets the warning`() {
        onXiaomi()

        banner()

        awaitText("Comandi al volante non rispondono")
        compose.onNodeWithText("// XIAOMI · ANDROID AUTO").assertIsDisplayed()
    }

    /** Every other phone is unaffected and must not be told otherwise. */
    @Test
    fun `a non-xiaomi phone gets nothing`() {
        ShadowBuild.setManufacturer("Google")
        ShadowBuild.setBrand("google")

        banner()

        compose.onAllNodesWithText("Comandi al volante non rispondono").assertCountEquals(0)
    }

    @Test
    fun `a poco phone counts as xiaomi`() {
        ShadowBuild.setManufacturer("Xiaomi Communications")
        ShadowBuild.setBrand("POCO")

        banner()

        awaitText("Comandi al volante non rispondono")
    }

    @Test
    fun `an already dismissed banner stays hidden`() {
        onXiaomi()
        runBlocking { PlayerSettings.instance.setMiuiWarningDismissed(true) }

        banner()

        compose.onAllNodesWithText("Comandi al volante non rispondono").assertCountEquals(0)
    }

    @Test
    fun `tapping the banner opens the fix-it sheet`() {
        onXiaomi()

        banner()
        awaitText("Comandi al volante non rispondono")
        compose.onNodeWithText("Comandi al volante non rispondono").performClick()

        awaitText("Sistema i permessi per Android Auto")
    }

    /**
     * Both switches are needed and the order matters, so the sheet numbers
     * them rather than listing two equal-looking options.
     */
    @Test
    fun `the sheet walks through both switches in order`() {
        onXiaomi()

        banner()
        awaitText("Comandi al volante non rispondono")
        compose.onNodeWithText("Comandi al volante non rispondono").performClick()
        awaitText("Sistema i permessi per Android Auto")

        compose.onNodeWithText("01").assertIsDisplayed()
        compose.onNodeWithText("Avvio automatico").assertIsDisplayed()
        compose.onNodeWithText("02").assertIsDisplayed()
        compose.onNodeWithText("Risparmio batteria").assertIsDisplayed()
    }

    @Test
    fun `dismissing from the banner is remembered`() {
        onXiaomi()

        banner()
        awaitText("Comandi al volante non rispondono")
        compose.onNodeWithContentDescription("Non mostrare più").performClick()
        compose.waitForIdle()

        awaitDismissed()
        compose.onAllNodesWithText("Comandi al volante non rispondono").assertCountEquals(0)
    }

    @Test
    fun `dismissing from the sheet is remembered too`() {
        onXiaomi()

        banner()
        awaitText("Comandi al volante non rispondono")
        compose.onNodeWithText("Comandi al volante non rispondono").performClick()
        awaitText("Sistema i permessi per Android Auto")
        compose.onNodeWithText("Non mostrare più").performClick()

        awaitDismissed()
    }

    private fun awaitDismissed() {
        val deadline = System.currentTimeMillis() + 5_000
        while (System.currentTimeMillis() < deadline) {
            if (runBlocking { PlayerSettings.instance.miuiWarningDismissed.first() }) return
            compose.waitForIdle()
            Thread.sleep(20)
        }
        assertTrue("the banner was never marked as dismissed", false)
    }
}
