package com.mediaplayer.android.ui.local

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

/**
 * Bottom-sheet picker for "Aggiungi a playlist locale". Shows existing
 * local playlists + a "Crea nuova playlist" row. Tapping an existing
 * playlist appends [trackIds] (de-duplicated server-side via
 * [LocalLibraryViewModel.addTracksToPlaylist]); the create row pops a
 * tiny rename dialog and creates the playlist seeded with [trackIds].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToLocalPlaylistSheet(
    trackIds: List<Long>,
    onDismiss: () -> Unit,
    onAdded: (playlistName: String) -> Unit = {},
    suggestedName: String? = null,
    viewModel: LocalLibraryViewModel = viewModel(),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var createOpen by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Aggiungi a playlist locale",
                style = MaterialTheme.typography.titleMedium,
                color = MHColors.TextHi,
                modifier = Modifier.padding(
                    horizontal = MediaPlayerSpacing.M,
                    vertical = MediaPlayerSpacing.S,
                ),
            )
            LazyColumn(
                contentPadding = PaddingValues(
                    horizontal = MediaPlayerSpacing.M,
                    vertical = MediaPlayerSpacing.S,
                ),
                verticalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
            ) {
                item(key = "create_new") {
                    CreateRow(onClick = { createOpen = true })
                }
                items(playlists, key = { it.id }) { p ->
                    PlaylistPickRow(
                        name = p.name,
                        count = p.trackIds.size,
                        onClick = {
                            viewModel.addTracksToPlaylist(p.id, trackIds)
                            onAdded(p.name)
                            onDismiss()
                        },
                    )
                }
            }
            Spacer(Modifier.height(MediaPlayerSpacing.M))
        }
    }

    if (createOpen) {
        CreatePlaylistDialog(
            initial = suggestedName.orEmpty(),
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                createOpen = false
                viewModel.createPlaylist(name, trackIds)
                onAdded(name)
                onDismiss()
            },
        )
    }
}

@Composable
private fun CreateRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.CardHigh)
            .clickable(onClick = onClick)
            .padding(MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MHColors.Lime),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.M))
        Text(
            text = "Crea nuova playlist",
            style = MaterialTheme.typography.titleSmall,
            color = MHColors.TextHi,
        )
    }
}

@Composable
private fun PlaylistPickRow(name: String, count: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MHColors.Card)
            .clickable(onClick = onClick)
            .padding(MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MHColors.CardHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MHColors.Lime,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.M))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                color = MHColors.TextHi,
                maxLines = 1,
            )
            Text(
                text = "$count brani",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nome") },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text("Crea") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
