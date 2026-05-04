package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.common.displayInitial
import com.mediaplayer.android.ui.theme.MHColors

private enum class LibraryFilter { Playlists, Albums, Artisti, Scaricati }

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel(),
    onPlaylistClick: (PlaylistDto) -> Unit = {},
    onLikedSongsClick: () -> Unit = {},
    onSpotifyImport: () -> Unit = {},
    onFindClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var createOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(LibraryFilter.Playlists) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LibraryTopBar(
                onSearch = onFindClick,
                onAdd = { createOpen = true },
                onProfileClick = onProfileClick,
            )
            FilterRow(filter = filter, onChange = { filter = it })

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::pullRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val s = state) {
                    PlaylistsUiState.Loading -> CenteredSpinner()
                    is PlaylistsUiState.Error -> ErrorWithRetry(
                        message = "Couldn't load playlists.\n${s.message}",
                        onRetry = viewModel::refresh,
                    )
                    is PlaylistsUiState.Success -> LibraryList(
                        playlists = s.playlists,
                        filter = filter,
                        onPlaylistClick = onPlaylistClick,
                        onLikedSongsClick = onLikedSongsClick,
                        onSpotifyImport = onSpotifyImport,
                        onDelete = viewModel::delete,
                    )
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { createOpen = true },
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Nuova") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 16.dp),
        )
    }

    if (createOpen) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                viewModel.create(name)
                createOpen = false
            },
            onDismiss = { createOpen = false },
        )
    }
}

@Composable
private fun LibraryTopBar(
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    onProfileClick: () -> Unit,
) {
    val currentUser = LocalCurrentUser.current
    val initial = currentUser?.user?.displayInitial().orEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (initial.isEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Libreria",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSearch) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Find",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onAdd) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onProfileClick) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "Profilo",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun FilterRow(filter: LibraryFilter, onChange: (LibraryFilter) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(LibraryFilter.values().toList()) { f ->
            FilterChip(
                selected = filter == f,
                onClick = { onChange(f) },
                label = {
                    Text(
                        text = when (f) {
                            LibraryFilter.Playlists -> "Playlist"
                            LibraryFilter.Albums -> "Album"
                            LibraryFilter.Artisti -> "Artisti"
                            LibraryFilter.Scaricati -> "Scaricati"
                        },
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onSurface,
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
private fun LibraryList(
    playlists: List<PlaylistDto>,
    filter: LibraryFilter,
    onPlaylistClick: (PlaylistDto) -> Unit,
    onLikedSongsClick: () -> Unit,
    onSpotifyImport: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    // 2-col grid for playlists (Spotify-tile style); Liked + Spotify-import
    // anchors span the full width via maxLineSpan. Mixing list-shaped rows
    // and grid tiles in one LazyVerticalGrid avoids the nested-scroll issues
    // a LazyColumn-wrapping-LazyVerticalGrid would create.
    LazyVerticalGrid(
        columns = GridCells.Fixed(1),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        when (filter) {
            LibraryFilter.Playlists -> {
                val userPlaylists = playlists.filterNot { it.isAuto }
                item(key = "liked", span = { GridItemSpan(maxLineSpan) }) {
                    LikedSongsRow(onClick = onLikedSongsClick)
                }
                if (userPlaylists.isEmpty()) {
                    item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                        EmptyPlaylistMessage()
                    }
                }
                items(items = userPlaylists, key = { it.id }) { p ->
                    PlaylistTile(
                        playlist = p,
                        onClick = { onPlaylistClick(p) },
                        onDelete = { onDelete(p.id) },
                    )
                }
                item(key = "spotify_import", span = { GridItemSpan(maxLineSpan) }) {
                    ImportFromSpotifyRow(onClick = onSpotifyImport)
                }
            }
            LibraryFilter.Albums, LibraryFilter.Artisti, LibraryFilter.Scaricati -> {
                item(key = "tab_placeholder", span = { GridItemSpan(maxLineSpan) }) {
                    TabPlaceholder(filter)
                }
            }
        }
    }
}

@Composable
private fun TabPlaceholder(filter: LibraryFilter) {
    val title = when (filter) {
        LibraryFilter.Albums -> "Album"
        LibraryFilter.Artisti -> "Artisti"
        LibraryFilter.Scaricati -> "Scaricati"
        else -> ""
    }
    val sub = when (filter) {
        LibraryFilter.Albums -> "Apri \"Album\" dal menu di ricerca per sfogliare la libreria completa."
        LibraryFilter.Artisti -> "Apri \"Artisti\" dal menu di ricerca per vedere tutti gli artisti."
        LibraryFilter.Scaricati -> "I brani scaricati per l'ascolto offline appariranno qui."
        else -> ""
    }
    EmptyState(
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        title = title,
        subtitle = sub,
    )
}

@Composable
private fun EmptyPlaylistMessage() {
    EmptyState(
        icon = Icons.AutoMirrored.Filled.QueueMusic,
        title = "No playlists yet",
        subtitle = "Tap + to create your first one.",
    )
}

@Composable
private fun LikedSongsRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CoverShapes.SongRow)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MHColors.LikedGradientStart,
                            MHColors.LikedGradientEnd,
                        )
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Brani preferiti",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Playlist",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImportFromSpotifyRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CoverShapes.SongRow)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.LibraryAdd,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "Import from Spotify",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Bring playlists into your library",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PlaylistTile(
    playlist: PlaylistDto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    val cover = playlist.coverSongId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                // Auto-playlists are server-managed and can't be deleted; long-press
                // only fires the confirm dialog for user playlists.
                onLongClick = if (playlist.isAuto) null else { -> confirmDelete = true },
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CoverShapes.SongRow)
                .background(
                    if (playlist.isAuto) {
                        com.mediaplayer.android.ui.common.autoPlaylistGradient(playlist.kind)
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surfaceVariant,
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (cover != null && !playlist.isAuto) {
                coil3.compose.AsyncImage(
                    model = com.mediaplayer.android.data.Network.coverUrl(cover),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = if (playlist.isAuto) Icons.Filled.AutoAwesome
                    else Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = if (playlist.isAuto) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = buildString {
                append(if (playlist.isAuto) "Per te" else "Playlist")
                append(" · ")
                append(pluralizeSongs(playlist.songCount))
                if (!playlist.isAuto) {
                    if (!playlist.isOwner && !playlist.ownerName.isNullOrBlank()) {
                        // Member of someone else's playlist — surface whose it is
                        // so the user remembers why it's in their library.
                        append(" · Condivisa da ${playlist.ownerName}")
                    } else if (playlist.isOwner && playlist.memberCount > 0) {
                        // You shared this playlist with N other users.
                        append(
                            if (playlist.memberCount == 1) " · Condivisa con 1 persona"
                            else " · Condivisa con ${playlist.memberCount} persone"
                        )
                    }
                }
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }

    if (confirmDelete) {
        // Members see "Remove from library" — their action just drops the
        // membership row server-side; the playlist keeps living for the
        // owner and other members. Owners see the destructive "Delete"
        // copy because their action cascades.
        val isMember = !playlist.isOwner
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(if (isMember) "Rimuovi dalla libreria?" else "Eliminare la playlist?") },
            text = {
                Text(
                    if (isMember) "\"${playlist.name}\" sparirà dalla tua libreria. " +
                        "Continuerà ad esistere per ${playlist.ownerName ?: "il proprietario"} " +
                        "e gli altri membri."
                    else "\"${playlist.name}\" verrà eliminata definitivamente."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text(if (isMember) "Rimuovi" else "Elimina") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun CreatePlaylistDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova playlist") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text("Nome playlist") },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.trim().isNotEmpty(),
            ) { Text("Crea") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        },
    )
}


private fun pluralizeSongs(count: Int): String =
    if (count == 1) "1 brano" else "$count brani"
