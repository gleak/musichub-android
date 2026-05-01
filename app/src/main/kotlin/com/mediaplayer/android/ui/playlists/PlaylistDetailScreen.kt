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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import com.mediaplayer.android.data.PlaylistRepository
import android.content.Intent
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
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.SpotifyHero
import com.mediaplayer.android.ui.search.SongRow
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.graphics.Color
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
    var sharing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    var snackMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val playlistRepository = remember { PlaylistRepository() }

    LaunchedEffect(snackMessage) {
        val msg = snackMessage ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        snackMessage = null
    }

    val successState = state as? PlaylistDetailUiState.Success

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
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
                        IconButton(
                            onClick = {
                                if (sharing) return@IconButton
                                sharing = true
                                scope.launch {
                                    try {
                                        val link = playlistRepository.createShare(playlistId)
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_SUBJECT,
                                                "Listen to ${successState.playlist.name}",
                                            )
                                            putExtra(Intent.EXTRA_TEXT, link.url)
                                        }
                                        // Wrap in chooser so the user picks WhatsApp /
                                        // Messages / etc. Each tap mints a new token,
                                        // so cancelling the chooser leaves a token
                                        // alive but unused — harmless.
                                        context.startActivity(
                                            Intent.createChooser(intent, "Share playlist")
                                        )
                                    } catch (t: Throwable) {
                                        snackMessage = t.message ?: "Couldn't create share link"
                                    } finally {
                                        sharing = false
                                    }
                                }
                            },
                            enabled = !sharing,
                        ) {
                            if (sharing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Share, contentDescription = "Share playlist")
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
                is PlaylistDetailUiState.Error -> ErrorWithRetry(
                    message = "Couldn't load playlist.\n${s.message}",
                    onRetry = viewModel::refresh,
                )
                is PlaylistDetailUiState.Success -> {
                    val songIds = s.playlist.songs.map { it.song.id }
                    val downloadedCount = songIds.count { it in downloadedIds }
                    PlaylistDetailBody(
                        playlist = s.playlist,
                        downloadedCount = downloadedCount,
                        downloadedIds = downloadedIds,
                        onPlayFromIndex = onPlayFromIndex,
                        onShufflePlay = onShufflePlay,
                        onRemoveSong = { songId ->
                            viewModel.removeSong(songId)
                            snackMessage = "Removed from playlist"
                        },
                        onReorderSongs = viewModel::reorderSongs,
                        onLongPressSong = { sheetSong = it },
                        onDownload = {
                            viewModel.downloadPlaylist()
                            val cm = context.getSystemService(ConnectivityManager::class.java)
                            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
                            val onWifi = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
                            if (!onWifi) snackMessage = "Download queued — will start on Wi-Fi"
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
                snackMessage = "Added to $playlistName"
                viewModel.refresh()
                sheetSong = null
            },
        )
    }

    if (addSongsOpen && successState != null) {
        AddSongsToPlaylistSheet(
            playlistId = playlistId,
            existingSongIds = successState.playlist.songs.map { it.song.id }.toSet(),
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
    // `entries` holds PlaylistSongEntryDto so each row carries its
    // playlist_songs.id — that's the stable per-occurrence key the
    // LazyColumn needs for `Modifier.animateItem()` to fire on reorder
    // (song.id alone collides on duplicate songs).
    var entries by remember { mutableStateOf(playlist.songs) }
    val songsForPlayback: List<SongDto> = entries.map { it.song }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Subtract 1 to account for the header item at LazyColumn index 0.
        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex !in entries.indices || toIndex !in entries.indices) return@rememberReorderableLazyListState
        entries = entries.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
    }

    // Sync from server only when no drag is in flight. Without the guard a
    // mid-drag refresh would yank the local list out from under the gesture.
    LaunchedEffect(playlist.songs) {
        if (!reorderState.isAnyItemDragging) entries = playlist.songs
    }

    LaunchedEffect(reorderState) {
        var wasEverDragging = false
        snapshotFlow { reorderState.isAnyItemDragging }.collect { dragging ->
            if (dragging) {
                wasEverDragging = true
            } else if (wasEverDragging) {
                onReorderSongs(entries.map { it.song.id })
                wasEverDragging = false
            }
        }
    }

    LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            val total = playlist.songs.size
            val allDownloaded = total > 0 && downloadedCount == total
            SpotifyHero(
                title = playlist.name,
                subtitle = if (downloadedCount > 0 && !allDownloaded)
                    "Playlist • ${pluralizeSongsDetail(total)} • $downloadedCount downloaded"
                else "Playlist • ${pluralizeSongsDetail(total)}",
                coverModel = null,
                onPlay = { if (entries.isNotEmpty()) onPlayFromIndex(songsForPlayback, 0) },
                onShuffle = { onShufflePlay(songsForPlayback) },
                playEnabled = entries.isNotEmpty(),
                extraActions = {
                    if (playlist.songs.isNotEmpty()) {
                        IconButton(
                            onClick = if (allDownloaded) onRemoveDownloads else onDownload,
                        ) {
                            Icon(
                                imageVector = if (allDownloaded) Icons.Filled.CloudDone else Icons.Filled.CloudDownload,
                                contentDescription = if (allDownloaded) "Remove downloads" else "Download",
                                tint = if (allDownloaded) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        }

        if (entries.isEmpty()) {
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
                items = entries,
                key = { _, entry -> entry.playlistSongId },
            ) { idx, entry ->
                val song = entry.song
                ReorderableItem(reorderState, key = entry.playlistSongId) {
                    // Canonical Material3 pattern: don't fire side effects from
                    // confirmValueChange (it can be invoked multiple times during
                    // a single gesture, and ignored values get re-tried). Observe
                    // currentValue settling instead. `removed` guards re-entry
                    // because the row stays composed until server response trims
                    // `entries`.
                    var removed by remember(entry.playlistSongId) { mutableStateOf(false) }
                    val dismissState = rememberSwipeToDismissBoxState()
                    LaunchedEffect(dismissState.currentValue) {
                        if (!removed && dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            removed = true
                            // Drop the row locally first so the LazyColumn unmounts
                            // it instead of leaving the SwipeToDismissBox stuck in
                            // its dismissed (background-only) state while the server
                            // round-trip is in flight. On API failure the VM's
                            // refresh() path re-syncs and the row reappears.
                            entries = entries.filterNot { it.playlistSongId == entry.playlistSongId }
                            onRemoveSong(song.id)
                        }
                    }
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
                                onClick = { onPlayFromIndex(songsForPlayback, idx) },
                                onLongPress = { onLongPressSong(song) },
                                onMore = { onLongPressSong(song) },
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


private fun pluralizeSongsDetail(count: Int): String =
    if (count == 1) "1 song" else "$count songs"
