package com.mediaplayer.android.ui.player

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediaplayer.android.playback.BandInfo
import com.mediaplayer.android.playback.EqPreset
import com.mediaplayer.android.playback.EqState
import com.mediaplayer.android.playback.EqualizerController
import com.mediaplayer.android.ui.ScreenTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The equalizer is the one feature that legitimately isn't available on
 * every device — a phone whose audio HAL exposes no Equalizer effect gets
 * null state. The sheet has to say so rather than render dead sliders.
 *
 * On a device that does support it the sheet is a read-out of the hardware:
 * band frequencies, the active preset, and the audio session the effect is
 * bound to, which is what makes it useful when routing goes wrong.
 */
class EqualizerSheetTest : ScreenTest() {

    @After
    fun clearEqualizerState() {
        EqualizerController.publishForTest(null)
    }

    private fun band(index: Int, freqHz: Int, levelMilliBel: Int = 0) = BandInfo(
        index = index,
        centerFreqHz = freqHz,
        levelMilliBel = levelMilliBel,
        minLevel = -1500,
        maxLevel = 1500,
    )

    private fun supported(
        enabled: Boolean = true,
        preset: EqPreset = EqPreset.FLAT,
        audioSessionId: Int = 0x2A,
        bands: List<BandInfo> = listOf(band(0, 60), band(1, 1000)),
    ) {
        EqualizerController.publishForTest(
            EqState(
                enabled = enabled,
                preset = preset,
                bands = bands,
                audioSessionId = audioSessionId,
            ),
        )
    }

    private fun sheet() {
        setScreen { EqualizerSheet(onDismiss = {}) }
        compose.waitForIdle()
    }

    @Test
    fun `an unsupported device is told so instead of shown dead controls`() {
        // Nothing has bound an audio session, so the controller's state is
        // null — the same shape a device with no Equalizer effect produces.
        sheet()

        compose.onNodeWithText("Equalizzatore non supportato su questo dispositivo")
            .assertIsDisplayed()
    }

    @Test
    fun `a supported device gets the full sheet`() {
        supported()

        sheet()

        compose.onNodeWithText("Equalizzatore").assertIsDisplayed()
        compose.onNodeWithText("// AUDIO").assertIsDisplayed()
    }

    @Test
    fun `each band is labelled with its centre frequency`() {
        supported(bands = listOf(band(0, 60), band(1, 1000), band(2, 14000)))

        sheet()

        compose.onNodeWithText("60 Hz").assertIsDisplayed()
        compose.onNodeWithText("1 kHz").assertIsDisplayed()
        compose.onNodeWithText("14 kHz").assertIsDisplayed()
    }

    @Test
    fun `the active preset is named on the card`() {
        supported(preset = EqPreset.BASS_BOOST)

        sheet()

        compose.onNodeWithText(EqPreset.BASS_BOOST.label).assertIsDisplayed()
    }

    /** The pill is the at-a-glance answer to "is this doing anything?". */
    @Test
    fun `the active pill shows only while the equalizer is on`() {
        supported(enabled = true)

        sheet()

        compose.onNodeWithText("ATTIVO").assertIsDisplayed()
        compose.onNode(isToggleable()).assertIsOn()
    }

    @Test
    fun `a disabled equalizer shows no active pill`() {
        supported(enabled = false)

        sheet()

        compose.onAllNodesWithText("ATTIVO").assertCountEquals(0)
        compose.onNode(isToggleable()).assertIsOff()
    }

    /**
     * The session id is what makes this card worth having — it is how you
     * confirm the effect is bound to the session that is actually playing.
     */
    @Test
    fun `the bound audio session is shown in hex`() {
        supported(audioSessionId = 0x2A)

        sheet()

        compose.onNodeWithText("// SESSIONE AUDIO").assertIsDisplayed()
        compose.onNodeWithText("0x2A").assertIsDisplayed()
    }

    @Test
    fun `an unbound session is shown as absent rather than as zero`() {
        supported(audioSessionId = 0)

        sheet()

        compose.onAllNodesWithText("0x0").assertCountEquals(0)
    }

    @Test
    fun `the preset card opens a picker listing every preset`() {
        supported(preset = EqPreset.FLAT)

        sheet()
        compose.onNodeWithText("Preset").performClick()

        EqPreset.entries.forEach { preset ->
            // The active preset is named on the card as well as in the
            // picker, so count rather than expect a single node.
            assertTrue(
                "${preset.label} missing from the picker",
                compose.onAllNodesWithText(preset.label).fetchSemanticsNodes().isNotEmpty(),
            )
        }
    }

    @Test
    fun `picking a preset closes the picker`() {
        supported(preset = EqPreset.FLAT)

        sheet()
        compose.onNodeWithText("Preset").performClick()
        compose.onNodeWithText(EqPreset.VOCAL.label).performClick()

        compose.onAllNodesWithText("Chiudi").assertCountEquals(0)
    }

    /**
     * CUSTOM is a state the sheet reports, not one the user picks — it only
     * appears once bands have been dragged by hand.
     */
    @Test
    fun `the custom entry cannot be picked`() {
        supported(preset = EqPreset.FLAT)

        sheet()
        compose.onNodeWithText("Preset").performClick()
        compose.onNodeWithText(EqPreset.CUSTOM.label).performClick()

        compose.onNodeWithText("Chiudi").assertIsDisplayed()
    }

    @Test
    fun `the picker can be dismissed without changing anything`() {
        supported(preset = EqPreset.FLAT)

        sheet()
        compose.onNodeWithText("Preset").performClick()
        compose.onNodeWithText("Chiudi").performClick()

        compose.onAllNodesWithText("Chiudi").assertCountEquals(0)
        compose.onNodeWithText(EqPreset.FLAT.label).assertIsDisplayed()
    }
}
