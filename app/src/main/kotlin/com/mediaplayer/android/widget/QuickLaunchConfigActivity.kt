package com.mediaplayer.android.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.ui.theme.MediaPlayerTheme
import kotlinx.coroutines.launch

/**
 * Configuration activity for [QuickLaunchWidget]. Picks 4 of the available
 * [QuickLaunchKind] slots, persists them to [QuickLaunchPrefs], then asks
 * Glance to repaint the new widget instance.
 *
 * Wired via the `android:configure` attribute on the widget provider XML;
 * the system launches it once when the user drops the widget on the home
 * screen, expecting `RESULT_OK` + `EXTRA_APPWIDGET_ID` on success.
 */
@UnstableApi
class QuickLaunchConfigActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // System default — until we explicitly setResult(OK) below, removing
        // the widget if the user backs out is the correct behavior.
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MediaPlayerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfigScreen(
                        onConfirm = { tiles ->
                            lifecycleScope.launch {
                                QuickLaunchPrefs.setTiles(this@QuickLaunchConfigActivity, appWidgetId, tiles)
                                QuickLaunchWidget().updateAll(this@QuickLaunchConfigActivity)
                                val result = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                                setResult(Activity.RESULT_OK, result)
                                finish()
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfigScreen(onConfirm: (List<QuickLaunchKind>) -> Unit) {
    val selected = remember {
        mutableStateListOf<QuickLaunchKind>().apply { addAll(QuickLaunchPrefs.DEFAULT_TILES) }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "// WIDGET · LANCIO RAPIDO",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Scegli 4 playlist da mostrare",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Tocca un riquadro selezionato per rimuoverlo, poi tocca un'altra voce per aggiungerla.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Text(
            text = "// SELEZIONE · ${selected.size} / 4",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            selected.forEach { k ->
                SelectionChip(k, onRemove = { selected.remove(k) })
            }
        }
        Spacer(Modifier.height(16.dp))

        Text(
            text = "// DISPONIBILI",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(QuickLaunchKind.values().toList()) { k ->
                val taken = selected.contains(k)
                AvailableRow(
                    kind = k,
                    taken = taken,
                    enabled = taken || selected.size < 4,
                    onClick = {
                        if (taken) selected.remove(k)
                        else if (selected.size < 4) selected.add(k)
                    },
                )
                Spacer(Modifier.height(6.dp))
            }
        }
        Button(
            onClick = { if (selected.size == 4) onConfirm(selected.toList()) },
            enabled = selected.size == 4,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (selected.size == 4) "Aggiungi widget" else "Seleziona ${4 - selected.size} altre voci")
        }
    }
}

@Composable
private fun SelectionChip(kind: QuickLaunchKind, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onRemove)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = kind.label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun AvailableRow(
    kind: QuickLaunchKind,
    taken: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val bg = when {
        taken -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        !enabled -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (taken) MaterialTheme.colorScheme.primary else Color.Transparent),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = kind.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
