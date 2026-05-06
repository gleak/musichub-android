package com.mediaplayer.android.ui.player

import android.content.Context
import android.media.AudioManager
import android.media.AudioDeviceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.playback.EqPreset
import com.mediaplayer.android.playback.EqState
import com.mediaplayer.android.playback.EqualizerController
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Equalizer bottom sheet — mockup `mh-player-sheets.jsx:101-168`. Replaces
 * the old horizontal-slider chrome with a vertical-slider column, a
 * single tappable preset row that opens a picker dialog, and a
 * `// SESSIONE AUDIO` info card at the bottom showing audio session +
 * active output device.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(onDismiss: () -> Unit) {
    val state by EqualizerController.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val mono = LocalMHMono.current
    val accent = MaterialTheme.colorScheme.primary
    val ctx = LocalContext.current
    var presetPickerOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            val s = state
            if (s == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Equalizzatore non supportato su questo dispositivo",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@Column
            }

            // Eyebrow + title + ATTIVO pill.
            Text(
                text = "// AUDIO",
                style = mono.eyebrow,
                color = accent,
            )
            Spacer(Modifier.size(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Equalizzatore",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                if (s.enabled) AttivoPill(accent = accent, label = "ATTIVO", style = mono.eyebrow)
                Spacer(Modifier.width(8.dp))
                Switch(
                    checked = s.enabled,
                    onCheckedChange = { EqualizerController.setEnabled(it) },
                )
            }
            Spacer(Modifier.size(12.dp))

            // Single tappable preset row → opens picker dialog.
            PresetCard(
                preset = s.preset,
                onClick = { presetPickerOpen = true },
            )
            Spacer(Modifier.size(20.dp))

            // Vertical slider grid — one column per band. Height fixed so the
            // rotate trick keeps a usable touch surface; bands stretch to fill.
            Row(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                s.bands.forEach { band ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = band.dbLabel,
                            style = mono.duration,
                            color = if (band.levelMilliBel >= 0) accent else MHColors.TextLo,
                        )
                        Spacer(Modifier.size(4.dp))
                        VerticalBandSlider(
                            valueMilliBel = band.levelMilliBel,
                            min = band.minLevel,
                            max = band.maxLevel,
                            enabled = s.enabled,
                            onChange = { EqualizerController.setBandLevel(band.index, it) },
                            accent = accent,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = band.freqLabel,
                            style = mono.duration,
                            color = MHColors.TextLo,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
            Spacer(Modifier.size(20.dp))

            // Audio-session + output-device info card.
            AudioSessionCard(
                audioSessionId = s.audioSessionId,
                output = remember(ctx) { activeOutputLabel(ctx) },
                mono = mono,
                accent = accent,
            )
            Spacer(Modifier.size(16.dp))
        }
    }

    if (presetPickerOpen) {
        AlertDialog(
            onDismissRequest = { presetPickerOpen = false },
            title = { Text("Preset") },
            text = {
                Column {
                    EqPreset.entries.forEach { p ->
                        // CUSTOM is read-only — only surfaces when the user has
                        // edited bands; tapping it inside the picker is a no-op.
                        val tappable = p != EqPreset.CUSTOM
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = tappable) {
                                    EqualizerController.applyPreset(p)
                                    presetPickerOpen = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = p.label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (tappable) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            if (state?.preset == p) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = accent,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { presetPickerOpen = false }) { Text("Chiudi") }
            },
        )
    }
}

@Composable
private fun AttivoPill(
    accent: Color,
    label: String,
    style: androidx.compose.ui.text.TextStyle,
) {
    Box(
        modifier = Modifier
            .background(
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.5f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text = label, style = style, color = accent)
    }
}

@Composable
private fun PresetCard(preset: EqPreset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MHColors.Card,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Preset",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                text = preset.label,
                style = MaterialTheme.typography.titleMedium,
                color = MHColors.TextHi,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MHColors.TextLo,
        )
    }
}

@Composable
private fun VerticalBandSlider(
    valueMilliBel: Int,
    min: Int,
    max: Int,
    enabled: Boolean,
    onChange: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    // Material3 Slider is horizontal-only; rotate it 90° around its layout
    // box so dragging up = positive dB and down = negative. The outer
    // `layout` swaps width/height so the parent reserves vertical room.
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Slider(
            value = valueMilliBel.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            enabled = enabled,
            colors = SliderDefaults.colors(
                activeTrackColor = accent,
                inactiveTrackColor = accent.copy(alpha = 0.2f),
                thumbColor = accent,
            ),
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(
                        androidx.compose.ui.unit.Constraints.fixed(
                            constraints.maxHeight,
                            constraints.maxWidth,
                        )
                    )
                    layout(placeable.height, placeable.width) {
                        placeable.place(
                            x = -(placeable.width - placeable.height) / 2,
                            y = -(placeable.height - placeable.width) / 2,
                        )
                    }
                }
                .rotate(-90f),
        )
    }
}

@Composable
private fun AudioSessionCard(
    audioSessionId: Int,
    output: String,
    mono: com.mediaplayer.android.ui.theme.MHMonoTextStyles,
    accent: Color,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFF0E0E0E),
                shape = RoundedCornerShape(10.dp),
            )
            .padding(14.dp),
    ) {
        Text(
            text = "// SESSIONE AUDIO",
            style = mono.eyebrow,
            color = accent,
        )
        Spacer(Modifier.size(6.dp))
        InfoRow(
            label = "session_id",
            value = if (audioSessionId == 0) "—" else "0x${audioSessionId.toString(16).uppercase()}",
            mono = mono,
        )
        Spacer(Modifier.size(2.dp))
        InfoRow(label = "output", value = output, mono = mono)
    }
}

@Composable
private fun InfoRow(label: String, value: String, mono: com.mediaplayer.android.ui.theme.MHMonoTextStyles) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label:",
            style = mono.duration,
            color = MHColors.TextLo,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = value,
            style = mono.duration,
            color = MHColors.TextHi,
        )
    }
}

/**
 * Best-effort label for the device the player is currently routing to.
 * Picks the first non-internal output device the system reports; falls
 * back to "Altoparlante interno" when only the built-in speaker is
 * available. Not authoritative — Android's audio policy can route
 * differently from what `getDevices` reports — but good enough for the
 * info card.
 */
private fun activeOutputLabel(ctx: Context): String {
    val am = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return "—"
    val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    val nonInternal = devices.firstOrNull {
        it.type != AudioDeviceInfo.TYPE_BUILTIN_SPEAKER &&
            it.type != AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
    }
    return when {
        nonInternal != null -> nonInternal.productName?.toString() ?: deviceTypeLabel(nonInternal.type)
        devices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER } -> "Altoparlante interno"
        else -> "—"
    }
}

private fun deviceTypeLabel(type: Int): String = when (type) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
    AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Cuffie cablate"
    AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Headset cablato"
    AudioDeviceInfo.TYPE_HDMI -> "HDMI"
    AudioDeviceInfo.TYPE_AUX_LINE -> "AUX"
    else -> "Altro"
}
