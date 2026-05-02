package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddToPhotos
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.theme.CoverShapes
import kotlinx.coroutines.launch

/**
 * "Add <song> to playlist" bottom sheet.
 *
 * Triggered from a long-press on a [SongRow][com.mediaplayer.android.ui.search.SongRow].
 * Loads the user's playlists on open so the list is always fresh — a
 * new playlist created from Playlists tab should be immediately pickable.
 *
 * UI contract:
 *  - Tap a playlist row -> add song, close sheet.
 *  - Tap "+ New playlist" row -> inline create dialog, then add song.
 *
 * Self-contained in terms of data: owns a [PlaylistRepository] directly
 * rather than piping through a ViewModel. The sheet is short-lived and
 * has no state worth preserving across config changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    songTitle: String,
    songId: Long,
    repository: PlaylistRepository = remember { PlaylistRepository() },
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onAdded: (playlistName: String) -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var playlists by remember { mutableStateOf<List<PlaylistDto>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var createOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        errorMessage = null
        try {
            playlists = repository.list()
        } catch (t: Throwable) {
            errorMessage = friendlyMessage(t)
        } finally {
            loading = false
        }
    }

    fun addTo(playlist: PlaylistDto) {
        scope.launch {
            try {
                repository.addSong(playlist.id, songId)
                onAdded(playlist.name)
                onDismiss()
            } catch (t: Throwable) {
                errorMessage = "Couldn't add: ${t.message ?: "unknown error"}"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Add to playlist",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = songTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.size(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (onPlayNext != null) {
                QueueActionRow(
                    label = "Play next",
                    icon = { Icon(Icons.Filled.SkipNext, contentDescription = null) },
                    onClick = { onPlayNext(); onDismiss() },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (onAddToQueue != null) {
                QueueActionRow(
                    label = "Add to queue",
                    icon = { Icon(Icons.Filled.AddToPhotos, contentDescription = null) },
                    onClick = { onAddToQueue(); onDismiss() },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (onDownload != null) {
                QueueActionRow(
                    label = "Download",
                    icon = { Icon(Icons.Filled.FileDownload, contentDescription = null) },
                    onClick = { onDownload(); onDismiss() },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            // New-playlist row always pinned at the top for discoverability.
            NewPlaylistRow(onClick = { createOpen = true })
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when {
                loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
                errorMessage != null && playlists.isEmpty() -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                playlists.isEmpty() -> EmptyState(
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    title = "No playlists yet",
                    subtitle = "Tap + New playlist to create one.",
                )
                else -> LazyColumn {
                    items(items = playlists, key = { it.id }) { p ->
                        PlaylistPickerRow(
                            playlist = p,
                            onClick = { addTo(p) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            // Inline error banner for add-failures once the list is loaded.
            if (errorMessage != null && playlists.isNotEmpty()) {
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }

            Spacer(Modifier.size(8.dp))
        }
    }

    if (createOpen) {
        CreateAndAddDialog(
            onDismiss = { createOpen = false },
            onConfirm = { name ->
                scope.launch {
                    try {
                        val created = repository.create(name)
                        repository.addSong(created.id, songId)
                        onAdded(created.name)
                        createOpen = false
                        onDismiss()
                    } catch (t: Throwable) {
                        errorMessage = "Couldn't create: ${t.message ?: "unknown error"}"
                        createOpen = false
                    }
                }
            },
        )
    }
}

@Composable
private fun PlaylistPickerRow(
    playlist: PlaylistDto,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (playlist.songCount == 1) "1 song" else "${playlist.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun QueueActionRow(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun NewPlaylistRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = "New playlist",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CreateAndAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Playlist name") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text.trim()) },
                enabled = text.trim().isNotEmpty(),
            ) { Text("Create & add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
