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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaplayer.android.data.dto.PlaylistDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.search.SongRow

/**
 * Playlist detail: header with big Play button + numbered song list.
 * Tapping a row kicks off playback from that index, giving users the
 * same "play from here" feel as the search screen.
 */
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    val title = (state as? PlaylistDetailUiState.Success)?.playlist?.name
                        ?: "Playlist"
                    Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
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
                is PlaylistDetailUiState.Success -> PlaylistDetailBody(
                    playlist = s.playlist,
                    onPlayFromIndex = onPlayFromIndex,
                    onShufflePlay = onShufflePlay,
                    onRemoveSong = viewModel::removeSong,
                )
            }
        }
    }
}

@Composable
private fun PlaylistDetailBody(
    playlist: PlaylistDetailDto,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onRemoveSong: (Long) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            Header(
                playlist = playlist,
                onPlayAll = {
                    if (playlist.songs.isNotEmpty()) {
                        onPlayFromIndex(playlist.songs, 0)
                    }
                },
                onShufflePlay = { onShufflePlay(playlist.songs) },
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }

        if (playlist.songs.isEmpty()) {
            item(key = "empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No songs yet. Add some from the Search tab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            itemsIndexed(
                items = playlist.songs,
                // Duplicates are legal — compose on (position, id) pair.
                key = { idx, song -> "$idx-${song.id}" },
            ) { idx, song ->
                SongRow(
                    song = song,
                    onClick = { onPlayFromIndex(playlist.songs, idx) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun Header(
    playlist: PlaylistDetailDto,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
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
                text = pluralizeSongsDetail(playlist.songs.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlayAll,
                    enabled = playlist.songs.isNotEmpty(),
                ) {
                    Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Play")
                }
                OutlinedButton(
                    onClick = onShufflePlay,
                    enabled = playlist.songs.isNotEmpty(),
                ) {
                    Icon(imageVector = Icons.Filled.Shuffle, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Shuffle")
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
