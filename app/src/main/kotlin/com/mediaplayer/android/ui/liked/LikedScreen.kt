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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.SpotifyHero
import com.mediaplayer.android.ui.search.SongRow
import com.mediaplayer.android.ui.theme.SpotifyColors

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
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (val s = state) {
                LikedUiState.Loading -> CenteredSpinner()
                is LikedUiState.Error -> CenteredMessage("Couldn't load liked songs.\n${s.message}")
                is LikedUiState.Success -> LikedBody(
                    songs = s.songs,
                    downloadedIds = downloadedIds,
                    onPlayFromIndex = onPlayFromIndex,
                    onShufflePlay = onShufflePlay,
                    onUnlike = viewModel::unlike,
                )
            }
        }
    }
}

@Composable
private fun LikedBody(
    songs: List<SongDto>,
    downloadedIds: Set<Long>,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onUnlike: (Long) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            SpotifyHero(
                title = "Liked Songs",
                subtitle = "Playlist • ${if (songs.size == 1) "1 song" else "${songs.size} songs"}",
                coverModel = null,
                fallbackGradient = SpotifyColors.LikedGradientStart to SpotifyColors.LikedGradientEnd,
                onPlay = { if (songs.isNotEmpty()) onPlayFromIndex(songs, 0) },
                onShuffle = { if (songs.isNotEmpty()) onShufflePlay(songs) },
                playEnabled = songs.isNotEmpty(),
            )
        }

        if (songs.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No liked songs yet. Heart tracks from the Search tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            itemsIndexed(items = songs, key = { _, song -> song.id }) { idx, song ->
                SongRow(
                    song = song,
                    isLiked = true,
                    isDownloaded = song.id in downloadedIds,
                    onClick = { onPlayFromIndex(songs, idx) },
                    onToggleLike = { onUnlike(song.id) },
                )
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
