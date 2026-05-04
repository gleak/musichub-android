package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.SharePreviewDto
import kotlinx.coroutines.launch

/**
 * Dialog flow for importing a shared playlist. Driven by a token coming
 * out of MainActivity's incoming-intent handler.
 *
 * Phases:
 *  1. Loading — fetch the preview.
 *  2a. Already accessible — preview reports the user already owns the
 *      playlist or is already a member; navigate straight in instead of
 *      asking to "import" (the model is collaborative since 0.13.0, so a
 *      second accept would be a no-op anyway).
 *  2b. Confirm — show owner + name + song count, ask user to join.
 *  3. Done / error — surface the error inline then let user dismiss.
 *
 * Joining grants edit rights — the recipient and owner edit the same row,
 * not a snapshot copy.
 */
@Composable
fun PlaylistShareImporter(
    token: String,
    onDismiss: () -> Unit,
    onImported: (playlistId: Long, playlistName: String) -> Unit,
    repository: PlaylistRepository = remember { PlaylistRepository() },
) {
    var preview by remember { mutableStateOf<SharePreviewDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(token) {
        try {
            val p = repository.previewShare(token)
            if (p.alreadyAccessible) {
                // Already in your library — accept() returns the existing
                // detail without creating a duplicate. Skip the dialog and
                // route the user straight to the playlist they already have.
                val detail = repository.acceptShare(token)
                onImported(detail.id, detail.name)
            } else {
                preview = p
            }
        } catch (t: Throwable) {
            error = t.message ?: "Impossibile caricare la playlist condivisa"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text("Aggiungi alla libreria?") },
        text = {
            when {
                error != null -> Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                preview == null -> Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                    Text("Carico la playlist condivisa…")
                }
                else -> {
                    val p = preview!!
                    val songs = if (p.songCount == 1) "1 brano" else "${p.songCount} brani"
                    Text(
                        text = "${p.ownerName} ha condiviso \"${p.playlistName}\" ($songs).\n" +
                                "Vedrai gli aggiornamenti in tempo reale e potrai modificarla insieme a ${p.ownerName}.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        confirmButton = {
            if (error == null) {
                TextButton(
                    enabled = preview != null && !importing,
                    onClick = {
                        importing = true
                        scope.launch {
                            try {
                                val detail = repository.acceptShare(token)
                                onImported(detail.id, detail.name)
                            } catch (t: Throwable) {
                                error = t.message ?: "Importazione non riuscita"
                                importing = false
                            }
                        }
                    },
                ) {
                    if (importing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Aggiungi")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !importing) { Text("Chiudi") }
        },
    )
}
