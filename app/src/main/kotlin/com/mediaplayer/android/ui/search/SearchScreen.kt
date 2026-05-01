package com.mediaplayer.android.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mediaplayer.android.R
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto
import com.mediaplayer.android.ui.common.CenteredMessage
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.SongCover
import com.mediaplayer.android.ui.common.SongListShimmer
import com.mediaplayer.android.ui.playlists.AddToPlaylistSheet
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.SpotifyColors

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = viewModel(),
    onSongClick: (SongDto) -> Unit = {},
    onPlayNext: ((SongDto) -> Unit)? = null,
    onAddToQueue: ((SongDto) -> Unit)? = null,
    onAlbumClick: (name: String, artist: String) -> Unit = { _, _ -> },
    onAlbumListClick: () -> Unit = {},
    onArtistClick: (name: String) -> Unit = {},
    onArtistListClick: () -> Unit = {},
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val likedIds by viewModel.likedIds.collectAsStateWithLifecycle()
    val downloadedIds by viewModel.downloadedIds.collectAsStateWithLifecycle()
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()

    var sheetSong by remember { mutableStateOf<SongDto?>(null) }
    val snackbar = remember { SnackbarHostState() }
    var lastAdded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastAdded) {
        val msg = lastAdded ?: return@LaunchedEffect
        snackbar.showSnackbar(msg)
        lastAdded = null
    }

    Column(modifier = modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onClear = viewModel::clearQuery,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.fillMaxSize()) {
            when (val s = state) {
                SearchUiState.Idle -> BrowseSections(
                    onAlbumListClick = onAlbumListClick,
                    onArtistListClick = onArtistListClick,
                )
                SearchUiState.Loading -> SongListShimmer()
                is SearchUiState.Success -> {
                    if (s.songs.isEmpty()) {
                        CenteredMessage(stringResource(R.string.search_no_matches))
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            if (query.isEmpty() && recentSongs.isNotEmpty()) {
                                item {
                                    RecentlyPlayedCarousel(
                                        songs = recentSongs,
                                        onSongClick = onSongClick,
                                    )
                                    }
                            }
                            items(items = s.songs, key = { it.id }) { song ->
                                SongRow(
                                    song = song,
                                    onClick = { onSongClick(song) },
                                    onLongPress = { sheetSong = song },
                                    isLiked = song.id in likedIds,
                                    onToggleLike = { viewModel.toggleLike(song.id) },
                                    isDownloaded = song.id in downloadedIds,
                                    onArtistClick = onArtistClick,
                                    onAlbumClick = onAlbumClick,
                                    onMore = { sheetSong = song },
                                )
                            }
                        }
                    }
                }
                is SearchUiState.Error -> ErrorWithRetry(
                    message = stringResource(R.string.search_error) + "\n" + s.message,
                    onRetry = viewModel::retry,
                )
            }

            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    sheetSong?.let { song ->
        AddToPlaylistSheet(
            songTitle = song.title,
            songId = song.id,
            onPlayNext = onPlayNext?.let { cb -> { cb(song); sheetSong = null } },
            onAddToQueue = onAddToQueue?.let { cb -> { cb(song); sheetSong = null } },
            onDownload = { viewModel.toggleDownload(song.id) },
            onDismiss = { sheetSong = null },
            onAdded = { playlistName ->
                lastAdded = "Added to $playlistName"
            },
        )
    }
}

@Composable
private fun RecentlyPlayedCarousel(
    songs: List<SongDto>,
    onSongClick: (SongDto) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Text(
            text = "Recently played",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = songs, key = { it.id }) { song ->
                RecentSongCard(song = song, onClick = { onSongClick(song) })
            }
        }
    }
}

@Composable
private fun RecentSongCard(song: SongDto, onClick: () -> Unit) {
    val shape = CoverShapes.Tile
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SongCover(
            song = song,
            size = 80.dp,
            shape = shape,
            contentDescription = "${song.title}, ${song.artist}",
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun BrowseSections(
    onAlbumListClick: () -> Unit,
    onArtistListClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Browse all",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BrowseTile(
                label = "Albums",
                color = SpotifyColors.BrowseAlbumsTile,
                icon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White) },
                onClick = onAlbumListClick,
                modifier = Modifier.weight(1f),
            )
            BrowseTile(
                label = "Artists",
                color = SpotifyColors.BrowseArtistsTile,
                icon = { Icon(Icons.Filled.Person, contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White) },
                onClick = onArtistListClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BrowseTile(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(96.dp)
            .clip(CoverShapes.Tile)
            .background(color)
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleLarge,
            color = androidx.compose.ui.graphics.Color.White,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
            icon()
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        singleLine = true,
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.content_desc_clear_query),
                    )
                }
            }
        },
    )
}

