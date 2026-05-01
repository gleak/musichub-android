package com.mediaplayer.android.ui.artists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.ArtistDetailDto
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.CoverShape
import com.mediaplayer.android.ui.common.SectionHeader
import com.mediaplayer.android.ui.common.SpotifyHero
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.search.SongRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArtistUiState {
    data object Loading : ArtistUiState
    data class Success(val detail: ArtistDetailDto) : ArtistUiState
    data class Error(val message: String) : ArtistUiState
}

@UnstableApi
class ArtistViewModel(
    private val name: String,
    private val repository: CatalogRepository = CatalogRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<ArtistUiState>(ArtistUiState.Loading)
    val state: StateFlow<ArtistUiState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val downloadedIds: StateFlow<Set<Long>> = DownloadRepository.downloadedIds

    init { load() }

    fun retry() { load() }

    private fun load() {
        viewModelScope.launch {
            _state.value = try {
                ArtistUiState.Success(repository.getArtist(name))
            } catch (t: Throwable) {
                ArtistUiState.Error(t.message ?: "Unknown error")
            }
        }
    }

    fun pullRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = try {
                ArtistUiState.Success(repository.getArtist(name))
            } catch (t: Throwable) {
                ArtistUiState.Error(t.message ?: "Unknown error")
            }
            _isRefreshing.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    artistName: String,
    onBack: () -> Unit,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onAlbumClick: (name: String, artist: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ArtistViewModel = viewModel(
        key = "artist-$artistName",
        factory = viewModelFactory {
            initializer { ArtistViewModel(artistName) }
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
                ArtistUiState.Loading -> CenteredSpinner()
                is ArtistUiState.Error -> ErrorWithRetry(
                    message = "Couldn't load artist.\n${s.message}",
                    onRetry = viewModel::retry,
                )
                is ArtistUiState.Success -> ArtistBody(
                    detail = s.detail,
                    downloadedIds = downloadedIds,
                    onPlayFromIndex = onPlayFromIndex,
                    onAlbumClick = onAlbumClick,
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
private fun ArtistBody(
    detail: ArtistDetailDto,
    downloadedIds: Set<Long>,
    onPlayFromIndex: (List<SongDto>, Int) -> Unit,
    onAlbumClick: (name: String, artist: String) -> Unit,
    onMoreSong: (SongDto) -> Unit,
) {
    val coverModel = detail.songs.firstOrNull { it.hasCoverArt }?.let { Network.coverUrl(it.id) }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "header") {
            SpotifyHero(
                title = detail.name,
                subtitle = "Artist • ${buildHeaderSubtitle(detail)}",
                coverModel = coverModel,
                coverShape = CoverShape.Circle,
                onPlay = { if (detail.songs.isNotEmpty()) onPlayFromIndex(detail.songs, 0) },
                onShuffle = {
                    if (detail.songs.isNotEmpty()) onPlayFromIndex(detail.songs.shuffled(), 0)
                },
                playEnabled = detail.songs.isNotEmpty(),
            )
        }

        if (detail.albums.isNotEmpty()) {
            item(key = "albums-header") {
                SectionHeader(
                    title = "Albums",
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            items(items = detail.albums, key = { "album-${it.name}" }) { album ->
                AlbumTile(album = album, onClick = { onAlbumClick(album.name, album.artist) })
            }
        }

        if (detail.songs.isNotEmpty()) {
            item(key = "songs-header") {
                SectionHeader(
                    title = "Popular",
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
            itemsIndexed(items = detail.songs, key = { _, song -> "song-${song.id}" }) { idx, song ->
                SongRow(
                    song = song,
                    isDownloaded = song.id in downloadedIds,
                    onClick = { onPlayFromIndex(detail.songs, idx) },
                    onMore = { onMoreSong(song) },
                )
            }
        }
    }
}

@Composable
private fun AlbumTile(album: AlbumDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = album.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (album.songCount == 1) "1 song" else "${album.songCount} songs",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildHeaderSubtitle(detail: ArtistDetailDto): String {
    val albumPart = if (detail.albums.size == 1) "1 album" else "${detail.albums.size} albums"
    val songPart = if (detail.songs.size == 1) "1 song" else "${detail.songs.size} songs"
    return "$albumPart · $songPart"
}

