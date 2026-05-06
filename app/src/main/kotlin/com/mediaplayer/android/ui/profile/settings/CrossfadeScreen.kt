package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings → Crossfade. Mockup `mh-settings.jsx:22-58`: lead-in
 * descriptive paragraph, single card with `// DURATA` eyebrow, big lime
 * ExtraBold value + glued mono `s` suffix, slider rail with mono numeric
 * ticks `0/2/4/6/8/10/12`. The mockup `Audizione` audition toggle is
 * intentionally omitted — preview-on-change requires player-side wiring
 * that isn't in scope.
 */
@Composable
fun CrossfadeScreen(onBack: () -> Unit) {
    val settings = PlayerSettings.instance
    val scope = rememberCoroutineScope()
    var seconds by remember { mutableFloatStateOf(0f) }
    var loaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        seconds = settings.crossfadeSeconds.first().toFloat()
        loaded = true
    }
    val mono = LocalMHMono.current

    SettingsSubScreen(title = "Crossfade", onBack = onBack) {
        Text(
            text = "Sovrappone le tracce in transizione. Una dissolvenza più lunga è ideale " +
                "per ambient e mix DJ; spegnila per album che chiedono silenzio fra una " +
                "traccia e l'altra.",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
            modifier = Modifier.fillMaxWidth(),
        )
        SettingsCard(eyebrow = "Durata") {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Durata".uppercase(),
                        style = mono.eyebrow,
                        color = MHColors.TextLo,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = seconds.toInt().toString(),
                            color = MHColors.Lime,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = MonoFamily,
                                fontSize = 36.sp,
                            ),
                        )
                        Text(
                            text = "s",
                            color = MHColors.Lime,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = MonoFamily,
                                fontSize = 22.sp,
                            ),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                Slider(
                    value = seconds,
                    onValueChange = { seconds = it },
                    onValueChangeFinished = {
                        if (loaded) scope.launch { settings.setCrossfadeSeconds(seconds.toInt()) }
                    },
                    valueRange = 0f..12f,
                    steps = 11,
                    colors = SliderDefaults.colors(
                        thumbColor = MHColors.Lime,
                        activeTrackColor = MHColors.Lime,
                        inactiveTrackColor = MHColors.Divider,
                    ),
                )
                // Mono numeric ticks `0,2,4,6,8,10,12` under the rail per
                // mockup `mh-settings.jsx:42-44`. Distributed evenly with
                // SpaceBetween so they line up with the slider thumb stops.
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(0, 2, 4, 6, 8, 10, 12).forEach { tick ->
                        val active = tick <= seconds.toInt()
                        Text(
                            text = "${tick}s",
                            style = mono.duration,
                            color = if (active) MHColors.Lime else MHColors.TextLo2,
                        )
                    }
                }
            }
        }
    }
}
