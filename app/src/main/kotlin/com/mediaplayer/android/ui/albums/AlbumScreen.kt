package com.mediaplayer.android.ui.albums

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.data.CatalogRepository
import com.mediaplayer.android.data.DownloadRepository
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.AlbumDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.SongListShimmer
import com.mediaplayer.android.ui.common.SpotifyHero
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.search.SongRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AlbumUiState {
    data object Loading : AlbumUiState
    data class Success(val detail: AlbumDetailDto) : AlbumUiState
    data class Error(val message: String) : AlbumUiState
}

@UnstableApi
class AlbumViewModel(
    private val name: String,
    private val artist: String,
    private val repository: CatalogRepository = CatalogRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val state: StateFlow<AlbumUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadedIds: StateFlow<Set<Long>> = DownloadRepository.downloadedIds

    init { load() }

    fun retry() { load() }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                AlbumUiState.Success(repository.getAlbum(name, artist))
            } catch (t: Throwable) {
                AlbumUiState.Error(friendlyMessage(t))
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                AlbumUiState.Success(repository.getAlbum(name, artist))
            } catch (t: Throwable) {
                AlbumUiState.Error(friendlyMessage(t))
            }
            _isRefreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    albumName: String,
    albumArtist: String,
    onBack: () -> Unit,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onArtistClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: AlbumViewModel = viewModel(
        key = "album-$albumName-$albumArtist",
        factory = viewModelFactory {
            initializer { AlbumViewModel(albumName, albumArtist) }
        },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()

    var sheetSong by remember { mutableStateOf<SongDto?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (val s = state) {
                AlbumUiState.Loading -> SongListShimmer()
                is AlbumUiState.Error -> ErrorWithRetry(
                    message = "Couldn't load album.\n${s.message}",
                    onRetry = viewModel::retry,
                )
                is AlbumUiState.Success -> AlbumBody(
                    detail = s.detail,
                    downloadedIds = downloadedIds,
                    onPlayFromIndex = onPlayFromIndex,
                    onShufflePlay = { songs ->
                        if (songs.isNotEmpty())
                            onPlayFromIndex(songs.shuffled(), 0)
                    },
                    onMoreSong = { song -> sheetSong = song },
                )
            }
        }
    }

    sheetSong?.let { song ->
        AddToPlaylistSheet(
            songTitle = song.title,
            songId = song.id,
            onDismiss = { sheetSong = null },
        )
    }
}

@Composable
private fun AlbumBody(
    detail: AlbumDetailDto,
    downloadedIds: Set<Long>,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onShufflePlay: (List<SongDto>) -> Unit,
    onMoreSong: (SongDto) -> Unit,
) {
    val coverModel = detail.songs.firstOrNull { it.hasCoverArt }?.let { Network.coverUrl(it.id) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            SpotifyHero(
                title = detail.name,
                subtitle = "Album · ${detail.artist} · ${detail.songs.size} brani",
                coverModel = coverModel,
                onPlay = { if (detail.songs.isNotEmpty()) onPlayFromIndex(detail.songs, 0) },
                onShuffle = { onShufflePlay(detail.songs) },
                playEnabled = detail.songs.isNotEmpty(),
            )
        }
        itemsIndexed(items = detail.songs, key = { _, song -> song.id }) { idx, song ->
            SongRow(
                song = song,
                isDownloaded = song.id in downloadedIds,
                onClick = { onPlayFromIndex(detail.songs, idx) },
                onMore = { onMoreSong(song) },
            )
        }
    }
}

