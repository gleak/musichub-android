package com.mediaplayer.android.ui.playlists

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.SpotifyImportTrack

@Composable
fun SpotifyImportScreen(
    modifier: Modifier = Modifier,
    viewModel: SpotifyImportViewModel = viewModel(),
    onBack: () -> Unit,
    onPlaylistCreated: (playlistId: Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
            }
            Text(
                text = "Importa da Spotify",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when (val s = state) {
            SpotifyImportUiState.Idle ->
                IdleContent(onFilePicked = viewModel::importFromUri)

            SpotifyImportUiState.FetchingPlaylist ->
                CenteredSpinnerWithLabel("Leggo il file…")

            is SpotifyImportUiState.Confirming ->
                ConfirmingContent(
                    state = s,
                    onStartImport = viewModel::startImport,
                    onCancel = viewModel::reset,
                )

            SpotifyImportUiState.Importing ->
                CenteredSpinnerWithLabel("Importo la playlist…")

            is SpotifyImportUiState.Done ->
                DoneContent(
                    state = s,
                    onViewPlaylist = { onPlaylistCreated(s.playlistId) },
                    onBack = onBack,
                )

            is SpotifyImportUiState.Error ->
                ErrorContent(message = s.message, onRetry = viewModel::reset)
        }
    }
}

@Composable
private fun IdleContent(onFilePicked: (Uri) -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(onFilePicked) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Come esportare la playlist da Spotify:",
            style = MaterialTheme.typography.titleSmall,
        )
        StepRow(step = "1", text = "Tocca il pulsante qui sotto per aprire Exportify")
        StepRow(step = "2", text = "Accedi con Spotify ed esporta la playlist in CSV")
        StepRow(step = "3", text = "Salva il file sul telefono, poi importalo qui")

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://exportify.net"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Apri Exportify")
        }

        Button(
            onClick = { launcher.launch("*/*") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Scegli file CSV")
        }
    }
}

@Composable
private fun StepRow(step: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 1.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ConfirmingContent(
    state: SpotifyImportUiState.Confirming,
    onStartImport: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var playlistName by remember(state.playlistName) { mutableStateOf(state.playlistName) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${state.tracks.size} brani trovati",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nome playlist") },
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                TextButton(onClick = onCancel) { Text("Annulla") }
                Button(
                    onClick = { onStartImport(playlistName) },
                    enabled = playlistName.trim().isNotEmpty(),
                ) {
                    Text("Avvia import")
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(
                items = state.tracks,
                key = { index, _ -> index },
            ) { _, track ->
                TrackPreviewRow(track)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun TrackPreviewRow(track: SpotifyImportTrack) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DoneContent(
    state: SpotifyImportUiState.Done,
    onViewPlaylist: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(text = "Import completato", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = buildString {
                val s = if (state.matched == 1) "o" else "i"
                append("${state.matched} bran$s aggiunt$s a \"${state.playlistName}\"")
                if (state.queued > 0) append("\n${state.queued} in scaricamento — saranno aggiunti quando pronti")
                if (state.failed > 0) append("\n${state.failed} non trovati")
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.weight(1f))
        Button(onClick = onViewPlaylist, modifier = Modifier.fillMaxWidth()) {
            Text("Apri playlist")
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Torna alle playlist")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        FilledTonalButton(onClick = onRetry) { Text("Try Again") }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CenteredSpinnerWithLabel(label: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
