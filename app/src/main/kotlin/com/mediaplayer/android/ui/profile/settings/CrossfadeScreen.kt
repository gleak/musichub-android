package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
        SettingsCard(eyebrow = "Durata transizione") {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = seconds.toInt().toString(),
                            color = MHColors.Lime,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.displayLarge.copy(fontFamily = MonoFamily),
                        )
                        Text(
                            text = " sec",
                            color = MHColors.TextLo,
                            style = mono.caption,
                            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Off", style = mono.duration, color = MHColors.TextLo)
                    Text("12 sec", style = mono.duration, color = MHColors.TextLo)
                }
            }
        }
        SettingsCard(eyebrow = "Come funziona") {
            Text(
                text = "Crossfade sovrappone la fine del brano corrente con l'inizio " +
                    "del successivo per la durata scelta. Lascia a 0 per cambi netti.",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
