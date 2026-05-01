package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.PlayerSettings
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.launch

@Composable
fun ThemeScreen(onBack: () -> Unit) {
    val settings = PlayerSettings.instance
    val scope = rememberCoroutineScope()
    val selected by settings.theme.collectAsState(initial = "dark")
    val options = listOf(
        "dark" to "Scuro",
        "light" to "Chiaro",
        "system" to "Sistema",
    )
    SettingsSubScreen(title = "Tema", onBack = onBack) {
        SettingsCard(eyebrow = "Aspetto") {
            options.forEachIndexed { i, (id, label) ->
                SettingsRadioRow(
                    label = label,
                    selected = id == selected,
                    onClick = { scope.launch { settings.setTheme(id) } },
                    showDivider = i < options.lastIndex,
                )
            }
        }
        SettingsCard(eyebrow = "Nota") {
            Text(
                text = "MusicHub è progettato per il tema scuro — gradienti, " +
                    "copertine generative e accento lime sono pensati su sfondi profondi. " +
                    "Il tema chiaro è sperimentale.",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                modifier = Modifier.padding(14.dp),
            )
        }
    }
}
