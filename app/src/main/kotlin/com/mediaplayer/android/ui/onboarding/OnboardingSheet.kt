package com.mediaplayer.android.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

/**
 * One-time onboarding sheet shown on first launch (gated by
 * {@code ChangelogPreferences.lastSeenVersion() == null}). Sets expectations
 * about what the app does — a personal music library streamed from the user's
 * own backend, with Spotify import and Android Auto support.
 *
 * Distinct from {@code ChangelogSheet}: an upgrade shows the changelog,
 * a *first* install shows this sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MediaPlayerSpacing.L, vertical = MediaPlayerSpacing.M),
            verticalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.M),
        ) {
            Text(
                text = "Benvenuto in MusicHub",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "La tua libreria musicale, in streaming dal tuo server. " +
                    "Accedi con Google — la tua libreria ti segue anche in auto.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(MediaPlayerSpacing.S))

            FeatureRow(
                icon = Icons.Filled.LibraryMusic,
                title = "Costruisci la tua libreria",
                subtitle = "Importa una playlist da Spotify oppure scopri nuova musica dalla scheda Scopri.",
            )
            FeatureRow(
                icon = Icons.Filled.MusicNote,
                title = "Metti mi piace e organizza",
                subtitle = "Aggiungi al cuore ciò che ami; alle playlist automatiche pensiamo noi.",
            )
            FeatureRow(
                icon = Icons.Filled.PlayCircle,
                title = "Connetti alla tua auto",
                subtitle = "Android Auto carica la tua libreria, la ricerca vocale, il timer di sospensione e altro.",
            )

            Spacer(Modifier.height(MediaPlayerSpacing.S))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Inizia") }

            Spacer(Modifier.height(MediaPlayerSpacing.S))
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.M),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

