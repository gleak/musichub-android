package com.mediaplayer.android.ui.local

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.local.LocalPlaylist
import com.mediaplayer.android.data.local.LocalTrack
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Detail screen for an on-device [LocalPlaylist]. Lets the user play /
 * shuffle the playlist, drag-reorder rows, swipe-remove a row, rename
 * the playlist, or delete it. All edits go through [LocalLibraryViewModel]
 * which writes to [com.mediaplayer.android.data.local.LocalPlaylistStore].
 *
 * Snapshot semantics: tracks resolve through the in-memory library — if a
 * file got deleted from disk between scans, the row simply disappears
 * (we don't try to surface dead ids).
 */
@Composable
fun LocalPlaylistDetailScreen(
    playlistId: String,
    onBack: () -> Unit,
    onPlay: (LocalTrack, List<LocalTrack>) -> Unit,
    onShuffle: (List<LocalTrack>) -> Unit,
    onPlayNext: (LocalTrack) -> Unit,
    onAddToQueue: (LocalTrack) -> Unit,
    viewModel: LocalLibraryViewModel = viewModel(),
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val playlist = playlists.firstOrNull { it.id == playlistId }
    val tracks: List<LocalTrack> = remember(playlist, state) {
        if (playlist == null) emptyList()
        else viewModel.resolveTracks(playlist.trackIds)
    }
    var renameOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var rowMenuFor by remember { mutableStateOf<LocalTrack?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Header(
            title = playlist?.name.orEmpty().ifBlank { "Playlist" },
            onBack = onBack,
            onMore = { menuOpen = true },
            menuOpen = menuOpen,
            onMenuDismiss = { menuOpen = false },
            onRename = {
                menuOpen = false
                renameOpen = true
            },
            onDelete = {
                menuOpen = false
                deleteOpen = true
            },
        )

        if (playlist == null) {
            CenteredMessage(text = "Playlist non trovata.")
            return@Column
        }

        if (tracks.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.PlayArrow,
                title = "Nessun brano qui",
                subtitle = "Aggiungi brani dalla scheda Brani o crea una playlist da una cartella.",
            )
            return@Column
        }

        ActionBar(
            count = tracks.size,
            onPlay = { onPlay(tracks.first(), tracks) },
            onShuffle = { onShuffle(tracks) },
        )
        Spacer(Modifier.height(MediaPlayerSpacing.S))

        TrackList(
            playlist = playlist,
            tracks = tracks,
            rowMenuFor = rowMenuFor,
            onClickTrack = { onPlay(it, tracks) },
            onMore = { rowMenuFor = it },
            onRowMenuDismiss = { rowMenuFor = null },
            onPlayNext = onPlayNext,
            onAddToQueue = onAddToQueue,
            onRemoveFromPlaylist = { trackId ->
                viewModel.removeTrackFromPlaylist(playlist.id, trackId)
            },
            onReorder = { newOrder ->
                viewModel.reorderPlaylist(playlist.id, newOrder)
            },
        )
    }

    if (renameOpen && playlist != null) {
        RenameDialog(
            initialName = playlist.name,
            onDismiss = { renameOpen = false },
            onConfirm = { newName ->
                viewModel.renamePlaylist(playlist.id, newName)
                renameOpen = false
            },
        )
    }

    if (deleteOpen && playlist != null) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("Elimina playlist") },
            text = { Text("Vuoi eliminare \"${playlist.name}\"? I brani sul dispositivo restano.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteOpen = false
                    viewModel.deletePlaylist(playlist.id)
                    onBack()
                }) { Text("Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { deleteOpen = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun Header(
    title: String,
    onBack: () -> Unit,
    onMore: () -> Unit,
    menuOpen: Boolean,
    onMenuDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.S, vertical = MediaPlayerSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Indietro",
            )
        }
        Spacer(Modifier.width(MediaPlayerSpacing.Xs))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "// PLAYLIST LOCALE",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo2,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MHColors.TextHi,
                maxLines = 1,
            )
        }
        Box {
            IconButton(onClick = onMore) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Altre opzioni")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                DropdownMenuItem(
                    text = { Text("Rinomina") },
                    onClick = onRename,
                    leadingIcon = {
                        Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Elimina playlist") },
                    onClick = onDelete,
                    leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null) },
                )
            }
        }
    }
}

@Composable
private fun ActionBar(count: Int, onPlay: () -> Unit, onShuffle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MediaPlayerSpacing.S),
    ) {
        FilledTonalButton(onClick = onPlay) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(MediaPlayerSpacing.Xs))
            Text("Riproduci")
        }
        FilledTonalButton(onClick = onShuffle) {
            Icon(
                imageVector = Icons.Filled.Shuffle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(MediaPlayerSpacing.Xs))
            Text("Casuale")
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count brani",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
        )
    }
}

@Composable
private fun TrackList(
    playlist: LocalPlaylist,
    tracks: List<LocalTrack>,
    rowMenuFor: LocalTrack?,
    onClickTrack: (LocalTrack) -> Unit,
    onMore: (LocalTrack) -> Unit,
    onRowMenuDismiss: () -> Unit,
    onPlayNext: (LocalTrack) -> Unit,
    onAddToQueue: (LocalTrack) -> Unit,
    onRemoveFromPlaylist: (Long) -> Unit,
    onReorder: (List<Long>) -> Unit,
) {
    val lazyState = rememberLazyListState()
    var entries by remember(playlist.id, tracks) { mutableStateOf(tracks) }

    val reorderState = rememberReorderableLazyListState(lazyState) { from, to ->
        val fromIdx = from.index
        val toIdx = to.index
        if (fromIdx !in entries.indices || toIdx !in entries.indices) return@rememberReorderableLazyListState
        entries = entries.toMutableList().apply { add(toIdx, removeAt(fromIdx)) }
    }

    LaunchedEffect(reorderState) {
        var draggedAtLeastOnce = false
        snapshotFlow { reorderState.isAnyItemDragging }.collect { dragging ->
            if (dragging) draggedAtLeastOnce = true
            else if (draggedAtLeastOnce) {
                onReorder(entries.map { it.id })
                draggedAtLeastOnce = false
            }
        }
    }

    LazyColumn(
        state = lazyState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        items(entries, key = { it.id }) { t ->
            ReorderableItem(reorderState, key = t.id) {
                var dismissed by remember(t.id) { mutableStateOf(false) }
                val dismissState = rememberSwipeToDismissBoxState()
                LaunchedEffect(dismissState.currentValue) {
                    if (!dismissed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                        dismissed = true
                        entries = entries.filterNot { it.id == t.id }
                        onRemoveFromPlaylist(t.id)
                    }
                }
                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .padding(end = MediaPlayerSpacing.M),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Rimuovi",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    },
                ) {
                    Box(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        LocalTrackRow(
                            track = t,
                            onClick = { onClickTrack(t) },
                            onMore = { onMore(t) },
                            modifier = Modifier.longPressDraggableHandle(),
                        )
                        if (rowMenuFor?.id == t.id) {
                            DropdownMenu(
                                expanded = true,
                                onDismissRequest = onRowMenuDismiss,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Riproduci") },
                                    onClick = {
                                        onRowMenuDismiss()
                                        onClickTrack(t)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Riproduci dopo") },
                                    onClick = {
                                        onRowMenuDismiss()
                                        onPlayNext(t)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Aggiungi alla coda") },
                                    onClick = {
                                        onRowMenuDismiss()
                                        onAddToQueue(t)
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("Rimuovi dalla playlist") },
                                    onClick = {
                                        onRowMenuDismiss()
                                        entries = entries.filterNot { it.id == t.id }
                                        onRemoveFromPlaylist(t.id)
                                    },
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rinomina playlist") },
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
                enabled = text.isNotBlank() && text.trim() != initialName,
            ) { Text("Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}
