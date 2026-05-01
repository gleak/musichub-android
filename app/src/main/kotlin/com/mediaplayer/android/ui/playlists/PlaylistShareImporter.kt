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
 * out of MainActivity's incoming-intent handler. Three phases:
 *  1. Loading — fetch the preview.
 *  2. Confirm — show owner + name + song count, ask user to import.
 *  3. Done / error — toast-equivalent then dismiss.
 *
 * The recipient gets a *copy* of the playlist; the link does not grant
 * editing rights to the original.
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
            preview = repository.previewShare(token)
        } catch (t: Throwable) {
            error = t.message ?: "Couldn't load shared playlist"
        }
    }

    AlertDialog(
        onDismissRequest = { if (!importing) onDismiss() },
        title = { Text("Import playlist?") },
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
                    Text("Loading shared playlist…")
                }
                else -> {
                    val p = preview!!
                    val songs = if (p.songCount == 1) "1 song" else "${p.songCount} songs"
                    Text(
                        text = "${p.ownerName} shared \"${p.playlistName}\" ($songs).\n" +
                                "A copy will be added to your library.",
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
                                error = t.message ?: "Import failed"
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
                        Text("Import")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !importing) { Text("Close") }
        },
    )
}
