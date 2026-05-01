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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.mediaplayer.android.ui.common.AnonymousBanner
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.LocalCurrentUser
import com.mediaplayer.android.ui.common.displayInitial
import com.mediaplayer.android.ui.theme.SpotifyColors

private enum class LibraryFilter { All, Playlists, Liked }

@Composable
fun PlaylistsScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = viewModel(),
    onPlaylistClick: (PlaylistDto) -> Unit = {},
    onLikedSongsClick: () -> Unit = {},
    onSpotifyImport: () -> Unit = {},
    onFindClick: () -> Unit = {},
    onSignOut: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var createOpen by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(LibraryFilter.All) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            LibraryTopBar(
                onSearch = onFindClick,
                onAdd = { createOpen = true },
                onSignOut = onSignOut,
            )
            AnonymousBanner()
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
            text = { Text("New") },
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
    onSignOut: () -> Unit,
) {
    val currentUser = LocalCurrentUser.current
    val isAnonymous = currentUser?.user?.anonymous == true
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
            if (isAnonymous || initial.isEmpty()) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = if (isAnonymous) "Guest" else "Account",
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
            text = "Your Library",
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
        IconButton(onClick = onSignOut) {
            Icon(
                imageVector = if (isAnonymous) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                contentDescription = if (isAnonymous) "Sign in" else "Sign out",
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
                            LibraryFilter.All -> "All"
                            LibraryFilter.Playlists -> "Playlists"
                            LibraryFilter.Liked -> "Liked"
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
    val showLiked = filter == LibraryFilter.All || filter == LibraryFilter.Liked
    val showPlaylists = filter == LibraryFilter.All || filter == LibraryFilter.Playlists

    // 2-col grid for playlists (Spotify-tile style); Liked + Spotify-import
    // anchors span the full width via maxLineSpan. Mixing list-shaped rows
    // and grid tiles in one LazyVerticalGrid avoids the nested-scroll issues
    // a LazyColumn-wrapping-LazyVerticalGrid would create.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showLiked) {
            item(key = "liked", span = { GridItemSpan(maxLineSpan) }) {
                LikedSongsRow(onClick = onLikedSongsClick)
            }
        }
        if (showPlaylists && playlists.isEmpty() && filter == LibraryFilter.Playlists) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyPlaylistMessage()
            }
        }
        if (showPlaylists) {
            items(items = playlists, key = { it.id }) { p ->
                PlaylistTile(
                    playlist = p,
                    onClick = { onPlaylistClick(p) },
                    onDelete = { onDelete(p.id) },
                )
            }
        }
        item(key = "spotify_import", span = { GridItemSpan(maxLineSpan) }) {
            ImportFromSpotifyRow(onClick = onSpotifyImport)
        }
    }
}

@Composable
private fun EmptyPlaylistMessage() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "No playlists yet. Tap + to create one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
                .clip(RoundedCornerShape(4.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            SpotifyColors.LikedGradientStart,
                            SpotifyColors.LikedGradientEnd,
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
                text = "Liked Songs",
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
                .clip(RoundedCornerShape(4.dp))
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

@Composable
private fun PlaylistRow(
    playlist: PlaylistDto,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }

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
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (playlist.isAuto) {
                        Brush.linearGradient(
                            listOf(
                                SpotifyColors.LikedGradientStart,
                                SpotifyColors.LikedGradientEnd,
                            )
                        )
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
            Icon(
                imageVector = if (playlist.isAuto) Icons.Filled.AutoAwesome
                else Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = if (playlist.isAuto) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
            Text(
                text = if (playlist.isAuto) "Made for you • ${pluralizeSongs(playlist.songCount)}"
                else "Playlist • ${pluralizeSongs(playlist.songCount)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!playlist.isAuto) {
            IconButton(onClick = { confirmDelete = true }) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete playlist",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("\"${playlist.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
        )
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                // Auto-playlists are server-managed and can't be deleted; long-press
                // only fires the confirm dialog for user playlists.
                onLongClick = if (playlist.isAuto) null else { -> confirmDelete = true },
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (playlist.isAuto) {
                        Brush.linearGradient(
                            listOf(
                                SpotifyColors.LikedGradientStart,
                                SpotifyColors.LikedGradientEnd,
                            )
                        )
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
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (playlist.isAuto) "Made for you · ${pluralizeSongs(playlist.songCount)}"
            else pluralizeSongs(playlist.songCount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete playlist?") },
            text = { Text("\"${playlist.name}\" will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
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
                onClick = { onConfirm(text) },
                enabled = text.trim().isNotEmpty(),
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}


private fun pluralizeSongs(count: Int): String =
    if (count == 1) "1 song" else "$count songs"
