package com.mediaplayer.android.ui.liked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.EmptyState
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.SongListShimmer
import com.mediaplayer.android.ui.common.SpotifyHero
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.search.SongRow
import com.mediaplayer.android.ui.theme.MHColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LikedScreen(
    onBack: () -> Unit,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LikedViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()

    var sheetSong by remember { mutableStateOf<SongDto?>(null) }
    var addedToast by remember { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(addedToast) {
        addedToast?.let {
            snackbar.showSnackbar(it)
            addedToast = null
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbar) { data -> Snackbar(snackbarData = data) }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (val s = state) {
                LikedUiState.Loading -> SongListShimmer()
                is LikedUiState.Error -> ErrorWithRetry(
                    message = "Couldn't load liked songs.\n${s.message}",
                    onRetry = viewModel::refresh,
                )
                is LikedUiState.Success -> LikedBody(
                    songs = s.songs,
                    totalItems = s.totalItems,
                    loadingMore = s.loadingMore,
                    endReached = s.endReached,
                    downloadedIds = downloadedIds,
                    onPlayFromIndex = onPlayFromIndex,
                    onShufflePlay = onShufflePlay,
                    onUnlike = viewModel::unlike,
                    onLongPress = { song -> sheetSong = song },
                    onLoadMore = viewModel::loadMore,
                )
            }
        }
    }

    sheetSong?.let { song ->
        val dislike = com.mediaplayer.android.ui.common.rememberDislikeActions(song.id, song.artist)
        val flagWrong = com.mediaplayer.android.ui.common.rememberFlagWrongAction(
            songId = song.id,
            onFlagged = { viewModel.refresh() },
        )
        AddToPlaylistSheet(
            songTitle = song.title,
            songId = song.id,
            onDislikeSong = dislike.song(),
            onDislikeArtist = dislike.artist(),
            onFlagWrong = flagWrong,
            onDismiss = { sheetSong = null },
            onAdded = { playlistName ->
                addedToast = "Added to $playlistName"
            },
        )
    }
}

@Composable
private fun LikedBody(
    songs: List<SongDto>,
    totalItems: Long,
    loadingMore: Boolean,
    endReached: Boolean,
    downloadedIds: Set<Long>,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onUnlike: (Long) -> Unit,
    onLongPress: (SongDto) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    // Trigger when the user scrolls within PREFETCH_THRESHOLD rows of the
    // bottom. derivedStateOf keeps the recomputation cheap as the user
    // scrolls; snapshotFlow + filter ensures we fire only when crossing
    // the threshold, not on every visible-item delta.
    LaunchedEffect(listState, songs.size, endReached) {
        if (endReached) return@LaunchedEffect
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@snapshotFlow false
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - PREFETCH_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            SpotifyHero(
                title = "Liked Songs",
                subtitle = "Playlist • ${pluralizeSongs(totalItems)}",
                coverModel = null,
                fallbackGradient = MHColors.LikedGradientStart to MHColors.LikedGradientEnd,
                onPlay = { if (songs.isNotEmpty()) onPlayFromIndex(songs, 0) },
                onShuffle = { if (songs.isNotEmpty()) onShufflePlay(songs) },
                playEnabled = songs.isNotEmpty(),
            )
        }

        if (songs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.FavoriteBorder,
                    title = "No liked songs yet",
                    subtitle = "Heart tracks from the Search tab to find them here.",
                )
            }
        } else {
            itemsIndexed(items = songs, key = { _, song -> song.id }) { idx, song ->
                SongRow(
                    song = song,
                    isLiked = true,
                    isDownloaded = song.id in downloadedIds,
                    onClick = { onPlayFromIndex(songs, idx) },
                    onToggleLike = { onUnlike(song.id) },
                    onMore = { onLongPress(song) },
                )
            }
            if (loadingMore) {
                item(key = "loading-more") {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
            }
        }
    }
}

private fun pluralizeSongs(count: Long): String =
    if (count == 1L) "1 song" else "$count songs"

private const val PREFETCH_THRESHOLD = 5

