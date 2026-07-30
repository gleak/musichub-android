package com.mediaplayer.android.ui.player

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.mediaplayer.android.ui.ScreenTest
import org.junit.Test

/**
 * The equalizer is the one feature that legitimately isn't available on
 * every device — a phone whose audio HAL exposes no Equalizer effect gets
 * null state. The sheet has to say so rather than render dead sliders.
 */
class EqualizerSheetTest : ScreenTest() {

    @Test
    fun `an unsupported device is told so instead of shown dead controls`() {
        // Nothing has bound an audio session, so the controller's state is
        // null — the same shape a device with no Equalizer effect produces.
        setScreen { EqualizerSheet(onDismiss = {}) }
        compose.waitForIdle()

        compose.onNodeWithText("Equalizzatore non supportato su questo dispositivo")
            .assertIsDisplayed()
    }
}
