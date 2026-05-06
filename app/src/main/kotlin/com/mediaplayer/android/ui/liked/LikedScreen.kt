package com.mediaplayer.android.ui.liked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.ui.text.style.TextAlign
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.ui.theme.LocalMHMono
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

    val kebab = com.mediaplayer.android.ui.common.rememberSongKebab()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
                    onLongPress = { song -> kebab.open(song) },
                    onLoadMore = viewModel::loadMore,
                )
            }
        }
    }

    com.mediaplayer.android.ui.common.SongKebabSheet(
        state = kebab,
        onFlagged = { viewModel.refresh() },
        onAdded = { playlistName, _ -> addedToast = "Added to $playlistName" },
    )
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
    onLongPress: (SongDto) -> Unit,
    onLoadMore: () -> Unit,
) {
    // Mark every loaded song as liked in the shared cache so each row's heart
    // renders filled instantly (no per-row server round-trip via prime()).
    LaunchedEffect(songs) {
        songs.forEach { com.mediaplayer.android.data.LikedSongsCache.markLiked(it.id, true) }
    }
    // Drop rows the user has unliked anywhere in the app — keeps the list
    // honest with the heart toggle without waiting for the next refresh.
    val likedIds by com.mediaplayer.android.data.LikedSongsCache.likedIds.collectAsStateWithLifecycle()
    val visibleSongs = songs.filter { it.id in likedIds }
    val listState = rememberLazyListState()

    // Prefetch the next page once the user scrolls within
    // PREFETCH_THRESHOLD rows of the tail; goes silent at end-of-list.
    com.mediaplayer.android.ui.common.PrefetchNearEnd(
        listState = listState,
        threshold = PREFETCH_THRESHOLD,
        enabled = !endReached,
        onLoadMore = onLoadMore,
    )

    val allDownloaded = visibleSongs.isNotEmpty() && visibleSongs.all { it.id in downloadedIds }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            SpotifyHero(
                title = "Brani che ti piacciono",
                subtitle = buildLikedSubtitle(totalItems, visibleSongs),
                coverModel = null,
                fallbackGradient = MHColors.LikedGradientStart to MHColors.LikedGradientEnd,
                eyebrow = "LIBRERIA · MI PIACE",
                subtitleStyle = com.mediaplayer.android.ui.common.SubtitleStyle.Mono,
                onPlay = { if (visibleSongs.isNotEmpty()) onPlayFromIndex(visibleSongs, 0) },
                onShuffle = { if (visibleSongs.isNotEmpty()) onShufflePlay(visibleSongs) },
                playEnabled = visibleSongs.isNotEmpty(),
                extraActions = {
                    if (visibleSongs.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                if (allDownloaded) {
                                    DownloadRepository.removeAll(visibleSongs.map { it.id })
                                } else {
                                    val missing = visibleSongs
                                        .filterNot { it.id in downloadedIds }
                                        .map { it.id to it.title }
                                    DownloadRepository.downloadAllLabeled(missing)
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (allDownloaded) Icons.Filled.CloudDone
                                              else Icons.Filled.CloudDownload,
                                contentDescription = if (allDownloaded) "Rimuovi scaricati"
                                                     else "Scarica tutti",
                                tint = if (allDownloaded) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                },
            )
        }

        if (visibleSongs.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Filled.FavoriteBorder,
                    title = "Nessun brano che ti piace",
                    subtitle = "Tocca il cuore su qualunque traccia: la troverai qui, sempre offline-ready.",
                )
            }
        } else {
            itemsIndexed(items = visibleSongs, key = { _, song -> song.id }) { idx, song ->
                IndexedSongRow(
                    index = idx + 1,
                    song = song,
                    isDownloaded = song.id in downloadedIds,
                    onClick = { onPlayFromIndex(visibleSongs, idx) },
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
    if (count == 1L) "1 brano" else "$count brani"

// Mockup specs `"284 brani · 18h 42m"`. The total count is server-authoritative
// (`totalItems`); duration sums whatever pages are loaded. When the loaded
// subset matches the total the duration is exact; otherwise we still show it
// as a directional read with the "/N totale" suffix on the count so the user
// understands the duration trails the count during paging.
private fun buildLikedSubtitle(totalItems: Long, loaded: List<SongDto>): String {
    val countLabel = pluralizeSongs(totalItems)
    if (loaded.isEmpty()) return countLabel
    val totalMs = loaded.sumOf { it.durationMs }
    val totalMinutes = (totalMs / 60_000L).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val durationLabel = when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> null
    }
    return if (durationLabel != null) "$countLabel · $durationLabel" else countLabel
}

private const val PREFETCH_THRESHOLD = 5

/**
 * SongRow with a leading mono numeric index column. Mockup shows
 * 18dp right-aligned index ahead of the cover (`mh-library.jsx:137`).
 * Wraps SongRow rather than threading a leading slot through it,
 * since this convention is unique to Liked.
 */
@Composable
private fun IndexedSongRow(
    index: Int,
    song: SongDto,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString(),
            style = mono.duration.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = TextAlign.End,
            modifier = Modifier.padding(start = 12.dp).width(22.dp),
        )
        SongRow(
            song = song,
            isDownloaded = isDownloaded,
            onClick = onClick,
            onMore = onMore,
            modifier = Modifier.weight(1f),
        )
    }
}

