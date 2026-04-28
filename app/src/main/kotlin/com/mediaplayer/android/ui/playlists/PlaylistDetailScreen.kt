package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.search.SongRow
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PlaylistDetailViewModel = viewModel(
        key = "playlist-$playlistId",
        factory = viewModelFactory {
            initializer { PlaylistDetailViewModel(playlistId) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var sheetSong by remember { mutableStateOf<SongDto?>(null) }
    var addSongsOpen by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var lastAdded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastAdded) {
        val msg = lastAdded ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        lastAdded = null
    }

    val successState = state as? PlaylistDetailUiState.Success

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    val title = successState?.playlist?.name ?: "Playlist"
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (successState != null) {
                        IconButton(
                            onClick = viewModel::pullRefresh,
                            enabled = !isRefreshing,
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                            }
                        }
                        IconButton(onClick = { addSongsOpen = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add songs")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                PlaylistDetailUiState.Loading -> CenteredSpinner()
                is PlaylistDetailUiState.Error -> CenteredMessage(
                    "Couldn't load playlist.\n${s.message}"
                )
                is PlaylistDetailUiState.Success -> {
                    val songIds = s.playlist.songs.map { it.id }
                    val downloadedCount = songIds.count { it in downloadedIds }
                    PlaylistDetailBody(
                        playlist = s.playlist,
                        downloadedCount = downloadedCount,
                        downloadedIds = downloadedIds,
                        onPlayFromIndex = onPlayFromIndex,
                        onShufflePlay = onShufflePlay,
                        onRemoveSong = viewModel::removeSong,
                        onReorderSongs = viewModel::reorderSongs,
                        onLongPressSong = { sheetSong = it },
                        onDownload = {
                            viewModel.downloadPlaylist()
                            val cm = context.getSystemService(ConnectivityManager::class.java)
                            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                            val onWifi = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
                            if (!onWifi) lastAdded = "Download queued — will start on Wi-Fi"
                        },
                        onRemoveDownloads = viewModel::removePlaylistDownloads,
                    )
                }
            }
        }
    }

    sheetSong?.let { song ->
        AddToPlaylistSheet(
            songTitle = song.title,
            songId = song.id,
            onDismiss = { sheetSong = null },
            onAdded = { playlistName ->
                lastAdded = "Added to $playlistName"
                viewModel.refresh()
                sheetSong = null
            },
        )
    }

    if (addSongsOpen && successState != null) {
        AddSongsToPlaylistSheet(
            playlistId = playlistId,
            existingSongIds = successState.playlist.songs.map { it.id }.toSet(),
            onDismiss = { addSongsOpen = false },
            onSongAdded = viewModel::refresh,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistDetailBody(
    playlist: PlaylistDetailDto,
    downloadedCount: Int,
    downloadedIds: Set<Long>,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onRemoveSong: (Long) -> Unit,
    onReorderSongs: (List<Long>) -> Unit,
    onLongPressSong: (SongDto) -> Unit,
    onDownload: () -> Unit,
    onRemoveDownloads: () -> Unit,
) {
    val lazyListState = rememberLazyListState()
    var songs by remember { mutableStateOf(playlist.songs) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(playlist.songs) {
        if (!isDragging) songs = playlist.songs
    }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Subtract 1 to account for the header item at LazyColumn index 0.
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        songs = songs.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    LaunchedEffect(reorderState) {
        var wasEverDragging = false
        snapshotFlow { reorderState.isAnyItemDragging }.collect { dragging ->
            isDragging = dragging
            if (dragging) {
                wasEverDragging = true
            } else if (wasEverDragging) {
                onReorderSongs(songs.map { it.id })
                wasEverDragging = false
            }
        }
    }

    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Header(
                playlist = playlist,
                downloadedCount = downloadedCount,
                onPlayAll = {
                    if (songs.isNotEmpty()) onPlayFromIndex(songs, 0)
                },
                onShufflePlay = { onShufflePlay(songs) },
                onDownload = onDownload,
                onRemoveDownloads = onRemoveDownloads,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (songs.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No songs yet. Add some from the Search tab or tap +.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id },
            ) { idx, song ->
                ReorderableItem(reorderState, key = song.id) {
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                onRemoveSong(song.id)
                                true
                            } else false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.errorContainer)
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        },
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                modifier = Modifier.draggableHandle(),
                                onClick = {},
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DragHandle,
                                    contentDescription = "Reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            SongRow(
                                song = song,
                                isDownloaded = song.id in downloadedIds,
                                onClick = { onPlayFromIndex(songs, idx) },
                                onLongPress = { onLongPressSong(song) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun Header(
    playlist: PlaylistDetailDto,
    downloadedCount: Int,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onDownload: () -> Unit,
    onRemoveDownloads: () -> Unit,
) {
    val total = playlist.songs.size
    val allDownloaded = total > 0 && downloadedCount == total

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (downloadedCount > 0 && !allDownloaded)
                    "${pluralizeSongsDetail(total)} · $downloadedCount downloaded"
                else
                    pluralizeSongsDetail(total),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val buttonPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            Button(
                onClick = onPlayAll,
                enabled = playlist.songs.isNotEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = buttonPadding,
            ) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Play", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OutlinedButton(
                onClick = onShufflePlay,
                enabled = playlist.songs.isNotEmpty(),
                modifier = Modifier.weight(1f),
                contentPadding = buttonPadding,
            ) {
                Icon(imageVector = Icons.Filled.Shuffle, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Shuffle", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (playlist.songs.isNotEmpty()) {
                OutlinedButton(
                    onClick = if (allDownloaded) onRemoveDownloads else onDownload,
                    modifier = Modifier.weight(1f),
                    contentPadding = buttonPadding,
                ) {
                    val isDownloaded = allDownloaded
                    Icon(
                        imageVector = if (isDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                        contentDescription = if (isDownloaded) "Remove downloads" else "Download",
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (isDownloaded) "Done" else "Download",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CenteredSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.size(32.dp))
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun pluralizeSongsDetail(count: Int): String =
    if (count == 1) "1 song" else "$count songs"
