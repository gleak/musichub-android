package com.mediaplayer.android.ui.player

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.playback.EqPreset
import com.mediaplayer.android.playback.EqState
import com.mediaplayer.android.playback.EqualizerController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(onDismiss: () -> Unit) {
    val state by EqualizerController.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            val s = state
            if (s == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Equalizer not supported on this device",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            // Header: title + enabled toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Equalizzatore", style = MaterialTheme.typography.titleLarge)
                Switch(
                    checked = s.enabled,
                    onCheckedChange = { EqualizerController.setEnabled(it) },
                )
            }

            Spacer(Modifier.height(12.dp))

            // Preset chips
            PresetChips(state = s)

            Spacer(Modifier.height(16.dp))

            // Band sliders
            s.bands.forEach { band ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = band.freqLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.width(52.dp),
                    )
                    Slider(
                        value = band.levelMilliBel.toFloat(),
                        onValueChange = { EqualizerController.setBandLevel(band.index, it.toInt()) },
                        valueRange = band.minLevel.toFloat()..band.maxLevel.toFloat(),
                        enabled = s.enabled,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = band.dbLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.width(52.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PresetChips(state: EqState) {
    val presets = EqPreset.entries.filter { it != EqPreset.CUSTOM || state.preset == EqPreset.CUSTOM }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        presets.forEach { preset ->
            FilterChip(
                selected = state.preset == preset,
                onClick = { if (preset != EqPreset.CUSTOM) EqualizerController.applyPreset(preset) },
                label = { Text(preset.label) },
            )
        }
    }
}
