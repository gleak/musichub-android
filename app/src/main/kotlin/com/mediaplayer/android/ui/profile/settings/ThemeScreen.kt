package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

/**
 * Settings → Tema. Mockup `mh-settings.jsx:120-153`: lead-in paragraph,
 * three large clickable cards in a vertical grid each with a 56×56 colour
 * preview tile + label + lime check on the selected card; selected card
 * gets a lime translucent fill + 1.5dp lime inset border.
 */
@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val settings = PlayerSettings.instance
    val scope = rememberCoroutineScope()
    val selected by settings.theme.collectAsState(initial = "dark")
    val options = listOf(
        ThemeOption(id = "light", label = "Chiaro", preview = ThemePreview.Light),
        ThemeOption(id = "dark", label = "Scuro", preview = ThemePreview.Dark),
        ThemeOption(id = "system", label = "Sistema", preview = ThemePreview.System),
    )
    SettingsSubScreen(title = "Tema", onBack = onBack) {
        Text(
            text = "L'app rispetta il tema di sistema per default. Scegli " +
                "\"Chiaro\" o \"Scuro\" per fissarlo. Il tema chiaro è sperimentale: " +
                "i gradienti, le copertine generative e l'accento lime sono " +
                "pensati su sfondi profondi.",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
            modifier = Modifier.fillMaxWidth(),
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { opt ->
                ThemeCard(
                    option = opt,
                    selected = opt.id == selected,
                    onClick = { scope.launch { settings.setTheme(opt.id) } },
                )
            }
        }
    }
}

private data class ThemeOption(val id: String, val label: String, val preview: ThemePreview)

private sealed class ThemePreview {
    data object Light : ThemePreview()
    data object Dark : ThemePreview()
    data object System : ThemePreview()
}

@Composable
private fun ThemeCard(option: ThemeOption, selected: Boolean, onClick: () -> Unit) {
    val limeTint = MHColors.Lime.copy(alpha = 0.08f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) limeTint else MHColors.Card)
            .let {
                if (selected) it.border(1.5.dp, MHColors.Lime, RoundedCornerShape(14.dp))
                else it
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemePreviewTile(preview = option.preview)
        Spacer(Modifier.width(14.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.titleMedium,
            color = MHColors.TextHi,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MHColors.Lime,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun ThemePreviewTile(preview: ThemePreview) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                when (preview) {
                    ThemePreview.Dark -> Brush.linearGradient(
                        listOf(Color(0xFF1F1F1F), Color(0xFF080808)),
                    )
                    ThemePreview.Light -> Brush.linearGradient(
                        listOf(Color(0xFFFAFAFA), Color(0xFFE8E8E8)),
                    )
                    // 135deg diagonal split — top-left dark, bottom-right light.
                    ThemePreview.System -> Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to Color(0xFF0A0A0A),
                            0.5f to Color(0xFF0A0A0A),
                            0.5001f to Color(0xFFF4F2EC),
                            1f to Color(0xFFF4F2EC),
                        ),
                    )
                },
            ),
    )
}
